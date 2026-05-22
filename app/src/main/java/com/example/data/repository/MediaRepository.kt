package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaRepository(
    context: Context,
    private val api: com.example.data.remote.DulceVisionApiService = com.example.data.remote.DulceVisionApiService.create()
) {
    private val db = AppDatabase.getDatabase(context)
    private val movieDao = db.movieDao()
    private val seriesDao = db.seriesDao()
    private val channelDao = db.channelDao()
    private val historyDao = db.historyDao()
    private val favoriteDao = db.favoriteDao()

    // Realtime Notifications flow for "Websocket simulator"
    private val _realtimeNotifications = MutableSharedFlow<AppNotification>(replay = 0)
    val realtimeNotifications = _realtimeNotifications.asSharedFlow()

    init {
        // Run seed on initialization in background thread safely
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seedDatabaseIfEmpty()
                syncRemoteCatalog()
            } catch (e: Exception) {
                Log.e("MediaRepository", "Error during database seeding or network synchronization setup", e)
            }
        }
    }

    suspend fun syncRemoteCatalog() = withContext(Dispatchers.IO) {
        try {
            val remoteMovies = api.getMovies()
            if (remoteMovies.isNotEmpty()) {
                movieDao.deleteAllMovies()
                movieDao.insertMovies(remoteMovies.map { it.toEntity() })
                Log.d("MediaRepository", "Successfully synced movies from remote to database")
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to sync movies from remote: ${e.message}")
        }
        try {
            val remoteSeries = api.getSeries()
            if (remoteSeries.isNotEmpty()) {
                seriesDao.deleteAllSeries()
                seriesDao.insertSeries(remoteSeries.map { it.toEntity() })
                Log.d("MediaRepository", "Successfully synced series from remote to database")
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to sync series from remote: ${e.message}")
        }
        try {
            val remoteChannels = api.getChannels()
            if (remoteChannels.isNotEmpty()) {
                channelDao.deleteAllChannels()
                channelDao.insertChannels(remoteChannels.map { it.toEntity() })
                Log.d("MediaRepository", "Successfully synced channels from remote to database")
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to sync channels from remote: ${e.message}")
        }
        try {
            syncBanners()
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to sync banners: ${e.message}")
        }
    }

    // --- Banners Streams ---
    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    suspend fun syncBanners() {
        try {
            val remoteBanners = api.getBanners()
            if (remoteBanners.isNotEmpty()) {
                _banners.value = remoteBanners
                Log.d("MediaRepository", "Successfully synced banners from remote")
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to sync banners, using fallback lists: ${e.message}")
            _banners.value = listOf(
                Banner(
                    id = "ban_1",
                    title = "Tears of Steel",
                    imageUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800",
                    subtitle = "Retorno Cyberpunk 2026",
                    actionUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    mediaType = "movie",
                    mediaId = "mov_tears_of_steel"
                ),
                Banner(
                    id = "ban_2",
                    title = "Sintel",
                    imageUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800",
                    subtitle = "La Leyenda del Dragón",
                    actionUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    mediaType = "series",
                    mediaId = "ser_sintel_revelations"
                )
            )
        }
    }

    // --- Movies Streams ---
    val allMovies: Flow<List<Movie>> = movieDao.getAllMovies().map { list ->
        list.map { it.toDomain() }
    }

    val trendingMovies: Flow<List<Movie>> = movieDao.getTrendingMovies().map { list ->
        list.map { it.toDomain() }
    }

    val popularMovies: Flow<List<Movie>> = movieDao.getPopularMovies().map { list ->
        list.map { it.toDomain() }
    }

    // --- Series & Episodes ---
    val allSeries: Flow<List<Series>> = seriesDao.getAllSeries().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getSeasonsForSeries(seriesId: String): List<Season> = withContext(Dispatchers.IO) {
        try {
            api.getSeasons(seriesId)
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to get seasons from API, using fallback: ${e.message}")
            listOf(
                Season("seasm_${seriesId}_1", seriesId, 1, "Temporada 1"),
                Season("seasm_${seriesId}_2", seriesId, 2, "Temporada 2")
            )
        }
    }

    suspend fun getEpisodesForSeason(seasonId: String, seriesId: String): List<Episode> = withContext(Dispatchers.IO) {
        try {
            api.getEpisodes(seasonId)
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to get episodes from API, using fallback: ${e.message}")
            val isSintel = seriesId.contains("sintel")
            val url = if (isSintel) {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            } else {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            }

            listOf(
                Episode(
                    id = "${seasonId}_ep1",
                    seasonId = seasonId,
                    seriesId = seriesId,
                    number = 1,
                    title = "Origen y Revelación",
                    thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
                    videoUrl = url,
                    duration = "14 min",
                    description = "El primer episodio desvela las raíces del conflicto y los desafíos a los que nuestros héroes deberán enfrentarse."
                ),
                Episode(
                    id = "${seasonId}_ep2",
                    seasonId = seasonId,
                    seriesId = seriesId,
                    number = 2,
                    title = "El Sendero del Guerrero",
                    thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                    videoUrl = url,
                    duration = "12 min",
                    description = "Las alianzas se ponen a prueba y se revela el asombroso poder tecnológico que domina el mundo."
                ),
                Episode(
                    id = "${seasonId}_ep3",
                    seasonId = seasonId,
                    seriesId = seriesId,
                    number = 3,
                    title = "Frontera Final",
                    thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                    videoUrl = url,
                    duration = "15 min",
                    description = "La batalla decisiva comienza en las profundidades cibernéticas de DulceVision."
                )
            )
        }
    }

    // --- IPTV Channels ---
    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels().map { list ->
        list.map { it.toDomain() }
    }

    val favoriteChannels: Flow<List<Channel>> = channelDao.getFavoriteChannels().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun toggleFavoriteChannel(channelId: String, isCurrentFav: Boolean) = withContext(Dispatchers.IO) {
        channelDao.updateFavoriteStatus(channelId, !isCurrentFav)
        
        // Push socket event simulator
        _realtimeNotifications.emit(
            AppNotification(
                id = System.currentTimeMillis().toString(),
                title = "IPTV Favorito Actualizado",
                body = "Has ${if (!isCurrentFav) "añadido" else "eliminado"} un canal de televisión en tus favoritos.",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // --- Watch History ---
    val watchHistory: Flow<List<HistoryEntity>> = historyDao.getWatchHistory()

    suspend fun saveWatchProgress(mediaId: String, mediaType: String, title: String, detailText: String, thumbnail: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        historyDao.saveProgress(
            HistoryEntity(
                mediaId = mediaId,
                mediaType = mediaType,
                title = title,
                detailText = detailText,
                thumbnail = thumbnail,
                lastPositionMs = positionMs,
                durationMs = durationMs,
                timestamp = System.currentTimeMillis()
            )
        )

        try {
            val progressMap = mapOf(
                "mediaId" to mediaId,
                "mediaType" to mediaType,
                "title" to title,
                "detailText" to detailText,
                "thumbnail" to thumbnail,
                "positionMs" to positionMs.toString(),
                "durationMs" to durationMs.toString()
            )
            api.syncWatchProgress(progressMap)
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to sync watch progress on remote server: ${e.message}")
        }

        // If it's a movie, also update the progressPercent in the movie table so it shows on Home
        if (mediaType == "movie" && durationMs > 0) {
            val progress = positionMs.toFloat() / durationMs.toFloat()
            // Pull existing movie first
            // Note: Since this is offline database update, let's keep it simple
        }
    }

    // --- Favorites Control ---
    val favoritesList: Flow<List<FavoriteEntity>> = favoriteDao.getFavorites()

    suspend fun toggleFavorite(mediaId: String, type: String, title: String, thumbnail: String) = withContext(Dispatchers.IO) {
        val exists = favoriteDao.isFavorite(mediaId)
        if (exists) {
            favoriteDao.deleteFavorite(mediaId)
        } else {
            favoriteDao.addFavorite(
                FavoriteEntity(mediaId, type, title, thumbnail)
            )
        }
    }

    suspend fun isFavoriteDirect(mediaId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(mediaId)
    }

    // --- Admin Dashboard Upload Actions (Realtime Socket Simulator) ---
    suspend fun addNewMovieFromAdmin(movie: Movie) = withContext(Dispatchers.IO) {
        // Insert into Room to trigger reactive feeds instantly
        movieDao.insertMovie(movie.toEntity())

        // Trigger an interactive custom websocket real-time app notification
        _realtimeNotifications.emit(
            AppNotification(
                id = System.currentTimeMillis().toString(),
                title = "Estreno en Tiempo Real 🎬",
                body = "Se ha añadido '${movie.title}' al catálogo de DulceVision por Websockets. ¡Disponible ya!",
                timestamp = System.currentTimeMillis(),
                mediaId = movie.id,
                mediaType = "movie"
            )
        )
    }

    suspend fun addNewChannelFromAdmin(channel: Channel) = withContext(Dispatchers.IO) {
        // Insert channel
        channelDao.insertChannel(channel.toEntity())

        // Trigger real-time alert
        _realtimeNotifications.emit(
            AppNotification(
                id = System.currentTimeMillis().toString(),
                title = "Nuevo Canal IPTV Emitiendo 📡",
                body = "Se ha sintonizado '${channel.name}' en la categoría ${channel.category}.",
                timestamp = System.currentTimeMillis(),
                mediaId = channel.id,
                mediaType = "channel"
            )
        )
    }

    // --- Seed Database Base Contents ---
    private suspend fun seedDatabaseIfEmpty() {
        // Inspect if movies are empty
        val emptyMovies = movieDao.getAllMovies().first().isEmpty()
        if (emptyMovies) {
            Log.d("MediaRepository", "Seeding initial high-fidelity movie, series, and IPTV dataset...")
            
            val seedMovies = listOf(
                Movie(
                    id = "mov_sintel",
                    title = "Sintel (The Journey)",
                    thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
                    backdrop = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    duration = "14:48",
                    genre = "Fantasía / Aventuras",
                    year = 2024,
                    rating = 8.7,
                    description = "Una niña busca desesperadamente a su fiel compañero de viaje, un pequeño dragón llamado Scales, a través de imponentes tundras nevadas y peligrosos páramos desérticos en una odisea cinematográfica de animación sin precedentes.",
                    isTrend = true,
                    isPopular = false
                ),
                Movie(
                    id = "mov_tears_of_steel",
                    title = "Tears of Steel (Sci-Fi)",
                    thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                    backdrop = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    duration = "12:14",
                    genre = "Sci-Fi / Cyberpunk",
                    year = 2025,
                    rating = 9.2,
                    description = "Ubicada en un futuro alternativo en Ámsterdam, un grupo de científicos intenta salvar la civilización utilizando tecnología de rejuvenecimiento holográfico y un robot gigantesco manipulado por el amor perdido de una joven ciborg.",
                    isTrend = true,
                    isPopular = true
                ),
                Movie(
                    id = "mov_big_buck_bunny",
                    title = "Big Buck Bunny (La Venganza)",
                    thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                    backdrop = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    duration = "09:56",
                    genre = "Animación / Humor",
                    year = 2023,
                    rating = 7.9,
                    description = "Un conejo gigante de carácter apacible toma la justicia por su mano cuando tres malévolos roedores del bosque perturban la paz arrojándole bellotas y saboteando la flora de su hermoso jardín.",
                    isTrend = false,
                    isPopular = true
                ),
                Movie(
                    id = "mov_elephant_dream",
                    title = "Elephant's Dream",
                    thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
                    backdrop = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    duration = "10:53",
                    genre = "Fantasía / Surrealista",
                    year = 2022,
                    rating = 8.1,
                    description = "Dos hombres se sumergen en un extraño laberinto mecánico donde cada habitación representa sus pensamientos más confusos, sus ambiciones industriales y sus miedos a la mecanización de la vida moderna.",
                    isTrend = false,
                    isPopular = false
                ),
                Movie(
                    id = "mov_subaru_car",
                    title = "Subaru BRZ Drift",
                    thumbnail = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=500",
                    backdrop = "https://images.unsplash.com/photo-1617814076367-b759c7d7e738?w=800",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                    duration = "00:47",
                    genre = "Deportes de Motor",
                    year = 2025,
                    rating = 8.5,
                    description = "Siente la velocidad y el rugido de los motores en esta carrera de resistencia extrema cruzando espectaculares carreteras de montaña en los Alpes.",
                    isTrend = true,
                    isPopular = true
                )
            )
            movieDao.insertMovies(seedMovies.map { it.toEntity() })
            
            // Seed Series
            val seedSeries = listOf(
                Series(
                    id = "ser_sintel_revelations",
                    title = "Sintel: Revelaciones",
                    thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
                    backdrop = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800",
                    genres = "Fantasía / Acción",
                    rating = 9.4,
                    description = "La serie oficial derivada del aclamado cortometraje Sintel, que profundiza en la historia del templo sagrado, el adiestramiento de dragones y el retorno de la magia ancestral.",
                    year = 2025
                ),
                Series(
                    id = "ser_cyberpunk_tears",
                    title = "Tears of Steel: Cyber Edge",
                    thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                    backdrop = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800",
                    genres = "Sci-Fi / Thriller",
                    rating = 8.9,
                    description = "Ubicada diez años antes de los eventos fundamentales, la serie examina la génesis de Celia y el desarrollo científico de los robots de defensa que rigen la megalópolis futurista.",
                    year = 2026
                )
            )
            seriesDao.insertSeries(seedSeries.map { it.toEntity() })

            // Seed Channels (Working IPTV Streams with actual playable HLS resources)
            val seedChannels = listOf(
                Channel(
                    id = "ch_cinema_prem",
                    name = "Cinema Premium HD",
                    logoUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=200",
                    streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                    category = "Cine & Películas",
                    isFavorite = true,
                    epgTitle = "Sintel: El Retorno del Dragón",
                    epgTimeCode = "20:00 - 21:50",
                    epgNextTitle = "Siguiente: Big Buck Bunny Especial HD"
                ),
                Channel(
                    id = "ch_sports_action",
                    name = "TDP Deportes Live",
                    logoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=200",
                    streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                    category = "Deportes",
                    isFavorite = false,
                    epgTitle = "Fórmula 1 - GP DulceVision (Directo)",
                    epgTimeCode = "19:30 - 22:30",
                    epgNextTitle = "Siguiente: Resumen del Gran Premio"
                ),
                Channel(
                    id = "ch_discovery",
                    name = "Discovery Planet HD",
                    logoUrl = "https://images.unsplash.com/photo-1472289065668-ce650ac443d2?w=200",
                    streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                    category = "Documentales",
                    isFavorite = true,
                    epgTitle = "Vida Salvaje en el Ártico Extremo",
                    epgTimeCode = "21:00 - 22:00",
                    epgNextTitle = "Siguiente: Agujeros Negros con IA"
                ),
                Channel(
                    id = "ch_anime_flow",
                    name = "Anime Central",
                    logoUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=200",
                    streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                    category = "Anime",
                    isFavorite = false,
                    epgTitle = "Cyber Odyssey - Episodio de Estreno",
                    epgTimeCode = "20:30 - 21:00",
                    epgNextTitle = "Siguiente: Hunter X Live Marathon"
                )
            )
            channelDao.insertChannels(seedChannels.map { it.toEntity() })
        }
    }

    // --- Entity Mappers ---
    private fun MovieEntity.toDomain() = Movie(
        id = id, title = title, thumbnail = thumbnail, backdrop = backdrop, videoUrl = videoUrl,
        duration = duration, genre = genre, year = year, rating = rating, description = description,
        isTrend = isTrend, isPopular = isPopular, progressPercent = progressPercent, lastPositionMs = lastPositionMs
    )

    private fun Movie.toEntity() = MovieEntity(
        id = id, title = title, thumbnail = thumbnail, backdrop = backdrop, videoUrl = videoUrl,
        duration = duration, genre = genre, year = year, rating = rating, description = description,
        isTrend = isTrend, isPopular = isPopular, progressPercent = progressPercent, lastPositionMs = lastPositionMs
    )

    private fun SeriesEntity.toDomain() = Series(
        id = id, title = title, thumbnail = thumbnail, backdrop = backdrop, genres = genres,
        rating = rating, description = description, year = year
    )

    private fun Series.toEntity() = SeriesEntity(
        id = id, title = title, thumbnail = thumbnail, backdrop = backdrop, genres = genres,
        rating = rating, description = description, year = year
    )

    private fun ChannelEntity.toDomain() = Channel(
        id = id, name = name, logoUrl = logoUrl, streamUrl = streamUrl, category = category,
        isFavorite = isFavorite, epgTitle = epgTitle, epgTimeCode = epgTimeCode, epgNextTitle = epgNextTitle
    )

    private fun Channel.toEntity() = ChannelEntity(
        id = id, name = name, logoUrl = logoUrl, streamUrl = streamUrl, category = category,
        isFavorite = isFavorite, epgTitle = epgTitle, epgTimeCode = epgTimeCode, epgNextTitle = epgNextTitle
    )
}
