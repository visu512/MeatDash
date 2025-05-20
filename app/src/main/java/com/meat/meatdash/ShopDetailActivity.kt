package com.meat.meatdash

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.meat.meatdash.adapter.PopularFoodAdapter
import com.meat.meatdash.databinding.ActivityShopDetailBinding
import com.meat.meatdash.model.CartManager
import com.meat.meatdash.model.FoodItem

class ShopDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShopDetailBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val shopId = intent.getStringExtra("shop_id") ?: run {
            Toast.makeText(this, "Invalid shop", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.backButton.setOnClickListener { onBackPressed() }
        binding.callButton.setOnClickListener { handleCallButton() }
        binding.directionButton.setOnClickListener { handleDirectionsButton() }

        setupRecyclerView()
        loadShopDetails(shopId)
    }

    private fun setupRecyclerView() {
        binding.popularItemsRecyclerView.layoutManager = GridLayoutManager(this, 2)
    }

    private fun loadShopDetails(shopId: String) {
        db.collection("ShopDetails").document(shopId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.shopName.text = document.getString("shopName")
//                    binding.shopDescription.text = document.getString("shopDescription")
                    binding.shopRating.text = "%.1f".format(document.getDouble("rating") ?: 0.0)
                    binding.shopReviewCount.text =
                        "(${document.getLong("reviewCount") ?: 0} reviews)"
                    binding.shopLocation.text = document.getString("shopLocation")

                    document.getString("imageBase64")?.let { loadShopImage(it) }

                    loadProducts(shopId)
                } else {
                    Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load shop details", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadShopImage(imageBase64: String) {
        try {
            val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.shopImage.setImageBitmap(bitmap)
        } catch (e: Exception) {
            binding.shopImage.setImageResource(R.drawable.placeholder)
            Log.e("ShopDetail", "Image load error", e)
        }
    }

    private fun loadProducts(shopId: String) {
        db.collection("products")
            .get()
            .addOnSuccessListener { result ->
                val foodItems = result.map { doc ->
                    val price = (doc.get("price") as? Number)?.toInt() ?: 0

                    FoodItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = price,
                        description = doc.getString("description") ?: "",
                        imageBase64 = doc.getString("imageBase64") ?: "",
                        shopId = doc.getString("shopId") ?: ""
                    )

                }

                val adapter = PopularFoodAdapter(foodItems) { foodItem ->
                    CartManager.addItem(foodItem)
                    val intent = Intent(this, CartActivity::class.java)
                    startActivity(intent)
                }

                binding.popularItemsRecyclerView.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading products: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun handleCallButton() {
        // Implement call functionality
    }

    private fun handleDirectionsButton() {
        // Implement directions functionality
    }
}
