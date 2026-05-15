package com.example.moviewatchlist

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.moviewatchlist.databinding.ActivitySplashBinding
import com.example.moviewatchlist.ui.auth.LoginActivity
import com.example.moviewatchlist.di.ServiceLocator
import com.example.moviewatchlist.data.repository.AuthRepository



class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Modern Animation: Fade in the logo and app name
        binding.logo.animate()
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(200)
            .start()

        binding.appName.animate()
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(500)
            .start()

        // Transition after 2.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val authRepository = ServiceLocator.provideAuthRepository()
            val intent = if (authRepository.currentUser != null) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2500)

    }
}
