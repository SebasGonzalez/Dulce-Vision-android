package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isTrend = 1")
    fun getTrendingMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isPopular = 1")
    fun getPopularMovies(): Flow<List<MovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Query("DELETE FROM movies")
    suspend fun deleteAllMovies()
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series")
    fun getAllSeries(): Flow<List<SeriesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(seriesList: List<SeriesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleSeries(series: SeriesEntity)

    @Query("DELETE FROM series")
    suspend fun deleteAllSeries()
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY category ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET isFavorite = :isFav WHERE id = :channelId")
    suspend fun updateFavoriteStatus(channelId: String, isFav: Boolean)

    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC")
    fun getWatchHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(history: HistoryEntity)

    @Query("DELETE FROM watch_history WHERE mediaId = :mediaId")
    suspend fun deleteProgress(mediaId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavorite(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean
}
