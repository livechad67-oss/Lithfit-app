package com.example.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import com.example.core.ImageStorageHelper
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.GarmentCategory
import com.example.data.model.LegalDocuments
import com.example.data.model.LithfitConstants
import com.example.data.model.Outfit
import com.example.data.model.ThemeMode
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ClastadLogoMark
import com.example.ui.components.ColorSwatchChip
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.LegalDocumentDialog
import com.example.ui.components.LithfitGarmentImage
import com.example.ui.components.LithfitWordmark
import com.example.ui.components.ReportContentDialog
import com.example.ui.viewmodel.LithfitViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: LithfitViewModel,
    onSignOut: () -> Unit,
    onDeleteAccount: suspend () -> Result<Unit>,
    onOpenStyleRadarForOutfit: (Outfit) -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val clastadUrl = stringResource(id = R.string.clastad_url)
    val clastadDomain = stringResource(id = R.string.clastad_domain)
    var copiedClastadUrl by remember { mutableStateOf(false) }

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val wardrobeState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val outfitsState by viewModel.outfits.collectAsStateWithLifecycle()
    val collections by viewModel.outfitCollections.collectAsStateWithLifecycle()
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val previews by viewModel.generatedPreviews.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val wardrobeCount = (wardrobeState as? UiState.Success)?.data?.size ?: 0
    val savedOutfits = (outfitsState as? UiState.Success)?.data.orEmpty()

    var showEditPreferences by remember { mutableStateOf(false) }
    var showCreateCollection by remember { mutableStateOf(false) }
    var showDataExport by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var openDocTitle by remember { mutableStateOf<String?>(null) }
    var openDocBody by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    openDocTitle?.let { title ->
        LegalDocumentDialog(
            title = title,
            bodyText = openDocBody,
            onDismiss = { openDocTitle = null }
        )
    }

    if (showReportDialog) {
        ReportContentDialog(
            targetTitle = "Copyright, Seller, or Safety Report",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                viewModel.submitReport("GENERAL_SAFETY", "profile_portal", reason, details)
                showReportDialog = false
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
                ) {
                    Text("Create")
                }
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
                }) {
                    Text("Share / Save Copy")
                }
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
                        "This permanently deletes your private wardrobe, saved outfits, AI stylist chats, saved previews, and profile from LITHFIT.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    deleteError?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
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
        EditProfilePreferencesDialog(
            profile = profile,
            onDismiss = { showEditPreferences = false },
            onSave = { updated ->
                viewModel.updateProfile(updated)
                showEditPreferences = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Account Header
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!profile?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile?.avatarUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
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

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.displayName ?: "LITHFIT Member",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "@${profile?.username ?: "member"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            profile?.email?.takeIf { it.isNotBlank() }?.let { email ->
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(
                                    if (profile?.privateProfile != false) "Private Profile" else "Custom Privacy",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        if (profile?.minorAccount == true) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(
                                        "Under-18 Protected Mode",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                        profile?.avatarStyle?.let { av ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$av Lookbook",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatBadge(value = wardrobeCount.toString(), label = "Garments")
                        ProfileStatBadge(value = savedOutfits.size.toString(), label = "Outfits")
                        ProfileStatBadge(value = savedItems.size.toString(), label = "Saved")
                        ProfileStatBadge(value = previews.size.toString(), label = "AI Looks")
                    }
                }
            }
        }

        // Theme Selector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    val currentTheme = ThemeMode.safeParse(profile?.themeMode)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = currentTheme == mode,
                                onClick = { viewModel.updateThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ThemeMode.entries.size
                                )
                            ) {
                                Text(mode.label)
                            }
                        }
                    }
                }
            }
        }

        // Personal Style Preferences Summary
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Personal Style Preferences", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = { showEditPreferences = true },
                            enabled = profile != null,
                            modifier = Modifier.testTag("edit_style_preferences_button")
                        ) {
                            Text("Edit Preferences")
                        }
                    }

                    if (profile != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PreferenceTagPill("Focus: ${profile.inspirationFocus}")
                            if (profile.gender.isNotBlank()) PreferenceTagPill("Gender: ${profile.gender}")
                            if (profile.age.isNotBlank()) PreferenceTagPill("Age: ${profile.age}")
                            if (profile.height.isNotBlank()) PreferenceTagPill("Height: ${profile.height}")
                            if (profile.bodySize.isNotBlank()) PreferenceTagPill("Size: ${profile.bodySize}")
                            PreferenceTagPill("Budget: ${profile.budgetPreference} (${profile.preferredCurrency})")
                        }

                        if (profile.preferredStyles.isNotEmpty()) {
                            Text("Favourite Styles", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profile.preferredStyles.forEach { PreferenceTagPill(it) }
                            }
                        }

                        if (profile.preferredColors.isNotEmpty()) {
                            Text("Favourite Colours", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profile.preferredColors.forEach { colorName ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            ColorSwatchDot(colorName = colorName, size = 12.dp)
                                            Text(colorName, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        if (profile.avoidColors.isNotEmpty()) {
                            Text("Colours to Avoid", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profile.avoidColors.forEach { colorName ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            ColorSwatchDot(colorName = colorName, size = 12.dp)
                                            Text(colorName, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        if (profile.preferredWeatherStyles.isNotEmpty()) {
                            Text("Weather Styling", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profile.preferredWeatherStyles.forEach { PreferenceTagPill(it) }
                            }
                        }

                        if (profile.preferredOccasions.isNotEmpty()) {
                            Text("Key Occasions", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profile.preferredOccasions.forEach { PreferenceTagPill(it) }
                            }
                        }

                        if (profile.preferredFootwear.isNotEmpty() || profile.preferredAccessories.isNotEmpty()) {
                            Text("Footwear & Accessories", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (profile.preferredFootwear + profile.preferredAccessories).forEach {
                                    PreferenceTagPill(it)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Hub & Collections
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("Saved Looks & Collections", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = { showCreateCollection = true }) {
                            Text("+ Collection")
                        }
                    }

                    if (collections.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            collections.forEach { col ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = col.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (savedOutfits.isNotEmpty()) {
                        Text("Recent Saved Outfits", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(savedOutfits.take(8), key = { it.id }) { outfit ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.width(190.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(outfit.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                        Text(
                                            "${outfit.occasion} • ${outfit.vibe}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = { onOpenStyleRadarForOutfit(outfit) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Explore in Style Radar")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (previews.isNotEmpty()) {
                        Text("Saved AI Visual Previews", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(previews.take(8), key = { it.id }) { prev ->
                                Column(
                                    modifier = Modifier.width(136.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    LithfitGarmentImage(
                                        localImagePath = prev.localImagePath,
                                        thumbnailBase64 = prev.thumbnailBase64,
                                        imageUrl = prev.remoteImageUrl,
                                        imageResKey = null,
                                        contentDescription = prev.promptSummary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Text(
                                        text = "${prev.vibe} • AI Look",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                    TextButton(
                                        onClick = {
                                            val bmp = prev.localImagePath?.let { path ->
                                                runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
                                            } ?: prev.thumbnailBase64?.let { b64 ->
                                                ImageStorageHelper.decodeBase64Thumbnail(b64)
                                            }
                                            if (bmp != null) {
                                                viewModel.savePreviewToDeviceGallery(
                                                    context = context,
                                                    bitmap = bmp,
                                                    title = "LITHFIT_${prev.vibe}"
                                                )
                                            }
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Save to Photos", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    if (savedItems.isNotEmpty()) {
                        Text("Saved Inspiration & Catalogue", style = MaterialTheme.typography.labelLarge)
                        savedItems.take(6).forEach { saved ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(saved.title, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${saved.category} • ${saved.subtitle.orEmpty()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (savedOutfits.isEmpty() && previews.isEmpty() && savedItems.isEmpty()) {
                        Text(
                            "Save outfits in Wardrobe, AI visual previews in LITHFIT AI, or bookmarks in Discover to organize them here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Privacy, Permissions & Data Controls
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("Privacy, Permissions & Data", style = MaterialTheme.typography.titleMedium)
                    }

                    if (profile != null) {
                        Text(
                            text = if (profile.minorAccount) {
                                "Under-18 Protected Mode active: profile stays private with zero public discovery."
                            } else {
                                "Your wardrobe, saved outfits, and style preferences are private to your account."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        Text("Export Wardrobe & Profile Data")
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
                }
            }
        }

        // Legal, Safety & Credits
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Policy, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("Legal, Safety & Credits", style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = {
                        openDocTitle = "LITHFIT Privacy Policy"
                        openDocBody = LegalDocuments.privacyPolicyText
                    }) { Text("Privacy Policy (CLASTAD)") }
                    TextButton(onClick = {
                        openDocTitle = "LITHFIT Terms of Service"
                        openDocBody = LegalDocuments.termsOfServiceText
                    }) { Text("Terms of Service") }
                    TextButton(onClick = {
                        openDocTitle = "LITHFIT Community Guidelines"
                        openDocBody = LegalDocuments.communityGuidelinesText
                    }) { Text("Community & Body-Neutral Guidelines") }

                    TextButton(onClick = {
                        openDocTitle = "Credits & Attribution"
                        openDocBody = LegalDocuments.creditsText
                    }) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Credits & Creator Attribution")
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Founder and Creator: Jason Prince",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Developed by CLASTAD",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Report Copyright or Safety Issue")
                    }
                }
            }
        }

        // Contact CLASTAD & Website
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.testTag("contact_clastad_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClastadLogoMark(size = 44.dp, cornerRadius = 10.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Contact CLASTAD", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Official website: $clastadDomain",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = clastadUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(clastadUrl))
                                    copiedClastadUrl = true
                                },
                                modifier = Modifier.testTag("copy_clastad_url_button")
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (copiedClastadUrl) "Copied" else "Copy URL")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clastadUrl)))
                                }
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
                                }.onFailure {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clastadUrl)))
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("email_clastad_button")
                        ) {
                            Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Contact Us")
                        }
                    }
                }
            }
        }

        // Account Sign Out & Deletion
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
        }

        // Brand Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LithfitWordmark(fontSizeSp = 14f, showAttribution = true)
                Text(
                    text = "Your Style. Your World. • Free Core Styling",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Developed by CLASTAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfilePreferencesDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var displayName by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var city by remember { mutableStateOf(profile.locationLabel.orEmpty()) }
    var avatarStyle by remember { mutableStateOf(profile.avatarStyle ?: LithfitConstants.AVATAR_STYLES.first()) }
    var inspirationFocus by remember { mutableStateOf(profile.inspirationFocus.ifBlank { "All" }) }
    var gender by remember { mutableStateOf(profile.gender.ifBlank { LithfitConstants.GENDER_OPTIONS.first() }) }
    var age by remember { mutableStateOf(profile.age.ifBlank { LithfitConstants.AGE_RANGES[1] }) }
    var height by remember { mutableStateOf(profile.height.ifBlank { LithfitConstants.HEIGHT_OPTIONS[2] }) }
    var bodySize by remember { mutableStateOf(profile.bodySize.ifBlank { LithfitConstants.BODY_SIZES[2] }) }
    var budgetPref by remember { mutableStateOf(profile.budgetPreference.ifBlank { "Balanced everyday" }) }
    var currency by remember { mutableStateOf(profile.preferredCurrency) }
    var minorMode by remember { mutableStateOf(profile.minorAccount) }
    var privateProf by remember { mutableStateOf(profile.privateProfile) }
    val styles = remember { mutableStateListOf<String>().apply { addAll(profile.preferredStyles) } }
    val colors = remember { mutableStateListOf<String>().apply { addAll(profile.preferredColors) } }
    val avoidColors = remember { mutableStateListOf<String>().apply { addAll(profile.avoidColors) } }
    val weatherStyles = remember { mutableStateListOf<String>().apply { addAll(profile.preferredWeatherStyles) } }
    val occasions = remember { mutableStateListOf<String>().apply { addAll(profile.preferredOccasions) } }
    val footwear = remember { mutableStateListOf<String>().apply { addAll(profile.preferredFootwear) } }
    val accessories = remember { mutableStateListOf<String>().apply { addAll(profile.preferredAccessories) } }
    val categories = remember { mutableStateListOf<String>().apply { addAll(profile.preferredCategories) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Style & Account Preferences") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Preferred City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Private Profile", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = privateProf || minorMode,
                        enabled = !minorMode,
                        onCheckedChange = { privateProf = it }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Under-18 Protected Mode", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = minorMode,
                        onCheckedChange = { minorMode = it }
                    )
                }

                Text("Gender", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.GENDER_OPTIONS.forEach { g ->
                        FilterChip(
                            selected = gender == g,
                            onClick = { gender = g },
                            label = { Text(g) }
                        )
                    }
                }

                Text("Age Range", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.AGE_RANGES.forEach { a ->
                        FilterChip(
                            selected = age == a,
                            onClick = { age = a },
                            label = { Text(a) }
                        )
                    }
                }

                Text("Height", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.HEIGHT_OPTIONS.forEach { h ->
                        FilterChip(
                            selected = height == h,
                            onClick = { height = h },
                            label = { Text(h) }
                        )
                    }
                }

                Text("Body Size & Fit", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.BODY_SIZES.forEach { sz ->
                        FilterChip(
                            selected = bodySize == sz,
                            onClick = { bodySize = sz },
                            label = { Text(sz) }
                        )
                    }
                }

                Text("Inspiration Focus", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.AUDIENCE_FILTERS.forEach { focus ->
                        FilterChip(
                            selected = inspirationFocus == focus,
                            onClick = { inspirationFocus = focus },
                            label = { Text(focus) }
                        )
                    }
                }

                Text("Lookbook Persona", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.AVATAR_STYLES.forEach { av ->
                        FilterChip(
                            selected = avatarStyle == av,
                            onClick = { avatarStyle = av },
                            label = { Text(av) }
                        )
                    }
                }

                Text("Preferred Styles", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.STYLE_CATEGORIES.forEach { st ->
                        val picked = st in styles
                        FilterChip(
                            selected = picked,
                            onClick = { if (picked) styles.remove(st) else styles.add(st) },
                            label = { Text(st) }
                        )
                    }
                }

                Text("Favourite Colours", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LithfitConstants.PREFERRED_COLORS.forEach { col ->
                        val picked = col in colors
                        ColorSwatchChip(
                            colorName = col,
                            selected = picked,
                            onClick = {
                                if (picked) {
                                    colors.remove(col)
                                } else {
                                    colors.add(col)
                                    avoidColors.remove(col)
                                }
                            }
                        )
                    }
                }

                Text("Colours to Avoid", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LithfitConstants.PREFERRED_COLORS.forEach { col ->
                        val picked = col in avoidColors
                        ColorSwatchChip(
                            colorName = col,
                            selected = picked,
                            onClick = {
                                if (picked) {
                                    avoidColors.remove(col)
                                } else {
                                    avoidColors.add(col)
                                    colors.remove(col)
                                }
                            }
                        )
                    }
                }

                Text("Weather Styling", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.WEATHER_STYLES.forEach { w ->
                        val picked = w in weatherStyles
                        FilterChip(
                            selected = picked,
                            onClick = { if (picked) weatherStyles.remove(w) else weatherStyles.add(w) },
                            label = { Text(w) }
                        )
                    }
                }

                Text("Preferred Occasions", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.OCCASIONS.forEach { occ ->
                        val picked = occ in occasions
                        FilterChip(
                            selected = picked,
                            onClick = { if (picked) occasions.remove(occ) else occasions.add(occ) },
                            label = { Text(occ) }
                        )
                    }
                }

                Text("Preferred Footwear", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.PREFERRED_FOOTWEAR_TYPES.forEach { shoe ->
                        val picked = shoe in footwear
                        FilterChip(
                            selected = picked,
                            onClick = { if (picked) footwear.remove(shoe) else footwear.add(shoe) },
                            label = { Text(shoe) }
                        )
                    }
                }

                Text("Preferred Accessories", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.PREFERRED_ACCESSORY_TYPES.forEach { acc ->
                        val picked = acc in accessories
                        FilterChip(
                            selected = picked,
                            onClick = { if (picked) accessories.remove(acc) else accessories.add(acc) },
                            label = { Text(acc) }
                        )
                    }
                }

                Text("Preferred Categories", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GarmentCategory.entries.filter { it != GarmentCategory.OTHER }.forEach { cat ->
                        val picked = cat.name in categories
                        FilterChip(
                            selected = picked,
                            onClick = { if (picked) categories.remove(cat.name) else categories.add(cat.name) },
                            label = { Text(cat.label) }
                        )
                    }
                }

                Text("Budget Comfort", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.BUDGET_PREFERENCES.forEach { b ->
                        FilterChip(
                            selected = budgetPref == b,
                            onClick = { budgetPref = b },
                            label = { Text(b) }
                        )
                    }
                }

                Text("Currency", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.CURRENCIES.forEach { cur ->
                        FilterChip(
                            selected = currency == cur,
                            onClick = { currency = cur },
                            label = { Text(cur) }
                        )
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            displayName = displayName.trim().ifBlank { profile.displayName },
                            username = username.trim().ifBlank { profile.username },
                            locationLabel = city.trim().ifBlank { null },
                            avatarStyle = avatarStyle,
                            inspirationFocus = inspirationFocus,
                            budgetPreference = budgetPref,
                            preferredCurrency = currency,
                            minorAccount = minorMode,
                            privateProfile = if (minorMode) true else privateProf,
                            locationAccessPreference = if (minorMode) false else profile.locationAccessPreference,
                            preferredStyles = styles.toList(),
                            preferredColors = colors.toList(),
                            avoidColors = avoidColors.toList(),
                            preferredWeatherStyles = weatherStyles.toList(),
                            preferredOccasions = occasions.toList(),
                            preferredFootwear = footwear.toList(),
                            preferredAccessories = accessories.toList(),
                            preferredCategories = categories.toList(),
                            gender = gender,
                            age = age,
                            height = height,
                            bodySize = bodySize
                        )
                    )
                },
                enabled = displayName.isNotBlank() && username.isNotBlank()
            ) {
                Text("Save")
            }
        }
    )
}
