plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/*
 * Release signing. Values live in ~/.gradle/gradle.properties (outside the repo); the keystore is
 * ~/.android/compacttrip-release.jks. Never change or lose it: an app signed with a different key
 * can't update over the old one, and uninstalling wipes the user's saved API key and trips.
 */
val releaseStoreFile = providers.gradleProperty("COMPACTTRIP_STORE_FILE").orNull
val releaseSigningAvailable = releaseStoreFile != null

android {
    namespace = "com.mryav.compacttrip"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mryav.compacttrip"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = providers.gradleProperty("COMPACTTRIP_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("COMPACTTRIP_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("COMPACTTRIP_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningAvailable) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Fail loudly instead of quietly producing an unsigned (or wrongly signed) release APK.
tasks.matching { it.name == "packageRelease" }.configureEach {
    val available = releaseSigningAvailable
    doFirst {
        if (!available) {
            throw GradleException(
                "Release signing not configured: add COMPACTTRIP_STORE_FILE, COMPACTTRIP_STORE_PASSWORD, " +
                    "COMPACTTRIP_KEY_ALIAS and COMPACTTRIP_KEY_PASSWORD to ~/.gradle/gradle.properties.",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
