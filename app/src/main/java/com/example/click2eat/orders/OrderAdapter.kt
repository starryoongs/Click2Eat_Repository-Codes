package com.example.click2eat.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.database.DatabaseHelper
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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)

        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: OrderViewHolder,
        position: Int
    ) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        private val restaurant =
            view.findViewById<TextView>(R.id.tvOrderRestaurant)

        private val status =
            view.findViewById<TextView>(R.id.tvOrderStatus)

        private val date =
            view.findViewById<TextView>(R.id.tvOrderDate)

        private val total =
            view.findViewById<TextView>(R.id.tvOrderTotal)

        private val items =
            view.findViewById<TextView>(R.id.tvOrderItems)

        private val details =
            view.findViewById<Button>(R.id.btnViewOrderDetails)

        private val restaurantImage =
            view.findViewById<ImageView>(R.id.imgOrderRestaurant)

        private val foodImage =
            view.findViewById<ImageView>(R.id.imgOrderFood)

        fun bind(entry: OrderRepository.OrderWithItems) {
            val order = entry.order

            restaurant.text = order.restaurantName
            status.text = "Status: ${order.status}"

            date.text = SimpleDateFormat(
                "MMMM d, yyyy (h:mm a)",
                Locale.getDefault()
            ).format(Date(order.createdAt))

            total.text = money(order.totalAmountCentavos)

            items.text = entry.items.joinToString("   ") {
                "${it.foodName} ×${it.quantity}"
            }

            // Load first food image
            val firstItem = entry.items.firstOrNull()

            val foodResource = itemView.context.resources.getIdentifier(
                firstItem?.imageResName.orEmpty(),
                "drawable",
                itemView.context.packageName
            )

            foodImage.setImageResource(
                if (foodResource != 0) {
                    foodResource
                } else {
                    R.drawable.click2eat_logo_no_bg
                }
            )

            // Load restaurant image from SQLite
            val db = DatabaseHelper(itemView.context).readableDatabase

            val cursor = db.rawQuery(
                "SELECT image FROM restaurants WHERE id = ?",
                arrayOf(order.restaurantId.toString())
            )

            val restaurantImageName = cursor.use {
                if (it.moveToFirst()) it.getString(0) else ""
            }

            val restaurantResource =
                itemView.context.resources.getIdentifier(
                    restaurantImageName.orEmpty(),
                    "drawable",
                    itemView.context.packageName
                )

            restaurantImage.setImageResource(
                if (restaurantResource != 0) {
                    restaurantResource
                } else {
                    R.drawable.click2eat_logo_no_bg
                }
            )

            details.setOnClickListener {
                onOpen(order.id)
            }

            itemView.setOnClickListener {
                onOpen(order.id)
            }
        }
    }

    private fun money(centavos: Int): String =
        String.format(
            Locale.US,
            "₱%,.2f",
            centavos / 100.0
        )
}
