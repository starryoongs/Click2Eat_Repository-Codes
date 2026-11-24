package com.example.click2eat.orders

import android.content.Intent
import android.os.Bundle
import java.io.File
import com.example.click2eat.database.UserRepository
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.click2eat.CartActivity
import com.example.click2eat.R
import com.example.click2eat.auth.ProfileActivity
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.DatabaseHelper
import com.example.click2eat.home.HomeActivity
import com.example.click2eat.home.RestaurantsActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID =
            "com.example.click2eat.EXTRA_ORDER_ID"
    }

    private lateinit var repository: OrderRepository
    private var userId = -1L
    private var orderId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)

        userId = SessionManager(this).getUserId()
        orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L)

        if (userId <= 0L || orderId <= 0L) {
            finish()
            return
        }

        repository = OrderRepository(this)

        findViewById<View>(R.id.orderDetailsBackButton)
            .setOnClickListener { finish() }

        findViewById<View>(R.id.orderDetailsMenu)
            .setOnClickListener { finish() }

        findViewById<View>(R.id.orderDetailsProfile)
            .setOnClickListener {
                startActivity(
                    Intent(this, ProfileActivity::class.java)
                )
            }

        findViewById<View>(R.id.detailsNavHome)
            .setOnClickListener {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }

        findViewById<View>(R.id.detailsNavCart)
            .setOnClickListener {
                startActivity(Intent(this, CartActivity::class.java))
                finish()
            }

        findViewById<View>(R.id.detailsNavRestaurants)
            .setOnClickListener {
                startActivity(Intent(this, RestaurantsActivity::class.java))
                finish()
            }

        findViewById<View>(R.id.detailsNavOrders)
            .setOnClickListener {
                startActivity(Intent(this, OrderHistoryActivity::class.java))
                finish()
            }

        findViewById<Button>(R.id.cancelOrderButton)
            .setOnClickListener { confirmCancel() }

        refreshDetails()
    }

    override fun onResume() {
        super.onResume()

        if (::repository.isInitialized &&
            userId > 0L &&
            orderId > 0L
        ) {
            loadProfilePicture()
            refreshDetails()
        }
    }

    private fun refreshDetails() {
        repository.refreshStatuses(userId)

        val order = repository.getOrder(userId, orderId)

        if (order == null) {
            Toast.makeText(
                this,
                "Order not found.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.orderDetailsHeading).text =
            "${order.restaurantName} • Order #${order.id}"

        findViewById<TextView>(R.id.orderDetailsStatus).text =
            "Status: ${order.status}"

        findViewById<TextView>(R.id.orderDetailsDate).text =
            "Placed: " + SimpleDateFormat(
                "MMM d, yyyy • h:mm a",
                Locale.getDefault()
            ).format(Date(order.createdAt))

        findViewById<TextView>(R.id.orderDetailsAddress).text =
            "Delivery address: ${order.deliveryAddress}"

        findViewById<TextView>(R.id.orderDetailsContact).text =
            "Contact: ${order.contactNumber}"

        findViewById<TextView>(R.id.orderDetailsTotal).text =
            "Total: ${money(order.totalAmountCentavos)}"

        // Restaurant logo
        val db = DatabaseHelper(this).readableDatabase
        val restaurantImageName = db.rawQuery(
            "SELECT image FROM restaurants WHERE id = ?",
            arrayOf(order.restaurantId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) ?: ""
            else ""
        }

        setDrawable(
            findViewById(R.id.imgDetailsRestaurant),
            restaurantImageName
        )

        // Food items
        val itemsBox =
            findViewById<LinearLayout>(R.id.orderDetailsItems)

        itemsBox.removeAllViews()

        repository.getItems(userId, orderId).forEach { item ->
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_order_detail, itemsBox, false)

            row.findViewById<TextView>(R.id.tvDetailItemName).text =
                item.foodName

            row.findViewById<TextView>(R.id.tvDetailItemDescription).text =
                item.foodDescription

            row.findViewById<TextView>(R.id.tvDetailItemQty).text =
                "Qty: ${item.quantity}"

            row.findViewById<TextView>(R.id.tvDetailItemPrice).text =
                "Subtotal: ${money(item.subtotalCentavos)}"

            setDrawable(
                row.findViewById(R.id.imgDetailFood),
                item.imageResName
            )

            itemsBox.addView(row)
        }

        findViewById<Button>(R.id.cancelOrderButton).visibility =
            if (order.status == "Pending") View.VISIBLE
            else View.GONE
    }

    private fun setDrawable(image: ImageView, name: String) {
        val resourceId = if (name.isNotBlank()) {
            resources.getIdentifier(name, "drawable", packageName)
        } else {
            0
        }

        image.setImageResource(
            if (resourceId != 0) resourceId
            else R.drawable.click2eat_logo_no_bg
        )
    }

    private fun money(centavos: Int): String =
        String.format(Locale.US, "₱%,.2f", centavos / 100.0)

    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle("Cancel order?")
            .setMessage("Are you sure you want to cancel this pending order?")
            .setNegativeButton("Keep order", null)
            .setPositiveButton("Cancel order") { _, _ ->
                val changed =
                    repository.cancelPendingOrder(userId, orderId)

                Toast.makeText(
                    this,
                    if (changed) "Order cancelled."
                    else "This order can no longer be cancelled.",
                    Toast.LENGTH_SHORT
                ).show()

                refreshDetails()
            }
            .show()
    }

    private fun loadProfilePicture() {
        val profileImage = findViewById<ImageView>(R.id.orderDetailsProfile) ?: return

        val user = UserRepository(this).getUserById(userId)

        val profileFile = user?.profilePicture
            ?.takeIf { it.isNotBlank() }
            ?.let { File(filesDir, it) }

        if (profileFile != null && profileFile.exists()) {
            profileImage.setImageURI(Uri.fromFile(profileFile))
        } else {
            profileImage.setImageResource(R.drawable.default_profile2)
        }
    }
}