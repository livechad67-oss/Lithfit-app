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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GarmentCategory
import com.example.data.model.LithfitConstants
import com.example.data.model.Outfit
import com.example.data.model.OutfitSlotGroup
import com.example.data.model.WardrobeItem
import com.example.ui.components.ColorSwatchChip
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.LithfitGarmentImage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveOutfitBuilderPane(
    wardrobeItems: List<WardrobeItem>,
    collections: List<Pair<String, String>>,
    initialOutfit: Outfit?,
    onSaveOutfit: (Outfit) -> Unit,
    onPreviewOutfit: (Outfit) -> Unit,
    onOpenStyleRadar: (Outfit) -> Unit
) {
    var outfitName by remember(initialOutfit) { mutableStateOf(initialOutfit?.name ?: "My Layered Look") }
    var occasion by remember(initialOutfit) { mutableStateOf(initialOutfit?.occasion ?: LithfitConstants.OCCASIONS.first()) }
    var vibe by remember(initialOutfit) { mutableStateOf(initialOutfit?.vibe ?: LithfitConstants.VIBES.first()) }
    var notes by remember(initialOutfit) { mutableStateOf(initialOutfit?.notes.orEmpty()) }
    var selectedCollectionId by remember(initialOutfit) { mutableStateOf(initialOutfit?.collectionId) }

    var previewIsMale by remember { mutableStateOf(true) }
    var previewWideLeg by remember { mutableStateOf(true) }

    var jacketItem by remember(initialOutfit, wardrobeItems) {
        mutableStateOf(wardrobeItems.firstOrNull { it.id == initialOutfit?.jacketItemId })
    }
    var topItem by remember(initialOutfit, wardrobeItems) {
        mutableStateOf(wardrobeItems.firstOrNull { it.id == initialOutfit?.topItemId })
    }
    var bottomItem by remember(initialOutfit, wardrobeItems) {
        mutableStateOf(wardrobeItems.firstOrNull { it.id == initialOutfit?.bottomItemId })
    }
    var shoesItem by remember(initialOutfit, wardrobeItems) {
        mutableStateOf(wardrobeItems.firstOrNull { it.id == initialOutfit?.shoesItemId })
    }
    val selectedAccessories = remember(initialOutfit, wardrobeItems) {
        mutableStateListOf<WardrobeItem>().apply {
            val ids = initialOutfit?.accessoryIds.orEmpty().toSet()
            addAll(wardrobeItems.filter { it.id in ids })
        }
    }
    val customPalette = remember(initialOutfit) {
        mutableStateListOf<String>().apply {
            addAll(initialOutfit?.colorPalette.orEmpty())
        }
    }
    val slotColorOverrides = remember(initialOutfit) {
        mutableStateMapOf<String, String>().apply {
            putAll(initialOutfit?.itemColorOverrides.orEmpty())
        }
    }

    var pickingSlot by remember { mutableStateOf<OutfitSlotGroup?>(null) }
    var editingColorForSlot by remember { mutableStateOf<OutfitSlotGroup?>(null) }

    val topEffectiveColor = slotColorOverrides[OutfitSlotGroup.TOP.name] ?: topItem?.color ?: "Cream"
    val bottomEffectiveColor = slotColorOverrides[OutfitSlotGroup.BOTTOM.name] ?: bottomItem?.color ?: "Charcoal"
    val outerEffectiveColor = slotColorOverrides[OutfitSlotGroup.JACKET.name] ?: jacketItem?.color
    val shoesEffectiveColor = slotColorOverrides[OutfitSlotGroup.SHOES.name] ?: shoesItem?.color ?: "Black"

    val draftOutfit = remember(
        outfitName, occasion, vibe, notes, selectedCollectionId,
        jacketItem, topItem, bottomItem, shoesItem, selectedAccessories.size,
        customPalette.size, slotColorOverrides.toMap()
    ) {
        val slotPairs = listOfNotNull(
            jacketItem?.let { (slotColorOverrides[OutfitSlotGroup.JACKET.name] ?: it.color) to it.name },
            topItem?.let { (slotColorOverrides[OutfitSlotGroup.TOP.name] ?: it.color) to it.name },
            bottomItem?.let { (slotColorOverrides[OutfitSlotGroup.BOTTOM.name] ?: it.color) to it.name },
            shoesItem?.let { (slotColorOverrides[OutfitSlotGroup.SHOES.name] ?: it.color) to it.name }
        ) + selectedAccessories.map { it.color to it.name }

        val colors = (slotPairs.map { it.first } + slotColorOverrides.values + customPalette)
            .distinct()
            .filter { it.isNotBlank() }

        Outfit(
            id = initialOutfit?.id.orEmpty(),
            name = outfitName.ifBlank { "Untitled Look" },
            occasion = occasion,
            vibe = vibe,
            notes = notes.ifBlank { null },
            collectionId = selectedCollectionId,
            topItemId = topItem?.id,
            bottomItemId = bottomItem?.id,
            shoesItemId = shoesItem?.id,
            jacketItemId = jacketItem?.id,
            accessoryIds = selectedAccessories.map { it.id },
            itemNames = slotPairs.map { (col, nm) -> if (col.isNotBlank()) "$col $nm" else nm },
            colorPalette = colors,
            itemColorOverrides = slotColorOverrides.toMap(),
            feedbackStatus = initialOutfit?.feedbackStatus
        )
    }

    // Live Colourway Override Modal for any slot
    editingColorForSlot?.let { slot ->
        val currentOverride = slotColorOverrides[slot.name]
        AlertDialog(
            onDismissRequest = { editingColorForSlot = null },
            title = { Text("Change ${slot.label} Colourway") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Preview a different colourway on your outfit moodboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LithfitConstants.COLOR_SWATCHES.forEach { swatch ->
                            ColorSwatchChip(
                                colorName = swatch.name,
                                selected = currentOverride.equals(swatch.name, ignoreCase = true),
                                onClick = {
                                    slotColorOverrides[slot.name] = swatch.name
                                    editingColorForSlot = null
                                }
                            )
                        }
                    }
                }
            },
            dismissButton = {
                if (currentOverride != null) {
                    TextButton(onClick = {
                        slotColorOverrides.remove(slot.name)
                        editingColorForSlot = null
                    }) {
                        Text("Reset Default")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingColorForSlot = null }) { Text("Close") }
            }
        )
    }

    pickingSlot?.let { slot ->
        val candidates = wardrobeItems.filter { item ->
            when (slot) {
                OutfitSlotGroup.JACKET -> item.categoryEnum() == GarmentCategory.JACKETS
                OutfitSlotGroup.TOP -> item.categoryEnum() in listOf(GarmentCategory.TOPS, GarmentCategory.HOODIES, GarmentCategory.DRESSES)
                OutfitSlotGroup.BOTTOM -> item.categoryEnum() in listOf(GarmentCategory.TROUSERS, GarmentCategory.JEANS, GarmentCategory.SKIRTS)
                OutfitSlotGroup.SHOES -> item.categoryEnum() == GarmentCategory.SHOES
                OutfitSlotGroup.ACCESSORIES -> item.categoryEnum() in listOf(GarmentCategory.ACCESSORIES, GarmentCategory.OTHER)
            }
        }.ifEmpty { wardrobeItems }

        AlertDialog(
            onDismissRequest = { pickingSlot = null },
            title = { Text("Select ${slot.label}") },
            text = {
                if (candidates.isEmpty()) {
                    Text("Add clothing items in the Closet tab first to swap pieces here — or test colourways live above right now.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { it.id }) { candidate ->
                            Card(
                                onClick = {
                                    when (slot) {
                                        OutfitSlotGroup.JACKET -> jacketItem = candidate
                                        OutfitSlotGroup.TOP -> topItem = candidate
                                        OutfitSlotGroup.BOTTOM -> bottomItem = candidate
                                        OutfitSlotGroup.SHOES -> shoesItem = candidate
                                        OutfitSlotGroup.ACCESSORIES -> {
                                            if (selectedAccessories.none { it.id == candidate.id }) {
                                                selectedAccessories.add(candidate)
                                            }
                                        }
                                    }
                                    pickingSlot = null
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LithfitGarmentImage(
                                        localImagePath = candidate.imagePath,
                                        thumbnailBase64 = candidate.thumbnailBase64,
                                        imageUrl = null,
                                        imageResKey = null,
                                        contentDescription = candidate.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Column {
                                        Text(candidate.name, style = MaterialTheme.typography.titleSmall)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            ColorSwatchDot(colorName = candidate.color, size = 12.dp)
                                            Text("${candidate.categoryEnum().label} • ${candidate.color}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingSlot = null }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Outfit Studio", style = MaterialTheme.typography.titleLarge)
        Text(
            "Assemble garment pieces, balance colourways, and preview photorealistic AI styling.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Live Photographic Piece Collage & Moodboard Preview Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutfitMoodboardGrid(
                        jacketItem = jacketItem,
                        topItem = topItem,
                        bottomItem = bottomItem,
                        shoesItem = shoesItem,
                        outerColor = outerEffectiveColor,
                        topColor = topEffectiveColor,
                        bottomColor = bottomEffectiveColor,
                        shoesColor = shoesEffectiveColor,
                        modifier = Modifier
                            .size(width = 120.dp, height = 152.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("outfit_studio_live_preview")
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = outfitName.ifBlank { "Live Studio Look" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$vibe • $occasion",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = previewIsMale,
                                onClick = { previewIsMale = true },
                                shape = SegmentedButtonDefaults.itemShape(0, 2)
                            ) { Text("Him") }
                            SegmentedButton(
                                selected = !previewIsMale,
                                onClick = { previewIsMale = false },
                                shape = SegmentedButtonDefaults.itemShape(1, 2)
                            ) { Text("Her") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = previewWideLeg,
                                onClick = { previewWideLeg = true },
                                label = { Text("Baggy") }
                            )
                            FilterChip(
                                selected = !previewWideLeg,
                                onClick = { previewWideLeg = false },
                                label = { Text("Slim") }
                            )
                        }
                    }
                }

                // Active Piece Swatch Summary Strip
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    outerEffectiveColor?.let { MoodboardColorPill(label = "Outer: $it", colorName = it) }
                    MoodboardColorPill(label = "Top: $topEffectiveColor", colorName = topEffectiveColor)
                    MoodboardColorPill(label = "Bottom: $bottomEffectiveColor", colorName = bottomEffectiveColor)
                    MoodboardColorPill(label = "Shoes: $shoesEffectiveColor", colorName = shoesEffectiveColor)
                }
            }
        }

        OutlinedTextField(
            value = outfitName,
            onValueChange = { outfitName = it },
            label = { Text("Outfit Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("outfit_name_input")
        )

        OutfitSlotCard(
            slotLabel = "Outerwear",
            selectedItem = jacketItem,
            overrideColor = slotColorOverrides[OutfitSlotGroup.JACKET.name],
            onSwap = { pickingSlot = OutfitSlotGroup.JACKET },
            onPickColor = { editingColorForSlot = OutfitSlotGroup.JACKET },
            onClear = {
                jacketItem = null
                slotColorOverrides.remove(OutfitSlotGroup.JACKET.name)
            }
        )
        OutfitSlotCard(
            slotLabel = "Top",
            selectedItem = topItem,
            overrideColor = slotColorOverrides[OutfitSlotGroup.TOP.name],
            onSwap = { pickingSlot = OutfitSlotGroup.TOP },
            onPickColor = { editingColorForSlot = OutfitSlotGroup.TOP },
            onClear = {
                topItem = null
                slotColorOverrides.remove(OutfitSlotGroup.TOP.name)
            }
        )
        OutfitSlotCard(
            slotLabel = "Bottom",
            selectedItem = bottomItem,
            overrideColor = slotColorOverrides[OutfitSlotGroup.BOTTOM.name],
            onSwap = { pickingSlot = OutfitSlotGroup.BOTTOM },
            onPickColor = { editingColorForSlot = OutfitSlotGroup.BOTTOM },
            onClear = {
                bottomItem = null
                slotColorOverrides.remove(OutfitSlotGroup.BOTTOM.name)
            }
        )
        OutfitSlotCard(
            slotLabel = "Footwear",
            selectedItem = shoesItem,
            overrideColor = slotColorOverrides[OutfitSlotGroup.SHOES.name],
            onSwap = { pickingSlot = OutfitSlotGroup.SHOES },
            onPickColor = { editingColorForSlot = OutfitSlotGroup.SHOES },
            onClear = {
                shoesItem = null
                slotColorOverrides.remove(OutfitSlotGroup.SHOES.name)
            }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accessories (${selectedAccessories.size})", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { pickingSlot = OutfitSlotGroup.ACCESSORIES }) {
                        Text("+ Add Accessory")
                    }
                }
                if (selectedAccessories.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedAccessories.forEach { acc ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedAccessories.remove(acc) },
                                label = { Text("${acc.name} ✕") }
                            )
                        }
                    }
                }
            }
        }

        Text("Colour Palette", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LithfitConstants.COLOR_SWATCHES) { swatch ->
                val active = swatch.name in customPalette
                ColorSwatchChip(
                    colorName = swatch.name,
                    selected = active,
                    onClick = {
                        if (active) customPalette.remove(swatch.name) else customPalette.add(swatch.name)
                    }
                )
            }
        }

        Text("Occasion", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LithfitConstants.OCCASIONS) { occ ->
                FilterChip(selected = occasion == occ, onClick = { occasion = occ }, label = { Text(occ) })
            }
        }

        Text("Style & Vibe", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LithfitConstants.VIBES) { vb ->
                FilterChip(selected = vibe == vb, onClick = { vibe = vb }, label = { Text(vb) })
            }
        }

        if (collections.isNotEmpty()) {
            Text("Assign Collection", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(collections) { (id, name) ->
                    FilterChip(
                        selected = selectedCollectionId == id,
                        onClick = { selectedCollectionId = if (selectedCollectionId == id) null else id },
                        label = { Text(name) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Styling Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onOpenStyleRadar(draftOutfit) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Style Radar")
            }
            OutlinedButton(
                onClick = {
                    val silhouetteDesc = "${if (previewIsMale) "Men's" else "Women's"} ${if (previewWideLeg) "baggy / wide-leg" else "slim tailored"} fit"
                    val enrichedNotes = listOfNotNull(draftOutfit.notes, silhouetteDesc).joinToString(" • ")
                    onPreviewOutfit(draftOutfit.copy(notes = enrichedNotes))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("AI Preview")
            }
        }

        Button(
            onClick = { onSaveOutfit(draftOutfit) },
            enabled = outfitName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_outfit_button")
        ) {
            Text(if (initialOutfit == null) "Save Outfit" else "Update Outfit")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OutfitMoodboardGrid(
    jacketItem: WardrobeItem?,
    topItem: WardrobeItem?,
    bottomItem: WardrobeItem?,
    shoesItem: WardrobeItem?,
    outerColor: String?,
    topColor: String,
    bottomColor: String,
    shoesColor: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MoodboardSlotTile(
                item = topItem,
                fallbackLabel = "TOP",
                colorName = topColor,
                modifier = Modifier.weight(1f).fillMaxSize()
            )
            MoodboardSlotTile(
                item = jacketItem,
                fallbackLabel = "OUTER",
                colorName = outerColor ?: topColor,
                modifier = Modifier.weight(1f).fillMaxSize()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MoodboardSlotTile(
                item = bottomItem,
                fallbackLabel = "BOTTOM",
                colorName = bottomColor,
                modifier = Modifier.weight(1f).fillMaxSize()
            )
            MoodboardSlotTile(
                item = shoesItem,
                fallbackLabel = "SHOES",
                colorName = shoesColor,
                modifier = Modifier.weight(1f).fillMaxSize()
            )
        }
    }
}

@Composable
private fun MoodboardSlotTile(
    item: WardrobeItem?,
    fallbackLabel: String,
    colorName: String,
    modifier: Modifier = Modifier
) {
    val hasPhoto = item != null && (!item.imagePath.isNullOrBlank() || !item.thumbnailBase64.isNullOrBlank())
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (hasPhoto) {
            LithfitGarmentImage(
                localImagePath = item?.imagePath,
                thumbnailBase64 = item?.thumbnailBase64,
                imageUrl = null,
                imageResKey = null,
                contentDescription = item?.name ?: fallbackLabel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(LithfitConstants.colorHexForName(colorName)).copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ColorSwatchDot(colorName = colorName, size = 14.dp)
                    Text(
                        text = fallbackLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodboardColorPill(
    label: String,
    colorName: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
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

@Composable
private fun OutfitSlotCard(
    slotLabel: String,
    selectedItem: WardrobeItem?,
    overrideColor: String?,
    onSwap: () -> Unit,
    onPickColor: () -> Unit,
    onClear: () -> Unit
) {
    val activeColor = overrideColor ?: selectedItem?.color
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LithfitGarmentImage(
                    localImagePath = selectedItem?.imagePath,
                    thumbnailBase64 = selectedItem?.thumbnailBase64,
                    imageUrl = null,
                    imageResKey = null,
                    contentDescription = selectedItem?.name ?: slotLabel,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(slotLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!activeColor.isNullOrBlank()) {
                            ColorSwatchDot(colorName = activeColor, size = 12.dp)
                        }
                        Text(
                            text = when {
                                selectedItem != null && overrideColor != null -> "${selectedItem.name} ($overrideColor override)"
                                selectedItem != null -> "${selectedItem.name} (${selectedItem.color})"
                                overrideColor != null -> "Previewing $overrideColor"
                                else -> "Tap Choose or Colour"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onPickColor) {
                    Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Colour")
                }
                OutlinedButton(onClick = onSwap) {
                    Text(if (selectedItem == null) "Choose" else "Swap")
                }
                if (selectedItem != null || overrideColor != null) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
            }
        }
    }
}
