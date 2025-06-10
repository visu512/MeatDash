package com.meat.meatdash.orders

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.meat.meatdash.activity.MainActivity
import com.meat.meatdash.adapter.OrderAdapter
import com.meat.meatdash.databinding.ActivityOrderBinding
import com.meat.meatdash.model.Order
import com.meat.meatdash.model.OrderItem

class OrderActivity : AppCompatActivity() {

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

        // RecyclerView & Adapter
        adapter = OrderAdapter(allOrders) { delivered ->
            binding.recyclerViewOrders.post {
                firestore.collection("OrderList")
                    .document(delivered.docId)
                    .update("status", "Delivered")
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Failed to mark delivered",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                // Move order between lists
                activeOrders.removeAll { it.docId == delivered.docId }
                if (inactiveOrders.none { it.docId == delivered.docId }) {
                    inactiveOrders.add(delivered)
                }

                // Refresh current tab
                if (binding.tabLayout.selectedTabPosition == 0) showActive()
                else showInactive()
            }
        }
        binding.recyclerViewOrders.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewOrders.adapter = adapter

        // Tab setup: Active / Inactive
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Active"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Inactive"))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) showActive() else showInactive()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Load orders from Firestore
        loadOrders()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Navigate back to MainActivity
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.also(::startActivity)
        finish()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadOrders() {
        // Show loading spinner
        binding.progressBarLoading.visibility = View.VISIBLE
        binding.recyclerViewOrders.visibility = View.GONE
        binding.textViewNoActiveOrders.visibility = View.GONE
        binding.textViewNoInactiveOrders.visibility = View.GONE

        val userId = auth.currentUser?.uid ?: return
        firestore.collection("OrderList")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                // Hide loading
                binding.progressBarLoading.visibility = View.GONE

                allOrders.clear()
                activeOrders.clear()
                inactiveOrders.clear()

                snapshot.documents.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val items = (data["items"] as? List<Map<String, Any>>)
                        ?.map { m -> OrderItem(
                            id       = m["id"] as? String ?: "",
                            name     = m["name"] as? String ?: "",
                            price    = (m["price"] as? Number)?.toDouble() ?: 0.0,
                            quantity = (m["quantity"] as? Number)?.toInt()    ?: 0,
                            image    = m["image"] as? String ?: ""
                        ) } ?: emptyList()

                    val order = Order(
                        docId         = doc.id,
                        orderId       = data["orderId"]    as? String ?: "",
                        userId        = data["userId"]     as? String ?: "",
                        shopName      = data["shopName"]   as? String ?: "",
                        title         = data["title"]      as? String ?: "",
                        streetAddress = data["streetAddress"] as? String ?: "",
                        houseApartment= data["houseApartment"] as? String ?: "",
                        city          = data["city"]       as? String ?: "",
                        state         = data["state"]      as? String ?: "",
                        zipCode       = data["zipCode"]    as? String ?: "",
                        fullName      = data["fullName"]   as? String ?: "",
                        phoneNumber   = data["phoneNumber"] as? String ?: "",
                        subtotal      = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                        shipping      = (data["shipping"] as? Number)?.toDouble() ?: 0.0,
                        tax           = (data["tax"]      as? Number)?.toDouble() ?: 0.0,
                        total         = (data["total"]    as? Number)?.toDouble() ?: 0.0,
                        paymentId     = data["paymentId"] as? String,
                        status        = data["status"]    as? String ?: "",
                        timestamp     = (data["timestamp"] as? Number)?.toLong()   ?: 0L,
                        deliveryDate  = data["deliveryDate"] as? String ?: "",
                        items         = items
                    )

                    if (order.status.equals("Delivered", true) ||
                        order.status.equals("Cancelled", true)) {
                        inactiveOrders.add(order)
                    } else {
                        activeOrders.add(order)
                    }
                }

                showActive()
            }
            .addOnFailureListener {
                binding.progressBarLoading.visibility = View.GONE
                Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showActive() {
        adapter.clearExpanded()
        allOrders.clear()
        allOrders.addAll(activeOrders)
        adapter.notifyDataSetChanged()

        if (activeOrders.isEmpty()) {
            binding.recyclerViewOrders.visibility = View.GONE
            binding.textViewNoActiveOrders.visibility = View.VISIBLE
        } else {
            binding.recyclerViewOrders.visibility = View.VISIBLE
        }
        binding.textViewNoInactiveOrders.visibility = View.GONE
    }

    private fun showInactive() {
        adapter.clearExpanded()
        allOrders.clear()
        allOrders.addAll(inactiveOrders)
        adapter.notifyDataSetChanged()

        if (inactiveOrders.isEmpty()) {
            binding.recyclerViewOrders.visibility = View.GONE
            binding.textViewNoInactiveOrders.visibility = View.VISIBLE
        } else {
            binding.recyclerViewOrders.visibility = View.VISIBLE
        }
        binding.textViewNoActiveOrders.visibility = View.GONE
    }
}


