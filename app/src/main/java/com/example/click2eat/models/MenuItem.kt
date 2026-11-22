package com.example.click2eat.models

data class MenuItem(
    val id: Long,
    val restaurantId: Long,
    val categoryId: Long?,
    val name: String,
    val description: String?,
    val priceCentavos: Int,
    val image: String?,
    val available: Boolean
)