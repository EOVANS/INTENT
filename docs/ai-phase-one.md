# INTENT v1.2：AI 第一阶段开发记录

更新日期：2026-10-26（Asia/Hong_Kong）。这份文件用于跨会话继续工作；每完成一项实现或验证后更新状态，不依赖聊天历史。

## 授权与范围

用户要求先改善未启用 BYOK 时的离线金句，并把版本升级为 1.2（含关于页）；随后授权开始第一阶段：BYOK、AI 速读、AI 金句。强调复用现有代码、控制规模。

第一阶段不包含翻译、笔记、月报、聊天和思维导图。不引入 AI SDK、Agent 框架、向量数据库或后台自动 AI 请求。

## 约定的最小实现

- BYOK：一个 OpenAI 兼容 Chat Completions 接口；用户填写 HTTPS 服务地址、模型和 API Key；支持保存、清除和连接测试。Key 通过 Android Keystore 加密，AI 配置和结果均排除云备份。连接测试也会消耗少量额度。
- 所有 AI 请求直接发往用户配置的服务，不经过 INTENT 后端；默认关闭。设置中说明发送范围和计费，启用后才可请求。
- AI 速读：阅读页右上角入口，中文摘要最多 100 个可见字符；仅使用 App 已获取的正文/摘要，范围不足或过长时明确提示，不静默截断；按文章内容、服务和模型缓存，可手动重新生成。
- AI 金句：独立开关；仅首页处于 RESUMED 时提取，每日每配置最多自动尝试一次，离开首页会取消请求，失败仍显示离线结果，提供手动重试。最多三篇正文、总计 24,000 字符，不截断候选文章；返回文章 ID 和完整原句，本地验证出处和句子边界后才能则的原句优先，其次摘要，最后标题；保留标点，HTML 段落不拼接，过滤广告/问句等；当天旧标题缓存可被新摘录显示，保留来源入口。成功时展示每日一条 AI 摘录，其余情况继续离线轮播。
- 离线金句：正文符合规替换。
- 版本：versionName=1.2，versionCode=4；关于页沿用 BuildConfig.VERSION_NAME。

## 结构与复用

- 现有：Compose、OkHttp、kotlinx.serialization、Room、DataStore、手动依赖注入。
- 新增集中在 ai/（配置、安全存储、客户端、文章分析和文件缓存）及一个 AI 设置页。
- 在 ReaderViewModel/ReaderScreen 接入速读；HomeViewModel 接入每日 AI 金句，复用 DailyQuote 展示，不把 AI 放进 RSS 同步 Worker。
- 本地缓存保存到非备份目录；不修改 Room schema，不重复保存正文。
- 当前工作区有大量已有未提交修改，必须保留；不要 reset、清理或覆盖无关文件。

## 状态

- [x] 阅读项目，确认金句只使用标题/摘要且标题加分。
- [x] 实现离线正文优先、保留标点、段落隔离及旧标题缓存刷新。
- [x] 版本修改为 1.2 / 4，确认关于页动态读取版本。
- [x] 增加金句引擎与仓库回归测试。
- [x] 离线金句测试通过（首轮完整测试与 Debug 构建已通过）。
- [x] BYOK 安全配置与统一客户端。
- [x] AI 设置页与导航，四种界面语言的新增文案。
- [x] AI 速读与缓存，限制100个Unicode码点，过长文章不发送。
- [x] AI 金句与每日调用限制，正文原句和句子边界校验、首页生命周期约束。
- [x] 更新 README 和工作区用户手册的操作说明与隐私表述。
- [x] 自动化测试、Debug 构建、Android lint、隐私说明更新。

## 验证与交接

项目目录：`D:\APP Data Core\Android APP Build\INTENT`。

本地工具：工作区 `.tools/jdk/jdk-17.0.20.1+1`、`.tools/gradle/gradle-8.11.1/bin/gradle.bat`；Gradle 用户目录应使用 `C:\Users\23069\.gradle`（这里已有依赖；`.tools/gradle-user-home` 缺少 AGP），Android SDK `D:\androidSDK`。沙箱内无法连接 daemon，已通过沙箱外工具执行获准的本机构建。

测试使用已有 `.tmp/bugfix-test.init.gradle` 指向 Maven Central 的 Robolectric 依赖仓库。优先离线运行，避免无必要下载。设置 JAVA_HOME、GRADLE_USER_HOME 后执行 `gradle -p INTENT --offline -I <初始化脚本绝对路径> :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain`；注意 `-I` 相对路径会按项目目录解析。

最终验证（2026-10-26）：完整 `testDebugUnitTest` 共 97 项，0 失败、0 错误；`assembleDebug` 和 `lintDebug` 均通过。首次 lint 发现已有设置文案缺少 13 项日语/韩语翻译，已补齐，没有禁用检查。仍有项目既存 lint 警告，未扩大到依赖升级等无关工作。

