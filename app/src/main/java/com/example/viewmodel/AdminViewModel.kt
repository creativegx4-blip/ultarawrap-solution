package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UltraWrapRepository
import com.example.model.AccountingBook
import com.example.model.AdminUser
import com.example.model.Category
import com.example.model.CompanySettings
import com.example.model.CustomerProfile
import com.example.model.Invoice
import com.example.model.InvoiceLineItem
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: UltraWrapRepository,
    val adminUid: String,
    val adminEmail: String
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureOfficialCatalogSeeded()
            repository.ensureMainBookExists()
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Admin error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val settingsState: StateFlow<UiState<CompanySettings>> =
        repository.observeSettings().toUiStateFlow("settings")

    val productsState: StateFlow<UiState<List<Product>>> =
        repository.observeProducts().toUiStateFlow("products")

    val categoriesState: StateFlow<UiState<List<Category>>> =
        repository.observeCategories().toUiStateFlow("categories")

    val ordersState: StateFlow<UiState<List<Order>>> =
        repository.observeAllOrders().toUiStateFlow("orders")

    val customersState: StateFlow<UiState<List<CustomerProfile>>> =
        repository.observeAllCustomers().toUiStateFlow("customers")

    val booksState: StateFlow<UiState<List<AccountingBook>>> =
        repository.observeBooks().toUiStateFlow("books")

    val invoicesState: StateFlow<UiState<List<Invoice>>> =
        repository.observeInvoices().toUiStateFlow("invoices")

    val adminUsersState: StateFlow<UiState<List<AdminUser>>> =
        repository.observeAdminUsers().toUiStateFlow("admin_users")

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun saveProduct(product: Product, onSuccess: () -> Unit = {}) {
        if (product.name.isBlank() || product.description.isBlank() || product.unit.isBlank()) {
            _actionMessage.value = "Product name, description, and unit are required."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveProduct(product)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Saved ${product.name}."
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to save product."
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteProduct(productId)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Product deleted."
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to delete product."
            }
        }
    }

    fun saveCategory(category: Category, onSuccess: () -> Unit = {}) {
        if (category.name.isBlank()) {
            _actionMessage.value = "Category name is required."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCategory(category)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Saved category ${category.name}."
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to save category."
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val res = repository.deleteCategory(categoryId)
            res.onSuccess {
                _actionMessage.value = "Category deleted."
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to delete category."
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateOrderStatus(orderId, status)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Order marked as ${status.label}."
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to update order status."
            }
        }
    }

    fun createAccountingBook(name: String, code: String, description: String, onSuccess: () -> Unit = {}) {
        if (name.isBlank() || code.isBlank()) {
            _actionMessage.value = "Book name and code are required."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createAccountingBook(name, code, description)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Created accounting book '$name'."
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to create book."
            }
        }
    }

    fun createDirectInvoice(
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
    ) {
        if (customerName.isBlank()) {
            _actionMessage.value = "Please select or enter a customer name."
            return
        }
        if (items.isEmpty()) {
            _actionMessage.value = "Add at least one product to generate a bill."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createDirectInvoice(
                book = book,
                customerId = customerId,
                customerName = customerName,
                customerEmail = customerEmail,
                customerMobile = customerMobile,
                customerAddress = customerAddress,
                customerGst = customerGst,
                items = items,
                discount = discount,
                gstPercent = gstPercent,
                paymentStatus = paymentStatus,
                notes = notes
            )
            _isBusy.value = false
            res.onSuccess { inv ->
                _actionMessage.value = "Invoice #${inv.invoiceNumber} generated!"
                onCreated(inv)
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to generate invoice."
            }
        }
    }

    fun updateCompanySettings(settings: CompanySettings) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateCompanySettings(settings)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Company & WhatsApp settings saved."
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to save settings."
            }
        }
    }

    fun addAuthorizedAdmin(email: String, displayName: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addAuthorizedAdmin(email, displayName)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Authorized admin added."
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to add admin."
            }
        }
    }

    fun removeAuthorizedAdmin(adminKey: String) {
        viewModelScope.launch {
            val res = repository.removeAuthorizedAdmin(adminKey)
            res.onSuccess {
                _actionMessage.value = "Admin access revoked."
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to remove admin."
            }
        }
    }

    private companion object {
        const val TAG = "AdminVM"
    }
}
