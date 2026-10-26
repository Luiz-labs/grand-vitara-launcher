package com.luis.gvlauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.luis.gvlauncher.Config
import com.luis.gvlauncher.R
import java.io.File

/** Modelo de la imagen del auto que se muestra en la tarjeta principal. */
enum class CarModel(val label: String) {
    TRES_PUERTAS("3 puertas"),
    CINCO_PUERTAS("5 puertas"),
    FOTO_PROPIA("Mi foto"),
}

/**
 * Color de pintura. hue = null → color sin tono (plata, negro, blanco…).
 * "Original" deja la foto tal cual (3 puertas: blanca, 5 puertas: vino).
 * El brillo final de cada píxel de la carrocería es vBase + vGain × brillo original normalizado,
 * así se conservan los reflejos y sombras de la foto.
 */
data class CarPaint(
    val name: String,
    val hue: Float?,
    val sat: Float,
    val vBase: Float,
    val vGain: Float,
    /** Color de la muestra en la pantalla de personalización. */
    val swatch: Long,
) {
    val isOriginal get() = name == ORIGINAL

    companion object {
        const val ORIGINAL = "Original"
        val ALL = listOf(
            CarPaint(ORIGINAL, null, 0f, 0f, 0f, 0xFFE9EAEC),
            CarPaint("Azul oscuro", 222f, 0.70f, 0.06f, 0.40f, 0xFF16275E),
            CarPaint("Azul", 212f, 0.72f, 0.20f, 0.62f, 0xFF2F6FD0),
            CarPaint("Plata", null, 0.02f, 0.42f, 0.50f, 0xFFC4C7CC),
            CarPaint("Gris", null, 0.03f, 0.20f, 0.42f, 0xFF6E7277),
            CarPaint("Negro", null, 0.04f, 0.02f, 0.22f, 0xFF1A1B1E),
            CarPaint("Blanco", null, 0.01f, 0.74f, 0.26f, 0xFFF2F2F0),
            CarPaint("Rojo", 356f, 0.82f, 0.18f, 0.65f, 0xFFB81C24),
            CarPaint("Vino", 348f, 0.75f, 0.08f, 0.38f, 0xFF5E1424),
            CarPaint("Verde", 152f, 0.55f, 0.08f, 0.42f, 0xFF1F6B45),
            CarPaint("Champagne", 38f, 0.28f, 0.45f, 0.45f, 0xFFCDB991),
        )

        fun byName(name: String) = ALL.firstOrNull { it.name == name } ?: ALL.first()
    }
}

data class CarProfile(
    val driverName: String,
    val model: CarModel,
    val paint: String,
)

/** Guarda la personalización en el equipo (sobrevive reinicios de la radio). */
class ProfileStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("perfil", Context.MODE_PRIVATE)

    /** La foto propia se copia aquí, así no depende de que el archivo original siga existiendo. */
    val customFile: File get() = File(context.filesDir, "auto_propio.img")

    fun load() = CarProfile(
        driverName = prefs.getString("nombre", null) ?: Config.DRIVER_NAME,
        model = runCatching { CarModel.valueOf(prefs.getString("modelo", null)!!) }
            .getOrDefault(Config.DEFAULT_MODEL),
        paint = prefs.getString("color", null) ?: Config.DEFAULT_PAINT,
    )

    fun save(p: CarProfile) {
        prefs.edit()
            .putString("nombre", p.driverName.trim().ifBlank { Config.DRIVER_NAME })
            .putString("modelo", p.model.name)
            .putString("color", p.paint)
            .apply()
    }

    /** Copia una imagen elegida en el selector de archivos. */
    fun importCustom(uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)!!.use { input ->
            customFile.outputStream().use { input.copyTo(it) }
        }
        true
    }.getOrDefault(false)

    /** Copia una imagen desde una ruta fija (para radios sin selector de archivos). */
    fun importCustom(file: File): Boolean = runCatching {
        file.copyTo(customFile, overwrite = true)
        true
    }.getOrDefault(false)
}

object CarImages {

    /** Arma la imagen final del auto: modelo elegido + color. Llamar fuera del hilo principal. */
    fun render(context: Context, profile: CarProfile, customFile: File): Bitmap? {
        // La foto propia se muestra tal cual: el cambio de color solo aplica a las imágenes incluidas.
        if (profile.model == CarModel.FOTO_PROPIA) {
            return decodeFile(customFile) ?: decodeRes(context, R.drawable.car_3p)
        }
        val (imgId, maskId) = when (profile.model) {
            CarModel.CINCO_PUERTAS -> R.drawable.car_5p to R.drawable.car_5p_mask
            else -> R.drawable.car_3p to R.drawable.car_3p_mask
        }
        val base = decodeRes(context, imgId) ?: return null
        val paint = CarPaint.byName(profile.paint)
        if (paint.isOriginal) return base
        val mask = decodeRes(context, maskId) ?: return base
        return recolor(base, mask, paint)
    }

    private fun decodeRes(context: Context, id: Int): Bitmap? =
        BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false })

    /** Decodifica reduciendo a ~1000 px de ancho para no gastar RAM con fotos de celular. */
    private fun decodeFile(file: File): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1000) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /**
     * Repinta la carrocería usando una máscara (blanco = pintura, negro = vidrios, aros, luces, fondo).
     * Toma el brillo de la foto original y lo estira entre sus percentiles 5 y 95, así cualquier
     * color base (blanco o vino) conserva reflejos y sombras al cambiar de color.
     */
    fun recolor(src: Bitmap, maskSrc: Bitmap, paint: CarPaint): Bitmap {
        val w = src.width
        val h = src.height
        val mask = if (maskSrc.width == w && maskSrc.height == h) maskSrc
            else Bitmap.createScaledBitmap(maskSrc, w, h, true)
        val px = IntArray(w * h)
        val mk = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        mask.getPixels(mk, 0, w, 0, 0, w, h)

        val hsv = FloatArray(3)
        val values = FloatArray(px.size)
        var n = 0
        for (i in px.indices) {
            if ((mk[i] shr 16 and 0xFF) > 127 && px[i] ushr 24 != 0) {
                Color.colorToHSV(px[i], hsv)
                values[n++] = hsv[2]
            }
        }
        if (n == 0) return src
        val sorted = values.copyOf(n).also { it.sort() }
        val p5 = sorted[(n * 0.05f).toInt()]
        val p95 = sorted[(n * 0.95f).toInt().coerceAtMost(n - 1)]
        val range = (p95 - p5).coerceAtLeast(0.001f)

        for (i in px.indices) {
            val m = (mk[i] shr 16 and 0xFF) / 255f
            if (m <= 0f) continue
            val c = px[i]
            val alpha = c ushr 24
            if (alpha == 0) continue
            Color.colorToHSV(c, hsv)
            val vn = ((hsv[2] - p5) / range).coerceIn(0f, 1f)
            hsv[0] = paint.hue ?: 0f
            hsv[1] = paint.sat
            hsv[2] = (paint.vBase + paint.vGain * vn).coerceIn(0f, 1f)
            val nc = Color.HSVToColor(hsv)
            // Mezcla suave en los bordes de la máscara.
            val r = (Color.red(c) * (1 - m) + Color.red(nc) * m).toInt()
            val g = (Color.green(c) * (1 - m) + Color.green(nc) * m).toInt()
            val b = (Color.blue(c) * (1 - m) + Color.blue(nc) * m).toInt()
            px[i] = Color.argb(alpha, r, g, b)
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }
}
