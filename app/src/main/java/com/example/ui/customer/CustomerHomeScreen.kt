package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CompanySettings
import com.example.model.CustomerProfile
import com.example.model.Order
import com.example.model.Product
import com.example.model.UiState
import com.example.ui.components.CircularGoldAddButton
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.ObsidianSectionCard
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.ProductStudioImage
import com.example.ui.components.UltraWrapLogoBadge
import com.example.ui.components.WhatsAppEnquiryChip
import com.example.ui.theme.GoldTintSurface
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.PureWhite
import com.example.util.Formatters
import com.example.util.WhatsAppHelper

@Composable
fun CustomerHomeScreen(
    profile: CustomerProfile?,
    fallbackName: String,
    settings: CompanySettings,
    productsState: UiState<List<Product>>,
    ordersState: UiState<List<Order>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToContact: () -> Unit,
    onProductClick: (Product) -> Unit,
    onQuickAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customerName = profile?.fullName?.takeIf { it.isNotBlank() } ?: fallbackName.ifBlank { "Valued Customer" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UltraWrapLogoBadge(size = 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = settings.companyName.ifBlank { "ULTRAWRAP SOLUTION" },
                            style = MaterialTheme.typography.titleMedium,
                            color = LightGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Welcome, $customerName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSilver
                        )
                    }
                }
                WhatsAppEnquiryChip(
                    label = "Support",
                    onClick = {
                        WhatsAppHelper.openGeneralSupport(
                            context = context,
                            whatsappNumber = settings.whatsappNumber,
                            customerName = customerName
                        )
                    },
                    modifier = Modifier.testTag("home_whatsapp_support_button")
                )
            }
        }

        // Search Products
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onSearchChange(it)
                    if (it.isNotBlank()) onNavigateToProducts()
                },
                placeholder = { Text("Search stretch film, bubble roll, BOPP tape...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MetallicGold) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                    focusedBorderColor = MetallicGold,
                    unfocusedBorderColor = ObsidianOutline,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = IvoryText
                ),
                modifier = Modifier.fillMaxWidth().testTag("home_search_input")
            )
        }

        // Premium Promotional Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.55f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(ObsidianBlack, Color(0xFF281F0C), ObsidianCard)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = GoldTintSurface,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "INDUSTRIAL GRADE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LightGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Premium Packaging Solutions",
                                style = MaterialTheme.typography.headlineSmall,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Quality • Strength • Trust",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightGold
                            )
                            Spacer(Modifier.height(14.dp))
                            GoldPrimaryButton(
                                text = "Shop Now →",
                                onClick = onNavigateToProducts,
                                testTag = "banner_shop_now_button"
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        ProductStudioImage(
                            imageUrl = "",
                            drawableKey = "img_stretch_film",
                            contentDescription = "UltraWrap Stretch Film Showcase",
                            modifier = Modifier.size(104.dp)
                        )
                    }
                }
            }
        }

        // Quick Action Tiles matching reference image
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ObsidianCard,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ObsidianOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionTile(
                        icon = Icons.Default.Storefront,
                        label = "Our Products",
                        testTag = "quick_our_products",
                        onClick = onNavigateToProducts
                    )
                    QuickActionTile(
                        icon = Icons.Default.LocalShipping,
                        label = "Track Order",
                        testTag = "quick_track_order",
                        onClick = onNavigateToOrders
                    )
                    QuickActionTile(
                        icon = Icons.Default.Phone,
                        label = "Contact Us",
                        testTag = "quick_contact_us",
                        onClick = onNavigateToContact
                    )
                    QuickActionTile(
                        icon = Icons.Default.Chat,
                        label = "WhatsApp",
                        testTag = "quick_whatsapp",
                        onClick = {
                            WhatsAppHelper.openGeneralSupport(
                                context = context,
                                whatsappNumber = settings.whatsappNumber,
                                customerName = customerName
                            )
                        }
                    )
                }
            }
        }

        // Our Products Horizontal Showcase
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Our Products",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onNavigateToProducts,
                    modifier = Modifier.testTag("view_all_products_button")
                ) {
                    Text("View All", color = MetallicGold)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MetallicGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        item {
            when (productsState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MetallicGold)
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard(
                        title = "Unable to load catalog",
                        subtitle = productsState.message
                    )
                }
                is UiState.Success -> {
                    val activeProducts = productsState.data.filter { it.active }
                    if (activeProducts.isEmpty()) {
                        EmptyStateCard(
                            title = "No products available",
                            subtitle = "Products managed by Admin will appear here."
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(activeProducts, key = { it.id }) { product ->
                                HomeProductCard(
                                    product = product,
                                    currency = settings.currencySymbol,
                                    onClick = { onProductClick(product) },
                                    onAdd = { onQuickAddToCart(product) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // My Recent Orders quick card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Orders",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToOrders) {
                    Text("All Orders", color = MetallicGold)
                }
            }
            Spacer(Modifier.height(6.dp))
            when (ordersState) {
                is UiState.Loading -> {
                    ObsidianSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = MetallicGold, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Loading your recent orders...", color = MutedSilver)
                        }
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard("Orders unavailable", ordersState.message)
                }
                is UiState.Success -> {
                    val latest = ordersState.data.firstOrNull()
                    if (latest == null) {
                        EmptyStateCard(
                            title = "No orders placed yet",
                            subtitle = "Explore our five official packaging lines and place your first order.",
                            actionLabel = "Browse Catalog",
                            onAction = onNavigateToProducts
                        )
                    } else {
                        ObsidianSectionCard(
                            modifier = Modifier.clickable(onClick = onNavigateToOrders)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "#${latest.orderNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = LightGold
                                    )
                                    Text(
                                        text = "${latest.items.size} item(s) • ${Formatters.formatCurrency(latest.totalAmount, settings.currencySymbol)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IvoryText
                                    )
                                    Text(
                                        text = Formatters.formatDate(latest.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedSilver
                                    )
                                }
                                OrderStatusBadge(latest.status)
                            }
                        }
                    }
                }
            }
        }

        // Contact Us & Direct WhatsApp Support Card
        item {
            ObsidianSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, null, tint = MetallicGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Bulk & Custom Packaging Enquiries",
                                style = MaterialTheme.typography.titleSmall,
                                color = PureWhite
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${settings.contactNumber} • ${settings.companyEmail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSilver
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    WhatsAppEnquiryChip(
                        label = "WhatsApp",
                        onClick = {
                            WhatsAppHelper.openGeneralSupport(
                                context = context,
                                whatsappNumber = settings.whatsappNumber,
                                customerName = customerName
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = GoldTintSurface,
            border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.65f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = LightGold,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IvoryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeProductCard(
    product: Product,
    currency: String,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(164.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("home_product_card_${product.id}"),
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            ProductStudioImage(
                imageUrl = product.imageUrl,
                drawableKey = product.drawableKey,
                contentDescription = product.name,
                modifier = Modifier.fillMaxWidth().height(112.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                color = PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.unit,
                style = MaterialTheme.typography.bodySmall,
                color = MutedSilver,
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Formatters.formatCurrency(product.priceInr, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = LightGold,
                    fontWeight = FontWeight.Bold
                )
                CircularGoldAddButton(
                    onClick = onAdd,
                    contentDescription = "Add ${product.name} to cart",
                    modifier = Modifier.size(40.dp).testTag("home_add_${product.id}")
                )
            }
        }
    }
}
