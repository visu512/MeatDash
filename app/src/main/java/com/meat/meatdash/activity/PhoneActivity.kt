package com.meat.meatdash.activity

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.meat.meatdash.R
import com.meat.meatdash.databinding.ActivityPhoneBinding
import com.meat.meatdash.sharedpref.PrefsHelper

class PhoneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize a ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Saving phone number…")
            setCancelable(false)
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnContinue.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            if (validatePhoneNumber(phoneNumber)) {
                savePhoneNumber(phoneNumber)
            }
        }
    }

    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        return if (phoneNumber.length == 10 && phoneNumber.all { it.isDigit() }) {
            binding.tilPhoneNumber.error = null
            true
        } else {
            binding.tilPhoneNumber.error = "Enter a valid 10-digit phone number"
            false
        }
    }

    private fun savePhoneNumber(phoneNumber: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in to save your phone number", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Show loading dialog
        progressDialog.show()

        // Simulate any asynchronous work here if needed. In this case, SharedPreferences is synchronous,
        PrefsHelper.saveString(this, "phoneNumber", phoneNumber)

        // Hide loading dialog
        progressDialog.dismiss()

        Toast.makeText(this, "Phone number saved!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
