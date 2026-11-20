
package com.example.click2eat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.adapters.RestaurantCartAdapter
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.CartRepository

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

        recyclerRestaurantCarts =
            findViewById(R.id.recyclerRestaurantCarts)

        txtEmptyCart = findViewById(R.id.txtEmptyCart)

        restaurantCartAdapter = RestaurantCartAdapter { restaurantId ->
            val intent = Intent(
                this,
                RestaurantCartActivity::class.java
            )
            intent.putExtra("restaurant_id", restaurantId)
            startActivity(intent)
        }

        recyclerRestaurantCarts.layoutManager =
            LinearLayoutManager(this)

        recyclerRestaurantCarts.adapter = restaurantCartAdapter

        loadCartItems()
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
