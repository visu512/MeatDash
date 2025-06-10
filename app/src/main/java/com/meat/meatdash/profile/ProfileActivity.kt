package com.meat.meatdash.profile

import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.meat.meatdash.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        loadUserProfile()

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        // Avatar letter
        val email = user.email.orEmpty()
        binding.tvAvatar.text = email
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "U"

        binding.etEmail.setText(email)

        val uid = user.uid
        firestore.collection("userLocation")
            .document(uid)
            .collection("selectedAddress")
            .document("profile")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    binding.apply {
                        etFullName.setText(snap.getString("fullName").orEmpty())
                        etPhone   .setText(snap.getString("phoneNumber").orEmpty())
                        etStreet  .setText(snap.getString("street").orEmpty())
                        etHouseApt.setText(snap.getString("houseApartment").orEmpty())
                        etCity    .setText(snap.getString("city").orEmpty())
                        etState   .setText(snap.getString("state").orEmpty())
                        etZip     .setText(snap.getString("zipCode").orEmpty())
                    }
                } else {
                    binding.etFullName.setText(user.displayName.orEmpty())
                }
            }
            .addOnFailureListener {
                binding.etFullName.setText(user.displayName.orEmpty())
            }
    }

    private fun saveProfile() {
        binding.apply {
            val fullName    = etFullName.text.toString().trim()
            val phoneNumber = etPhone.text.toString().trim()
            // … collect other fields …

            if (TextUtils.isEmpty(fullName)) {
                etFullName.error = "Please enter your name"
                return
            }
            if (phoneNumber.length != 10) {
                etPhone.error = "Enter a valid 10‐digit phone number"
                return
            }

            val data = hashMapOf(
                "fullName" to fullName,
                "phoneNumber" to phoneNumber,
                "street" to etStreet.text.toString().trim(),
                "houseApartment" to etHouseApt.text.toString().trim(),
                "city" to etCity.text.toString().trim(),
                "state" to etState.text.toString().trim(),
                "zipCode" to etZip.text.toString().trim(),
                "updatedAt" to System.currentTimeMillis()
            )

            auth.currentUser?.uid?.let { uid ->
                firestore.collection("userLocation")
                    .document(uid)
                    .collection("selectedAddress")
                    .document("profile")
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(this@ProfileActivity,
                            "Profile updated successfully",
                            Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@ProfileActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                Toast.makeText(this@ProfileActivity,
                    "Please sign in first",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}
