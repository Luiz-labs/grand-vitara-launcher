package com.luis.gvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import com.luis.gvlauncher.data.UpdateInfo
import com.luis.gvlauncher.data.Updates

/**
 * Aviso de versión nueva: novedades + QR para bajarla con el celular.
 * "Cómo instalarla" muestra los pasos; "Más tarde" lo cierra por unos días.
 */
@Composable
fun UpdateDialog(info: UpdateInfo, installed: String, onLater: () -> Unit) {
    var showSteps by remember { mutableStateOf(false) }
    val qr = remember(info.pageUrl) { runCatching { Updates.qr(info.pageUrl).asImageBitmap() }.getOrNull() }
    // Fondo oscuro que bloquea los toques del inicio
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB3020612))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(44.u)
        OnColor {
            Row(
                Modifier
                    .width(1380.u)
                    .height(760.u)
                    .clip(shape)
                    .background(Color(0xFF101C40))
                    .border(2.u, Color(0xFF2A3D74), shape)
                    .padding(56.u),
            ) {
                Column(Modifier.weight(1f).fillMaxSize()) {
                    Row(
                        Modifier.clip(RoundedCornerShape(30.u)).background(Color(0x2E3D8BFF))
                            .padding(horizontal = 24.u, vertical = 12.u),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, null, tint = Color(0xFF7FB2FF), modifier = Modifier.size(38.u))
                        Spacer(Modifier.width(12.u))
                        Label("Actualización disponible", 30, color = Color(0xFF7FB2FF))
                    }
                    Spacer(Modifier.height(24.u))
                    Label("Hay una versión nueva", 60, weight = FontWeight.SemiBold)
                    Label("v${info.version} · tú tienes la v$installed", 38, color = Color(0xFFB4C4EA))
                    Spacer(Modifier.height(30.u))
                    val lines = if (showSteps) listOf(
                        "1. Escanea el QR y baja el APK",
                        "2. Cópialo a un USB y conéctalo",
                        "3. En la radio abre el Gestor de archivos",
                        "4. Toca el APK → Instalar",
                    ) else info.news.ifEmpty { listOf("Mejoras y arreglos") }
                    Column(verticalArrangement = Arrangement.spacedBy(14.u)) {
                        lines.forEach { line ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!showSteps) {
                                    Box(Modifier.size(16.u).clip(CircleShape).background(Color(0xFF3D8BFF)))
                                    Spacer(Modifier.width(18.u))
                                }
                                Label(line, 32)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(26.u)) {
                        DialogButton("Más tarde", accent = false, onClick = onLater)
                        DialogButton(if (showSteps) "Ver novedades" else "Cómo instalarla", accent = true) {
                            showSteps = !showSteps
                        }
                    }
                }
                Spacer(Modifier.width(40.u))
                Column(Modifier.width(400.u), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(380.u).clip(RoundedCornerShape(26.u)).background(Color.White).padding(14.u),
                        contentAlignment = Alignment.Center,
                    ) {
                        qr?.let { Image(it, "Código QR de la versión nueva", modifier = Modifier.fillMaxSize()) }
                    }
                    Spacer(Modifier.height(22.u))
                    Label("Escanéalo con tu celular", 30, color = Color(0xFFB4C4EA))
                    Label("para bajar la versión nueva", 30, color = Color(0xFFB4C4EA))
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, accent: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(124.u)
            .clip(RoundedCornerShape(62.u))
            .background(if (accent) Color(0xFF3D8BFF) else Color(0x1FFFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 52.u),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, 40, weight = FontWeight.SemiBold)
    }
}
