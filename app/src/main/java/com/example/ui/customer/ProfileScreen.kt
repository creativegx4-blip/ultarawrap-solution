package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CompanySettings
import com.example.model.CustomerAddress
import com.example.model.CustomerProfile
import com.example.model.Product
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.ObsidianSectionCard
import com.example.ui.components.ProductStudioImage
import com.example.ui.components.UltraWrapLogoBadge
import com.example.ui.components.WhatsAppEnquiryChip
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldTintSurface
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

private enum class ProfileSubPage {
    MENU,
    ADDRESSES,
    WISHLIST,
    CONTACT_US,
    ABOUT_US,
    HELP_SUPPORT
}

@Composable
fun ProfileScreen(
    profile: CustomerProfile?,
    userEmail: String,
    fallbackName: String,
    addresses: List<CustomerAddress>,
    allProducts: List<Product>,
    settings: CompanySettings,
    initialSubPage: String? = null,
    onClearInitialSubPage: () -> Unit = {},
    onNavigateToOrders: () -> Unit,
    onProductClick: (Product) -> Unit,
    onSaveProfile: (fullName: String, mobile: String, company: String, gst: String) -> Unit,
    onSaveAddress: (CustomerAddress) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onToggleWishlist: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var subPage by remember(initialSubPage) {
        mutableStateOf(
            when (initialSubPage) {
                "contact" -> ProfileSubPage.CONTACT_US
                else -> ProfileSubPage.MENU
            }
        )
    }
    var showEditProfile by remember { mutableStateOf(false) }

    if (subPage != ProfileSubPage.MENU) {
        BackHandler {
            subPage = ProfileSubPage.MENU
            onClearInitialSubPage()
        }
    }

    when (subPage) {
        ProfileSubPage.ADDRESSES -> {
            AddressesSubScreen(
                addresses = addresses,
                onBack = { subPage = ProfileSubPage.MENU },
                onSaveAddress = onSaveAddress,
                onDeleteAddress = onDeleteAddress
            )
            return
        }
        ProfileSubPage.WISHLIST -> {
            WishlistSubScreen(
                wishlistIds = profile?.wishlistProductIds.orEmpty(),
                allProducts = allProducts,
                currency = settings.currencySymbol,
                onBack = { subPage = ProfileSubPage.MENU },
                onProductClick = onProductClick,
                onRemoveWishlist = onToggleWishlist
            )
            return
        }
        ProfileSubPage.CONTACT_US -> {
            ContactUsSubScreen(
                settings = settings,
                customerName = profile?.fullName ?: fallbackName,
                onBack = {
                    subPage = ProfileSubPage.MENU
                    onClearInitialSubPage()
                }
            )
            return
        }
        ProfileSubPage.ABOUT_US -> {
            AboutUsSubScreen(
                settings = settings,
                onBack = { subPage = ProfileSubPage.MENU }
            )
            return
        }
        ProfileSubPage.HELP_SUPPORT -> {
            HelpSupportSubScreen(
                settings = settings,
                customerName = profile?.fullName ?: fallbackName,
                onBack = { subPage = ProfileSubPage.MENU }
            )
            return
        }
        ProfileSubPage.MENU -> Unit
    }

    val displayName = profile?.fullName?.takeIf { it.isNotBlank() } ?: fallbackName.ifBlank { "Customer" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "My Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ObsidianCard,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = GoldTintSurface,
                    border = BorderStroke(1.5.dp, MetallicGold)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Customer Avatar",
                            tint = LightGold,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, style = MaterialTheme.typography.titleLarge, color = PureWhite, fontWeight = FontWeight.Bold)
                    Text(userEmail, style = MaterialTheme.typography.bodySmall, color = LightGold)
                    if (!profile?.mobileNumber.isNullOrBlank()) {
                        Text(profile!!.mobileNumber, style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                    }
                }
                IconButton(onClick = { showEditProfile = true }, modifier = Modifier.testTag("edit_profile_button")) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MetallicGold)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ObsidianCard,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ObsidianOutline)
        ) {
            Column {
                ProfileMenuItem(Icons.Default.ReceiptLong, "My Orders", onClick = onNavigateToOrders)
                ProfileMenuItem(Icons.Default.LocationOn, "My Addresses (${addresses.size})", onClick = { subPage = ProfileSubPage.ADDRESSES })
                ProfileMenuItem(Icons.Default.FavoriteBorder, "Wishlist (${profile?.wishlistProductIds?.size ?: 0})", onClick = { subPage = ProfileSubPage.WISHLIST })
                ProfileMenuItem(Icons.AutoMirrored.Filled.HelpOutline, "Help & Support", onClick = { subPage = ProfileSubPage.HELP_SUPPORT })
                ProfileMenuItem(Icons.Default.Phone, "Contact Us", onClick = { subPage = ProfileSubPage.CONTACT_US })
                ProfileMenuItem(Icons.Default.Info, "About Us", onClick = { subPage = ProfileSubPage.ABOUT_US })
                ProfileMenuItem(
                    icon = Icons.Default.Chat,
                    title = "WhatsApp Support",
                    iconTint = EmeraldSuccess,
                    titleColor = EmeraldSuccess,
                    onClick = {
                        WhatsAppHelper.openGeneralSupport(
                            context = context,
                            whatsappNumber = settings.whatsappNumber,
                            customerName = displayName
                        )
                    }
                )
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Logout",
                    iconTint = CrimsonError,
                    titleColor = CrimsonError,
                    testTag = "customer_logout_button",
                    onClick = onLogout
                )
            }
        }
    }

    if (showEditProfile) {
        EditProfileDialog(
            initialName = displayName,
            initialMobile = profile?.mobileNumber.orEmpty(),
            initialCompany = profile?.companyName.orEmpty(),
            initialGst = profile?.gstNumber.orEmpty(),
            onDismiss = { showEditProfile = false },
            onSave = { n, m, c, g ->
                onSaveProfile(n, m, c, g)
                showEditProfile = false
            }
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    iconTint: Color = MetallicGold,
    titleColor: Color = PureWhite,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedSilver)
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialMobile: String,
    initialCompany: String,
    initialGst: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var mobile by remember { mutableStateOf(initialMobile) }
    var company by remember { mutableStateOf(initialCompany) }
    var gst by remember { mutableStateOf(initialGst) }

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
        title = { Text("Edit Profile", color = PureWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, colors = colors)
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = colors
                )
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company Name") }, colors = colors)
                OutlinedTextField(value = gst, onValueChange = { gst = it }, label = { Text("GST Number (Optional)") }, colors = colors)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, mobile, company, gst) }) {
                Text("Save", color = MetallicGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MutedSilver) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressesSubScreen(
    addresses: List<CustomerAddress>,
    onBack: () -> Unit,
    onSaveAddress: (CustomerAddress) -> Unit,
    onDeleteAddress: (String) -> Unit
) {
    var editingAddress by remember { mutableStateOf<CustomerAddress?>(null) }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = { Text("My Addresses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LightGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack, titleContentColor = PureWhite)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingAddress = CustomerAddress() },
                containerColor = MetallicGold,
                contentColor = ObsidianBlack
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Address")
            }
        }
    ) { innerPadding ->
        if (addresses.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                EmptyStateCard(
                    title = "No Saved Addresses",
                    subtitle = "Save your warehouse or office delivery addresses for faster checkout.",
                    actionLabel = "Add Address",
                    onAction = { editingAddress = CustomerAddress() }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(addresses, key = { it.id }) { addr ->
                    ObsidianSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(addr.label, color = LightGold, fontWeight = FontWeight.Bold)
                                    if (addr.defaultAddress) {
                                        Spacer(Modifier.width(8.dp))
                                        Text("DEFAULT", color = EmeraldSuccess, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text(addr.recipientName, color = PureWhite, style = MaterialTheme.typography.bodyMedium)
                                Text(addr.mobileNumber, color = IvoryText, style = MaterialTheme.typography.bodySmall)
                                Text("${addr.addressLine}, ${addr.city}, ${addr.state} - ${addr.pincode}", color = MutedSilver, style = MaterialTheme.typography.bodySmall)
                            }
                            Row {
                                IconButton(onClick = { editingAddress = addr }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MetallicGold)
                                }
                                IconButton(onClick = { onDeleteAddress(addr.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingAddress != null) {
        AddressEditorDialog(
            initial = editingAddress!!,
            onDismiss = { editingAddress = null },
            onSave = { updated ->
                onSaveAddress(updated)
                editingAddress = null
            }
        )
    }
}

@Composable
private fun AddressEditorDialog(
    initial: CustomerAddress,
    onDismiss: () -> Unit,
    onSave: (CustomerAddress) -> Unit
) {
    var label by remember { mutableStateOf(initial.label) }
    var recipient by remember { mutableStateOf(initial.recipientName) }
    var mobile by remember { mutableStateOf(initial.mobileNumber) }
    var line by remember { mutableStateOf(initial.addressLine) }
    var city by remember { mutableStateOf(initial.city) }
    var state by remember { mutableStateOf(initial.state) }
    var pincode by remember { mutableStateOf(initial.pincode) }
    var isDefault by remember { mutableStateOf(initial.defaultAddress) }

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
        title = { Text(if (initial.id.isBlank()) "Add Delivery Address" else "Edit Address", color = PureWhite) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (e.g. Warehouse)") }, colors = colors)
                OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Recipient Name") }, colors = colors)
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile") }, colors = colors)
                OutlinedTextField(value = line, onValueChange = { line = it }, label = { Text("Street / Plot Address") }, colors = colors)
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, colors = colors)
                OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, colors = colors)
                OutlinedTextField(value = pincode, onValueChange = { pincode = it }, label = { Text("Pincode") }, colors = colors)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Text("Set as default address", color = IvoryText)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    initial.copy(
                        label = label,
                        recipientName = recipient,
                        mobileNumber = mobile,
                        addressLine = line,
                        city = city,
                        state = state,
                        pincode = pincode,
                        defaultAddress = isDefault
                    )
                )
            }) {
                Text("Save", color = MetallicGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedSilver) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishlistSubScreen(
    wishlistIds: List<String>,
    allProducts: List<Product>,
    currency: String,
    onBack: () -> Unit,
    onProductClick: (Product) -> Unit,
    onRemoveWishlist: (String) -> Unit
) {
    val saved = allProducts.filter { wishlistIds.contains(it.id) }
    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = { Text("My Wishlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LightGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack, titleContentColor = PureWhite)
            )
        }
    ) { innerPadding ->
        if (saved.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                EmptyStateCard(
                    title = "Wishlist is Empty",
                    subtitle = "Tap the heart icon on any product to bookmark it for quick reordering."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(saved, key = { it.id }) { prod ->
                    ObsidianSectionCard(modifier = Modifier.clickable { onProductClick(prod) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProductStudioImage(prod.imageUrl, prod.drawableKey, prod.name, Modifier.size(60.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(prod.name, color = PureWhite, fontWeight = FontWeight.Bold)
                                Text("${Formatters.formatCurrency(prod.priceInr, currency)} / ${prod.unit}", color = LightGold)
                            }
                            IconButton(onClick = { onRemoveWishlist(prod.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsSubScreen(
    settings: CompanySettings,
    customerName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = { Text("Contact Us") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LightGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack, titleContentColor = PureWhite)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(settings.companyName, style = MaterialTheme.typography.titleLarge, color = LightGold, fontWeight = FontWeight.Bold)
                    Text(settings.tagline, style = MaterialTheme.typography.bodyMedium, color = MetallicGold)
                    Spacer(Modifier.height(4.dp))
                    Text("Phone: ${settings.contactNumber}", color = PureWhite)
                    Text("WhatsApp: +${settings.whatsappNumber}", color = EmeraldSuccess)
                    Text("Email: ${settings.companyEmail}", color = IvoryText)
                    Text("GSTIN: ${settings.gstNumber}", color = MutedSilver)
                    Text("Address: ${settings.companyAddress}", color = MutedSilver)
                }
            }
            WhatsAppEnquiryChip(
                label = "Chat on WhatsApp Now",
                onClick = {
                    WhatsAppHelper.openGeneralSupport(
                        context = context,
                        whatsappNumber = settings.whatsappNumber,
                        customerName = customerName
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutUsSubScreen(
    settings: CompanySettings,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = { Text("About Us") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LightGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack, titleContentColor = PureWhite)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UltraWrapLogoBadge(size = 84.dp)
            Text(settings.companyName, style = MaterialTheme.typography.headlineSmall, color = PureWhite, fontWeight = FontWeight.Bold)
            Text(settings.tagline, style = MaterialTheme.typography.titleMedium, color = MetallicGold)
            ObsidianSectionCard {
                Text(
                    text = "${settings.companyName} supplies high-performance industrial packaging materials including Stretch Film, Bubble Rolls, BOPP Tapes, Nylon Cable Ties, and EPE Foam Rolls across India. Built for durability, consistent micron accuracy, and dependable B2B logistics.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = IvoryText
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpSupportSubScreen(
    settings: CompanySettings,
    customerName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LightGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack, titleContentColor = PureWhite)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ObsidianSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order & Bulk Pricing Support", color = LightGold, style = MaterialTheme.typography.titleMedium)
                    Text("• Bulk orders automatically receive volume trade discounts in the cart.", color = IvoryText)
                    Text("• Track real-time dispatch statuses inside My Orders.", color = IvoryText)
                    Text("• Use WhatsApp Enquiry on any product for custom widths, microns, or GST quotes.", color = IvoryText)
                }
            }
            GoldPrimaryButton(
                text = "Message Support on WhatsApp",
                onClick = {
                    WhatsAppHelper.openGeneralSupport(
                        context = context,
                        whatsappNumber = settings.whatsappNumber,
                        customerName = customerName
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