//
//// app/src/main/java/com/meat/meatdash/orders/OrderActivity.kt
//package com.meat.meatdash.orders
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.os.Bundle
//import android.view.View
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.google.android.material.tabs.TabLayout
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.meat.meatdash.activity.MainActivity
//import com.meat.meatdash.adapter.OrderAdapter
//import com.meat.meatdash.databinding.ActivityOrderBinding
//import com.meat.meatdash.model.Order
//import com.meat.meatdash.model.OrderItem
//
//class OrderActivity : AppCompatActivity() {
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
//        adapter = OrderAdapter(allOrders) { delivered ->
//            // mark delivered in Firestore
//            firestore.collection("OrderList")
//                .document(delivered.docId)
//                .update("status", "Delivered")
//                .addOnFailureListener {
//                    Toast.makeText(this, "Failed to mark delivered", Toast.LENGTH_SHORT).show()
//                }
//
//            // move between lists
//            activeOrders.removeAll { it.docId == delivered.docId }
//            if (inactiveOrders.none { it.docId == delivered.docId }) {
//                inactiveOrders.add(delivered)
//            }
//
//            // refresh
//            if (binding.tabLayout.selectedTabPosition == 0) showActive()
//            else showInactive()
//        }
//
//        binding.recyclerViewOrders.layoutManager = LinearLayoutManager(this)
//        binding.recyclerViewOrders.adapter = adapter
//
//        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Active"))
//        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Inactive"))
//        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab) {
//                if (tab.position == 0) showActive() else showInactive()
//            }
//            override fun onTabUnselected(tab: TabLayout.Tab) {}
//            override fun onTabReselected(tab: TabLayout.Tab) {}
//        })
//
//        loadOrders()
//    }
//
//    override fun onBackPressed() {
//        super.onBackPressed()
//        Intent(this, MainActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//        }.also(::startActivity)
//        finish()
//    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    private fun loadOrders() {
//        binding.progressBarLoading.visibility = View.VISIBLE
//        binding.recyclerViewOrders.visibility = View.GONE
//        binding.textViewNoActiveOrders.visibility = View.GONE
//        binding.textViewNoInactiveOrders.visibility = View.GONE
//
//        val userId = auth.currentUser?.uid ?: return
//        firestore.collection("OrderList")
//            .whereEqualTo("userId", userId)
//            .get()
//            .addOnSuccessListener { snapshot ->
//                binding.progressBarLoading.visibility = View.GONE
//
//                allOrders.clear()
//                activeOrders.clear()
//                inactiveOrders.clear()
//
//                snapshot.documents.forEach { doc ->
//                    val data = doc.data ?: return@forEach
//                    val items = (data["items"] as? List<Map<String, Any>>)
//                        ?.map { m ->
//                            OrderItem(
//                                id       = m["id"] as? String ?: "",
//                                name     = m["name"] as? String ?: "",
//                                price    = (m["price"] as? Number)?.toDouble() ?: 0.0,
//                                quantity = (m["quantity"] as? Number)?.toInt()    ?: 0,
//                                image    = m["image"] as? String ?: ""
//                            )
//                        } ?: emptyList()
//
//                    val order = Order(
//                        docId         = doc.id,
//                        orderId       = data["orderId"]    as? String ?: "",
//                        userId        = data["userId"]     as? String ?: "",
//                        shopName      = data["shopName"]   as? String ?: "",
//                        title         = data["title"]      as? String ?: "",
//                        streetAddress = data["streetAddress"] as? String ?: "",
//                        houseApartment= data["houseApartment"] as? String ?: "",
//                        city          = data["city"]       as? String ?: "",
//                        state         = data["state"]      as? String ?: "",
//                        zipCode       = data["zipCode"]    as? String ?: "",
//                        fullName      = data["fullName"]   as? String ?: "",
//                        phoneNumber   = data["phoneNumber"] as? String ?: "",
//                        subtotal      = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
//                        shipping      = (data["shipping"] as? Number)?.toDouble() ?: 0.0,
//                        tax           = (data["tax"]      as? Number)?.toDouble() ?: 0.0,
//                        total         = (data["total"]    as? Number)?.toDouble() ?: 0.0,
//                        paymentId     = data["paymentId"] as? String,
//                        status        = data["status"]    as? String ?: "",
//                        timestamp     = (data["timestamp"] as? Number)?.toLong()   ?: 0L,
//                        deliveryDate  = data["deliveryDate"] as? String ?: "",
//                        shopLat       = (data["shopLat"]  as? Number)?.toDouble() ?: 0.0,
//                        shopLng       = (data["shopLng"]  as? Number)?.toDouble() ?: 0.0,
//                        items         = items
//                    )
//
//                    if (order.status.equals("Delivered", true) ||
//                        order.status.equals("Cancelled", true)) {
//                        inactiveOrders.add(order)
//                    } else {
//                        activeOrders.add(order)
//                    }
//                }
//
//                showActive()
//            }
//            .addOnFailureListener {
//                binding.progressBarLoading.visibility = View.GONE
//                Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    private fun showActive() {
////        adapter.clearExpanded()
//        allOrders.apply {
//            clear()
//            addAll(activeOrders)
//        }
//        adapter.notifyDataSetChanged()
//
//        binding.recyclerViewOrders.visibility = if (activeOrders.isEmpty()) {
//            binding.textViewNoActiveOrders.visibility = View.VISIBLE
//            View.GONE
//        } else {
//            binding.textViewNoActiveOrders.visibility = View.GONE
//            View.VISIBLE
//        }
//        binding.textViewNoInactiveOrders.visibility = View.GONE
//    }
//
//    private fun showInactive() {
////        adapter.clearExpanded()
//        allOrders.apply {
//            clear()
//            addAll(inactiveOrders)
//        }
//        adapter.notifyDataSetChanged()
//
//        binding.recyclerViewOrders.visibility = if (inactiveOrders.isEmpty()) {
//            binding.textViewNoInactiveOrders.visibility = View.VISIBLE
//            View.GONE
//        } else {
//            binding.textViewNoInactiveOrders.visibility = View.GONE
//            View.VISIBLE
//        }
//        binding.textViewNoActiveOrders.visibility = View.GONE
//    }
//}

