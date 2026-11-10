package com.zaidun.photozaidun.presentation.screen.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.zaidun.photozaidun.data.remote.SupabaseHelper
import com.zaidun.photozaidun.data.remote.UserDto
import com.zaidun.photozaidun.presentation.component.motion.PressAnimatedButton
import com.zaidun.photozaidun.presentation.screen.settings.SettingsUiState
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

private val ScreenBg = Color(0xFFF7F4FC)
private val TitleDark = Color(0xFF2A1A44)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSaveSuccess: (whatsapp: String, instagram: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var whatsapp by remember { mutableStateOf(uiState.photographerWa) }
    var instagram by remember { mutableStateOf(uiState.photographerIg) }
    var isSaving by remember { mutableStateOf(false) }

    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        screenVisible = true
    }

    LaunchedEffect(uiState.photographerWa, uiState.photographerIg) {
        whatsapp = uiState.photographerWa
        instagram = uiState.photographerIg
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = { Text("Profil Fotografer", fontWeight = FontWeight.Bold, color = TitleDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TitleDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBg)
            )
        }
    ) { padding ->
        AnimatedVisibility(
            visible = screenVisible,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400), initialOffsetY = { -it / 10 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Foto Profil
                val photoUrl = uiState.googleUser?.photoUrl
                val userName = uiState.googleUser?.name ?: "Zidan Syahid"
                val initials = userName.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")

                Surface(
                    shape = CircleShape,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(104.dp)
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFD6C7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials.ifEmpty { "ZS" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        color = Color(0xFF2E184D)
                                    )
                                }
                            },
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFD6C7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials.ifEmpty { "ZS" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        color = Color(0xFF2E184D)
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFD6C7FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials.ifEmpty { "ZS" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = Color(0xFF2E184D)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nama & Email
                Text(
                    text = uiState.googleUser?.name ?: "Guest User",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitleDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uiState.googleUser?.email ?: "Belum Login",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Form WhatsApp
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("Nomor WhatsApp", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("Contoh: 08123456789", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF7B4FE0)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7B4FE0),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF1E293B),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Form Instagram
                OutlinedTextField(
                    value = instagram,
                    onValueChange = { instagram = it },
                    label = { Text("Username Instagram", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("Contoh: @fotografer", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7B4FE0),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF1E293B),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Tombol Simpan dengan Animasi Tekan
                PressAnimatedButton(
                    onClick = {
                        if (whatsapp.isBlank() || instagram.isBlank()) {
                            Toast.makeText(context, "Harap lengkapi WA dan Instagram", Toast.LENGTH_SHORT).show()
                            return@PressAnimatedButton
                        }

                        val user = uiState.googleUser
                        if (user == null) {
                            Toast.makeText(context, "Harap login Google terlebih dahulu di menu Pengaturan", Toast.LENGTH_LONG).show()
                            return@PressAnimatedButton
                        }

                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val userDto = UserDto(
                                    id = user.id,
                                    name = user.name,
                                    email = user.email,
                                    avatarUrl = user.photoUrl
                                )

                                SupabaseHelper.client.postgrest.from("users").upsert(userDto)
                                onSaveSuccess(whatsapp, instagram)

                                Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                onBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Simpan Profil", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
