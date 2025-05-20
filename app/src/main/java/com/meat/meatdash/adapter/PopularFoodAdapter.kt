package com.meat.meatdash.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.meat.meatdash.R
import com.meat.meatdash.model.CartManager
import com.meat.meatdash.model.FoodItem


class PopularFoodAdapter(
    private val foodItems: List<FoodItem>,
    private val onItemClick: (FoodItem) -> Unit
) : RecyclerView.Adapter<PopularFoodAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodImage: ImageView = itemView.findViewById(R.id.foodImage)
        val foodName: TextView = itemView.findViewById(R.id.foodName)
        val foodDescription: TextView = itemView.findViewById(R.id.foodDescription)
        val foodPrice: TextView = itemView.findViewById(R.id.foodPrice)
        val btnAddToCart: MaterialButton = itemView.findViewById(R.id.AddToCartBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_popular_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val foodItem = foodItems[position]

        holder.foodName.text = foodItem.name
        holder.foodDescription.text = foodItem.description
        holder.foodPrice.text = "₹${foodItem.price}"

        if (!foodItem.imageBase64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(foodItem.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.foodImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.foodImage.setImageResource(R.drawable.placeholder)
            }
        } else {
            holder.foodImage.setImageResource(R.drawable.placeholder)
        }


        // Handle Add to Cart button
        holder.btnAddToCart.setOnClickListener {
            CartManager.addItem(foodItem)
//            Toast.makeText(holder.itemView.context,
//                "${foodItem.name} added to cart",
//                Toast.LENGTH_SHORT).show()
            onItemClick(foodItem)
        }


//        holder.itemView.setOnClickListener {
//            onItemClick(foodItem)
//        }
    }

    override fun getItemCount() = foodItems.size
}
