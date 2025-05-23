package com.meat.meatdash.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.google.firebase.firestore.FirebaseFirestore
import com.meat.meatdash.R
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
        val progressBar = binding.progressBar
        val emptyText = binding.emptyTextView
        val recyclerView = binding.shopRecyclerView

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Show progress while loading
        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        recyclerView.visibility = View.GONE

        db.collection("Shops")
            .get()
            .addOnSuccessListener { result ->
                val shopList = result.map { doc ->
                    doc.toObject(Shop::class.java).copy(id = doc.id)
                }

                progressBar.visibility = View.GONE

                if (shopList.isNotEmpty()) {
                    recyclerView.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE

                    val adapter = ShopAdapter(shopList) { shop ->
                        Intent(this, ShopDetailActivity::class.java).apply {
                            putExtra("shopRegId", shop.id)
                            putExtra("shopName", shop.shopName)
                            putExtra("shopDescription", shop.shopDescription ?: "No description")
                            putExtra("shopRating", shop.rating)
                            putExtra("shopReviewCount", shop.reviewCount)
                            putExtra("shopCategory", shop.category)
                            putExtra("shopLocation", shop.shopLocation)
                            startActivity(this)
                        }
                    }
                    recyclerView.adapter = adapter
                } else {
                    emptyText.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                emptyText.text = "Error loading shops: ${exception.message}"
            }

        // Cart click
        binding.cartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
    }

}