package com.luis.gvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import com.luis.gvlauncher.Shortcut
import com.luis.gvlauncher.data.NowPlaying
import kotlinx.coroutines.delay

/** Fuente de música que se muestra como botón arriba del reproductor. */
data class MusicSource(
    val label: String,
    val icon: ImageVector,
    val shortcut: Shortcut,
    /** Paquetes que, si están sonando, marcan esta fuente como activa. */
    val activeWhen: Set<String>,
)

class MusicActions(
    val openSource: (MusicSource) -> Unit,
    /** Abre la app que está sonando o, si no hay, la app de música elegida en Ajustes. */
    val openCurrent: () -> Unit,
    val playPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
)

fun musicSourceIcon(label: String): ImageVector = when (label) {
    "Bluetooth" -> Icons.Rounded.Bluetooth
    "USB" -> Icons.Rounded.Usb
    "Radio FM" -> Icons.Rounded.CellTower
    else -> Icons.Rounded.LibraryMusic
}

@Composable
fun MusicScreen(np: NowPlaying?, sourceName: String?, sources: List<MusicSource>, actions: MusicActions) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(np?.playing) {
        while (np?.playing == true) {
            tick++
            delay(500)
        }
    }
    val active = sources.firstOrNull { np?.sourcePackage in it.activeWhen }

    Column(Modifier.fillMaxSize().padding(start = 40.u, end = 36.u, top = 16.u, bottom = 30.u)) {
        ScreenHeader("Música", sourceName?.let { "Sonando desde $it" } ?: "Elige una fuente") {
            ActionPill("Abrir app", Icons.Rounded.OpenInNew, onClick = actions.openCurrent)
        }
        Spacer(Modifier.height(14.u))
        Row(horizontalArrangement = Arrangement.spacedBy(12.u)) {
            sources.forEach { s ->
                ActionPill(s.label, s.icon, accent = s == active) { actions.openSource(s) }
            }
        }
        Spacer(Modifier.height(22.u))

        GlassCard(
            Palette.MediaTop, Palette.MediaBottom, Palette.MediaBorder, Modifier.fillMaxSize(),
            onClick = actions.openCurrent,
        ) {
            CardInk {
                Row(Modifier.fillMaxSize().padding(44.u), horizontalArrangement = Arrangement.spacedBy(52.u)) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .clip(RoundedCornerShape(22.u))
                            .background(Brush.linearGradient(listOf(Color(0xFFE05F8A), Color(0xFF2D1F8A)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        val art = np?.art
                        if (art != null) {
                            Image(art.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Rounded.MusicNote, null, tint = Color(0x99FFFFFF), modifier = Modifier.size(140.u))
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Spacer(Modifier.height(30.u))
                        Label("AHORA SUENA", 21, color = LocalInk.current.secondary)
                        Spacer(Modifier.height(10.u))
                        Label(np?.title?.ifBlank { null } ?: "Sin música", 60, weight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.u))
                        Label(np?.artist?.ifBlank { null } ?: "Toca una fuente para empezar", 30, color = LocalInk.current.secondary)
                        Spacer(Modifier.weight(1f))
                        BigProgress(np, tick)
                        Spacer(Modifier.height(18.u))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(56.u, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircleButton(Icons.Rounded.SkipPrevious, 92, Palette.ChipBg, LocalInk.current.primary, actions.previous)
                            CircleButton(
                                if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                124, Palette.MediaAccent, Palette.OnMediaAccent, actions.playPause,
                            )
                            CircleButton(Icons.Rounded.SkipNext, 92, Palette.ChipBg, LocalInk.current.primary, actions.next)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigProgress(np: NowPlaying?, @Suppress("UNUSED_PARAMETER") tick: Long) {
    val pos = np?.currentPosition() ?: 0L
    val dur = np?.durationMs ?: 0L
    val fraction = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(26.u), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(8.u).clip(RoundedCornerShape(4.u)).background(LocalInk.current.primary.copy(alpha = 0.25f)))
        Box(Modifier.fillMaxWidth(fraction).height(8.u).clip(RoundedCornerShape(4.u)).background(Palette.MediaAccent))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.fillMaxWidth(fraction))
            Box(Modifier.offset(x = (-13).u).size(26.u).clip(CircleShape).background(Palette.MediaAccent))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.u)) {
        Label(fmt(pos), 22)
        Spacer(Modifier.weight(1f))
        Label(fmt(dur), 22)
    }
}

private fun fmt(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun CircleButton(icon: ImageVector, size: Int, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(size.u).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size((size * 0.5f).u))
    }
}
