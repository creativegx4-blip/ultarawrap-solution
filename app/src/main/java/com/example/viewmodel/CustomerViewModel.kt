package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UltraWrapRepository
import com.example.model.CartItem
import com.example.model.Category
import com.example.model.CompanySettings
import com.example.model.CustomerAddress
import com.example.model.CustomerProfile
import com.example.model.Order
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

class CustomerViewModel(
    private val repository: UltraWrapRepository,
    val currentUserId: String,
    val userEmail: String,
    val userDisplayName: String
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow("ALL")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _lastPlacedOrder = MutableStateFlow<Order?>(null)
    val lastPlacedOrder: StateFlow<Order?> = _lastPlacedOrder.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureOfficialCatalogSeeded()
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
                Log.w(TAG, "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val settingsState: StateFlow<UiState<CompanySettings>> =
        repository.observeSettings().toUiStateFlow("company settings")

    val categoriesState: StateFlow<UiState<List<Category>>> =
        repository.observeCategories().toUiStateFlow("categories")

    val productsState: StateFlow<UiState<List<Product>>> =
        repository.observeProducts().toUiStateFlow("products")

    val profileState: StateFlow<UiState<CustomerProfile?>> =
        repository.observeCustomerProfile(currentUserId).toUiStateFlow("profile")

    val addressesState: StateFlow<UiState<List<CustomerAddress>>> =
        repository.observeAddresses(currentUserId).toUiStateFlow("addresses")

    val cartState: StateFlow<UiState<List<CartItem>>> =
        repository.observeCart(currentUserId).toUiStateFlow("cart")

    val ordersState: StateFlow<UiState<List<Order>>> =
        repository.observeCustomerOrders(currentUserId).toUiStateFlow("orders")

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun clearLastPlacedOrder() {
        _lastPlacedOrder.value = null
    }

    fun saveProfile(
        fullName: String,
        mobileNumber: String,
        companyName: String,
        gstNumber: String,
        onDone: () -> Unit = {}
    ) {
        if (fullName.isBlank()) {
            _actionMessage.value = "Please enter your full name."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.upsertCustomerProfile(
                fullName = fullName,
                email = userEmail,
                mobileNumber = mobileNumber,
                companyName = companyName,
                gstNumber = gstNumber
            )
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Profile saved."
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to save profile."
            }
        }
    }

    fun toggleWishlist(productId: String, currentWishlist: List<String>) {
        viewModelScope.launch {
            val res = repository.toggleWishlist(productId, currentWishlist)
            res.onSuccess {
                val added = !currentWishlist.contains(productId)
                _actionMessage.value = if (added) "Added to Wishlist" else "Removed from Wishlist"
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Could not update Wishlist"
            }
        }
    }

    fun addToCart(product: Product, quantity: Long = 1L, onAdded: () -> Unit = {}) {
        if (product.stockQty <= 0L) {
            _actionMessage.value = "${product.name} is currently out of stock."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addToCart(product, quantity)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Added $quantity × ${product.name} to cart."
                onAdded()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to add to cart."
            }
        }
    }

    fun updateCartQuantity(productId: String, newQuantity: Long) {
        viewModelScope.launch {
            val res = repository.updateCartQuantity(productId, newQuantity)
            res.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to update quantity."
            }
        }
    }

    fun saveAddress(address: CustomerAddress, onDone: () -> Unit = {}) {
        if (address.recipientName.isBlank() ||
            address.mobileNumber.trim().length < 5 ||
            address.addressLine.trim().length < 3 ||
            address.city.isBlank() ||
            address.state.isBlank() ||
            address.pincode.trim().length < 4
        ) {
            _actionMessage.value = "Please enter recipient name, valid mobile, street address, city, state, and pincode."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveAddress(address)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Delivery address saved."
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to save address."
            }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            val res = repository.deleteAddress(addressId)
            res.onSuccess {
                _actionMessage.value = "Address removed."
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to delete address."
            }
        }
    }

    fun placeOrder(
        customerName: String,
        customerMobile: String,
        deliveryAddress: String,
        city: String,
        state: String,
        pincode: String,
        orderNotes: String,
        paymentMethod: String,
        cartItems: List<CartItem>,
        liveProducts: List<Product>,
        discount: Double,
        onSuccess: (Order) -> Unit
    ) {
        if (customerName.isBlank() ||
            customerMobile.trim().length < 5 ||
            deliveryAddress.trim().length < 3 ||
            city.isBlank() ||
            state.isBlank() ||
            pincode.trim().length < 4
        ) {
            _actionMessage.value = "Please fill in valid name, mobile number, delivery address, city, state, and pincode."
            return
        }
        if (cartItems.isEmpty()) {
            _actionMessage.value = "Your cart is empty."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.placeOrder(
                customerName = customerName,
                customerEmail = userEmail,
                customerMobile = customerMobile,
                deliveryAddress = deliveryAddress,
                city = city,
                state = state,
                pincode = pincode,
                orderNotes = orderNotes,
                paymentMethod = paymentMethod,
                cartItems = cartItems,
                liveProducts = liveProducts,
                discount = discount
            )
            _isBusy.value = false
            res.onSuccess { order ->
                _lastPlacedOrder.value = order
                onSuccess(order)
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to place order."
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.cancelCustomerOrder(orderId)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Order cancelled."
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Cannot cancel this order."
            }
        }
    }

    private companion object {
        const val TAG = "CustomerVM"
    }
}
