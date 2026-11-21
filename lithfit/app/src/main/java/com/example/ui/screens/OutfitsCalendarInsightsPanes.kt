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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Replay
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CalendarOutfitEntry
import com.example.data.model.Outfit
import com.example.data.model.OutfitWearHistory
import com.example.data.model.WardrobeInsightsSummary
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.LithfitPillButton
import com.example.ui.components.LithfitPrimaryButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun buildUpcomingDateKeys(daysCount: Int = 14): List<Pair<String, String>> {
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt = SimpleDateFormat("EEE, MMM d", Locale.US)
    val cal = Calendar.getInstance()
    return (0 until daysCount).map { offset ->
        val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
        val key = keyFmt.format(c.time)
        val label = when (offset) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> labelFmt.format(c.time)
        }
        key to label
    }
}

@Composable
fun ScheduleOutfitDateDialog(
    outfit: Outfit,
    onDismiss: () -> Unit,
    onConfirmDate: (dateKey: String, notes: String?) -> Unit
) {
    val dates = remember { buildUpcomingDateKeys(14) }
    var selectedDateKey by remember { mutableStateOf(dates.first().first) }
    var customDateInput by remember { mutableStateOf(dates.first().first) }
    var notes by remember { mutableStateOf(outfit.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule \"${outfit.name}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Pick an upcoming day or enter any YYYY-MM-DD date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(dates) { (key, label) ->
                        FilterChip(
                            selected = selectedDateKey == key,
                            onClick = {
                                selectedDateKey = key
                                customDateInput = key
                            },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = customDateInput,
                    onValueChange = {
                        customDateInput = it
                        selectedDateKey = it
                    },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Day Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Button(
                onClick = {
                    val finalDate = customDateInput.trim().ifBlank { dates.first().first }
                    onConfirmDate(finalDate, notes.trim().ifBlank { null })
                }
            ) {
                Text("Schedule")
            }
        }
    )
}

@Composable
fun OutfitCalendarPane(
    entries: List<CalendarOutfitEntry>,
    savedOutfits: List<Outfit>,
    onAssignOutfit: (dateKey: String, outfit: Outfit) -> Unit,
    onMarkWorn: (entryId: String) -> Unit,
    onRemoveEntry: (entryId: String) -> Unit,
    onOpenBuilder: () -> Unit
) {
    val dates = remember { buildUpcomingDateKeys(14) }
    var selectedDateKey by rememberSaveable { mutableStateOf(dates.first().first) }
    var showPickOutfitDialog by remember { mutableStateOf(false) }

    if (showPickOutfitDialog) {
        AlertDialog(
            onDismissRequest = { showPickOutfitDialog = false },
            title = { Text("Assign Look for $selectedDateKey") },
            text = {
                if (savedOutfits.isEmpty()) {
                    Text("Save an outfit in Builder or AI Stylist first, then assign it to any day.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedOutfits, key = { it.id }) { outfit ->
                            Surface(
                                onClick = {
                                    onAssignOutfit(selectedDateKey, outfit)
                                    showPickOutfitDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(outfit.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${outfit.occasion} • ${outfit.vibe}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                if (savedOutfits.isEmpty()) {
                    TextButton(onClick = {
                        showPickOutfitDialog = false
                        onOpenBuilder()
                    }) { Text("Open Builder") }
                }
            },
            confirmButton = { TextButton(onClick = { showPickOutfitDialog = false }) { Text("Close") } }
        )
    }

    val selectedDayEntries = remember(entries, selectedDateKey) {
        entries.filter { it.dateKey == selectedDateKey }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Outfit Calendar", style = MaterialTheme.typography.titleLarge)
                LithfitPillButton(
                    text = "Assign Look",
                    icon = Icons.Outlined.CalendarMonth,
                    selected = true,
                    onClick = { showPickOutfitDialog = true },
                    testTag = "calendar_assign_button"
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dates) { (key, label) ->
                    val hasScheduled = entries.any { it.dateKey == key }
                    FilterChip(
                        selected = selectedDateKey == key,
                        onClick = { selectedDateKey = key },
                        label = {
                            Text(if (hasScheduled) "$label •" else label)
                        },
                        modifier = Modifier.testTag("calendar_day_$key")
                    )
                }
            }
        }

        if (selectedDayEntries.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Nothing scheduled for $selectedDateKey", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Plan a look ahead of time or assign an outfit from your Lookbook.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LithfitPrimaryButton(
                            text = "Assign Outfit for This Day",
                            onClick = { showPickOutfitDialog = true }
                        )
                    }
                }
            }
        } else {
            items(selectedDayEntries, key = { it.id }) { entry ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.outfitName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${entry.dateKey} • ${entry.occasion} • ${entry.vibe}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onRemoveEntry(entry.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remove schedule")
                            }
                        }
                        entry.notes?.takeIf { it.isNotBlank() }?.let { note ->
                            Text(note, style = MaterialTheme.typography.bodySmall)
                        }
                        FilterChip(
                            selected = entry.worn,
                            onClick = { onMarkWorn(entry.id) },
                            leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(if (entry.worn) "Worn ✓" else "Mark as Worn") }
                        )
                    }
                }
            }
        }

        if (entries.isNotEmpty()) {
            item { Text("All Planned Days (${entries.size})", style = MaterialTheme.typography.titleMedium) }
            items(entries, key = { "all_${it.id}" }) { entry ->
                Surface(
                    onClick = { selectedDateKey = entry.dateKey },
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
                            Text(entry.outfitName, style = MaterialTheme.typography.titleSmall)
                            Text("${entry.dateKey} • ${entry.occasion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (entry.worn) {
                            Text("Worn", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutfitHistoryAndInsightsPane(
    wearHistory: List<OutfitWearHistory>,
    insights: WardrobeInsightsSummary,
    savedOutfits: List<Outfit>,
    onReuseOutfit: (Outfit) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Analytics, contentDescription = null)
                Text("Wardrobe Insights (Real Data)", style = MaterialTheme.typography.titleLarge)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightStatCard("Garments", insights.totalItems.toString(), Modifier.weight(1f))
                InsightStatCard("Outfits", insights.savedOutfitsCount.toString(), Modifier.weight(1f))
                InsightStatCard("Logged Wears", insights.totalWearsLogged.toString(), Modifier.weight(1f))
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.testTag("wardrobe_insights_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Closet Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (insights.totalItems == 0) {
                        Text(
                            "Add items to your Wardrobe and log worn outfits to see your real closet usage, top colours, and most-worn pieces.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        if (insights.frequentColors.isNotEmpty()) {
                            Text("Frequent Colours", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                insights.frequentColors.forEach { (color, count) ->
                                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            ColorSwatchDot(colorName = color, size = 10.dp)
                                            Text("$color ($count)", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                        if (insights.topCategories.isNotEmpty()) {
                            Text(
                                "Top Categories: ${insights.topCategories.joinToString { "${it.first} (${it.second})" }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (insights.mostUsedItems.isNotEmpty()) {
                            Text(
                                "Most-Used Pieces: ${insights.mostUsedItems.joinToString { "${it.first} (${it.second}x)" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (insights.leastUsedItems.isNotEmpty()) {
                            Text(
                                "Unworn / Least-Used: ${insights.leastUsedItems.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.History, contentDescription = null)
                Text("Outfit Wear History (${wearHistory.size})", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (wearHistory.isEmpty()) {
            item {
                Text(
                    "No worn outfits logged yet. Tap 'Wear Now' on any outfit in your Lookbook or mark a scheduled Calendar look as Worn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(wearHistory, key = { it.id }) { history ->
                val matchingOutfit = savedOutfits.firstOrNull { it.id == history.outfitId }
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
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(history.outfitName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${history.wornDateKey} • ${history.occasion} • ${history.vibe}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (history.itemNames.isNotEmpty()) {
                                Text(
                                    history.itemNames.joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(onClick = {
                            onReuseOutfit(
                                matchingOutfit ?: Outfit(
                                    name = history.outfitName,
                                    occasion = history.occasion,
                                    vibe = history.vibe,
                                    itemNames = history.itemNames
                                )
                            )
                        }) {
                            Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reuse")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
