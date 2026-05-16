package com.example.moviewatchlist.util

object TmdbConstants {
    const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

    // Images are served from a separate CDN base URL.
    const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

    // TODO: Replace this with a safer mechanism (e.g., build config) before production.
    const val TMDB_API_KEY: String = ""
}

