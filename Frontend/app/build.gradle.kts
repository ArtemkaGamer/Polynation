plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.polynation"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.polynation"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Travelpayouts (Aviasales) — токен и партнёрский marker берутся из gradle.properties.
        // Получить бесплатно: https://www.travelpayouts.com/  (см. инструкцию в коде).
        buildConfigField("String", "TRAVELPAYOUTS_TOKEN",
            "\"${project.findProperty("TRAVELPAYOUTS_TOKEN") ?: ""}\"")
        buildConfigField("String", "TRAVELPAYOUTS_MARKER",
            "\"${project.findProperty("TRAVELPAYOUTS_MARKER") ?: ""}\"")
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // MVVM: ViewModel + LiveData (AndroidX Lifecycle)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.17")
    implementation("androidx.cardview:cardview:1.0.0")

    // Retrofit для API запросов
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp для логирования запросов
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

}