package com.luis.gvlauncher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Versión publicada en GitHub (Releases) más nueva que la instalada. */
data class UpdateInfo(
    val version: String,
    /** Hasta 4 novedades, tomadas de las líneas "- ..." de la descripción del release. */
    val news: List<String>,
    /** Página del release: es lo que abre el QR en el celular. */
    val pageUrl: String,
    /** Descarga directa del APK (para "Actualizar ahora" desde la radio). */
    val apkUrl: String?,
)

/** Resultado de "Buscar actualizaciones" (botón de Ajustes). */
sealed interface UpdateCheck {
    data class Available(val info: UpdateInfo) : UpdateCheck
    data object UpToDate : UpdateCheck
    data object Offline : UpdateCheck
}

/**
 * Actualizaciones desde GitHub Releases del proyecto, como una tienda de apps:
 *  - Al prender, revisa sola (como mucho cada 12 horas) y avisa si hay versión nueva.
 *  - En Ajustes → "Buscar actualizaciones" revisa en el momento.
 *  - "Actualizar ahora" descarga el APK en la radio y abre el instalador de Android
 *    (Android siempre pide un toque en "Instalar"; eso no se puede saltar).
 *  - El QR sigue disponible para radios sin internet.
 */
object Updates {
    const val REPO = "Luiz-labs/grand-vitara-launcher"
    private const val CHECK_EVERY_MS = 12 * 60 * 60 * 1000L
    private const val SNOOZE_MS = 3 * 24 * 60 * 60 * 1000L

    /** Revisión automática al prender (respeta el intervalo y el "Más tarde"). */
    suspend fun check(context: Context, installed: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val prefs = prefs(context)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("ultima_revision", 0L) < CHECK_EVERY_MS) return@withContext null
        val info = runCatching { fetchLatest() }.getOrElse { return@withContext null } ?: return@withContext null
        prefs.edit().putLong("ultima_revision", now).apply()
        if (!isNewer(info.version, installed)) return@withContext null
        val snoozed = prefs.getString("pospuesta", null) == info.version &&
            now - prefs.getLong("pospuesta_en", 0L) < SNOOZE_MS
        if (snoozed) null else info
    }

    /** Revisión a pedido (botón "Buscar actualizaciones"): sin esperas ni "Más tarde". */
    suspend fun checkNow(context: Context, installed: String): UpdateCheck = withContext(Dispatchers.IO) {
        val info = try {
            fetchLatest()
        } catch (e: Exception) {
            return@withContext UpdateCheck.Offline
        }
        prefs(context).edit().putLong("ultima_revision", System.currentTimeMillis()).apply()
        if (info != null && isNewer(info.version, installed)) UpdateCheck.Available(info) else UpdateCheck.UpToDate
    }

    /** "Más tarde": no volver a avisar de esta versión por unos días. */
    fun snooze(context: Context, version: String) {
        prefs(context).edit()
            .putString("pospuesta", version)
            .putLong("pospuesta_en", System.currentTimeMillis())
            .apply()
    }

    /**
     * Descarga el APK a la memoria interna del launcher. [onProgress] recibe 0..1
     * (o -1 si GitHub no informa el tamaño).
     */
    suspend fun download(context: Context, url: String, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "actualizaciones").apply { mkdirs() }
        val file = File(dir, "launcher.apk")
        file.delete()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "GrandVitaraLauncher")
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            val total = conn.contentLength.toLong()
            var read = 0L
            var lastStep = -1
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) {
                            val step = (read * 100 / total).toInt()
                            if (step != lastStep) { lastStep = step; onProgress(step / 100f) }
                        } else {
                            onProgress(-1f)
                        }
                    }
                }
            }
            if (total > 0 && read < total) throw IOException("Descarga incompleta")
            file
        } finally {
            conn.disconnect()
        }
    }

    /** Abre el instalador de Android con el APK descargado. */
    fun install(context: Context, apk: File): Boolean = try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    private fun prefs(context: Context) = context.getSharedPreferences("actualizaciones", Context.MODE_PRIVATE)

    /** null = no hay versiones publicadas. Lanza excepción si no hay internet. */
    private fun fetchLatest(): UpdateInfo? {
        val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "GrandVitaraLauncher")
        try {
            val code = conn.responseCode
            if (code == 404) return null
            if (code != 200) throw IOException("HTTP $code")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val version = json.optString("tag_name").removePrefix("v").ifBlank { return null }
            val news = json.optString("body").lines()
                .map { it.trim() }
                .filter { it.startsWith("- ") || it.startsWith("* ") }
                .map { it.drop(2).trim() }
                .take(4)
            val page = json.optString("html_url").ifBlank { "https://github.com/$REPO/releases/latest" }
            return UpdateInfo(version, news, page, apkUrl(json.optJSONArray("assets")))
        } finally {
            conn.disconnect()
        }
    }

    private fun apkUrl(assets: JSONArray?): String? {
        if (assets == null) return null
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            if (a.optString("name").endsWith(".apk")) return a.optString("browser_download_url").ifBlank { null }
        }
        return null
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
