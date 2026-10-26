package com.luis.gvlauncher

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.luis.gvlauncher.data.CarImages
import com.luis.gvlauncher.data.CarProfile
import com.luis.gvlauncher.data.ProfileStore
import com.luis.gvlauncher.ui.CustomizeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.gvlauncher.data.AppEntry
import com.luis.gvlauncher.data.Apps
import com.luis.gvlauncher.data.LocationRepository
import com.luis.gvlauncher.data.MediaRepository
import com.luis.gvlauncher.data.Weather
import com.luis.gvlauncher.data.WeatherRepository
import com.luis.gvlauncher.ui.AppBackground
import com.luis.gvlauncher.ui.AppDrawer
import com.luis.gvlauncher.ui.DESIGN_H
import com.luis.gvlauncher.ui.DESIGN_W
import com.luis.gvlauncher.ui.HomeActions
import com.luis.gvlauncher.ui.HomeScreen
import com.luis.gvlauncher.ui.HomeState
import com.luis.gvlauncher.ui.LocalUnit
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var media: MediaRepository
    private lateinit var location: LocationRepository
    private var weather by mutableStateOf<Weather?>(null)
    private var bluetoothOn by mutableStateOf(false)
    private var drawerApps by mutableStateOf<List<AppEntry>?>(null)

    private lateinit var store: ProfileStore
    private var profile by mutableStateOf(CarProfile(Config.DRIVER_NAME, Config.DEFAULT_MODEL, Config.DEFAULT_PAINT))
    private var carImage by mutableStateOf<ImageBitmap?>(null)
    /** Sube cada vez que cambia la foto propia, para volver a dibujarla aunque el perfil sea igual. */
    private var photoVersion by mutableIntStateOf(0)
    private var customizing by mutableStateOf(false)

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        if (store.importCustom(uri)) photoVersion++ else toast("No se pudo leer esa imagen")
    }

    /** -1 = automatic (GPS), otherwise index into [Config.CITIES]. */
    private var cityIndex by mutableIntStateOf(-1)
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshBluetooth()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        media = MediaRepository(applicationContext)
        location = LocationRepository(applicationContext)
        store = ProfileStore(applicationContext)
        profile = store.load()
        cityIndex = prefs.getInt("city", -1).coerceIn(-1, Config.CITIES.lastIndex)
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ),
            1,
        )

        val actions = HomeActions(
            openNavigation = { openWaze() },
            wazeHome = { if (!Apps.openWazeFavorite(this, "home")) openWaze() },
            wazeWork = { if (!Apps.openWazeFavorite(this, "work")) openWaze() },
            openPhone = { open(Config.PHONE, "Teléfono") },
            openMusic = { open(Config.MUSIC, "Música") },
            openRadio = { open(Config.RADIO, "Radio FM") },
            openEqualizer = { open(Config.EQUALIZER, "Ecualizador") },
            openCarPlay = { open(Config.CARPLAY, "CarPlay") },
            openApps = { drawerApps = Apps.installedLaunchables(this) },
            openCustomize = { customizing = true },
            openSettings = { Apps.openSettings(this) },
            cycleCity = {
                cityIndex = if (cityIndex >= Config.CITIES.lastIndex) -1 else cityIndex + 1
                prefs.edit().putInt("city", cityIndex).apply()
                weather = null
            },
            playPause = { media.playPause() },
            next = { media.next() },
            previous = { media.previous() },
        )

        setContent {
            val nowPlaying by media.state.collectAsStateWithLifecycle()
            val fix by location.fix.collectAsStateWithLifecycle()

            // Redibuja el auto cuando cambia el modelo, el color o la foto propia (en segundo plano).
            LaunchedEffect(profile.model, profile.paint, photoVersion) {
                val p = profile
                carImage = withContext(Dispatchers.Default) {
                    CarImages.render(applicationContext, p, store.customFile)?.asImageBitmap()
                }
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
                CompositionLocalProvider(LocalUnit provides unit) {
                    AppBackground()
                    val apps = drawerApps
                    if (customizing) {
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
                    } else if (apps != null) {
                        BackHandler { drawerApps = null }
                        AppDrawer(
                            apps,
                            onLaunch = { Apps.launch(this@MainActivity, it.packageName); drawerApps = null },
                            onClose = { drawerApps = null },
                        )
                    } else {
                        BackHandler { /* home screen: swallow back */ }
                        HomeScreen(
                            HomeState(
                                weather, nowPlaying, bluetoothOn,
                                cityLabel = Config.CITIES.getOrNull(cityIndex)?.name ?: "Automático",
                                driverName = profile.driverName.ifBlank { Config.DRIVER_NAME },
                                carImage = carImage,
                                fix = fix,
                            ),
                            actions,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        media.start()
        location.start()
        registerReceiver(btReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        refreshBluetooth()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        location.start()
    }

    override fun onStop() {
        media.stop()
        location.stop()
        unregisterReceiver(btReceiver)
        super.onStop()
    }

    /** Pressing Home while already home closes the app drawer. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        drawerApps = null
        if (customizing) closeCustomize()
    }

    private fun refreshBluetooth() {
        @Suppress("DEPRECATION")
        bluetoothOn = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)
    }

    private fun closeCustomize() {
        store.save(profile)
        profile = store.load()
        customizing = false
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun open(shortcut: Shortcut, what: String) {
        if (!Apps.launchShortcut(this, shortcut)) Apps.notInstalled(this, what)
    }

    private fun openWaze() {
        if (!Apps.launchShortcut(this, Config.WAZE)) open(Config.NAVIGATION_FALLBACK, "Waze")
    }
}
