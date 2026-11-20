package com.example.click2eat.checkout

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.click2eat.R
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.CheckoutRepository
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESTAURANT_ID = "RESTAURANT_ID"
    }

    private lateinit var checkoutRepository: CheckoutRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var tvRestaurantName: TextView
    private lateinit var tvDeliveryTime: TextView
    private lateinit var llOrderItemsContainer: LinearLayout
    private lateinit var etDeliveryAddress: EditText
    private lateinit var etContactNumber: EditText
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnPlaceOrder: Button

    private var currentUserId: Int = -1
    private var activeRestaurantId: Int = -1
    private var isPlacingOrder: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        checkoutRepository = CheckoutRepository(this)
        sessionManager = SessionManager(this)

        // Fallback to userId = 1 during standalone testing if no login session is active yet
        currentUserId = getLoggedInUserId()
        activeRestaurantId = intent.getIntExtra(EXTRA_RESTAURANT_ID, -1)

        bindViews()
        loadCheckoutData()
    }

    private fun bindViews() {
        tvRestaurantName = findViewById(R.id.tvRestaurantName)
        tvDeliveryTime = findViewById(R.id.tvDeliveryTime)
        llOrderItemsContainer = findViewById(R.id.llOrderItemsContainer)
        etDeliveryAddress = findViewById(R.id.etDeliveryAddress)
        etContactNumber = findViewById(R.id.etContactNumber)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder)

        findViewById<LinearLayout>(R.id.btnBackCheckout).setOnClickListener {
            finish()
        }

        btnPlaceOrder.setOnClickListener {
            handlePlaceOrder()
        }
    }

    private fun loadCheckoutData() {
        val summary = checkoutRepository.getCheckoutSummary(currentUserId, activeRestaurantId)
        if (summary == null || summary.items.isEmpty()) {
            Toast.makeText(this, "Your cart is empty.", Toast.LENGTH_SHORT).show()
            btnPlaceOrder.isEnabled = false
            return
        }

        activeRestaurantId = summary.restaurantId
        tvRestaurantName.text = summary.restaurantName
        tvDeliveryTime.text = summary.deliveryTime
        tvTotalAmount.text = formatCentavosToPeso(summary.totalCentavos)

        llOrderItemsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (item in summary.items) {
            val rowView = inflater.inflate(R.layout.item_checkout, llOrderItemsContainer, false)
            rowView.findViewById<TextView>(R.id.tvFoodName).text = item.foodName
            rowView.findViewById<TextView>(R.id.tvFoodDescription).text = item.foodDescription
            rowView.findViewById<TextView>(R.id.tvQuantity).text = "Qty: ${item.quantity}"
            rowView.findViewById<TextView>(R.id.tvSubtotal).text = formatCentavosToPeso(item.subtotalCentavos)
            llOrderItemsContainer.addView(rowView)
        }
    }

    private fun handlePlaceOrder() {
        // Prevent duplicate orders from rapid double-taps
        if (isPlacingOrder) return

        val address = etDeliveryAddress.text.toString().trim()
        val contact = etContactNumber.text.toString().trim()

        if (address.isEmpty()) {
            etDeliveryAddress.error = "Delivery address is required"
            etDeliveryAddress.requestFocus()
            return
        }

        if (contact.isEmpty() || contact.length < 10) {
            etContactNumber.error = "Enter a valid contact number"
            etContactNumber.requestFocus()
            return
        }

        isPlacingOrder = true
        btnPlaceOrder.isEnabled = false
        btnPlaceOrder.text = "Placing Order..."

        when (val result = checkoutRepository.placeOrderAtomic(currentUserId, activeRestaurantId, address, contact)) {
            is CheckoutRepository.PlaceOrderResult.Success -> {
                Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, OrderConfirmationActivity::class.java).apply {
                    putExtra(OrderConfirmationActivity.EXTRA_ORDER_ID, result.orderId)
                    putExtra(OrderConfirmationActivity.EXTRA_RESTAURANT_NAME, result.restaurantName)
                    putExtra(OrderConfirmationActivity.EXTRA_TOTAL_CENTAVOS, result.totalCentavos)
                }
                startActivity(intent)
                finish()
            }

            is CheckoutRepository.PlaceOrderResult.Error -> {
                isPlacingOrder = false
                btnPlaceOrder.isEnabled = true
                btnPlaceOrder.text = "Place Order"
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getLoggedInUserId(): Int {
        // Adjust if SessionManager uses a different getter name (e.g., getUserId())
        return try {
            val method = sessionManager.javaClass.methods.firstOrNull {
                it.name.equals("getUserId", ignoreCase = true) && it.parameterCount == 0
            }
            val id = (method?.invoke(sessionManager) as? Int) ?: -1
            if (id > 0) id else 1
        } catch (_: Exception) {
            1
        }
    }

    private fun formatCentavosToPeso(centavos: Int): String {
        val pesos = centavos / 100.0
        return String.format(Locale.US, "₱%,.2f", pesos)
    }
}