plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.cinelist"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.cinelist"
        minSdk = 24
        targetSdk = 34
        versionCode = 8
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Assinatura automatizada (lê variáveis locais ou as injetadas pelo GitHub Actions)
    signingConfigs {
        create("release") {
            val keystoreFile = file("cinelist-key.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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

    kotlinOptions {
        jvmTarget = "17"

        testOptions {
            unitTests.all {
                it.jvmArgs("-XX:+EnableDynamicAgentLoading")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true // Habilita BuildConfig.VERSION_CODE e VERSION_NAME para o UpdateManager
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core e Ciclo de Vida
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Google Play Services Auth
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Room Database via KSP
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Palette API
    implementation(libs.androidx.palette)

    // Dagger Hilt (Injeção de Dependência via KSP)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Retrofit + Conversor Gson para APIs
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Coil (Imagens assíncronas)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Jetpack Compose Framework
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Paginação Reativa (Paging 3)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Splash Screen Oficial
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Gráficos
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Testes Unitários e Instrumentados
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}