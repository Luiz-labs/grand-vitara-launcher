package com.luis.gvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import com.luis.gvlauncher.data.NavCardMode
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modo Conducción: inicio con botones grandes para usar mientras se maneja.
 * Pocas cosas, letras grandes y botones de al menos ~100 px reales en la pantalla de 7".
 * Ocupa toda la pantalla (sin barra lateral): abajo va un dock con 5 íconos grandes.
 * La velocidad se ve en la tarjeta del auto y en la de navegación.
 *
 * Los tamaños están en unidades de diseño (.u): en la radio de 1024×600, 1.u ≈ 0,61 px,
 * así que 160.u ≈ 98 px reales.
 */
@Composable
fun DriveHomeScreen(state: HomeState, actions: HomeActions) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    Column(Modifier.fillMaxSize().padding(start = 30.u, end = 30.u, bottom = 20.u)) {
        DriveTopBar(now, state, actions)
        Row(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.u),
        ) {
            DriveHero(state, actions, Modifier.weight(1.15f).fillMaxHeight())
            DriveNav(state, actions, Modifier.weight(1f).fillMaxHeight())
            DriveMusic(state, actions, Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(20.u))
        DriveDock(actions)
    }
}

private val spanishDrive = Locale("es", "ES")

@Composable
private fun DriveTopBar(now: Date, state: HomeState, actions: HomeActions) {
    val time = remember(now.time / 60000) { SimpleDateFormat("HH:mm", spanishDrive).format(now) }
    val date = remember(now.time / 60000) {
        SimpleDateFormat("EEE d MMM", spanishDrive).format(now).replace(".", "")
            .replaceFirstChar { it.titlecase(spanishDrive) }
    }
    Row(
        Modifier.fillMaxWidth().height(118.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(time, 74, weight = FontWeight.Medium, modifier = Modifier.clickable(onClick = actions.openSaver))
        Spacer(Modifier.width(24.u))
        Label(date, 38, color = LocalInk.current.secondary)
        Spacer(Modifier.weight(1f))
        state.weather?.let {
            Label("${it.temp}°", 46, weight = FontWeight.Medium)
            Spacer(Modifier.width(28.u))
        }
        Icon(
            if (state.bluetoothOn) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled, null,
            tint = if (state.bluetoothOn) LocalInk.current.primary else LocalInk.current.secondary,
            modifier = Modifier.size(50.u),
        )
        Spacer(Modifier.width(24.u))
        // Cambiar al modo Completo (tarjetas chicas + barra lateral)
        TopPill(Icons.Rounded.Dashboard, "Completo", actions.switchMode)
        Spacer(Modifier.width(16.u))
        Box(
            Modifier.size(92.u).clip(CircleShape).background(Palette.ChipBg).clickable(onClick = actions.openSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Settings, "Ajustes", tint = LocalInk.current.primary, modifier = Modifier.size(50.u))
        }
    }
}

@Composable
private fun TopPill(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .height(92.u)
            .clip(RoundedCornerShape(46.u))
            .background(Palette.ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Palette.Accent, modifier = Modifier.size(46.u))
        Spacer(Modifier.width(12.u))
        Label(text, 34, weight = FontWeight.Medium)
    }
}

/** Saludo, el auto y la temperatura en grande. Tocar el auto abre Personalizar. */
@Composable
private fun DriveHero(state: HomeState, actions: HomeActions, modifier: Modifier) {
    GlassCard(Palette.HeroTop, Palette.HeroBottom, Palette.HeroBorder, modifier, radius = 36.u) { CardInk {
        Column(Modifier.fillMaxSize().padding(start = 38.u, end = 30.u, top = 32.u, bottom = 28.u)) {
            Label("Hola, ${state.driverName}", 58, weight = FontWeight.SemiBold)
            Box(
                Modifier.weight(1f).fillMaxWidth().clickable(onClick = actions.openCustomize),
                contentAlignment = Alignment.Center,
            ) {
                state.carImage?.let {
                    Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
            }
            // Abajo: clima y velocidad, cada uno en su bloque grande
            Row(Modifier.fillMaxWidth().height(150.u), horizontalArrangement = Arrangement.spacedBy(18.u)) {
                val w = state.weather
                Row(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(28.u))
                        .background(Palette.ChipBg).clickable(onClick = actions.cycleCity),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    WeatherIcon(w?.code, w?.isDay ?: true, 70.u)
                    Spacer(Modifier.width(10.u))
                    Label(w?.let { "${it.temp}°" } ?: "--°", 88, weight = FontWeight.Normal)
                }
                Row(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(28.u)).background(Palette.ChipBg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Label(state.fix?.speedKmh?.toString() ?: "0", 88, weight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.u))
                    Label("km/h", 32, color = LocalInk.current.secondary)
                }
            }
        }
    } }
}

