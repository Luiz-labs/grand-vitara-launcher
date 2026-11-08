package com.luis.gvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.luis.gvlauncher.data.HomeMode
import com.luis.gvlauncher.data.MusicApp
import com.luis.gvlauncher.data.MusicCardMode
import com.luis.gvlauncher.data.NavCardMode
import com.luis.gvlauncher.data.SaverEffect
import com.luis.gvlauncher.data.Wallpaper
import com.luis.gvlauncher.data.NavApp
import com.luis.gvlauncher.data.ThemeMode

/** Estado de los controles del sistema, leído por MainActivity. */
data class SystemState(
    val brightness: Float = 0.7f,
    val canSetBrightness: Boolean = false,
    val wifiOn: Boolean = false,
    val wifiName: String? = null,
    val bluetoothOn: Boolean = false,
)

class SettingsActions(
    val setBrightness: (Float) -> Unit,
    val requestBrightnessPermission: () -> Unit,
    /** Volumen real de la radio: true = subir, false = bajar. */
    val volumeStep: (Boolean) -> Unit,
    val toggleMute: () -> Unit,
    val setWifi: (Boolean) -> Unit,
    val setBluetooth: (Boolean) -> Unit,
    /** Tocar la tarjeta (no el interruptor): lista de redes / emparejar dispositivos. */
    val openWifiList: () -> Unit = {},
    val openBluetoothList: () -> Unit = {},
    val setTheme: (ThemeMode) -> Unit,
    val setMusicApp: (MusicApp) -> Unit,
    val setNavApp: (NavApp) -> Unit,
    val openCustomize: () -> Unit,
    val openCarSettings: () -> Unit,
    val openAndroidSettings: () -> Unit,
    val setNavCard: (NavCardMode) -> Unit = {},
    val setMusicCard: (MusicCardMode) -> Unit = {},
    val setWallpaper: (Wallpaper) -> Unit = {},
    /** Abre el permiso "Acceso a notificaciones" (para ver la canción del iPhone por Bluetooth). */
    val openNotificationAccess: () -> Unit = {},
    /** Abre la pantalla de Android para elegir la app de inicio. */
    val openHomeChooser: () -> Unit = {},
    val setSaverMinutes: (Int) -> Unit = {},
    val setSaverEffect: (SaverEffect) -> Unit = {},
    /** Muestra el protector ahora (para probarlo). */
    val previewSaver: () -> Unit = {},
    /** Inicio con botones grandes (Conducción) o completo. */
    val setHomeMode: (HomeMode) -> Unit = {},
    /** Vuelve a mostrar el asistente de la primera vez. */
    val openWizard: () -> Unit = {},
    /** Revisa en GitHub si hay versión nueva. */
    val checkUpdates: () -> Unit = {},
)

