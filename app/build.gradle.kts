plugins {
    // AGP 9.0 起内置 Kotlin 支持，无需再声明 org.jetbrains.kotlin.android
    id("com.android.application")
    // Compose 编译器插件（版本与内置 Kotlin 一致）
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// 版本号唯一来源：根目录 gradle.properties 的 virgo.versionName / virgo.versionCode
val virgoVersionName: String = providers.gradleProperty("virgo.versionName").get()
val virgoVersionCode: Int = providers.gradleProperty("virgo.versionCode").get().toInt()

android {
    namespace = "com.virgo.cubetimer"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.virgo.cubetimer"
        minSdk = 24
        targetSdk = 37
        // 界面如需显示版本，请使用 BuildConfig.VERSION_NAME（buildFeatures.buildConfig 已开启）
        versionCode = virgoVersionCode
        versionName = virgoVersionName
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 不单独配置签名：复用 debug 密钥，直接可安装
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        // 生成 BuildConfig，使界面可读取与 APK 一致的 versionName
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    // 产物命名为 virgo-<版本号>.apk（debug 包追加 -debug），版本号与 APK 内一致（同一处配置）
    onVariants { variant ->
        val suffix = if (variant.buildType == "release") "" else "-${variant.buildType}"
        variant.outputs.forEach { output ->
            output.outputFileName.set("virgo-$virgoVersionName$suffix.apk")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    testImplementation("junit:junit:4.13.2")
}