package com.example.ui.customer

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.example.model.CompanySettings
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.ObsidianSectionCard
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.ProductStudioImage
import com.example.ui.components.WhatsAppEnquiryChip
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
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
fun OrdersScreen(
    ordersState: UiState<List<Order>>,
    settings: CompanySettings,
    onCancelOrder: (String) -> Unit,
    onBrowseProducts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    if (selectedOrder != null) {
        val currentOrder = (ordersState as? UiState.Success)?.data?.firstOrNull { it.id == selectedOrder!!.id }
            ?: selectedOrder!!
        BackHandler { selectedOrder = null }
        OrderTrackingDetailScreen(
            order = currentOrder,
            settings = settings,
            onBack = { selectedOrder = null },
            onCancelOrder = { onCancelOrder(currentOrder.id) }
        )
        return
    }

    val filterOptions = listOf("ALL" to "All") + OrderStatus.entries.map { it.name to it.label }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "My Orders",
            style = MaterialTheme.typography.headlineMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filterOptions, key = { it.first }) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetallicGold,
                        selectedLabelColor = ObsidianBlack,
                        containerColor = ObsidianCard,
                        labelColor = IvoryText
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when (ordersState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MetallicGold)
                }
            }
            is UiState.Error -> {
                EmptyStateCard("Failed to load orders", ordersState.message)
            }
            is UiState.Success -> {
                val filtered = ordersState.data.filter {
                    selectedFilter == "ALL" || it.status == selectedFilter
                }
                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        title = "No Orders Found",
                        subtitle = "Orders you place will appear here with live status updates.",
                        actionLabel = "Shop Packaging",
                        onAction = onBrowseProducts
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(filtered, key = { it.id }) { order ->
                            CustomerOrderCard(
                                order = order,
                                currency = settings.currencySymbol,
                                onTrackOrder = { selectedOrder = order }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerOrderCard(
    order: Order,
    currency: String,
    onTrackOrder: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onTrackOrder)
            .testTag("order_card_${order.orderNumber}"),
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.orderNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = LightGold,
                    fontWeight = FontWeight.Bold
                )
                OrderStatusBadge(order.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${Formatters.formatCurrency(order.totalAmount, currency)} • ${Formatters.formatDate(order.createdAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = IvoryText
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    order.items.take(3).forEach { item ->
                        ProductStudioImage(
                            imageUrl = item.imageUrl,
                            drawableKey = item.drawableKey,
                            contentDescription = item.productName,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                GoldPrimaryButton(
                    text = "Track Order →",
                    onClick = onTrackOrder,
                    testTag = "track_order_${order.orderNumber}"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderTrackingDetailScreen(
    order: Order,
    settings: CompanySettings,
    onBack: () -> Unit,
    onCancelOrder: () -> Unit
) {
    val context = LocalContext.current
    val status = OrderStatus.fromRaw(order.status)

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = { Text("Order #${order.orderNumber}") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("order_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LightGold)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Order Tracking Timeline", style = MaterialTheme.typography.titleMedium, color = LightGold)
                        OrderStatusBadge(order.status)
                    }
                    OrderTimeline(status = status)
                }
            }

            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Items Ordered", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductStudioImage(
                                imageUrl = item.imageUrl,
                                drawableKey = item.drawableKey,
                                contentDescription = item.productName,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, color = PureWhite, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${item.quantity} ${item.unit} × ${Formatters.formatCurrency(item.unitPrice, settings.currencySymbol)}",
                                    color = MutedSilver,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                Formatters.formatCurrency(item.lineTotal, settings.currencySymbol),
                                color = LightGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    HorizontalDivider(color = ObsidianOutline)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total (${order.paymentMethod})", color = PureWhite, fontWeight = FontWeight.Bold)
                        Text(
                            Formatters.formatCurrency(order.totalAmount, settings.currencySymbol),
                            color = LightGold,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Delivery Address", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    Text(order.customerName, color = PureWhite, fontWeight = FontWeight.SemiBold)
                    Text(order.customerMobile, color = IvoryText, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${order.deliveryAddress}, ${order.city}, ${order.state} - ${order.pincode}",
                        color = MutedSilver,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (order.orderNotes.isNotBlank()) {
                        Text("Notes: ${order.orderNotes}", color = LightGold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            WhatsAppEnquiryChip(
                label = "WhatsApp Support for #${order.orderNumber}",
                onClick = {
                    WhatsAppHelper.openOrderEnquiry(
                        context = context,
                        whatsappNumber = settings.whatsappNumber,
                        order = order,
                        currencySymbol = settings.currencySymbol
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (status == OrderStatus.PROCESSING) {
                OutlinedButton(
                    onClick = onCancelOrder,
                    border = BorderStroke(1.dp, CrimsonError),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cancel_order_button")
                ) {
                    Text("Cancel Order")
                }
            }
        }
    }
}

@Composable
private fun OrderTimeline(status: OrderStatus) {
    if (status == OrderStatus.CANCELLED) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(CrimsonError),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = PureWhite, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("This order has been cancelled.", color = CrimsonError, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val steps = listOf(
        OrderStatus.PROCESSING to "Order received & under review",
        OrderStatus.CONFIRMED to "Confirmed for packaging dispatch",
        OrderStatus.SHIPPED to "Dispatched & in transit",
        OrderStatus.DELIVERED to "Delivered to destination"
    )
    val activeIdx = steps.indexOfFirst { it.first == status }.coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { idx, (stepStatus, desc) ->
            val reached = idx <= activeIdx
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (reached) EmeraldSuccess else ObsidianElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (reached) {
                        Icon(Icons.Default.Check, null, tint = ObsidianBlack, modifier = Modifier.size(16.dp))
                    } else {
                        Text("${idx + 1}", color = MutedSilver, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stepStatus.label,
                        color = if (reached) PureWhite else MutedSilver,
                        fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(desc, color = MutedSilver, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
