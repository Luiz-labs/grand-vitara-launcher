#!/usr/bin/env bash
# Saca la lista de apps de la radio con su nombre visible, para completar Config.kt
# y decidir el debloat. Genera apps_radio.txt.
# Uso: bash scripts/listar_apps.sh 192.168.X.X
set -euo pipefail
IP="${1:?Falta la IP de la radio}"
D="$IP:5555"
OUT="apps_radio.txt"
adb connect "$D" >/dev/null
{
  echo "== INFO =="
  echo "Android: $(adb -s "$D" shell getprop ro.build.version.release)"
  echo "Plataforma: $(adb -s "$D" shell getprop ro.board.platform)"
  echo "Pantalla: $(adb -s "$D" shell wm size)"
  adb -s "$D" shell cat /proc/meminfo | head -2
  echo
  echo "== APPS CON ICONO (paquete -> actividad) =="
  adb -s "$D" shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
    | grep -E "packageName=|name=" | sed 's/^ *//' | paste - - || true
  echo
  echo "== TODOS LOS PAQUETES =="
  adb -s "$D" shell pm list packages -f | sed 's/package://' | sort -t= -k2
  echo
  echo "== APPS DE TERCEROS =="
  adb -s "$D" shell pm list packages -3 | sed 's/package://' | sort
} > "$OUT"
echo "Listo: $OUT ($(wc -l < "$OUT") líneas). Pégamelo o adjúntalo."
