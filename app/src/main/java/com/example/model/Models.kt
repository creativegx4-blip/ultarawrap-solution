package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class OrderStatus(val label: String) {
    PROCESSING("Processing"),
    CONFIRMED("Confirmed"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: PROCESSING
    }
}

enum class InvoicePaymentStatus(val label: String) {
    PAID("Paid"),
    PENDING("Pending"),
    PARTIAL("Partial");

    companion object {
        fun fromRaw(raw: String?): InvoicePaymentStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class CustomerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val companyName: String = "",
    val gstNumber: String = "",
    val wishlistProductIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomerAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Warehouse",
    val recipientName: String = "",
    val mobileNumber: String = "",
    val addressLine: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val defaultAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val description: String = "",
    val priceInr: Double = 0.0,
    val unit: String = "Roll",
    val stockQty: Long = 0L,
    val sku: String = "",
    val drawableKey: String = "",
    val imageUrl: String = "",
    val active: Boolean = true,
    val featured: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val unit: String = "Roll",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val drawableKey: String = "",
    val imageUrl: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    val productId: String = "",
    val productName: String = "",
    val unit: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val lineTotal: Double = 0.0,
    val drawableKey: String = "",
    val imageUrl: String = ""
)

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerMobile: String = "",
    val deliveryAddress: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val orderNotes: String = "",
    val paymentMethod: String = "Cash on Delivery",
    val items: List<OrderLineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = OrderStatus.PROCESSING.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AccountingBook(
    @DocumentId val id: String = "",
    val name: String = "Main Book",
    val code: String = "MAIN",
    val description: String = "",
    val invoiceCount: Long = 0L,
    val totalBilled: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InvoiceLineItem(
    val productId: String = "",
    val productName: String = "",
    val unit: String = "Roll",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val lineTotal: Double = 0.0
)

@Keep
@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val invoiceNumber: String = "",
    val bookId: String = "",
    val bookName: String = "Main Book",
    val customerId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerMobile: String = "",
    val customerAddress: String = "",
    val customerGst: String = "",
    val items: List<InvoiceLineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val gstPercent: Double = 18.0,
    val gstAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentStatus: String = InvoicePaymentStatus.PAID.name,
    val notes: String = "",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminUser(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "admin",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CompanySettings(
    @DocumentId val id: String = "company",
    val companyName: String = "ULTRAWRAP SOLUTION",
    val tagline: String = "Wrap Smarter. Grow Stronger.",
    val companyEmail: String = "sales@ultrawrapsolution.com",
    val whatsappNumber: String = "919876543210",
    val contactNumber: String = "+91 98765 43210",
    val gstNumber: String = "24AABCU9603R1ZM",
    val companyAddress: String = "Plot 42, Industrial Packaging Park, Ahmedabad, Gujarat 382445",
    val invoiceFooter: String = "Thank you for choosing ULTRAWRAP SOLUTION — Wrap Smarter. Grow Stronger.",
    val currencySymbol: String = "₹",
    val defaultGstPercent: Double = 18.0,
    val catalogSeeded: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object OfficialCatalogSeed {
    data class SeedCategory(
        val id: String,
        val name: String,
        val description: String,
        val sortOrder: Long
    )

    data class SeedProduct(
        val id: String,
        val name: String,
        val categoryId: String,
        val categoryName: String,
        val description: String,
        val priceInr: Double,
        val unit: String,
        val stockQty: Long,
        val sku: String,
        val drawableKey: String
    )

    val categories = listOf(
        SeedCategory("cat_films", "Stretch Film", "Industrial pallet and hand stretch wrapping films", 1),
        SeedCategory("cat_cushioning", "Bubble Roll", "Protective air bubble cushioning rolls", 2),
        SeedCategory("cat_tapes", "BOPP Tape", "High-tack carton sealing adhesive tapes", 3),
        SeedCategory("cat_fasteners", "Cable Tie", "Heavy-duty nylon self-locking cable ties", 4),
        SeedCategory("cat_foam", "EPE Roll", "Lightweight expanded polyethylene foam rolls", 5)
    )

    val products = listOf(
        SeedProduct(
            id = "stretch_film",
            name = "Stretch Film",
            categoryId = "cat_films",
            categoryName = "Stretch Film",
            description = "High-quality stretch film for secure packaging and pallet wrapping. Superior cling, puncture resistance, and optical clarity for transit protection.",
            priceInr = 499.0,
            unit = "Roll",
            stockQty = 150L,
            sku = "UW-SF-001",
            drawableKey = "img_stretch_film"
        ),
        SeedProduct(
            id = "bubble_roll",
            name = "Bubble Roll",
            categoryId = "cat_cushioning",
            categoryName = "Bubble Roll",
            description = "Protective bubble packaging for safe transportation. Multi-layer air retention cells absorb shock and prevent surface scratches on fragile goods.",
            priceInr = 650.0,
            unit = "Roll",
            stockQty = 95L,
            sku = "UW-BR-002",
            drawableKey = "img_bubble_roll"
        ),
        SeedProduct(
            id = "bopp_tape",
            name = "BOPP Tape",
            categoryId = "cat_tapes",
            categoryName = "BOPP Tape",
            description = "Strong adhesive tape for carton sealing. Available in brown and transparent finishes with high shear strength for heavy corrugated boxes.",
            priceInr = 199.0,
            unit = "Pack (6 Rolls)",
            stockQty = 320L,
            sku = "UW-BT-003",
            drawableKey = "img_bopp_tape"
        ),
        SeedProduct(
            id = "cable_tie",
            name = "Cable Tie",
            categoryId = "cat_fasteners",
            categoryName = "Cable Tie",
            description = "Durable cable ties for securing and bundling materials. UV-resistant nylon 66 construction in black and natural white for industrial fastening.",
            priceInr = 149.0,
            unit = "Pack (100 Pcs)",
            stockQty = 400L,
            sku = "UW-CT-004",
            drawableKey = "img_cable_tie"
        ),
        SeedProduct(
            id = "epe_roll",
            name = "EPE Roll",
            categoryId = "cat_foam",
            categoryName = "EPE Roll",
            description = "Lightweight protective foam packaging material. Non-abrasive closed-cell cushioning ideal for electronics, furniture, glass, and automotive parts.",
            priceInr = 799.0,
            unit = "Roll",
            stockQty = 75L,
            sku = "UW-ER-005",
            drawableKey = "img_epe_roll"
        )
    )
}
