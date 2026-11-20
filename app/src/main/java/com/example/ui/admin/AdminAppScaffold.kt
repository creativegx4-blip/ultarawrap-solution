package com.example.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CompanySettings
import com.example.model.UiState
import com.example.ui.components.UltraWrapLogoBadge
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.GoldTintSurface
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

enum class AdminSection(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    PRODUCTS("Products", Icons.Default.Inventory2),
    CATEGORIES("Categories", Icons.Default.Category),
    ORDERS("Orders", Icons.Default.ReceiptLong),
    CUSTOMERS("Customers", Icons.Default.Group),
    CREATE_BILL("Create Bill", Icons.Default.MenuBook),
    REPORTS("Reports", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppScaffold(
    viewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val booksState by viewModel.booksState.collectAsStateWithLifecycle()
    val invoicesState by viewModel.invoicesState.collectAsStateWithLifecycle()
    val adminUsersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data ?: CompanySettings()
    val products = (productsState as? UiState.Success)?.data.orEmpty()
    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val customers = (customersState as? UiState.Success)?.data.orEmpty()
    val books = (booksState as? UiState.Success)?.data.orEmpty()
    val invoices = (invoicesState as? UiState.Success)?.data.orEmpty()
    val adminUsers = (adminUsersState as? UiState.Success)?.data.orEmpty()

    var activeSection by remember { mutableStateOf(AdminSection.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val drawerBody: @Composable (onItemSelected: () -> Unit) -> Unit = { closeDrawer ->
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                UltraWrapLogoBadge(size = 44.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("UltraWrap", style = MaterialTheme.typography.titleMedium, color = LightGold, fontWeight = FontWeight.Bold)
                    Text("ADMIN PANEL", style = MaterialTheme.typography.labelSmall, color = MetallicGold)
                }
            }
            Text(viewModel.adminEmail, style = MaterialTheme.typography.bodySmall, color = MutedSilver)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = ObsidianOutline)
            Spacer(Modifier.height(10.dp))

            AdminSection.entries.forEach { section ->
                NavigationDrawerItem(
                    label = { Text(section.label) },
                    selected = activeSection == section,
                    onClick = {
                        activeSection = section
                        closeDrawer()
                    },
                    icon = { Icon(section.icon, contentDescription = section.label) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = GoldTintSurface,
                        selectedIconColor = MetallicGold,
                        selectedTextColor = LightGold,
                        unselectedIconColor = MutedSilver,
                        unselectedTextColor = IvoryText
                    ),
                    modifier = Modifier.padding(vertical = 2.dp).testTag("admin_nav_${section.name.lowercase()}")
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = ObsidianOutline)
            Spacer(Modifier.height(8.dp))

            NavigationDrawerItem(
                label = { Text("Logout") },
                selected = false,
                onClick = onLogout,
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = CrimsonError) },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedIconColor = CrimsonError,
                    unselectedTextColor = CrimsonError
                ),
                modifier = Modifier.testTag("admin_logout_button")
            )
        }
    }

    val mainContent: @Composable (Modifier) -> Unit = { mod ->
        Box(modifier = mod.fillMaxSize()) {
            when (activeSection) {
                AdminSection.DASHBOARD -> AdminDashboardScreen(
                    settings = settings,
                    orders = orders,
                    products = products,
                    customers = customers,
                    invoices = invoices,
                    books = books,
                    onNavigateToOrders = { activeSection = AdminSection.ORDERS },
                    onNavigateToProducts = { activeSection = AdminSection.PRODUCTS },
                    onNavigateToCustomers = { activeSection = AdminSection.CUSTOMERS },
                    onNavigateToBilling = { activeSection = AdminSection.CREATE_BILL }
                )
                AdminSection.PRODUCTS -> AdminProductsScreen(
                    products = products,
                    categories = categories,
                    settings = settings,
                    onSaveProduct = { viewModel.saveProduct(it) },
                    onDeleteProduct = viewModel::deleteProduct
                )
                AdminSection.CATEGORIES -> AdminCategoriesScreen(
                    categories = categories,
                    onSaveCategory = { viewModel.saveCategory(it) },
                    onDeleteCategory = viewModel::deleteCategory
                )
                AdminSection.ORDERS -> AdminOrdersScreen(
                    orders = orders,
                    settings = settings,
                    onUpdateOrderStatus = viewModel::updateOrderStatus
                )
                AdminSection.CUSTOMERS -> AdminCustomersScreen(
                    customers = customers,
                    orders = orders,
                    settings = settings
                )
                AdminSection.CREATE_BILL -> AdminBillingScreen(
                    books = books,
                    customers = customers,
                    products = products,
                    invoices = invoices,
                    settings = settings,
                    isBusy = isBusy,
                    onCreateBook = { n, c, d -> viewModel.createAccountingBook(n, c, d) },
                    onGenerateInvoice = viewModel::createDirectInvoice
                )
                AdminSection.REPORTS -> AdminReportsScreen(
                    orders = orders,
                    invoices = invoices,
                    settings = settings
                )
                AdminSection.SETTINGS -> AdminSettingsScreen(
                    settings = settings,
                    adminUsers = adminUsers,
                    isBusy = isBusy,
                    onSaveSettings = viewModel::updateCompanySettings,
                    onAddAdmin = { email, name -> viewModel.addAuthorizedAdmin(email, name) },
                    onRemoveAdmin = viewModel::removeAuthorizedAdmin
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 720.dp
        if (isExpanded) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(240.dp),
                        drawerContainerColor = ObsidianSurface
                    ) {
                        drawerBody {}
                    }
                }
            ) {
                Scaffold(
                    containerColor = ObsidianBlack,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    mainContent(Modifier.padding(innerPadding))
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(260.dp),
                        drawerContainerColor = ObsidianSurface
                    ) {
                        drawerBody {
                            scope.launch { drawerState.close() }
                        }
                    }
                }
            ) {
                Scaffold(
                    containerColor = ObsidianBlack,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(activeSection.label, color = PureWhite, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Text("ADMIN", color = MetallicGold, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("admin_drawer_toggle")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Admin Menu", tint = LightGold)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianSurface)
                        )
                    }
                ) { innerPadding ->
                    mainContent(Modifier.padding(innerPadding))
                }
            }
        }
    }
}
