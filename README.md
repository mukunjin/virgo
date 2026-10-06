# Virgo

English | [简体中文](README.zh-CN.md)

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="120" alt="Virgo logo">
</p>

An offline 3x3x3 Rubik's Cube timer for Android. **Fully native Android (Kotlin + Jetpack Compose), no WebView**; the scramble and timer logic are referenced from and reimplemented after csTimer (GPLv3). It **runs completely offline** — no INTERNET permission is requested and it does not depend on cstimer.net.

The 3x3x3 (333) scramble algorithm is a **full reimplementation of csTimer's**: ISAAC random source + random-state generation + min2phase inverse solution. With the same seed, the generated scrambles match csTimer **character for character**, guarded by golden-standard unit tests.

## Features

- **Scramble**: 333 scramble generation, previous/next history, refresh, scramble expansion diagram
- **Timer**: tap anywhere on the full screen to time. Long-press 300 ms to enter inspection (15 s countdown; over 15 s gives +2, over 17 s gives DNF), long-press 300 ms again to get ready, release to start
- **Solves**: automatically stored (including the scramble text), per-solve +2 / DNF / delete
- **Groups**: create, switch, delete groups (deleting a group also deletes its solves, with a confirmation dialog)
- **Statistics**: ao5 / ao12 / best / worst / mean / count / DNF
- **Settings**: inspection toggle, millisecond precision, scramble text wrap and alignment
- **Display**: full screen, keep screen on, pure black background

## Not Included

Bluetooth/hardware timers, versus mode, online competitions, solver UI, virtual cube, online services, donations, solve import/export UI.

## UI

A persistent sliding capsule at the bottom switches between four full-screen pages: **Timer / Stats / Settings / About**, supporting a smooth slide on tap and drag-to-snap tracking the finger.

The color scheme is pure black + white: background `#000000`, primary text and time `#FFFFFF`, panels dark gray, destructive actions red.

## Project Structure

| Path | Description |
| --- | --- |
| `app/src/main/java/com/virgo/cubetimer/scramble/` | Full 333 scramble reimplementation (ISAAC random source + cube model + min2phase solver core + scramble assembly) |
| `app/src/main/java/com/virgo/cubetimer/timer/` | Timer state machine (reimplemented from csTimer `timer.js`) and time formatting |
| `app/src/main/java/com/virgo/cubetimer/stats/` | ao5/ao12, trimmed average, group statistics (reimplemented from `timestat.js`) |
| `app/src/main/java/com/virgo/cubetimer/data/` | Room database (groups and solves) and settings repository |
| `app/src/main/java/com/virgo/cubetimer/ui/` | Compose UI (timer page + stats/settings/about pages + bottom segmented capsule) |
| `app/src/main/java/com/virgo/cubetimer/ui/menu/` | Bottom segmented capsule navigation component |
| `app/src/main/java/com/virgo/cubetimer/ui/theme/` | Black + white palette, dark theme and timer font |
| `app/src/test/` | Golden-standard tests for the scramble and random source |

## Version

The **single source of truth** for the version is the root [gradle.properties](gradle.properties):

```properties
virgo.versionCode=2
virgo.versionName=0.1.7
```

`app/build.gradle.kts` reads these two values directly and writes them into the APK; if the UI needs to display the version, read `BuildConfig.VERSION_NAME` to stay exactly consistent with the APK (`buildConfig` is enabled). To change the version, change only this place.

- `versionName` is a free-form string, e.g. `1.0.0`; used for display only
- `versionCode` must be an increasing integer, used by the system to judge upgrades

The APK artifact name also follows the version automatically: for release it is **`virgo-<versionName>.apk`**, for debug `virgo-<versionName>-debug.apk`. The current version is `0.1.7`, so the artifact is `virgo-0.1.7.apk`.

The "About" screen likewise reads `BuildConfig.VERSION_NAME`, so it never disagrees with the APK.

## App Icon

The current icon is `app/src/main/res/mipmap-*/ic_launcher.png` (5 densities). There are two ways to replace it with your own PNG.

### Option 1: Android Studio GUI (recommended)

Right-click `res` → **New → Image Asset** → set Icon Type to `Launcher Icons (Adaptive and Legacy)` → choose your PNG as Source Asset → adjust scaling and safe zone → Finish. Studio generates the density PNGs and the adaptive icon XML automatically.

### Option 2: Manual replacement

Scale the same PNG to the sizes below and overwrite the same-named files (keep the name `ic_launcher.png`):

| Directory | Size |
| --- | --- |
| `mipmap-mdpi` | 48 px |
| `mipmap-hdpi` | 72 px |
| `mipmap-xhdpi` | 96 px |
| `mipmap-xxhdpi` | 144 px |
| `mipmap-xxxhdpi` | 192 px |

Android 8.0 and above also uses adaptive icons (circle/square/rounded masks). To support that, additionally do the following:

1. Put the PNG at `res/drawable-nodpi/ic_launcher_foreground.png`, canvas **432x432**, with the main graphic inside the **centered 66% safe zone** (leave margins so the mask does not clip it)
2. Define a background color in `res/values/colors.xml`, e.g. `<color name="ic_launcher_background">#000000</color>`
3. Create `res/mipmap-anydpi-v26/ic_launcher.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

If you only do Option 2 without steps 1-3, Android 8.0+ will put the plain PNG into a circular/square mask and may clip the corners.

## Requirements

- Windows + PowerShell 5.1 or newer
- Android Studio (bundled JDK: `C:\Program Files\Android\Android Studio\jbr`)
- Android SDK (`platform-tools` provides adb)

## 1. Build the APK

`gradlew` needs `JAVA_HOME` at startup (`org.gradle.java.home` in `gradle.properties` only affects the Gradle daemon and cannot replace it):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleRelease --console=plain
```

Artifact: `app\build\outputs\apk\release\virgo-<versionName>.apk` (currently `virgo-0.1.7.apk`)

The release build reuses the debug signing key, so it can be installed directly without configuring a separate keystore.

For a debug build use `.\gradlew.bat assembleDebug --console=plain`; the artifact is `app\build\outputs\apk\debug\virgo-0.1.7-debug.apk`.

## 2. Run Unit Tests

Scramble and random source correctness is guaranteed by golden-standard tests (fixed seed → character-for-character comparison with csTimer):

```powershell
.\gradlew.bat testDebugUnitTest --console=plain
```

## 3. Install on a Phone

When USB debugging is available, install directly:

```powershell
adb install -r app\build\outputs\apk\release\virgo-0.1.7.apk
```

If adb is not on PATH, use the full path:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\release\virgo-0.1.7.apk
```

If you get `INSTALL_FAILED_USER_RESTRICTED` (e.g. Xiaomi with "Install via USB" disabled), push and install manually on the phone:

```powershell
adb push app\build\outputs\apk\release\virgo-0.1.7.apk /sdcard/Download/
```

PATH:

```powershell
$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"
```

Then open the APK under `Download` in the phone's file manager and install it.

## Full Workflow

```powershell
.\gradlew.bat assembleRelease
adb install -r app\build\outputs\apk\release\virgo-0.1.7.apk
```

## License

This project is a derivative work based on csTimer and is licensed under GPLv3. Special thanks to csTimer, a great project.