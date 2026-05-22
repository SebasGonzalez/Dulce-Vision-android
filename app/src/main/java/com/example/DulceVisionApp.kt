package com.example

import android.app.Application
import android.util.Log
import com.example.data.di.DulceContainer

/**
 * DulceVisionApp
 * Custom Application class that initializes the dependency injection container and launches 
 * standard performance diagnostics.
 */
class DulceVisionApp : Application() {

    lateinit var container: DulceContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d("DulceVisionApp", "Starting DulceVision Commercial Streaming Suite...")
        
        // Dynamic construction of our production DI container
        container = DulceContainer(this)
        
        // Establish real-time persistent socket connections asynchronously
        container.webSocketClient.connect()
    }

    override fun onTerminate() {
        container.webSocketClient.disconnect()
        super.onTerminate()
    }
}
