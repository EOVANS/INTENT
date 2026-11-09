package com.intent.reader

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.intent.reader.ai.AiConfig
import com.intent.reader.ai.AiConfigStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class AiConfigStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val secret = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val file get() = File(context.noBackupFilesDir, "ai-config.bin")
    private val config = AiConfig(true, true, "https://example.com/v1", "test-model", "test-key-not-real")

    @Before fun setup() { file.delete() }
    @After fun cleanup() { file.delete() }

    @Test fun configIsEncryptedOutsideBackupAndRestoredWithSameKey() = runBlocking {
        AiConfigStore(context) { secret }.save(config)
        assertFalse(file.readBytes().toString(Charsets.UTF_8).contains(config.apiKey))
        assertFalse(file.readBytes().toString(Charsets.UTF_8).contains(config.baseUrl))
        assertEquals(config, AiConfigStore(context) { secret }.config.value)
    }

    @Test fun wrongKeyAndCorruptedFilesDisableAiSafely() = runBlocking {
        AiConfigStore(context) { secret }.save(config)
        val other = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        assertFalse(AiConfigStore(context) { other }.config.value.enabled)
        file.writeText("broken")
        assertFalse(AiConfigStore(context) { secret }.config.value.enabled)
    }

    @Test fun clearingConfigurationDeletesPersistedCredentials() = runBlocking {
        val store = AiConfigStore(context) { secret }
        store.save(config)
        store.clear()
        assertFalse(file.exists())
        assertEquals(AiConfig(), store.config.value)
        assertEquals(AiConfig(), AiConfigStore(context) { secret }.config.value)
    }

    @Test fun providerAndWorkspaceRoundTripWithoutPlaintextStorage() = runBlocking {
        val value = config.copy(baseUrl = "https://api.anthropic.com/v1", providerId = "claude", workspaceId = "workspace-1")
        AiConfigStore(context) { secret }.save(value)
        assertEquals(value, AiConfigStore(context) { secret }.config.value)
        assertFalse(file.readBytes().toString(Charsets.UTF_8).contains(value.workspaceId))
    }
}
