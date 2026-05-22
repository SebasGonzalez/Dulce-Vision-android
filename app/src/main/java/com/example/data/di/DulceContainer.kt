package com.example.data.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.AuthTokenManager
import com.example.data.remote.AuthInterceptor
import com.example.data.remote.DulceVisionApiService
import com.example.data.remote.DulceVisionWebSocketClient
import com.example.data.repository.MediaRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * DulceContainer
 * Decoupled commercial-grade DI Container providing unified singleton references.
 * Resolves dependency queries cleanly across ViewModels and Repositories.
 */
class DulceContainer(private val context: Context) {

    val tokenManager: AuthTokenManager by lazy {
        AuthTokenManager(context)
    }

    val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(tokenManager)
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val apiService: DulceVisionApiService by lazy {
        val baseUrl = "http://10.0.2.2:5000/"
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DulceVisionApiService::class.java)
    }

    val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(context, apiService)
    }

    val webSocketClient: DulceVisionWebSocketClient by lazy {
        DulceVisionWebSocketClient(context, mediaRepository)
    }
}
