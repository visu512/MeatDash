//package com.meat.meatdash.model
//
//import android.content.Intent
//import android.view.View
//import android.widget.Toast
//import com.meat.meatdash.activity.CartActivity
//import com.meat.meatdash.adapter.PopularFoodAdapter
//
//data class FoodItem(
//    val id: String = "",
//    val name: String = "",
//    val description: String = "",
//    val price: Int = 0,
//    val shopName: String="",
//    val imageBase64: String = "",
//    var weight: Int = 250, // in grams by default
//    val shopId: String = "", // reference to shop
//    val timestamp: com.google.firebase.Timestamp? = null
//)


package com.meat.meatdash.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.firebase.Timestamp

data class FoodItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val shopName: String = "",
    val imageBase64: String = "",
    var weight: Int = 250, // in grams
    val shopId: String = "",
    val timestamp: Timestamp? = null
) {
    fun getBitmap(): Bitmap? {
        return try {
            val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}