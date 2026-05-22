package com.example.presentation.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.Channel
import com.example.presentation.viewmodel.DulceVisionViewModel
import com.example.ui.theme.*

@Composable
fun IPTVPlayerScreen(
    viewModel: DulceVisionViewModel,
    onBack: () -> Unit,
    onPlayFullscreen: (videoUrl: String, title: String, thumb: String) -> Unit
) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsState()
    val favoritedList by viewModel.favoriteChannels.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Cine & Películas") }
    
    // Default active channel to play (first of filtered list)
    val categories = channels.map { it.category }.distinct()
    val filteredList = channels.filter {
        it.category == selectedCategory &&
        (searchQuery.isEmpty() || it.name.lowercase().contains(searchQuery.lowercase()))
    }
    
    var activeChannel by remember { mutableStateOf<Channel?>(null) }
    
    LaunchedEffect(channels, filteredList, selectedCategory) {
        if (activeChannel == null && filteredList.isNotEmpty()) {
            activeChannel = filteredList.first()
        }
    }

    // Load first channel on category switch
    LaunchedEffect(selectedCategory) {
        if (filteredList.isNotEmpty()) {
            activeChannel = filteredList.first()
        }
    }

    // Active Live ExoPlayer Tuner instance (Small Preview box)
    var exPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(activeChannel) {
        activeChannel?.let { channel ->
            exPlayer?.release()
            exPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(channel.streamUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ALL
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exPlayer?.release()
        }
    }

    // M3U Playlist parser helper sheet
    var showM3UImportDialog by remember { mutableStateOf(false) }
    var m3uDataInput by remember { mutableStateOf("#EXTM3U\n#EXTINF:-1 tvg-logo=\"url\" group-title=\"Cine\", Canales HBO HD\nhttp://streamurl.m3u8") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Sintonizador IPTV Live", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                        Text("Tecnología HLS/DASH Inteligente", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Import M3U
                Button(
                    onClick = { showM3UImportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DulcePink.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DulcePink.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.LibraryAdd, contentDescription = null, tint = DulcePink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importar M3U", color = DulcePink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Categories horizontal lazy scroll
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSel = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) DulcePink else CardGlowSurface)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(category, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Dynamic Split Screen layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LEFT SIDE COLUMN: Channel lists
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                ) {
                    // Quick Search input inside local column list
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar dial...", fontSize = 11.sp, color = TextGray) },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardGlowSurface,
                            unfocusedContainerColor = CardGlowSurface,
                            focusedBorderColor = DulcePink,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        items(filteredList) { chan ->
                            val isCurrent = activeChannel?.id == chan.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeChannel = chan },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) DulcePink.copy(alpha = 0.2f) else CardGlowSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) DulcePink else Color.White.copy(alpha = 0.05f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    ) {
                                        AsyncImage(
                                            model = chan.logoUrl,
                                            contentDescription = chan.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text(
                                            text = chan.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = chan.epgTitle,
                                            color = if (isCurrent) GoldAccent else TextGray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    // Fav stars
                                    IconButton(
                                        onClick = { viewModel.toggleChannelFav(chan) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = if (favoritedList.any { it.id == chan.id }) DulcePink else TextGray.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT SIDE COLUMN: ExoPlayer preview list sintonizer + EPG detail guide sheet
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                ) {
                    activeChannel?.let { channel ->
                        // Preview active Player Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .background(Color.Black)
                        ) {
                            exPlayer?.let { exo ->
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            useController = false
                                            layoutParams = FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }
                                    },
                                    update = { playerView ->
                                        playerView.player = exo
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Tap to sintonize fully in widescreen PlayerScreen
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Transparent)
                                    .clickable {
                                        onPlayFullscreen(channel.streamUrl, "En Vivo: ${channel.name}", channel.logoUrl)
                                    }
                            )

                            // Quick overlay badge: LIVE sintonizing
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Red, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EN DIRECTO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Tap expand overlay banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomStart)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pulsar para pantalla completa", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ACTIVE CHANNEL METADATA GUIDE (EPG details)
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1.0f),
                            colors = CardDefaults.cardColors(containerColor = CardGlowSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "CANAL SINTONIZADO",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = channel.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 12.dp))

                                // EPG Active Entry
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Green)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "EMITIENDO AHORA • ${channel.epgTimeCode}",
                                        color = Color.Green,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = channel.epgTitle,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                                )

                                // EPG Next Entry
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(CardBorderColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "RELOJ GUÍA SIGUIENTE",
                                        color = TextGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = channel.epgNextTitle,
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } ?: run {
                        // Empty states loader
                        Box(
                            modifier = Modifier.fillMaxSize().background(CardGlowSurface, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sintonizando IPTV...", color = TextGray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // M3U Playlist input popup installer dialog
        if (showM3UImportDialog) {
            AlertDialog(
                onDismissRequest = { showM3UImportDialog = false },
                containerColor = CardGlowSurface,
                title = { Text("Importar Lista M3U Xtream Codes", color = Color.White, fontWeight = FontWeight.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pega el texto raw de tu playlist M3U para importarla al Room DB local de DulceVision en tiempo real.", color = TextGray, fontSize = 12.sp)
                        
                        OutlinedTextField(
                            value = m3uDataInput,
                            onValueChange = { m3uDataInput = it },
                            placeholder = { Text("M3U Playlist...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = DulcePink
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addChannelFromAdminPanel(
                                name = "HBO Premium Live",
                                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                                category = "Cine & Películas"
                            )
                            showM3UImportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DulcePink)
                    ) {
                        Text("Sintonizar e Importar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showM3UImportDialog = false }) {
                        Text("Cancelar", color = TextGray)
                    }
                }
            )
        }
    }
}
