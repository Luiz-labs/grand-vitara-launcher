package com.luis.gvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/** Título de pantalla con subtítulo y botones a la derecha. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(80.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(title, 44, weight = FontWeight.SemiBold)
        if (subtitle != null) {
            Spacer(Modifier.width(22.u))
            Label(subtitle, 23, color = LocalInk.current.secondary)
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(14.u), content = actions)
    }
}

/** Botón redondeado con ícono y texto. [accent] = fondo azul y texto blanco. */
@Composable
fun ActionPill(
    text: String,
    icon: ImageVector? = null,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fg = if (accent) Color.White else LocalInk.current.primary
    Row(
        modifier
            .height(66.u)
            .clip(RoundedCornerShape(33.u))
            .background(if (accent) Palette.Accent else Palette.ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(30.u))
            Spacer(Modifier.width(10.u))
        }
        Label(text, 23, color = fg)
    }
}

/** Tarjeta neutra (cambia con el tema). */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    GlassCard(Palette.TileTop, Palette.TileBottom, Palette.TileBorder, modifier, radius = 22.u, onClick = onClick) {
        content()
    }
}

/** Fila de lista: círculo de color con ícono o iniciales, título, subtítulo y acción a la derecha. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    badgeColor: Color = Palette.Accent,
    badgeIcon: ImageVector? = null,
    badgeText: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(98.u)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 28.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(62.u).clip(CircleShape).background(badgeColor), contentAlignment = Alignment.Center) {
            if (badgeIcon != null) Icon(badgeIcon, null, tint = Color.White, modifier = Modifier.size(32.u))
            else if (badgeText != null) Label(badgeText, 24, color = Color.White, weight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(22.u))
        Column(Modifier.weight(1f)) {
            Label(title, 26)
            if (subtitle != null) Label(subtitle, 19, color = LocalInk.current.secondary)
        }
        trailing()
    }
    Box(Modifier.fillMaxWidth().height(1.u).background(Palette.Divider))
}

/** Encabezado pequeño dentro de una tarjeta ("FAVORITOS   + Agregar"). */
@Composable
fun CardSectionTitle(text: String, actionText: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(62.u).padding(horizontal = 28.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(text.uppercase(), 19, color = LocalInk.current.secondary)
        Spacer(Modifier.weight(1f))
        if (actionText != null) {
            Label(actionText, 21, color = Palette.Accent, modifier = Modifier.clickable(onClick = onAction).padding(8.u))
        }
    }
    Box(Modifier.fillMaxWidth().height(1.u).background(Palette.Divider))
}

/** Campo de texto de una línea con el estilo del launcher. */
@Composable
fun InputField(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    phone: Boolean = false,
    onDone: () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        textStyle = TextStyle(color = LocalInk.current.primary, fontSize = 26.us),
        cursorBrush = SolidColor(Palette.Accent),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (phone) KeyboardType.Phone else KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); onDone() }),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(76.u)
                    .clip(RoundedCornerShape(18.u))
                    .background(Palette.ChipBg)
                    .border(1.5.u, Palette.Accent.copy(alpha = 0.35f), RoundedCornerShape(18.u))
                    .padding(horizontal = 22.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    Icon(leading, null, tint = LocalInk.current.secondary, modifier = Modifier.size(32.u))
                    Spacer(Modifier.width(14.u))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Label(placeholder, 24, color = LocalInk.current.secondary)
                    inner()
                }
            }
        },
    )
}

/** Formulario en línea para agregar un favorito (nombre + dato). */
@Composable
fun AddForm(
    nameHint: String,
    valueHint: String,
    name: String,
    value: String,
    onName: (String) -> Unit,
    onValue: (String) -> Unit,
    phone: Boolean = false,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(20.u), verticalArrangement = Arrangement.spacedBy(12.u)) {
        InputField(name, onName, nameHint)
        InputField(value, onValue, valueHint, phone = phone)
        Row(horizontalArrangement = Arrangement.spacedBy(12.u)) {
            ActionPill("Guardar", accent = true, modifier = Modifier.weight(1f), onClick = onSave)
            ActionPill("Cancelar", modifier = Modifier.weight(1f), onClick = onCancel)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.u).background(Palette.Divider))
}

/** Área táctil circular (para íconos sueltos como borrar o editar). */
fun Modifier.clickableRound(onClick: () -> Unit): Modifier = this.clip(CircleShape).clickable(onClick = onClick)
