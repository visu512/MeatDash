package com.meat.meatdash.adapter

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.meat.meatdash.R
import com.meat.meatdash.model.CartManager
import com.meat.meatdash.model.FoodItem

class CartAdapter(
    private var cartItems: MutableList<FoodItem>,
    private val onQuantityChanged: () -> Unit,
    private val onItemRemoved: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.cartItemImage)
        val itemName: TextView = itemView.findViewById(R.id.cartItemName)
        val itemPrice: TextView = itemView.findViewById(R.id.cartItemPrice)
        val itemWeight: EditText = itemView.findViewById(R.id.cartItemWeight)
        val weightUnitSpinner: Spinner = itemView.findViewById(R.id.weightUnitSpinner)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun getItemCount(): Int = cartItems.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]
        val pricePerKg = item.price

        // Load image
        if (!item.imageBase64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(item.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.itemImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.itemImage.setImageResource(R.drawable.placeholder)
            }
        } else {
            holder.itemImage.setImageResource(R.drawable.placeholder)
        }

        holder.itemName.text = item.name

        // Setup spinner
        val unitAdapter = ArrayAdapter.createFromResource(
            holder.itemView.context,
            R.array.weight_units,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        holder.weightUnitSpinner.adapter = unitAdapter

        // Set initial weight and unit
        if (item.weight >= 1000) {
            holder.itemWeight.setText((item.weight / 1000).toString())
            holder.weightUnitSpinner.setSelection(1) // kg
        } else {
            holder.itemWeight.setText(item.weight.toString())
            holder.weightUnitSpinner.setSelection(0) // g
        }

        updatePrice(holder, pricePerKg)

        // Weight change listener
        holder.itemWeight.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateWeightAndPrice(holder, item, pricePerKg)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Spinner selection listener
        holder.weightUnitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                updateWeightAndPrice(holder, item, pricePerKg)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Remove item
        holder.btnRemove.setOnClickListener {
            CartManager.removeItem(item) { success ->
                if (success) {
                    cartItems.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, cartItems.size)
                    onQuantityChanged()
                    onItemRemoved()
                }
            }
        }
    }

    private fun updateWeightAndPrice(holder: CartViewHolder, item: FoodItem, pricePerKg: Int) {
        val weightText = holder.itemWeight.text.toString()
        val weight = weightText.toDoubleOrNull() ?: 0.0
        val unit = holder.weightUnitSpinner.selectedItem.toString()
        val weightInGrams = if (unit == "kg") (weight * 1000).toInt() else weight.toInt()

        CartManager.updateItemWeight(item, weightInGrams) { success ->
            if (success) {
                item.weight = weightInGrams
                updatePrice(holder, pricePerKg)
                onQuantityChanged()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePrice(holder: CartViewHolder, pricePerKg: Int) {
        val weightText = holder.itemWeight.text.toString()
        val weight = weightText.toDoubleOrNull() ?: 0.0
        val unit = holder.weightUnitSpinner.selectedItem.toString()
        val weightInKg = if (unit == "kg") weight else weight / 1000.0

        val totalPrice = pricePerKg * weightInKg
        holder.itemPrice.text = "₹%.2f (₹%d)".format(totalPrice, pricePerKg)
    }

    fun updateItems(newItems: List<FoodItem>) {
        cartItems = newItems.toMutableList()
        notifyDataSetChanged()
    }
}