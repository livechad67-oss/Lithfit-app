package com.example.data.model

import androidx.annotation.Keep

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System");

    companion object {
        fun safeParse(raw: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: LIGHT
    }
}

enum class GarmentCategory(val label: String, val slotGroup: OutfitSlotGroup) {
    TOPS("Tops", OutfitSlotGroup.TOP),
    TROUSERS("Trousers", OutfitSlotGroup.BOTTOM),
    JEANS("Jeans", OutfitSlotGroup.BOTTOM),
    JACKETS("Outerwear", OutfitSlotGroup.JACKET),
    HOODIES("Hoodies & Knits", OutfitSlotGroup.TOP),
    SHOES("Footwear", OutfitSlotGroup.SHOES),
    ACCESSORIES("Accessories", OutfitSlotGroup.ACCESSORIES),
    DRESSES("Dresses", OutfitSlotGroup.TOP),
    SKIRTS("Skirts", OutfitSlotGroup.BOTTOM),
    OTHER("Other", OutfitSlotGroup.ACCESSORIES);

    companion object {
        fun safeParse(raw: String?): GarmentCategory =
            entries.firstOrNull {
                it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true)
            } ?: OTHER
    }
}

enum class OutfitSlotGroup(val label: String) {
    JACKET("Outerwear"),
    TOP("Top"),
    BOTTOM("Bottom"),
    SHOES("Footwear"),
    ACCESSORIES("Accessories")
}

data class LithfitColorSwatch(
    val name: String,
    val hex: Long,
    val group: String
)

@Keep
data class AvatarConfig(
    val presentation: String = "Neutral",
    val skinToneHex: Long = 0xFFD9A682L,
    val faceShape: String = "Oval",
    val eyeColor: String = "Espresso",
    val expression: String = "Confident",
    val hairStyle: String = "Textured Crop",
    val hairColor: String = "Deep Black",
    val glassesStyle: String = "None",
    val topStyle: String = "Boxy Hoodie",
    val topColor: String = "Charcoal",
    val bottomStyle: String = "Wide Cargos",
    val bottomColor: String = "Olive",
    val shoesStyle: String = "Low-Top Sneakers",
    val shoesColor: String = "Cream",
    val accessoryStyle: String = "Minimal Chain",
    val aestheticPreset: String = "Streetwear"
)

@Keep
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String? = null,
    val avatarUrl: String? = null,
    val avatarStyle: String? = "Streetwear",
    val avatarConfig: AvatarConfig = AvatarConfig(),
    val hasPersonalPhoto: Boolean = false,
    val guideCompleted: Boolean = false,
    val privateProfile: Boolean = true,
    val minorAccount: Boolean = false,
    val preferredStyles: List<String> = emptyList(),
    val preferredColors: List<String> = emptyList(),
    val avoidColors: List<String> = emptyList(),
    val preferredCategories: List<String> = emptyList(),
    val preferredWeatherStyles: List<String> = emptyList(),
    val preferredOccasions: List<String> = emptyList(),
    val preferredFootwear: List<String> = emptyList(),
    val preferredAccessories: List<String> = emptyList(),
    val budgetPreference: String = "Balanced everyday",
    val inspirationFocus: String = "All",
    val preferredCurrency: String = "USD",
    val preferredRegion: String? = null,
    val locationLabel: String? = null,
    val themeMode: String = ThemeMode.LIGHT.name,
    val notificationsEnabled: Boolean = false,
    val locationAccessPreference: Boolean = false,
    val gender: String = "",
    val age: String = "",
    val height: String = "",
    val bodySize: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class PreGenerationOptions(
    val gender: String = "Unisex",
    val outfitSize: String = "M",
    val fitSilhouette: String = "Baggy / Oversized",
    val footwearVibe: String = "Clean Sneakers",
    val accessoriesVibe: String = "Minimalist Silver / Cap",
    val useSavedPersonalPhoto: Boolean = false
)

