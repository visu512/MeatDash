package com.meat.meatdash.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.meat.meatdash.CartManager
import com.meat.meatdash.R
import com.meat.meatdash.model.FoodItem

class PopularFoodAdapter(
    private val foodItems: List<FoodItem>,
    private val onItemClick: (FoodItem) -> Unit,
    private val onCartUpdated: () -> Unit
) : RecyclerView.Adapter<PopularFoodAdapter.FoodViewHolder>() {

    private val addedItems = CartManager.getCartItems().map { it.id }.toMutableSet()

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foodImage: ImageView = itemView.findViewById(R.id.foodImage)
        private val foodName: TextView = itemView.findViewById(R.id.foodName)
        private val foodPrice: TextView = itemView.findViewById(R.id.foodPrice)
        private val foodDescription: TextView = itemView.findViewById(R.id.foodDescription)
        private val btnAddToCart: MaterialButton = itemView.findViewById(R.id.cartButton)
        private val cardView: CardView = itemView.findViewById(R.id.cardView)

        fun bind(foodItem: FoodItem) {
            // Name & description
            foodName.text = foodItem.name
            foodDescription.text = foodItem.description

            // Price formatting with “/kg”
            val priceText = "₹${foodItem.price} "
            val unitText = "/kg"
            val start = priceText.length
            foodPrice.text = android.text.SpannableStringBuilder().apply {
                append(priceText)
                append(unitText)
                setSpan(
                    android.text.style.ForegroundColorSpan(Color.parseColor("#808080")),
                    start, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    android.text.style.AbsoluteSizeSpan(11, true),
                    start, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // cardview color always white both mode nigh/light
            cardView.setCardBackgroundColor(Color.WHITE)


            // Image load
            if (foodItem.imageBase64.isNotEmpty()) {
                try {
                    val bytes = android.util.Base64.decode(
                        foodItem.imageBase64, android.util.Base64.DEFAULT
                    )
                    val bitmap =
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    Glide.with(itemView.context)
                        .load(bitmap)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(foodImage)
                } catch (_: Exception) {
                    foodImage.setImageResource(R.drawable.placeholder)
                }
            } else {
                foodImage.setImageResource(R.drawable.placeholder)
            }

            // Update button visuals
            updateButtonState(foodItem)

            // Only button click adds to cart
            btnAddToCart.setOnClickListener {
                if (!addedItems.contains(foodItem.id)) {
                    CartManager.addItemWithFeedback(foodItem, itemView.context) { success ->
                        if (success) {
                            addedItems.add(foodItem.id)
                            updateButtonState(foodItem)
                            onCartUpdated()  // notify activity
                        }
                    }
                }
            }

            // Whole-item click → detail screen
            itemView.setOnClickListener {
                onItemClick(foodItem)
            }
        }

        private fun updateButtonState(foodItem: FoodItem) {
            if (addedItems.contains(foodItem.id)) {
                btnAddToCart.apply {
                    text = "ADDED"
                    isEnabled = false
                    // Filled with error color + white text
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.colorError)
                    )
                    strokeWidth = 0
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
            } else {
                btnAddToCart.apply {
                    text = "ADD"
                    isEnabled = true
                    // White fill + black outline + black text
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, android.R.color.white)
                    )
                    strokeWidth = context.resources.getDimensionPixelSize(R.dimen.outline_stroke)
                    strokeColor = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.colorError)
                    )
                    setTextColor(ContextCompat.getColor(context, R.color.colorError))
                }
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
