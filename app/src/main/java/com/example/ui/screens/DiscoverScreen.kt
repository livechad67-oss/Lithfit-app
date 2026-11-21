package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.AppGuidePreferences
import com.example.data.model.CommunityPost
import com.example.data.model.DiscoveryPost
import com.example.data.model.LithfitConstants
import com.example.data.model.SeedCatalogData
import com.example.data.model.StyleBoard
import com.example.data.model.UiState
import com.example.ui.components.ColorSwatchChip
import com.example.ui.components.ColorSwatchDot
import com.example.ui.components.ContextualTipBanner
import com.example.ui.components.LithfitGarmentImage
import com.example.ui.components.ReportContentDialog
import com.example.ui.viewmodel.LithfitViewModel

private enum class DiscoverMode(val label: String, val testTagKey: String) {
    EDITORIAL("Editorial", "discover_mode_editorial"),
    TRENDING("Trending Now", "discover_mode_trending"),
    BOARDS("Style Boards", "discover_mode_boards"),
    COMMUNITY("Community", "discover_mode_community"),
    RADAR("Style Radar", "discover_mode_radar")
}

private enum class DiscoverSortOption(val label: String) {
    RECOMMENDED("Recommended"),
    TITLE_AZ("A–Z"),
    CULTURAL("Cultural First")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    viewModel: LithfitViewModel,
    initialSectionFilter: String? = null,
    onClearInitialSectionFilter: () -> Unit = {},
    initialRadarMode: Boolean = false,
    onClearInitialRadarMode: () -> Unit = {},
    initialRadarQuery: String? = null,
    onClearInitialRadarQuery: () -> Unit = {},
    onAskStylistAboutLook: (String) -> Unit,
    onOpenOutfitBuilder: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val posts by viewModel.discoveryPosts.collectAsStateWithLifecycle()
    val wardrobeState by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val outfitsState by viewModel.outfits.collectAsStateWithLifecycle()
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val styleBoards by viewModel.styleBoards.collectAsStateWithLifecycle()
    val recentlyViewed by viewModel.recentlyViewed.collectAsStateWithLifecycle()
    val communityPosts by viewModel.communityPosts.collectAsStateWithLifecycle()
    val followedAuthors by viewModel.followedAuthors.collectAsStateWithLifecycle()
    val blockedAuthors by viewModel.blockedAuthors.collectAsStateWithLifecycle()
    val visualSearchQuery by viewModel.visualSearchQuery.collectAsStateWithLifecycle()
    val visualSearchBusy by viewModel.visualSearchBusy.collectAsStateWithLifecycle()
    val dismissedTips by viewModel.dismissedTips.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val wardrobeItems = (wardrobeState as? UiState.Success)?.data.orEmpty()
    val savedOutfits = (outfitsState as? UiState.Success)?.data.orEmpty()

    var activeMode by rememberSaveable { mutableStateOf(DiscoverMode.EDITORIAL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSection by rememberSaveable { mutableStateOf("For You") }
    var selectedVibe by rememberSaveable { mutableStateOf("All") }
    var selectedColorFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sortOption by rememberSaveable { mutableStateOf(DiscoverSortOption.RECOMMENDED) }
    var showFilterDrawer by rememberSaveable { mutableStateOf(false) }
    var visibleCount by rememberSaveable { mutableIntStateOf(12) }

    var selectedPostForDetail by remember { mutableStateOf<DiscoveryPost?>(null) }
    var boardPickerTargetPost by remember { mutableStateOf<DiscoveryPost?>(null) }
    var editingBoard by remember { mutableStateOf<StyleBoard?>(null) }
    var showCreateBoardDialog by remember { mutableStateOf(false) }
    var selectedBoardDetail by remember { mutableStateOf<StyleBoard?>(null) }
    var showCommunityComposer by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) } // targetType, id, title

    LaunchedEffect(initialSectionFilter) {
        if (!initialSectionFilter.isNullOrBlank()) {
            selectedSection = if (initialSectionFilter in LithfitConstants.DISCOVERY_CATEGORIES) {
                initialSectionFilter
            } else {
                "For You"
            }
            activeMode = DiscoverMode.EDITORIAL
            onClearInitialSectionFilter()
        }
    }

    LaunchedEffect(initialRadarMode) {
        if (initialRadarMode) {
            activeMode = DiscoverMode.RADAR
            onClearInitialRadarMode()
        }
    }

    LaunchedEffect(selectedSection, selectedVibe, selectedColorFilter, searchQuery, visualSearchQuery, sortOption) {
        visibleCount = 12
    }

