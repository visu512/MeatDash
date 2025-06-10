package com.meat.meatdash.activity

import android.annotation.SuppressLint
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
import com.meat.meatdash.CartManager
import com.meat.meatdash.R
import com.meat.meatdash.adapter.PopularFoodAdapter
import com.meat.meatdash.databinding.ActivityShopDetailBinding
import com.meat.meatdash.model.FoodItem

class ShopDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShopDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var shopId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("shopRegId") ?: run {
            Toast.makeText(this, "Invalid shop", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.backButton.setOnClickListener { onBackPressed() }
        binding.popularItemsRecyclerView.layoutManager = GridLayoutManager(this, 1)

        // cardview color always white both mode nigh/light
        binding.cardView.setCardBackgroundColor(Color.WHITE)

        // Bottom sheet initially hidden
        binding.bottomBar.root.visibility = View.GONE
        binding.bottomBar.btnViewCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        loadShopDetails(shopId)
    }

    private fun loadShopDetails(shopId: String) {
        db.collection("Shops").document(shopId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
                    finish(); return@addOnSuccessListener
                }
                binding.shopName.text = doc.getString("shopName") ?: ""
                binding.shopDescription.text = doc.getString("shopDescription") ?: ""
                binding.shopRating.text = "%.1f".format(doc.getDouble("rating") ?: 0.0)
                binding.shopLocation.text = doc.getString("shopLocation") ?: ""

                doc.getString("imageBase64")?.let { base64 ->
                    try {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        binding.shopImage.setImageBitmap(bmp)
                    } catch (_: Exception) {
                        binding.shopImage.setImageResource(R.drawable.placeholder)
                    }
                }

                loadProducts(shopId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading shop: ${it.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadProducts(shopId: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.popularItemsRecyclerView.visibility = View.GONE
        binding.emptyTextView.visibility = View.GONE

        db.collection("Shops")
            .document(shopId)
            .collection("products_name")
            .get()
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE
                val items = result.map { doc ->
                    FoodItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = (doc.get("price") as? Number)?.toInt() ?: 0,
                        description = doc.getString("description") ?: "",
                        imageBase64 = doc.getString("imageBase64") ?: "",
                        shopId = shopId,
                        shopName = binding.shopName.text.toString()
                    )
                }

                if (items.isEmpty()) {
                    binding.emptyTextView.visibility = View.VISIBLE
                } else {
                    val adapter = PopularFoodAdapter(
                        items,
                        onItemClick = { food ->
                            // open detail screen if needed
                        },
                        onCartUpdated = { updateBottomCartBar() }
                    )
                    binding.popularItemsRecyclerView.adapter = adapter
                    binding.popularItemsRecyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.emptyTextView.visibility = View.VISIBLE
            }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBottomCartBar() {
        val count = CartManager.getCartItems().size
        if (count > 0) {
            binding.bottomBar.root.visibility = View.VISIBLE
            binding.bottomBar.tvItemCount.text =
                "$count Item${if (count > 1) "s" else ""}"
        } else {
            binding.bottomBar.root.visibility = View.GONE
        }
    }
}
