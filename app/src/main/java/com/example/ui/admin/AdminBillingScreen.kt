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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.model.AccountingBook
import com.example.model.CompanySettings
import com.example.model.CustomerProfile
import com.example.model.Invoice
import com.example.model.InvoiceLineItem
import com.example.model.InvoicePaymentStatus
import com.example.model.Product
import com.example.ui.components.EmptyStateCard
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
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.util.Formatters
import com.example.util.InvoicePdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBillingScreen(
    books: List<AccountingBook>,
    customers: List<CustomerProfile>,
    products: List<Product>,
    invoices: List<Invoice>,
    settings: CompanySettings,
    isBusy: Boolean,
    onCreateBook: (name: String, code: String, description: String) -> Unit,
    onGenerateInvoice: (
        book: AccountingBook,
        customerId: String,
        customerName: String,
        customerEmail: String,
        customerMobile: String,
        customerAddress: String,
        customerGst: String,
        items: List<InvoiceLineItem>,
        discount: Double,
        gstPercent: Double,
        paymentStatus: String,
        notes: String,
        onCreated: (Invoice) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currency = settings.currencySymbol.ifBlank { "₹" }

    val availableBooks = books.ifEmpty {
        listOf(AccountingBook(id = "main_book", name = "Main Book", code = "MAIN"))
    }
    var selectedBook by remember(books) { mutableStateOf(availableBooks.first()) }
    var bookExpanded by remember { mutableStateOf(false) }
    var showNewBookDialog by remember { mutableStateOf(false) }

    var selectedCustomerId by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }
    var customerMobile by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var customerGst by remember { mutableStateOf("") }
    var custExpanded by remember { mutableStateOf(false) }

    var selectedProduct by remember(products) { mutableStateOf(products.firstOrNull()) }
    var prodExpanded by remember { mutableStateOf(false) }
    var addQtyText by remember { mutableStateOf("1") }
    val billItems = remember { mutableStateListOf<InvoiceLineItem>() }

    var discountText by remember { mutableStateOf("0") }
    var gstPercentText by remember(settings.defaultGstPercent) { mutableStateOf(settings.defaultGstPercent.toString()) }
    var paymentStatus by remember { mutableStateOf(InvoicePaymentStatus.PAID.name) }
    var notes by remember { mutableStateOf("") }
    var generatedInvoiceDialog by remember { mutableStateOf<Invoice?>(null) }

    val subtotal = Formatters.roundTwoDecimals(billItems.sumOf { it.lineTotal })
    val discount = (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
    val gstPercent = (gstPercentText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 40.0)
    val taxable = (subtotal - discount).coerceAtLeast(0.0)
    val gstAmount = Formatters.roundTwoDecimals(taxable * gstPercent / 100.0)
    val grandTotal = Formatters.roundTwoDecimals(taxable + gstAmount)

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
            Text(
                text = "Create Bill from Book (Direct)",
                style = MaterialTheme.typography.headlineMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Admin-only accounting book invoicing with GST, PDF & Print",
                style = MaterialTheme.typography.bodySmall,
                color = MetallicGold
            )
        }

        // 1. Accounting Book Selection
        item {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("1. Select Accounting Book", style = MaterialTheme.typography.titleMedium, color = LightGold)
                        TextButton(onClick = { showNewBookDialog = true }) {
                            Text("+ New Book", color = MetallicGold)
                        }
                    }
                    ExposedDropdownMenuBox(expanded = bookExpanded, onExpandedChange = { bookExpanded = !bookExpanded }) {
                        OutlinedTextField(
                            value = "${selectedBook.name} (${selectedBook.code})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Accounting Book") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bookExpanded) },
                            colors = fieldColors,
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = bookExpanded, onDismissRequest = { bookExpanded = false }) {
                            availableBooks.forEach { bk ->
                                DropdownMenuItem(
                                    text = { Text("${bk.name} (${bk.code})") },
                                    onClick = {
                                        selectedBook = bk
                                        bookExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Customer Selection or Entry
        item {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Customer Details", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    if (customers.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = custExpanded, onExpandedChange = { custExpanded = !custExpanded }) {
                            OutlinedTextField(
                                value = if (selectedCustomerId.isBlank()) "Select existing customer (or type below)" else customerName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Registered Customer Lookup") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(custExpanded) },
                                colors = fieldColors,
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = custExpanded, onDismissRequest = { custExpanded = false }) {
                                customers.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.fullName} (${c.mobileNumber})") },
                                        onClick = {
                                            selectedCustomerId = c.userId
                                            customerName = c.fullName
                                            customerEmail = c.email
                                            customerMobile = c.mobileNumber
                                            customerGst = c.gstNumber
                                            custExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer / Business Name *") }, colors = fieldColors, modifier = Modifier.fillMaxWidth().testTag("bill_customer_name"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = customerMobile, onValueChange = { customerMobile = it }, label = { Text("Mobile *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = fieldColors, modifier = Modifier.weight(1f).testTag("bill_customer_mobile"))
                        OutlinedTextField(value = customerGst, onValueChange = { customerGst = it }, label = { Text("Customer GSTIN") }, colors = fieldColors, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = customerAddress, onValueChange = { customerAddress = it }, label = { Text("Billing Address") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // 3. Add Official & Custom Products
        item {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("3. Add Products to Bill", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    if (products.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = prodExpanded, onExpandedChange = { prodExpanded = !prodExpanded }) {
                            OutlinedTextField(
                                value = selectedProduct?.let { "${it.name} (${Formatters.formatCurrency(it.priceInr, currency)}/${it.unit})" } ?: "Select Product",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Product") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(prodExpanded) },
                                colors = fieldColors,
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = prodExpanded, onDismissRequest = { prodExpanded = false }) {
                                products.forEach { prod ->
                                    DropdownMenuItem(
                                        text = { Text("${prod.name} — ${Formatters.formatCurrency(prod.priceInr, currency)} / ${prod.unit}") },
                                        onClick = {
                                            selectedProduct = prod
                                            prodExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = addQtyText,
                                onValueChange = { addQtyText = it },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = fieldColors,
                                modifier = Modifier.width(100.dp).testTag("bill_qty_input")
                            )
                            GoldPrimaryButton(
                                text = "Add Item",
                                onClick = {
                                    val prod = selectedProduct ?: return@GoldPrimaryButton
                                    val qty = (addQtyText.toLongOrNull() ?: 1L).coerceAtLeast(1L)
                                    val idx = billItems.indexOfFirst { it.productId == prod.id }
                                    if (idx >= 0) {
                                        val existing = billItems[idx]
                                        val newQty = existing.quantity + qty
                                        billItems[idx] = existing.copy(
                                            quantity = newQty,
                                            lineTotal = Formatters.roundTwoDecimals(existing.unitPrice * newQty)
                                        )
                                    } else {
                                        billItems.add(
                                            InvoiceLineItem(
                                                productId = prod.id,
                                                productName = prod.name,
                                                unit = prod.unit,
                                                unitPrice = prod.priceInr,
                                                quantity = qty,
                                                lineTotal = Formatters.roundTwoDecimals(prod.priceInr * qty)
                                            )
                                        )
                                    }
                                },
                                testTag = "bill_add_item_button",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (billItems.isNotEmpty()) {
                        HorizontalDivider(color = ObsidianOutline)
                        billItems.forEachIndexed { idx, line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(line.productName, color = PureWhite, fontWeight = FontWeight.SemiBold)
                                    Text("${line.quantity} ${line.unit} × ${Formatters.formatCurrency(line.unitPrice, currency)}", color = MutedSilver, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(Formatters.formatCurrency(line.lineTotal, currency), color = LightGold, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { billItems.removeAt(idx) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove item", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Discount, GST, Total & Generate
        item {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("4. Discount, GST & Final Total", style = MaterialTheme.typography.titleMedium, color = LightGold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text("Discount ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gstPercentText,
                            onValueChange = { gstPercentText = it },
                            label = { Text("GST (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(InvoicePaymentStatus.entries) { st ->
                            FilterChip(
                                selected = paymentStatus == st.name,
                                onClick = { paymentStatus = st.name },
                                label = { Text(st.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MetallicGold,
                                    selectedLabelColor = ObsidianBlack,
                                    containerColor = ObsidianCard,
                                    labelColor = IvoryText
                                )
                            )
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Invoice Notes (Optional)") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())

                    HorizontalDivider(color = ObsidianOutline)
                    BillRow("Subtotal", Formatters.formatCurrency(subtotal, currency), IvoryText)
                    BillRow("Discount", "-${Formatters.formatCurrency(discount, currency)}", EmeraldSuccess)
                    BillRow("GST ($gstPercent%)", Formatters.formatCurrency(gstAmount, currency), IvoryText)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Grand Total", color = PureWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(Formatters.formatCurrency(grandTotal, currency), color = MetallicGold, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }

                    GoldPrimaryButton(
                        text = if (isBusy) "Generating Invoice..." else "Generate Invoice & Save to Book",
                        enabled = !isBusy && billItems.isNotEmpty(),
                        testTag = "generate_invoice_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onGenerateInvoice(
                                selectedBook,
                                selectedCustomerId,
                                customerName,
                                customerEmail,
                                customerMobile,
                                customerAddress,
                                customerGst,
                                billItems.toList(),
                                discount,
                                gstPercent,
                                paymentStatus,
                                notes
                            ) { createdInv ->
                                billItems.clear()
                                generatedInvoiceDialog = createdInv
                            }
                        }
                    )
                }
            }
        }

        // 5. Billing History
        item {
            Text("Billing History (${invoices.size})", style = MaterialTheme.typography.titleLarge, color = PureWhite, fontWeight = FontWeight.Bold)
        }

        if (invoices.isEmpty()) {
            item {
                EmptyStateCard("No Invoices Generated Yet", "Generated bills from Main Book or other books appear here.")
            }
        } else {
            items(invoices, key = { it.id }) { inv ->
                ObsidianSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("#${inv.invoiceNumber}", color = LightGold, fontWeight = FontWeight.Bold)
                                Text("${inv.customerName} • Book: ${inv.bookName}", color = PureWhite, style = MaterialTheme.typography.bodySmall)
                                Text("${Formatters.formatDate(inv.createdAt)} • ${inv.paymentStatus}", color = MutedSilver, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(Formatters.formatCurrency(inv.grandTotal, currency), color = MetallicGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldOutlineButton(
                                text = "PDF",
                                onClick = {
                                    val file = InvoicePdfGenerator.generateInvoicePdf(context, inv, settings)
                                    InvoicePdfGenerator.shareFile(context, file, "application/pdf", "Share Invoice #${inv.invoiceNumber}")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            GoldOutlineButton(
                                text = "Print",
                                onClick = {
                                    val file = InvoicePdfGenerator.generateInvoicePdf(context, inv, settings)
                                    InvoicePdfGenerator.printPdfFile(context, file, "UltraWrap_Invoice_${inv.invoiceNumber}")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewBookDialog) {
        var bName by remember { mutableStateOf("") }
        var bCode by remember { mutableStateOf("") }
        var bDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewBookDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("Create Accounting Book", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = bName, onValueChange = { bName = it }, label = { Text("Book Name (e.g. Wholesale Book)") }, colors = fieldColors)
                    OutlinedTextField(value = bCode, onValueChange = { bCode = it }, label = { Text("Short Code (e.g. WHSL)") }, colors = fieldColors)
                    OutlinedTextField(value = bDesc, onValueChange = { bDesc = it }, label = { Text("Description") }, colors = fieldColors)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreateBook(bName, bCode, bDesc)
                    showNewBookDialog = false
                }) { Text("Create", color = MetallicGold) }
            },
            dismissButton = { TextButton(onClick = { showNewBookDialog = false }) { Text("Cancel", color = MutedSilver) } }
        )
    }

    if (generatedInvoiceDialog != null) {
        val inv = generatedInvoiceDialog!!
        AlertDialog(
            onDismissRequest = { generatedInvoiceDialog = null },
            containerColor = ObsidianSurface,
            title = { Text("Invoice #${inv.invoiceNumber} Ready", color = LightGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Customer: ${inv.customerName}", color = PureWhite)
                    Text("Book: ${inv.bookName}", color = IvoryText)
                    Text("Grand Total: ${Formatters.formatCurrency(inv.grandTotal, currency)}", color = MetallicGold, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val file = InvoicePdfGenerator.generateInvoicePdf(context, inv, settings)
                    InvoicePdfGenerator.shareFile(context, file, "application/pdf", "Share Invoice #${inv.invoiceNumber}")
                }) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = MetallicGold)
                    Spacer(Modifier.width(4.dp))
                    Text("Download / Share PDF", color = MetallicGold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val file = InvoicePdfGenerator.generateInvoicePdf(context, inv, settings)
                    InvoicePdfGenerator.printPdfFile(context, file, "UltraWrap_Invoice_${inv.invoiceNumber}")
                }) {
                    Icon(Icons.Default.Print, null, tint = LightGold)
                    Spacer(Modifier.width(4.dp))
                    Text("Print", color = LightGold)
                }
            }
        )
    }
}

@Composable
private fun BillRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedSilver, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
