#!/usr/bin/env bash
# Instala el Grand Vitara Launcher en la radio y lo deja como pantalla de inicio.
# Uso (Terminal de la Mac, desde la carpeta del proyecto):
#   bash scripts/instalar.sh 192.168.X.X
set -euo pipefail

IP="${1:?Falta la IP de la radio. Ej: bash scripts/instalar.sh 192.168.43.120}"
D="$IP:5555"
APK="launcher/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.luis.gvlauncher"

[ -f "$APK" ] || { echo "No encuentro $APK. Primero compila: cd launcher && ./gradlew assembleDebug"; exit 1; }

adb connect "$D" >/dev/null
adb -s "$D" get-state >/dev/null || { echo "Radio no conectada en $D"; exit 1; }

echo "Instalando..."
adb -s "$D" install -r "$APK"

echo "Dando permisos (música, ubicación para clima y velocidad)..."
adb -s "$D" shell cmd notification allow_listener "$PKG/$PKG.data.MediaListenerService" || true
adb -s "$D" shell pm grant "$PKG" android.permission.ACCESS_FINE_LOCATION || true
adb -s "$D" shell pm grant "$PKG" android.permission.ACCESS_COARSE_LOCATION || true
adb -s "$D" shell pm grant "$PKG" android.permission.CALL_PHONE || true
# Brillo desde la pantalla Ajustes del launcher
adb -s "$D" shell appops set "$PKG" WRITE_SETTINGS allow || true

echo "Poniéndolo como launcher predeterminado..."
adb -s "$D" shell cmd package set-home-activity "$PKG/.MainActivity"
adb -s "$D" shell input keyevent KEYCODE_HOME

echo "Listo. Para volver al launcher original: bash scripts/revertir.sh $IP"
