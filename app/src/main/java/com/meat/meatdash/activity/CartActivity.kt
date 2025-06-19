
// CartActivity.kt
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

        // 1) Init CartManager
        CartManager.init(this)

        // 2) Setup RecyclerView & adapter
        setupRecyclerView()

        // 3) Hook up Back & Checkout buttons
        setupButtonListeners()

        // 4) Listen for ANY cart change (add/remove/update/clear)
        CartManager.setCartUpdateListener { _ ->
            adapter.updateItems(CartManager.getCartItems())
            updateTotalPrice()
            checkEmptyCart()
        }

        // 5) Kick off initial load
        loadCartItems()
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(
            CartManager.getCartItems().toMutableList(),
            onQuantityChanged = { updateTotalPrice() },
            onItemRemoved     = {
                updateTotalPrice()
                checkEmptyCart()
            }
        )

        binding.cartRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter       = this@CartActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupButtonListeners() {
        binding.backButton.setOnClickListener { onBackPressed() }

        binding.btnCheckout.setOnClickListener {
            val items = CartManager.getCartItems()
            // find all positions where weight < 100 g
            val invalid = items
                .mapIndexedNotNull { idx, item -> idx.takeIf { item.weight < 100 } }
                .toSet()

            if (invalid.isNotEmpty()) {
                // flag errors & scroll to first
                adapter.setInvalidWeights(invalid)
                binding.cartRecyclerView.scrollToPosition(invalid.minOrNull()!!)
                return@setOnClickListener
            }

            // clear any stale errors and proceed
            adapter.setInvalidWeights(emptySet())
            startActivity(Intent(this, CheckoutActivity::class.java))
        }
    }

    private fun loadCartItems() {
        binding.progressBar.visibility = View.VISIBLE
        CartManager.loadCartItems { success ->
            binding.progressBar.visibility = View.GONE
            if (success) {
                adapter.updateItems(CartManager.getCartItems())
                updateTotalPrice()
                checkEmptyCart()
            }
        }
    }

    private fun updateTotalPrice() {
        val total = CartManager.getCartItems()
            .sumOf { it.price * (it.weight / 1000.0) }
        binding.tvTotalPrice.text = "Total: ₹%.2f".format(total)
    }

    private fun checkEmptyCart() {
        val empty = CartManager.getCartItems().isEmpty()
        binding.cartRecyclerView.visibility  = if (empty) View.GONE else View.VISIBLE
        binding.emptyCartImage.visibility    = if (empty) View.VISIBLE else View.GONE
        binding.tvTotalPrice.visibility      = if (empty) View.GONE else View.VISIBLE
        binding.btnCheckout.visibility       = if (empty) View.GONE else View.VISIBLE
    }
}

