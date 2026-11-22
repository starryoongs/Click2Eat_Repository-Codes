package com.example.click2eat.orders

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.click2eat.R
import com.example.click2eat.auth.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetailsActivity : AppCompatActivity() {
    companion object { const val EXTRA_ORDER_ID = "com.example.click2eat.EXTRA_ORDER_ID" }
    private lateinit var repository: OrderRepository
    private var userId = -1L
    private var orderId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)
        userId = SessionManager(this).getUserId()
        orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L)
        if (userId <= 0L || orderId <= 0L) { finish(); return }
        repository = OrderRepository(this)
        findViewById<View>(R.id.orderDetailsBackButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.cancelOrderButton).setOnClickListener { confirmCancel() }
        refreshDetails()
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized && userId > 0L && orderId > 0L) refreshDetails()
    }

    private fun refreshDetails() {
        repository.refreshStatuses(userId)
        val order = repository.getOrder(userId, orderId)
        if (order == null) { Toast.makeText(this, "Order not found.", Toast.LENGTH_SHORT).show(); finish(); return }
        findViewById<TextView>(R.id.orderDetailsHeading).text = "Order #${order.id} • ${order.restaurantName}"
        findViewById<TextView>(R.id.orderDetailsStatus).text = "Status: ${order.status}"
        findViewById<TextView>(R.id.orderDetailsDate).text = "Placed: " + SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(order.createdAt))
        findViewById<TextView>(R.id.orderDetailsTotal).text = "Total: ₱${"%.2f".format(Locale.US, order.totalAmountCentavos / 100.0)}"
        findViewById<TextView>(R.id.orderDetailsAddress).text = "Delivery address: ${order.deliveryAddress}"
        findViewById<TextView>(R.id.orderDetailsContact).text = "Contact: ${order.contactNumber}"
        val itemsBox = findViewById<LinearLayout>(R.id.orderDetailsItems)
        itemsBox.removeAllViews()
        repository.getItems(userId, orderId).forEach { item ->
            val row = TextView(this).apply {
                text = "${item.foodName}  × ${item.quantity}\n" + "₱${"%.2f".format(Locale.US, 
                    item.priceCentavos / 100.0)} each  •  " +
                    "Subtotal ₱${"%.2f".format(Locale.US, item.subtotalCentavos / 100.0)}"
                textSize = 15f
                setPadding(0, 12, 0, 12)
            }
            itemsBox.addView(row)
        }
        findViewById<Button>(R.id.cancelOrderButton).apply {
            visibility = if (order.status == "Pending") View.VISIBLE else View.GONE
        }
    }

    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle("Cancel order?")
            .setMessage("Are you sure you want to cancel this pending order?")
            .setNegativeButton("Keep order", null)
            .setPositiveButton("Cancel order") { _, _ ->
                val changed = repository.cancelPendingOrder(userId, orderId)
                if (changed) Toast.makeText(this, "Order cancelled.", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this, "This order can no longer be cancelled.", Toast.LENGTH_SHORT).show()
                refreshDetails()
            }
            .show()
    }
}
