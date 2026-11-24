plugins {
    alias(libs.plugins.android.application)
}

// ── Versión de la app ────────────────────────────────────────────────────────
// Para sacar una versión nueva, se cambian estos dos valores y se sube a la rama:
//   VERSION_NAME: la que ve la persona (2.4.0, 2.5.0, 3.0.0…)
//   VERSION_CODE: número entero que SIEMPRE tiene que subir (Android no instala
//                 una actualización con un versionCode menor o igual)
// Esta app ya "existía" antes de VersApp: arranca en 2.4.0 (24) y no en 1.0.0.
// En la primera build automatizada, VersApp lee estos valores del archivo.
// GitHub Actions puede pisarlos con -PversionName=… -PversionCode=…
val VERSION_NAME = "2.4.0"
val VERSION_CODE = 24
val versionNameApp = (findProperty("versionName") as String?)?.takeIf { it.isNotBlank() } ?: VERSION_NAME
val versionCodeApp = (findProperty("versionCode") as String?)?.takeIf { it.isNotBlank() }?.toInt() ?: VERSION_CODE

android {
    namespace = "com.versapp.delivery"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.versapp.delivery"
        minSdk = 26
        targetSdk = 37
        versionCode = versionCodeApp
        versionName = versionNameApp
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            // Solo para la tesis: el APK release se firma con la clave de depuración
            // que Gradle genera sola, así se puede instalar en un teléfono.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
}
