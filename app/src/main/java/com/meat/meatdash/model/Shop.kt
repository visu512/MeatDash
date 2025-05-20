package com.meat.meatdash.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

data class Shop(
    val id: String = "",
    val shopName: String = "",
    val shopDescription: String = "",
    val shopLocation: String = "",
    val shopRegId: String = "",
    val ownerName: String = "",
    val aadharNumber: String = "",
    val phoneNumber: String = "",
    val imageBase64: String = "",
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val category: String = "General",
    val status: String = "pending"
) {
    fun getBitmap(): Bitmap? {
        return try {
            val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}