package com.intent.reader

import com.intent.reader.ai.AiConfig
import com.intent.reader.ai.AiProvider
import com.intent.reader.ai.aiHash
import com.intent.reader.ai.editBaseUrl
import com.intent.reader.ai.selectProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AiProviderTest {
    private val existing = AiConfig(true, true, "https://api.deepseek.com", "my-model", "test-key-not-real")

    @Test fun allNineRequestedPresetsHaveUsableCompatibleEndpoints() {
        assertEquals(setOf("volcengine", "bailian", "deepseek", "siliconflow", "openrouter", "openai", "gemini", "claude", "groq"),
            AiProvider.entries.filter { it != AiProvider.CUSTOM }.map { it.id }.toSet())
        AiProvider.entries.filter { it != AiProvider.CUSTOM }.forEach {
            val endpoint = existing.selectProvider(it).endpoint()
            assertTrue(endpoint.isHttps)
            assertTrue(endpoint.encodedPath.endsWith("/chat/completions"))
        }
    }

    @Test fun switchingProvidersClearsCredentialsAndModelWithoutSavingOrEnablingAnything() {
        val updated = existing.copy(workspaceId = "workspace-1").selectProvider(AiProvider.GEMINI)
        assertEquals("gemini", updated.providerId)
        assertEquals("https://generativelanguage.googleapis.com/v1beta/openai", updated.baseUrl)
        assertEquals("", updated.apiKey)
        assertEquals("", updated.model)
        assertEquals("", updated.workspaceId)
        assertTrue(updated.enabled)
        assertTrue(updated.quoteEnabled)
        assertEquals("test-key-not-real", existing.apiKey)
    }

    @Test fun selectingPresetForSameEndpointRetainsAnExistingCustomConfiguration() {
        val updated = existing.copy(baseUrl = "https://api.deepseek.com/chat/completions/").selectProvider(AiProvider.DEEPSEEK)
        assertEquals(existing.apiKey, updated.apiKey)
        assertEquals(existing.model, updated.model)
        assertEquals("deepseek", updated.providerId)
    }

    @Test fun customSelectionPreservesValuesAndUnknownProviderIdsRemainEditable() {
        assertEquals(existing, existing.selectProvider(AiProvider.CUSTOM))
        assertEquals(AiProvider.CUSTOM, AiProvider.fromId("future-provider"))
    }

    @Test fun editingOriginClearsKeyButEditingOnlyPathPreservesIt() {
        assertEquals(existing.apiKey, existing.editBaseUrl("https://api.deepseek.com/v1").apiKey)
        for (url in listOf("https://another.example/v1", "http://api.deepseek.com", "https://api.deepseek.com:8443", "")) {
            val changed = existing.copy(workspaceId = "workspace-1").editBaseUrl(url)
            assertEquals("", changed.apiKey)
            assertEquals("", changed.workspaceId)
            assertEquals("custom", changed.providerId)
        }
    }

    @Test fun oldConfigurationDecodesAndRetainsTodaysCacheAndAttemptIdentity() {
        val old = Json.decodeFromString<AiConfig>("""{"enabled":true,"baseUrl":"https://api.deepseek.com","model":"my-model","apiKey":"test-key-not-real"}""")
        assertEquals("custom", old.providerId)
        assertEquals("", old.workspaceId)
        assertEquals(aiHash("${old.baseUrl}\n${old.model}\n${old.apiKey}"), old.fingerprint())
        assertEquals(old.fingerprint(), old.copy(providerId = "deepseek").fingerprint())
        assertNotEquals(old.fingerprint(), old.copy(workspaceId = "workspace-1").fingerprint())
    }
}
