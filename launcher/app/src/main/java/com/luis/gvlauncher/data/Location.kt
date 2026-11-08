package com.luis.gvlauncher.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

data class GeoFix(
    val lat: Double,
    val lon: Double,
    /** Degrees clockwise from north, or null when stopped / unknown. */
    val bearing: Float?,
    val speedKmh: Int?,
)

/** Live GPS position for the navigation card (1 s updates while the home screen is visible). */
class LocationRepository(private val context: Context) {

    private val _fix = MutableStateFlow<GeoFix?>(null)
    val fix: StateFlow<GeoFix?> = _fix

    private val manager = context.getSystemService(LocationManager::class.java)

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = publish(location)
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted || manager == null) return
        runCatching {
            // La última posición guardada puede ser vieja: se usa solo para ubicarse, con velocidad 0.
            manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { publish(it, stale = true) }
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, listener, Looper.getMainLooper())
        }
    }

    fun stop() {
        runCatching { manager?.removeUpdates(listener) }
    }

    /** Lecturas seguidas por encima del umbral para arrancar (evita saltos sueltos del GPS). */
    private var risingCount = 0

    private fun publish(l: Location, stale: Boolean = false) {
        val shown = if (stale) 0 else filteredSpeed(l)
        val moving = shown > 0
        _fix.value = GeoFix(
            lat = l.latitude,
            lon = l.longitude,
            bearing = if (moving && l.hasBearing()) l.bearing else _fix.value?.bearing,
            speedKmh = if (stale || l.hasSpeed()) shown else null,
        )
    }

    /**
     * Velocidad "limpia" para mostrar. Con el auto parado el GPS se mueve unos metros y
     * marca 1–5 km/h falsos, pero al arrancar tiene que reaccionar rápido:
     *  - sale de 0 con UNA lectura de [START_FAST_KMH] o más, o con dos seguidas de [START_KMH] o más,
     *  - vuelve a 0 por debajo de [STOP_KMH],
     *  - la precisión mala (bajo techo, cochera) solo se tiene en cuenta con el auto casi parado.
     */
    private fun filteredSpeed(l: Location): Int {
        if (!l.hasSpeed()) return 0
        val raw = l.speed * 3.6f
        val badFix = l.hasAccuracy() && l.accuracy > BAD_ACCURACY_M
        val current = _fix.value?.speedKmh ?: 0
        if (badFix && raw < 10f) { risingCount = 0; return 0 }
        if (current == 0) {
            if (raw >= START_FAST_KMH) { risingCount = 0; return raw.roundToInt() }
            if (raw >= START_KMH) risingCount++ else risingCount = 0
            return if (risingCount >= 2) { risingCount = 0; raw.roundToInt() } else 0
        }
        if (raw < STOP_KMH) { risingCount = 0; return 0 }
        return raw.roundToInt()
    }

    private companion object {
        const val START_FAST_KMH = 8f
        const val START_KMH = 4f
        const val STOP_KMH = 3f
        const val BAD_ACCURACY_M = 25f
    }
}
