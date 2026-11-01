#!/usr/bin/env bash
# Quita el Grand Vitara Launcher y vuelve al launcher original de la radio.
# Uso: bash scripts/revertir.sh 192.168.X.X
set -euo pipefail
IP="${1:?Falta la IP de la radio}"
D="$IP:5555"
adb connect "$D" >/dev/null
adb -s "$D" uninstall com.luizlabs.gvlauncher || true
adb -s "$D" shell input keyevent KEYCODE_HOME
echo "Listo. Si la radio pregunta qué launcher usar, elige el original y toca 'Siempre'."
