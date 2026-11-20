package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppGuidePreferences
import com.example.core.ImageStorageHelper
import com.example.data.model.AvatarConfig
import com.example.data.model.GarmentCategory
import com.example.data.model.LithfitConstants
import com.example.data.model.OutfitSlotGroup
import com.example.data.model.UiState
import com.example.data.model.WardrobeItem
import com.example.ui.components.ColorSwatchChip
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.ContextualTipBanner
import com.example.ui.components.LithfitGarmentImage
import com.example.ui.viewmodel.LithfitViewModel
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarStudioScreen(
    viewModel: LithfitViewModel,
    onAskStylistWithAvatar: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val wardrobeState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val dismissedTips by viewModel.dismissedTips.collectAsStateWithLifecycle()
    val personalPhotoRevision by viewModel.personalPhotoRevision.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val wardrobeItems = (wardrobeState as? UiState.Success)?.data.orEmpty()

    val personalPhotoPath = remember(personalPhotoRevision) {
        if (ImageStorageHelper.hasPersonalFullBodyPhoto(context)) {
            ImageStorageHelper.getPersonalFullBodyPhotoFile(context).absolutePath
        } else {
            null
        }
    }

    var config by remember { mutableStateOf(profile?.avatarConfig ?: AvatarConfig()) }
    var initialized by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(profile?.avatarConfig) {
        if (!initialized && profile != null) {
            config = profile.avatarConfig
            initialized = true
        }
    }

    val studioTabs = listOf(
        "Style Identity",
        "Hair & Eyewear",
        "Clothes & Shoes",
        "Closet Try-On",
        "Style Presets"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ContextualTipBanner(
                tipKey = AppGuidePreferences.TIP_AVATAR,
                title = "Personal Style Persona",
                body = "Tune your aesthetic preset, garments from your Closet, and private studio photo reference for realistic AI try-on generation.",
                dismissedTips = dismissedTips,
                onDismiss = { viewModel.dismissTip(it) }
            )
        }

        // Photorealistic Editorial Lookbook & Personal Photo Card (zero pixelated vector figures)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Style Studio", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "${config.presentation} • ${config.aestheticPreset} Aesthetic",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { config = AvatarConfig() },
                            modifier = Modifier.testTag("avatar_reset_button")
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }

                    StylePersonaLookbookCard(
                        config = config,
                        hasPersonalPhoto = personalPhotoPath != null,
                        personalPhotoPath = personalPhotoPath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("style_persona_lookbook_card")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onAskStylistWithAvatar(
                                    "Generate a ${config.aestheticPreset} outfit tailored for a ${config.presentation} fit featuring a ${config.topColor} ${config.topStyle}, ${config.bottomColor} ${config.bottomStyle}, and ${config.shoesColor} ${config.shoesStyle} (${config.accessoryStyle})."
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Style Fit", maxLines = 1)
                        }
                        Button(
                            onClick = { viewModel.saveAvatarConfig(config) },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("save_avatar_button")
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Style Studio")
                        }
                    }
                }
            }
        }

        // Studio Customization Dock Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent
            ) {
                studioTabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label) }
                    )
                }
            }
        }

        when (selectedTab) {
            // Identity & Face
            0 -> {
                item {
                    AvatarSectionCard(title = "Presentation & Silhouette") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LithfitConstants.AVATAR_PRESENTATIONS.forEach { pres ->
                                FilterChip(
                                    selected = config.presentation == pres,
                                    onClick = { config = config.copy(presentation = pres) },
                                    label = { Text(pres) }
                                )
                            }
                        }
                    }
                }
                item {
                    AvatarSectionCard(title = "Skin Tone Reference") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LithfitConstants.SKIN_TONE_SWATCHES.forEach { swatch ->
                                val picked = config.skinToneHex == swatch.hex
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(swatch.hex))
                                        .border(
                                            width = if (picked) 3.dp else 1.dp,
                                            color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable { config = config.copy(skinToneHex = swatch.hex) }
                                )
                            }
                        }
                    }
                }
                item {
                    AvatarSectionCard(title = "Face Shape, Eyes & Vibe") {
                        Text("Face Shape", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LithfitConstants.FACE_SHAPES.forEach { shape ->
                                FilterChip(
                                    selected = config.faceShape == shape,
                                    onClick = { config = config.copy(faceShape = shape) },
                                    label = { Text(shape) }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Eye Colour", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LithfitConstants.EYE_COLORS.forEach { eye ->
                                FilterChip(
                                    selected = config.eyeColor == eye,
                                    onClick = { config = config.copy(eyeColor = eye) },
                                    label = { Text(eye) }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Lookbook Expression", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LithfitConstants.EXPRESSIONS.forEach { exp ->
                                FilterChip(
                                    selected = config.expression == exp,
                                    onClick = { config = config.copy(expression = exp) },
                                    label = { Text(exp) }
                                )
                            }
                        }
                    }
                }
            }

            // Hair & Glasses
            1 -> {
                item {
                    AvatarSectionCard(title = "Hairstyle (${LithfitConstants.HAIR_STYLES.size} Cuts)") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LithfitConstants.HAIR_STYLES.forEach { style ->
                                FilterChip(
                                    selected = config.hairStyle == style,
                                    onClick = { config = config.copy(hairStyle = style) },
                                    label = { Text(style) }
                                )
                            }
                        }
                    }
                }
                item {
                    AvatarSectionCard(title = "Hair Colour") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LithfitConstants.HAIR_COLORS.forEach { hairCol ->
                                val picked = config.hairColor == hairCol.name
                                Surface(
                                    onClick = { config = config.copy(hairColor = hairCol.name) },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (picked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(hairCol.hex))
                                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        )
                                        Text(hairCol.name, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    AvatarSectionCard(title = "Eyewear") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LithfitConstants.GLASSES_STYLES.forEach { glasses ->
                                FilterChip(
                                    selected = config.glassesStyle == glasses,
                                    onClick = { config = config.copy(glassesStyle = glasses) },
                                    label = { Text(glasses) }
                                )
                            }
                        }
                    }
                }
            }

            // Clothes & Shoes
            2 -> {
                item {
                    AvatarSectionCard(title = "Tops & Outerwear") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(LithfitConstants.AVATAR_TOPS) { top ->
                                FilterChip(
                                    selected = config.topStyle == top,
                                    onClick = { config = config.copy(topStyle = top) },
                                    label = { Text(top) }
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(LithfitConstants.COLOR_SWATCHES) { swatch ->
                                ColorSwatchChip(
                                    colorName = swatch.name,
                                    selected = config.topColor == swatch.name,
                                    onClick = { config = config.copy(topColor = swatch.name) }
                                )
                            }
                        }
                    }
                }
                item {
                    AvatarSectionCard(title = "Bottoms & Silhouette") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(LithfitConstants.AVATAR_BOTTOMS) { bottom ->
                                FilterChip(
                                    selected = config.bottomStyle == bottom,
                                    onClick = { config = config.copy(bottomStyle = bottom) },
                                    label = { Text(bottom) }
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(LithfitConstants.COLOR_SWATCHES) { swatch ->
                                ColorSwatchChip(
                                    colorName = swatch.name,
                                    selected = config.bottomColor == swatch.name,
                                    onClick = { config = config.copy(bottomColor = swatch.name) }
                                )
                            }
                        }
                    }
                }
                item {
                    AvatarSectionCard(title = "Footwear & Accessories") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(LithfitConstants.AVATAR_SHOES) { shoe ->
                                FilterChip(
                                    selected = config.shoesStyle == shoe,
                                    onClick = { config = config.copy(shoesStyle = shoe) },
                                    label = { Text(shoe) }
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(LithfitConstants.COLOR_SWATCHES) { swatch ->
                                ColorSwatchChip(
                                    colorName = swatch.name,
                                    selected = config.shoesColor == swatch.name,
                                    onClick = { config = config.copy(shoesColor = swatch.name) }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Accessory", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LithfitConstants.AVATAR_ACCESSORIES.forEach { acc ->
                                FilterChip(
                                    selected = config.accessoryStyle == acc,
                                    onClick = { config = config.copy(accessoryStyle = acc) },
                                    label = { Text(acc) }
                                )
                            }
                        }
                    }
                }
            }

            // Closet Try-On (Real user wardrobe garments)
            3 -> {
                if (wardrobeItems.isEmpty()) {
                    item {
                        AvatarSectionCard(title = "Try Pieces from Your Closet") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Outlined.Checkroom, contentDescription = null)
                                Text(
                                    "Add shirts, hoodies, trousers, or sneakers in the Closet tab and tap them here to map them into your AI styling brief.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    item {
                        AvatarSectionCard(title = "Tap Any Closet Item to Stage Fit") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                wardrobeItems.forEach { item ->
                                    val cat = item.categoryEnum()
                                    Surface(
                                        onClick = {
                                            config = applyWardrobeItemToAvatar(config, item, cat)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            ColorSwatchDot(colorName = item.color, size = 18.dp)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    "${cat.label} • ${item.color}${item.style?.let { " • $it" }.orEmpty()}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                "Stage",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Style Presets
            4 -> {
                item {
                    AvatarSectionCard(title = "Curated Aesthetic Presets") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            avatarAestheticPresets().forEach { (name, presetFn) ->
                                FilterChip(
                                    selected = config.aestheticPreset == name,
                                    onClick = { config = presetFn(config) },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StylePersonaLookbookCard(
    config: AvatarConfig,
    hasPersonalPhoto: Boolean,
    personalPhotoPath: String?,
    modifier: Modifier = Modifier
) {
    val personalBitmap = remember(personalPhotoPath, hasPersonalPhoto) {
        if (hasPersonalPhoto && !personalPhotoPath.isNullOrBlank()) {
            val file = File(personalPhotoPath)
            if (file.exists() && file.length() > 0L) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } else {
            null
        }
    }
    val lookbookKey = remember(config.aestheticPreset) {
        editorialKeyForPreset(config.aestheticPreset)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(276.dp)
                .clip(RoundedCornerShape(18.dp))
        ) {
            if (personalBitmap != null) {
                Image(
                    bitmap = personalBitmap.asImageBitmap(),
                    contentDescription = "Your private try-on photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LithfitGarmentImage(
                    localImagePath = null,
                    thumbnailBase64 = null,
                    imageUrl = null,
                    imageResKey = lookbookKey,
                    contentDescription = "${config.aestheticPreset} editorial inspiration",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.72f)
                        )
                    )
            )

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (personalBitmap != null) "Private Studio Photo Active" else "${config.aestheticPreset} Editorial Model",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${config.topColor} ${config.topStyle} + ${config.bottomColor} ${config.bottomStyle}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${config.shoesColor} ${config.shoesStyle} • ${config.hairStyle} (${config.hairColor})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LookbookColorChip(label = "Top: ${config.topColor}", colorName = config.topColor)
            LookbookColorChip(label = "Bottom: ${config.bottomColor}", colorName = config.bottomColor)
            LookbookColorChip(label = "Shoes: ${config.shoesColor}", colorName = config.shoesColor)
            if (config.accessoryStyle != "None") {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = config.accessoryStyle,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LookbookColorChip(label: String, colorName: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ColorSwatchDot(colorName = colorName, size = 10.dp)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun editorialKeyForPreset(preset: String): String = when (preset.lowercase()) {
    "y2k" -> "img_discover_y2k_pink_cargos"
    "old money" -> "img_discover_old_money_mens"
    "minimalist" -> "img_hero_minimalist"
    "afro-inspired" -> "img_editorial_afro_fashion"
    "grunge" -> "img_editorial_grunge_teen"
    "sporty" -> "img_editorial_trio_3"
    else -> "img_editorial_streetwear"
}

private fun applyWardrobeItemToAvatar(
    current: AvatarConfig,
    item: WardrobeItem,
    cat: GarmentCategory
): AvatarConfig {
    val label = item.subType?.takeIf { it.isNotBlank() } ?: item.name
    val color = item.color.takeIf { it.isNotBlank() } ?: "Black"
    return when (cat.slotGroup) {
        OutfitSlotGroup.TOP, OutfitSlotGroup.JACKET -> current.copy(topStyle = label, topColor = color)
        OutfitSlotGroup.BOTTOM -> current.copy(bottomStyle = label, bottomColor = color)
        OutfitSlotGroup.SHOES -> current.copy(shoesStyle = label, shoesColor = color)
        OutfitSlotGroup.ACCESSORIES -> current.copy(accessoryStyle = label)
    }
}

private fun avatarAestheticPresets(): List<Pair<String, (AvatarConfig) -> AvatarConfig>> = listOf(
    "Streetwear" to {
        it.copy(
            aestheticPreset = "Streetwear",
            topStyle = "Boxy Hoodie",
            topColor = "Charcoal",
            bottomStyle = "Wide Cargos",
            bottomColor = "Olive",
            shoesStyle = "Low-Top Sneakers",
            shoesColor = "Cream",
            accessoryStyle = "Minimal Chain"
        )
    },
    "Y2K" to {
        it.copy(
            aestheticPreset = "Y2K",
            topStyle = "Cropped Jacket",
            topColor = "Burgundy",
            bottomStyle = "Baggy Jeans",
            bottomColor = "Blue",
            shoesStyle = "High-Top Sneakers",
            shoesColor = "White",
            glassesStyle = "Y2K Tinted",
            accessoryStyle = "Crossbody Bag"
        )
    },
    "Old money" to {
        it.copy(
            aestheticPreset = "Old money",
            topStyle = "Knit Polo",
            topColor = "Navy",
            bottomStyle = "Pleated Trousers",
            bottomColor = "Cream",
            shoesStyle = "Chunky Loafers",
            shoesColor = "Chocolate",
            glassesStyle = "None",
            accessoryStyle = "Wristwatch"
        )
    },
    "Minimalist" to {
        it.copy(
            aestheticPreset = "Minimalist",
            topStyle = "Structured Blazer",
            topColor = "Black",
            bottomStyle = "Straight Denim",
            bottomColor = "Charcoal",
            shoesStyle = "Low-Top Sneakers",
            shoesColor = "White",
            accessoryStyle = "None"
        )
    },
    "Afro-inspired" to {
        it.copy(
            aestheticPreset = "Afro-inspired",
            topStyle = "Relaxed Shirt",
            topColor = "Orange",
            bottomStyle = "Pleated Trousers",
            bottomColor = "Cream",
            shoesStyle = "Chunky Loafers",
            shoesColor = "Brown",
            accessoryStyle = "Minimal Chain"
        )
    },
    "Grunge" to {
        it.copy(
            aestheticPreset = "Grunge",
            topStyle = "Layered Flannel",
            topColor = "Burgundy",
            bottomStyle = "Baggy Jeans",
            bottomColor = "Black",
            shoesStyle = "Combat Boots",
            shoesColor = "Black",
            accessoryStyle = "Beanie"
        )
    },
    "Sporty" to {
        it.copy(
            aestheticPreset = "Sporty",
            topStyle = "Oversized Tee",
            topColor = "Sky Blue",
            bottomStyle = "Track Pants",
            bottomColor = "Charcoal",
            shoesStyle = "Running Trainers",
            shoesColor = "White",
            accessoryStyle = "Cap"
        )
    }
)

@Composable
private fun AvatarSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
