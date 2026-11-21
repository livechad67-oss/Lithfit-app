package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LithfitLogoMark
import com.example.ui.components.QuickActionType
import com.example.ui.screens.AddOrEditGarmentDialog
import com.example.ui.screens.AiStylistScreen
import com.example.ui.screens.AvatarStudioScreen
import com.example.ui.screens.ColourMatchingOverlay
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.GlobalSearchOverlay
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsInboxOverlay
import com.example.ui.screens.OutfitsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WardrobeScreen
import com.example.ui.screens.WishlistOverlay
import com.example.ui.viewmodel.LithfitViewModel

enum class LithfitTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    DISCOVER("Discover", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_discover"),
    AI("AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai"),
    WARDROBE("Wardrobe", Icons.Filled.Checkroom, Icons.Outlined.Checkroom, "nav_wardrobe"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline, "nav_profile")
}

enum class LithfitOverlay {
    NONE,
    AI_STYLIST,
    PROFILE_SETTINGS,
    NOTIFICATIONS_INBOX,
    WISHLIST,
    COLOUR_MATCHING,
    GLOBAL_SEARCH,
    AVATAR_STUDIO,
    OUTFITS_CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    viewModel: LithfitViewModel,
    onSignOut: () -> Unit,
    onDeleteAccount: suspend () -> Result<Unit>
) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(LithfitTab.HOME) }
    var activeOverlay by rememberSaveable { mutableStateOf(LithfitOverlay.NONE) }
    var showGlobalAddGarmentDialog by remember { mutableStateOf(false) }

    var prefillAiPrompt by remember { mutableStateOf<String?>(null) }
    var discoverSectionRequest by remember { mutableStateOf<String?>(null) }
    var discoverRadarModeRequest by remember { mutableStateOf(false) }
    var styleRadarInitialSearch by remember { mutableStateOf<String?>(null) }
    var openOutfitsSectionRequest by remember { mutableStateOf<Int?>(null) }

    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val recognitionState by viewModel.garmentRecognition.collectAsStateWithLifecycle()
    val catalogue by viewModel.catalogueProducts.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    BackHandler(enabled = activeOverlay != LithfitOverlay.NONE) {
        activeOverlay = LithfitOverlay.NONE
    }
    BackHandler(enabled = activeOverlay == LithfitOverlay.NONE && currentTab != LithfitTab.HOME) {
        currentTab = LithfitTab.HOME
    }

    val openOutfitsOverlay: (Int?) -> Unit = { sectionIdx ->
        openOutfitsSectionRequest = sectionIdx
        activeOverlay = LithfitOverlay.OUTFITS_CALENDAR
    }

    val openAiDestination: (String?) -> Unit = { prompt ->
        if (!prompt.isNullOrBlank()) {
            prefillAiPrompt = prompt
        }
        activeOverlay = LithfitOverlay.NONE
        currentTab = LithfitTab.AI
    }

    val triggerQuickAction: (QuickActionType) -> Unit = { action ->
        when (action) {
            QuickActionType.AI_STYLIST -> openAiDestination(null)
            QuickActionType.ADD_CLOTHING -> {
                viewModel.resetGarmentRecognition()
                showGlobalAddGarmentDialog = true
            }
            QuickActionType.OUTFIT_BUILDER -> openOutfitsOverlay(1)
            QuickActionType.COLOUR_MATCHING -> activeOverlay = LithfitOverlay.COLOUR_MATCHING
            QuickActionType.STYLE_ADVICE -> {
                openAiDestination("Give me practical, confidence-boosting styling advice for my current wardrobe.")
            }
            QuickActionType.WISHLIST -> activeOverlay = LithfitOverlay.WISHLIST
            QuickActionType.SEARCH -> activeOverlay = LithfitOverlay.GLOBAL_SEARCH
            QuickActionType.NOTIFICATIONS -> activeOverlay = LithfitOverlay.NOTIFICATIONS_INBOX
        }
    }

    if (showGlobalAddGarmentDialog) {
        AddOrEditGarmentDialog(
            existing = null,
            recognitionState = recognitionState,
            catalogue = catalogue,
            onAnalyzeUri = { uri -> viewModel.analyzeWardrobeUri(context, uri) },
            onAnalyzeBitmap = { bmp -> viewModel.analyzeWardrobeBitmap(context, bmp) },
            onImportUrl = { url -> viewModel.importImageFromUrl(context, url) },
            onDismiss = {
                showGlobalAddGarmentDialog = false
                viewModel.resetGarmentRecognition()
            },
            onSave = { item ->
                viewModel.saveWardrobeItem(item) { ok ->
                    if (ok) {
                        showGlobalAddGarmentDialog = false
                        viewModel.resetGarmentRecognition()
                    }
                }
            },
            onDelete = null
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail && activeOverlay == LithfitOverlay.NONE) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        shadowElevation = 2.dp
                    ) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            LithfitTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    alwaysShowLabel = true,
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && activeOverlay == LithfitOverlay.NONE) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            LithfitLogoMark(
                                size = 32.dp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    ) {
                        LithfitTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "CLASTAD",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (activeOverlay != LithfitOverlay.NONE) {
                        when (activeOverlay) {
                            LithfitOverlay.AI_STYLIST -> AiStylistScreen(
                                viewModel = viewModel,
                                initialPrefillPrompt = prefillAiPrompt,
                                onClearPrefill = { prefillAiPrompt = null },
                                onBack = { activeOverlay = LithfitOverlay.NONE },
                                onOpenWardrobe = {
                                    activeOverlay = LithfitOverlay.NONE
                                    currentTab = LithfitTab.WARDROBE
                                },
                                onOpenOutfitsCalendar = {
                                    openOutfitsOverlay(2)
                                },
                                onOpenStyleRadarWithOutfit = { outfit ->
                                    styleRadarInitialSearch = outfit.vibe
                                    discoverRadarModeRequest = true
                                    activeOverlay = LithfitOverlay.NONE
                                    currentTab = LithfitTab.DISCOVER
                                },
                                onOpenSettings = {
                                    activeOverlay = LithfitOverlay.NONE
                                    currentTab = LithfitTab.PROFILE
                                },
                                showBackButton = true
                            )

                            LithfitOverlay.PROFILE_SETTINGS -> ProfileScreen(
                                viewModel = viewModel,
                                onBack = { activeOverlay = LithfitOverlay.NONE },
                                onOpenNotificationsInbox = { activeOverlay = LithfitOverlay.NOTIFICATIONS_INBOX },
                                onOpenStyleRadarForOutfit = { outfit ->
                                    styleRadarInitialSearch = outfit.vibe
                                    discoverRadarModeRequest = true
                                    activeOverlay = LithfitOverlay.NONE
                                    currentTab = LithfitTab.DISCOVER
                                },
                                onSignOut = onSignOut,
                                onDeleteAccount = onDeleteAccount,
                                showBackButton = true
                            )

                            LithfitOverlay.NOTIFICATIONS_INBOX -> NotificationsInboxOverlay(
                                viewModel = viewModel,
                                onBack = { activeOverlay = LithfitOverlay.NONE }
                            )

                            LithfitOverlay.WISHLIST -> WishlistOverlay(
                                viewModel = viewModel,
                                onBack = { activeOverlay = LithfitOverlay.NONE },
                                onAskStylist = { prompt -> openAiDestination(prompt) },
                                onOpenRadar = {
                                    discoverRadarModeRequest = true
                                    activeOverlay = LithfitOverlay.NONE
                                    currentTab = LithfitTab.DISCOVER
                                }
                            )

                            LithfitOverlay.COLOUR_MATCHING -> ColourMatchingOverlay(
                                viewModel = viewModel,
                                onBack = { activeOverlay = LithfitOverlay.NONE },
                                onAskStylistWithPalette = { prompt -> openAiDestination(prompt) }
                            )

                            LithfitOverlay.GLOBAL_SEARCH -> GlobalSearchOverlay(
                                viewModel = viewModel,
                                onBack = { activeOverlay = LithfitOverlay.NONE },
                                onOpenWardrobe = {
                                    activeOverlay = LithfitOverlay.NONE
                                    currentTab = LithfitTab.WARDROBE
                                },
                                onOpenOutfits = { openOutfitsOverlay(0) },
                                onOpenDiscover = { tag ->
                                    if (tag == "Radar") {
                                        discoverRadarModeRequest = true
                                    } else {
                                        discoverSectionRequest = tag
                                        discoverRadarModeRequest = false
                                    }
                                    activeOverlay = LithfitOverlay.NONE
                                    currentTab = LithfitTab.DISCOVER
                                },
                                onAskStylist = { prompt -> openAiDestination(prompt) }
                            )

                            LithfitOverlay.AVATAR_STUDIO -> Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Outfit Studio", style = MaterialTheme.typography.titleLarge) },
                                        navigationIcon = {
                                            IconButton(
                                                onClick = { activeOverlay = LithfitOverlay.NONE },
                                                modifier = Modifier.testTag("overlay_back_button")
                                            ) {
                                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                                    )
                                }
                            ) { pad ->
                                Box(modifier = Modifier.padding(pad)) {
                                    AvatarStudioScreen(
                                        viewModel = viewModel,
                                        onAskStylistWithAvatar = { prompt -> openAiDestination(prompt) }
                                    )
                                }
                            }

                            LithfitOverlay.OUTFITS_CALENDAR -> Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Outfits & Calendar", style = MaterialTheme.typography.titleLarge) },
                                        navigationIcon = {
                                            IconButton(
                                                onClick = { activeOverlay = LithfitOverlay.NONE },
                                                modifier = Modifier.testTag("overlay_back_button")
                                            ) {
                                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                                    )
                                }
                            ) { pad ->
                                Box(modifier = Modifier.padding(pad)) {
                                    OutfitsScreen(
                                        viewModel = viewModel,
                                        requestedSection = openOutfitsSectionRequest,
                                        onClearRequestedSection = { openOutfitsSectionRequest = null },
                                        onOpenStyleRadarForOutfit = { outfit ->
                                            styleRadarInitialSearch = outfit.vibe
                                            discoverRadarModeRequest = true
                                            activeOverlay = LithfitOverlay.NONE
                                            currentTab = LithfitTab.DISCOVER
                                        },
                                        onAskStylist = { prompt -> openAiDestination(prompt) }
                                    )
                                }
                            }

                            LithfitOverlay.NONE -> Unit
                        }
                    } else {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(110)) togetherWith fadeOut(animationSpec = tween(90))
                            },
                            label = "lithfit_tab_transition"
                        ) { tab ->
                            when (tab) {
                                LithfitTab.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onOpenOverlay = { overlay, prompt ->
                                        if (overlay == LithfitOverlay.AI_STYLIST) {
                                            openAiDestination(prompt)
                                        } else if (overlay == LithfitOverlay.PROFILE_SETTINGS) {
                                            currentTab = LithfitTab.PROFILE
                                        } else {
                                            if (!prompt.isNullOrBlank()) prefillAiPrompt = prompt
                                            activeOverlay = overlay
                                        }
                                    },
                                    onOpenOutfits = { sectionIdx -> openOutfitsOverlay(sectionIdx) },
                                    onOpenWardrobe = { currentTab = LithfitTab.WARDROBE },
                                    onOpenDiscover = { category ->
                                        discoverSectionRequest = category
                                        discoverRadarModeRequest = false
                                        currentTab = LithfitTab.DISCOVER
                                    },
                                    onOpenAddGarment = {
                                        viewModel.resetGarmentRecognition()
                                        showGlobalAddGarmentDialog = true
                                    },
                                    onQuickAction = triggerQuickAction
                                )

                                LithfitTab.DISCOVER -> DiscoverScreen(
                                    viewModel = viewModel,
                                    initialSectionFilter = discoverSectionRequest,
                                    onClearInitialSectionFilter = { discoverSectionRequest = null },
                                    initialRadarMode = discoverRadarModeRequest,
                                    onClearInitialRadarMode = { discoverRadarModeRequest = false },
                                    initialRadarQuery = styleRadarInitialSearch,
                                    onClearInitialRadarQuery = { styleRadarInitialSearch = null },
                                    onAskStylistAboutLook = { prompt -> openAiDestination(prompt) },
                                    onOpenOutfitBuilder = { openOutfitsOverlay(1) }
                                )

                                LithfitTab.AI -> AiStylistScreen(
                                    viewModel = viewModel,
                                    initialPrefillPrompt = prefillAiPrompt,
                                    onClearPrefill = { prefillAiPrompt = null },
                                    onBack = { currentTab = LithfitTab.HOME },
                                    onOpenWardrobe = { currentTab = LithfitTab.WARDROBE },
                                    onOpenOutfitsCalendar = { openOutfitsOverlay(2) },
                                    onOpenStyleRadarWithOutfit = { outfit ->
                                        styleRadarInitialSearch = outfit.vibe
                                        discoverRadarModeRequest = true
                                        currentTab = LithfitTab.DISCOVER
                                    },
                                    onOpenSettings = { currentTab = LithfitTab.PROFILE },
                                    showBackButton = false
                                )

                                LithfitTab.WARDROBE -> WardrobeScreen(
                                    viewModel = viewModel,
                                    onOpenAddGarment = {
                                        viewModel.resetGarmentRecognition()
                                        showGlobalAddGarmentDialog = true
                                    },
                                    onOpenOutfitsTab = { sectionIdx -> openOutfitsOverlay(sectionIdx) },
                                    onOpenAvatarStudio = {
                                        activeOverlay = LithfitOverlay.AVATAR_STUDIO
                                    },
                                    onStyleItemWithAi = { prompt -> openAiDestination(prompt) }
                                )

                                LithfitTab.PROFILE -> ProfileScreen(
                                    viewModel = viewModel,
                                    onBack = { currentTab = LithfitTab.HOME },
                                    onOpenNotificationsInbox = { activeOverlay = LithfitOverlay.NOTIFICATIONS_INBOX },
                                    onOpenStyleRadarForOutfit = { outfit ->
                                        styleRadarInitialSearch = outfit.vibe
                                        discoverRadarModeRequest = true
                                        currentTab = LithfitTab.DISCOVER
                                    },
                                    onSignOut = onSignOut,
                                    onDeleteAccount = onDeleteAccount,
                                    showBackButton = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
