package com.luis.gvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import com.luis.gvlauncher.data.CarModel
import com.luis.gvlauncher.data.CarPaint
import com.luis.gvlauncher.data.CarProfile
import com.luis.gvlauncher.data.MusicApp
import com.luis.gvlauncher.data.NavApp

/**
 * Asistente de la primera vez: 4 pasos para dejar el launcher a gusto de cada uno
 * (nombre, modelo, color, apps de música y navegación). Los cambios se ven al instante;
 * "Saltar" o "¡Listo!" lo cierran y no vuelve a salir (se puede repetir desde Ajustes).
 */
@Composable
fun WizardScreen(
    profile: CarProfile,
    /** Imagen del auto con el modelo y color actuales. */
    preview: ImageBitmap?,
    /** Dibuja el auto para un perfil dado (para las tarjetas de modelo). */
    renderCar: suspend (CarProfile) -> ImageBitmap?,
    onChange: (CarProfile) -> Unit,
    onFinish: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(start = 70.u, end = 70.u, top = 46.u, bottom = 46.u)) {
        // Progreso
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { i ->
                Box(
                    Modifier.width(76.u).height(13.u).clip(RoundedCornerShape(7.u))
                        .background(if (i <= step) Palette.Accent else Palette.ChipBg),
                )
                Spacer(Modifier.width(14.u))
            }
            Spacer(Modifier.width(8.u))
            Label("Paso ${step + 1} de 4", 30, color = LocalInk.current.secondary)
        }
        Spacer(Modifier.height(36.u))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                0 -> StepName(profile, preview, onChange)
                1 -> StepModel(profile, renderCar, onChange)
                2 -> StepColor(profile, preview, onChange)
                else -> StepApps(profile, onChange)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WizardButton(if (step == 0) "Saltar" else "Atrás", accent = false) {
                if (step == 0) onFinish() else step--
            }
            Spacer(Modifier.weight(1f))
            WizardButton(if (step == 3) "¡Listo!" else "Siguiente", accent = true) {
                if (step == 3) onFinish() else step++
            }
        }
    }
}

@Composable
private fun Title(text: String, subtitle: String? = null) {
    Column {
        Label(text, 60, weight = FontWeight.SemiBold)
        if (subtitle != null) {
            Spacer(Modifier.height(8.u))
            Label(subtitle, 32, color = LocalInk.current.secondary)
        }
    }
}

@Composable
private fun StepName(profile: CarProfile, preview: ImageBitmap?, onChange: (CarProfile) -> Unit) {
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1.2f)) {
            Title("¡Hola! Vamos a configurarlo", "Menos de un minuto · luego puedes cambiarlo")
            Spacer(Modifier.height(60.u))
            Label("¿Cómo te llamas?", 40)
            Spacer(Modifier.height(18.u))
            InputField(
                value = profile.driverName,
                onValue = { onChange(profile.copy(driverName = it.take(18))) },
                placeholder = "Tu nombre",
                modifier = Modifier.width(900.u),
            )
            Spacer(Modifier.height(16.u))
            Label("Aparecerá en el saludo: “Hola, ${profile.driverName.ifBlank { "…" }}”", 30,
                color = LocalInk.current.secondary)
        }
        Box(Modifier.weight(0.8f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
            preview?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(330.u)) }
        }
    }
}

@Composable
private fun StepModel(profile: CarProfile, renderCar: suspend (CarProfile) -> ImageBitmap?, onChange: (CarProfile) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Title("¿Cuál es tu Suzuki?")
        Spacer(Modifier.height(36.u))
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(36.u)) {
            CarModel.entries.forEach { m ->
                val image by produceState<ImageBitmap?>(null, m, profile.paint) {
                    value = if (m == CarModel.FOTO_PROPIA) null else renderCar(profile.copy(model = m))
                }
                SelectCard(selected = profile.model == m, onClick = { onChange(profile.copy(model = m)) }) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        val img = image
                        if (img != null) {
                            Image(img, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        } else if (m == CarModel.FOTO_PROPIA) {
                            Box(Modifier.size(190.u).clip(CircleShape).background(Palette.ChipBg), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PhotoCamera, null, tint = LocalInk.current.secondary, modifier = Modifier.size(100.u))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.u))
                    Label(m.label, 48, weight = FontWeight.SemiBold)
                    Label(m.detail, 32, color = LocalInk.current.secondary)
                }
            }
        }
        if (profile.model == CarModel.FOTO_PROPIA) {
            Spacer(Modifier.height(16.u))
            Label("Tu foto la eliges después en Ajustes → Personalizar.", 30, color = LocalInk.current.secondary)
        }
    }
}