//
//
//// app/src/main/java/com/meat/meatdash/orders/OrderActivity.kt
//package com.meat.meatdash.orders
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.os.Bundle
//import android.view.View
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.transition.AutoTransition
//import androidx.transition.TransitionManager
//import com.google.android.material.tabs.TabLayout
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.meat.meatdash.activity.MainActivity
//import com.meat.meatdash.adapter.OrderAdapter
//import com.meat.meatdash.databinding.ActivityOrderBinding
//import com.meat.meatdash.model.Order
//import com.meat.meatdash.model.OrderItem
//
//class OrderActivity : AppCompatActivity() {
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
//        adapter = OrderAdapter(allOrders) { delivered ->
//            firestore.collection("OrderList")
//                .document(delivered.docId)
//                .update("status", "Delivered")
//                .addOnFailureListener {
//                    Toast.makeText(this, "Failed to mark delivered", Toast.LENGTH_SHORT).show()
//                }
//
//            activeOrders.removeAll { it.docId == delivered.docId }
//            if (inactiveOrders.none { it.docId == delivered.docId }) {
//                inactiveOrders.add(delivered)
//            }
//
//            if (binding.tabLayout.selectedTabPosition == 0) showActive()
//            else showInactive()
//        }
//
//        binding.recyclerViewOrders.layoutManager = LinearLayoutManager(this)
//        binding.recyclerViewOrders.adapter = adapter
//
//        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Active"))
//        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Inactive"))
//        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab) {
//                if (tab.position == 0) showActive() else showInactive()
//            }
//            override fun onTabUnselected(tab: TabLayout.Tab) {}
//            override fun onTabReselected(tab: TabLayout.Tab) {}
//        })
//
//        loadOrders()
//    }
//
//    override fun onBackPressed() {
//        super.onBackPressed()
//        Intent(this, MainActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//        }.also(::startActivity)
//        finish()
//    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    private fun loadOrders() {
//        binding.progressBarLoading.visibility       = View.VISIBLE
//        binding.recyclerViewOrders.visibility      = View.GONE
//        binding.textViewNoActiveOrders.visibility   = View.GONE
//        binding.textViewNoInactiveOrders.visibility = View.GONE
//
//        val userId = auth.currentUser?.uid ?: return
//        firestore.collection("OrderList")
//            .whereEqualTo("userId", userId)
//            .get()
//            .addOnSuccessListener { snapshot ->
//                binding.progressBarLoading.visibility = View.GONE
//
//                allOrders.clear()
//                activeOrders.clear()
//                inactiveOrders.clear()
//
//                snapshot.documents.forEach { doc ->
//                    val data = doc.data ?: return@forEach
//                    val items = (data["items"] as? List<Map<String, Any>>)
//                        ?.map { m ->
//                            OrderItem(
//                                id       = m["id"] as? String ?: "",
//                                name     = m["name"] as? String ?: "",
//                                price    = (m["price"] as? Number)?.toDouble() ?: 0.0,
//                                quantity = (m["quantity"] as? Number)?.toInt()    ?: 0,
//                                image    = m["image"] as? String ?: ""
//                            )
//                        } ?: emptyList()
//
//                    val order = Order(
//                        docId         = doc.id,
//                        orderId       = data["orderId"]    as? String ?: "",
//                        userId        = data["userId"]     as? String ?: "",
//                        shopName      = data["shopName"]   as? String ?: "",
//                        title         = data["title"]      as? String ?: "",
//                        streetAddress = data["streetAddress"] as? String ?: "",
//                        houseApartment= data["houseApartment"] as? String ?: "",
//                        city          = data["city"]       as? String ?: "",
//                        state         = data["state"]      as? String ?: "",
//                        zipCode       = data["zipCode"]    as? String ?: "",
//                        fullName      = data["fullName"]   as? String ?: "",
//                        phoneNumber   = data["phoneNumber"] as? String ?: "",
//                        subtotal      = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
//                        shipping      = (data["shipping"] as? Number)?.toDouble() ?: 0.0,
//                        tax           = (data["tax"]      as? Number)?.toDouble() ?: 0.0,
//                        total         = (data["total"]    as? Number)?.toDouble() ?: 0.0,
//                        paymentId     = data["paymentId"] as? String,
//                        status        = data["status"]    as? String ?: "",
//                        timestamp     = (data["timestamp"] as? Number)?.toLong()   ?: 0L,
//                        deliveryDate  = data["deliveryDate"] as? String ?: "",
//                        shopLat       = (data["shopLat"]  as? Number)?.toDouble() ?: 0.0,
//                        shopLng       = (data["shopLng"]  as? Number)?.toDouble() ?: 0.0,
//                        items         = items
//                    )
//
//                    if (order.status.equals("Delivered", true) ||
//                        order.status.equals("Cancelled", true)
//                    ) {
//                        inactiveOrders.add(order)
//                    } else {
//                        activeOrders.add(order)
//                    }
//                }
//
//                showActive()
//            }
//            .addOnFailureListener {
//                binding.progressBarLoading.visibility = View.GONE
//                Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    private fun showActive() {
//        // animate open all
//        TransitionManager.beginDelayedTransition(
//            binding.recyclerViewOrders,
//            AutoTransition().apply { duration = 300 }
//        )
//        adapter.expandAll()
//
//        allOrders.clear()
//        allOrders.addAll(activeOrders)
//        adapter.notifyDataSetChanged()
//
//        if (activeOrders.isEmpty()) {
//            binding.recyclerViewOrders.visibility     = View.GONE
//            binding.textViewNoActiveOrders.visibility = View.VISIBLE
//        } else {
//            binding.recyclerViewOrders.visibility     = View.VISIBLE
//            binding.textViewNoActiveOrders.visibility = View.GONE
//        }
//        binding.textViewNoInactiveOrders.visibility = View.GONE
//    }
//
//    private fun showInactive() {
//        adapter.clearExpanded()
//
//        allOrders.clear()
//        allOrders.addAll(inactiveOrders)
//        adapter.notifyDataSetChanged()
//
//        if (inactiveOrders.isEmpty()) {
//            binding.recyclerViewOrders.visibility       = View.GONE
//            binding.textViewNoInactiveOrders.visibility = View.VISIBLE
//        } else {
//            binding.recyclerViewOrders.visibility       = View.VISIBLE
//            binding.textViewNoInactiveOrders.visibility = View.GONE
//        }
//        binding.textViewNoActiveOrders.visibility = View.GONE
//    }
//}
