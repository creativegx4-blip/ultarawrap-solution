package com.example.ui.admin

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AdminUser
import com.example.model.CompanySettings
import com.example.model.Invoice
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.ObsidianSectionCard
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
import com.example.util.InvoicePdfGenerator
import java.util.concurrent.TimeUnit

private enum class ReportDateFilter(val label: String, val days: Long?) {
    DAILY("Today", 1L),
    WEEKLY("Weekly (7d)", 7L),
    MONTHLY("Monthly (30d)", 30L),
    ALL("All Time", null)
}

@Composable
fun AdminReportsScreen(
    orders: List<Order>,
    invoices: List<Invoice>,
    settings: CompanySettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currency = settings.currencySymbol.ifBlank { "₹" }
    var selectedFilter by remember { mutableStateOf(ReportDateFilter.ALL) }

    val nowMs = System.currentTimeMillis()
    val cutoffMs = selectedFilter.days?.let { nowMs - TimeUnit.DAYS.toMillis(it) }

    val filteredOrders = orders.filter { o ->
        o.status != OrderStatus.CANCELLED.name &&
            (cutoffMs == null || (o.createdAt?.toDate()?.time ?: nowMs) >= cutoffMs)
    }
    val filteredInvoices = invoices.filter { inv ->
        cutoffMs == null || (inv.createdAt?.toDate()?.time ?: nowMs) >= cutoffMs
    }

    val orderRevenue = filteredOrders.sumOf { it.totalAmount }
    val invoiceRevenue = filteredInvoices.sumOf { it.grandTotal }
    val totalRevenue = orderRevenue + invoiceRevenue

    val productSalesMap = mutableMapOf<String, Double>()
    filteredOrders.forEach { o ->
        o.items.forEach { item ->
            productSalesMap[item.productName] = (productSalesMap[item.productName] ?: 0.0) + item.lineTotal
        }
    }
    filteredInvoices.forEach { inv ->
        inv.items.forEach { item ->
            productSalesMap[item.productName] = (productSalesMap[item.productName] ?: 0.0) + item.lineTotal
        }
    }
    val productSales = productSalesMap.entries.sortedByDescending { it.value }.map { it.key to it.value }

    val customerSalesMap = mutableMapOf<String, Double>()
    filteredOrders.forEach { o ->
        customerSalesMap[o.customerName] = (customerSalesMap[o.customerName] ?: 0.0) + o.totalAmount
    }
    filteredInvoices.forEach { inv ->
        customerSalesMap[inv.customerName] = (customerSalesMap[inv.customerName] ?: 0.0) + inv.grandTotal
    }
    val customerSales = customerSalesMap.entries.sortedByDescending { it.value }.map { it.key to it.value }

    val maxProdSale = productSales.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Reports & Analytics", style = MaterialTheme.typography.headlineMedium, color = PureWhite, fontWeight = FontWeight.Bold)
            Text("Real-time sales analytics from Orders & Book Invoices", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReportDateFilter.entries) { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f.label) },
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

        item {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Revenue (${selectedFilter.label})", color = MutedSilver, style = MaterialTheme.typography.labelLarge)
                    Text(Formatters.formatCurrency(totalRevenue, currency), color = MetallicGold, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Online Orders: ${filteredOrders.size} (${Formatters.formatCurrency(orderRevenue, currency)})", color = IvoryText, style = MaterialTheme.typography.bodySmall)
                        Text("Direct Bills: ${filteredInvoices.size} (${Formatters.formatCurrency(invoiceRevenue, currency)})", color = LightGold, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldPrimaryButton(
                            text = "Export PDF",
                            onClick = {
                                val pdf = InvoicePdfGenerator.generateSalesReportPdf(
                                    context = context,
                                    title = "Sales & Revenue Report",
                                    dateRangeLabel = selectedFilter.label,
                                    totalRevenue = totalRevenue,
                                    totalOrders = filteredOrders.size,
                                    totalInvoices = filteredInvoices.size,
                                    productSales = productSales,
                                    customerSales = customerSales,
                                    settings = settings
                                )
                                InvoicePdfGenerator.shareFile(context, pdf, "application/pdf", "Export UltraWrap Sales Report (PDF)")
                            },
                            testTag = "export_report_pdf",
                            modifier = Modifier.weight(1f)
                        )
                        GoldOutlineButton(
                            text = "Export Excel (CSV)",
                            onClick = {
                                val csv = InvoicePdfGenerator.generateSalesReportCsv(context, filteredOrders, filteredInvoices)
                                InvoicePdfGenerator.shareFile(context, csv, "text/csv", "Export UltraWrap Sales Report (CSV)")
                            },
                            testTag = "export_report_csv",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Product-wise Sales", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    if (productSales.isEmpty()) {
                        Text("No product sales recorded in this period.", color = MutedSilver)
                    } else {
                        productSales.forEach { (name, amt) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(name, color = PureWhite, style = MaterialTheme.typography.bodyMedium)
                                    Text(Formatters.formatCurrency(amt, currency), color = LightGold, fontWeight = FontWeight.SemiBold)
                                }
                                LinearProgressIndicator(
                                    progress = { (amt / maxProdSale).toFloat().coerceIn(0.05f, 1f) },
                                    color = MetallicGold,
                                    trackColor = ObsidianElevated,
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer-wise Sales", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    if (customerSales.isEmpty()) {
                        Text("No customer revenue recorded in this period.", color = MutedSilver)
                    } else {
                        customerSales.take(10).forEach { (name, amt) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, color = PureWhite, style = MaterialTheme.typography.bodyMedium)
                                Text(Formatters.formatCurrency(amt, currency), color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSettingsScreen(
    settings: CompanySettings,
    adminUsers: List<AdminUser>,
    isBusy: Boolean,
    onSaveSettings: (CompanySettings) -> Unit,
    onAddAdmin: (email: String, name: String) -> Unit,
    onRemoveAdmin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var companyName by remember(settings) { mutableStateOf(settings.companyName) }
    var tagline by remember(settings) { mutableStateOf(settings.tagline) }
    var companyEmail by remember(settings) { mutableStateOf(settings.companyEmail) }
    var whatsappNumber by remember(settings) { mutableStateOf(settings.whatsappNumber) }
    var contactNumber by remember(settings) { mutableStateOf(settings.contactNumber) }
    var gstNumber by remember(settings) { mutableStateOf(settings.gstNumber) }
    var companyAddress by remember(settings) { mutableStateOf(settings.companyAddress) }
    var invoiceFooter by remember(settings) { mutableStateOf(settings.invoiceFooter) }
    var currencySymbol by remember(settings) { mutableStateOf(settings.currencySymbol) }
    var defaultGst by remember(settings) { mutableStateOf(settings.defaultGstPercent.toString()) }

    var newAdminEmail by remember { mutableStateOf("") }
    var newAdminName by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MetallicGold,
        unfocusedBorderColor = ObsidianOutline,
        focusedLabelColor = LightGold,
        unfocusedLabelColor = MutedSilver,
        focusedTextColor = PureWhite,
        unfocusedTextColor = IvoryText
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Admin Settings", style = MaterialTheme.typography.headlineMedium, color = PureWhite, fontWeight = FontWeight.Bold)
            Text("Configure branding, WhatsApp number, GST, and authorized admins", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
        }

        item {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Company & WhatsApp Settings", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = whatsappNumber,
                        onValueChange = { whatsappNumber = it },
                        label = { Text("WhatsApp Number (with country code, e.g. 919876543210)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("settings_whatsapp_input")
                    )
                    OutlinedTextField(value = contactNumber, onValueChange = { contactNumber = it }, label = { Text("Contact Phone") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = companyEmail, onValueChange = { companyEmail = it }, label = { Text("Company Email") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = gstNumber, onValueChange = { gstNumber = it }, label = { Text("GST Number") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = companyAddress, onValueChange = { companyAddress = it }, label = { Text("Company Address") }, minLines = 2, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = currencySymbol, onValueChange = { currencySymbol = it }, label = { Text("Currency") }, colors = fieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = defaultGst,
                            onValueChange = { defaultGst = it },
                            label = { Text("Default GST %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(value = invoiceFooter, onValueChange = { invoiceFooter = it }, label = { Text("Invoice Footer Note") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())

                    GoldPrimaryButton(
                        text = if (isBusy) "Saving..." else "Save Company Settings",
                        enabled = !isBusy,
                        testTag = "save_settings_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    companyName = companyName,
                                    tagline = tagline,
                                    companyEmail = companyEmail,
                                    whatsappNumber = whatsappNumber,
                                    contactNumber = contactNumber,
                                    gstNumber = gstNumber,
                                    companyAddress = companyAddress,
                                    invoiceFooter = invoiceFooter,
                                    currencySymbol = currencySymbol,
                                    defaultGstPercent = defaultGst.toDoubleOrNull() ?: 18.0
                                )
                            )
                        }
                    )
                }
            }
        }

        item {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Authorized Admin Accounts", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    Text("Add additional administrator Gmail accounts who are allowed to access the Admin Panel.", color = MutedSilver, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = newAdminEmail, onValueChange = { newAdminEmail = it }, label = { Text("Admin Gmail Address") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newAdminName, onValueChange = { newAdminName = it }, label = { Text("Display Name") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    GoldOutlineButton(
                        text = "Authorize Admin Gmail",
                        onClick = {
                            onAddAdmin(newAdminEmail, newAdminName)
                            newAdminEmail = ""
                            newAdminName = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    adminUsers.forEach { adm ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(adm.displayName, color = PureWhite, fontWeight = FontWeight.SemiBold)
                                Text(adm.email, color = LightGold, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onRemoveAdmin(adm.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Admin", tint = CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }
}
