package com.example.moviewatchlist.data.remote.tmdb.dto

import com.google.gson.annotations.SerializedName

data class MovieDto(
    val id: Long,
    val title: String?,
    val overview: String?,
    val poster_path: String?,
    val release_date: String?,
    @SerializedName("vote_average")
    val vote_average: Double?,
)

