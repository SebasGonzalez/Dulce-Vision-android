package com.example.presentation.screens

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.DulceVisionViewModel
import com.example.ui.theme.*

@Composable
fun AdminPanelSessionScreen(
    viewModel: DulceVisionViewModel,
    onBack: () -> Unit
) {
    var activeAdminTab by remember { mutableStateOf("VOD") }

    // Upload movie forms
    var movieTitle by remember { mutableStateOf("Sintel: Odisea de Héroes") }
    var movieVideoUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4") }
    var movieGenre by remember { mutableStateOf("Fantasía / Aventuras") }
    var movieDesc by remember { mutableStateOf("Adéntrate más en el universo sagrado de Sintel con este corte especial del director subido vía Websockets.") }

    // Upload channel forms
    var chanName by remember { mutableStateOf("DulceVision Prime") }
    var chanStreamUrl by remember { mutableStateOf("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8") }
    var chanCategory by remember { mutableStateOf("Cine & Películas") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retroceder", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Panel Realtime Admin", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text("Puntos de Enlace WebSockets & REST", color = DulcePink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Realtime metrics indicators cards (Visually plot connection indexes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Online users
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardGlowSurface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Usuarios Online", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("3,481", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
                    }
                }

                // Node server status
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardGlowSurface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = DulcePink, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gateway Socket.io", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Activo (12ms)", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            // Tabs Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardGlowSurface)
                    .padding(4.dp)
            ) {
                listOf("VOD" to "Subir VODs", "IPTV" to "Agregar IPTV Channels").forEach { (tab, text) ->
                    val active = activeAdminTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) DulcePink else Color.Transparent)
                            .clickable { activeAdminTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tab Panels
            if (activeAdminTab == "VOD") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardGlowSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Publicar Película o Episodio",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = movieTitle,
                        onValueChange = { movieTitle = it },
                        label = { Text("Título de Película", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DulcePink
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_title_input")
                    )

                    OutlinedTextField(
                        value = movieVideoUrl,
                        onValueChange = { movieVideoUrl = it },
                        label = { Text("Enlace del Video Stream (HLS MP4 Url)", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DulcePink
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_video_url_input")
                    )

                    OutlinedTextField(
                        value = movieGenre,
                        onValueChange = { movieGenre = it },
                        label = { Text("Género / Categoría", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DulcePink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = movieDesc,
                        onValueChange = { movieDesc = it },
                        label = { Text("Breve Sinopsis Informativa", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DulcePink
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.addMovieFromAdminPanel(
                                title = movieTitle,
                                videoUrl = movieVideoUrl,
                                genre = movieGenre,
                                description = movieDesc
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DulcePink),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("admin_submit_movie_button")
                    ) {
                        Icon(Icons.Default.Power, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simular Push WebSocket real-time VOD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardGlowSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Agregar Canal IPTV al Tuner",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = chanName,
                        onValueChange = { chanName = it },
                        label = { Text("Nombre del Dial Live", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DulcePink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = chanStreamUrl,
                        onValueChange = { chanStreamUrl = it },
                        label = { Text("Stream HLS / RTSP (.m3u8) Url", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DulcePink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = chanCategory,
                        onValueChange = { chanCategory = it },
                        label = { Text("Categoría Temática", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DulcePink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.addChannelFromAdminPanel(
                                name = chanName,
                                streamUrl = chanStreamUrl,
                                category = chanCategory
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DulceOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Dvr, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publicar Canal vía Sockets", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Informational tip notice
            Text(
                text = "Nota: Al pulsar el botón, se inyectará una señal simulando un Socket Event hacia el App Database. Verás que el contenido aparece de inmediato en la sección de inicio sin reiniciar la interfaz.",
                color = TextGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}
