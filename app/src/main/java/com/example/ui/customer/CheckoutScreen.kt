package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CartItem
import com.example.model.CompanySettings
import com.example.model.CustomerAddress
import com.example.model.CustomerProfile
import com.example.model.Order
import com.example.model.Product
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.ObsidianSectionCard
import com.example.ui.components.WhatsAppEnquiryChip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    profile: CustomerProfile?,
    userEmail: String,
    addresses: List<CustomerAddress>,
    cartItems: List<CartItem>,
    liveProducts: List<Product>,
    discount: Double,
    settings: CompanySettings,
    isBusy: Boolean,
    confirmedOrder: Order?,
    onBack: () -> Unit,
    onPlaceOrder: (
        name: String,
        mobile: String,
        address: String,
        city: String,
        state: String,
        pincode: String,
        notes: String,
        paymentMethod: String
    ) -> Unit,
    onViewOrders: () -> Unit,
    onContinueShopping: () -> Unit
) {
    val context = LocalContext.current

    if (confirmedOrder != null) {
        OrderConfirmationView(
            order = confirmedOrder,
            settings = settings,
            onTrackOrder = onViewOrders,
            onContinueShopping = onContinueShopping
        )
        return
    }

    val defaultAddr = addresses.firstOrNull { it.defaultAddress } ?: addresses.firstOrNull()
    var customerName by remember(profile, defaultAddr) {
        mutableStateOf(defaultAddr?.recipientName?.ifBlank { null } ?: profile?.fullName.orEmpty())
    }
    var mobileNumber by remember(profile, defaultAddr) {
        mutableStateOf(defaultAddr?.mobileNumber?.ifBlank { null } ?: profile?.mobileNumber.orEmpty())
    }
    var deliveryAddress by remember(defaultAddr) {
        mutableStateOf(defaultAddr?.addressLine.orEmpty())
    }
    var city by remember(defaultAddr) { mutableStateOf(defaultAddr?.city.orEmpty()) }
    var state by remember(defaultAddr) { mutableStateOf(defaultAddr?.state.orEmpty()) }
    var pincode by remember(defaultAddr) { mutableStateOf(defaultAddr?.pincode.orEmpty()) }
    var orderNotes by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf("Cash on Delivery") }

    val paymentMethods = listOf(
        "Cash on Delivery",
        "Bank Transfer",
        "UPI",
        "Contact for Payment"
    )

    val subtotal = Formatters.roundTwoDecimals(cartItems.sumOf { it.unitPrice * it.quantity })
    val totalAmount = Formatters.roundTwoDecimals((subtotal - discount).coerceAtLeast(0.0))

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MetallicGold,
        unfocusedBorderColor = ObsidianOutline,
        focusedLabelColor = LightGold,
        unfocusedLabelColor = MutedSilver,
        focusedTextColor = PureWhite,
        unfocusedTextColor = IvoryText
    )

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("checkout_back")) {
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
            if (addresses.isNotEmpty()) {
                Text("Saved Addresses", style = MaterialTheme.typography.titleSmall, color = LightGold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    addresses.take(3).forEach { addr ->
                        FilterChip(
                            selected = deliveryAddress == addr.addressLine && pincode == addr.pincode,
                            onClick = {
                                customerName = addr.recipientName
                                mobileNumber = addr.mobileNumber
                                deliveryAddress = addr.addressLine
                                city = addr.city
                                state = addr.state
                                pincode = addr.pincode
                            },
                            label = { Text(addr.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetallicGold,
                                selectedLabelColor = ObsidianBlack,
                                containerColor = ObsidianCard,
                                labelColor = IvoryText
                            )
                        )
                    }
                }
            }

            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Delivery Details", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name *") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_name")
                    )
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Verified Email") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        label = { Text("Mobile Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_mobile")
                    )
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        label = { Text("Delivery Address *") },
                        minLines = 2,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_address")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City *") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f).testTag("checkout_city")
                        )
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State *") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f).testTag("checkout_state")
                        )
                    }
                    OutlinedTextField(
                        value = pincode,
                        onValueChange = { pincode = it },
                        label = { Text("Pincode *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_pincode")
                    )
                    OutlinedTextField(
                        value = orderNotes,
                        onValueChange = { orderNotes = it },
                        label = { Text("Order Notes / Dispatch Instructions (Optional)") },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_notes")
                    )
                }
            }

            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Payment Option", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    Text(
                        text = "Payment coordination is confirmed directly with your order invoice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                    paymentMethods.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPayment = method }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPayment == method,
                                onClick = { selectedPayment = method },
                                colors = RadioButtonDefaults.colors(selectedColor = MetallicGold)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(method, color = IvoryText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Order Summary (${cartItems.size} items)", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    cartItems.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.productName} × ${item.quantity}", color = IvoryText, style = MaterialTheme.typography.bodySmall)
                            Text(
                                Formatters.formatCurrency(item.unitPrice * item.quantity, settings.currencySymbol),
                                color = IvoryText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    HorizontalDivider(color = ObsidianOutline)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Payable", color = PureWhite, fontWeight = FontWeight.Bold)
                        Text(
                            Formatters.formatCurrency(totalAmount, settings.currencySymbol),
                            color = LightGold,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            GoldPrimaryButton(
                text = if (isBusy) "Placing Order..." else "Place Order",
                enabled = !isBusy && cartItems.isNotEmpty(),
                testTag = "place_order_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onPlaceOrder(
                        customerName,
                        mobileNumber,
                        deliveryAddress,
                        city,
                        state,
                        pincode,
                        orderNotes,
                        selectedPayment
                    )
                }
            )

            WhatsAppEnquiryChip(
                label = "WhatsApp Checkout Help",
                onClick = {
                    WhatsAppHelper.openCartEnquiry(
                        context = context,
                        whatsappNumber = settings.whatsappNumber,
                        items = cartItems,
                        totalAmount = totalAmount,
                        currencySymbol = settings.currencySymbol
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OrderConfirmationView(
    order: Order,
    settings: CompanySettings,
    onTrackOrder: () -> Unit,
    onContinueShopping: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = RoundedCornerShape(999.dp),
            color = EmeraldSurface,
            border = BorderStroke(2.dp, EmeraldSuccess)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Order Confirmed",
                tint = EmeraldSuccess,
                modifier = Modifier.padding(18.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Order Placed Successfully!",
            style = MaterialTheme.typography.headlineMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Order ID: #${order.orderNumber}",
            style = MaterialTheme.typography.titleLarge,
            color = MetallicGold,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Total: ${Formatters.formatCurrency(order.totalAmount, settings.currencySymbol)} • ${order.paymentMethod}",
            style = MaterialTheme.typography.bodyLarge,
            color = IvoryText
        )
        Spacer(Modifier.height(24.dp))
        GoldPrimaryButton(
            text = "Track Order",
            onClick = onTrackOrder,
            testTag = "confirmation_track_order",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        WhatsAppEnquiryChip(
            label = "Send Order on WhatsApp",
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
        Spacer(Modifier.height(12.dp))
        GoldOutlineButton(
            text = "Continue Shopping",
            onClick = onContinueShopping,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
