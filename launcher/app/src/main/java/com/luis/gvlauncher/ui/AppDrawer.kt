package com.luis.gvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.luis.gvlauncher.data.AppEntry

/**
 * Cajón de apps. Los botones − y + cambian el tamaño de los íconos (80 % a 160 %),
 * para tocarlos fácil sin quitar la vista del camino.
 */
@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    iconScale: Int,
    onScale: (Int) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onClose: () -> Unit,
) {
    val k = iconScale / 100f
    Column(Modifier.fillMaxSize().padding(horizontal = 60.u, vertical = 36.u)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.ArrowBack, "Volver", tint = LocalInk.current.primary,
                modifier = Modifier.size(64.u).clip(RoundedCornerShape(32.u)).clickable(onClick = onClose).padding(10.u),
            )
            Spacer(Modifier.width(20.u))
            Label("Aplicaciones", 40, weight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Label("Tamaño", 22, color = LocalInk.current.secondary)
            Spacer(Modifier.width(16.u))
            SizeButton(Icons.Rounded.Remove, enabled = iconScale > 80) { onScale((iconScale - 20).coerceAtLeast(80)) }
            Spacer(Modifier.width(12.u))
            Label("$iconScale%", 24, modifier = Modifier.width(90.u))
            SizeButton(Icons.Rounded.Add, enabled = iconScale < 160) { onScale((iconScale + 20).coerceAtMost(160)) }
        }
        Spacer(Modifier.height(30.u))
        LazyVerticalGrid(
            columns = GridCells.Adaptive((170 * k).u),
            verticalArrangement = Arrangement.spacedBy((26 * k).u),
            horizontalArrangement = Arrangement.spacedBy(16.u),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps, key = { it.packageName }) { app ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(18.u))
                        .clickable { onLaunch(app) }
                        .padding(vertical = 14.u),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(app.icon, null, modifier = Modifier.size((96 * k).u))
                    Spacer(Modifier.height(12.u))
                    Label(app.label, (21 * k).coerceAtMost(30f), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.u))
                }
            }
        }
    }
}

@Composable
private fun SizeButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(66.u)
            .clip(CircleShape)
            .background(if (enabled) Palette.ChipBg else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, null,
            tint = if (enabled) LocalInk.current.primary else LocalInk.current.secondary.copy(alpha = 0.4f),
            modifier = Modifier.size(36.u),
        )
    }
}
