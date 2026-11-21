package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Outfit
import com.example.data.model.UiState
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.LithfitPillButton
import com.example.ui.components.LithfitPrimaryButton
import com.example.ui.viewmodel.LithfitViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutfitsScreen(
    viewModel: LithfitViewModel,
    requestedSection: Int? = null,
    onClearRequestedSection: () -> Unit = {},
    onOpenStyleRadarForOutfit: (Outfit) -> Unit,
    onAskStylist: (String) -> Unit
) {
    val wardrobeState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val outfitsState by viewModel.outfits.collectAsStateWithLifecycle()
    val collections by viewModel.outfitCollections.collectAsStateWithLifecycle()
    val calendarEntries by viewModel.calendarEntries.collectAsStateWithLifecycle()
    val wearHistory by viewModel.wearHistory.collectAsStateWithLifecycle()
    val wardrobeInsights by viewModel.wardrobeInsights.collectAsStateWithLifecycle()

    val wardrobeItems = (wardrobeState as? UiState.Success)?.data.orEmpty()
    val outfits = (outfitsState as? UiState.Success)?.data.orEmpty()

    var activeSection by rememberSaveable { mutableIntStateOf(0) } // 0=Lookbook, 1=Builder, 2=Calendar, 3=Insights
    var editingOutfit by remember { mutableStateOf<Outfit?>(null) }
    var renamingOutfit by remember { mutableStateOf<Outfit?>(null) }
    var schedulingOutfit by remember { mutableStateOf<Outfit?>(null) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(requestedSection) {
        if (requestedSection != null) {
            activeSection = requestedSection.coerceIn(0, 3)
            onClearRequestedSection()
        }
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
            dismissButton = { TextButton(onClick = { showCreateCollectionDialog = false }) { Text("Cancel") } },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createCollection(colName, colDesc.ifBlank { null })
                        showCreateCollectionDialog = false
                    },
                    enabled = colName.isNotBlank()
                ) { Text("Create") }
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

    schedulingOutfit?.let { target ->
        ScheduleOutfitDateDialog(
            outfit = target,
            onDismiss = { schedulingOutfit = null },
            onConfirmDate = { dateKey, note ->
                if (target.id.isBlank()) {
                    viewModel.saveOutfit(target) { ok ->
                        if (ok) viewModel.assignOutfitToDate(dateKey, target, note)
                    }
                } else {
                    viewModel.assignOutfitToDate(dateKey, target, note)
                }
                schedulingOutfit = null
                activeSection = 2
            }
        )
    }

    val todayKey = remember { buildUpcomingDateKeys(1).first().first }

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
                Text("Outfits", style = MaterialTheme.typography.displayMedium)
                LithfitPillButton(
                    text = "Build New",
                    icon = Icons.Outlined.Add,
                    selected = activeSection == 1,
                    onClick = {
                        editingOutfit = null
                        activeSection = 1
                    },
                    testTag = "create_outfit_button"
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Lookbook", "Builder", "Calendar", "Insights").forEachIndexed { idx, label ->
                    item {
                        LithfitPillButton(
                            text = label,
                            selected = activeSection == idx,
                            onClick = { activeSection = idx },
                            testTag = "outfits_tab_${label.lowercase()}"
                        )
                    }
                }
            }
        }

        when (activeSection) {
            0 -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Collections (${collections.size})", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showCreateCollectionDialog = true }) { Text("+ New Collection") }
                    }
                    if (collections.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(collections, key = { it.id }) { col ->
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(col.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                                }
                            }
                        }
                    }
                }

                if (outfits.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("No saved outfits yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Create looks manually in the Personal Outfit Builder or save advice from AI Stylist.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LithfitPrimaryButton(
                                        text = "Open Builder",
                                        onClick = { activeSection = 1 }
                                    )
                                    OutlinedButton(onClick = { onAskStylist("Suggest an outfit using pieces from my wardrobe.") }) {
                                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Ask AI")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(outfits, key = { it.id }) { outfit ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
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
                                            activeSection = 1
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

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(onClick = { viewModel.logOutfitWorn(outfit, todayKey) }) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Wear Now")
                                    }
                                    OutlinedButton(onClick = { schedulingOutfit = outfit }) {
                                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Schedule")
                                    }
                                    OutlinedButton(onClick = { onOpenStyleRadarForOutfit(outfit) }) {
                                        Icon(Icons.Outlined.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Style Radar")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> InteractiveOutfitBuilderPane(
                wardrobeItems = wardrobeItems,
                collections = collections.map { it.id to it.name },
                initialOutfit = editingOutfit,
                onSaveOutfit = { outfit ->
                    viewModel.saveOutfit(outfit) { ok ->
                        if (ok) {
                            editingOutfit = null
                            activeSection = 0
                        }
                    }
                },
                onScheduleOutfit = { draft -> schedulingOutfit = draft },
                onOpenStyleRadar = onOpenStyleRadarForOutfit
            )

            2 -> OutfitCalendarPane(
                entries = calendarEntries,
                savedOutfits = outfits,
                onAssignOutfit = { dateKey, outfit -> viewModel.assignOutfitToDate(dateKey, outfit) },
                onMarkWorn = { entryId -> viewModel.markCalendarEntryWorn(entryId) },
                onRemoveEntry = { entryId -> viewModel.removeCalendarEntry(entryId) },
                onOpenBuilder = { activeSection = 1 }
            )

            else -> OutfitHistoryAndInsightsPane(
                wearHistory = wearHistory,
                insights = wardrobeInsights,
                savedOutfits = outfits,
                onReuseOutfit = { outfit ->
                    editingOutfit = outfit
                    activeSection = 1
                }
            )
        }
    }
}
