package com.luis.gvlauncher.data

import com.luis.gvlauncher.Config
import com.luis.gvlauncher.Shortcut

/**
 * App de música por defecto (botón Música y tarjeta de música).
 * Si la elegida no está instalada (ej. Apple Music en una radio con Android 7),
 * se abre la siguiente que sí exista: YouTube Music, Spotify y por último el reproductor de la radio.
 */
enum class MusicApp(val label: String, private val own: Shortcut) {
    APPLE_MUSIC("Apple Music", Config.APPLE_MUSIC),
    SPOTIFY("Spotify", Config.SPOTIFY),
    YOUTUBE_MUSIC("YouTube Music", Config.YOUTUBE_MUSIC);

    val shortcut: Shortcut
        get() = Shortcut(
            packages = own.packages + Config.YOUTUBE_MUSIC.packages + Config.SPOTIFY.packages + Config.USB_MUSIC.packages,
            labels = own.labels,
        )
}

/**
 * App de navegación. Apple Maps no existe para Android: se usa a través de CarPlay.
 * Si Waze no está instalado, se abre Google Maps (y al revés).
 */
enum class NavApp(val label: String, private val own: Shortcut, private val fallback: Shortcut) {
    WAZE("Waze", Config.WAZE, Config.GOOGLE_MAPS),
    GOOGLE_MAPS("Google Maps", Config.GOOGLE_MAPS, Config.WAZE);

    val shortcut: Shortcut
        get() = Shortcut(packages = own.packages + fallback.packages, labels = own.labels + fallback.labels)
}
