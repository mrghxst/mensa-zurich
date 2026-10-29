plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// Shared public ZFV key (ANDROID_PORT_PROMPT.md §5.3), read in the root build.
val zfvApiKey: String by rootProject.extra
val appVersionCode: Int by rootProject.extra
val appVersionName: String by rootProject.extra
val releaseSigning: Map<String, String> by rootProject.extra

android {
    namespace = "io.github.mrghxst.ethmensa.wear"
    compileSdk = 37

    defaultConfig {
        // Same id as the phone app, as Wear OS expects for a companion watch app.
        applicationId = "io.github.mrghxst.ethmensa"
        minSdk = 30
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "ZFV_API_KEY", "\"$zfvApiKey\"")
    }

    signingConfigs {
        if (releaseSigning.isNotEmpty()) {
            create("release") {
                storeFile = file(releaseSigning.getValue("RELEASE_STORE_FILE"))
                storePassword = releaseSigning.getValue("RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigning.getValue("RELEASE_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = false
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation3)
    implementation(libs.androidx.wear.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
