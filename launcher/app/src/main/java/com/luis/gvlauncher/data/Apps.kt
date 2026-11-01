package com.luis.gvlauncher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.luis.gvlauncher.Shortcut
import java.text.Normalizer

data class AppEntry(val label: String, val packageName: String, val icon: ImageBitmap)

object Apps {

    fun installedLaunchables(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map {
                AppEntry(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm).toBitmap(96).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun firstInstalled(context: Context, candidates: List<String>): String? =
        candidates.firstOrNull { context.packageManager.getLaunchIntentForPackage(it) != null }

    /** Launches the first installed candidate; returns false if none exist. */
    fun launchAny(context: Context, candidates: List<String>): Boolean {
        val pkg = firstInstalled(context, candidates) ?: return false
        return launch(context, pkg)
    }

    /**
     * Abre un acceso directo: primero por paquete exacto y, si no existe,
     * por nombre visible de la app (así funciona aunque no sepamos el paquete).
     */
    fun launchShortcut(context: Context, shortcut: Shortcut): Boolean {
        if (launchAny(context, shortcut.packages)) return true
        val pkg = findByLabel(context, shortcut.labels, shortcut.exclude) ?: return false
        return launch(context, pkg)
    }

    /** Paquete que se abriría con este acceso, o null si no hay app instalada. */
    fun resolve(context: Context, shortcut: Shortcut): String? =
        firstInstalled(context, shortcut.packages) ?: findByLabel(context, shortcut.labels, shortcut.exclude)

    fun findByLabel(context: Context, keywords: List<String>, exclude: Set<String> = emptySet()): String? {
        if (keywords.isEmpty()) return null
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName && it.activityInfo.packageName !in exclude }
            .map { normalize(it.loadLabel(pm).toString()) to it.activityInfo.packageName }
        // Orden de prioridad = orden de las palabras clave en Config.
        for (k in keywords.map(::normalize)) {
            apps.firstOrNull { (label, _) -> label.split(' ', '-', '_').any { it == k } || (k.length > 3 && k in label) }
                ?.let { return it.second }
        }
        return null
    }

    /** Navega a una dirección o lugar escrito, con la app de navegación elegida. */
    fun navigateTo(context: Context, app: NavApp, destination: String): Boolean {
        val q = Uri.encode(destination.trim())
        // Usa la app que realmente está instalada (si Waze no está, cae a Google Maps).
        val pkg = resolve(context, app.shortcut)
        val uri = if (pkg == "com.waze") "waze://?q=$q&navigate=yes" else "google.navigation:q=$q"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        pkg?.let { intent.setPackage(it) }
        return start(context, intent)
    }

    /**
     * Ir a Casa / Trabajo. Con Waze instalado usa los lugares guardados en Waze; si no,
     * usa la dirección que el usuario guardó en la pantalla Navegación.
     */
    fun navigateFavorite(context: Context, app: NavApp, favorite: String, address: String): Boolean =
        when {
            resolve(context, app.shortcut) == "com.waze" -> openWazeFavorite(context, favorite)
            address.isNotBlank() -> navigateTo(context, app, address)
            else -> false
        }

    /** Llama por la app de teléfono de la radio (Bluetooth). Sin permiso, abre el marcador. */
    fun call(context: Context, number: String): Boolean {
        val uri = Uri.parse("tel:" + Uri.encode(number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }))
        val canCall = context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val intent = Intent(if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, intent) || start(context, Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Waze: "home" / "work" abren directo la ruta a Casa / Trabajo guardados en Waze. */
    fun openWazeFavorite(context: Context, favorite: String): Boolean =
        start(
            context,
            Intent(Intent.ACTION_VIEW, Uri.parse("waze://?favorite=$favorite&navigate=yes"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return start(context, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openSettings(context: Context) =
        start(context, Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    fun notInstalled(context: Context, what: String) =
        Toast.makeText(context, "$what: app no encontrada en el radio", Toast.LENGTH_SHORT).show()

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: Exception) { // ActivityNotFound o SecurityException
        false
    }

    private fun Drawable.toBitmap(size: Int): Bitmap {
        if (this is BitmapDrawable && bitmap != null) return bitmap
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, size, size)
        draw(canvas)
        return bmp
    }
}
