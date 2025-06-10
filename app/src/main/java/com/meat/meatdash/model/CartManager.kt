package com.meat.meatdash

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.meat.meatdash.activity.CartActivity
import com.meat.meatdash.model.FoodItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CartManager {
    private var currentShopId: String? = null
    private var currentShopName: String? = null
    private val cartItems = mutableListOf<FoodItem>()
    private var cartUpdateListener: ((Int) -> Unit)? = null
    private lateinit var sharedPref: SharedPreferences
    private val gson = Gson()
    private const val PREF_CART_KEY = "cart_items"
    private const val PREF_SHOP_ID = "current_shop_id"
    private const val PREF_SHOP_NAME = "current_shop_name"

    fun init(context: Context) {
        sharedPref = context.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)
        loadCartFromPrefs()
    }

    fun setCartUpdateListener(listener: (Int) -> Unit) {
        cartUpdateListener = listener
    }

    private fun saveCartToPrefs() {
        val editor = sharedPref.edit()
        editor.putString(PREF_CART_KEY, gson.toJson(cartItems))
        editor.putString(PREF_SHOP_ID, currentShopId)
        editor.putString(PREF_SHOP_NAME, currentShopName)
        editor.apply()
    }

    private fun loadCartFromPrefs() {
        val json = sharedPref.getString(PREF_CART_KEY, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<FoodItem>>() {}.type
            val savedList: MutableList<FoodItem> = gson.fromJson(json, type)
            cartItems.clear()
            cartItems.addAll(savedList)

            currentShopId = sharedPref.getString(PREF_SHOP_ID, null)
            currentShopName = sharedPref.getString(PREF_SHOP_NAME, null)
        }
        cartUpdateListener?.invoke(cartItems.size)
    }

    fun canAddItem(newItem: FoodItem): Boolean {
        return currentShopId == null || currentShopId == newItem.shopId
    }

    fun addItem(item: FoodItem, callback: (Boolean) -> Unit = {}) {
        val existingIndex = cartItems.indexOfFirst { it.id == item.id }
        if (existingIndex != -1) {
            cartItems[existingIndex] = item
        } else {
            cartItems.add(item)
        }
        if (currentShopId == null) {
            currentShopId = item.shopId
            currentShopName = item.shopName
        }
        saveCartToPrefs()
        cartUpdateListener?.invoke(cartItems.size)
        callback(true)
    }

    fun addItemWithFeedback(item: FoodItem, context: Context, callback: (Boolean) -> Unit = {}) {
        if (canAddItem(item)) {
            addItem(item) { success -> callback(success) }
        } else {
            showShopMismatchDialog(context, item) {
                clearCart {
                    addItem(item) { success -> callback(success) }
                }
            }
        }
    }

    fun removeItem(item: FoodItem, callback: (Boolean) -> Unit = {}) {
        cartItems.removeAll { it.id == item.id }
        if (cartItems.isEmpty()) {
            currentShopId = null
            currentShopName = null
        }
        saveCartToPrefs()
        cartUpdateListener?.invoke(cartItems.size)
        callback(true)
    }

    fun updateItemWeight(item: FoodItem, newWeight: Int, callback: () -> Unit) {
        val index = cartItems.indexOfFirst { it.id == item.id }
        if (index != -1) {
            cartItems[index].weight = newWeight
            saveCartToPrefs()
        }
        callback()
    }

    fun clearCart(callback: (() -> Unit)? = null) {
        cartItems.clear()
        currentShopId = null
        currentShopName = null
        saveCartToPrefs()
        cartUpdateListener?.invoke(0)
        callback?.invoke()
    }

    fun getCartItems(): List<FoodItem> = cartItems.toList()
    fun getCurrentShopId(): String? = currentShopId
    fun getCurrentShopName(): String? = currentShopName
    fun getCartItemCount(): Int = cartItems.size

    fun loadCartItems(callback: (Boolean) -> Unit) {
        // We already load on init and saved in prefs, just return success
        callback(true)
    }

    fun showShopMismatchDialog(context: Context, newItem: FoodItem, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Replace cart item?")
            .setMessage(
                "Your cart contains item from ${newItem.shopName}." + " Do you want to discard the selection and add the item from $currentShopName."
            )
            .setPositiveButton("REPLACE") { dialog, _ ->
                dialog.dismiss()
                onConfirm()
            }
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun showCartBottomSheet(context: Context) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_cart_summary, null)

        val itemCount = getCartItemCount()
        view.findViewById<TextView>(R.id.tvItemCount).text =
            context.resources.getQuantityString(R.plurals.items_in_cart, itemCount, itemCount)

        view.findViewById<Button>(R.id.btnViewCart).setOnClickListener {
            context.startActivity(Intent(context, CartActivity::class.java))
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }
}



