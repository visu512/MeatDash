//package com.meat.meatdash
//
//import android.os.Bundle
//import android.view.View
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.google.android.material.button.MaterialButton
//import com.meat.meatdash.adapter.CartAdapter
//import com.meat.meatdash.model.CartManager
//
//class CartActivity : AppCompatActivity() {
//
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var adapter: CartAdapter
//    private lateinit var tvTotalPrice: TextView
//    private lateinit var btnCheckout: MaterialButton
//    private lateinit var btnContinueShopping: MaterialButton
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_cart)
//
//        initViews()
//        setupRecyclerView()
//        updateTotalPrice()
//        setupButtonListeners()
//    }
//
//    private fun initViews() {
//        recyclerView = findViewById(R.id.cartRecyclerView)
//        tvTotalPrice = findViewById(R.id.tvTotalPrice)
//        btnCheckout = findViewById(R.id.btnCheckout)
//        btnContinueShopping = findViewById(R.id.btnContinueShopping)
//    }
//
//    private fun setupRecyclerView() {
//        adapter = CartAdapter(CartManager.getCartItems().toMutableList()) {
//            updateTotalPrice()
//        }
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = adapter
//    }
//
//    private fun updateTotalPrice() {
//        val total = CartManager.getTotalPrice()
//        tvTotalPrice.text = "Total: ₹%.2f".format(total)
//
//
//        val isCartEmpty = CartManager.getCartItems().isEmpty()
//        recyclerView.visibility = if (isCartEmpty) View.GONE else View.VISIBLE
//        emptyCartImage.visibility = if (isCartEmpty) View.VISIBLE else View.GONE
//    }
//
//    private fun setupButtonListeners() {
////        btnCheckout.setOnClickListener {
////            if (CartManager.getCartItems().isNotEmpty()) {
////                Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show()
////                CartManager.clearCart()
////                finish()
////            } else {
////                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
////            }
////        }
//
//        btnContinueShopping.setOnClickListener {
//            finish()
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        adapter.updateItems(CartManager.getCartItems())
//        updateTotalPrice()
//    }
//}


package com.meat.meatdash

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.meat.meatdash.adapter.CartAdapter
import com.meat.meatdash.databinding.ActivityCartBinding
import com.meat.meatdash.model.CartManager

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        updateTotalPrice()
        setupButtonListeners()
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(CartManager.getCartItems().toMutableList()) {
            updateTotalPrice()
        }
        binding.cartRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.cartRecyclerView.adapter = adapter
    }

    private fun updateTotalPrice() {
        val total = CartManager.getTotalPrice()
        binding.tvTotalPrice.text = "Total: ₹%.2f".format(total)

        val isCartEmpty = CartManager.getCartItems().isEmpty()
        binding.cartRecyclerView.visibility = if (isCartEmpty) View.GONE else View.VISIBLE
        binding.emptyCartImage.visibility = if (isCartEmpty) View.VISIBLE else View.GONE
    }

    private fun setupButtonListeners() {
        binding.btnCheckout.setOnClickListener {
            if (CartManager.getCartItems().isNotEmpty()) {
                Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                CartManager.clearCart()
                adapter.updateItems(emptyList())
                updateTotalPrice()
            } else {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnContinueShopping.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.updateItems(CartManager.getCartItems())
        updateTotalPrice()
    }
}
