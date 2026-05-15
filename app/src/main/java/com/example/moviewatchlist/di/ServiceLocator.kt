package com.example.moviewatchlist.di

import android.content.Context
import com.example.moviewatchlist.data.db.AppDatabase
import com.example.moviewatchlist.data.repository.MovieRepository
import com.example.moviewatchlist.data.repository.AuthRepository

import com.example.moviewatchlist.data.remote.tmdb.TmdbApiService
import com.example.moviewatchlist.util.TmdbConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ServiceLocator {
    @Volatile
    private var movieRepository: MovieRepository? = null

    @Volatile
    private var authRepository: AuthRepository? = null

    private val tmdbApiService: TmdbApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(TmdbConstants.TMDB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(TmdbApiService::class.java)
    }

    fun provideMovieRepository(appContext: Context): MovieRepository =
        movieRepository ?: synchronized(this) {
            movieRepository
                ?: MovieRepository(
                    AppDatabase.getInstance(appContext).movieDao(),
                    tmdbApiService,
                    FirebaseFirestore.getInstance(),
                    FirebaseAuth.getInstance()
                ).also { movieRepository = it }
        }

    fun provideAuthRepository(): AuthRepository =
        authRepository ?: synchronized(this) {
            authRepository ?: AuthRepository().also { authRepository = it }
        }

}

