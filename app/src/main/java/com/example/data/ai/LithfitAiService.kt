package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AvatarConfig
import com.example.data.model.CatalogueProduct
import com.example.data.model.GarmentCategory
import com.example.data.model.Outfit
import com.example.data.model.PreGenerationOptions
import com.example.data.model.UserProfile
import com.example.data.model.WardrobeItem
import com.example.data.remote.WeatherSnapshot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

enum class StylistIntent {
    GENERAL_QUESTION,
    OUTFIT_GENERATION
}

data class RecognizedGarmentAttributes(
    val name: String,
    val category: GarmentCategory,
    val color: String,
    val pattern: String,
    val material: String,
    val style: String,
    val garmentType: String
)

data class StructuredOutfitSuggestion(
    val outfitTitle: String,
    val topPiece: String,
    val bottomPiece: String,
    val footwearPiece: String,
    val jacketPiece: String,
    val accessories: List<String>,
    val colorPalette: List<String>,
    val stylingRationale: String,
    val ownedItemsUsed: List<String>,
    val alternativeStylingTip: String
)

data class CachedStylistResponse(
    val visibleText: String,
    val structuredOutfit: StructuredOutfitSuggestion?,
    val intent: StylistIntent
)

class LithfitAiService {

    private val responseCache = object : LinkedHashMap<String, CachedStylistResponse>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedStylistResponse>?): Boolean {
            return size > 24
        }
    }

    private val generalQaModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            systemInstruction = content {
                text(
                    """
                    You are LITHFIT AI, a sharp, inclusive fashion specialist inside LITHFIT by CLASTAD.
                    The user is asking a direct fashion knowledge, colour-pairing, or styling question (NOT requesting a full outfit build unless they follow up later).
                    Rules:
                    1. Answer directly and concisely in 2–5 short, easy-to-scan lines or bullets.
                    2. Speak fluently about all fashion cultures (Y2K, Streetwear, Old money, Grunge, Afro-inspired, Techwear, Formal, Minimalist, Vintage, Casual, Sporty, Contemporary, Experimental).
                    3. Stay age-appropriate, body-neutral, and encouraging. Never rank attractiveness or comment negatively on bodies.
                    4. Never mention Google, Gemini, AI Studio, or underlying AI providers. You are LITHFIT AI.
                    """.trimIndent()
                )
            }
        )
    }

    private val stylistTextModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are LITHFIT AI, the personal fashion stylist inside the LITHFIT app by CLASTAD.
                    Your tone is modern, inclusive, friendly, welcoming, and fashion-forward for teenagers and adults across all styles and genders.

                    Supported Fashion Identities:
                    - Y2K, Streetwear, Casual, Old money, Minimalist, Vintage, Grunge, Techwear, Formal, Smart casual, Sporty, Afro-inspired fashion, Contemporary fashion, Experimental fashion.

                    Strict Integrity & Safety Rules:
                    1. Prioritize items the user already owns in their wardrobe before suggesting new items.
                    2. Respect the user's preferred colours, colours to avoid, footwear, accessories, budget, inspiration focus, and weather style preferences.
                    3. Learn from liked outfits and avoid repeating disliked patterns.
                    4. Tailor choices to live weather if provided; never invent live weather.
                    5. Stay 100% age-appropriate, body-neutral, and positive. Never restrict looks by gender stereotypes.
                    6. Never fabricate store stock, prices, or links unless listed in the verified catalogue.
                    7. Never mention Google, Gemini, AI Studio, or underlying model providers. You are LITHFIT AI.

                    Single-Pass Output Contract (CRITICAL):
                    Write clear, scannable outfit advice (Title, Key Pieces, Colourway, Footwear, Accessories, Why It Works).
                    Then, on a new line at the very end of your response, append ONE compact JSON object wrapped exactly in:
                    [[LITHFIT_OUTFIT_JSON]]{"outfitTitle":"...","topPiece":"...","bottomPiece":"...","footwearPiece":"...","jacketPiece":"...","accessories":["..."],"colorPalette":["..."],"stylingRationale":"...","ownedItemsUsed":["..."],"alternativeStylingTip":"..."}[[/LITHFIT_OUTFIT_JSON]]
                    Do not wrap that marker in markdown code fences.
                    """.trimIndent()
                )
            }
        )
    }

    private val garmentRecognitionModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string("Short descriptive garment name, e.g. Washed Black Boxy Hoodie"),
                "category" to Schema.enumeration(
                    values = GarmentCategory.entries.map { it.name },
                    description = "Closest garment category"
                ),
                "color" to Schema.string("Primary color, e.g. Charcoal, Ivory, Navy, Stone"),
                "pattern" to Schema.string("Pattern such as Solid, Striped, Plaid, Graphic, Ribbed"),
                "material" to Schema.string("Likely fabric material if reasonably identifiable, else Cotton or Unknown"),
                "style" to Schema.string("Fashion style such as Minimalist, Streetwear, Smart casual, Vintage, Grunge, Y2K"),
                "garmentType" to Schema.string("Specific garment silhouette, e.g. Tailored Blazer, Cargo Trousers, Low-top Sneakers")
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val outfitStructureModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "outfitTitle" to Schema.string("Short editorial outfit title"),
                "topPiece" to Schema.string("Top garment description"),
                "bottomPiece" to Schema.string("Bottom garment description"),
                "footwearPiece" to Schema.string("Footwear suggestion"),
                "jacketPiece" to Schema.string("Jacket or outer layer, or None if warm"),
                "accessories" to Schema.array(Schema.string(), "1-3 coordinating accessories"),
                "colorPalette" to Schema.array(Schema.string(), "2-4 colors in the outfit"),
                "stylingRationale" to Schema.string("Why this outfit suits the occasion, vibe, and weather"),
                "ownedItemsUsed" to Schema.array(Schema.string(), "Exact names of owned wardrobe items included"),
                "alternativeStylingTip" to Schema.string("One quick swap or alternative styling option")
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val outfitResearchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are a senior fashion editorial visual director researching the user's exact outfit prompt before image rendering.
                    Rules:
                    1. STRICT GARMENT & COLOR LOCK: NEVER alter, swap, or substitute explicitly requested garments, cuts, silhouettes, or colors.
                       - Keep top colors strictly on the top garment and bottom colors strictly on the bottom garment (e.g., for "pink baggy trouser and a black chromheart shirt", the upper half MUST be a BLACK short-sleeve boxy graphic streetwear t-shirt with Gothic Cross prints, and the lower half MUST be PINK ultra-baggy wide-leg cargo/lounge trousers — NEVER turn a t-shirt/shirt into a hoodie, NEVER turn baggy trousers into skinny jeans, and NEVER turn trousers pink-on-top/black-on-bottom).
                    2. Deeply interpret aesthetic & streetwear references accurately: Chrome Hearts aesthetic = washed heavy jersey tee, Gothic Cross & tonal back/chest motifs, layered sterling silver hardware; Old Money = cable knits, tailored double pleats, suede loafers; Y2K = low-rise wide parachute cargos, metallic accents; Techwear = articulated ripstop utility pockets and weather-sealed seams.
                    3. Apply the user's selected gender styling, outfit size, fit silhouette, footwear direction, and accessories faithfully.
                    4. Output a dense, crystal-clear visual specification (6–9 sentences max) covering head-to-toe camera framing, model styling, exact upper garment, exact lower garment & drape, footwear, accessories, studio lighting, and fabric textures. No markdown or preamble.
                    """.trimIndent()
                )
            }
        )
    }

    private val outfitImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    fun getCachedResponse(cacheKey: String): CachedStylistResponse? = synchronized(responseCache) {
        responseCache[cacheKey]
    }

    fun putCachedResponse(cacheKey: String, value: CachedStylistResponse) {
        synchronized(responseCache) {
            responseCache[cacheKey] = value
        }
    }

    fun buildCacheKey(
        intent: StylistIntent,
        prompt: String,
        occasion: String,
        vibe: String,
        wardrobeCount: Int,
        weatherLabel: String?
    ): String {
        val normPrompt = prompt.trim().lowercase().replace(Regex("\\s+"), " ")
        return "${intent.name}|$normPrompt|${occasion.lowercase()}|${vibe.lowercase()}|w=$wardrobeCount|${weatherLabel.orEmpty()}"
    }

    fun streamStylistResponse(
        intent: StylistIntent,
        userPrompt: String,
        occasion: String,
        vibe: String,
        profile: UserProfile?,
        wardrobe: List<WardrobeItem>,
        weather: WeatherSnapshot?,
        catalogue: List<CatalogueProduct>,
        conversationHistory: List<Pair<String, String>>,
        savedOutfits: List<Outfit> = emptyList()
    ): Flow<String> = flow {
        val buffer = StringBuilder()
        if (intent == StylistIntent.GENERAL_QUESTION) {
            val quickPrompt = buildGeneralQaContext(userPrompt, profile, conversationHistory)
            withTimeout(30_000L) {
                generalQaModel.generateContentStream(quickPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        buffer.append(piece)
                        emit(buffer.toString())
                    }
                }
            }
        } else {
            val contextPrompt = buildStylistContext(
                userPrompt = userPrompt,
                occasion = occasion,
                vibe = vibe,
                profile = profile,
                wardrobe = wardrobe,
                weather = weather,
                catalogue = catalogue,
                history = conversationHistory,
                savedOutfits = savedOutfits
            )
            withTimeout(50_000L) {
                stylistTextModel.generateContentStream(contextPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        buffer.append(piece)
                        emit(buffer.toString())
                    }
                }
            }
        }
        if (buffer.isBlank()) {
            throw IllegalStateException("LITHFIT AI returned an empty response. Please try again.")
        }
    }

    suspend fun resolveStructuredOutfit(
        rawResponseText: String,
        occasion: String,
        vibe: String,
        wardrobe: List<WardrobeItem>
    ): StructuredOutfitSuggestion? {
        parseInlineStructuredOutfit(rawResponseText)?.let { return it }
        return extractStructuredOutfit(
            stylistText = stripInlineOutfitMarker(rawResponseText),
            occasion = occasion,
            vibe = vibe,
            wardrobe = wardrobe
        ).getOrNull()
    }

    suspend fun extractStructuredOutfit(
        stylistText: String,
        occasion: String,
        vibe: String,
        wardrobe: List<WardrobeItem>
    ): Result<StructuredOutfitSuggestion> {
        return try {
            val ownedList = wardrobe.take(20).joinToString(", ") { "${it.name} (${it.color} ${it.category})" }
                .ifBlank { "None added yet" }
            val prompt = """
                Convert the styling advice below into a clean structured outfit breakdown.
                Occasion: $occasion
                Vibe: $vibe
                User owned wardrobe: $ownedList

                Styling response:
                $stylistText
            """.trimIndent()

            val response = withTimeout(30_000L) {
                outfitStructureModel.generateContent(prompt)
            }
            val raw = response.text ?: return Result.failure(IllegalStateException("Empty outfit structure"))
            val parsed = parseJsonToStructuredOutfit(raw, vibe)
                ?: return Result.failure(IllegalStateException("Invalid outfit JSON"))
            Result.success(parsed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Structured outfit extraction failed", e)
            Result.failure(e)
        }
    }

    suspend fun recognizeGarmentFromBitmap(bitmap: Bitmap): Result<RecognizedGarmentAttributes> {
        return try {
            val response = withTimeout(45_000L) {
                garmentRecognitionModel.generateContent(
                    content {
                        image(bitmap)
                        text(
                            "Identify the primary clothing garment or footwear/accessory in this image. " +
                                "Classify its category, color, pattern, likely material if visible, fashion style, and garment type."
                        )
                    }
                )
            }
            val raw = response.text ?: return Result.failure(IllegalStateException("No garment recognition output returned."))
            val json = JSONObject(raw)
            Result.success(
                RecognizedGarmentAttributes(
                    name = json.optString("name", "Clothing Item"),
                    category = GarmentCategory.safeParse(json.optString("category")),
                    color = json.optString("color", "Neutral"),
                    pattern = json.optString("pattern", "Solid"),
                    material = json.optString("material", "Cotton blend"),
                    style = json.optString("style", "Minimalist"),
                    garmentType = json.optString("garmentType", "Garment")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Garment analysis timed out. You can still enter details manually."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Garment recognition failed", e)
            Result.failure(e)
        }
    }

    suspend fun researchOutfitVisualSpec(
        rawPrompt: String,
        items: List<String>,
        colors: List<String>,
        occasion: String,
        vibe: String,
        profile: UserProfile?,
        preGenOptions: PreGenerationOptions,
        hasUserPhoto: Boolean
    ): String {
        val deterministicBrief = buildAccurateVisualBriefPrompt(
            rawPrompt = rawPrompt,
            items = items,
            colors = colors,
            occasion = occasion,
            vibe = vibe,
            profile = profile,
            preGenOptions = preGenOptions,
            hasUserPhoto = hasUserPhoto
        )
        return try {
            val response = withTimeout(35_000L) {
                outfitResearchModel.generateContent(
                    """
                    Study the request and produce a faithful, accurate visual rendering specification for an editorial fashion photograph.
                    Base brief:
                    $deterministicBrief
                    """.trimIndent()
                )
            }
            response.text?.trim()?.takeIf { it.length > 40 } ?: deterministicBrief
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Outfit visual research fell back to deterministic spec", e)
            deterministicBrief
        }
    }

    suspend fun generateAccurateOutfitImage(
        visualSpec: String,
        userPhotoBitmap: Bitmap? = null
    ): Result<Bitmap> {
        return try {
            val response = withTimeout(90_000L) {
                if (userPhotoBitmap != null) {
                    outfitImageModel.generateContent(
                        content {
                            image(userPhotoBitmap)
                            text(
                                """
                                Virtual try-on editorial fashion photograph using the person shown in the uploaded reference image.
                                CRITICAL IDENTITY RULE: Keep the exact person from the uploaded image (same face, skin tone, hair, build, and expression) and dress them from head to toe in the exact outfit below.
                                Do not swap garment types or colors. Keep cuts accurate (baggy stays truly wide/baggy, t-shirt stays a t-shirt, not a hoodie).
                                Outfit specification:
                                $visualSpec
                                """.trimIndent()
                            )
                        }
                    )
                } else {
                    outfitImageModel.generateContent(
                        content {
                            text(
                                """
                                Realistic high-resolution full-body editorial fashion photograph on a clean neutral studio cyclorama backdrop.
                                CRITICAL GARMENT ACCURACY RULE: Follow every garment type, color, fit silhouette (baggy vs slim), print motif, footwear, and accessory strictly without substitution.
                                Outfit specification:
                                $visualSpec
                                Sharp photography, natural fabric folds, zero text overlays, zero watermarks, non-pixelated crisp realistic human proportions.
                                """.trimIndent()
                            )
                        }
                    )
                }
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()

            val bmp = imagePart?.image
            if (bmp != null) {
                Result.success(bmp)
            } else {
                Result.failure(
                    IllegalStateException("Could not render this outfit image right now. Please refine your prompt or try again.")
                )
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Outfit rendering took longer than 90 seconds. Please tap Retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI outfit image generation failed", e)
            Result.failure(
                IllegalStateException(e.localizedMessage ?: "AI image generation failed. Check connection and tap Retry.")
            )
        }
    }

    private fun buildGeneralQaContext(
        userPrompt: String,
        profile: UserProfile?,
        history: List<Pair<String, String>>
    ): String {
        val styleFocus = profile?.preferredStyles?.take(5)?.joinToString().orEmpty()
        val bodyContext = listOfNotNull(
            profile?.gender?.takeIf { it.isNotBlank() }?.let { "Gender: $it" },
            profile?.age?.takeIf { it.isNotBlank() }?.let { "Age: $it" },
            profile?.height?.takeIf { it.isNotBlank() }?.let { "Height: $it" },
            profile?.bodySize?.takeIf { it.isNotBlank() }?.let { "Size/Fit: $it" }
        ).joinToString(" | ")
        val recentTurns = history.takeLast(4).joinToString("\n") { (r, t) -> "$r: ${t.take(220)}" }
        return buildString {
            if (styleFocus.isNotBlank()) appendLine("User's liked styles: $styleFocus")
            if (bodyContext.isNotBlank()) appendLine("User styling context: $bodyContext")
            if (recentTurns.isNotBlank()) appendLine("Recent context:\n$recentTurns")
            appendLine("Question: $userPrompt")
        }.trim()
    }

    private fun buildStylistContext(
        userPrompt: String,
        occasion: String,
        vibe: String,
        profile: UserProfile?,
        wardrobe: List<WardrobeItem>,
        weather: WeatherSnapshot?,
        catalogue: List<CatalogueProduct>,
        history: List<Pair<String, String>>,
        savedOutfits: List<Outfit> = emptyList()
    ): String {
        val prefsText = if (profile != null) {
            buildString {
                append("Focus: ${profile.inspirationFocus}; ")
                if (profile.gender.isNotBlank()) append("Gender: ${profile.gender}; ")
                if (profile.age.isNotBlank()) append("Age: ${profile.age}; ")
                if (profile.height.isNotBlank()) append("Height: ${profile.height}; ")
                if (profile.bodySize.isNotBlank()) append("Body Size: ${profile.bodySize}; ")
                append("Styles: ${profile.preferredStyles.take(6).joinToString().ifBlank { "Open" }}; ")
                append("Likes colors: ${profile.preferredColors.take(6).joinToString().ifBlank { "Open" }}; ")
                append("Avoids colors: ${profile.avoidColors.take(4).joinToString().ifBlank { "None" }}; ")
                append("Budget: ${profile.budgetPreference}")
            }
        } else "Open preferences"

        val wardrobeText = if (wardrobe.isNotEmpty()) {
            wardrobe.take(18).joinToString("\n") {
                val sub = it.subType?.takeIf { s -> s.isNotBlank() } ?: it.category
                "- ${it.name} [$sub, ${it.color}${it.style?.let { st -> ", $st" }.orEmpty()}]"
            }
        } else {
            "No personal wardrobe items uploaded yet."
        }

        val likedOutfits = savedOutfits.filter { it.feedbackStatus.equals("LIKED", ignoreCase = true) }
            .take(3).joinToString("; ") { "${it.name} (${it.vibe})" }.ifBlank { "None" }
        val dislikedOutfits = savedOutfits.filter { it.feedbackStatus.equals("DISLIKED", ignoreCase = true) }
            .take(3).joinToString("; ") { "${it.name} (${it.vibe})" }.ifBlank { "None" }

        val weatherText = if (weather != null) {
            "${weather.locationName}: ${weather.temperatureC.toInt()}°C, ${weather.conditionLabel}, Rain ${weather.rainProbabilityPct}%"
        } else {
            "Unavailable"
        }

        val catalogueText = if (catalogue.isNotEmpty()) {
            catalogue.take(6).joinToString("; ") { "${it.name} (${it.sellerName}, ${it.currency} ${it.price.toInt()})" }
        } else "None"

        val historyText = history.takeLast(5).joinToString("\n") { (role, text) ->
            "$role: ${text.take(260)}"
        }

        return """
            Occasion: $occasion | Vibe: $vibe
            Profile: $prefsText
            Feedback Memory -> Liked: $likedOutfits | Disliked: $dislikedOutfits
            Weather: $weatherText
            Wardrobe:
            $wardrobeText
            Verified Catalogue: $catalogueText
            History:
            $historyText
            Request: $userPrompt
        """.trimIndent()
    }

    companion object {
        private const val TAG = "LithfitAiService"
        const val OUTFIT_MARKER_START = "[[LITHFIT_OUTFIT_JSON]]"
        const val OUTFIT_MARKER_END = "[[/LITHFIT_OUTFIT_JSON]]"

        private val OUTFIT_KEYWORDS = listOf(
            "outfit", "fit check", "wear", "style me", "dress me", "create a", "build a",
            "recommend", "look for", "what should i wear", "pair these", "match with",
            "swap the", "change the", "lighter version", "dressier", "more casual",
            "birthday", "party", "dinner", "date", "school outfit", "interview", "formal",
            "casual fit", "streetwear fit", "y2k fit", "grunge fit"
        )

        private val GENERAL_QUESTION_PREFIXES = listOf(
            "what is ", "what's ", "what does ", "meaning of", "explain ", "define ",
            "difference between", "how do i match", "what colours go", "what colors go",
            "what goes with", "which shoes go", "do brown shoes", "is y2k", "tell me about",
            "why does", "how to care", "fashion tip"
        )

        fun classifyIntent(rawPrompt: String): StylistIntent {
            val lower = rawPrompt.trim().lowercase()
            if (lower.isEmpty()) return StylistIntent.GENERAL_QUESTION

            if (GENERAL_QUESTION_PREFIXES.any { lower.startsWith(it) || it in lower } &&
                !lower.contains("create an outfit") &&
                !lower.contains("build me") &&
                !lower.contains("style me") &&
                !lower.contains("what should i wear")
            ) {
                return StylistIntent.GENERAL_QUESTION
            }

            if (OUTFIT_KEYWORDS.any { it in lower }) {
                return StylistIntent.OUTFIT_GENERATION
            }

            return if (lower.endsWith("?") && lower.length < 72) {
                StylistIntent.GENERAL_QUESTION
            } else {
                StylistIntent.OUTFIT_GENERATION
            }
        }

        fun stripInlineOutfitMarker(raw: String): String {
            val startIdx = raw.indexOf(OUTFIT_MARKER_START)
            if (startIdx >= 0) {
                return raw.substring(0, startIdx).trimEnd()
            }
            // Prevent partial marker brackets from flashing at the end while streaming
            val partialBracketIdx = raw.lastIndexOf("[[")
            if (partialBracketIdx >= 0 && raw.length - partialBracketIdx <= OUTFIT_MARKER_START.length + 4) {
                val tail = raw.substring(partialBracketIdx)
                if (OUTFIT_MARKER_START.startsWith(tail)) {
                    return raw.substring(0, partialBracketIdx).trimEnd()
                }
            }
            return raw
        }

        fun parseInlineStructuredOutfit(raw: String, fallbackVibe: String = "Streetwear"): StructuredOutfitSuggestion? {
            val startIdx = raw.indexOf(OUTFIT_MARKER_START)
            if (startIdx < 0) return null
            val jsonStart = startIdx + OUTFIT_MARKER_START.length
            val endIdx = raw.indexOf(OUTFIT_MARKER_END, startIndex = jsonStart)
            val jsonStr = (if (endIdx > jsonStart) raw.substring(jsonStart, endIdx) else raw.substring(jsonStart)).trim()
            if (!jsonStr.startsWith("{")) return null
            return parseJsonToStructuredOutfit(jsonStr, fallbackVibe)
        }

        private fun parseJsonToStructuredOutfit(rawJson: String, fallbackVibe: String): StructuredOutfitSuggestion? {
            return try {
                val json = JSONObject(rawJson)
                val accessoriesArr = json.optJSONArray("accessories")
                val paletteArr = json.optJSONArray("colorPalette")
                val ownedArr = json.optJSONArray("ownedItemsUsed")
                StructuredOutfitSuggestion(
                    outfitTitle = json.optString("outfitTitle", "$fallbackVibe Look"),
                    topPiece = json.optString("topPiece", "Relaxed tee"),
                    bottomPiece = json.optString("bottomPiece", "Wide-leg trousers"),
                    footwearPiece = json.optString("footwearPiece", "Low-top sneakers"),
                    jacketPiece = json.optString("jacketPiece", "None"),
                    accessories = if (accessoriesArr != null) (0 until accessoriesArr.length()).mapNotNull { accessoriesArr.optString(it).takeIf { s -> s.isNotBlank() } } else emptyList(),
                    colorPalette = if (paletteArr != null) (0 until paletteArr.length()).mapNotNull { paletteArr.optString(it).takeIf { s -> s.isNotBlank() } } else listOf("Charcoal", "Cream"),
                    stylingRationale = json.optString("stylingRationale", ""),
                    ownedItemsUsed = if (ownedArr != null) (0 until ownedArr.length()).mapNotNull { ownedArr.optString(it).takeIf { s -> s.isNotBlank() } } else emptyList(),
                    alternativeStylingTip = json.optString("alternativeStylingTip", "")
                )
            } catch (_: Exception) {
                null
            }
        }

        fun buildAccurateVisualBriefPrompt(
            rawPrompt: String,
            items: List<String>,
            colors: List<String>,
            occasion: String,
            vibe: String,
            profile: UserProfile?,
            preGenOptions: PreGenerationOptions,
            hasUserPhoto: Boolean = false
        ): String {
            val cleanPrompt = rawPrompt.trim()
            val cleanItems = items.filter { it.isNotBlank() && !it.equals("None", ignoreCase = true) }
            val cleanColors = colors.filter { it.isNotBlank() }
            val demographicNote = buildList {
                val g = preGenOptions.gender.ifBlank { profile?.gender.orEmpty() }.ifBlank { "Unisex" }
                add("Model Gender Presentation: $g")
                add("Outfit Size: ${preGenOptions.outfitSize}")
                add("Fit & Silhouette: ${preGenOptions.fitSilhouette}")
                profile?.age?.takeIf { it.isNotBlank() }?.let { add("Age Group: $it") }
                profile?.height?.takeIf { it.isNotBlank() }?.let { add("Height Proportion: $it") }
                profile?.bodySize?.takeIf { it.isNotBlank() }?.let { add("Body Build: $it") }
            }.joinToString(" | ")

            return buildString {
                appendLine("USER EXACT PROMPT: ${cleanPrompt.ifBlank { "$vibe look for $occasion" }}")
                if (cleanItems.isNotEmpty()) appendLine("REQUIRED GARMENTS: ${cleanItems.joinToString(", ")}")
                if (cleanColors.isNotEmpty()) appendLine("COLOR PALETTE: ${cleanColors.joinToString(", ")}")
                appendLine("OCCASION & VIBE: $occasion — $vibe")
                appendLine("FIT & PREFERENCES: $demographicNote")
                appendLine("FOOTWEAR DIRECTION: ${preGenOptions.footwearVibe}")
                appendLine("ACCESSORIES DIRECTION: ${preGenOptions.accessoriesVibe}")
                if (hasUserPhoto) {
                    appendLine("VIRTUAL TRY-ON: Preserve the exact person's identity from the uploaded reference photo while wearing every exact garment above.")
                }
                appendLine("NEGATIVE CONSTRAINTS: Do NOT turn a t-shirt/shirt into a hoodie. Do NOT turn baggy/wide trousers into skinny jeans. Preserve exact requested colors per garment without swapping top and bottom hues.")
            }.trim()
        }

        fun buildFitDetailsSummary(
            occasion: String,
            vibe: String,
            preGenOptions: PreGenerationOptions,
            profile: UserProfile?,
            hasUserPhoto: Boolean
        ): String {
            val parts = buildList {
                add("${preGenOptions.gender} • Size ${preGenOptions.outfitSize}")
                add(preGenOptions.fitSilhouette)
                add("$vibe ($occasion)")
                if (preGenOptions.footwearVibe.isNotBlank()) add("Shoes: ${preGenOptions.footwearVibe}")
                if (preGenOptions.accessoriesVibe.isNotBlank()) add("Extras: ${preGenOptions.accessoriesVibe}")
                profile?.height?.takeIf { it.isNotBlank() }?.let { add("Height: $it") }
                if (hasUserPhoto) add("Virtual Try-On Photo")
            }
            return parts.joinToString(" • ")
        }

        fun fallbackRecognizeFromBitmap(bitmap: Bitmap): RecognizedGarmentAttributes {
            val w = bitmap.width.coerceAtLeast(1)
            val h = bitmap.height.coerceAtLeast(1)
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0
            val startX = (w * 0.25f).toInt()
            val endX = (w * 0.75f).toInt().coerceAtLeast(startX + 1)
            val startY = (h * 0.25f).toInt()
            val endY = (h * 0.75f).toInt().coerceAtLeast(startY + 1)
            val stepX = ((endX - startX) / 10).coerceAtLeast(1)
            val stepY = ((endY - startY) / 10).coerceAtLeast(1)
            for (y in startY until endY step stepY) {
                for (x in startX until endX step stepX) {
                    val px = bitmap.getPixel(x, y)
                    rSum += (px shr 16) and 0xFF
                    gSum += (px shr 8) and 0xFF
                    bSum += px and 0xFF
                    count++
                }
            }
            val avgR = if (count > 0) (rSum / count).toInt() else 128
            val avgG = if (count > 0) (gSum / count).toInt() else 128
            val avgB = if (count > 0) (bSum / count).toInt() else 128
            val detectedColor = closestColorName(avgR, avgG, avgB)
            val ratio = h.toFloat() / w.toFloat()
            val (cat, piece) = when {
                ratio > 1.45f -> GarmentCategory.TROUSERS to "Relaxed Trousers"
                ratio < 0.78f -> GarmentCategory.SHOES to "Sneakers"
                else -> GarmentCategory.TOPS to "Everyday Top"
            }
            return RecognizedGarmentAttributes(
                name = "$detectedColor $piece",
                category = cat,
                color = detectedColor,
                pattern = "Solid",
                material = "Cotton blend",
                style = "Casual",
                garmentType = piece
            )
        }

        private fun closestColorName(r: Int, g: Int, b: Int): String {
            val brightness = (r + g + b) / 3
            val maxC = maxOf(r, g, b)
            val minC = minOf(r, g, b)
            val sat = maxC - minC
            return when {
                brightness < 45 -> "Black"
                brightness > 220 && sat < 24 -> "White"
                sat < 26 && brightness < 110 -> "Charcoal"
                sat < 26 && brightness < 180 -> "Grey"
                sat < 26 -> "Cream"
                r > g + 35 && r > b + 35 -> if (brightness > 180) "Pink" else "Burgundy"
                b > r + 25 && b > g + 15 -> if (brightness < 95) "Navy" else "Cobalt"
                r > 180 && g > 135 && b < 110 -> "Camel"
                g >= r && g > b + 15 -> "Olive"
                r > 135 && b > 145 && g < r - 15 -> "Lavender"
                else -> "Sand"
            }
        }
    }
}
