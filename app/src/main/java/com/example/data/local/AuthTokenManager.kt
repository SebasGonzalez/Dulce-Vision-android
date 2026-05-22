package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * AuthTokenManager
 * Manages client-side JWT persistence securely in Android EncryptedSharedPreferences.
 */
class AuthTokenManager(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "dulcevision_secure_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("AuthTokenManager", "Encryption keystore failure, fallback to standard prefs: ${e.message}")
        context.getSharedPreferences("dulcevision_auth_prefs", Context.MODE_PRIVATE)
    }

    fun saveAccessToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
        Log.d("AuthTokenManager", "Saved encrypted JWT Access Token.")
    }

    fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun saveRefreshToken(token: String) {
        prefs.edit().putString("refresh_token", token).apply()
    }

    fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }

    fun saveCurrentUser(email: String, userId: String, displayName: String?) {
        prefs.edit().apply {
            putString("user_email", email)
            putString("user_id", userId)
            putString("user_display_name", displayName)
        }.apply()
    }

    fun getCurrentUserEmail(): String? = prefs.getString("user_email", null)
    fun getCurrentUserId(): String? = prefs.getString("user_id", null)
    fun getCurrentUserDisplayName(): String? = prefs.getString("user_display_name", null)

    fun clearAuth() {
        prefs.edit().clear().apply()
        Log.d("AuthTokenManager", "Cleared credentials cache successfully.")
    }
}
