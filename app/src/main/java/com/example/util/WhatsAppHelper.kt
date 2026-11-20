package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.model.CartItem
import com.example.model.Order
import java.net.URLEncoder

object WhatsAppHelper {

    fun openProductEnquiry(
        context: Context,
        whatsappNumber: String,
        productName: String,
        quantity: Long,
        unit: String
    ) {
        val message = buildString {
            appendLine("Hello ULTRAWRAP SOLUTION,")
            appendLine()
            appendLine("I want to enquire about:")
            appendLine()
            appendLine("Product: $productName")
            appendLine("Quantity: $quantity $unit")
            appendLine()
            append("Please share the latest price and details.")
        }
        launchWhatsApp(context, whatsappNumber, message)
    }

    fun openCartEnquiry(
        context: Context,
        whatsappNumber: String,
        items: List<CartItem>,
        totalAmount: Double,
        currencySymbol: String
    ) {
        val message = buildString {
            appendLine("Hello ULTRAWRAP SOLUTION,")
            appendLine()
            appendLine("I want to enquire about my cart items:")
            appendLine()
            items.forEachIndexed { index, item ->
                appendLine("${index + 1}. ${item.productName} - ${item.quantity} ${item.unit} (${Formatters.formatCurrency(item.unitPrice * item.quantity, currencySymbol)})")
            }
            appendLine()
            appendLine("Estimated Total: ${Formatters.formatCurrency(totalAmount, currencySymbol)}")
            append("Please confirm availability and dispatch details.")
        }
        launchWhatsApp(context, whatsappNumber, message)
    }

    fun openOrderEnquiry(
        context: Context,
        whatsappNumber: String,
        order: Order,
        currencySymbol: String
    ) {
        val message = buildString {
            appendLine("Hello ULTRAWRAP SOLUTION,")
            appendLine()
            appendLine("Order Reference: #${order.orderNumber}")
            appendLine("Customer: ${order.customerName}")
            appendLine("Status: ${order.status}")
            appendLine("Total: ${Formatters.formatCurrency(order.totalAmount, currencySymbol)}")
            appendLine("Payment Mode: ${order.paymentMethod}")
            appendLine()
            append("Please share an update on my order.")
        }
        launchWhatsApp(context, whatsappNumber, message)
    }

    fun openGeneralSupport(
        context: Context,
        whatsappNumber: String,
        customerName: String? = null
    ) {
        val message = buildString {
            appendLine("Hello ULTRAWRAP SOLUTION,")
            appendLine()
            if (!customerName.isNullOrBlank()) {
                appendLine("Name: $customerName")
            }
            append("I need assistance with packaging products and pricing.")
        }
        launchWhatsApp(context, whatsappNumber, message)
    }

    fun launchWhatsApp(context: Context, rawNumber: String, message: String) {
        val digits = rawNumber.filter { it.isDigit() }.ifBlank { "919876543210" }
        val encoded = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$digits?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(
                context,
                "Unable to open WhatsApp or browser for $digits",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
