package com.meat.meatdash

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.meat.meatdash.databinding.ActivityLoginBinding
import com.vdx.designertoast.DesignerToast

@Suppress("UNUSED_EXPRESSION")
class LoginActivity : AppCompatActivity() {
    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    // firebase auth
    private lateinit var auth: FirebaseAuth


    // if user already logged in
    override fun onStart() {
        super.onStart()

        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize auth
        auth = FirebaseAuth.getInstance()

        binding.Loginbutton.setOnClickListener {
            val Email: String = binding.UserEmail.text.toString()
            var password: String = binding.password.text.toString()

            // if field is blank
            if (Email.isEmpty() || password.isEmpty()) {

//                Toast.makeText(this, "Please Fill all details", Toast.LENGTH_SHORT).show()
                DesignerToast.Info(
                    this,
                    "Info",
                    "Please fill the all details!",
                    Gravity.TOP,
                    Toast.LENGTH_SHORT,
                    DesignerToast.STYLE_DARK
                );

            } else {
                auth.signInWithEmailAndPassword(Email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
//                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        DesignerToast.Success(
                            this,
                            "Success",
                            "Login successful!",
                            Gravity.TOP,
                            Toast.LENGTH_SHORT,
                            DesignerToast.STYLE_DARK
                        );

                        // go to mainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()

                    } else if (password.length < 6) {
                        DesignerToast.Error(
                            this,
                            "Error!",
                            "Password must be at least 6 characters!",
                            Gravity.TOP,
                            Toast.LENGTH_SHORT,
                            DesignerToast.STYLE_DARK
                        );
                    } else {
                        DesignerToast.Error(
                            this,
                            "Login failed!",
                            "Your email and password does't  exist!",
                            Gravity.TOP,
                            Toast.LENGTH_SHORT,
                            DesignerToast.STYLE_DARK
                        );
                        //  ${task.exception?.message}

                    }
                }
            }

        }


        // go to register page
        binding.RegisterBtn.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))

        }
    }
}