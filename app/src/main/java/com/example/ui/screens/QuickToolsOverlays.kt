package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LithfitConstants
import com.example.data.model.UiState
import com.example.data.repository.AuthRepository
import com.example.ui.components.ColorSwatchChip
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.LithfitGarmentImage
import com.example.ui.components.LithfitPrimaryButton
import com.example.ui.components.LithfitSecondaryButton
import com.example.ui.viewmodel.LithfitViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsInboxOverlay(
    viewModel: LithfitViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val notifications by viewModel.appNotifications.collectAsStateWithLifecycle()
    val ownerNotifications by viewModel.ownerNotifications.collectAsStateWithLifecycle()
    val currentUserEmail = remember(viewModel.currentUserId) {
        AuthRepository().currentUser(context)?.email
    }
    val isOwner = currentUserEmail.equals(LithfitConstants.OWNER_NOTIFY_EMAIL, ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("overlay_back_button")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearNotifications() }) {
                            Text("Clear")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isOwner) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("owner_notifications_section")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null)
                                Text("Owner Account Registration Alerts", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                "Visible exclusively to ${LithfitConstants.OWNER_NOTIFY_EMAIL}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (ownerNotifications.isEmpty()) {
                                Text("No new member alerts recorded yet.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                ownerNotifications.forEach { alert ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(alert.message, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    "${alert.authMethod} • ${formatTimestamp(alert.createdAtMillis)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (!alert.read) {
                                                TextButton(onClick = { viewModel.markOwnerNotificationRead(alert.id) }) {
                                                    Text("Mark read")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(36.dp))
                            Text("All caught up", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Outfit reminders, calendar alerts, and security updates appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    notif.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    formatTimestamp(notif.createdAtMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!notif.read) {
                                IconButton(onClick = { viewModel.markNotificationRead(notif.id) }) {
                                    Icon(Icons.Outlined.DoneAll, contentDescription = "Mark read")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistOverlay(
    viewModel: LithfitViewModel,
    onBack: () -> Unit,
    onAskStylist: (String) -> Unit,
    onOpenRadar: () -> Unit
) {
    BackHandler(onBack = onBack)
    val wardrobeState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val outfitsState by viewModel.outfits.collectAsStateWithLifecycle()

    val favoriteGarments = (wardrobeState as? UiState.Success)?.data.orEmpty().filter { it.favorite }
    val favoriteOutfits = (outfitsState as? UiState.Success)?.data.orEmpty().filter { it.favorite }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved & Wishlist", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("overlay_back_button")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (favoriteGarments.isEmpty() && favoriteOutfits.isEmpty() && savedItems.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(38.dp))
                            Text("Your Wishlist is Empty", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Heart garments in your Wardrobe or bookmark pieces in Explore & Style Radar to keep them organized here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LithfitSecondaryButton(text = "Explore Style Radar", onClick = onOpenRadar)
                        }
                    }
                }
            }

            if (favoriteGarments.isNotEmpty()) {
                item { Text("Favourite Wardrobe Pieces (${favoriteGarments.size})", style = MaterialTheme.typography.titleMedium) }
                items(favoriteGarments, key = { "fav_w_${it.id}" }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LithfitGarmentImage(
                                localImagePath = item.imagePath,
                                thumbnailBase64 = item.thumbnailBase64,
                                imageUrl = null,
                                imageResKey = null,
                                contentDescription = item.name,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${item.categoryEnum().label} • ${item.color}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = {
                                onAskStylist("Style my favourite ${item.color} ${item.name} in a fresh outfit.")
                            }) {
                                Text("Style")
                            }
                        }
                    }
                }
            }

            if (savedItems.isNotEmpty()) {
                item { Text("Bookmarked Looks & Products (${savedItems.size})", style = MaterialTheme.typography.titleMedium) }
                items(savedItems, key = { "saved_${it.id}" }) { saved ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(saved.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${saved.category} • ${saved.subtitle.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColourMatchingOverlay(
    viewModel: LithfitViewModel,
    onBack: () -> Unit,
    onAskStylistWithPalette: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val wardrobeState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val wardrobe = (wardrobeState as? UiState.Success)?.data.orEmpty()
    var baseColor by rememberSaveable { mutableStateOf("Cream") }
    var selectedNeutralCombo by rememberSaveable { mutableStateOf(LithfitConstants.NEUTRAL_COMBOS.first()) }

    val complementaryColors = remember(baseColor) {
        when (baseColor.lowercase()) {
            "cream", "beige", "white" -> listOf("Charcoal", "Brown", "Olive", "Navy", "Burgundy")
            "black", "charcoal", "grey" -> listOf("Cream", "White", "Sky Blue", "Burgundy", "Lavender")
            "navy", "blue", "sky blue" -> listOf("Cream", "Beige", "Brown", "White", "Charcoal")
            "olive", "green" -> listOf("Cream", "Chocolate", "Black", "Beige", "Orange")
            "pink", "lavender", "purple" -> listOf("Black", "White", "Charcoal", "Grey", "Blue")
            "red", "burgundy", "orange" -> listOf("Cream", "Black", "Navy", "Beige", "Charcoal")
            else -> listOf("Black", "Cream", "Charcoal", "White")
        }
    }

    val ownedMatches = remember(wardrobe, baseColor, complementaryColors) {
        wardrobe.filter { item ->
            item.color.equals(baseColor, ignoreCase = true) ||
                complementaryColors.any { comp -> item.color.contains(comp, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Colour Matching", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("overlay_back_button")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Pick a base shade to see balanced complementary pairings and matching garments already in your wardrobe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text("Select Base Colour", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LithfitConstants.COLOR_SWATCHES) { swatch ->
                        ColorSwatchChip(
                            colorName = swatch.name,
                            selected = baseColor == swatch.name,
                            onClick = { baseColor = swatch.name }
                        )
                    }
                }
            }

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
                            ColorSwatchDot(colorName = baseColor, size = 20.dp, selected = true)
                            Text("Pairs Best With $baseColor", style = MaterialTheme.typography.titleMedium)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            complementaryColors.forEach { col ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ColorSwatchDot(colorName = col, size = 12.dp)
                                        Text(col, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        LithfitPrimaryButton(
                            text = "Ask AI to Style $baseColor",
                            icon = Icons.Outlined.AutoAwesome,
                            onClick = {
                                onAskStylistWithPalette(
                                    "Suggest a balanced outfit palette built around $baseColor paired with ${complementaryColors.take(2).joinToString(" and ")}."
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Text("Curated Neutral Foundations", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LithfitConstants.NEUTRAL_COMBOS.forEach { combo ->
                        Surface(
                            onClick = { selectedNeutralCombo = combo },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedNeutralCombo == combo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (selectedNeutralCombo == combo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = combo,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Matching Pieces in Your Wardrobe (${ownedMatches.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (ownedMatches.isEmpty()) {
                item {
                    Text(
                        "No garments matching $baseColor in your wardrobe yet. Add items to your Closet to match pieces automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(ownedMatches, key = { it.id }) { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ColorSwatchDot(colorName = item.color, size = 16.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${item.categoryEnum().label} • ${item.color}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchOverlay(
    viewModel: LithfitViewModel,
    onBack: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenOutfits: () -> Unit,
    onOpenDiscover: (String) -> Unit,
    onAskStylist: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val wardrobeState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val outfitsState by viewModel.outfits.collectAsStateWithLifecycle()
    val discoveryPosts by viewModel.discoveryPosts.collectAsStateWithLifecycle()
    val products by viewModel.catalogueProducts.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()

    val wardrobeHits = remember(wardrobeState, q) {
        val list = (wardrobeState as? UiState.Success)?.data.orEmpty()
        if (q.isBlank()) list.take(5)
        else list.filter {
            it.name.lowercase().contains(q) || it.color.lowercase().contains(q) || it.categoryEnum().label.lowercase().contains(q)
        }
    }
    val outfitHits = remember(outfitsState, q) {
        val list = (outfitsState as? UiState.Success)?.data.orEmpty()
        if (q.isBlank()) list.take(5)
        else list.filter {
            it.name.lowercase().contains(q) || it.vibe.lowercase().contains(q) || it.occasion.lowercase().contains(q)
        }
    }
    val discoverHits = remember(discoveryPosts, q) {
        if (q.isBlank()) emptyList()
        else discoveryPosts.filter {
            it.title.lowercase().contains(q) || it.category.lowercase().contains(q) || it.colorPalette.any { c -> c.lowercase().contains(q) }
        }.take(8)
    }
    val productHits = remember(products, q) {
        if (q.isBlank()) emptyList()
        else products.filter {
            it.name.lowercase().contains(q) || it.sellerName.lowercase().contains(q) || it.color.lowercase().contains(q)
        }.take(8)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search LITHFIT", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("overlay_back_button")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search wardrobe, outfits, styles, colours...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input")
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (wardrobeHits.isNotEmpty()) {
                    item { Text("Wardrobe (${wardrobeHits.size})", style = MaterialTheme.typography.titleSmall) }
                    items(wardrobeHits, key = { "gs_w_${it.id}" }) { item ->
                        SearchResultRow(
                            title = item.name,
                            subtitle = "${item.categoryEnum().label} • ${item.color}",
                            onClick = onOpenWardrobe
                        )
                    }
                }
                if (outfitHits.isNotEmpty()) {
                    item { Text("Saved Outfits (${outfitHits.size})", style = MaterialTheme.typography.titleSmall) }
                    items(outfitHits, key = { "gs_o_${it.id}" }) { outfit ->
                        SearchResultRow(
                            title = outfit.name,
                            subtitle = "${outfit.occasion} • ${outfit.vibe}",
                            onClick = onOpenOutfits
                        )
                    }
                }
                if (discoverHits.isNotEmpty()) {
                    item { Text("Explore Looks (${discoverHits.size})", style = MaterialTheme.typography.titleSmall) }
                    items(discoverHits, key = { "gs_d_${it.id}" }) { post ->
                        SearchResultRow(
                            title = post.title,
                            subtitle = "${post.category} • ${post.sectionTag}",
                            onClick = { onOpenDiscover(post.sectionTag) }
                        )
                    }
                }
                if (productHits.isNotEmpty()) {
                    item { Text("Style Radar (${productHits.size})", style = MaterialTheme.typography.titleSmall) }
                    items(productHits, key = { "gs_p_${it.id}" }) { prod ->
                        SearchResultRow(
                            title = prod.name,
                            subtitle = "${prod.sellerName} • ${prod.currency} ${prod.price.toInt()}",
                            onClick = { onOpenDiscover("Radar") }
                        )
                    }
                }
                if (q.isNotBlank() && wardrobeHits.isEmpty() && outfitHits.isEmpty() && discoverHits.isEmpty() && productHits.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("No direct matches for \"$query\"", style = MaterialTheme.typography.titleMedium)
                                LithfitSecondaryButton(
                                    text = "Ask AI Stylist about \"$query\"",
                                    icon = Icons.Outlined.AutoAwesome,
                                    onClick = { onAskStylist("Style ideas around $query") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.Checkroom, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    if (millis <= 0L) return "Just now"
    return runCatching {
        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(millis))
    }.getOrDefault("")
}
