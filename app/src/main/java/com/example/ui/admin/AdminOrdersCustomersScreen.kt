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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CompanySettings
import com.example.model.CustomerProfile
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ObsidianSectionCard
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.WhatsAppEnquiryChip
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.util.Formatters
import com.example.util.WhatsAppHelper

@Composable
fun AdminOrdersScreen(
    orders: List<Order>,
    settings: CompanySettings,
    onUpdateOrderStatus: (orderId: String, status: OrderStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("ALL") }
    var inspectingOrder by remember { mutableStateOf<Order?>(null) }

    val filters = listOf("ALL" to "All") + OrderStatus.entries.map { it.name to it.label }
    val filtered = orders.filter {
        (filterStatus == "ALL" || it.status == filterStatus) &&
            (search.isBlank() ||
                it.orderNumber.contains(search, true) ||
                it.customerName.contains(search, true) ||
                it.customerMobile.contains(search, true))
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Order Management (${orders.size})", style = MaterialTheme.typography.headlineMedium, color = PureWhite, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search #Order ID, customer, mobile...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MetallicGold) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard,
                focusedBorderColor = MetallicGold,
                unfocusedBorderColor = ObsidianOutline,
                focusedTextColor = PureWhite,
                unfocusedTextColor = IvoryText
            ),
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters, key = { it.first }) { (key, label) ->
                FilterChip(
                    selected = filterStatus == key,
                    onClick = { filterStatus = key },
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

        if (filtered.isEmpty()) {
            EmptyStateCard("No matching orders", "Customer orders will appear here when placed.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filtered, key = { it.id }) { order ->
                    ObsidianSectionCard(modifier = Modifier.clickable { inspectingOrder = order }) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("#${order.orderNumber}", color = LightGold, fontWeight = FontWeight.Bold)
                                OrderStatusBadge(order.status)
                            }
                            Text("${order.customerName} • ${order.customerMobile}", color = PureWhite)
                            Text(
                                "${order.items.size} items • ${Formatters.formatCurrency(order.totalAmount, settings.currencySymbol)} • ${order.paymentMethod}",
                                color = MutedSilver,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("Tap to manage status & view details", color = MetallicGold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    if (inspectingOrder != null) {
        val currentOrder = orders.firstOrNull { it.id == inspectingOrder!!.id } ?: inspectingOrder!!
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { inspectingOrder = null },
            containerColor = ObsidianSurface,
            title = { Text("Order #${currentOrder.orderNumber}", color = LightGold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Customer: ${currentOrder.customerName}", color = PureWhite, fontWeight = FontWeight.Bold)
                    Text("Email: ${currentOrder.customerEmail}", color = IvoryText, style = MaterialTheme.typography.bodySmall)
                    Text("Mobile: ${currentOrder.customerMobile}", color = IvoryText, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Address: ${currentOrder.deliveryAddress}, ${currentOrder.city}, ${currentOrder.state} - ${currentOrder.pincode}",
                        color = MutedSilver,
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider(color = ObsidianOutline)
                    currentOrder.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.productName} × ${item.quantity}", color = IvoryText, style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.formatCurrency(item.lineTotal, settings.currencySymbol), color = LightGold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider(color = ObsidianOutline)
                    Text("Total: ${Formatters.formatCurrency(currentOrder.totalAmount, settings.currencySymbol)} (${currentOrder.paymentMethod})", color = PureWhite, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(6.dp))
                    Text("Update Order Status", color = LightGold, style = MaterialTheme.typography.titleSmall)
                    OrderStatus.entries.forEach { st ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateOrderStatus(currentOrder.id, st) }
                                .testTag("admin_set_status_${st.name}"),
                            color = if (currentOrder.status == st.name) MetallicGold else ObsidianCard,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ObsidianOutline)
                        ) {
                            Text(
                                text = "Mark as ${st.label}",
                                color = if (currentOrder.status == st.name) ObsidianBlack else IvoryText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    WhatsAppEnquiryChip(
                        label = "Message Customer on WhatsApp",
                        onClick = {
                            WhatsAppHelper.openOrderEnquiry(
                                context = context,
                                whatsappNumber = currentOrder.customerMobile,
                                order = currentOrder,
                                currencySymbol = settings.currencySymbol
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectingOrder = null }) { Text("Close", color = MetallicGold) }
            }
        )
    }
}

@Composable
fun AdminCustomersScreen(
    customers: List<CustomerProfile>,
    orders: List<Order>,
    settings: CompanySettings,
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }
    var inspectingCustomer by remember { mutableStateOf<CustomerProfile?>(null) }

    val filtered = customers.filter {
        search.isBlank() ||
            it.fullName.contains(search, true) ||
            it.email.contains(search, true) ||
            it.mobileNumber.contains(search, true)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Customers (${customers.size})", style = MaterialTheme.typography.headlineMedium, color = PureWhite, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search customer name, email, mobile...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MetallicGold) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard,
                focusedBorderColor = MetallicGold,
                unfocusedBorderColor = ObsidianOutline,
                focusedTextColor = PureWhite,
                unfocusedTextColor = IvoryText
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (filtered.isEmpty()) {
            EmptyStateCard("No customers registered yet", "Signed-in customers appear here automatically.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { cust ->
                    val custOrders = orders.filter { it.userId == cust.userId }
                    val spent = custOrders.filter { it.status != OrderStatus.CANCELLED.name }.sumOf { it.totalAmount }
                    ObsidianSectionCard(modifier = Modifier.clickable { inspectingCustomer = cust }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(cust.fullName, color = PureWhite, fontWeight = FontWeight.Bold)
                                Text(cust.email, color = LightGold, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${cust.mobileNumber} • ${cust.companyName.ifBlank { "Individual" }}",
                                    color = MutedSilver,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${custOrders.size} Orders", color = IvoryText, style = MaterialTheme.typography.labelMedium)
                                Text(Formatters.formatCurrency(spent, settings.currencySymbol), color = MetallicGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (inspectingCustomer != null) {
        val cust = inspectingCustomer!!
        val custOrders = orders.filter { it.userId == cust.userId }
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { inspectingCustomer = null },
            containerColor = ObsidianSurface,
            title = { Text(cust.fullName, color = LightGold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Email: ${cust.email}", color = IvoryText)
                    Text("Mobile: ${cust.mobileNumber}", color = IvoryText)
                    if (cust.companyName.isNotBlank()) Text("Company: ${cust.companyName}", color = MutedSilver)
                    if (cust.gstNumber.isNotBlank()) Text("GSTIN: ${cust.gstNumber}", color = MutedSilver)
                    HorizontalDivider(color = ObsidianOutline)
                    Text("Order History (${custOrders.size})", color = LightGold, fontWeight = FontWeight.SemiBold)
                    if (custOrders.isEmpty()) {
                        Text("No orders placed yet.", color = MutedSilver)
                    } else {
                        custOrders.forEach { ord ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("#${ord.orderNumber} (${ord.status})", color = PureWhite, style = MaterialTheme.typography.bodySmall)
                                Text(Formatters.formatCurrency(ord.totalAmount, settings.currencySymbol), color = LightGold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (cust.mobileNumber.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        WhatsAppEnquiryChip(
                            label = "WhatsApp Customer",
                            onClick = {
                                WhatsAppHelper.openGeneralSupport(
                                    context = context,
                                    whatsappNumber = cust.mobileNumber,
                                    customerName = cust.fullName
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { inspectingCustomer = null }) { Text("Close", color = MetallicGold) } }
        )
    }
}
