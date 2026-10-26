package com.luis.gvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Pantallas del launcher, en el orden de la barra lateral. */
enum class Screen(val icon: ImageVector, val title: String) {
    HOME(Icons.Rounded.Home, "Inicio"),
    NAV(Icons.Rounded.Navigation, "Navegación"),
    PHONE(Icons.Rounded.Phone, "Teléfono"),
    MUSIC(Icons.Rounded.MusicNote, "Música"),
    APPS(Icons.Rounded.Apps, "Aplicaciones"),
    SETTINGS(Icons.Rounded.Settings, "Ajustes"),
}

/** Barra lateral compartida por todas las pantallas. */
@Composable
fun SideRail(current: Screen, onSelect: (Screen) -> Unit) {
    Column(
        Modifier
            .width(128.u)
            .fillMaxHeight()
            .background(Palette.Rail)
            .padding(top = 135.u, bottom = 110.u),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Screen.entries.forEach { s ->
            RailItem(s.icon, selected = s == current) { onSelect(s) }
        }
    }
}

@Composable
private fun RailItem(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.u)
            .background(if (selected) Palette.RailSelected else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(5.u)
                    .height(76.u)
                    .background(Palette.Accent, RoundedCornerShape(3.u))
            )
        }
        Icon(
            icon, null,
            tint = if (selected) Palette.Accent else LocalInk.current.secondary,
            modifier = Modifier.size(48.u),
        )
    }
}
