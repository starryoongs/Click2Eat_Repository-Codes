package com.example.click2eat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.adapters.RestaurantCartAdapter
import com.example.click2eat.auth.ProfileActivity
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.CartRepository
import com.example.click2eat.database.UserRepository
import com.example.click2eat.home.HomeActivity
import com.example.click2eat.home.RestaurantsActivity
import com.example.click2eat.orders.OrderHistoryActivity
import java.io.File

class CartActivity : AppCompatActivity() {

    private lateinit var cartRepository: CartRepository
    private lateinit var restaurantCartAdapter: RestaurantCartAdapter
    private lateinit var recyclerRestaurantCarts: RecyclerView
    private lateinit var txtEmptyCart: TextView

    private var userId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

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
        cartRepository = CartRepository(this)

        setupNavigation()
        loadUserProfilePicture()

        recyclerRestaurantCarts =
            findViewById(R.id.recyclerRestaurantCarts)

        txtEmptyCart = findViewById(R.id.txtEmptyCart)

        restaurantCartAdapter = RestaurantCartAdapter(
            onViewCart = { restaurantId ->
                val intent = Intent(
                    this,
                    RestaurantCartActivity::class.java
                )

                intent.putExtra("restaurant_id", restaurantId)
                startActivity(intent)
            },

            onClearRestaurantCart = { restaurantId, restaurantName ->
                confirmClearRestaurantCart(
                    restaurantId,
                    restaurantName
                )
            }
        )

        recyclerRestaurantCarts.layoutManager =
            LinearLayoutManager(this)

        recyclerRestaurantCarts.adapter = restaurantCartAdapter

        loadCartItems()
    }

    // Bottom navigation
    private fun setupNavigation() {

        // Home
        findViewById<View>(R.id.btnHome)?.setOnClickListener {
            val intent = Intent(
                this,
                HomeActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            startActivity(intent)
            finish()
        }

        // Restaurants
        findViewById<View>(R.id.btnRestaurants)?.setOnClickListener {
            val intent = Intent(
                this,
                RestaurantsActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            startActivity(intent)
            finish()
        }

        // Order History
        findViewById<View>(R.id.btnOrders)?.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    OrderHistoryActivity::class.java
                )
            )
        }

        // Profile
        findViewById<View>(R.id.btnProfile)?.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ProfileActivity::class.java
                )
            )
        }
    }

    // Load profile picture
    private fun loadUserProfilePicture() {
        val profileImage =
            findViewById<ImageView>(R.id.btnProfile) ?: return

        val user = UserRepository(this)
            .getUserById(userId.toLong())

        val profileFile = user?.profilePicture
            ?.takeIf { it.isNotBlank() }
            ?.let { File(filesDir, it) }

        if (profileFile != null && profileFile.exists()) {
            profileImage.setImageURI(Uri.fromFile(profileFile))
        } else {
            profileImage.setImageResource(
                R.drawable.default_profile2
            )
        }
    }

    // Clear restaurant cart
    private fun confirmClearRestaurantCart(
        restaurantId: Int,
        restaurantName: String
    ) {
        AlertDialog.Builder(this)
            .setTitle("Clear Restaurant Cart")
            .setMessage(
                "Are you sure you want to remove all items from $restaurantName?"
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

                    loadCartItems()
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

    // Load cart items
    private fun loadCartItems() {
        val items = cartRepository.getCartItems(userId)

        restaurantCartAdapter.updateItems(items)

        if (items.isEmpty()) {
            txtEmptyCart.visibility = View.VISIBLE
            recyclerRestaurantCarts.visibility = View.GONE
        } else {
            txtEmptyCart.visibility = View.GONE
            recyclerRestaurantCarts.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()

        if (userId > 0) {
            loadUserProfilePicture()
        }

        if (::cartRepository.isInitialized &&
            ::restaurantCartAdapter.isInitialized
        ) {
            loadCartItems()
        }
    }
}
