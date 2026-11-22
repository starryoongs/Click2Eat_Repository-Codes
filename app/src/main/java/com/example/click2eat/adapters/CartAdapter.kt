package com.example.click2eat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.models.CartItem
import com.example.click2eat.utils.PriceUtils
import com.example.click2eat.utils.ImageUtils

class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit,
    private val onRemove: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val foodImage: ImageView = view.findViewById(R.id.imgFood)
        val foodName: TextView = view.findViewById(R.id.txtFoodName)
        val foodDescription: TextView =
            view.findViewById(R.id.txtFoodDescription)
        val quantity: TextView = view.findViewById(R.id.txtQuantity)
        val subtotal: TextView = view.findViewById(R.id.txtItemSubtotal)

        val increase: TextView = view.findViewById(R.id.btnIncrease)
        val decrease: TextView = view.findViewById(R.id.btnDecrease)
        val remove: ImageView = view.findViewById(R.id.btnRemoveItem)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CartViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)

        return CartViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CartViewHolder,
        position: Int
    ) {
        val item = items[position]

        holder.foodName.text = item.foodName
        holder.foodDescription.text = item.foodDescription ?: ""
        holder.quantity.text = item.quantity.toString()
        holder.subtotal.text = PriceUtils.formatPrice(item.subtotal)

        holder.foodImage.setImageResource(
            ImageUtils.getImageResource(
                holder.itemView.context,
                item.image
            )
        )

        holder.increase.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onIncrease(items[currentPosition])
            }
        }

        holder.decrease.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onDecrease(items[currentPosition])
            }
        }

        holder.remove.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onRemove(items[currentPosition])
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItems(newItems: List<CartItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
