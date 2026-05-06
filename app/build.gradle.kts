plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.proyecto_definitivo"
    // Actualizado a 36 para cumplir con los requisitos de las librerías de AndroidX y Navigation
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.proyecto_definitivo"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Requerido por el Navigation SDK para soportar Java 8+ features en dispositivos antiguos
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Esta sección previene errores de "Duplicate files" durante el empaquetado del SDK
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Resolver conflicto de namespace de Cronet (múltiples librerías usan org.chromium.net)
configurations.all {
    exclude(group = "org.chromium.net", module = "cronet-api")
    exclude(group = "org.chromium.net", module = "cronet-common")
}

dependencies {
    // Soporte para Java 8+ features (desugaring) con NIO flavor requerido por Navigation SDK
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation("com.google.firebase:firebase-storage:21.0.1")

    // Google Maps Navigation SDK
    implementation(libs.navigation.sdk)

    // Servicios de ubicación actualizados
    implementation("com.google.android.gms:play-services-location:21.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}