@Composable
private fun RowScope.SelectCard(selected: Boolean, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(40.u)
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shape)
            .background(if (selected) Palette.Accent.copy(alpha = 0.16f) else Palette.ChipBg)
            .border(3.u, if (selected) Palette.Accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(28.u),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) { content() }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).size(64.u).clip(CircleShape).background(Palette.Accent),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(44.u)) }
        }
    }
}

@Composable
private fun StepColor(profile: CarProfile, preview: ImageBitmap?, onChange: (CarProfile) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Title("¿De qué color es?")
        Spacer(Modifier.height(24.u))
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                preview?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth()) }
            }
            Spacer(Modifier.width(40.u))
            // Muestras en 4 columnas
            Column(verticalArrangement = Arrangement.spacedBy(24.u)) {
                CarPaint.ALL.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.u)) {
                        row.forEach { paint ->
                            val selected = profile.paint == paint.name
                            Column(
                                Modifier.width(150.u).clip(RoundedCornerShape(20.u))
                                    .clickable { onChange(profile.copy(paint = paint.name)) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.size(112.u)
                                        .border(if (selected) 6.u else 3.u, if (selected) Palette.Accent else Palette.ChipBg, CircleShape)
                                        .padding(if (selected) 10.u else 4.u)
                                        .clip(CircleShape)
                                        .background(Color(paint.swatch)),
                                )
                                Spacer(Modifier.height(8.u))
                                Label(paint.name, 24, weight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepApps(profile: CarProfile, onChange: (CarProfile) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Title("Música y navegación")
        Spacer(Modifier.height(40.u))
        Label("¿Con qué escuchas música?", 36, color = LocalInk.current.secondary)
        Spacer(Modifier.height(16.u))
        Row(horizontalArrangement = Arrangement.spacedBy(26.u)) {
            MusicApp.entries.forEach { app ->
                val color = when (app) {
                    MusicApp.APPLE_MUSIC -> Color(0xFFFA2D48)
                    MusicApp.SPOTIFY -> Color(0xFF1DB954)
                    MusicApp.YOUTUBE_MUSIC -> Color(0xFFFF0033)
                }
                AppChip(app.label, Icons.Rounded.MusicNote, color, profile.musicApp == app, Modifier.weight(1f)) {
                    onChange(profile.copy(musicApp = app))
                }
            }
        }
        Spacer(Modifier.height(40.u))
        Label("¿Con qué navegas?", 36, color = LocalInk.current.secondary)
        Spacer(Modifier.height(16.u))
        Row(horizontalArrangement = Arrangement.spacedBy(26.u)) {
            NavApp.entries.forEach { app ->
                val (icon, color) = when (app) {
                    NavApp.WAZE -> Icons.Rounded.Navigation to Color(0xFF33CCFF)
                    NavApp.GOOGLE_MAPS -> Icons.Rounded.Place to Color(0xFF34A853)
                }
                AppChip(app.label, icon, color, profile.navApp == app, Modifier.weight(1f)) {
                    onChange(profile.copy(navApp = app))
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppChip(text: String, icon: ImageVector, color: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(36.u)
    Row(
        modifier
            .height(160.u)
            .clip(shape)
            .background(if (selected) Palette.Accent.copy(alpha = 0.16f) else Palette.ChipBg)
            .border(3.u, if (selected) Palette.Accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 34.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(92.u).clip(RoundedCornerShape(24.u)).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(56.u))
        }
        Spacer(Modifier.width(24.u))
        Label(text, 40, weight = FontWeight.Medium)
    }
}

@Composable
private fun WizardButton(text: String, accent: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(124.u)
            .clip(RoundedCornerShape(62.u))
            .background(if (accent) Palette.Accent else Palette.ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 60.u),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, 42, color = if (accent) Color.White else LocalInk.current.primary, weight = FontWeight.SemiBold)
    }
}
