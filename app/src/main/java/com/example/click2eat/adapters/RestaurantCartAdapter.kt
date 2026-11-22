package com.example.click2eat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.models.CartItem
import com.example.click2eat.utils.ImageUtils
import com.example.click2eat.utils.PriceUtils

class RestaurantCartAdapter(
    private val onViewCart: (Int) -> Unit,
    private val onClearRestaurantCart: (Int, String) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RestaurantCartAdapter.RestaurantViewHolder>() {

    private var restaurantGroups: List<List<CartItem>> = emptyList()

    class RestaurantViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val restaurantName: TextView =
            view.findViewById(R.id.txtRestaurantName)

        val deliveryTime: TextView =
            view.findViewById(R.id.txtDeliveryTime)

        val restaurantImage: ImageView =
            view.findViewById(R.id.imgRestaurant)

        val optionsButton: View? =
            view.findViewById(R.id.btnRestaurantOptions)

        val subtotal: TextView =
            view.findViewById(R.id.txtRestaurantSubtotal)

        val foodPreviews: LinearLayout =
            view.findViewById(R.id.layoutFoodPreviews)

        val viewCartButton: TextView =
            view.findViewById(R.id.btnViewCart)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RestaurantViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_restaurant_cart, parent, false)

        return RestaurantViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RestaurantViewHolder,
        position: Int
    ) {
        val items = restaurantGroups[position]
        val firstItem = items.firstOrNull() ?: return

        holder.restaurantName.text = firstItem.restaurantName
        holder.deliveryTime.text = ""

        holder.restaurantImage.setImageResource(
            ImageUtils.getImageResource(
                holder.itemView.context,
                firstItem.restaurantImage
            )
        )

        val restaurantTotal = items.sumOf { it.subtotal }
        holder.subtotal.text = PriceUtils.formatPrice(restaurantTotal)

        holder.foodPreviews.removeAllViews()

        items.take(3).forEach { item ->
            val imageView = ImageView(holder.itemView.context)
            val density = holder.itemView.resources.displayMetrics.density
            val size = (65 * density).toInt()
            val margin = (8 * density).toInt()

            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = margin

            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.setImageResource(
                ImageUtils.getImageResource(
                    holder.itemView.context,
                    item.image
                )
            )
            imageView.contentDescription = item.foodName

            holder.foodPreviews.addView(imageView)
        }

        // Three-dots menu ("Clear Restaurant Cart")
        holder.optionsButton?.setOnClickListener { anchorView ->
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val selectedItems = restaurantGroups[currentPosition]
                val targetItem = selectedItems.firstOrNull() ?: return@setOnClickListener

                val popup = PopupMenu(anchorView.context, anchorView)
                popup.menu.add("Clear Restaurant Cart")
                    .setOnMenuItemClickListener {
                        onClearRestaurantCart(targetItem.restaurantId, targetItem.restaurantName)
                        true
                    }
                popup.show()
            }
        }

        // Open the selected restaurant's cart
        holder.viewCartButton.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val selectedItems = restaurantGroups[currentPosition]
                val restaurantId = selectedItems.firstOrNull()?.restaurantId
                if (restaurantId != null) {
                    onViewCart(restaurantId)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return restaurantGroups.size
    }

    fun updateItems(cartItems: List<CartItem>) {
        restaurantGroups = cartItems
            .groupBy { it.restaurantId }
            .values
            .toList()

        notifyDataSetChanged()
    }
}