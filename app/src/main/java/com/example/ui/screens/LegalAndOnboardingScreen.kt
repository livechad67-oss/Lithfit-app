package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GarmentCategory
import com.example.data.model.LegalDocuments
import com.example.data.model.LithfitConstants
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.UsernameAvailability
import com.example.ui.components.ColorSwatchChip
import com.example.ui.components.LegalDocumentDialog
import com.example.ui.components.LithfitWordmark
import com.example.ui.theme.SageSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegalAndOnboardingScreen(
    userId: String,
    initialDisplayName: String,
    initialUsername: String? = null,
    initialEmail: String?,
    initialAvatarUrl: String?,
    onComplete: (
        cameraConsent: Boolean,
        photoConsent: Boolean,
        locationConsent: Boolean,
        notificationsConsent: Boolean,
        profile: UserProfile
    ) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    var step by remember { mutableIntStateOf(0) }
    var privacyAccepted by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var guidelinesAccepted by remember { mutableStateOf(false) }
    var openDocTitle by remember { mutableStateOf<String?>(null) }
    var openDocBody by remember { mutableStateOf("") }

    var cameraGranted by remember { mutableStateOf(false) }
    var notificationsGranted by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraGranted = granted }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    var displayName by remember { mutableStateOf(initialDisplayName.ifBlank { "Lithfit Stylist" }) }
    var username by remember {
        mutableStateOf(
            initialUsername?.takeIf { it.isNotBlank() } ?: run {
                val base = initialDisplayName.lowercase().replace(Regex("[^a-z0-9]"), "_").trim('_').ifBlank { "lithfit" }
                val suffix = userId.take(4).lowercase().replace(Regex("[^a-z0-9]"), "x")
                "${base.take(16)}_$suffix".take(24)
            }
        )
    }
    var usernameAvailability by remember { mutableStateOf<UsernameAvailability>(UsernameAvailability.Idle) }
    var submitError by remember { mutableStateOf<String?>(null) }

    var cityLabel by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("USD") }
    var selectedAvatarStyle by remember { mutableStateOf(LithfitConstants.AVATAR_STYLES.first()) }
    var selectedFocus by remember { mutableStateOf("All") }
    var selectedGender by remember { mutableStateOf(LithfitConstants.GENDER_OPTIONS.first()) }
    var selectedAge by remember { mutableStateOf(LithfitConstants.AGE_RANGES[1]) }
    var selectedHeight by remember { mutableStateOf(LithfitConstants.HEIGHT_OPTIONS[2]) }
    var selectedBodySize by remember { mutableStateOf(LithfitConstants.BODY_SIZES[2]) }
    var selectedBudget by remember { mutableStateOf("Balanced everyday") }
    var under18Account by remember { mutableStateOf(false) }
    val selectedStyles = remember { mutableStateListOf("Streetwear", "Y2K", "Minimalist") }
    val selectedColors = remember { mutableStateListOf("Black", "Cream", "Charcoal") }
    val selectedWeatherStyles = remember { mutableStateListOf("Warm", "Cool") }
    val selectedOccasions = remember { mutableStateListOf("Casual day", "Weekend") }
    val selectedCategories = remember { mutableStateListOf("TOPS", "TROUSERS", "JACKETS", "SHOES") }
    var isSaving by remember { mutableStateOf(false) }

    // Validate unique username format + availability when on profile step
    LaunchedEffect(step, username) {
        if (step != 2) return@LaunchedEffect
        val trimmed = username.trim()
        val validation = authRepository.validateUsername(trimmed)
        if (!validation.isValid) {
            usernameAvailability = UsernameAvailability.Invalid(validation.errorMessage ?: "Invalid username.")
            return@LaunchedEffect
        }
        usernameAvailability = UsernameAvailability.Checking
        delay(180)
        usernameAvailability = authRepository.checkUsernameAvailability(context, trimmed)
    }

    openDocTitle?.let { title ->
        LegalDocumentDialog(
            title = title,
            bodyText = openDocBody,
            onDismiss = { openDocTitle = null }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LithfitWordmark(fontSizeSp = 18f, showAttribution = true)
                TextButton(onClick = onSignOut) {
                    Text("Switch account")
                }
            }

            LinearProgressIndicator(
                progress = { (step + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )

            when (step) {
                0 -> {
                    Text(
                        text = "Privacy, Terms & Safety",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Before creating your LITHFIT wardrobe profile, please review and accept our policies. Optional device permissions are kept completely separate on the next step.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    PolicyRowCard(
                        title = "1. Privacy Policy (v${LegalDocuments.PRIVACY_VERSION})",
                        summary = "Explains how wardrobe items, photos, AI styling requests, and optional city weather are stored privately.",
                        checked = privacyAccepted,
                        onCheckedChange = { privacyAccepted = it },
                        onRead = {
                            openDocTitle = "LITHFIT Privacy Policy"
                            openDocBody = LegalDocuments.privacyPolicyText
                        },
                        testTag = "privacy_checkbox"
                    )

                    PolicyRowCard(
                        title = "2. Terms of Service (v${LegalDocuments.TERMS_VERSION})",
                        summary = "Covers free core access, intellectual property, AI outfit preview disclaimers, and marketplace limits.",
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        onRead = {
                            openDocTitle = "LITHFIT Terms of Service"
                            openDocBody = LegalDocuments.termsOfServiceText
                        },
                        testTag = "terms_checkbox"
                    )

                    PolicyRowCard(
                        title = "3. Community Guidelines (v${LegalDocuments.GUIDELINES_VERSION})",
                        summary = "Body-neutral styling, copyright respect, no brand impersonation, and under-18 safety protections.",
                        checked = guidelinesAccepted,
                        onCheckedChange = { guidelinesAccepted = it },
                        onRead = {
                            openDocTitle = "LITHFIT Community Guidelines"
                            openDocBody = LegalDocuments.communityGuidelinesText
                        },
                        testTag = "guidelines_checkbox"
                    )

                    Button(
                        onClick = { step = 1 },
                        enabled = privacyAccepted && termsAccepted && guidelinesAccepted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("accept_legal_continue_button")
                    ) {
                        Text("Accept & Continue")
                    }
                }

                1 -> {
                    Text(
                        text = "Optional Permissions",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "All permissions below are optional. You can decline any of them and still use LITHFIT's wardrobe, avatar studio, AI stylist, and discovery tools.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    PermissionExplainCard(
                        icon = Icons.Outlined.CameraAlt,
                        title = "Camera Access",
                        explanation = "Used only when you tap 'Take Photo' to capture garments for your private digital wardrobe.",
                        granted = cameraGranted,
                        actionLabel = if (cameraGranted) "Granted" else "Allow Camera",
                        onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) }
                    )

                    PermissionExplainCard(
                        icon = Icons.Outlined.PhotoLibrary,
                        title = "Photo Picker (Zero Broad Storage Permission)",
                        explanation = "LITHFIT uses Android's private Photo Picker so you only share the exact clothing photos you select — no broad gallery permission is ever requested.",
                        granted = true,
                        actionLabel = "Private Picker Active",
                        onRequest = {}
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionExplainCard(
                            icon = Icons.Outlined.Notifications,
                            title = "Notifications",
                            explanation = "Optional outfit reminders and weather alerts. Disabled unless you opt in.",
                            granted = notificationsGranted,
                            actionLabel = if (notificationsGranted) "Granted" else "Allow Notifications",
                            onRequest = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { step = 2 },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("skip_permissions_button")
                        ) {
                            Text("Skip for Now")
                        }
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("permissions_continue_button")
                        ) {
                            Text("Continue")
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Your Style Profile",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Personalize LITHFIT AI and your discovery feed. Personal photos are never required.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(60) },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_display_name")
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.take(24) },
                        label = { Text("Unique Username") },
                        prefix = { Text("@") },
                        singleLine = true,
                        isError = usernameAvailability is UsernameAvailability.Taken ||
                            usernameAvailability is UsernameAvailability.Invalid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_username")
                    )
                    when (val avail = usernameAvailability) {
                        is UsernameAvailability.Available -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SageSuccess, modifier = Modifier.size(16.dp))
                                Text("@${avail.normalized} is available", style = MaterialTheme.typography.labelSmall, color = SageSuccess)
                            }
                        }
                        is UsernameAvailability.Taken -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Text("@${avail.normalized} is already taken. Choose another username.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        is UsernameAvailability.Invalid -> {
                            Text(avail.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                        else -> Unit
                    }

                    OutlinedTextField(
                        value = cityLabel,
                        onValueChange = { cityLabel = it },
                        label = { Text("Preferred City for Weather Styling (Optional)") },
                        placeholder = { Text("e.g. London, New York, Tokyo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.VerifiedUser, contentDescription = null)
                                Column {
                                    Text("Under-18 Protected Mode", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Enforces private profile, blocks public location sharing, and disables stranger contact.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = under18Account,
                                onCheckedChange = { under18Account = it }
                            )
                        }
                    }

                    Text("Gender", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.GENDER_OPTIONS.forEach { g ->
                            FilterChip(
                                selected = selectedGender == g,
                                onClick = { selectedGender = g },
                                label = { Text(g) },
                                modifier = Modifier.testTag("onboarding_gender_${g.lowercase().replace(Regex("[^a-z0-9]"), "_")}")
                            )
                        }
                    }

                    Text("Age Range", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.AGE_RANGES.forEach { a ->
                            FilterChip(
                                selected = selectedAge == a,
                                onClick = {
                                    selectedAge = a
                                    if (a.startsWith("13")) under18Account = true
                                },
                                label = { Text(a) }
                            )
                        }
                    }

                    Text("Height", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.HEIGHT_OPTIONS.forEach { h ->
                            FilterChip(
                                selected = selectedHeight == h,
                                onClick = { selectedHeight = h },
                                label = { Text(h) }
                            )
                        }
                    }

                    Text("Body Size & Fit", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.BODY_SIZES.forEach { sz ->
                            FilterChip(
                                selected = selectedBodySize == sz,
                                onClick = { selectedBodySize = sz },
                                label = { Text(sz) }
                            )
                        }
                    }

                    Text("Inspiration Focus", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.AUDIENCE_FILTERS.forEach { focus ->
                            FilterChip(
                                selected = selectedFocus == focus,
                                onClick = { selectedFocus = focus },
                                label = { Text(focus) }
                            )
                        }
                    }

                    Text("Lookbook Style Persona", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.AVATAR_STYLES.forEach { avatar ->
                            FilterChip(
                                selected = selectedAvatarStyle == avatar,
                                onClick = { selectedAvatarStyle = avatar },
                                label = { Text(avatar) }
                            )
                        }
                    }

                    Text("Favourite Fashion Cultures", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.STYLE_CATEGORIES.forEach { style ->
                            val picked = style in selectedStyles
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    if (picked) selectedStyles.remove(style) else selectedStyles.add(style)
                                },
                                label = { Text(style) }
                            )
                        }
                    }

                    Text("Favourite Colours", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LithfitConstants.PREFERRED_COLORS.forEach { color ->
                            val picked = color in selectedColors
                            ColorSwatchChip(
                                colorName = color,
                                selected = picked,
                                onClick = {
                                    if (picked) selectedColors.remove(color) else selectedColors.add(color)
                                }
                            )
                        }
                    }

                    Text("Weather Styling", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.WEATHER_STYLES.forEach { weather ->
                            val picked = weather in selectedWeatherStyles
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    if (picked) selectedWeatherStyles.remove(weather) else selectedWeatherStyles.add(weather)
                                },
                                label = { Text(weather) }
                            )
                        }
                    }

                    Text("Key Occasions", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.OCCASIONS.forEach { occ ->
                            val picked = occ in selectedOccasions
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    if (picked) selectedOccasions.remove(occ) else selectedOccasions.add(occ)
                                },
                                label = { Text(occ) }
                            )
                        }
                    }

                    Text("Preferred Categories", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GarmentCategory.entries.filter { it != GarmentCategory.OTHER }.forEach { cat ->
                            val picked = cat.name in selectedCategories
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    if (picked) selectedCategories.remove(cat.name) else selectedCategories.add(cat.name)
                                },
                                label = { Text(cat.label) }
                            )
                        }
                    }

                    Text("Budget Comfort", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.BUDGET_PREFERENCES.forEach { budget ->
                            FilterChip(
                                selected = selectedBudget == budget,
                                onClick = { selectedBudget = budget },
                                label = { Text(budget) }
                            )
                        }
                    }

                    Text("Currency", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LithfitConstants.CURRENCIES.forEach { cur ->
                            FilterChip(
                                selected = selectedCurrency == cur,
                                onClick = { selectedCurrency = cur },
                                label = { Text(cur) }
                            )
                        }
                    }

                    submitError?.let { msg ->
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

                    val canSubmit = !isSaving && displayName.isNotBlank() &&
                        authRepository.validateUsername(username).isValid &&
                        usernameAvailability !is UsernameAvailability.Taken

                    Button(
                        onClick = {
                            if (!canSubmit) return@Button
                            isSaving = true
                            submitError = null
                            scope.launch {
                                val normalizedHandle = authRepository.validateUsername(username).normalized
                                val availability = authRepository.checkUsernameAvailability(context, normalizedHandle)
                                if (availability is UsernameAvailability.Taken) {
                                    isSaving = false
                                    submitError = "@$normalizedHandle is already taken on this device."
                                    return@launch
                                }
                                val profile = UserProfile(
                                    userId = userId,
                                    displayName = displayName.trim().ifBlank { "Lithfit Member" },
                                    username = normalizedHandle,
                                    email = initialEmail,
                                    avatarUrl = initialAvatarUrl,
                                    avatarStyle = selectedAvatarStyle,
                                    privateProfile = true,
                                    minorAccount = under18Account,
                                    preferredStyles = selectedStyles.toList(),
                                    preferredColors = selectedColors.toList(),
                                    preferredCategories = selectedCategories.toList(),
                                    preferredWeatherStyles = selectedWeatherStyles.toList(),
                                    preferredOccasions = selectedOccasions.toList(),
                                    budgetPreference = selectedBudget,
                                    inspirationFocus = selectedFocus,
                                    preferredCurrency = selectedCurrency,
                                    locationLabel = cityLabel.trim().ifBlank { null },
                                    themeMode = ThemeMode.LIGHT.name,
                                    notificationsEnabled = notificationsGranted,
                                    locationAccessPreference = false,
                                    gender = selectedGender,
                                    age = selectedAge,
                                    height = selectedHeight,
                                    bodySize = selectedBodySize
                                )
                                onComplete(
                                    cameraGranted,
                                    true,
                                    false,
                                    notificationsGranted,
                                    profile
                                )
                            }
                        },
                        enabled = canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("finish_onboarding_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Enter LITHFIT")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PolicyRowCard(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRead: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCheckedChange(!checked) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.testTag(testTag)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onRead, modifier = Modifier.align(Alignment.End)) {
                Text("Read Full Document")
            }
        }
    }
}

@Composable
private fun PermissionExplainCard(
    icon: ImageVector,
    title: String,
    explanation: String,
    granted: Boolean,
    actionLabel: String,
    onRequest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (granted) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                OutlinedButton(onClick = onRequest) {
                    Text(actionLabel)
                }
            }
        }
    }
}
