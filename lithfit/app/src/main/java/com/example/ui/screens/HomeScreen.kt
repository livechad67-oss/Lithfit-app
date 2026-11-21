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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppGuidePreferences
import com.example.data.model.Outfit
import com.example.data.model.SeedCatalogData
import com.example.data.model.UiState
import com.example.ui.LithfitOverlay
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.ContextualTipBanner
import com.example.ui.components.LithfitGarmentImage
import com.example.ui.components.LithfitIconButton
import com.example.ui.components.LithfitLogoMark
import com.example.ui.components.LithfitPillButton
import com.example.ui.components.LithfitPrimaryButton
import com.example.ui.components.LithfitQuickActionsGrid
import com.example.ui.components.LithfitSecondaryButton
import com.example.ui.components.LithfitWordmark
import com.example.ui.components.QuickActionType
import com.example.ui.viewmodel.LithfitViewModel
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: LithfitViewModel,
    onOpenOverlay: (LithfitOverlay, String?) -> Unit,
    onOpenOutfits: (sectionIndex: Int) -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenDiscover: (category: String?) -> Unit,
    onOpenAddGarment: () -> Unit,
    onQuickAction: (QuickActionType) -> Unit
) {
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val wardrobeState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val outfitsState by viewModel.outfits.collectAsStateWithLifecycle()
    val discoveryPosts by viewModel.discoveryPosts.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val dismissedTips by viewModel.dismissedTips.collectAsStateWithLifecycle()
    val unreadNotifications by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val wardrobeItems = (wardrobeState as? UiState.Success)?.data.orEmpty()
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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Top Header: Brand + Search + Alerts + Settings Profile
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
                    LithfitLogoMark(size = 30.dp)
                    LithfitWordmark(fontSizeSp = 17f)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitIconButton(
                        icon = Icons.Outlined.Search,
                        contentDescription = "Search LITHFIT",
                        onClick = { onOpenOverlay(LithfitOverlay.GLOBAL_SEARCH, null) },
                        testTag = "home_search_button"
                    )
                    LithfitIconButton(
                        icon = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        badgeCount = unreadNotifications,
                        onClick = { onOpenOverlay(LithfitOverlay.NOTIFICATIONS_INBOX, null) },
                        testTag = "home_notifications_button"
                    )
                    LithfitIconButton(
                        icon = Icons.Outlined.PersonOutline,
                        contentDescription = "Profile and settings",
                        onClick = { onOpenOverlay(LithfitOverlay.PROFILE_SETTINGS, null) },
                        testTag = "home_profile_button"
                    )
                }
            }
        }

        // Greeting + Discover Your Style + Weather Pill
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hey $firstName,",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Discover Your Style",
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                    AssistChip(
                        onClick = { showCityDialog = true },
                        leadingIcon = { Icon(Icons.Outlined.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = {
                            Text(
                                text = weather?.let {
                                    "${it.locationName.substringBefore(",")} • ${it.temperatureC.toInt()}°C"
                                } ?: "Weather",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }

                // Compact Editorial Shortcuts
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LithfitPillButton(
                        text = "Outfit Suggestions",
                        icon = Icons.Outlined.AutoAwesome,
                        selected = true,
                        onClick = {
                            onOpenOverlay(
                                LithfitOverlay.AI_STYLIST,
                                "Suggest a complete $primaryVibe outfit for today using items from my wardrobe."
                            )
                        },
                        testTag = "home_shortcut_outfit_suggestions"
                    )
                    LithfitPillButton(
                        text = "AI Style Chat",
                        icon = Icons.Outlined.ChatBubbleOutline,
                        onClick = { onOpenOverlay(LithfitOverlay.AI_STYLIST, null) },
                        testTag = "home_shortcut_ai_chat"
                    )
                    LithfitPillButton(
                        text = "Outfit Studio",
                        icon = Icons.Outlined.Style,
                        onClick = { onOpenOverlay(LithfitOverlay.AVATAR_STUDIO, null) },
                        testTag = "home_shortcut_outfit_studio"
                    )
                }
            }
        }

        // Core Destinations Grid: AI Stylist, My Wardrobe, Discover, Outfit Studio
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeDestinationCard(
                        title = "LITHFIT AI",
                        subtitle = "Chat, Q&A & outfit styling",
                        icon = Icons.Outlined.AutoAwesome,
                        onClick = { onOpenOverlay(LithfitOverlay.AI_STYLIST, null) },
                        testTag = "home_hub_ai",
                        modifier = Modifier.weight(1f)
                    )
                    HomeDestinationCard(
                        title = "My Wardrobe",
                        subtitle = "${wardrobeItems.size} pieces organized",
                        icon = Icons.Outlined.Checkroom,
                        onClick = onOpenWardrobe,
                        testTag = "home_hub_wardrobe",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeDestinationCard(
                        title = "Discover",
                        subtitle = "Editorial street & lookbooks",
                        icon = Icons.Outlined.Explore,
                        onClick = { onOpenDiscover(null) },
                        testTag = "home_hub_discover",
                        modifier = Modifier.weight(1f)
                    )
                    HomeDestinationCard(
                        title = "Outfit Studio",
                        subtitle = "Model try-on & silhouettes",
                        icon = Icons.Outlined.Style,
                        onClick = { onOpenOverlay(LithfitOverlay.AVATAR_STUDIO, null) },
                        testTag = "home_hub_outfit_studio",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            ContextualTipBanner(
                tipKey = AppGuidePreferences.TIP_HOME,
                title = "Welcome to LITHFIT",
                body = "Build real fits from your closet, plan your calendar in Outfits, or ask AI Stylist for tailored styling advice.",
                dismissedTips = dismissedTips,
                onDismiss = { viewModel.dismissTip(it) }
            )
        }

        // Quick Tools Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick Tools", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        LithfitPillButton(
                            text = "Find My Colour",
                            icon = Icons.Outlined.Palette,
                            onClick = { onOpenOverlay(LithfitOverlay.COLOUR_MATCHING, null) },
                            testTag = "tool_find_colour"
                        )
                    }
                    item {
                        LithfitPillButton(
                            text = "Find My Fit",
                            icon = Icons.Outlined.Straighten,
                            onClick = {
                                onOpenOverlay(
                                    LithfitOverlay.AI_STYLIST,
                                    "Help me pick silhouettes and cuts that fit comfortably for my proportions and $primaryVibe style."
                                )
                            },
                            testTag = "tool_find_fit"
                        )
                    }
                    item {
                        LithfitPillButton(
                            text = "Rate My Style",
                            icon = Icons.Outlined.ThumbUp,
                            onClick = {
                                onOpenOverlay(
                                    LithfitOverlay.AI_STYLIST,
                                    "Attach or describe an outfit and give me constructive, body-neutral styling feedback."
                                )
                            },
                            testTag = "tool_rate_style"
                        )
                    }
                    item {
                        LithfitPillButton(
                            text = "Style Advice",
                            icon = Icons.Outlined.Style,
                            onClick = {
                                onOpenOverlay(
                                    LithfitOverlay.AI_STYLIST,
                                    "Give me practical layering and colour balance tips for today."
                                )
                            },
                            testTag = "tool_style_advice"
                        )
                    }
                }
            }
        }

        // 8 Compact Quick Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
                LithfitQuickActionsGrid(
                    unreadNotificationsCount = unreadNotifications,
                    onActionClick = onQuickAction
                )
            }
        }

        // Your Daily Look Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
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
                        text = "YOUR DAILY LOOK • ${primaryVibe.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                    )
                    Text(
                        text = when {
                            weather != null && wardrobeItems.isNotEmpty() ->
                                "${weather.temperatureC.toInt()}°C ${weather.conditionLabel} — remix from your ${wardrobeItems.size} wardrobe pieces cleanly."
                            wardrobeItems.isNotEmpty() ->
                                "Pair your ${wardrobeItems.first().color} ${wardrobeItems.first().name} with balanced neutrals for today's rotation."
                            else ->
                                "Ask LITHFIT AI for today's personalised outfit plan or build a clean look in Outfit Studio."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = {
                                onOpenOverlay(
                                    LithfitOverlay.AI_STYLIST,
                                    "Create a fresh $primaryVibe daily look using pieces from my wardrobe."
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Style Today", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Surface(
                            onClick = { onOpenOutfits(1) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Build Look", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // Daily Outfit Tip Card
        if (activeDailyTip != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
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
                                    text = "DAILY STYLE NOTE",
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
                                Icon(Icons.Outlined.Refresh, contentDescription = "Next daily outfit tip")
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
                                contentDescription = activeDailyTip.featuredTitle,
                                modifier = Modifier
                                    .width(104.dp)
                                    .height(136.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(activeDailyTip.featuredTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    activeDailyTip.tipBody,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LithfitSecondaryButton(
                                text = "Browse ${activeDailyTip.vibe}",
                                onClick = { onOpenDiscover(activeDailyTip.vibe) },
                                modifier = Modifier.weight(1f)
                            )
                            LithfitPrimaryButton(
                                text = "Ask AI",
                                icon = Icons.Outlined.AutoAwesome,
                                onClick = {
                                    onOpenOverlay(
                                        LithfitOverlay.AI_STYLIST,
                                        "Style a ${activeDailyTip.vibe} outfit inspired by \"${activeDailyTip.featuredTitle}\" using colors ${activeDailyTip.colorCombos.joinToString(", ")}."
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Recently Added Wardrobe Shelf
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Wardrobe (${wardrobeItems.size})", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onOpenAddGarment) { Text("+ Add") }
                        TextButton(onClick = onOpenWardrobe) { Text("Open") }
                    }
                }
                if (wardrobeItems.isEmpty()) {
                    Surface(
                        onClick = onOpenAddGarment,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Column {
                                Text("Add your first piece", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Snap a top, hoodie, trouser, or shoe so LITHFIT can style what you own.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(wardrobeItems.take(8), key = { "home_w_${it.id}" }) { item ->
                            Card(
                                onClick = onOpenWardrobe,
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.width(136.dp)
                            ) {
                                Column {
                                    LithfitGarmentImage(
                                        localImagePath = item.imagePath,
                                        thumbnailBase64 = item.thumbnailBase64,
                                        imageUrl = null,
                                        imageResKey = null,
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(112.dp)
                                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                                    )
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(item.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.color, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Explore Styles Carousel
        if (discoveryPosts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Explore Styles", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onOpenDiscover(null) }) { Text("Explore All") }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(discoveryPosts.take(8), key = { it.id }) { post ->
                        Card(
                            onClick = { onOpenDiscover(post.sectionTag) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.width(184.dp)
                        ) {
                            Column {
                                LithfitGarmentImage(
                                    localImagePath = null,
                                    thumbnailBase64 = null,
                                    imageUrl = post.imageUrl,
                                    imageResKey = post.imageResKey,
                                    contentDescription = post.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(168.dp)
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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

        // Recently Saved Outfits
        if (savedOutfits.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recently Saved Outfits", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onOpenOutfits(0) }) { Text("View All") }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(savedOutfits.take(5), key = { it.id }) { outfit ->
                        HomeSavedOutfitCard(outfit = outfit, onClick = { onOpenOutfits(0) })
                    }
                }
            }
        }

        // Clean Wardrobe Summary Footer Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Checkroom, contentDescription = null)
                        Column {
                            Text("${wardrobeItems.size} Garments • ${savedOutfits.size} Outfits", style = MaterialTheme.typography.titleSmall)
                            Text("View Outfit Calendar & Insights", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = { onOpenOutfits(3) }) {
                        Text("Insights")
                    }
                }
            }
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(196.dp)
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

@Composable
private fun HomeDestinationCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
