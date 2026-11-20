package com.example.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.Category
import com.example.model.CompanySettings
import com.example.model.Product
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.ObsidianSectionCard
import com.example.ui.components.ProductStudioImage
import com.example.ui.components.StockBadge
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.util.Formatters
import com.example.util.InvoicePdfGenerator

@Composable
fun AdminProductsScreen(
    products: List<Product>,
    categories: List<Category>,
    settings: CompanySettings,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Products (${products.size})", style = MaterialTheme.typography.headlineMedium, color = PureWhite, fontWeight = FontWeight.Bold)
                Text("Changes sync instantly to Customer App", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
            }
            GoldPrimaryButton(
                text = "+ Add",
                onClick = {
                    val firstCat = categories.firstOrNull()
                    editingProduct = Product(
                        categoryId = firstCat?.id ?: "cat_films",
                        categoryName = firstCat?.name ?: "Stretch Film",
                        drawableKey = "img_stretch_film"
                    )
                },
                testTag = "admin_add_product_button"
            )
        }

        if (products.isEmpty()) {
            EmptyStateCard(
                title = "No Products Found",
                subtitle = "Add your first UltraWrap packaging product.",
                actionLabel = "Add Product",
                onAction = { editingProduct = Product() }
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ObsidianSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProductStudioImage(
                                imageUrl = product.imageUrl,
                                drawableKey = product.drawableKey,
                                contentDescription = product.name,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(product.name, color = PureWhite, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (product.active) "ACTIVE" else "HIDDEN",
                                        color = if (product.active) EmeraldSuccess else CrimsonError,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    "${Formatters.formatCurrency(product.priceInr, settings.currencySymbol)} / ${product.unit} • SKU ${product.sku}",
                                    color = LightGold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(4.dp))
                                StockBadge(product.stockQty)
                            }
                            IconButton(
                                onClick = { editingProduct = product },
                                modifier = Modifier.testTag("admin_edit_product_${product.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit ${product.name}", tint = MetallicGold)
                            }
                            IconButton(
                                onClick = { onDeleteProduct(product.id) },
                                modifier = Modifier.testTag("admin_delete_product_${product.id}")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${product.name}", tint = CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingProduct != null) {
        AdminProductEditorDialog(
            initial = editingProduct!!,
            categories = categories,
            onDismiss = { editingProduct = null },
            onSave = { updated ->
                onSaveProduct(updated)
                editingProduct = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminProductEditorDialog(
    initial: Product,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial.name) }
    var description by remember { mutableStateOf(initial.description) }
    var priceText by remember { mutableStateOf(if (initial.priceInr > 0) initial.priceInr.toString() else "499") }
    var unit by remember { mutableStateOf(initial.unit.ifBlank { "Roll" }) }
    var stockText by remember { mutableStateOf(initial.stockQty.toString()) }
    var sku by remember { mutableStateOf(initial.sku) }
    var selectedCatId by remember { mutableStateOf(initial.categoryId.ifBlank { categories.firstOrNull()?.id ?: "cat_films" }) }
    var selectedCatName by remember { mutableStateOf(initial.categoryName.ifBlank { categories.firstOrNull()?.name ?: "Stretch Film" }) }
    var imageUrl by remember { mutableStateOf(initial.imageUrl) }
    var drawableKey by remember { mutableStateOf(initial.drawableKey.ifBlank { "img_stretch_film" }) }
    var active by remember { mutableStateOf(initial.active) }
    var featured by remember { mutableStateOf(initial.featured) }
    var catMenuExpanded by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = InvoicePdfGenerator.encodePickedImageToDataUri(context, uri)
            if (encoded != null) {
                imageUrl = encoded
            }
        }
    }

    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MetallicGold,
        unfocusedBorderColor = ObsidianOutline,
        focusedLabelColor = LightGold,
        unfocusedLabelColor = MutedSilver,
        focusedTextColor = PureWhite,
        unfocusedTextColor = IvoryText
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = { Text(if (initial.id.isBlank()) "Add Product" else "Edit ${initial.name}", color = PureWhite) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProductStudioImage(imageUrl, drawableKey, name, Modifier.size(64.dp))
                    GoldOutlineButton(
                        text = "Pick Image",
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") }, colors = colors)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description *") }, minLines = 2, colors = colors)

                ExposedDropdownMenuBox(
                    expanded = catMenuExpanded,
                    onExpandedChange = { catMenuExpanded = !catMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCatName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuExpanded) },
                        colors = colors,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCatId = cat.id
                                    selectedCatName = cat.name
                                    catMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit *") },
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU") },
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Product Active in Store", color = IvoryText)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = featured, onCheckedChange = { featured = it })
                    Text("Featured on Home Screen", color = IvoryText)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    initial.copy(
                        name = name,
                        description = description,
                        priceInr = priceText.toDoubleOrNull() ?: 0.0,
                        unit = unit,
                        stockQty = stockText.toLongOrNull() ?: 0L,
                        sku = sku,
                        categoryId = selectedCatId,
                        categoryName = selectedCatName,
                        imageUrl = imageUrl,
                        drawableKey = drawableKey,
                        active = active,
                        featured = featured
                    )
                )
            }) {
                Text("Save Product", color = MetallicGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedSilver) } }
    )
}

@Composable
fun AdminCategoriesScreen(
    categories: List<Category>,
    onSaveCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Categories (${categories.size})", style = MaterialTheme.typography.headlineMedium, color = PureWhite, fontWeight = FontWeight.Bold)
            GoldPrimaryButton(
                text = "+ Category",
                onClick = { editing = Category(sortOrder = (categories.size + 1).toLong()) }
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories, key = { it.id }) { cat ->
                ObsidianSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, color = PureWhite, fontWeight = FontWeight.Bold)
                            Text(cat.description, color = MutedSilver, style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = { editing = cat }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MetallicGold)
                            }
                            IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editing != null) {
        val cat = editing!!
        var name by remember { mutableStateOf(cat.name) }
        var desc by remember { mutableStateOf(cat.description) }
        var active by remember { mutableStateOf(cat.active) }
        val colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MetallicGold,
            unfocusedBorderColor = ObsidianOutline,
            focusedLabelColor = LightGold,
            unfocusedLabelColor = MutedSilver,
            focusedTextColor = PureWhite,
            unfocusedTextColor = IvoryText
        )
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = ObsidianSurface,
            title = { Text(if (cat.id.isBlank()) "New Category" else "Edit Category", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, colors = colors)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, colors = colors)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Active", color = IvoryText)
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveCategory(cat.copy(name = name, description = desc, active = active))
                    editing = null
                }) { Text("Save", color = MetallicGold) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel", color = MutedSilver) } }
        )
    }
}
