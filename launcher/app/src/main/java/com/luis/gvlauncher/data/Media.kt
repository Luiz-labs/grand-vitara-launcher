package com.luis.gvlauncher.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.app.Notification
import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Con el permiso "Acceso a notificaciones", el sistema nos deja leer las sesiones de música
 * de otras apps. Además guardamos las notificaciones de las apps de la radio (Bluetooth y FM):
 * muchas radios chinas no publican una sesión de música, pero sí muestran la canción o la
 * emisora en su notificación.
 */
class MediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        runCatching { activeNotifications?.forEach(::store) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = store(sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotifBridge.remove(sbn.packageName)
    }

    private fun store(sbn: StatusBarNotification) {
        if (sbn.packageName !in NotifBridge.watched) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val art = runCatching { extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON) }.getOrNull()
        NotifBridge.put(sbn.packageName, NotifInfo(title, text, art))
    }
}

data class NotifInfo(val title: String, val text: String, val art: Bitmap?)

/** Último texto que mostró cada app de la radio en su notificación. */
object NotifBridge {
    /** Se completa desde MainActivity con los paquetes de Bluetooth y Radio de Config. */
    @Volatile var watched: Set<String> = emptySet()
    private val _items = MutableStateFlow<Map<String, NotifInfo>>(emptyMap())
    val items: StateFlow<Map<String, NotifInfo>> = _items

    fun put(pkg: String, info: NotifInfo) { _items.value = _items.value + (pkg to info) }
    fun remove(pkg: String) { _items.value = _items.value - pkg }
}

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val durationMs: Long,
    val positionMs: Long,
    val positionAt: Long,
    val playing: Boolean,
    val sourcePackage: String,
) {
    fun currentPosition(now: Long = SystemClock.elapsedRealtime()): Long =
        if (playing) (positionMs + (now - positionAt)).coerceAtMost(durationMs.coerceAtLeast(positionMs))
        else positionMs
}

/**
 * Tracks the active media session of whatever app is playing (Spotify on the
 * unit, the manufacturer's BT-music app, etc.). Requires notification-listener
 * access, granted once over ADB.
 */
class MediaRepository(private val context: Context) {

    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state

    /** Todas las sesiones activas, por paquete (para elegir Bluetooth o Radio en la tarjeta). */
    private val _all = MutableStateFlow<Map<String, NowPlaying>>(emptyMap())
    val all: StateFlow<Map<String, NowPlaying>> = _all
    private var allControllers: List<MediaController> = emptyList()
    private val audio = context.getSystemService(AudioManager::class.java)

    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = attach(null)
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        allControllers = list.orEmpty()
        attach(pickController(list))
    }

    val hasAccess: Boolean
        get() = runCatching { manager.getActiveSessions(listenerComponent); true }.getOrDefault(false)

    fun start() {
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent)
            val list = manager.getActiveSessions(listenerComponent)
            allControllers = list.orEmpty()
            attach(pickController(list))
        }
    }

    fun stop() {
        runCatching { manager.removeOnActiveSessionsChangedListener(sessionsListener) }
        attach(null)
    }

    /**
     * Controles. [preferred] = paquetes de la fuente elegida en la tarjeta (Bluetooth o Radio).
     * Si no hay sesión de música (pasa con las apps de la radio), se envía la tecla multimedia,
     * igual que los botones del timón.
     */
    fun playPause(preferred: Set<String> = emptySet()) {
        val c = controllerFor(preferred) ?: return key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next(preferred: Set<String> = emptySet()) {
        controllerFor(preferred)?.transportControls?.skipToNext() ?: key(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previous(preferred: Set<String> = emptySet()) {
        controllerFor(preferred)?.transportControls?.skipToPrevious() ?: key(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    private fun controllerFor(preferred: Set<String>): MediaController? =
        if (preferred.isEmpty()) controller else allControllers.firstOrNull { it.packageName in preferred }

    private fun key(code: Int) {
        runCatching {
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
    }
    fun seekTo(ms: Long) = controller?.transportControls?.seekTo(ms)

    private fun pickController(list: List<MediaController>?): MediaController? =
        list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()

    private fun attach(new: MediaController?) {
        if (new?.sessionToken == controller?.sessionToken) return publish()
        controller?.unregisterCallback(callback)
        controller = new
        new?.registerCallback(callback)
        publish()
    }

    private fun publish() {
        _all.value = allControllers.mapNotNull { mc -> toNowPlaying(mc)?.let { mc.packageName to it } }.toMap()
        _state.value = controller?.let(::toNowPlaying)
    }

    private fun toNowPlaying(c: MediaController): NowPlaying? {
        val md = c.metadata ?: return null
        val ps = c.playbackState
        return NowPlaying(
            title = md.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: "",
            artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
            art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
            durationMs = md.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0),
            positionMs = ps?.position ?: 0,
            positionAt = ps?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            playing = ps?.state == PlaybackState.STATE_PLAYING,
            sourcePackage = c.packageName,
        )
    }
}
