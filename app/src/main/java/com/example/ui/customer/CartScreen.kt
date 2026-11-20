package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CartItem
import com.example.model.CompanySettings
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.ProductStudioImage
import com.example.ui.components.QuantityStepper
import com.example.ui.components.WhatsAppEnquiryChip
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.PureWhite
import com.example.util.Formatters
import com.example.util.WhatsAppHelper

@Composable
fun CartScreen(
    cartState: UiState<List<CartItem>>,
    settings: CompanySettings,
    onUpdateQuantity: (productId: String, newQty: Long) -> Unit,
    onContinueShopping: () -> Unit,
    onProceedToCheckout: (discount: Double) -> Unit,
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
            text = "My Cart",
            style = MaterialTheme.typography.headlineMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        when (cartState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MetallicGold)
                }
            }
            is UiState.Error -> {
                EmptyStateCard("Unable to load cart", cartState.message)
            }
            is UiState.Success -> {
                val items = cartState.data
                if (items.isEmpty()) {
                    EmptyStateCard(
                        title = "Your Cart is Empty",
                        subtitle = "Add Stretch Film, Bubble Roll, BOPP Tape, Cable Ties, or EPE Foam Rolls to get started.",
                        actionLabel = "Continue Shopping",
                        onAction = onContinueShopping
                    )
                } else {
                    val subtotal = Formatters.roundTwoDecimals(
                        items.sumOf { it.unitPrice * it.quantity }
                    )
                    // Automatic volume trade discount: 5% off orders ₹5,000+, or ₹50 off orders ₹600+
                    val discount = when {
                        subtotal >= 5000.0 -> Formatters.roundTwoDecimals(subtotal * 0.05)
                        subtotal >= 600.0 -> 50.0
                        else -> 0.0
                    }
                    val grandTotal = Formatters.roundTwoDecimals((subtotal - discount).coerceAtLeast(0.0))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(items, key = { it.productId }) { item ->
                            CartItemRow(
                                item = item,
                                currency = settings.currencySymbol,
                                onDecrement = { onUpdateQuantity(item.productId, item.quantity - 1L) },
                                onIncrement = { onUpdateQuantity(item.productId, item.quantity + 1L) },
                                onRemove = { onUpdateQuantity(item.productId, 0L) }
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ObsidianCard,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ObsidianOutline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PriceSummaryRow("Subtotal", Formatters.formatCurrency(subtotal, settings.currencySymbol), IvoryText)
                            PriceSummaryRow(
                                "Volume Discount",
                                if (discount > 0) "-${Formatters.formatCurrency(discount, settings.currencySymbol)}" else "₹0",
                                EmeraldSuccess
                            )
                            HorizontalDivider(color = ObsidianOutline)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Grand Total", style = MaterialTheme.typography.titleMedium, color = PureWhite, fontWeight = FontWeight.Bold)
                                Text(
                                    text = Formatters.formatCurrency(grandTotal, settings.currencySymbol),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = LightGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                            GoldPrimaryButton(
                                text = "Proceed to Checkout",
                                onClick = { onProceedToCheckout(discount) },
                                testTag = "proceed_to_checkout_button",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = onContinueShopping) {
                                    Text("Continue Shopping", color = LightGold)
                                }
                                WhatsAppEnquiryChip(
                                    label = "WhatsApp Cart",
                                    onClick = {
                                        WhatsAppHelper.openCartEnquiry(
                                            context = context,
                                            whatsappNumber = settings.whatsappNumber,
                                            items = items,
                                            totalAmount = grandTotal,
                                            currencySymbol = settings.currencySymbol
                                        )
                                    },
                                    modifier = Modifier.testTag("cart_whatsapp_button")
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    currency: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("cart_item_${item.productId}"),
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductStudioImage(
                imageUrl = item.imageUrl,
                drawableKey = item.drawableKey,
                contentDescription = item.productName,
                modifier = Modifier.size(68.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${Formatters.formatCurrency(item.unitPrice, currency)} / ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSilver
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuantityStepper(
                        quantity = item.quantity,
                        onDecrement = onDecrement,
                        onIncrement = onIncrement
                    )
                    Text(
                        text = Formatters.formatCurrency(item.unitPrice * item.quantity, currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = LightGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("remove_cart_${item.productId}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove ${item.productName}",
                    tint = CrimsonError
                )
            }
        }
    }
}

@Composable
private fun PriceSummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MutedSilver)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}
