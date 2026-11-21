package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppGuidePreferences
import com.example.core.ImageStorageHelper
import com.example.data.model.GarmentCategory
import com.example.data.model.LithfitConstants
import com.example.data.model.UiState
import com.example.data.model.WardrobeItem
import com.example.ui.components.ColorSwatchChip
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.ContextualTipBanner
import com.example.ui.components.LithfitGarmentImage
import com.example.ui.components.LithfitPillButton
import com.example.ui.components.LithfitPrimaryButton
import com.example.ui.viewmodel.LithfitViewModel

enum class WardrobeSortOption(val label: String) {
    NEWEST("Newest"),
    NAME_AZ("Name A–Z"),
    CATEGORY("Category"),
    COLOUR("Colour"),
    FAVORITES("Favorites")
}

@Composable
fun WardrobeScreen(
    viewModel: LithfitViewModel,
    onOpenAddGarment: () -> Unit,
    onOpenOutfitsTab: (sectionIndex: Int) -> Unit,
    onOpenAvatarStudio: () -> Unit,
    onStyleItemWithAi: (String) -> Unit
) {
    val context = LocalContext.current
    val itemsState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val recognitionState by viewModel.garmentRecognition.collectAsStateWithLifecycle()
    val catalogue by viewModel.catalogueProducts.collectAsStateWithLifecycle()
    val dismissedTips by viewModel.dismissedTips.collectAsStateWithLifecycle()
    val personalPhotoRevision by viewModel.personalPhotoRevision.collectAsStateWithLifecycle()

    val wardrobeItems = (itemsState as? UiState.Success)?.data.orEmpty()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<GarmentCategory?>(null) }
    var selectedColorFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCollectionFilter by rememberSaveable { mutableStateOf<String?>(null) } // "Workwear", "Weekend", "Wishlist"
    var sortOption by rememberSaveable { mutableStateOf(WardrobeSortOption.NEWEST) }
    var groupedFormation by rememberSaveable { mutableStateOf(true) }

    var editingItem by remember { mutableStateOf<WardrobeItem?>(null) }
    var showPersonalPhotoSheet by remember { mutableStateOf(false) }
    var showWardrobeGuideDialog by remember { mutableStateOf(false) }

    val personalPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.savePersonalFullBodyPhoto(context, uri)
        }
    }

    val filteredItems = remember(
        wardrobeItems,
        searchQuery,
        selectedCategory,
        selectedColorFilter,
        selectedCollectionFilter,
        sortOption
    ) {
        val q = searchQuery.trim().lowercase()
        val matching = wardrobeItems.filter { item ->
            val catOk = selectedCategory == null || item.categoryEnum() == selectedCategory
            val colOk = selectedColorFilter == null || item.color.contains(selectedColorFilter!!, ignoreCase = true)
            val collectionOk = when (selectedCollectionFilter) {
                "Wishlist" -> item.favorite
                "Workwear" -> item.collectionTags.any { it.equals("Workwear", true) } ||
                    item.style.orEmpty().contains("smart", true) ||
                    item.style.orEmpty().contains("formal", true) ||
                    item.style.orEmpty().contains("minimal", true)
                "Weekend" -> item.collectionTags.any { it.equals("Weekend", true) } ||
                    item.style.orEmpty().contains("casual", true) ||
                    item.style.orEmpty().contains("street", true)
                else -> true
            }
            val queryOk = q.isBlank() ||
                item.name.lowercase().contains(q) ||
                item.color.lowercase().contains(q) ||
                item.categoryEnum().label.lowercase().contains(q) ||
                (item.subType?.lowercase()?.contains(q) == true) ||
                (item.garmentType?.lowercase()?.contains(q) == true) ||
                (item.brand?.lowercase()?.contains(q) == true) ||
                (item.style?.lowercase()?.contains(q) == true) ||
                (item.material?.lowercase()?.contains(q) == true) ||
                (item.notes?.lowercase()?.contains(q) == true)
            catOk && colOk && collectionOk && queryOk
        }

        when (sortOption) {
            WardrobeSortOption.NEWEST -> matching.sortedByDescending { it.updatedAtMillis }
            WardrobeSortOption.NAME_AZ -> matching.sortedBy { it.name.lowercase() }
            WardrobeSortOption.CATEGORY -> matching.sortedBy { it.categoryEnum().label }
            WardrobeSortOption.COLOUR -> matching.sortedBy { it.color.lowercase() }
            WardrobeSortOption.FAVORITES -> matching.sortedByDescending { if (it.favorite) 1 else 0 }
        }
    }

    val formationShelves = remember(filteredItems) {
        val shelfOrder = listOf(
            GarmentCategory.JACKETS,
            GarmentCategory.TOPS,
            GarmentCategory.HOODIES,
            GarmentCategory.DRESSES,
            GarmentCategory.TROUSERS,
            GarmentCategory.JEANS,
            GarmentCategory.SKIRTS,
            GarmentCategory.SHOES,
            GarmentCategory.ACCESSORIES,
            GarmentCategory.OTHER
        )
        val byCat = filteredItems.groupBy { it.categoryEnum() }
        shelfOrder.mapNotNull { cat ->
            val list = byCat[cat]
            if (list.isNullOrEmpty()) null else cat to list
        }
    }

    editingItem?.let { target ->
        AddOrEditGarmentDialog(
            existing = target,
            recognitionState = recognitionState,
            catalogue = catalogue,
            onAnalyzeUri = { uri -> viewModel.analyzeWardrobeUri(context, uri) },
            onImportUrl = { url -> viewModel.importImageFromUrl(context, url) },
            onDismiss = {
                editingItem = null
                viewModel.resetGarmentRecognition()
            },
            onSave = { item ->
                viewModel.saveWardrobeItem(item) { ok ->
                    if (ok) {
                        editingItem = null
                        viewModel.resetGarmentRecognition()
                    }
                }
            },
            onDelete = {
                viewModel.deleteWardrobeItem(target.id)
                editingItem = null
            }
        )
    }

    if (showPersonalPhotoSheet) {
        val photoFile = remember(personalPhotoRevision) {
            ImageStorageHelper.getPersonalFullBodyPhotoFile(context)
        }
        val bitmap = remember(photoFile, personalPhotoRevision) {
            photoFile?.let { BitmapFactory.decodeFile(it.absolutePath) }
        }
        AlertDialog(
            onDismissRequest = { showPersonalPhotoSheet = false },
            title = { Text("Private Reference Photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Stored strictly on your device—never shared publicly, never uploaded to any feed, and never rated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Private styling photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("No private reference photo set", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                personalPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (bitmap == null) "Choose Photo" else "Replace")
                        }
                        if (bitmap != null) {
                            OutlinedButton(
                                onClick = { viewModel.deletePersonalFullBodyPhoto(context) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Delete") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPersonalPhotoSheet = false }) { Text("Done") } }
        )
    }

    if (showWardrobeGuideDialog) {
        WardrobeGuideDialog(
            onDismiss = { showWardrobeGuideDialog = false },
            onOpenAddGarment = {
                showWardrobeGuideDialog = false
                onOpenAddGarment()
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wardrobe", style = MaterialTheme.typography.displayMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitPillButton(
                        text = "Guide",
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = { showWardrobeGuideDialog = true },
                        testTag = "wardrobe_guide_button"
                    )
                    LithfitPillButton(
                        text = "Add",
                        icon = Icons.Outlined.Add,
                        selected = true,
                        onClick = onOpenAddGarment,
                        testTag = "wardrobe_add_button"
                    )
                }
            }

            // Quick shortcuts into Outfits, Insights & Style Studio
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    LithfitPillButton(
                        text = "Outfit Builder",
                        icon = Icons.Outlined.Style,
                        onClick = { onOpenOutfitsTab(1) }
                    )
                }
                item {
                    LithfitPillButton(
                        text = "Insights",
                        onClick = { onOpenOutfitsTab(3) }
                    )
                }
                item {
                    LithfitPillButton(
                        text = "Outfit Studio",
                        icon = Icons.Outlined.AutoAwesome,
                        onClick = onOpenAvatarStudio,
                        testTag = "wardrobe_outfit_studio_button"
                    )
                }
                item {
                    LithfitPillButton(
                        text = if (personalPhotoRevision > 0) "My Photo" else "Private Photo",
                        icon = Icons.Outlined.Lock,
                        onClick = { showPersonalPhotoSheet = true },
                        testTag = "personal_photo_button"
                    )
                }
            }

            ContextualTipBanner(
                tipKey = AppGuidePreferences.TIP_WARDROBE,
                title = "Digital Closet",
                body = "Snap clothing photos for automatic recognition, sort by category or colour, and send any item straight to AI Stylist.",
                dismissedTips = dismissedTips,
                onDismiss = { viewModel.dismissTip(it) }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search hoodies, cargos, colours, brands...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wardrobe_search_input")
            )
        }

        // Collection + Category Filters
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null && selectedCollectionFilter == null && selectedColorFilter == null,
                    onClick = {
                        selectedCategory = null
                        selectedCollectionFilter = null
                        selectedColorFilter = null
                    },
                    label = { Text("All") }
                )
            }
            listOf("Wishlist", "Workwear", "Weekend").forEach { colTag ->
                item {
                    FilterChip(
                        selected = selectedCollectionFilter == colTag,
                        onClick = {
                            selectedCollectionFilter = if (selectedCollectionFilter == colTag) null else colTag
                        },
                        label = { Text(colTag) }
                    )
                }
            }
            items(GarmentCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text(cat.label) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Sort + Formation/Grid Switch
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(WardrobeSortOption.entries) { opt ->
                FilterChip(
                    selected = sortOption == opt,
                    onClick = { sortOption = opt },
                    label = { Text("Sort: ${opt.label}") }
                )
            }
            item {
                FilterChip(
                    selected = groupedFormation,
                    onClick = { groupedFormation = true },
                    leadingIcon = { Icon(Icons.Outlined.ViewAgenda, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text("Formation") },
                    modifier = Modifier.testTag("wardrobe_view_formation")
                )
            }
            item {
                FilterChip(
                    selected = !groupedFormation,
                    onClick = { groupedFormation = false },
                    leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text("Grid") },
                    modifier = Modifier.testTag("wardrobe_view_grid")
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LithfitConstants.COLOR_SWATCHES) { swatch ->
                ColorSwatchChip(
                    colorName = swatch.name,
                    selected = selectedColorFilter == swatch.name,
                    onClick = {
                        selectedColorFilter = if (selectedColorFilter == swatch.name) null else swatch.name
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (itemsState is UiState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.Checkroom,
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (wardrobeItems.isEmpty()) "Your wardrobe is ready" else "No matching pieces",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Tap the + button to capture or pick clothing photos with automatic garment & colour recognition.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (wardrobeItems.isNotEmpty()) {
                        OutlinedButton(onClick = {
                            searchQuery = ""
                            selectedCategory = null
                            selectedCollectionFilter = null
                            selectedColorFilter = null
                        }) {
                            Text("Clear Filters")
                        }
                    }
                    LithfitPrimaryButton(
                        text = if (wardrobeItems.isEmpty()) "Add First Garment" else "Add Garment",
                        onClick = onOpenAddGarment
                    )
                }
            }
        } else if (groupedFormation) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                formationShelves.forEach { (shelfCat, shelfItems) ->
                    item(key = "shelf_header_${shelfCat.name}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(shelfCat.label, style = MaterialTheme.typography.titleMedium)
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${shelfItems.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    val chunkedRows = shelfItems.chunked(2)
                    items(
                        items = chunkedRows,
                        key = { row -> "row_${shelfCat.name}_${row.joinToString("_") { it.id }}" }
                    ) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    WardrobeGridCard(
                                        item = item,
                                        onClick = { editingItem = item },
                                        onToggleFavorite = { viewModel.toggleWardrobeFavorite(item) },
                                        onAskStylist = {
                                            onStyleItemWithAi(
                                                "Suggest an outfit and accessories built around my ${item.color} ${item.name} (${item.categoryEnum().label})."
                                            )
                                        }
                                    )
                                }
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    WardrobeGridCard(
                        item = item,
                        onClick = { editingItem = item },
                        onToggleFavorite = { viewModel.toggleWardrobeFavorite(item) },
                        onAskStylist = {
                            onStyleItemWithAi(
                                "Suggest an outfit and accessories built around my ${item.color} ${item.name} (${item.categoryEnum().label})."
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WardrobeGridCard(
    item: WardrobeItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAskStylist: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                LithfitGarmentImage(
                    localImagePath = item.imagePath,
                    thumbnailBase64 = item.thumbnailBase64,
                    imageUrl = null,
                    imageResKey = null,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (item.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (item.favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ColorSwatchDot(colorName = item.color, size = 12.dp)
                    Text(
                        text = "${item.subType ?: item.categoryEnum().label} • ${item.color}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = onAskStylist,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Style this", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun WardrobeGuideDialog(
    onDismiss: () -> Unit,
    onOpenAddGarment: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Checkroom, contentDescription = null)
                Text("How to Use Your Wardrobe")
            }
        },
        text = {
            Column(
                modifier = Modifier.testTag("wardrobe_guide_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WardrobeGuideStepRow(
                    step = "1",
                    title = "Capture or Upload Clothes",
                    detail = "Tap + to select Camera or Gallery for any shirt, hoodie, jacket, trouser, or shoe."
                )
                WardrobeGuideStepRow(
                    step = "2",
                    title = "Automatic Cloth Identification",
                    detail = "LITHFIT automatically detects clothing type, colourway, and fabric cleanly."
                )
                WardrobeGuideStepRow(
                    step = "3",
                    title = "Organized Shelf Formation",
                    detail = "View items grouped by shelf or in a unified grid sorted by newest, colour, or favorites."
                )
                WardrobeGuideStepRow(
                    step = "4",
                    title = "Build, Schedule & Track Wears",
                    detail = "Open the Outfits tab to build looks manually, schedule on Calendar, and view real Wardrobe Insights."
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
        confirmButton = {
            Button(onClick = onOpenAddGarment) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Scan a Garment")
            }
        }
    )
}

@Composable
private fun WardrobeGuideStepRow(
    step: String,
    title: String,
    detail: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
