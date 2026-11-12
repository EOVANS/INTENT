package com.intent.reader.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class AiError { CONFIG, URL, EMPTY, TOO_LONG, AUTH, RATE_LIMIT, NETWORK, SERVICE, RESPONSE, QUOTE, QUOTE_CANDIDATES, QUOTE_SELECTION, STORAGE }
class AiException(val reason: AiError) : Exception(reason.name)

@Serializable
data class AiConfig(
    val enabled: Boolean = false,
    val quoteEnabled: Boolean = false,
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val providerId: String = "custom",
    val workspaceId: String = "",
) {
    fun endpoint(): HttpUrl {
        val url = baseUrl.trim().toHttpUrlOrNull() ?: throw AiException(AiError.URL)
        if (!url.isHttps || url.username.isNotEmpty() || url.password.isNotEmpty() ||
            url.query != null || url.fragment != null) throw AiException(AiError.URL)
        val path = url.encodedPath.trimEnd('/')
        return url.newBuilder().encodedPath(
            if (path.endsWith("/chat/completions")) path else "$path/chat/completions"
        ).build()
    }

    fun validate() {
        if (model.isBlank() || apiKey.isBlank() || apiKey.any { it.code !in 0x21..0x7e } ||
            workspaceId.any { it.code !in 0x21..0x7e }) {
            throw AiException(AiError.CONFIG)
        }
        endpoint()
    }

    fun fingerprint(): String {
        val identity = "$baseUrl\n$model\n$apiKey"
        // Preserve existing cache/attempt keys when upgrading a configuration
        // without a workspace, avoiding an extra automatic paid request today.
        return aiHash(if (workspaceId.isBlank()) identity else "$identity\n$workspaceId")
    }

    override fun toString(): String = "AiConfig(enabled=$enabled, quoteEnabled=$quoteEnabled, apiKey=[redacted])"
}

internal fun aiHash(text: String): String = MessageDigest.getInstance("SHA-256")
    .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

/** The entire configuration is encrypted; neither keys nor results enter Android backup. */
class AiConfigStore(context: Context, private val encryptionKey: () -> SecretKey = ::androidAiKey) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "ai-config.bin"))
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(read())
    val config = mutable.asStateFlow()

    private fun read(): AiConfig = runCatching {
        if (!file.baseFile.exists()) return AiConfig()
        val bytes = file.readFully()
        require(bytes.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        json.decodeFromString<AiConfig>(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8))
    }.getOrDefault(AiConfig())

    suspend fun save(value: AiConfig) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val clean = value.copy(baseUrl = value.baseUrl.trim().trimEnd('/'), model = value.model.trim(),
                apiKey = value.apiKey.trim(), workspaceId = value.workspaceId.trim())
            if (clean.enabled) clean.validate()
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
                require(cipher.iv.size == 12)
                val output = file.startWrite()
                try {
                    output.write(cipher.iv + cipher.doFinal(json.encodeToString(clean).toByteArray(Charsets.UTF_8)))
                    file.finishWrite(output)
                } catch (error: Exception) {
                    file.failWrite(output)
                    throw error
                }
                mutable.value = clean
            } catch (_: Exception) {
                throw AiException(AiError.STORAGE)
            }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            file.delete()
            if (file.baseFile.exists()) throw AiException(AiError.STORAGE)
            mutable.value = AiConfig()
        }
    }

}

private fun androidAiKey(): SecretKey {
    val alias = "intent-ai-config-v1"
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(alias, null) as? SecretKey)?.let { return it }
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
        init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
    }.generateKey()
}
