package com.luis.gvlauncher.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.luis.gvlauncher.data.GeoFix
import com.luis.gvlauncher.data.NavApp
import com.luis.gvlauncher.data.Place

class NavActions(
    val search: (String) -> Unit,
    val goHome: () -> Unit,
    val goWork: () -> Unit,
    val goPlace: (Place) -> Unit,
    val addPlace: (Place) -> Unit,
    val deletePlace: (Place) -> Unit,
    val setHomeAddress: (String) -> Unit,
    val setWorkAddress: (String) -> Unit,
    val switchApp: () -> Unit,
)

@Composable
fun NavScreen(
    app: NavApp,
    fix: GeoFix?,
    places: List<Place>,
    homeAddress: String,
    workAddress: String,
    actions: NavActions,
) {
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    // "casa" / "trabajo" mientras se edita su dirección (solo hace falta con Google Maps).
    var editing by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(start = 40.u, end = 36.u, top = 16.u, bottom = 30.u)) {
        ScreenHeader("Navegación", "Abre en ${app.label}") {
            ActionPill("App: ${app.label}", Icons.Rounded.Navigation, onClick = actions.switchApp)
        }
        Spacer(Modifier.height(14.u))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.u)) {
            InputField(
                query, { query = it }, "¿A dónde vas? Escribe un destino…",
                Modifier.weight(1f), leading = Icons.Rounded.Search,
                onDone = { if (query.isNotBlank()) actions.search(query) },
            )
            ActionPill("Ir", accent = true) { if (query.isNotBlank()) actions.search(query) }
        }
        Spacer(Modifier.height(20.u))

        // Casa / Trabajo / velocidad
        Row(Modifier.fillMaxWidth().height(270.u), horizontalArrangement = Arrangement.spacedBy(22.u)) {
            DestinationCard(
                "Casa", Icons.Rounded.Home, Palette.HeroTop, Palette.HeroBottom, Palette.HeroBorder,
                subtitle = favSubtitle(app, homeAddress),
                onClick = actions.goHome,
                onEdit = { editing = "casa"; editText = homeAddress },
                modifier = Modifier.weight(1f),
            )
            DestinationCard(
                "Trabajo", Icons.Rounded.Work, Palette.TealTop, Palette.TealBottom, Palette.TealBorder,
                subtitle = favSubtitle(app, workAddress),
                onClick = actions.goWork,
                onEdit = { editing = "trabajo"; editText = workAddress },
                modifier = Modifier.weight(1f),
            )
            SurfaceCard(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.padding(30.u)) {
                    Label("VELOCIDAD", 19, color = LocalInk.current.secondary)
                    Spacer(Modifier.height(6.u))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Label(fix?.speedKmh?.toString() ?: "--", 96, weight = FontWeight.SemiBold)
                        Spacer(Modifier.width(10.u))
                        Label("km/h", 30, color = LocalInk.current.secondary, modifier = Modifier.padding(bottom = 18.u))
                    }
                    Label(if (fix != null) "GPS activo" else "Buscando GPS…", 19, color = LocalInk.current.secondary)
                }
            }
        }
        Spacer(Modifier.height(20.u))

        // Favoritos
        SurfaceCard(Modifier.fillMaxSize()) {
            Column {
                val target = editing
                if (target != null) {
                    CardSectionTitle("Dirección de ${if (target == "casa") "Casa" else "Trabajo"} para ${app.label}")
                    Row(Modifier.padding(20.u), horizontalArrangement = Arrangement.spacedBy(12.u)) {
                        InputField(editText, { editText = it }, "Calle, número, distrito", Modifier.weight(1f))
                        ActionPill("Guardar", accent = true) {
                            if (target == "casa") actions.setHomeAddress(editText) else actions.setWorkAddress(editText)
                            editing = null
                        }
                        ActionPill("Cancelar") { editing = null }
                    }
                }
                CardSectionTitle("Destinos favoritos", if (adding) null else "+ Agregar") { adding = true }
                if (adding) {
                    AddForm(
                        "Nombre (ej. Compañía de Bomberos)", "Dirección o lugar",
                        newName, newAddress, { newName = it }, { newAddress = it },
                        onSave = {
                            if (newName.isNotBlank() && newAddress.isNotBlank()) {
                                actions.addPlace(Place(newName.trim(), newAddress.trim()))
                                newName = ""; newAddress = ""; adding = false
                            }
                        },
                        onCancel = { adding = false },
                    )
                }
                if (places.isEmpty() && !adding) {
                    Label(
                        "Agrega los lugares a los que vas seguido para ir con un toque.", 22,
                        color = LocalInk.current.secondary, modifier = Modifier.padding(28.u),
                    )
                }
                LazyColumn {
                    items(places) { p ->
                        ListRow(
                            p.name, p.address,
                            badgeColor = Palette.EqAmber, badgeIcon = Icons.Rounded.Star,
                            onClick = { actions.goPlace(p) },
                        ) {
                            Icon(
                                Icons.Rounded.Delete, "Eliminar", tint = LocalInk.current.secondary,
                                modifier = Modifier.size(56.u).clickableRound { actions.deletePlace(p) }.padding(12.u),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun favSubtitle(app: NavApp, address: String) = when {
    address.isNotBlank() -> address
    app == NavApp.WAZE -> "Guardado en Waze · o pon la dirección con el lápiz"
    else -> "Toca el lápiz para poner la dirección"
}

@Composable
private fun DestinationCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    top: Color,
    bottom: Color,
    border: Color,
    subtitle: String,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    modifier: Modifier,
) {
    GlassCard(top, bottom, border, modifier.fillMaxHeight(), onClick = onClick) {
        CardInk {
            Column(Modifier.fillMaxSize().padding(30.u)) {
                Row {
                    Icon(icon, null, tint = LocalInk.current.primary, modifier = Modifier.size(68.u))
                    Spacer(Modifier.weight(1f))
                    if (onEdit != null) {
                        Icon(
                            Icons.Rounded.Edit, "Editar dirección", tint = LocalInk.current.primary,
                            modifier = Modifier.size(56.u).clickableRound(onEdit).padding(10.u),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Label(title, 38, weight = FontWeight.SemiBold)
                Label(subtitle, 20, color = LocalInk.current.secondary)
            }
        }
    }
}
