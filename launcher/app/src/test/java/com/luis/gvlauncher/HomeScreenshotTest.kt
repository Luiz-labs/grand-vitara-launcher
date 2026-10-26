package com.luis.gvlauncher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.SystemClock
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.luis.gvlauncher.data.NowPlaying
import com.luis.gvlauncher.data.Weather
import com.luis.gvlauncher.ui.AppBackground
import com.luis.gvlauncher.ui.DESIGN_H
import com.luis.gvlauncher.ui.DESIGN_W
import com.luis.gvlauncher.ui.HomeActions
import com.luis.gvlauncher.ui.HomeScreen
import com.luis.gvlauncher.ui.HomeState
import com.luis.gvlauncher.ui.LocalUnit
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenshotTest {

    private val noop = HomeActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

    private fun fakeArt(): Bitmap {
        val bmp = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, 300f, 0xFFE07AA8.toInt(), 0xFF2B1F9A.toInt(), Shader.TileMode.CLAMP)
        }
        Canvas(bmp).drawRect(0f, 0f, 300f, 300f, paint)
        return bmp
    }

    private val sample = HomeState(
        weather = Weather(temp = 17, feelsLike = 17, max = 20, min = 10, humidity = 64, windKmh = 2, code = 53,
            place = "Lima", lat = -12.0464, lon = -77.0428),
        nowPlaying = NowPlaying(
            title = "I'm Sorry", artist = "Aventura", art = fakeArt(),
            durationMs = 252_000, positionMs = 85_000, positionAt = SystemClock.elapsedRealtime(),
            playing = true, sourcePackage = "com.syu.bt",
        ),
        bluetoothOn = true,
        radioLabel = "106.3 MHz",
        cityLabel = "Lima",
    )

    private fun shot(name: String, state: HomeState) = captureRoboImage("../preview/$name.png") {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val unit = minOf(maxWidth / DESIGN_W, maxHeight / DESIGN_H)
            CompositionLocalProvider(LocalUnit provides unit) {
                AppBackground()
                HomeScreen(state, noop)
            }
        }
    }

    @Test
    @Config(qualifiers = "w1024dp-h600dp-land-mdpi", sdk = [34])
    fun home_1024x600() = shot("home_1024x600", sample)

    @Test
    @Config(qualifiers = "w1280dp-h720dp-land-mdpi", sdk = [34])
    fun home_1280x720() = shot("home_1280x720", sample)

    @Test
    @Config(qualifiers = "w1024dp-h600dp-land-mdpi", sdk = [34])
    fun home_empty_1024x600() = shot("home_empty_1024x600", HomeState())
}
