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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.Outfit
import com.example.data.model.UiState
import com.example.data.model.WardrobeItem
import com.example.ui.components.ColorSwatchChip
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.ContextualTipBanner
import com.example.ui.components.LithfitGarmentImage
import com.example.ui.viewmodel.LithfitViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WardrobeScreen(
    viewModel: LithfitViewModel,
    requestedSection: Int? = null,
    onClearRequestedSection: () -> Unit = {},
    onStyleItemWithAi: (String) -> Unit,
    onOpenStyleRadarForOutfit: (Outfit) -> Unit,
    onPreviewOutfitImage: (Outfit) -> Unit
) {
    val context = LocalContext.current
    val itemsState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val outfitsState by viewModel.outfits.collectAsStateWithLifecycle()
    val collections by viewModel.outfitCollections.collectAsStateWithLifecycle()
    val recognitionState by viewModel.garmentRecognition.collectAsStateWithLifecycle()
    val catalogue by viewModel.catalogueProducts.collectAsStateWithLifecycle()
    val dismissedTips by viewModel.dismissedTips.collectAsStateWithLifecycle()
    val personalPhotoRevision by viewModel.personalPhotoRevision.collectAsStateWithLifecycle()

    val wardrobeItems = (itemsState as? UiState.Success)?.data.orEmpty()
    val outfits = (outfitsState as? UiState.Success)?.data.orEmpty()

    var activeSection by rememberSaveable { mutableIntStateOf(0) } // 0=Closet, 1=Outfits, 2=Studio, 3=Avatar
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<GarmentCategory?>(null) }
    var selectedColorFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }

    var showAddGarmentDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<WardrobeItem?>(null) }
    var editingOutfit by remember { mutableStateOf<Outfit?>(null) }
    var renamingOutfit by remember { mutableStateOf<Outfit?>(null) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var showPersonalPhotoSheet by remember { mutableStateOf(false) }
    var showWardrobeGuideDialog by remember { mutableStateOf(false) }
    var groupedFormation by rememberSaveable { mutableStateOf(true) }

    val personalPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.savePersonalFullBodyPhoto(context, uri)
        }
    }

    LaunchedEffect(requestedSection) {
        if (requestedSection != null) {
            activeSection = requestedSection.coerceIn(0, 3)
            onClearRequestedSection()
        }
    }

    val filteredItems = remember(wardrobeItems, searchQuery, selectedCategory, selectedColorFilter, onlyFavorites) {
        val q = searchQuery.trim().lowercase()
        wardrobeItems.filter { item ->
            val catOk = selectedCategory == null || item.categoryEnum() == selectedCategory
            val colOk = selectedColorFilter == null || item.color.contains(selectedColorFilter!!, ignoreCase = true)
            val favOk = !onlyFavorites || item.favorite
            val queryOk = q.isBlank() ||
                item.name.lowercase().contains(q) ||
                item.color.lowercase().contains(q) ||
                item.categoryEnum().label.lowercase().contains(q) ||
                (item.subType?.lowercase()?.contains(q) == true) ||
                (item.garmentType?.lowercase()?.contains(q) == true) ||
                (item.brand?.lowercase()?.contains(q) == true) ||
                (item.style?.lowercase()?.contains(q) == true) ||
                (item.material?.lowercase()?.contains(q) == true) ||
                (item.pattern?.lowercase()?.contains(q) == true) ||
                (item.notes?.lowercase()?.contains(q) == true)
            catOk && colOk && favOk && queryOk
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

    if (showAddGarmentDialog || editingItem != null) {
        AddOrEditGarmentDialog(
            existing = editingItem,
            recognitionState = recognitionState,
            catalogue = catalogue,
            onAnalyzeUri = { uri -> viewModel.analyzeWardrobeUri(context, uri) },
            onImportUrl = { url -> viewModel.importImageFromUrl(context, url) },
            onDismiss = {
                showAddGarmentDialog = false
                editingItem = null
                viewModel.resetGarmentRecognition()
            },
            onSave = { item ->
                viewModel.saveWardrobeItem(item) { ok ->
                    if (ok) {
                        showAddGarmentDialog = false
                        editingItem = null
                        viewModel.resetGarmentRecognition()
                    }
                }
            },
            onDelete = editingItem?.let { item ->
                {
                    viewModel.deleteWardrobeItem(item.id)
                    editingItem = null
                }
            }
        )
    }

    if (showCreateCollectionDialog) {
        var colName by remember { mutableStateOf("") }
        var colDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateCollectionDialog = false },
            title = { Text("New Outfit Collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = colName,
                        onValueChange = { colName = it },
                        label = { Text("Collection Name (e.g. Weekend looks)") },
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
            dismissButton = {
                TextButton(onClick = { showCreateCollectionDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createCollection(colName, colDesc.ifBlank { null })
                        showCreateCollectionDialog = false
                    },
                    enabled = colName.isNotBlank()
                ) {
                    Text("Create")
                }
            }
        )
    }

    renamingOutfit?.let { target ->
        var newTitle by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renamingOutfit = null },
            title = { Text("Rename Outfit") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Outfit Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = { TextButton(onClick = { renamingOutfit = null }) { Text("Cancel") } },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameOutfit(target.id, newTitle)
                        renamingOutfit = null
                    },
                    enabled = newTitle.isNotBlank()
                ) { Text("Save") }
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
            title = { Text("Private Styling Photo") },
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
                            contentDescription = "Private full-body styling reference photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("No private styling photo set", style = MaterialTheme.typography.bodySmall)
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
                                onClick = {
                                    viewModel.deletePersonalFullBodyPhoto(context)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPersonalPhotoSheet = false }) { Text("Done") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (activeSection == 0) {
                FloatingActionButton(
                    onClick = {
                        viewModel.resetGarmentRecognition()
                        showAddGarmentDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_garment_fab")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add clothing item")
                }
            } else if (activeSection == 1) {
                FloatingActionButton(
                    onClick = {
                        editingOutfit = null
                        activeSection = 2
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_outfit_fab")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Build new outfit")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showWardrobeGuideDialog) {
                    WardrobeGuideDialog(
                        onDismiss = { showWardrobeGuideDialog = false },
                        onOpenAddGarment = {
                            showWardrobeGuideDialog = false
                            viewModel.resetGarmentRecognition()
                            showAddGarmentDialog = true
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Wardrobe", style = MaterialTheme.typography.displayMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { showWardrobeGuideDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("wardrobe_guide_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Guide")
                        }
                        if (activeSection == 0) {
                            OutlinedButton(
                                onClick = { showPersonalPhotoSheet = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("personal_photo_button")
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (personalPhotoRevision > 0) "My Photo" else "Photo")
                            }
                        }
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("Closet", "Outfits", "Studio", "Avatar").forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = activeSection == idx,
                            onClick = { activeSection = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = 4),
                            modifier = Modifier.testTag("wardrobe_tab_${label.lowercase()}")
                        ) {
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (activeSection == 0 || activeSection == 1) {
                    ContextualTipBanner(
                        tipKey = AppGuidePreferences.TIP_WARDROBE,
                        title = "Digital Closet, Outfits & Avatar",
                        body = "Add clothing photos, mix outfits in Studio, rename combinations, or switch to Avatar to dress your personal model.",
                        dismissedTips = dismissedTips,
                        onDismiss = { viewModel.dismissTip(it) }
                    )
                }
            }

            when (activeSection) {
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null && !onlyFavorites && selectedColorFilter == null,
                                    onClick = {
                                        selectedCategory = null
                                        selectedColorFilter = null
                                        onlyFavorites = false
                                    },
                                    label = { Text("All") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = onlyFavorites,
                                    onClick = { onlyFavorites = !onlyFavorites },
                                    label = { Text("Favorites") }
                                )
                            }
                            items(GarmentCategory.entries) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = {
                                        selectedCategory = if (selectedCategory == cat) null else cat
                                    },
                                    label = { Text(cat.label) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredItems.size} piece${if (filteredItems.size == 1) "" else "s"} • ${formationShelves.size} shelf${if (formationShelves.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = groupedFormation,
                                    onClick = { groupedFormation = true },
                                    leadingIcon = { Icon(Icons.Outlined.ViewAgenda, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                    label = { Text("Formation") },
                                    modifier = Modifier.testTag("wardrobe_view_formation")
                                )
                                FilterChip(
                                    selected = !groupedFormation,
                                    onClick = { groupedFormation = false },
                                    leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                    label = { Text("Grid") },
                                    modifier = Modifier.testTag("wardrobe_view_grid")
                                )
                            }
                        }

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
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (wardrobeItems.isEmpty()) "Your wardrobe is ready" else "No matching pieces",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Add pieces by photo or from our expanded clothing & 21-colour library so LITHFIT AI can style what you own.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (wardrobeItems.isNotEmpty()) {
                                        OutlinedButton(onClick = {
                                            searchQuery = ""
                                            selectedCategory = null
                                            selectedColorFilter = null
                                            onlyFavorites = false
                                        }) {
                                            Text("Clear Filters")
                                        }
                                    }
                                    Button(onClick = { showAddGarmentDialog = true }) {
                                        Text(if (wardrobeItems.isEmpty()) "Add First Garment" else "Add Garment")
                                    }
                                }
                            }
                        } else if (groupedFormation) {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 92.dp),
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
                                            Text(
                                                text = shelfCat.label,
                                                style = MaterialTheme.typography.titleMedium
                                            )
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
                                                            onStyleItemWithAi("Suggest an outfit and accessories built around my ${item.color} ${item.name} (${item.categoryEnum().label}).")
                                                        }
                                                    )
                                                }
                                            }
                                            if (rowItems.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 92.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    WardrobeGridCard(
                                        item = item,
                                        onClick = { editingItem = item },
                                        onToggleFavorite = { viewModel.toggleWardrobeFavorite(item) },
                                        onAskStylist = {
                                            onStyleItemWithAi("Suggest an outfit and accessories built around my ${item.color} ${item.name} (${item.categoryEnum().label}).")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Collections (${collections.size})", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = { showCreateCollectionDialog = true }) {
                                    Text("+ New Collection")
                                }
                            }
                            if (collections.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(collections, key = { it.id }) { col ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = col.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (outfits.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("No saved outfits yet", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Open the interactive Outfit Studio to mix garments, switch styles, test colourways, and save fits.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(onClick = { activeSection = 2 }) {
                                            Text("Open Outfit Studio")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(outfits, key = { it.id }) { outfit ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(outfit.name, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    "${outfit.occasion} • ${outfit.vibe}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row {
                                                IconButton(onClick = { renamingOutfit = outfit }) {
                                                    Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = "Rename outfit")
                                                }
                                                IconButton(onClick = {
                                                    editingOutfit = outfit
                                                    activeSection = 2
                                                }) {
                                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit outfit")
                                                }
                                                IconButton(onClick = { viewModel.duplicateOutfit(outfit) }) {
                                                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate outfit")
                                                }
                                                IconButton(onClick = { viewModel.deleteOutfit(outfit.id) }) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete outfit")
                                                }
                                            }
                                        }

                                        if (outfit.colorPalette.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                outfit.colorPalette.take(6).forEach { col ->
                                                    ColorSwatchDot(colorName = col, size = 14.dp)
                                                }
                                            }
                                        }

                                        if (outfit.itemNames.isNotEmpty()) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                outfit.itemNames.forEach { piece ->
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = piece,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = outfit.feedbackStatus == "LIKED",
                                                onClick = { viewModel.setOutfitFeedback(outfit.id, "LIKED") },
                                                leadingIcon = { Icon(Icons.Outlined.ThumbUp, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                                label = { Text("Liked") }
                                            )
                                            FilterChip(
                                                selected = outfit.feedbackStatus == "DISLIKED",
                                                onClick = { viewModel.setOutfitFeedback(outfit.id, "DISLIKED") },
                                                leadingIcon = { Icon(Icons.Outlined.ThumbDown, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                                label = { Text("Pass") }
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onOpenStyleRadarForOutfit(outfit) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Outlined.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Style Radar")
                                            }
                                            Button(
                                                onClick = { onPreviewOutfitImage(outfit) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("AI Preview")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    InteractiveOutfitBuilderPane(
                        wardrobeItems = wardrobeItems,
                        collections = collections.map { it.id to it.name },
                        initialOutfit = editingOutfit,
                        onSaveOutfit = { outfit ->
                            viewModel.saveOutfit(outfit) { ok ->
                                if (ok) {
                                    editingOutfit = null
                                    activeSection = 1
                                }
                            }
                        },
                        onPreviewOutfit = onPreviewOutfitImage,
                        onOpenStyleRadar = onOpenStyleRadarForOutfit
                    )
                }

                else -> {
                    AvatarStudioScreen(
                        viewModel = viewModel,
                        onAskStylistWithAvatar = onStyleItemWithAi
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
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
                    detail = "Tap the + button, then select Camera or Gallery to snap a photo of any shirt, hoodie, jacket, trouser, or shoe."
                )
                WardrobeGuideStepRow(
                    step = "2",
                    title = "Automatic Cloth Identification",
                    detail = "LITHFIT automatically detects the clothing type, colourway, and fabric so you don't have to type everything manually."
                )
                WardrobeGuideStepRow(
                    step = "3",
                    title = "Organized Shelf Formation",
                    detail = "Your closet sorts items into clean top-to-toe shelves (Outerwear, Tops, Hoodies, Trousers, Footwear & Accessories)."
                )
                WardrobeGuideStepRow(
                    step = "4",
                    title = "Build & Generate Outfits",
                    detail = "Switch to Studio to mix combinations or tap 'Style this' on any piece to generate full looks with LITHFIT AI."
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
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
