package com.luis.gvlauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.luis.gvlauncher.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Un auto del carrusel: imagen, nombre y largo relativo (para que se vean en proporción). */
private data class SplashCar(val image: Int, val name: String, val lengthFactor: Float)

private val splashCars = listOf(
    SplashCar(R.drawable.splash_1, "Grand Vitara", 0.90f),
    SplashCar(R.drawable.splash_2, "Grand Nomade", 1f),
    SplashCar(R.drawable.splash_3, "Grand Vitara", 0.90f),
    SplashCar(R.drawable.splash_4, "Grand Nomade", 1f),
    SplashCar(R.drawable.splash_5, "Grand Vitara", 0.90f),
    SplashCar(R.drawable.splash_6, "Grand Nomade", 1f),
)

/** Pasos del carrusel antes de entrar al launcher (~4 segundos en total). */
private const val SPLASH_STEPS = 3
private const val HOLD_MS = 650L
private const val MOVE_MS = 600

/**
 * Pantalla de carga: carrusel de autos que avanza de derecha a izquierda.
 * El del centro va grande y nítido; los de los costados más chicos y atenuados.
 * Abajo: barra de carga y "Desarrollado por Labs Project · versión".
 * Tocar en cualquier parte la salta.
 */
@Composable
fun SplashScreen(version: String, onDone: () -> Unit) {
    val position = remember { Animatable(0f) }
    var progress by remember { mutableFloatStateOf(0f) }
    val total = SPLASH_STEPS * (HOLD_MS + MOVE_MS) + HOLD_MS
    LaunchedEffect(Unit) {
        launch {
            val start = System.currentTimeMillis()
            while (progress < 1f) {
                progress = ((System.currentTimeMillis() - start).toFloat() / total).coerceAtMost(1f)
                delay(50)
            }
        }
        repeat(SPLASH_STEPS) { step ->
            delay(HOLD_MS)
            position.animateTo((step + 1).toFloat(), tween(MOVE_MS, easing = FastOutSlowInEasing))
        }
        delay(HOLD_MS)
        onDone()
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF050B1F))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDone),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        // Resplandor azul de fondo
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Color(0xFF0F2A78), Color(0xFF050B1F)),
                    center = Offset(widthPx / 2f, heightPx * 0.4f),
                    radius = widthPx * 0.7f,
                ),
            ),
        )

        val carW = 790.u
        val carH = 380.u
        val spacing = 670.u
        val ground = 523.u
        val n = splashCars.size
        val pos = position.value

        // Del más lejano al más cercano, para que el del centro quede encima
        splashCars.indices
            .map { i ->
                var x = (i - pos) % n
                if (x < -n / 2f) x += n
                if (x >= n / 2f) x -= n
                i to x
            }
            .filter { abs(it.second) < 1.9f }
            .sortedByDescending { abs(it.second) }
            .forEach { (i, x) ->
                val dist = abs(x)
                val scale = maxOf(0.5f, 1f - 0.42f * dist) * splashCars[i].lengthFactor
                val alpha = maxOf(0f, 1f - 0.6f * dist)
                Box(
                    Modifier
                        .offset(x = maxWidth / 2 - carW / 2 + spacing * x, y = ground - carH)
                        .size(carW, carH)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        },
                ) {
                    Image(
                        painterResource(splashCars[i].image), null,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

        OnColor {
            // Nombre del auto del centro (aparece cuando se detiene)
            val nearest = ((pos % n) + n) % n
            val frac = abs(pos - Math.round(pos))
            val nameAlpha = (1f - frac / 0.3f).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().offset(y = ground + 26.u), contentAlignment = Alignment.TopCenter) {
                Label(
                    splashCars[Math.round(nearest) % n].name, 40,
                    color = Color(0xFFC8D6F5).copy(alpha = nameAlpha),
                )
            }
            // Barra de carga
            Box(Modifier.fillMaxWidth().offset(y = 702.u), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.width(425.u).height(10.u).clip(RoundedCornerShape(5.u)).background(Color(0xFF1E2D5A))) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(progress.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(5.u)).background(Color(0xFF3D8BFF)),
                    )
                }
            }
            // Créditos
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 60.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label("Desarrollado por ", 28, color = Color(0xFFA9BCE8))
                Label("Labs Project", 32, weight = FontWeight.Bold)
                Label("  ·  v$version", 28, color = Color(0xFFA9BCE8))
            }
        }
    }
}
