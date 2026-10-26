package com.luis.gvlauncher.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Phone
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
import androidx.compose.ui.text.font.FontWeight
import com.luis.gvlauncher.data.Contact

class PhoneActions(
    val call: (String) -> Unit,
    val addContact: (Contact) -> Unit,
    val deleteContact: (Contact) -> Unit,
    /** Abre la app de Bluetooth de la radio (contactos, recientes, emparejar). */
    val openRadioPhone: () -> Unit,
)

private val avatarColors = listOf(
    Color(0xFFE05F8A), Color(0xFF3D8BFF), Color(0xFF22C55E), Color(0xFFFFB547), Color(0xFFA06BFF), Color(0xFF14B8A6),
)

/**
 * Teléfono: favoritos de marcación rápida + teclado. La llamada la hace la app Bluetooth de la radio
 * (la pantalla de "llamada en curso" es la de la radio; ninguna app externa puede reemplazarla).
 */
@Composable
fun PhoneScreen(bluetoothOn: Boolean, contacts: List<Contact>, actions: PhoneActions) {
    var number by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newNumber by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(start = 40.u, end = 36.u, top = 16.u, bottom = 30.u)) {
        ScreenHeader("Teléfono", if (bluetoothOn) "Bluetooth activado" else "Bluetooth apagado") {
            ActionPill("Contactos y recientes", Icons.Rounded.Bluetooth, onClick = actions.openRadioPhone)
        }
        Spacer(Modifier.height(18.u))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.u)) {
            // Favoritos
            SurfaceCard(Modifier.weight(1f).fillMaxHeight()) {
                Column {
                    CardSectionTitle("Favoritos", if (adding) null else "+ Agregar") { adding = true }
                    if (adding) {
                        AddForm(
                            "Nombre", "Número", newName, newNumber, { newName = it }, { newNumber = it },
                            phone = true,
                            onSave = {
                                if (newName.isNotBlank() && newNumber.isNotBlank()) {
                                    actions.addContact(Contact(newName.trim(), newNumber.trim()))
                                    newName = ""; newNumber = ""; adding = false
                                }
                            },
                            onCancel = { adding = false },
                        )
                    }
                    if (contacts.isEmpty() && !adding) {
                        Label(
                            "Agrega a las personas que llamas seguido para marcar con un toque.", 22,
                            color = LocalInk.current.secondary, modifier = Modifier.padding(28.u),
                        )
                    }
                    LazyColumn {
                        itemsIndexed(contacts) { i, c ->
                            ListRow(
                                c.name, c.number,
                                badgeColor = avatarColors[i % avatarColors.size],
                                badgeText = initials(c.name),
                                onClick = { actions.call(c.number) },
                            ) {
                                Icon(
                                    Icons.Rounded.Delete, "Eliminar", tint = LocalInk.current.secondary,
                                    modifier = Modifier.size(56.u).clickableRound { actions.deleteContact(c) }.padding(12.u),
                                )
                                Spacer(Modifier.width(8.u))
                                Icon(Icons.Rounded.Phone, null, tint = Palette.PhoneGreen, modifier = Modifier.size(40.u))
                            }
                        }
                    }
                }
            }
            // Teclado
            SurfaceCard(Modifier.weight(1f).fillMaxHeight()) {
                Column(
                    Modifier.fillMaxSize().padding(24.u),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.fillMaxWidth().height(80.u), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(56.u))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Label(number.ifEmpty { " " }, 50, weight = FontWeight.Medium)
                        }
                        Icon(
                            Icons.Rounded.Backspace, "Borrar", tint = LocalInk.current.secondary,
                            modifier = Modifier.size(56.u).clickableRound { number = number.dropLast(1) }.padding(10.u),
                        )
                    }
                    Spacer(Modifier.height(10.u))
                    listOf("123", "456", "789", "*0#").forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.u), modifier = Modifier.padding(bottom = 14.u)) {
                            row.forEach { k ->
                                Box(
                                    Modifier
                                        .size(width = 140.u, height = 84.u)
                                        .clip(RoundedCornerShape(20.u))
                                        .background(Palette.ChipBg)
                                        .clickable { if (number.length < 20) number += k },
                                    contentAlignment = Alignment.Center,
                                ) { Label(k.toString(), 40) }
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(104.u)
                            .clip(CircleShape)
                            .background(Palette.PhoneGreen)
                            .clickable { if (number.isNotBlank()) actions.call(number) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Phone, "Llamar", tint = Color.White, modifier = Modifier.size(52.u)) }
                }
            }
        }
    }
}

private fun initials(name: String) =
    name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
