# Día de instalación — Grand Vitara Launcher

Checklist para instalar el launcher en la radio (Allwinner T3, 1 GB, firmware K2001N).
Todos los comandos se corren en la Terminal de la Mac, desde la carpeta del proyecto:

```bash
cd ~/Proyectos/grand-vitara-launcher
```

---

## 1. Antes de ir al auto (en casa, con la Mac)

- [ ] **Último build en verde**: en GitHub → pestaña **Actions**, el run *"Pantallas propias…"* debe tener ✓.
      Si sale ✗, enviar las últimas líneas del paso **Compilar** para corregirlo antes.
- [ ] **Descargar el APK**: entrar al run verde → **Artifacts** → `grand-vitara-launcher-apk`.
- [ ] **Dejar el APK donde lo busca el script**:
  ```bash
  mkdir -p launcher/app/build/outputs/apk/debug && \
  unzip -o ~/Downloads/grand-vitara-launcher-apk.zip -d launcher/app/build/outputs/apk/debug/ && \
  ls launcher/app/build/outputs/apk/debug/
  ```
  Debe aparecer `app-debug.apk`.
- [ ] **Verificar herramientas**:
  ```bash
  adb version
  ./tools/scrcpy-macos-x86_64-v5.0.1/scrcpy --version
  ```
- [ ] **Mac cargada** y, por si acaso, una memoria USB en FAT32 con el APK copiado (plan B).

## 2. En el auto, en la radio

- [ ] Auto **en contacto o encendido** (para no bajar la batería).
- [ ] **Fotos de respaldo** de los ajustes de fábrica (Ajustes → Los ajustes de fábrica, clave 8888, 3368 o 1234):
      cámara de retroceso, botones del timón, canbus / tipo de auto, región de la radio FM.
- [ ] **Opciones de desarrollador**: Ajustes → Información → tocar 7 veces "Número de compilación".
- [ ] Activar **Depuración USB** (y "Depuración inalámbrica" si aparece).
- [ ] Conectar la radio al **hotspot del celular** y anotar su **IP** (Ajustes → WiFi → la red).
- [ ] Revisar en la radio si hay **Play Store** y si están **Waze** y **Apple Music** (si no, se instalan después).

## 3. En el auto, con la Mac (conectada al mismo hotspot)

Reemplaza `IP` por la IP de la radio.

- [ ] **Conectar**:
  ```bash
  adb connect IP:5555 && adb devices
  ```
  Si la radio muestra un aviso de "permitir depuración", aceptarlo.
- [ ] **Inventario de la radio** (genera `apps_radio.txt`):
  ```bash
  bash scripts/listar_apps.sh IP
  adb -s IP:5555 shell cat /proc/swaps
  ```
  Enviar `apps_radio.txt` y el resultado de `/proc/swaps` (zram) a Claude.
- [ ] **Debloat**: correr el script que prepara Claude con la lista real (protege cámara, timón, radio y Bluetooth).
- [ ] **Reiniciar la radio** y probar que funcionen: cámara de retroceso, botones del timón, radio FM, Bluetooth.
- [ ] **Instalar el launcher**:
  ```bash
  adb connect IP:5555
  bash scripts/instalar.sh IP
  ```
- [ ] Si la radio pregunta qué pantalla de inicio usar: **Grand Vitara Launcher → Siempre**.

## 4. Configurar el launcher

- [ ] **Personalizar** (ícono de ajustes en la tarjeta del auto): nombre **Luis**, modelo **3 puertas**,
      color **Azul oscuro**, tema **Automático**.
- [ ] **Ajustes** (engranaje): música **Apple Music**, navegación **Waze**. Si el brillo dice
      "Toca para dar permiso", tocarlo y activar el permiso.
- [ ] **Navegación**: agregar destinos favoritos. Revisar que Casa y Trabajo estén guardados dentro de Waze.
- [ ] **Teléfono**: agregar contactos de marcación rápida y probar una llamada con el celular conectado por Bluetooth.

## 5. Pruebas

- [ ] Cada botón del inicio y de la barra lateral abre lo correcto (Teléfono, Radio FM, Ecualizador, Música, Ajustes del auto).
- [ ] La tarjeta de música muestra la canción con Apple Music / Spotify y **con música por Bluetooth**.
- [ ] El **clima** carga (necesita internet) y la **velocidad** aparece al moverse (GPS).
- [ ] Brillo, volumen, WiFi y Bluetooth desde Ajustes tienen efecto (en algunas radios los maneja el MCU).
- [ ] Modo claro / oscuro y cambio de color del auto.
- [ ] Fluidez general: anotar si algo se siente lento.

Anotar lo que **no** funcione para corregirlo en `Config.kt` (GitHub compila un APK nuevo solo).

## 6. Capturas reales para el README

```bash
adb -s IP:5555 exec-out screencap -p > diseno/real-inicio.png
```
Repetir en cada pantalla cambiando el nombre del archivo (`real-navegacion.png`, `real-musica.png`, …).
Para ver y manejar la radio desde la Mac:
```bash
./tools/scrcpy-macos-x86_64-v5.0.1/scrcpy -s IP:5555
```

## Si algo sale mal

```bash
bash scripts/revertir.sh IP     # quita el launcher y vuelve al original
```
Las apps quitadas con el debloat se restauran con:
```bash
adb -s IP:5555 shell cmd package install-existing NOMBRE.DEL.PAQUETE
```

## Después (otro día)

- [ ] Comprar el **dongle Carlinkit para radios Android** (versión con cable, va más fluido con 1 GB).
- [ ] Instalar **Autokit**, conectar el iPhone y probar CarPlay (ahí van Apple Maps y Apple Music completos).
