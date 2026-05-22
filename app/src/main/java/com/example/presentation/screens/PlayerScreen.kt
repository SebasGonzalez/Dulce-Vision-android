package com.example.presentation.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.presentation.viewmodel.DulceVisionViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    videoUrl: String,
    title: String,
    thumbnail: String,
    viewModel: DulceVisionViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // ExoPlayer Instance life-cycle
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // Playback state parameters
    var isPlaying by remember { mutableStateOf(true) }
    var playbackProgress by remember { mutableStateOf(0f) }
    var videoDurationMs by remember { mutableStateOf(0L) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var showPlayerControls by remember { mutableStateOf(true) }

    // Selected Speed Control
    var selectedSpeed by remember { mutableStateOf(1.0f) }
    val playbackSpeeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Multi-audio mockup
    var audioTrackName by remember { mutableStateOf("Español Latino (Estéreo)") }
    var showAudioDialog by remember { mutableStateOf(false) }

    // Gesture status overlays
    var gestureStatusOverlayText by remember { mutableStateOf<String?>(null) }
    var showOverlayTimer by remember { mutableStateOf(0) }

    // Manage volume & brightness gestures
    var maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume = remember { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) }
    var currentBrightness = remember { 0.5f } // Mid light

    // Fade out automatic timer for player HUD
    LaunchedEffect(showPlayerControls) {
        if (showPlayerControls) {
            delay(5000)
            showPlayerControls = false
        }
    }

    // Progress poller stream
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition
            videoDurationMs = exoPlayer.duration
            if (videoDurationMs > 0) {
                playbackProgress = currentPositionMs.toFloat() / videoDurationMs.toFloat()
            }
            delay(1000)
        }
    }

    // ExoPlayer State changes listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                isPlaying = exoPlayer.isPlaying
                if (playbackState == Player.STATE_READY) {
                    videoDurationMs = exoPlayer.duration
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            // Unify playback progress and commit dynamically to history Room DB
            if (videoDurationMs > 0) {
                viewModel.saveProgress(
                    mediaId = videoUrl,
                    mediaType = "movie",
                    title = title,
                    detailText = "Proceso de reproducción sintonizado",
                    thumbnail = thumbnail,
                    positionMs = currentPositionMs,
                    durationMs = videoDurationMs
                )
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Brightness/Volume swipe gesture dismiss timer
    LaunchedEffect(showOverlayTimer) {
        if (showOverlayTimer > 0) {
            delay(1500)
            gestureStatusOverlayText = null
            showOverlayTimer = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Gesture Swipe tracking
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { showPlayerControls = true },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val isLeftSide = change.position.x < size.width / 2

                        if (isLeftSide) {
                            // Adjust simulated Brightness state
                            currentBrightness = (currentBrightness - (dragAmount.y / 800f)).coerceIn(0.1f, 1.0f)
                            gestureStatusOverlayText = "Brillo: ${(currentBrightness * 100).toInt()}%"
                            showOverlayTimer++
                        } else {
                            // Adjust audio system volume
                            currentVolume = (currentVolume - (dragAmount.y / 150f).toInt()).coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                            gestureStatusOverlayText = "Volumen: ${(currentVolume * 100 / maxVolume)}%"
                            showOverlayTimer++
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showPlayerControls = !showPlayerControls
            }
    ) {
        // ExoPlayer Canvas Surface Frame Rendering
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
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        // SCREEN DIMMER OVERLAY (linked back to hand gesture slide parameters)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 1.0f - currentBrightness))
        )

        // 1. Elegant Buffering Loading Animation
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DulcePink, strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sinto-Buffer inteligente...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Brightness/Volume level Toast HUD Widget
        AnimatedVisibility(
            visible = gestureStatusOverlayText != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = gestureStatusOverlayText ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 3. ADVANCED HUD PLAYER CONTROLS (Dynamic show/hide toggle)
        AnimatedVisibility(
            visible = showPlayerControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -50 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -50 })
        ) {
            // Gradient Overlay backgrounds
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            ) {
                // Top Header Overlap Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retroceder", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "DulceVision Player UltraHD HLS",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quality & Audio selectors
                    IconButton(onClick = { showAudioDialog = true }) {
                        Icon(Icons.Default.Audiotrack, contentDescription = "Audio track", tint = Color.White)
                    }

                    IconButton(onClick = { showSpeedDialog = true }) {
                        Icon(Icons.Default.Speed, contentDescription = "Playback speed", tint = Color.White)
                    }
                }

                // Middle Media controls (Jump backwards/forwards 10s, Play/Pause)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    // Back 10 Sec
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier.size(54.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Atrás 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    // Main controller trigger
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                            isPlaying = !isPlaying
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(DulcePink, CircleShape)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Reproducción",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Forward 10 Sec
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(videoDurationMs)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier.size(54.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Adelante 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // BOTTOM PROGRESS POSITION TRACK BAR HUD
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimeCode(currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = formatTimeCode(videoDurationMs),
                            color = TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Custom Slider indicator track
                    Slider(
                        value = playbackProgress,
                        onValueChange = { percent ->
                            playbackProgress = percent
                            val newSeek = (percent * videoDurationMs).toLong()
                            exoPlayer.seekTo(newSeek)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = DulcePink,
                            activeTrackColor = DulcePink,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("video_position_slider")
                    )
                }
            }
        }

        // Audio track selector dialog sheet
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                containerColor = CardGlowSurface,
                title = { Text("Canales de Audio / Subs", color = Color.White, fontWeight = FontWeight.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Español Latino (Estéreo) [Predeterminado]", "English 5.1 (Surround)", "Subtítulos: Español Castellano", "Subtítulos: Desactivados").forEach { track ->
                            val isSelected = audioTrackName.contains(track) || (track.contains("Desactivados") && !audioTrackName.contains("Subs"))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DulcePink else ObsidianBg)
                                    .clickable {
                                        if (track.contains("Audio") || track.contains("Español") || track.contains("English")) {
                                            audioTrackName = track
                                        }
                                        showAudioDialog = false
                                        gestureStatusOverlayText = "Canal de Audio: $track"
                                        showOverlayTimer++
                                    }
                                    .padding(14.dp)
                            ) {
                                Text(track, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDialog = false }) {
                        Text("Cerrar", color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Playback speed selector dialog sheet
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                containerColor = CardGlowSurface,
                title = { Text("Velocidad de Reproducción", color = Color.White, fontWeight = FontWeight.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        playbackSpeeds.forEach { speed ->
                            val active = selectedSpeed == speed
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) DulcePink else ObsidianBg)
                                    .clickable {
                                        selectedSpeed = speed
                                        exoPlayer.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                        gestureStatusOverlayText = "Velocidad: ${speed}x"
                                        showOverlayTimer++
                                    }
                                    .padding(14.dp)
                            ) {
                                Text("${speed}x ${if (speed == 1.0f) "(Normal)" else ""}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Cerrar", color = GoldAccent)
                    }
                }
            )
        }
    }
}

// Convert Ms to human readable 00:00 - 00:00 format
private fun formatTimeCode(mSec: Long): String {
    val totalSeconds = mSec / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
