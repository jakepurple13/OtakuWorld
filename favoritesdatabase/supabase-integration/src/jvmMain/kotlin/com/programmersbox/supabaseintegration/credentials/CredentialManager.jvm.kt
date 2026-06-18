package com.programmersbox.supabaseintegration.credentials

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class JvmCredentialManager : CredentialManager {
    private val configDir = File(System.getProperty("user.home"), ".otakuworld")
    private val credFile = File(configDir, "supabase.enc")
    private val keyFile = File(configDir, "supabase.key")
    private val _hasCredentials = MutableStateFlow(credFile.exists())

    private fun getOrCreateKey(): SecretKey {
        if (!keyFile.exists()) {
            configDir.mkdirs()
            val kg = KeyGenerator.getInstance("AES").apply { init(256) }
            keyFile.writeBytes(kg.generateKey().encoded)
        }
        return SecretKeySpec(keyFile.readBytes(), "AES")
    }

    override fun hasCredentials(): Flow<Boolean> = _hasCredentials

    override suspend fun saveCredentials(credentials: SupabaseCredentials) {
        configDir.mkdirs()
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val json = Json.encodeToString(credentials)
        credFile.writeBytes(cipher.doFinal(json.toByteArray()))
        _hasCredentials.value = true
    }

    override fun getCredentials(): SupabaseCredentials? {
        if (!credFile.exists()) return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey())
            }
            Json.decodeFromString<SupabaseCredentials>(String(cipher.doFinal(credFile.readBytes())))
        }.getOrNull()
    }

    override suspend fun clearCredentials() {
        credFile.delete()
        _hasCredentials.value = false
    }
}

actual fun createCredentialManager(context: Any?): CredentialManager = JvmCredentialManager()
