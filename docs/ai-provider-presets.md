# AI 服务商预设

核对日期：2026-10-26（Asia/Hong_Kong）。按用户要求增加九家预设及自定义入口，统一复用现有非流式 Chat Completions 请求，不新增 SDK 或原生协议客户端。

## 地址及官方依据

| 预设 | 自动填写的 Base URL | 官方文档 |
| --- | --- | --- |
| 字节火山引擎 | `https://ark.cn-beijing.volces.com/api/v3` | [生态工具接入](https://docs.volcengine.com/docs/ark/integrate-third-party-tools?lang=en) |
| 阿里云百炼 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | [地域及套餐地址](https://help.aliyun.com/en/model-studio/base-url) |
| DeepSeek | `https://api.deepseek.com` | [首次 API 调用](https://api-docs.deepseek.com/) |
| 硅基流动 | `https://api.siliconflow.cn/v1` | [Chat Completions](https://docs.siliconflow.cn/docs/api/chat-completions-post) |
| OpenRouter | `https://openrouter.ai/api/v1` | [Quickstart](https://openrouter.ai/docs/quickstart) |
| ChatGPT / OpenAI | `https://api.openai.com/v1` | [Chat Completions](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create) |
| Google Gemini | `https://generativelanguage.googleapis.com/v1beta/openai` | [OpenAI 兼容接口](https://ai.google.dev/gemini-api/docs/openai) |
| Claude | `https://api.anthropic.com/v1` | [OpenAI 兼容接口](https://platform.claude.com/docs/en/cli-sdks-libraries/libraries/openai-sdk) |
| Groq | `https://api.groq.com/openai/v1` | [OpenAI 兼容配置](https://console.groq.com/docs/openai) |

所有地址由共用客户端补齐 `/chat/completions`；用户仍可修改地址。预设不写死模型名称，用户从控制台复制可用的完整模型 ID，避免过期型号、账号未开通和自动选择收费模型。火山引擎也支持填推理接入点 ID。

火山引擎与百炼默认使用北京按量付费地址。不同地区、工作区专属域名或订阅套餐需要使用对应的地址和 Key，页面有提示。百炼共享域名仍可用，官方建议生产环境迁移工作区专属域名。

## 最小协议差异

- OpenAI 官方主机使用 `max_completion_tokens`，其他兼容主机保留 `max_tokens`。不自动提高额度上限或重试。
- Gemini 使用官方兼容路径，并按[库与平台接入说明](https://ai.google.dev/gemini-api/docs/partner-integration.md)发送 `x-goog-api-client` 客户端标识。
- Claude 增加可选工作区 ID。未限定工作区的个人/服务账号 Key 需要此值；只有目标为 `api.anthropic.com` 时发送 `anthropic-workspace-id`。
- ChatGPT / OpenAI 预设接入 OpenAI API，使用平台 API Key；不使用 ChatGPT 登录凭据或会话 Cookie。
- 火山 `deepseek-v4-1-flash-economy-261017` 及已核实的标准版 261017/260910 使用 `thinking.type=disabled`。这些模型默认思考内容占用 max_tokens，连接测试的小额度会被思考消耗；阅读任务使用官方支持的直接回答模式，不提高 token 上限。[官方深度思考说明](https://console.volcengine.com/ark/region%3Acn-beijing/docs/deep-thinking)。已在授权账号验证该模式返回 HTTP 200、stop、非空文字且无 reasoning_content；未把用户密钥写入本文件。

## 配置和密钥

- 仍管理一套当前配置，不增加多账号密钥库。
- 选择不同端点的预设，清空草稿中的旧密钥、模型和工作区 ID；同一端点规范化后相等则保留，方便已有自定义配置选择对应预设。
- 手动改变协议、域名或端口时清空草稿密钥和工作区 ID；只修改相同来源下的路径可保留。
- 选择预设或编辑字段不会发送请求或自动保存；用户点击保存/测试后才分别保存/请求。
- 新增 `providerId`、`workspaceId` 均有默认值，旧加密配置可继续读取。没有工作区 ID 时保持旧 fingerprint 算法，避免升级后重复自动请求每日金句。

## 验证

新增 AiProviderTest，扩展 AiClientTest 和 AiConfigStoreTest，覆盖全部预设路径、旧配置、密钥清理、缓存身份稳定、OpenAI 参数、Gemini 标识和 Claude 工作区头部隔离。完整测试共 108 项、0 失败，Debug 构建和 lint 通过；测试包已更新并保留数据安装到已连接手机。没有九家实际 Key，不把协议模拟测试描述为九家真实服务联调。

后续获授权完成火山指定 DeepSeek 模型的真实服务与手机联调，加入直接回答适配后共 109 项测试通过，真机显示「连接成功」。其他八家仍以官方文档核对和协议模拟测试为验证边界。
