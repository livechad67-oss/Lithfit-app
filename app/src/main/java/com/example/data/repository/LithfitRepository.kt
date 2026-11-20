package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AiMessage
import com.example.data.model.AvatarConfig
import com.example.data.model.CatalogueProduct
import com.example.data.model.CommunityPost
import com.example.data.model.DiscoveryPost
import com.example.data.model.GeneratedPreview
import com.example.data.model.LegalAcceptance
import com.example.data.model.LegalDocuments
import com.example.data.model.Outfit
import com.example.data.model.OutfitCollection
import com.example.data.model.RecentlyViewedEntry
import com.example.data.model.SavedItem
import com.example.data.model.SeedCatalogData
import com.example.data.model.Seller
import com.example.data.model.StoreLocation
import com.example.data.model.StyleBoard
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.model.WardrobeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LithfitRepository(context: Context) {

    private val appContext = context.applicationContext

    private class UserStore(
        profile: UserProfile?,
        legal: LegalAcceptance?,
        wardrobe: List<WardrobeItem>,
        collections: List<OutfitCollection>,
        outfits: List<Outfit>,
        saved: List<SavedItem>,
        previews: List<GeneratedPreview>,
        aiMessages: List<AiMessage>,
        styleBoards: List<StyleBoard>,
        recentlyViewed: List<RecentlyViewedEntry>,
        followedAuthors: Set<String>,
        blockedAuthors: Set<String>
    ) {
        val profileFlow = MutableStateFlow(profile)
        val legalFlow = MutableStateFlow(legal)
        val wardrobeFlow = MutableStateFlow(wardrobe)
        val collectionsFlow = MutableStateFlow(collections)
        val outfitsFlow = MutableStateFlow(outfits)
        val savedFlow = MutableStateFlow(saved)
        val previewsFlow = MutableStateFlow(previews)
        val aiMessagesFlow = MutableStateFlow(aiMessages)
        val styleBoardsFlow = MutableStateFlow(styleBoards)
        val recentlyViewedFlow = MutableStateFlow(recentlyViewed)
        val followedAuthorsFlow = MutableStateFlow(followedAuthors)
        val blockedAuthorsFlow = MutableStateFlow(blockedAuthors)
    }

    private fun storeFor(userId: String): UserStore = synchronized(CACHE_LOCK) {
        userStores.getOrPut(userId) {
            val sp = storePrefs(appContext, userId)
            UserStore(
                profile = decodeProfile(sp.getString(KEY_PROFILE, null)),
                legal = decodeLegal(sp.getString(KEY_LEGAL, null)),
                wardrobe = decodeWardrobeList(sp.getString(KEY_WARDROBE, null)),
                collections = decodeCollectionList(sp.getString(KEY_COLLECTIONS, null)),
                outfits = decodeOutfitList(sp.getString(KEY_OUTFITS, null)),
                saved = decodeSavedList(sp.getString(KEY_SAVED, null)),
                previews = decodePreviewList(sp.getString(KEY_PREVIEWS, null)),
                aiMessages = decodeAiMessages(sp.getString(KEY_AI_MESSAGES, null)),
                styleBoards = decodeStyleBoards(sp.getString(KEY_STYLE_BOARDS, null)),
                recentlyViewed = decodeRecentlyViewed(sp.getString(KEY_RECENTLY_VIEWED, null)),
                followedAuthors = sp.getStringSet(KEY_FOLLOWED_AUTHORS, emptySet()).orEmpty().toSet(),
                blockedAuthors = sp.getStringSet(KEY_BLOCKED_AUTHORS, emptySet()).orEmpty().toSet()
            )
        }
    }

    private fun communityFlow(): MutableStateFlow<List<CommunityPost>> = synchronized(CACHE_LOCK) {
        communityPostsFlowCache ?: MutableStateFlow(
            decodeCommunityPosts(communityPrefs(appContext).getString(KEY_COMMUNITY_POSTS, null))
        ).also { communityPostsFlowCache = it }
    }

    private fun currentUserId(): String {
        return AuthRepository().currentUser(appContext)?.uid
            ?: throw IllegalStateException("Please sign in to access your LITHFIT wardrobe.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        storeFor(userId).profileFlow.asStateFlow()

    fun observeLegalAcceptance(userId: String): Flow<LegalAcceptance?> =
        storeFor(userId).legalFlow.asStateFlow()

    fun observeWardrobeItems(userId: String): Flow<List<WardrobeItem>> =
        storeFor(userId).wardrobeFlow.asStateFlow().map { list ->
            list.sortedByDescending { it.createdAtMillis }
        }

    fun observeOutfitCollections(userId: String): Flow<List<OutfitCollection>> =
        storeFor(userId).collectionsFlow.asStateFlow()

    fun observeOutfits(userId: String): Flow<List<Outfit>> =
        storeFor(userId).outfitsFlow.asStateFlow().map { list ->
            list.sortedByDescending { maxOf(it.updatedAtMillis, it.createdAtMillis) }
        }

    fun observeSavedItems(userId: String): Flow<List<SavedItem>> =
        storeFor(userId).savedFlow.asStateFlow().map { list ->
            list.sortedByDescending { it.createdAtMillis }
        }

    fun observeGeneratedPreviews(userId: String): Flow<List<GeneratedPreview>> =
        storeFor(userId).previewsFlow.asStateFlow().map { list ->
            list.sortedByDescending { it.createdAtMillis }
        }

    fun observeAiMessages(userId: String, @Suppress("UNUSED_PARAMETER") conversationId: String): Flow<List<AiMessage>> =
        storeFor(userId).aiMessagesFlow.asStateFlow().map { list ->
            list.sortedBy { it.createdAtMillis }
        }

    fun observeDiscoveryPosts(): Flow<List<DiscoveryPost>> =
        flowOf(SeedCatalogData.sampleDiscoveryPosts)

    fun observeStyleBoards(userId: String): Flow<List<StyleBoard>> =
        storeFor(userId).styleBoardsFlow.asStateFlow().map { list ->
            list.sortedByDescending { it.updatedAtMillis }
        }

    fun observeRecentlyViewed(userId: String): Flow<List<RecentlyViewedEntry>> =
        storeFor(userId).recentlyViewedFlow.asStateFlow().map { list ->
            list.sortedByDescending { it.viewedAtMillis }
        }

    fun observeCommunityPosts(): Flow<List<CommunityPost>> =
        communityFlow().asStateFlow().map { list ->
            list.sortedByDescending { it.createdAtMillis }
        }

    fun observeFollowedAuthors(userId: String): Flow<Set<String>> =
        storeFor(userId).followedAuthorsFlow.asStateFlow()

    fun observeBlockedAuthors(userId: String): Flow<Set<String>> =
        storeFor(userId).blockedAuthorsFlow.asStateFlow()

    fun observeSellers(): Flow<List<Seller>> = flowOf(emptyList())

    fun observeCatalogueProducts(): Flow<List<CatalogueProduct>> = flowOf(emptyList())

    fun observePartnerStores(): Flow<List<StoreLocation>> = flowOf(emptyList())

    suspend fun recordLegalAcceptance(
        cameraConsent: Boolean,
        photoConsent: Boolean,
        locationConsent: Boolean,
        notificationsConsent: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val legal = LegalAcceptance(
                id = "v1_consent",
                userId = uid,
                privacyPolicyVersion = LegalDocuments.PRIVACY_VERSION,
                termsVersion = LegalDocuments.TERMS_VERSION,
                communityGuidelinesVersion = LegalDocuments.GUIDELINES_VERSION,
                cameraConsent = cameraConsent,
                photoConsent = photoConsent,
                locationConsent = locationConsent,
                notificationsConsent = notificationsConsent,
                acceptedAtMillis = System.currentTimeMillis()
            )
            storePrefs(appContext, uid).edit()
                .putString(KEY_LEGAL, encodeLegal(legal))
                .apply()
            store.legalFlow.value = legal
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val now = System.currentTimeMillis()
            val safeDisplayName = profile.displayName.trim().take(60).ifBlank { "Lithfit Member" }
            val safeUsername = profile.username.trim().lowercase().removePrefix("@")
                .replace(Regex("[^a-z0-9_.]"), "_")
                .take(24)
                .let { if (it.length < 3) "${it}user".take(24) else it }
            val saved = profile.copy(
                id = uid,
                userId = uid,
                displayName = safeDisplayName,
                username = safeUsername,
                updatedAtMillis = now
            )
            storePrefs(appContext, uid).edit()
                .putString(KEY_PROFILE, encodeProfile(saved))
                .apply()
            store.profileFlow.value = saved
        }
    }

    suspend fun saveAvatarConfig(avatarConfig: AvatarConfig): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val current = store.profileFlow.value
                ?: UserProfile(id = uid, userId = uid, displayName = "Lithfit Member", username = "member")
            val updated = current.copy(
                avatarStyle = avatarConfig.aestheticPreset.take(40),
                avatarConfig = avatarConfig,
                updatedAtMillis = System.currentTimeMillis()
            )
            storePrefs(appContext, uid).edit()
                .putString(KEY_PROFILE, encodeProfile(updated))
                .apply()
            store.profileFlow.value = updated
        }
    }

    suspend fun setPersonalPhotoFlag(hasPhoto: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val current = store.profileFlow.value ?: return@runCatching
            val updated = current.copy(
                hasPersonalPhoto = hasPhoto,
                updatedAtMillis = System.currentTimeMillis()
            )
            storePrefs(appContext, uid).edit()
                .putString(KEY_PROFILE, encodeProfile(updated))
                .apply()
            store.profileFlow.value = updated
        }
    }

    suspend fun saveWardrobeItem(item: WardrobeItem): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            require(item.name.trim().isNotEmpty()) { "Item name cannot be empty." }
            require(item.color.trim().isNotEmpty()) { "Item color cannot be empty." }
            val store = storeFor(uid)
            val now = System.currentTimeMillis()
            val isNew = item.id.isBlank()
            val docId = if (isNew) "item_${UUID.randomUUID().toString().replace("-", "").take(16)}" else item.id
            val normalized = item.copy(
                id = docId,
                userId = uid,
                name = item.name.trim().take(80),
                color = item.color.trim().take(40),
                subType = item.subType?.trim()?.take(50)?.ifBlank { null },
                pattern = item.pattern?.trim()?.take(40)?.ifBlank { null },
                material = item.material?.trim()?.take(40)?.ifBlank { null },
                texture = item.texture?.trim()?.take(40)?.ifBlank { null },
                finish = item.finish?.trim()?.take(40)?.ifBlank { null },
                style = item.style?.trim()?.take(40)?.ifBlank { null },
                garmentType = item.garmentType?.trim()?.take(60)?.ifBlank { null },
                brand = item.brand?.trim()?.take(60)?.ifBlank { null },
                notes = item.notes?.trim()?.take(300)?.ifBlank { null },
                createdAtMillis = if (isNew) now else item.createdAtMillis,
                updatedAtMillis = now
            )
            val nextList = store.wardrobeFlow.value
                .filterNot { it.id == docId } + normalized
            storePrefs(appContext, uid).edit()
                .putString(KEY_WARDROBE, encodeWardrobeList(nextList))
                .apply()
            store.wardrobeFlow.value = nextList
            docId
        }
    }

    suspend fun deleteWardrobeItem(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val nextList = store.wardrobeFlow.value.filterNot { it.id == itemId }
            storePrefs(appContext, uid).edit()
                .putString(KEY_WARDROBE, encodeWardrobeList(nextList))
                .apply()
            store.wardrobeFlow.value = nextList
        }
    }

    suspend fun createOutfitCollection(name: String, description: String? = null): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val now = System.currentTimeMillis()
            val docId = "col_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val created = OutfitCollection(
                id = docId,
                userId = uid,
                name = name.trim().take(60),
                description = description?.trim()?.take(200)?.ifBlank { null },
                createdAtMillis = now,
                updatedAtMillis = now
            )
            val nextList = store.collectionsFlow.value + created
            storePrefs(appContext, uid).edit()
                .putString(KEY_COLLECTIONS, encodeCollectionList(nextList))
                .apply()
            store.collectionsFlow.value = nextList
            docId
        }
    }

    suspend fun saveOutfit(outfit: Outfit): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            require(outfit.name.trim().isNotEmpty()) { "Outfit name cannot be empty." }
            val store = storeFor(uid)
            val now = System.currentTimeMillis()
            val isNew = outfit.id.isBlank()
            val docId = if (isNew) "outfit_${UUID.randomUUID().toString().replace("-", "").take(16)}" else outfit.id
            val normalized = outfit.copy(
                id = docId,
                userId = uid,
                name = outfit.name.trim().take(80),
                createdAtMillis = if (isNew) now else outfit.createdAtMillis,
                updatedAtMillis = now
            )
            val nextList = store.outfitsFlow.value.filterNot { it.id == docId } + normalized
            storePrefs(appContext, uid).edit()
                .putString(KEY_OUTFITS, encodeOutfitList(nextList))
                .apply()
            store.outfitsFlow.value = nextList
            docId
        }
    }

    suspend fun renameOutfit(outfitId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val clean = newName.trim().take(80)
            require(clean.isNotEmpty()) { "Outfit name cannot be empty." }
            val store = storeFor(uid)
            val now = System.currentTimeMillis()
            val nextList = store.outfitsFlow.value.map {
                if (it.id == outfitId) it.copy(name = clean, updatedAtMillis = now) else it
            }
            storePrefs(appContext, uid).edit()
                .putString(KEY_OUTFITS, encodeOutfitList(nextList))
                .apply()
            store.outfitsFlow.value = nextList
        }
    }

    suspend fun setOutfitFeedback(outfitId: String, feedbackStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            require(feedbackStatus == "LIKED" || feedbackStatus == "DISLIKED") { "Invalid feedback status" }
            val store = storeFor(uid)
            val now = System.currentTimeMillis()
            val nextList = store.outfitsFlow.value.map {
                if (it.id == outfitId) it.copy(feedbackStatus = feedbackStatus, updatedAtMillis = now) else it
            }
            storePrefs(appContext, uid).edit()
                .putString(KEY_OUTFITS, encodeOutfitList(nextList))
                .apply()
            store.outfitsFlow.value = nextList
        }
    }

    suspend fun deleteOutfit(outfitId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val nextList = store.outfitsFlow.value.filterNot { it.id == outfitId }
            storePrefs(appContext, uid).edit()
                .putString(KEY_OUTFITS, encodeOutfitList(nextList))
                .apply()
            store.outfitsFlow.value = nextList
        }
    }

    suspend fun ensureConversation(
        @Suppress("UNUSED_PARAMETER") conversationId: String,
        @Suppress("UNUSED_PARAMETER") title: String,
        @Suppress("UNUSED_PARAMETER") occasion: String,
        @Suppress("UNUSED_PARAMETER") vibe: String
    ): Result<Unit> = Result.success(Unit)

    suspend fun appendAiMessage(
        @Suppress("UNUSED_PARAMETER") conversationId: String,
        role: String,
        text: String,
        recommendedItemNames: List<String> = emptyList(),
        stylingReason: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val msg = AiMessage(
                id = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                userId = uid,
                role = role,
                text = text.take(8000),
                recommendedItemNames = recommendedItemNames.take(15),
                stylingReason = stylingReason?.take(1000)?.ifBlank { null },
                createdAtMillis = System.currentTimeMillis()
            )
            val nextList = (store.aiMessagesFlow.value + msg).takeLast(60)
            storePrefs(appContext, uid).edit()
                .putString(KEY_AI_MESSAGES, encodeAiMessages(nextList))
                .apply()
            store.aiMessagesFlow.value = nextList
        }
    }

    suspend fun saveGeneratedPreview(preview: GeneratedPreview): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val docId = preview.id.ifBlank { "prev_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
            val normalized = preview.copy(
                id = docId,
                userId = uid,
                promptSummary = preview.promptSummary.take(500).ifBlank { "Outfit Preview" },
                createdAtMillis = System.currentTimeMillis()
            )
            val nextList = store.previewsFlow.value.filterNot { it.id == docId } + normalized
            storePrefs(appContext, uid).edit()
                .putString(KEY_PREVIEWS, encodePreviewList(nextList))
                .apply()
            store.previewsFlow.value = nextList
            docId
        }
    }

    suspend fun toggleSaveItem(
        targetType: String,
        targetId: String,
        title: String,
        subtitle: String?,
        category: String,
        collectionName: String? = null,
        liked: Boolean = false
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val safeTarget = targetId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val docId = "${targetType.lowercase()}_$safeTarget".take(128)
            val existing = store.savedFlow.value.firstOrNull { it.id == docId }
            if (existing != null) {
                val nextList = store.savedFlow.value.filterNot { it.id == docId }
                storePrefs(appContext, uid).edit()
                    .putString(KEY_SAVED, encodeSavedList(nextList))
                    .apply()
                store.savedFlow.value = nextList
                false
            } else {
                val item = SavedItem(
                    id = docId,
                    userId = uid,
                    targetType = targetType,
                    targetId = targetId.take(128),
                    title = title.take(100),
                    subtitle = subtitle?.take(160)?.ifBlank { null },
                    category = category.take(50),
                    collectionName = collectionName?.take(60)?.ifBlank { null },
                    liked = liked,
                    createdAtMillis = System.currentTimeMillis()
                )
                val nextList = store.savedFlow.value + item
                storePrefs(appContext, uid).edit()
                    .putString(KEY_SAVED, encodeSavedList(nextList))
                    .apply()
                store.savedFlow.value = nextList
                true
            }
        }
    }

    suspend fun toggleLikeSavedItem(
        targetType: String,
        targetId: String,
        title: String,
        subtitle: String?,
        category: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val safeTarget = targetId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val docId = "${targetType.lowercase()}_$safeTarget".take(128)
            val existing = store.savedFlow.value.firstOrNull { it.id == docId }
            val newLiked: Boolean
            val nextList = if (existing != null) {
                newLiked = !existing.liked
                store.savedFlow.value.map { if (it.id == docId) it.copy(liked = newLiked) else it }
            } else {
                newLiked = true
                store.savedFlow.value + SavedItem(
                    id = docId,
                    userId = uid,
                    targetType = targetType,
                    targetId = targetId.take(128),
                    title = title.take(100),
                    subtitle = subtitle?.take(160)?.ifBlank { null },
                    category = category.take(50),
                    liked = true,
                    createdAtMillis = System.currentTimeMillis()
                )
            }
            storePrefs(appContext, uid).edit()
                .putString(KEY_SAVED, encodeSavedList(nextList))
                .apply()
            store.savedFlow.value = nextList
            newLiked
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val sp = storePrefs(appContext, uid)
            val existing = runCatching { JSONArray(sp.getString(KEY_REPORTS, "[]")) }.getOrDefault(JSONArray())
            existing.put(
                JSONObject().apply {
                    put("id", "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}")
                    put("targetType", targetType)
                    put("targetId", targetId)
                    put("reason", reason.trim())
                    put("details", details?.trim().orEmpty())
                    put("createdAtMillis", System.currentTimeMillis())
                }
            )
            sp.edit().putString(KEY_REPORTS, existing.toString()).apply()
        }
    }

    suspend fun saveStyleBoard(board: StyleBoard): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            require(board.name.trim().isNotEmpty()) { "Board name cannot be empty." }
            val store = storeFor(uid)
            val now = System.currentTimeMillis()
            val isNew = board.id.isBlank()
            val docId = if (isNew) "board_${UUID.randomUUID().toString().replace("-", "").take(14)}" else board.id
            val normalized = board.copy(
                id = docId,
                userId = uid,
                name = board.name.trim().take(60),
                description = board.description.trim().take(200),
                aesthetic = board.aesthetic.trim().ifBlank { "Streetwear" },
                occasion = board.occasion.trim().ifBlank { "Casual day" },
                updatedAtMillis = now
            )
            val next = store.styleBoardsFlow.value.filterNot { it.id == docId } + normalized
            storePrefs(appContext, uid).edit()
                .putString(KEY_STYLE_BOARDS, encodeStyleBoards(next))
                .apply()
            store.styleBoardsFlow.value = next
            docId
        }
    }

    suspend fun deleteStyleBoard(boardId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val next = store.styleBoardsFlow.value.filterNot { it.id == boardId }
            storePrefs(appContext, uid).edit()
                .putString(KEY_STYLE_BOARDS, encodeStyleBoards(next))
                .apply()
            store.styleBoardsFlow.value = next
        }
    }

    suspend fun toggleItemInStyleBoard(boardId: String, itemId: String, coverImageKey: String? = null): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uid = currentUserId()
                val store = storeFor(uid)
                var added = false
                val next = store.styleBoardsFlow.value.map { board ->
                    if (board.id == boardId) {
                        val exists = itemId in board.itemIds
                        added = !exists
                        val nextItems = if (exists) board.itemIds - itemId else (board.itemIds + itemId).distinct()
                        board.copy(
                            itemIds = nextItems,
                            imageResKey = board.imageResKey ?: coverImageKey,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    } else {
                        board
                    }
                }
                storePrefs(appContext, uid).edit()
                    .putString(KEY_STYLE_BOARDS, encodeStyleBoards(next))
                    .apply()
                store.styleBoardsFlow.value = next
                added
            }
        }

    suspend fun recordDiscoveryView(post: DiscoveryPost): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val entry = RecentlyViewedEntry(
                id = "rv_${post.id}",
                postId = post.id,
                title = post.title,
                category = post.category,
                imageResKey = post.imageResKey,
                colorPalette = post.colorPalette,
                viewedAtMillis = System.currentTimeMillis()
            )
            val next = (listOf(entry) + store.recentlyViewedFlow.value.filterNot { it.postId == post.id }).take(12)
            storePrefs(appContext, uid).edit()
                .putString(KEY_RECENTLY_VIEWED, encodeRecentlyViewed(next))
                .apply()
            store.recentlyViewedFlow.value = next
        }
    }

    suspend fun publishCommunityPost(
        title: String,
        caption: String,
        aesthetic: String,
        imagePath: String?,
        thumbnailBase64: String?,
        colorPalette: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val profile = store.profileFlow.value
            val authUser = AuthRepository().currentUser(appContext)
            require(title.trim().isNotEmpty()) { "Post title cannot be empty." }
            val docId = "comm_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val created = CommunityPost(
                id = docId,
                authorId = uid,
                authorName = profile?.displayName ?: authUser?.displayName ?: "LITHFIT Stylist",
                authorHandle = profile?.username ?: authUser?.username ?: "member",
                title = title.trim().take(80),
                caption = caption.trim().take(320),
                aesthetic = aesthetic.trim().ifBlank { "Streetwear" },
                imagePath = imagePath,
                thumbnailBase64 = thumbnailBase64,
                colorPalette = colorPalette.take(5),
                createdAtMillis = System.currentTimeMillis()
            )
            val flow = communityFlow()
            val next = (listOf(created) + flow.value).take(50)
            communityPrefs(appContext).edit()
                .putString(KEY_COMMUNITY_POSTS, encodeCommunityPosts(next))
                .apply()
            flow.value = next
            docId
        }
    }

    suspend fun deleteCommunityPost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val flow = communityFlow()
            val next = flow.value.filterNot { it.id == postId && it.authorId == uid }
            communityPrefs(appContext).edit()
                .putString(KEY_COMMUNITY_POSTS, encodeCommunityPosts(next))
                .apply()
            flow.value = next
        }
    }

    suspend fun toggleFollowAuthor(authorId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val current = store.followedAuthorsFlow.value
            val nowFollowed = authorId !in current
            val next = if (nowFollowed) current + authorId else current - authorId
            storePrefs(appContext, uid).edit()
                .putStringSet(KEY_FOLLOWED_AUTHORS, next)
                .apply()
            store.followedAuthorsFlow.value = next
            nowFollowed
        }
    }

    suspend fun blockAuthor(authorId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val nextBlocked = store.blockedAuthorsFlow.value + authorId
            val nextFollowed = store.followedAuthorsFlow.value - authorId
            storePrefs(appContext, uid).edit()
                .putStringSet(KEY_BLOCKED_AUTHORS, nextBlocked)
                .putStringSet(KEY_FOLLOWED_AUTHORS, nextFollowed)
                .apply()
            store.blockedAuthorsFlow.value = nextBlocked
            store.followedAuthorsFlow.value = nextFollowed
        }
    }

    // --- JSON Serialization Helpers ---

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotEmpty() } }

    private fun List<String>.toJsonArray(): JSONArray = JSONArray().apply {
        forEach { put(it) }
    }

    private fun encodeAvatar(a: AvatarConfig): JSONObject = JSONObject().apply {
        put("presentation", a.presentation)
        put("skinToneHex", a.skinToneHex)
        put("faceShape", a.faceShape)
        put("eyeColor", a.eyeColor)
        put("expression", a.expression)
        put("hairStyle", a.hairStyle)
        put("hairColor", a.hairColor)
        put("glassesStyle", a.glassesStyle)
        put("topStyle", a.topStyle)
        put("topColor", a.topColor)
        put("bottomStyle", a.bottomStyle)
        put("bottomColor", a.bottomColor)
        put("shoesStyle", a.shoesStyle)
        put("shoesColor", a.shoesColor)
        put("accessoryStyle", a.accessoryStyle)
        put("aestheticPreset", a.aestheticPreset)
    }

    private fun decodeAvatar(o: JSONObject?): AvatarConfig {
        if (o == null) return AvatarConfig()
        return AvatarConfig(
            presentation = o.optString("presentation", "Neutral"),
            skinToneHex = o.optLong("skinToneHex", 0xFFD9A682L),
            faceShape = o.optString("faceShape", "Oval"),
            eyeColor = o.optString("eyeColor", "Espresso"),
            expression = o.optString("expression", "Confident"),
            hairStyle = o.optString("hairStyle", "Textured Crop"),
            hairColor = o.optString("hairColor", "Deep Black"),
            glassesStyle = o.optString("glassesStyle", "None"),
            topStyle = o.optString("topStyle", "Boxy Hoodie"),
            topColor = o.optString("topColor", "Charcoal"),
            bottomStyle = o.optString("bottomStyle", "Wide Cargos"),
            bottomColor = o.optString("bottomColor", "Olive"),
            shoesStyle = o.optString("shoesStyle", "Low-Top Sneakers"),
            shoesColor = o.optString("shoesColor", "Cream"),
            accessoryStyle = o.optString("accessoryStyle", "Minimal Chain"),
            aestheticPreset = o.optString("aestheticPreset", "Streetwear")
        )
    }

    private fun encodeProfile(p: UserProfile): String = JSONObject().apply {
        put("id", p.id)
        put("userId", p.userId)
        put("displayName", p.displayName)
        put("username", p.username)
        put("email", p.email)
        put("avatarUrl", p.avatarUrl)
        put("avatarStyle", p.avatarStyle)
        put("avatarConfig", encodeAvatar(p.avatarConfig))
        put("hasPersonalPhoto", p.hasPersonalPhoto)
        put("guideCompleted", p.guideCompleted)
        put("privateProfile", p.privateProfile)
        put("minorAccount", p.minorAccount)
        put("preferredStyles", p.preferredStyles.toJsonArray())
        put("preferredColors", p.preferredColors.toJsonArray())
        put("avoidColors", p.avoidColors.toJsonArray())
        put("preferredCategories", p.preferredCategories.toJsonArray())
        put("preferredWeatherStyles", p.preferredWeatherStyles.toJsonArray())
        put("preferredOccasions", p.preferredOccasions.toJsonArray())
        put("preferredFootwear", p.preferredFootwear.toJsonArray())
        put("preferredAccessories", p.preferredAccessories.toJsonArray())
        put("budgetPreference", p.budgetPreference)
        put("inspirationFocus", p.inspirationFocus)
        put("preferredCurrency", p.preferredCurrency)
        put("preferredRegion", p.preferredRegion)
        put("locationLabel", p.locationLabel)
        put("themeMode", p.themeMode)
        put("notificationsEnabled", p.notificationsEnabled)
        put("locationAccessPreference", p.locationAccessPreference)
        put("gender", p.gender)
        put("age", p.age)
        put("height", p.height)
        put("bodySize", p.bodySize)
        put("createdAtMillis", p.createdAtMillis)
        put("updatedAtMillis", p.updatedAtMillis)
    }.toString()

    private fun decodeProfile(raw: String?): UserProfile? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val o = JSONObject(raw)
            UserProfile(
                id = o.optString("id"),
                userId = o.optString("userId"),
                displayName = o.optString("displayName"),
                username = o.optString("username"),
                email = o.optString("email").ifBlank { null },
                avatarUrl = o.optString("avatarUrl").ifBlank { null },
                avatarStyle = o.optString("avatarStyle").ifBlank { "Streetwear" },
                avatarConfig = decodeAvatar(o.optJSONObject("avatarConfig")),
                hasPersonalPhoto = o.optBoolean("hasPersonalPhoto", false),
                guideCompleted = o.optBoolean("guideCompleted", false),
                privateProfile = o.optBoolean("privateProfile", true),
                minorAccount = o.optBoolean("minorAccount", false),
                preferredStyles = o.optJSONArray("preferredStyles")?.toStringList().orEmpty(),
                preferredColors = o.optJSONArray("preferredColors")?.toStringList().orEmpty(),
                avoidColors = o.optJSONArray("avoidColors")?.toStringList().orEmpty(),
                preferredCategories = o.optJSONArray("preferredCategories")?.toStringList().orEmpty(),
                preferredWeatherStyles = o.optJSONArray("preferredWeatherStyles")?.toStringList().orEmpty(),
                preferredOccasions = o.optJSONArray("preferredOccasions")?.toStringList().orEmpty(),
                preferredFootwear = o.optJSONArray("preferredFootwear")?.toStringList().orEmpty(),
                preferredAccessories = o.optJSONArray("preferredAccessories")?.toStringList().orEmpty(),
                budgetPreference = o.optString("budgetPreference", "Balanced everyday"),
                inspirationFocus = o.optString("inspirationFocus", "All"),
                preferredCurrency = o.optString("preferredCurrency", "USD"),
                preferredRegion = o.optString("preferredRegion").ifBlank { null },
                locationLabel = o.optString("locationLabel").ifBlank { null },
                themeMode = o.optString("themeMode", ThemeMode.LIGHT.name),
                notificationsEnabled = o.optBoolean("notificationsEnabled", false),
                locationAccessPreference = o.optBoolean("locationAccessPreference", false),
                gender = o.optString("gender"),
                age = o.optString("age"),
                height = o.optString("height"),
                bodySize = o.optString("bodySize"),
                createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    private fun encodeLegal(l: LegalAcceptance): String = JSONObject().apply {
        put("id", l.id)
        put("userId", l.userId)
        put("privacyPolicyVersion", l.privacyPolicyVersion)
        put("termsVersion", l.termsVersion)
        put("communityGuidelinesVersion", l.communityGuidelinesVersion)
        put("cameraConsent", l.cameraConsent)
        put("photoConsent", l.photoConsent)
        put("locationConsent", l.locationConsent)
        put("notificationsConsent", l.notificationsConsent)
        put("acceptedAtMillis", l.acceptedAtMillis)
    }.toString()

    private fun decodeLegal(raw: String?): LegalAcceptance? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val o = JSONObject(raw)
            LegalAcceptance(
                id = o.optString("id", "v1_consent"),
                userId = o.optString("userId"),
                privacyPolicyVersion = o.optString("privacyPolicyVersion", "1.0"),
                termsVersion = o.optString("termsVersion", "1.0"),
                communityGuidelinesVersion = o.optString("communityGuidelinesVersion", "1.0"),
                cameraConsent = o.optBoolean("cameraConsent", false),
                photoConsent = o.optBoolean("photoConsent", false),
                locationConsent = o.optBoolean("locationConsent", false),
                notificationsConsent = o.optBoolean("notificationsConsent", false),
                acceptedAtMillis = o.optLong("acceptedAtMillis", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    private fun encodeWardrobeList(list: List<WardrobeItem>): String = JSONArray().apply {
        list.forEach { w ->
            put(
                JSONObject().apply {
                    put("id", w.id)
                    put("userId", w.userId)
                    put("wardrobeId", w.wardrobeId)
                    put("name", w.name)
                    put("category", w.category)
                    put("subType", w.subType)
                    put("color", w.color)
                    put("pattern", w.pattern)
                    put("material", w.material)
                    put("texture", w.texture)
                    put("finish", w.finish)
                    put("style", w.style)
                    put("garmentType", w.garmentType)
                    put("brand", w.brand)
                    put("notes", w.notes)
                    put("imagePath", w.imagePath)
                    put("thumbnailBase64", w.thumbnailBase64)
                    put("favorite", w.favorite)
                    put("privateItem", w.privateItem)
                    put("collectionTags", w.collectionTags.toJsonArray())
                    put("createdAtMillis", w.createdAtMillis)
                    put("updatedAtMillis", w.updatedAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeWardrobeList(raw: String?): List<WardrobeItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                WardrobeItem(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    wardrobeId = o.optString("wardrobeId", "default"),
                    name = o.optString("name"),
                    category = o.optString("category"),
                    subType = o.optString("subType").ifBlank { null },
                    color = o.optString("color"),
                    pattern = o.optString("pattern").ifBlank { null },
                    material = o.optString("material").ifBlank { null },
                    texture = o.optString("texture").ifBlank { null },
                    finish = o.optString("finish").ifBlank { null },
                    style = o.optString("style").ifBlank { null },
                    garmentType = o.optString("garmentType").ifBlank { null },
                    brand = o.optString("brand").ifBlank { null },
                    notes = o.optString("notes").ifBlank { null },
                    imagePath = o.optString("imagePath").ifBlank { null },
                    thumbnailBase64 = o.optString("thumbnailBase64").ifBlank { null },
                    favorite = o.optBoolean("favorite", false),
                    privateItem = o.optBoolean("privateItem", true),
                    collectionTags = o.optJSONArray("collectionTags")?.toStringList().orEmpty(),
                    createdAtMillis = o.optLong("createdAtMillis", 0L),
                    updatedAtMillis = o.optLong("updatedAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeCollectionList(list: List<OutfitCollection>): String = JSONArray().apply {
        list.forEach { c ->
            put(
                JSONObject().apply {
                    put("id", c.id)
                    put("userId", c.userId)
                    put("name", c.name)
                    put("description", c.description)
                    put("createdAtMillis", c.createdAtMillis)
                    put("updatedAtMillis", c.updatedAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeCollectionList(raw: String?): List<OutfitCollection> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                OutfitCollection(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    name = o.optString("name"),
                    description = o.optString("description").ifBlank { null },
                    createdAtMillis = o.optLong("createdAtMillis", 0L),
                    updatedAtMillis = o.optLong("updatedAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeOutfitList(list: List<Outfit>): String = JSONArray().apply {
        list.forEach { f ->
            put(
                JSONObject().apply {
                    put("id", f.id)
                    put("userId", f.userId)
                    put("name", f.name)
                    put("occasion", f.occasion)
                    put("vibe", f.vibe)
                    put("notes", f.notes)
                    put("collectionId", f.collectionId)
                    put("topItemId", f.topItemId)
                    put("bottomItemId", f.bottomItemId)
                    put("shoesItemId", f.shoesItemId)
                    put("jacketItemId", f.jacketItemId)
                    put("accessoryIds", f.accessoryIds.toJsonArray())
                    put("itemNames", f.itemNames.toJsonArray())
                    put("colorPalette", f.colorPalette.toJsonArray())
                    put("itemColorOverrides", JSONObject(f.itemColorOverrides))
                    put("feedbackStatus", f.feedbackStatus)
                    put("previewPath", f.previewPath)
                    put("favorite", f.favorite)
                    put("createdAtMillis", f.createdAtMillis)
                    put("updatedAtMillis", f.updatedAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeOutfitList(raw: String?): List<Outfit> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                val overridesObj = o.optJSONObject("itemColorOverrides")
                val overridesMap = mutableMapOf<String, String>()
                overridesObj?.keys()?.forEach { k -> overridesMap[k] = overridesObj.optString(k) }
                Outfit(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    name = o.optString("name"),
                    occasion = o.optString("occasion", "Casual day"),
                    vibe = o.optString("vibe", "Streetwear"),
                    notes = o.optString("notes").ifBlank { null },
                    collectionId = o.optString("collectionId").ifBlank { null },
                    topItemId = o.optString("topItemId").ifBlank { null },
                    bottomItemId = o.optString("bottomItemId").ifBlank { null },
                    shoesItemId = o.optString("shoesItemId").ifBlank { null },
                    jacketItemId = o.optString("jacketItemId").ifBlank { null },
                    accessoryIds = o.optJSONArray("accessoryIds")?.toStringList().orEmpty(),
                    itemNames = o.optJSONArray("itemNames")?.toStringList().orEmpty(),
                    colorPalette = o.optJSONArray("colorPalette")?.toStringList().orEmpty(),
                    itemColorOverrides = overridesMap,
                    feedbackStatus = o.optString("feedbackStatus").ifBlank { null },
                    previewPath = o.optString("previewPath").ifBlank { null },
                    favorite = o.optBoolean("favorite", false),
                    createdAtMillis = o.optLong("createdAtMillis", 0L),
                    updatedAtMillis = o.optLong("updatedAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeSavedList(list: List<SavedItem>): String = JSONArray().apply {
        list.forEach { s ->
            put(
                JSONObject().apply {
                    put("id", s.id)
                    put("userId", s.userId)
                    put("targetType", s.targetType)
                    put("targetId", s.targetId)
                    put("title", s.title)
                    put("subtitle", s.subtitle)
                    put("category", s.category)
                    put("collectionName", s.collectionName)
                    put("liked", s.liked)
                    put("createdAtMillis", s.createdAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeSavedList(raw: String?): List<SavedItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                SavedItem(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    targetType = o.optString("targetType"),
                    targetId = o.optString("targetId"),
                    title = o.optString("title"),
                    subtitle = o.optString("subtitle").ifBlank { null },
                    category = o.optString("category"),
                    collectionName = o.optString("collectionName").ifBlank { null },
                    liked = o.optBoolean("liked", false),
                    createdAtMillis = o.optLong("createdAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodePreviewList(list: List<GeneratedPreview>): String = JSONArray().apply {
        list.forEach { p ->
            put(
                JSONObject().apply {
                    put("id", p.id)
                    put("userId", p.userId)
                    put("outfitId", p.outfitId)
                    put("promptSummary", p.promptSummary)
                    put("occasion", p.occasion)
                    put("vibe", p.vibe)
                    put("localImagePath", p.localImagePath)
                    put("remoteImageUrl", p.remoteImageUrl)
                    put("thumbnailBase64", p.thumbnailBase64)
                    put("createdAtMillis", p.createdAtMillis)
                }
            )
        }
    }.toString()

    private fun decodePreviewList(raw: String?): List<GeneratedPreview> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                GeneratedPreview(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    outfitId = o.optString("outfitId").ifBlank { null },
                    promptSummary = o.optString("promptSummary"),
                    occasion = o.optString("occasion"),
                    vibe = o.optString("vibe"),
                    localImagePath = o.optString("localImagePath").ifBlank { null },
                    remoteImageUrl = o.optString("remoteImageUrl").ifBlank { null },
                    thumbnailBase64 = o.optString("thumbnailBase64").ifBlank { null },
                    createdAtMillis = o.optLong("createdAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeAiMessages(list: List<AiMessage>): String = JSONArray().apply {
        list.forEach { m ->
            put(
                JSONObject().apply {
                    put("id", m.id)
                    put("userId", m.userId)
                    put("role", m.role)
                    put("text", m.text)
                    put("recommendedItemNames", m.recommendedItemNames.toJsonArray())
                    put("stylingReason", m.stylingReason)
                    put("createdAtMillis", m.createdAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeAiMessages(raw: String?): List<AiMessage> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                AiMessage(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    role = o.optString("role", "ASSISTANT"),
                    text = o.optString("text"),
                    recommendedItemNames = o.optJSONArray("recommendedItemNames")?.toStringList().orEmpty(),
                    stylingReason = o.optString("stylingReason").ifBlank { null },
                    createdAtMillis = o.optLong("createdAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeStyleBoards(list: List<StyleBoard>): String = JSONArray().apply {
        list.forEach { b ->
            put(
                JSONObject().apply {
                    put("id", b.id)
                    put("userId", b.userId)
                    put("name", b.name)
                    put("description", b.description)
                    put("aesthetic", b.aesthetic)
                    put("colors", b.colors.toJsonArray())
                    put("occasion", b.occasion)
                    put("itemIds", b.itemIds.toJsonArray())
                    put("imageResKey", b.imageResKey)
                    put("updatedAtMillis", b.updatedAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeStyleBoards(raw: String?): List<StyleBoard> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                StyleBoard(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    name = o.optString("name"),
                    description = o.optString("description"),
                    aesthetic = o.optString("aesthetic", "Streetwear"),
                    colors = o.optJSONArray("colors")?.toStringList().orEmpty(),
                    occasion = o.optString("occasion", "Casual day"),
                    itemIds = o.optJSONArray("itemIds")?.toStringList().orEmpty(),
                    imageResKey = o.optString("imageResKey").ifBlank { null },
                    updatedAtMillis = o.optLong("updatedAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeRecentlyViewed(list: List<RecentlyViewedEntry>): String = JSONArray().apply {
        list.forEach { r ->
            put(
                JSONObject().apply {
                    put("id", r.id)
                    put("postId", r.postId)
                    put("title", r.title)
                    put("category", r.category)
                    put("imageResKey", r.imageResKey)
                    put("colorPalette", r.colorPalette.toJsonArray())
                    put("viewedAtMillis", r.viewedAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeRecentlyViewed(raw: String?): List<RecentlyViewedEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                RecentlyViewedEntry(
                    id = o.optString("id"),
                    postId = o.optString("postId"),
                    title = o.optString("title"),
                    category = o.optString("category"),
                    imageResKey = o.optString("imageResKey").ifBlank { null },
                    colorPalette = o.optJSONArray("colorPalette")?.toStringList().orEmpty(),
                    viewedAtMillis = o.optLong("viewedAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeCommunityPosts(list: List<CommunityPost>): String = JSONArray().apply {
        list.forEach { c ->
            put(
                JSONObject().apply {
                    put("id", c.id)
                    put("authorId", c.authorId)
                    put("authorName", c.authorName)
                    put("authorHandle", c.authorHandle)
                    put("title", c.title)
                    put("caption", c.caption)
                    put("aesthetic", c.aesthetic)
                    put("imagePath", c.imagePath)
                    put("thumbnailBase64", c.thumbnailBase64)
                    put("colorPalette", c.colorPalette.toJsonArray())
                    put("createdAtMillis", c.createdAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeCommunityPosts(raw: String?): List<CommunityPost> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                CommunityPost(
                    id = o.optString("id"),
                    authorId = o.optString("authorId"),
                    authorName = o.optString("authorName"),
                    authorHandle = o.optString("authorHandle"),
                    title = o.optString("title"),
                    caption = o.optString("caption"),
                    aesthetic = o.optString("aesthetic", "Streetwear"),
                    imagePath = o.optString("imagePath").ifBlank { null },
                    thumbnailBase64 = o.optString("thumbnailBase64").ifBlank { null },
                    colorPalette = o.optJSONArray("colorPalette")?.toStringList().orEmpty(),
                    createdAtMillis = o.optLong("createdAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val KEY_PROFILE = "profile_json"
        private const val KEY_LEGAL = "legal_json"
        private const val KEY_WARDROBE = "wardrobe_json"
        private const val KEY_COLLECTIONS = "collections_json"
        private const val KEY_OUTFITS = "outfits_json"
        private const val KEY_SAVED = "saved_json"
        private const val KEY_PREVIEWS = "previews_json"
        private const val KEY_AI_MESSAGES = "ai_messages_json"
        private const val KEY_REPORTS = "reports_json"
        private const val KEY_STYLE_BOARDS = "style_boards_json"
        private const val KEY_RECENTLY_VIEWED = "recently_viewed_json"
        private const val KEY_FOLLOWED_AUTHORS = "followed_authors_set"
        private const val KEY_BLOCKED_AUTHORS = "blocked_authors_set"
        private const val KEY_COMMUNITY_POSTS = "community_posts_json"

        private val CACHE_LOCK = Any()
        private val userStores = mutableMapOf<String, UserStore>()
        private var communityPostsFlowCache: MutableStateFlow<List<CommunityPost>>? = null

        private fun storePrefs(context: Context, userId: String): SharedPreferences =
            context.getSharedPreferences("lithfit_store_$userId", Context.MODE_PRIVATE)

        private fun communityPrefs(context: Context): SharedPreferences =
            context.getSharedPreferences("lithfit_community_feed", Context.MODE_PRIVATE)

        fun clearUserStore(context: Context, userId: String) {
            synchronized(CACHE_LOCK) {
                userStores.remove(userId)
            }
            storePrefs(context, userId).edit().clear().apply()
        }
    }
}
