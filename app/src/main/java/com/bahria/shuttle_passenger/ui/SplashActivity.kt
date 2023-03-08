package com.bahria.shuttle_passenger.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.databinding.DataBindingUtil
import com.bahria.shuttle_passenger.BuildConfig
import com.bahria.shuttle_passenger.R
import com.bahria.shuttle_passenger.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        binding.version.text =
            "Version " + BuildConfig.VERSION_NAME + "." + BuildConfig.VERSION_CODE
        Handler(Looper.myLooper()!!).postDelayed({
            startActivity(
                Intent(
                    this@SplashActivity,
                    MainActivity::class.java
                )
            )
            finish()
        }, 3000)
    }
}