package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppGuidePreferences
import com.example.data.ai.StructuredOutfitSuggestion
import com.example.data.ai.StylistIntent
import com.example.data.model.AiConversation
import com.example.data.model.LithfitConstants
import com.example.data.model.Outfit
import com.example.data.model.UiState
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.ContextualTipBanner
import com.example.ui.components.LithfitHangerEmblem
import com.example.ui.components.LithfitPillButton
import com.example.ui.components.LithfitPrimaryButton
import com.example.ui.components.LithfitShimmerBox
import com.example.ui.viewmodel.LithfitViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiStylistScreen(
    viewModel: LithfitViewModel,
    initialPrefillPrompt: String?,
    onClearPrefill: () -> Unit,
    onBack: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenOutfitsCalendar: () -> Unit,
    onOpenStyleRadarWithOutfit: (Outfit) -> Unit,
    onOpenSettings: () -> Unit = {},
    showBackButton: Boolean = false
) {
    val context = LocalContext.current
    val messages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val conversations by viewModel.aiConversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val streamingText by viewModel.aiStreamingText.collectAsStateWithLifecycle()
    val aiBusy by viewModel.aiBusy.collectAsStateWithLifecycle()
    val aiIntent by viewModel.aiIntent.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val structuredOutfit by viewModel.latestStructuredOutfit.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val dismissedTips by viewModel.dismissedTips.collectAsStateWithLifecycle()

    val weather = (weatherState as? UiState.Success)?.data
    var selectedOccasion by remember { mutableStateOf(LithfitConstants.OCCASIONS.first()) }
    var selectedVibe by remember { mutableStateOf(LithfitConstants.VIBES.first()) }
    var showContextChips by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var inputPrompt by remember { mutableStateOf("") }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var scheduleTargetOutfit by remember { mutableStateOf<Outfit?>(null) }
    var latestFeedbackChoice by remember(structuredOutfit?.outfitTitle) { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    val stylingPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val promptText = inputPrompt.trim().also { inputPrompt = "" }
            viewModel.analyzeAttachedStylingPhoto(
                context = context,
                uri = uri,
                prompt = promptText,
                occasion = selectedOccasion,
                vibe = selectedVibe
            )
        }
    }

    LaunchedEffect(initialPrefillPrompt) {
        if (!initialPrefillPrompt.isNullOrBlank()) {
            inputPrompt = initialPrefillPrompt
            onClearPrefill()
        }
    }

    LaunchedEffect(messages.size, streamingText, structuredOutfit) {
        val total = listState.layoutInfo.totalItemsCount
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AiConversationHistorySheetContent(
                conversations = conversations,
                activeConversationId = activeConversationId,
                onNewChat = {
                    viewModel.startNewAiConversation(selectedOccasion, selectedVibe)
                    showHistorySheet = false
                },
                onSelectConversation = { id ->
                    viewModel.selectAiConversation(id)
                    showHistorySheet = false
                },
                onDeleteConversation = { id ->
                    viewModel.deleteAiConversation(id)
                }
            )
        }
    }

    scheduleTargetOutfit?.let { outfit ->
        ScheduleOutfitDateDialog(
            outfit = outfit,
            onDismiss = { scheduleTargetOutfit = null },
            onConfirmDate = { dateKey, notes ->
                viewModel.assignOutfitToDate(dateKey, outfit, notes)
                scheduleTargetOutfit = null
                onOpenOutfitsCalendar()
            }
        )
    }

    if (showReplaceDialog && structuredOutfit != null) {
        var slotToReplace by remember { mutableStateOf("Footwear") }
        var preference by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("Swap One Piece") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Keep \"${structuredOutfit!!.outfitTitle}\" balanced and swap one item:")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Top", "Bottom", "Outerwear", "Footwear", "Accessories")) { slot ->
                            FilterChip(
                                selected = slotToReplace == slot,
                                onClick = { slotToReplace = slot },
                                label = { Text(slot) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = preference,
                        onValueChange = { preference = it },
                        label = { Text("Preference (e.g. chunky loafers, olive jacket)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = { TextButton(onClick = { showReplaceDialog = false }) { Text("Cancel") } },
            confirmButton = {
                Button(onClick = {
                    val extra = if (preference.isNotBlank()) " with $preference" else ""
                    viewModel.sendStylistPrompt(
                        prompt = "Keep the rest of the \"${structuredOutfit!!.outfitTitle}\" outfit the same, and replace only the $slotToReplace$extra.",
                        occasion = selectedOccasion,
                        vibe = selectedVibe,
                        forceRegenerate = true
                    )
                    showReplaceDialog = false
                }) {
                    Text("Swap Piece")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (showBackButton) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("overlay_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                        LithfitHangerEmblem(size = 26.dp, tint = MaterialTheme.colorScheme.onSurface)
                        Column {
                            Text("LITHFIT AI", style = MaterialTheme.typography.titleLarge)
                            weather?.let {
                                Text(
                                    "${it.temperatureC.toInt()}°C • ${it.conditionLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.startNewAiConversation(selectedOccasion, selectedVibe) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ai_new_chat_button")
                        ) {
                            Icon(Icons.Outlined.EditNote, contentDescription = "New Chat")
                        }
                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ai_history_button")
                        ) {
                            Icon(Icons.Outlined.History, contentDescription = "Conversation History")
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ai_settings_button")
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }
                }

                // Compact toggleable styling context strip
                Surface(
                    onClick = { showContextChips = !showContextChips },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Style context: $selectedOccasion • $selectedVibe",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (showContextChips) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = "Toggle occasion and vibe",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showContextChips) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(LithfitConstants.OCCASIONS) { occ ->
                                FilterChip(
                                    selected = selectedOccasion == occ,
                                    onClick = { selectedOccasion = occ },
                                    label = { Text(occ) }
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(LithfitConstants.VIBES) { vb ->
                                FilterChip(
                                    selected = selectedVibe == vb,
                                    onClick = { selectedVibe = vb },
                                    label = { Text(vb) }
                                )
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ContextualTipBanner(
                    tipKey = AppGuidePreferences.TIP_STYLIST,
                    title = "Intelligent Styling & Everyday Q&A",
                    body = "Ask LITHFIT anything—everyday questions, tech, style advice, or request an outfit built around your closet.",
                    dismissedTips = dismissedTips,
                    onDismiss = { viewModel.dismissTip(it) }
                )
            }

            if (messages.isEmpty() && streamingText == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LithfitHangerEmblem(
                            size = 46.dp,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Your style, reimagined.",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Ask me anything. Let's find your style.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "Hey!",
                                "What is artificial intelligence?",
                                "Create an outfit using my wardrobe.",
                                "What shoes go with baggy cargos?"
                            ).forEach { promptChip ->
                                AssistChip(
                                    onClick = {
                                        viewModel.sendStylistPrompt(promptChip, selectedOccasion, selectedVibe)
                                    },
                                    label = { Text(promptChip) }
                                )
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                if (msg.role == "USER") {
                    UserChatBubble(text = msg.text)
                } else {
                    AssistantChatBubble(text = msg.text)
                }
            }

            if (streamingText != null) {
                item {
                    val live = streamingText.orEmpty()
                    if (live.isBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (aiIntent == StylistIntent.GENERAL_QUESTION) "Thinking..." else "Looking through your wardrobe...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LithfitShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp))
                                LithfitShimmerBox(modifier = Modifier.fillMaxWidth(0.72f).height(16.dp))
                            }
                        }
                    } else {
                        AssistantChatBubble(text = live)
                    }
                }
            }

            aiError?.let { err ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.retryLastStylistPrompt(forceRegenerate = true) },
                                    modifier = Modifier.testTag("ai_retry_button")
                                ) {
                                    Text("Retry")
                                }
                                TextButton(onClick = { viewModel.clearAiError() }) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }

            if (aiIntent == StylistIntent.OUTFIT_GENERATION && structuredOutfit != null) {
                val suggestion = structuredOutfit!!
                item {
                    val suggestedOutfit = suggestion.toOutfitModel(
                        occasion = selectedOccasion,
                        vibe = selectedVibe,
                        feedbackStatus = latestFeedbackChoice
                    )
                    StructuredOutfitAdviceCard(
                        suggestion = suggestion,
                        occasion = selectedOccasion,
                        vibe = selectedVibe,
                        feedbackChoice = latestFeedbackChoice,
                        onFeedback = { status ->
                            latestFeedbackChoice = status
                            viewModel.saveOutfit(suggestedOutfit.copy(feedbackStatus = status))
                        },
                        onReplaceOneItem = { showReplaceDialog = true },
                        onRegenerate = { viewModel.retryLastStylistPrompt(forceRegenerate = true) },
                        onSaveAsOutfit = { viewModel.saveOutfit(suggestedOutfit) },
                        onAddToCalendar = { scheduleTargetOutfit = suggestedOutfit },
                        onOpenWardrobe = onOpenWardrobe,
                        onOpenStyleRadar = { onOpenStyleRadarWithOutfit(suggestedOutfit) }
                    )
                }
            }
        }

        // Composer with Photo Attachment + Send Prompt
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        stylingPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !aiBusy,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_attach_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Attach clothing photo for styling advice"
                    )
                }
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = { Text("Ask LITHFIT anything...") },
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input")
                )
                Button(
                    onClick = {
                        val trimmed = inputPrompt.trim()
                        if (trimmed.isNotEmpty()) {
                            inputPrompt = ""
                            viewModel.sendStylistPrompt(trimmed, selectedOccasion, selectedVibe)
                        }
                    },
                    enabled = !aiBusy && inputPrompt.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("ai_send_button")
                ) {
                    if (aiBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiConversationHistorySheetContent(
    conversations: List<AiConversation>,
    activeConversationId: String,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Conversations", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Switch, continue, or clean up past chats",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LithfitPrimaryButton(
                text = "New Chat",
                icon = Icons.Outlined.Add,
                onClick = onNewChat,
                testTag = "history_new_chat_button"
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (conversations.isEmpty()) {
            Text(
                text = "No saved conversations yet. Start chatting below!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 18.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(conversations, key = { it.id }) { conv ->
                    val isActive = conv.id == activeConversationId
                    Surface(
                        color = if (isActive) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectConversation(conv.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = conv.title.ifBlank { "Untitled Chat" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = conv.preview.ifBlank { "${conv.occasion} • ${conv.vibe}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDeleteConversation(conv.id) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_conv_${conv.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete conversation"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun StructuredOutfitSuggestion.toOutfitModel(
    occasion: String,
    vibe: String,
    feedbackStatus: String?
): Outfit {
    val items = listOf(topPiece, bottomPiece, jacketPiece, footwearPiece)
        .filter { it.isNotBlank() && !it.equals("None", ignoreCase = true) } + accessories
    return Outfit(
        name = outfitTitle,
        occasion = occasion,
        vibe = vibe,
        notes = stylingRationale,
        itemNames = items,
        colorPalette = colorPalette,
        feedbackStatus = feedbackStatus
    )
}

@Composable
private fun UserChatBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
            modifier = Modifier.fillMaxWidth(0.84f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun AssistantChatBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StructuredOutfitAdviceCard(
    suggestion: StructuredOutfitSuggestion,
    occasion: String,
    vibe: String,
    feedbackChoice: String?,
    onFeedback: (String) -> Unit,
    onReplaceOneItem: () -> Unit,
    onRegenerate: () -> Unit,
    onSaveAsOutfit: () -> Unit,
    onAddToCalendar: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenStyleRadar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.testTag("structured_outfit_advice_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(suggestion.outfitTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text("$occasion • $vibe", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (suggestion.colorPalette.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestion.colorPalette.forEach { col ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ColorSwatchDot(colorName = col, size = 14.dp)
                            Text(col, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Text("Top: ${suggestion.topPiece}", style = MaterialTheme.typography.bodySmall)
            Text("Bottom: ${suggestion.bottomPiece}", style = MaterialTheme.typography.bodySmall)
            if (suggestion.jacketPiece.isNotBlank() && !suggestion.jacketPiece.equals("None", ignoreCase = true)) {
                Text("Outerwear: ${suggestion.jacketPiece}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Footwear: ${suggestion.footwearPiece}", style = MaterialTheme.typography.bodySmall)
            if (suggestion.accessories.isNotEmpty()) {
                Text("Accessories: ${suggestion.accessories.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
            if (suggestion.ownedItemsUsed.isNotEmpty()) {
                Text(
                    "From Your Wardrobe: ${suggestion.ownedItemsUsed.joinToString(", ")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = feedbackChoice == "LIKED",
                    onClick = { onFeedback("LIKED") },
                    leadingIcon = { Icon(Icons.Outlined.ThumbUp, contentDescription = "Like", modifier = Modifier.size(16.dp)) },
                    label = { Text("Love this fit") }
                )
                FilterChip(
                    selected = feedbackChoice == "DISLIKED",
                    onClick = { onFeedback("DISLIKED") },
                    leadingIcon = { Icon(Icons.Outlined.ThumbDown, contentDescription = "Dislike", modifier = Modifier.size(16.dp)) },
                    label = { Text("Not my vibe") }
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LithfitPillButton(
                    text = "Swap 1",
                    icon = Icons.Outlined.SwapHoriz,
                    onClick = onReplaceOneItem
                )
                LithfitPillButton(
                    text = "Remix",
                    icon = Icons.Outlined.Refresh,
                    onClick = onRegenerate,
                    testTag = "ai_regenerate_button"
                )
                LithfitPillButton(
                    text = "Save Outfit",
                    icon = Icons.Outlined.BookmarkAdd,
                    onClick = onSaveAsOutfit
                )
                LithfitPillButton(
                    text = "Calendar",
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = onAddToCalendar,
                    testTag = "ai_schedule_calendar_button"
                )
                LithfitPillButton(
                    text = "Wardrobe",
                    icon = Icons.Outlined.Checkroom,
                    onClick = onOpenWardrobe
                )
                LithfitPillButton(
                    text = "Style Radar",
                    icon = Icons.Outlined.Storefront,
                    onClick = onOpenStyleRadar
                )
            }
        }
    }
}
