//package com.meat.meatdash.model
//
//import com.google.firebase.firestore.DocumentId
//
//data class Order(
//    var id: String = "",
//    @DocumentId
//    val docId: String = "",
//    val orderId: String = "",
//    val userId: String = "",
//    val shopName: String = "",
//    val shopRegId: String = "",
//    val shopLocation: String = "",
//    val title: String = "",
//    val streetAddress: String = "",
//    val houseApartment: String = "",
//    val city: String = "",
//    val state: String = "",
//    val zipCode: String = "",
//    val fullName: String = "",
//    val phoneNumber: String = "",
//    val subtotal: Double = 0.0,
//    val shipping: Double = 0.0,
//    val tax: Double = 0.0,
//    val total: Double = 0.0,
//    val paymentId: String? = null,
//    val shopLat: Double = 0.0,
//    val shopLng: Double = 0.0,
//    var timestamp: Long = 0L,           // when order was placed (sec since epoch)
//    var status: String = "",            // "Pending", "Delivered", etc.
//    val deliveryDate: String = "",
//    val items: List<Unit> = listOf(),
//    val userAddress: String = "",
//    val userLat: Double = 0.0,
//    val userLng: Double = 0.0,
//    val createdAt: Long = 0L,
//
//    // NEW: absolute epoch-second deadline for delivery
//    var expectedDeliveryAt: Long = 0L
//)
package com.meat.meatdash.model

import java.io.Serializable

data class Order(
    val docId: String,
    val orderId: String,
    val userId: String,
    val shopName: String,
    val shopLocation: String,
    val title: String,
    val streetAddress: String,
    val houseApartment: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val fullName: String,
    val phoneNumber: String,
    val subtotal: Double,
    val shipping: Double,
    val tax: Double,
    val total: Double,
    val paymentId: String?,
    var status: String,
    val timestamp: Long,
    val deliveryDate: String,
    val shopLat: Double,
    val shopLng: Double,
    val userLat: Double,
    val userLng: Double,
    var timerEndTime: Long = 0L,
    val items: List<OrderItem>,
    val expectedDeliveryAt: Long

) : Serializable

