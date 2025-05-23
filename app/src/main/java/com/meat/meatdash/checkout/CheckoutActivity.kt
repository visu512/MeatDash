package com.meat.meatdash.checkout

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.meat.meatdash.adapter.CheckoutItemsAdapter
import com.meat.meatdash.confirm.ConfirmOrderActivity
import com.meat.meatdash.databinding.ActivityCheckoutBinding
import com.meat.meatdash.model.CartManager
import com.meat.meatdash.model.DeliveryAddress
import com.meat.meatdash.model.FoodItem

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: CheckoutItemsAdapter
    private lateinit var itemList: List<FoodItem>
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemList = CartManager.getCartItems()
        adapter = CheckoutItemsAdapter(itemList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Placing your order...")
        progressDialog.setCancelable(false)

        // Back button
        binding.backButton.setOnClickListener { onBackPressed() }

        // Load address from SharedPreferences
        loadSavedAddress()

        // Button click
        binding.btnPlaceOrder.setOnClickListener {
            if (validateAndSaveDeliveryAddress()) {
                progressDialog.show()
            }
        }

        // Show subtotal on checkout screen
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val subtotal = itemList.sumOf { it.price * it.weight / 1000.0 }
        binding.tvTotalPrice.text = "Total: ₹%.2f".format(subtotal)
    }

    private fun loadSavedAddress() {
        val prefs = getSharedPreferences("checkout_address", MODE_PRIVATE)
        binding.etFullName.setText(prefs.getString("fullName", ""))
        binding.etPhone.setText(prefs.getString("phone", ""))
        binding.etStreet.setText(prefs.getString("street", ""))
        binding.etState.setText(prefs.getString("state", ""))
        binding.etCity.setText(prefs.getString("city", ""))
        binding.etPinCode.setText(prefs.getString("pinCode", ""))
    }

    private fun validateAndSaveDeliveryAddress(): Boolean {
        val fullName = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val street = binding.etStreet.text.toString().trim()
        val state = binding.etState.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val pinCode = binding.etPinCode.text.toString().trim()

        var isValid = true

        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full name is required"
            isValid = false
        }

        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone number is required"
            isValid = false
        } else if (!phone.matches(Regex("^\\d{10}$"))) {
            binding.etPhone.error = "Phone number must be 10 digits"
            isValid = false
        }

        if (street.isEmpty()) {
            binding.etStreet.error = "Street is required"
            isValid = false
        }

        if (state.isEmpty()) {
            binding.etState.error = "State is required"
            isValid = false
        }

        if (city.isEmpty()) {
            binding.etCity.error = "City is required"
            isValid = false
        }

        if (pinCode.isEmpty()) {
            binding.etPinCode.error = "PIN code is required"
            isValid = false
        }

        if (!isValid) return false

        // Save to SharedPreferences
        val sharedPrefs = getSharedPreferences("checkout_address", MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("fullName", fullName)
            putString("phone", phone)
            putString("street", street)
            putString("state", state)
            putString("city", city)
            putString("pinCode", pinCode)
            apply()
        }

        // Save to Firestore
        val address = DeliveryAddress(fullName, phone, street, city, state, pinCode)
        val subtotal = itemList.sumOf { it.price * it.weight / 1000.0 }

        firestore.collection("DeliveryAddress")
            .add(address)
            .addOnSuccessListener { document ->
                progressDialog.dismiss()
                Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ConfirmOrderActivity::class.java)
                intent.putExtra("addressId", document.id)
                intent.putExtra("subtotal", subtotal)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to place order. Try again.", Toast.LENGTH_SHORT).show()
            }

        return true
    }
}
