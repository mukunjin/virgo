plugins {
    id("com.android.application") version "9.4.1" apply false
    // AGP 9 内置 Kotlin（版本 2.4.20），Compose 编译器插件需与 Kotlin 版本一致
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    // KSP 2.3.12 是当前最新版；此处为单次兼容性尝试
    id("com.google.devtools.ksp") version "2.3.12" apply false
}