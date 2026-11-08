package com.luis.gvlauncher

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.gvlauncher.data.AppEntry
import com.luis.gvlauncher.data.Apps
import com.luis.gvlauncher.data.CarImages
import com.luis.gvlauncher.data.CarProfile
import com.luis.gvlauncher.data.Contact
import com.luis.gvlauncher.data.FavoritesStore
import com.luis.gvlauncher.data.HomeMode
import com.luis.gvlauncher.data.LocationRepository
import com.luis.gvlauncher.data.MediaRepository
import com.luis.gvlauncher.data.MusicCardMode
import com.luis.gvlauncher.data.NotifBridge
import com.luis.gvlauncher.data.NowPlaying
import com.luis.gvlauncher.data.SaverEffect
import com.luis.gvlauncher.data.NavApp
import com.luis.gvlauncher.data.Place
import com.luis.gvlauncher.data.ProfileStore
import com.luis.gvlauncher.data.SystemControls
import com.luis.gvlauncher.data.Weather
import com.luis.gvlauncher.data.WeatherRepository
import com.luis.gvlauncher.ui.AppBackground
import com.luis.gvlauncher.ui.AppDrawer
import com.luis.gvlauncher.ui.CustomizeScreen
import com.luis.gvlauncher.ui.DESIGN_H
import com.luis.gvlauncher.ui.DESIGN_W
import com.luis.gvlauncher.ui.DriveHomeScreen
import com.luis.gvlauncher.ui.HomeActions
import com.luis.gvlauncher.ui.HomeScreen
import com.luis.gvlauncher.ui.HomeState
import com.luis.gvlauncher.ui.Ink
import com.luis.gvlauncher.ui.LocalInk
import com.luis.gvlauncher.ui.LocalUnit
import com.luis.gvlauncher.ui.MusicActions
import com.luis.gvlauncher.ui.MusicScreen
import com.luis.gvlauncher.ui.MusicSource
import com.luis.gvlauncher.ui.NavActions
import com.luis.gvlauncher.ui.NavScreen
import com.luis.gvlauncher.ui.Palette
import com.luis.gvlauncher.ui.PhoneActions
import com.luis.gvlauncher.ui.PhoneScreen
import com.luis.gvlauncher.ui.Screen
import com.luis.gvlauncher.ui.SettingsActions
import com.luis.gvlauncher.ui.SettingsScreen
import com.luis.gvlauncher.ui.ScreenSaver
import com.luis.gvlauncher.ui.SideRail
import com.luis.gvlauncher.ui.SystemState
import com.luis.gvlauncher.ui.musicSourceIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var media: MediaRepository
    private lateinit var location: LocationRepository
    private lateinit var store: ProfileStore
    private lateinit var favorites: FavoritesStore
    private lateinit var system: SystemControls

    private var weather by mutableStateOf<Weather?>(null)
    private var bluetoothOn by mutableStateOf(false)

    /** Pantalla actual de la barra lateral. */
    private var screen by mutableStateOf(Screen.HOME)
    private var drawerApps by mutableStateOf<List<AppEntry>>(emptyList())
    private var customizing by mutableStateOf(false)
    /** Asistente de la primera vez (o repetido desde Ajustes). */
    private var wizardOn by mutableStateOf(false)
    /** Versión nueva disponible en GitHub, si la hay. */
    private var update by mutableStateOf<com.luis.gvlauncher.data.UpdateInfo?>(null)
    /** Descarga de la actualización: null = sin descargar, 0..1 = progreso, -1 = sin tamaño. */
    private var updateProgress by mutableStateOf<Float?>(null)
    private var updateError by mutableStateOf<String?>(null)
    /** Texto del botón "Buscar actualizaciones" en Ajustes. */
    private var updateStatus by mutableStateOf("Toca para revisar")
    private val scope = kotlinx.coroutines.MainScope()
    /** Pantalla de carga con el carrusel: una vez cada vez que arranca el launcher. */
    private var splashOn by mutableStateOf(!splashShown)

    private var profile by mutableStateOf(CarProfile(Config.DRIVER_NAME, Config.DEFAULT_MODEL, Config.DEFAULT_PAINT))
    private var carImage by mutableStateOf<ImageBitmap?>(null)
    /** Sube cada vez que cambia la foto propia, para volver a dibujarla aunque el perfil sea igual. */
    private var photoVersion by mutableIntStateOf(0)

    private var places by mutableStateOf<List<Place>>(emptyList())
    private var contacts by mutableStateOf<List<Contact>>(emptyList())
    private var homeAddress by mutableStateOf("")
    private var workAddress by mutableStateOf("")
    private var sys by mutableStateOf(SystemState())
    private var notificationAccess by mutableStateOf(false)
    private var mediaStarted = false

    /** Protector de pantalla: visible y último toque del usuario. */
    private var saverOn by mutableStateOf(false)
    private var lastTouch = android.os.SystemClock.uptimeMillis()

    /** Paquetes que cuentan como "Bluetooth" o "Radio" para la tarjeta de música. */
    private val btPackages by lazy { Config.BLUETOOTH_MEDIA_SOURCES + Config.BT_MUSIC.packages }
    private val radioPackages by lazy { Config.RADIO.packages.toSet() }

    /** Paquetes a los que se dirigen los botones de música según la fuente elegida. */
    private fun preferredPackages(): Set<String> = when (profile.musicCard) {
        MusicCardMode.AUTO -> emptySet()
        MusicCardMode.BLUETOOTH -> btPackages
        MusicCardMode.RADIO -> radioPackages
    }

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        if (store.importCustom(uri)) photoVersion++ else toast("No se pudo leer esa imagen")
    }

    /** -1 = automatic (GPS), otherwise index into [Config.CITIES]. */
    private var cityIndex by mutableIntStateOf(-1)
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshSystem()
    }

    /** Fuentes de la pantalla Música, en el orden pedido. */
    private val musicSources by lazy {
        listOf(
            MusicSource("Bluetooth", musicSourceIcon("Bluetooth"), Config.BT_MUSIC, Config.BLUETOOTH_MEDIA_SOURCES),
            MusicSource("Apple Music", musicSourceIcon(""), Config.APPLE_MUSIC, Config.APPLE_MUSIC.packages.toSet()),
            MusicSource("Spotify", musicSourceIcon(""), Config.SPOTIFY, Config.SPOTIFY.packages.toSet()),
            MusicSource("YouTube Music", musicSourceIcon(""), Config.YOUTUBE_MUSIC, Config.YOUTUBE_MUSIC.packages.toSet()),
            MusicSource("USB", musicSourceIcon("USB"), Config.USB_MUSIC, Config.USB_MUSIC.packages.toSet()),
            MusicSource("Radio FM", musicSourceIcon("Radio FM"), Config.RADIO, Config.RADIO.packages.toSet()),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        media = MediaRepository(applicationContext)
        location = LocationRepository(applicationContext)
        store = ProfileStore(applicationContext)
        wizardOn = !store.wizardDone()
        favorites = FavoritesStore(applicationContext)
        NotifBridge.watched = btPackages + radioPackages
        system = SystemControls(applicationContext)
        profile = store.load()
        places = favorites.places()
        contacts = favorites.contacts()
        homeAddress = favorites.homeAddress
        workAddress = favorites.workAddress
        cityIndex = prefs.getInt("city", -1).coerceIn(-1, Config.CITIES.lastIndex)
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CALL_PHONE,
            ),
            1,
        )

        val homeActions = HomeActions(
            openNavigation = { openNavApp() },
            wazeHome = { goFavorite("home", homeAddress) },
            wazeWork = { goFavorite("work", workAddress) },
            openPhone = { goTo(Screen.PHONE) },
            openMusic = {
                when (profile.musicCard) {
                    MusicCardMode.AUTO -> openMusicApp()
                    MusicCardMode.BLUETOOTH -> open(Config.BT_MUSIC, "Música BT")
                    MusicCardMode.RADIO -> open(Config.RADIO, "Radio FM")
                }
            },
            openRadio = { open(Config.RADIO, "Radio FM") },
            openEqualizer = { open(Config.EQUALIZER, "Ecualizador") },
            openCarPlay = { open(Config.CARPLAY, "CarPlay") },
            openApps = { goTo(Screen.APPS) },
            openCustomize = { customizing = true },
            openSettings = { goTo(Screen.SETTINGS) },
            cycleCity = {
                cityIndex = if (cityIndex >= Config.CITIES.lastIndex) -1 else cityIndex + 1
                prefs.edit().putInt("city", cityIndex).apply()
                weather = null
            },
            playPause = { media.playPause(preferredPackages()) },
            next = { media.next(preferredPackages()) },
            previous = { media.previous(preferredPackages()) },
            goPlace = { if (!Apps.navigateTo(this, profile.navApp, it.address)) notInstalled(profile.navApp.label) },
            openSaver = { saverOn = true },
            openNavScreen = { goTo(Screen.NAV) },
            openMusicScreen = { goTo(Screen.MUSIC) },
            switchMode = {
                val next = if (profile.homeMode == HomeMode.CONDUCCION) HomeMode.COMPLETO else HomeMode.CONDUCCION
                updateProfile(profile.copy(homeMode = next))
            },
        )

        val navActions = NavActions(
            search = { if (!Apps.navigateTo(this, profile.navApp, it)) notInstalled(profile.navApp.label) },
            goHome = { goFavorite("home", homeAddress) },
            goWork = { goFavorite("work", workAddress) },
            goPlace = { if (!Apps.navigateTo(this, profile.navApp, it.address)) notInstalled(profile.navApp.label) },
            addPlace = { places = places + it; favorites.savePlaces(places) },
            deletePlace = { p -> places = places - p; favorites.savePlaces(places) },
            setHomeAddress = { homeAddress = it; favorites.homeAddress = it },
            setWorkAddress = { workAddress = it; favorites.workAddress = it },
            switchApp = {
                val next = NavApp.entries[(profile.navApp.ordinal + 1) % NavApp.entries.size]
                updateProfile(profile.copy(navApp = next))
            },
        )

        val phoneActions = PhoneActions(
            call = { if (!Apps.call(this, it)) toast("No se pudo llamar. ¿Está conectado el celular por Bluetooth?") },
            addContact = { contacts = contacts + it; favorites.saveContacts(contacts) },
            deleteContact = { c -> contacts = contacts - c; favorites.saveContacts(contacts) },
            openRadioPhone = { open(Config.PHONE, "Teléfono") },
        )

        val musicActions = MusicActions(
            openSource = { open(it.shortcut, it.label) },
            openCurrent = {
                val playing = media.state.value?.sourcePackage
                if (playing == null || !Apps.launch(this, playing)) openMusicApp()
            },
            playPause = { media.playPause() },
            next = { media.next() },
            previous = { media.previous() },
        )

        val settingsActions = SettingsActions(
            setBrightness = { system.setBrightness(it); sys = sys.copy(brightness = it) },
            requestBrightnessPermission = { system.requestWriteSettings() },
            volumeStep = { up -> system.volumeStep(up) },
            toggleMute = { system.toggleMute() },
            setWifi = { system.setWifi(it); sys = sys.copy(wifiOn = it) },
            setBluetooth = { system.setBluetooth(it); sys = sys.copy(bluetoothOn = it) },
            setTheme = { updateProfile(profile.copy(theme = it)) },
            setMusicApp = { updateProfile(profile.copy(musicApp = it)) },
            setNavApp = { updateProfile(profile.copy(navApp = it)) },
            openCustomize = { customizing = true },
            openCarSettings = { open(Config.CAR_SETTINGS, "Ajustes del auto") },
            openAndroidSettings = { Apps.openSettings(this) },
            openWifiList = { if (!Apps.openWifiSettings(this)) notInstalled("Lista de WiFi") },
            openBluetoothList = { if (!Apps.openBluetoothPairing(this, Config.PHONE)) notInstalled("Bluetooth") },
            setNavCard = { updateProfile(profile.copy(navCard = it)) },
            setMusicCard = { updateProfile(profile.copy(musicCard = it)) },
            setWallpaper = { updateProfile(profile.copy(wallpaper = it)) },
            openNotificationAccess = {
                if (!startSafely(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))) {
                    toast("Abre Activity Launcher y busca NotificationAccess")
                }
            },
            setSaverMinutes = { updateProfile(profile.copy(saverMinutes = it)) },
            setSaverEffect = { updateProfile(profile.copy(saverEffect = it)) },
            previewSaver = { saverOn = true },
            openWizard = { wizardOn = true },
            checkUpdates = { checkUpdatesNow() },
            setHomeMode = { updateProfile(profile.copy(homeMode = it)) },
            openHomeChooser = {
                if (!startSafely(Intent(android.provider.Settings.ACTION_HOME_SETTINGS)) &&
                    !startSafely(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                ) {
                    toast("Tu radio no tiene esa pantalla. Usa Activity Launcher → HomeSettings")
                }
            },
        )

        setContent {
            val nowPlaying by media.state.collectAsStateWithLifecycle()
            val fix by location.fix.collectAsStateWithLifecycle()
            val allSessions by media.all.collectAsStateWithLifecycle()
            val notifs by NotifBridge.items.collectAsStateWithLifecycle()

            // Lo que muestra la tarjeta de música según la fuente elegida en Ajustes.
            fun fromNotif(pkgs: Set<String>): NowPlaying? = notifs.entries.firstOrNull { it.key in pkgs }?.let { (pkg, n) ->
                NowPlaying(n.title, n.text, n.art, 0, 0, android.os.SystemClock.elapsedRealtime(), false, pkg)
            }
            val cardNowPlaying: NowPlaying? = when (profile.musicCard) {
                MusicCardMode.AUTO -> nowPlaying ?: fromNotif(btPackages)
                MusicCardMode.BLUETOOTH -> allSessions.entries.firstOrNull { it.key in btPackages }?.value ?: fromNotif(btPackages)
                MusicCardMode.RADIO -> allSessions.entries.firstOrNull { it.key in radioPackages }?.value ?: fromNotif(radioPackages)
            }
            val cardSource: String? = when (profile.musicCard) {
                MusicCardMode.BLUETOOTH -> "Bluetooth"
                MusicCardMode.RADIO -> "Radio FM"
                MusicCardMode.AUTO -> when (cardNowPlaying?.sourcePackage) {
                    null -> null
                    in btPackages -> "Bluetooth"
                    in radioPackages -> "Radio FM"
                    else -> null
                }
            }

            // Redibuja el auto cuando cambia el modelo, el color o la foto propia (en segundo plano).
            LaunchedEffect(profile.model, profile.paint, photoVersion) {
                val p = profile
                carImage = withContext(Dispatchers.Default) {
                    CarImages.render(applicationContext, p, store.customFile)?.asImageBitmap()
                }
            }

            // Tema claro/oscuro: se revisa cada minuto (el modo Automático cambia solo a las 6:00 y 18:00).
            LaunchedEffect(profile.theme) {
                while (true) {
                    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    Palette.light = profile.theme.isLight(hour)
                    delay(60_000L - System.currentTimeMillis() % 60_000L)
                }
            }

            // Protector de pantalla: se activa tras X minutos sin tocar la pantalla.
            LaunchedEffect(profile.saverMinutes) {
                while (true) {
                    delay(5_000)
                    val mins = profile.saverMinutes
                    if (mins > 0 && !saverOn && !splashOn && !customizing && !wizardOn && update == null &&
                        android.os.SystemClock.uptimeMillis() - lastTouch > mins * 60_000L
                    ) saverOn = true
                }
            }

            // Aviso de versión nueva: se revisa un rato después de prender (si hay internet).
            LaunchedEffect(Unit) {
                delay(30_000)
                val installed = installedVersion()
                com.luis.gvlauncher.data.Updates.check(applicationContext, installed)?.let { update = it }
            }

            LaunchedEffect(cityIndex) {
                val city = Config.CITIES.getOrNull(cityIndex)
                while (true) {
                    WeatherRepository.fetch(applicationContext, city)?.let { weather = it }
                    delay(if (weather == null) 15_000L else 15 * 60_000L)
                }
            }

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val unit = minOf(maxWidth / DESIGN_W, maxHeight / DESIGN_H)
                CompositionLocalProvider(LocalUnit provides unit, LocalInk provides Ink.surface()) {
                    AppBackground(profile.wallpaper)
                    if (wizardOn) {
                        BackHandler { finishWizard() }
                        com.luis.gvlauncher.ui.WizardScreen(
                            profile = profile,
                            preview = carImage,
                            renderCar = { p ->
                                withContext(Dispatchers.Default) {
                                    CarImages.render(applicationContext, p, store.customFile)?.asImageBitmap()
                                }
                            },
                            onChange = { profile = it },
                            onFinish = { finishWizard() },
                        )
                    } else if (customizing) {
                        BackHandler { closeCustomize() }
                        CustomizeScreen(
                            profile = profile,
                            preview = carImage,
                            onChange = { profile = it },
                            onPickPhoto = {
                                try { pickPhoto.launch("image/*") } catch (e: Exception) {
                                    toast("La radio no tiene selector de fotos. Copia tu imagen a ${Config.CUSTOM_PHOTO_PATH}")
                                }
                            },
                            onUseDownloadPhoto = {
                                val f = java.io.File(Config.CUSTOM_PHOTO_PATH)
                                if (store.importCustom(f)) photoVersion++
                                else toast("No encontré ${Config.CUSTOM_PHOTO_PATH}")
                            },
                            onDone = { closeCustomize() },
                        )
                    } else {
                        // Atrás: vuelve al inicio; en el inicio no hace nada.
                        BackHandler { if (screen != Screen.HOME) goTo(Screen.HOME) }
                        val homeState = HomeState(
                            weather, cardNowPlaying, bluetoothOn,
                            cityLabel = Config.CITIES.getOrNull(cityIndex)?.name ?: "Automático",
                            navLabel = profile.navApp.label,
                            driverName = profile.driverName.ifBlank { Config.DRIVER_NAME },
                            carImage = carImage,
                            fix = fix,
                            navMode = profile.navCard,
                            places = places,
                            musicSource = cardSource,
                        )
                        if (screen == Screen.HOME && profile.homeMode == HomeMode.CONDUCCION) {
                            // Modo Conducción: pantalla completa con botones grandes, sin barra lateral.
                            DriveHomeScreen(homeState, homeActions)
                        } else Row(Modifier.fillMaxSize()) {
                            SideRail(screen) { goTo(it) }
                            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                                when (screen) {
                                    Screen.HOME -> HomeScreen(homeState, homeActions)
                                    Screen.NAV -> NavScreen(profile.navApp, fix, places, homeAddress, workAddress, navActions)
                                    Screen.PHONE -> PhoneScreen(bluetoothOn, contacts, phoneActions)
                                    Screen.MUSIC -> MusicScreen(
                                        nowPlaying ?: fromNotif(btPackages + radioPackages),
                                        musicSources.firstOrNull { (nowPlaying ?: fromNotif(btPackages + radioPackages))?.sourcePackage in it.activeWhen }?.label,
                                        musicSources, musicActions,
                                    )
                                    Screen.APPS -> AppDrawer(
                                        drawerApps,
                                        iconScale = profile.iconScale,
                                        onScale = { updateProfile(profile.copy(iconScale = it)) },
                                        onLaunch = { Apps.launch(this@MainActivity, it.packageName) },
                                        onClose = { goTo(Screen.HOME) },
                                    )
                                    Screen.SETTINGS -> SettingsScreen(
                                        sys, profile.theme, profile.musicApp, profile.navApp,
                                        version = installedVersion(),
                                        updateStatus = updateStatus,
                                        actions = settingsActions,
                                        navCard = profile.navCard,
                                        musicCard = profile.musicCard,
                                        wallpaper = profile.wallpaper,
                                        notificationAccess = notificationAccess,
                                        saverMinutes = profile.saverMinutes,
                                        saverEffect = profile.saverEffect,
                                        homeMode = profile.homeMode,
                                    )
                                }
                            }
                        }
                    }
                    // Aviso de actualización (encima del inicio, debajo del protector)
                    update?.let { info ->
                        if (!wizardOn) {
                            com.luis.gvlauncher.ui.UpdateDialog(
                                info = info,
                                installed = installedVersion(),
                                progress = updateProgress,
                                error = updateError,
                                onUpdate = { downloadAndInstall(info) },
                                onLater = {
                                    com.luis.gvlauncher.data.Updates.snooze(applicationContext, info.version)
                                    update = null
                                    updateError = null
                                },
                            )
                        }
                    }
                    // Protector de pantalla encima de todo
                    if (saverOn) {
                        BackHandler { closeSaver() }
                        ScreenSaver(
                            wallpaper = profile.wallpaper,
                            effect = profile.saverEffect,
                            weather = weather,
                            nowPlaying = cardNowPlaying ?: nowPlaying,
                            onNextEffect = {
                                val n = SaverEffect.entries[(profile.saverEffect.ordinal + 1) % SaverEffect.entries.size]
                                updateProfile(profile.copy(saverEffect = n))
                            },
                            onDismiss = { closeSaver() },
                        )
                    }
                    // Pantalla de carga (encima de todo, solo al arrancar)
                    if (splashOn) {
                        com.luis.gvlauncher.ui.SplashScreen(installedVersion()) {
                            splashOn = false
                            splashShown = true
                            lastTouch = android.os.SystemClock.uptimeMillis()
                        }
                    }
                }
            }
        }
    }

    /**
     * Pantalla completa: oculta la barra de Android (◁ ○ □, hora, íconos) para que el launcher
     * use los 600 px de alto. Deslizando desde el borde aparece un momento y se vuelve a ocultar.
     */
    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Cualquier toque reinicia el contador del protector de pantalla. */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        lastTouch = android.os.SystemClock.uptimeMillis()
        return super.dispatchTouchEvent(ev)
    }

    private fun closeSaver() {
        saverOn = false
        lastTouch = android.os.SystemClock.uptimeMillis()
    }

    override fun onStart() {
        super.onStart()
        media.start()
        location.start()
        registerReceiver(
            stateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            },
        )
        refreshSystem()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        // Al volver de otra app (ej. permiso de brillo o volumen físico), relee el estado.
        refreshSystem()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        location.start()
    }

    override fun onStop() {
        saverOn = false
        media.stop()
        location.stop()
        unregisterReceiver(stateReceiver)
        super.onStop()
    }

    /** Presionar Home: cierra Personalizar y vuelve al inicio. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (customizing) closeCustomize()
        closeSaver()
        screen = Screen.HOME
    }

    private fun goTo(target: Screen) {
        if (target == Screen.APPS) drawerApps = Apps.installedLaunchables(this)
        if (target == Screen.SETTINGS) refreshSystem()
        screen = target
    }

    private fun refreshSystem() {
        notificationAccess = runCatching {
            android.provider.Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                ?.contains(packageName) == true
        }.getOrDefault(false)
        // Recién dado el permiso: reinicia la lectura de música para empezar a ver canciones.
        if (notificationAccess && !mediaStarted) { media.stop(); media.start(); mediaStarted = true }
        bluetoothOn = system.bluetoothOn()
        sys = SystemState(
            brightness = system.brightness(),
            canSetBrightness = system.canWriteSettings(),
            wifiOn = system.wifiOn(),
            wifiName = system.wifiName(),
            bluetoothOn = bluetoothOn,
        )
    }

    private fun updateProfile(p: CarProfile) {
        profile = p
        store.save(p)
    }

    /** Botón "Buscar actualizaciones": revisa GitHub en el momento. */
    private fun checkUpdatesNow() {
        if (updateStatus == "Revisando…") return
        updateStatus = "Revisando…"
        scope.launch {
            when (val r = com.luis.gvlauncher.data.Updates.checkNow(applicationContext, installedVersion())) {
                is com.luis.gvlauncher.data.UpdateCheck.Available -> {
                    updateStatus = "Hay una versión nueva: v${r.info.version}"
                    updateError = null
                    update = r.info
                }
                com.luis.gvlauncher.data.UpdateCheck.UpToDate -> updateStatus = "Estás al día · v${installedVersion()}"
                com.luis.gvlauncher.data.UpdateCheck.Offline -> updateStatus = "Sin internet: conecta el WiFi e intenta de nuevo"
            }
        }
    }

    /** "Actualizar ahora": descarga el APK de GitHub y abre el instalador de Android. */
    private fun downloadAndInstall(info: com.luis.gvlauncher.data.UpdateInfo) {
        val url = info.apkUrl ?: return
        if (updateProgress != null) return
        updateError = null
        updateProgress = 0f
        scope.launch {
            val apk = runCatching {
                com.luis.gvlauncher.data.Updates.download(applicationContext, url) { p -> updateProgress = p }
            }.getOrNull()
            updateProgress = null
            when {
                apk == null -> updateError = "No se pudo descargar. Revisa el internet y vuelve a intentar."
                !com.luis.gvlauncher.data.Updates.install(this@MainActivity, apk) ->
                    updateError = "No se pudo abrir el instalador. Usa el QR e instala por USB."
                else -> update = null // el instalador de Android toma el control
            }
        }
    }

    private fun installedVersion(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "1.0"

    /** Guarda lo elegido en el asistente y no lo vuelve a mostrar. */
    private fun finishWizard() {
        store.save(profile)
        store.setWizardDone()
        wizardOn = false
    }

    private fun closeCustomize() {
        store.save(profile)
        profile = store.load()
        customizing = false
    }

    private fun startSafely(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun notInstalled(what: String) = Apps.notInstalled(this, what)

    private fun open(shortcut: Shortcut, what: String) {
        if (!Apps.launchShortcut(this, shortcut)) notInstalled(what)
    }

    private fun openNavApp() {
        val app = profile.navApp
        if (!Apps.launchShortcut(this, app.shortcut)) notInstalled(app.label)
    }

    private fun openMusicApp() {
        val app = profile.musicApp
        if (!Apps.launchShortcut(this, app.shortcut)) notInstalled(app.label)
    }

    /** Casa / Trabajo con la app elegida. Con Google Maps sin dirección guardada, lleva a Navegación. */
    private fun goFavorite(favorite: String, address: String) {
        val ok = Apps.navigateFavorite(this, profile.navApp, favorite, address)
        if (!ok) {
            if (Apps.resolve(this, profile.navApp.shortcut) != "com.waze" && address.isBlank()) {
                toast("Pon la dirección con el lápiz en la tarjeta")
                screen = Screen.NAV
            } else {
                notInstalled(profile.navApp.label)
            }
        }
    }

    private companion object {
        /** El carrusel se muestra una vez por arranque (no cada vez que vuelves al inicio). */
        var splashShown = false
    }
}
