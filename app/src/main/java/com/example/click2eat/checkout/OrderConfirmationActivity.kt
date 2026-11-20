package com.example.click2eat.checkout

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.click2eat.R
import com.example.click2eat.database.CheckoutRepository
import java.util.Locale

class OrderConfirmationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "ORDER_ID"
        const val EXTRA_RESTAURANT_NAME = "RESTAURANT_NAME"
        const val EXTRA_TOTAL_CENTAVOS = "TOTAL_CENTAVOS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)

        val orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L)
        var restaurantName = intent.getStringExtra(EXTRA_RESTAURANT_NAME) ?: ""
        var totalCentavos = intent.getIntExtra(EXTRA_TOTAL_CENTAVOS, 0)
        var status = "Pending"

        // Re-fetch from SQLite to confirm persistence
        if (orderId > 0) {
            val savedOrder = CheckoutRepository(this).getOrderById(orderId)
            if (savedOrder != null) {
                restaurantName = savedOrder.restaurantName
                totalCentavos = savedOrder.totalAmountCentavos
                status = savedOrder.status
            }
        }

        findViewById<TextView>(R.id.tvConfirmOrderId).text =
            if (orderId > 0) String.format(Locale.US, "#%06d", orderId) else "#000001"
        findViewById<TextView>(R.id.tvConfirmRestaurant).text =
            restaurantName.ifEmpty { "Restaurant Placeholder" }
        findViewById<TextView>(R.id.tvConfirmTotal).text =
            String.format(Locale.US, "₱%,.2f", totalCentavos / 100.0)
        findViewById<TextView>(R.id.tvConfirmStatus).text = status

        // Transfers generated ORDER_ID to Member 5's OrderDetailsActivity via Explicit Intent
        findViewById<Button>(R.id.btnViewOrderDetails).setOnClickListener {
            try {
                val detailsClass = Class.forName("com.example.click2eat.orders.OrderDetailsActivity")
                val detailsIntent = Intent(this, detailsClass).apply {
                    putExtra("ORDER_ID", orderId)
                }
                startActivity(detailsIntent)
                finish()
            } catch (_: ClassNotFoundException) {
                // Safe fallback while Member 5 hasn't pushed OrderDetailsActivity yet
                Toast.makeText(
                    this,
                    "Order #${String.format(Locale.US, "%06d", orderId)} saved! (Waiting for Member 5's Order Details screen)",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}