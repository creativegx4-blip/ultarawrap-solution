package com.example.util

import com.example.R
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object Formatters {
    private val indianLocale = Locale("en", "IN")

    fun formatCurrency(amount: Double, symbol: String = "₹"): String {
        val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
            minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "$symbol${formatter.format(amount)}"
    }

    fun formatDate(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: Date()
        return SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(date)
    }

    fun formatDateTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: Date()
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(date)
    }

    fun resolveProductDrawable(key: String?): Int = when (key) {
        "img_stretch_film" -> R.drawable.img_stretch_film
        "img_bubble_roll" -> R.drawable.img_bubble_roll
        "img_bopp_tape" -> R.drawable.img_bopp_tape
        "img_cable_tie" -> R.drawable.img_cable_tie
        "img_epe_roll" -> R.drawable.img_epe_roll
        else -> R.drawable.img_stretch_film
    }

    fun roundTwoDecimals(value: Double): Double = (value * 100.0).roundToLong() / 100.0
}
