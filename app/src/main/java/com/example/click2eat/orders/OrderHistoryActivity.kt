package com.example.click2eat.orders

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.auth.SessionManager
import com.example.click2eat.CartActivity
import com.example.click2eat.home.HomeActivity
import com.example.click2eat.home.RestaurantsActivity
import com.example.click2eat.auth.ProfileActivity

class OrderHistoryActivity : AppCompatActivity() {
    private lateinit var repository: OrderRepository
    private lateinit var adapter: OrderAdapter
    private lateinit var empty: TextView
    private lateinit var filter: Spinner
    private var userId: Long = -1L
    private var selectedFilter = OrderRepository.FILTER_ALL
    private val statuses = listOf("All", "Pending", "Preparing", "Completed", "Cancelled")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_history)


        findViewById<View>(R.id.ordersNavHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.ordersNavCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.ordersNavRestaurants).setOnClickListener {
            startActivity(Intent(this, RestaurantsActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.ordersNavOrders).setOnClickListener {
            // Already on Order History
        }

        findViewById<View>(R.id.ordersProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        userId = SessionManager(this).getUserId()
        if (userId <= 0L) {
            finish()
            return
        }

        repository = OrderRepository(this)
        empty = findViewById(R.id.orderHistoryEmpty)
        filter = findViewById(R.id.orderStatusFilter)
        adapter = OrderAdapter(emptyList()) { orderId ->
            startActivity(
                Intent(this, OrderDetailsActivity::class.java)
                    .putExtra(OrderDetailsActivity.EXTRA_ORDER_ID, orderId)
            )
        }

        findViewById<RecyclerView>(R.id.orderHistoryRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@OrderHistoryActivity)
            adapter = this@OrderHistoryActivity.adapter
        }

        filter.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )
        filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedFilter = statuses[position]
                renderOrders()
            }
        }

        findViewById<View>(R.id.ordersBackButton).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized && userId > 0L) {
            repository.refreshStatuses(userId)
            renderOrders()
        }
    }

    private fun renderOrders() {
        val orders = repository.getOrdersWithItems(userId, selectedFilter)
        adapter.submitList(orders)
        empty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
    }
}
