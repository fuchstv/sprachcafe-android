import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.sprachcafe.team"
    compileSdk = 34

    val localProperties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                this@apply.load(stream)
            }
        }
    }
    val teamApiKey = (project.findProperty("TEAM_API_KEY") as? String)
        ?: localProperties.getProperty("TEAM_API_KEY")
        ?: (System.getenv("TEAM_API_KEY") ?: "")

    defaultConfig {
        applicationId = "org.sprachcafe.team"
        minSdk = 24
        targetSdk = 34
        versionCode = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 2
        versionName = "1.0.${System.getenv("BUILD_NUMBER") ?: "1"}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "TEAM_API_KEY", "\"$teamApiKey\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "sprachcafe2026"
            keyAlias = "sprachcafe-key"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "sprachcafe2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Google ML Kit Barcode Scanning (100% on-device, 0 cost)
    implementation(libs.mlkit.barcode.scanning)

    // Google Play In-App Updates
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

android {
    testOptions {
        unitTests.all {
            it.systemProperty("robolectric.dependency.repo.id", "gspot")
            it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
        }
    }
}
