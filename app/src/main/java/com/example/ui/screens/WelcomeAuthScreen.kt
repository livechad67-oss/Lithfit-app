package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.data.repository.UsernameAvailability
import com.example.ui.components.ClastadLogoMark
import com.example.ui.components.LithfitLogoMark
import com.example.ui.components.LithfitPillButton
import com.example.ui.components.LithfitPrimaryButton
import com.example.ui.components.LithfitSecondaryButton
import com.example.ui.components.LithfitWordmark
import com.example.ui.theme.SageSuccess
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthScreenRoute {
    LOGIN,
    REGISTER,
    OTP_VERIFY,
    PASSWORD_RECOVERY
}

@Composable
fun WelcomeAuthScreen(
    authRepository: AuthRepository,
    onAuthenticated: (pendingDisplayName: String?, pendingUsername: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var route by remember { mutableStateOf(AuthScreenRoute.LOGIN) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Login state
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Sign Up state
    var regDisplayName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var showRegPassword by remember { mutableStateOf(false) }
    var usernameAvailability by remember { mutableStateOf<UsernameAvailability>(UsernameAvailability.Idle) }

    // Phone OTP verification UI state
    var otpCodeInput by remember { mutableStateOf("") }
    var generatedOtpPreview by remember { mutableStateOf("") }
    var otpCooldownSec by remember { mutableIntStateOf(0) }

    // Password Recovery state
    var recoveryIdentifier by remember { mutableStateOf("") }
    var recoveryNewPassword by remember { mutableStateOf("") }
    var showRecoveryPassword by remember { mutableStateOf(false) }

    BackHandler(enabled = route != AuthScreenRoute.LOGIN) {
        errorMessage = null
        infoMessage = null
        route = if (route == AuthScreenRoute.OTP_VERIFY) AuthScreenRoute.REGISTER else AuthScreenRoute.LOGIN
    }

    LaunchedEffect(otpCooldownSec) {
        if (otpCooldownSec > 0) {
            delay(1000L)
            otpCooldownSec -= 1
        }
    }

    LaunchedEffect(route, regUsername) {
        if (route != AuthScreenRoute.REGISTER) return@LaunchedEffect
        val raw = regUsername.trim()
        if (raw.isEmpty()) {
            usernameAvailability = UsernameAvailability.Idle
            return@LaunchedEffect
        }
        val localValidation = authRepository.validateUsername(raw)
        if (!localValidation.isValid) {
            usernameAvailability = UsernameAvailability.Invalid(localValidation.errorMessage ?: "Invalid username.")
            return@LaunchedEffect
        }
        usernameAvailability = UsernameAvailability.Checking
        delay(180)
        usernameAvailability = authRepository.checkUsernameAvailability(context, raw)
    }

    fun submitLogin() {
        if (isLoading) return
        isLoading = true
        errorMessage = null
        infoMessage = null
        val res = authRepository.signInWithUsernamePassword(context, loginIdentifier, loginPassword)
        isLoading = false
        res.onSuccess { user ->
            onAuthenticated(user.displayName, user.username)
        }.onFailure { err ->
            errorMessage = err.localizedMessage ?: "Sign in failed."
        }
    }

    fun completeRegistration() {
        isLoading = true
        errorMessage = null
        val res = authRepository.registerWithUsernamePassword(
            context = context,
            displayName = regDisplayName,
            rawUsername = regUsername,
            password = regPassword,
            email = regEmail.trim().ifBlank { null },
            phone = regPhone.trim().ifBlank { null }
        )
        isLoading = false
        res.onSuccess { user ->
            onAuthenticated(user.displayName, user.username)
        }.onFailure { err ->
            errorMessage = err.localizedMessage ?: "Could not create account."
        }
    }

    fun submitSignUp(verifyPhoneFirst: Boolean = false) {
        if (isLoading) return
        if (regPassword != regConfirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }
        val emailErr = authRepository.validateEmail(regEmail)
        if (emailErr != null) {
            errorMessage = emailErr
            return
        }
        val phoneErr = authRepository.validatePhone(regPhone)
        if (phoneErr != null) {
            errorMessage = phoneErr
            return
        }
        if (verifyPhoneFirst && regPhone.isNotBlank()) {
            generatedOtpPreview = (100000..999999).random().toString()
            otpCodeInput = ""
            otpCooldownSec = 30
            errorMessage = null
            infoMessage = "Verification step ready for ${regPhone.trim()} (Demo code: $generatedOtpPreview)."
            route = AuthScreenRoute.OTP_VERIFY
            return
        }
        completeRegistration()
    }

    fun startGoogleSignIn() {
        if (isLoading) return
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            errorMessage = "Google Sign-In configuration is unavailable on this build."
            return
        }
        val activity = context.findActivity()
        if (activity == null) {
            errorMessage = "Unable to open Google Sign-In sheet from this context."
            return
        }
        isLoading = true
        errorMessage = null
        infoMessage = null
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
        scope.launch {
            try {
                val credential = credentialManager.getCredential(activity, request).credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val res = authRepository.signInWithGoogleIdToken(context, token)
                    isLoading = false
                    res.onSuccess { user ->
                        onAuthenticated(user.displayName, user.username)
                    }.onFailure { err ->
                        errorMessage = err.localizedMessage ?: "Google Sign-In failed."
                    }
                } else {
                    isLoading = false
                    errorMessage = "Unexpected credential response."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                isLoading = false
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                isLoading = false
                errorMessage = e.localizedMessage ?: "Google Sign-In failed."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Editorial Header
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LithfitLogoMark(size = 56.dp, cornerRadius = 16.dp)
                LithfitWordmark(
                    textColor = MaterialTheme.colorScheme.onBackground,
                    fontSizeSp = 22f,
                    showAttribution = false
                )
                Text(
                    text = "Your style. Your way.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Welcome back to LITHFIT.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Editorial Auth Card
            Surface(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                tonalElevation = 1.dp
            ) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                    },
                    label = "auth_screen_route"
                ) { activeRoute ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (activeRoute == AuthScreenRoute.PASSWORD_RECOVERY || activeRoute == AuthScreenRoute.OTP_VERIFY) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        route = if (activeRoute == AuthScreenRoute.OTP_VERIFY) {
                                            AuthScreenRoute.REGISTER
                                        } else {
                                            AuthScreenRoute.LOGIN
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                                Text(
                                    text = if (activeRoute == AuthScreenRoute.OTP_VERIFY) "Verify Phone Number" else "Reset Password",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val onLogin = activeRoute == AuthScreenRoute.LOGIN
                                LithfitPillButton(
                                    text = "Sign In",
                                    selected = onLogin,
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        route = AuthScreenRoute.LOGIN
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_login_button")
                                )
                                LithfitPillButton(
                                    text = "Create Account",
                                    selected = !onLogin,
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        route = AuthScreenRoute.REGISTER
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_register_button")
                                )
                            }
                        }

                        errorMessage?.let { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        infoMessage?.let { msg ->
                            Surface(
                                color = SageSuccess.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SageSuccess.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SageSuccess,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        when (activeRoute) {
                            AuthScreenRoute.LOGIN -> {
                                OutlinedTextField(
                                    value = loginIdentifier,
                                    onValueChange = { loginIdentifier = it.take(120) },
                                    label = { Text("Username, Email, or Phone") },
                                    placeholder = { Text("e.g. @alex_styles or email") },
                                    leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_username_input")
                                )

                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = { loginPassword = it.take(128) },
                                    label = { Text("Password") },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                            Icon(
                                                imageVector = if (showLoginPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = if (showLoginPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { submitLogin() }),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_password_input")
                                )

                                LithfitPrimaryButton(
                                    text = "Sign In",
                                    onClick = { submitLogin() },
                                    enabled = !isLoading && loginIdentifier.isNotBlank() && loginPassword.isNotBlank(),
                                    loading = isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_submit_button")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            errorMessage = null
                                            infoMessage = null
                                            recoveryIdentifier = loginIdentifier
                                            route = AuthScreenRoute.PASSWORD_RECOVERY
                                        },
                                        modifier = Modifier.testTag("open_recovery_button")
                                    ) {
                                        Text("Forgot Password?")
                                    }
                                    TextButton(
                                        onClick = {
                                            errorMessage = null
                                            infoMessage = null
                                            route = AuthScreenRoute.REGISTER
                                        }
                                    ) {
                                        Text("Create Account")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "or",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                }

                                LithfitSecondaryButton(
                                    text = "Continue with Google",
                                    onClick = { startGoogleSignIn() },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("google_sign_in_button")
                                )
                            }

                            AuthScreenRoute.REGISTER -> {
                                OutlinedTextField(
                                    value = regDisplayName,
                                    onValueChange = { regDisplayName = it.take(60) },
                                    label = { Text("Display Name") },
                                    leadingIcon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_display_name")
                                )

                                OutlinedTextField(
                                    value = regEmail,
                                    onValueChange = { regEmail = it.take(120) },
                                    label = { Text("Email (optional)") },
                                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_email_input")
                                )

                                OutlinedTextField(
                                    value = regPhone,
                                    onValueChange = { regPhone = it.take(24) },
                                    label = { Text("Phone Number (optional)") },
                                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_phone_input")
                                )

                                OutlinedTextField(
                                    value = regUsername,
                                    onValueChange = { regUsername = it.take(24) },
                                    label = { Text("Unique Username") },
                                    placeholder = { Text("e.g. alex_styles") },
                                    leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                                    singleLine = true,
                                    isError = usernameAvailability is UsernameAvailability.Taken ||
                                        usernameAvailability is UsernameAvailability.Invalid,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Ascii,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_username")
                                )

                                when (val avail = usernameAvailability) {
                                    UsernameAvailability.Idle -> {
                                        Text(
                                            text = "3–24 chars • lowercase letters, numbers, '.' or '_'",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    UsernameAvailability.Checking -> {
                                        Text(
                                            text = "Checking availability...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    is UsernameAvailability.Available -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                tint = SageSuccess,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "@${avail.normalized} is available",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SageSuccess
                                            )
                                        }
                                    }
                                    is UsernameAvailability.Taken -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "@${avail.normalized} is already taken.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    is UsernameAvailability.Invalid -> {
                                        Text(
                                            text = avail.message,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = regPassword,
                                    onValueChange = { regPassword = it.take(128) },
                                    label = { Text("Password (min 6 chars)") },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                            Icon(
                                                imageVector = if (showRegPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = if (showRegPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_password_input")
                                )

                                OutlinedTextField(
                                    value = regConfirmPassword,
                                    onValueChange = { regConfirmPassword = it.take(128) },
                                    label = { Text("Confirm Password") },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                    visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    isError = regConfirmPassword.isNotEmpty() && regConfirmPassword != regPassword,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { submitSignUp() }),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_confirm_password_input")
                                )

                                val canCompleteRegister = regDisplayName.isNotBlank() &&
                                    usernameAvailability is UsernameAvailability.Available &&
                                    regPassword.length >= 6 &&
                                    regConfirmPassword == regPassword &&
                                    !isLoading

                                LithfitPrimaryButton(
                                    text = "Create Account",
                                    onClick = { submitSignUp(verifyPhoneFirst = false) },
                                    enabled = canCompleteRegister,
                                    loading = isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_continue_button")
                                )

                                if (regPhone.isNotBlank() && canCompleteRegister) {
                                    LithfitSecondaryButton(
                                        text = "Verify Phone via OTP & Create",
                                        icon = Icons.Outlined.Sms,
                                        onClick = { submitSignUp(verifyPhoneFirst = true) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "or",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                }

                                LithfitSecondaryButton(
                                    text = "Sign Up with Google",
                                    onClick = { startGoogleSignIn() },
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            AuthScreenRoute.OTP_VERIFY -> {
                                Text(
                                    text = "Enter the 6-digit verification code for ${regPhone.trim()}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Live carrier SMS dispatch requires Firebase Phone Auth activation in Firebase Console. For local device verification, use code $generatedOtpPreview.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                OutlinedTextField(
                                    value = otpCodeInput,
                                    onValueChange = { ch -> otpCodeInput = ch.filter { it.isDigit() }.take(6) },
                                    label = { Text("6-Digit Verification Code") },
                                    leadingIcon = { Icon(Icons.Outlined.Sms, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("otp_code_input")
                                )
                                LithfitPrimaryButton(
                                    text = "Verify & Create Account",
                                    onClick = {
                                        if (otpCodeInput == generatedOtpPreview) {
                                            completeRegistration()
                                        } else {
                                            errorMessage = "Incorrect verification code. Please check and retry."
                                        }
                                    },
                                    enabled = otpCodeInput.length == 6 && !isLoading,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                LithfitSecondaryButton(
                                    text = if (otpCooldownSec > 0) "Resend Code (${otpCooldownSec}s)" else "Resend Code",
                                    enabled = otpCooldownSec == 0,
                                    onClick = {
                                        generatedOtpPreview = (100000..999999).random().toString()
                                        otpCooldownSec = 30
                                        errorMessage = null
                                        infoMessage = "New verification code ready: $generatedOtpPreview"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            AuthScreenRoute.PASSWORD_RECOVERY -> {
                                Text(
                                    text = "Enter your username, email, or phone and set a new password (min 6 characters).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = recoveryIdentifier,
                                    onValueChange = { recoveryIdentifier = it.take(120) },
                                    label = { Text("Username, Email, or Phone") },
                                    leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("recovery_username_input")
                                )
                                OutlinedTextField(
                                    value = recoveryNewPassword,
                                    onValueChange = { recoveryNewPassword = it.take(128) },
                                    label = { Text("New Password") },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showRecoveryPassword = !showRecoveryPassword }) {
                                            Icon(
                                                imageVector = if (showRecoveryPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    visualTransformation = if (showRecoveryPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("recovery_password_input")
                                )
                                LithfitPrimaryButton(
                                    text = "Update Password",
                                    onClick = {
                                        if (isLoading) return@LithfitPrimaryButton
                                        isLoading = true
                                        errorMessage = null
                                        infoMessage = null
                                        val res = authRepository.resetLocalPassword(
                                            context = context,
                                            rawIdentifier = recoveryIdentifier,
                                            newPassword = recoveryNewPassword
                                        )
                                        isLoading = false
                                        if (res.isSuccess) {
                                            loginIdentifier = recoveryIdentifier.trim()
                                            loginPassword = ""
                                            infoMessage = "Password updated. You can sign in now."
                                            route = AuthScreenRoute.LOGIN
                                        } else {
                                            errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Could not reset password."
                                        }
                                    },
                                    enabled = recoveryIdentifier.isNotBlank() && recoveryNewPassword.length >= 6 && !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("send_recovery_button")
                                )
                                LithfitSecondaryButton(
                                    text = "Back to Sign In",
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        route = AuthScreenRoute.LOGIN
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.widthIn(max = 520.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClastadLogoMark(size = 18.dp, cornerRadius = 5.dp)
                Text(
                    text = "From CLASTAD • Private by default • Protected credentials",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
