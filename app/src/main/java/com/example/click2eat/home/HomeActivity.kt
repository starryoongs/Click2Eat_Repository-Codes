package com.example.click2eat.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.example.click2eat.database.UserRepository
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.auth.LoginActivity
import com.example.click2eat.auth.ProfileActivity
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.DatabaseSeeder
import com.example.click2eat.database.MenuRepository
import com.example.click2eat.database.RestaurantRepository
import java.util.concurrent.Executors
import android.net.Uri
import java.io.File

class HomeActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var session: SessionManager
    private lateinit var restaurantRepository: RestaurantRepository
    private lateinit var menuRepository: MenuRepository
    private lateinit var restaurantAdapter: RestaurantAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)

        if (!session.isLoggedIn()) {
            goToLogin()
            return
        }

        setContentView(R.layout.activity_home)

        restaurantRepository = RestaurantRepository(this)
        menuRepository = MenuRepository(this)

        restaurantAdapter = RestaurantAdapter(
            onRestaurantClick = ::openRestaurant
        )

        categoryAdapter = CategoryAdapter { category ->
            startActivity(
                Intent(this, RestaurantsActivity::class.java).apply {
                    putExtra(
                        RestaurantsActivity.EXTRA_CATEGORY,
                        category.name
                    )
                }
            )
        }

        findViewById<RecyclerView>(
            R.id.rvCategories
        ).apply {
            layoutManager = GridLayoutManager(
                this@HomeActivity,
                4
            )

            adapter = categoryAdapter
        }

        findViewById<RecyclerView>(
            R.id.rvFeaturedRestaurants
        ).apply {
            layoutManager = GridLayoutManager(
                this@HomeActivity,
                2
            )

            adapter = restaurantAdapter
        }

        findViewById<ImageView>(
            R.id.ivProfile
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ProfileActivity::class.java
                )
            )
        }

        findViewById<View>(
            R.id.btnRestaurants
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RestaurantsActivity::class.java
                )
            )
        }

        findViewById<EditText>(
            R.id.etSearch
        ).setOnEditorActionListener { _, _, _ ->
            openSearch()
            true
        }

        findViewById<View>(
            R.id.btnSearch
        ).setOnClickListener {
            openSearch()
        }

        findViewById<View>(
            R.id.tvSeeRestaurants
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RestaurantsActivity::class.java
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (::session.isInitialized && session.isLoggedIn()) {
            loadHomeData()
        }
    }

    private fun loadHomeData() {
        executor.execute {
            DatabaseSeeder.seed(this)

            val restaurants = restaurantRepository
                .getAllRestaurants()
                .take(6)

            val categories = menuRepository.getCategories()

            val userId = session.getUserId()

            val user = if (userId != -1L) {
                UserRepository(this).getUserById(userId)
            } else {
                null
            }

            val profileFile = user?.profilePicture
                ?.takeIf { it.isNotBlank() }
                ?.let { File(filesDir, it) }

            runOnUiThread {
                val firstName = user?.firstName?.trim().orEmpty()

                findViewById<TextView>(R.id.tvUsername).text =
                    if (firstName.isEmpty()) {
                        "Hi,"
                    } else {
                        "Hi, $firstName"
                    }

                val profileImage = findViewById<ImageView>(R.id.ivProfile)

                if (profileFile != null && profileFile.exists()) {
                    profileImage.setImageURI(Uri.fromFile(profileFile))
                } else {
                    profileImage.setImageResource(R.drawable.default_profile2)
                }

                restaurantAdapter.submitList(restaurants)
                categoryAdapter.submitList(categories)
            }
        }
    }

    private fun openSearch() {
        val query = findViewById<EditText>(
            R.id.etSearch
        ).text.toString().trim()

        startActivity(
            Intent(
                this,
                SearchActivity::class.java
            ).putExtra(
                SearchActivity.EXTRA_QUERY,
                query
            )
        )
    }

    private fun openRestaurant(
        restaurant: com.example.click2eat.models.Restaurant
    ) {
        startActivity(
            Intent(
                this,
                RestaurantActivity::class.java
            ).putExtra(
                RestaurantActivity.EXTRA_RESTAURANT_ID,
                restaurant.id
            )
        )
    }

    private fun goToLogin() {
        val intent = Intent(
            this,
            LoginActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}