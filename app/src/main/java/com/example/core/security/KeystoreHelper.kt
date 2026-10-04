package com.example.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreHelper(context: Context) {

    private val prefs = context.getSharedPreferences("nova_ai_secure_store", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "KeystoreHelper"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "NovaAiMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        fun redactSecrets(text: String): String {
            // Redact potential API keys (e.g. AIza..., sk-..., etc.)
            var sanitized = text
            sanitized = sanitized.replace(Regex("AIza[0-9A-Za-z-_]{35}"), "AIza*********************************")
            sanitized = sanitized.replace(Regex("sk-[0-9A-Za-z]{20,}"), "sk-********************")
            sanitized = sanitized.replace(Regex("bearer\\s+[A-Za-z0-9-_.]+", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
            return sanitized
        }
    }

    init {
        ensureKey()
    }

    private fun ensureKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                keyGen.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .build()
                )
                keyGen.generateKey()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hardware Keystore init issue (fallback mode enabled): ${e.message}")
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }

    fun saveEncryptedString(key: String, plainText: String) {
        val secretKey = getSecretKey()
        if (secretKey != null) {
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                val combined = ByteArray(iv.size + encryptedBytes.size)
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
                val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
                prefs.edit().putString(key, encoded).apply()
                return
            } catch (e: Exception) {
                Log.e(TAG, "Encryption failed, saving obfuscated fallback", e)
            }
        }
        // Fallback obfuscation if Keystore is unavailable in test environment
        val obfuscated = Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        prefs.edit().putString(key, "raw:$obfuscated").apply()
    }

    fun getDecryptedString(key: String, defaultValue: String = ""): String {
        val stored = prefs.getString(key, null) ?: return defaultValue
        if (stored.startsWith("raw:")) {
            return try {
                String(Base64.decode(stored.removePrefix("raw:"), Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: Exception) {
                defaultValue
            }
        }

        val secretKey = getSecretKey() ?: return defaultValue
        return try {
            val combined = Base64.decode(stored, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed for key $key", e)
            defaultValue
        }
    }
}
