package com.example.click2eat.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.models.MenuItem
import java.util.Locale

class MenuItemAdapter(
    private var items: List<MenuItem> = emptyList(),
    private val onAddToCart: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder>() {
    fun submitList(newItems: List<MenuItem>) { items = newItems; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder =
        MenuItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_menu, parent, false))
    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image = itemView.findViewById<ImageView>(R.id.ivMenuItem)
        private val name = itemView.findViewById<TextView>(R.id.tvMenuName)
        private val description = itemView.findViewById<TextView>(R.id.tvMenuDescription)
        private val price = itemView.findViewById<TextView>(R.id.tvMenuPrice)
        private val availability = itemView.findViewById<TextView>(R.id.tvAvailability)
        private val addButton = itemView.findViewById<ImageButton>(R.id.btnAddToCart)
        fun bind(menuItem: MenuItem) {
            name.text = menuItem.name
            description.text = menuItem.description.orEmpty()
            price.text = String.format(Locale.US, "₱%.2f", menuItem.priceCentavos / 100.0)
            availability.text = if (menuItem.available) "Available" else "Unavailable"
            addButton.isEnabled = menuItem.available
            addButton.setOnClickListener {
                if (menuItem.available) onAddToCart(menuItem)
                else Toast.makeText(itemView.context, "This item is unavailable", Toast.LENGTH_SHORT).show()
            }
            val resource = itemView.context.resources.getIdentifier(menuItem.image.orEmpty(), "drawable", itemView.context.packageName)
            image.setImageResource(if (resource != 0) resource else R.drawable.click2eat_logo)
        }
    }
}
