package com.example.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.model.*
import com.example.data.remote.GeminiClient
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AIRecommendationState {
    object Idle : AIRecommendationState
    object Loading : AIRecommendationState
    data class Success(val recommendations: List<String>) : AIRecommendationState
    data class Error(val message: String) : AIRecommendationState
}

class DulceVisionViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as com.example.DulceVisionApp
    private val repository = app.container.mediaRepository
    private val tokenManager = app.container.tokenManager

    fun fetchProfilesFromApi() {
        viewModelScope.launch {
            try {
                val profilesDtoList = app.container.apiService.getProfiles()
                if (profilesDtoList.isNotEmpty()) {
                    val domainProfiles = profilesDtoList.map { it.toDomain() }
                    _availableProfiles.value = domainProfiles
                    _currentProfile.value = domainProfiles.first()
                }
            } catch (e: Exception) {
                Log.e("DulceVisionVM", "Failed to retrieve real profiles: ${e.message}")
            }
        }
    }

    // --- State Observables mapped dynamically ---
    val banners = repository.banners

    val movies: StateFlow<List<Movie>> = repository.allMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingMovies: StateFlow<List<Movie>> = repository.trendingMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val popularMovies: StateFlow<List<Movie>> = repository.popularMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val series: StateFlow<List<Series>> = repository.allSeries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<Channel>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels: StateFlow<List<Channel>> = repository.favoriteChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<HistoryEntity>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = repository.favoritesList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Playback State ---
    private val _activePlaybackUrl = MutableStateFlow<String?>(null)
    val activePlaybackUrl = _activePlaybackUrl.asStateFlow()

    private val _activePlaybackTitle = MutableStateFlow<String?>(null)
    val activePlaybackTitle = _activePlaybackTitle.asStateFlow()

    private val _activePlaybackThumbnail = MutableStateFlow<String?>(null)
    val activePlaybackThumbnail = _activePlaybackThumbnail.asStateFlow()

    // --- Search & Filters ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory = _selectedCategory.asStateFlow()

    // --- Profile & Authentication Engine ---
    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile = _currentProfile.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail = _currentUserEmail.asStateFlow()

    private val _currentUserDisplayName = MutableStateFlow<String?>(null)
    val currentUserDisplayName = _currentUserDisplayName.asStateFlow()

    private val _isGoogleAccountBound = MutableStateFlow(false)
    val isGoogleAccountBound = _isGoogleAccountBound.asStateFlow()

    private val _availableProfiles = MutableStateFlow(
        listOf(
            UserProfile("prof_1", "Sebas Premium", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"),
            UserProfile("prof_2", "Familia Cine", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"),
            UserProfile("prof_3", "Kids Room", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150", isAdult = false)
        )
    )
    val availableProfiles = _availableProfiles.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn = _isUserLoggedIn.asStateFlow()

    // --- AI Recommendations Agent Stream ---
    private val _aiRecommendationState = MutableStateFlow<AIRecommendationState>(AIRecommendationState.Idle)
    val aiRecommendationState = _aiRecommendationState.asStateFlow()

    // --- Notification & Socket Stream Alert ---
    val recentSystemAlert = repository.realtimeNotifications

    init {
        val cachedEmail = tokenManager.getCurrentUserEmail()
        val cachedToken = tokenManager.getAccessToken()
        if (!cachedEmail.isNullOrEmpty() && !cachedToken.isNullOrEmpty()) {
            _currentUserEmail.value = cachedEmail
            _currentUserDisplayName.value = tokenManager.getCurrentUserDisplayName()
            _isUserLoggedIn.value = true
            fetchProfilesFromApi()
        }
    }

    fun searchContent(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun login(email: String, password: String = "******", onFinished: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val response = app.container.apiService.login(mapOf("email" to email, "password" to password))
                val token = response["token"] as? String ?: ""
                val userId = response["userId"] as? String ?: ""
                tokenManager.saveAccessToken(token)
                tokenManager.saveCurrentUser(email, userId, null)

                _currentUserEmail.value = email
                _isUserLoggedIn.value = true
                _isGoogleAccountBound.value = email.contains("google") || email.contains("gmail")
                
                fetchProfilesFromApi()
                onFinished(true, null)
            } catch (e: Exception) {
                Log.e("DulceVisionVM", "Login network failed, defaulting to local fallback: ${e.message}")
                val errorMsg = e.localizedMessage ?: "Error de comunicación con el Servidor DulceVision"
                
                _currentUserEmail.value = email
                _isUserLoggedIn.value = true
                _isGoogleAccountBound.value = email.contains("google") || email.contains("gmail")
                val existingProfile = _availableProfiles.value.find { it.name.lowercase().contains(email.substringBefore("@").lowercase()) }
                _currentProfile.value = existingProfile ?: _availableProfiles.value.first()
                onFinished(true, "Acceso Local Fallback debido a: $errorMsg")
            }
        }
    }

    fun registerWithCredentials(email: String, fullName: String, password: String = "******", onFinished: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val response = app.container.apiService.register(mapOf("email" to email, "password" to password, "fullName" to fullName))
                val token = response["token"] as? String ?: ""
                val userId = response["userId"] as? String ?: ""
                tokenManager.saveAccessToken(token)
                tokenManager.saveCurrentUser(email, userId, fullName)

                _currentUserEmail.value = email
                _currentUserDisplayName.value = fullName
                _isGoogleAccountBound.value = false
                _isUserLoggedIn.value = true
                
                fetchProfilesFromApi()
                onFinished(true, null)
            } catch (e: Exception) {
                Log.e("DulceVisionVM", "Register network failed, defaulting to local fallback: ${e.message}")
                val errorMsg = e.localizedMessage ?: "Error de conexión al registrarse"
                
                _currentUserEmail.value = email
                _currentUserDisplayName.value = fullName
                _isGoogleAccountBound.value = false
                _isUserLoggedIn.value = true
                
                val newProfileUuid = "prof_dyn_" + System.currentTimeMillis()
                val registerProfile = UserProfile(
                    uuid = newProfileUuid,
                    name = fullName,
                    avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150"
                )
                _availableProfiles.value = _availableProfiles.value + registerProfile
                _currentProfile.value = registerProfile
                onFinished(true, "Registro local Fallback debido a: $errorMsg")
            }
        }
    }

    fun registerAndLoginWithGoogle(email: String, fullName: String, avatarUrl: String, onFinished: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = app.container.apiService.register(mapOf("email" to email, "password" to "google_sso_oauth_verified", "fullName" to fullName))
                val token = response["token"] as? String ?: ""
                val userId = response["userId"] as? String ?: ""
                tokenManager.saveAccessToken(token)
                tokenManager.saveCurrentUser(email, userId, fullName)

                _currentUserEmail.value = email
                _currentUserDisplayName.value = fullName
                _isGoogleAccountBound.value = true
                _isUserLoggedIn.value = true
                
                fetchProfilesFromApi()
                onFinished(true)
            } catch (e: Exception) {
                Log.e("DulceVisionVM", "SSO registration failed, defaulting to local fallback: ${e.message}")
                _currentUserEmail.value = email
                _currentUserDisplayName.value = fullName
                _isGoogleAccountBound.value = true

                val newProfileUuid = "prof_google_" + System.currentTimeMillis()
                val googleProfile = UserProfile(
                    uuid = newProfileUuid,
                    name = fullName,
                    avatarUrl = avatarUrl,
                    isAdult = true
                )
                _availableProfiles.value = _availableProfiles.value + googleProfile
                _currentProfile.value = googleProfile
                _isUserLoggedIn.value = true
                onFinished(true)
            }
        }
    }

    fun createNewProfile(profileName: String, avatarSelection: String, isAdult: Boolean) {
        viewModelScope.launch {
            val newProfile = UserProfile(
                uuid = "prof_custom_" + System.currentTimeMillis(),
                name = profileName,
                avatarUrl = avatarSelection,
                isAdult = isAdult
            )
            try {
                val responseDto = app.container.apiService.createProfile(ProfileDto.fromDomain(newProfile))
                _availableProfiles.value = _availableProfiles.value + responseDto.toDomain()
            } catch (e: Exception) {
                Log.e("DulceVisionVM", "Failed to sync profile creation on remote backend: ${e.message}")
                _availableProfiles.value = _availableProfiles.value + newProfile
            }
        }
    }

    fun editUserProfile(profileId: String, newName: String, newAvatarUrl: String, isAdult: Boolean) {
        viewModelScope.launch {
            val updated = UserProfile(uuid = profileId, name = newName, avatarUrl = newAvatarUrl, isAdult = isAdult)
            try {
                val responseDto = app.container.apiService.updateProfile(profileId, ProfileDto.fromDomain(updated))
                val domainProfile = responseDto.toDomain()
                _availableProfiles.value = _availableProfiles.value.map { if (it.uuid == profileId) domainProfile else it }
                if (_currentProfile.value?.uuid == profileId) {
                    _currentProfile.value = domainProfile
                }
            } catch (e: Exception) {
                Log.e("DulceVisionVM", "Failed to update profile on backend: ${e.message}")
                _availableProfiles.value = _availableProfiles.value.map { if (it.uuid == profileId) updated else it }
                if (_currentProfile.value?.uuid == profileId) {
                    _currentProfile.value = updated
                }
            }
        }
    }

    fun deleteUserProfile(profileId: String) {
        viewModelScope.launch {
            try {
                app.container.apiService.deleteProfile(profileId)
                _availableProfiles.value = _availableProfiles.value.filter { it.uuid != profileId }
                if (_currentProfile.value?.uuid == profileId) {
                    _currentProfile.value = _availableProfiles.value.firstOrNull()
                }
            } catch (e: Exception) {
                Log.e("DulceVisionVM", "Failed to delete profile: ${e.message}")
                _availableProfiles.value = _availableProfiles.value.filter { it.uuid != profileId }
                if (_currentProfile.value?.uuid == profileId) {
                    _currentProfile.value = _availableProfiles.value.firstOrNull()
                }
            }
        }
    }

    fun logout() {
        tokenManager.clearAuth()
        _isUserLoggedIn.value = false
        _currentProfile.value = null
        _currentUserEmail.value = null
        _currentUserDisplayName.value = null
        _isGoogleAccountBound.value = false
    }

    fun selectProfile(profile: UserProfile) {
        _currentProfile.value = profile
    }

    // --- Playback Controls ---
    fun selectMediaToPlay(url: String, title: String, thumbnail: String) {
        _activePlaybackUrl.value = url
        _activePlaybackTitle.value = title
        _activePlaybackThumbnail.value = thumbnail
    }

    fun clearActivePlayback() {
        _activePlaybackUrl.value = null
        _activePlaybackTitle.value = null
        _activePlaybackThumbnail.value = null
    }

    // --- Favorite Management ---
    fun toggleMovieFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavorite(movie.id, "movie", movie.title, movie.thumbnail)
        }
    }

    fun toggleSeriesFavorite(series: Series) {
        viewModelScope.launch {
            repository.toggleFavorite(series.id, "series", series.title, series.thumbnail)
        }
    }

    fun toggleChannelFav(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavoriteChannel(channel.id, channel.isFavorite)
        }
    }

    // --- History Control ---
    fun saveProgress(mediaId: String, mediaType: String, title: String, detailText: String, thumbnail: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.saveWatchProgress(mediaId, mediaType, title, detailText, thumbnail, positionMs, durationMs)
        }
    }

    // --- Realtime WebSocket simulation actions ---
    fun addMovieFromAdminPanel(title: String, videoUrl: String, genre: String, description: String) {
        viewModelScope.launch {
            val newMovie = Movie(
                id = "admin_mov_" + System.currentTimeMillis(),
                title = title,
                thumbnail = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=500",
                backdrop = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800",
                videoUrl = videoUrl,
                duration = "10:00",
                genre = genre,
                year = 2026,
                rating = 9.8,
                description = description,
                isTrend = true,
                isPopular = true
            )
            repository.addNewMovieFromAdmin(newMovie)
        }
    }

    fun addChannelFromAdminPanel(name: String, streamUrl: String, category: String) {
        viewModelScope.launch {
            val newChannel = Channel(
                id = "admin_ch_" + System.currentTimeMillis(),
                name = name,
                logoUrl = "https://images.unsplash.com/photo-1472289065668-ce650ac443d2?w=200",
                streamUrl = streamUrl,
                category = category,
                isFavorite = false,
                epgTitle = "Emisión subida desde Panel de Suministro",
                epgTimeCode = "En directo",
                epgNextTitle = "Siguiente: DulceVision Premiere"
            )
            repository.addNewChannelFromAdmin(newChannel)
        }
    }

    // --- AI Recommendations ---
    fun generateAIRecommendations(userMoodPrompt: String) {
        if (userMoodPrompt.isBlank()) return
        Log.d("DulceVisionVM", "Starting Gemini call with prompt: $userMoodPrompt")
        viewModelScope.launch {
            _aiRecommendationState.value = AIRecommendationState.Loading
            try {
                val results = GeminiClient.getSmartRecommendations(userMoodPrompt)
                _aiRecommendationState.value = AIRecommendationState.Success(results)
            } catch (e: Exception) {
                _aiRecommendationState.value = AIRecommendationState.Error(e.message ?: "Error al llamar a Gemini")
            }
        }
    }

    fun resetAIRecommendationState() {
        _aiRecommendationState.value = AIRecommendationState.Idle
    }

    suspend fun getSeasonsForSeries(seriesId: String): List<Season> {
        return repository.getSeasonsForSeries(seriesId)
    }

    suspend fun getEpisodesForSeason(seasonId: String, seriesId: String): List<Episode> {
        return repository.getEpisodesForSeason(seasonId, seriesId)
    }
}
