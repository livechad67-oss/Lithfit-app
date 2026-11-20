package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.example.ui.screens.AiStylistScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WardrobeScreen
import com.example.ui.viewmodel.LithfitViewModel

enum class LithfitTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    DISCOVER("Discover", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_discover"),
    AI_STYLIST("AI Stylist", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai_stylist"),
    WARDROBE("My Wardrobe", Icons.Filled.Checkroom, Icons.Outlined.Checkroom, "nav_wardrobe"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun MainShellScreen(
    viewModel: LithfitViewModel,
    onSignOut: () -> Unit,
    onDeleteAccount: suspend () -> Result<Unit>
) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(LithfitTab.HOME) }
    var prefillAiPrompt by remember { mutableStateOf<String?>(null) }
    var discoverSectionRequest by remember { mutableStateOf<String?>(null) }
    var discoverRadarModeRequest by remember { mutableStateOf(false) }
    var styleRadarInitialSearch by remember { mutableStateOf<String?>(null) }
    var openWardrobeSectionRequest by remember { mutableStateOf<Int?>(null) }

    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    BackHandler(enabled = currentTab != LithfitTab.HOME) {
        currentTab = LithfitTab.HOME
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                windowInsets = WindowInsets(0.dp)
                            ) {
                                LithfitTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                            Text(
                                text = "Developed by CLASTAD",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                            )
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
                if (useRail) {
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
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Developed by\nCLASTAD",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
                        },
                        label = "lithfit_tab_transition"
                    ) { tab ->
                        when (tab) {
                            LithfitTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDiscover = { category ->
                                    discoverSectionRequest = category
                                    discoverRadarModeRequest = false
                                    currentTab = LithfitTab.DISCOVER
                                },
                                onOpenAiStylist = { prompt ->
                                    prefillAiPrompt = prompt
                                    currentTab = LithfitTab.AI_STYLIST
                                },
                                onOpenWardrobe = { sectionIndex ->
                                    openWardrobeSectionRequest = sectionIndex
                                    currentTab = LithfitTab.WARDROBE
                                }
                            )

                            LithfitTab.DISCOVER -> DiscoverScreen(
                                viewModel = viewModel,
                                initialSectionFilter = discoverSectionRequest,
                                onClearInitialSectionFilter = { discoverSectionRequest = null },
                                initialRadarMode = discoverRadarModeRequest,
                                onClearInitialRadarMode = { discoverRadarModeRequest = false },
                                initialRadarQuery = styleRadarInitialSearch,
                                onClearInitialRadarQuery = { styleRadarInitialSearch = null },
                                onAskStylistAboutLook = { prompt ->
                                    if (prompt.isNotBlank()) prefillAiPrompt = prompt
                                    currentTab = LithfitTab.AI_STYLIST
                                },
                                onOpenOutfitBuilder = {
                                    openWardrobeSectionRequest = 2
                                    currentTab = LithfitTab.WARDROBE
                                }
                            )

                            LithfitTab.AI_STYLIST -> AiStylistScreen(
                                viewModel = viewModel,
                                initialPrefillPrompt = prefillAiPrompt,
                                onClearPrefill = { prefillAiPrompt = null },
                                onOpenStyleRadarWithOutfit = { outfit ->
                                    styleRadarInitialSearch = outfit.vibe
                                    discoverRadarModeRequest = true
                                    currentTab = LithfitTab.DISCOVER
                                }
                            )

                            LithfitTab.WARDROBE -> WardrobeScreen(
                                viewModel = viewModel,
                                requestedSection = openWardrobeSectionRequest,
                                onClearRequestedSection = { openWardrobeSectionRequest = null },
                                onStyleItemWithAi = { prompt ->
                                    prefillAiPrompt = prompt
                                    currentTab = LithfitTab.AI_STYLIST
                                },
                                onOpenStyleRadarForOutfit = { outfit ->
                                    styleRadarInitialSearch = outfit.vibe
                                    discoverRadarModeRequest = true
                                    currentTab = LithfitTab.DISCOVER
                                },
                                onPreviewOutfitImage = { outfit ->
                                    prefillAiPrompt = "Refine and style my saved outfit \"${outfit.name}\" (${outfit.itemNames.joinToString(", ")}) for ${outfit.occasion}."
                                    viewModel.generateOutfitPreview(
                                        context = context,
                                        title = outfit.name,
                                        items = outfit.itemNames,
                                        colors = outfit.colorPalette,
                                        occasion = outfit.occasion,
                                        vibe = outfit.vibe,
                                        outfitId = outfit.id.ifBlank { null }
                                    )
                                    currentTab = LithfitTab.AI_STYLIST
                                }
                            )

                            LithfitTab.PROFILE -> ProfileScreen(
                                viewModel = viewModel,
                                onSignOut = onSignOut,
                                onDeleteAccount = onDeleteAccount,
                                onOpenStyleRadarForOutfit = { outfit ->
                                    styleRadarInitialSearch = outfit.vibe
                                    discoverRadarModeRequest = true
                                    currentTab = LithfitTab.DISCOVER
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
