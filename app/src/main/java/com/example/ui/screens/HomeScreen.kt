package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppGuidePreferences
import com.example.data.model.Outfit
import com.example.data.model.SeedCatalogData
import com.example.data.model.UiState
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.ContextualTipBanner
import com.example.ui.components.LithfitGarmentImage
import com.example.ui.components.LithfitLogoMark
import com.example.ui.components.LithfitWordmark
import com.example.ui.viewmodel.LithfitViewModel
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: LithfitViewModel,
    onOpenDiscover: (category: String?) -> Unit,
    onOpenAiStylist: (prefillPrompt: String?) -> Unit,
    onOpenWardrobe: (sectionIndex: Int) -> Unit
) {
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val wardrobeState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val outfitsState by viewModel.outfits.collectAsStateWithLifecycle()
    val discoveryPosts by viewModel.discoveryPosts.collectAsStateWithLifecycle()
    val recentlyViewed by viewModel.recentlyViewed.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val dismissedTips by viewModel.dismissedTips.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val wardrobeCount = (wardrobeState as? UiState.Success)?.data?.size ?: 0
    val savedOutfits = (outfitsState as? UiState.Success)?.data.orEmpty()
    val weather = (weatherState as? UiState.Success)?.data

    var showCityDialog by remember { mutableStateOf(false) }
    val initialDayIndex = remember {
        val tips = SeedCatalogData.dailyOutfitTips
        if (tips.isEmpty()) 0 else Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % tips.size
    }
    var tipOffset by rememberSaveable { mutableIntStateOf(0) }
    val dailyTips = SeedCatalogData.dailyOutfitTips
    val activeDailyTip = remember(tipOffset, dailyTips) {
        dailyTips.getOrNull((initialDayIndex + tipOffset) % dailyTips.size.coerceAtLeast(1))
    }

    if (showCityDialog) {
        var cityInput by remember { mutableStateOf(profile?.locationLabel.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = { Text("Weather Styling Context") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter a city for live temperature guidance in LITHFIT AI. No device GPS permission needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("City (e.g. Lagos, London, Tokyo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = { TextButton(onClick = { showCityDialog = false }) { Text("Cancel") } },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.fetchWeatherForCity(cityInput)
                        showCityDialog = false
                    },
                    enabled = cityInput.isNotBlank()
                ) { Text("Update") }
            }
        )
    }

    val firstName = profile?.displayName?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Creator"
    val primaryVibe = profile?.preferredStyles?.firstOrNull() ?: profile?.avatarConfig?.aestheticPreset ?: "Contemporary"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LithfitLogoMark(size = 28.dp)
                    LithfitWordmark(fontSizeSp = 17f)
                }
                AssistChip(
                    onClick = { showCityDialog = true },
                    leadingIcon = { Icon(Icons.Outlined.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = {
                        Text(
                            text = if (weather != null) {
                                "${weather.locationName.substringBefore(",")} • ${weather.temperatureC.toInt()}°C"
                            } else {
                                "Weather"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        // Greeting + Style Identity
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Hey $firstName,",
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = "Your Style. Your World. Curated for $primaryVibe, streetwear, cultural edits, and everyday rotation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Contextual Dismissible First-Run Tip
        item {
            ContextualTipBanner(
                tipKey = AppGuidePreferences.TIP_HOME,
                title = "Welcome to LITHFIT Home",
                body = "Jump straight into LITHFIT AI for quick outfit advice, build fits in your Closet, or design your Avatar.",
                dismissedTips = dismissedTips,
                onDismiss = { viewModel.dismissTip(it) }
            )
        }

        // Daily Style Focus Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_daily_focus_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "TODAY'S STYLE FOCUS • ${primaryVibe.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                    )
                    Text(
                        text = if (weather != null) {
                            "${weather.temperatureC.toInt()}°C ${weather.conditionLabel} — build a breathable layered look around pieces you already own."
                        } else {
                            "Ask LITHFIT AI to remix pieces from your wardrobe or answer any styling question in seconds."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = {
                                onOpenAiStylist("Give me a fresh $primaryVibe outfit for today using my closet pieces.")
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Style Today's Fit", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Surface(
                            onClick = { onOpenWardrobe(2) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Outfit Studio", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // 2x2 Quick Studio Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Studios", style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeStudioTile(
                        title = "AI Stylist",
                        subtitle = "Q&A + outfit creator",
                        icon = Icons.Outlined.AutoAwesome,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tile_ai_stylist"),
                        onClick = { onOpenAiStylist(null) }
                    )
                    HomeStudioTile(
                        title = "My Wardrobe",
                        subtitle = "$wardrobeCount garments • ${savedOutfits.size} outfits",
                        icon = Icons.Outlined.Checkroom,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tile_wardrobe"),
                        onClick = { onOpenWardrobe(0) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeStudioTile(
                        title = "Avatar Studio",
                        subtitle = "${profile?.avatarConfig?.aestheticPreset ?: "Streetwear"} persona",
                        icon = Icons.Outlined.FaceRetouchingNatural,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tile_avatar"),
                        onClick = { onOpenWardrobe(3) }
                    )
                    HomeStudioTile(
                        title = "Discover",
                        subtitle = "Editorial & Style Radar",
                        icon = Icons.Outlined.Explore,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tile_discover"),
                        onClick = { onOpenDiscover(null) }
                    )
                }
            }
        }

        // Daily Outfit Tips & Inspiration Card
        if (activeDailyTip != null) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_daily_tip_card")
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DAILY OUTFIT TIP & INSPIRATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = activeDailyTip.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = { tipOffset += 1 },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("home_daily_tip_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Next daily outfit tip"
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            LithfitGarmentImage(
                                localImagePath = null,
                                thumbnailBase64 = null,
                                imageUrl = null,
                                imageResKey = activeDailyTip.featuredImageResKey,
                                paletteNames = activeDailyTip.colorCombos,
                                contentDescription = activeDailyTip.featuredTitle,
                                modifier = Modifier
                                    .width(118.dp)
                                    .height(152.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = activeDailyTip.featuredTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = activeDailyTip.tipBody,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Colourway:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    activeDailyTip.colorCombos.forEach { col ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            ColorSwatchDot(colorName = col, size = 10.dp)
                                            Text(col, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Matching Accessories",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = activeDailyTip.matchingAccessories.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenDiscover(activeDailyTip.vibe) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Browse ${activeDailyTip.vibe}")
                            }
                            Button(
                                onClick = {
                                    onOpenAiStylist(
                                        "Style a ${activeDailyTip.vibe} outfit inspired by \"${activeDailyTip.featuredTitle}\" using colors ${activeDailyTip.colorCombos.joinToString(", ")}."
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Try with AI")
                            }
                        }
                    }
                }
            }
        }

        // Recently Viewed Section (only shown once the user has opened looks)
        if (recentlyViewed.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recently Viewed", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { onOpenDiscover(null) }) {
                            Text("Discover")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentlyViewed.take(8), key = { it.id }) { entry ->
                            Card(
                                onClick = { onOpenDiscover(entry.category) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.width(142.dp)
                            ) {
                                Column {
                                    LithfitGarmentImage(
                                        localImagePath = null,
                                        thumbnailBase64 = null,
                                        imageUrl = null,
                                        imageResKey = entry.imageResKey,
                                        paletteNames = entry.colorPalette,
                                        contentDescription = entry.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                                    )
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Featured Cultural & Youth Aesthetics Carousel
        if (discoveryPosts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Featured Lookbooks", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onOpenDiscover(null) }) {
                        Text("Explore All")
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(discoveryPosts.take(6), key = { it.id }) { post ->
                        Card(
                            onClick = { onOpenDiscover(post.sectionTag) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.width(196.dp)
                        ) {
                            Column {
                                LithfitGarmentImage(
                                    localImagePath = null,
                                    thumbnailBase64 = null,
                                    imageUrl = post.imageUrl,
                                    imageResKey = post.imageResKey,
                                    paletteNames = post.colorPalette,
                                    contentDescription = post.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(176.dp)
                                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                )
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${post.category} • ${post.sectionTag}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = post.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Outfit Rotation Peek
        if (savedOutfits.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Saved Rotation", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onOpenWardrobe(1) }) { Text("View All") }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(savedOutfits.take(4), key = { it.id }) { outfit ->
                        HomeSavedOutfitCard(outfit = outfit, onClick = { onOpenWardrobe(1) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStudioTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeSavedOutfitCard(
    outfit: Outfit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(204.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(outfit.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${outfit.occasion} • ${outfit.vibe}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (outfit.colorPalette.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    outfit.colorPalette.take(5).forEach { colorName ->
                        ColorSwatchDot(colorName = colorName, size = 12.dp)
                    }
                }
            }
        }
    }
}
