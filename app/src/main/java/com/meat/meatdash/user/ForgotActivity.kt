package com.meat.meatdash.user

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.meat.meatdash.databinding.ActivityForgotBinding

class ForgotActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Back to login
        binding.backToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }


        // Reset password button
        binding.resetButton.setOnClickListener {
            val email = binding.UserEmail.text.toString().trim()
            if (validateEmail(email)) {
                sendPasswordResetEmail(email)
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.emailLayout.error = "Email is required"
                false
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailLayout.error = "Please enter a valid email"
                false
            }

            else -> {
                binding.emailLayout.error = null
                true
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.resetButton.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                binding.resetButton.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                    // Optionally navigate back to login
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}