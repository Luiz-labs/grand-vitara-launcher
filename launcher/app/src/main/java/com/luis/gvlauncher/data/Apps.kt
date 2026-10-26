package com.luis.gvlauncher.data

import android.content.ActivityNotFoundException
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
        val pkg = findByLabel(context, shortcut.labels) ?: return false
        return launch(context, pkg)
    }

    fun findByLabel(context: Context, keywords: List<String>): String? {
        if (keywords.isEmpty()) return null
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { normalize(it.loadLabel(pm).toString()) to it.activityInfo.packageName }
        // Orden de prioridad = orden de las palabras clave en Config.
        for (k in keywords.map(::normalize)) {
            apps.firstOrNull { (label, _) -> label.split(' ', '-', '_').any { it == k } || (k.length > 3 && k in label) }
                ?.let { return it.second }
        }
        return null
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
    } catch (e: ActivityNotFoundException) {
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
