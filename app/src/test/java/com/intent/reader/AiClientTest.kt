package com.intent.reader

import com.intent.reader.ai.AiClient
import com.intent.reader.ai.AiConfig
import com.intent.reader.ai.AiError
import com.intent.reader.ai.AiException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class AiClientTest {
    private val config = AiConfig(true, true, "https://example.com/v1/", "test-model", "test-key-not-real")

    private fun client(body: String, code: Int = 200, inspect: (okhttp3.Request) -> Unit = {}) = AiClient(
        OkHttpClient.Builder().addInterceptor { chain ->
            inspect(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Test response")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
    )

    @Test fun compatibleRequestUsesConfiguredEndpointAndReturnsContent() = runBlocking {
        val transport = client("""{"choices":[{"message":{"content":"OK"},"finish_reason":"stop"}]}""") { request ->
            assertEquals("https://example.com/v1/chat/completions", request.url.toString())
            assertEquals("Bearer test-key-not-real", request.header("Authorization"))
            val buffer = Buffer()
            request.body!!.writeTo(buffer)
            val payload = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            assertEquals("test-model", payload["model"]!!.jsonPrimitive.content)
            assertEquals("false", payload["stream"]!!.jsonPrimitive.content)
            assertEquals("system instruction", payload["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonPrimitive.content)
        }
        assertEquals("OK", transport.complete(config, "system instruction", "article", 300))
    }

    @Test fun errorResponsesAreSanitizedAndCategorized() = runBlocking {
        for ((code, expected) in listOf(401 to AiError.AUTH, 403 to AiError.AUTH, 429 to AiError.RATE_LIMIT, 500 to AiError.SERVICE)) {
            try {
                client("private provider data ${config.apiKey}", code).complete(config, "", "", 16)
                fail("Expected error")
            } catch (error: AiException) {
                assertEquals(expected, error.reason)
                assertFalse(error.toString().contains(config.apiKey))
            }
        }
    }

    @Test fun invalidTruncatedAndOversizedResponsesAreRejected() = runBlocking {
        for (response in listOf(
            "not JSON", "{}", """{"choices":[{"message":{"content":""}}]}""",
            """{"choices":[{"message":{"content":"partial"},"finish_reason":"length"}]}""",
            "x".repeat(65_537),
        )) {
            try { client(response).complete(config, "", "", 300); fail("Expected invalid response") }
            catch (error: AiException) { assertEquals(AiError.RESPONSE, error.reason) }
        }
    }

    @Test fun endpointAcceptsAFullPathWithoutDuplicatingIt() {
        assertEquals("https://example.com/v1/chat/completions", config.copy(baseUrl = "https://example.com/v1/chat/completions/").endpoint().toString())
    }

    @Test fun unsafeUrlsAreRejectedBeforeAnyNetworkRequest() {
        for (url in listOf("http://example.com/v1", "https://user:password@example.com/v1", "https://example.com/v1?key=secret", "https://example.com/v1#fragment", "invalid")) {
            try { config.copy(baseUrl = url).validate(); fail("Expected URL rejection") }
            catch (error: AiException) { assertEquals(AiError.URL, error.reason) }
        }
        assertFalse(config.toString().contains(config.apiKey))
    }

    @Test fun openAiUsesTheCurrentCompletionTokenParameter() = runBlocking {
        val transport = client("""{"choices":[{"message":{"content":"OK"}}]}""") { request ->
            val buffer = Buffer()
            request.body!!.writeTo(buffer)
            val payload = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            assertEquals("300", payload["max_completion_tokens"]!!.jsonPrimitive.content)
            assertFalse(payload.containsKey("max_tokens"))
        }
        assertEquals("OK", transport.complete(config.copy(baseUrl = "https://api.openai.com/v1"), "", "", 300))
    }

    @Test fun claudeWorkspaceHeaderIsOnlySentToAnthropic() = runBlocking {
        for (url in listOf("https://api.anthropic.com/v1", "https://example.com/v1")) {
            val transport = client("""{"choices":[{"message":{"content":"OK"}}]}""") { request ->
                assertEquals(if (request.url.host == "api.anthropic.com") "workspace-1" else null,
                    request.header("anthropic-workspace-id"))
                assertEquals("Bearer test-key-not-real", request.header("Authorization"))
            }
            transport.complete(config.copy(baseUrl = url, workspaceId = "workspace-1"), "", "", 300)
        }
    }

    @Test fun geminiUsesCompatiblePathAndClientIdentification() = runBlocking {
        val transport = client("""{"choices":[{"message":{"content":"OK"}}]}""") { request ->
            assertEquals("/v1beta/openai/chat/completions", request.url.encodedPath)
            assertTrue(request.header("x-goog-api-client")!!.startsWith("intent-reader-oai/"))
        }
        assertEquals("OK", transport.complete(config.copy(baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai"), "", "", 300))
    }

    @Test fun invalidCredentialHeaderValuesAreRejectedBeforeSending() {
        for (value in listOf(config.copy(apiKey = "key\ninjected"), config.copy(workspaceId = "workspace\ninjected"))) {
            try { value.validate(); fail("Expected invalid credential") }
            catch (error: AiException) { assertEquals(AiError.CONFIG, error.reason) }
        }
    }

    @Test fun arkFlashModelsUseDirectAnswersWithoutChangingOtherModelsOrProviders() = runBlocking {
        val models = listOf("deepseek-v4-1-flash-261017", "deepseek-v4-1-flash-260910", "deepseek-v4-1-flash-economy-261017")
        for ((url, model) in models.map { "https://ark.cn-beijing.volces.com/api/v3" to it } + listOf(
            "https://example.com/v1" to models.last(),
            "https://ark.cn-beijing.volces.com/api/v3" to "another-model",
        )) {
            val transport = client("""{"choices":[{"message":{"content":"OK"},"finish_reason":"stop"}]}""") { request ->
                val buffer = Buffer()
                request.body!!.writeTo(buffer)
                val payload = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
                assertEquals("16", payload["max_tokens"]!!.jsonPrimitive.content)
                if (request.url.host == "ark.cn-beijing.volces.com" && model in models) {
                    assertEquals("disabled", payload["thinking"]!!.jsonObject["type"]!!.jsonPrimitive.content)
                } else assertFalse(payload.containsKey("thinking"))
            }
            assertEquals("OK", transport.complete(config.copy(baseUrl = url, model = model), "", "", 16))
        }
    }
}
