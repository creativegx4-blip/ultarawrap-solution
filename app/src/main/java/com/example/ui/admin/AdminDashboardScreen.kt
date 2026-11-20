package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AccountingBook
import com.example.model.CompanySettings
import com.example.model.CustomerProfile
import com.example.model.Invoice
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.ObsidianSectionCard
import com.example.ui.components.OrderStatusBadge
import com.example.ui.theme.AmberPending
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldTintSurface
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyShipped
import com.example.util.Formatters

@Composable
fun AdminDashboardScreen(
    settings: CompanySettings,
    orders: List<Order>,
    products: List<Product>,
    customers: List<CustomerProfile>,
    invoices: List<Invoice>,
    books: List<AccountingBook>,
    onNavigateToOrders: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToBilling: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = settings.currencySymbol.ifBlank { "₹" }
    val validOrderSales = orders.filter { it.status != OrderStatus.CANCELLED.name }.sumOf { it.totalAmount }
    val invoiceSales = invoices.sumOf { it.grandTotal }
    val totalSales = validOrderSales + invoiceSales
    val pendingOrdersCount = orders.count { it.status == OrderStatus.PROCESSING.name }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Admin Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${settings.companyName} • Live Business Overview",
                style = MaterialTheme.typography.bodySmall,
                color = MetallicGold
            )
        }

        // KPI Grid (2 rows x 3 / responsive)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        title = "Total Orders",
                        value = orders.size.toString(),
                        icon = Icons.Default.ShoppingBag,
                        accent = SkyShipped,
                        onClick = onNavigateToOrders,
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        title = "Total Sales",
                        value = Formatters.formatCurrency(totalSales, currency),
                        icon = Icons.Default.AttachMoney,
                        accent = EmeraldSuccess,
                        onClick = onNavigateToBilling,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        title = "Customers",
                        value = customers.size.toString(),
                        icon = Icons.Default.Group,
                        accent = LightGold,
                        onClick = onNavigateToCustomers,
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        title = "Products",
                        value = products.size.toString(),
                        icon = Icons.Default.Inventory2,
                        accent = MetallicGold,
                        onClick = onNavigateToProducts,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        title = "Pending Orders",
                        value = pendingOrdersCount.toString(),
                        icon = Icons.Default.HourglassTop,
                        accent = AmberPending,
                        onClick = onNavigateToOrders,
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        title = "Book Invoices",
                        value = invoices.size.toString(),
                        icon = Icons.Default.ReceiptLong,
                        accent = LightGold,
                        onClick = onNavigateToBilling,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Create Bill from Book (Direct) Signature Card matching reference image
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GoldTintSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, null, tint = MetallicGold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Create Bill from Book (Direct)",
                            style = MaterialTheme.typography.titleMedium,
                            color = LightGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Active Book: ${books.firstOrNull()?.name ?: "Main Book"} • Generate instant GST PDF invoices directly from your accounting book.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IvoryText
                    )
                    GoldPrimaryButton(
                        text = "Generate Bill →",
                        onClick = onNavigateToBilling,
                        testTag = "dashboard_create_bill_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Recent Orders
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Orders", style = MaterialTheme.typography.titleLarge, color = PureWhite, fontWeight = FontWeight.Bold)
                TextButton(onClick = onNavigateToOrders) {
                    Text("View All →", color = MetallicGold)
                }
            }
        }

        if (orders.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Customer Orders Yet",
                    subtitle = "Incoming orders from the Customer App will appear here in real time."
                )
            }
        } else {
            items(orders.take(5), key = { it.id }) { order ->
                ObsidianSectionCard(modifier = Modifier.clickable(onClick = onNavigateToOrders)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("#${order.orderNumber} • ${order.customerName}", color = PureWhite, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${Formatters.formatCurrency(order.totalAmount, currency)} • ${Formatters.formatDate(order.createdAt)}",
                                color = MutedSilver,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OrderStatusBadge(order.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MutedSilver)
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = PureWhite,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
