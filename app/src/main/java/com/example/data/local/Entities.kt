package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val thumbnail: String,
    val backdrop: String,
    val videoUrl: String,
    val duration: String,
    val genre: String,
    val year: Int,
    val rating: Double,
    val description: String,
    val isTrend: Boolean,
    val isPopular: Boolean,
    val progressPercent: Float,
    val lastPositionMs: Long
)

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val thumbnail: String,
    val backdrop: String,
    val genres: String,
    val rating: Double,
    val description: String,
    val year: Int
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String,
    val streamUrl: String,
    val category: String,
    val isFavorite: Boolean,
    val epgTitle: String,
    val epgTimeCode: String,
    val epgNextTitle: String
)

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey val mediaId: String, // movie ID or episode ID
    val mediaType: String, // "movie" or "episode"
    val title: String,
    val detailText: String, // e.g., "S1 E2 • Episode Title" or "Pelicula"
    val thumbnail: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val type: String, // "movie", "series", "channel"
    val title: String,
    val thumbnail: String,
    val timestamp: Long = System.currentTimeMillis()
)