@Composable
fun SettingsScreen(
    sys: SystemState,
    theme: ThemeMode,
    musicApp: MusicApp,
    navApp: NavApp,
    version: String,
    actions: SettingsActions,
    updateStatus: String = "Toca para revisar",
    navCard: NavCardMode = NavCardMode.CASA_TRABAJO,
    musicCard: MusicCardMode = MusicCardMode.AUTO,
    wallpaper: Wallpaper = Wallpaper.AZUL,
    notificationAccess: Boolean = false,
    saverMinutes: Int = 3,
    saverEffect: SaverEffect = SaverEffect.ONDAS,
    homeMode: HomeMode = HomeMode.COMPLETO,
) {
    Column(Modifier.fillMaxSize().padding(start = 40.u, end = 36.u, top = 16.u, bottom = 30.u)) {
        ScreenHeader("Ajustes", "Grand Vitara Launcher")
        Spacer(Modifier.height(14.u))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.u),
        ) {
            // Pantalla de inicio: Conducción (botones grandes) o Completo
            Row(Modifier.fillMaxWidth().height(140.u), horizontalArrangement = Arrangement.spacedBy(20.u)) {
                ChoiceCard(Icons.Rounded.DirectionsCar, "Pantalla de inicio", HomeMode.entries, homeMode,
                    { it.label }, actions.setHomeMode, Modifier.weight(1f))
            }
            // Brillo y volumen
            Row(Modifier.fillMaxWidth().height(180.u), horizontalArrangement = Arrangement.spacedBy(20.u)) {
                SliderCard(
                    Icons.Rounded.BrightnessMedium, "Brillo", sys.brightness,
                    note = if (sys.canSetBrightness) "Pantalla de la radio" else "Toca para dar permiso",
                    enabled = sys.canSetBrightness,
                    onDisabledClick = actions.requestBrightnessPermission,
                    onChange = actions.setBrightness,
                    modifier = Modifier.weight(1f),
                )
                VolumeCard(actions, Modifier.weight(1.3f))
            }
            // WiFi, Bluetooth, tema
            Row(Modifier.fillMaxWidth().height(140.u), horizontalArrangement = Arrangement.spacedBy(20.u)) {
                ToggleCard(Icons.Rounded.Wifi, "WiFi", sys.wifiName ?: if (sys.wifiOn) "Toca para ver redes" else "Apagado",
                    sys.wifiOn, actions.setWifi, actions.openWifiList, Modifier.weight(1f))
                ToggleCard(Icons.Rounded.Bluetooth, "Bluetooth", if (sys.bluetoothOn) "Toca para emparejar" else "Apagado",
                    sys.bluetoothOn, actions.setBluetooth, actions.openBluetoothList, Modifier.weight(1f))
                ChoiceCard(Icons.Rounded.DarkMode, "Tema", ThemeMode.entries, theme, { it.label }, actions.setTheme,
                    Modifier.weight(2f))
            }
            // Apps por defecto
            Row(Modifier.fillMaxWidth().height(140.u), horizontalArrangement = Arrangement.spacedBy(20.u)) {
                ChoiceCard(Icons.Rounded.LibraryMusic, "Música", MusicApp.entries, musicApp, { it.label },
                    actions.setMusicApp, Modifier.weight(3f))
                ChoiceCard(Icons.Rounded.Navigation, "Navegación", NavApp.entries, navApp, { it.label },
                    actions.setNavApp, Modifier.weight(2f))
            }
            // Tarjetas del inicio
            Row(Modifier.fillMaxWidth().height(140.u), horizontalArrangement = Arrangement.spacedBy(20.u)) {
                ChoiceCard(Icons.Rounded.Navigation, "Tarjeta de navegación", NavCardMode.entries, navCard, { it.label },
                    actions.setNavCard, Modifier.weight(1f))
                ChoiceCard(Icons.Rounded.LibraryMusic, "Tarjeta de música", MusicCardMode.entries, musicCard, { it.label },
                    actions.setMusicCard, Modifier.weight(1f))
            }
            // Protector de pantalla
            Row(Modifier.fillMaxWidth().height(140.u), horizontalArrangement = Arrangement.spacedBy(20.u)) {
                ChoiceCard(Icons.Rounded.AccessTime, "Protector de pantalla (hora grande)", listOf(0, 1, 3, 5), saverMinutes,
                    { if (it == 0) "No" else "$it min" }, actions.setSaverMinutes, Modifier.weight(1f))
                ChoiceCard(Icons.Rounded.AutoAwesome, "Efecto", SaverEffect.entries, saverEffect, { it.label },
                    actions.setSaverEffect, Modifier.weight(1f))
            }
            // Fondo de pantalla
            SurfaceCard(Modifier.fillMaxWidth().height(140.u)) {
                Column(Modifier.fillMaxSize().padding(horizontal = 26.u, vertical = 18.u)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Wallpaper, null, tint = LocalInk.current.primary, modifier = Modifier.size(34.u))
                        Spacer(Modifier.width(12.u))
                        Label("Fondo de pantalla: ${wallpaper.label}", 24)
                        Spacer(Modifier.width(12.u))
                        Label("(se ve en modo oscuro)", 18, color = LocalInk.current.secondary)
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.u)) {
                        Wallpaper.entries.forEach { w ->
                            WallpaperSwatch(w, selected = w == wallpaper) { actions.setWallpaper(w) }
                        }
                    }
                }
            }
            // Accesos
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    ListRow("Mostrar canciones del iPhone",
                        if (notificationAccess) "Activado" else "Toca y activa \"Grand Vitara Launcher\" en Acceso a notificaciones",
                        badgeColor = if (notificationAccess) Palette.PhoneGreen else Palette.EqAmber,
                        badgeIcon = Icons.Rounded.Bluetooth, onClick = actions.openNotificationAccess) { Chevron() }
                    ListRow("Ver protector de pantalla", "Hora, temperatura y música en grande · también tocando la hora del inicio",
                        badgeColor = Palette.Accent, badgeIcon = Icons.Rounded.AccessTime, onClick = actions.previewSaver) { Chevron() }
                    ListRow("Cambiar pantalla de inicio", "Volver al launcher original de la radio",
                        badgeColor = Color(0xFF5A6A8F), badgeIcon = Icons.Rounded.Home, onClick = actions.openHomeChooser) { Chevron() }
                    ListRow("Personalizar", "Nombre, modelo y color del auto, tema",
                        badgeColor = Palette.Accent, badgeIcon = Icons.Rounded.Tune, onClick = actions.openCustomize) { Chevron() }
                    ListRow("Volver a configurar", "Repite el asistente de la primera vez",
                        badgeColor = Palette.Accent, badgeIcon = Icons.Rounded.AutoAwesome, onClick = actions.openWizard) { Chevron() }
                    ListRow("Ajustes del auto", "Cámara de retroceso, timón, radio (ajustes de fábrica)",
                        badgeColor = Palette.PhoneGreen, badgeIcon = Icons.Rounded.DirectionsCar, onClick = actions.openCarSettings) { Chevron() }
                    ListRow("Ajustes de Android", "Sistema, fecha y hora, idioma",
                        badgeColor = Color(0xFF5A6A8F), badgeIcon = Icons.Rounded.Settings, onClick = actions.openAndroidSettings) { Chevron() }
                    ListRow("Buscar actualizaciones", updateStatus,
                        badgeColor = Color(0xFF22C55E), badgeIcon = Icons.Rounded.SystemUpdate, onClick = actions.checkUpdates) { Chevron() }
                    ListRow("Acerca de", "Grand Vitara Launcher · Desarrollado por Labs Project",
                        badgeColor = Color(0xFFA06BFF), badgeIcon = Icons.Rounded.Info) {
                        Label("v$version", 21, color = LocalInk.current.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Chevron() = Label("›", 40, color = LocalInk.current.secondary)

@Composable
private fun SliderCard(
    icon: ImageVector,
    title: String,
    value: Float,
    note: String,
    enabled: Boolean,
    onDisabledClick: () -> Unit,
    onChange: (Float) -> Unit,
    modifier: Modifier,
) {
    SurfaceCard(modifier.fillMaxHeight(), onClick = if (enabled) null else onDisabledClick) {
        Column(Modifier.fillMaxSize().padding(horizontal = 28.u, vertical = 18.u)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = LocalInk.current.primary, modifier = Modifier.size(38.u))
                Spacer(Modifier.width(14.u))
                Label(title, 26)
                Spacer(Modifier.width(18.u))
                Slider(
                    value = value,
                    onValueChange = onChange,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Palette.Accent,
                        activeTrackColor = Palette.Accent,
                        inactiveTrackColor = Palette.ChipBg,
                    ),
                )
                Spacer(Modifier.width(14.u))
                Label("${(value * 100).toInt()}%", 23)
            }
            Label(note, 18, color = LocalInk.current.secondary)
        }
    }
}

