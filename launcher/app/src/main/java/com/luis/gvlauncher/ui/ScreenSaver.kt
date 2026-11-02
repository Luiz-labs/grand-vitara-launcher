package com.luis.gvlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import com.luis.gvlauncher.data.NowPlaying
import com.luis.gvlauncher.data.SaverEffect
import com.luis.gvlauncher.data.Wallpaper
import com.luis.gvlauncher.data.Weather
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Protector de pantalla: hora muy grande, fecha, temperatura y lo que suena,
 * sobre un fondo animado suave. Tocar cualquier parte lo cierra; el botón ✦ cambia el efecto.
 * La animación corre a ~20 cuadros por segundo para no cargar el procesador T3.
 */
@Composable
fun ScreenSaver(
    wallpaper: Wallpaper,
    effect: SaverEffect,
    weather: Weather?,
    nowPlaying: NowPlaying?,
    onNextEffect: () -> Unit,
    onDismiss: () -> Unit,
) {
    var now by remember { mutableStateOf(Date()) }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }
    LaunchedEffect(effect) {
        while (effect != SaverEffect.QUIETO) {
            t += 0.05f
            delay(50)
        }
    }

    val base = Color(wallpaper.base)
    val glow = Color(wallpaper.glow)
    val tap = remember { MutableInteractionSource() }

    Box(
        Modifier
            .fillMaxSize()
            .background(base)
            .clickable(interactionSource = tap, indication = null, onClick = onDismiss),
    ) {
        // Fondo animado
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (effect) {
                SaverEffect.ONDAS -> {
                    for (layer in 0 until 4) {
                        val path = Path()
                        val amp = h * (0.05f + layer * 0.018f)
                        val baseY = h * (0.62f + layer * 0.08f)
                        val speed = 0.6f + layer * 0.25f
                        val freq = 1.4f + layer * 0.35f
                        path.moveTo(0f, h)
                        var x = 0f
                        while (x <= w + 8f) {
                            val y = baseY + amp * sin((x / w) * freq * 2f * PI.toFloat() + t * speed + layer)
                            path.lineTo(x, y)
                            x += 16f
                        }
                        path.lineTo(w, h)
                        path.close()
                        drawPath(path, glow.copy(alpha = 0.16f + layer * 0.05f))
                    }
                }
                SaverEffect.AURORA -> {
                    val c1 = Offset(w * (0.5f + 0.3f * cos(t * 0.35f)), h * (0.35f + 0.15f * sin(t * 0.5f)))
                    val c2 = Offset(w * (0.5f + 0.35f * sin(t * 0.27f + 1f)), h * (0.7f + 0.12f * cos(t * 0.4f)))
                    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), Color.Transparent), c1, w * 0.45f), w * 0.45f, c1)
                    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent), c2, w * 0.38f), w * 0.38f, c2)
                }
                SaverEffect.QUIETO -> {
                    val c = Offset(w * 0.7f, h * 0.1f)
                    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), c, w * 0.6f), w * 0.6f, c)
                }
            }
        }

        // Contenido (siempre en blanco: el fondo del protector es oscuro)
        OnColor { Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 90.u, vertical = 60.u),
                verticalArrangement = Arrangement.Center,
            ) {
                val time = SimpleDateFormat("HH:mm", spanishLocale).format(now)
                val date = SimpleDateFormat("EEEE d 'de' MMMM", spanishLocale).format(now)
                    .replaceFirstChar { it.titlecase(spanishLocale) }
                Label(time, 300, weight = FontWeight.Light)
                Label(date, 46, color = Color(0xFFD5DEF5))
                Spacer(Modifier.height(40.u))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (weather != null) {
                        Icon(Icons.Rounded.Thermostat, null, tint = Color.White, modifier = Modifier.size(64.u))
                        Spacer(Modifier.width(10.u))
                        Label("${weather.temp}°", 72, weight = FontWeight.Medium)
                        Spacer(Modifier.width(18.u))
                        Label(weather.description, 40, color = Color(0xFFD5DEF5))
                        Spacer(Modifier.width(60.u))
                    }
                    val title = nowPlaying?.title?.ifBlank { null }
                    if (title != null) {
                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(56.u))
                        Spacer(Modifier.width(12.u))
                        Column(Modifier.weight(1f)) {
                            Label(title, 40, weight = FontWeight.Medium)
                            nowPlaying?.artist?.ifBlank { null }?.let { Label(it, 30, color = Color(0xFFD5DEF5)) }
                        }
                    }
                }
            }

            // Botón para cambiar el efecto sin salir del protector
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(36.u)
                    .clip(RoundedCornerShape(40.u))
                    .background(Color(0x33FFFFFF))
                    .clickable(onClick = onNextEffect)
                    .padding(horizontal = 28.u, vertical = 18.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(36.u))
                Spacer(Modifier.width(10.u))
                Label(effect.label, 26)
            }
        } }
    }
}

private val spanishLocale = Locale("es", "ES")
