package com.example.presentation.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.presentation.viewmodel.DulceVisionViewModel
import com.example.ui.theme.*

@Composable
fun VODDetailScreen(
    mediaId: String,
    isSeries: Boolean,
    viewModel: DulceVisionViewModel,
    onBack: () -> Unit,
    onPlayMedia: (videoUrl: String, title: String, thumb: String) -> Unit
) {
    val movies by viewModel.movies.collectAsState()
    val seriesList by viewModel.series.collectAsState()
    val favoritesList by viewModel.favorites.collectAsState()

    var selectedSeasonIndex by remember { mutableStateOf(0) }
    var seasons by remember { mutableStateOf<List<Season>>(emptyList()) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }

    val isFavorite = favoritesList.any { it.id == mediaId }

    val scope = rememberCoroutineScope()

    // 1. Resolve media entity
    val movie: Movie? = if (!isSeries) movies.find { it.id == mediaId } else null
    val targetSeries: Series? = if (isSeries) seriesList.find { it.id == mediaId } else null

    val title = movie?.title ?: targetSeries?.title ?: "Cargando..."
    val thumbnail = movie?.thumbnail ?: targetSeries?.thumbnail ?: ""
    val backdrop = movie?.backdrop ?: targetSeries?.backdrop ?: ""
    val rating = movie?.rating ?: targetSeries?.rating ?: 0.0
    val genres = movie?.genre ?: targetSeries?.genres ?: ""
    val year = movie?.year ?: targetSeries?.year ?: 2026
    val desc = movie?.description ?: targetSeries?.description ?: ""

    // Fetch episodes if it's a TV Series from backend nodes
    LaunchedEffect(mediaId, isSeries) {
        if (isSeries) {
            seasons = viewModel.getSeasonsForSeries(mediaId)
            if (seasons.isNotEmpty()) {
                episodes = viewModel.getEpisodesForSeason(seasons.first().id, mediaId)
            }
        }
    }

    // Toggle episodes based on season click
    LaunchedEffect(selectedSeasonIndex) {
        if (seasons.isNotEmpty() && selectedSeasonIndex in seasons.indices) {
            episodes = viewModel.getEpisodesForSeason(seasons[selectedSeasonIndex].id, mediaId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Backdrop Header banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = backdrop,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Soft vignette gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.5f), ObsidianBg)
                            )
                        )
                )

                // Floating Close/Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 48.dp, start = 16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }

                // Floating Favorite Toggle Button
                IconButton(
                    onClick = {
                        if (isSeries) {
                            targetSeries?.let { viewModel.toggleSeriesFavorite(it) }
                        } else {
                            movie?.let { viewModel.toggleMovieFavorite(it) }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) DulcePink else Color.White
                    )
                }
            }

            // Detailed Content Panel description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )

                // Media tags
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Valoración",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$rating",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "$year",
                        color = TextGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = genres,
                        color = TextGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "UltraHD 4K",
                        color = GoldAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Description
                Text(
                    text = desc,
                    color = TextGray,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // VOD / SERIES ACTIONS PANEL
                if (!isSeries) {
                    // Movie is simple reproducer button
                    Button(
                        onClick = {
                            movie?.let { onPlayMedia(it.videoUrl, it.title, it.thumbnail) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DulcePink),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Película Ahora", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    // Series listing seasons & episodes
                    Text(
                        text = "Temporadas",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        seasons.forEachIndexed { idx, s ->
                            val active = selectedSeasonIndex == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) DulcePink else CardGlowSurface)
                                    .border(1.dp, if (active) Color.Transparent else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .clickable { selectedSeasonIndex = idx }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = s.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Episodes Column List
                    Text(
                        text = "Episodios (${episodes.size})",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    episodes.forEach { episode ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    onPlayMedia(episode.videoUrl, "DV Series: ${title} - ${episode.title}", episode.thumbnail)
                                },
                            colors = CardDefaults.cardColors(containerColor = CardGlowSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = episode.thumbnail,
                                        contentDescription = episode.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "${episode.number}. ${episode.title}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = episode.duration,
                                        color = GoldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = episode.description,
                                        color = TextGray,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Cast list labels
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Protagonistas",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    val cast = listOf(
                        "Ton Roosendaal" to "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                        "Blender Foundation" to "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=100",
                        "Alia Cyberpunk" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100"
                    )

                    cast.forEach { (name, url) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(80.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = name,
                                color = TextGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// Inline mock episode returns matching season indexing
private fun getMockEpisodes(seasonId: String, seriesId: String): List<Episode> {
    val isSintel = seriesId.contains("sintel")
    val url = if (isSintel) {
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
    } else {
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    }

    return listOf(
        Episode(
            id = "${seasonId}_ep1",
            seasonId = seasonId,
            seriesId = seriesId,
            number = 1,
            title = "La Caída del Reino Sagrado",
            thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
            videoUrl = url,
            duration = "14:48 min",
            description = "Un viaje audaz que sacudirá los cimientos del antiguo dragón Scales."
        ),
        Episode(
            id = "${seasonId}_ep2",
            seasonId = seasonId,
            seriesId = seriesId,
            number = 2,
            title = "Senda Cibernética",
            thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
            videoUrl = url,
            duration = "12:14 min",
            description = "Un laberinto tecnológico repleto de peligros y revelaciones sobre la vida de Celia."
        ),
        Episode(
            id = "${seasonId}_ep3",
            seasonId = seasonId,
            seriesId = seriesId,
            number = 3,
            title = "Retorno de la Magia",
            thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
            videoUrl = url,
            duration = "09:56 min",
            description = "Especial decisivo donde convergen el poder, amor y tecnología cyberpunk."
        )
    )
}
