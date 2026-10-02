plugins {
    // AGP 9.0 起内置 Kotlin 支持，无需再声明 org.jetbrains.kotlin.android
    id("com.android.application")
}

android {
    namespace = "com.virgo.cubetimer"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.virgo.cubetimer"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 不单独配置签名：复用 debug 密钥，直接可安装
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.17.1")
}