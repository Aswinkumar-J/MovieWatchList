package com.example.moviewatchlist.di

import android.content.Context
import com.example.moviewatchlist.data.db.AppDatabase
import com.example.moviewatchlist.data.repository.MovieRepository

object ServiceLocator {
    @Volatile
    private var repository: MovieRepository? = null

    fun provideMovieRepository(appContext: Context): MovieRepository =
        repository ?: synchronized(this) {
            repository
                ?: MovieRepository(
                    AppDatabase.getInstance(appContext).movieDao(),
                ).also { repository = it }
        }
}

