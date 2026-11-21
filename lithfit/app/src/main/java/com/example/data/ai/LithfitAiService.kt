package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.CatalogueProduct
import com.example.data.model.GarmentCategory
import com.example.data.model.Outfit
import com.example.data.model.UserProfile
import com.example.data.model.WardrobeItem
import com.example.data.remote.WeatherSnapshot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
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
                    You are LITHFIT AI, a sharp, friendly general assistant with deep fashion specialization inside LITHFIT by CLASTAD.
                    Help users smoothly with casual greetings, general knowledge, technology questions, writing, daily planning, and practical fashion or colour-matching questions.
                    Rules:
                    1. Answer greetings naturally and warmly (e.g., respond concisely to "Hey" and invite the user to ask anything or plan a look).
                    2. Give clear, accurate, direct answers in 2–6 scannable lines or bullet points.
                    3. Speak fluently about all global fashion cultures (Y2K, Streetwear, Old money, Grunge, Afro-inspired, Techwear, Formal, Minimalist, Vintage, Casual, Sporty, Contemporary, Experimental).
                    4. Stay age-appropriate, body-neutral, and encouraging. Never rank attractiveness or comment negatively on bodies.
                    5. Never mention Google, Gemini, AI Studio, or underlying AI model providers. You are LITHFIT AI.
                    6. Never claim you generated outfit images or automatically edited a user's avatar; offer written styling direction.
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
                    Your purpose is tailored fashion advice, wardrobe curation, and written outfit combinations grounded in the user's real owned items.

                    Supported Fashion Identities:
                    - Y2K, Streetwear, Casual, Old money, Minimalist, Vintage, Grunge, Techwear, Formal, Smart casual, Sporty, Afro-inspired fashion, Contemporary fashion, Experimental fashion.

                    Strict Integrity & Safety Rules:
                    1. Prioritize items the user already owns in their wardrobe before suggesting optional additions.
                    2. Respect the user's preferred colours, frequent colours, neutral combos, colours to avoid, footwear, silhouettes, budget, and weather styles.
                    3. Learn from liked outfits and avoid repeating disliked patterns.
                    4. Tailor choices to live weather if provided; never invent live weather.
                    5. Stay 100% age-appropriate, body-neutral, and positive. Never restrict looks by gender stereotypes.
                    6. Never fabricate store stock, prices, or links unless listed in the verified catalogue.
                    7. Never claim to generate synthetic outfit images or auto-dress the avatar studio; deliver detailed text outfit pairings with swappable pieces.
                    8. Never mention Google, Gemini, AI Studio, or underlying model providers. You are LITHFIT AI.

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

    fun streamPhotoStylingAdvice(
        bitmap: Bitmap,
        userPrompt: String,
        occasion: String,
        vibe: String,
        profile: UserProfile?,
        wardrobe: List<WardrobeItem>
    ): Flow<String> = flow {
        val buffer = StringBuilder()
        val ownedText = if (wardrobe.isNotEmpty()) {
            wardrobe.take(18).joinToString(", ") { "${it.name} (${it.color} ${it.category})" }
        } else {
            "No wardrobe pieces added yet"
        }
        val requestText = """
            Analyze this garment or outfit photo and provide concise, encouraging fashion styling advice.
            Pair it first with owned pieces from the user's wardrobe if available.
            Occasion: $occasion | Vibe: $vibe
            User's preferred styles: ${profile?.preferredStyles?.joinToString().orEmpty().ifBlank { "Open" }}
            User's owned wardrobe: $ownedText
            User request: ${userPrompt.ifBlank { "How should I style this piece?" }}
        """.trimIndent()

        withTimeout(55_000L) {
            stylistTextModel.generateContentStream(
                content {
                    image(bitmap)
                    text(requestText)
                }
            ).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    buffer.append(piece)
                    emit(buffer.toString())
                }
            }
        }
        if (buffer.isBlank()) {
            throw IllegalStateException("Could not analyze the attached photo right now. Please try again.")
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

    private fun buildGeneralQaContext(
        userPrompt: String,
        profile: UserProfile?,
        history: List<Pair<String, String>>
    ): String {
        val styleFocus = (profile?.preferredStyles.orEmpty() + profile?.personalStyleTags.orEmpty())
            .distinct().take(6).joinToString()
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
                val allStyles = (profile.preferredStyles + profile.personalStyleTags).distinct()
                append("Styles: ${allStyles.take(8).joinToString().ifBlank { "Open" }}; ")
                val allColors = (profile.preferredColors + profile.frequentColors).distinct()
                append("Likes colors: ${allColors.take(8).joinToString().ifBlank { "Open" }}; ")
                append("Avoids colors: ${profile.avoidColors.take(4).joinToString().ifBlank { "None" }}; ")
                if (profile.preferredSilhouettes.isNotEmpty()) {
                    append("Cuts/Silhouettes: ${profile.preferredSilhouettes.take(4).joinToString()}; ")
                }
                append("Budget: ${profile.budgetPreference}")
            }
        } else "Open preferences"

        val wardrobeText = if (wardrobe.isNotEmpty()) {
            wardrobe.take(22).joinToString("\n") {
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

        private val GREETING_TRIGGERS = setOf(
            "hi", "hey", "hello", "yo", "sup", "hola", "good morning",
            "good afternoon", "good evening", "howdy", "what's up", "whats up"
        )

        private val OUTFIT_TRIGGERS = listOf(
            "create an outfit", "create outfit", "build an outfit", "build me a", "build a ",
            "suggest a ", "suggest an outfit", "make me an outfit", "put together an outfit",
            "style me", "dress me", "what should i wear", "what to wear", "outfit for ",
            "outfit from my", "using my wardrobe", "from my closet", "fit check",
            "replace only the", "swap the", "lighter version", "more formal version",
            "streetwear outfit", "y2k outfit", "casual outfit", "birthday outfit", "party outfit",
            "dinner outfit", "date outfit", "school outfit", "interview outfit"
        )

        private val GENERAL_QUESTION_PREFIXES = listOf(
            "what is ", "what's ", "whats ", "what are ", "what does ", "meaning of",
            "explain ", "define ", "who is ", "who are ", "how does ", "why is ", "why does ",
            "difference between", "how do i match", "what colours go", "what colors go",
            "what goes with", "which shoes go", "do brown shoes", "is y2k", "tell me about",
            "how to care", "fashion tip", "recommend music", "help me understand"
        )

        fun classifyIntent(rawPrompt: String): StylistIntent {
            val lower = rawPrompt.trim().lowercase().trimEnd('!', '.', '?')
            if (lower.isEmpty()) return StylistIntent.GENERAL_QUESTION
            if (lower in GREETING_TRIGGERS || GREETING_TRIGGERS.any { lower.startsWith("$it ") && lower.length <= 28 }) {
                return StylistIntent.GENERAL_QUESTION
            }

            if (OUTFIT_TRIGGERS.any { it in lower }) {
                return StylistIntent.OUTFIT_GENERATION
            }

            if (GENERAL_QUESTION_PREFIXES.any { lower.startsWith(it) || it in lower }) {
                return StylistIntent.GENERAL_QUESTION
            }

            val wardrobeStylingMentions = lower.contains("wardrobe") ||
                lower.contains("closet") ||
                lower.contains("outfit") ||
                lower.contains("wear today") ||
                lower.contains("pair this")
            return if (wardrobeStylingMentions) {
                StylistIntent.OUTFIT_GENERATION
            } else {
                StylistIntent.GENERAL_QUESTION
            }
        }

        fun stripInlineOutfitMarker(raw: String): String {
            val startIdx = raw.indexOf(OUTFIT_MARKER_START)
            if (startIdx >= 0) {
                return raw.substring(0, startIdx).trimEnd()
            }
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
