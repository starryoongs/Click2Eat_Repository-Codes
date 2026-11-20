package com.example.click2eat.utils

import android.util.Patterns

object ValidationUtils {

    fun isNotEmpty(value: String): Boolean {
        return value.trim().isNotEmpty()
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS
            .matcher(email.trim())
            .matches()
    }

    fun isValidUsername(username: String): Boolean {
        return username.matches(
            Regex("^[a-zA-Z0-9_]{3,30}$")
        )
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() }
    }

    fun passwordsMatch(
        password: String,
        confirmPassword: String
    ): Boolean {
        return password == confirmPassword
    }
}
