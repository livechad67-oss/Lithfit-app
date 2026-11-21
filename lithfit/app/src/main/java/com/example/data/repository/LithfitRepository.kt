package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.R
import com.example.data.model.AiConversation
import com.example.data.model.AiMessage
import com.example.data.model.AppNotification
import com.example.data.model.AvatarConfig
import com.example.data.model.CalendarOutfitEntry
import com.example.data.model.CatalogueProduct
import com.example.data.model.CommunityPost
import com.example.data.model.DiscoveryPost
import com.example.data.model.GarmentCategory
import com.example.data.model.LegalAcceptance
import com.example.data.model.LegalDocuments
import com.example.data.model.LithfitConstants
import com.example.data.model.Outfit
import com.example.data.model.OutfitCollection
import com.example.data.model.OutfitWearHistory
import com.example.data.model.OwnerRegistrationNotification
import com.example.data.model.RecentlyViewedEntry
import com.example.data.model.SavedItem
import com.example.data.model.SeedCatalogData
import com.example.data.model.Seller
import com.example.data.model.StyleBoard
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.model.WardrobeInsightsSummary
import com.example.data.model.WardrobeItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class LithfitRepository(context: Context) {

    private val appContext = context.applicationContext

    private val firestore: FirebaseFirestore? by lazy {
        runCatching {
            val dbId = appContext.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        }.getOrNull()
    }

    private fun isFirebaseSessionFor(userId: String): Boolean {
        val fbUser = runCatching { Firebase.auth.currentUser }.getOrNull() ?: return false
        return fbUser.uid == userId && userId.isNotBlank()
    }

    private class UserStore(
        profile: UserProfile?,
        legal: LegalAcceptance?,
        wardrobe: List<WardrobeItem>,
        collections: List<OutfitCollection>,
        outfits: List<Outfit>,
        saved: List<SavedItem>,
        aiConversations: List<AiConversation>,
        aiMessages: List<AiMessage>,
        styleBoards: List<StyleBoard>,
        recentlyViewed: List<RecentlyViewedEntry>,
        calendarEntries: List<CalendarOutfitEntry>,
        wearHistory: List<OutfitWearHistory>,
        notifications: List<AppNotification>,
        followedAuthors: Set<String>,
        blockedAuthors: Set<String>
    ) {
        val profileFlow = MutableStateFlow(profile)
        val legalFlow = MutableStateFlow(legal)
        val wardrobeFlow = MutableStateFlow(wardrobe)
        val collectionsFlow = MutableStateFlow(collections)
        val outfitsFlow = MutableStateFlow(outfits)
        val savedFlow = MutableStateFlow(saved)
        val aiConversationsFlow = MutableStateFlow(aiConversations)
        val aiMessagesFlow = MutableStateFlow(aiMessages)
        val styleBoardsFlow = MutableStateFlow(styleBoards)
        val recentlyViewedFlow = MutableStateFlow(recentlyViewed)
        val calendarFlow = MutableStateFlow(calendarEntries)
        val wearHistoryFlow = MutableStateFlow(wearHistory)
        val notificationsFlow = MutableStateFlow(notifications)
        val followedAuthorsFlow = MutableStateFlow(followedAuthors)
        val blockedAuthorsFlow = MutableStateFlow(blockedAuthors)
    }

    suspend fun warmUpUserStore(userId: String) {
        withContext(Dispatchers.IO) {
            storeFor(userId)
        }
    }

    private fun storeFor(userId: String): UserStore = synchronized(CACHE_LOCK) {
        userStores.getOrPut(userId) {
            val sp = storePrefs(appContext, userId)
            val decodedMsgs = decodeAiMessages(sp.getString(KEY_AI_MESSAGES, null))
            val decodedConvs = decodeAiConversations(sp.getString(KEY_AI_CONVERSATIONS, null)).let { list ->
                if (list.isEmpty() && decodedMsgs.isNotEmpty()) {
                    listOf(
                        AiConversation(
                            id = "main_stylist",
                            userId = userId,
                            title = decodedMsgs.firstOrNull { it.role == "USER" }?.text?.take(42) ?: "LITHFIT AI Styling",
                            preview = decodedMsgs.lastOrNull()?.text?.take(80).orEmpty(),
                            updatedAtMillis = decodedMsgs.lastOrNull()?.createdAtMillis ?: System.currentTimeMillis()
                        )
                    )
                } else {
                    list
                }
            }
            UserStore(
                profile = decodeProfile(sp.getString(KEY_PROFILE, null)),
                legal = decodeLegal(sp.getString(KEY_LEGAL, null)),
                wardrobe = decodeWardrobeList(sp.getString(KEY_WARDROBE, null)),
                collections = decodeCollectionList(sp.getString(KEY_COLLECTIONS, null)),
                outfits = decodeOutfitList(sp.getString(KEY_OUTFITS, null)),
                saved = decodeSavedList(sp.getString(KEY_SAVED, null)),
                aiConversations = decodedConvs,
                aiMessages = decodedMsgs,
                styleBoards = decodeStyleBoards(sp.getString(KEY_STYLE_BOARDS, null)),
                recentlyViewed = decodeRecentlyViewed(sp.getString(KEY_RECENTLY_VIEWED, null)),
                calendarEntries = decodeCalendarEntries(sp.getString(KEY_CALENDAR, null)),
                wearHistory = decodeWearHistory(sp.getString(KEY_WEAR_HISTORY, null)),
                notifications = decodeNotifications(sp.getString(KEY_NOTIFICATIONS, null)),
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

    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val store = storeFor(userId)
        trySend(store.profileFlow.value)
        val db = firestore
        val registration = if (db != null && isFirebaseSessionFor(userId)) {
            val path = "users/$userId"
            db.document(path).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, path)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val mapped = snapshot.toUserProfile(store.profileFlow.value)
                    storePrefs(appContext, userId).edit()
                        .putString(KEY_PROFILE, encodeProfile(mapped))
                        .apply()
                    store.profileFlow.value = mapped
                    trySend(mapped)
                }
            }
        } else null
        val localJob = kotlinx.coroutines.CoroutineScope(Dispatchers.Main.immediate)
        // Observe local flow as primary updates
        val collector = store.profileFlow.asStateFlow()
        awaitClose {
            registration?.remove()
            localJob.let {}
        }
    }.let { fbFlow ->
        // Keep local flow reactive even when signed in locally without Firebase
        if (isFirebaseSessionFor(userId)) {
            kotlinx.coroutines.flow.merge(storeFor(userId).profileFlow.asStateFlow(), fbFlow)
        } else {
            storeFor(userId).profileFlow.asStateFlow()
        }
    }

    fun observeLegalAcceptance(userId: String): Flow<LegalAcceptance?> {
        val localFlow = storeFor(userId).legalFlow.asStateFlow()
        if (!isFirebaseSessionFor(userId)) return localFlow
        val db = firestore ?: return localFlow
        val path = "users/$userId/legalAcceptances/v1_consent"
        val fbFlow = callbackFlow {
            val reg = db.document(path).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, path)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val acceptedAt = snapshot.getTimestamp(
                        "acceptedAt",
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )?.toDate()?.time ?: System.currentTimeMillis()
                    val legal = LegalAcceptance(
                        id = snapshot.id,
                        userId = snapshot.getString("userId") ?: userId,
                        privacyPolicyVersion = snapshot.getString("privacyPolicyVersion") ?: LegalDocuments.PRIVACY_VERSION,
                        termsVersion = snapshot.getString("termsVersion") ?: LegalDocuments.TERMS_VERSION,
                        communityGuidelinesVersion = snapshot.getString("communityGuidelinesVersion") ?: LegalDocuments.GUIDELINES_VERSION,
                        cameraConsent = snapshot.getBoolean("cameraConsent") ?: false,
                        photoConsent = snapshot.getBoolean("photoConsent") ?: false,
                        locationConsent = snapshot.getBoolean("locationConsent") ?: false,
                        notificationsConsent = snapshot.getBoolean("notificationsConsent") ?: false,
                        acceptedAtMillis = acceptedAt
                    )
                    storePrefs(appContext, userId).edit().putString(KEY_LEGAL, encodeLegal(legal)).apply()
                    storeFor(userId).legalFlow.value = legal
                    trySend(legal)
                }
            }
            awaitClose { reg.remove() }
        }
        return kotlinx.coroutines.flow.merge(localFlow, fbFlow)
    }

    fun observeWardrobeItems(userId: String): Flow<List<WardrobeItem>> {
        val localFlow = storeFor(userId).wardrobeFlow.asStateFlow().map { list ->
            list.sortedByDescending { it.createdAtMillis }
        }
        if (!isFirebaseSessionFor(userId)) return localFlow
        val db = firestore ?: return localFlow
        val path = "users/$userId/wardrobeItems"
        val fbFlow = callbackFlow {
            val reg = db.collection(path).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val remote = snapshot.documents.map { doc -> doc.toWardrobeItem(userId) }
                        .sortedByDescending { it.createdAtMillis }
                    val merged = mergeById(storeFor(userId).wardrobeFlow.value, remote)
                    storePrefs(appContext, userId).edit().putString(KEY_WARDROBE, encodeWardrobeList(merged)).apply()
                    storeFor(userId).wardrobeFlow.value = merged
                    trySend(merged.sortedByDescending { it.createdAtMillis })
                }
            }
            awaitClose { reg.remove() }
        }
        return kotlinx.coroutines.flow.merge(localFlow, fbFlow)
    }

    fun observeOutfitCollections(userId: String): Flow<List<OutfitCollection>> =
        storeFor(userId).collectionsFlow.asStateFlow()

    fun observeOutfits(userId: String): Flow<List<Outfit>> {
        val localFlow = storeFor(userId).outfitsFlow.asStateFlow().map { list ->
            list.sortedByDescending { maxOf(it.updatedAtMillis, it.createdAtMillis) }
        }
        if (!isFirebaseSessionFor(userId)) return localFlow
        val db = firestore ?: return localFlow
        val path = "users/$userId/outfits"
        val fbFlow = callbackFlow {
            val reg = db.collection(path).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val remote = snapshot.documents.map { it.toOutfit(userId) }
                    val merged = mergeById(storeFor(userId).outfitsFlow.value, remote)
                    storePrefs(appContext, userId).edit().putString(KEY_OUTFITS, encodeOutfitList(merged)).apply()
                    storeFor(userId).outfitsFlow.value = merged
                    trySend(merged.sortedByDescending { maxOf(it.updatedAtMillis, it.createdAtMillis) })
                }
            }
            awaitClose { reg.remove() }
        }
        return kotlinx.coroutines.flow.merge(localFlow, fbFlow)
    }

    fun observeSavedItems(userId: String): Flow<List<SavedItem>> =
        storeFor(userId).savedFlow.asStateFlow().map { list ->
            list.sortedByDescending { it.createdAtMillis }
        }

    fun observeCalendarEntries(userId: String): Flow<List<CalendarOutfitEntry>> =
        storeFor(userId).calendarFlow.asStateFlow().map { list ->
            list.sortedBy { it.dateKey }
        }

    fun observeWearHistory(userId: String): Flow<List<OutfitWearHistory>> =
        storeFor(userId).wearHistoryFlow.asStateFlow().map { list ->
            list.sortedByDescending { it.wornAtMillis }
        }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        storeFor(userId).notificationsFlow.asStateFlow().map { list ->
            list.sortedByDescending { it.createdAtMillis }
        }

    fun observeOwnerRegistrationNotifications(requestingEmail: String?): Flow<List<OwnerRegistrationNotification>> {
        if (!requestingEmail.equals(LithfitConstants.OWNER_NOTIFY_EMAIL, ignoreCase = true)) {
            return flowOf(emptyList())
        }
        val localList = AuthRepository().getOwnerRegistrationNotifications(appContext, requestingEmail)
        val db = firestore
        val fbUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (db == null || fbUser == null || !fbUser.email.equals(LithfitConstants.OWNER_NOTIFY_EMAIL, ignoreCase = true)) {
            return flowOf(localList)
        }
        val path = "ownerNotifications"
        return callbackFlow {
            trySend(localList)
            val reg = db.collection(path).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val remote = snapshot.documents.map { doc ->
                        OwnerRegistrationNotification(
                            id = doc.id,
                            newUserId = doc.getString("newUserId").orEmpty(),
                            username = doc.getString("username").orEmpty(),
                            displayName = doc.getString("displayName").orEmpty(),
                            authMethod = doc.getString("authMethod") ?: "GOOGLE",
                            recipientOwnerEmail = doc.getString("recipientOwnerEmail") ?: LithfitConstants.OWNER_NOTIFY_EMAIL,
                            message = doc.getString("message").orEmpty(),
                            read = doc.getBoolean("read") ?: false,
                            createdAtMillis = doc.getTimestamp(
                                "createdAt",
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                    val combined = (localList + remote)
                        .distinctBy { it.id }
                        .sortedByDescending { it.createdAtMillis }
                    trySend(combined)
                }
            }
            awaitClose { reg.remove() }
        }
    }

    fun observeAiConversations(userId: String): Flow<List<AiConversation>> =
        storeFor(userId).aiConversationsFlow.asStateFlow().map { list ->
            list.sortedByDescending { it.updatedAtMillis }
        }

    fun observeAiMessages(userId: String, conversationId: String): Flow<List<AiMessage>> =
        storeFor(userId).aiMessagesFlow.asStateFlow().map { list ->
            list.filter { it.conversationId == conversationId }
                .sortedBy { it.createdAtMillis }
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

    fun observeSellers(): Flow<List<Seller>> {
        val db = firestore ?: return flowOf(emptyList())
        if (runCatching { Firebase.auth.currentUser }.getOrNull() == null) return flowOf(emptyList())
        val path = "sellers"
        return callbackFlow {
            val reg = db.collection(path)
                .whereEqualTo("active", true)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, path)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snap?.documents?.map { doc ->
                        Seller(
                            id = doc.id,
                            ownerUserId = doc.getString("ownerUserId").orEmpty(),
                            brandName = doc.getString("brandName").orEmpty(),
                            description = doc.getString("description"),
                            websiteUrl = doc.getString("websiteUrl"),
                            contactEmail = doc.getString("contactEmail"),
                            active = doc.getBoolean("active") ?: true
                        )
                    }.orEmpty()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }
    }

    fun observeCatalogueProducts(): Flow<List<CatalogueProduct>> {
        val db = firestore ?: return flowOf(emptyList())
        if (runCatching { Firebase.auth.currentUser }.getOrNull() == null) return flowOf(emptyList())
        val path = "products"
        return callbackFlow {
            val reg = db.collection(path)
                .whereEqualTo("active", true)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, path)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snap?.documents?.map { doc ->
                        CatalogueProduct(
                            id = doc.id,
                            sellerOwnerId = doc.getString("sellerOwnerId").orEmpty(),
                            sellerId = doc.getString("sellerId").orEmpty(),
                            sellerName = doc.getString("sellerName").orEmpty(),
                            storeId = doc.getString("storeId"),
                            name = doc.getString("name").orEmpty(),
                            category = doc.getString("category") ?: GarmentCategory.TOPS.name,
                            color = doc.getString("color").orEmpty(),
                            style = doc.getString("style").orEmpty(),
                            occasion = doc.getString("occasion"),
                            price = (doc.get("price") as? Number)?.toDouble() ?: 0.0,
                            currency = doc.getString("currency") ?: "USD",
                            sizes = doc.getStringList("sizes"),
                            description = doc.getString("description"),
                            availabilityStatus = doc.getString("availabilityStatus") ?: "IN_STOCK",
                            productUrl = doc.getString("productUrl"),
                            imageUrl = doc.getString("imageUrl"),
                            imageResKey = doc.getString("imageResKey"),
                            active = doc.getBoolean("active") ?: true
                        )
                    }.orEmpty()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }
    }

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

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/legalAcceptances/v1_consent"
                try {
                    val ref = firestore?.document(path)
                    if (ref != null && !ref.get().await().exists()) {
                        ref.set(
                            mapOf(
                                "userId" to uid,
                                "privacyPolicyVersion" to legal.privacyPolicyVersion,
                                "termsVersion" to legal.termsVersion,
                                "communityGuidelinesVersion" to legal.communityGuidelinesVersion,
                                "cameraConsent" to legal.cameraConsent,
                                "photoConsent" to legal.photoConsent,
                                "locationConsent" to legal.locationConsent,
                                "notificationsConsent" to legal.notificationsConsent,
                                "acceptedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, path)
                }
            }
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

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid"
                try {
                    val ref = firestore?.document(path)
                    if (ref != null) {
                        val exists = ref.get().await().exists()
                        val avatarMap = mapOf(
                            "presentation" to saved.avatarConfig.presentation,
                            "skinToneHex" to saved.avatarConfig.skinToneHex,
                            "faceShape" to saved.avatarConfig.faceShape,
                            "eyeColor" to saved.avatarConfig.eyeColor,
                            "expression" to saved.avatarConfig.expression,
                            "hairStyle" to saved.avatarConfig.hairStyle,
                            "hairColor" to saved.avatarConfig.hairColor,
                            "glassesStyle" to saved.avatarConfig.glassesStyle,
                            "topStyle" to saved.avatarConfig.topStyle,
                            "topColor" to saved.avatarConfig.topColor,
                            "bottomStyle" to saved.avatarConfig.bottomStyle,
                            "bottomColor" to saved.avatarConfig.bottomColor,
                            "shoesStyle" to saved.avatarConfig.shoesStyle,
                            "shoesColor" to saved.avatarConfig.shoesColor,
                            "accessoryStyle" to saved.avatarConfig.accessoryStyle,
                            "aestheticPreset" to saved.avatarConfig.aestheticPreset
                        )
                        val commonFields = mutableMapOf<String, Any?>(
                            "displayName" to saved.displayName,
                            "username" to saved.username,
                            "email" to saved.email?.take(120),
                            "phone" to saved.phone?.take(32),
                            "avatarUrl" to saved.avatarUrl?.take(500),
                            "profilePhotoPath" to saved.profilePhotoPath?.take(400),
                            "profilePhotoPrivate" to saved.profilePhotoPrivate,
                            "avatarStyle" to saved.avatarStyle?.take(40),
                            "avatarConfig" to avatarMap,
                            "hasPersonalPhoto" to saved.hasPersonalPhoto,
                            "guideCompleted" to saved.guideCompleted,
                            "privateProfile" to saved.privateProfile,
                            "minorAccount" to saved.minorAccount,
                            "preferredStyles" to saved.preferredStyles.take(25),
                            "personalStyleTags" to saved.personalStyleTags.take(20),
                            "preferredColors" to saved.preferredColors.take(25),
                            "frequentColors" to saved.frequentColors.take(25),
                            "neutralCombos" to saved.neutralCombos.take(12),
                            "experimentColors" to saved.experimentColors.take(25),
                            "avoidColors" to saved.avoidColors.take(25),
                            "preferredCategories" to saved.preferredCategories.take(20),
                            "preferredSilhouettes" to saved.preferredSilhouettes.take(15),
                            "enjoyedCategories" to saved.enjoyedCategories.take(20),
                            "rarelyWornCategories" to saved.rarelyWornCategories.take(20),
                            "preferredWeatherStyles" to saved.preferredWeatherStyles.take(10),
                            "preferredOccasions" to saved.preferredOccasions.take(15),
                            "preferredFootwear" to saved.preferredFootwear.take(15),
                            "preferredAccessories" to saved.preferredAccessories.take(15),
                            "lifestyleTags" to saved.lifestyleTags.take(15),
                            "budgetPreference" to saved.budgetPreference.take(40),
                            "inspirationFocus" to saved.inspirationFocus.take(20),
                            "preferredCurrency" to saved.preferredCurrency.take(8),
                            "preferredRegion" to saved.preferredRegion?.take(60),
                            "locationLabel" to saved.locationLabel?.take(80),
                            "themeMode" to saved.themeMode,
                            "language" to saved.language.take(30),
                            "reducedMotion" to saved.reducedMotion,
                            "largeReadableText" to saved.largeReadableText,
                            "notificationsEnabled" to saved.notificationsEnabled,
                            "notifOutfitRecs" to saved.notifOutfitRecs,
                            "notifDailyReminders" to saved.notifDailyReminders,
                            "notifWardrobeReminders" to saved.notifWardrobeReminders,
                            "notifSecurityAlerts" to saved.notifSecurityAlerts,
                            "notifAppUpdates" to saved.notifAppUpdates,
                            "notifProductUpdates" to saved.notifProductUpdates,
                            "locationAccessPreference" to saved.locationAccessPreference,
                            "gender" to saved.gender.take(40),
                            "age" to saved.age.take(30),
                            "height" to saved.height.take(40),
                            "bodySize" to saved.bodySize.take(40),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null }

                        if (!exists) {
                            val createMap = commonFields.toMutableMap().apply {
                                put("userId", uid)
                                put("createdAt", FieldValue.serverTimestamp())
                            }
                            ref.set(createMap).await()
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            ref.update(commonFields as Map<String, Any>).await()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, path)
                }
            }
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
            saveUserProfile(updated).getOrThrow()
        }
    }

    suspend fun setPersonalPhotoFlag(hasPhoto: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val current = storeFor(uid).profileFlow.value ?: return@runCatching
            saveUserProfile(current.copy(hasPersonalPhoto = hasPhoto)).getOrThrow()
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
            val safeCategory = GarmentCategory.safeParse(item.category).name
            val normalized = item.copy(
                id = docId,
                userId = uid,
                name = item.name.trim().take(80),
                category = safeCategory,
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
            val nextList = store.wardrobeFlow.value.filterNot { it.id == docId } + normalized
            storePrefs(appContext, uid).edit()
                .putString(KEY_WARDROBE, encodeWardrobeList(nextList))
                .apply()
            store.wardrobeFlow.value = nextList

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/wardrobeItems/$docId"
                try {
                    val ref = firestore?.document(path)
                    if (ref != null) {
                        val exists = ref.get().await().exists()
                        val updateFields = mutableMapOf<String, Any?>(
                            "wardrobeId" to normalized.wardrobeId,
                            "name" to normalized.name,
                            "category" to normalized.category,
                            "subType" to normalized.subType,
                            "color" to normalized.color,
                            "pattern" to normalized.pattern,
                            "material" to normalized.material,
                            "texture" to normalized.texture,
                            "finish" to normalized.finish,
                            "style" to normalized.style,
                            "garmentType" to normalized.garmentType,
                            "brand" to normalized.brand,
                            "notes" to normalized.notes,
                            "imagePath" to normalized.imagePath,
                            "thumbnailBase64" to normalized.thumbnailBase64,
                            "favorite" to normalized.favorite,
                            "privateItem" to normalized.privateItem,
                            "collectionTags" to normalized.collectionTags.take(10),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null }
                        if (!exists) {
                            val createFields = updateFields.toMutableMap().apply {
                                put("userId", uid)
                                put("createdAt", FieldValue.serverTimestamp())
                            }
                            ref.set(createFields).await()
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            ref.update(updateFields as Map<String, Any>).await()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, path)
                }
            }
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

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/wardrobeItems/$itemId"
                try {
                    firestore?.document(path)?.delete()?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, path)
                }
            }
        }
    }

    suspend fun createOutfitCollection(name: String, description: String? = null): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty()) { "Collection name cannot be empty." }
            val store = storeFor(uid)
            val now = System.currentTimeMillis()
            val docId = "col_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val created = OutfitCollection(
                id = docId,
                userId = uid,
                name = cleanName,
                description = description?.trim()?.take(200)?.ifBlank { null },
                createdAtMillis = now,
                updatedAtMillis = now
            )
            val nextList = store.collectionsFlow.value + created
            storePrefs(appContext, uid).edit()
                .putString(KEY_COLLECTIONS, encodeCollectionList(nextList))
                .apply()
            store.collectionsFlow.value = nextList

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/outfitCollections/$docId"
                try {
                    val data = mutableMapOf<String, Any?>(
                        "userId" to uid,
                        "name" to created.name,
                        "description" to created.description,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    firestore?.document(path)?.set(data)?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, path)
                }
            }
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
                occasion = outfit.occasion.trim().take(60).ifBlank { "Casual day" },
                vibe = outfit.vibe.trim().take(60).ifBlank { "Streetwear" },
                notes = outfit.notes?.trim()?.take(400)?.ifBlank { null },
                createdAtMillis = if (isNew) now else outfit.createdAtMillis,
                updatedAtMillis = now
            )
            val nextList = store.outfitsFlow.value.filterNot { it.id == docId } + normalized
            storePrefs(appContext, uid).edit()
                .putString(KEY_OUTFITS, encodeOutfitList(nextList))
                .apply()
            store.outfitsFlow.value = nextList

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/outfits/$docId"
                try {
                    val ref = firestore?.document(path)
                    if (ref != null) {
                        val exists = ref.get().await().exists()
                        val updateFields = mutableMapOf<String, Any?>(
                            "name" to normalized.name,
                            "occasion" to normalized.occasion,
                            "vibe" to normalized.vibe,
                            "notes" to normalized.notes,
                            "collectionId" to normalized.collectionId,
                            "topItemId" to normalized.topItemId,
                            "bottomItemId" to normalized.bottomItemId,
                            "shoesItemId" to normalized.shoesItemId,
                            "jacketItemId" to normalized.jacketItemId,
                            "accessoryIds" to normalized.accessoryIds.take(10),
                            "itemNames" to normalized.itemNames.take(15),
                            "colorPalette" to normalized.colorPalette.take(10),
                            "itemColorOverrides" to normalized.itemColorOverrides.takeIf { it.isNotEmpty() },
                            "feedbackStatus" to normalized.feedbackStatus,
                            "previewPath" to normalized.previewPath,
                            "favorite" to normalized.favorite,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null }
                        if (!exists) {
                            val createFields = updateFields.toMutableMap().apply {
                                put("userId", uid)
                                put("createdAt", FieldValue.serverTimestamp())
                            }
                            ref.set(createFields).await()
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            ref.update(updateFields as Map<String, Any>).await()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, path)
                }
            }
            docId
        }
    }

    suspend fun renameOutfit(outfitId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val clean = newName.trim().take(80)
            require(clean.isNotEmpty()) { "Outfit name cannot be empty." }
            val existing = storeFor(uid).outfitsFlow.value.firstOrNull { it.id == outfitId }
                ?: throw IllegalArgumentException("Outfit not found.")
            saveOutfit(existing.copy(name = clean)).getOrThrow()
            Unit
        }
    }

    suspend fun setOutfitFeedback(outfitId: String, feedbackStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            require(feedbackStatus == "LIKED" || feedbackStatus == "DISLIKED") { "Invalid feedback status" }
            val existing = storeFor(uid).outfitsFlow.value.firstOrNull { it.id == outfitId }
                ?: throw IllegalArgumentException("Outfit not found.")
            saveOutfit(existing.copy(feedbackStatus = feedbackStatus)).getOrThrow()
            Unit
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

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/outfits/$outfitId"
                try {
                    firestore?.document(path)?.delete()?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, path)
                }
            }
        }
    }

    suspend fun assignOutfitToCalendar(
        dateKey: String,
        outfit: Outfit,
        notes: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            require(DATE_KEY_REGEX.matches(dateKey)) { "Use YYYY-MM-DD date format." }
            val store = storeFor(uid)
            val entryId = "cal_$dateKey"
            val entry = CalendarOutfitEntry(
                id = entryId,
                userId = uid,
                dateKey = dateKey,
                outfitId = outfit.id,
                outfitName = outfit.name.take(80),
                occasion = outfit.occasion.take(60),
                vibe = outfit.vibe.take(60),
                notes = notes?.trim()?.take(300)?.ifBlank { null },
                worn = false,
                updatedAtMillis = System.currentTimeMillis()
            )
            val nextList = (store.calendarFlow.value.filterNot { it.dateKey == dateKey } + entry)
                .sortedBy { it.dateKey }
            storePrefs(appContext, uid).edit()
                .putString(KEY_CALENDAR, encodeCalendarEntries(nextList))
                .apply()
            store.calendarFlow.value = nextList

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/calendarEntries/$entryId"
                try {
                    val data = mutableMapOf<String, Any?>(
                        "userId" to uid,
                        "dateKey" to entry.dateKey,
                        "outfitId" to entry.outfitId,
                        "outfitName" to entry.outfitName,
                        "occasion" to entry.occasion,
                        "vibe" to entry.vibe,
                        "notes" to entry.notes,
                        "worn" to entry.worn,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    firestore?.document(path)?.set(data)?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, path)
                }
            }
            entryId
        }
    }

    suspend fun removeCalendarEntry(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val nextList = store.calendarFlow.value.filterNot { it.id == entryId }
            storePrefs(appContext, uid).edit()
                .putString(KEY_CALENDAR, encodeCalendarEntries(nextList))
                .apply()
            store.calendarFlow.value = nextList

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/calendarEntries/$entryId"
                try {
                    firestore?.document(path)?.delete()?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, path)
                }
            }
        }
    }

    suspend fun markCalendarEntryWorn(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val target = store.calendarFlow.value.firstOrNull { it.id == entryId }
                ?: throw IllegalArgumentException("Calendar entry not found.")
            val updated = target.copy(worn = !target.worn, updatedAtMillis = System.currentTimeMillis())
            val nextList = store.calendarFlow.value.map { if (it.id == entryId) updated else it }
            storePrefs(appContext, uid).edit()
                .putString(KEY_CALENDAR, encodeCalendarEntries(nextList))
                .apply()
            store.calendarFlow.value = nextList

            if (updated.worn) {
                val matchedOutfit = store.outfitsFlow.value.firstOrNull { it.id == target.outfitId }
                    ?: Outfit(
                        id = target.outfitId,
                        userId = uid,
                        name = target.outfitName,
                        occasion = target.occasion,
                        vibe = target.vibe
                    )
                logOutfitWorn(
                    outfit = matchedOutfit,
                    wardrobeItems = store.wardrobeFlow.value,
                    dateKey = target.dateKey,
                    notes = target.notes
                ).getOrThrow()
            }

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/calendarEntries/$entryId"
                try {
                    val data = mutableMapOf<String, Any?>(
                        "userId" to uid,
                        "dateKey" to updated.dateKey,
                        "outfitId" to updated.outfitId,
                        "outfitName" to updated.outfitName,
                        "occasion" to updated.occasion,
                        "vibe" to updated.vibe,
                        "notes" to updated.notes,
                        "worn" to updated.worn,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    firestore?.document(path)?.set(data)?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, path)
                }
            }
        }
    }

    suspend fun logOutfitWorn(
        outfit: Outfit,
        wardrobeItems: List<WardrobeItem>,
        dateKey: String,
        notes: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            require(DATE_KEY_REGEX.matches(dateKey)) { "Invalid date key." }
            val store = storeFor(uid)
            val resolvedIds = listOfNotNull(
                outfit.topItemId,
                outfit.bottomItemId,
                outfit.shoesItemId,
                outfit.jacketItemId
            ) + outfit.accessoryIds
            val distinctIds = resolvedIds.filter { it.isNotBlank() }.distinct().take(15)
            val idToName = wardrobeItems.associate { it.id to it.name }
            val resolvedNames = (distinctIds.mapNotNull { idToName[it] } + outfit.itemNames)
                .filter { it.isNotBlank() }
                .distinct()
                .take(15)

            val historyId = "wear_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val entry = OutfitWearHistory(
                id = historyId,
                userId = uid,
                outfitId = outfit.id.ifBlank { "custom_outfit" },
                outfitName = outfit.name.take(80).ifBlank { "Daily Fit" },
                occasion = outfit.occasion.take(60),
                vibe = outfit.vibe.take(60),
                itemIds = distinctIds,
                itemNames = resolvedNames,
                wornDateKey = dateKey,
                notes = notes?.trim()?.take(300)?.ifBlank { null },
                wornAtMillis = System.currentTimeMillis()
            )
            val nextHistory = (listOf(entry) + store.wearHistoryFlow.value).take(200)
            storePrefs(appContext, uid).edit()
                .putString(KEY_WEAR_HISTORY, encodeWearHistory(nextHistory))
                .apply()
            store.wearHistoryFlow.value = nextHistory

            val profile = store.profileFlow.value
            if (profile?.notificationsEnabled == true && profile.notifWardrobeReminders) {
                addAppNotificationInternal(
                    uid = uid,
                    category = "WARDROBE",
                    title = "Outfit wear logged",
                    body = "${entry.outfitName} logged for $dateKey."
                )
            }

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/outfitHistory/$historyId"
                try {
                    val data = mutableMapOf<String, Any?>(
                        "userId" to uid,
                        "outfitId" to entry.outfitId,
                        "outfitName" to entry.outfitName,
                        "occasion" to entry.occasion,
                        "vibe" to entry.vibe,
                        "itemIds" to entry.itemIds,
                        "itemNames" to entry.itemNames,
                        "wornDateKey" to entry.wornDateKey,
                        "notes" to entry.notes,
                        "wornAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    firestore?.document(path)?.set(data)?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, path)
                }
            }
            historyId
        }
    }

    suspend fun addAppNotification(category: String, title: String, body: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uid = currentUserId()
                addAppNotificationInternal(uid, category, title, body)
            }
        }

    private suspend fun addAppNotificationInternal(
        uid: String,
        category: String,
        title: String,
        body: String
    ) {
        val store = storeFor(uid)
        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val validCat = if (category in VALID_NOTIFICATION_CATEGORIES) category else "OUTFIT_REC"
        val item = AppNotification(
            id = notifId,
            userId = uid,
            category = validCat,
            title = title.trim().take(120),
            body = body.trim().take(400),
            read = false,
            createdAtMillis = System.currentTimeMillis()
        )
        val next = (listOf(item) + store.notificationsFlow.value).take(80)
        storePrefs(appContext, uid).edit()
            .putString(KEY_NOTIFICATIONS, encodeNotifications(next))
            .apply()
        store.notificationsFlow.value = next

        if (isFirebaseSessionFor(uid)) {
            val path = "users/$uid/notifications/$notifId"
            try {
                firestore?.document(path)?.set(
                    mapOf(
                        "userId" to uid,
                        "category" to item.category,
                        "title" to item.title,
                        "body" to item.body,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )?.await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, path)
            }
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val next = store.notificationsFlow.value.map {
                if (it.id == notifId) it.copy(read = true) else it
            }
            storePrefs(appContext, uid).edit()
                .putString(KEY_NOTIFICATIONS, encodeNotifications(next))
                .apply()
            store.notificationsFlow.value = next

            if (isFirebaseSessionFor(uid)) {
                val path = "users/$uid/notifications/$notifId"
                try {
                    firestore?.document(path)?.update("read", true)?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, path)
                }
            }
        }
    }

    suspend fun clearNotifications(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            storePrefs(appContext, uid).edit().putString(KEY_NOTIFICATIONS, "[]").apply()
            store.notificationsFlow.value = emptyList()
        }
    }

    suspend fun recordOwnerRegistrationToFirestore(newUser: LocalAuthUser, authMethod: String) {
        if (!isFirebaseSessionFor(newUser.uid)) return
        val db = firestore ?: return
        val notifId = "owner_reg_${newUser.uid}"
        val path = "ownerNotifications/$notifId"
        try {
            val ref = db.document(path)
            val data = mapOf(
                "newUserId" to newUser.uid,
                "username" to newUser.username.take(30),
                "displayName" to newUser.displayName.take(60),
                "authMethod" to authMethod,
                "recipientOwnerEmail" to LithfitConstants.OWNER_NOTIFY_EMAIL,
                "message" to "New user joined LITHFIT! @${newUser.username} (${newUser.displayName})",
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            ref.set(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }

    suspend fun markOwnerNotificationRead(requestingEmail: String?, notifId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!requestingEmail.equals(LithfitConstants.OWNER_NOTIFY_EMAIL, ignoreCase = true)) {
                    return@runCatching
                }
                AuthRepository().markOwnerNotificationRead(appContext, requestingEmail, notifId)
                val fbUser = runCatching { Firebase.auth.currentUser }.getOrNull()
                if (fbUser?.email.equals(LithfitConstants.OWNER_NOTIFY_EMAIL, ignoreCase = true)) {
                    val path = "ownerNotifications/$notifId"
                    try {
                        firestore?.document(path)?.update("read", true)?.await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        handleFirestoreError(e, OperationType.UPDATE, path)
                    }
                }
            }
        }

    suspend fun ensureConversation(
        conversationId: String,
        title: String,
        occasion: String,
        vibe: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val existing = store.aiConversationsFlow.value.firstOrNull { it.id == conversationId }
            if (existing == null) {
                val created = AiConversation(
                    id = conversationId,
                    userId = uid,
                    title = title.trim().ifBlank { "New Chat" }.take(60),
                    preview = "",
                    occasion = occasion,
                    vibe = vibe,
                    updatedAtMillis = System.currentTimeMillis()
                )
                val next = (listOf(created) + store.aiConversationsFlow.value).take(40)
                storePrefs(appContext, uid).edit()
                    .putString(KEY_AI_CONVERSATIONS, encodeAiConversations(next))
                    .apply()
                store.aiConversationsFlow.value = next
            }
        }
    }

    suspend fun deleteAiConversation(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val nextConvs = store.aiConversationsFlow.value.filterNot { it.id == conversationId }
            val nextMsgs = store.aiMessagesFlow.value.filterNot { it.conversationId == conversationId }
            storePrefs(appContext, uid).edit()
                .putString(KEY_AI_CONVERSATIONS, encodeAiConversations(nextConvs))
                .putString(KEY_AI_MESSAGES, encodeAiMessages(nextMsgs))
                .apply()
            store.aiConversationsFlow.value = nextConvs
            store.aiMessagesFlow.value = nextMsgs
        }
    }

    suspend fun appendAiMessage(
        conversationId: String,
        role: String,
        text: String,
        recommendedItemNames: List<String> = emptyList(),
        stylingReason: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserId()
            val store = storeFor(uid)
            val now = System.currentTimeMillis()
            val msg = AiMessage(
                id = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                conversationId = conversationId,
                userId = uid,
                role = role,
                text = text.take(8000),
                recommendedItemNames = recommendedItemNames.take(15),
                stylingReason = stylingReason?.take(1000)?.ifBlank { null },
                createdAtMillis = now
            )
            val nextList = (store.aiMessagesFlow.value + msg).takeLast(240)
            val currentConvs = store.aiConversationsFlow.value
            val existingConv = currentConvs.firstOrNull { it.id == conversationId }
            val updatedConv = if (existingConv != null) {
                val shouldAutoTitle = role == "USER" &&
                    (existingConv.title == "New Chat" || existingConv.title == "LITHFIT AI Styling")
                existingConv.copy(
                    title = if (shouldAutoTitle) text.trim().take(42).ifBlank { "New Chat" } else existingConv.title,
                    preview = text.trim().take(80),
                    updatedAtMillis = now
                )
            } else {
                AiConversation(
                    id = conversationId,
                    userId = uid,
                    title = if (role == "USER") text.trim().take(42).ifBlank { "New Chat" } else "New Chat",
                    preview = text.trim().take(80),
                    updatedAtMillis = now
                )
            }
            val nextConvs = (listOf(updatedConv) + currentConvs.filterNot { it.id == conversationId }).take(40)
            storePrefs(appContext, uid).edit()
                .putString(KEY_AI_CONVERSATIONS, encodeAiConversations(nextConvs))
                .putString(KEY_AI_MESSAGES, encodeAiMessages(nextList))
                .apply()
            store.aiConversationsFlow.value = nextConvs
            store.aiMessagesFlow.value = nextList
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
                if (isFirebaseSessionFor(uid)) {
                    val path = "users/$uid/savedItems/$docId"
                    runCatching { firestore?.document(path)?.delete()?.await() }
                }
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
                if (isFirebaseSessionFor(uid)) {
                    val path = "users/$uid/savedItems/$docId"
                    try {
                        val map = mutableMapOf<String, Any?>(
                            "userId" to uid,
                            "targetType" to item.targetType,
                            "targetId" to item.targetId,
                            "title" to item.title,
                            "subtitle" to item.subtitle,
                            "category" to item.category,
                            "collectionName" to item.collectionName,
                            "liked" to item.liked,
                            "createdAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null }
                        firestore?.document(path)?.set(map)?.await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        handleFirestoreError(e, OperationType.CREATE, path)
                    }
                }
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
            val repId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val existing = runCatching { JSONArray(sp.getString(KEY_REPORTS, "[]")) }.getOrDefault(JSONArray())
            existing.put(
                JSONObject().apply {
                    put("id", repId)
                    put("targetType", targetType)
                    put("targetId", targetId)
                    put("reason", reason.trim())
                    put("details", details?.trim().orEmpty())
                    put("createdAtMillis", System.currentTimeMillis())
                }
            )
            sp.edit().putString(KEY_REPORTS, existing.toString()).apply()

            if (isFirebaseSessionFor(uid)) {
                val path = "reports/$repId"
                try {
                    val data = mutableMapOf<String, Any?>(
                        "reporterId" to uid,
                        "targetType" to targetType.take(40),
                        "targetId" to targetId.take(128),
                        "reason" to reason.trim().take(120),
                        "details" to details?.trim()?.take(500)?.ifBlank { null },
                        "status" to "SUBMITTED",
                        "createdAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    firestore?.document(path)?.set(data)?.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, path)
                }
            }
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

    fun exportUserDataJson(userId: String): Result<String> = runCatching {
        val store = storeFor(userId)
        val profile = store.profileFlow.value
        JSONObject().apply {
            put("app", "LITHFIT")
            put("developer", "CLASTAD")
            put("exportedAtMillis", System.currentTimeMillis())
            put("userId", userId)
            put("profile", profile?.let { JSONObject(encodeProfile(it)) } ?: JSONObject.NULL)
            put("wardrobeItems", JSONArray(encodeWardrobeList(store.wardrobeFlow.value)))
            put("outfits", JSONArray(encodeOutfitList(store.outfitsFlow.value)))
            put("calendarEntries", JSONArray(encodeCalendarEntries(store.calendarFlow.value)))
            put("wearHistory", JSONArray(encodeWearHistory(store.wearHistoryFlow.value)))
            put("savedItems", JSONArray(encodeSavedList(store.savedFlow.value)))
        }.toString(2)
    }

    // --- Real Firestore Snapshot Mapping ---

    private fun DocumentSnapshot.getStringList(field: String): List<String> =
        (get(field) as? List<*>)?.mapNotNull { it as? String }.orEmpty()

    private fun DocumentSnapshot.toUserProfile(fallback: UserProfile?): UserProfile {
        val rawAvatar = get("avatarConfig") as? Map<*, *>
        val avatarCfg = if (rawAvatar != null) {
            AvatarConfig(
                presentation = (rawAvatar["presentation"] as? String) ?: "Neutral",
                skinToneHex = (rawAvatar["skinToneHex"] as? Number)?.toLong() ?: 0xFFD9A682L,
                faceShape = (rawAvatar["faceShape"] as? String) ?: "Oval",
                eyeColor = (rawAvatar["eyeColor"] as? String) ?: "Espresso",
                expression = (rawAvatar["expression"] as? String) ?: "Confident",
                hairStyle = (rawAvatar["hairStyle"] as? String) ?: "Textured Crop",
                hairColor = (rawAvatar["hairColor"] as? String) ?: "Deep Black",
                glassesStyle = (rawAvatar["glassesStyle"] as? String) ?: "None",
                topStyle = (rawAvatar["topStyle"] as? String) ?: "Boxy Hoodie",
                topColor = (rawAvatar["topColor"] as? String) ?: "Charcoal",
                bottomStyle = (rawAvatar["bottomStyle"] as? String) ?: "Wide Cargos",
                bottomColor = (rawAvatar["bottomColor"] as? String) ?: "Olive",
                shoesStyle = (rawAvatar["shoesStyle"] as? String) ?: "Low-Top Sneakers",
                shoesColor = (rawAvatar["shoesColor"] as? String) ?: "Cream",
                accessoryStyle = (rawAvatar["accessoryStyle"] as? String) ?: "Minimal Chain",
                aestheticPreset = (rawAvatar["aestheticPreset"] as? String) ?: "Streetwear"
            )
        } else fallback?.avatarConfig ?: AvatarConfig()

        return UserProfile(
            id = id,
            userId = getString("userId") ?: id,
            displayName = getString("displayName") ?: fallback?.displayName.orEmpty(),
            username = getString("username") ?: fallback?.username.orEmpty(),
            email = getString("email") ?: fallback?.email,
            phone = getString("phone") ?: fallback?.phone,
            avatarUrl = getString("avatarUrl") ?: fallback?.avatarUrl,
            profilePhotoPath = getString("profilePhotoPath") ?: fallback?.profilePhotoPath,
            profilePhotoPrivate = getBoolean("profilePhotoPrivate") ?: (fallback?.profilePhotoPrivate ?: true),
            avatarStyle = getString("avatarStyle") ?: fallback?.avatarStyle ?: "Streetwear",
            avatarConfig = avatarCfg,
            hasPersonalPhoto = getBoolean("hasPersonalPhoto") ?: (fallback?.hasPersonalPhoto ?: false),
            guideCompleted = getBoolean("guideCompleted") ?: (fallback?.guideCompleted ?: false),
            privateProfile = getBoolean("privateProfile") ?: (fallback?.privateProfile ?: true),
            minorAccount = getBoolean("minorAccount") ?: (fallback?.minorAccount ?: false),
            preferredStyles = getStringList("preferredStyles").ifEmpty { fallback?.preferredStyles.orEmpty() },
            personalStyleTags = getStringList("personalStyleTags"),
            preferredColors = getStringList("preferredColors").ifEmpty { fallback?.preferredColors.orEmpty() },
            frequentColors = getStringList("frequentColors"),
            neutralCombos = getStringList("neutralCombos"),
            experimentColors = getStringList("experimentColors"),
            avoidColors = getStringList("avoidColors"),
            preferredCategories = getStringList("preferredCategories"),
            preferredSilhouettes = getStringList("preferredSilhouettes"),
            enjoyedCategories = getStringList("enjoyedCategories"),
            rarelyWornCategories = getStringList("rarelyWornCategories"),
            preferredWeatherStyles = getStringList("preferredWeatherStyles"),
            preferredOccasions = getStringList("preferredOccasions"),
            preferredFootwear = getStringList("preferredFootwear"),
            preferredAccessories = getStringList("preferredAccessories"),
            lifestyleTags = getStringList("lifestyleTags"),
            budgetPreference = getString("budgetPreference") ?: "Balanced everyday",
            inspirationFocus = getString("inspirationFocus") ?: "All",
            preferredCurrency = getString("preferredCurrency") ?: "USD",
            preferredRegion = getString("preferredRegion"),
            locationLabel = getString("locationLabel"),
            themeMode = getString("themeMode") ?: ThemeMode.LIGHT.name,
            language = getString("language") ?: "English",
            reducedMotion = getBoolean("reducedMotion") ?: false,
            largeReadableText = getBoolean("largeReadableText") ?: false,
            notificationsEnabled = getBoolean("notificationsEnabled") ?: true,
            notifOutfitRecs = getBoolean("notifOutfitRecs") ?: true,
            notifDailyReminders = getBoolean("notifDailyReminders") ?: true,
            notifWardrobeReminders = getBoolean("notifWardrobeReminders") ?: true,
            notifSecurityAlerts = getBoolean("notifSecurityAlerts") ?: true,
            notifAppUpdates = getBoolean("notifAppUpdates") ?: false,
            notifProductUpdates = getBoolean("notifProductUpdates") ?: false,
            locationAccessPreference = getBoolean("locationAccessPreference") ?: false,
            gender = getString("gender").orEmpty(),
            age = getString("age").orEmpty(),
            height = getString("height").orEmpty(),
            bodySize = getString("bodySize").orEmpty(),
            createdAtMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
                ?: fallback?.createdAtMillis ?: System.currentTimeMillis(),
            updatedAtMillis = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
                ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toWardrobeItem(uid: String): WardrobeItem = WardrobeItem(
        id = id,
        userId = getString("userId") ?: uid,
        wardrobeId = getString("wardrobeId") ?: "default",
        name = getString("name").orEmpty(),
        category = getString("category") ?: GarmentCategory.OTHER.name,
        subType = getString("subType"),
        color = getString("color").orEmpty(),
        pattern = getString("pattern"),
        material = getString("material"),
        texture = getString("texture"),
        finish = getString("finish"),
        style = getString("style"),
        garmentType = getString("garmentType"),
        brand = getString("brand"),
        notes = getString("notes"),
        imagePath = getString("imagePath"),
        thumbnailBase64 = getString("thumbnailBase64"),
        favorite = getBoolean("favorite") ?: false,
        privateItem = getBoolean("privateItem") ?: true,
        collectionTags = getStringList("collectionTags"),
        createdAtMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: System.currentTimeMillis(),
        updatedAtMillis = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: System.currentTimeMillis()
    )

    private fun DocumentSnapshot.toOutfit(uid: String): Outfit {
        val rawOverrides = (get("itemColorOverrides") as? Map<*, *>)?.entries
            ?.mapNotNull { (k, v) ->
                val ks = k as? String
                val vs = v as? String
                if (ks != null && vs != null) ks to vs else null
            }?.toMap().orEmpty()
        return Outfit(
            id = id,
            userId = getString("userId") ?: uid,
            name = getString("name").orEmpty(),
            occasion = getString("occasion") ?: "Casual day",
            vibe = getString("vibe") ?: "Streetwear",
            notes = getString("notes"),
            collectionId = getString("collectionId"),
            topItemId = getString("topItemId"),
            bottomItemId = getString("bottomItemId"),
            shoesItemId = getString("shoesItemId"),
            jacketItemId = getString("jacketItemId"),
            accessoryIds = getStringList("accessoryIds"),
            itemNames = getStringList("itemNames"),
            colorPalette = getStringList("colorPalette"),
            itemColorOverrides = rawOverrides,
            feedbackStatus = getString("feedbackStatus"),
            previewPath = getString("previewPath"),
            favorite = getBoolean("favorite") ?: false,
            createdAtMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
                ?: System.currentTimeMillis(),
            updatedAtMillis = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
                ?: System.currentTimeMillis()
        )
    }

    private inline fun <T> mergeById(local: List<T>, remote: List<T>, idSelector: (T) -> String): List<T> {
        val map = LinkedHashMap<String, T>()
        local.forEach { map[idSelector(it)] = it }
        remote.forEach { map[idSelector(it)] = it }
        return map.values.toList()
    }

    @JvmName("mergeWardrobeById")
    private fun mergeById(local: List<WardrobeItem>, remote: List<WardrobeItem>): List<WardrobeItem> =
        mergeById(local, remote) { it.id }

    @JvmName("mergeOutfitById")
    private fun mergeById(local: List<Outfit>, remote: List<Outfit>): List<Outfit> =
        mergeById(local, remote) { it.id }

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
        put("phone", p.phone)
        put("avatarUrl", p.avatarUrl)
        put("profilePhotoPath", p.profilePhotoPath)
        put("profilePhotoPrivate", p.profilePhotoPrivate)
        put("avatarStyle", p.avatarStyle)
        put("avatarConfig", encodeAvatar(p.avatarConfig))
        put("hasPersonalPhoto", p.hasPersonalPhoto)
        put("guideCompleted", p.guideCompleted)
        put("privateProfile", p.privateProfile)
        put("minorAccount", p.minorAccount)
        put("preferredStyles", p.preferredStyles.toJsonArray())
        put("personalStyleTags", p.personalStyleTags.toJsonArray())
        put("preferredColors", p.preferredColors.toJsonArray())
        put("frequentColors", p.frequentColors.toJsonArray())
        put("neutralCombos", p.neutralCombos.toJsonArray())
        put("experimentColors", p.experimentColors.toJsonArray())
        put("avoidColors", p.avoidColors.toJsonArray())
        put("preferredCategories", p.preferredCategories.toJsonArray())
        put("preferredSilhouettes", p.preferredSilhouettes.toJsonArray())
        put("enjoyedCategories", p.enjoyedCategories.toJsonArray())
        put("rarelyWornCategories", p.rarelyWornCategories.toJsonArray())
        put("preferredWeatherStyles", p.preferredWeatherStyles.toJsonArray())
        put("preferredOccasions", p.preferredOccasions.toJsonArray())
        put("preferredFootwear", p.preferredFootwear.toJsonArray())
        put("preferredAccessories", p.preferredAccessories.toJsonArray())
        put("lifestyleTags", p.lifestyleTags.toJsonArray())
        put("budgetPreference", p.budgetPreference)
        put("inspirationFocus", p.inspirationFocus)
        put("preferredCurrency", p.preferredCurrency)
        put("preferredRegion", p.preferredRegion)
        put("locationLabel", p.locationLabel)
        put("themeMode", p.themeMode)
        put("language", p.language)
        put("reducedMotion", p.reducedMotion)
        put("largeReadableText", p.largeReadableText)
        put("notificationsEnabled", p.notificationsEnabled)
        put("notifOutfitRecs", p.notifOutfitRecs)
        put("notifDailyReminders", p.notifDailyReminders)
        put("notifWardrobeReminders", p.notifWardrobeReminders)
        put("notifSecurityAlerts", p.notifSecurityAlerts)
        put("notifAppUpdates", p.notifAppUpdates)
        put("notifProductUpdates", p.notifProductUpdates)
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
                phone = o.optString("phone").ifBlank { null },
                avatarUrl = o.optString("avatarUrl").ifBlank { null },
                profilePhotoPath = o.optString("profilePhotoPath").ifBlank { null },
                profilePhotoPrivate = o.optBoolean("profilePhotoPrivate", true),
                avatarStyle = o.optString("avatarStyle").ifBlank { "Streetwear" },
                avatarConfig = decodeAvatar(o.optJSONObject("avatarConfig")),
                hasPersonalPhoto = o.optBoolean("hasPersonalPhoto", false),
                guideCompleted = o.optBoolean("guideCompleted", false),
                privateProfile = o.optBoolean("privateProfile", true),
                minorAccount = o.optBoolean("minorAccount", false),
                preferredStyles = o.optJSONArray("preferredStyles")?.toStringList().orEmpty(),
                personalStyleTags = o.optJSONArray("personalStyleTags")?.toStringList().orEmpty(),
                preferredColors = o.optJSONArray("preferredColors")?.toStringList().orEmpty(),
                frequentColors = o.optJSONArray("frequentColors")?.toStringList().orEmpty(),
                neutralCombos = o.optJSONArray("neutralCombos")?.toStringList().orEmpty(),
                experimentColors = o.optJSONArray("experimentColors")?.toStringList().orEmpty(),
                avoidColors = o.optJSONArray("avoidColors")?.toStringList().orEmpty(),
                preferredCategories = o.optJSONArray("preferredCategories")?.toStringList().orEmpty(),
                preferredSilhouettes = o.optJSONArray("preferredSilhouettes")?.toStringList().orEmpty(),
                enjoyedCategories = o.optJSONArray("enjoyedCategories")?.toStringList().orEmpty(),
                rarelyWornCategories = o.optJSONArray("rarelyWornCategories")?.toStringList().orEmpty(),
                preferredWeatherStyles = o.optJSONArray("preferredWeatherStyles")?.toStringList().orEmpty(),
                preferredOccasions = o.optJSONArray("preferredOccasions")?.toStringList().orEmpty(),
                preferredFootwear = o.optJSONArray("preferredFootwear")?.toStringList().orEmpty(),
                preferredAccessories = o.optJSONArray("preferredAccessories")?.toStringList().orEmpty(),
                lifestyleTags = o.optJSONArray("lifestyleTags")?.toStringList().orEmpty(),
                budgetPreference = o.optString("budgetPreference", "Balanced everyday"),
                inspirationFocus = o.optString("inspirationFocus", "All"),
                preferredCurrency = o.optString("preferredCurrency", "USD"),
                preferredRegion = o.optString("preferredRegion").ifBlank { null },
                locationLabel = o.optString("locationLabel").ifBlank { null },
                themeMode = o.optString("themeMode", ThemeMode.LIGHT.name),
                language = o.optString("language", "English"),
                reducedMotion = o.optBoolean("reducedMotion", false),
                largeReadableText = o.optBoolean("largeReadableText", false),
                notificationsEnabled = o.optBoolean("notificationsEnabled", true),
                notifOutfitRecs = o.optBoolean("notifOutfitRecs", true),
                notifDailyReminders = o.optBoolean("notifDailyReminders", true),
                notifWardrobeReminders = o.optBoolean("notifWardrobeReminders", true),
                notifSecurityAlerts = o.optBoolean("notifSecurityAlerts", true),
                notifAppUpdates = o.optBoolean("notifAppUpdates", false),
                notifProductUpdates = o.optBoolean("notifProductUpdates", false),
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
                val overridesJson = o.optJSONObject("itemColorOverrides")
                val overridesMap = mutableMapOf<String, String>()
                if (overridesJson != null) {
                    val keys = overridesJson.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        overridesMap[k] = overridesJson.optString(k)
                    }
                }
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

    private fun encodeCalendarEntries(list: List<CalendarOutfitEntry>): String = JSONArray().apply {
        list.forEach { c ->
            put(
                JSONObject().apply {
                    put("id", c.id)
                    put("userId", c.userId)
                    put("dateKey", c.dateKey)
                    put("outfitId", c.outfitId)
                    put("outfitName", c.outfitName)
                    put("occasion", c.occasion)
                    put("vibe", c.vibe)
                    put("notes", c.notes)
                    put("worn", c.worn)
                    put("updatedAtMillis", c.updatedAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeCalendarEntries(raw: String?): List<CalendarOutfitEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                CalendarOutfitEntry(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    dateKey = o.optString("dateKey"),
                    outfitId = o.optString("outfitId"),
                    outfitName = o.optString("outfitName"),
                    occasion = o.optString("occasion", "Casual day"),
                    vibe = o.optString("vibe", "Streetwear"),
                    notes = o.optString("notes").ifBlank { null },
                    worn = o.optBoolean("worn", false),
                    updatedAtMillis = o.optLong("updatedAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeWearHistory(list: List<OutfitWearHistory>): String = JSONArray().apply {
        list.forEach { h ->
            put(
                JSONObject().apply {
                    put("id", h.id)
                    put("userId", h.userId)
                    put("outfitId", h.outfitId)
                    put("outfitName", h.outfitName)
                    put("occasion", h.occasion)
                    put("vibe", h.vibe)
                    put("itemIds", h.itemIds.toJsonArray())
                    put("itemNames", h.itemNames.toJsonArray())
                    put("wornDateKey", h.wornDateKey)
                    put("notes", h.notes)
                    put("wornAtMillis", h.wornAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeWearHistory(raw: String?): List<OutfitWearHistory> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                OutfitWearHistory(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    outfitId = o.optString("outfitId"),
                    outfitName = o.optString("outfitName"),
                    occasion = o.optString("occasion", "Casual day"),
                    vibe = o.optString("vibe", "Streetwear"),
                    itemIds = o.optJSONArray("itemIds")?.toStringList().orEmpty(),
                    itemNames = o.optJSONArray("itemNames")?.toStringList().orEmpty(),
                    wornDateKey = o.optString("wornDateKey"),
                    notes = o.optString("notes").ifBlank { null },
                    wornAtMillis = o.optLong("wornAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeNotifications(list: List<AppNotification>): String = JSONArray().apply {
        list.forEach { n ->
            put(
                JSONObject().apply {
                    put("id", n.id)
                    put("userId", n.userId)
                    put("category", n.category)
                    put("title", n.title)
                    put("body", n.body)
                    put("read", n.read)
                    put("createdAtMillis", n.createdAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeNotifications(raw: String?): List<AppNotification> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                AppNotification(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    category = o.optString("category", "OUTFIT_REC"),
                    title = o.optString("title"),
                    body = o.optString("body"),
                    read = o.optBoolean("read", false),
                    createdAtMillis = o.optLong("createdAtMillis", 0L)
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

    private fun encodeAiConversations(list: List<AiConversation>): String = JSONArray().apply {
        list.forEach { c ->
            put(
                JSONObject().apply {
                    put("id", c.id)
                    put("userId", c.userId)
                    put("title", c.title)
                    put("preview", c.preview)
                    put("occasion", c.occasion)
                    put("vibe", c.vibe)
                    put("updatedAtMillis", c.updatedAtMillis)
                }
            )
        }
    }.toString()

    private fun decodeAiConversations(raw: String?): List<AiConversation> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                AiConversation(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    title = o.optString("title", "New Chat"),
                    preview = o.optString("preview"),
                    occasion = o.optString("occasion", "Casual day"),
                    vibe = o.optString("vibe", "Streetwear"),
                    updatedAtMillis = o.optLong("updatedAtMillis", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeAiMessages(list: List<AiMessage>): String = JSONArray().apply {
        list.forEach { m ->
            put(
                JSONObject().apply {
                    put("id", m.id)
                    put("conversationId", m.conversationId)
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
                    conversationId = o.optString("conversationId").ifBlank { "main_stylist" },
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
        private const val KEY_AI_CONVERSATIONS = "ai_conversations_json"
        private const val KEY_AI_MESSAGES = "ai_messages_json"
        private const val KEY_REPORTS = "reports_json"
        private const val KEY_STYLE_BOARDS = "style_boards_json"
        private const val KEY_RECENTLY_VIEWED = "recently_viewed_json"
        private const val KEY_CALENDAR = "calendar_entries_json"
        private const val KEY_WEAR_HISTORY = "wear_history_json"
        private const val KEY_NOTIFICATIONS = "notifications_json"
        private const val KEY_FOLLOWED_AUTHORS = "followed_authors_set"
        private const val KEY_BLOCKED_AUTHORS = "blocked_authors_set"
        private const val KEY_COMMUNITY_POSTS = "community_posts_json"

        private val DATE_KEY_REGEX = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
        private val VALID_NOTIFICATION_CATEGORIES = setOf(
            "OUTFIT_REC", "DAILY_REMINDER", "WARDROBE", "SECURITY", "APP_UPDATE", "PRODUCT"
        )

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

        fun computeWardrobeInsights(
            wardrobe: List<WardrobeItem>,
            outfits: List<Outfit>,
            wearHistory: List<OutfitWearHistory>,
            calendarEntries: List<CalendarOutfitEntry>
        ): WardrobeInsightsSummary {
            val wearCountByItemName = mutableMapOf<String, Int>()
            val idToName = wardrobe.associate { it.id to it.name }

            wearHistory.forEach { history ->
                val resolvedNames = (history.itemIds.mapNotNull { idToName[it] } + history.itemNames)
                    .filter { it.isNotBlank() }
                    .distinct()
                resolvedNames.forEach { name ->
                    wearCountByItemName[name] = (wearCountByItemName[name] ?: 0) + 1
                }
            }

            val mostUsed = wearCountByItemName.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }

            val leastUsed = wardrobe
                .sortedBy { w -> wearCountByItemName[w.name] ?: 0 }
                .filter { w -> (wearCountByItemName[w.name] ?: 0) == 0 }
                .take(5)
                .map { it.name }

            val recentlyAdded = wardrobe
                .sortedByDescending { it.createdAtMillis }
                .take(4)
                .map { it.name }

            val frequentColors = wardrobe
                .filter { it.color.isNotBlank() }
                .groupingBy { it.color.trim().replaceFirstChar { ch -> ch.uppercase() } }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }

            val topCategories = wardrobe
                .groupingBy { it.categoryEnum().label }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }

            return WardrobeInsightsSummary(
                totalItems = wardrobe.size,
                favoriteItemsCount = wardrobe.count { it.favorite },
                savedOutfitsCount = outfits.size,
                favoriteOutfitsCount = outfits.count { it.favorite || it.feedbackStatus == "LIKED" },
                totalWearsLogged = wearHistory.size,
                calendarPlannedCount = calendarEntries.size,
                mostUsedItems = mostUsed,
                leastUsedItems = leastUsed,
                recentlyAddedItems = recentlyAdded,
                frequentColors = frequentColors,
                topCategories = topCategories
            )
        }
    }
}
