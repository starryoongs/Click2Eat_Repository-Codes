package com.example.click2eat.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.CartActivity
import com.example.click2eat.R
import com.example.click2eat.auth.ProfileActivity
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.CartRepository
import com.example.click2eat.database.DatabaseSeeder
import com.example.click2eat.database.MenuRepository
import com.example.click2eat.database.RestaurantRepository
import com.example.click2eat.orders.OrderHistoryActivity
import java.util.concurrent.Executors
import java.io.File
import com.example.click2eat.database.UserRepository

class RestaurantActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESTAURANT_ID = "restaurant_id"
    }

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restaurant)

        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(
                Intent(this, ProfileActivity::class.java)
            )
        }


        findViewById<ImageButton>(R.id.btnCart)?.setOnClickListener {
            startActivity(
                Intent(this, CartActivity::class.java)
            )
        }

        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(
                Intent(this, ProfileActivity::class.java)
            )
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

        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            startActivity(intent)
            finish()
        }

        findViewById<ImageView>(R.id.btnOrders).setOnClickListener {
            startActivity(
                Intent(this, OrderHistoryActivity::class.java).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }

        val restaurantId = intent.getLongExtra(
            EXTRA_RESTAURANT_ID,
            -1L
        )

        if (restaurantId == -1L) {
            finish()
            return
        }

        val cartRepository = CartRepository(this)
        val sessionManager = SessionManager(this)

        val menuAdapter = MenuItemAdapter { menuItem ->
            val userId = sessionManager.getUserId().toInt()
            if (userId <= 0) {
                Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show()
                return@MenuItemAdapter
            }

            executor.execute {
                val added = cartRepository.addToCart(
                    userId = userId,
                    menuItemId = menuItem.id.toInt(),
                    quantity = 1
                )
                runOnUiThread {
                    if (added) {
                        Toast.makeText(
                            this,
                            "Added ${menuItem.name} to cart!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Could not add ${menuItem.name} to cart.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        findViewById<RecyclerView>(R.id.rvMenuItems).apply {
            layoutManager = LinearLayoutManager(
                this@RestaurantActivity
            )
            adapter = menuAdapter
        }

        executor.execute {
            DatabaseSeeder.seed(this)

            val restaurant =
                RestaurantRepository(this)
                    .getRestaurantById(restaurantId)

            val menuItems =
                MenuRepository(this)
                    .getMenuItemsForRestaurant(restaurantId)

            runOnUiThread {
                if (restaurant == null) {
                    finish()
                    return@runOnUiThread
                }

                findViewById<TextView>(
                    R.id.tvRestaurantName
                ).text = restaurant.name

                findViewById<TextView>(
                    R.id.tvRestaurantDescription
                ).text = restaurant.description.orEmpty()

                findViewById<TextView>(
                    R.id.tvDeliveryTime
                ).text = restaurant.deliveryTime.orEmpty()

                val imageResource =
                    restaurant.image?.let {
                        resources.getIdentifier(
                            it,
                            "drawable",
                            packageName
                        )
                    } ?: 0

                findViewById<ImageView>(
                    R.id.ivRestaurantImage
                ).setImageResource(
                    if (imageResource != 0) {
                        imageResource
                    } else {
                        R.drawable.click2eat_logo_no_bg
                    }
                )

                menuAdapter.submitList(menuItems)
            }
        }
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

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}