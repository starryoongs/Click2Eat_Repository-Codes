package com.example.click2eat.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.database.DatabaseSeeder
import com.example.click2eat.database.MenuRepository
import com.example.click2eat.database.RestaurantRepository
import java.util.concurrent.Executors

class RestaurantsActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var repository: RestaurantRepository
    private lateinit var adapter: RestaurantAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurants)

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
            loadRestaurants(category.name)
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

        findViewById<View>(R.id.btnSearch).setOnClickListener {
            val keyword = findViewById<EditText>(
                R.id.etSearch
            ).text.toString()

            loadRestaurants(keyword)
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

    private fun loadRestaurants(keyword: String) {
        executor.execute {
            DatabaseSeeder.seed(this)

            val restaurants =
                if (keyword.trim().isEmpty()) {
                    repository.getAllRestaurants()
                } else {
                    repository.searchRestaurants(keyword)
                }

            runOnUiThread {
                adapter.submitList(restaurants)

                findViewById<TextView>(
                    R.id.tvNoRestaurants
                ).visibility =
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