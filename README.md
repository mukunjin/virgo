# Virgo

基于 csTimer（GPLv3）的安卓离线魔方计时器。原生 Android（Kotlin）+ 系统 WebView 承载 csTimer 前端，**完全离线运行**：不申请 INTERNET 权限，不依赖 cstimer.net，不包含蓝牙与对战功能。

## 目录结构

| 路径 | 说明 |
| --- | --- |
| `web-src/` | csTimer 前端源码（本项目裁剪版，仅保留简体中文与离线功能） |
| `tools/build-web.ps1` | 把 `web-src/` 生成为 `app/src/main/assets/www` |
| `app/` | Android 工程（Kotlin + WebView + WebViewAssetLoader） |

## 环境要求

- Windows + PowerShell 5.1 及以上
- Android Studio（自带 JDK：`C:\Program Files\Android\Android Studio\jbr`）
- Android SDK（`platform-tools` 提供 adb）

## 1. 生成网页资源

修改 `web-src/` 后必须先执行这一步，否则改动不会进入 APK。产物会先清空再整体重新生成：

```powershell
powershell -ExecutionPolicy Bypass -File tools\build-web.ps1
```

输出目录：`app/src/main/assets/www`（预期 100 个文件、约 1.8 MB）。

## 2. 编译 APK

`gradlew` 启动时需要 `JAVA_HOME`（`gradle.properties` 里的 `org.gradle.java.home` 只作用于 Gradle 守护进程，无法替代它）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleRelease --console=plain
```

产物：`app\build\outputs\apk\release\app-release.apk`

release 构建复用 debug 密钥签名，因此可直接安装，无需另行配置 keystore。

调试包用 `.\gradlew.bat assembleDebug --console=plain`，产物在 `app\build\outputs\apk\debug\app-debug.apk`。

## 3. 安装到手机

USB 调试可用时直接安装：

```powershell
adb install -r app\build\outputs\apk\release\app-release.apk
```

adb 不在 PATH 时用完整路径：

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\release\app-release.apk
```

若报 `INSTALL_FAILED_USER_RESTRICTED`（小米等机型的「USB 安装」未开启），改为推送后在手机上手动安装：

```powershell
adb push app\build\outputs\apk\release\app-release.apk /sdcard/Download/
```
PATH：
```powershell
$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"
```

再在手机文件管理里打开 `Download` 目录下的 APK 安装。

## 完整流程

```powershell
powershell -ExecutionPolicy Bypass -File tools\build-web.ps1
.\gradlew.bat assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

## 许可证

本项目基于 csTimer 二次开发，遵循 GPLv3。