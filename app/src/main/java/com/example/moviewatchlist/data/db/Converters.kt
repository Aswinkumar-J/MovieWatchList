package com.example.moviewatchlist.data.db

import androidx.room.TypeConverter
import com.example.moviewatchlist.data.model.WatchStatus

class Converters {
    @TypeConverter
    fun watchStatusToString(value: WatchStatus): String = value.name

    @TypeConverter
    fun stringToWatchStatus(value: String): WatchStatus = WatchStatus.valueOf(value)
}

