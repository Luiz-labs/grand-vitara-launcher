package com.luis.gvlauncher

/**
 * Todo lo específico de la radio y del carro vive aquí.
 *
 * Cada acceso directo tiene dos listas:
 *  - packages: se abre el primer paquete instalado.
 *  - labels:   si ningún paquete existe, se busca una app cuyo NOMBRE contenga
 *              alguna de estas palabras (sin importar mayúsculas ni tildes).
 *
 * Radio: Allwinner T3, firmware K2001N, 1 GB RAM, 9".
 * TODO: cuando saquemos la lista real por ADB, poner los paquetes exactos
 *       de radio FM, Bluetooth y ecualizador al inicio de cada lista.
 */
object Config {
    /** Valores por defecto: cada usuario los cambia desde la pantalla Personalizar (toca el ícono del auto). */
    const val DRIVER_NAME = "Conductor"
    const val CAR_NAME = "Grand Vitara"
    val DEFAULT_MODEL = com.luis.gvlauncher.data.CarModel.TRES_PUERTAS
    const val DEFAULT_PAINT = "Original"

    /** Ruta alternativa para la foto propia, para radios sin selector de archivos. */
    const val CUSTOM_PHOTO_PATH = "/sdcard/Download/auto.png"

    /** Ciudades para el clima (toca el clima para cambiar). "Automático" usa el GPS. */
    val CITIES = listOf(
        City("Lima", -12.0464, -77.0428),
        City("Lurín", -12.2747, -76.8708),
        City("Miraflores", -12.1211, -77.0297),
        City("Callao", -12.0566, -77.1181),
    )

    // ---- Navegación (se elige en Ajustes; por defecto Waze) ----
    val WAZE = Shortcut(
        packages = listOf("com.waze"),
        labels = listOf("waze"),
    )
    val GOOGLE_MAPS = Shortcut(
        packages = listOf("com.google.android.apps.maps"),
        labels = listOf("maps", "mapas"),
    )

    // ---- Música (se elige en Ajustes; por defecto Apple Music) ----
    val APPLE_MUSIC = Shortcut(
        packages = listOf("com.apple.android.music"),
        labels = listOf("apple music"),
    )
    val SPOTIFY = Shortcut(
        packages = listOf("com.spotify.music"),
        labels = listOf("spotify"),
    )
    val YOUTUBE_MUSIC = Shortcut(
        packages = listOf("com.google.android.apps.youtube.music"),
        labels = listOf("youtube music", "yt music"),
    )
    /** Música del celular por Bluetooth (app de la radio). */
    val BT_MUSIC = Shortcut(
        packages = listOf("com.syu.btmusic", "com.ts.btmusic"),
        labels = listOf("bt música", "música bt", "bt music", "música bluetooth", "bluetooth music", "a2dp"),
    )
    /** Reproductor de USB / tarjeta de la radio. */
    val USB_MUSIC = Shortcut(
        packages = listOf("com.syu.music", "com.ts.music", "com.android.music"),
        labels = listOf("usb", "música", "musica", "reproductor", "multimedia"),
        exclude = setOf("com.apple.android.music", "com.spotify.music", "com.google.android.apps.youtube.music"),
    )
    /** Ajustes de fábrica de la radio (cámara de retroceso, timón, canbus). */
    val CAR_SETTINGS = Shortcut(
        packages = listOf("com.syu.settings", "com.ts.settings"),
        labels = listOf("ajustes del auto", "ajustes del coche", "car settings", "configuración", "ajustes", "settings"),
        exclude = setOf("com.android.settings"),
    )

    val PHONE = Shortcut(
        packages = listOf("com.android.bluetooth.phone", "com.syu.bt", "com.ts.bt", "com.jancar.bluetooth"),
        labels = listOf("teléfono", "telefono", "bluetooth", "phone", "bt"),
    )
    val RADIO = Shortcut(
        packages = listOf("com.android.fmradio", "com.syu.radio", "com.ts.radio", "com.jancar.radio"),
        labels = listOf("radio", "fm"),
    )
    val EQUALIZER = Shortcut(
        packages = emptyList(),
        labels = listOf("ecualizador", "equalizer", "eq", "audio", "sonido"),
    )
    /** Para el dongle Carlinkit (Autokit) o apps de proyección que traiga la radio. */
    val CARPLAY = Shortcut(
        packages = listOf("com.carlinkit.autokit", "cn.manstep.phonemirrorBox", "com.zjinnova.zlink"),
        labels = listOf("autokit", "carplay", "zlink", "carlink", "phonelink", "autolink", "easyconnection"),
    )

    /** Fuentes de audio que significan "música del celular por Bluetooth". */
    val BLUETOOTH_MEDIA_SOURCES = setOf("com.android.bluetooth")
}

data class City(val name: String, val lat: Double, val lon: Double)

/**
 * Acceso a una app: primero por paquete exacto, luego por nombre visible.
 * [exclude]: paquetes que nunca deben coincidir por nombre (ej. no confundir "Ajustes" de Android).
 */
data class Shortcut(
    val packages: List<String>,
    val labels: List<String>,
    val exclude: Set<String> = emptySet(),
)
