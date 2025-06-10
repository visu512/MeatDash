package com.meat.meatdash.adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.meat.meatdash.R
import com.meat.meatdash.model.FoodItem

class CheckoutItemsAdapter(
    private val items: List<FoodItem>
) : RecyclerView.Adapter<CheckoutItemsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val itemWeight: TextView = itemView.findViewById(R.id.ItemWeight)
        val cardview: CardView = itemView.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checkout_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val price = item.price * item.weight / 1000.0
        holder.itemName.text = item.name
        holder.itemPrice.text = "₹%.2f".format(price)
        holder.itemWeight.text = if (item.weight >= 1000) {
            "Weight: ${item.weight / 1000} kg"
        } else {
            "Weight: ${item.weight} g"
        }

        if (!item.imageBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(item.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.itemImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.itemImage.setImageResource(R.drawable.placeholder)
            }
        } else {
            holder.itemImage.setImageResource(R.drawable.placeholder)
        }


        //// cardview always white
        holder.cardview.setCardBackgroundColor(Color.WHITE)
    }

    override fun getItemCount(): Int = items.size
}
