package com.meat.meatdash

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.google.firebase.firestore.FirebaseFirestore
import com.meat.meatdash.adapter.ShopAdapter
import com.meat.meatdash.databinding.ActivityMainBinding
import com.meat.meatdash.model.Shop

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupImageSlider()
        setupRecyclerView()
    }

    private fun setupImageSlider() {
        val imageList = arrayListOf(
            SlideModel(R.drawable.img1, ScaleTypes.FIT),
            SlideModel(R.drawable.img2, ScaleTypes.FIT),
            SlideModel(R.drawable.img3, ScaleTypes.FIT),
            SlideModel(R.drawable.img4, ScaleTypes.FIT),
            SlideModel(R.drawable.img5, ScaleTypes.FIT)
        )
        binding.imageSlider.setImageList(imageList)
    }

    private fun setupRecyclerView() {
        binding.shopRecyclerView.layoutManager = GridLayoutManager(this, 2)

        db.collection("ShopDetails") // retrieve  data
            .get()
            .addOnSuccessListener { result ->
                val shopList = result.map { doc ->
                    doc.toObject(Shop::class.java).copy(id = doc.id)
                }

                val adapter = ShopAdapter(shopList) { shop ->
                    Intent(this, ShopDetailActivity::class.java).apply {
                        putExtra("shop_id", shop.id)
                        putExtra("shopName", shop.shopName)
                        putExtra("shopDescription", shop.shopDescription ?: "No description") // Uncommented
                        putExtra("shopRating", shop.rating)
                        putExtra("shopReviewCount", shop.reviewCount)
                        putExtra("shopCategory", shop.category)
                        putExtra("shopLocation", shop.shopLocation)
//                        putExtra("shopImageBase64", shop.imageBase64)
                        startActivity(this)
                    }
                }
                binding.shopRecyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading shops: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

        // click on cart

        binding.cartButton.setOnClickListener {
            val intent = Intent(this,CartActivity::class.java)
            startActivity(intent)
        }
    }
}