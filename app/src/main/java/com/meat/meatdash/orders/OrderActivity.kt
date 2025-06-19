package com.meat.meatdash.orders

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.meat.meatdash.adapter.OrderAdapter
import com.meat.meatdash.databinding.ActivityOrderBinding
import com.meat.meatdash.model.Order
import com.meat.meatdash.model.OrderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OrderActivity : AppCompatActivity() {

    companion object { private const val REQUEST_LOCATION_PERMISSION = 1001 }

    private lateinit var binding: ActivityOrderBinding
    private lateinit var adapter: OrderAdapter

    private val allOrders      = mutableListOf<Order>()
    private val activeOrders   = mutableListOf<Order>()
    private val inactiveOrders = mutableListOf<Order>()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth      = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkLocationPermission()

        adapter = OrderAdapter(allOrders, onDelivered = { delivered ->
            // Mark delivered in Firestore
            firestore.collection("OrderList")
                .document(delivered.docId)
                .update("status", "Delivered")
                .addOnSuccessListener {
                    // Move it locally
                    activeOrders.removeAll { it.docId == delivered.docId }
                    if (inactiveOrders.none { it.docId == delivered.docId })
                        inactiveOrders.add(delivered)
                    // Refresh the active tab
                    showActive()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update status: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }, this)

        binding.recyclerViewOrders.apply {
            layoutManager = LinearLayoutManager(this@OrderActivity)
            adapter = this@OrderActivity.adapter
        }

        binding.tabLayout.apply {
            addTab(newTab().setText("Active"))
            addTab(newTab().setText("Inactive"))
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (tab.position == 0) showActive() else showInactive()
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }

        loadOrders()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadOrders() {
        binding.progressBarLoading.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                withContext(Dispatchers.Main) {
                    binding.progressBarLoading.visibility = View.GONE
                    Toast.makeText(this@OrderActivity, "Not logged in", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                val snap = firestore.collection("OrderList")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()

                allOrders.clear()
                activeOrders.clear()
                inactiveOrders.clear()

                for (doc in snap.documents) {
                    val data = doc.data ?: continue

                    // 1) Build items
                    val items = (data["items"] as? List<Map<String, Any>>)?.map { m ->
                        OrderItem(
                            id       = m["id"]       as? String   ?: "",
                            name     = m["name"]     as? String   ?: "",
                            price    = (m["price"]    as? Number)?.toDouble() ?: 0.0,
                            quantity = (m["quantity"] as? Number)?.toInt()    ?: 0,
                            image    = m["image"]    as? String   ?: ""
                        )
                    } ?: emptyList()

                    // 2) Compute expectedDeliveryAt = timestamp/1000 + 3 days
                    val tsMillis = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val expAt = tsMillis / 1000 + 3 * 24 * 3600

                    // 3) Create Order model
                    val order = Order(
                        docId              = doc.id,
                        orderId            = data["orderId"]   as? String ?: "",
                        userId             = data["userId"]    as? String ?: "",
                        shopName           = data["shopName"]  as? String ?: "",
                        shopLocation       = data["shopLocation"] as? String ?: "",
                        title              = data["title"]     as? String ?: "",
                        streetAddress      = data["streetAddress"] as? String ?: "",
                        houseApartment     = data["houseApartment"] as? String ?: "",
                        city               = data["city"]      as? String ?: "",
                        state              = data["state"]     as? String ?: "",
                        zipCode            = data["zipCode"]   as? String ?: "",
                        fullName           = data["fullName"]  as? String ?: "",
                        phoneNumber        = data["phoneNumber"] as? String ?: "",
                        subtotal           = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                        shipping           = (data["shipping"] as? Number)?.toDouble() ?: 0.0,
                        tax                = (data["tax"]      as? Number)?.toDouble() ?: 0.0,
                        total              = (data["total"]    as? Number)?.toDouble() ?: 0.0,
                        paymentId          = data["paymentId"] as? String,
                        status             = data["status"]    as? String ?: "",
                        timestamp          = tsMillis,
                        deliveryDate       = data["deliveryDate"] as? String ?: "",
                        shopLat            = 0.0, // drop geocoding for now
                        shopLng            = 0.0,
                        userLat            = 0.0,
                        userLng            = 0.0,
                        items              = items,
                        expectedDeliveryAt = expAt
                    )

                    // 4) Sort into active/inactive
                    if (order.status.equals("Delivered", true)
                        || order.status.equals("Cancelled", true)
                    ) {
                        inactiveOrders.add(order)
                    } else {
                        activeOrders.add(order)
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.progressBarLoading.visibility = View.GONE
                    showActive()
                }

            } catch (e: Exception) {
                Log.e("OrderActivity", "loadOrders failed", e)
                withContext(Dispatchers.Main) {
                    binding.progressBarLoading.visibility = View.GONE
                    Toast.makeText(
                        this@OrderActivity,
                        "Failed to load orders: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showActive() {
        allOrders.clear()
        allOrders.addAll(activeOrders)
        adapter.clearExpanded()
        adapter.notifyDataSetChanged()

        binding.recyclerViewOrders.visibility      =
            if (activeOrders.isEmpty()) View.GONE else View.VISIBLE
        binding.textViewNoActiveOrders.visibility  =
            if (activeOrders.isEmpty()) View.VISIBLE else View.GONE
        binding.textViewNoInactiveOrders.visibility = View.GONE
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showInactive() {
        allOrders.clear()
        allOrders.addAll(inactiveOrders)
        adapter.clearExpanded()
        adapter.notifyDataSetChanged()

        binding.recyclerViewOrders.visibility       =
            if (inactiveOrders.isEmpty()) View.GONE else View.VISIBLE
        binding.textViewNoInactiveOrders.visibility =
            if (inactiveOrders.isEmpty()) View.VISIBLE else View.GONE
        binding.textViewNoActiveOrders.visibility   = View.GONE
    }
}


//
//package com.meat.meatdash.orders
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.view.View
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.google.android.material.tabs.TabLayout
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.meat.meatdash.databinding.ActivityOrderBinding
//import com.meat.meatdash.model.Order
//import com.meat.meatdash.orders.adapter.OrderAdapter
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.withContext
//
//class OrderActivity : AppCompatActivity() {
//
//    companion object { private const val REQUEST_LOCATION_PERMISSION = 1001 }
//
//    private lateinit var binding: ActivityOrderBinding
//    private lateinit var adapter: OrderAdapter
//
//    private val allOrders      = mutableListOf<Order>()
//    private val activeOrders   = mutableListOf<Order>()
//    private val inactiveOrders = mutableListOf<Order>()
//
//    private val firestore = FirebaseFirestore.getInstance()
//    private val auth      = FirebaseAuth.getInstance()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityOrderBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        checkLocationPermission()
//
//        adapter = OrderAdapter(allOrders, onDelivered = { delivered ->
//            // 1) Update status in Firestore
//            firestore.collection("OrderList")
//                .document(delivered.docId)
//                .update("status", "Delivered")
//                .addOnSuccessListener {
//                    // 2) Locally move from active→inactive
//                    activeOrders.removeAll { it.docId == delivered.docId }
//                    if (inactiveOrders.none { it.docId == delivered.docId })
//                        inactiveOrders.add(delivered)
//                    showActive()
//                }
//                .addOnFailureListener { e ->
//                    Toast.makeText(
//                        this,
//                        "Failed to update status: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//        }, this)
//
//        binding.recyclerViewOrders.apply {
//            layoutManager = LinearLayoutManager(this@OrderActivity)
//            adapter = this@OrderActivity.adapter
//        }
//
//        binding.tabLayout.apply {
//            addTab(newTab().setText("Active"))
//            addTab(newTab().setText("Inactive"))
//            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//                override fun onTabSelected(tab: TabLayout.Tab) {
//                    if (tab.position == 0) showActive() else showInactive()
//                }
//                override fun onTabUnselected(tab: TabLayout.Tab) {}
//                override fun onTabReselected(tab: TabLayout.Tab) {}
//            })
//        }
//
//        loadOrders()
//    }
//
//    private fun checkLocationPermission() {
//        if (ContextCompat.checkSelfPermission(
//                this, Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                REQUEST_LOCATION_PERMISSION
//            )
//        }
//    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    private fun loadOrders() {
//        binding.progressBarLoading.visibility = View.VISIBLE
//        lifecycleScope.launch(Dispatchers.IO) {
//            val uid = auth.currentUser?.uid
//            if (uid == null) {
//                withContext(Dispatchers.Main) {
//                    binding.progressBarLoading.visibility = View.GONE
//                    Toast.makeText(this@OrderActivity,"Not logged in",Toast.LENGTH_SHORT).show()
//                }
//                return@launch
//            }
//
//            try {
//                val snap = firestore.collection("OrderList")
//                    .whereEqualTo("userId", uid)
//                    .get().await()
//
//                allOrders.clear()
//                activeOrders.clear()
//                inactiveOrders.clear()
//
//                for (doc in snap.documents) {
//                    val data = doc.data ?: continue
//
//                    // Build item list, parse timestamp, compute expectedDeliveryAt (in sec)
//                    val tsMillis = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
//                    val expAtSec = tsMillis/1000 + 3*24*3600
//
//                    val items = (data["items"] as? List<Map<String,Any>>)?.map { m ->
//                        // … map fields …
//                    } ?: emptyList()
//
//                    val order = Order(
//                        docId = doc.id,
//                        orderId = data["orderId"] as? String ?: "",
//                        userId = data["userId"] as? String ?: "",
//                        shopName = data["shopName"] as? String ?: "",
//                        shopLocation = data["shopLocation"] as? String ?: "",
//                        title = data["title"] as? String ?: "",
//                        streetAddress = data["streetAddress"] as? String ?: "",
//                        houseApartment = data["houseApartment"] as? String ?: "",
//                        city = data["city"] as? String ?: "",
//                        state = data["state"] as? String ?: "",
//                        zipCode = data["zipCode"] as? String ?: "",
//                        fullName = data["fullName"] as? String ?: "",
//                        phoneNumber = data["phoneNumber"] as? String ?: "",
//                        subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
//                        shipping = (data["shipping"] as? Number)?.toDouble() ?: 0.0,
//                        tax = (data["tax"]      as? Number)?.toDouble() ?: 0.0,
//                        total = (data["total"]    as? Number)?.toDouble() ?: 0.0,
//                        paymentId = data["paymentId"] as? String,
//                        status = data["status"]    as? String ?: "",
//                        timestamp = tsMillis,
//                        deliveryDate = data["deliveryDate"] as? String ?: "",
//                        shopLat = 0.0, // omitted for brevity
//                        shopLng = 0.0,
//                        userLat = 0.0,
//                        userLng = 0.0,
//                        items = items,
//                        expectedDeliveryAt = expAtSec
//                    )
//
//                    if (order.status.equals("Delivered", true) ||
//                        order.status.equals("Cancelled", true)
//                    ) inactiveOrders.add(order)
//                    else activeOrders.add(order)
//                }
//
//                withContext(Dispatchers.Main) {
//                    binding.progressBarLoading.visibility = View.GONE
//                    showActive()
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    binding.progressBarLoading.visibility = View.GONE
//                    Toast.makeText(
//                        this@OrderActivity,
//                        "Failed to load orders: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        }
//    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    private fun showActive() {
//        allOrders.clear()
//        allOrders.addAll(activeOrders)
//        adapter.clearExpanded()
//        adapter.notifyDataSetChanged()
//        binding.recyclerViewOrders.visibility     = if (activeOrders.isEmpty()) View.GONE else View.VISIBLE
//        binding.textViewNoActiveOrders.visibility = if (activeOrders.isEmpty()) View.VISIBLE else View.GONE
//        binding.textViewNoInactiveOrders.visibility = View.GONE
//    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    private fun showInactive() {
//        allOrders.clear()
//        allOrders.addAll(inactiveOrders)
//        adapter.clearExpanded()
//        adapter.notifyDataSetChanged()
//        binding.recyclerViewOrders.visibility      = if (inactiveOrders.isEmpty()) View.GONE else View.VISIBLE
//        binding.textViewNoInactiveOrders.visibility= if (inactiveOrders.isEmpty()) View.VISIBLE else View.GONE
//        binding.textViewNoActiveOrders.visibility   = View.GONE
//    }
//}
