package com.meat.meatdash.adapter

import android.graphics.BitmapFactory
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.meat.meatdash.CartManager
import com.meat.meatdash.R
import com.meat.meatdash.model.FoodItem

class CartAdapter(
    private var cartItems: MutableList<FoodItem>,
    private val onQuantityChanged: () -> Unit,
    private val onItemRemoved: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView       = itemView.findViewById(R.id.cartItemImage)
        val itemName: TextView         = itemView.findViewById(R.id.cartItemName)
        val itemPrice: TextView        = itemView.findViewById(R.id.cartItemPrice)
        val itemWeight: EditText       = itemView.findViewById(R.id.cartItemWeight)
        val weightUnitSpinner: Spinner = itemView.findViewById(R.id.weightUnitSpinner)
        val btnRemove: ImageButton     = itemView.findViewById(R.id.btnRemove)
        var currentTextWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun getItemCount(): Int = cartItems.size

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]
        val pricePerKg = item.price

        // 1) Enforce stored minimum weight of 100g
        if (item.weight < 100) {
            item.weight = 100
            CartManager.updateItemWeight(item, 100) {}
        }

        // 2) Load image (or placeholder)
        holder.itemImage.setImageResource(R.drawable.placeholder)
        item.imageBase64?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.itemImage.setImageBitmap(bmp)
            } catch (_: Exception) {
                holder.itemImage.setImageResource(R.drawable.placeholder)
            }
        }

        // 3) Name
        holder.itemName.text = item.name

        // 4) Weight display & unit spinner
        val isKg = item.weight >= 1000
        val displayVal = if (isKg) (item.weight / 1000).toString() else item.weight.toString()
        holder.itemWeight.setText(displayVal)

        ArrayAdapter.createFromResource(
            holder.itemView.context,
            R.array.weight_units,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            holder.weightUnitSpinner.adapter = this
        }
        holder.weightUnitSpinner.setSelection(if (isKg) 1 else 0, false)

        // 5) Watcher for user edits
        holder.currentTextWatcher?.let {
            holder.itemWeight.removeTextChangedListener(it)
        }
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) =
                updateWeightAndPrice(holder, item, pricePerKg)
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        holder.itemWeight.addTextChangedListener(watcher)
        holder.currentTextWatcher = watcher

        // 6) Unit‐change listener
        holder.weightUnitSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, pos: Int, id: Long
                ) {
                    updateWeightAndPrice(holder, item, pricePerKg)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        // 7) Remove button
        holder.btnRemove.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                CartManager.removeItem(cartItems[pos]) { success ->
                    if (success) {
                        cartItems.removeAt(pos)
                        notifyItemRemoved(pos)
                        notifyItemRangeChanged(pos, cartItems.size)
                        onItemRemoved()
                        onQuantityChanged()
                    }
                }
            }
        }

        // 8) Price update
        updatePrice(holder, pricePerKg)
    }

    private fun updateWeightAndPrice(
        holder: CartViewHolder,
        item: FoodItem,
        pricePerKg: Int
    ) {
        val raw = holder.itemWeight.text.toString()
        val num = raw.toDoubleOrNull() ?: 0.0
        val unit = holder.weightUnitSpinner.selectedItem.toString()
        val grams = if (unit == "kg") (num * 1000).toInt() else num.toInt()

        // Enforce minimum 100g
        if (grams < 100) {
//            Toast.makeText(
//                holder.itemView.context,
//                "Minimum order is 100 g",
//                Toast.LENGTH_SHORT
//            ).show()
//            holder.itemWeight.setText("100")
            holder.itemWeight.setSelection(holder.itemWeight.text.length)
            return
        }

        item.weight = grams
        CartManager.updateItemWeight(item, grams) {
            updatePrice(holder, pricePerKg)
            onQuantityChanged()
        }
    }

    private fun updatePrice(holder: CartViewHolder, pricePerKg: Int) {
        val raw = holder.itemWeight.text.toString()
        val num = raw.toDoubleOrNull() ?: 0.0
        val unit = holder.weightUnitSpinner.selectedItem.toString()
        val kg = if (unit == "kg") num else num / 1000.0
        val total = pricePerKg * kg
        holder.itemPrice.text =
            holder.itemView.context.getString(R.string.price_format, total, pricePerKg)
    }

    fun updateItems(newItems: List<FoodItem>) {
        cartItems = newItems.toMutableList()
        notifyDataSetChanged()
    }
}
