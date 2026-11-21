package com.example.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.model.LithfitConstants
import com.example.data.model.OwnerRegistrationNotification
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

data class LocalAuthUser(
    val uid: String,
    val username: String,
    val displayName: String,
    val email: String? = null,
    val phone: String? = null,
    val googleSignedIn: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val isAuthorizedOwner: Boolean
        get() = email.equals(LithfitConstants.OWNER_NOTIFY_EMAIL, ignoreCase = true)
}

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

    fun validateEmail(raw: String): String? {
        val clean = raw.trim()
        if (clean.isBlank()) return null
        return if (!Patterns.EMAIL_ADDRESS.matcher(clean).matches()) {
            "Enter a valid email address."
        } else null
    }

    fun validatePhone(raw: String): String? {
        val digits = raw.filter { it.isDigit() || it == '+' }
        if (raw.isBlank()) return null
        val digitCount = digits.count { it.isDigit() }
        return if (digitCount !in 7..15) {
            "Enter a valid phone number (7–15 digits)."
        } else null
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
        password: String,
        email: String? = null,
        phone: String? = null
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
        val cleanEmail = email?.trim()?.ifBlank { null }
        cleanEmail?.let {
            validateEmail(it)?.let { msg -> return Result.failure(IllegalArgumentException(msg)) }
        }
        val cleanPhone = normalizePhone(phone)
        if (!phone.isNullOrBlank()) {
            validatePhone(phone)?.let { msg -> return Result.failure(IllegalArgumentException(msg)) }
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
            if (cleanEmail != null && obj.optString("email").equals(cleanEmail, ignoreCase = true)) {
                return Result.failure(IllegalStateException("An account with that email already exists."))
            }
            if (cleanPhone != null && obj.optString("phone") == cleanPhone) {
                return Result.failure(IllegalStateException("An account with that phone number already exists."))
            }
        }

        val uid = "user_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val now = System.currentTimeMillis()
        val saltedHash = hashPassword(password)
        val newRecord = JSONObject().apply {
            put("uid", uid)
            put("username", usernameVal.normalized)
            put("displayName", cleanName)
            put("email", cleanEmail.orEmpty())
            put("phone", cleanPhone.orEmpty())
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
            email = cleanEmail,
            phone = cleanPhone,
            googleSignedIn = false,
            createdAtMillis = now
        )
        synchronized(LOCK) {
            loaded = true
            sharedAuthFlow.value = user
        }
        recordOwnerRegistrationAlert(appContext, user, authMethod = "LOCAL")
        return Result.success(user)
    }

    /**
     * Authenticates using Username, Email, or Phone + Password stored in encrypted salted SHA-256 format.
     */
    fun signInWithUsernamePassword(
        context: Context,
        rawIdentifier: String,
        password: String
    ): Result<LocalAuthUser> {
        val appContext = context.applicationContext
        val trimmed = rawIdentifier.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your username, email, or phone number."))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your password."))
        }

        val normUsername = trimmed.lowercase().removePrefix("@")
        val normPhone = normalizePhone(trimmed)
        val accounts = readAccountsArray(appContext)
        for (i in 0 until accounts.length()) {
            val obj = accounts.optJSONObject(i) ?: continue
            val matchesUsername = obj.optString("username").equals(normUsername, ignoreCase = true)
            val matchesEmail = obj.optString("email").takeIf { it.isNotBlank() }?.equals(trimmed, ignoreCase = true) == true
            val matchesPhone = normPhone != null && obj.optString("phone").takeIf { it.isNotBlank() } == normPhone
            if (matchesUsername || matchesEmail || matchesPhone) {
                val storedHash = obj.optString("passwordHash")
                if (!verifyPassword(password, storedHash)) {
                    return Result.failure(IllegalArgumentException("Incorrect password for @${obj.optString("username")}."))
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
        return Result.failure(IllegalStateException("No account found matching \"$trimmed\". Tap Create Account to join LITHFIT."))
    }

    suspend fun signInWithGoogleIdToken(
        context: Context,
        idToken: String
    ): Result<LocalAuthUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = Firebase.auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Google authentication succeeded without user profile."))
            val appContext = context.applicationContext
            val rawHandle = (firebaseUser.email?.substringBefore("@")
                ?: firebaseUser.displayName
                ?: "member")
                .lowercase()
                .replace(Regex("[^a-z0-9_.]"), "_")
                .trim('_', '.')
                .ifBlank { "member" }
                .take(24)
            val safeHandle = if (rawHandle.length < 3) "${rawHandle}fit".take(24) else rawHandle
            val user = LocalAuthUser(
                uid = firebaseUser.uid,
                username = safeHandle,
                displayName = firebaseUser.displayName?.take(60)?.ifBlank { "Lithfit Member" } ?: "Lithfit Member",
                email = firebaseUser.email,
                phone = firebaseUser.phoneNumber,
                googleSignedIn = true,
                createdAtMillis = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
            )
            prefs(appContext).edit()
                .putString(KEY_ACTIVE_UID, user.uid)
                .apply()
            synchronized(LOCK) {
                loaded = true
                sharedAuthFlow.value = user
            }
            if (authResult.additionalUserInfo?.isNewUser == true) {
                recordOwnerRegistrationAlert(appContext, user, authMethod = "GOOGLE")
            }
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in with Firebase failed", e)
            Result.failure(e)
        }
    }

    fun resetLocalPassword(
        context: Context,
        rawIdentifier: String,
        newPassword: String
    ): Result<Unit> {
        val appContext = context.applicationContext
        val trimmed = rawIdentifier.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your username, email, or phone."))
        }
        val passErr = validatePassword(newPassword)
        if (passErr != null) {
            return Result.failure(IllegalArgumentException(passErr))
        }

        val normUsername = trimmed.lowercase().removePrefix("@")
        val normPhone = normalizePhone(trimmed)
        val accounts = readAccountsArray(appContext)
        for (i in 0 until accounts.length()) {
            val obj = accounts.optJSONObject(i) ?: continue
            val matchesUsername = obj.optString("username").equals(normUsername, ignoreCase = true)
            val matchesEmail = obj.optString("email").takeIf { it.isNotBlank() }?.equals(trimmed, ignoreCase = true) == true
            val matchesPhone = normPhone != null && obj.optString("phone").takeIf { it.isNotBlank() } == normPhone
            if (matchesUsername || matchesEmail || matchesPhone) {
                obj.put("passwordHash", hashPassword(newPassword))
                prefs(appContext).edit()
                    .putString(KEY_ACCOUNTS_JSON, accounts.toString())
                    .apply()
                return Result.success(Unit)
            }
        }
        return Result.failure(IllegalStateException("No account found matching \"$trimmed\"."))
    }

    fun changePasswordForSignedInUser(
        context: Context,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        val appContext = context.applicationContext
        val active = currentUser(appContext)
            ?: return Result.failure(IllegalStateException("No active account."))
        if (active.googleSignedIn) {
            return Result.failure(IllegalStateException("Your account signs in with Google. Manage your password in your Google Account settings."))
        }
        val passErr = validatePassword(newPassword)
        if (passErr != null) return Result.failure(IllegalArgumentException(passErr))

        val accounts = readAccountsArray(appContext)
        for (i in 0 until accounts.length()) {
            val obj = accounts.optJSONObject(i) ?: continue
            if (obj.optString("uid") == active.uid) {
                if (!verifyPassword(currentPassword, obj.optString("passwordHash"))) {
                    return Result.failure(IllegalArgumentException("Current password is incorrect."))
                }
                obj.put("passwordHash", hashPassword(newPassword))
                prefs(appContext).edit().putString(KEY_ACCOUNTS_JSON, accounts.toString()).apply()
                return Result.success(Unit)
            }
        }
        return Result.failure(IllegalStateException("Account record not found."))
    }

    suspend fun signOut(context: Context) {
        val appContext = context.applicationContext
        withContext(NonCancellable) {
            runCatching {
                CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
            }
            runCatching { Firebase.auth.signOut() }
            prefs(appContext).edit().remove(KEY_ACTIVE_UID).apply()
            synchronized(LOCK) {
                loaded = true
                sharedAuthFlow.value = null
            }
        }
    }

    suspend fun deleteAccount(context: Context): Result<Unit> {
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

        LithfitRepository.clearUserStore(appContext, current.uid)

        appContext.filesDir.resolve("wardrobe").deleteRecursively()
        appContext.filesDir.resolve("previews").deleteRecursively()
        appContext.filesDir.resolve("personal_preview").deleteRecursively()
        appContext.filesDir.resolve("profile_photos").deleteRecursively()

        withContext(NonCancellable) {
            runCatching {
                CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
            }
            if (Firebase.auth.currentUser != null) {
                runCatching { Firebase.auth.currentUser?.delete()?.await() }
                runCatching { Firebase.auth.signOut() }
            }
            synchronized(LOCK) {
                loaded = true
                sharedAuthFlow.value = null
            }
        }
        return Result.success(Unit)
    }

    fun getOwnerRegistrationNotifications(
        context: Context,
        requestingUserEmail: String?
    ): List<OwnerRegistrationNotification> {
        if (!requestingUserEmail.equals(LithfitConstants.OWNER_NOTIFY_EMAIL, ignoreCase = true)) {
            return emptyList()
        }
        val raw = ownerNotifyPrefs(context.applicationContext).getString(KEY_OWNER_ALERTS_JSON, null)
            ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                OwnerRegistrationNotification(
                    id = o.optString("id"),
                    newUserId = o.optString("newUserId"),
                    username = o.optString("username"),
                    displayName = o.optString("displayName"),
                    authMethod = o.optString("authMethod", "LOCAL"),
                    recipientOwnerEmail = o.optString("recipientOwnerEmail", LithfitConstants.OWNER_NOTIFY_EMAIL),
                    message = o.optString("message"),
                    read = o.optBoolean("read", false),
                    createdAtMillis = o.optLong("createdAtMillis", 0L)
                )
            }.sortedByDescending { it.createdAtMillis }
        }.getOrDefault(emptyList())
    }

    fun markOwnerNotificationRead(context: Context, requestingUserEmail: String?, notificationId: String) {
        if (!requestingUserEmail.equals(LithfitConstants.OWNER_NOTIFY_EMAIL, ignoreCase = true)) return
        val sp = ownerNotifyPrefs(context.applicationContext)
        val arr = runCatching { JSONArray(sp.getString(KEY_OWNER_ALERTS_JSON, "[]")) }.getOrDefault(JSONArray())
        val updated = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("id") == notificationId) {
                o.put("read", true)
            }
            updated.put(o)
        }
        sp.edit().putString(KEY_OWNER_ALERTS_JSON, updated.toString()).apply()
    }

    private fun recordOwnerRegistrationAlert(
        appContext: Context,
        newUser: LocalAuthUser,
        authMethod: String
    ) {
        val sp = ownerNotifyPrefs(appContext)
        val existing = runCatching { JSONArray(sp.getString(KEY_OWNER_ALERTS_JSON, "[]")) }.getOrDefault(JSONArray())
        // Deduplicate by newUserId
        for (i in 0 until existing.length()) {
            val obj = existing.optJSONObject(i) ?: continue
            if (obj.optString("newUserId") == newUser.uid) return
        }
        val alert = JSONObject().apply {
            put("id", "owner_reg_${newUser.uid}")
            put("newUserId", newUser.uid)
            put("username", newUser.username)
            put("displayName", newUser.displayName)
            put("authMethod", authMethod)
            put("recipientOwnerEmail", LithfitConstants.OWNER_NOTIFY_EMAIL)
            put("message", "New user joined LITHFIT! @${newUser.username} (${newUser.displayName})")
            put("read", false)
            put("createdAtMillis", System.currentTimeMillis())
        }
        existing.put(alert)
        sp.edit().putString(KEY_OWNER_ALERTS_JSON, existing.toString()).apply()
    }

    private fun ensureLoaded(appContext: Context) {
        synchronized(LOCK) {
            if (loaded) return
            val fbUser = runCatching { Firebase.auth.currentUser }.getOrNull()
            if (fbUser != null) {
                val handle = (fbUser.email?.substringBefore("@")
                    ?: fbUser.displayName ?: "member")
                    .lowercase().replace(Regex("[^a-z0-9_.]"), "_").trim('_', '.').ifBlank { "member" }.take(24)
                sharedAuthFlow.value = LocalAuthUser(
                    uid = fbUser.uid,
                    username = handle,
                    displayName = fbUser.displayName?.ifBlank { "Lithfit Member" } ?: "Lithfit Member",
                    email = fbUser.email,
                    phone = fbUser.phoneNumber,
                    googleSignedIn = true,
                    createdAtMillis = fbUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                )
                loaded = true
                return
            }

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

    private fun ownerNotifyPrefs(appContext: Context) =
        appContext.getSharedPreferences(OWNER_NOTIFY_PREFS_NAME, Context.MODE_PRIVATE)

    private fun normalizePhone(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.filter { it.isDigit() || it == '+' }.trim()
        return cleaned.takeIf { it.count { ch -> ch.isDigit() } in 7..15 }
    }

    private fun JSONObject.toLocalAuthUser(): LocalAuthUser = LocalAuthUser(
        uid = optString("uid"),
        username = optString("username"),
        displayName = optString("displayName").ifBlank { optString("username") },
        email = optString("email").ifBlank { null },
        phone = optString("phone").ifBlank { null },
        googleSignedIn = false,
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
        const val TAG = "AuthRepository"
        const val PREFS_NAME = "lithfit_local_auth"
        const val OWNER_NOTIFY_PREFS_NAME = "lithfit_owner_notifications"
        const val KEY_ACCOUNTS_JSON = "accounts_json"
        const val KEY_ACTIVE_UID = "active_user_id"
        const val KEY_OWNER_ALERTS_JSON = "owner_registration_alerts"
        val USERNAME_REGEX = Regex("^[a-z0-9](?:[a-z0-9_.]{1,22}[a-z0-9])?$")
        val LOCK = Any()
        var loaded = false
        val sharedAuthFlow = MutableStateFlow<LocalAuthUser?>(null)
    }
}
