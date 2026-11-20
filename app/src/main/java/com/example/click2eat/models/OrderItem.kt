package com.example.click2eat.models

data class OrderItem(
    val id: Long = 0L,
    val orderId: Long = 0L,
    val menuItemId: Int,
    val foodName: String,
    val foodDescription: String = "",
    val imageResName: String = "",
    val quantity: Int,
    val priceCentavos: Int // Purchase-time unit price in centavos
) {
    val subtotalCentavos: Int
        get() = priceCentavos * quantity
}