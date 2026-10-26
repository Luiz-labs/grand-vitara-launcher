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
    const val DRIVER_NAME = "Luis"
    const val CAR_NAME = "Grand Vitara"

    /** Ciudades para el clima (toca el clima para cambiar). "Automático" usa el GPS. */
    val CITIES = listOf(
        City("Lima", -12.0464, -77.0428),
        City("Lurín", -12.2747, -76.8708),
        City("Miraflores", -12.1211, -77.0297),
        City("Callao", -12.0566, -77.1181),
    )

    val WAZE = Shortcut(
        packages = listOf("com.waze"),
        labels = listOf("waze"),
    )
    /** Respaldo si Waze no está instalado. */
    val NAVIGATION_FALLBACK = Shortcut(
        packages = listOf("com.google.android.apps.maps"),
        labels = listOf("maps", "mapas", "navi"),
    )

    val PHONE = Shortcut(
        packages = listOf("com.android.bluetooth.phone", "com.syu.bt", "com.ts.bt", "com.jancar.bluetooth"),
        labels = listOf("teléfono", "telefono", "bluetooth", "phone", "bt"),
    )
    val RADIO = Shortcut(
        packages = listOf("com.android.fmradio", "com.syu.radio", "com.ts.radio", "com.jancar.radio"),
        labels = listOf("radio", "fm"),
    )
    val MUSIC = Shortcut(
        packages = listOf("com.spotify.music", "com.google.android.apps.youtube.music"),
        labels = listOf("música", "musica", "music"),
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

data class Shortcut(val packages: List<String>, val labels: List<String>)
