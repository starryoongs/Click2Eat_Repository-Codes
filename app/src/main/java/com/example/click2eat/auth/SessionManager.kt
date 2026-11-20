package com.example.click2eat.auth

import android.content.Context

class SessionManager(context: Context) {

    private val preferences = context.getSharedPreferences(
        "Click2EatSession",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_USER_ID = "user_id"
    }

    fun saveLoginSession(userId: Long) {
        preferences.edit()
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(): Long {
        return preferences.getLong(KEY_USER_ID, -1L)
    }

    fun isLoggedIn(): Boolean {
        return getUserId() != -1L
    }

    fun logout() {
        preferences.edit()
            .remove(KEY_USER_ID)
            .apply()
    }
}