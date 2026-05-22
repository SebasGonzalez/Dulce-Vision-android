package com.example.data.remote

import com.example.data.model.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import retrofit2.http.*

interface DulceVisionApiService {

    @POST("api/auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Map<String, Any>

    @POST("api/auth/register")
    suspend fun register(@Body data: Map<String, String>): Map<String, Any>

    @GET("api/users/profiles")
    suspend fun getProfiles(): List<ProfileDto>

    @POST("api/users/profiles")
    suspend fun createProfile(@Body profile: ProfileDto): ProfileDto

    @PUT("api/users/profiles/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body profile: ProfileDto): ProfileDto

    @DELETE("api/users/profiles/{id}")
    suspend fun deleteProfile(@Path("id") id: String): Map<String, Any>

    @GET("api/movies")
    suspend fun getMovies(): List<Movie>

    @GET("api/movies/{id}")
    suspend fun getMovieById(@Path("id") id: String): Movie

    @GET("api/series")
    suspend fun getSeries(): List<Series>

    @GET("api/series/{seriesId}/seasons")
    suspend fun getSeasons(@Path("seriesId") seriesId: String): List<Season>

    @GET("api/series/seasons/{seasonId}/episodes")
    suspend fun getEpisodes(@Path("seasonId") seasonId: String): List<Episode>

    @GET("api/iptv/channels")
    suspend fun getChannels(): List<Channel>

    @POST("api/analytics/watch-progress")
    suspend fun syncWatchProgress(@Body progress: Map<String, String>): Map<String, Any>

    @GET("api/banners")
    suspend fun getBanners(): List<Banner>

    companion object {
        private const val BASE_URL = "http://10.0.2.2:5000/"

        fun create(): DulceVisionApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(DulceVisionApiService::class.java)
        }
    }
}
