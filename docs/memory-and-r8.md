# 内存检测与 R8 构建

## 构建方式

项目使用 AGP 8.9.2、Gradle 8.11.1 和 JDK 17。本仓库尚未提供 Gradle Wrapper，以下命令中的 `gradle` 指已安装的 Gradle 8.11.1。

```text
gradle :app:assembleDebug
gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.intent.reader.MemoryLeakTest
gradle :app:testDebugUnitTest
gradle :app:assembleRelease :app:bundleRelease
```

- `debug`：包含 LeakCanary 2.14，自动监测销毁的 Activity、清理后的 ViewModel 等。启动日志应出现 `LeakCanary is running and ready to detect leaks`。发生泄漏时从 LeakCanary 通知/入口查看引用链。
- `release`：开启 `isMinifyEnabled`、`isShrinkResources` 和 `proguard-android-optimize.txt`，不包含 LeakCanary、堆分析器及 Compose 调试工具。AGP 8.9.2 默认启用 R8 full mode。
- `app/build/outputs/apk/release/app-release-unsigned.apk` 是未签名的发布产物，正式分发需使用发行密钥签名。不能将 debug APK 当作 R8 优化结果。
- 每次发布保留对应的 `app/build/outputs/mapping/release/mapping.txt`，用于还原混淆后的崩溃堆栈；同目录的 `configuration.txt`、`usage.txt`、`seeds.txt`、`resources.txt` 可用于审查规则和裁剪结果。

未加入 `android.r8.optimizedResourceShrinking`：该新管线要求 AGP 8.12+，当前项目使用 AGP 8.9.2 的资源裁剪。没有为本次任务扩大到构建工具链升级。

## 本次修复

1. 无障碍设置页原先在创建 TextToSpeech 后延迟 400 ms，随后才将实例交给清理逻辑。延迟期间离开页面或切换引擎会取消协程，导致新建实例没有调用 `shutdown()`。现改用以引擎为键的 `DisposableEffect`，直接持有并清理每个实例。
2. 阅读页与设置页的 TTS 使用 application context，避免服务连接持有 Activity。阅读页退出时解除进度监听、停止并关闭引擎、移除待执行回调；已退出或被替换的引擎不能再更新界面状态。
3. 删除重复的 Room 保留规则和覆盖整个应用的 Companion/serializer 保留规则，使用依赖自带的 consumer rules。WorkManager、Glance、Room、kotlinx.serialization 和 sharedview-kit 的必要规则由 AGP 合并。没有使用全应用 `-keep` 或关闭优化。
4. 真机 LeakCanary 首轮检测捕获 `SharedTransitionObserver → SnapshotStateObserver → SharedTransitionScopeImpl → LayoutNode → MainActivity` 泄漏链。Compose BOM 从 2024.12.01 升至 2025.06.01（Compose 1.8.3、Material3 1.3.2）；已核对发布源码，1.8.3 在 scope 销毁时设置 `disposed` 并阻止后续观察注册，避免销毁过程重新订阅。未添加泄漏忽略规则。

## 设备回归

`MemoryLeakTest` 使用 LeakCanary 官方的 instrumentation 集成，在 Activity/页面释放后调用 `LeakAssertions.assertNoLeaks()`。覆盖 Activity 重建/关闭，以及设置、阅读页面快速进入退出。测试不会主动修改订阅、笔记或设置。

自动测试期间 LeakCanary 的普通后台堆转储由其测试集成接管；以测试结果为准。通过代表这些测试路径未发现应用泄漏，不代表所有交互均无泄漏。

本次 API 37 真机上，`ActivityScenario.recreate()` 的中间 EmptyActivity 流程发生停顿；重建测试改为直接请求 `Activity.recreate()` 并限时等待生命周期回调，仍检查真实重建后的对象释放。

手工回归还应覆盖：真实文章连续阅读和返回、朗读播放/停止、切换 TTS 引擎、网页进入退出、切换语言/主题、应用前后台，以及桌面小组件点击。每组操作结束后等待对象监测与堆分析完成。

## 验证记录

2026-11-06，在 OPPO PLJ110 / API 37 真机验证：

- Debug、AndroidTest、Release APK、Release AAB、未压缩正式配置对照包均构建成功；release lint 检查通过。
- 126 项单元测试通过，0 失败；3 项真机 LeakCanary 测试全部通过，耗时 29.75 秒。覆盖阅读页与无障碍页各 5 次快速进入退出，以及 Activity 重建 3 次再关闭。
- 首轮捕获的共享转场泄漏在升级后未再出现在这些测试中。没有将它配置为忽略项。
- Release 依赖清单、混淆映射中均无 LeakCanary / Shark / Plumber；最终安装包不带 DEBUGGABLE 标志，签名验证通过，安装后正常启动、无当前进程崩溃记录。
- 测试保留了手机上的原有应用数据，并清理本次生成的 53 MB 堆转储。手机最终安装的是 R8 优化包。

### 体积（签名后，十进制 MB）

| 包 | 字节 | MB | DEX 文件数 | DEX 总字节 |
| --- | ---: | ---: | ---: | ---: |
| 原分发调试包 INTENT-v1.5.apk | 67,503,557 | 67.504 | 16 | 65,919,044 |
| 同源码、同依赖正式配置，关闭 R8/资源裁剪 | 50,626,415 | 50.626 | 3 | 49,050,196 |
| 最终 R8 优化包 INTENT-v1.5-R8.apk | 5,384,442 | 5.384 | 1 | 4,095,732 |

相对同配置对照包减少 **89.36%**，DEX 字节减少 **91.65%**；相对原分发调试包减少 **92.02%**。后一个比例还包含正式版移除调试工具等差异，不能全部归因于 R8。

可用下列命令重建同源码对照包（默认发布配置不会被更改）：

```text
gradle -I tools/r8-size-baseline.init.gradle :app:assembleSizeBaseline :app:assembleRelease
```

未签名 release APK 为 5,291,727 字节；release AAB 为 6,537,625 字节。本次可安装 APK 沿用原安装包的本地 Android Debug 证书签名，便于覆盖安装测试；正式发行仍应使用受管理的发行密钥。签名方式不会重新启用 DEBUGGABLE 或加入 LeakCanary。

最终 APK SHA-256：`17e235884cb2422570b2b888716146d787665e2b2834556bd45a3c0d9dd5b9a0`。

### 启动抽样

同一手机、相同数据，使用 `adb shell am start -S -W -n com.intent.reader/.MainActivity`，两种包各舍弃首次启动，记录之后 5 次均标记为 COLD 的 `TotalTime`：

- 未压缩正式配置：181、175、162、188、179 ms；中位数 **179 ms**。
- R8 优化：157、154、153、168、155 ms；中位数 **155 ms**，本次抽样降低约 **13.4%**。

这是单台设备的小样本启动检查，不是 Macrobenchmark，也不代表完整内容加载、所有操作或所有机型的性能提升。尚未对全部阅读、网页、小组件与第三方转场路径做完整端到端回归。

## 官方依据

- [LeakCanary 接入](https://square.github.io/leakcanary/getting_started/)
- [LeakCanary instrumentation 检测](https://square.github.io/leakcanary/ui-tests/)
- [Android R8 优化与资源裁剪](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)
