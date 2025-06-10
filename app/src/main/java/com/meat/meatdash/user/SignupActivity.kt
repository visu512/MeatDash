package com.meat.meatdash.user

import android.annotation.SuppressLint
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.meat.meatdash.R
import com.meat.meatdash.activity.PhoneActivity
import com.meat.meatdash.sharedpref.PrefsHelper
import com.meat.meatdash.databinding.ActivitySignupBinding
import com.vdx.designertoast.DesignerToast
import kotlin.random.Random

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private var emailVerificationHandler = Handler(Looper.getMainLooper())
    private var isCheckingVerification = false
    private var tempPassword = ""

    private companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
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
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (e: ApiException) {
                showToast("Google Sign-In failed: ${e.message}", ToastType.ERROR)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    val displayName = user?.displayName
                    if (!displayName.isNullOrEmpty()) {
                        PrefsHelper.saveString(this, "fullName", displayName)
                    }
                    showToast("Welcome ${user?.displayName}", ToastType.SUCCESS)
                    redirectToPhoneActivity()
                } else {
                    showToast("Google Auth Failed: ${task.exception?.message}", ToastType.ERROR)
                }
            }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        binding.icVerify.visibility = View.GONE
        binding.Register.isEnabled = false

        binding.email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val emailText = s?.toString()?.trim() ?: ""
                binding.icVerify.visibility =
                    if (emailText.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(emailText).matches())
                        View.VISIBLE
                    else
                        View.GONE

                if (binding.icVerify.text != "Verify") {
                    binding.icVerify.text = "Verify"
                    binding.icVerify.setTextColor(
                        ContextCompat.getColor(this@SignupActivity, R.color.black)
                    )
                    binding.Register.isEnabled = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.icVerify.setOnClickListener {
            val email = binding.email.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailLayout.error = "Invalid email"
                return@setOnClickListener
            }
            showToast("Sending verification link…", ToastType.INFO)
            tempPassword = generateRandomPassword()
            sendVerificationEmail(email)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        binding.password.setOnTouchListener { _, _ ->
            binding.btnSuggestPassword.visibility = View.VISIBLE
            false
        }

        binding.btnSuggestPassword.setOnClickListener {
            val password = generateRandomPassword()
            binding.password.setText(password)
            binding.RePassword.setText(password)
            binding.btnSuggestPassword.visibility = View.GONE
        }

        binding.Register.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val rePassword = binding.RePassword.text.toString().trim()
            val userName = binding.userName.text.toString().trim()

            if (userName.isEmpty() || email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("Please fill all fields properly", ToastType.ERROR)
                return@setOnClickListener
            }

            if (password != rePassword || password.length < 6) {
                showToast("Passwords do not match or are too short", ToastType.ERROR)
                return@setOnClickListener
            }

            if (!isEmailVerified()) {
                showToast("Please verify your email first", ToastType.ERROR)
                return@setOnClickListener
            }

            binding.Register.isEnabled = false
            verifyAndRegister(email, password, userName)
        }

        binding.LoginPageGoto.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun sendVerificationEmail(email: String) {
        auth.createUserWithEmailAndPassword(email, tempPassword)
            .addOnSuccessListener {
                val user = auth.currentUser
                user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        showToast("Verification email sent to $email", ToastType.SUCCESS)
                        updateVerificationUI("Sent", R.color.blue)
                        startEmailVerificationCheck()
                        auth.signOut()
                    }
                    ?.addOnFailureListener { e ->
                        showToast("Failed to send verification: ${e.message}", ToastType.ERROR)
                        user?.delete()
                    }
            }
            .addOnFailureListener { e ->
                showToast("Failed to create temp account: ${e.message}", ToastType.ERROR)
            }
    }

    private fun startEmailVerificationCheck() {
        if (isCheckingVerification) return
        isCheckingVerification = true
        val email = binding.email.text.toString().trim()
        if (email.isEmpty()) return

        emailVerificationHandler.post(object : Runnable {
            override fun run() {
                auth.signInWithEmailAndPassword(email, tempPassword)
                    .addOnSuccessListener {
                        val user = auth.currentUser
                        user?.reload()?.addOnSuccessListener {
                            if (user.isEmailVerified) {
                                updateVerificationUI("Verified ✓", R.color.blue)
                                binding.emailLayout.isEnabled = false
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

    private fun updateVerificationUI(text: String, colorRes: Int) {
        binding.icVerify.text = text
        binding.icVerify.setTextColor(ContextCompat.getColor(this, colorRes))
        binding.Register.isEnabled = text.contains("Verified")
    }

    private fun isEmailVerified(): Boolean {
        return binding.icVerify.text.toString().contains("Verified")
    }

    private fun verifyAndRegister(email: String, password: String, userName: String) {

        // Save fullName to SharedPreferences instead of Firestore
        PrefsHelper.saveString(this, "fullName", userName)

        auth.signInWithEmailAndPassword(email, tempPassword)
            .addOnSuccessListener {
                val verifiedUser = auth.currentUser
                if (verifiedUser?.isEmailVerified == true) {
                    // 1) Delete the temporary account
                    verifiedUser.delete().addOnCompleteListener {
                        // 2) Create the real account with the final password
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                val user = auth.currentUser
                                // 3) Update display name
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(userName)
                                    .build()
                                user?.updateProfile(profileUpdates)
                                    ?.addOnSuccessListener {
                                        // 4) No Firestore save here, just redirect
                                        redirectToPhoneActivity()
                                    }
                                    ?.addOnFailureListener { e ->
                                        showToast(
                                            "Failed to set display name: ${e.message}",
                                            ToastType.ERROR
                                        )
                                        binding.Register.isEnabled = true
                                    }
                            }
                            .addOnFailureListener { e ->
                                showToast(
                                    "Final account creation failed: ${e.message}",
                                    ToastType.ERROR
                                )
                                binding.Register.isEnabled = true
                            }
                    }
                } else {
                    showToast("Please verify your email first", ToastType.ERROR)
                    binding.Register.isEnabled = true
                    auth.signOut()
                }
            }
            .addOnFailureListener { e ->
                showToast("Failed login: ${e.message}", ToastType.ERROR)
                binding.Register.isEnabled = true
            }
    }

    private fun redirectToPhoneActivity() {
        startActivity(Intent(this, PhoneActivity::class.java))
        finish()
    }

    private fun generateRandomPassword(): String {
        val allChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()_+"
        return (1..12).map { allChars[Random.nextInt(allChars.length)] }.joinToString("")
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

    enum class ToastType { SUCCESS, ERROR, INFO }
}
