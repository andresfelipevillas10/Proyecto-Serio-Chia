plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.ksp)
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
    // 1. EL JEFE: Firebase BOM (Gestiona todas las versiones de Firebase automáticamente)
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    // 2. FIREBASE: Sin versiones manuales, el BOM se encarga
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // 3. EL PUENTE: Para que el .await() funcione con Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // 4. MAPS & NAV: Tus piezas de Google Maps
    implementation(libs.navigation.sdk)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // 5. ROOM: El Búnker local
    val roomVersion = "2.7.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // 6. WORKMANAGER: El Rayo de sincronización
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // 7. CICLO DE VIDA: Para usar el lifecycleScope
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // 8. CAMERAX: Para el Step 6
    val cameraXVersion = "1.3.3"
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")

    // 9. CORE & UI: Lo básico de Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.2")

    // TESTING
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}