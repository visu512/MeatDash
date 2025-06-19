package com.meat.meatdash.model

import java.io.Serializable
import java.sql.Timestamp

data class Address(
    val fullName: String = "",
    val phoneNumber: String = "",
    val street: String = "",
    val houseApartment: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val current: String = "",
    val title: String = "",
    var savedAt: Long = 0,
    var createdAt: Long = 0,
    val latitude: Double=0.0,
    val longitude: Double=0.0,
    val isDefault: Boolean = false
) : Serializable {

}
