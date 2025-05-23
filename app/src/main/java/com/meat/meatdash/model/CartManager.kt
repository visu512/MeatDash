//package com.meat.meatdash.model
//
//import android.util.Log
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.SetOptions
//
//object CartManager {
//    private val db = FirebaseFirestore.getInstance()
//    private val auth = FirebaseAuth.getInstance()
//    private val cartItems = mutableListOf<FoodItem>()
//
//    fun addItem(item: FoodItem, onComplete: (Boolean) -> Unit = {}) {
//        val userId = auth.currentUser?.uid ?: run {
//            onComplete(false)
//            return
//        }
//
//        // Check if item already exists in cart
//        val existingItem = cartItems.find { it.id == item.id }
//        if (existingItem != null) {
//            // Update quantity if item exists
//            existingItem.weight += item.weight
//            updateFirestoreCart(userId, onComplete)
//        } else {
//            // Add new item with default weight (250g)
//            cartItems.add(item.copy(weight = 250))
//            updateFirestoreCart(userId, onComplete)
//        }
//    }
//
//    fun removeItem(item: FoodItem, onComplete: (Boolean) -> Unit = {}) {
//        val userId = auth.currentUser?.uid ?: run {
//            onComplete(false)
//            return
//        }
//
//        cartItems.removeAll { it.id == item.id }
//        updateFirestoreCart(userId, onComplete)
//    }
//
//    fun updateItemWeight(item: FoodItem, newWeight: Int, onComplete: (Boolean) -> Unit = {}) {
//        val userId = auth.currentUser?.uid ?: run {
//            onComplete(false)
//            return
//        }
//
//        cartItems.find { it.id == item.id }?.weight = newWeight
//        updateFirestoreCart(userId, onComplete)
//    }
//
//    fun clearCart(onComplete: (Boolean) -> Unit = {}) {
//        val userId = auth.currentUser?.uid ?: run {
//            onComplete(false)
//            return
//        }
//
//        cartItems.clear()
//        db.collection("Carts").document(userId)
//            .delete()
//            .addOnSuccessListener { onComplete(true) }
//            .addOnFailureListener { onComplete(false) }
//    }
//
//    fun getCartItems(): List<FoodItem> = cartItems.toList()
//
//    fun getCartTotal(): Double {
//        return cartItems.sumOf { it.price * (it.weight / 1000.0) }
//    }
//
//    fun loadCartItems(onComplete: (Boolean) -> Unit) {
//        val userId = auth.currentUser?.uid ?: run {
//            onComplete(false)
//            return
//        }
//
//        db.collection("Carts").document(userId)
//            .get()
//            .addOnSuccessListener { document ->
//                if (document.exists()) {
//                    cartItems.clear()
//                    val items = document.get("items") as? List<Map<String, Any>> ?: emptyList()
//                    items.forEach { itemMap ->
//                        cartItems.add(
//                            FoodItem(
//                                id = itemMap["id"] as? String ?: "",
//                                name = itemMap["name"] as? String ?: "",
//                                price = (itemMap["price"] as? Number)?.toInt() ?: 0,
//                                description = itemMap["description"] as? String ?: "",
//                                imageBase64 = itemMap["imageBase64"] as? String ?: "",
//                                shopId = itemMap["shopId"] as? String ?: "",
//                                shopName = itemMap["shopName"] as? String ?: "",
//                                weight = (itemMap["weight"] as? Number)?.toInt() ?: 250
//                            )
//                        )
//                    }
//                    onComplete(true)
//                } else {
//                    onComplete(true) // No cart exists yet, which is fine
//                }
//            }
//            .addOnFailureListener {
//                onComplete(false)
//            }
//    }
//
//    private fun updateFirestoreCart(userId: String, onComplete: (Boolean) -> Unit) {
//        val cartData = hashMapOf(
//            "items" to cartItems.map { item ->
//                hashMapOf(
//                    "id" to item.id,
//                    "name" to item.name,
//                    "price" to item.price,
//                    "description" to item.description,
//                    "imageBase64" to item.imageBase64,
//                    "shopId" to item.shopId,
//                    "shopName" to item.shopName,
//                    "weight" to item.weight,
//                    "timestamp" to System.currentTimeMillis()
//                )
//            }
//        )
//
//        db.collection("Carts").document(userId)
//            .set(cartData, SetOptions.merge())
//            .addOnSuccessListener { onComplete(true) }
//            .addOnFailureListener { e ->
//                Log.e("CartManager", "Error updating cart", e)
//                onComplete(false)
//            }
//    }
//}



package com.meat.meatdash.model

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object CartManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val cartItems = mutableListOf<FoodItem>()

    fun addItem(item: FoodItem, onComplete: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false)
            return
        }

        val existingItem = cartItems.find { it.id == item.id }
        if (existingItem != null) {
            existingItem.weight += item.weight
        } else {
            cartItems.add(item.copy(weight = 250))
        }

        updateFirestoreCart(userId, onComplete)
    }

    fun removeItem(item: FoodItem, onComplete: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false)
            return
        }

        cartItems.removeAll { it.id == item.id }
        updateFirestoreCart(userId, onComplete)
    }

    fun updateItemWeight(item: FoodItem, newWeight: Int, onComplete: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false)
            return
        }

        cartItems.find { it.id == item.id }?.weight = newWeight
        updateFirestoreCart(userId, onComplete)
    }

    fun clearCart(onComplete: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false)
            return
        }

        cartItems.clear()
        db.collection("Carts").document(userId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getCartItems(): List<FoodItem> = cartItems.toList()

    fun getCartTotal(): Double {
        return cartItems.sumOf { it.price * (it.weight / 1000.0) }
    }

    fun loadCartItems(onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false)
            return
        }

        db.collection("Carts").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    cartItems.clear()
                    val items = document.get("items") as? List<Map<String, Any>> ?: emptyList()
                    items.forEach { itemMap ->
                        cartItems.add(
                            FoodItem(
                                id = itemMap["id"] as? String ?: "",
                                name = itemMap["name"] as? String ?: "",
                                price = (itemMap["price"] as? Number)?.toInt() ?: 0,
                                description = itemMap["description"] as? String ?: "",
                                imageBase64 = itemMap["imageBase64"] as? String ?: "",
                                shopId = itemMap["shopId"] as? String ?: "",
                                shopName = itemMap["shopName"] as? String ?: "",
                                weight = (itemMap["weight"] as? Number)?.toInt() ?: 250
                            )
                        )
                    }
                    onComplete(true)
                } else {
                    onComplete(true)
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    private fun updateFirestoreCart(userId: String, onComplete: (Boolean) -> Unit) {
        val cartData = hashMapOf(
            "items" to cartItems.map { item ->
                hashMapOf(
                    "id" to item.id,
                    "name" to item.name,
                    "price" to item.price,
                    "description" to item.description,
                    "imageBase64" to item.imageBase64,
                    "shopId" to item.shopId,
                    "shopName" to item.shopName,
                    "weight" to item.weight,
                    "timestamp" to System.currentTimeMillis()
                )
            }
        )

        db.collection("Carts").document(userId)
            .set(cartData, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("CartManager", "Error updating cart", e)
                onComplete(false)
            }
    }
}
