package com.meat.meatdash.model

object CartManager {
    private val cartItems = mutableListOf<FoodItem>()

    fun addItem(item: FoodItem) {
        // Check if item already exists in cart
        val existingItem = cartItems.find { it.id == item.id }
        if (existingItem != null) {
            existingItem.weight += item.weight
        } else {
            cartItems.add(item.copy())
        }
    }

    fun removeItem(item: FoodItem) {
        cartItems.removeAll { it.id == item.id }
    }

    fun updateItemWeight(item: FoodItem, newWeight: Int) {
        cartItems.find { it.id == item.id }?.weight = newWeight
    }

    fun getCartItems(): List<FoodItem> {
        return cartItems.toList()
    }

    fun getTotalPrice(): Double {
        return cartItems.sumOf {
            val weightInKg = it.weight / 1000.0
            it.price * weightInKg
        }
    }

    fun clearCart() {
        cartItems.clear()

    }

}
