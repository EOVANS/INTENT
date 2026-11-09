package com.intent.reader.ai

import com.intent.reader.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun interface AiCompletion {
    suspend fun complete(config: AiConfig, system: String, user: String, maxTokens: Int): String
}

/** One small compatible transport, with cancellation and no logging of keys or content. */
class AiClient(private val client: OkHttpClient) : AiCompletion {
    override suspend fun complete(config: AiConfig, system: String, user: String, maxTokens: Int): String {
        config.validate()
        val endpoint = config.endpoint()
        val body = buildJsonObject {
            put("model", config.model)
            put("stream", false)
            put(if (endpoint.host == "api.openai.com") "max_completion_tokens" else "max_tokens", maxTokens)
            // These Ark models include hidden reasoning in max_tokens. Reader
            // tasks use their supported direct-answer mode to keep the existing budget.
            if (endpoint.host == "ark.cn-beijing.volces.com" && config.model in setOf(
                    "deepseek-v4-1-flash-261017", "deepseek-v4-1-flash-260910", "deepseek-v4-1-flash-economy-261017")) {
                put("thinking", buildJsonObject { put("type", "disabled") })
            }
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                add(buildJsonObject { put("role", "user"); put("content", user) })
            })
        }
        val builder = Request.Builder().url(endpoint)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Accept", "application/json")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        if (endpoint.host == "api.anthropic.com" && config.workspaceId.isNotBlank()) {
            builder.header("anthropic-workspace-id", config.workspaceId)
        }
        if (endpoint.host == "generativelanguage.googleapis.com") {
            builder.header("x-goog-api-client", "intent-reader-oai/${BuildConfig.VERSION_NAME}")
        }
        val request = builder.build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(AiException(AiError.NETWORK))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            if (!it.isSuccessful) throw AiException(when (it.code) {
                                401, 403 -> AiError.AUTH
                                429 -> AiError.RATE_LIMIT
                                else -> AiError.SERVICE
                            })
                            val source = it.body?.source() ?: throw AiException(AiError.RESPONSE)
                            if (source.request(65_537)) throw AiException(AiError.RESPONSE)
                            val root = Json.parseToJsonElement(source.readUtf8()).jsonObject
                            val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                                ?: throw AiException(AiError.RESPONSE)
                            if (choice["finish_reason"]?.jsonPrimitive?.contentOrNull == "length") {
                                throw AiException(AiError.RESPONSE)
                            }
                            val text = choice["message"]?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                                ?.trim()?.takeIf { value -> value.isNotEmpty() } ?: throw AiException(AiError.RESPONSE)
                            if (continuation.isActive) continuation.resume(text)
                        } catch (error: Exception) {
                            val safe = error as? AiException ?: AiException(AiError.RESPONSE)
                            if (continuation.isActive) continuation.resumeWithException(safe)
                        }
                    }
                }
            })
        }
    }
}
