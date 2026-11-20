package com.example.click2eat.models

data class CartItem(
    val id: Int,
    val userId: Int,
    val menuItemId: Int,
    val restaurantId: Int,
    val restaurantName: String,
    val foodName: String,
    val foodDescription: String?,
    val price: Int,
    val quantity: Int,
    val image: String?,
    val restaurantImage: String? = null
) {
    val subtotal: Long
        get() = price.toLong() * quantity
}
