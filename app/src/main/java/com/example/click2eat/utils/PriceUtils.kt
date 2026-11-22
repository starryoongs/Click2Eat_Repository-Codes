package com.example.click2eat.utils

import java.text.NumberFormat
import java.util.Locale

object PriceUtils {

    fun formatPrice(centavos: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("en-PH")
        )

        return formatter.format(centavos / 100.0)
    }

    fun calculateSubtotal(
        price: Int,
        quantity: Int
    ): Long {
        require(price >= 0) {
            "Price cannot be negative"
        }

        require(quantity > 0) {
            "Quantity must be greater than zero"
        }

        return price.toLong() * quantity
    }

    fun calculateTotal(
        subtotals: List<Long>
    ): Long {
        return subtotals.sum()
    }
}
