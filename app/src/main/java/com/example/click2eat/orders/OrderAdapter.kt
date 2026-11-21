package com.example.click2eat.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private var orders: List<OrderRepository.OrderWithItems>,
    private val onOpen: (Long) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    fun submitList(newOrders: List<OrderRepository.OrderWithItems>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val restaurant = view.findViewById<TextView>(R.id.tvOrderRestaurant)
        private val status = view.findViewById<TextView>(R.id.tvOrderStatus)
        private val date = view.findViewById<TextView>(R.id.tvOrderDate)
        private val total = view.findViewById<TextView>(R.id.tvOrderTotal)
        private val items = view.findViewById<TextView>(R.id.tvOrderItems)
        private val details = view.findViewById<Button>(R.id.btnViewOrderDetails)

        fun bind(entry: OrderRepository.OrderWithItems) {
            val order = entry.order
            restaurant.text = order.restaurantName
            status.text = "Status: ${order.status}"
            date.text = SimpleDateFormat("MMMM d, yyyy (h:mm a)", Locale.getDefault())
                .format(Date(order.createdAt))
            total.text = money(order.totalAmountCentavos)
            items.text = entry.items.joinToString("   ") { "${it.foodName} ×${it.quantity}" }
            details.setOnClickListener { onOpen(order.id) }
            itemView.setOnClickListener { onOpen(order.id) }
        }
    }

    private fun money(centavos: Int): String =
        String.format(Locale.US, "₱%,.2f", centavos / 100.0)
}
