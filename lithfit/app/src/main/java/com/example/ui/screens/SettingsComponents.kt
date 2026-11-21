package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.GarmentCategory
import com.example.data.model.LithfitConstants
import com.example.data.model.UserProfile
import com.example.ui.components.ColorSwatchChip
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditStylePreferencesDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var displayName by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var email by remember { mutableStateOf(profile.email.orEmpty()) }
    var phone by remember { mutableStateOf(profile.phone.orEmpty()) }
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

    var customTagInput by remember { mutableStateOf("") }
    val styles = remember { mutableStateListOf<String>().apply { addAll(profile.preferredStyles) } }
    val personalTags = remember { mutableStateListOf<String>().apply { addAll(profile.personalStyleTags) } }
    val colors = remember { mutableStateListOf<String>().apply { addAll(profile.preferredColors) } }
    val frequentColors = remember { mutableStateListOf<String>().apply { addAll(profile.frequentColors) } }
    val neutralCombos = remember { mutableStateListOf<String>().apply { addAll(profile.neutralCombos) } }
    val experimentColors = remember { mutableStateListOf<String>().apply { addAll(profile.experimentColors) } }
    val avoidColors = remember { mutableStateListOf<String>().apply { addAll(profile.avoidColors) } }
    val silhouettes = remember { mutableStateListOf<String>().apply { addAll(profile.preferredSilhouettes) } }
    val enjoyedCats = remember { mutableStateListOf<String>().apply { addAll(profile.enjoyedCategories) } }
    val rarelyCats = remember { mutableStateListOf<String>().apply { addAll(profile.rarelyWornCategories) } }
    val weatherStyles = remember { mutableStateListOf<String>().apply { addAll(profile.preferredWeatherStyles) } }
    val occasions = remember { mutableStateListOf<String>().apply { addAll(profile.preferredOccasions) } }
    val footwear = remember { mutableStateListOf<String>().apply { addAll(profile.preferredFootwear) } }
    val accessories = remember { mutableStateListOf<String>().apply { addAll(profile.preferredAccessories) } }
    val lifestyleTags = remember { mutableStateListOf<String>().apply { addAll(profile.lifestyleTags) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Style & Account Preferences") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
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
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Preferred City for Weather") },
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ChipGroupSection("Gender", LithfitConstants.GENDER_OPTIONS, gender) { gender = it }
                ChipGroupSection("Age Range", LithfitConstants.AGE_RANGES, age) { age = it }
                ChipGroupSection("Height", LithfitConstants.HEIGHT_OPTIONS, height) { height = it }
                ChipGroupSection("Body Size & Fit", LithfitConstants.BODY_SIZES, bodySize) { bodySize = it }
                ChipGroupSection("Inspiration Focus", LithfitConstants.AUDIENCE_FILTERS, inspirationFocus) { inspirationFocus = it }
                ChipGroupSection("Lookbook Persona", LithfitConstants.AVATAR_STYLES, avatarStyle) { avatarStyle = it }

                MultiChipGroupSection("Preferred Styles", LithfitConstants.STYLE_CATEGORIES, styles)

                Text("Custom Personal Style Tags", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customTagInput,
                        onValueChange = { customTagInput = it },
                        placeholder = { Text("e.g. Earthy Layers") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_style_tag_input")
                    )
                    Button(
                        onClick = {
                            val clean = customTagInput.trim()
                            if (clean.isNotBlank() && personalTags.none { it.equals(clean, true) }) {
                                personalTags.add(clean)
                            }
                            customTagInput = ""
                        },
                        enabled = customTagInput.isNotBlank(),
                        modifier = Modifier.testTag("add_custom_style_tag_button")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add tag", modifier = Modifier.size(16.dp))
                    }
                }
                if (personalTags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        personalTags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { personalTags.remove(tag) },
                                label = { Text("$tag ×") }
                            )
                        }
                    }
                }

                ColorMultiGroup("Favourite Colours", colors, avoidColors)
                ColorMultiGroup("Frequently Worn Colours", frequentColors, null)
                MultiChipGroupSection("Preferred Neutral Combos", LithfitConstants.NEUTRAL_COMBOS, neutralCombos)
                ColorMultiGroup("Experiment Colours", experimentColors, null)
                ColorMultiGroup("Colours to Avoid", avoidColors, colors)

                MultiChipGroupSection("Preferred Silhouettes", LithfitConstants.FIT_SILHOUETTES, silhouettes)
                val catLabels = GarmentCategory.entries.filter { it != GarmentCategory.OTHER }.map { it.label }
                MultiChipGroupSection("Enjoyed Categories", catLabels, enjoyedCats)
                MultiChipGroupSection("Rarely Worn Categories", catLabels, rarelyCats)
                MultiChipGroupSection("Weather Styling", LithfitConstants.WEATHER_STYLES, weatherStyles)
                MultiChipGroupSection("Preferred Occasions", LithfitConstants.OCCASIONS, occasions)
                MultiChipGroupSection("Preferred Footwear", LithfitConstants.PREFERRED_FOOTWEAR_TYPES, footwear)
                MultiChipGroupSection("Preferred Accessories", LithfitConstants.PREFERRED_ACCESSORY_TYPES, accessories)
                MultiChipGroupSection("Lifestyle Tags", LithfitConstants.LIFESTYLE_TAGS, lifestyleTags)
                ChipGroupSection("Budget Comfort", LithfitConstants.BUDGET_PREFERENCES, budgetPref) { budgetPref = it }
                ChipGroupSection("Currency", LithfitConstants.CURRENCIES, currency) { currency = it }
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
                            email = email.trim().ifBlank { null },
                            phone = phone.trim().ifBlank { null },
                            locationLabel = city.trim().ifBlank { null },
                            avatarStyle = avatarStyle,
                            inspirationFocus = inspirationFocus,
                            budgetPreference = budgetPref,
                            preferredCurrency = currency,
                            minorAccount = minorMode,
                            privateProfile = if (minorMode) true else privateProf,
                            locationAccessPreference = if (minorMode) false else profile.locationAccessPreference,
                            preferredStyles = styles.toList(),
                            personalStyleTags = personalTags.toList(),
                            preferredColors = colors.toList(),
                            frequentColors = frequentColors.toList(),
                            neutralCombos = neutralCombos.toList(),
                            experimentColors = experimentColors.toList(),
                            avoidColors = avoidColors.toList(),
                            preferredSilhouettes = silhouettes.toList(),
                            enjoyedCategories = enjoyedCats.toList(),
                            rarelyWornCategories = rarelyCats.toList(),
                            preferredCategories = enjoyedCats.toList(),
                            preferredWeatherStyles = weatherStyles.toList(),
                            preferredOccasions = occasions.toList(),
                            preferredFootwear = footwear.toList(),
                            preferredAccessories = accessories.toList(),
                            lifestyleTags = lifestyleTags.toList(),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroupSection(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick = { onSelect(opt) },
                label = { Text(opt) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChipGroupSection(
    title: String,
    options: List<String>,
    selectedList: MutableList<String>
) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { opt ->
            val picked = opt in selectedList
            FilterChip(
                selected = picked,
                onClick = { if (picked) selectedList.remove(opt) else selectedList.add(opt) },
                label = { Text(opt) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorMultiGroup(
    title: String,
    targetList: MutableList<String>,
    mutuallyExclusiveList: MutableList<String>?
) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LithfitConstants.PREFERRED_COLORS.forEach { col ->
            val picked = col in targetList
            ColorSwatchChip(
                colorName = col,
                selected = picked,
                onClick = {
                    if (picked) {
                        targetList.remove(col)
                    } else {
                        targetList.add(col)
                        mutuallyExclusiveList?.remove(col)
                    }
                }
            )
        }
    }
}

@Composable
fun ChangePasswordDialog(
    isGoogleSignedIn: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (current: String, next: String) -> Result<Unit>
) {
    var currentPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isGoogleSignedIn) {
                    Text(
                        "You signed in with Google. Manage or change your password directly in your Google Account security settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = currentPw,
                        onValueChange = { currentPw = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPw,
                        onValueChange = { newPw = it },
                        label = { Text("New Password (6+ chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPw,
                        onValueChange = { confirmPw = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorMsg?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        dismissButton = {
            if (!isGoogleSignedIn) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        confirmButton = {
            if (isGoogleSignedIn) {
                Button(onClick = onDismiss) { Text("OK") }
            } else {
                Button(
                    onClick = {
                        if (newPw != confirmPw) {
                            errorMsg = "New passwords do not match."
                            return@Button
                        }
                        val res = onSubmit(currentPw, newPw)
                        if (res.isSuccess) {
                            onDismiss()
                        } else {
                            errorMsg = res.exceptionOrNull()?.localizedMessage ?: "Could not update password."
                        }
                    },
                    enabled = currentPw.isNotBlank() && newPw.length >= 6
                ) {
                    Text("Update")
                }
            }
        }
    )
}

@Composable
fun PrivateProfilePhotoDialog(
    profile: UserProfile?,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val localFile = remember(profile?.profilePhotoPath) {
        profile?.profilePhotoPath?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Private Profile Photo") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    localFile != null -> {
                        AsyncImage(
                            model = localFile,
                            contentDescription = "Your private profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(132.dp)
                                .clip(CircleShape)
                        )
                    }
                    !profile?.avatarUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = profile?.avatarUrl,
                            contentDescription = "Account photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(132.dp)
                                .clip(CircleShape)
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(132.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonOutline,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            "Stored privately on your device for you only — never shared publicly.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onPickPhoto, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload")
                    }
                    OutlinedButton(onClick = onTakePhoto, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                }

                if (localFile != null) {
                    OutlinedButton(
                        onClick = onRemovePhoto,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove Profile Photo")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
