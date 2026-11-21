package com.example.click2eat.orders

import android.content.Context
import com.example.click2eat.auth.SessionManager

class OrderStatusManager(context: Context) {
    private val sessionManager = SessionManager(context.applicationContext)
    private val repository = OrderRepository(context.applicationContext)

    fun refreshCurrentUserOrders() {
        repository.advanceEligibleStatuses(sessionManager.getUserId())
    }
}
