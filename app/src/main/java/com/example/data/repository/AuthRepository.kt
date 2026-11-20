package com.example.data.repository

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

data class LocalAuthUser(
    val uid: String,
    val username: String,
    val displayName: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class UsernameValidationResult(
    val normalized: String,
    val isValid: Boolean,
    val errorMessage: String? = null
)

sealed interface UsernameAvailability {
    data object Idle : UsernameAvailability
    data object Checking : UsernameAvailability
    data class Available(val normalized: String) : UsernameAvailability
    data class Taken(val normalized: String) : UsernameAvailability
    data class Invalid(val message: String) : UsernameAvailability
}

class AuthRepository {

    fun authStateFlow(context: Context): Flow<LocalAuthUser?> {
        ensureLoaded(context.applicationContext)
        return sharedAuthFlow.asStateFlow()
    }

    fun currentUser(context: Context): LocalAuthUser? {
        ensureLoaded(context.applicationContext)
        return sharedAuthFlow.value
    }

    fun validateUsername(raw: String): UsernameValidationResult {
        val normalized = raw.trim().lowercase().removePrefix("@")
        return when {
            normalized.isBlank() -> UsernameValidationResult(normalized, false, "Enter a username.")
            normalized.length < 3 -> UsernameValidationResult(normalized, false, "Username must be at least 3 characters.")
            normalized.length > 24 -> UsernameValidationResult(normalized, false, "Username cannot exceed 24 characters.")
            !normalized.matches(USERNAME_REGEX) -> UsernameValidationResult(
                normalized,
                false,
                "Use lowercase letters, numbers, '_' or '.' (start and end with a letter or number)."
            )
            ".." in normalized || "__" in normalized -> UsernameValidationResult(
                normalized,
                false,
                "Avoid consecutive dots or underscores."
            )
            else -> UsernameValidationResult(normalized, true, null)
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Enter a password."
            password.length < 6 -> "Password must be at least 6 characters."
            password.length > 128 -> "Password is too long."
            else -> null
        }
    }

    fun checkUsernameAvailability(context: Context, rawUsername: String): UsernameAvailability {
        val validation = validateUsername(rawUsername)
        if (!validation.isValid) {
            return UsernameAvailability.Invalid(validation.errorMessage ?: "Invalid username format.")
        }
        val accounts = readAccountsArray(context.applicationContext)
        val activeUid = sharedAuthFlow.value?.uid
        for (i in 0 until accounts.length()) {
            val obj = accounts.optJSONObject(i) ?: continue
            if (obj.optString("username").equals(validation.normalized, ignoreCase = true)) {
                if (activeUid != null && obj.optString("uid") == activeUid) {
                    return UsernameAvailability.Available(validation.normalized)
                }
                return UsernameAvailability.Taken(validation.normalized)
            }
        }
        return UsernameAvailability.Available(validation.normalized)
    }

    fun registerWithUsernamePassword(
        context: Context,
        displayName: String,
        rawUsername: String,
        password: String
    ): Result<LocalAuthUser> {
        val appContext = context.applicationContext
        val cleanName = displayName.trim().take(60)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your display name."))
        }
        val usernameVal = validateUsername(rawUsername)
        if (!usernameVal.isValid) {
            return Result.failure(IllegalArgumentException(usernameVal.errorMessage ?: "Invalid username."))
        }
        val passErr = validatePassword(password)
        if (passErr != null) {
            return Result.failure(IllegalArgumentException(passErr))
        }

        val accounts = readAccountsArray(appContext)
        for (i in 0 until accounts.length()) {
            val obj = accounts.optJSONObject(i) ?: continue
            if (obj.optString("username").equals(usernameVal.normalized, ignoreCase = true)) {
                return Result.failure(IllegalStateException("Username @${usernameVal.normalized} is already taken."))
            }
        }

        val uid = "user_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val now = System.currentTimeMillis()
        val saltedHash = hashPassword(password)
        val newRecord = JSONObject().apply {
            put("uid", uid)
            put("username", usernameVal.normalized)
            put("displayName", cleanName)
            put("passwordHash", saltedHash)
            put("createdAtMillis", now)
        }
        accounts.put(newRecord)

        prefs(appContext).edit()
            .putString(KEY_ACCOUNTS_JSON, accounts.toString())
            .putString(KEY_ACTIVE_UID, uid)
            .apply()

        val user = LocalAuthUser(
            uid = uid,
            username = usernameVal.normalized,
            displayName = cleanName,
            createdAtMillis = now
        )
        synchronized(LOCK) {
            loaded = true
            sharedAuthFlow.value = user
        }
        return Result.success(user)
    }

    fun signInWithUsernamePassword(
        context: Context,
        rawUsername: String,
        password: String
    ): Result<LocalAuthUser> {
        val appContext = context.applicationContext
        val usernameVal = validateUsername(rawUsername)
        if (!usernameVal.isValid) {
            return Result.failure(IllegalArgumentException(usernameVal.errorMessage ?: "Enter a valid username."))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your password."))
        }

        val accounts = readAccountsArray(appContext)
        for (i in 0 until accounts.length()) {
            val obj = accounts.optJSONObject(i) ?: continue
            if (obj.optString("username").equals(usernameVal.normalized, ignoreCase = true)) {
                val storedHash = obj.optString("passwordHash")
                if (!verifyPassword(password, storedHash)) {
                    return Result.failure(IllegalArgumentException("Incorrect password for @${usernameVal.normalized}."))
                }
                val user = obj.toLocalAuthUser()
                prefs(appContext).edit()
                    .putString(KEY_ACTIVE_UID, user.uid)
                    .apply()
                synchronized(LOCK) {
                    loaded = true
                    sharedAuthFlow.value = user
                }
                return Result.success(user)
            }
        }
        return Result.failure(IllegalStateException("No account found with username @${usernameVal.normalized}. Tap Sign Up to create one."))
    }

    fun resetLocalPassword(
        context: Context,
        rawUsername: String,
        newPassword: String
    ): Result<Unit> {
        val appContext = context.applicationContext
        val usernameVal = validateUsername(rawUsername)
        if (!usernameVal.isValid) {
            return Result.failure(IllegalArgumentException(usernameVal.errorMessage ?: "Enter a valid username."))
        }
        val passErr = validatePassword(newPassword)
        if (passErr != null) {
            return Result.failure(IllegalArgumentException(passErr))
        }

        val accounts = readAccountsArray(appContext)
        for (i in 0 until accounts.length()) {
            val obj = accounts.optJSONObject(i) ?: continue
            if (obj.optString("username").equals(usernameVal.normalized, ignoreCase = true)) {
                obj.put("passwordHash", hashPassword(newPassword))
                prefs(appContext).edit()
                    .putString(KEY_ACCOUNTS_JSON, accounts.toString())
                    .apply()
                return Result.success(Unit)
            }
        }
        return Result.failure(IllegalStateException("No account found matching @${usernameVal.normalized}."))
    }

    fun signOut(context: Context) {
        val appContext = context.applicationContext
        prefs(appContext).edit().remove(KEY_ACTIVE_UID).apply()
        synchronized(LOCK) {
            loaded = true
            sharedAuthFlow.value = null
        }
    }

    fun deleteAccount(context: Context): Result<Unit> {
        val appContext = context.applicationContext
        val current = currentUser(appContext)
            ?: return Result.failure(IllegalStateException("No signed-in user."))

        val accounts = readAccountsArray(appContext)
        val remaining = JSONArray()
        for (i in 0 until accounts.length()) {
            val obj = accounts.optJSONObject(i) ?: continue
            if (obj.optString("uid") != current.uid) {
                remaining.put(obj)
            }
        }

        prefs(appContext).edit()
            .putString(KEY_ACCOUNTS_JSON, remaining.toString())
            .remove(KEY_ACTIVE_UID)
            .apply()

        // Wipe per-user wardrobe & preferences data store
        LithfitRepository.clearUserStore(appContext, current.uid)

        // Wipe private local media folders for this device install
        appContext.filesDir.resolve("wardrobe").deleteRecursively()
        appContext.filesDir.resolve("previews").deleteRecursively()
        appContext.filesDir.resolve("personal_preview").deleteRecursively()

        synchronized(LOCK) {
            loaded = true
            sharedAuthFlow.value = null
        }
        return Result.success(Unit)
    }

    private fun ensureLoaded(appContext: Context) {
        synchronized(LOCK) {
            if (loaded) return
            val sp = prefs(appContext)
            val activeUid = sp.getString(KEY_ACTIVE_UID, null)
            var foundUser: LocalAuthUser? = null
            if (!activeUid.isNullOrBlank()) {
                val accounts = readAccountsArray(appContext)
                for (i in 0 until accounts.length()) {
                    val obj = accounts.optJSONObject(i) ?: continue
                    if (obj.optString("uid") == activeUid) {
                        foundUser = obj.toLocalAuthUser()
                        break
                    }
                }
            }
            sharedAuthFlow.value = foundUser
            loaded = true
        }
    }

    private fun readAccountsArray(appContext: Context): JSONArray {
        val raw = prefs(appContext).getString(KEY_ACCOUNTS_JSON, null) ?: return JSONArray()
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }

    private fun prefs(appContext: Context) =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun JSONObject.toLocalAuthUser(): LocalAuthUser = LocalAuthUser(
        uid = optString("uid"),
        username = optString("username"),
        displayName = optString("displayName").ifBlank { optString("username") },
        createdAtMillis = optLong("createdAtMillis", System.currentTimeMillis())
    )

    private fun hashPassword(password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val digest = sha256(saltB64, password)
        return "$saltB64:$digest"
    }

    private fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val actual = sha256(parts[0], password)
        return MessageDigest.isEqual(actual.toByteArray(), parts[1].toByteArray())
    }

    private fun sha256(saltB64: String, password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(saltB64.toByteArray(Charsets.UTF_8))
        val hashBytes = md.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    internal fun resetInMemoryForTesting() {
        synchronized(LOCK) {
            loaded = false
            sharedAuthFlow.value = null
        }
    }

    private companion object {
        const val PREFS_NAME = "lithfit_local_auth"
        const val KEY_ACCOUNTS_JSON = "accounts_json"
        const val KEY_ACTIVE_UID = "active_user_id"
        val USERNAME_REGEX = Regex("^[a-z0-9](?:[a-z0-9_.]{1,22}[a-z0-9])?$")
        val LOCK = Any()
        var loaded = false
        val sharedAuthFlow = MutableStateFlow<LocalAuthUser?>(null)
    }
}
