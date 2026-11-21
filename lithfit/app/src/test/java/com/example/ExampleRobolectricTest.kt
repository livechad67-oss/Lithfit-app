package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.core.GarmentCropAspect
import com.example.core.ImageStorageHelper
import com.example.data.ai.LithfitAiService
import com.example.data.ai.StylistIntent
import com.example.data.model.AvatarConfig
import com.example.data.model.CalendarOutfitEntry
import com.example.data.model.GarmentCategory
import com.example.data.model.LegalDocuments
import com.example.data.model.LithfitConstants
import com.example.data.model.Outfit
import com.example.data.model.OutfitWearHistory
import com.example.data.model.SeedCatalogData
import com.example.data.model.StyleBoard
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.model.WardrobeItem
import com.example.data.repository.AuthRepository
import com.example.data.repository.LithfitRepository
import com.example.ui.LithfitTab
import com.example.ui.components.LithfitWordmark
import com.example.ui.components.QuickActionType
import com.example.ui.screens.BudgetTier
import com.example.ui.screens.LegalAndOnboardingScreen
import com.example.ui.screens.OnboardingGuideScreen
import com.example.ui.screens.StartupSplashScreen
import com.example.ui.screens.StylePersonaLookbookCard
import com.example.ui.screens.lithfitGuideSteps
import com.example.ui.theme.LithfitTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
        assertEquals(listOf("Home", "Discover", "AI", "Wardrobe", "Profile"), labels)
        assertFalse(labels.any { it.equals("Nearby", ignoreCase = true) })
    }

    @Test
    fun quickActionsExposeEightCompactShortcuts() {
        val types = QuickActionType.entries
        assertEquals(8, types.size)
        assertTrue(
            types.containsAll(
                listOf(
                    QuickActionType.AI_STYLIST,
                    QuickActionType.ADD_CLOTHING,
                    QuickActionType.OUTFIT_BUILDER,
                    QuickActionType.COLOUR_MATCHING,
                    QuickActionType.STYLE_ADVICE,
                    QuickActionType.WISHLIST,
                    QuickActionType.SEARCH,
                    QuickActionType.NOTIFICATIONS
                )
            )
        )
    }

    @Test
    fun aiIntentClassifierRoutesGeneralQaVsOutfitRequests() {
        assertEquals(
            StylistIntent.GENERAL_QUESTION,
            LithfitAiService.classifyIntent("Hey")
        )
        assertEquals(
            StylistIntent.GENERAL_QUESTION,
            LithfitAiService.classifyIntent("What is artificial intelligence?")
        )
        assertEquals(
            StylistIntent.GENERAL_QUESTION,
            LithfitAiService.classifyIntent("What shoes go with baggy cargo trousers?")
        )
        assertEquals(
            StylistIntent.OUTFIT_GENERATION,
            LithfitAiService.classifyIntent("Create an outfit using my wardrobe.")
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
    fun localAuthSignUpSignInAndResetWorkViaSharedPreferences() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lithfit_local_auth", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("lithfit_owner_notifications", Context.MODE_PRIVATE).edit().clear().apply()
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
    fun ownerRegistrationAlertsAreStrictlyGatedToOwnerEmail() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lithfit_local_auth", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("lithfit_owner_notifications", Context.MODE_PRIVATE).edit().clear().apply()
        val repo = AuthRepository()
        repo.resetInMemoryForTesting()

        val reg = repo.registerWithUsernamePassword(
            context = context,
            displayName = "Taylor",
            rawUsername = "taylor_styles",
            password = "password123",
            email = "taylor@example.com"
        )
        assertTrue(reg.isSuccess)
        assertEquals("livechad67@gmail.com", LithfitConstants.OWNER_NOTIFY_EMAIL)
        assertTrue(repo.getOwnerRegistrationNotifications(context, "taylor@example.com").isEmpty())
        assertTrue(repo.getOwnerRegistrationNotifications(context, null).isEmpty())

        val ownerAlerts = repo.getOwnerRegistrationNotifications(context, LithfitConstants.OWNER_NOTIFY_EMAIL)
        assertEquals(1, ownerAlerts.size)
        assertTrue(ownerAlerts.first().message.contains("@taylor_styles"))
    }

    @Test
    fun wardrobeInsightsComputesRealMetricsAndZeroEmptyState() {
        val empty = LithfitRepository.computeWardrobeInsights(
            wardrobe = emptyList(),
            outfits = emptyList(),
            wearHistory = emptyList(),
            calendarEntries = emptyList()
        )
        assertEquals(0, empty.totalItems)
        assertEquals(0, empty.savedOutfitsCount)
        assertEquals(0, empty.totalWearsLogged)
        assertTrue(empty.mostUsedItems.isEmpty())

        val tee = WardrobeItem(id = "w1", name = "Cream Heavy Tee", category = GarmentCategory.TOPS.name, color = "Cream", favorite = true)
        val cargos = WardrobeItem(id = "w2", name = "Charcoal Cargos", category = GarmentCategory.TROUSERS.name, color = "Charcoal", favorite = false)
        val boots = WardrobeItem(id = "w3", name = "Black Boots", category = GarmentCategory.SHOES.name, color = "Black", favorite = false)
        val outfit = Outfit(id = "o1", name = "City Layers", itemNames = listOf("Cream Heavy Tee", "Charcoal Cargos"), favorite = true)
        val history = listOf(
            OutfitWearHistory(id = "h1", outfitId = "o1", outfitName = "City Layers", itemIds = listOf("w1", "w2"), wornDateKey = "2026-11-20"),
            OutfitWearHistory(id = "h2", outfitId = "o1", outfitName = "City Layers", itemIds = listOf("w1"), wornDateKey = "2026-11-21")
        )
        val calendar = listOf(CalendarOutfitEntry(id = "c1", dateKey = "2026-11-22", outfitId = "o1", outfitName = "City Layers"))

        val summary = LithfitRepository.computeWardrobeInsights(listOf(tee, cargos, boots), listOf(outfit), history, calendar)
        assertEquals(3, summary.totalItems)
        assertEquals(1, summary.favoriteItemsCount)
        assertEquals(1, summary.savedOutfitsCount)
        assertEquals(1, summary.favoriteOutfitsCount)
        assertEquals(2, summary.totalWearsLogged)
        assertEquals(1, summary.calendarPlannedCount)
        assertEquals("Cream Heavy Tee" to 2, summary.mostUsedItems.first())
        assertEquals(listOf("Black Boots"), summary.leastUsedItems)
    }

    @Test
    fun userProfileSupportsNotificationTogglesAccessibilityAndStyleTags() {
        val profile = UserProfile(
            personalStyleTags = listOf("Warm Neutrals", "Boxy Layers"),
            largeReadableText = true,
            reducedMotion = true,
            notifOutfitRecs = true,
            notifDailyReminders = false,
            notifWardrobeReminders = true,
            notifSecurityAlerts = true,
            notifAppUpdates = false,
            notifProductUpdates = true
        )
        assertEquals(listOf("Warm Neutrals", "Boxy Layers"), profile.personalStyleTags)
        assertTrue(profile.largeReadableText)
        assertTrue(profile.reducedMotion)
        assertTrue(profile.notifOutfitRecs)
        assertFalse(profile.notifDailyReminders)
        assertTrue(profile.notifWardrobeReminders)
        assertTrue(profile.notifSecurityAlerts)
        assertFalse(profile.notifAppUpdates)
        assertTrue(profile.notifProductUpdates)
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
        composeRule.onNodeWithTag("splash_progress_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("splash_clastad_footer").assertIsDisplayed()
        composeRule.onNodeWithTag("splash_lithfit_logo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("LITHFIT").assertIsDisplayed()
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
    fun legalGateDisplaysRequiredPolicyCheckboxesAndBodyFields() {
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
    fun garmentCropBitmapProducesExpectedAspectAndBounds() {
        val src = Bitmap.createBitmap(200, 120, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.DKGRAY)
        }
        val croppedSquare = ImageStorageHelper.cropBitmap(
            source = src,
            zoom = 1.5f,
            offsetXFrac = 0.2f,
            offsetYFrac = -0.2f,
            aspectRatio = GarmentCropAspect.SQUARE.ratio
        )
        assertEquals(croppedSquare.width, croppedSquare.height)
        assertTrue(croppedSquare.width in 40..120)

        val croppedPortrait = ImageStorageHelper.cropBitmap(
            source = src,
            zoom = 1f,
            offsetXFrac = 0f,
            offsetYFrac = 0f,
            aspectRatio = GarmentCropAspect.PORTRAIT.ratio
        )
        assertTrue(croppedPortrait.height > croppedPortrait.width)
    }

    @Test
    fun removeBackgroundSeparatesSubjectOntoWarmCreamAndFallsBackOnFlatImage() = runBlocking {
        // 1. Garment photo with white studio border and dark charcoal jacket center
        val studioPhoto = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.WHITE)
            for (y in 20 until 60) {
                for (x in 20 until 60) {
                    setPixel(x, y, AndroidColor.rgb(26, 26, 28))
                }
            }
        }
        val cutout = ImageStorageHelper.removeBackgroundFromGarmentBitmap(studioPhoto, tolerance = 50)
        assertFalse(cutout.usedFallback)
        // Corner pixels become Warm Cream (#F4F0E8), while garment center stays charcoal dark
        assertEquals(AndroidColor.rgb(0xF4, 0xF0, 0xE8), cutout.studioBitmap.getPixel(2, 2))
        val centerPixel = cutout.studioBitmap.getPixel(40, 40)
        assertNotEquals(AndroidColor.rgb(0xF4, 0xF0, 0xE8), centerPixel)

        // 2. Completely flat solid black image should fall back gracefully without erasing the subject
        val flatSolid = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.BLACK)
        }
        val fallback = ImageStorageHelper.removeBackgroundFromGarmentBitmap(flatSolid, tolerance = 50)
        assertTrue(fallback.usedFallback)
        assertEquals(AndroidColor.BLACK, fallback.studioBitmap.getPixel(20, 20))
    }

    @Test
    fun repositoryPersistsMultipleAiConversationsSwitchAndDelete() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lithfit_local_auth", Context.MODE_PRIVATE).edit().clear().apply()
        val authRepo = AuthRepository()
        authRepo.resetInMemoryForTesting()
        val reg = authRepo.registerWithUsernamePassword(context, "Morgan", "morgan_chat", "secret123")
        assertTrue(reg.isSuccess)
        val uid = reg.getOrThrow().uid

        val repo = LithfitRepository(context)

        repo.ensureConversation("chat_a", "Streetwear Fit", "Campus", "Streetwear")
        repo.appendAiMessage("chat_a", "USER", "Build me a black hoodie streetwear look")
        repo.appendAiMessage("chat_a", "ASSISTANT", "Pair an oversized black hoodie with charcoal cargos.")

        repo.ensureConversation("chat_b", "Tech Q&A", "Casual", "Minimalist")
        repo.appendAiMessage("chat_b", "USER", "What is artificial intelligence?")

        val convs = repo.observeAiConversations(uid).first()
        assertTrue(convs.any { it.id == "chat_a" })
        assertTrue(convs.any { it.id == "chat_b" })

        val msgsA = repo.observeAiMessages(uid, "chat_a").first()
        val msgsB = repo.observeAiMessages(uid, "chat_b").first()
        assertEquals(2, msgsA.size)
        assertEquals(1, msgsB.size)

        repo.deleteAiConversation("chat_a")
        val remaining = repo.observeAiConversations(uid).first()
        assertFalse(remaining.any { it.id == "chat_a" })
        assertTrue(remaining.any { it.id == "chat_b" })
    }
}