/** "Ir a" con dos botones enormes: Casa y Trabajo (o los 2 favoritos, o la velocidad). */
@Composable
private fun DriveNav(state: HomeState, actions: HomeActions, modifier: Modifier) {
    GlassCard(Palette.TileTop, Palette.TileBottom, Palette.TileBorder, modifier, radius = 36.u, onClick = actions.openNavigation) {
        Column(Modifier.fillMaxSize().padding(26.u), verticalArrangement = Arrangement.spacedBy(22.u)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(80.u).clip(RoundedCornerShape(22.u)).background(Palette.WazeBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Navigation, null, tint = Color.White, modifier = Modifier.size(48.u))
                }
                Spacer(Modifier.width(18.u))
                Column(Modifier.weight(1f)) {
                    Label("Ir a", 50, weight = FontWeight.SemiBold)
                    Label("con ${state.navLabel}", 30, color = LocalInk.current.secondary)
                }
                // Velocidad actual
                Row(
                    Modifier.clip(RoundedCornerShape(22.u)).background(Palette.ChipBg)
                        .padding(horizontal = 16.u, vertical = 10.u),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Label(state.fix?.speedKmh?.toString() ?: "0", 54, weight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.u))
                    Label("km/h", 26, color = LocalInk.current.secondary, modifier = Modifier.padding(bottom = 8.u))
                }
            }
            when (state.navMode) {
                NavCardMode.FAVORITOS -> {
                    val top = state.places.take(2)
                    if (top.isEmpty()) {
                        BigButton(Icons.Rounded.Home, Palette.Accent, "Casa", "Ir ahora", actions.wazeHome)
                        BigButton(Icons.Rounded.Work, Color(0xFF14B8A6), "Trabajo", "Ir ahora", actions.wazeWork)
                    } else top.forEach { p ->
                        BigButton(Icons.Rounded.Star, Palette.EqAmber, p.name, "Ir ahora", { actions.goPlace(p) })
                    }
                }
                // En este modo la velocidad ya está arriba: "Velocidad" muestra Casa y Trabajo
                NavCardMode.VELOCIDAD, NavCardMode.CASA_TRABAJO -> {
                    BigButton(Icons.Rounded.Home, Palette.Accent, "Casa", "Ir ahora", actions.wazeHome)
                    BigButton(Icons.Rounded.Work, Color(0xFF14B8A6), "Trabajo", "Ir ahora", actions.wazeWork)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.BigButton(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.u))
            .background(Palette.ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 30.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(86.u))
        Spacer(Modifier.width(22.u))
        Column(Modifier.weight(1f)) {
            Label(title, 60, weight = FontWeight.SemiBold)
            Label(subtitle, 34, color = LocalInk.current.secondary)
        }
    }
}

/** Música: fuente, canción y tres botones grandes (anterior, play/pausa, siguiente). */
@Composable
private fun DriveMusic(state: HomeState, actions: HomeActions, modifier: Modifier) {
    val np = state.nowPlaying
    val source = state.musicSource
    GlassCard(Palette.MediaTop, Palette.MediaBottom, Palette.MediaBorder, modifier, radius = 36.u, onClick = actions.openMusic) { CardInk {
        Column(Modifier.fillMaxSize().padding(start = 32.u, end = 32.u, top = 30.u, bottom = 30.u)) {
            Label("Música", 50, weight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.u))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (source) {
                        "Bluetooth" -> Icons.Rounded.Bluetooth
                        "Radio FM" -> Icons.Rounded.CellTower
                        else -> Icons.Rounded.MusicNote
                    },
                    null, tint = LocalInk.current.secondary, modifier = Modifier.size(38.u),
                )
                Spacer(Modifier.width(8.u))
                Label(source ?: "Lo que suene", 34, color = LocalInk.current.secondary)
            }
            Spacer(Modifier.weight(1f))
            Label(
                np?.title?.ifBlank { null } ?: when (source) {
                    "Radio FM" -> "Radio FM"
                    else -> "Sin música"
                },
                56, weight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.u))
            Label(
                np?.artist?.ifBlank { null } ?: when (source) {
                    "Bluetooth" -> "Pon música en tu iPhone"
                    "Radio FM" -> "Toca para abrir la radio"
                    else -> "Toca para abrir"
                },
                38, color = LocalInk.current.secondary,
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BigRound(Icons.Rounded.SkipPrevious, 130, Palette.ChipBg, LocalInk.current.primary, actions.previous)
                BigRound(
                    if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    160, Palette.MediaAccent, Palette.OnMediaAccent, actions.playPause,
                )
                BigRound(Icons.Rounded.SkipNext, 130, Palette.ChipBg, LocalInk.current.primary, actions.next)
            }
        }
    } }
}

@Composable
private fun BigRound(icon: ImageVector, size: Int, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(size.u).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size((size * 0.58f).u))
    }
}

/** Dock inferior: 5 botones grandes con nombre. */
@Composable
private fun DriveDock(actions: HomeActions) {
    val shape = RoundedCornerShape(40.u)
    Row(
        Modifier
            .fillMaxWidth()
            .height(150.u)
            .clip(shape)
            .background(Palette.Rail)
            .border(1.5.u, Palette.TileBorder, shape)
            .padding(12.u),
        horizontalArrangement = Arrangement.spacedBy(12.u),
    ) {
        DockButton(Icons.Rounded.Home, "Inicio", selected = true) {}
        DockButton(Icons.Rounded.Navigation, "Navegar", onClick = actions.openNavScreen)
        DockButton(Icons.Rounded.MusicNote, "Música", onClick = actions.openMusicScreen)
        DockButton(Icons.Rounded.Phone, "Teléfono", onClick = actions.openPhone)
        DockButton(Icons.Rounded.Apps, "Apps", onClick = actions.openApps)
    }
}

@Composable
private fun RowScope.DockButton(icon: ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit) {
    val color = if (selected) Palette.Accent else LocalInk.current.secondary
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(30.u))
            .background(if (selected) Palette.RailSelected else Color.Transparent)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Solo el ícono (el nombre queda para lectores de pantalla)
        Icon(icon, label, tint = color, modifier = Modifier.size(76.u))
    }
}
