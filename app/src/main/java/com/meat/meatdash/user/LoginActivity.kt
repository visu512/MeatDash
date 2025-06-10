package com.meat.meatdash.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.meat.meatdash.R
import com.meat.meatdash.activity.PhoneActivity
import com.meat.meatdash.databinding.ActivityLoginBinding
import com.meat.meatdash.sharedpref.PrefsHelper
import com.vdx.designertoast.DesignerToast

class LoginActivity : AppCompatActivity() {

    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Request code for Google Sign-In
    private companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "GoogleSignIn"
    }

    override fun onStart() {
        super.onStart()
        // If already signed in, go straight to PhoneActivity
        val currentUser = auth.currentUser
        if (currentUser != null) {
            redirectToPhoneActivity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        configureGoogleSignIn()

        // Set up click listeners
        setupClickListeners()
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        // Email/Password Login
        binding.Loginbutton.setOnClickListener {
            val email = binding.UserEmail.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please fill all details", DesignerToast.STYLE_DARK)
            } else {
                loginWithEmailPassword(email, password)
            }
        }

        // Register Button → go to SignupActivity
        binding.RegisterBtn.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // Forgot Password → (you presumably have a ForgotActivity)
        binding.forgetBtn.setOnClickListener {
            startActivity(Intent(this, ForgotActivity::class.java))
        }

        // Privacy Policy link
        binding.privacyPolicyText.setOnClickListener {
            openUrl("https://privicy-policies.netlify.app/")
        }

        // Google Sign-In
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun loginWithEmailPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Login successful!", DesignerToast.STYLE_DARK, isSuccess = true)
                    redirectToPhoneActivity()
                } else {
                    when {
                        password.length < 6 -> {
                            showToast(
                                "Password must be at least 6 characters!",
                                DesignerToast.STYLE_DARK
                            )
                        }

                        task.exception?.message?.contains("no user record") == true -> {
                            showToast("Account doesn't exist", DesignerToast.STYLE_DARK)
                        }

                        else -> {
                            showToast(
                                "Login failed: ${task.exception?.message}",
                                DesignerToast.STYLE_DARK
                            )
                        }
                    }
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in API 30")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign-In succeeded, now authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                showToast("Google sign in failed: ${e.message}", DesignerToast.STYLE_DARK)
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Save display name to SharedPreferences
                    val displayName = user?.displayName
                    if (!displayName.isNullOrEmpty()) {
                        PrefsHelper.saveString(this, "fullName", displayName)
                    }
                    showToast(
                        "Google sign in successful!",
                        DesignerToast.STYLE_DARK,
                        isSuccess = true
                    )
                    redirectToPhoneActivity()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    showToast(
                        "Authentication failed: ${task.exception?.message}",
                        DesignerToast.STYLE_DARK
                    )
                }
            }
    }


    private fun redirectToPhoneActivity() {
        startActivity(Intent(this, PhoneActivity::class.java))
        finish()
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showToast("No browser found to open link", DesignerToast.STYLE_DARK)
        }
    }

    private fun showToast(
        message: String,
        style: String,
        isSuccess: Boolean = false,
        title: String = if (isSuccess) "Success" else "Error"
    ) {
        if (isSuccess) {
            DesignerToast.Success(
                this,
                title,
                message,
                Gravity.TOP,
                Toast.LENGTH_SHORT,
                style
            )
        } else {
            DesignerToast.Error(
                this,
                title,
                message,
                Gravity.TOP,
                Toast.LENGTH_SHORT,
                style
            )
        }
    }
}
