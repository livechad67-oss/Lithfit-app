package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.AppGuidePreferences
import com.example.core.ImageStorageHelper
import com.example.data.ai.CachedStylistResponse
import com.example.data.ai.LithfitAiService
import com.example.data.ai.RecognizedGarmentAttributes
import com.example.data.ai.StructuredOutfitSuggestion
import com.example.data.ai.StylistIntent
import com.example.data.model.AiConversation
import com.example.data.model.AiMessage
import com.example.data.model.AppNotification
import com.example.data.model.AvatarConfig
import com.example.data.model.CalendarOutfitEntry
import com.example.data.model.CatalogueProduct
import com.example.data.model.CommunityPost
import com.example.data.model.DiscoveryPost
import com.example.data.model.LegalAcceptance
import com.example.data.model.LithfitConstants
import com.example.data.model.Outfit
import com.example.data.model.OutfitCollection
import com.example.data.model.OutfitWearHistory
import com.example.data.model.OwnerRegistrationNotification
import com.example.data.model.RecentlyViewedEntry
import com.example.data.model.SavedItem
import com.example.data.model.Seller
import com.example.data.model.StyleBoard
import com.example.data.model.ThemeMode
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WardrobeInsightsSummary
import com.example.data.model.WardrobeItem
import com.example.data.remote.WeatherService
import com.example.data.remote.WeatherSnapshot
import com.example.data.repository.AuthRepository
import com.example.data.repository.LithfitRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface GarmentRecognitionState {
    data object Idle : GarmentRecognitionState
    data object Analyzing : GarmentRecognitionState
    data class Ready(
        val attributes: RecognizedGarmentAttributes,
        val localImagePath: String,
        val thumbnailBase64: String
    ) : GarmentRecognitionState
    data class Error(val message: String, val localImagePath: String = "", val thumbnailBase64: String = "") : GarmentRecognitionState
}

data class LastStylistPromptRequest(
    val prompt: String,
    val occasion: String,
    val vibe: String
)

