package com.example.moviewatchlist

import android.os.Bundle
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.moviewatchlist.di.ServiceLocator

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start Firestore synchronization
        ServiceLocator.provideMovieRepository(this).startWatchlistSync()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop sync to avoid leaks
        ServiceLocator.provideMovieRepository(this).stopWatchlistSync()
    }
}