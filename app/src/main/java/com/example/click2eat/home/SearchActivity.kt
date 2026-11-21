package com.example.click2eat.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.database.DatabaseSeeder
import com.example.click2eat.database.MenuRepository
import com.example.click2eat.database.RestaurantRepository
import java.util.concurrent.Executors

class SearchActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUERY = "query"
    }

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var restaurantAdapter: RestaurantAdapter
    private lateinit var menuAdapter: MenuItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val search = findViewById<EditText>(R.id.etSearch)

        search.setText(
            intent.getStringExtra(EXTRA_QUERY).orEmpty()
        )

        restaurantAdapter = RestaurantAdapter { item ->
            startActivity(
                Intent(this, RestaurantActivity::class.java)
                    .putExtra(
                        RestaurantActivity.EXTRA_RESTAURANT_ID,
                        item.id
                    )
            )
        }

        menuAdapter = MenuItemAdapter {
            Toast.makeText(
                this,
                "Cart connection will be added by Member 3",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<RecyclerView>(
            R.id.rvRestaurantResults
        ).apply {
            layoutManager = LinearLayoutManager(
                this@SearchActivity
            )
            adapter = restaurantAdapter
        }

        findViewById<RecyclerView>(
            R.id.rvMenuResults
        ).apply {
            layoutManager = LinearLayoutManager(
                this@SearchActivity
            )
            adapter = menuAdapter
        }

        findViewById<View>(
            R.id.btnRestaurants
        ).setOnClickListener {
            finish()
        }

        findViewById<View>(
            R.id.btnSearch
        ).setOnClickListener {
            load(search.text.toString())
        }

        load(search.text.toString())
    }

    private fun load(keyword: String) {
        executor.execute {
            DatabaseSeeder.seed(this)

            val restaurants =
                RestaurantRepository(this)
                    .searchRestaurants(keyword)

            val menuItems =
                MenuRepository(this)
                    .searchMenuItems(keyword)

            runOnUiThread {
                restaurantAdapter.submitList(restaurants)
                menuAdapter.submitList(menuItems)

                findViewById<TextView>(
                    R.id.tvNoResults
                ).visibility =
                    if (restaurants.isEmpty() && menuItems.isEmpty()) {
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