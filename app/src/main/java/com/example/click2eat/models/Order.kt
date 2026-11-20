package com.example.click2eat.models

data class Order(
    val id: Long = 0L,
    val userId: Int,
    val restaurantId: Int,
    val restaurantName: String = "",
    val totalAmountCentavos: Int,
    val deliveryAddress: String,
    val contactNumber: String,
    val status: String = "Pending",
    val createdAt: Long = System.currentTimeMillis()
)