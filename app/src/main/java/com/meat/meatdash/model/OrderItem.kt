package com.meat.meatdash.model

import java.io.Serializable

data class OrderItem(
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val image: String // Base64
)