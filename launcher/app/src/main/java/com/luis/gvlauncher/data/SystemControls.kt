package com.luis.gvlauncher.data

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.app.Instrumentation
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.Settings

/**
 * Brillo, volumen, WiFi y Bluetooth para la pantalla Ajustes.
 * Ojo: en algunas radios chinas el brillo y el volumen los maneja el MCU y no Android;
 * en ese caso estos controles pueden no tener efecto y conviene usar los botones físicos.
 */
class SystemControls(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    // ---- Brillo (0..1) ----
    fun canWriteSettings() = Settings.System.canWrite(context)

    fun brightness(): Float = runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    }.getOrDefault(0.7f)

    fun setBrightness(value: Float): Boolean {
        if (!canWriteSettings()) return false
        return runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS,
                (value.coerceIn(0.05f, 1f) * 255).toInt())
        }.getOrDefault(false)
    }

    /** Abre la pantalla de Android para dar permiso de "modificar ajustes del sistema". */
    fun requestWriteSettings() = runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---- Volumen ----
    // En esta radio el volumen real lo maneja el chip de audio (la ventanita con el número 0..40),
    // no el volumen de Android, que se queda siempre al máximo. Por eso no se muestra un porcentaje:
    // los botones − / + hacen lo mismo que los botones físicos de volumen de la radio.

    /** Sube (+1) o baja (−1) el volumen como el botón físico. */
    fun volumeStep(up: Boolean) =
        pressVolumeKey(if (up) KeyEvent.KEYCODE_VOLUME_UP else KeyEvent.KEYCODE_VOLUME_DOWN) {
            audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI,
            )
        }

    /** Silencia o quita el silencio. */
    fun toggleMute() = pressVolumeKey(KeyEvent.KEYCODE_VOLUME_MUTE) {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Simula la tecla de volumen dentro del launcher (igual que el botón físico, así el firmware
     * muestra su ventanita y cambia el volumen real). Si el sistema no lo permite, usa el
     * volumen de Android como respaldo.
     */
    private fun pressVolumeKey(keyCode: Int, fallback: () -> Unit) {
        Thread {
            val ok = runCatching { Instrumentation().sendKeyDownUpSync(keyCode) }.isSuccess
            if (!ok) Handler(Looper.getMainLooper()).post { runCatching(fallback) }
        }.start()
    }

    // ---- WiFi ----
    fun wifiOn() = runCatching { wifi?.isWifiEnabled == true }.getOrDefault(false)

    @Suppress("DEPRECATION") // targetSdk 28: todavía permitido en Android 8/9
    fun setWifi(on: Boolean) = runCatching { wifi?.isWifiEnabled = on }

    fun wifiName(): String? = runCatching {
        @Suppress("DEPRECATION")
        wifi?.connectionInfo?.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    }.getOrNull()

    // ---- Bluetooth ----
    @Suppress("DEPRECATION", "MissingPermission")
    fun bluetoothOn() = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)

    @Suppress("DEPRECATION", "MissingPermission")
    fun setBluetooth(on: Boolean) = runCatching {
        val bt = BluetoothAdapter.getDefaultAdapter() ?: return@runCatching
        if (on) bt.enable() else bt.disable()
    }
}
