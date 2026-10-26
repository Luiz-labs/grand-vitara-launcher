package com.luis.gvlauncher.data

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.Settings

/**
 * Brillo, volumen, WiFi y Bluetooth para la pantalla Ajustes.
 * Ojo: en algunas radios chinas el brillo y el volumen los maneja el MCU y no Android;
 * en ese caso estos controles pueden no tener efecto y conviene usar los botones físicos.
 */
class SystemControls(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    // ---- Brillo (0..1) ----
    fun canWriteSettings() = Settings.System.canWrite(context)

    fun brightness(): Float = runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    }.getOrDefault(0.7f)

    fun setBrightness(value: Float): Boolean {
        if (!canWriteSettings()) return false
        return runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS,
                (value.coerceIn(0.05f, 1f) * 255).toInt())
        }.getOrDefault(false)
    }

    /** Abre la pantalla de Android para dar permiso de "modificar ajustes del sistema". */
    fun requestWriteSettings() = runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---- Volumen multimedia (0..1) ----
    fun volume(): Float = runCatching {
        audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }.getOrDefault(0.5f)

    fun setVolume(value: Float) = runCatching {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (value.coerceIn(0f, 1f) * max).toInt(), 0)
    }

    // ---- WiFi ----
    fun wifiOn() = runCatching { wifi?.isWifiEnabled == true }.getOrDefault(false)

    @Suppress("DEPRECATION") // targetSdk 28: todavía permitido en Android 8/9
    fun setWifi(on: Boolean) = runCatching { wifi?.isWifiEnabled = on }

    fun wifiName(): String? = runCatching {
        @Suppress("DEPRECATION")
        wifi?.connectionInfo?.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    }.getOrNull()

    // ---- Bluetooth ----
    @Suppress("DEPRECATION", "MissingPermission")
    fun bluetoothOn() = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)

    @Suppress("DEPRECATION", "MissingPermission")
    fun setBluetooth(on: Boolean) = runCatching {
        val bt = BluetoothAdapter.getDefaultAdapter() ?: return@runCatching
        if (on) bt.enable() else bt.disable()
    }
}
