package com.meat.meatdash.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.meat.meatdash.CartManager
import com.meat.meatdash.adapter.CartAdapter
import com.meat.meatdash.checkout.CheckoutActivity
import com.meat.meatdash.databinding.ActivityCartBinding

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CartManager.init(this)
        loadCartItems()
    }

    private fun loadCartItems() {
        binding.progressBar.visibility = View.VISIBLE
        CartManager.loadCartItems {
            binding.progressBar.visibility = View.GONE
            if (it) {
                setupRecyclerView()
                updateTotalPrice()
                setupButtonListeners()
                checkEmptyCart()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(
            CartManager.getCartItems().toMutableList(),
            onQuantityChanged = { updateTotalPrice() },
            onItemRemoved = {
                updateTotalPrice()
                checkEmptyCart()
            }
        )
        binding.cartRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = this@CartActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun updateTotalPrice() {
        val total = CartManager.getCartItems().sumOf { item ->
            val weightInKg = if (item.weight >= 1000) item.weight / 1000.0 else item.weight / 1000.0
            item.price * weightInKg
        }
        binding.tvTotalPrice.text = "Total: ₹%.2f".format(total)
    }

    private fun checkEmptyCart() {
        val isCartEmpty = CartManager.getCartItems().isEmpty()
        binding.cartRecyclerView.visibility = if (isCartEmpty) View.GONE else View.VISIBLE
        binding.emptyCartImage.visibility = if (isCartEmpty) View.VISIBLE else View.GONE
        binding.tvTotalPrice.visibility = if (isCartEmpty) View.GONE else View.VISIBLE
        binding.btnCheckout.visibility = if (isCartEmpty) View.GONE else View.VISIBLE
    }

    private fun setupButtonListeners() {
        binding.backButton.setOnClickListener { onBackPressed() }

        // add details
        binding.btnCheckout.setOnClickListener {
            if (CartManager.getCartItems().isNotEmpty()) {
                startActivity(Intent(this, CheckoutActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadCartItems()
    }
}




