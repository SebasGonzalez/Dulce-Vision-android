package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class Movie(
    val id: String,
    val title: String,
    val thumbnail: String,
    val backdrop: String,
    val videoUrl: String,
    val duration: String,
    val genre: String,
    val year: Int,
    val rating: Double,
    val description: String,
    val isTrend: Boolean = false,
    val isPopular: Boolean = false,
    val progressPercent: Float = 0f,
    val lastPositionMs: Long = 0L
)

data class Series(
    val id: String,
    val title: String,
    val thumbnail: String,
    val backdrop: String,
    val genres: String,
    val rating: Double,
    val description: String,
    val year: Int
)

data class Season(
    val id: String,
    val seriesId: String,
    val number: Int,
    val title: String
)

data class Episode(
    val id: String,
    val seasonId: String,
    val seriesId: String,
    val number: Int,
    val title: String,
    val thumbnail: String,
    val videoUrl: String,
    val duration: String,
    val description: String,
    val progressPercent: Float = 0f,
    val lastPositionMs: Long = 0L
)

data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String,
    val streamUrl: String,
    val category: String,
    val isFavorite: Boolean = false,
    val epgTitle: String = "Sintonizando emisión dial...",
    val epgTimeCode: String = "20:00 - 22:00",
    val epgNextTitle: String = "A continuación: DulceVision Premiere"
)

data class Banner(
    val id: String,
    val title: String,
    val imageUrl: String,
    val subtitle: String,
    val actionUrl: String,
    val mediaType: String, // "movie", "series", "channel"
    val mediaId: String
)

data class UserProfile(
    val uuid: String,
    val name: String,
    val avatarUrl: String,
    val isAdult: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "avatarUrl") val avatarUrl: String,
    @Json(name = "isAdult") val isAdult: Any? = null
) {
    fun toDomain() = UserProfile(
        uuid = id,
        name = name,
        avatarUrl = avatarUrl,
        isAdult = when (isAdult) {
            is Boolean -> isAdult
            is Number -> isAdult.toInt() == 1
            is String -> isAdult == "1" || isAdult.lowercase() == "true"
            else -> true
        }
    )

    companion object {
        fun fromDomain(profile: UserProfile) = ProfileDto(
            id = profile.uuid,
            name = profile.name,
            avatarUrl = profile.avatarUrl,
            isAdult = profile.isAdult
        )
    }
}

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val mediaId: String? = null,
    val mediaType: String? = null // "movie", "series", "channel"
)
