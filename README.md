# Virgo

基于 csTimer（GPLv3）的安卓离线魔方计时器。**全原生 Android（Kotlin + Jetpack Compose）实现，不使用 WebView**，**完全离线运行**：不申请 INTERNET 权限，不依赖 cstimer.net。

当前仅支持三阶魔方（333）的打乱与计时。三阶打乱算法**全复刻 csTimer**：随机态生成 + min2phase 求逆解，同种子下与 csTimer 生成的打乱逐字符一致，并有金标准单测保障。

不包含：蓝牙/硬件计时器、对战、在线比赛、求解器界面、虚拟魔方、其他题型打乱。

## 目录结构

| 路径 | 说明 |
| --- | --- |
| `app/src/main/java/com/virgo/cubetimer/scramble/` | 333 打乱全复刻（ISAAC 随机源 + 魔方模型 + min2phase 求解内核 + 打乱组装） |
| `app/src/main/java/com/virgo/cubetimer/timer/` | 计时状态机（复刻 csTimer `timer.js`）与时间格式化 |
| `app/src/main/java/com/virgo/cubetimer/stats/` | ao5/ao12、修剪平均、会话统计（复刻 `timestat.js`） |
| `app/src/main/java/com/virgo/cubetimer/data/` | Room 数据库（会话与成绩）与设置仓库 |
| `app/src/main/java/com/virgo/cubetimer/ui/` | Compose 界面（左侧图标列 + 浮动窗格 + 大字 LCD + 打乱区） |
| `app/src/test/` | 打乱与随机源的金标准测试 |

配色为白 + 灰：背景 `#FFFFFF`、次级表面 `#F5F5F5`、边框 `#E0E0E0`、主文本 `#212121`、次要文本 `#757575`。

## 环境要求

- Windows + PowerShell 5.1 及以上
- Android Studio（自带 JDK：`C:\Program Files\Android\Android Studio\jbr`）
- Android SDK（`platform-tools` 提供 adb）

## 1. 编译 APK

`gradlew` 启动时需要 `JAVA_HOME`（`gradle.properties` 里的 `org.gradle.java.home` 只作用于 Gradle 守护进程，无法替代它）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleRelease --console=plain
```

产物：`app\build\outputs\apk\release\app-release.apk`

release 构建复用 debug 密钥签名，因此可直接安装，无需另行配置 keystore。

调试包用 `.\gradlew.bat assembleDebug --console=plain`，产物在 `app\build\outputs\apk\debug\app-debug.apk`。

## 2. 运行单元测试

打乱与随机源的正确性由金标准测试保证（固定种子 → 与 csTimer 逐字符比对）：

```powershell
.\gradlew.bat testDebugUnitTest --console=plain
```

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
.\gradlew.bat assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

## 许可证

本项目基于 csTimer 二次开发，遵循 GPLv3。