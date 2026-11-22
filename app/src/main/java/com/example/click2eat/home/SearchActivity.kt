package com.example.click2eat.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
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
import android.widget.ImageButton
import com.example.click2eat.home.HomeActivity
import com.example.click2eat.home.RestaurantsActivity
import android.net.Uri
import java.io.File
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.UserRepository
import java.util.concurrent.Executors

class SearchActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUERY = "query"
    }

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var restaurantAdapter: RestaurantAdapter
    private lateinit var menuAdapter: MenuItemAdapter

    private var latestSearchId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)

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
            startActivity(
                Intent(this, RestaurantsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }

        val search = findViewById<EditText>(R.id.etSearch)

        search.setOnEditorActionListener { _, _, _ ->
            load(search.text.toString())
            true
        }

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

        findViewById<View>(R.id.btnSearch).setOnClickListener {
            load(search.text.toString())
        }

        load(search.text.toString())
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
                    profileImage.setImageResource(R.drawable.default_profile2)
                }
            }
        }
    }

    private fun load(keyword: String) {
        val query = keyword.trim()
        val requestId = ++latestSearchId

        executor.execute {
            DatabaseSeeder.seed(this)

            val restaurants =
                RestaurantRepository(this).searchRestaurants(query)

            val menuItems =
                MenuRepository(this).searchMenuItems(query)

            runOnUiThread {
                // Ignore an older search if a newer search was already made
                if (requestId != latestSearchId) return@runOnUiThread

                restaurantAdapter.submitList(restaurants)
                menuAdapter.submitList(menuItems)

                findViewById<TextView>(R.id.tvNoResults).visibility =
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