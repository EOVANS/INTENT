package com.intent.reader.ai

import com.intent.reader.R
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Official compatible endpoints, verified in docs/ai-provider-presets.md. */
enum class AiProvider(val id: String, val label: Int, val baseUrl: String, val hint: Int = R.string.ai_provider_hint) {
    CUSTOM("custom", R.string.ai_provider_custom, ""),
    VOLCENGINE("volcengine", R.string.ai_provider_volcengine, "https://ark.cn-beijing.volces.com/api/v3", R.string.ai_provider_volcengine_hint),
    BAILIAN("bailian", R.string.ai_provider_bailian, "https://dashscope.aliyuncs.com/compatible-mode/v1", R.string.ai_provider_bailian_hint),
    DEEPSEEK("deepseek", R.string.ai_provider_deepseek, "https://api.deepseek.com"),
    SILICONFLOW("siliconflow", R.string.ai_provider_siliconflow, "https://api.siliconflow.cn/v1"),
    OPENROUTER("openrouter", R.string.ai_provider_openrouter, "https://openrouter.ai/api/v1"),
    OPENAI("openai", R.string.ai_provider_openai, "https://api.openai.com/v1", R.string.ai_provider_openai_hint),
    GEMINI("gemini", R.string.ai_provider_gemini, "https://generativelanguage.googleapis.com/v1beta/openai"),
    CLAUDE("claude", R.string.ai_provider_claude, "https://api.anthropic.com/v1", R.string.ai_provider_claude_hint),
    GROQ("groq", R.string.ai_provider_groq, "https://api.groq.com/openai/v1");

    companion object {
        fun fromId(id: String): AiProvider = entries.firstOrNull { it.id == id } ?: CUSTOM
    }
}

fun AiConfig.selectProvider(provider: AiProvider): AiConfig {
    if (provider == AiProvider.CUSTOM) return copy(providerId = provider.id)
    val sameEndpoint = runCatching { endpoint() == copy(baseUrl = provider.baseUrl).endpoint() }.getOrDefault(false)
    return copy(
        providerId = provider.id, baseUrl = provider.baseUrl,
        model = if (sameEndpoint) model else "",
        apiKey = if (sameEndpoint) apiKey else "",
        workspaceId = if (sameEndpoint) workspaceId else "",
    )
}

fun AiConfig.editBaseUrl(value: String): AiConfig {
    val old = baseUrl.trim().toHttpUrlOrNull()
    val new = value.trim().toHttpUrlOrNull()
    val sameOrigin = old != null && new != null && old.scheme == new.scheme && old.host == new.host && old.port == new.port
    return copy(baseUrl = value, providerId = AiProvider.CUSTOM.id,
        apiKey = if (sameOrigin) apiKey else "", workspaceId = if (sameOrigin) workspaceId else "")
}
