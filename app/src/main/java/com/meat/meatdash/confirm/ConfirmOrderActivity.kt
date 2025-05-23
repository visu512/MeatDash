package com.meat.meatdash.confirm

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.meat.meatdash.databinding.ActivityConfirmOrderBinding
import com.meat.meatdash.model.DeliveryAddress
import java.util.jar.Attributes

class ConfirmOrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmOrderBinding
    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val addressId = intent.getStringExtra("addressId")

        if (addressId != null) {
            firestore.collection("DeliveryAddress")
                .document(addressId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val address = snapshot.toObject(DeliveryAddress::class.java)
                    if (address != null) {
                        binding.tvCName.text = address.fullName
                        binding.tvCPhone.text = address.phone
                        binding.tvCStreet.text = address.street
                        binding.tvCPinCode.text = address.pinCode
                        binding.tvCCity.text = "${address.city}, ${address.state}"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load address.", Toast.LENGTH_SHORT).show()
                }
        } else {
        }

        // prices calculate
        val subtotal = intent.getDoubleExtra("subtotal", 0.0)
        val shipping = 5.0
        val tax = 2.0
        val total = subtotal + shipping + tax

        binding.tvSubtotal.text = "₹%.2f".format(subtotal)
        binding.tvShipping.text = "₹%.2f".format(shipping)
        binding.tvTax.text = "₹%.2f".format(tax)
        binding.tvTotal.text = "₹%.2f".format(total)

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed() //  back to CheckoutActivity
        return true
    }
}
