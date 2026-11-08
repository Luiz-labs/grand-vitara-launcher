# Grand Vitara Launcher

Launcher propio para la radio Android china (Allwinner T3, firmware K2001N, 1 GB RAM, 9")
de la Suzuki Grand Vitara 2009.
Sin mapa offline (se usa Waze) para ahorrar RAM.

## Qué tiene
- Saludo, hora, fecha y clima de Lima (toca el clima para cambiar de ciudad).
- Tarjeta de **Waze** con botones directos a **Casa** y **Trabajo** (los que tengas guardados en Waze) y velocidad por GPS.
- Control de la música que esté sonando (Bluetooth, Spotify, etc.).
- Accesos a Teléfono, Radio FM, Ecualizador y Apple CarPlay (para el dongle Autokit).
- Cajón de apps y acceso a Ajustes en la barra lateral.

## Compilar en la Mac (una sola vez)
1. Instala Android Studio: `brew install --cask android-studio`
2. Ábrelo una vez y deja que instale el SDK (asistente "Standard").
3. En Android Studio: **Open** → carpeta `launcher`. Espera a que termine el "Gradle sync".
4. Compila desde Terminal:
   ```bash
   cd launcher
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew assembleDebug
   ```
   APK: `app/build/outputs/apk/debug/app-debug.apk`

## Instalar en la radio
Desde la carpeta raíz del proyecto, con la Mac y la radio en la misma WiFi:
```bash
bash scripts/listar_apps.sh 192.168.X.X   # inventario de apps (para Config.kt y debloat)
bash scripts/instalar.sh 192.168.X.X      # instala y lo deja como inicio
bash scripts/revertir.sh 192.168.X.X      # vuelve al launcher original
```

## Personalizar
- `app/src/main/java/com/luis/gvlauncher/Config.kt`: nombre, ciudades del clima y apps de cada acceso.
  Cada acceso busca primero por **paquete** y, si no lo encuentra, por **nombre visible** de la app.
- `app/src/main/res/drawable-nodpi/car_3p.png` y `car_5p.png`: imágenes base del auto (carrocería azul, así funciona el cambio de color).
- El nombre, el modelo y el color se cambian desde la app: toca el ícono de ajustes de la tarjeta principal.
