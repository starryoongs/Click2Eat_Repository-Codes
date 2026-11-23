package com.example.click2eat.checkout

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.click2eat.CartActivity
import com.example.click2eat.R
import com.example.click2eat.auth.LoginActivity
import com.example.click2eat.auth.ProfileActivity
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.CheckoutRepository
import com.example.click2eat.database.UserRepository
import com.example.click2eat.home.HomeActivity
import com.example.click2eat.home.RestaurantsActivity
import com.example.click2eat.orders.OrderHistoryActivity
import java.io.File
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESTAURANT_ID = "RESTAURANT_ID"
    }

    private lateinit var checkoutRepository: CheckoutRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var ivProfile: ImageView
    private lateinit var ivRestaurantLogo: ImageView
    private lateinit var tvRestaurantName: TextView
    private lateinit var tvDeliveryTime: TextView
    private lateinit var llOrderItemsContainer: LinearLayout
    private lateinit var etDeliveryAddress: EditText
    private lateinit var etContactNumber: EditText
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnPlaceOrder: TextView

    private var currentUserId: Int = -1
    private var activeRestaurantId: Int = -1
    private var isPlacingOrder: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        checkoutRepository = CheckoutRepository(this)
        sessionManager = SessionManager(this)

        currentUserId = sessionManager.getUserId().toInt()
        if (currentUserId <= 0) {
            Toast.makeText(this, "Please log in to continue checkout.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        activeRestaurantId = intent.getIntExtra(EXTRA_RESTAURANT_ID, -1).let { id ->
            if (id > 0) id else intent.getIntExtra("restaurant_id", -1)
        }

        if (activeRestaurantId <= 0) {
            Toast.makeText(this, "No restaurant selected for checkout.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        loadUserProfilePicture()
        loadCheckoutData()
    }

    override fun onResume() {
        super.onResume()
        if (::sessionManager.isInitialized && currentUserId > 0) {
            loadUserProfilePicture()
        }
    }

    private fun bindViews() {
        ivProfile = findViewById(R.id.ivProfile)
        ivRestaurantLogo = findViewById(R.id.ivRestaurantLogo)
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

        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<View>(R.id.btnHome)?.setOnClickListener {
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }

        findViewById<View>(R.id.btnCart)?.setOnClickListener {
            startActivity(
                Intent(this, CartActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }

        findViewById<View>(R.id.btnRestaurants)?.setOnClickListener {
            startActivity(
                Intent(this, RestaurantsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }

        findViewById<View>(R.id.btnOrders)?.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        btnPlaceOrder.setOnClickListener {
            handlePlaceOrder()
        }
    }

    private fun loadUserProfilePicture() {
        val user = UserRepository(this).getUserById(currentUserId.toLong())
        val profileFile = user?.profilePicture
            ?.takeIf { it.isNotBlank() }
            ?.let { File(filesDir, it) }

        if (profileFile != null && profileFile.exists()) {
            ivProfile.setImageURI(Uri.fromFile(profileFile))
        } else {
            ivProfile.setImageResource(R.drawable.default_profile2)
        }
    }

    private fun loadCheckoutData() {
        val summary = checkoutRepository.getCheckoutSummary(currentUserId, activeRestaurantId)
        if (summary == null || summary.items.isEmpty()) {
            Toast.makeText(this, "Your cart for this restaurant is empty.", Toast.LENGTH_SHORT).show()
            btnPlaceOrder.isEnabled = false
            finish()
            return
        }

        tvRestaurantName.text = summary.restaurantName
        tvDeliveryTime.text = summary.deliveryTime
        tvTotalAmount.text = formatCentavosToPeso(summary.totalCentavos)
        bindDrawableByName(ivRestaurantLogo, summary.restaurantImage, R.drawable.click2eat_logo_no_bg)

        llOrderItemsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (item in summary.items) {
            val rowView = inflater.inflate(R.layout.item_checkout, llOrderItemsContainer, false)
            val ivFood = rowView.findViewById<ImageView>(R.id.ivFoodImage)
            bindDrawableByName(ivFood, item.imageResName, R.drawable.click2eat_logo_no_bg)

            rowView.findViewById<TextView>(R.id.tvFoodName).text = item.foodName
            rowView.findViewById<TextView>(R.id.tvFoodDescription).text = item.foodDescription
            rowView.findViewById<TextView>(R.id.tvQuantity).text = "Qty: ${item.quantity}"
            rowView.findViewById<TextView>(R.id.tvSubtotal).text = formatCentavosToPeso(item.subtotalCentavos)
            llOrderItemsContainer.addView(rowView)
        }
    }

    private fun handlePlaceOrder() {
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

    private fun bindDrawableByName(imageView: ImageView, rawImageName: String, fallbackResId: Int) {
        val cleanName = rawImageName
            .substringAfterLast("/")
            .substringBeforeLast(".")
            .trim()
        if (cleanName.isNotEmpty()) {
            val resId = resources.getIdentifier(cleanName, "drawable", packageName)
            if (resId != 0) {
                imageView.setImageResource(resId)
                return
            }
        }
        imageView.setImageResource(fallbackResId)
    }

    private fun formatCentavosToPeso(centavos: Int): String {
        return String.format(Locale.US, "₱%,.2f", centavos / 100.0)
    }
}