package com.example.moviewatchlist.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movies",
    indices = [Index(value = ["tmdbId"], unique = true)],
)
data class Movie(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val status: WatchStatus,
    val rating: Float = 0f,
    val review: String? = null,
    val synopsis: String? = null,
    val runtimeMinutes: Int? = null,
    val posterUrl: String? = null,
    val tmdbId: Long? = null,
    val tmdbVoteAverage: Float? = null,
)

