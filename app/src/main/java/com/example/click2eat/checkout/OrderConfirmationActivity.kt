package com.example.click2eat.checkout

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.click2eat.CartActivity
import com.example.click2eat.R
import com.example.click2eat.auth.ProfileActivity
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.database.CheckoutRepository
import com.example.click2eat.database.UserRepository
import com.example.click2eat.home.HomeActivity
import com.example.click2eat.home.RestaurantsActivity
import com.example.click2eat.orders.OrderDetailsActivity
import com.example.click2eat.orders.OrderHistoryActivity
import java.io.File
import java.util.Locale

class OrderConfirmationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "ORDER_ID"
        const val EXTRA_RESTAURANT_NAME = "RESTAURANT_NAME"
        const val EXTRA_TOTAL_CENTAVOS = "TOTAL_CENTAVOS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_order_confirmation)

        val orderId = intent.getLongExtra(
            EXTRA_ORDER_ID,
            -1L
        )

        var restaurantName =
            intent.getStringExtra(EXTRA_RESTAURANT_NAME) ?: ""

        var totalCentavos =
            intent.getIntExtra(EXTRA_TOTAL_CENTAVOS, 0)

        var status = "Pending"

        if (orderId > 0) {
            val savedOrder =
                CheckoutRepository(this).getOrderById(orderId)

            if (savedOrder != null) {
                restaurantName = savedOrder.restaurantName
                totalCentavos = savedOrder.totalAmountCentavos
                status = savedOrder.status
            }
        }

        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(
                Intent(this, ProfileActivity::class.java)
            )
        }

        findViewById<View>(R.id.btnHome).setOnClickListener {
            goTo(HomeActivity::class.java)
        }

        findViewById<View>(R.id.btnCart).setOnClickListener {
            goTo(CartActivity::class.java)
        }

        findViewById<View>(R.id.btnRestaurants).setOnClickListener {
            goTo(RestaurantsActivity::class.java)
        }

        findViewById<View>(R.id.btnOrders).setOnClickListener {
            goTo(OrderHistoryActivity::class.java)
        }

        findViewById<TextView>(R.id.tvConfirmOrderId).text =
            if (orderId > 0) {
                String.format(
                    Locale.US,
                    "#%06d",
                    orderId
                )
            } else {
                "#000001"
            }

        findViewById<TextView>(R.id.tvConfirmRestaurant).text =
            restaurantName

        findViewById<TextView>(R.id.tvConfirmTotal).text =
            String.format(
                Locale.US,
                "₱%,.2f",
                totalCentavos / 100.0
            )

        findViewById<TextView>(R.id.tvConfirmStatus).text =
            status

        findViewById<Button>(R.id.btnViewOrderDetails)
            .setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        OrderDetailsActivity::class.java
                    ).apply {
                        putExtra(
                            OrderDetailsActivity.EXTRA_ORDER_ID,
                            orderId
                        )
                    }
                )

                finish()
            }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfilePicture()
    }

    private fun loadUserProfilePicture() {
        val profileImage =
            findViewById<ImageView>(R.id.ivProfile)

        val userId =
            SessionManager(this).getUserId()

        val user =
            if (userId > 0L) {
                UserRepository(this).getUserById(userId)
            } else {
                null
            }

        val profileFile =
            user?.profilePicture
                ?.takeIf { it.isNotBlank() }
                ?.let { File(filesDir, it) }

        if (profileFile != null && profileFile.exists()) {
            profileImage.setImageURI(
                Uri.fromFile(profileFile)
            )
        } else {
            profileImage.setImageResource(
                R.drawable.default_profile2
            )
        }
    }

    private fun goTo(destination: Class<*>) {
        startActivity(
            Intent(this, destination).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )

        finish()
    }
}