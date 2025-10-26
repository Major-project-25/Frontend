plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.testapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.testapplication"
        minSdk = 29
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.navigation:navigation-compose:2.9.3")

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.compose.material:material-icons-extended:1.7.8")

// This is the core library for Compose's UI elements

    implementation("androidx.compose.ui:ui")



// This provides the Material Design 3 components we are using

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")



// This is needed for @Preview to work

    implementation("androidx.compose.ui:ui-tooling-preview")



// This integrates Compose with Android Activities

    implementation("androidx.activity:activity-compose")

    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    //implementation("com.squareup.okhttp3:okhttp-logging-interceptor:4.12.0")

    // In your dependencies block

// Ktor for WebSockets
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-client-cio:2.3.11") // Engine for the client
    implementation("io.ktor:ktor-client-websockets:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-gson:2.3.11")
    //implementation("com.squareup.okhttp3:okhttp-logging-interceptor:4.12.0")
}