package com.meat.meatdash.activity

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.hbb20.CountryCodePicker
import com.meat.meatdash.activity.MainActivity
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

        auth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this).apply {
            setMessage("Saving phone number…")
            setCancelable(false)
        }

        // Lock CCP to India, purely decorative (no filters applied)
        binding.ccp.apply {
            setDefaultCountryUsingNameCode("IN")
            setClickable(false)
            setFocusable(false)
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnContinue.setOnClickListener {
            val raw = binding.etPhoneNumber.text.toString().trim()

            // 1) Must be exactly 10 digits
            if (raw.length != 10 || !raw.all { it.isDigit() }) {
                binding.tilPhoneNumber.error = "Enter exactly 10 digits"
                binding.etPhoneNumber.requestFocus()
                return@setOnClickListener
            }

            // Clear error
            binding.tilPhoneNumber.error = null

            // Prepend +91 and save
            val fullNumber = "+91$raw"
            savePhoneNumber(fullNumber)
        }
    }

    private fun savePhoneNumber(phoneNumber: String) {
        auth.currentUser ?: run {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()
        PrefsHelper.saveString(this, "phoneNumber", phoneNumber)
        progressDialog.dismiss()

//        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
