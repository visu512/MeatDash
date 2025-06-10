package com.meat.meatdash.model

import java.io.Serializable

data class OrderAddress(
    val fullName: String = "",
    val phoneNumber: String = "",
    val streetAddress: String = "",
    val houseApartment: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = ""
) : Serializable
