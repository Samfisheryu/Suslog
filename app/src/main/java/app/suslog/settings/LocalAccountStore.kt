package app.suslog.settings

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class LocalAccount(
    val id: String,
    val email: String,
    val createdAtMillis: Long,
)

data class LocalAccountAuthResult(
    val account: LocalAccount?,
    val errorMessage: String?,
)

class LocalAccountStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "suslog_local_accounts",
        Context.MODE_PRIVATE
    )
    private val random = SecureRandom()

    fun accounts(): List<LocalAccount> =
        storedAccounts().map { it.account }

    fun hasAccounts(): Boolean =
        storedAccounts().isNotEmpty()

    fun activeAccount(): LocalAccount? {
        val activeId = preferences.getString(KEY_ACTIVE_ACCOUNT_ID, null) ?: return null

        return storedAccounts().firstOrNull { it.account.id == activeId }?.account
    }

    fun createAccount(
        email: String,
        password: String,
    ): LocalAccountAuthResult {
        val normalizedEmail = normalizeEmail(email)
        validateEmail(normalizedEmail)?.let { return LocalAccountAuthResult(null, it) }
        validatePassword(password)?.let { return LocalAccountAuthResult(null, it) }

        val existingAccounts = storedAccounts()
        if (existingAccounts.any { it.account.email.equals(normalizedEmail, ignoreCase = true) }) {
            return LocalAccountAuthResult(null, "An account with this email already exists.")
        }

        val salt = ByteArray(SALT_BYTE_COUNT).also(random::nextBytes)
        val stored = StoredLocalAccount(
            account = LocalAccount(
                id = UUID.randomUUID().toString(),
                email = normalizedEmail,
                createdAtMillis = System.currentTimeMillis()
            ),
            passwordSalt = encode(salt),
            passwordHash = encode(hashPassword(password, salt))
        )

        saveAccounts(existingAccounts + stored)
        setActiveAccount(stored.account.id)

        return LocalAccountAuthResult(stored.account, null)
    }

    fun authenticate(
        email: String,
        password: String,
    ): LocalAccountAuthResult {
        val normalizedEmail = normalizeEmail(email)
        val stored = storedAccounts().firstOrNull {
            it.account.email.equals(normalizedEmail, ignoreCase = true)
        } ?: return LocalAccountAuthResult(null, "Account not found.")

        val salt = decode(stored.passwordSalt)
        val candidateHash = hashPassword(password, salt)
        val expectedHash = decode(stored.passwordHash)

        return if (MessageDigest.isEqual(candidateHash, expectedHash)) {
            setActiveAccount(stored.account.id)
            LocalAccountAuthResult(stored.account, null)
        } else {
            LocalAccountAuthResult(null, "Email or password is incorrect.")
        }
    }

    fun logOff() {
        preferences.edit()
            .remove(KEY_ACTIVE_ACCOUNT_ID)
            .apply()
    }

    private fun setActiveAccount(accountId: String) {
        preferences.edit()
            .putString(KEY_ACTIVE_ACCOUNT_ID, accountId)
            .apply()
    }

    private fun storedAccounts(): List<StoredLocalAccount> {
        val raw = preferences.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()

        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                val account = LocalAccount(
                    id = item.optString("id"),
                    email = item.optString("email"),
                    createdAtMillis = item.optLong("createdAtMillis")
                )
                val salt = item.optString("passwordSalt")
                val hash = item.optString("passwordHash")

                if (account.id.isNotBlank() && account.email.isNotBlank() &&
                    salt.isNotBlank() && hash.isNotBlank()
                ) {
                    add(
                        StoredLocalAccount(
                            account = account,
                            passwordSalt = salt,
                            passwordHash = hash
                        )
                    )
                }
            }
        }
    }

    private fun saveAccounts(accounts: List<StoredLocalAccount>) {
        val array = JSONArray(
            accounts.map { stored ->
                JSONObject()
                    .put("id", stored.account.id)
                    .put("email", stored.account.email)
                    .put("createdAtMillis", stored.account.createdAtMillis)
                    .put("passwordSalt", stored.passwordSalt)
                    .put("passwordHash", stored.passwordHash)
            }
        )

        preferences.edit()
            .putString(KEY_ACCOUNTS, array.toString())
            .apply()
    }

    private fun normalizeEmail(email: String): String =
        email.trim().lowercase()

    private fun validateEmail(email: String): String? =
        when {
            email.isBlank() -> "Email is required."
            "@" !in email || "." !in email.substringAfter("@", "") -> "Enter a valid email."
            else -> null
        }

    private fun validatePassword(password: String): String? =
        when {
            password.length < MIN_PASSWORD_LENGTH ->
                "Password must be at least $MIN_PASSWORD_LENGTH characters."
            else -> null
        }

    private fun hashPassword(
        password: String,
        salt: ByteArray,
    ): ByteArray {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            PASSWORD_KEY_LENGTH_BITS
        )

        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(spec)
            .encoded
    }

    private fun encode(value: ByteArray): String =
        Base64.encodeToString(value, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    private data class StoredLocalAccount(
        val account: LocalAccount,
        val passwordSalt: String,
        val passwordHash: String,
    )

    private companion object {
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_ACTIVE_ACCOUNT_ID = "active_account_id"
        const val MIN_PASSWORD_LENGTH = 8
        const val SALT_BYTE_COUNT = 16
        const val PBKDF2_ITERATIONS = 120_000
        const val PASSWORD_KEY_LENGTH_BITS = 256
    }
}
