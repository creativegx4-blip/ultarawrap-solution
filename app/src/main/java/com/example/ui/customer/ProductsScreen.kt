package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Category
import com.example.model.CompanySettings
import com.example.model.Product
import com.example.model.UiState
import com.example.ui.components.CircularGoldAddButton
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProductStudioImage
import com.example.ui.components.StockBadge
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.PureWhite
import com.example.util.Formatters
import com.example.util.WhatsAppHelper

@Composable
fun ProductsScreen(
    productsState: UiState<List<Product>>,
    categoriesState: UiState<List<Category>>,
    settings: CompanySettings,
    searchQuery: String,
    selectedCategoryId: String,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Products",
            style = MaterialTheme.typography.headlineMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search products or SKU...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MetallicGold) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard,
                focusedBorderColor = MetallicGold,
                unfocusedBorderColor = ObsidianOutline,
                focusedTextColor = PureWhite,
                unfocusedTextColor = IvoryText
            ),
            modifier = Modifier.fillMaxWidth().testTag("products_search_input")
        )

        Spacer(Modifier.height(10.dp))

        val categories = (categoriesState as? UiState.Success)?.data?.filter { it.active }.orEmpty()
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == "ALL",
                    onClick = { onCategorySelect("ALL") },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetallicGold,
                        selectedLabelColor = ObsidianBlack,
                        containerColor = ObsidianCard,
                        labelColor = IvoryText
                    )
                )
            }
            items(categories, key = { it.id }) { cat ->
                FilterChip(
                    selected = selectedCategoryId == cat.id,
                    onClick = { onCategorySelect(cat.id) },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetallicGold,
                        selectedLabelColor = ObsidianBlack,
                        containerColor = ObsidianCard,
                        labelColor = IvoryText
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        when (productsState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MetallicGold)
                }
            }
            is UiState.Error -> {
                EmptyStateCard("Failed to load products", productsState.message)
            }
            is UiState.Success -> {
                val filtered = productsState.data
                    .filter { it.active }
                    .filter { selectedCategoryId == "ALL" || it.categoryId == selectedCategoryId }
                    .filter {
                        searchQuery.isBlank() ||
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true) ||
                            it.sku.contains(searchQuery, ignoreCase = true)
                    }

                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        title = "No matching products",
                        subtitle = "Try clearing your search filter or selecting All categories.",
                        actionLabel = "Show All",
                        onAction = {
                            onSearchChange("")
                            onCategorySelect("ALL")
                        }
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 165.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { product ->
                            CatalogProductCard(
                                product = product,
                                currency = settings.currencySymbol,
                                onDetails = { onProductClick(product) },
                                onAddToCart = { onAddToCart(product) },
                                onWhatsAppEnquiry = {
                                    WhatsAppHelper.openProductEnquiry(
                                        context = context,
                                        whatsappNumber = settings.whatsappNumber,
                                        productName = product.name,
                                        quantity = 10L,
                                        unit = product.unit
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogProductCard(
    product: Product,
    currency: String,
    onDetails: () -> Unit,
    onAddToCart: () -> Unit,
    onWhatsAppEnquiry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onDetails)
            .testTag("product_card_${product.id}"),
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ProductStudioImage(
                imageUrl = product.imageUrl,
                drawableKey = product.drawableKey,
                contentDescription = product.name,
                modifier = Modifier.fillMaxWidth().height(126.dp)
            )
            Spacer(Modifier.height(10.dp))
            StockBadge(stockQty = product.stockQty)
            Spacer(Modifier.height(6.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodySmall,
                color = MutedSilver,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Formatters.formatCurrency(product.priceInr, currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = LightGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "per ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                }
                CircularGoldAddButton(
                    onClick = onAddToCart,
                    contentDescription = "Add ${product.name} to Cart",
                    modifier = Modifier.size(44.dp).testTag("add_to_cart_${product.id}")
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDetails,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("details_${product.id}")
                ) {
                    Text("Details", color = LightGold, style = MaterialTheme.typography.labelMedium)
                }
                Surface(
                    color = EmeraldSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                ) {
                    IconButton(
                        onClick = onWhatsAppEnquiry,
                        modifier = Modifier.size(40.dp).testTag("whatsapp_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp enquiry for ${product.name}",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
