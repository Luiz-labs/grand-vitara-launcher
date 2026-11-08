# Reglas para la versión final (R8).
# Compose, coroutines y zxing ya traen sus propias reglas.
# El servicio de notificaciones y la actividad principal se mantienen porque están en el Manifest.

# Mantener nombres para que los errores se puedan leer si algún día hay que revisar un fallo
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
