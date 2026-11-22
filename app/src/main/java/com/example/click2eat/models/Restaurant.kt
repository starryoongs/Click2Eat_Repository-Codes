package com.example.click2eat.models

data class Restaurant(
    val id: Long,
    val name: String,
    val description: String?,
    val image: String?,
    val deliveryTime: String?
)