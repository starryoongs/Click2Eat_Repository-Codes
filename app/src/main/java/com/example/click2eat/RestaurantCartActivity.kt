package com.example.click2eat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.adapters.CartAdapter
import com.example.click2eat.auth.ProfileActivity
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.checkout.CheckoutActivity
import com.example.click2eat.database.CartRepository
import com.example.click2eat.database.DatabaseHelper
import com.example.click2eat.home.HomeActivity
import com.example.click2eat.home.RestaurantsActivity
import com.example.click2eat.models.CartItem
import com.example.click2eat.utils.ImageUtils
import com.example.click2eat.utils.PriceUtils

class RestaurantCartActivity : AppCompatActivity() {

    private lateinit var cartRepository: CartRepository
    private lateinit var cartAdapter: CartAdapter

    private var userId = -1
    private var restaurantId = -1

    private lateinit var txtCartTotal: TextView
    private lateinit var recyclerCartItems: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_cart)

        val sessionManager = SessionManager(this)
        val sessionUserId = sessionManager.getUserId()

        if (!sessionManager.isLoggedIn() ||
            sessionUserId <= 0L ||
            sessionUserId > Int.MAX_VALUE.toLong()
        ) {
            Toast.makeText(
                this,
                "Please log in first",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        userId = sessionUserId.toInt()
        restaurantId = intent.getIntExtra("restaurant_id", -1)

        if (restaurantId <= 0) {
            Toast.makeText(
                this,
                "Invalid restaurant",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        cartRepository = CartRepository(this)

        txtCartTotal = findViewById(R.id.txtCartTotal)
        recyclerCartItems = findViewById(R.id.recyclerCartItems)

        cartAdapter = CartAdapter(
            mutableListOf(),
            onIncrease = { item ->
                changeQuantity(item, 1)
            },
            onDecrease = { item ->
                changeQuantity(item, -1)
            },
            onRemove = { item ->
                confirmRemoveItem(item)
            }
        )

        recyclerCartItems.layoutManager =
            LinearLayoutManager(this)

        recyclerCartItems.adapter = cartAdapter

        findViewById<ImageView>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        setupBottomNavigation()

        findViewById<TextView>(R.id.btnRestaurantOptions)
            .setOnClickListener { view ->

                val popup = PopupMenu(this, view)

                popup.menu.add("Clear Restaurant Cart")
                    .setOnMenuItemClickListener {

                        confirmClearRestaurantCart()
                        true
                    }

                popup.show()
            }

        findViewById<TextView>(R.id.btnProceedCheckout)
            .setOnClickListener {
                val currentItems = cartRepository.getRestaurantCartItems(userId, restaurantId)
                if (currentItems.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Your cart is empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val checkoutIntent = Intent(this, CheckoutActivity::class.java).apply {
                    putExtra(CheckoutActivity.EXTRA_RESTAURANT_ID, restaurantId)
                }
                startActivity(checkoutIntent)
            }

        loadRestaurantInfo()
        loadCartItems()
    }

    private fun setupBottomNavigation() {
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.navCart)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.navRestaurants)?.setOnClickListener {
            val intent = Intent(this, RestaurantsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.btnProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun loadCartItems() {
        val items = cartRepository.getRestaurantCartItems(
            userId,
            restaurantId
        )

        cartAdapter.updateItems(items)

        val total = items.sumOf { it.subtotal }

        txtCartTotal.text = PriceUtils.formatPrice(total)
    }

    private fun changeQuantity(item: CartItem, change: Int) {

        val newQuantity = item.quantity.toLong() + change

        if (newQuantity <= 0L) {
            confirmRemoveItem(item)
            return
        }

        if (newQuantity > Int.MAX_VALUE) {
            return
        }

        val updated = cartRepository.updateQuantity(
            userId,
            item.id,
            newQuantity.toInt()
        )

        if (updated) {
            loadCartItems()
        } else {
            Toast.makeText(
                this,
                "Unable to update quantity",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun confirmRemoveItem(item: CartItem) {

        AlertDialog.Builder(this)
            .setTitle("Remove Item")
            .setMessage(
                "Remove ${item.foodName} from your cart?"
            )
            .setPositiveButton("Remove") { _, _ ->

                val removed = cartRepository.removeCartItem(
                    userId,
                    item.id
                )

                if (removed) {
                    loadCartItems()
                } else {
                    Toast.makeText(
                        this,
                        "Unable to remove item",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()

        if (::cartRepository.isInitialized &&
            ::cartAdapter.isInitialized
        ) {
            loadCartItems()
        }
    }

    private fun loadRestaurantInfo() {

        val databaseHelper = DatabaseHelper(this)
        val db = databaseHelper.readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT name, image, delivery_time
            FROM restaurants
            WHERE id = ?
            """.trimIndent(),
            arrayOf(restaurantId.toString())
        )

        cursor.use {
            if (it.moveToFirst()) {

                val name = it.getString(0)
                val image = it.getString(1)
                val deliveryTime = it.getString(2)

                findViewById<TextView>(R.id.txtRestaurantName).text =
                    name

                findViewById<TextView>(R.id.txtDeliveryTime).text =
                    deliveryTime ?: ""

                findViewById<ImageView>(R.id.imgRestaurant)
                    .setImageResource(
                        ImageUtils.getImageResource(
                            this,
                            image
                        )
                    )
            }
        }
    }

    private fun confirmClearRestaurantCart() {

        AlertDialog.Builder(this)
            .setTitle("Clear Restaurant Cart")
            .setMessage(
                "Are you sure you want to remove all items from this restaurant?"
            )
            .setPositiveButton("Clear Cart") { _, _ ->

                val cleared = cartRepository.clearRestaurantCart(
                    userId,
                    restaurantId
                )

                if (cleared) {
                    Toast.makeText(
                        this,
                        "Restaurant cart cleared successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                } else {
                    Toast.makeText(
                        this,
                        "Unable to clear restaurant cart",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}