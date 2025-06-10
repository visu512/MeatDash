package com.meat.meatdash.success

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.meat.meatdash.activity.MainActivity
import com.meat.meatdash.databinding.ActivityOrderSuccessBinding
import com.meat.meatdash.model.Address
import com.meat.meatdash.orders.OrderActivity

class OrderSuccessActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // cardview always white
        binding.cardView.setCardBackgroundColor(Color.WHITE)

        // Retrieve Order ID
        val orderId = intent.getStringExtra("orderId") ?: ""
        binding.orderIdText.text = "Order ID: $orderId"

        // Show delivery timing message
        binding.deliveryDateText.text = "Your order will be delivered in 30 mins"

        // Display customer address passed via Intent
        val address = intent.getSerializableExtra("address") as? Address
        address?.let {
            binding.deliveryAddressText.text = with(it) {
                buildString {
                    if (fullName.isNotEmpty()) append("$fullName\n")
                    if (phoneNumber.isNotEmpty()) append("$phoneNumber\n")
                    append(street)
                    if (houseApartment.isNotEmpty()) append(", $houseApartment")
                    append("\n$city, $state $zipCode")
                }
            }
        }

        // Continue shopping -> MainActivity
        binding.btnContinueShopping.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // Track order -> OrderActivity
        binding.btnTrackOrder.setOnClickListener {
            val intent = Intent(this, OrderActivity::class.java)
            startActivity(intent)
        }
    }

    // Redirect back to home rather than previous screen

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
