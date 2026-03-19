package com.example.moviewatchlist.data.remote.tmdb

import com.example.moviewatchlist.data.remote.tmdb.dto.MovieDto
import com.example.moviewatchlist.data.remote.tmdb.dto.MovieSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
    ): MovieSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
    ): MovieDto
}

