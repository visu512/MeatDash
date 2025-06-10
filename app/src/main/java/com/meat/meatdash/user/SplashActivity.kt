package com.meat.meatdash.user

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.meat.meatdash.R
import com.meat.meatdash.activity.MainActivity
import com.meat.meatdash.activity.PhoneActivity
import com.meat.meatdash.sharedpref.PrefsHelper

class SplashActivity : AppCompatActivity() {

    // 0.5 seconds (1000 milliseconds)
    private val splashDelayMillis = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hide navigation and status bars for a full-screen splash
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        Handler(Looper.getMainLooper()).postDelayed({
            routeAfterSplash()
        }, splashDelayMillis)
    }

    private fun routeAfterSplash() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // 1) If no Firebase user is logged in, go to LoginActivity
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 2) Otherwise, check SharedPreferences for a saved phone number
        val savedPhone = PrefsHelper.getString(this, "phoneNumber") ?: ""

        if (savedPhone.isNotEmpty()) {
            // → phone number was already saved; go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // → no phone number found; go to PhoneActivity
            startActivity(Intent(this, PhoneActivity::class.java))
        }
        finish()
    }
}