@Keep
data class StyleBoard(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val aesthetic: String = "Streetwear",
    val colors: List<String> = emptyList(),
    val occasion: String = "Casual day",
    val itemIds: List<String> = emptyList(),
    val imageResKey: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class RecentlyViewedEntry(
    val id: String = "",
    val postId: String = "",
    val title: String = "",
    val category: String = "",
    val imageResKey: String? = null,
    val colorPalette: List<String> = emptyList(),
    val viewedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class CommunityPost(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val title: String = "",
    val caption: String = "",
    val aesthetic: String = "Streetwear",
    val imagePath: String? = null,
    val thumbnailBase64: String? = null,
    val colorPalette: List<String> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class DailyOutfitTip(
    val id: String,
    val title: String,
    val tipBody: String,
    val featuredImageResKey: String,
    val featuredTitle: String,
    val pieces: List<String>,
    val colorCombos: List<String>,
    val matchingAccessories: List<String>,
    val vibe: String
)

@Keep
data class LegalAcceptance(
    val id: String = "v1_consent",
    val userId: String = "",
    val privacyPolicyVersion: String = "1.0",
    val termsVersion: String = "1.0",
    val communityGuidelinesVersion: String = "1.0",
    val cameraConsent: Boolean = false,
    val photoConsent: Boolean = false,
    val locationConsent: Boolean = false,
    val notificationsConsent: Boolean = false,
    val acceptedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class WardrobeItem(
    val id: String = "",
    val userId: String = "",
    val wardrobeId: String = "default",
    val name: String = "",
    val category: String = GarmentCategory.OTHER.name,
    val subType: String? = null,
    val color: String = "",
    val pattern: String? = null,
    val material: String? = null,
    val texture: String? = null,
    val finish: String? = null,
    val style: String? = null,
    val garmentType: String? = null,
    val brand: String? = null,
    val notes: String? = null,
    val imagePath: String? = null,
    val thumbnailBase64: String? = null,
    val favorite: Boolean = false,
    val privateItem: Boolean = true,
    val collectionTags: List<String> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun categoryEnum(): GarmentCategory = GarmentCategory.safeParse(category)
}

@Keep
data class OutfitCollection(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class Outfit(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val occasion: String = "Casual day",
    val vibe: String = "Streetwear",
    val notes: String? = null,
    val collectionId: String? = null,
    val topItemId: String? = null,
    val bottomItemId: String? = null,
    val shoesItemId: String? = null,
    val jacketItemId: String? = null,
    val accessoryIds: List<String> = emptyList(),
    val itemNames: List<String> = emptyList(),
    val colorPalette: List<String> = emptyList(),
    val itemColorOverrides: Map<String, String> = emptyMap(),
    val feedbackStatus: String? = null,
    val previewPath: String? = null,
    val favorite: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class AiMessage(
    val id: String = "",
    val userId: String = "",
    val role: String = "ASSISTANT",
    val text: String = "",
    val recommendedItemNames: List<String> = emptyList(),
    val stylingReason: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class GeneratedPreview(
    val id: String = "",
    val userId: String = "",
    val outfitId: String? = null,
    val promptSummary: String = "",
    val occasion: String = "",
    val vibe: String = "",
    val localImagePath: String? = null,
    val remoteImageUrl: String? = null,
    val thumbnailBase64: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class SavedItem(
    val id: String = "",
    val userId: String = "",
    val targetType: String = "DISCOVERY_POST",
    val targetId: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val category: String = "",
    val collectionName: String? = null,
    val liked: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class DiscoveryPost(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val targetAudience: String = "UNISEX",
    val ageGroup: String = "ALL",
    val culturalTag: String? = null,
    val sectionTag: String = "Trending",
    val sourceType: String = "EDITORIAL",
    val imageResKey: String? = null,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val colorPalette: List<String> = emptyList(),
    val outfitBreakdown: List<String> = emptyList(),
    val published: Boolean = true,
    val likesCount: Long = 0L
)

@Keep
data class Seller(
    val id: String = "",
    val ownerUserId: String = "",
    val brandName: String = "",
    val description: String? = null,
    val websiteUrl: String? = null,
    val contactEmail: String? = null,
    val active: Boolean = true
)

@Keep
data class StoreLocation(
    val id: String = "",
    val ownerUserId: String = "",
    val sellerId: String = "",
    val name: String = "",
    val category: String = "",
    val address: String = "",
    val city: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val contactPhone: String? = null,
    val active: Boolean = true
)

@Keep
data class CatalogueProduct(
    val id: String = "",
    val sellerOwnerId: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val storeId: String? = null,
    val name: String = "",
    val category: String = GarmentCategory.TOPS.name,
    val color: String = "",
    val style: String = "",
    val occasion: String? = null,
    val price: Double = 0.0,
    val currency: String = "USD",
    val sizes: List<String> = emptyList(),
    val description: String? = null,
    val availabilityStatus: String = "IN_STOCK",
    val productUrl: String? = null,
    val imageUrl: String? = null,
    val imageResKey: String? = null,
    val active: Boolean = true
)

object LithfitConstants {
    val STYLE_CATEGORIES = listOf(
        "Y2K",
        "Streetwear",
        "Casual",
        "Old money",
        "Minimalist",
        "Vintage",
        "Grunge",
        "Techwear",
        "Formal",
        "Smart casual",
        "Sporty",
        "Afro-inspired",
        "Contemporary",
        "Experimental",
        "Oversized",
        "Baggy",
        "Skate",
        "Denim culture",
        "Preppy",
        "School-appropriate"
    )

    val OCCASIONS = listOf(
        "Casual day",
        "School-appropriate casual wear",
        "Weekend",
        "Hangout",
        "Birthday",
        "Party",
        "Dinner",
        "Travel",
        "Sport & active",
        "Formal event",
        "Work"
    )

    val WEATHER_STYLES = listOf(
        "Hot",
        "Warm",
        "Cool",
        "Cold",
        "Rainy",
        "Windy",
        "Mixed conditions"
    )

    val VIBES = listOf(
        "Y2K",
        "Streetwear",
        "Casual",
        "Old money",
        "Minimalist",
        "Vintage",
        "Grunge",
        "Techwear",
        "Formal",
        "Smart casual",
        "Sporty",
        "Afro-inspired",
        "Contemporary",
        "Experimental"
    )

    val DISCOVERY_CATEGORIES = listOf(
        "For You",
        "Men's",
        "Women's",
        "Gender-Neutral",
        "Teenage",
        "Adult",
        "Cultural",
        "Trending",
        "Seasonal"
    )

    val AUDIENCE_FILTERS = listOf(
        "All",
        "For Him",
        "For Her",
        "Unisex"
    )

    val COLOR_SWATCHES = listOf(
        LithfitColorSwatch("Black", 0xFF17171A, "Neutrals"),
        LithfitColorSwatch("White", 0xFFFAFAFA, "Neutrals"),
        LithfitColorSwatch("Grey", 0xFF90949C, "Neutrals"),
        LithfitColorSwatch("Charcoal", 0xFF3A3C42, "Neutrals"),
        LithfitColorSwatch("Cream", 0xFFF5EFE0, "Neutrals"),
        LithfitColorSwatch("Beige", 0xFFD9C7AE, "Neutrals"),
        LithfitColorSwatch("Brown", 0xFF7B543A, "Earth"),
        LithfitColorSwatch("Chocolate", 0xFF472C1E, "Earth"),
        LithfitColorSwatch("Red", 0xFFD63838, "Bold"),
        LithfitColorSwatch("Burgundy", 0xFF6E2030, "Bold"),
        LithfitColorSwatch("Blue", 0xFF2F69C8, "Cool"),
        LithfitColorSwatch("Navy", 0xFF1D2D50, "Cool"),
        LithfitColorSwatch("Sky Blue", 0xFF7AB8E8, "Cool"),
        LithfitColorSwatch("Green", 0xFF2D8A56, "Earth"),
        LithfitColorSwatch("Olive", 0xFF6B7445, "Earth"),
        LithfitColorSwatch("Lime", 0xFFCFEF4A, "Bold"),
        LithfitColorSwatch("Purple", 0xFF6F42C1, "Bold"),
        LithfitColorSwatch("Lavender", 0xFFB8A3E3, "Cool"),
        LithfitColorSwatch("Pink", 0xFFEC7FA9, "Bold"),
        LithfitColorSwatch("Orange", 0xFFEA7A34, "Bold"),
        LithfitColorSwatch("Yellow", 0xFFF2C94C, "Bold")
    )

    val PREFERRED_COLORS = COLOR_SWATCHES.map { it.name }

    fun colorHexForName(name: String): Long {
        val norm = name.trim().lowercase()
        COLOR_SWATCHES.firstOrNull { it.name.lowercase() == norm }?.let { return it.hex }
        return when {
            "ivory" in norm || "off-white" in norm -> 0xFFF6F2E8
            "stone" in norm || "sand" in norm || "khaki" in norm -> 0xFFD7CCB9
            "camel" in norm || "tan" in norm -> 0xFFC59B6D
            "slate" in norm -> 0xFF636B78
            "indigo" in norm || "denim" in norm -> 0xFF34527A
            "espresso" in norm -> 0xFF43291F
            "silver" in norm -> 0xFFC2C6CE
            "mint" in norm || "sage" in norm -> 0xFF8AB59D
            "coral" in norm -> 0xFFE97468
            "terracotta" in norm || "rust" in norm -> 0xFFC85D3B
            "gold" in norm -> 0xFFD8B04A
            "cobalt" in norm -> 0xFF2657B8
            "purple" in norm || "violet" in norm -> 0xFFCCADFD
            else -> 0xFF8E929B
        }
    }

    val SUBTYPES_BY_CATEGORY: Map<GarmentCategory, List<String>> = mapOf(
        GarmentCategory.TOPS to listOf(
            "T-shirt", "Graphic tee", "Plain tee", "Polo shirt",
            "Long-sleeve shirt", "Shirt", "Tank top", "Jersey"
        ),
        GarmentCategory.HOODIES to listOf(
            "Hoodie", "Sweatshirt", "Sweater", "Knitwear", "Zip hoodie"
        ),
        GarmentCategory.JEANS to listOf(
            "Baggy jeans", "Straight-leg jeans", "Slim jeans", "Relaxed denim", "Carpenter jeans"
        ),
        GarmentCategory.TROUSERS to listOf(
            "Cargo trousers", "Wide-leg trousers", "Joggers", "Track pants",
            "Shorts", "Formal trousers", "Chinos", "Sweatpants"
        ),
        GarmentCategory.JACKETS to listOf(
            "Denim jacket", "Leather-style jacket", "Puffer jacket", "Bomber jacket",
            "Windbreaker", "Blazer", "Oversized jacket", "Cardigan", "Varsity jacket"
        ),
        GarmentCategory.SHOES to listOf(
            "Sneakers", "High-tops", "Low-tops", "Boots",
            "Loafers", "Slides", "Sandals", "Casual shoes"
        ),
        GarmentCategory.ACCESSORIES to listOf(
            "Watch", "Bracelet", "Necklace", "Ring", "Cap",
            "Beanie", "Belt", "Sunglasses", "Bag", "Backpack", "Chain"
        ),
        GarmentCategory.DRESSES to listOf("Casual dress", "Midi dress", "Shirt dress", "Evening dress"),
        GarmentCategory.SKIRTS to listOf("Denim skirt", "Pleated skirt", "Midi skirt", "Cargo skirt"),
        GarmentCategory.OTHER to listOf("Full set", "Layering piece", "Custom item")
    )

    val PATTERNS = listOf(
        "Solid", "Graphic", "Striped", "Plaid", "Checkered",
        "Colorblock", "Distressed", "Washed", "Ribbed", "Woven print"
    )

    val TEXTURES_FINISHES = listOf(
        "Heavyweight cotton", "Soft fleece", "Raw denim", "Washed denim",
        "Matte leather-style", "Ripstop nylon", "Brushed knit", "Crisp poplin", "Relaxed drape"
    )

    val PREFERRED_FOOTWEAR_TYPES = SUBTYPES_BY_CATEGORY[GarmentCategory.SHOES].orEmpty()
    val PREFERRED_ACCESSORY_TYPES = SUBTYPES_BY_CATEGORY[GarmentCategory.ACCESSORIES].orEmpty()

    val BUDGET_PREFERENCES = listOf(
        "Budget-friendly",
        "Balanced everyday",
        "Premium splurge",
        "No limit"
    )

    val CURRENCIES = listOf("USD", "GBP", "EUR", "CAD", "AUD", "JPY")
    val AVATAR_STYLES = listOf("Streetwear", "Y2K", "Old money", "Minimalist", "Afro-inspired", "Grunge", "Contemporary", "Sporty")

    val AVATAR_PRESENTATIONS = listOf("Feminine", "Neutral", "Masculine")
    val SKIN_TONE_SWATCHES = listOf(
        LithfitColorSwatch("Porcelain", 0xFFF7DAC7, "Skin"),
        LithfitColorSwatch("Warm Fair", 0xFFEEC1A0, "Skin"),
        LithfitColorSwatch("Golden Olive", 0xFFD9A682, "Skin"),
        LithfitColorSwatch("Honey Caramel", 0xFFB87B54, "Skin"),
        LithfitColorSwatch("Rich Umber", 0xFF8C5535, "Skin"),
        LithfitColorSwatch("Deep Espresso", 0xFF5A3422, "Skin")
    )
    val FACE_SHAPES = listOf("Oval", "Soft", "Square", "Angular")
    val EYE_COLORS = listOf("Espresso", "Hazel", "Amber", "Green", "Blue", "Slate")
    val EXPRESSIONS = listOf("Confident", "Calm", "Warm Smile", "Editorial")
    val HAIR_STYLES = listOf(
        "Textured Crop",
        "Box Braids",
        "Afro Fade",
        "Layered Wolfcut",
        "Sleek Bob",
        "Long Waves",
        "Slick Back",
        "High Puff",
        "Messy Fringe",
        "Short Buzz"
    )
    val HAIR_COLORS = listOf(
        LithfitColorSwatch("Deep Black", 0xFF1A191D, "Hair"),
        LithfitColorSwatch("Dark Espresso", 0xFF3E271E, "Hair"),
        LithfitColorSwatch("Chestnut", 0xFF70422A, "Hair"),
        LithfitColorSwatch("Honey Blonde", 0xFFD2A35E, "Hair"),
        LithfitColorSwatch("Copper Red", 0xFFAD4729, "Hair"),
        LithfitColorSwatch("Platinum Ice", 0xFFE6E3DC, "Hair")
    )
    val GLASSES_STYLES = listOf("None", "Thin Metal", "Chunky Acetate", "Y2K Tinted")
    val AVATAR_TOPS = listOf(
        "Boxy Hoodie",
        "Structured Blazer",
        "Oversized Tee",
        "Knit Polo",
        "Cropped Jacket",
        "Relaxed Shirt",
        "Layered Flannel",
        "Contemporary Coat"
    )
    val AVATAR_BOTTOMS = listOf(
        "Wide Cargos",
        "Baggy Jeans",
        "Pleated Trousers",
        "Straight Denim",
        "Track Pants",
        "Pleated Skirt"
    )
    val AVATAR_SHOES = listOf(
        "Low-Top Sneakers",
        "High-Top Sneakers",
        "Chunky Loafers",
        "Combat Boots",
        "Running Trainers"
    )
    val AVATAR_ACCESSORIES = listOf(
        "None",
        "Minimal Chain",
        "Cap",
        "Beanie",
        "Crossbody Bag",
        "Wristwatch"
    )

    val GENDER_OPTIONS = listOf(
        "Women's",
        "Men's",
        "Non-binary / Unisex",
        "Prefer not to say"
    )

    val AGE_RANGES = listOf(
        "13–17 (Teen)",
        "18–24 (Young Adult)",
        "25–34",
        "35–44",
        "45+"
    )

    val HEIGHT_OPTIONS = listOf(
        "Under 5'3\" (<160 cm)",
        "5'3\"–5'6\" (160–168 cm)",
        "5'7\"–5'10\" (169–178 cm)",
        "5'11\"–6'2\" (179–188 cm)",
        "Over 6'2\" (>188 cm)"
    )

    val BODY_SIZES = listOf(
        "Petite / XS",
        "S / Lean",
        "M / Athletic",
        "L / Relaxed",
        "XL / Broad",
        "XXL+ / Tall & Full"
    )

    val OUTFIT_FIT_SIZES = listOf("XS", "S", "M", "L", "XL", "XXL")

    val FIT_SILHOUETTES = listOf(
        "Baggy / Oversized",
        "Boxy & Wide-Leg",
        "Relaxed Straight",
        "Tailored & Clean",
        "Slim Fit"
    )
}
