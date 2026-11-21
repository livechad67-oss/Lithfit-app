package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.core.ImageStorageHelper
import com.example.data.model.LegalDocuments
import com.example.data.model.LithfitConstants
import com.example.data.model.Outfit
import com.example.data.model.ThemeMode
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.ui.components.ClastadLogoMark
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.LegalDocumentDialog
import com.example.ui.components.LithfitWordmark
import com.example.ui.components.ReportContentDialog
import com.example.ui.viewmodel.LithfitViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: LithfitViewModel,
    onBack: () -> Unit,
    onOpenNotificationsInbox: () -> Unit,
    onOpenStyleRadarForOutfit: (Outfit) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: suspend () -> Result<Unit>,
    showBackButton: Boolean = true
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val clastadUrl = stringResource(id = R.string.clastad_url)
    val clastadDomain = stringResource(id = R.string.clastad_domain)
    var copiedClastadUrl by remember { mutableStateOf(false) }

    val authRepo = remember { AuthRepository() }
    val activeAuthUser = remember(viewModel.currentUserId) { authRepo.currentUser(context) }
    val isAuthorizedOwner = activeAuthUser?.email.equals(LithfitConstants.OWNER_NOTIFY_EMAIL, ignoreCase = true)

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val wardrobeState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val outfitsState by viewModel.outfits.collectAsStateWithLifecycle()
    val collections by viewModel.outfitCollections.collectAsStateWithLifecycle()
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val wardrobeInsights by viewModel.wardrobeInsights.collectAsStateWithLifecycle()
    val ownerNotifications by viewModel.ownerNotifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val wardrobeCount = (wardrobeState as? UiState.Success)?.data?.size ?: 0
    val savedOutfits = (outfitsState as? UiState.Success)?.data.orEmpty()

    var showEditPreferences by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showCreateCollection by remember { mutableStateOf(false) }
    var showDataExport by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var openDocTitle by remember { mutableStateOf<String?>(null) }
    var openDocBody by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.saveProfilePhoto(context, uri)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        if (saved) pendingCameraUri?.let { viewModel.saveProfilePhoto(context, it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (uri, _) = ImageStorageHelper.createCameraCaptureUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    val launchCameraCapture = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val (uri, _) = ImageStorageHelper.createCameraCaptureUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    openDocTitle?.let { title ->
        LegalDocumentDialog(title = title, bodyText = openDocBody, onDismiss = { openDocTitle = null })
    }

    if (showReportDialog) {
        ReportContentDialog(
            targetTitle = "Copyright, Seller, or Safety Report",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                viewModel.submitReport("GENERAL_SAFETY", "settings_portal", reason, details)
                showReportDialog = false
            }
        )
    }

    if (showPhotoDialog) {
        PrivateProfilePhotoDialog(
            profile = profile,
            onPickPhoto = {
                showPhotoDialog = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onTakePhoto = {
                showPhotoDialog = false
                launchCameraCapture()
            },
            onRemovePhoto = {
                viewModel.deleteProfilePhoto()
                showPhotoDialog = false
            },
            onDismiss = { showPhotoDialog = false }
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            isGoogleSignedIn = activeAuthUser?.googleSignedIn == true,
            onDismiss = { showChangePassword = false },
            onSubmit = { cur, next ->
                authRepo.changePasswordForSignedInUser(context, cur, next)
            }
        )
    }

    if (showCreateCollection) {
        var colName by remember { mutableStateOf("") }
        var colDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateCollection = false },
            title = { Text("New Outfit Collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = colName,
                        onValueChange = { colName = it },
                        label = { Text("Collection Name (e.g. Autumn Capsule)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = colDesc,
                        onValueChange = { colDesc = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = { TextButton(onClick = { showCreateCollection = false }) { Text("Cancel") } },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createCollection(colName.trim(), colDesc.trim().ifBlank { null })
                        showCreateCollection = false
                    },
                    enabled = colName.isNotBlank()
                ) { Text("Create") }
            }
        )
    }

    if (showDataExport) {
        val summary = remember(wardrobeCount, savedOutfits.size, savedItems.size, profile) {
            viewModel.buildUserDataExportSummary()
        }
        AlertDialog(
            onDismissRequest = { showDataExport = false },
            title = { Text("Personal Data Export") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(summary, style = MaterialTheme.typography.bodySmall)
                }
            },
            dismissButton = { TextButton(onClick = { showDataExport = false }) { Text("Close") } },
            confirmButton = {
                Button(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "LITHFIT Personal Data Export")
                        putExtra(Intent.EXTRA_TEXT, summary)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export LITHFIT Data"))
                    showDataExport = false
                }) { Text("Share / Save Copy") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
            title = { Text("Delete LITHFIT Account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This permanently deletes your private wardrobe, saved outfits, AI stylist chats, and profile from LITHFIT.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    deleteError?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }, enabled = !isDeleting) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isDeleting) return@Button
                        isDeleting = true
                        deleteError = null
                        scope.launch {
                            val res = onDeleteAccount()
                            isDeleting = false
                            if (res.isFailure) {
                                deleteError = res.exceptionOrNull()?.localizedMessage ?: "Account deletion failed."
                            } else {
                                showDeleteConfirm = false
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Permanently Delete")
                    }
                }
            }
        )
    }

    if (showEditPreferences && profile != null) {
        EditStylePreferencesDialog(
            profile = profile,
            onDismiss = { showEditPreferences = false },
            onSave = { updated ->
                viewModel.updateProfile(updated)
                showEditPreferences = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("overlay_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            }
            Text(
                text = if (showBackButton) "Settings" else "Profile & Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. MY ACCOUNT
            item {
                val localPhotoFile = remember(profile?.profilePhotoPath) {
                    profile?.profilePhotoPath?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
                }
                SettingsCard {
                    Text("My Account", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { showPhotoDialog = true }
                                .testTag("profile_photo_button")
                        ) {
                            when {
                                localPhotoFile != null -> AsyncImage(
                                    model = localPhotoFile,
                                    contentDescription = "Private profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                !profile?.avatarUrl.isNullOrBlank() -> AsyncImage(
                                    model = profile?.avatarUrl,
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (profile?.displayName?.firstOrNull() ?: 'L').uppercaseChar().toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile?.displayName ?: "LITHFIT Member", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "@${profile?.username ?: "member"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            profile?.email?.takeIf { it.isNotBlank() }?.let { email ->
                                Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        OutlinedButton(
                            onClick = { showEditPreferences = true },
                            enabled = profile != null,
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BadgePill(
                            icon = Icons.Outlined.Lock,
                            text = if (profile?.privateProfile != false) "Private Profile" else "Custom Privacy"
                        )
                        if (profile?.minorAccount == true) {
                            BadgePill(icon = Icons.Outlined.VerifiedUser, text = "Under-18 Protected")
                        }
                        profile?.avatarStyle?.let { BadgePill(icon = null, text = "$it Studio") }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ProfileStatBadge(value = wardrobeCount.toString(), label = "Garments")
                        ProfileStatBadge(value = savedOutfits.size.toString(), label = "Outfits")
                        ProfileStatBadge(value = savedItems.size.toString(), label = "Saved")
                        ProfileStatBadge(value = wardrobeInsights.totalWearsLogged.toString(), label = "Wears")
                    }
                }
            }

            // 2. STYLE PREFERENCES
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Style Preferences", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = { showEditPreferences = true },
                            enabled = profile != null,
                            modifier = Modifier.testTag("edit_style_preferences_button")
                        ) { Text("Edit All") }
                    }
                    if (profile != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PreferenceTagPill("Focus: ${profile.inspirationFocus}")
                            if (profile.gender.isNotBlank()) PreferenceTagPill("Gender: ${profile.gender}")
                            if (profile.age.isNotBlank()) PreferenceTagPill("Age: ${profile.age}")
                            if (profile.height.isNotBlank()) PreferenceTagPill("Height: ${profile.height}")
                            if (profile.bodySize.isNotBlank()) PreferenceTagPill("Size: ${profile.bodySize}")
                            PreferenceTagPill("${profile.budgetPreference} • ${profile.preferredCurrency}")
                        }
                        val allTags = (profile.preferredStyles + profile.personalStyleTags + profile.preferredSilhouettes + profile.lifestyleTags).distinct()
                        if (allTags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allTags.take(12).forEach { PreferenceTagPill(it) }
                            }
                        }
                        if (profile.preferredColors.isNotEmpty() || profile.avoidColors.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profile.preferredColors.forEach { col ->
                                    ColorBadgePill(col)
                                }
                                profile.avoidColors.forEach { col ->
                                    ColorBadgePill("Avoid: $col", colorRef = col)
                                }
                            }
                        }
                    }
                }
            }

            // 3. SAVED LOOKS & COLLECTIONS
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("Collections & Saved Outfits", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = { showCreateCollection = true }) { Text("+ Collection") }
                    }
                    if (collections.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            collections.forEach { PreferenceTagPill(it.name) }
                        }
                    }
                    if (savedOutfits.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(savedOutfits.take(8), key = { it.id }) { outfit ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.width(190.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(outfit.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                        Text("${outfit.occasion} • ${outfit.vibe}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        TextButton(
                                            onClick = { onOpenStyleRadarForOutfit(outfit) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) { Text("Explore in Style Radar") }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "Save outfits in Outfits or LITHFIT AI to organize them into collections.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. APPEARANCE
            item {
                SettingsCard {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    val currentTheme = ThemeMode.safeParse(profile?.themeMode)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = currentTheme == mode,
                                onClick = { viewModel.updateThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size)
                            ) { Text(mode.label) }
                        }
                    }
                    if (profile != null) {
                        SettingsSwitchRow(
                            title = "Large Readable Text",
                            subtitle = "Increase body & label typography scale",
                            checked = profile.largeReadableText,
                            testTag = "toggle_large_readable_text",
                            onCheckedChange = { viewModel.updateProfile(profile.copy(largeReadableText = it)) }
                        )
                        SettingsSwitchRow(
                            title = "Reduced Motion",
                            subtitle = "Minimize press scales and transitions",
                            checked = profile.reducedMotion,
                            testTag = "toggle_reduced_motion",
                            onCheckedChange = { viewModel.updateProfile(profile.copy(reducedMotion = it)) }
                        )
                    }
                }
            }

            // 5. NOTIFICATIONS (6 toggles + Inbox + Owner-Only Registration Alerts)
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("Notifications", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(
                            onClick = onOpenNotificationsInbox,
                            modifier = Modifier.testTag("open_notifications_inbox_button")
                        ) {
                            Text(if (unreadCount > 0) "Inbox ($unreadCount)" else "Open Inbox")
                        }
                    }
                    if (profile != null) {
                        SettingsSwitchRow(
                            title = "All Notifications",
                            subtitle = "Master switch for LITHFIT updates",
                            checked = profile.notificationsEnabled,
                            testTag = "toggle_notifications_master",
                            onCheckedChange = { viewModel.updateProfile(profile.copy(notificationsEnabled = it)) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val enabled = profile.notificationsEnabled
                        SettingsSwitchRow("Outfit Recommendations", null, profile.notifOutfitRecs, "toggle_notif_outfit_recs", enabled) {
                            viewModel.updateProfile(profile.copy(notifOutfitRecs = it))
                        }
                        SettingsSwitchRow("Daily Outfit Reminders", null, profile.notifDailyReminders, "toggle_notif_daily", enabled) {
                            viewModel.updateProfile(profile.copy(notifDailyReminders = it))
                        }
                        SettingsSwitchRow("Wardrobe Reminders", null, profile.notifWardrobeReminders, "toggle_notif_wardrobe", enabled) {
                            viewModel.updateProfile(profile.copy(notifWardrobeReminders = it))
                        }
                        SettingsSwitchRow("Security Alerts", null, profile.notifSecurityAlerts, "toggle_notif_security", enabled) {
                            viewModel.updateProfile(profile.copy(notifSecurityAlerts = it))
                        }
                        SettingsSwitchRow("App Updates", null, profile.notifAppUpdates, "toggle_notif_app_updates", enabled) {
                            viewModel.updateProfile(profile.copy(notifAppUpdates = it))
                        }
                        SettingsSwitchRow("Product & Catalogue Updates", null, profile.notifProductUpdates, "toggle_notif_product_updates", enabled) {
                            viewModel.updateProfile(profile.copy(notifProductUpdates = it))
                        }
                    }

                    if (isAuthorizedOwner) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_registration_alerts_section")
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Owner Account Registration Alerts (${ownerNotifications.size})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Visible strictly to ${LithfitConstants.OWNER_NOTIFY_EMAIL}.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (ownerNotifications.isEmpty()) {
                                    Text("No new member sign-ups recorded yet.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    ownerNotifications.take(3).forEach { alert ->
                                        Text("• ${alert.message}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    TextButton(onClick = onOpenNotificationsInbox, contentPadding = PaddingValues(0.dp)) {
                                        Text("View all in Inbox")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. PRIVACY & SECURITY
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("Privacy & Security", style = MaterialTheme.typography.titleMedium)
                    }
                    if (profile != null) {
                        SettingsSwitchRow(
                            title = "Keep Profile Photo Private",
                            subtitle = "Stored strictly on-device for your private account",
                            checked = profile.profilePhotoPrivate,
                            testTag = "toggle_profile_photo_private",
                            onCheckedChange = { viewModel.updateProfile(profile.copy(profilePhotoPrivate = it)) }
                        )
                    }
                    OutlinedButton(
                        onClick = { showChangePassword = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_password_button")
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Change Password")
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Manage Android Permissions")
                    }
                    OutlinedButton(
                        onClick = { showDataExport = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_data_button")
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export Wardrobe & Profile JSON")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            openDocTitle = "LITHFIT Privacy Policy"
                            openDocBody = LegalDocuments.privacyPolicyText
                        }) { Text("Privacy Policy") }
                        TextButton(onClick = {
                            openDocTitle = "LITHFIT Terms of Service"
                            openDocBody = LegalDocuments.termsOfServiceText
                        }) { Text("Terms") }
                        TextButton(onClick = {
                            openDocTitle = "LITHFIT Community Guidelines"
                            openDocBody = LegalDocuments.communityGuidelinesText
                        }) { Text("Guidelines") }
                    }
                    TextButton(onClick = {
                        openDocTitle = "Credits & Attribution"
                        openDocBody = LegalDocuments.creditsText
                    }) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Credits • Founder Jason Prince (CLASTAD)")
                    }
                    OutlinedButton(onClick = { showReportDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Report Copyright or Safety Issue")
                    }
                }
            }

            // 7. APP SETTINGS & CONTACT CLASTAD
            item {
                SettingsCard(modifier = Modifier.testTag("contact_clastad_card")) {
                    Text("App Settings", style = MaterialTheme.typography.titleMedium)
                    if (profile != null) {
                        Text("Language", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LithfitConstants.LANGUAGES.forEach { lang ->
                                FilterChip(
                                    selected = profile.language == lang,
                                    onClick = { viewModel.updateProfile(profile.copy(language = lang)) },
                                    label = { Text(lang) }
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetGuideAndTips() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("replay_guide_button")
                    ) {
                        Icon(Icons.Outlined.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Replay 6-Step App Guide & Reset Tips")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClastadLogoMark(size = 42.dp, cornerRadius = 10.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Contact CLASTAD", style = MaterialTheme.typography.titleSmall)
                            Text("Official site: $clastadDomain", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(clastadUrl))
                                copiedClastadUrl = true
                            },
                            modifier = Modifier.testTag("copy_clastad_url_button")
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (copiedClastadUrl) "Copied" else "Copy")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clastadUrl))) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("visit_clastad_website_button")
                        ) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open $clastadDomain")
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:contact@clastad.xyz")
                                        putExtra(Intent.EXTRA_SUBJECT, "LITHFIT Support / Feedback")
                                    }
                                    context.startActivity(Intent.createChooser(emailIntent, "Contact CLASTAD"))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("email_clastad_button")
                        ) {
                            Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Email")
                        }
                    }
                    Text(
                        text = "LITHFIT Version 1.0 • Developed by CLASTAD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 8. SIGN OUT & DELETE ACCOUNT
            item {
                SettingsCard {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_button")
                    ) {
                        Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Account & Local Data")
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LithfitWordmark(fontSizeSp = 14f, showAttribution = true, centered = true)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    testTag: String,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun ProfileStatBadge(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreferenceTagPill(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
private fun BadgePill(icon: androidx.compose.ui.graphics.vector.ImageVector?, text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ColorBadgePill(label: String, colorRef: String = label) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ColorSwatchDot(colorName = colorRef, size = 12.dp)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
