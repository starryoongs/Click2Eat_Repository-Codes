package com.example.click2eat.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.models.Restaurant

class RestaurantAdapter(
    private var items: List<Restaurant> = emptyList(),
    private val onRestaurantClick: (Restaurant) -> Unit
) : RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {
    fun submitList(newItems: List<Restaurant>) { items = newItems; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder =
        RestaurantViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant, parent, false))
    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class RestaurantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image = itemView.findViewById<ImageView>(R.id.ivRestaurant)
        private val name = itemView.findViewById<TextView>(R.id.tvRestaurantName)
        private val description = itemView.findViewById<TextView>(R.id.tvRestaurantDescription)
        private val delivery = itemView.findViewById<TextView>(R.id.tvDeliveryTime)
        fun bind(restaurant: Restaurant) {
            name.text = restaurant.name
            description.text = restaurant.description.orEmpty()
            delivery.text = restaurant.deliveryTime.orEmpty()
            val resource = itemView.context.resources.getIdentifier(restaurant.image.orEmpty(), "drawable", itemView.context.packageName)
            image.setImageResource(if (resource != 0) resource else R.drawable.click2eat_logo)
            itemView.setOnClickListener { onRestaurantClick(restaurant) }
        }
    }
}
