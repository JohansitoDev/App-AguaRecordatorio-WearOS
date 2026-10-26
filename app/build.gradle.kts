plugins {
    id("com.android.application")
}

android {
    namespace = "com.aguardatorio.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aguardatorio.app"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
