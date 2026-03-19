package com.example.moviewatchlist.di

import android.content.Context
import com.example.moviewatchlist.data.db.AppDatabase
import com.example.moviewatchlist.data.repository.MovieRepository
import com.example.moviewatchlist.data.remote.tmdb.TmdbApiService
import com.example.moviewatchlist.util.TmdbConstants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceLocator {
    @Volatile
    private var repository: MovieRepository? = null

    private val tmdbApiService: TmdbApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(TmdbConstants.TMDB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(TmdbApiService::class.java)
    }

    fun provideMovieRepository(appContext: Context): MovieRepository =
        repository ?: synchronized(this) {
            repository
                ?: MovieRepository(
                    AppDatabase.getInstance(appContext).movieDao(),
                    tmdbApiService,
                ).also { repository = it }
        }
}

