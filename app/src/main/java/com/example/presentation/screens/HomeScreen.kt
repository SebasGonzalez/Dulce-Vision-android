package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Movie
import com.example.data.model.Series
import com.example.data.model.Channel
import com.example.presentation.viewmodel.AIRecommendationState
import com.example.presentation.viewmodel.DulceVisionViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: DulceVisionViewModel,
    onNavigateToPlayer: (videoUrl: String, title: String, thumb: String) -> Unit,
    onNavigateToDetail: (mediaId: String, isSeries: Boolean) -> Unit,
    onNavigateToIPTV: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val movies by viewModel.movies.collectAsState()
    val banners by viewModel.banners.collectAsState()
    val trending by viewModel.trendingMovies.collectAsState()
    val popular by viewModel.popularMovies.collectAsState()
    val seriesList by viewModel.series.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val profile by viewModel.currentProfile.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val aiState by viewModel.aiRecommendationState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var aiPromptInput by remember { mutableStateOf("") }

    // Floating websocket notifications
    val recentNotify = viewModel.recentSystemAlert.collectAsState(initial = null)
    var activeToastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recentNotify.value) {
        recentNotify.value?.let {
            activeToastMessage = "${it.title}: ${it.body}"
            delay(5000)
            activeToastMessage = null
        }
    }

    // Categories filter pills
    val categories = listOf("Todos", "Cine & VOD", "Series", "IPTV Live Channels", "Favoritos")
    var selectedCategory by remember { mutableStateOf("Todos") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Main Scroll Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Customized Unified App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DulceVision • En Vivo",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "Hola, ${profile?.name ?: "Usuario"}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Admin Quick-Launch Button (Real-time Websocket Center)
                    IconButton(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(DulcePink.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, DulcePink.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = "Admin Node Sync",
                            tint = DulcePink
                        )
                    }

                    // User profile avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(2.dp, DulcePink, CircleShape)
                            .clickable { viewModel.logout() }
                    ) {
                        AsyncImage(
                            model = profile?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar películas, canales IPTV, series...", color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextGray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardGlowSurface,
                    unfocusedContainerColor = CardGlowSurface.copy(alpha = 0.5f),
                    focusedBorderColor = DulcePink,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category filter rows
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                if (isSelected) Brush.linearGradient(listOf(DulcePink, DulceOrange))
                                else Brush.linearGradient(listOf(CardGlowSurface, CardGlowSurface))
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else TextGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // FILTER CONTENT LOGIC BASED ON CATEGORY SELECTION & SEARCH
            val filteredMovies = movies.filter {
                (searchQuery.isEmpty() || it.title.lowercase().contains(searchQuery.lowercase())) &&
                (selectedCategory == "Todos" || selectedCategory == "Cine & VOD" || (selectedCategory == "Favoritos" && favorites.any { f -> f.id == it.id }))
            }

            val filteredSeries = seriesList.filter {
                (searchQuery.isEmpty() || it.title.lowercase().contains(searchQuery.lowercase())) &&
                (selectedCategory == "Todos" || selectedCategory == "Series" || (selectedCategory == "Favoritos" && favorites.any { f -> f.id == it.id }))
            }

            val filteredChannels = channels.filter {
                (searchQuery.isEmpty() || it.name.lowercase().contains(searchQuery.lowercase())) &&
                (selectedCategory == "Todos" || selectedCategory == "IPTV Live Channels" || (selectedCategory == "Favoritos" && favorites.any { f -> f.id == it.id }))
            }

            if (selectedCategory == "Todos" && searchQuery.isEmpty()) {
                // 2. High-Fidelity HERO VOD Banner (First movie of trends, or loaded dynamically from API banners list!)
                val heroBanner = banners.firstOrNull()
                if (heroBanner != null) {
                    val fallbackMovie = Movie(
                        id = heroBanner.mediaId,
                        title = heroBanner.title,
                        thumbnail = heroBanner.imageUrl,
                        backdrop = heroBanner.imageUrl,
                        videoUrl = heroBanner.actionUrl,
                        duration = "02:00",
                        genre = heroBanner.subtitle,
                        year = 2026,
                        rating = 9.5,
                        description = "Cargado directamente vía API Banners desde el Servidor DulceVision."
                    )
                    val movie = fallbackMovie
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    ) {
                        AsyncImage(
                            model = movie.backdrop,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Ambient Fade Gradients
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.7f), ObsidianBg),
                                        startY = 100f
                                    )
                                )
                        )

                        // Meta details
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "RECOMENDADO",
                                    color = ObsidianBg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .background(GoldAccent, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                                Text(
                                    text = "• ${movie.genre}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = movie.description,
                                color = TextGray,
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToPlayer(movie.videoUrl, movie.title, movie.thumbnail) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reproducir", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onNavigateToDetail(movie.id, heroBanner.mediaType == "series") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Detalles", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    val heroMovie = trending.firstOrNull() ?: movies.firstOrNull()
                    heroMovie?.let { movie ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    ) {
                        AsyncImage(
                            model = movie.backdrop,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Ambient Fade Gradients
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.7f), ObsidianBg),
                                        startY = 100f
                                    )
                                )
                        )

                        // Meta details
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "ESTRENO EXCLUSIVO",
                                    color = ObsidianBg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .background(GoldAccent, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                                Text(
                                    text = "• ${movie.genre}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = movie.description,
                                color = TextGray,
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToPlayer(movie.videoUrl, movie.title, movie.thumbnail) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reproducir", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onNavigateToDetail(movie.id, false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Detalles", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Continue Watching (Incremental Watch Progress Cache)
                if (watchHistory.isNotEmpty()) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = "Continuar Viendo",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(watchHistory) { item ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clickable { onNavigateToPlayer(item.mediaId, item.title, item.thumbnail) }
                                ) {
                                    Box(modifier = Modifier.height(125.dp)) {
                                        AsyncImage(
                                            model = item.thumbnail,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Glow Play Icon Overlay
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.PlayCircleFilled,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        // Progress Bar
                                        val percent = if (item.durationMs > 0) item.lastPositionMs.toFloat() / item.durationMs.toFloat() else 0f
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .align(Alignment.BottomStart)
                                                .background(Color.White.copy(alpha = 0.3f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(percent)
                                                    .fillMaxHeight()
                                                    .background(DulcePink)
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CardGlowSurface)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = item.detailText,
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. GEMINI IA SMART ADVICE WIDGET (Breathtaking custom cards)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CyberPurple, DulcePink)
                            )
                        )
                        .padding(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardGlowSurface, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Asistente de Recomendación IA",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Dime tu estado de ánimo o género de interés y Gemini buscará recomendaciones de streaming personalizadas y canales IPTV alineados en segundos.",
                            color = TextGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = aiPromptInput,
                                onValueChange = { aiPromptInput = it },
                                placeholder = { Text("Quiero ver ciencia ficción futurista...", fontSize = 12.sp, color = TextGray) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ObsidianBg,
                                    unfocusedContainerColor = ObsidianBg,
                                    focusedBorderColor = DulcePink,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1.0f)
                                    .padding(end = 8.dp)
                            )

                            Button(
                                onClick = {
                                    viewModel.generateAIRecommendations(aiPromptInput)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DulcePink)
                            ) {
                                Text("Preguntar IA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Response states
                        Spacer(modifier = Modifier.height(10.dp))
                        when (val st = aiState) {
                            is AIRecommendationState.Loading -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DulcePink, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gemini analizando el catálogo de DulceVision...", color = TextGray, fontSize = 12.sp)
                                }
                            }
                            is AIRecommendationState.Success -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ObsidianBg)
                                        .padding(12.dp)
                                ) {
                                    Text("Sugerencias Mágicas de IA:", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    st.recommendations.forEach { title ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // Quick search click
                                                    searchQuery = title
                                                }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.MovieFilter, contentDescription = null, tint = DulcePink, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                            is AIRecommendationState.Error -> {
                                Text("Error: ${st.message}", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                            }
                            else -> {}
                        }
                    }
                }

                // 5. Tendencias (Movies Card Lists)
                MediaSection(
                    title = "Tendencias Mundiales",
                    mediaItems = trending,
                    onItemClick = { onNavigateToDetail(it.id, false) }
                )

                // 6. IPTV Quick Sintonizer Row
                IPTVQuickSection(
                    channels = channels,
                    onChannelClick = { onNavigateToIPTV() }
                )

                // 7. Series Populares Section
                SeriesSection(
                    title = "Series de Éxito en DulceVision",
                    seriesList = seriesList,
                    onItemClick = { onNavigateToDetail(it.id, true) }
                )

                // 8. Top 10 list
                TopTenSection(
                    movies = popular,
                    onItemClick = { onNavigateToDetail(it.id, false) }
                )
            } else {
                // RENDER SEARCH / FILTER RESULTS DIRECTLY
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Resultados encontrados para '$selectedCategory'",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (filteredMovies.isEmpty() && filteredSeries.isEmpty() && filteredChannels.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                            Text("Oops, no encontramos contenido acorde.", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    } else {
                        if (filteredMovies.isNotEmpty()) {
                            Text("Películas", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            filteredMovies.forEach { movie ->
                                DetailedMediaRow(
                                    title = movie.title,
                                    subtitle = "${movie.genre} • ${movie.year}",
                                    imageUrl = movie.thumbnail,
                                    onClick = { onNavigateToDetail(movie.id, false) }
                                )
                            }
                        }

                        if (filteredSeries.isNotEmpty()) {
                            Text("Series", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            filteredSeries.forEach { ser ->
                                DetailedMediaRow(
                                    title = ser.title,
                                    subtitle = "${ser.genres} • ${ser.year}",
                                    imageUrl = ser.thumbnail,
                                    onClick = { onNavigateToDetail(ser.id, true) }
                                )
                            }
                        }

                        if (filteredChannels.isNotEmpty()) {
                            Text("TV en Vivo IPTV", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                            filteredChannels.forEach { item ->
                                DetailedMediaRow(
                                    title = item.name,
                                    subtitle = "Categoría: ${item.category}",
                                    imageUrl = item.logoUrl,
                                    onClick = {
                                        viewModel.selectMediaToPlay(item.streamUrl, item.name, item.logoUrl)
                                        onNavigateToIPTV()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // FLOATING WEBSOCKETS TOAST GADGET ALERT
        AnimatedVisibility(
            visible = activeToastMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 20.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(DulcePink, DulceOrange)))
                    .padding(16.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudSync,
                        contentDescription = "WebSocket Push",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = activeToastMessage ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun MediaSection(
    title: String,
    mediaItems: List<Movie>,
    onItemClick: (Movie) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mediaItems) { movie ->
                Column(
                    modifier = Modifier
                        .width(130.dp)
                        .clickable { onItemClick(movie) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = movie.thumbnail,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Rating pill top-left
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "★ ${movie.rating}",
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = movie.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "${movie.genre} • ${movie.year}",
                        color = TextGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesSection(
    title: String,
    seriesList: List<Series>,
    onItemClick: (Series) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(seriesList) { series ->
                Column(
                    modifier = Modifier
                        .width(135.dp)
                        .clickable { onItemClick(series) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = series.thumbnail,
                            contentDescription = series.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = series.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "${series.genres} • ${series.year}",
                        color = TextGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun IPTVQuickSection(
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tuner IPTV En Vivo HLS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ver Guía EPG",
                color = DulcePink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onChannelClick(channels.first()) }
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(channels) { channel ->
                Card(
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { onChannelClick(channel) },
                    colors = CardDefaults.cardColors(containerColor = CardGlowSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = channel.logoUrl,
                                    contentDescription = channel.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = channel.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = channel.epgTitle,
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = channel.category,
                            color = TextGray,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopTenSection(
    movies: List<Movie>,
    onItemClick: (Movie) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "Las 10 Más Vistas Hoy",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(movies.take(10)) { index, movie ->
                Box(
                    modifier = Modifier
                        .clickable { onItemClick(movie) }
                        .width(165.dp)
                        .height(160.dp)
                ) {
                    // Jumbo index back number
                    Text(
                        text = "${index + 1}",
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.07f),
                        modifier = Modifier.align(Alignment.BottomStart)
                    )

                    // Card foreground offset
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(105.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = movie.thumbnail,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedMediaRow(
    title: String,
    subtitle: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardGlowSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray)
        }
    }
}
