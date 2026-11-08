plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.luis.gvlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.luizlabs.gvlauncher"
        minSdk = 24
        targetSdk = 28 // head units: keep legacy behaviour (no background-start / storage restrictions)
        versionCode = 7
        versionName = "0.6"
    }

    // Firma de la versión final (release). La clave NO está en el repositorio:
    //  - En GitHub sale de los secretos (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD).
    //  - En la Mac se usa la copia de ~/Documents/Claves/gvlauncher.keystore si existe.
    // Siempre es la misma clave, así cada versión se instala encima de la anterior sin perder ajustes.
    val keystorePath = System.getenv("KEYSTORE_FILE")
        ?: "${System.getProperty("user.home")}/Documents/Claves/gvlauncher.keystore"
    val hasKeystore = file(keystorePath).exists()
    signingConfigs {
        create("labs") {
            if (hasKeystore) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "gvlauncher"
                keyAlias = System.getenv("KEY_ALIAS") ?: "gvlauncher"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "gvlauncher"
            }
        }
    }

    buildTypes {
        release {
            // Versión final: código optimizado y sin recursos que no se usan (más liviana y rápida).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasKeystore) signingConfig = signingConfigs.getByName("labs")
        }
        debug {
            // Las pruebas también se firman con la misma clave (si está disponible).
            if (hasKeystore) signingConfig = signingConfigs.getByName("labs")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint { abortOnError = false; checkReleaseBuilds = false }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("roborazzi.test.record", "true")
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    // Código QR del aviso de actualización
    implementation("com.google.zxing:core:3.5.3")

    // Off-device screenshots of the home screen (./gradlew testDebugUnitTest)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.39.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.39.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
