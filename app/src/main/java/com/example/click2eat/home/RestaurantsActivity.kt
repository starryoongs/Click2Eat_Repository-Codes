package com.example.click2eat.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.database.DatabaseSeeder
import com.example.click2eat.database.MenuRepository
import com.example.click2eat.database.RestaurantRepository
import android.widget.ImageButton
import com.example.click2eat.home.HomeActivity
import java.util.concurrent.Executors
import android.net.Uri
import java.io.File
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.UserRepository

class RestaurantsActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var repository: RestaurantRepository
    private lateinit var adapter: RestaurantAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restaurants)

        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }

        findViewById<ImageButton>(R.id.btnRestaurants).setOnClickListener {
            // Already on the Restaurants screen
        }

        repository = RestaurantRepository(this)

        adapter = RestaurantAdapter { restaurant ->
            startActivity(
                Intent(this, RestaurantActivity::class.java)
                    .putExtra(
                        RestaurantActivity.EXTRA_RESTAURANT_ID,
                        restaurant.id
                    )
            )
        }

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

        findViewById<RecyclerView>(R.id.rvRestaurants).apply {
            layoutManager = GridLayoutManager(
                this@RestaurantsActivity,
                1
            )
            adapter = this@RestaurantsActivity.adapter
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val search = findViewById<EditText>(R.id.etSearch)

        search.setOnEditorActionListener { _, _, _ ->
            loadRestaurants(search.text.toString().trim())
            true
        }

        findViewById<View>(R.id.btnSearch).setOnClickListener {
            loadRestaurants(search.text.toString().trim())
        }

        executor.execute {
            DatabaseSeeder.seed(this)

            val categories = MenuRepository(this).getCategories()

            runOnUiThread {
                categoryAdapter.submitList(categories)
            }
        }

        loadRestaurants("")
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