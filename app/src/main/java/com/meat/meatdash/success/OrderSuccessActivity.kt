//package com.meat.meatdash.success
//
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.os.Bundle
//import androidx.appcompat.app.AppCompatActivity
//import com.meat.meatdash.R
//import com.meat.meatdash.activity.MainActivity
//import com.meat.meatdash.databinding.ActivityOrderSuccessBinding
//import com.meat.meatdash.model.Address
//import com.meat.meatdash.orders.OrderActivity
//
//class OrderSuccessActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityOrderSuccessBinding
//    private val prefs by lazy { getSharedPreferences("meatdash_user_prefs", Context.MODE_PRIVATE) }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityOrderSuccessBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        setupUI()
//        handleIntentData()
//        setupClickListeners()
//    }
//
//    private fun setupUI() {
//        // Set card background to white
//        binding.cardView.setCardBackgroundColor(Color.WHITE)
//
//        // Set delivery estimate message
//        binding.deliveryDateText.text = getString(R.string.delivery_estimate, 30)
//    }
//
//    private fun handleIntentData() {
//        // Retrieve Order ID
//        val orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
//        binding.orderIdText.text = getString(R.string.order_id_format, orderId)
//
//        // Display and save customer address
//        (intent.getSerializableExtra(EXTRA_ADDRESS) as? Address)?.let { address ->
//            saveUserLocation(address)
//            displayAddress(address)
//        }
//    }
//
//    private fun saveUserLocation(address: Address) {
//        prefs.edit().apply {
//            putString(PREF_USER_CITY, address.city)
//            putString(PREF_USER_STATE, address.state)
//            apply()
//        }
//    }
//
//    private fun displayAddress(address: Address) {
//        binding.deliveryAddressText.text = buildString {
//            if (address.fullName.isNotEmpty()) append("${address.fullName}\n")
//            if (address.phoneNumber.isNotEmpty()) append("${address.phoneNumber}\n")
//            append(address.street)
//            if (address.houseApartment.isNotEmpty()) append(", ${address.houseApartment}")
//            append("\n${address.city}, ${address.state} ${address.zipCode}")
//        }
//    }
//
//    private fun setupClickListeners() {
//        // Continue shopping -> MainActivity
//        binding.btnContinueShopping.setOnClickListener {
//            navigateToMainActivity()
//        }
//
//        // Track order -> OrderActivity
//        binding.btnTrackOrder.setOnClickListener {
//            startActivity(Intent(this, OrderActivity::class.java))
//        }
//    }
//
//    private fun navigateToMainActivity() {
//        Intent(this, MainActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//            startActivity(this)
//        }
//        finish()
//    }
//
//    // Redirect back to home rather than previous screen
//    override fun onBackPressed() {
//        super.onBackPressed()
//        navigateToMainActivity()
//    }
//
//    companion object {
//        const val EXTRA_ORDER_ID = "orderId"
//        const val EXTRA_ADDRESS = "address"
//        private const val PREF_USER_CITY = "user_city"
//        private const val PREF_USER_STATE = "user_state"
//
//        fun start(context: Context, orderId: String, address: Address) {
//            Intent(context, OrderSuccessActivity::class.java).apply {
//                putExtra(EXTRA_ORDER_ID, orderId)
//                putExtra(EXTRA_ADDRESS, address)
//                context.startActivity(this)
//            }
//        }
//    }
//}


package com.meat.meatdash.success

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.meat.meatdash.R
import com.meat.meatdash.activity.MainActivity
import com.meat.meatdash.databinding.ActivityOrderSuccessBinding
import com.meat.meatdash.model.Address
import com.meat.meatdash.orders.OrderActivity

class OrderSuccessActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderSuccessBinding
    private val prefs by lazy {
        getSharedPreferences("meatdash_user_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        displayIntentData()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.cardView.setCardBackgroundColor(Color.WHITE)
//        binding.deliveryDateText.text = getString(R.string.delivery_estimate, 30)
    }

    @Suppress("SetTextI18n")
    private fun displayIntentData() {
        // Order ID
        val orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        binding.orderIdText.text = getString(R.string.order_id_format, orderId)

        // Shop Registration ID
        val shopRegId = intent.getStringExtra(EXTRA_SHOP_REG_ID) ?: ""
        val shopLocKey = "shop_location_$shopRegId"
        val shopLoc = prefs.getString(shopLocKey, null)
        if (!shopLoc.isNullOrBlank()) {
//            binding.shopLocationText.text = getString(R.string.shop_location_format, shopLoc)
//            binding.shopLocationText.visibility = View.VISIBLE
        }

        // Customer Address
        (intent.getSerializableExtra(EXTRA_ADDRESS) as? Address)?.let { address ->
            prefs.edit().apply {
                putString(PREF_USER_CITY, address.city)
                putString(PREF_USER_STATE, address.state)
                apply()
            }
            binding.deliveryAddressText.text = with(address) {
                buildString {
                    if (fullName.isNotEmpty()) append("$fullName\n")
                    if (phoneNumber.isNotEmpty()) append("$phoneNumber\n")
                    append(street)
                    if (houseApartment.isNotEmpty()) append(", $houseApartment")
                    append("\n$city, $state $zipCode")
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnContinueShopping.setOnClickListener { navigateToMain() }
        binding.btnTrackOrder.setOnClickListener {
            startActivity(Intent(this, OrderActivity::class.java))
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    override fun onBackPressed() {
        navigateToMain()
    }

    companion object {
        const val EXTRA_ORDER_ID = "orderId"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_SHOP_REG_ID = "shopRegId"

        private const val PREF_USER_CITY = "user_city"
        private const val PREF_USER_STATE = "user_state"

        fun start(
            context: Context,
            orderId: String,
            address: Address,
            shopRegId: String
        ) {
            Intent(context, OrderSuccessActivity::class.java).apply {
                putExtra(EXTRA_ORDER_ID, orderId)
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_SHOP_REG_ID, shopRegId)
                context.startActivity(this)
            }
        }
    }
}
