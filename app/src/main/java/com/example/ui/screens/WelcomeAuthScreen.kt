package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.data.repository.UsernameAvailability
import com.example.ui.components.ClastadLogoMark
import com.example.ui.components.LithfitLogoMark
import com.example.ui.components.LithfitWordmark
import com.example.ui.theme.SageSuccess
import kotlinx.coroutines.delay

enum class AuthScreenRoute {
    LOGIN,
    REGISTER,
    PASSWORD_RECOVERY
}

@Composable
fun WelcomeAuthScreen(
    authRepository: AuthRepository,
    onAuthenticated: (pendingDisplayName: String?, pendingUsername: String?) -> Unit
) {
    val context = LocalContext.current

    var route by remember { mutableStateOf(AuthScreenRoute.LOGIN) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Login state
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Sign Up state
    var regDisplayName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var showRegPassword by remember { mutableStateOf(false) }
    var usernameAvailability by remember { mutableStateOf<UsernameAvailability>(UsernameAvailability.Idle) }

    // Local Password Reset state
    var recoveryUsername by remember { mutableStateOf("") }
    var recoveryNewPassword by remember { mutableStateOf("") }
    var showRecoveryPassword by remember { mutableStateOf(false) }

    BackHandler(enabled = route != AuthScreenRoute.LOGIN) {
        errorMessage = null
        infoMessage = null
        route = AuthScreenRoute.LOGIN
    }

    // Live debounced username validation + availability check during Sign Up
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
        val res = authRepository.signInWithUsernamePassword(context, loginUsername, loginPassword)
        isLoading = false
        res.onSuccess { user ->
            onAuthenticated(user.displayName, user.username)
        }.onFailure { err ->
            errorMessage = err.localizedMessage ?: "Sign in failed."
        }
    }

    fun submitSignUp() {
        if (isLoading) return
        if (regPassword != regConfirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }
        isLoading = true
        errorMessage = null
        infoMessage = null
        val res = authRepository.registerWithUsernamePassword(
            context = context,
            displayName = regDisplayName,
            rawUsername = regUsername,
            password = regPassword
        )
        isLoading = false
        res.onSuccess { user ->
            sendNewAccountNotificationEmail(
                context = context,
                displayName = user.displayName,
                username = user.username
            )
            onAuthenticated(user.displayName, user.username)
        }.onFailure { err ->
            errorMessage = err.localizedMessage ?: "Could not create account."
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_editorial_diverse_trio),
            contentDescription = "LITHFIT editorial fashion showcase",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66101014),
                            Color(0x44101014),
                            Color(0xF0101014)
                        )
                    )
                )
        )

        // Top Brand Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LithfitLogoMark(size = 38.dp)
            LithfitWordmark(
                textColor = Color(0xFFFAF8F5),
                fontSizeSp = 20f,
                showAttribution = true
            )
        }

        // Bottom Editorial Auth Sheet with smooth Username + Password Sign In / Sign Up
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
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
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeRoute == AuthScreenRoute.PASSWORD_RECOVERY) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    route = AuthScreenRoute.LOGIN
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Text(
                                text = "Reset Password",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    } else {
                        // Clean Sign In / Sign Up switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val onLogin = activeRoute == AuthScreenRoute.LOGIN
                            Button(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    route = AuthScreenRoute.LOGIN
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (onLogin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (onLogin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("open_login_button")
                            ) {
                                Text("Sign In", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    route = AuthScreenRoute.REGISTER
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!onLogin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (!onLogin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("open_register_button")
                            ) {
                                Text("Sign Up", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    errorMessage?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
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
                            color = SageSuccess.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
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
                            Text(
                                text = "Your style. Your world.",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sign in with your LITHFIT username and password stored safely on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = loginUsername,
                                onValueChange = { loginUsername = it.take(24) },
                                label = { Text("Username") },
                                placeholder = { Text("e.g. alex_styles") },
                                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Next
                                ),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input")
                            )

                            Button(
                                onClick = { submitLogin() },
                                enabled = !isLoading && loginUsername.isNotBlank() && loginPassword.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("login_submit_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(
                                        text = "Sign In",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        recoveryUsername = loginUsername
                                        route = AuthScreenRoute.PASSWORD_RECOVERY
                                    },
                                    modifier = Modifier.testTag("open_recovery_button")
                                ) {
                                    Text("Reset password")
                                }
                                TextButton(
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        route = AuthScreenRoute.REGISTER
                                    }
                                ) {
                                    Text("New here? Sign Up")
                                }
                            }
                        }

                        AuthScreenRoute.REGISTER -> {
                            Text(
                                text = "Create your wardrobe account",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "Quick local setup — pick a handle and password to save your closet, fits, and style preferences.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = regDisplayName,
                                onValueChange = { regDisplayName = it.take(60) },
                                label = { Text("Display Name") },
                                leadingIcon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_display_name")
                            )

                            OutlinedTextField(
                                value = regUsername,
                                onValueChange = { regUsername = it.take(24) },
                                label = { Text("Username") },
                                placeholder = { Text("e.g. alex_styles") },
                                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                                singleLine = true,
                                isError = usernameAvailability is UsernameAvailability.Taken ||
                                    usernameAvailability is UsernameAvailability.Invalid,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Next
                                ),
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
                                            text = "@${avail.normalized} is already registered on this device.",
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_confirm_password_input")
                            )

                            val canCompleteRegister = regDisplayName.isNotBlank() &&
                                usernameAvailability is UsernameAvailability.Available &&
                                regPassword.length >= 6 &&
                                regConfirmPassword == regPassword &&
                                !isLoading

                            Button(
                                onClick = { submitSignUp() },
                                enabled = canCompleteRegister,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("register_continue_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Create Account", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        AuthScreenRoute.PASSWORD_RECOVERY -> {
                            Text(
                                text = "Enter your LITHFIT username and choose a new password (at least 6 characters).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = recoveryUsername,
                                onValueChange = { recoveryUsername = it.take(24) },
                                label = { Text("Username") },
                                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                                singleLine = true,
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recovery_password_input")
                            )
                            Button(
                                onClick = {
                                    if (isLoading) return@Button
                                    isLoading = true
                                    errorMessage = null
                                    infoMessage = null
                                    val res = authRepository.resetLocalPassword(
                                        context = context,
                                        rawUsername = recoveryUsername,
                                        newPassword = recoveryNewPassword
                                    )
                                    isLoading = false
                                    if (res.isSuccess) {
                                        loginUsername = recoveryUsername.trim().lowercase().removePrefix("@")
                                        loginPassword = ""
                                        infoMessage = "Password updated for @$loginUsername. You can sign in now."
                                        route = AuthScreenRoute.LOGIN
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Could not reset password."
                                    }
                                },
                                enabled = recoveryUsername.isNotBlank() && recoveryNewPassword.length >= 6 && !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("send_recovery_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Update Password")
                            }
                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    route = AuthScreenRoute.LOGIN
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Back to Sign In")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Private by default",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Private on-device account • Encrypted credentials • Free core access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClastadLogoMark(size = 18.dp, cornerRadius = 5.dp)
                            Text(
                                text = "Developed by CLASTAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sendNewAccountNotificationEmail(
    context: Context,
    displayName: String,
    username: String
) {
    val notifyRecipient = runCatching {
        context.getString(R.string.clastad_notify_email)
    }.getOrDefault("livechad67@gmail.com")
    val subject = "New LITHFIT Member Joined: @$username"
    val body = buildString {
        appendLine("A new member joined LITHFIT by CLASTAD.")
        appendLine("Display Name: $displayName")
        appendLine("Username: @$username")
        appendLine("Joined At: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())}")
    }
    runCatching {
        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$notifyRecipient")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(notifyRecipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(mailIntent, "Send LITHFIT Welcome Notification").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
