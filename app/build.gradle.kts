plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.vicoror.appandroidfinal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vicoror.appandroidfinal"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.gson)
    // Dagger Hilt para DI
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Para ViewModel con Hilt
    implementation(libs.androidx.hilt.navigation.compose)

    // Network monitoring
    implementation(libs.androidx.lifecycle.service)
    // Coroutines para Flow
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jetbrains.kotlinx.coroutines.core)

    // Lifecycle para ViewModelScope y lifecycleScope
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.common.java8)

    // Para los Flows
    implementation(libs.androidx.lifecycle.livedata.ktx)
    // ✅ RETROFIT PARA CONSUMO DE API
    implementation(libs.retrofit)
    implementation(libs.retrofit2.converter.gson)

    // ✅ OKHTTP PARA LOGGING Y CONFIGURACIÓN
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

