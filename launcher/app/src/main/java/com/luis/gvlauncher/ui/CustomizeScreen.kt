package com.luis.gvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.luis.gvlauncher.Config
import com.luis.gvlauncher.data.CarModel
import com.luis.gvlauncher.data.CarPaint
import com.luis.gvlauncher.data.CarProfile

/**
 * Pantalla "Personalizar": nombre del conductor, modelo (3 / 5 puertas / foto propia) y color.
 * Cada cambio se aplica al instante en la vista previa; "Listo" guarda y vuelve al inicio.
 */
@Composable
fun CustomizeScreen(
    profile: CarProfile,
    preview: ImageBitmap?,
    onChange: (CarProfile) -> Unit,
    onPickPhoto: () -> Unit,
    onUseDownloadPhoto: () -> Unit,
    onDone: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxSize().padding(horizontal = 60.u, vertical = 36.u)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.ArrowBack, "Volver", tint = Color.White,
                modifier = Modifier.size(64.u).clip(CircleShape).clickable { focus.clearFocus(); onDone() }.padding(10.u),
            )
            Spacer(Modifier.width(20.u))
            Label("Personalizar", 40, weight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            PillButton(Icons.Rounded.Check, "Listo", Palette.Accent) { focus.clearFocus(); onDone() }
        }
        Spacer(Modifier.height(28.u))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.u)) {
            // Vista previa
            GlassCard(
                Palette.HeroTop, Palette.HeroBottom, Palette.HeroBorder,
                Modifier.weight(1f).fillMaxHeight(),
            ) {
                Column(Modifier.fillMaxSize().padding(36.u)) {
                    Label("Hola, ${profile.driverName.ifBlank { Config.DRIVER_NAME }}", 48, weight = FontWeight.SemiBold)
                    Label(Config.CAR_NAME, 26, color = Palette.TextSecondary)
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (preview != null) {
                            Image(preview, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        } else {
                            Label("Preparando imagen…", 24, color = Palette.TextSecondary)
                        }
                    }
                }
            }
            // Opciones
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.u)) {
                Section("Tu nombre")
                NameField(profile.driverName, focus) { onChange(profile.copy(driverName = it)) }

                Section("Modelo")
                Row(horizontalArrangement = Arrangement.spacedBy(14.u)) {
                    CarModel.entries.forEach { m ->
                        Chip(m.label, selected = profile.model == m, Modifier.weight(1f)) {
                            focus.clearFocus()
                            onChange(profile.copy(model = m))
                        }
                    }
                }

                if (profile.model == CarModel.FOTO_PROPIA) {
                    Section("Tu foto (PNG con fondo transparente se ve mejor)")
                    Row(horizontalArrangement = Arrangement.spacedBy(14.u)) {
                        PillButton(Icons.Rounded.Image, "Elegir foto", Color(0x33FFFFFF), Modifier.weight(1f), onPickPhoto)
                        PillButton(Icons.Rounded.Folder, "Usar Download/auto.png", Color(0x33FFFFFF), Modifier.weight(1.4f), onUseDownloadPhoto)
                    }
                } else {
                    Section("Color: ${profile.paint}")
                    // 11 colores en 2 filas
                    CarPaint.ALL.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(18.u)) {
                            row.forEach { p ->
                                Swatch(p, selected = p.name == profile.paint) {
                                    focus.clearFocus()
                                    onChange(profile.copy(paint = p.name))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(text: String) {
    Spacer(Modifier.height(6.u))
    Label(text, 22, color = Palette.TextSecondary)
}

@Composable
private fun NameField(value: String, focus: FocusManager, onValue: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = { onValue(it.take(18)) },
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 30.us),
        cursorBrush = SolidColor(Palette.Accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(76.u)
                    .clip(RoundedCornerShape(18.u))
                    .background(Color(0x22FFFFFF))
                    .border(1.5.u, Color(0x553D8BFF), RoundedCornerShape(18.u))
                    .padding(horizontal = 24.u),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Label("Escribe tu nombre", 30, color = Palette.TextSecondary)
                inner()
            }
        },
    )
}

@Composable
private fun Chip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(70.u)
            .clip(RoundedCornerShape(35.u))
            .background(if (selected) Palette.Accent else Color(0x22FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, 24, weight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun Swatch(paint: CarPaint, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(72.u)
            .clip(CircleShape)
            .border(if (selected) 4.u else 1.5.u, if (selected) Color.White else Color(0x55FFFFFF), CircleShape)
            .padding(7.u)
            .clip(CircleShape)
            .background(Color(paint.swatch))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (paint.isOriginal && !selected) Label("Orig.", 15)
        if (selected) {
            val dark = Color(paint.swatch).let { it.red + it.green + it.blue } > 2.0f
            Icon(Icons.Rounded.Check, null, tint = if (dark) Color.Black else Color.White, modifier = Modifier.size(34.u))
        }
    }
}

@Composable
private fun PillButton(
    icon: ImageVector,
    text: String,
    bg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(70.u)
            .clip(RoundedCornerShape(35.u))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.u))
        Spacer(Modifier.width(10.u))
        Label(text, 22)
    }
}
