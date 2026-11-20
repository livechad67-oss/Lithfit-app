package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.LithfitAiService
import com.example.data.ai.StylistIntent
import com.example.data.model.AvatarConfig
import com.example.data.model.GarmentCategory
import com.example.data.model.LegalDocuments
import com.example.data.model.LithfitConstants
import com.example.data.model.PreGenerationOptions
import com.example.data.model.SeedCatalogData
import com.example.data.model.StyleBoard
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.model.WardrobeItem
import com.example.data.repository.AuthRepository
import com.example.ui.LithfitTab
import com.example.ui.components.LithfitWordmark
import com.example.ui.screens.BudgetTier
import com.example.ui.screens.InlineChatImageGenerationCard
import com.example.ui.screens.LegalAndOnboardingScreen
import com.example.ui.screens.OnboardingGuideScreen
import com.example.ui.screens.StartupSplashScreen
import com.example.ui.screens.StylePersonaLookbookCard
import com.example.ui.screens.lithfitGuideSteps
import com.example.ui.theme.LithfitTheme
import com.example.ui.viewmodel.ImageGenUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appNameMatchesLithfitBrand() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LITHFIT", appName)
    }

    @Test
    fun manifestRequestsInternetAndDoesNotRequestLocationPermissions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions?.toList().orEmpty()
        assertTrue(permissions.contains(android.Manifest.permission.INTERNET))
        assertFalse(permissions.contains(android.Manifest.permission.ACCESS_COARSE_LOCATION))
        assertFalse(permissions.contains(android.Manifest.permission.ACCESS_FINE_LOCATION))
    }

    @Test
    fun clastadWebsiteEmailAndUrlAreConfigured() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("https://clastad.xyz", context.getString(R.string.clastad_url))
        assertEquals("Clastad.xyz", context.getString(R.string.clastad_domain))
        assertEquals("livechad67@gmail.com", context.getString(R.string.clastad_notify_email))
    }

    @Test
    fun navigationUsesRedesignedFiveCoreTabs() {
        val labels = LithfitTab.entries.map { it.label }
        assertEquals(listOf("Home", "Discover", "AI Stylist", "My Wardrobe", "Profile"), labels)
        assertFalse(labels.any { it.equals("Nearby", ignoreCase = true) })
    }

    @Test
    fun aiIntentClassifierRoutesGeneralQaVsOutfitRequests() {
        assertEquals(
            StylistIntent.GENERAL_QUESTION,
            LithfitAiService.classifyIntent("What shoes go with baggy cargo trousers?")
        )
        assertEquals(
            StylistIntent.OUTFIT_GENERATION,
            LithfitAiService.classifyIntent("Build me a streetwear outfit for a birthday dinner.")
        )
    }

    @Test
    fun aiInlineMarkerStrippingAndParsingWorks() {
        val raw = "Wear crisp trousers.\n[[LITHFIT_OUTFIT_JSON]]{\"outfitTitle\":\"Clean Fit\",\"topPiece\":\"Navy Knit Polo\",\"bottomPiece\":\"Cream Trousers\",\"footwearPiece\":\"Loafers\",\"jacketPiece\":\"None\",\"accessories\":[\"Watch\"],\"colorPalette\":[\"Navy\",\"Cream\"]}"
        assertEquals("Wear crisp trousers.", LithfitAiService.stripInlineOutfitMarker(raw))
        val parsed = LithfitAiService.parseInlineStructuredOutfit(raw)
        assertNotNull(parsed)
        assertEquals("Clean Fit", parsed?.outfitTitle)
        assertEquals("Navy Knit Polo", parsed?.topPiece)
    }

    @Test
    fun accurateVisualBriefPreservesExactRequestedGarmentsAndFits() {
        val profile = UserProfile(
            gender = "Women's",
            age = "18–24 (Young Adult)",
            height = "5'7\"–5'10\" (169–178 cm)",
            bodySize = "M / Athletic"
        )
        val preGen = PreGenerationOptions(
            gender = "Women's",
            outfitSize = "S",
            fitSilhouette = "Baggy / Oversized",
            footwearVibe = "Chunky Skate Sneakers",
            accessoriesVibe = "Layered Silver Chains"
        )
        val brief = LithfitAiService.buildAccurateVisualBriefPrompt(
            rawPrompt = "pink baggy trouser and a black chromheart shirt",
            items = listOf("pink baggy trouser", "black chromheart shirt"),
            colors = listOf("Pink", "Black"),
            occasion = "Hangout",
            vibe = "Streetwear",
            profile = profile,
            preGenOptions = preGen,
            hasUserPhoto = true
        )
        assertTrue(brief.contains("pink baggy trouser and a black chromheart shirt"))
        assertTrue(brief.contains("Women's"))
        assertTrue(brief.contains("Outfit Size: S"))
        assertTrue(brief.contains("Baggy / Oversized"))
        assertTrue(brief.contains("Chunky Skate Sneakers"))
        assertTrue(brief.contains("Do NOT turn a t-shirt/shirt into a hoodie"))
        assertTrue(brief.contains("Do NOT turn baggy/wide trousers into skinny jeans"))
        assertTrue(brief.contains("VIRTUAL TRY-ON"))
    }

    @Test
    fun usernameValidatorEnforcesFormatAndNormalizesCase() {
        val repo = AuthRepository()
        assertFalse(repo.validateUsername("ab").isValid)
        assertFalse(repo.validateUsername("bad..name").isValid)
        assertFalse(repo.validateUsername("has space").isValid)
        val valid = repo.validateUsername("@Alex_Style.01")
        assertTrue(valid.isValid)
        assertEquals("alex_style.01", valid.normalized)
    }

    @Test
    fun localAuthSignUpSignInAndResetWorkViaSharedPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lithfit_local_auth", Context.MODE_PRIVATE).edit().clear().apply()
        val repo = AuthRepository()
        repo.resetInMemoryForTesting()

        val reg = repo.registerWithUsernamePassword(context, "Jordan", "jordan_fit", "secret123")
        assertTrue(reg.isSuccess)
        assertEquals("jordan_fit", repo.currentUser(context)?.username)

        // Duplicate username is rejected
        val dup = repo.registerWithUsernamePassword(context, "Jordan 2", "jordan_fit", "secret123")
        assertTrue(dup.isFailure)

        repo.signOut(context)
        assertEquals(null, repo.currentUser(context))

        // Wrong password rejected, valid password succeeds
        assertTrue(repo.signInWithUsernamePassword(context, "jordan_fit", "wrongpass").isFailure)
        assertTrue(repo.signInWithUsernamePassword(context, "jordan_fit", "secret123").isSuccess)

        // Password reset updates credentials cleanly
        assertTrue(repo.resetLocalPassword(context, "jordan_fit", "newpass456").isSuccess)
        repo.signOut(context)
        assertTrue(repo.signInWithUsernamePassword(context, "jordan_fit", "newpass456").isSuccess)
    }

    @Test
    fun creditsAttributionMentionsFounderAndClastadCleanly() {
        assertTrue(LegalDocuments.creditsText.contains("Jason Prince"))
        assertTrue(LegalDocuments.creditsText.contains("CLASTAD"))
        assertFalse(LegalDocuments.privacyPolicyText.contains("AI Studio", ignoreCase = true))
        assertFalse(LegalDocuments.termsOfServiceText.contains("Gemini", ignoreCase = true))
        assertFalse(LegalDocuments.privacyPolicyText.contains("Google Places", ignoreCase = true))
    }

    @Test
    fun budgetTierFiltersPricesAccurately() {
        assertTrue(BudgetTier.ALL.matches(250.0))
        assertTrue(BudgetTier.UNDER_75.matches(49.0))
        assertFalse(BudgetTier.UNDER_75.matches(75.0))
        assertTrue(BudgetTier.FROM_75_TO_150.matches(120.0))
        assertTrue(BudgetTier.FROM_150_TO_300.matches(240.0))
        assertTrue(BudgetTier.OVER_300.matches(420.0))
    }

    @Test
    fun garmentCategorySafeParsingHandlesUnknownAndCaseInsensitiveValues() {
        assertEquals(GarmentCategory.JACKETS, GarmentCategory.safeParse("Jackets"))
        assertEquals(GarmentCategory.TROUSERS, GarmentCategory.safeParse("trousers"))
        assertEquals(GarmentCategory.OTHER, GarmentCategory.safeParse("unknown_value"))
    }

    @Test
    fun themeModeSafeParsingDefaultsToLight() {
        assertEquals(ThemeMode.DARK, ThemeMode.safeParse("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.safeParse("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.safeParse("invalid"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.safeParse(null))
    }

    @Test
    fun discoveryCatalogHasOverFortyFiveUniqueHighResLookbooksAcrossAllVibes() {
        val posts = SeedCatalogData.sampleDiscoveryPosts
        assertTrue("Expected >= 45 editorial looks, got ${posts.size}", posts.size >= 45)
        assertTrue(posts.none { it.imageResKey.isNullOrBlank() || it.imageResKey.orEmpty().startsWith("vector_") })
        val keys = posts.mapNotNull { it.imageResKey }
        assertEquals(posts.size, keys.toSet().size)
        keys.forEach { key ->
            assertNotNull("Missing drawable mapping for $key", SeedCatalogData.editorialDrawableForKey(key))
        }

        // Verify every vibe in STYLE_CATEGORIES has at least one matching lookbook
        LithfitConstants.STYLE_CATEGORIES.forEach { vibe ->
            assertTrue(
                "Missing recommendation coverage for vibe: $vibe",
                posts.any { p ->
                    p.category.equals(vibe, ignoreCase = true) ||
                        p.tags.any { t -> t.equals(vibe, ignoreCase = true) }
                }
            )
        }

        assertTrue(SeedCatalogData.dailyOutfitTips.isNotEmpty())
        SeedCatalogData.dailyOutfitTips.forEach { tip ->
            assertNotNull(SeedCatalogData.editorialDrawableForKey(tip.featuredImageResKey))
        }
        assertTrue(SeedCatalogData.trendingHighlights.isNotEmpty())
        SeedCatalogData.trendingHighlights.forEach { highlight ->
            assertNotNull(SeedCatalogData.editorialDrawableForKey(highlight.imageResKey))
        }
    }

    @Test
    fun seedCatalogIsInclusiveAcrossCulturesAgesAndAudiences() {
        val posts = SeedCatalogData.sampleDiscoveryPosts
        assertTrue(posts.isNotEmpty())
        assertTrue(posts.any { it.targetAudience == "HIM" })
        assertTrue(posts.any { it.targetAudience == "HER" })
        assertTrue(posts.any { it.targetAudience == "UNISEX" })
        assertTrue(posts.any { it.ageGroup == "TEEN" })
        assertTrue(posts.any { it.culturalTag != null })
        assertTrue(LithfitConstants.DISCOVERY_CATEGORIES.containsAll(listOf("For You", "Teenage", "Cultural", "Gender-Neutral")))
        assertTrue(LithfitConstants.HAIR_STYLES.size >= 10)
        assertTrue(LithfitConstants.STYLE_CATEGORIES.size >= 18)
        assertTrue(LithfitConstants.COLOR_SWATCHES.size >= 21)
    }

    @Test
    fun userProfileStoresBodyAndDemographicPreferences() {
        val profile = UserProfile(
            gender = "Men's",
            age = "18–24 (Young Adult)",
            height = "5'11\"–6'2\" (179–188 cm)",
            bodySize = "L / Relaxed"
        )
        assertEquals("Men's", profile.gender)
        assertEquals("18–24 (Young Adult)", profile.age)
        assertEquals("5'11\"–6'2\" (179–188 cm)", profile.height)
        assertEquals("L / Relaxed", profile.bodySize)
        val board = StyleBoard(name = "Baggy Streetwear", aesthetic = "Streetwear", itemIds = listOf("look_1"))
        assertEquals(listOf("look_1"), board.itemIds)
    }

    @Test
    fun startupSplashIsNonSkippableAndDisplaysClastadBranding() {
        composeRule.setContent {
            LithfitTheme {
                StartupSplashScreen(onFinished = {})
            }
        }
        composeRule.onNodeWithTag("skip_splash_button").assertDoesNotExist()
        composeRule.onNodeWithText("LITHFIT").assertIsDisplayed()
        composeRule.onNodeWithText("From CLASTAD").assertIsDisplayed()
    }

    @Test
    fun wardrobeItemDefaultsArePrivateAndGuideHasSixSteps() {
        val item = WardrobeItem(name = "Charcoal Wool Coat", category = GarmentCategory.JACKETS.name, color = "Charcoal")
        assertTrue(item.privateItem)
        assertEquals(GarmentCategory.JACKETS, item.categoryEnum())
        assertEquals(6, lithfitGuideSteps.size)
    }

    @Test
    fun stylePersonaLookbookCardRendersCleanly() {
        composeRule.setContent {
            LithfitTheme {
                StylePersonaLookbookCard(
                    config = AvatarConfig(),
                    hasPersonalPhoto = false,
                    personalPhotoPath = null
                )
            }
        }
        composeRule.onNodeWithText("Streetwear Editorial Model").assertIsDisplayed()
    }

    @Test
    fun onboardingGuideShowsSkipAndNextControls() {
        composeRule.setContent {
            LithfitTheme {
                OnboardingGuideScreen(onFinish = {}, onSkip = {})
            }
        }
        composeRule.onNodeWithTag("guide_skip_button").assertIsDisplayed()
        composeRule.onNodeWithTag("guide_next_button").assertIsDisplayed()
        composeRule.onNodeWithText("Discovering Fashion").assertIsDisplayed()
    }

    @Test
    fun lithfitWordmarkAndAttributionRender() {
        composeRule.setContent {
            LithfitTheme {
                LithfitWordmark(showAttribution = true)
            }
        }
        composeRule.onNodeWithText("LITHFIT").assertIsDisplayed()
        composeRule.onNodeWithText("From CLASTAD").assertIsDisplayed()
    }

    @Test
    fun legalGateDisplaysRequiredPolicyCheckboxes() {
        composeRule.setContent {
            LithfitTheme {
                LegalAndOnboardingScreen(
                    userId = "test_uid",
                    initialDisplayName = "Alex",
                    initialEmail = "alex@example.com",
                    initialAvatarUrl = null,
                    onComplete = { _, _, _, _, _ -> },
                    onSignOut = {}
                )
            }
        }
        composeRule.onNodeWithTag("privacy_checkbox").assertIsDisplayed()
        composeRule.onNodeWithTag("terms_checkbox").assertIsDisplayed()
        composeRule.onNodeWithTag("guidelines_checkbox").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("accept_legal_continue_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()

        composeRule.onNodeWithTag("privacy_checkbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("terms_checkbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("guidelines_checkbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("accept_legal_continue_button")
            .performScrollTo()
            .assertIsEnabled()
    }

    @Test
    fun inlineChatGenerationCardRendersOneToHundredPercentProgressAndCompletedImage() {
        composeRule.setContent {
            LithfitTheme {
                InlineChatImageGenerationCard(
                    state = ImageGenUiState.Loading(
                        progressPct = 42,
                        stageMessage = "Researching Streetwear vibe & exact cuts...",
                        title = "Pink Baggy Cargos & Black Tee",
                        fitDetails = "Women's • Size S • Baggy / Oversized"
                    ),
                    onRetry = {},
                    onDismiss = {},
                    onSaveGallery = {},
                    onShare = {},
                    onSaveLook = {},
                    onExpandImage = {}
                )
            }
        }
        composeRule.onNodeWithTag("inline_image_generation_card").assertIsDisplayed()
        composeRule.onNodeWithTag("image_gen_progress_percent").assertIsDisplayed()
        composeRule.onNodeWithTag("image_gen_progress_bar").assertIsDisplayed()
        composeRule.onNodeWithText("42%").assertIsDisplayed()
    }

    @Test
    fun inlineChatGenerationSuccessRendersImageAndActions() {
        val dummyBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        composeRule.setContent {
            LithfitTheme {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    InlineChatImageGenerationCard(
                        state = ImageGenUiState.Success(
                            bitmap = dummyBitmap,
                            title = "Black Graphic Shirt & Pink Baggy Trousers",
                            promptSummary = "Studio portrait",
                            occasion = "Hangout",
                            vibe = "Streetwear",
                            usedPersonalPhoto = true,
                            fitDetails = "Unisex • Size M • Virtual Try-On Photo"
                        ),
                        onRetry = {},
                        onDismiss = {},
                        onSaveGallery = {},
                        onShare = {},
                        onSaveLook = {},
                        onExpandImage = {}
                    )
                }
            }
        }
        composeRule.onNodeWithTag("inline_image_generation_card").assertIsDisplayed()
        composeRule.onNodeWithTag("generated_outfit_image").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("save_preview_gallery_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("share_preview_image_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("save_preview_profile_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("100% Ready").performScrollTo().assertIsDisplayed()
    }
}
