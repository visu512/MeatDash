package com.meat.meatdash.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.meat.meatdash.R
import com.meat.meatdash.model.Shop

class ShopAdapter(
    private val shopList: List<Shop>,
    private val onClick: (Shop) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    inner class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val shopImage: ImageView = itemView.findViewById(R.id.shopImage)
        private val shopName: TextView = itemView.findViewById(R.id.shopName)
        private val shopLocation: TextView = itemView.findViewById(R.id.shopLocations)
        private val shopRatingText: TextView = itemView.findViewById(R.id.shopRatingText)
        private val shopDescription: TextView = itemView.findViewById(R.id.shopsDescription)

        @SuppressLint("SetTextI18n")
        fun bind(shop: Shop) {
            shopName.text = shop.shopName
            shopLocation.text = shop.shopLocation
            shopRatingText.text = "Rating: ${shop.rating} ⭐ (${shop.reviewCount})"
            shopDescription.text = shop.shopDescription

            try {
                if (shop.imageBase64.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(shop.getBitmap())
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(shopImage)
                } else {
                    shopImage.setImageResource(R.drawable.placeholder)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                shopImage.setImageResource(R.drawable.placeholder)
            }

            itemView.setOnClickListener {
                onClick(shop)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_cards, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        holder.bind(shopList[position])
    }

    override fun getItemCount(): Int = shopList.size
}


