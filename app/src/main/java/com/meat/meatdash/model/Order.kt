package com.meat.meatdash.model


//data class Order(
//    var id: String ="",
//    val docId: String ="",     // Fire store document ID
//    val orderId: String = "",
//    val userId: String = "",
//    val shopName: String = "",
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
//    val shopLng: Double,
//    val timestamp: Long = 0L,
//    var status: String,    // mutable so we can set “Delivered”
//    val deliveryDate: String = "",
//    val items: List<OrderItem> = listOf()
//)



data class Order(
    var id: String = "",
    val docId: String = "",     // Firestore document ID
    val orderId: String = "",
    val userId: String = "",
    val shopName: String = "",
    // address fields…
    val title: String = "",
    val streetAddress: String = "",
    val houseApartment: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    // payment & totals…
    val subtotal: Double = 0.0,
    val shipping: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val paymentId: String? = null,
    // **Add shopLat here**:
    val shopLat: Double = 0.0,
    val shopLng: Double = 0.0,
    val timestamp: Long = 0L,
    var status: String,    // mutable so we can set “Delivered”
    val deliveryDate: String = "",
    val items: List<OrderItem> = listOf()
)
