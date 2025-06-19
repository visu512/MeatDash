// SignupActivity.kt
package com.meat.meatdash.user

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.meat.meatdash.R
import com.meat.meatdash.activity.PhoneActivity
import com.meat.meatdash.databinding.ActivitySignupBinding
import com.meat.meatdash.sharedpref.PrefsHelper
import com.vdx.designertoast.DesignerToast
import kotlin.random.Random

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    private var emailVerificationHandler = Handler(Looper.getMainLooper())
    private var isCheckingVerification = false
    private var tempPassword = ""

    companion object { private const val RC_SIGN_IN = 9001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupGoogleSignIn()
        setupUI()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    @Deprecated("Deprecated in API 30")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
                ?.idToken
                ?.let { token -> firebaseAuthWithGoogle(token) }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.displayName?.let {
                        PrefsHelper.saveString(this, "fullName", it)
                    }
                    DesignerToast.Success(this, "Welcome", "Signed in with Google", Gravity.TOP, Toast.LENGTH_SHORT, DesignerToast.STYLE_DARK)
                    redirectToPhoneActivity()
                }
            }
    }

    private fun setupUI() {
        binding.icVerify.visibility = View.GONE
        binding.Register.isEnabled = false

        binding.email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                binding.icVerify.visibility = if (Patterns.EMAIL_ADDRESS.matcher(text).matches()) View.VISIBLE else View.GONE
                if (binding.icVerify.text != "Verify") {
                    binding.icVerify.text = "Verify"
                    binding.Register.isEnabled = false
                }
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        binding.icVerify.setOnClickListener {
            val email = binding.email.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailLayout.error = "Invalid email"
                return@setOnClickListener
            }
            binding.emailLayout.error = null
            tempPassword = generateRandomPassword()
            sendVerificationEmail(email)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }

        binding.Register.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val pw = binding.password.text.toString()
            val rp = binding.RePassword.text.toString()
            val name = binding.userName.text.toString().trim()

            when {
                name.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    DesignerToast.Error(this, "Error", "Fill out all fields", Gravity.TOP, Toast.LENGTH_SHORT, DesignerToast.STYLE_DARK)
                }
                pw != rp || pw.length < 6 -> {
                    DesignerToast.Error(this, "Error", "Passwords must match and be ≥6 chars", Gravity.TOP, Toast.LENGTH_SHORT, DesignerToast.STYLE_DARK)
                }
                binding.icVerify.text.toString().contains("Verified").not() -> {
                    DesignerToast.Error(this, "Error", "Please verify email first", Gravity.TOP, Toast.LENGTH_SHORT, DesignerToast.STYLE_DARK)
                }
                else -> verifyAndRegister(email, pw, name)
            }
        }

        binding.LoginPageGoto.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun sendVerificationEmail(email: String) {
        auth.createUserWithEmailAndPassword(email, tempPassword)
            .addOnSuccessListener {
                auth.currentUser
                    ?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        DesignerToast.Info(this, "Info", "Verification email sent", Gravity.TOP, Toast.LENGTH_SHORT, DesignerToast.STYLE_DARK)
                        binding.icVerify.text = "Sent"
                        startEmailVerificationCheck(email)
                        auth.signOut()
                    }
            }
    }

    private fun startEmailVerificationCheck(email: String) {
        if (isCheckingVerification) return
        isCheckingVerification = true
        emailVerificationHandler.post(object : Runnable {
            override fun run() {
                auth.signInWithEmailAndPassword(email, tempPassword)
                    .addOnSuccessListener {
                        auth.currentUser?.reload()?.addOnSuccessListener {
                            if (auth.currentUser!!.isEmailVerified) {
                                binding.icVerify.text = "Verified ✓"
                                binding.Register.isEnabled = true
                                stopEmailVerificationCheck()
                                auth.signOut()
                            } else {
                                emailVerificationHandler.postDelayed(this, 3000)
                            }
                        }
                    }
                    .addOnFailureListener {
                        emailVerificationHandler.postDelayed(this, 3000)
                    }
            }
        })
    }

    private fun stopEmailVerificationCheck() {
        isCheckingVerification = false
        emailVerificationHandler.removeCallbacksAndMessages(null)
    }

    private fun verifyAndRegister(email: String, password: String, userName: String) {
        auth.signInWithEmailAndPassword(email, tempPassword)
            .addOnSuccessListener {
                auth.currentUser?.delete()?.addOnCompleteListener {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            auth.currentUser?.updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(userName)
                                    .build()
                            )?.addOnSuccessListener {
                                PrefsHelper.saveString(this, "fullName", userName)
                                redirectToPhoneActivity()
                            }
                        }
                }
            }
    }

    private fun redirectToPhoneActivity() {
        startActivity(Intent(this, PhoneActivity::class.java))
        finish()
    }

    private fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..12).map { chars.random() }.joinToString("")
    }
}
