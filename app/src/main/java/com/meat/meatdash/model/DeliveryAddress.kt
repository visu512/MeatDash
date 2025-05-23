package com.meat.meatdash.model

data class DeliveryAddress(
    val fullName: String = "",
    val phone: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: String = ""
)
