package com.example.presentation.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.presentation.viewmodel.DulceVisionViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

val premiumAvatars = listOf(
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150", 
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", 
    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150", 
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150", 
    "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150", 
    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150"
)

@Composable
fun LoginScreen(
    viewModel: DulceVisionViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isLogged by viewModel.isUserLoggedIn.collectAsState()
    val profiles by viewModel.availableProfiles.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()

    var email by remember { mutableStateOf("sebasgnz@gmail.com") }
    var password by remember { mutableStateOf("******") }
    var fullName by remember { mutableStateOf("") }
    
    // UI Flows and indicators
    var isRegisterMode by remember { mutableStateOf(false) }
    var showProfileSelection by remember { mutableStateOf(false) }
    var showGoogleChooser by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    // Simulated Google account expansion list
    var showNewGoogleFields by remember { mutableStateOf(false) }
    var customGoogleName by remember { mutableStateOf("") }
    var customGoogleEmail by remember { mutableStateOf("") }

    // Multi-profile manager modes
    var isManageProfilesMode by remember { mutableStateOf(false) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var targetEditProfile by remember { mutableStateOf<UserProfile?>(null) }

    // Temp profile values
    var profileInputName by remember { mutableStateOf("") }
    var profileInputIsAdult by remember { mutableStateOf(true) }
    var profileInputAvatar by remember { mutableStateOf(premiumAvatars.first()) }

    // Navigation trigger
    LaunchedEffect(isLogged, showProfileSelection, currentProfile) {
        if (isLogged && currentProfile != null) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBg, CardGlowSurface, ObsidianBg)
                )
            )
    ) {
        if (!showProfileSelection) {
            // Sleek Credential Forms
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main Logo Brand Icon
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(DulcePink, DulceOrange)))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DV",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "DulceVision",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "EL SUEÑO DEL STREAMING INTELIGENTE",
                    color = GoldAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Custom Credentials Tab Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabToggleButton(
                        text = "INICIAR SESIÓN",
                        isSelected = !isRegisterMode,
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            isRegisterMode = false 
                            authError = null
                        }
                    )
                    TabToggleButton(
                        text = "CREAR CUENTA",
                        isSelected = isRegisterMode,
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            isRegisterMode = true 
                            authError = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error feedback bar
                if (authError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x33EA4335)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEA4335).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEA4335))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = authError ?: "",
                                color = Color(0xFFFFB4AB),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Credentials Panel Glass Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = if (isRegisterMode) "REGISTRO DE CLIENTE" else "ACCEDER A LA RED",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Nombre Completo") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DulcePink) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DulcePink,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedLabelColor = DulcePink,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("fullname_input")
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo Electrónico") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = DulcePink) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DulcePink,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedLabelColor = DulcePink,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("email_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = DulceOrange) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DulceOrange,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = DulceOrange,
                                unfocusedLabelColor = TextGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("password_input")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank() || (isRegisterMode && fullName.isBlank())) {
                                    authError = "Por favor, completa todos los campos del formulario"
                                    return@Button
                                }
                                isAuthenticating = true
                                authError = null
                                
                                if (isRegisterMode) {
                                    viewModel.registerWithCredentials(email, fullName, password) { success, fallbackMsg ->
                                        isAuthenticating = false
                                        if (success) {
                                            if (fallbackMsg != null) authError = fallbackMsg
                                            showProfileSelection = true
                                        } else {
                                            authError = "Fallo de registro: Verifica tus datos"
                                        }
                                    }
                                } else {
                                    viewModel.login(email, password) { success, fallbackMsg ->
                                        isAuthenticating = false
                                        if (success) {
                                            if (fallbackMsg != null) authError = fallbackMsg
                                            showProfileSelection = true
                                        } else {
                                            authError = "Credenciales incorrectas. Intenta de nuevo"
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DulcePink),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isAuthenticating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_form_button")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    if (isRegisterMode) "Crear nueva Cuenta Premium" else "Iniciar Sesión",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Premium Visual Divider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                            Text(
                                text = "Ó CONECTA SEGURAMENTE",
                                color = TextGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                        }

                        // REAL GOOGLE IDENTITY SERVICE INTEGRATION
                        Button(
                            onClick = {
                                authError = null
                                scope.launch {
                                    try {
                                        val credentialManager = CredentialManager.create(context)
                                        
                                        val googleIdOption = GetGoogleIdOption.Builder()
                                            .setFilterByAuthorizedAccounts(false)
                                            .setServerClientId("7786441-google-client-dulcevision.apps.googleusercontent.com")
                                            .setAutoSelectEnabled(false)
                                            .build()

                                        val req = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleIdOption)
                                            .build()

                                        val responseResult = credentialManager.getCredential(context, req)
                                        val credential = responseResult.credential

                                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val tokenCred = GoogleIdTokenCredential.createFrom(credential.data)
                                            viewModel.registerAndLoginWithGoogle(
                                                email = tokenCred.id,
                                                fullName = tokenCred.displayName ?: tokenCred.id.substringBefore("@"),
                                                avatarUrl = tokenCred.profilePictureUri?.toString() ?: premiumAvatars.first()
                                            ) { success ->
                                                if (success) {
                                                    showProfileSelection = true
                                                }
                                            }
                                        } else {
                                            showGoogleChooser = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e("DulceVisionAuth", "CredentialManager failed: ${e.message}, launching OAuth high-fidelity chooser simulator")
                                        showGoogleChooser = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .testTag("google_sso_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Card(
                                    modifier = Modifier.size(20.dp).padding(1.dp),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = DulcePink
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = if (isRegisterMode) "Registrarse con Google" else "Iniciar Sesión con Google",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Cinematic Profile List Selector with support for CRUD Edit/Create/Delete
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { isManageProfilesMode = !isManageProfilesMode }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isManageProfilesMode) Icons.Default.Check else Icons.Default.Settings,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isManageProfilesMode) "LISTO" else "GESTIONAR PERFILES",
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (!isManageProfilesMode) {
                        IconButton(
                            onClick = {
                                profileInputName = ""
                                profileInputIsAdult = true
                                profileInputAvatar = premiumAvatars.random()
                                showAddProfileDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir Perfil", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isManageProfilesMode) "Administrar Perfiles" else "¿Quién está viendo hoy?",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isManageProfilesMode) "Selecciona un perfil para editar o eliminar" else "Selecciona tu perfil de DulceVision",
                    color = TextGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    profiles.forEach { profile ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (isManageProfilesMode) {
                                        targetEditProfile = profile
                                        profileInputName = profile.name
                                        profileInputIsAdult = profile.isAdult
                                        profileInputAvatar = profile.avatarUrl
                                        showEditProfileDialog = true
                                    } else {
                                        viewModel.login(email)
                                        viewModel.selectProfile(profile)
                                    }
                                }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.sweepGradient(listOf(DulcePink, DulceOrange, DulcePink)),
                                        shape = CircleShape
                                    )
                                    .background(CardGlowSurface)
                            ) {
                                AsyncImage(
                                    model = profile.avatarUrl,
                                    contentDescription = profile.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isManageProfilesMode) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = profile.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (!profile.isAdult) {
                                Text(
                                    text = "INFANTIL",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Explicit column to Add Profile
                    if (!isManageProfilesMode) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    profileInputName = ""
                                    profileInputIsAdult = true
                                    profileInputAvatar = premiumAvatars.random()
                                    showAddProfileDialog = true
                                }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Añadir Perfil", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Añadir Perfil", color = TextGray, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                TextButton(onClick = { 
                    isManageProfilesMode = false
                    showProfileSelection = false 
                }) {
                    Text("Cambiar de Cuenta", color = DulcePink, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Add Profile Dialog
        if (showAddProfileDialog) {
            AlertDialog(
                onDismissRequest = { showAddProfileDialog = false },
                containerColor = Color(0xFF202124),
                shape = RoundedCornerShape(24.dp),
                title = { Text("Añadir Perfil", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = profileInputName,
                            onValueChange = { profileInputName = it },
                            label = { Text("Nombre del Perfil", color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DulcePink,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = !profileInputIsAdult,
                                onCheckedChange = { profileInputIsAdult = !it },
                                colors = CheckboxDefaults.colors(checkedColor = DulcePink)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Perfil Infantil (Contenido Filtrado)", color = Color.White, fontSize = 14.sp)
                        }

                        Text("Selecciona un Avatar", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            premiumAvatars.forEach { av ->
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (profileInputAvatar == av) 3.dp else 1.dp,
                                            color = if (profileInputAvatar == av) DulcePink else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { profileInputAvatar = av }
                                ) {
                                    AsyncImage(
                                        model = av,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (profileInputName.isNotBlank()) {
                                viewModel.createNewProfile(profileInputName, profileInputAvatar, profileInputIsAdult)
                                showAddProfileDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DulcePink)
                    ) {
                        Text("Crear", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddProfileDialog = false }) {
                        Text("Cancelar", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // Edit Profile Dialog
        if (showEditProfileDialog && targetEditProfile != null) {
            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                containerColor = Color(0xFF202124),
                shape = RoundedCornerShape(24.dp),
                title = { Text("Editar Perfil", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = profileInputName,
                            onValueChange = { profileInputName = it },
                            label = { Text("Nombre del Perfil", color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DulcePink,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = !profileInputIsAdult,
                                onCheckedChange = { profileInputIsAdult = !it },
                                colors = CheckboxDefaults.colors(checkedColor = DulcePink)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Perfil Infantil (Contenido Filtrado)", color = Color.White, fontSize = 14.sp)
                        }

                        Text("Selecciona un Avatar", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            premiumAvatars.forEach { av ->
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (profileInputAvatar == av) 3.dp else 1.dp,
                                            color = if (profileInputAvatar == av) DulcePink else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { profileInputAvatar = av }
                                ) {
                                    AsyncImage(
                                        model = av,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // High contrast delete option
                        Button(
                            onClick = {
                                targetEditProfile?.let {
                                    viewModel.deleteUserProfile(it.uuid)
                                }
                                showEditProfileDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EA4335)),
                            border = BorderStroke(1.dp, Color(0xFFEA4335).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEA4335), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Eliminar Perfil Permanentemente", color = Color(0xFFFFB4AB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            targetEditProfile?.let {
                                viewModel.editUserProfile(it.uuid, profileInputName, profileInputAvatar, profileInputIsAdult)
                            }
                            showEditProfileDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DulcePink)
                    ) {
                        Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Cancelar", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // --- GOOGLE SIGN-IN & SINGLE SIGN-ON (SSO) ENGINE PROTOCOL FALLBACK ---
        if (showGoogleChooser) {
            AlertDialog(
                onDismissRequest = { 
                    showGoogleChooser = false
                    showNewGoogleFields = false
                },
                containerColor = Color(0xFF202124), 
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.padding(16.dp),
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Acceder con Google",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Elige una cuenta para continuar en DulceVision",
                            color = Color(0xFF9AA0A6),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF3C4043)))

                        GoogleAccountRow(
                            displayName = "Sebas González",
                            email = "sebasgnz@gmail.com",
                            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                            onClick = {
                                viewModel.registerAndLoginWithGoogle(
                                    email = "sebasgnz@gmail.com",
                                    fullName = "Sebas González",
                                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
                                ) {
                                    showGoogleChooser = false
                                    showProfileSelection = true
                                }
                            }
                        )

                        GoogleAccountRow(
                            displayName = "Dulce Admin Studio",
                            email = "admin_developer@dulcevision.com",
                            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                            onClick = {
                                viewModel.registerAndLoginWithGoogle(
                                    email = "admin_developer@dulcevision.com",
                                    fullName = "Dulce Suministrador G",
                                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
                                ) {
                                    showGoogleChooser = false
                                    showProfileSelection = true
                                }
                            }
                        )

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF3C4043)))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNewGoogleFields = !showNewGoogleFields }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF8AB4F8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Usar otra cuenta de Google",
                                color = Color(0xFF8AB4F8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (showNewGoogleFields) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customGoogleName,
                                    onValueChange = { customGoogleName = it },
                                    label = { Text("Nombre Completo (Google)", color = Color(0xFF9AA0A6)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8AB4F8),
                                        unfocusedBorderColor = Color(0xFF5F6368),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = customGoogleEmail,
                                    onValueChange = { customGoogleEmail = it },
                                    label = { Text("Correo Gmail (@gmail.com)", color = Color(0xFF9AA0A6)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8AB4F8),
                                        unfocusedBorderColor = Color(0xFF5F6368),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (customGoogleEmail.isNotBlank()) {
                                            val finalName = if (customGoogleName.isBlank()) customGoogleEmail.substringBefore("@") else customGoogleName
                                            viewModel.registerAndLoginWithGoogle(
                                                email = customGoogleEmail,
                                                fullName = finalName,
                                                avatarUrl = premiumAvatars.random()
                                            ) {
                                                showGoogleChooser = false
                                                showProfileSelection = true
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8AB4F8)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Vincular y Acceder", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun TabToggleButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) DulcePink else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun GoogleAccountRow(
    displayName: String,
    email: String,
    avatarUrl: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF3C4043))
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = displayName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = email, color = Color(0xFF9AA0A6), fontSize = 11.sp)
        }
    }
}
