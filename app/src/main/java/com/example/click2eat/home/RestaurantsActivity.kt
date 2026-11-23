package com.example.click2eat.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.CartActivity
import com.example.click2eat.R
import com.example.click2eat.auth.ProfileActivity
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.DatabaseSeeder
import com.example.click2eat.database.MenuRepository
import com.example.click2eat.database.RestaurantRepository
import com.example.click2eat.database.UserRepository
import com.example.click2eat.orders.OrderHistoryActivity
import java.io.File
import java.util.concurrent.Executors

class RestaurantsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY = "category"
    }

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var repository: RestaurantRepository
    private lateinit var adapter: RestaurantAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restaurants)

        // Profile
        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(
                Intent(this, ProfileActivity::class.java)
            )
        }

        // Home
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }

        // Cart
        findViewById<ImageButton>(R.id.btnCart).setOnClickListener {
            startActivity(
                Intent(this, CartActivity::class.java)
            )
        }

        // Restaurants
        findViewById<ImageButton>(R.id.btnRestaurants).setOnClickListener {
            // Already on the Restaurants screen
        }

        // Order History
        findViewById<View>(R.id.btnOrders).setOnClickListener {
            startActivity(
                Intent(this, OrderHistoryActivity::class.java)
            )
        }

        // Restaurant Repository
        repository = RestaurantRepository(this)

        // Restaurant Cards
        adapter = RestaurantAdapter { restaurant ->
            startActivity(
                Intent(this, RestaurantActivity::class.java)
                    .putExtra(
                        RestaurantActivity.EXTRA_RESTAURANT_ID,
                        restaurant.id
                    )
            )
        }

        // Categories
        categoryAdapter = CategoryAdapter { category ->
            loadRestaurants(category.name.trim())
        }

        findViewById<RecyclerView>(R.id.rvCategories).apply {
            layoutManager = GridLayoutManager(
                this@RestaurantsActivity,
                4
            )
            adapter = categoryAdapter
        }

        // Restaurant List
        findViewById<RecyclerView>(R.id.rvRestaurants).apply {
            layoutManager = GridLayoutManager(
                this@RestaurantsActivity,
                1
            )
            adapter = this@RestaurantsActivity.adapter
        }

        // Back Button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Search
        val search = findViewById<EditText>(R.id.etSearch)

        search.setOnEditorActionListener { _, _, _ ->
            loadRestaurants(search.text.toString().trim())
            true
        }

        findViewById<View>(R.id.btnSearch).setOnClickListener {
            loadRestaurants(search.text.toString().trim())
        }

        // Load Categories
        executor.execute {
            DatabaseSeeder.seed(this)

            val categories = MenuRepository(this).getCategories()

            runOnUiThread {
                categoryAdapter.submitList(categories)
            }
        }

        // Load Restaurants
        val selectedCategory =
            intent.getStringExtra(EXTRA_CATEGORY).orEmpty()

        loadRestaurants(selectedCategory)
    }

    override fun onResume() {
        super.onResume()
        loadProfileImage()
    }

    private fun loadProfileImage() {
        val userId = SessionManager(this).getUserId()

        if (userId == -1L) return

        executor.execute {
            val user = UserRepository(this).getUserById(userId)

            val profileFile = user?.profilePicture
                ?.takeIf { it.isNotBlank() }
                ?.let { File(filesDir, it) }

            runOnUiThread {
                val profileImage =
                    findViewById<ImageView>(R.id.ivProfile)

                if (profileFile != null && profileFile.exists()) {
                    profileImage.setImageURI(Uri.fromFile(profileFile))
                } else {
                    profileImage.setImageResource(
                        R.drawable.default_profile2
                    )
                }
            }
        }
    }

    private fun loadRestaurants(keyword: String) {
        val query = keyword.trim()

        executor.execute {
            DatabaseSeeder.seed(this)

            val restaurants =
                if (query.isEmpty()) {
                    repository.getAllRestaurants()
                } else {
                    repository.searchRestaurants(query)
                }

            runOnUiThread {
                adapter.submitList(restaurants)

                findViewById<TextView>(R.id.tvNoRestaurants).visibility =
                    if (restaurants.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
