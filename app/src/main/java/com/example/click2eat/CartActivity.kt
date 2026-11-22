package com.example.click2eat

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.example.click2eat.home.HomeActivity
import com.example.click2eat.home.RestaurantsActivity

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
                confirmClearRestaurantCart(restaurantId, restaurantName)
            }
        )

        recyclerRestaurantCarts.layoutManager =
            LinearLayoutManager(this)

        recyclerRestaurantCarts.adapter = restaurantCartAdapter

        loadCartItems()
    }

    private fun confirmClearRestaurantCart(restaurantId: Int, restaurantName: String) {
        AlertDialog.Builder(this)
            .setTitle("Clear Restaurant Cart")
            .setMessage("Are you sure you want to remove all items from $restaurantName?")
            .setPositiveButton("Clear Cart") { _, _ ->
                val cleared = cartRepository.clearRestaurantCart(userId, restaurantId)
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

    private fun setupNavigation() {
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
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

        if (::cartRepository.isInitialized &&
            ::restaurantCartAdapter.isInitialized
        ) {
            loadCartItems()
        }
    }
}