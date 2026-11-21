package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CatalogueProduct
import com.example.data.model.GarmentCategory
import com.example.data.model.Seller
import com.example.ui.components.LithfitGarmentImage
import com.example.ui.components.ReportContentDialog
import com.example.ui.viewmodel.LithfitViewModel
import java.util.Locale

enum class BudgetTier(val label: String) {
    ALL("All Budgets"),
    UNDER_75("Under 75"),
    FROM_75_TO_150("75 – 150"),
    FROM_150_TO_300("150 – 300"),
    OVER_300("300+");

    fun matches(price: Double): Boolean = when (this) {
        ALL -> true
        UNDER_75 -> price < 75.0
        FROM_75_TO_150 -> price in 75.0..150.0
        FROM_150_TO_300 -> price > 150.0 && price <= 300.0
        OVER_300 -> price > 300.0
    }
}

data class CuratedRadarCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val keywords: Set<String>
)

private val CURATED_RADAR_COLLECTIONS = listOf(
    CuratedRadarCollection("all", "All Collections", "Every verified style edit", emptySet()),
    CuratedRadarCollection("minimalist", "Minimalist Edit", "Clean tailoring & tonal layers", setOf("minimalist", "smart casual", "formal", "work")),
    CuratedRadarCollection("streetwear", "Streetwear Studio", "Relaxed silhouettes & utility", setOf("streetwear", "hoodies", "casual", "sporty")),
    CuratedRadarCollection("vintage", "Vintage & Y2K", "Archive textures & retro cuts", setOf("vintage", "y2k")),
    CuratedRadarCollection("essentials", "Everyday Essentials", "Daily staples & travel layers", setOf("everyday essentials", "casual", "relaxed"))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleRadarScreen(
    viewModel: LithfitViewModel,
    initialSearchQuery: String?,
    onClearInitialSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenAiStylist: () -> Unit
) {
    val context = LocalContext.current
    val sellers by viewModel.sellers.collectAsStateWithLifecycle()
    val products by viewModel.catalogueProducts.collectAsStateWithLifecycle()
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<GarmentCategory?>(null) }
    var selectedBudget by remember { mutableStateOf(BudgetTier.ALL) }
    var selectedCollectionId by remember { mutableStateOf("all") }
    var selectedSellerId by remember { mutableStateOf<String?>(null) }
    var priceSortAscending by remember { mutableStateOf<Boolean?>(null) }
    var activeProductDetail by remember { mutableStateOf<CatalogueProduct?>(null) }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(initialSearchQuery) {
        if (!initialSearchQuery.isNullOrBlank()) {
            searchQuery = initialSearchQuery
            onClearInitialSearch()
        }
    }

    val activeCollection = remember(selectedCollectionId) {
        CURATED_RADAR_COLLECTIONS.firstOrNull { it.id == selectedCollectionId } ?: CURATED_RADAR_COLLECTIONS.first()
    }

    val filteredSellers = remember(sellers, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) sellers
        else sellers.filter { s ->
            s.brandName.lowercase().contains(q) ||
                (s.description?.lowercase()?.contains(q) == true)
        }
    }

    val filteredProducts = remember(
        products,
        searchQuery,
        selectedCategory,
        selectedBudget,
        selectedCollectionId,
        selectedSellerId,
        priceSortAscending
    ) {
        val q = searchQuery.trim().lowercase()
        val list = products.filter { prod ->
            val catOk = selectedCategory == null || GarmentCategory.safeParse(prod.category) == selectedCategory
            val budgetOk = selectedBudget.matches(prod.price)
            val sellerOk = selectedSellerId == null || prod.sellerId == selectedSellerId
            val collectionOk = activeCollection.keywords.isEmpty() ||
                activeCollection.keywords.any { kw ->
                    prod.style.lowercase().contains(kw) ||
                        (prod.occasion?.lowercase()?.contains(kw) == true)
                }
            val queryOk = q.isEmpty() ||
                prod.name.lowercase().contains(q) ||
                prod.sellerName.lowercase().contains(q) ||
                prod.category.lowercase().contains(q) ||
                GarmentCategory.safeParse(prod.category).label.lowercase().contains(q) ||
                prod.color.lowercase().contains(q) ||
                prod.style.lowercase().contains(q) ||
                (prod.description?.lowercase()?.contains(q) == true)

            catOk && budgetOk && sellerOk && collectionOk && queryOk
        }
        when (priceSortAscending) {
            true -> list.sortedBy { it.price }
            false -> list.sortedByDescending { it.price }
            null -> list
        }
    }

    reportTarget?.let { (targetId, title) ->
        ReportContentDialog(
            targetTitle = title,
            onDismiss = { reportTarget = null },
            onSubmit = { reason, details ->
                viewModel.submitReport("PRODUCT", targetId, reason, details)
                reportTarget = null
            }
        )
    }

    activeProductDetail?.let { prod ->
        val saved = savedItems.any { it.targetType == "PRODUCT" && it.targetId == prod.id }
        val seller = sellers.firstOrNull { it.id == prod.sellerId }
        val verifiedUrl = sanitizeExternalUrl(prod.productUrl) ?: sanitizeExternalUrl(seller?.websiteUrl)
        val verifiedEmail = sanitizeContactEmail(seller?.contactEmail)

        AlertDialog(
            onDismissRequest = { activeProductDetail = null },
            title = { Text(prod.name, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LithfitGarmentImage(
                        localImagePath = null,
                        thumbnailBase64 = null,
                        imageUrl = prod.imageUrl,
                        imageResKey = prod.imageResKey,
                        contentDescription = prod.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Text(
                        text = "${prod.sellerName} • ${GarmentCategory.safeParse(prod.category).label}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${prod.currency} ${formatCurrencyAmount(prod.price)} • ${prod.availabilityStatus.replace('_', ' ')}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${prod.color} • ${prod.style}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    prod.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(desc, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (prod.sizes.isNotEmpty()) {
                        Text(
                            text = "Sizes: ${prod.sizes.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.toggleSaveProduct(prod) }) {
                            Icon(
                                if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (saved) "Saved" else "Save")
                        }
                        if (verifiedEmail != null) {
                            OutlinedButton(onClick = {
                                openExternalUri(context, Uri.parse("mailto:$verifiedEmail"))
                            }) {
                                Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Contact")
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            reportTarget = prod.id to prod.name
                            activeProductDetail = null
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Report listing")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { activeProductDetail = null }) { Text("Close") }
            },
            confirmButton = {
                if (verifiedUrl != null) {
                    Button(onClick = {
                        openExternalUri(context, Uri.parse(verifiedUrl))
                    }) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Visit Seller Site")
                    }
                } else {
                    Button(onClick = { activeProductDetail = null }) { Text("Done") }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "STYLE RADAR",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("style_radar_title")
                )
                Text(
                    text = "Discover verified fashion brands, stores, curated collections, and pieces.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name, brand, category, colour, or style...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("style_radar_search_input")
            )
        }

        // Curated Collections
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Curated Fashion Collections", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CURATED_RADAR_COLLECTIONS, key = { it.id }) { collection ->
                        FilterChip(
                            selected = selectedCollectionId == collection.id,
                            onClick = { selectedCollectionId = collection.id },
                            label = { Text(collection.title) },
                            modifier = Modifier.testTag("collection_chip_${collection.id}")
                        )
                    }
                }
            }
        }

        // Budget & Price Sort Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Budget & Category", style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(
                        onClick = {
                            priceSortAscending = when (priceSortAscending) {
                                null -> true
                                true -> false
                                false -> null
                            }
                        },
                        modifier = Modifier.testTag("style_radar_sort_button")
                    ) {
                        Text(
                            when (priceSortAscending) {
                                true -> "Price ↑"
                                false -> "Price ↓"
                                null -> "Sort"
                            }
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BudgetTier.entries, key = { it.name }) { tier ->
                        FilterChip(
                            selected = selectedBudget == tier,
                            onClick = { selectedBudget = tier },
                            label = { Text(tier.label) },
                            modifier = Modifier.testTag("budget_chip_${tier.name}")
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Categories") }
                        )
                    }
                    items(GarmentCategory.entries, key = { it.name }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                            label = { Text(cat.label) }
                        )
                    }
                }
            }
        }

        // Verified Brands & Stores Directory (shown only when real seller data exists)
        if (filteredSellers.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Verified Brands & Stores", style = MaterialTheme.typography.titleMedium)
                        if (selectedSellerId != null) {
                            TextButton(onClick = { selectedSellerId = null }) {
                                Text("Show All Brands")
                            }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredSellers, key = { it.id }) { seller ->
                            SellerDirectoryCard(
                                seller = seller,
                                selected = selectedSellerId == seller.id,
                                onSelect = {
                                    selectedSellerId = if (selectedSellerId == seller.id) null else seller.id
                                },
                                onOpenWebsite = { url ->
                                    openExternalUri(context, Uri.parse(url))
                                },
                                onContactEmail = { email ->
                                    openExternalUri(context, Uri.parse("mailto:$email"))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Empty State vs Live Catalogue Listings
        if (products.isEmpty() && sellers.isEmpty()) {
            item {
                StyleRadarEmptyCatalogState(
                    selectedCollection = activeCollection,
                    onOpenDiscover = onOpenDiscover,
                    onOpenAiStylist = onOpenAiStylist
                )
            }
        } else if (filteredProducts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("No matching products", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "No verified seller listings match your current search, budget, or category filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = {
                            searchQuery = ""
                            selectedCategory = null
                            selectedBudget = BudgetTier.ALL
                            selectedCollectionId = "all"
                            selectedSellerId = null
                        }) {
                            Text("Clear Filters")
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Verified Listings (${filteredProducts.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(filteredProducts, key = { it.id }) { product ->
                val isSaved = savedItems.any { it.targetType == "PRODUCT" && it.targetId == product.id }
                val seller = sellers.firstOrNull { it.id == product.sellerId }
                StyleRadarProductCard(
                    product = product,
                    isSaved = isSaved,
                    verifiedWebsiteUrl = sanitizeExternalUrl(product.productUrl) ?: sanitizeExternalUrl(seller?.websiteUrl),
                    verifiedContactEmail = sanitizeContactEmail(seller?.contactEmail),
                    onClick = { activeProductDetail = product },
                    onToggleSave = { viewModel.toggleSaveProduct(product) },
                    onOpenWebsite = { url -> openExternalUri(context, Uri.parse(url)) },
                    onContactSeller = { email -> openExternalUri(context, Uri.parse("mailto:$email")) }
                )
            }
        }
    }
}

@Composable
private fun StyleRadarEmptyCatalogState(
    selectedCollection: CuratedRadarCollection,
    onOpenDiscover: () -> Unit,
    onOpenAiStylist: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("style_radar_empty_state")
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Style,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (selectedCollection.id == "all") {
                        "Seller Catalogue Integration Pending"
                    } else {
                        "${selectedCollection.title} • Pending Catalogue"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "STYLE RADAR displays verified clothing brands, stores, pricing, and links exclusively from connected seller catalogues. Listings will appear here automatically when live seller data becomes available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "Meanwhile, explore editorial looks in Discover or ask LITHFIT AI to style pieces from your personal wardrobe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDiscover,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Discover")
                }
                Button(
                    onClick = onOpenAiStylist,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("LITHFIT AI")
                }
            }
        }
    }
}

@Composable
private fun SellerDirectoryCard(
    seller: Seller,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenWebsite: (String) -> Unit,
    onContactEmail: (String) -> Unit
) {
    val verifiedUrl = sanitizeExternalUrl(seller.websiteUrl)
    val verifiedEmail = sanitizeContactEmail(seller.contactEmail)

    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.width(248.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = seller.brandName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            seller.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (verifiedUrl != null) {
                    AssistChip(
                        onClick = { onOpenWebsite(verifiedUrl) },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Website") }
                    )
                }
                if (verifiedEmail != null) {
                    AssistChip(
                        onClick = { onContactEmail(verifiedEmail) },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Contact") }
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleRadarProductCard(
    product: CatalogueProduct,
    isSaved: Boolean,
    verifiedWebsiteUrl: String?,
    verifiedContactEmail: String?,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
    onOpenWebsite: (String) -> Unit,
    onContactSeller: (String) -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LithfitGarmentImage(
                    localImagePath = null,
                    thumbnailBase64 = null,
                    imageUrl = product.imageUrl,
                    imageResKey = product.imageResKey,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.sellerName} • ${GarmentCategory.safeParse(product.category).label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.color} • ${product.style}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${product.currency} ${formatCurrencyAmount(product.price)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = product.availabilityStatus.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from favourites" else "Save to favourites"
                    )
                }
            }

            product.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (verifiedWebsiteUrl != null || verifiedContactEmail != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (verifiedWebsiteUrl != null) {
                        OutlinedButton(onClick = { onOpenWebsite(verifiedWebsiteUrl) }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Seller Website")
                        }
                    }
                    if (verifiedContactEmail != null) {
                        OutlinedButton(onClick = { onContactSeller(verifiedContactEmail) }) {
                            Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Contact")
                        }
                    }
                }
            }
        }
    }
}

private fun sanitizeExternalUrl(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return null
    val host = Uri.parse(trimmed).host?.lowercase().orEmpty()
    if (host.isBlank() || host == "example.com" || host.endsWith(".example.com") || host.endsWith(".example")) {
        return null
    }
    return trimmed
}

private fun sanitizeContactEmail(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (!trimmed.contains("@") || trimmed.endsWith(".example", ignoreCase = true) || trimmed.endsWith("@example.com", ignoreCase = true)) {
        return null
    }
    return trimmed
}

private fun openExternalUri(context: android.content.Context, uri: Uri) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun formatCurrencyAmount(price: Double): String {
    return if (price % 1.0 == 0.0) {
        price.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", price)
    }
}
