// CartAdapter.kt
package com.meat.meatdash.adapter

import android.graphics.BitmapFactory
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.meat.meatdash.CartManager
import com.meat.meatdash.R
import com.meat.meatdash.model.FoodItem

class CartAdapter(
    private var cartItems: MutableList<FoodItem>,
    private val onQuantityChanged: () -> Unit,
    private val onItemRemoved: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    // track positions where weight < 100g
    private var invalidPositions: Set<Int> = emptySet()

    init {
        // ensure each item view is tied to its item’s stable ID
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return cartItems[position].id.hashCode().toLong()
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

        // --- load image or placeholder ---
        holder.itemImage.setImageResource(R.drawable.placeholder)
        item.imageBase64?.takeIf { it.isNotEmpty() }?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.itemImage.setImageBitmap(bmp)
            } catch (_: Exception) {
                holder.itemImage.setImageResource(R.drawable.placeholder)
            }
        }

        // name ---
        holder.itemName.text = item.name

        //  weight input & unit spinner ---
        val isKg = item.weight >= 1000
        val displayVal = if (isKg) (item.weight / 1000) else item.weight
        holder.itemWeight.setText(displayVal.toString())

        ArrayAdapter.createFromResource(
            holder.itemView.context,
            R.array.weight_units,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            holder.weightUnitSpinner.adapter = this
        }
        holder.weightUnitSpinner.setSelection(if (isKg) 1 else 0, false)

        // --- TextWatcher: detach old, attach new ---
        holder.currentTextWatcher?.let { holder.itemWeight.removeTextChangedListener(it) }
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) =
                updateWeightAndPrice(holder, item, pricePerKg)
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        holder.itemWeight.addTextChangedListener(watcher)
        holder.currentTextWatcher = watcher

        // --- unit change listener ---
        holder.weightUnitSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, pos: Int, id: Long
                ) {
                    updateWeightAndPrice(holder, item, pricePerKg)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        // --- remove button: delegate to CartManager only ---
        holder.btnRemove.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                val toDelete = cartItems[pos]
                CartManager.removeItem(toDelete) { success ->
                    if (success) {
                        // Activity’s listener (setCartUpdateListener) will call adapter.updateItems(...)
                        onItemRemoved()
                        onQuantityChanged()
                    } else {
                        Toast.makeText(
                            holder.itemView.context,
                            "Failed to delete item. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // --- initial price render ---
        updatePrice(holder, pricePerKg)

        // --- show or clear error if weight < 100g ---
        if (invalidPositions.contains(position)) {
            holder.itemWeight.error = "Minimum weight 100 g"
        } else {
            holder.itemWeight.error = null
        }
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView      = itemView.findViewById(R.id.cartItemImage)
        val itemName: TextView        = itemView.findViewById(R.id.cartItemName)
        val itemPrice: TextView       = itemView.findViewById(R.id.cartItemPrice)
        val itemWeight: EditText      = itemView.findViewById(R.id.cartItemWeight)
        val weightUnitSpinner: Spinner= itemView.findViewById(R.id.weightUnitSpinner)
        val btnRemove: ImageButton    = itemView.findViewById(R.id.btnRemove)
        var currentTextWatcher: TextWatcher? = null
    }

    // Update model’s weight, recalc UI price & totals, then async-persist change. */
    private fun updateWeightAndPrice(
        holder: CartViewHolder,
        item: FoodItem,
        pricePerKg: Int
    ) {
        val raw  = holder.itemWeight.text.toString()
        val num  = raw.toDoubleOrNull() ?: 0.0
        val unit = holder.weightUnitSpinner.selectedItem.toString()
        val grams = if (unit == "kg") (num * 1000).toInt() else num.toInt()

        // 1) persist to CartManager (prefs + internal list)
        CartManager.updateItemWeight(item, grams) { /* no-op */ }

        // 2) refresh UI & notify total callback
        updatePrice(holder, pricePerKg)
        onQuantityChanged()
    }

    // Compute and bind the price text */
    private fun updatePrice(holder: CartViewHolder, pricePerKg: Int) {
        val raw  = holder.itemWeight.text.toString()
        val num  = raw.toDoubleOrNull() ?: 0.0
        val unit = holder.weightUnitSpinner.selectedItem.toString()
        val kg   = if (unit == "kg") num else num / 1000.0
        val total = pricePerKg * kg
        holder.itemPrice.text =
            holder.itemView.context.getString(R.string.price_format, total, pricePerKg)
    }

    // Highlight any positions where weight < 100g */
    fun setInvalidWeights(invalid: Set<Int>) {
        invalidPositions = invalid
        notifyDataSetChanged()
    }

    // Replace adapter data from CartManager */
    fun updateItems(newItems: List<FoodItem>) {
        cartItems = newItems.toMutableList()
        invalidPositions = emptySet()
        notifyDataSetChanged()
    }

    // Expose current list */
    fun getItems(): List<FoodItem> = cartItems.toList()
}
