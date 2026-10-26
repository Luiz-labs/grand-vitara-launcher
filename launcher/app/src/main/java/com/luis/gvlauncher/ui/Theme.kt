package com.luis.gvlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Colores del launcher. En modo oscuro las tarjetas del auto y de música son de color (texto blanco);
 * en modo claro todo es blanco con texto oscuro.
 * [light] es un estado de Compose: al cambiarlo, toda la pantalla se vuelve a pintar sola.
 */
object Palette {
    var light by mutableStateOf(false)

    private fun pick(dark: Long, lightColor: Long) = Color(if (light) lightColor else dark)

    val Background get() = pick(0xFF050B1F, 0xFFEEF2F9)
    val BackgroundGlow get() = pick(0xFF0F2A78, 0xFFBFD3FF)
    val Rail get() = pick(0xFF070F27, 0xFFFFFFFF)
    val RailSelected get() = pick(0xFF0C1C47, 0xFFE3ECFF)
    val Divider get() = pick(0x332F4A8A, 0x220F1B3D)
    val TextPrimary get() = pick(0xFFFFFFFF, 0xFF0F1B3D)
    val TextSecondary get() = pick(0xFFA9BCE8, 0xFF5A6A8F)
    /** Fondo de botones/chips sobre superficies neutras. */
    val ChipBg get() = pick(0x33FFFFFF, 0x140F1B3D)

    val Accent = Color(0xFF3D8BFF)
    val WazeBlue = Color(0xFF33CCFF)
    val EqAmber = Color(0xFFFFB547)

    // Tarjetas principales: de color en modo oscuro, blancas en modo claro.
    val HeroTop get() = pick(0xFF123A9E, 0xFFFFFFFF)
    val HeroBottom get() = pick(0xFF0A1F5E, 0xFFEAF1FF)
    val HeroBorder get() = pick(0xFF2C57C2, 0xFFBFD3FF)

    val MediaTop get() = pick(0xFF2B1F9A, 0xFFFFFFFF)
    val MediaBottom get() = pick(0xFF3A2BC4, 0xFFF1EEFF)
    val MediaBorder get() = pick(0xFF5B49E8, 0xFFD5CCFF)
    /** Acento de la tarjeta de música (botón play, barra de progreso). */
    val MediaAccent get() = pick(0xFFFFFFFF, 0xFF5B49E8)
    /** Ícono/texto sobre [MediaAccent]. */
    val OnMediaAccent get() = pick(0xFF2B1F9A, 0xFFFFFFFF)

    // Tarjetas neutras: cambian con el modo.
    val NavTop get() = pick(0xFF0C2160, 0xFFFFFFFF)
    val NavBottom get() = pick(0xFF0A1A4C, 0xFFEAF1FF)
    val NavBorder get() = pick(0xFF2F6BE6, 0xFFBFD3FF)

    val TileTop get() = pick(0xFF0E1A3C, 0xFFFFFFFF)
    val TileBottom get() = pick(0xFF0A1430, 0xFFF4F7FC)
    val TileBorder get() = pick(0xFF1F2E5C, 0xFFD6DFEE)

    val PhoneGreen = Color(0xFF22C55E)
    val CarPlayGreen = Color(0xFF2BD15A)
}

/** Colores de texto e íconos del contenido actual. Las tarjetas de color proveen [Ink.OnColor]. */
data class Ink(val primary: Color, val secondary: Color) {
    companion object {
        val OnColor = Ink(Color.White, Color(0xFFA9BCE8))
        fun surface() = Ink(Palette.TextPrimary, Palette.TextSecondary)
    }
}

val LocalInk = compositionLocalOf { Ink.OnColor }

/** Contenido de las tarjetas del auto y de música: blanco en modo oscuro, oscuro en modo claro. */
@Composable
fun CardInk(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalInk provides if (Palette.light) Ink.surface() else Ink.OnColor, content = content)

/** Envuelve contenido que va sobre un fondo de color fijo (siempre texto blanco). */
@Composable
fun OnColor(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalInk provides Ink.OnColor, content = content)

/**
 * The reference design is 1672×941 px. Every size in the UI is written in those
 * design pixels and scaled to the real screen, so the layout keeps its
 * proportions on a 1024×600 or 1280×720 unit alike.
 */
val LocalUnit = staticCompositionLocalOf { 1.dp }

val Number.u: Dp
    @Composable @ReadOnlyComposable
    get() = LocalUnit.current * this.toFloat()

val Number.us: TextUnit
    @Composable @ReadOnlyComposable
    get() = with(LocalDensity.current) { (LocalUnit.current * this@us.toFloat()).toSp() }

const val DESIGN_W = 1672f
const val DESIGN_H = 941f