    val visualPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.runVisualSearchFromUri(context, uri)
        }
    }

    reportTarget?.let { (targetType, id, title) ->
        ReportContentDialog(
            targetTitle = title,
            onDismiss = { reportTarget = null },
            onSubmit = { reason, details ->
                viewModel.submitReport(targetType, id, reason, details)
                reportTarget = null
            }
        )
    }

    selectedPostForDetail?.let { post ->
        DiscoveryPostDetailDialog(
            post = post,
            onDismiss = { selectedPostForDetail = null },
            onAddToBoard = {
                boardPickerTargetPost = post
            },
            onReport = {
                reportTarget = Triple("DISCOVERY_POST", post.id, post.title)
                selectedPostForDetail = null
            },
            onStyleSimilar = {
                onAskStylistAboutLook(
                    "Style a look inspired by \"${post.title}\" (${post.category}, ${post.colorPalette.joinToString()}): ${post.description}"
                )
                selectedPostForDetail = null
            }
        )
    }

    boardPickerTargetPost?.let { post ->
        AddToStyleBoardDialog(
            post = post,
            boards = styleBoards,
            onDismiss = { boardPickerTargetPost = null },
            onToggleBoard = { board ->
                viewModel.toggleItemInStyleBoard(board.id, post.id, post.title, post.imageResKey)
            },
            onCreateBoardAndAdd = { newName, aesthetic ->
                viewModel.saveStyleBoard(
                    StyleBoard(
                        name = newName,
                        aesthetic = aesthetic,
                        colors = post.colorPalette,
                        itemIds = listOf(post.id),
                        imageResKey = post.imageResKey
                    )
                )
                boardPickerTargetPost = null
            }
        )
    }

    if (showCreateBoardDialog || editingBoard != null) {
        EditStyleBoardDialog(
            initialBoard = editingBoard,
            onDismiss = {
                showCreateBoardDialog = false
                editingBoard = null
            },
            onSave = { updatedBoard ->
                viewModel.saveStyleBoard(updatedBoard)
                showCreateBoardDialog = false
                editingBoard = null
            },
            onDelete = editingBoard?.let { b ->
                {
                    viewModel.deleteStyleBoard(b.id)
                    if (selectedBoardDetail?.id == b.id) selectedBoardDetail = null
                    editingBoard = null
                }
            }
        )
    }

    selectedBoardDetail?.let { board ->
        val freshBoard = styleBoards.firstOrNull { it.id == board.id } ?: board
        StyleBoardDetailDialog(
            board = freshBoard,
            allPosts = posts,
            onDismiss = { selectedBoardDetail = null },
            onOpenPost = { post ->
                viewModel.recordDiscoveryView(post)
                selectedPostForDetail = post
            },
            onRemovePost = { post ->
                viewModel.toggleItemInStyleBoard(freshBoard.id, post.id, post.title, post.imageResKey)
            },
            onStyleBoardVibe = {
                onAskStylistAboutLook(
                    "Create an outfit inspired by my \"${freshBoard.name}\" board (${freshBoard.aesthetic}, ${freshBoard.occasion}, palette: ${freshBoard.colors.joinToString()})."
                )
                selectedBoardDetail = null
            }
        )
    }

    if (showCommunityComposer) {
        CreateCommunityPostDialog(
            onDismiss = { showCommunityComposer = false },
            onPublish = { title, caption, aesthetic, photoUri, palette ->
                viewModel.publishCommunityPostFromUri(
                    context = context,
                    title = title,
                    caption = caption,
                    aesthetic = aesthetic,
                    photoUri = photoUri,
                    colorPalette = palette
                )
                showCommunityComposer = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header + 5-Mode Editorial Switcher
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeMode.label,
                    style = MaterialTheme.typography.displayMedium
                )
                TextButton(onClick = onOpenOutfitBuilder) {
                    Text("Outfit Studio")
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DiscoverMode.entries) { mode ->
                    val selected = activeMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { activeMode = mode },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag(mode.testTagKey)
                    )
                }
            }
        }

        when (activeMode) {
            DiscoverMode.RADAR -> {
                StyleRadarScreen(
                    viewModel = viewModel,
                    initialSearchQuery = initialRadarQuery,
                    onClearInitialSearch = onClearInitialRadarQuery,
                    onOpenDiscover = { activeMode = DiscoverMode.EDITORIAL },
                    onOpenAiStylist = { onAskStylistAboutLook("") }
                )
            }

            DiscoverMode.TRENDING -> {
                val trendingPosts = remember(posts) {
                    posts.filter {
                        it.sectionTag.equals("Trending", true) ||
                            it.tags.any { tag -> tag.equals("Trending", true) }
                    }.ifEmpty { posts.take(6) }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Curated Style Spotlights",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Text(
                                "Seasonal silhouettes, proportions, and street-to-smart colour pairings curated by LITHFIT.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(SeedCatalogData.trendingHighlights, key = { it.id }) { highlight ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                ) {
                                    LithfitGarmentImage(
                                        localImagePath = null,
                                        thumbnailBase64 = null,
                                        imageUrl = null,
                                        imageResKey = highlight.imageResKey,
                                        contentDescription = highlight.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0.4f to Color.Transparent,
                                                    1f to Color.Black.copy(alpha = 0.65f)
                                                )
                                            )
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "${highlight.seasonTag} • ${highlight.vibe}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = highlight.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(14.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = highlight.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            highlight.palette.forEach { col ->
                                                ColorSwatchDot(colorName = col, size = 14.dp)
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                onAskStylistAboutLook(
                                                    "Style a ${highlight.vibe} outfit inspired by \"${highlight.title}\" using colours ${highlight.palette.joinToString()}: ${highlight.subtitle}"
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Style This Vibe")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Trending Editorial Looks",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }

                    val trendingRows = trendingPosts.chunked(2)
                    items(trendingRows, key = { pair -> "trend_${pair.joinToString("_") { it.id }}" }) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { post ->
                                val savedRecord = savedItems.firstOrNull {
                                    it.targetType == "DISCOVERY_POST" && it.targetId == post.id
                                }
                                DiscoverLookGridCard(
                                    post = post,
                                    isSaved = savedRecord != null,
                                    isLiked = savedRecord?.liked == true,
                                    onClick = {
                                        viewModel.recordDiscoveryView(post)
                                        selectedPostForDetail = post
                                    },
                                    onLike = { viewModel.toggleLikeDiscoveryPost(post) },
                                    onSave = { viewModel.toggleSaveDiscoveryPost(post) },
                                    onAddToBoard = { boardPickerTargetPost = post },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            DiscoverMode.BOARDS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Your Style Boards", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Organise saved fits by aesthetic, occasion, and colourway.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { showCreateBoardDialog = true },
                                modifier = Modifier.testTag("create_style_board_button")
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Board")
                            }
                        }
                    }

                    if (styleBoards.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
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
                                    Icon(
                                        Icons.Outlined.CollectionsBookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("No Style Boards Yet", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Create a board (like Y2K Rotation, Campus Casual, or Night Out) and pin looks directly from Editorial Discover.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(onClick = { showCreateBoardDialog = true }) {
                                        Text("Create Your First Board")
                                    }
                                }
                            }
                        }
                    } else {
                        items(styleBoards, key = { it.id }) { board ->
                            Card(
                                onClick = { selectedBoardDetail = board },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.testTag("style_board_card_${board.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LithfitGarmentImage(
                                        localImagePath = null,
                                        thumbnailBase64 = null,
                                        imageUrl = null,
                                        imageResKey = board.imageResKey ?: "img_editorial_streetwear",
                                        contentDescription = board.name,
                                        modifier = Modifier
                                            .size(86.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = board.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${board.aesthetic} • ${board.occasion} • ${board.itemIds.size} saved",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (board.description.isNotBlank()) {
                                            Text(
                                                text = board.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (board.colors.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                board.colors.take(5).forEach { col ->
                                                    ColorSwatchDot(colorName = col, size = 12.dp)
                                                }
                                            }
                                        }
                                    }
                                    Column {
                                        IconButton(onClick = { editingBoard = board }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit board")
                                        }
                                        IconButton(onClick = { viewModel.deleteStyleBoard(board.id) }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Delete board")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DiscoverMode.COMMUNITY -> {
                val visibleCommunityPosts = remember(communityPosts, blockedAuthors) {
                    communityPosts.filter { it.authorId !in blockedAuthors }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text("LITHFIT Community", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Share your real outfit fit checks with photos from your gallery.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Button(
                                    onClick = { showCommunityComposer = true },
                                    modifier = Modifier.testTag("community_share_fit_button")
                                ) {
                                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Share Fit")
                                }
                            }
                        }
                    }

                    if (visibleCommunityPosts.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Checkroom,
                                        contentDescription = null,
                                        modifier = Modifier.size(38.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("No Community Posts Yet", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Only real community outfits shared by members appear here — zero fabricated feeds. Tap Share Fit to post your first look!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(onClick = { showCommunityComposer = true }) {
                                        Text("Share Your First Look")
                                    }
                                }
                            }
                        }
                    } else {
                        items(visibleCommunityPosts, key = { it.id }) { cPost ->
                            val isOwnPost = cPost.authorId == viewModel.currentUserId
                            val isFollowed = cPost.authorId in followedAuthors
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column {
                                    if (!cPost.imagePath.isNullOrBlank() || !cPost.thumbnailBase64.isNullOrBlank()) {
                                        LithfitGarmentImage(
                                            localImagePath = cPost.imagePath,
                                            thumbnailBase64 = cPost.thumbnailBase64,
                                            imageUrl = null,
                                            imageResKey = null,
                                            contentDescription = cPost.title,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(230.dp)
                                        )
                                    }
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
                                                Text(cPost.title, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    "${cPost.authorName} ${cPost.authorHandle} • ${cPost.aesthetic}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (!isOwnPost) {
                                                FilterChip(
                                                    selected = isFollowed,
                                                    onClick = { viewModel.toggleFollowAuthor(cPost.authorId, cPost.authorName) },
                                                    leadingIcon = {
                                                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    },
                                                    label = { Text(if (isFollowed) "Following" else "Follow") }
                                                )
                                            }
                                        }
                                        if (cPost.caption.isNotBlank()) {
                                            Text(cPost.caption, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (cPost.colorPalette.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                cPost.colorPalette.forEach { col ->
                                                    ColorSwatchDot(colorName = col, size = 14.dp)
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    onAskStylistAboutLook(
                                                        "Style an outfit inspired by \"${cPost.title}\" (${cPost.aesthetic}): ${cPost.caption}"
                                                    )
                                                }
                                            ) {
                                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Style Similar")
                                            }
                                            Spacer(Modifier.weight(1f))
                                            if (isOwnPost) {
                                                IconButton(onClick = { viewModel.deleteCommunityPost(cPost.id) }) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete post")
                                                }
                                            } else {
                                                IconButton(onClick = {
                                                    reportTarget = Triple("COMMUNITY_POST", cPost.id, cPost.title)
                                                }) {
                                                    Icon(Icons.Outlined.Flag, contentDescription = "Report post")
                                                }
                                                IconButton(onClick = {
                                                    viewModel.blockAuthor(cPost.authorId, cPost.authorName)
                                                }) {
                                                    Icon(Icons.Outlined.Block, contentDescription = "Block creator")
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

            DiscoverMode.EDITORIAL -> {
                val effectiveSearch = (visualSearchQuery ?: searchQuery).trim().lowercase()
                val hasActiveFilters = effectiveSearch.isNotEmpty() ||
                    selectedSection != "For You" ||
                    selectedVibe != "All" ||
                    selectedColorFilter != null

                val filteredPosts = remember(
                    posts,
                    selectedSection,
                    selectedVibe,
                    selectedColorFilter,
                    effectiveSearch,
                    sortOption,
                    profile
                ) {
                    val preferred = profile?.preferredStyles.orEmpty().map { it.lowercase() }.toSet()
                    val preferredCols = profile?.preferredColors.orEmpty().map { it.lowercase() }.toSet()
                    val preferredAudience = when {
                        profile?.gender?.contains("Men", ignoreCase = true) == true -> "HIM"
                        profile?.gender?.contains("Women", ignoreCase = true) == true -> "HER"
                        else -> null
                    }
                    val list = posts.filter { post ->
                        val secOk = when (selectedSection) {
                            "For You" -> true
                            "Men's" -> post.targetAudience.equals("HIM", true) || post.sectionTag.equals("Men's", true)
                            "Women's" -> post.targetAudience.equals("HER", true) || post.sectionTag.equals("Women's", true)
                            "Gender-Neutral" -> post.targetAudience.equals("UNISEX", true)
                            "Teenage" -> post.ageGroup.equals("TEEN", true) || post.sectionTag.equals("Teenage", true) || post.tags.any { it.contains("Teen", true) }
                            "Adult" -> post.ageGroup.equals("ADULT", true) || post.ageGroup.equals("ALL", true)
                            "Cultural" -> post.culturalTag != null || post.sectionTag.equals("Cultural", true) || post.category.contains("Afro", true) || post.tags.any { it.contains("Cultural", true) }
                            "Trending" -> post.sectionTag.equals("Trending", true) || post.tags.any { it.contains("Trending", true) }
                            "Seasonal" -> post.sectionTag.equals("Seasonal", true) || post.tags.any { it.contains("Seasonal", true) }
                            else -> true
                        }
                        val vibeOk = selectedVibe == "All" ||
                            post.category.equals(selectedVibe, ignoreCase = true) ||
                            post.tags.any { it.contains(selectedVibe, ignoreCase = true) }
                        val colOk = selectedColorFilter == null ||
                            post.colorPalette.any { it.contains(selectedColorFilter!!, ignoreCase = true) }
                        val searchOk = effectiveSearch.isBlank() ||
                            post.title.lowercase().contains(effectiveSearch) ||
                            post.description.lowercase().contains(effectiveSearch) ||
                            post.category.lowercase().contains(effectiveSearch) ||
                            post.sectionTag.lowercase().contains(effectiveSearch) ||
                            post.authorName.lowercase().contains(effectiveSearch) ||
                            post.colorPalette.any { it.lowercase().contains(effectiveSearch) } ||
                            post.outfitBreakdown.any { it.lowercase().contains(effectiveSearch) } ||
                            post.tags.any { it.lowercase().contains(effectiveSearch) }
                        secOk && vibeOk && colOk && searchOk
                    }

                    when (sortOption) {
                        DiscoverSortOption.RECOMMENDED -> list.sortedByDescending { post ->
                            var score = 0
                            if (post.category.lowercase() in preferred || post.tags.any { it.lowercase() in preferred }) score += 4
                            if (preferredAudience != null && (post.targetAudience.equals(preferredAudience, true) || post.targetAudience.equals("UNISEX", true))) score += 2
                            if (post.colorPalette.any { it.lowercase() in preferredCols }) score += 2
                            if (post.imageResKey?.startsWith("img_") == true) score += 1
                            score
                        }
                        DiscoverSortOption.TITLE_AZ -> list.sortedBy { it.title }
                        DiscoverSortOption.CULTURAL -> list.sortedByDescending {
                            if (it.culturalTag != null || it.sectionTag.equals("Cultural", true)) 1 else 0
                        }
                    }
                }

                val tailoredSpotlight = remember(posts, profile) {
                    val prefStyles = profile?.preferredStyles.orEmpty().map { it.lowercase() }.toSet()
                    val prefColors = profile?.preferredColors.orEmpty().map { it.lowercase() }.toSet()
                    val prefAudience = when {
                        profile?.gender?.contains("Men", ignoreCase = true) == true -> "HIM"
                        profile?.gender?.contains("Women", ignoreCase = true) == true -> "HER"
                        else -> null
                    }
                    posts.sortedByDescending { post ->
                        var s = 0
                        if (post.category.lowercase() in prefStyles || post.tags.any { it.lowercase() in prefStyles }) s += 5
                        if (prefAudience != null && post.targetAudience.equals(prefAudience, true)) s += 3
                        if (post.targetAudience.equals("UNISEX", true)) s += 1
                        if (post.colorPalette.any { it.lowercase() in prefColors }) s += 2
                        if (post.imageResKey?.startsWith("img_rec_") == true) s += 1
                        s
                    }.take(10)
                }

                val matchingWardrobe = remember(wardrobeItems, effectiveSearch, selectedColorFilter) {
                    if (effectiveSearch.isBlank() && selectedColorFilter == null) emptyList()
                    else wardrobeItems.filter { item ->
                        val colOk = selectedColorFilter == null || item.color.contains(selectedColorFilter!!, ignoreCase = true)
                        val qOk = effectiveSearch.isBlank() ||
                            item.name.lowercase().contains(effectiveSearch) ||
                            item.color.lowercase().contains(effectiveSearch) ||
                            (item.subType?.lowercase()?.contains(effectiveSearch) == true) ||
                            (item.garmentType?.lowercase()?.contains(effectiveSearch) == true) ||
                            (item.style?.lowercase()?.contains(effectiveSearch) == true) ||
                            item.categoryEnum().label.lowercase().contains(effectiveSearch)
                        colOk && qOk
                    }
                }

                val matchingOutfits = remember(savedOutfits, effectiveSearch) {
                    if (effectiveSearch.isBlank()) emptyList()
                    else savedOutfits.filter {
                        it.name.lowercase().contains(effectiveSearch) ||
                            it.vibe.lowercase().contains(effectiveSearch) ||
                            it.occasion.lowercase().contains(effectiveSearch) ||
                            it.itemNames.any { piece -> piece.lowercase().contains(effectiveSearch) }
                    }
                }

                val pagedPosts = filteredPosts.take(visibleCount)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            ContextualTipBanner(
                                tipKey = AppGuidePreferences.TIP_DISCOVER,
                                title = "Explore Inclusive Edits",
                                body = "Browse real high-resolution editorial looks, pin fits to Style Boards, or tap the camera to match your photo.",
                                dismissedTips = dismissedTips,
                                onDismiss = { viewModel.dismissTip(it) }
                            )
                        }
                    }

                    // Picked for Your Vibe Personalized Carousel
                    if (tailoredSpotlight.isNotEmpty() && !hasActiveFilters) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (profile?.preferredStyles?.isNotEmpty() == true) {
                                            "Picked for Your Vibe (${profile.preferredStyles.take(2).joinToString(" & ")})"
                                        } else {
                                            "Curated Across Every Vibe (${posts.size} looks)"
                                        },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(tailoredSpotlight, key = { "picked_${it.id}" }) { post ->
                                        Card(
                                            onClick = {
                                                viewModel.recordDiscoveryView(post)
                                                selectedPostForDetail = post
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.width(152.dp)
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
                                                        .height(178.dp)
                                                )
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        text = post.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = post.title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Recently Viewed Looks Strip
                    if (recentlyViewed.isNotEmpty() && !hasActiveFilters) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Recently Viewed",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(recentlyViewed, key = { it.id }) { entry ->
                                        val resolvedPost = posts.firstOrNull { it.id == entry.postId }
                                        Card(
                                            onClick = {
                                                if (resolvedPost != null) {
                                                    selectedPostForDetail = resolvedPost
                                                }
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.width(132.dp)
                                        ) {
                                            Column {
                                                LithfitGarmentImage(
                                                    localImagePath = null,
                                                    thumbnailBase64 = null,
                                                    imageUrl = null,
                                                    imageResKey = entry.imageResKey,
                                                    contentDescription = entry.title,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(92.dp)
                                                )
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = entry.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = entry.title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search + Filter + Visual Search Bar
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        if (visualSearchQuery != null) viewModel.clearVisualSearch()
                                        searchQuery = it
                                    },
                                    placeholder = { Text("Search streetwear, Y2K, Afro, cargos...") },
                                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty() || visualSearchQuery != null) {
                                            IconButton(onClick = {
                                                searchQuery = ""
                                                viewModel.clearVisualSearch()
                                            }) {
                                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("discover_search_input")
                                )

                                Surface(
                                    onClick = { showFilterDrawer = !showFilterDrawer },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (showFilterDrawer || selectedColorFilter != null) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Tune, contentDescription = "Filter colour and sort")
                                    }
                                }

                                Surface(
                                    onClick = {
                                        visualPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .testTag("visual_search_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (visualSearchBusy) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Icon(Icons.Outlined.CameraAlt, contentDescription = "Visual search")
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = showFilterDrawer) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Colourway", style = MaterialTheme.typography.labelLarge)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Sort:", style = MaterialTheme.typography.labelLarge)
                                        DiscoverSortOption.entries.forEach { opt ->
                                            FilterChip(
                                                selected = sortOption == opt,
                                                onClick = { sortOption = opt },
                                                label = { Text(opt.label) }
                                            )
                                        }
                                    }
                                }
                            }

                            visualSearchQuery?.let { vq ->
                                AssistChip(
                                    onClick = { viewModel.clearVisualSearch() },
                                    label = { Text("Visual match: $vq (tap to clear)") }
                                )
                            }
                        }
                    }

                    // Category Pills
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(LithfitConstants.DISCOVERY_CATEGORIES) { category ->
                                FilterChip(
                                    selected = selectedSection == category,
                                    onClick = { selectedSection = category },
                                    label = { Text(category) },
                                    modifier = Modifier.testTag("discover_cat_${category.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }

                    // Aesthetic Culture Chips
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedVibe == "All",
                                    onClick = { selectedVibe = "All" },
                                    label = { Text("All Vibes") }
                                )
                            }
                            items(LithfitConstants.STYLE_CATEGORIES) { vibe ->
                                FilterChip(
                                    selected = selectedVibe == vibe,
                                    onClick = { selectedVibe = if (selectedVibe == vibe) "All" else vibe },
                                    label = { Text(vibe) }
                                )
                            }
                        }
                    }

                    // Cross-App Search Hits (Wardrobe & Saved Outfits)
                    if (matchingWardrobe.isNotEmpty() || matchingOutfits.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "From Your Closet & Outfits (${matchingWardrobe.size + matchingOutfits.size})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(matchingWardrobe, key = { "w_${it.id}" }) { wItem ->
                                        AssistChip(
                                            onClick = {
                                                onAskStylistAboutLook("Style my ${wItem.color} ${wItem.name} in a fresh outfit.")
                                            },
                                            leadingIcon = { ColorSwatchDot(colorName = wItem.color, size = 12.dp) },
                                            label = { Text(wItem.name) }
                                        )
                                    }
                                    items(matchingOutfits, key = { "o_${it.id}" }) { outfit ->
                                        AssistChip(
                                            onClick = onOpenOutfitBuilder,
                                            leadingIcon = { Icon(Icons.Outlined.Style, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            label = { Text(outfit.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Editorial 2-Column Lookbook Grid with Load More Pagination
                    if (filteredPosts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Checkroom, contentDescription = null, modifier = Modifier.size(36.dp))
                                    Text("No matching looks", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Try switching category pills or clearing filters.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (hasActiveFilters) {
                                        OutlinedButton(onClick = {
                                            searchQuery = ""
                                            viewModel.clearVisualSearch()
                                            selectedSection = "For You"
                                            selectedVibe = "All"
                                            selectedColorFilter = null
                                        }) {
                                            Text("Reset Filters")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val rows = pagedPosts.chunked(2)
                        items(rows, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { post ->
                                    val savedRecord = savedItems.firstOrNull {
                                        it.targetType == "DISCOVERY_POST" && it.targetId == post.id
                                    }
                                    DiscoverLookGridCard(
                                        post = post,
                                        isSaved = savedRecord != null,
                                        isLiked = savedRecord?.liked == true,
                                        onClick = {
                                            viewModel.recordDiscoveryView(post)
                                            selectedPostForDetail = post
                                        },
                                        onLike = { viewModel.toggleLikeDiscoveryPost(post) },
                                        onSave = { viewModel.toggleSaveDiscoveryPost(post) },
                                        onAddToBoard = { boardPickerTargetPost = post },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        if (filteredPosts.size > visibleCount) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { visibleCount += 12 },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("discover_load_more_button")
                                    ) {
                                        Text("Load More Looks (${filteredPosts.size - visibleCount} more)")
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

@Composable
private fun DiscoverLookGridCard(
    post: DiscoveryPost,
    isSaved: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onAddToBoard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("discover_grid_${post.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
            ) {
                LithfitGarmentImage(
                    localImagePath = null,
                    thumbnailBase64 = null,
                    imageUrl = post.imageUrl,
                    imageResKey = post.imageResKey,
                    paletteNames = post.colorPalette,
                    contentDescription = post.title,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = post.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        post.colorPalette.take(3).forEach { col ->
                            ColorSwatchDot(colorName = col, size = 10.dp)
                        }
                    }
                    Row {
                        IconButton(
                            onClick = onLike,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like look",
                                modifier = Modifier.size(16.dp),
                                tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onSave,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save look",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onAddToBoard,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CollectionsBookmark,
                                contentDescription = "Pin to Style Board",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoveryPostDetailDialog(
    post: DiscoveryPost,
    onDismiss: () -> Unit,
    onAddToBoard: () -> Unit,
    onReport: () -> Unit,
    onStyleSimilar: () -> Unit
) {
    val audienceLabel = when (post.targetAudience) {
        "HIM" -> "Men's"
        "HER" -> "Women's"
        else -> "Gender-Neutral"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(post.title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LithfitGarmentImage(
                    localImagePath = null,
                    thumbnailBase64 = null,
                    imageUrl = post.imageUrl,
                    imageResKey = post.imageResKey,
                    paletteNames = post.colorPalette,
                    contentDescription = post.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Text(
                    text = "$audienceLabel • ${post.category} • ${post.sectionTag}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(post.description, style = MaterialTheme.typography.bodyMedium)

                if (post.colorPalette.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        post.colorPalette.forEach { colorName ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ColorSwatchDot(colorName = colorName, size = 14.dp)
                                Text(colorName, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                if (post.outfitBreakdown.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Outfit Breakdown", style = MaterialTheme.typography.labelLarge)
                            post.outfitBreakdown.forEach { piece ->
                                Text("• $piece", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onAddToBoard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pin to Style Board")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onReport) {
                Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Report")
            }
        },
        confirmButton = {
            Button(onClick = onStyleSimilar) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Style Similar")
            }
        }
    )
}

@Composable
private fun AddToStyleBoardDialog(
    post: DiscoveryPost,
    boards: List<StyleBoard>,
    onDismiss: () -> Unit,
    onToggleBoard: (StyleBoard) -> Unit,
    onCreateBoardAndAdd: (name: String, aesthetic: String) -> Unit
) {
    var quickName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pin to Style Board") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Save \"${post.title}\" into any personal Style Board.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (boards.isEmpty()) {
                    Text(
                        "Create your first Style Board below:",
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    boards.forEach { board ->
                        val included = post.id in board.itemIds
                        Surface(
                            onClick = { onToggleBoard(board) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (included) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(board.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${board.aesthetic} • ${board.itemIds.size} looks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (included) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Pinned",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = quickName,
                    onValueChange = { quickName = it },
                    label = { Text("Quick Create New Board") },
                    placeholder = { Text("e.g. Weekend Streetwear") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (quickName.isNotBlank()) {
                    Button(
                        onClick = { onCreateBoardAndAdd(quickName.trim(), post.category) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create & Pin Look")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditStyleBoardDialog(
    initialBoard: StyleBoard?,
    onDismiss: () -> Unit,
    onSave: (StyleBoard) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember(initialBoard) { mutableStateOf(initialBoard?.name.orEmpty()) }
    var description by remember(initialBoard) { mutableStateOf(initialBoard?.description.orEmpty()) }
    var aesthetic by remember(initialBoard) { mutableStateOf(initialBoard?.aesthetic ?: LithfitConstants.VIBES.first()) }
    var occasion by remember(initialBoard) { mutableStateOf(initialBoard?.occasion ?: LithfitConstants.OCCASIONS.first()) }
    val selectedColors = remember(initialBoard) {
        mutableStateListOf<String>().apply {
            addAll(initialBoard?.colors.orEmpty())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBoard == null) "Create Style Board" else "Customize Board") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Board Name *") },
                    placeholder = { Text("e.g. Oversized Fall Fits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Aesthetic Focus", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LithfitConstants.VIBES) { vibe ->
                        FilterChip(
                            selected = aesthetic == vibe,
                            onClick = { aesthetic = vibe },
                            label = { Text(vibe) }
                        )
                    }
                }
                Text("Occasion", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LithfitConstants.OCCASIONS) { occ ->
                        FilterChip(
                            selected = occasion == occ,
                            onClick = { occasion = occ },
                            label = { Text(occ) }
                        )
                    }
                }
                Text("Colourway Mood", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LithfitConstants.COLOR_SWATCHES.take(12).forEach { swatch ->
                        val active = swatch.name in selectedColors
                        ColorSwatchChip(
                            colorName = swatch.name,
                            selected = active,
                            onClick = {
                                if (active) selectedColors.remove(swatch.name) else selectedColors.add(swatch.name)
                            }
                        )
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete Board", color = MaterialTheme.colorScheme.error)
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
                    onSave(
                        (initialBoard ?: StyleBoard()).copy(
                            name = name.trim(),
                            description = description.trim(),
                            aesthetic = aesthetic,
                            occasion = occasion,
                            colors = selectedColors.toList()
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Board")
            }
        }
    )
}

@Composable
private fun StyleBoardDetailDialog(
    board: StyleBoard,
    allPosts: List<DiscoveryPost>,
    onDismiss: () -> Unit,
    onOpenPost: (DiscoveryPost) -> Unit,
    onRemovePost: (DiscoveryPost) -> Unit,
    onStyleBoardVibe: () -> Unit
) {
    val pinnedPosts = remember(board.itemIds, allPosts) {
        board.itemIds.mapNotNull { id -> allPosts.firstOrNull { it.id == id } }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(board.name) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "${board.aesthetic} • ${board.occasion}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (board.description.isNotBlank()) {
                    Text(board.description, style = MaterialTheme.typography.bodySmall)
                }
                if (pinnedPosts.isEmpty()) {
                    Text(
                        "No looks pinned here yet. Open any Editorial look and tap Pin to Style Board.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    pinnedPosts.forEach { post ->
                        Card(
                            onClick = { onOpenPost(post) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LithfitGarmentImage(
                                    localImagePath = null,
                                    thumbnailBase64 = null,
                                    imageUrl = post.imageUrl,
                                    imageResKey = post.imageResKey,
                                    contentDescription = post.title,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(post.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(post.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onRemovePost(post) }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Remove from board")
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        confirmButton = {
            Button(onClick = onStyleBoardVibe) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Style Board Vibe")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateCommunityPostDialog(
    onDismiss: () -> Unit,
    onPublish: (title: String, caption: String, aesthetic: String, photoUri: Uri?, palette: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var aesthetic by remember { mutableStateOf(LithfitConstants.VIBES.first()) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val selectedColors = remember { mutableStateListOf<String>() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedPhotoUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Your Real Fit") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pickedPhotoUri != null) {
                    AsyncImage(
                        model = pickedPhotoUri,
                        contentDescription = "Selected community outfit photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (pickedPhotoUri == null) "Select Outfit Photo" else "Change Photo")
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Look Title *") },
                    placeholder = { Text("e.g. Oversized Pink Cargos & Cross Tee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Fit Breakdown / Notes") },
                    placeholder = { Text("Share pieces, sizing, or styling details...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Aesthetic", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LithfitConstants.VIBES) { vibe ->
                        FilterChip(
                            selected = aesthetic == vibe,
                            onClick = { aesthetic = vibe },
                            label = { Text(vibe) }
                        )
                    }
                }
                Text("Colourway", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LithfitConstants.COLOR_SWATCHES.take(10).forEach { swatch ->
                        val active = swatch.name in selectedColors
                        ColorSwatchChip(
                            colorName = swatch.name,
                            selected = active,
                            onClick = {
                                if (active) selectedColors.remove(swatch.name) else selectedColors.add(swatch.name)
                            }
                        )
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
                    onPublish(
                        title.trim(),
                        caption.trim(),
                        aesthetic,
                        pickedPhotoUri,
                        selectedColors.toList()
                    )
                },
                enabled = title.isNotBlank() && pickedPhotoUri != null
            ) {
                Text("Publish Fit")
            }
        }
    )
}
