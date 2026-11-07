package com.luis.gvlauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Versión publicada en GitHub (Releases) más nueva que la instalada. */
data class UpdateInfo(
    val version: String,
    /** Hasta 4 novedades, tomadas de las líneas "- ..." de la descripción del release. */
    val news: List<String>,
    /** Página del release: es lo que abre el QR en el celular. */
    val pageUrl: String,
)

/**
 * Revisa si hay una versión nueva en GitHub Releases del proyecto.
 * Solo necesita internet (WiFi del iPhone, por ejemplo); si no hay, no pasa nada.
 * Como mucho revisa una vez cada 12 horas, y si tocas "Más tarde" no vuelve a avisar
 * de esa misma versión por 3 días.
 */
object Updates {
    const val REPO = "Luiz-labs/grand-vitara-launcher"
    private const val CHECK_EVERY_MS = 12 * 60 * 60 * 1000L
    private const val SNOOZE_MS = 3 * 24 * 60 * 60 * 1000L

    suspend fun check(context: Context, installed: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("actualizaciones", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("ultima_revision", 0L) < CHECK_EVERY_MS) return@withContext null
        val info = runCatching { fetchLatest() }.getOrNull() ?: return@withContext null
        prefs.edit().putLong("ultima_revision", now).apply()
        if (!isNewer(info.version, installed)) return@withContext null
        val snoozed = prefs.getString("pospuesta", null) == info.version &&
            now - prefs.getLong("pospuesta_en", 0L) < SNOOZE_MS
        if (snoozed) null else info
    }

    /** "Más tarde": no volver a avisar de esta versión por unos días. */
    fun snooze(context: Context, version: String) {
        context.getSharedPreferences("actualizaciones", Context.MODE_PRIVATE).edit()
            .putString("pospuesta", version)
            .putLong("pospuesta_en", System.currentTimeMillis())
            .apply()
    }

    private fun fetchLatest(): UpdateInfo? {
        val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "GrandVitaraLauncher")
        try {
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val version = json.optString("tag_name").removePrefix("v").ifBlank { return null }
            val news = json.optString("body").lines()
                .map { it.trim() }
                .filter { it.startsWith("- ") || it.startsWith("* ") }
                .map { it.drop(2).trim() }
                .take(4)
            val page = json.optString("html_url").ifBlank { "https://github.com/$REPO/releases/latest" }
            return UpdateInfo(version, news, page)
        } finally {
            conn.disconnect()
        }
    }

    /** Compara "0.10" contra "0.9" número por número. */
    fun isNewer(remote: String, installed: String): Boolean {
        val a = remote.split('.').map { it.toIntOrNull() ?: 0 }
        val b = installed.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Código QR en blanco y negro para mostrar en pantalla. */
    fun qr(text: String, size: Int = 360): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
        val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}
