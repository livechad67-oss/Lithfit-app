package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.core.configureAppCheck
import com.example.data.model.ThemeMode
import com.example.data.model.UiState
import com.example.data.repository.AuthRepository
import com.example.data.repository.LithfitRepository
import com.example.ui.MainShellScreen
import com.example.ui.screens.LegalAndOnboardingScreen
import com.example.ui.screens.OnboardingGuideScreen
import com.example.ui.screens.StartupSplashScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.theme.LithfitTheme
import com.example.ui.viewmodel.LithfitViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase App Check BEFORE enableEdgeToEdge / ViewModels / AI calls
        configureAppCheck(this, intent)
        super.onCreate(savedInstanceState)

        // Configure fast memory + disk image cache for smoother Discover & Wardrobe scrolling
        Coil.setImageLoader {
            ImageLoader.Builder(applicationContext)
                .memoryCache {
                    MemoryCache.Builder(applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(applicationContext.cacheDir.resolve("lithfit_image_cache"))
                        .maxSizeBytes(64L * 1024L * 1024L)
                        .build()
                }
                .crossfade(120)
                .build()
        }

        enableEdgeToEdge()

        setContent {
            LithfitAppRoot()
        }
    }
}

@Composable
private fun LithfitAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    val authUser by authRepository.authStateFlow(context).collectAsStateWithLifecycle(
        initialValue = authRepository.currentUser(context)
    )
    var showSplash by remember { mutableStateOf(true) }
    var pendingRegDisplayName by remember { mutableStateOf<String?>(null) }
    var pendingRegUsername by remember { mutableStateOf<String?>(null) }

    val currentUser = authUser
    // Pre-parse user store JSON off the main thread while splash animates so Home opens instantaneously
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        launch(Dispatchers.IO) {
            runCatching { LithfitRepository(context.applicationContext).warmUpUserStore(uid) }
        }
    }

    if (showSplash) {
        LithfitTheme(themeMode = ThemeMode.LIGHT) {
            StartupSplashScreen(onFinished = { showSplash = false })
        }
    } else if (currentUser == null) {
        LithfitTheme(themeMode = ThemeMode.LIGHT) {
            WelcomeAuthScreen(
                authRepository = authRepository,
                onAuthenticated = { dispName, username ->
                    pendingRegDisplayName = dispName
                    pendingRegUsername = username
                }
            )
        }
    } else {
        key(currentUser.uid) {
            val userViewModel: LithfitViewModel = viewModel(
                key = "lithfit_vm_${currentUser.uid}",
                factory = LithfitViewModel.provideFactory(context, currentUser.uid)
            )
            val profileState by userViewModel.userProfile.collectAsStateWithLifecycle()
            val legalState by userViewModel.legalAcceptance.collectAsStateWithLifecycle()
            val hasSeenIntroGuide by userViewModel.hasSeenIntroGuide.collectAsStateWithLifecycle()

            val loadedProfile = (profileState as? UiState.Success)?.data
            val themeMode = ThemeMode.safeParse(loadedProfile?.themeMode)
            val largeReadableText = loadedProfile?.largeReadableText == true
            val reducedMotion = loadedProfile?.reducedMotion == true

            LithfitTheme(
                themeMode = themeMode,
                largeReadableText = largeReadableText,
                reducedMotion = reducedMotion
            ) {
                when {
                    profileState is UiState.Loading || legalState is UiState.Loading -> {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    profileState is UiState.Error || legalState is UiState.Error -> {
                        val errMsg = (profileState as? UiState.Error)?.message
                            ?: (legalState as? UiState.Error)?.message
                            ?: "Unable to load your LITHFIT account."
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Could not reach your wardrobe", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = errMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                Button(onClick = { scope.launch { authRepository.signOut(context) } }) {
                                    Text("Switch Account")
                                }
                            }
                        }
                    }
                    else -> {
                        val legal = (legalState as? UiState.Success)?.data
                        if (legal == null || loadedProfile == null) {
                            LegalAndOnboardingScreen(
                                userId = currentUser.uid,
                                initialDisplayName = pendingRegDisplayName?.takeIf { it.isNotBlank() }
                                    ?: currentUser.displayName.ifBlank { "Lithfit Member" },
                                initialUsername = pendingRegUsername ?: currentUser.username,
                                initialEmail = currentUser.email,
                                initialPhone = currentUser.phone,
                                initialAvatarUrl = null,
                                onComplete = { cam, photo, loc, notif, profile ->
                                    pendingRegDisplayName = null
                                    pendingRegUsername = null
                                    userViewModel.saveLegalAndOnboardingProfile(
                                        cameraConsent = cam,
                                        photoConsent = photo,
                                        locationConsent = loc,
                                        notificationsConsent = notif,
                                        profile = profile,
                                        onComplete = {}
                                    )
                                },
                                onSignOut = {
                                    pendingRegDisplayName = null
                                    pendingRegUsername = null
                                    scope.launch { authRepository.signOut(context) }
                                }
                            )
                        } else if (!hasSeenIntroGuide) {
                            OnboardingGuideScreen(
                                onFinish = { userViewModel.markIntroGuideSeen() },
                                onSkip = { userViewModel.markIntroGuideSeen() }
                            )
                        } else {
                            MainShellScreen(
                                viewModel = userViewModel,
                                onSignOut = {
                                    scope.launch { authRepository.signOut(context) }
                                },
                                onDeleteAccount = {
                                    authRepository.deleteAccount(context)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
