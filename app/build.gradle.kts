plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.netraguide"
    compileSdk = 35

    aaptOptions {
        // Penting agar model .tflite tidak dikompres saat build APK
        noCompress += "tflite"
    }

    defaultConfig {
        applicationId = "com.example.netraguide"
        minSdk = 26
        // Disarankan targetSdk disamakan dengan compileSdk
        targetSdk = 34
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
        // Disarankan upgrade ke Java 17 untuk Android Studio terbaru
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            // Excludes standar untuk library ML yang sering konflik lisensi
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    // --- CameraX ---
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // --- TensorFlow Lite & GPU Delegate (Standar untuk YOLOv8) ---
    // Saya menggunakan versi hardcoded di sini untuk memastikan kompatibilitas
    // jika Anda belum mengatur libs.versions.toml dengan benar.
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // --- GPU DELEGATE (Perbaikan disini) ---
    // 1. Library Implementasi (Native Code)
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    // 2. Library API (Wajib ditambahkan agar kelas 'Options' terbaca)
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0") // GPU Delegate
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") // ImageProcessor

    // Jika ingin menggunakan metadata (opsional untuk YOLO, tapi bagus ada)
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")

    // --- Library Standar Android ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    // --- UI (Compose & Material3) ---
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}