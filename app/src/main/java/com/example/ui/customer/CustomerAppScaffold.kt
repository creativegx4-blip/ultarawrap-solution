package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CompanySettings
import com.example.model.Product
import com.example.model.UiState
import com.example.ui.auth.CompleteProfileDialogScreen
import com.example.ui.theme.GoldTintSurface
import com.example.ui.theme.LightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianSurface
import com.example.viewmodel.CustomerViewModel

enum class CustomerTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    PRODUCTS("Products", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    ORDERS("Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    CART("Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun CustomerAppScaffold(
    viewModel: CustomerViewModel,
    pendingFullName: String,
    pendingMobile: String,
    pendingCompany: String,
    onClearPendingSignup: () -> Unit,
    onLogout: () -> Unit
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val addressesState by viewModel.addressesState.collectAsStateWithLifecycle()
    val cartState by viewModel.cartState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val lastPlacedOrder by viewModel.lastPlacedOrder.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data ?: CompanySettings()
    val profile = (profileState as? UiState.Success)?.data
    val addresses = (addressesState as? UiState.Success)?.data.orEmpty()
    val cartItems = (cartState as? UiState.Success)?.data.orEmpty()
    val allProducts = (productsState as? UiState.Success)?.data.orEmpty()

    // Auto-persist Sign-Up details on first authentication
    LaunchedEffect(profileState, pendingFullName, pendingMobile) {
        if (profileState is UiState.Success && (profileState as UiState.Success).data == null && pendingFullName.isNotBlank()) {
            viewModel.saveProfile(
                fullName = pendingFullName,
                mobileNumber = pendingMobile,
                companyName = pendingCompany,
                gstNumber = "",
                onDone = onClearPendingSignup
            )
        }
    }

    // Require profile completion if user signed in via Login without an existing profile
    if (profileState is UiState.Success && profile == null && pendingFullName.isBlank()) {
        CompleteProfileDialogScreen(
            email = viewModel.userEmail,
            defaultName = viewModel.userDisplayName,
            isBusy = isBusy,
            onSave = { n, m, c ->
                viewModel.saveProfile(n, m, c, "")
            },
            onSignOut = onLogout
        )
        return
    }

    var currentTab by remember { mutableStateOf(CustomerTab.HOME) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showCheckout by remember { mutableStateOf(false) }
    var checkoutDiscount by remember { mutableDoubleStateOf(0.0) }
    var profileInitialSubPage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (showCheckout) {
        BackHandler {
            showCheckout = false
            viewModel.clearLastPlacedOrder()
        }
        CheckoutScreen(
            profile = profile,
            userEmail = viewModel.userEmail,
            addresses = addresses,
            cartItems = cartItems,
            liveProducts = allProducts,
            discount = checkoutDiscount,
            settings = settings,
            isBusy = isBusy,
            confirmedOrder = lastPlacedOrder,
            onBack = {
                showCheckout = false
                viewModel.clearLastPlacedOrder()
            },
            onPlaceOrder = { name, mobile, addr, city, state, pin, notes, payment ->
                viewModel.placeOrder(
                    customerName = name,
                    customerMobile = mobile,
                    deliveryAddress = addr,
                    city = city,
                    state = state,
                    pincode = pin,
                    orderNotes = notes,
                    paymentMethod = payment,
                    cartItems = cartItems,
                    liveProducts = allProducts,
                    discount = checkoutDiscount,
                    onSuccess = {}
                )
            },
            onViewOrders = {
                showCheckout = false
                viewModel.clearLastPlacedOrder()
                currentTab = CustomerTab.ORDERS
            },
            onContinueShopping = {
                showCheckout = false
                viewModel.clearLastPlacedOrder()
                currentTab = CustomerTab.PRODUCTS
            }
        )
        return
    }

    if (selectedProduct != null) {
        val liveProd = allProducts.firstOrNull { it.id == selectedProduct!!.id } ?: selectedProduct!!
        BackHandler { selectedProduct = null }
        ProductDetailsScreen(
            product = liveProd,
            settings = settings,
            isWishlisted = profile?.wishlistProductIds?.contains(liveProd.id) == true,
            onToggleWishlist = {
                viewModel.toggleWishlist(liveProd.id, profile?.wishlistProductIds.orEmpty())
            },
            onBack = { selectedProduct = null },
            onAddToCart = { qty ->
                viewModel.addToCart(liveProd, qty)
            },
            onBuyNow = { qty ->
                viewModel.addToCart(liveProd, qty) {
                    selectedProduct = null
                    checkoutDiscount = 0.0
                    showCheckout = true
                }
            }
        )
        return
    }

    val totalCartQty = cartItems.sumOf { it.quantity }.toInt()

    Scaffold(
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianSurface,
                contentColor = LightGold
            ) {
                CustomerTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                        icon = {
                            if (tab == CustomerTab.CART && totalCartQty > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MetallicGold, contentColor = ObsidianBlack) {
                                            Text(totalCartQty.toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MetallicGold,
                            selectedTextColor = LightGold,
                            indicatorColor = GoldTintSurface,
                            unselectedIconColor = MutedSilver,
                            unselectedTextColor = MutedSilver
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                CustomerTab.HOME -> {
                    CustomerHomeScreen(
                        profile = profile,
                        fallbackName = viewModel.userDisplayName,
                        settings = settings,
                        productsState = productsState,
                        ordersState = ordersState,
                        searchQuery = searchQuery,
                        onSearchChange = viewModel::updateSearchQuery,
                        onNavigateToProducts = { currentTab = CustomerTab.PRODUCTS },
                        onNavigateToOrders = { currentTab = CustomerTab.ORDERS },
                        onNavigateToContact = {
                            profileInitialSubPage = "contact"
                            currentTab = CustomerTab.PROFILE
                        },
                        onProductClick = { selectedProduct = it },
                        onQuickAddToCart = { viewModel.addToCart(it, 1L) }
                    )
                }
                CustomerTab.PRODUCTS -> {
                    ProductsScreen(
                        productsState = productsState,
                        categoriesState = categoriesState,
                        settings = settings,
                        searchQuery = searchQuery,
                        selectedCategoryId = selectedCategoryId,
                        onSearchChange = viewModel::updateSearchQuery,
                        onCategorySelect = viewModel::selectCategory,
                        onProductClick = { selectedProduct = it },
                        onAddToCart = { viewModel.addToCart(it, 1L) }
                    )
                }
                CustomerTab.ORDERS -> {
                    OrdersScreen(
                        ordersState = ordersState,
                        settings = settings,
                        onCancelOrder = viewModel::cancelOrder,
                        onBrowseProducts = { currentTab = CustomerTab.PRODUCTS }
                    )
                }
                CustomerTab.CART -> {
                    CartScreen(
                        cartState = cartState,
                        settings = settings,
                        onUpdateQuantity = viewModel::updateCartQuantity,
                        onContinueShopping = { currentTab = CustomerTab.PRODUCTS },
                        onProceedToCheckout = { discount ->
                            checkoutDiscount = discount
                            showCheckout = true
                        }
                    )
                }
                CustomerTab.PROFILE -> {
                    ProfileScreen(
                        profile = profile,
                        userEmail = viewModel.userEmail,
                        fallbackName = viewModel.userDisplayName,
                        addresses = addresses,
                        allProducts = allProducts,
                        settings = settings,
                        initialSubPage = profileInitialSubPage,
                        onClearInitialSubPage = { profileInitialSubPage = null },
                        onNavigateToOrders = { currentTab = CustomerTab.ORDERS },
                        onProductClick = { selectedProduct = it },
                        onSaveProfile = { n, m, c, g -> viewModel.saveProfile(n, m, c, g) },
                        onSaveAddress = viewModel::saveAddress,
                        onDeleteAddress = viewModel::deleteAddress,
                        onToggleWishlist = { prodId ->
                            viewModel.toggleWishlist(prodId, profile?.wishlistProductIds.orEmpty())
                        },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
