package com.meat.meatdash.model

data class FoodItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val imageBase64: String = "",
    var weight: Int = 125, // in grams
    val shopId: String = "" // reference to shop
)

