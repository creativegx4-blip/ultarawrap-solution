package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CompanySettings
import com.example.model.Product
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.ProductStudioImage
import com.example.ui.components.QuantityStepper
import com.example.ui.components.StockBadge
import com.example.ui.components.WhatsAppEnquiryChip
import com.example.ui.theme.GoldTintSurface
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    product: Product,
    settings: CompanySettings,
    isWishlisted: Boolean,
    onToggleWishlist: () -> Unit,
    onBack: () -> Unit,
    onAddToCart: (Long) -> Unit,
    onBuyNow: (Long) -> Unit
) {
    val context = LocalContext.current
    var quantity by remember(product.id) { mutableLongStateOf(1L) }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = { Text(product.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("product_details_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LightGold)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleWishlist, modifier = Modifier.testTag("product_wishlist_toggle")) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isWishlisted) "Remove from Wishlist" else "Add to Wishlist",
                            tint = MetallicGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBlack,
                    titleContentColor = PureWhite
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductStudioImage(
                imageUrl = product.imageUrl,
                drawableKey = product.drawableKey,
                contentDescription = product.name,
                modifier = Modifier.fillMaxWidth().height(260.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = GoldTintSurface,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${product.categoryName} • SKU: ${product.sku}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LightGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                StockBadge(stockQty = product.stockQty)
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = Formatters.formatCurrency(product.priceInr, settings.currencySymbol),
                    style = MaterialTheme.typography.displaySmall,
                    color = MetallicGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "/ ${product.unit}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MutedSilver,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianOutline)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = "Product Specifications",
                        style = MaterialTheme.typography.titleSmall,
                        color = LightGold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = IvoryText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Quantity (${product.unit})", style = MaterialTheme.typography.titleSmall, color = PureWhite)
                    Text(
                        text = "Total: ${Formatters.formatCurrency(product.priceInr * quantity, settings.currencySymbol)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightGold
                    )
                }
                QuantityStepper(
                    quantity = quantity,
                    onDecrement = { if (quantity > 1L) quantity-- },
                    onIncrement = { if (quantity < maxOf(product.stockQty, 1L)) quantity++ }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoldOutlineButton(
                    text = "Add to Cart",
                    onClick = { onAddToCart(quantity) },
                    testTag = "details_add_to_cart",
                    modifier = Modifier.weight(1f)
                )
                GoldPrimaryButton(
                    text = "Buy Now",
                    onClick = { onBuyNow(quantity) },
                    enabled = product.stockQty > 0L,
                    testTag = "details_buy_now",
                    modifier = Modifier.weight(1f)
                )
            }

            WhatsAppEnquiryChip(
                label = "WhatsApp Enquiry ($quantity ${product.unit})",
                onClick = {
                    WhatsAppHelper.openProductEnquiry(
                        context = context,
                        whatsappNumber = settings.whatsappNumber,
                        productName = product.name,
                        quantity = quantity,
                        unit = product.unit
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("details_whatsapp_enquiry")
            )
        }
    }
}
