package com.example.click2eat.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.click2eat.R
import com.example.click2eat.models.Category

class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var items: List<Category> = emptyList()

    fun submitList(newItems: List<Category>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)

        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class CategoryViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val categoryName =
            itemView.findViewById<TextView>(R.id.tvCategoryName)

        private val categoryImage =
            itemView.findViewById<ImageView>(R.id.ivCategory)

        fun bind(category: Category) {
            categoryName.text = category.name

            val drawableName = when (category.name.lowercase()) {
                "fastfood" -> "fastfood"
                "burger" -> "burger"
                "chicken" -> "chicken"
                "coffee" -> "coffee"
                "japanese" -> "japanese"
                "korean" -> "korean"
                "pastry" -> "pastry"
                "beverage" -> "beverage"
                else -> "click2eat_logo_no_bg"
            }

            val drawableId = itemView.context.resources.getIdentifier(
                drawableName,
                "drawable",
                itemView.context.packageName
            )

            categoryImage.setImageResource(
                if (drawableId != 0) {
                    drawableId
                } else {
                    R.drawable.click2eat_logo_no_bg
                }
            )

            itemView.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }
}