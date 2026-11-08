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
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import com.luis.gvlauncher.data.UpdateInfo
import com.luis.gvlauncher.data.Updates

/**
 * Aviso de versión nueva, como en una tienda de apps:
 * "Actualizar ahora" descarga el APK en la radio y abre el instalador de Android.
 * El QR queda para radios sin internet (se baja en el celular y se pasa por USB).
 *
 * [progress]: null = sin descargar, 0..1 = descargando, -1 = descargando sin tamaño conocido.
 */
@Composable
fun UpdateDialog(
    info: UpdateInfo,
    installed: String,
    progress: Float?,
    error: String?,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    val qr = remember(info.pageUrl) { runCatching { Updates.qr(info.pageUrl).asImageBitmap() }.getOrNull() }
    val downloading = progress != null
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
                    Column(verticalArrangement = Arrangement.spacedBy(14.u)) {
                        info.news.ifEmpty { listOf("Mejoras y arreglos") }.forEach { line ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(16.u).clip(CircleShape).background(Color(0xFF3D8BFF)))
                                Spacer(Modifier.width(18.u))
                                Label(line, 32)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    when {
                        downloading -> {
                            // Barra de descarga
                            val p = progress ?: 0f
                            Label(
                                if (p < 0f) "Descargando…" else "Descargando… ${(p * 100).toInt()}%",
                                32, color = Color(0xFFB4C4EA),
                            )
                            Spacer(Modifier.height(14.u))
                            Box(
                                Modifier.fillMaxWidth().height(18.u).clip(RoundedCornerShape(9.u))
                                    .background(Color(0x26FFFFFF)),
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(if (p < 0f) 1f else p.coerceIn(0.02f, 1f)).fillMaxHeight()
                                        .clip(RoundedCornerShape(9.u)).background(Color(0xFF3D8BFF)),
                                )
                            }
                            Spacer(Modifier.height(20.u))
                            Label("Al terminar, toca \"Instalar\" en la pantalla de Android.", 28, color = Color(0xFFB4C4EA))
                        }
                        else -> {
                            if (error != null) {
                                Label(error, 28, color = Color(0xFFFFB547))
                                Spacer(Modifier.height(14.u))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(26.u)) {
                                DialogButton("Más tarde", accent = false, onClick = onLater)
                                if (info.apkUrl != null) {
                                    DialogButton(if (error != null) "Reintentar" else "Actualizar ahora", accent = true, onClick = onUpdate)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(40.u))
                Column(Modifier.width(400.u), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(340.u).clip(RoundedCornerShape(26.u)).background(Color.White).padding(14.u),
                        contentAlignment = Alignment.Center,
                    ) {
                        qr?.let { Image(it, "Código QR de la versión nueva", modifier = Modifier.fillMaxSize()) }
                    }
                    Spacer(Modifier.height(20.u))
                    Label("¿La radio sin internet?", 28, color = Color(0xFFB4C4EA))
                    Label("Escanéalo con tu celular,", 26, color = Color(0xFFB4C4EA))
                    Label("baja el APK y pásalo por USB", 26, color = Color(0xFFB4C4EA))
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
