package com.meat.meatdash.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.meat.meatdash.R
import com.meat.meatdash.model.CartManager
import com.meat.meatdash.model.FoodItem

class PopularFoodAdapter(
    private val foodItems: List<FoodItem>,
    private val onItemClick: (FoodItem) -> Unit
) : RecyclerView.Adapter<PopularFoodAdapter.FoodViewHolder>() {

    // Initialize with items already in cart
    private val addedItems = CartManager.getCartItems().map { it.id }.toMutableSet()

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foodImage: ImageView = itemView.findViewById(R.id.foodImage)
        private val foodName: TextView = itemView.findViewById(R.id.foodName)
        private val foodPrice: TextView = itemView.findViewById(R.id.foodPrice)
        private val foodDescription: TextView = itemView.findViewById(R.id.foodDescription)
        private val btnAddToCart: com.google.android.material.button.MaterialButton =
            itemView.findViewById(R.id.AddToCartBtn)

        fun bind(foodItem: FoodItem) {
            foodName.text = foodItem.name
            foodPrice.text = "₹${foodItem.price}"
            foodDescription.text = foodItem.description

            // Load image
            try {
                if (foodItem.imageBase64.isNotEmpty()) {
                    val bytes = Base64.decode(foodItem.imageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    Glide.with(itemView.context)
                        .load(bitmap)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(foodImage)
                } else {
                    foodImage.setImageResource(R.drawable.placeholder)
                }
            } catch (e: Exception) {
                foodImage.setImageResource(R.drawable.placeholder)
            }

            // Update button state
            updateButtonState(foodItem)

            btnAddToCart.setOnClickListener {
                if (!addedItems.contains(foodItem.id)) {
                    CartManager.addItem(foodItem) { success ->
                        if (success) {
                            addedItems.add(foodItem.id)
                            updateButtonState(foodItem)
                            onItemClick(foodItem)
                        }
                    }
                }
            }

            itemView.setOnClickListener { onItemClick(foodItem) }
        }

        private fun updateButtonState(foodItem: FoodItem) {
            if (addedItems.contains(foodItem.id)) {
                btnAddToCart.text = "Added In Cart"
                btnAddToCart.isEnabled = false
                btnAddToCart.setBackgroundColor(itemView.context.getColor(R.color.gray))
            } else {
                btnAddToCart.text = "Add to Cart"
                btnAddToCart.isEnabled = true
                btnAddToCart.setBackgroundColor(itemView.context.getColor(R.color.black))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_popular_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(foodItems[position])
    }

    override fun getItemCount(): Int = foodItems.size

    fun refreshCartState() {
        addedItems.clear()
        addedItems.addAll(CartManager.getCartItems().map { it.id })
        notifyDataSetChanged()
    }
}