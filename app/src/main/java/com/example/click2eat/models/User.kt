
package com.example.click2eat.models

data class User(
    val id: Long,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val createdAt: String,
    val profilePicture: String? = null
)