class LithfitViewModel(
    private val repository: LithfitRepository,
    private val aiService: LithfitAiService,
    private val weatherService: WeatherService,
    val currentUserId: String,
    private val appContext: Context
) : ViewModel() {

    private val authRepo = AuthRepository()

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreException(error)
            emit(UiState.Error(fsErr.message ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val legalAcceptance: StateFlow<UiState<LegalAcceptance?>> = repository.observeLegalAcceptance(currentUserId)
        .map<LegalAcceptance?, UiState<LegalAcceptance?>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreException(error)
            emit(UiState.Error(fsErr.message ?: "Failed to check legal acceptance"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val wardrobeItems: StateFlow<UiState<List<WardrobeItem>>> = repository.observeWardrobeItems(currentUserId)
        .map<List<WardrobeItem>, UiState<List<WardrobeItem>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreException(error)
            emit(UiState.Error(fsErr.message ?: "Failed to load wardrobe"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val outfitCollections: StateFlow<List<OutfitCollection>> = repository.observeOutfitCollections(currentUserId)
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val outfits: StateFlow<UiState<List<Outfit>>> = repository.observeOutfits(currentUserId)
        .map<List<Outfit>, UiState<List<Outfit>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreException(error)
            emit(UiState.Error(fsErr.message ?: "Failed to load outfits"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val savedItems: StateFlow<List<SavedItem>> = repository.observeSavedItems(currentUserId)
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val calendarEntries: StateFlow<List<CalendarOutfitEntry>> = repository.observeCalendarEntries(currentUserId)
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val wearHistory: StateFlow<List<OutfitWearHistory>> = repository.observeWearHistory(currentUserId)
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val appNotifications: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId)
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = appNotifications
        .map { list -> list.count { !it.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val wardrobeInsights: StateFlow<WardrobeInsightsSummary> = combine(
        wardrobeItems,
        outfits,
        wearHistory,
        calendarEntries
    ) { wState, oState, history, calendar ->
        val wList = (wState as? UiState.Success)?.data.orEmpty()
        val oList = (oState as? UiState.Success)?.data.orEmpty()
        LithfitRepository.computeWardrobeInsights(wList, oList, history, calendar)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), WardrobeInsightsSummary())

    val ownerNotifications: StateFlow<List<OwnerRegistrationNotification>> =
        repository.observeOwnerRegistrationNotifications(authRepo.currentUser(appContext)?.email)
            .catch { error ->
                unwrapFirestoreException(error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val discoveryPosts: StateFlow<List<DiscoveryPost>> = repository.observeDiscoveryPosts()
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val sellers: StateFlow<List<Seller>> = repository.observeSellers()
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val catalogueProducts: StateFlow<List<CatalogueProduct>> = repository.observeCatalogueProducts()
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeConversationId = MutableStateFlow("main_stylist")
    val activeConversationId: StateFlow<String> = _activeConversationId.asStateFlow()

    val aiConversations: StateFlow<List<AiConversation>> = repository.observeAiConversations(currentUserId)
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val aiMessages: StateFlow<List<AiMessage>> = _activeConversationId
        .flatMapLatest { convId -> repository.observeAiMessages(currentUserId, convId) }
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val styleBoards: StateFlow<List<StyleBoard>> = repository.observeStyleBoards(currentUserId)
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val recentlyViewed: StateFlow<List<RecentlyViewedEntry>> = repository.observeRecentlyViewed(currentUserId)
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val communityPosts: StateFlow<List<CommunityPost>> = repository.observeCommunityPosts()
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val followedAuthors: StateFlow<Set<String>> = repository.observeFollowedAuthors(currentUserId)
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val blockedAuthors: StateFlow<Set<String>> = repository.observeBlockedAuthors(currentUserId)
        .catch { error ->
            unwrapFirestoreException(error)
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val hasSeenIntroGuide: StateFlow<Boolean> = AppGuidePreferences.hasSeenIntroGuideFlow(appContext)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val dismissedTips: StateFlow<Set<String>> = AppGuidePreferences.dismissedTipsFlow(appContext)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    private val _personalPhotoRevision = MutableStateFlow(
        if (ImageStorageHelper.hasPersonalFullBodyPhoto(appContext)) 1 else 0
    )
    val personalPhotoRevision: StateFlow<Int> = _personalPhotoRevision.asStateFlow()

    private val _weatherState = MutableStateFlow<UiState<WeatherSnapshot?>>(UiState.Success(null))
    val weatherState: StateFlow<UiState<WeatherSnapshot?>> = _weatherState.asStateFlow()

    private val _aiStreamingText = MutableStateFlow<String?>(null)
    val aiStreamingText: StateFlow<String?> = _aiStreamingText.asStateFlow()

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()

    private val _aiIntent = MutableStateFlow(StylistIntent.OUTFIT_GENERATION)
    val aiIntent: StateFlow<StylistIntent> = _aiIntent.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _lastStylistRequest = MutableStateFlow<LastStylistPromptRequest?>(null)
    val lastStylistRequest: StateFlow<LastStylistPromptRequest?> = _lastStylistRequest.asStateFlow()

    private val _latestStructuredOutfit = MutableStateFlow<StructuredOutfitSuggestion?>(null)
    val latestStructuredOutfit: StateFlow<StructuredOutfitSuggestion?> = _latestStructuredOutfit.asStateFlow()

    private val _garmentRecognition = MutableStateFlow<GarmentRecognitionState>(GarmentRecognitionState.Idle)
    val garmentRecognition: StateFlow<GarmentRecognitionState> = _garmentRecognition.asStateFlow()

    private val _visualSearchQuery = MutableStateFlow<String?>(null)
    val visualSearchQuery: StateFlow<String?> = _visualSearchQuery.asStateFlow()

    private val _visualSearchBusy = MutableStateFlow(false)
    val visualSearchBusy: StateFlow<Boolean> = _visualSearchBusy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearAiError() {
        _aiError.value = null
    }

    fun resetGarmentRecognition() {
        _garmentRecognition.value = GarmentRecognitionState.Idle
    }

    fun markIntroGuideSeen() {
        viewModelScope.launch {
            AppGuidePreferences.setIntroGuideSeen(appContext, true)
        }
    }

    fun dismissTip(tipKey: String) {
        viewModelScope.launch {
            AppGuidePreferences.dismissTip(appContext, tipKey)
        }
    }

    fun resetGuideAndTips() {
        viewModelScope.launch {
            AppGuidePreferences.resetAllGuides(appContext)
            _statusMessage.value = "App guide and styling tips reset."
        }
    }

    fun saveLegalAndOnboardingProfile(
        cameraConsent: Boolean,
        photoConsent: Boolean,
        locationConsent: Boolean,
        notificationsConsent: Boolean,
        profile: UserProfile,
        onComplete: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val legalRes = repository.recordLegalAcceptance(
                cameraConsent = cameraConsent,
                photoConsent = photoConsent,
                locationConsent = locationConsent,
                notificationsConsent = notificationsConsent
            )
            if (legalRes.isFailure) {
                onComplete(legalRes)
                return@launch
            }
            val profRes = repository.saveUserProfile(profile)
            if (profRes.isSuccess) {
                authRepo.currentUser(appContext)?.let { activeUser ->
                    if (activeUser.googleSignedIn) {
                        repository.recordOwnerRegistrationToFirestore(activeUser, authMethod = "GOOGLE")
                    }
                }
                if (notificationsConsent) {
                    repository.addAppNotification(
                        category = "APP_UPDATE",
                        title = "Welcome to LITHFIT",
                        body = "Your personal digital wardrobe and style preferences are ready."
                    )
                }
            }
            onComplete(profRes)
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            val res = repository.saveUserProfile(profile)
            _statusMessage.value = if (res.isSuccess) "Profile updated." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveProfilePhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            val current = (userProfile.value as? UiState.Success)?.data ?: return@launch
            val oldPath = current.profilePhotoPath
            val res = ImageStorageHelper.saveProfilePhoto(context, uri, currentUserId)
            if (res.isSuccess) {
                val path = res.getOrThrow()
                val updateRes = repository.saveUserProfile(
                    current.copy(profilePhotoPath = path, hasPersonalPhoto = true)
                )
                if (updateRes.isSuccess) {
                    if (!oldPath.isNullOrBlank() && oldPath != path) {
                        ImageStorageHelper.deleteProfilePhoto(oldPath)
                    }
                    _statusMessage.value = "Profile photo updated privately."
                } else {
                    _statusMessage.value = updateRes.exceptionOrNull()?.localizedMessage ?: "Failed to update profile."
                }
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not save profile photo."
            }
        }
    }

    fun deleteProfilePhoto() {
        viewModelScope.launch {
            val current = (userProfile.value as? UiState.Success)?.data ?: return@launch
            ImageStorageHelper.deleteProfilePhoto(current.profilePhotoPath)
            val res = repository.saveUserProfile(current.copy(profilePhotoPath = null))
            _statusMessage.value = if (res.isSuccess) "Profile photo removed." else "Failed to remove profile photo."
        }
    }

    fun saveAvatarConfig(avatarConfig: AvatarConfig) {
        viewModelScope.launch {
            val res = repository.saveAvatarConfig(avatarConfig)
            _statusMessage.value = if (res.isSuccess) "Avatar saved to your studio profile." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        val current = (userProfile.value as? UiState.Success)?.data ?: return
        updateProfile(current.copy(themeMode = mode.name))
    }

    fun savePersonalFullBodyPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            val res = ImageStorageHelper.savePersonalFullBodyPhoto(context, uri)
            if (res.isSuccess) {
                repository.setPersonalPhotoFlag(true)
                _personalPhotoRevision.value = _personalPhotoRevision.value + 1
                _statusMessage.value = "Private reference photo saved on-device."
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save reference photo."
            }
        }
    }

    fun deletePersonalFullBodyPhoto(context: Context) {
        viewModelScope.launch {
            ImageStorageHelper.deletePersonalFullBodyPhoto(context)
            repository.setPersonalPhotoFlag(false)
            _personalPhotoRevision.value = 0
            _statusMessage.value = "Private reference photo deleted."
        }
    }

    fun analyzeWardrobeUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _garmentRecognition.value = GarmentRecognitionState.Analyzing
            val processed = ImageStorageHelper.processUriForWardrobe(context, uri)
            if (processed.isFailure) {
                _garmentRecognition.value = GarmentRecognitionState.Error(
                    processed.exceptionOrNull()?.localizedMessage ?: "Failed to read selected photo."
                )
                return@launch
            }
            val bundle = processed.getOrThrow()
            val aiResult = aiService.recognizeGarmentFromBitmap(bundle.bitmapForAi)
            val attrs = aiResult.getOrElse {
                LithfitAiService.fallbackRecognizeFromBitmap(bundle.bitmapForAi)
            }
            _garmentRecognition.value = GarmentRecognitionState.Ready(
                attributes = attrs,
                localImagePath = bundle.localFilePath,
                thumbnailBase64 = bundle.thumbnailBase64
            )
        }
    }

    fun analyzeWardrobeBitmap(context: Context, bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            _garmentRecognition.value = GarmentRecognitionState.Analyzing
            val processed = ImageStorageHelper.processBitmapForWardrobe(context, bitmap)
            if (processed.isFailure) {
                _garmentRecognition.value = GarmentRecognitionState.Error(
                    processed.exceptionOrNull()?.localizedMessage ?: "Failed to save edited garment image."
                )
                return@launch
            }
            val bundle = processed.getOrThrow()
            val aiResult = aiService.recognizeGarmentFromBitmap(bundle.bitmapForAi)
            val attrs = aiResult.getOrElse {
                LithfitAiService.fallbackRecognizeFromBitmap(bundle.bitmapForAi)
            }
            _garmentRecognition.value = GarmentRecognitionState.Ready(
                attributes = attrs,
                localImagePath = bundle.localFilePath,
                thumbnailBase64 = bundle.thumbnailBase64
            )
        }
    }

    fun importImageFromUrl(context: Context, url: String) {
        viewModelScope.launch {
            _garmentRecognition.value = GarmentRecognitionState.Analyzing
            val processed = ImageStorageHelper.processRemoteUrlForWardrobe(context, url)
            if (processed.isFailure) {
                _garmentRecognition.value = GarmentRecognitionState.Error(
                    processed.exceptionOrNull()?.localizedMessage ?: "Could not import image from URL."
                )
                return@launch
            }
            val bundle = processed.getOrThrow()
            val aiResult = aiService.recognizeGarmentFromBitmap(bundle.bitmapForAi)
            val attrs = aiResult.getOrElse {
                LithfitAiService.fallbackRecognizeFromBitmap(bundle.bitmapForAi)
            }
            _garmentRecognition.value = GarmentRecognitionState.Ready(
                attributes = attrs,
                localImagePath = bundle.localFilePath,
                thumbnailBase64 = bundle.thumbnailBase64
            )
        }
    }

    fun saveWardrobeItem(item: WardrobeItem, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.saveWardrobeItem(item)
            if (res.isSuccess) {
                _statusMessage.value = "Saved \"${item.name}\" to your wardrobe."
                onDone(true)
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not save garment."
                onDone(false)
            }
        }
    }

    fun toggleWardrobeFavorite(item: WardrobeItem) {
        viewModelScope.launch {
            repository.saveWardrobeItem(item.copy(favorite = !item.favorite))
        }
    }

    fun deleteWardrobeItem(itemId: String) {
        viewModelScope.launch {
            val res = repository.deleteWardrobeItem(itemId)
            _statusMessage.value = if (res.isSuccess) "Item removed from wardrobe." else "Failed to remove item."
        }
    }

    fun saveOutfit(outfit: Outfit, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.saveOutfit(outfit)
            if (res.isSuccess) {
                _statusMessage.value = "Saved outfit \"${outfit.name}\"."
                onDone(true)
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not save outfit."
                onDone(false)
            }
        }
    }

    fun renameOutfit(outfitId: String, newName: String) {
        viewModelScope.launch {
            val res = repository.renameOutfit(outfitId, newName)
            _statusMessage.value = if (res.isSuccess) "Outfit renamed." else res.exceptionOrNull()?.localizedMessage ?: "Rename failed."
        }
    }

    fun duplicateOutfit(outfit: Outfit) {
        viewModelScope.launch {
            val copy = outfit.copy(id = "", name = "${outfit.name} (Copy)", createdAtMillis = 0L, updatedAtMillis = 0L)
            val res = repository.saveOutfit(copy)
            _statusMessage.value = if (res.isSuccess) "Duplicated outfit." else "Failed to duplicate outfit."
        }
    }

    fun setOutfitFeedback(outfitId: String, feedbackStatus: String) {
        viewModelScope.launch {
            val res = repository.setOutfitFeedback(outfitId, feedbackStatus)
            if (res.isSuccess) {
                _statusMessage.value = if (feedbackStatus == "LIKED") {
                    "Noted — LITHFIT AI will lean into looks like this."
                } else {
                    "Got it — LITHFIT AI will steer away from this style."
                }
            }
        }
    }

    fun deleteOutfit(outfitId: String) {
        viewModelScope.launch {
            val res = repository.deleteOutfit(outfitId)
            _statusMessage.value = if (res.isSuccess) "Outfit deleted." else "Failed to delete outfit."
        }
    }

    fun assignOutfitToDate(dateKey: String, outfit: Outfit, notes: String? = null) {
        viewModelScope.launch {
            val res = repository.assignOutfitToCalendar(dateKey, outfit, notes)
            _statusMessage.value = if (res.isSuccess) {
                "Scheduled \"${outfit.name}\" for $dateKey."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not schedule outfit."
            }
        }
    }

    fun removeCalendarEntry(entryId: String) {
        viewModelScope.launch {
            val res = repository.removeCalendarEntry(entryId)
            if (res.isSuccess) _statusMessage.value = "Removed scheduled outfit."
        }
    }

    fun markCalendarEntryWorn(entryId: String) {
        viewModelScope.launch {
            val res = repository.markCalendarEntryWorn(entryId)
            if (res.isSuccess) _statusMessage.value = "Updated wear log for scheduled outfit."
        }
    }

    fun logOutfitWorn(outfit: Outfit, dateKey: String, notes: String? = null) {
        viewModelScope.launch {
            val items = (wardrobeItems.value as? UiState.Success)?.data.orEmpty()
            val res = repository.logOutfitWorn(outfit, items, dateKey, notes)
            _statusMessage.value = if (res.isSuccess) {
                "Logged \"${outfit.name}\" as worn on $dateKey."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to log outfit wear."
            }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notifId)
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
            _statusMessage.value = "Notifications cleared."
        }
    }

    fun markOwnerNotificationRead(notifId: String) {
        viewModelScope.launch {
            val email = authRepo.currentUser(appContext)?.email
            repository.markOwnerNotificationRead(email, notifId)
        }
    }

    fun createCollection(name: String, description: String? = null) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            val res = repository.createOutfitCollection(name, description)
            _statusMessage.value = if (res.isSuccess) "Collection \"$name\" created." else "Failed to create collection."
        }
    }

    fun startNewAiConversation(occasion: String = LithfitConstants.OCCASIONS.first(), vibe: String = LithfitConstants.VIBES.first()) {
        val nextId = "chat_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        _aiStreamingText.value = null
        _aiError.value = null
        _latestStructuredOutfit.value = null
        _activeConversationId.value = nextId
        viewModelScope.launch {
            repository.ensureConversation(nextId, "New Chat", occasion, vibe)
        }
    }

    fun selectAiConversation(conversationId: String) {
        if (conversationId.isBlank() || conversationId == _activeConversationId.value) return
        _aiStreamingText.value = null
        _aiError.value = null
        _latestStructuredOutfit.value = null
        _activeConversationId.value = conversationId
    }

    fun deleteAiConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteAiConversation(conversationId)
            if (_activeConversationId.value == conversationId) {
                val remaining = aiConversations.value.firstOrNull { it.id != conversationId }
                if (remaining != null) {
                    _activeConversationId.value = remaining.id
                } else {
                    startNewAiConversation()
                }
            }
            _statusMessage.value = "Conversation deleted."
        }
    }

    fun retryLastStylistPrompt(forceRegenerate: Boolean = true) {
        val last = _lastStylistRequest.value ?: return
        sendStylistPrompt(last.prompt, last.occasion, last.vibe, forceRegenerate = forceRegenerate)
    }

    fun sendStylistPrompt(
        prompt: String,
        occasion: String,
        vibe: String,
        forceRegenerate: Boolean = false
    ) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() || _aiBusy.value) return
        val convId = _activeConversationId.value
        val intent = LithfitAiService.classifyIntent(trimmed)
        _lastStylistRequest.value = LastStylistPromptRequest(trimmed, occasion, vibe)
        _aiIntent.value = intent

        viewModelScope.launch {
            _aiBusy.value = true
            _aiError.value = null
            _aiStreamingText.value = ""

            val profile = (userProfile.value as? UiState.Success)?.data
            val items = (wardrobeItems.value as? UiState.Success)?.data.orEmpty()
            val savedOutfitList = (outfits.value as? UiState.Success)?.data.orEmpty()
            val weather = (_weatherState.value as? UiState.Success)?.data
            val catalogue = catalogueProducts.value
            val history = aiMessages.value.map { it.role to it.text }

            val cacheKey = aiService.buildCacheKey(
                intent = intent,
                prompt = trimmed,
                occasion = occasion,
                vibe = vibe,
                wardrobeCount = items.size,
                weatherLabel = weather?.let { "${it.locationName}_${it.temperatureC.toInt()}" }
            )

            repository.ensureConversation(convId, "New Chat", occasion, vibe)
            repository.appendAiMessage(convId, "USER", trimmed)

            val cached = if (!forceRegenerate) aiService.getCachedResponse(cacheKey) else null
            if (cached != null) {
                if (cached.structuredOutfit != null) {
                    _latestStructuredOutfit.value = cached.structuredOutfit
                }
                val recNames = cached.structuredOutfit?.let {
                    listOf(it.topPiece, it.bottomPiece, it.footwearPiece, it.jacketPiece)
                        .filter { p -> p.isNotBlank() && !p.equals("None", ignoreCase = true) }
                }.orEmpty()
                repository.appendAiMessage(
                    conversationId = convId,
                    role = "ASSISTANT",
                    text = cached.visibleText,
                    recommendedItemNames = recNames,
                    stylingReason = cached.structuredOutfit?.stylingRationale
                )
                _aiStreamingText.value = null
                _aiBusy.value = false
                return@launch
            }

            val accumulated = StringBuilder()
            try {
                aiService.streamStylistResponse(
                    intent = intent,
                    userPrompt = trimmed,
                    occasion = occasion,
                    vibe = vibe,
                    profile = profile,
                    wardrobe = items,
                    weather = weather,
                    catalogue = catalogue,
                    conversationHistory = history,
                    savedOutfits = savedOutfitList
                ).collect { rawSoFar ->
                    accumulated.clear()
                    accumulated.append(rawSoFar)
                    _aiStreamingText.value = LithfitAiService.stripInlineOutfitMarker(rawSoFar)
                }

                val finalRaw = accumulated.toString().trim()
                val visibleText = LithfitAiService.stripInlineOutfitMarker(finalRaw).trim()
                if (visibleText.isNotEmpty()) {
                    val structured = if (intent == StylistIntent.OUTFIT_GENERATION) {
                        aiService.resolveStructuredOutfit(
                            rawResponseText = finalRaw,
                            occasion = occasion,
                            vibe = vibe,
                            wardrobe = items
                        )
                    } else {
                        null
                    }

                    if (structured != null) {
                        _latestStructuredOutfit.value = structured
                    }
                    aiService.putCachedResponse(
                        cacheKey,
                        CachedStylistResponse(
                            visibleText = visibleText,
                            structuredOutfit = structured,
                            intent = intent
                        )
                    )
                    val recNames = structured?.let {
                        listOf(it.topPiece, it.bottomPiece, it.footwearPiece, it.jacketPiece)
                            .filter { piece -> piece.isNotBlank() && !piece.equals("None", ignoreCase = true) }
                    }.orEmpty()

                    repository.appendAiMessage(
                        conversationId = convId,
                        role = "ASSISTANT",
                        text = visibleText,
                        recommendedItemNames = recNames,
                        stylingReason = structured?.stylingRationale
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "LITHFIT AI request failed", e)
                _aiError.value = e.localizedMessage ?: "LITHFIT AI is temporarily unavailable."
            } finally {
                _aiStreamingText.value = null
                _aiBusy.value = false
            }
        }
    }

    fun analyzeAttachedStylingPhoto(
        context: Context,
        uri: Uri,
        prompt: String,
        occasion: String,
        vibe: String
    ) {
        if (_aiBusy.value) return
        viewModelScope.launch {
            _aiBusy.value = true
            _aiError.value = null
            _aiIntent.value = StylistIntent.OUTFIT_GENERATION
            _aiStreamingText.value = "Inspecting your uploaded piece..."

            val bmp = ImageStorageHelper.loadBitmapFromUri(context, uri)
            if (bmp == null) {
                _aiStreamingText.value = null
                _aiBusy.value = false
                _aiError.value = "Could not open the selected image."
                return@launch
            }

            val convId = _activeConversationId.value
            val trimmedPrompt = prompt.trim().ifBlank { "How should I style this piece for $occasion?" }
            repository.ensureConversation(convId, "New Chat", occasion, vibe)
            repository.appendAiMessage(convId, "USER", "[Photo attached] $trimmedPrompt")

            val profile = (userProfile.value as? UiState.Success)?.data
            val items = (wardrobeItems.value as? UiState.Success)?.data.orEmpty()
            val accumulated = StringBuilder()
            try {
                aiService.streamPhotoStylingAdvice(
                    bitmap = bmp,
                    userPrompt = trimmedPrompt,
                    occasion = occasion,
                    vibe = vibe,
                    profile = profile,
                    wardrobe = items
                ).collect { rawSoFar ->
                    accumulated.clear()
                    accumulated.append(rawSoFar)
                    _aiStreamingText.value = LithfitAiService.stripInlineOutfitMarker(rawSoFar)
                }

                val finalRaw = accumulated.toString().trim()
                val visibleText = LithfitAiService.stripInlineOutfitMarker(finalRaw).trim()
                if (visibleText.isNotEmpty()) {
                    val structured = aiService.resolveStructuredOutfit(
                        rawResponseText = finalRaw,
                        occasion = occasion,
                        vibe = vibe,
                        wardrobe = items
                    )
                    if (structured != null) {
                        _latestStructuredOutfit.value = structured
                    }
                    val recNames = structured?.let {
                        listOf(it.topPiece, it.bottomPiece, it.footwearPiece, it.jacketPiece)
                            .filter { piece -> piece.isNotBlank() && !piece.equals("None", ignoreCase = true) }
                    }.orEmpty()
                    repository.appendAiMessage(
                        conversationId = convId,
                        role = "ASSISTANT",
                        text = visibleText,
                        recommendedItemNames = recNames,
                        stylingReason = structured?.stylingRationale
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photo styling analysis failed", e)
                _aiError.value = e.localizedMessage ?: "Unable to analyze this photo right now."
            } finally {
                _aiStreamingText.value = null
                _aiBusy.value = false
            }
        }
    }

    fun fetchWeatherForCity(city: String) {
        if (city.isBlank()) return
        viewModelScope.launch {
            _weatherState.value = UiState.Loading
            val weatherRes = weatherService.fetchWeatherForCity(city.trim())
            _weatherState.value = if (weatherRes.isSuccess) {
                UiState.Success(weatherRes.getOrThrow())
            } else {
                UiState.Error(
                    weatherRes.exceptionOrNull()?.localizedMessage ?: "Weather unavailable for \"$city\"."
                )
            }
        }
    }

    fun runVisualSearchFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _visualSearchBusy.value = true
            val bmp = ImageStorageHelper.loadBitmapFromUri(context, uri)
            if (bmp == null) {
                _statusMessage.value = "Could not open image for visual search."
                _visualSearchBusy.value = false
                return@launch
            }
            val res = aiService.recognizeGarmentFromBitmap(bmp)
            val attr = res.getOrElse {
                LithfitAiService.fallbackRecognizeFromBitmap(bmp)
            }
            _visualSearchQuery.value = "${attr.color} ${attr.garmentType}".trim()
            _statusMessage.value = "Visual match: ${attr.color} ${attr.style} ${attr.category.label.lowercase()}"
            _visualSearchBusy.value = false
        }
    }

    fun clearVisualSearch() {
        _visualSearchQuery.value = null
    }

    fun toggleSaveDiscoveryPost(post: DiscoveryPost) {
        viewModelScope.launch {
            val res = repository.toggleSaveItem(
                targetType = "DISCOVERY_POST",
                targetId = post.id,
                title = post.title,
                subtitle = post.authorName,
                category = post.category
            )
            res.onSuccess { saved ->
                _statusMessage.value = if (saved) "Saved \"${post.title}\"." else "Removed from saved."
            }
        }
    }

    fun toggleLikeDiscoveryPost(post: DiscoveryPost) {
        viewModelScope.launch {
            repository.toggleLikeSavedItem(
                targetType = "DISCOVERY_POST",
                targetId = post.id,
                title = post.title,
                subtitle = post.authorName,
                category = post.category
            )
        }
    }

    fun toggleSaveProduct(product: CatalogueProduct) {
        viewModelScope.launch {
            val res = repository.toggleSaveItem(
                targetType = "PRODUCT",
                targetId = product.id,
                title = product.name,
                subtitle = "${product.sellerName} • ${product.currency} ${product.price.toInt()}",
                category = product.style
            )
            res.onSuccess { saved ->
                _statusMessage.value = if (saved) "Saved \"${product.name}\"." else "Removed from saved."
            }
        }
    }

    fun submitReport(targetType: String, targetId: String, reason: String, details: String?) {
        viewModelScope.launch {
            val res = repository.submitReport(targetType, targetId, reason, details)
            _statusMessage.value = if (res.isSuccess) {
                "Report submitted to CLASTAD moderation. Thank you."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to submit report."
            }
        }
    }

    fun saveStyleBoard(board: StyleBoard) {
        viewModelScope.launch {
            val res = repository.saveStyleBoard(board)
            _statusMessage.value = if (res.isSuccess) {
                "Saved Style Board \"${board.name}\"."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not save Style Board."
            }
        }
    }

    fun deleteStyleBoard(boardId: String) {
        viewModelScope.launch {
            val res = repository.deleteStyleBoard(boardId)
            _statusMessage.value = if (res.isSuccess) "Style Board deleted." else "Failed to delete board."
        }
    }

    fun toggleItemInStyleBoard(boardId: String, itemId: String, itemTitle: String, coverImageKey: String? = null) {
        viewModelScope.launch {
            val res = repository.toggleItemInStyleBoard(boardId, itemId, coverImageKey)
            res.onSuccess { added ->
                _statusMessage.value = if (added) "Added \"$itemTitle\" to board." else "Removed \"$itemTitle\" from board."
            }
        }
    }

    fun recordDiscoveryView(post: DiscoveryPost) {
        viewModelScope.launch {
            repository.recordDiscoveryView(post)
        }
    }

    fun publishCommunityPostFromUri(
        context: Context,
        title: String,
        caption: String,
        aesthetic: String,
        photoUri: Uri?,
        colorPalette: List<String>
    ) {
        viewModelScope.launch {
            val media = if (photoUri != null) {
                ImageStorageHelper.processUriForCommunity(context, photoUri).getOrNull()
            } else null
            val res = repository.publishCommunityPost(
                title = title,
                caption = caption,
                aesthetic = aesthetic,
                imagePath = media?.localFilePath,
                thumbnailBase64 = media?.thumbnailBase64,
                colorPalette = colorPalette
            )
            _statusMessage.value = if (res.isSuccess) {
                "Shared your look to LITHFIT Community."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not publish look."
            }
        }
    }

    fun deleteCommunityPost(postId: String) {
        viewModelScope.launch {
            val res = repository.deleteCommunityPost(postId)
            if (res.isSuccess) _statusMessage.value = "Community post removed."
        }
    }

    fun toggleFollowAuthor(authorId: String, authorName: String) {
        viewModelScope.launch {
            val res = repository.toggleFollowAuthor(authorId)
            res.onSuccess { followed ->
                _statusMessage.value = if (followed) "Following $authorName." else "Unfollowed $authorName."
            }
        }
    }

    fun blockAuthor(authorId: String, authorName: String) {
        viewModelScope.launch {
            val res = repository.blockAuthor(authorId)
            if (res.isSuccess) {
                _statusMessage.value = "Blocked $authorName and hid their posts."
            }
        }
    }

    fun buildUserDataExportSummary(): String =
        repository.exportUserDataJson(currentUserId).getOrElse {
            val profile = (userProfile.value as? UiState.Success)?.data
            "LITHFIT Export for @${profile?.username ?: currentUserId} (by ${LithfitConstants.CLASTAD_WEBSITE_URL})"
        }

    private fun unwrapFirestoreException(error: Throwable): Throwable {
        if (error is CancellationException) throw error
        return error
    }

    companion object {
        private const val TAG = "LithfitViewModel"

        fun provideFactory(context: Context, userId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LithfitViewModel(
                        repository = LithfitRepository(context.applicationContext),
                        aiService = LithfitAiService(),
                        weatherService = WeatherService(),
                        currentUserId = userId,
                        appContext = context.applicationContext
                    ) as T
                }
            }
    }
}