新增 AiClientTest、AiConfigStoreTest、AiRepositoryTest 验证协议、AES-GCM 加密往返、缓存、费用限制、取消、拒绝伪造/拼接/截去句内条件的摘录。加密往返测试注入测试 AES Key；真实 Android Keystore 提供方需随真机验收确认，不把测试替身描述为硬件安全验证。

构建产物：`app/build/outputs/apk/debug/app-debug.apk`（可安装的 Debug 测试包）；打包元数据确认为 versionName=1.2、versionCode=4。关于页从同一 BuildConfig 读取版本。工作区根目录提供便于查找的 `INTENT-1.2.apk` 副本。

用户随后告知手机已连接，已通过 `adb install -r` 将实体手机上的 1.1.2（versionCode=3）保留数据升级到 1.2（versionCode=4），并确认应用启动成功、进程运行。未填写或调用真实 API 配置。尚待配置后的真实服务和界面验收：

1. 设置 → AI配置，输入地址/模型/Key，测试连接并保存。未启用时进入速读应引导到设置。
2. 打开有正文的文章，点击 AI 速读，核对正文/仅摘要提示、100 字上限、再次打开复用缓存、手动重新生成及关闭弹层期间不重复调用。
3. 单独开启 AI 金句并保存，返回首页，确认原文摘录和来源入口；重进首页不自动重复付费，离开首页取消在途自动请求。
4. 验证错误密钥、额度不足、断网等真实服务响应；切换配置或关闭开关后不再沿用旧配置请求。

首版边界：只支持普通非流式文本 Chat Completions，摘要固定简体中文。超长文章不自动分块，纯摘要源不作为 AI 金句正文候选。不支持原生 Gemini/Anthropic 协议、推理参数适配、流式输出或远程计费查询。

后续新增九家预设，Gemini、Claude 使用官方兼容接口，无须引入原生协议客户端。地址、地区差异和密钥切换规则见 [AI 服务商预设](ai-provider-presets.md)。

## 设置入口调整（2026-10-26）

按用户要求，设置页提供独立的「AI配置」入口，页面标题使用相同名称。页面分为服务配置（总开关、地址、模型、密钥）、功能管理（AI 金句开关）、数据管理（结果缓存、配置与密钥清除）。保存和连接测试按钮放在功能管理上方，保存仍会包括功能开关。此调整未新增 AI 能力或改变请求逻辑。

调整后 `assembleDebug`、`lintDebug` 均通过，四种语言资源无重名，已更新工作区根目录 `INTENT-1.2.apk`。此次仅调整界面和文案，未重复运行上一轮已通过的 97 项业务测试。

用户密钥不得写入项目、开发文档或测试夹具。只记录不含凭据的联调结果，区分模拟协议测试与获授权的实际服务验证。

## 服务商预设（2026-10-26）

已实现字节火山引擎、阿里云百炼、DeepSeek、硅基流动、OpenRouter、ChatGPT / OpenAI、Google Gemini、Claude、Groq 和自定义选择。预设只填写地址，模型及 Key 由用户输入。补充 Claude 工作区 ID、OpenAI token 参数、Gemini 客户端标识；切换端点清除草稿旧密钥。未改变现有 AI 的开启状态或自动请求策略。

新增预设后的完整单元测试共 108 项，0 失败、0 错误；`assembleDebug`、`lintDebug` 均通过。已更新根目录 `INTENT-1.2.apk`，并保留数据安装到此前连接的同一台实体手机，手机报告 versionName=1.2、versionCode=4。本轮未手动发起真实服务测试，九家实际账号/Key 的可用性仍需用户在配置页验证。

## 火山真机配置与直接回答适配（2026-10-26）

用户授权在已连接手机填写字节火山引擎的 Key 与 `deepseek-v4-1-flash-economy-261017` 模型，且明确提供了模型 ID。手机配置已显示保存成功，AI 总开关开启，自动金句开关保持关闭。不得把示例 Seedance 视频任务或 Responses 联网搜索请求当作用户授权执行的工作。

原 App 的 16-token 连接测试返回“不完整”提示；授权文字接口探测返回 HTTP 200 和有效文字。官方文档确认该模型支持关闭思考，且思考也占 max_tokens。关闭思考后的同额度探测返回 stop、非空正文、0 思考字符、1 输出 token。客户端仅为火山该组已核实的 Flash 模型附加 `thinking.type=disabled`，其余模型/厂商不变；没有提升请求额度。

修复后共 109 项测试、0 失败，`assembleDebug`、`lintDebug` 均通过。已更新根目录测试包并保留数据安装手机。真机冷启动后确认预设、模型、非空隐藏密钥和开关均成功读回，App 内再次测试明确显示「连接成功」。AI 总开关已开启；每日自动金句仍保持关闭。真机加密存储读写路径已实际使用，不等同于硬件安全能力认证；未把用户文章用于本轮联调。
