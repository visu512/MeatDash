package com.meat.meatdash

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.meat.meatdash.databinding.ActivitySignupBinding
import com.vdx.designertoast.DesignerToast

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.LoginPageGoto.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.Register.setOnClickListener {
            registerUser()
        }

    }

    private fun registerUser() {
        val email = binding.email.text.toString().trim()
        val userName = binding.userName.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val rePassword = binding.RePassword.text.toString().trim()

        when {
            email.isEmpty() || userName.isEmpty() || password.isEmpty() || rePassword.isEmpty() -> {
                showToast("Please fill all details", ToastType.INFO)
            }

            password != rePassword -> {
                showToast("Passwords do not match", ToastType.ERROR)
            }

            password.length < 6 -> {
                showToast("Password must be at least 6 characters", ToastType.ERROR)
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showToast("Please enter a valid email", ToastType.ERROR)
            }

            else -> {
                createUserWithEmail(email, password, userName)
            }
        }
    }

    private fun createUserWithEmail(email: String, password: String, userName: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Update user profile with display name
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(userName)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            showToast("Registration successful", ToastType.SUCCESS)
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            showToast(
                                "Profile setup failed: ${profileTask.exception?.message}",
                                ToastType.ERROR
                            )
                        }
                    }
                } else {
                    showToast("Registration failed: ${task.exception?.message}", ToastType.ERROR)
                }
            }
    }

    private fun showToast(message: String, type: ToastType) {
        when (type) {
            ToastType.SUCCESS -> DesignerToast.Success(
                this,
                "Success",
                message,
                Gravity.TOP,
                Toast.LENGTH_SHORT,
                DesignerToast.STYLE_DARK
            )

            ToastType.ERROR -> DesignerToast.Error(
                this,
                "Error",
                message,
                Gravity.TOP,
                Toast.LENGTH_SHORT,
                DesignerToast.STYLE_DARK
            )

            ToastType.INFO -> DesignerToast.Info(
                this,
                "Info",
                message,
                Gravity.TOP,
                Toast.LENGTH_SHORT,
                DesignerToast.STYLE_DARK
            )
        }
    }

    enum class ToastType {
        SUCCESS, ERROR, INFO
    }
}