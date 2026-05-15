package com.example.moviewatchlist.data.remote.tmdb.dto

data class GenreDto(
    val id: Int,
    val name: String,
)

data class GenreResponse(
    val genres: List<GenreDto>,
)
