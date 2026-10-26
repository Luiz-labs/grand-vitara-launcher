package com.luis.gvlauncher.data

import com.luis.gvlauncher.Config
import com.luis.gvlauncher.Shortcut

/** App de música por defecto (botón Música, tarjeta de música y fuente principal). */
enum class MusicApp(val label: String, val shortcut: Shortcut) {
    APPLE_MUSIC("Apple Music", Config.APPLE_MUSIC),
    SPOTIFY("Spotify", Config.SPOTIFY),
    YOUTUBE_MUSIC("YouTube Music", Config.YOUTUBE_MUSIC),
}

/** App de navegación. Apple Maps no existe para Android: se usa a través de CarPlay. */
enum class NavApp(val label: String, val shortcut: Shortcut) {
    WAZE("Waze", Config.WAZE),
    GOOGLE_MAPS("Google Maps", Config.GOOGLE_MAPS),
}
