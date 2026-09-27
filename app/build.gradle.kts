plugins {
    id("com.android.application")
}

android {
    namespace = "dk.femto.albumframe"
    compileSdk = 35

    defaultConfig {
        applicationId = "dk.femto.albumframe"
        minSdk = 28
        targetSdk = 35
        versionCode = 14
        versionName = "0.8.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
