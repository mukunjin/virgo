# Virgo

安卓离线三阶魔方计时器。**全原生 Android（Kotlin + Jetpack Compose）实现，不使用 WebView**，打乱与计时逻辑参考并复刻自 csTimer（GPLv3）。**完全离线运行**，不申请 INTERNET 权限，不依赖 cstimer.net。

三阶（333）打乱算法**全复刻 csTimer**：ISAAC 随机源 + 随机态生成 + min2phase 求逆解，同种子下生成的打乱与 csTimer **逐字符一致**，并有金标准单测保障。

## 功能

- **打乱**：333 打乱生成、上一条/下一条历史、刷新、打乱展开图
- **计时**：全屏任意位置计时。长按 300ms 进入观察（15 秒倒计时，超时 +2、超过 17 秒 DNF），再长按 300ms 进入就绪，松手开跑
- **成绩**：自动落库（含打乱文本）、单次 +2 / DNF / 删除
- **分组**：新建、切换、删除分组（删除分组会连同其成绩一并删除，需弹窗确认）
- **统计**：ao5 / ao12 / 单次最好 / 单次最差 / 总平均 / 次数 / DNF
- **设置**：观察计时开关、精确到毫秒、打乱文本换行与对齐
- **显示**：全屏、屏幕常亮、纯黑背景

## 不包含

蓝牙/硬件计时器、对战、在线比赛、求解器界面、虚拟魔方、在线服务、捐赠、成绩导入导出界面。

## 界面

底部常驻滑动胶囊在 **计时 / 成绩 / 设置 / 关于** 四个整屏界面之间切换，支持点击平滑滑动与拖动跟手吸附。

配色为纯黑 + 白：背景 `#000000`、主文本与时间 `#FFFFFF`、面板深灰、危险操作红色。

## 目录结构

| 路径 | 说明 |
| --- | --- |
| `app/src/main/java/com/virgo/cubetimer/scramble/` | 333 打乱全复刻（ISAAC 随机源 + 魔方模型 + min2phase 求解内核 + 打乱组装） |
| `app/src/main/java/com/virgo/cubetimer/timer/` | 计时状态机（复刻 csTimer `timer.js`）与时间格式化 |
| `app/src/main/java/com/virgo/cubetimer/stats/` | ao5/ao12、修剪平均、分组统计（复刻 `timestat.js`） |
| `app/src/main/java/com/virgo/cubetimer/data/` | Room 数据库（分组与成绩）与设置仓库 |
| `app/src/main/java/com/virgo/cubetimer/ui/` | Compose 界面（计时页 + 成绩/设置/关于页 + 底部分段胶囊） |
| `app/src/main/java/com/virgo/cubetimer/ui/menu/` | 底部分段胶囊导航组件 |
| `app/src/main/java/com/virgo/cubetimer/ui/theme/` | 黑 + 白配色、暗色主题与计时字体 |
| `app/src/test/` | 打乱与随机源的金标准测试 |

## 版本号

版本号**唯一来源**是根目录 [gradle.properties](gradle.properties)：

```properties
virgo.versionCode=2
virgo.versionName=0.1.7
```

`app/build.gradle.kts` 直接读取这两项写入 APK；界面如需显示版本，读 `BuildConfig.VERSION_NAME` 即可与 APK 完全一致（`buildConfig` 已开启）。改版本只改这一处。

- `versionName` 是自由字符串，可写 `1.0.0` 这类三位小数，仅用于展示
- `versionCode` 必须是递增整数，用于系统判断升级

APK 产物名也跟随版本号自动生成：release 为 **`virgo-<versionName>.apk`**，debug 为 `virgo-<versionName>-debug.apk`。当前版本 `0.1.7`，产物即 `virgo-0.1.7.apk`。

“关于”界面同样读取 `BuildConfig.VERSION_NAME` 显示版本号，不会与 APK 不一致。

## 应用图标

现用图标是 `app/src/main/res/mipmap-*/ic_launcher.png`（5 档密度）。替换成自己的 PNG 有两种方式。

### 方式一：Android Studio 图形化（推荐）

`res` 右键 → **New → Image Asset** → Icon Type 选 `Launcher Icons (Adaptive and Legacy)` → Source Asset 选你的 PNG → 调整缩放与安全区 → Finish。Studio 会自动生成各密度 PNG 与自适应图标 XML。

### 方式二：手动替换

把同一张 PNG 缩放成下列尺寸，逐一覆盖同名文件（保持文件名 `ic_launcher.png`）：

| 目录 | 边长 |
| --- | --- |
| `mipmap-mdpi` | 48 px |
| `mipmap-hdpi` | 72 px |
| `mipmap-xhdpi` | 96 px |
| `mipmap-xxhdpi` | 144 px |
| `mipmap-xxxhdpi` | 192 px |

Android 8.0 及以上还会用自适应图标（圆形/方形/圆角遮罩）。若要支持，需要额外做：

1. 把 PNG 放到 `res/drawable-nodpi/ic_launcher_foreground.png`，画布 **432×432**，主体图形放在**居中 66% 的安全区内**（四周留白，避免被遮罩切掉）
2. 在 `res/values/colors.xml` 定义背景色，如 `<color name="ic_launcher_background">#000000</color>`
3. 新建 `res/mipmap-anydpi-v26/ic_launcher.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

只做方式二而不做第 1~3 步时，Android 8.0+ 会把普通 PNG 塞进圆形/方形遮罩里，可能被裁掉四角。

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

产物：`app\build\outputs\apk\release\virgo-<versionName>.apk`（当前为 `virgo-0.1.7.apk`）

release 构建复用 debug 密钥签名，因此可直接安装，无需另行配置 keystore。

调试包用 `.\gradlew.bat assembleDebug --console=plain`，产物在 `app\build\outputs\apk\debug\virgo-0.1.7-debug.apk`。

## 2. 运行单元测试

打乱与随机源的正确性由金标准测试保证（固定种子 → 与 csTimer 逐字符比对）：

```powershell
.\gradlew.bat testDebugUnitTest --console=plain
```

## 3. 安装到手机

USB 调试可用时直接安装：

```powershell
adb install -r app\build\outputs\apk\release\virgo-0.1.7.apk
```

adb 不在 PATH 时用完整路径：

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\release\virgo-0.1.7.apk
```

若报 `INSTALL_FAILED_USER_RESTRICTED`（小米等机型的「USB 安装」未开启），改为推送后在手机上手动安装：

```powershell
adb push app\build\outputs\apk\release\virgo-0.1.7.apk /sdcard/Download/
```

PATH：

```powershell
$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"
```

再在手机文件管理里打开 `Download` 目录下的 APK 安装。

## 完整流程

```powershell
.\gradlew.bat assembleRelease
adb install -r app\build\outputs\apk\release\virgo-0.1.7.apk
```

## 许可证

本项目基于 csTimer 二次开发，遵循 GPLv3。特别感谢csTimer，一个伟大的项目。