package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.example.R
import com.example.model.AccountingBook
import com.example.model.AdminUser
import com.example.model.CartItem
import com.example.model.Category
import com.example.model.CompanySettings
import com.example.model.CustomerAddress
import com.example.model.CustomerProfile
import com.example.model.Invoice
import com.example.model.InvoiceLineItem
import com.example.model.OfficialCatalogSeed
import com.example.model.Order
import com.example.model.OrderLineItem
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.util.Formatters
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UltraWrapRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.wrapFirestoreFlow(path: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser != null) {
                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
            }
            throw error
        }

    // --- Admin Role Verification ---

    suspend fun verifyIsAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        if (!user.isEmailVerified) return false
        val email = user.email?.lowercase()?.trim().orEmpty()
        val configuredAdminEmail = BuildConfig.ADMIN_EMAIL
            .lowercase()
            .trim()
            .takeUnless { it == "my_admin_email" }
            .orEmpty()
        if (email == "creativegx4@gmail.com" || (configuredAdminEmail.isNotBlank() && email == configuredAdminEmail)) {
            return true
        }
        return try {
            val byUid = db.collection("admin_users").document(user.uid).get().await()
            if (byUid.exists() && (byUid.getBoolean("active") != false)) return true
            if (email.isNotBlank()) {
                val byEmail = db.collection("admin_users").document(email).get().await()
                if (byEmail.exists() && (byEmail.getBoolean("active") != false)) return true
            }
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    // --- Initial Seeding (Settings, Categories, 5 Official Products, Main Book) ---

    suspend fun ensureOfficialCatalogSeeded(): Result<Unit> = try {
        requireUserId()
        val settingsRef = db.collection("settings").document("company")
        val settingsSnap = settingsRef.get().await()
        if (!settingsSnap.exists()) {
            val seedSettings = mapOf(
                "companyName" to "ULTRAWRAP SOLUTION",
                "tagline" to "Wrap Smarter. Grow Stronger.",
                "companyEmail" to "sales@ultrawrapsolution.com",
                "whatsappNumber" to "919876543210",
                "contactNumber" to "+91 98765 43210",
                "gstNumber" to "24AABCU9603R1ZM",
                "companyAddress" to "Plot 42, Industrial Packaging Park, Ahmedabad, Gujarat 382445",
                "invoiceFooter" to "Thank you for choosing ULTRAWRAP SOLUTION — Wrap Smarter. Grow Stronger.",
                "currencySymbol" to "₹",
                "defaultGstPercent" to 18.0,
                "catalogSeeded" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            settingsRef.set(seedSettings).await()
        }

        val existingProducts = db.collection("products").limit(1).get().await()
        if (existingProducts.isEmpty) {
            OfficialCatalogSeed.categories.forEach { cat ->
                val catRef = db.collection("categories").document(cat.id)
                if (!catRef.get().await().exists()) {
                    catRef.set(
                        mapOf(
                            "name" to cat.name,
                            "description" to cat.description,
                            "sortOrder" to cat.sortOrder,
                            "active" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            OfficialCatalogSeed.products.forEach { prod ->
                val prodRef = db.collection("products").document(prod.id)
                if (!prodRef.get().await().exists()) {
                    prodRef.set(
                        mapOf(
                            "name" to prod.name,
                            "categoryId" to prod.categoryId,
                            "categoryName" to prod.categoryName,
                            "description" to prod.description,
                            "priceInr" to prod.priceInr,
                            "unit" to prod.unit,
                            "stockQty" to prod.stockQty,
                            "sku" to prod.sku,
                            "drawableKey" to prod.drawableKey,
                            "active" to true,
                            "featured" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "products")
        Result.failure(e)
    }

    suspend fun ensureMainBookExists(): Result<Unit> = try {
        requireUserId()
        val mainBookRef = db.collection("books").document("main_book")
        if (!mainBookRef.get().await().exists()) {
            mainBookRef.set(
                mapOf(
                    "name" to "Main Book",
                    "code" to "MAIN",
                    "description" to "Default UltraWrap Solution primary accounting book",
                    "invoiceCount" to 0L,
                    "totalBilled" to 0.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "books/main_book")
        Result.failure(e)
    }

    // --- Shared Catalog & Settings Streams ---

    fun observeSettings(): Flow<CompanySettings> = flow {
        requireUserId()
        emitAll(
            db.collection("settings").document("company")
                .snapshots()
                .map { snap ->
                    snap.toObject(CompanySettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: CompanySettings()
                }
                .wrapFirestoreFlow("settings/company")
        )
    }

    fun observeCategories(): Flow<List<Category>> = flow {
        requireUserId()
        emitAll(
            db.collection("categories")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .wrapFirestoreFlow("categories")
        )
    }

    fun observeProducts(): Flow<List<Product>> = flow {
        requireUserId()
        emitAll(
            db.collection("products")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sku }
                }
                .wrapFirestoreFlow("products")
        )
    }

    // --- Customer Profile, Addresses, Wishlist & Cart ---

    fun observeCustomerProfile(userId: String): Flow<CustomerProfile?> = flow {
        requireUserId()
        emitAll(
            db.collection("customers").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(CustomerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .wrapFirestoreFlow("customers/$userId")
        )
    }

    suspend fun upsertCustomerProfile(
        fullName: String,
        email: String,
        mobileNumber: String,
        companyName: String = "",
        gstNumber: String = ""
    ): Result<Unit> {
        var docPath = "customers/unauthenticated"
        return try {
            val uid = requireUserId()
            docPath = "customers/$uid"
            val docRef = db.collection("customers").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to fullName.trim().take(100),
                    "email" to email.trim().take(150),
                    "mobileNumber" to mobileNumber.trim().take(20),
                    "companyName" to companyName.trim().take(120),
                    "gstNumber" to gstNumber.trim().take(25),
                    "wishlistProductIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "fullName" to fullName.trim().take(100),
                        "email" to email.trim().take(150),
                        "mobileNumber" to mobileNumber.trim().take(20),
                        "companyName" to companyName.trim().take(120),
                        "gstNumber" to gstNumber.trim().take(25),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleWishlist(productId: String, currentWishlist: List<String>): Result<Unit> {
        var docPath = "customers/unauthenticated"
        return try {
            val uid = requireUserId()
            docPath = "customers/$uid"
            val updated = if (currentWishlist.contains(productId)) {
                currentWishlist.filterNot { it == productId }
            } else {
                (currentWishlist + productId).distinct().take(50)
            }
            db.collection("customers").document(uid)
                .update(
                    mapOf(
                        "wishlistProductIds" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeAddresses(userId: String): Flow<List<CustomerAddress>> = flow {
        requireUserId()
        emitAll(
            db.collection("customers").document(userId).collection("addresses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CustomerAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.defaultAddress }
                }
                .wrapFirestoreFlow("customers/$userId/addresses")
        )
    }

    suspend fun saveAddress(address: CustomerAddress): Result<Unit> {
        var docPath = "customers/unauthenticated/addresses"
        return try {
            val uid = requireUserId()
            val col = db.collection("customers").document(uid).collection("addresses")
            val docId = address.id.ifBlank { "addr_${UUID.randomUUID().toString().take(10)}" }
            docPath = "customers/$uid/addresses/$docId"
            val docRef = col.document(docId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "label" to address.label.trim().ifBlank { "Warehouse" }.take(40),
                        "recipientName" to address.recipientName.trim().take(100),
                        "mobileNumber" to address.mobileNumber.trim().take(20),
                        "addressLine" to address.addressLine.trim().take(300),
                        "city" to address.city.trim().take(80),
                        "state" to address.state.trim().take(80),
                        "pincode" to address.pincode.trim().take(12),
                        "defaultAddress" to address.defaultAddress,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "label" to address.label.trim().ifBlank { "Warehouse" }.take(40),
                        "recipientName" to address.recipientName.trim().take(100),
                        "mobileNumber" to address.mobileNumber.trim().take(20),
                        "addressLine" to address.addressLine.trim().take(300),
                        "city" to address.city.trim().take(80),
                        "state" to address.state.trim().take(80),
                        "pincode" to address.pincode.trim().take(12),
                        "defaultAddress" to address.defaultAddress,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> {
        var docPath = "customers/unauthenticated/addresses/$addressId"
        return try {
            val uid = requireUserId()
            docPath = "customers/$uid/addresses/$addressId"
            db.collection("customers").document(uid).collection("addresses").document(addressId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("customers").document(userId).collection("cart")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.productName }
                }
                .wrapFirestoreFlow("customers/$userId/cart")
        )
    }

    suspend fun addToCart(product: Product, addQuantity: Long): Result<Unit> {
        var docPath = "customers/unauthenticated/cart/${product.id}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("customers").document(uid).collection("cart").document(product.id)
            docPath = docRef.path
            val existingSnap = docRef.get().await()
            val safeAdd = addQuantity.coerceIn(1L, 10000L)
            val safeCartImageUrl = if (product.imageUrl.startsWith("data:")) "" else product.imageUrl.take(1000)
            if (!existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "productId" to product.id,
                    "productName" to product.name.take(120),
                    "unit" to product.unit.take(40),
                    "unitPrice" to product.priceInr,
                    "quantity" to safeAdd,
                    "drawableKey" to product.drawableKey.take(80),
                    "imageUrl" to safeCartImageUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val currentQty = existingSnap.getLong("quantity") ?: 0L
                val newQty = (currentQty + safeAdd).coerceIn(1L, 10000L)
                docRef.update(
                    mapOf(
                        "productName" to product.name.take(120),
                        "unit" to product.unit.take(40),
                        "unitPrice" to product.priceInr,
                        "quantity" to newQty,
                        "drawableKey" to product.drawableKey.take(80),
                        "imageUrl" to safeCartImageUrl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(productId: String, newQuantity: Long): Result<Unit> {
        var docPath = "customers/unauthenticated/cart/$productId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("customers").document(uid).collection("cart").document(productId)
            docPath = docRef.path
            if (newQuantity <= 0L) {
                docRef.delete().await()
            } else {
                docRef.update(
                    mapOf(
                        "quantity" to newQuantity.coerceIn(1L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearCart(): Result<Unit> {
        var docPath = "customers/unauthenticated/cart"
        return try {
            val uid = requireUserId()
            docPath = "customers/$uid/cart"
            val snap = db.collection("customers").document(uid).collection("cart").get().await()
            val batch = db.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Customer Orders ---

    fun observeCustomerOrders(userId: String): Flow<List<Order>> = flow {
        requireUserId()
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow("orders")
        )
    }

    suspend fun placeOrder(
        customerName: String,
        customerEmail: String,
        customerMobile: String,
        deliveryAddress: String,
        city: String,
        state: String,
        pincode: String,
        orderNotes: String,
        paymentMethod: String,
        cartItems: List<CartItem>,
        liveProducts: List<Product>,
        discount: Double
    ): Result<Order> {
        var docPath = "orders"
        return try {
            val uid = requireUserId()
            require(cartItems.isNotEmpty()) { "Cart cannot be empty" }
            val productLookup = liveProducts.associateBy { it.id }

            // Authoritative price recalculation against live Firestore products
            val lineItems = cartItems.map { item ->
                val live = productLookup[item.productId]
                val verifiedUnitPrice = live?.priceInr ?: item.unitPrice
                val lineTotal = Formatters.roundTwoDecimals(verifiedUnitPrice * item.quantity)
                val rawImg = live?.imageUrl ?: item.imageUrl
                val safeOrderImageUrl = if (rawImg.startsWith("data:")) "" else rawImg.take(1000)
                mapOf(
                    "productId" to item.productId,
                    "productName" to (live?.name ?: item.productName),
                    "unit" to (live?.unit ?: item.unit),
                    "unitPrice" to verifiedUnitPrice,
                    "quantity" to item.quantity,
                    "lineTotal" to lineTotal,
                    "drawableKey" to (live?.drawableKey ?: item.drawableKey),
                    "imageUrl" to safeOrderImageUrl
                )
            }
            val subtotal = Formatters.roundTwoDecimals(
                lineItems.sumOf { (it["lineTotal"] as? Number)?.toDouble() ?: 0.0 }
            )
            require(subtotal > 0.0) { "Order subtotal must be greater than zero." }
            val safeDiscount = Formatters.roundTwoDecimals(discount.coerceIn(0.0, subtotal))
            val totalAmount = Formatters.roundTwoDecimals((subtotal - safeDiscount).coerceAtLeast(0.0))

            val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val orderNumber = "UW${(System.currentTimeMillis() % 1000000L).toString().padStart(6, '0')}"
            docPath = "orders/$orderId"

            val payload = mapOf(
                "orderNumber" to orderNumber,
                "userId" to uid,
                "customerName" to customerName.trim().take(100),
                "customerEmail" to customerEmail.trim().take(150),
                "customerMobile" to customerMobile.trim().take(20),
                "deliveryAddress" to deliveryAddress.trim().take(400),
                "city" to city.trim().take(80),
                "state" to state.trim().take(80),
                "pincode" to pincode.trim().take(12),
                "orderNotes" to orderNotes.trim().take(500),
                "paymentMethod" to paymentMethod,
                "items" to lineItems,
                "subtotal" to subtotal,
                "discount" to safeDiscount,
                "totalAmount" to totalAmount,
                "status" to OrderStatus.PROCESSING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("orders").document(orderId).set(payload).await()
            clearCart()

            val createdOrder = Order(
                id = orderId,
                orderNumber = orderNumber,
                userId = uid,
                customerName = customerName.trim(),
                customerEmail = customerEmail.trim(),
                customerMobile = customerMobile.trim(),
                deliveryAddress = deliveryAddress.trim(),
                city = city.trim(),
                state = state.trim(),
                pincode = pincode.trim(),
                orderNotes = orderNotes.trim(),
                paymentMethod = paymentMethod,
                items = lineItems.map {
                    OrderLineItem(
                        productId = it["productId"] as String,
                        productName = it["productName"] as String,
                        unit = it["unit"] as String,
                        unitPrice = (it["unitPrice"] as Number).toDouble(),
                        quantity = (it["quantity"] as Number).toLong(),
                        lineTotal = (it["lineTotal"] as Number).toDouble(),
                        drawableKey = it["drawableKey"] as String,
                        imageUrl = it["imageUrl"] as String
                    )
                },
                subtotal = subtotal,
                discount = safeDiscount,
                totalAmount = totalAmount,
                status = OrderStatus.PROCESSING.name
            )
            Result.success(createdOrder)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun cancelCustomerOrder(orderId: String): Result<Unit> {
        val docPath = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- Admin Streams & Mutations ---

    fun observeAllOrders(): Flow<List<Order>> = flow {
        requireUserId()
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow("orders")
        )
    }

    fun observeAllCustomers(): Flow<List<CustomerProfile>> = flow {
        requireUserId()
        emitAll(
            db.collection("customers")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CustomerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow("customers")
        )
    }

    fun observeBooks(): Flow<List<AccountingBook>> = flow {
        requireUserId()
        emitAll(
            db.collection("books")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AccountingBook::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name }
                }
                .wrapFirestoreFlow("books")
        )
    }

    fun observeInvoices(): Flow<List<Invoice>> = flow {
        requireUserId()
        emitAll(
            db.collection("invoices")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Invoice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreFlow("invoices")
        )
    }

    fun observeAdminUsers(): Flow<List<AdminUser>> = flow {
        requireUserId()
        emitAll(
            db.collection("admin_users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreFlow("admin_users")
        )
    }

    suspend fun saveProduct(product: Product): Result<Unit> {
        val docId = product.id.ifBlank { "prod_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
        val docPath = "products/$docId"
        return try {
            requireUserId()
            val docRef = db.collection("products").document(docId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "name" to product.name.trim().take(120),
                    "categoryId" to product.categoryId.ifBlank { "cat_films" },
                    "categoryName" to product.categoryName.ifBlank { "Stretch Film" }.take(80),
                    "description" to product.description.trim().take(1000),
                    "priceInr" to product.priceInr.coerceAtLeast(0.0),
                    "unit" to product.unit.trim().ifBlank { "Roll" }.take(40),
                    "stockQty" to product.stockQty.coerceAtLeast(0L),
                    "sku" to product.sku.trim().ifBlank { "UW-${docId.takeLast(4).uppercase()}" }.take(50),
                    "drawableKey" to product.drawableKey.ifBlank { "img_stretch_film" }.take(80),
                    "imageUrl" to product.imageUrl.take(250000),
                    "active" to product.active,
                    "featured" to product.featured,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to product.name.trim().take(120),
                        "categoryId" to product.categoryId.ifBlank { "cat_films" },
                        "categoryName" to product.categoryName.ifBlank { "Stretch Film" }.take(80),
                        "description" to product.description.trim().take(1000),
                        "priceInr" to product.priceInr.coerceAtLeast(0.0),
                        "unit" to product.unit.trim().ifBlank { "Roll" }.take(40),
                        "stockQty" to product.stockQty.coerceAtLeast(0L),
                        "sku" to product.sku.trim().ifBlank { "UW-${docId.takeLast(4).uppercase()}" }.take(50),
                        "drawableKey" to product.drawableKey.ifBlank { "img_stretch_film" }.take(80),
                        "imageUrl" to product.imageUrl.take(250000),
                        "active" to product.active,
                        "featured" to product.featured,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val docPath = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveCategory(category: Category): Result<Unit> {
        val docId = category.id.ifBlank { "cat_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
        val docPath = "categories/$docId"
        return try {
            requireUserId()
            val docRef = db.collection("categories").document(docId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "name" to category.name.trim().take(80),
                        "description" to category.description.trim().take(300),
                        "sortOrder" to category.sortOrder.coerceIn(0L, 1000L),
                        "active" to category.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to category.name.trim().take(80),
                        "description" to category.description.trim().take(300),
                        "sortOrder" to category.sortOrder.coerceIn(0L, 1000L),
                        "active" to category.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val docPath = "categories/$categoryId"
        return try {
            requireUserId()
            db.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> {
        val docPath = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createAccountingBook(name: String, code: String, description: String): Result<Unit> {
        val docId = "book_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val docPath = "books/$docId"
        return try {
            requireUserId()
            db.collection("books").document(docId).set(
                mapOf(
                    "name" to name.trim().take(100),
                    "code" to code.trim().uppercase().take(40),
                    "description" to description.trim().take(300),
                    "invoiceCount" to 0L,
                    "totalBilled" to 0.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createDirectInvoice(
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
        notes: String
    ): Result<Invoice> {
        var docPath = "invoices"
        return try {
            val uid = requireUserId()
            require(items.isNotEmpty()) { "Add at least one product to the bill." }
            val subtotal = Formatters.roundTwoDecimals(items.sumOf { it.lineTotal })
            require(subtotal > 0.0) { "Invoice subtotal must be greater than zero." }
            val safeDiscount = Formatters.roundTwoDecimals(discount.coerceIn(0.0, subtotal))
            val taxable = (subtotal - safeDiscount).coerceAtLeast(0.0)
            val safeGstPct = gstPercent.coerceIn(0.0, 40.0)
            val gstAmount = Formatters.roundTwoDecimals(taxable * safeGstPct / 100.0)
            val grandTotal = Formatters.roundTwoDecimals(taxable + gstAmount)

            val invId = "inv_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val invoiceNumber = "UW-${book.code.take(4)}-${(System.currentTimeMillis() % 100000L).toString().padStart(5, '0')}"
            docPath = "invoices/$invId"

            val itemMaps = items.map {
                mapOf(
                    "productId" to it.productId,
                    "productName" to it.productName,
                    "unit" to it.unit,
                    "unitPrice" to it.unitPrice,
                    "quantity" to it.quantity,
                    "lineTotal" to it.lineTotal
                )
            }

            val payload = mapOf(
                "invoiceNumber" to invoiceNumber,
                "bookId" to book.id.ifBlank { "main_book" },
                "bookName" to book.name.ifBlank { "Main Book" },
                "customerId" to customerId.take(128),
                "customerName" to customerName.trim().take(100),
                "customerEmail" to customerEmail.trim().take(150),
                "customerMobile" to customerMobile.trim().take(20),
                "customerAddress" to customerAddress.trim().take(400),
                "customerGst" to customerGst.trim().take(25),
                "items" to itemMaps,
                "subtotal" to subtotal,
                "discount" to safeDiscount,
                "gstPercent" to safeGstPct,
                "gstAmount" to gstAmount,
                "grandTotal" to grandTotal,
                "paymentStatus" to paymentStatus,
                "notes" to notes.trim().take(500),
                "createdByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("invoices").document(invId).set(payload).await()

            val bookId = book.id.ifBlank { "main_book" }
            val bookRef = db.collection("books").document(bookId)
            if (bookRef.get().await().exists()) {
                bookRef.update(
                    mapOf(
                        "invoiceCount" to FieldValue.increment(1L),
                        "totalBilled" to FieldValue.increment(grandTotal),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                bookRef.set(
                    mapOf(
                        "name" to book.name.ifBlank { "Main Book" },
                        "code" to book.code.ifBlank { "MAIN" },
                        "description" to book.description.ifBlank { "UltraWrap accounting book" },
                        "invoiceCount" to 1L,
                        "totalBilled" to grandTotal,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(
                Invoice(
                    id = invId,
                    invoiceNumber = invoiceNumber,
                    bookId = bookId,
                    bookName = book.name.ifBlank { "Main Book" },
                    customerId = customerId,
                    customerName = customerName.trim(),
                    customerEmail = customerEmail.trim(),
                    customerMobile = customerMobile.trim(),
                    customerAddress = customerAddress.trim(),
                    customerGst = customerGst.trim(),
                    items = items,
                    subtotal = subtotal,
                    discount = safeDiscount,
                    gstPercent = safeGstPct,
                    gstAmount = gstAmount,
                    grandTotal = grandTotal,
                    paymentStatus = paymentStatus,
                    notes = notes.trim(),
                    createdByUid = uid
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCompanySettings(settings: CompanySettings): Result<Unit> {
        val docPath = "settings/company"
        return try {
            requireUserId()
            db.collection("settings").document("company").set(
                mapOf(
                    "companyName" to settings.companyName.trim().ifBlank { "ULTRAWRAP SOLUTION" }.take(120),
                    "tagline" to settings.tagline.trim().take(150),
                    "companyEmail" to settings.companyEmail.trim().take(150),
                    "whatsappNumber" to settings.whatsappNumber.filter { it.isDigit() }.take(25),
                    "contactNumber" to settings.contactNumber.trim().take(25),
                    "gstNumber" to settings.gstNumber.trim().take(30),
                    "companyAddress" to settings.companyAddress.trim().take(400),
                    "invoiceFooter" to settings.invoiceFooter.trim().take(300),
                    "currencySymbol" to settings.currencySymbol.trim().ifBlank { "₹" }.take(10),
                    "defaultGstPercent" to settings.defaultGstPercent.coerceIn(0.0, 40.0),
                    "catalogSeeded" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addAuthorizedAdmin(email: String, displayName: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        val docPath = "admin_users/$cleanEmail"
        return try {
            requireUserId()
            require(cleanEmail.contains("@")) { "Enter a valid Gmail address." }
            db.collection("admin_users").document(cleanEmail).set(
                mapOf(
                    "email" to cleanEmail,
                    "displayName" to displayName.trim().ifBlank { "Administrator" }.take(100),
                    "role" to "admin",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeAuthorizedAdmin(adminKey: String): Result<Unit> {
        val docPath = "admin_users/$adminKey"
        return try {
            requireUserId()
            db.collection("admin_users").document(adminKey).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
