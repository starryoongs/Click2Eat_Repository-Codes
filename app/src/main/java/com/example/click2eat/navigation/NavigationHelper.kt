package com.example.click2eat.navigation

import android.app.Activity
import android.content.Intent
import com.example.click2eat.auth.ProfileActivity
import com.example.click2eat.MainActivity
import com.example.click2eat.orders.OrderHistoryActivity

object NavigationHelper {
    fun goHome(activity: Activity) = open(activity, MainActivity::class.java)
    fun goOrders(activity: Activity) = open(activity, OrderHistoryActivity::class.java)
    fun goProfile(activity: Activity) = open(activity, ProfileActivity::class.java)

    private fun open(activity: Activity, destination: Class<out Activity>) {
        if (activity::class.java == destination) return
        activity.startActivity(Intent(activity, destination))
    }
}
