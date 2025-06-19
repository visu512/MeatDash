package com.meat.meatdash.user

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.identity.intents.AddressConstants.Themes
import com.google.firebase.auth.FirebaseAuth
import com.meat.meatdash.R
import com.meat.meatdash.activity.MainActivity
import com.meat.meatdash.activity.PhoneActivity
import com.meat.meatdash.sharedpref.PrefsHelper

class SplashActivity : AppCompatActivity() {

    // 2 seconds
    private val splashDelayMillis = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
//        setTheme(R.style.SplashTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

//        // Hide navigation and status bars
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        // Tint status bar on Lollipop+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.parseColor("#FEF3ED")
        }

        Handler(Looper.getMainLooper()).postDelayed({
            routeAfterSplash()
        }, splashDelayMillis)
    }

    private fun routeAfterSplash() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        val target = when {
            currentUser == null -> LoginActivity::class.java
            PrefsHelper.getString(this, "phoneNumber").orEmpty().isNotEmpty() ->
                MainActivity::class.java

            else -> PhoneActivity::class.java
        }

        startActivity(Intent(this, target))
        finish()
    }
}

