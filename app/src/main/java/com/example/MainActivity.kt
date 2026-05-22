package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConnectedTv
import androidx.compose.material.icons.filled.Dvr
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.DulceVisionViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CardGlowSurface
import com.example.ui.theme.DulcePink
import com.example.ui.theme.TextGray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: DulceVisionViewModel = viewModel()
                val navController = rememberNavController()

                // State listener on current navigation route to show/hide elegant Bottom Nav Rail in Wide/Medium size classes
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Display bottom bar only if logged in and not in full player screen
                        if (currentRoute != null && currentRoute != "login" && !currentRoute.startsWith("player")) {
                            NavigationBar(
                                containerColor = CardGlowSurface,
                                contentColor = Color.White
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
                                    label = { Text("Inicio", fontSize = 11.sp) },
                                    selected = currentRoute == "home",
                                    onClick = {
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = false }
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DulcePink,
                                        selectedTextColor = DulcePink,
                                        unselectedIconColor = TextGray,
                                        unselectedTextColor = TextGray,
                                        indicatorColor = Color.Transparent
                                    )
                                )

                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.ConnectedTv, contentDescription = "IPTV", modifier = Modifier.size(24.dp)) },
                                    label = { Text("IPTV Live", fontSize = 11.sp) },
                                    selected = currentRoute == "iptv",
                                    onClick = {
                                        navController.navigate("iptv") {
                                            popUpTo("home")
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DulcePink,
                                        selectedTextColor = DulcePink,
                                        unselectedIconColor = TextGray,
                                        unselectedTextColor = TextGray,
                                        indicatorColor = Color.Transparent
                                    )
                                )

                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Dvr, contentDescription = "Suministro", modifier = Modifier.size(24.dp)) },
                                    label = { Text("Realtime Admin", fontSize = 11.sp) },
                                    selected = currentRoute == "admin",
                                    onClick = {
                                        navController.navigate("admin") {
                                            popUpTo("home")
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DulcePink,
                                        selectedTextColor = DulcePink,
                                        unselectedIconColor = TextGray,
                                        unselectedTextColor = TextGray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0.dp) // We delegate to edgeToEdge manually in padding screens
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (currentRoute != "login" && currentRoute != "player") innerPadding.calculateBottomPadding() else 0.dp)
                    ) {
                        composable("login") {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { url, title, thumb ->
                                    viewModel.selectMediaToPlay(url, title, thumb)
                                    navController.navigate("player")
                                },
                                onNavigateToDetail = { mediaId, isSeries ->
                                    navController.navigate("detail/$mediaId/$isSeries")
                                },
                                onNavigateToIPTV = {
                                    navController.navigate("iptv")
                                },
                                onNavigateToAdmin = {
                                    navController.navigate("admin")
                                }
                            )
                        }

                        composable(
                            route = "detail/{mediaId}/{isSeries}",
                            arguments = listOf(
                                navArgument("mediaId") { type = NavType.StringType },
                                navArgument("isSeries") { type = NavType.BoolType }
                            )
                        ) { backStackEntry ->
                            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
                            val isSeries = backStackEntry.arguments?.getBoolean("isSeries") ?: false
                            VODDetailScreen(
                                mediaId = mediaId,
                                isSeries = isSeries,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onPlayMedia = { url, title, thumb ->
                                    viewModel.selectMediaToPlay(url, title, thumb)
                                    navController.navigate("player")
                                }
                            )
                        }

                        composable("player") {
                            val playUrl by viewModel.activePlaybackUrl.collectAsState()
                            val playTitle by viewModel.activePlaybackTitle.collectAsState()
                            val playThumb by viewModel.activePlaybackThumbnail.collectAsState()

                            if (playUrl != null) {
                                PlayerScreen(
                                    videoUrl = playUrl!!,
                                    title = playTitle ?: "Reproducción",
                                    thumbnail = playThumb ?: "",
                                    viewModel = viewModel,
                                    onBack = {
                                        viewModel.clearActivePlayback()
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }

                        composable("iptv") {
                            IPTVPlayerScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onPlayFullscreen = { url, title, thumb ->
                                    viewModel.selectMediaToPlay(url, title, thumb)
                                    navController.navigate("player")
                                }
                            )
                        }

                        composable("admin") {
                            AdminPanelSessionScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
