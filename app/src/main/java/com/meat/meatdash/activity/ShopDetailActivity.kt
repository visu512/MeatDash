//package com.meat.meatdash.activity
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.graphics.BitmapFactory
//import android.graphics.Color
//import android.os.Bundle
//import android.util.Base64
//import android.view.View
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.GridLayoutManager
//import com.google.firebase.firestore.FirebaseFirestore
//import com.meat.meatdash.CartManager
//import com.meat.meatdash.R
//import com.meat.meatdash.adapter.PopularFoodAdapter
//import com.meat.meatdash.databinding.ActivityShopDetailBinding
//import com.meat.meatdash.model.FoodItem
//
//class ShopDetailActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityShopDetailBinding
//    private val db = FirebaseFirestore.getInstance()
//    private lateinit var shopId: String
//
//    private val SHARED_PREFS_NAME = "MeatDashPrefs"
//    private val KEY_SHOP_LOCATION = "SHOP_LOCATION"
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityShopDetailBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        shopId = intent.getStringExtra("shopRegId") ?: run {
//            Toast.makeText(this, "Invalid shop", Toast.LENGTH_SHORT).show()
//            finish()
//            return
//        }
//
//        binding.backButton.setOnClickListener { onBackPressed() }
//        binding.popularItemsRecyclerView.layoutManager = GridLayoutManager(this, 1)
//
//        // cardview color always white both mode nigh/light
//        binding.cardView.setCardBackgroundColor(Color.WHITE)
//
//        // Bottom sheet initially hidden
//        binding.bottomBar.root.visibility = View.GONE
//        binding.bottomBar.btnViewCart.setOnClickListener {
//            startActivity(Intent(this, CartActivity::class.java))
//        }
//
//        loadShopDetails(shopId)
//    }
//
//    private fun loadShopDetails(shopId: String) {
//        db.collection("Shops").document(shopId)
//            .get()
//            .addOnSuccessListener { doc ->
//                if (!doc.exists()) {
//                    Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
//                    finish(); return@addOnSuccessListener
//                }
//                binding.shopName.text = doc.getString("shopName") ?: ""
//                binding.shopDescription.text = doc.getString("shopDescription") ?: ""
//                binding.shopRating.text = "%.1f".format(doc.getDouble("rating") ?: 0.0)
//                binding.shopLocation.text = doc.getString("shopLocation") ?: ""
//
//                doc.getString("imageBase64")?.let { base64 ->
//                    try {
//                        val bytes = Base64.decode(base64, Base64.DEFAULT)
//                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//                        binding.shopImage.setImageBitmap(bmp)
//                    } catch (_: Exception) {
//                        binding.shopImage.setImageResource(R.drawable.placeholder)
//                    }
//                }
//
//                loadProducts(shopId)
//            }
//            .addOnFailureListener {
//                Toast.makeText(this, "Error loading shop: ${it.message}", Toast.LENGTH_SHORT).show()
//                finish()
//            }
//    }
//
//    private fun loadProducts(shopId: String) {
//        binding.progressBar.visibility = View.VISIBLE
//        binding.popularItemsRecyclerView.visibility = View.GONE
//        binding.emptyTextView.visibility = View.GONE
//
//        db.collection("Shops")
//            .document(shopId)
//            .collection("products_name")
//            .get()
//            .addOnSuccessListener { result ->
//                binding.progressBar.visibility = View.GONE
//                val items = result.map { doc ->
//                    FoodItem(
//                        id = doc.id,
//                        name = doc.getString("name") ?: "",
//                        price = (doc.get("price") as? Number)?.toInt() ?: 0,
//                        description = doc.getString("description") ?: "",
//                        imageBase64 = doc.getString("imageBase64") ?: "",
//                        shopId = shopId,
//                        shopName = binding.shopName.text.toString()
//                    )
//                }
//
//                if (items.isEmpty()) {
//                    binding.emptyTextView.visibility = View.VISIBLE
//                } else {
//                    val adapter = PopularFoodAdapter(
//                        items,
//                        onItemClick = { food ->
//                            // open detail screen if needed
//                        },
//                        onCartUpdated = { updateBottomCartBar() }
//                    )
//                    binding.popularItemsRecyclerView.adapter = adapter
//                    binding.popularItemsRecyclerView.visibility = View.VISIBLE
//                }
//            }
//            .addOnFailureListener { e ->
//                binding.progressBar.visibility = View.GONE
//                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                binding.emptyTextView.visibility = View.VISIBLE
//            }
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun updateBottomCartBar() {
//        val count = CartManager.getCartItems().size
//        if (count > 0) {
//            binding.bottomBar.root.visibility = View.VISIBLE
//            binding.bottomBar.tvItemCount.text =
//                "$count Item${if (count > 1) "s" else ""}"
//        } else {
//            binding.bottomBar.root.visibility = View.GONE
//        }
//    }
//}


// ShopDetailActivity.kt
package com.meat.meatdash.activity

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.meat.meatdash.R
import com.meat.meatdash.activity.CartActivity
import com.meat.meatdash.adapter.PopularFoodAdapter
import com.meat.meatdash.databinding.ActivityShopDetailBinding
import com.meat.meatdash.model.FoodItem

class ShopDetailActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME         = "MeatDashPrefs"
        private const val KEY_SHOP_LOCATION  = "SHOP_LOCATION"
    }

    private lateinit var binding: ActivityShopDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var shopId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Read shopId from Intent
        shopId = intent.getStringExtra("shopRegId") ?: run {
            Toast.makeText(this, "Invalid shop", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2) Back button
        binding.backButton.setOnClickListener { onBackPressed() }

        // 3) RecyclerView layout
        binding.popularItemsRecyclerView.layoutManager = GridLayoutManager(this, 1)

        // 4) Always white card
        binding.cardView.setCardBackgroundColor(Color.WHITE)

        // 5) Bottom-bar setup
        binding.bottomBar.root.visibility = View.GONE
        binding.bottomBar.btnViewCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // 6) Load shop details
        loadShopDetails(shopId)
    }

    private fun loadShopDetails(shopId: String) {
        db.collection("Shops").document(shopId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val shopName     = doc.getString("shopName")        ?: ""
                val shopDesc     = doc.getString("shopDescription") ?: ""
                val rating       = doc.getDouble("rating")          ?: 0.0
                val shopLocation = doc.getString("shopLocation")    ?: ""

                // --- Update UI ---
                binding.shopName.text        = shopName
                binding.shopDescription.text = shopDesc
                binding.shopRating.text      = "%.1f".format(rating)
                binding.shopLocation.text    = shopLocation

                // --- Persist for checkout ---
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SHOP_LOCATION, shopLocation)
                    .apply()

                // Load image if present
                doc.getString("imageBase64")?.takeIf { it.isNotEmpty() }?.let { base64 ->
                    try {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        val bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        binding.shopImage.setImageBitmap(bmp)
                    } catch (_: Exception) {
                        binding.shopImage.setImageResource(R.drawable.placeholder)
                    }
                } ?: binding.shopImage.setImageResource(R.drawable.placeholder)

                // Now load the shop’s products
                loadProducts(shopId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading shop: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadProducts(shopId: String) {
        binding.progressBar.visibility              = View.VISIBLE
        binding.popularItemsRecyclerView.visibility = View.GONE
        binding.emptyTextView.visibility            = View.GONE

        db.collection("Shops")
            .document(shopId)
            .collection("products_name")
            .get()
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE

                val items = result.map { doc ->
                    FoodItem(
                        id           = doc.id,
                        name         = doc.getString("name") ?: "",
                        price        = (doc.get("price") as? Number)?.toInt() ?: 0,
                        description  = doc.getString("description") ?: "",
                        imageBase64  = doc.getString("imageBase64") ?: "",
                        shopId       = shopId,
                        shopName     = binding.shopName.text.toString(),
                        shopLocation = binding.shopLocation.text.toString()
                    )
                }

                if (items.isEmpty()) {
                    binding.emptyTextView.visibility = View.VISIBLE
                } else {
                    binding.popularItemsRecyclerView.adapter =
                        PopularFoodAdapter(
                            items,
                            onItemClick   = { /* … */ },
                            onCartUpdated = { updateBottomCartBar() }
                        )
                    binding.popularItemsRecyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility   = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.emptyTextView.visibility = View.VISIBLE
            }
    }

    private fun updateBottomCartBar() {
        val count = com.meat.meatdash.CartManager.getCartItems().size
        if (count > 0) {
            binding.bottomBar.root.visibility = View.VISIBLE
            binding.bottomBar.tvItemCount.text =
                "$count Item${if (count > 1) "s" else ""}"
        } else {
            binding.bottomBar.root.visibility = View.GONE
        }
    }
}
