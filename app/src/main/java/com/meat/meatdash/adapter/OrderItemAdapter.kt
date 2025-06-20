////////package com.meat.meatdash.adapter
////////
////////import android.graphics.BitmapFactory
////////import android.util.Base64
////////import android.view.LayoutInflater
////////import android.view.View
////////import android.view.ViewGroup
////////import android.widget.ImageView
////////import android.widget.TextView
////////import androidx.recyclerview.widget.RecyclerView
////////import com.meat.meatdash.R
////////
////////class OrderItemAdapter(
////////    private val items: List<Unit>
////////) : RecyclerView.Adapter<OrderItemAdapter.ItemViewHolder>() {
////////
////////    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
////////        val imageOrderItem: ImageView = itemView.findViewById(R.id.imageOrderItem)
////////        val textItemName: TextView = itemView.findViewById(R.id.textItemName)
////////        val textItemQtyPrice: TextView = itemView.findViewById(R.id.textItemQtyPrice)
//////////        val cardView: CardView = itemView.findViewById(R.id.cardView)
////////    }
////////
////////    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
////////        val view = LayoutInflater.from(parent.context)
////////            .inflate(R.layout.item_order_item, parent, false)
////////        return ItemViewHolder(view)
////////    }
////////
////////    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
////////        val orderItem = items[position]
////////
////////        // cardview always white
//////////        holder.cardView.setCardBackgroundColor(Color.WHITE)
////////
////////        // Decode Base64 image if available
////////        if (!orderItem.image.isNullOrEmpty()) {
////////            try {
////////                val bytes = Base64.decode(orderItem.image, Base64.DEFAULT)
////////                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
////////                holder.imageOrderItem.setImageBitmap(bmp)
////////            } catch (e: Exception) {
////////                holder.imageOrderItem.setImageResource(R.drawable.placeholder)
////////            }
////////        } else {
////////            holder.imageOrderItem.setImageResource(R.drawable.placeholder)
////////        }
////////
////////        holder.textItemName.text = orderItem.name
////////
////////        // Format: “Qty: 500g  • ₹250.00”
////////        val weightStr = if (orderItem.quantity >= 1000) {
////////            "${orderItem.quantity / 1000} kg"
////////        } else {
////////            "${orderItem.quantity} g"
////////        }
////////        val priceStr = "₹%.2f".format(orderItem.price)
////////        holder.textItemQtyPrice.text = "Qty: $weightStr  • $priceStr"
////////
////////    }
////////
////////    override fun getItemCount(): Int = items.size
////////}
//////
//////
//////package com.meat.meatdash.adapter
//////
//////import android.graphics.BitmapFactory
//////import android.util.Base64
//////import android.view.LayoutInflater
//////import android.view.View
//////import android.view.ViewGroup
//////import android.widget.ImageView
//////import android.widget.TextView
//////import androidx.recyclerview.widget.RecyclerView
//////import com.meat.meatdash.R
//////import com.meat.meatdash.model.OrderItem
//////
//////class OrderItemAdapter(
//////    private val items: List<OrderItem>
//////) : RecyclerView.Adapter<OrderItemAdapter.ItemViewHolder>() {
//////
//////    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//////        val imageOrderItem: ImageView = itemView.findViewById(R.id.imageOrderItem)
//////        val textItemName: TextView = itemView.findViewById(R.id.textItemName)
//////        val textItemQtyPrice: TextView = itemView.findViewById(R.id.textItemQtyPrice)
//////    }
//////
//////    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
//////        val view = LayoutInflater.from(parent.context)
//////            .inflate(R.layout.item_order_item, parent, false)
//////        return ItemViewHolder(view)
//////    }
//////
//////    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
//////        val orderItem = items[position]
//////
//////        // Decode Base64 image if available, else placeholder
//////        if (!orderItem.image.isNullOrEmpty()) {
//////            try {
//////                val bytes = Base64.decode(orderItem.image, Base64.DEFAULT)
//////                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//////                holder.imageOrderItem.setImageBitmap(bmp)
//////            } catch (e: Exception) {
//////                holder.imageOrderItem.setImageResource(R.drawable.placeholder)
//////            }
//////        } else {
//////            holder.imageOrderItem.setImageResource(R.drawable.placeholder)
//////        }
//////
//////        // Name
//////        holder.textItemName.text = orderItem.name
//////
//////        // Format quantity and price: “Qty: 500g  • ₹250.00”
//////        val weightStr = if (orderItem.quantity >= 1000) {
//////            "${orderItem.quantity / 1000} kg"
//////        } else {
//////            "${orderItem.quantity} g"
//////        }
//////        val priceStr = "₹%.2f".format(orderItem.price)
//////        holder.textItemQtyPrice.text = "Qty: $weightStr  • $priceStr"
//////    }
//////
//////    override fun getItemCount(): Int = items.size
//////}
////
////package com.meat.meatdash.adapter
////
////import android.graphics.BitmapFactory
////import android.util.Base64
////import android.view.LayoutInflater
////import android.view.View
////import android.view.ViewGroup
////import android.widget.ImageView
////import android.widget.TextView
////import androidx.recyclerview.widget.RecyclerView
////import com.meat.meatdash.R
////
////class OrderItemAdapter(
////    private val items: List<Unit>     // ← use OrderItem, not Unit
////) : RecyclerView.Adapter<OrderItemAdapter.ItemViewHolder>() {
////
////    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
////        val imageOrderItem: ImageView  = itemView.findViewById(R.id.imageOrderItem)
////        val textItemName: TextView     = itemView.findViewById(R.id.textItemName)
////        val textItemQtyPrice: TextView = itemView.findViewById(R.id.textItemQtyPrice)
////    }
////
////    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
////        ItemViewHolder(
////            LayoutInflater.from(parent.context)
////                .inflate(R.layout.item_order_item, parent, false)
////        )
////
////    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
////        val orderItem = items[position]
////
////        // Decode Base64 image or fallback to placeholder
////        if (!orderItem.image.isNullOrEmpty()) {
////            try {
////                val bytes = Base64.decode(orderItem.image, Base64.DEFAULT)
////                val bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
////                holder.imageOrderItem.setImageBitmap(bmp)
////            } catch (_: Exception) {
////                holder.imageOrderItem.setImageResource(R.drawable.placeholder)
////            }
////        } else {
////            holder.imageOrderItem.setImageResource(R.drawable.placeholder)
////        }
////
////        // Set name
////        holder.textItemName.text = orderItem.name
////
////        // Format quantity and price
////        val weightStr = if (orderItem.quantity >= 1000)
////            "${orderItem.quantity / 1000} kg"
////        else
////            "${orderItem.quantity} g"
////
////        val priceStr = "₹%.2f".format(orderItem.price)
////        holder.textItemQtyPrice.text = "Qty: $weightStr  • $priceStr"
////    }
////
////    override fun getItemCount() = items.size
////}
//
//
//
//package com.meat.meatdash.adapter
//
//import android.graphics.BitmapFactory
//import android.util.Base64
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.meat.meatdash.R
//import com.meat.meatdash.model.OrderItem
//
//class OrderItemAdapter(
//    private val items: List<OrderItem>
//) : RecyclerView.Adapter<OrderItemAdapter.ItemViewHolder>() {
//
//    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val imageOrderItem: ImageView  = itemView.findViewById(R.id.imageOrderItem)
//        val textItemName: TextView     = itemView.findViewById(R.id.textItemName)
//        val textItemQtyPrice: TextView = itemView.findViewById(R.id.textItemQtyPrice)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
//        ItemViewHolder(
//            LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_order_item, parent, false)
//        )
//
//    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
//        val orderItem = items[position]
//
//        // Decode Base64 image or show placeholder
//        if (orderItem.image.isNotEmpty()) {
//            try {
//                val bytes = Base64.decode(orderItem.image, Base64.DEFAULT)
//                val bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//                holder.imageOrderItem.setImageBitmap(bmp)
//            } catch (_: Exception) {
//                holder.imageOrderItem.setImageResource(R.drawable.placeholder)
//            }
//        } else {
//            holder.imageOrderItem.setImageResource(R.drawable.placeholder)
//        }
//
//        // Name
//        holder.textItemName.text = orderItem.name
//
//        // Quantity & Price formatting
//        val weightStr = if (orderItem.quantity >= 1000)
//            "${orderItem.quantity / 1000} kg"
//        else
//            "${orderItem.quantity} g"
//
//        val priceStr = "₹%.2f".format(orderItem.price)
//        holder.textItemQtyPrice.text = "Qty: $weightStr  • $priceStr"
//    }
//
//    override fun getItemCount() = items.size
//}



