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
    val shopLocation: String ="",
    val imageBase64: String = "",
    var weight: Int = 0, // in grams minimum by default
    val shopId: String = "",
    val timestamp: Timestamp? = null,
    val shopLat: Double = 0.0,
    val shopLng: Double = 0.0,
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