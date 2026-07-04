package app.suslog.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AiCredentialStore(
    context: Context,
    private val localUserId: String?,
) {
    private val prefs = context.applicationContext
        .getSharedPreferences("suslog_ai", Context.MODE_PRIVATE)

    var apiKey: String
        get() {
            val userId = localUserId ?: return ""
            val encrypted = prefs.getString(apiKeyKey(userId), null) ?: return ""

            return runCatching { decrypt(encrypted) }.getOrDefault("")
        }
        set(value) {
            val userId = localUserId ?: return
            val trimmed = value.trim()
            if (trimmed.isBlank()) {
                prefs.edit().remove(apiKeyKey(userId)).apply()
            } else {
                prefs.edit().putString(apiKeyKey(userId), encrypt(trimmed)).apply()
            }
        }

    var model: String
        get() {
            val userId = localUserId ?: return DEFAULT_MODEL
            return prefs.getString(modelKey(userId), DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL }
        }
        set(value) {
            val userId = localUserId ?: return
            prefs.edit().putString(modelKey(userId), value.trim().ifBlank { DEFAULT_MODEL }).apply()
        }

    val isConnected: Boolean
        get() {
            val userId = localUserId ?: return false
            return prefs.contains(apiKeyKey(userId))
        }

    var monthlyBudgetUsd: Double?
        get() {
            val userId = localUserId ?: return null
            return prefs.getString(monthlyBudgetKey(userId), null)
                ?.toDoubleOrNull()
                ?.takeIf { it > 0.0 }
        }
        set(value) {
            val userId = localUserId ?: return
            val editor = prefs.edit()
            if (value == null || value <= 0.0) {
                editor.remove(monthlyBudgetKey(userId))
            } else {
                editor.putString(monthlyBudgetKey(userId), value.toString())
            }
            editor.apply()
        }

    fun clear() {
        val userId = localUserId ?: return
        prefs.edit()
            .remove(apiKeyKey(userId))
            .remove(modelKey(userId))
            .remove(monthlyBudgetKey(userId))
            .apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

        return listOf(cipher.iv, ciphertext)
            .joinToString(separator = ":") {
                Base64.encodeToString(it, Base64.NO_WRAP)
            }
    }

    private fun decrypt(value: String): String {
        val parts = value.split(":")
        require(parts.size == 2)

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun apiKeyKey(localUserId: String): String =
        "openai_api_key_$localUserId"

    private fun modelKey(localUserId: String): String =
        "openai_model_$localUserId"

    private fun monthlyBudgetKey(localUserId: String): String =
        "openai_monthly_budget_usd_$localUserId"

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
        private const val KEY_ALIAS = "suslog_ai_credentials_v1"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}
