plugins {
    id("com.android.application")
}

android {
    namespace = "com.asukanemu.mi9clicker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.asukanemu.mi9clicker"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
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