package com.meat.meatdash.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.meat.meatdash.R
import com.meat.meatdash.model.OrderItem

class OrderItemAdapter(
    private val items: List<OrderItem>
) : RecyclerView.Adapter<OrderItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageOrderItem: ImageView  = itemView.findViewById(R.id.imageOrderItem)
        val textItemName: TextView     = itemView.findViewById(R.id.textItemName)
        val textItemQtyPrice: TextView = itemView.findViewById(R.id.textItemQtyPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ItemViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_order_item, parent, false)
        )

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val orderItem = items[position]

        // Decode Base64 image or show placeholder
        if (orderItem.image.isNotEmpty()) {
            try {
                val bytes = Base64.decode(orderItem.image, Base64.DEFAULT)
                val bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.imageOrderItem.setImageBitmap(bmp)
            } catch (_: Exception) {
                holder.imageOrderItem.setImageResource(R.drawable.placeholder)
            }
        } else {
            holder.imageOrderItem.setImageResource(R.drawable.placeholder)
        }

        // Name
        holder.textItemName.text = orderItem.name

        // Quantity & Price formatting
        val weightStr = if (orderItem.quantity >= 1000)
            "${orderItem.quantity / 1000} kg"
        else
            "${orderItem.quantity} g"

        val priceStr = "₹%.2f".format(orderItem.price)
        holder.textItemQtyPrice.text = "Qty: $weightStr  • $priceStr"
    }

    override fun getItemCount() = items.size
}
