package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppGuidePreferences
import com.example.core.ImageStorageHelper
import com.example.data.ai.StructuredOutfitSuggestion
import com.example.data.ai.StylistIntent
import com.example.data.model.LithfitConstants
import com.example.data.model.Outfit
import com.example.data.model.PreGenerationOptions
import com.example.data.model.UiState
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.ContextualTipBanner
import com.example.ui.components.LithfitLogoMark
import com.example.ui.components.LithfitShimmerBox
import com.example.ui.theme.BrandPink
import com.example.ui.viewmodel.ImageGenUiState
import com.example.ui.viewmodel.LithfitViewModel
import kotlinx.coroutines.launch

private data class PendingPreGenTarget(
    val promptText: String,
    val structuredTitle: String?,
    val structuredItems: List<String>,
    val structuredColors: List<String>,
    val fromStructuredCard: Boolean
)

@Composable
fun AiStylistScreen(
    viewModel: LithfitViewModel,
    initialPrefillPrompt: String?,
    onClearPrefill: () -> Unit,
    onOpenStyleRadarWithOutfit: (Outfit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val messages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val streamingText by viewModel.aiStreamingText.collectAsStateWithLifecycle()
    val aiBusy by viewModel.aiBusy.collectAsStateWithLifecycle()
    val aiIntent by viewModel.aiIntent.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val structuredOutfit by viewModel.latestStructuredOutfit.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val imageGenState by viewModel.imageGenState.collectAsStateWithLifecycle()
    val personalPhotoRevision by viewModel.personalPhotoRevision.collectAsStateWithLifecycle()
    val dismissedTips by viewModel.dismissedTips.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val weather = (weatherState as? UiState.Success)?.data
    var selectedOccasion by remember { mutableStateOf(LithfitConstants.OCCASIONS.first()) }
    var selectedVibe by remember { mutableStateOf(LithfitConstants.VIBES.first()) }
    var inputPrompt by remember { mutableStateOf("") }
    var fullscreenImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingPreGen by remember { mutableStateOf<PendingPreGenTarget?>(null) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var latestFeedbackChoice by remember(structuredOutfit?.outfitTitle) { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(initialPrefillPrompt) {
        if (!initialPrefillPrompt.isNullOrBlank()) {
            inputPrompt = initialPrefillPrompt
            onClearPrefill()
        }
    }

    LaunchedEffect(messages.size, streamingText, imageGenState, structuredOutfit) {
        val total = listState.layoutInfo.totalItemsCount
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        } else if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showReplaceDialog && structuredOutfit != null) {
        var slotToReplace by remember { mutableStateOf("Footwear") }
        var preference by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("Swap One Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Keep \"${structuredOutfit!!.outfitTitle}\" intact and replace one piece:")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Top", "Bottom", "Jacket", "Footwear", "Accessories")) { slot ->
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
                        label = { Text("Preference (e.g. chunky loafers, olive cap)") },
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

    pendingPreGen?.let { target ->
        PreGenerationSetupDialog(
            initialGender = profile?.gender?.ifBlank { null } ?: LithfitConstants.GENDER_OPTIONS.first(),
            initialBodySize = profile?.bodySize.orEmpty(),
            hasSavedPersonalPhoto = personalPhotoRevision > 0,
            onDismiss = { pendingPreGen = null },
            onConfirm = { options, uploadedUri ->
                pendingPreGen = null
                scope.launch {
                    val uploadedBmp = if (uploadedUri != null) {
                        ImageStorageHelper.loadBitmapFromUri(context, uploadedUri)
                    } else null
                    if (target.fromStructuredCard) {
                        viewModel.generateOutfitPreview(
                            context = context,
                            title = target.structuredTitle ?: "$selectedVibe Outfit",
                            items = target.structuredItems,
                            colors = target.structuredColors,
                            occasion = selectedOccasion,
                            vibe = selectedVibe,
                            preGenOptions = options,
                            uploadedPhotoBitmap = uploadedBmp
                        )
                    } else {
                        viewModel.generateOutfitFromPrompt(
                            context = context,
                            prompt = target.promptText,
                            occasion = selectedOccasion,
                            vibe = selectedVibe,
                            preGenOptions = options,
                            uploadedPhotoBitmap = uploadedBmp,
                            alsoSendStylistText = true
                        )
                    }
                }
            }
        )
    }

    fullscreenImageBitmap?.let { zoomBmp ->
        AlertDialog(
            onDismissRequest = { fullscreenImageBitmap = null },
            title = { Text("Generated Outfit Look") },
            text = {
                Image(
                    bitmap = zoomBmp.asImageBitmap(),
                    contentDescription = "Fullscreen generated outfit preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            },
            confirmButton = {
                TextButton(onClick = { fullscreenImageBitmap = null }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Header + Occasion/Vibe Filters
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LithfitLogoMark(size = 24.dp)
                        Text("LITHFIT AI", style = MaterialTheme.typography.titleLarge)
                    }
                    weather?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text("${it.temperatureC.toInt()}°C • ${it.conditionLabel}") }
                        )
                    }
                }

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

        // Chat Stream
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
                    title = "Fast Q&A + Outfit Builder",
                    body = "Ask quick questions for instant answers, or ask for a full outfit to get swappable pieces & AI visual previews.",
                    dismissedTips = dismissedTips,
                    onDismiss = { viewModel.dismissTip(it) }
                )
            }

            item {
                AssistantChatBubble(
                    text = "Hey! Ask any quick style question or tell me the occasion and let's build your next fit."
                )
            }

            if (messages.isEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            listOf(
                                "What shoes go with baggy cargos?",
                                "Build me a clean streetwear outfit for today.",
                                "Afro-inspired dinner outfit with warm tones.",
                                "School-appropriate Y2K fit from my closet."
                            )
                        ) { promptChip ->
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
                                    text = if (aiIntent == StylistIntent.GENERAL_QUESTION) "Thinking..." else "Styling your look...",
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
                    StructuredOutfitPreviewCard(
                        suggestion = suggestion,
                        occasion = selectedOccasion,
                        vibe = selectedVibe,
                        feedbackChoice = latestFeedbackChoice,
                        onFeedback = { status ->
                            latestFeedbackChoice = status
                            val items = listOf(
                                suggestion.topPiece,
                                suggestion.bottomPiece,
                                suggestion.jacketPiece,
                                suggestion.footwearPiece
                            ).filter { it.isNotBlank() && !it.equals("None", ignoreCase = true) } + suggestion.accessories
                            viewModel.saveOutfit(
                                Outfit(
                                    name = suggestion.outfitTitle,
                                    occasion = selectedOccasion,
                                    vibe = selectedVibe,
                                    notes = suggestion.stylingRationale,
                                    itemNames = items,
                                    colorPalette = suggestion.colorPalette,
                                    feedbackStatus = status
                                )
                            )
                        },
                        onReplaceOneItem = { showReplaceDialog = true },
                        onRegenerate = { viewModel.retryLastStylistPrompt(forceRegenerate = true) },
                        onSaveAsOutfit = {
                            val items = listOf(
                                suggestion.topPiece,
                                suggestion.bottomPiece,
                                suggestion.jacketPiece,
                                suggestion.footwearPiece
                            ).filter { it.isNotBlank() && !it.equals("None", ignoreCase = true) } + suggestion.accessories
                            viewModel.saveOutfit(
                                Outfit(
                                    name = suggestion.outfitTitle,
                                    occasion = selectedOccasion,
                                    vibe = selectedVibe,
                                    notes = suggestion.stylingRationale,
                                    itemNames = items,
                                    colorPalette = suggestion.colorPalette,
                                    feedbackStatus = latestFeedbackChoice
                                )
                            )
                        },
                        onGeneratePreview = {
                            val pieces = listOf(
                                suggestion.topPiece,
                                suggestion.bottomPiece,
                                suggestion.jacketPiece,
                                suggestion.footwearPiece
                            ).filter { it.isNotBlank() && !it.equals("None", ignoreCase = true) } + suggestion.accessories
                            pendingPreGen = PendingPreGenTarget(
                                promptText = suggestion.outfitTitle,
                                structuredTitle = suggestion.outfitTitle,
                                structuredItems = pieces,
                                structuredColors = suggestion.colorPalette,
                                fromStructuredCard = true
                            )
                        },
                        onOpenStyleRadar = {
                            val items = listOf(
                                suggestion.topPiece,
                                suggestion.bottomPiece,
                                suggestion.jacketPiece,
                                suggestion.footwearPiece
                            ).filter { it.isNotBlank() && !it.equals("None", ignoreCase = true) }
                            onOpenStyleRadarWithOutfit(
                                Outfit(
                                    name = suggestion.outfitTitle,
                                    occasion = selectedOccasion,
                                    vibe = selectedVibe,
                                    itemNames = items,
                                    colorPalette = suggestion.colorPalette
                                )
                            )
                        }
                    )
                }
            }

            if (imageGenState !is ImageGenUiState.Idle) {
                item(key = "inline_image_generation") {
                    InlineChatImageGenerationCard(
                        state = imageGenState,
                        onRetry = { viewModel.retryLastOutfitImageGeneration(context) },
                        onDismiss = { viewModel.resetImageGenState() },
                        onSaveGallery = { st ->
                            viewModel.savePreviewToDeviceGallery(
                                context = context,
                                bitmap = st.bitmap,
                                title = st.title
                            )
                        },
                        onShare = { st ->
                            viewModel.sharePreviewImage(
                                context = context,
                                bitmap = st.bitmap,
                                title = st.title
                            )
                        },
                        onSaveLook = { st ->
                            viewModel.saveCurrentGeneratedPreview(
                                bitmap = st.bitmap,
                                promptSummary = st.promptSummary,
                                occasion = st.occasion,
                                vibe = st.vibe,
                                outfitId = st.outfitId
                            )
                        },
                        onExpandImage = { bmp -> fullscreenImageBitmap = bmp }
                    )
                }
            }
        }

        // Composer with Send + direct Generate Outfit Image action
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = { Text("Describe an outfit or ask a style question...") },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            val promptToUse = inputPrompt.trim().ifBlank {
                                "$selectedVibe outfit for $selectedOccasion"
                            }
                            inputPrompt = ""
                            pendingPreGen = PendingPreGenTarget(
                                promptText = promptToUse,
                                structuredTitle = null,
                                structuredItems = emptyList(),
                                structuredColors = emptyList(),
                                fromStructuredCard = false
                            )
                        },
                        enabled = !aiBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_quick_generate_button")
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (inputPrompt.isBlank()) "Generate $selectedVibe Outfit Look"
                            else "Generate Outfit & Image"
                        )
                    }
                }
            }
        }
    }
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
private fun StructuredOutfitPreviewCard(
    suggestion: StructuredOutfitSuggestion,
    occasion: String,
    vibe: String,
    feedbackChoice: String?,
    onFeedback: (String) -> Unit,
    onReplaceOneItem: () -> Unit,
    onRegenerate: () -> Unit,
    onSaveAsOutfit: () -> Unit,
    onGeneratePreview: () -> Unit,
    onOpenStyleRadar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onReplaceOneItem, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Swap 1")
                }
                OutlinedButton(
                    onClick = onRegenerate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_regenerate_button")
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remix")
                }
                OutlinedButton(onClick = onSaveAsOutfit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onOpenStyleRadar, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Style Radar")
                }
                Button(
                    onClick = onGeneratePreview,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generate_outfit_image_button")
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Generate")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreGenerationSetupDialog(
    initialGender: String,
    initialBodySize: String,
    hasSavedPersonalPhoto: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PreGenerationOptions, Uri?) -> Unit
) {
    var selectedGender by remember { mutableStateOf(initialGender.ifBlank { "Unisex" }) }
    val initialSize = when {
        initialBodySize.startsWith("XS", true) || initialBodySize.contains("Petite", true) -> "XS"
        initialBodySize.startsWith("S", true) -> "S"
        initialBodySize.startsWith("L", true) -> "L"
        initialBodySize.startsWith("XXL", true) -> "XXL"
        initialBodySize.startsWith("XL", true) -> "XL"
        else -> "M"
    }
    var selectedSize by remember { mutableStateOf(initialSize) }
    var selectedSilhouette by remember { mutableStateOf(LithfitConstants.FIT_SILHOUETTES.first()) }
    var footwearVibe by remember { mutableStateOf("Clean Street Sneakers") }
    var accessoriesVibe by remember { mutableStateOf("Silver chain, clean watch or cap") }
    var useSavedPhoto by remember { mutableStateOf(hasSavedPersonalPhoto) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri
            useSavedPhoto = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tailor Before Generating") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Set gender, outfit size, footwear vibe, or add a photo of yourself so LITHFIT AI renders your exact outfit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Gender Styling", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Women's", "Men's", "Unisex").forEach { g ->
                        FilterChip(
                            selected = selectedGender == g,
                            onClick = { selectedGender = g },
                            label = { Text(g) }
                        )
                    }
                }

                Text("Outfit Size", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.OUTFIT_FIT_SIZES.forEach { sz ->
                        FilterChip(
                            selected = selectedSize == sz,
                            onClick = { selectedSize = sz },
                            label = { Text(sz) }
                        )
                    }
                }

                Text("Fit & Cut", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LithfitConstants.FIT_SILHOUETTES.forEach { fit ->
                        FilterChip(
                            selected = selectedSilhouette == fit,
                            onClick = { selectedSilhouette = fit },
                            label = { Text(fit) }
                        )
                    }
                }

                OutlinedTextField(
                    value = footwearVibe,
                    onValueChange = { footwearVibe = it },
                    label = { Text("Shoes Vibe") },
                    placeholder = { Text("e.g. Chunky skate shoes, loafers, boots") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accessoriesVibe,
                    onValueChange = { accessoriesVibe = it },
                    label = { Text("Accessories & Extras") },
                    placeholder = { Text("e.g. Silver chain, beanie, sunglasses") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.PersonOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Put Clothes on Your Photo (Optional)", style = MaterialTheme.typography.titleSmall)
                        }
                        if (hasSavedPersonalPhoto) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Use saved Studio Photo", style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = useSavedPhoto && pickedPhotoUri == null,
                                    onCheckedChange = { checked ->
                                        useSavedPhoto = checked
                                        if (checked) pickedPhotoUri = null
                                    }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pre_gen_upload_photo_button")
                            ) {
                                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (pickedPhotoUri != null) "Photo Attached" else "Upload Photo")
                            }
                            if (pickedPhotoUri != null) {
                                TextButton(onClick = { pickedPhotoUri = null }) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        PreGenerationOptions(
                            gender = selectedGender,
                            outfitSize = selectedSize,
                            fitSilhouette = selectedSilhouette,
                            footwearVibe = footwearVibe.ifBlank { "Matched Sneakers" },
                            accessoriesVibe = accessoriesVibe.ifBlank { "Minimalist details" },
                            useSavedPersonalPhoto = useSavedPhoto && pickedPhotoUri == null
                        ),
                        pickedPhotoUri
                    )
                },
                modifier = Modifier.testTag("pre_gen_confirm_button")
            ) {
                Text("Generate Outfit")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InlineChatImageGenerationCard(
    state: ImageGenUiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onSaveGallery: (ImageGenUiState.Success) -> Unit,
    onShare: (ImageGenUiState.Success) -> Unit,
    onSaveLook: (ImageGenUiState.Success) -> Unit,
    onExpandImage: (Bitmap) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Card(
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("inline_image_generation_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (state) {
                    ImageGenUiState.Idle -> Unit

                    is ImageGenUiState.Loading -> {
                        val clampedPct = state.progressPct.coerceIn(1, 100)
                        val animatedFraction by animateFloatAsState(
                            targetValue = clampedPct / 100f,
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                            label = "inline_image_gen_progress"
                        )

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
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "LITHFIT Visual Generation",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                color = BrandPink.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$clampedPct%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .testTag("image_gen_progress_percent")
                                )
                            }
                        }

                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        // Animated 1-100% horizontal filling progress track inside the chat
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("image_gen_progress_bar")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedFraction.coerceIn(0.01f, 1f))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                BrandPink
                                            )
                                        )
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.stageMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onDismiss,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Cancel")
                            }
                        }

                        if (state.fitDetails.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.fitDetails,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    is ImageGenUiState.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Generation Interrupted",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Close, contentDescription = "Dismiss")
                            }
                        }
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.testTag("retry_outfit_image_button")
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Retry")
                            }
                            TextButton(onClick = onDismiss) {
                                Text("Dismiss")
                            }
                        }
                    }

                    is ImageGenUiState.Success -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "100% Ready",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                                .testTag("image_gen_progress_percent")
                                        )
                                    }
                                    if (state.usedPersonalPhoto) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Virtual Try-On",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = state.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Close, contentDescription = "Hide look")
                            }
                        }

                        Image(
                            bitmap = state.bitmap.asImageBitmap(),
                            contentDescription = "Generated LITHFIT outfit look (tap to zoom)",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onExpandImage(state.bitmap) }
                                .testTag("generated_outfit_image")
                        )

                        if (state.fitDetails.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.fitDetails,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onSaveGallery(state) },
                                modifier = Modifier.testTag("save_preview_gallery_button")
                            ) {
                                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save")
                            }
                            OutlinedButton(
                                onClick = { onShare(state) },
                                modifier = Modifier.testTag("share_preview_image_button")
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }
                            OutlinedButton(onClick = onRetry) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Regenerate")
                            }
                            Button(
                                onClick = { onSaveLook(state) },
                                modifier = Modifier.testTag("save_preview_profile_button")
                            ) {
                                Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save Look")
                            }
                        }
                    }
                }
            }
        }
    }
}