/**
 * Volumen con botones − y + grandes. No muestra porcentaje: el volumen real lo lleva la radio
 * y al tocar aparece su propia ventanita con el número verdadero.
 */
@Composable
private fun VolumeCard(actions: SettingsActions, modifier: Modifier) {
    SurfaceCard(modifier.fillMaxHeight()) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 18.u, vertical = 14.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VolumeButton(Icons.Rounded.Remove, "Bajar volumen", Palette.ChipBg, LocalInk.current.primary) {
                actions.volumeStep(false)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Label("Volumen", 30, weight = FontWeight.Medium)
                Label("El de la radio", 20, color = LocalInk.current.secondary)
                Spacer(Modifier.height(8.u))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(30.u))
                        .background(Palette.ChipBg)
                        .clickable(onClick = actions.toggleMute)
                        .padding(horizontal = 22.u, vertical = 10.u),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.VolumeOff, null, tint = LocalInk.current.primary, modifier = Modifier.size(30.u))
                    Spacer(Modifier.width(8.u))
                    Label("Silenciar", 22)
                }
            }
            VolumeButton(Icons.Rounded.Add, "Subir volumen", Palette.Accent, Color.White) {
                actions.volumeStep(true)
            }
        }
    }
}

@Composable
private fun VolumeButton(icon: ImageVector, description: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(150.u).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = fg, modifier = Modifier.size(84.u))
    }
}

@Composable
private fun ToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    on: Boolean,
    onChange: (Boolean) -> Unit,
    /** Al tocar la tarjeta (fuera del interruptor) se abre la lista; el interruptor solo prende/apaga. */
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    SurfaceCard(modifier.fillMaxHeight(), onClick = onOpen) {
        Column(Modifier.fillMaxSize().padding(horizontal = 26.u, vertical = 18.u)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = LocalInk.current.primary, modifier = Modifier.size(38.u))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = on, onCheckedChange = onChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Palette.Accent,
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = Palette.ChipBg,
                    ),
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label(title, 26)
                    Label(subtitle, 18, color = LocalInk.current.secondary)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = LocalInk.current.secondary, modifier = Modifier.size(34.u))
            }
        }
    }
}

@Composable
private fun <T> ChoiceCard(
    icon: ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier,
) {
    SurfaceCard(modifier.fillMaxHeight()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 26.u, vertical = 18.u)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = LocalInk.current.primary, modifier = Modifier.size(34.u))
                Spacer(Modifier.width(12.u))
                Label(title, 24)
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.u)) {
                options.forEach { o ->
                    ActionPill(label(o), accent = o == selected, modifier = Modifier.weight(1f)) { onSelect(o) }
                }
            }
        }
    }
}

@Composable
private fun WallpaperSwatch(w: Wallpaper, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(width = 110.u, height = 60.u)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.u))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(w.base), Color(w.glow)))
            )
            .border(
                if (selected) 4.u else 1.5.u,
                if (selected) Palette.Accent else LocalInk.current.secondary.copy(alpha = 0.4f),
                androidx.compose.foundation.shape.RoundedCornerShape(14.u),
            )
            .clickable(onClick = onClick),
    )
}
