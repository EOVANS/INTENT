# AGENTS.md — INTENT 阅读器

给后续 AI/协作者的工作说明。先读完本文件再动手。

## 项目概况

- Android Kotlin + Jetpack Compose（Material3），单模块 `app`，包名 `com.intent.reader`。
- 当前版本：`versionName = "v1.6"`，`versionCode = 12`（`app/build.gradle.kts`）。
- minSdk 30 / targetSdk 36 / compileSdk 36。
- 主要代码：`app/src/main/java/com/intent/reader/`，UI 在 `ui/`（`ui/settings`、`ui/reader`、`ui/home`…），手动 DI 在 `di/AppContainer`，数据层 `data/`、`notes/`、`rss/`。
- 仓库根目录即本文件所在目录；git 仓库也在这一层（不是上层 `Android APP Build`）。

## 构建环境（必须照此设置，机器已离线配置）

```powershell
$env:JAVA_HOME="D:\APP Data Core\Android APP Build\.tools\jdk\jdk-17.0.20.1+1"
$env:GRADLE_USER_HOME="D:\APP Data Core\Android APP Build\.tools\gradle-user-home"
& "D:\APP Data Core\Android APP Build\.tools\gradle\gradle-8.11.1\bin\gradle.bat" <task> --offline
```

- **必须加 `--offline`**，网络依赖不可用；Gradle 与 JDK 都在 `.tools/` 下，不要用系统里的。
- 没有全局 `java`/`gradle`，单独跑 `apksigner`、`keytool` 等工具前要先设 `JAVA_HOME`。
- 常用任务：
  - 调试包：`:app:assembleDebug`
  - 正式包：`:app:assembleRelease`（R8 混淆 + 资源压缩）
  - 单元测试：`:app:testDebugUnitTest`

## 安装与调试

```powershell
& "D:\androidSDK\platform-tools\adb.exe" devices
& "D:\androidSDK\platform-tools\adb.exe" -s <serial> install -r <apk>
& "D:\androidSDK\platform-tools\adb.exe" -s <serial> shell am start -W -n com.intent.reader/.MainActivity
```

- adb 固定用 `D:\androidSDK\platform-tools\adb.exe`。
- 截图：`shell screencap -p /sdcard/x.png` 再 `pull`；注意截图原始像素与模型看到的缩放比例不一致，点按坐标要以 `wm size` 的真实像素为准。
- 本机曾用设备：平板 `661b3aaf`（OPPO Pad Air，1200×2000，宽屏 Medium，用来验证分栏/宽屏布局）、手机 `3B15A7010G300000`（验证手机端整页布局）。设备会变，先 `adb devices` 确认。
- ColorOS 上 `wm size` / `settings put system accelerometer_rotation` 会被 SecurityException 拒绝，无法用命令强行切宽窄屏。

## 签名与发版（重要）

- release 证书：`keystore/intent-release.jks`（PKCS12、RSA 2048、10 年有效），别名 `intent`。
- 密码与路径在 `keystore.properties`（仓库根），**`keystore.properties` 和 `keystore/` 已被 `.gitignore` 排除**。不要把密码写进任何会被提交的文件、文档或 commit message。
- `app/build.gradle.kts` 会自动读取 `keystore.properties` 生成 `signingConfigs.release`；文件不存在时 release 构建不会失败，只是产出未签名包。
- **证书一次生成、永久复用**。不要重新生成：换了证书就无法覆盖升级，老用户只能卸载重装。只有证书或密码彻底丢失才考虑更换（且等于换应用身份）。
- 发新版固定流程：
  1. 递增 `versionCode`（12 → 13），更新 `versionName`；
  2. `:app:assembleRelease`；
  3. 用 `apksigner verify --print-certs` 确认签名，`aapt dump badging` 确认版本。
- 验签/取版本：

```powershell
& "D:\androidSDK\build-tools\35.0.0\apksigner.bat" verify --print-certs <apk>
& "D:\androidSDK\build-tools\35.0.0\aapt.exe" dump badging <apk> | Select-String "package:"
```

- 已知证书指纹（SHA-256）：`ba25155d2da2829d9ef9e6d93a1fc32d449fabdd7ea0f610a73c71855a65fd7d`。如果 `apksigner` 打印出别的值，说明签名配置读错了，先排查再交付。
- debug 包与 release 包签名不同，**不能互相覆盖安装**；从 debug 切到 release 需要先卸载（会清空本地数据），操作前先问用户。

## 架构与约定

- 路由集中在 `ui/IntentNavHost.kt` 的 `Routes` 对象，页面统一用 `framedComposable` 注册；新增页面要同时加常量、路由体和入口。
- 依赖注入是手写的 `AppContainer`（`IntentApplication.container`）：`database`、`settingsRepository`、`sourceRepository`、`aiConfigStore`、`aiRepository`、`readingTracker`、`quoteRepository`、`notesRepository`。新增依赖在这里挂。
- 设置项统一走 `SettingsViewModel` → `SettingsRepository`（DataStore）。
- 宽窄屏适配：用 `WindowWidthSizeClass`。`Compact` = 手机，走整页导航；`Medium`/`Expanded` 走宽屏布局（底部导航分离 `GlassTabBar(split = …)`、设置页分栏 `SettingsSplit.kt`）。
- 分栏里的设置页用 `LocalSettingsPaneEmbedded` 隐藏返回箭头；左栏切换分类是「替换」而不是叠加。
- 文案必须四语言齐全：`values`（中）、`values-en`、`values-ja`、`values-ko`。新增字符串不要只加中文。
- 数据库是 Room v3，迁移在 `AppDatabase`（`MIGRATION_1_2`、`MIGRATION_2_3`）；改表结构必须加迁移，并同步 `ReadingTrackerTest` 的 `addMigrations`。
- R8 注意事项见 `docs/memory-and-r8.md`，反射/序列化相关改动要验证 release 包。

## 提交约定

- 用户要求每次改动都提交并汇报 commit 号。提交身份固定（不改 git config）：

```powershell
git -c user.name="Jayden" -c user.email="jayden@users.noreply.github.com" commit -m "type(scope): summary"
```

- 提交信息风格沿用历史：`feat(settings): …`、`fix(privacy): …`、`chore(release): …`、`build(release): …`。
- 只提交本次任务相关文件；临时截图/构建产物不要入库。仓库已忽略 `build/`、`.gradle/`、`keystore*`。
- 不要主动创建文档或 README，除非用户明确要求（本文件即为用户要求）。

## 历史文档

`docs/` 下有历次设计记录：`ai-phase-one.md`、`ai-provider-presets.md`、`island-notes.md`、`island-notes-handwriting.md`、`memory-and-r8.md`、`shared-view-transition.md`、`tab-bar-validation.md`、`widget-artwork-fix.md`、`widget-link-and-greeting.md`。改相关模块前先看对应文档。
