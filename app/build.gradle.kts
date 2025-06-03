import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}

android {
    namespace = "com.example.appescapetocinema"
    compileSdk = 35

    buildFeatures {
        buildConfig = true // Habilita BuildConfig
        compose = true
    }

    defaultConfig {
        applicationId = "com.example.appescapetocinema"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { input ->
                localProperties.load(input)
            }
        }
        //buildConfigField("String", "TMDB_API_KEY", "\"${localProperties.getProperty("TMDB_API_KEY", "")}\"")
        buildConfigField("String", "MOVIEGLU_API_KEY", "\"${localProperties.getProperty("MOVIEGLU_API_KEY", "")}\"")
        buildConfigField("String", "MOVIEGLU_CLIENT", "\"${localProperties.getProperty("MOVIEGLU_CLIENT", "")}\"")
        buildConfigField("String", "MOVIEGLU_AUTHORIZATION_HEADER", "\"${localProperties.getProperty("MOVIEGLU_AUTHORIZATION_HEADER", "")}\"")
        buildConfigField("String", "MOVIEGLU_TERRITORY_PASS_KEY", "\"${localProperties.getProperty("MOVIEGLU_TERRITORY_PASS_KEY", "")}\"")
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.auth.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.compose.material:material-icons-core:1.6.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.google.firebase:firebase-firestore-ktx") // <-- AÑADE ESTA LÍNEA
    implementation("androidx.compose.material3:material3:1.2.1") // O la versión específica que uses
    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    implementation("androidx.paging:paging-compose:3.3.0")
    implementation("com.valentinilk.shimmer:compose-shimmer:1.2.0") // O versión más reciente
    implementation("com.prof18.rssparser:rssparser:6.0.10")
    implementation("io.ktor:ktor-client-core:2.3.9")
    // Elige UN engine, OkHttp es buena opción si ya usas Retrofit/OkHttp
    implementation("io.ktor:ktor-client-okhttp:2.3.9")
    // O el engine de Android (más simple, menos configuración)
    // implementation("io.ktor:ktor-client-android:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.9") // Para manejo de contenido
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9") // Si Ktor necesita JSON (no para RSS parser)
    // En build.gradle.kts (app)
    implementation("androidx.core:core-splashscreen:1.0.1") // O versión más reciente
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0") // O la versión más reciente compatible con tu Retrofit
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1") // O la versión más reciente

}