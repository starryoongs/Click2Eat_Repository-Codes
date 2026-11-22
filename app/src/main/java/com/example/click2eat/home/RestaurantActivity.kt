package com.example.click2eat.home

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.content.Intent
import android.widget.ImageButton
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
import android.net.Uri
import java.io.File
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.UserRepository

class RestaurantActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESTAURANT_ID = "restaurant_id"
    }

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restaurant)

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

        val restaurantId = intent.getLongExtra(
            EXTRA_RESTAURANT_ID,
            -1L
        )

        if (restaurantId == -1L) {
            finish()
            return
        }

        val menuAdapter = MenuItemAdapter { menuItem ->
            Toast.makeText(
                this,
                "${menuItem.name} will be connected to the cart by Member 3.",
                Toast.LENGTH_SHORT
            ).show()
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