package com.zaidun.carifoto.presentation.profile

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.zaidun.carifoto.presentation.auth.CariFotoAuthViewModel
import com.zaidun.carifoto.presentation.home.CariFotoHomeViewModel

private val ScreenBg = Color(0xFFF4F7FB)
private val TextDark = Color(0xFF1E293B)
private val TextGray = Color(0xFF64748B)
private val PrimaryPurple = Color(0xFF7B4FE0)
private val DarkPurple = Color(0xFF23143D)
private val PillBg = Color(0xFFE8F2FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CariFotoProfileScreen(
    authViewModel: CariFotoAuthViewModel,
    homeViewModel: CariFotoHomeViewModel,
    onBack: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onLogoutFinished: () -> Unit
) {
    val context = LocalContext.current
    val authUiState by authViewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        screenVisible = true
    }

    val user = authUiState.googleUser
    val userName = user?.name?.ifBlank { "Guest" } ?: "Guest"
    val userEmail = user?.email?.ifBlank { "Guest@carifoto.id" } ?: "Guest@carifoto.id"
    val photoUrl = user?.photoUrl

    val initials = userName.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    val purchasedCount = homeUiState.purchasedPhotoIds.size

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profil Rider", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ScreenBg)
            )
        }
    ) { padding ->
        AnimatedVisibility(
            visible = screenVisible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400), initialOffsetY = { -it / 10 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 1. AVATAR & HEADER PROFIL
                Surface(
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(108.dp)
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
                                        text = initials.ifEmpty { "RD" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        color = DarkPurple
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
                                        text = initials.ifEmpty { "RD" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        color = DarkPurple
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
                                text = initials.ifEmpty { "RD" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = DarkPurple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = PillBg,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = userEmail,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. KARTU STATISTIK RIDER
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$purchasedCount Foto", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                            Spacer(Modifier.height(2.dp))
                            Text("Foto Koleksi Dibeli", fontSize = 11.sp, color = TextGray)
                        }

                        VerticalDivider(modifier = Modifier.height(36.dp), color = Color(0xFFE2E8F0))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val isGuest = user == null || user.id.isBlank() || user.id == "guest_rider_001" || user.id.startsWith("guest")
                            Text(
                                text = if (isGuest) "Mode Tamu" else "Aktif",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGuest) PrimaryPurple else Color(0xFF22C55E)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("Status Akun", fontSize = 11.sp, color = TextGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. MENU NAVIGASI KARTU
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        MenuItemRow(
                            icon = Icons.Default.ShoppingCart,
                            title = "Pesanan Saya",
                            subtitle = "Lihat foto jepretan yang sudah dibeli",
                            onClick = onNavigateToOrders
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))

                        MenuItemRow(
                            icon = Icons.Default.Info,
                            title = "Pusat Bantuan & FAQ",
                            subtitle = "Cara cari foto, bayar QRIS & download HD",
                            onClick = {
                                Toast.makeText(context, "Bantuan: Pembayaran via QRIS otomatis terverifikasi & foto HD langsung bisa didownload.", Toast.LENGTH_LONG).show()
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))

                        MenuItemRow(
                            icon = Icons.Default.Lock,
                            title = "Syarat & Ketentuan Rider",
                            subtitle = "Kebijakan privasi & hak cipta foto",
                            onClick = {
                                Toast.makeText(context, "Syarat: Foto dipublikasikan oleh Fotografer terverifikasi untuk komunitas Rider.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 4. TOMBOL AUTH (GUEST: "Masuk dengan Google" | LOGGED IN: "Keluar Akun")
                val isGuest = user == null || user.id.isBlank() || user.id == "guest_rider_001" || user.id.startsWith("guest")
                if (isGuest) {
                    Button(
                        onClick = {
                            authViewModel.logout(context) {
                                onLogoutFinished()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Masuk dengan Google", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            authViewModel.logout(context) {
                                Toast.makeText(context, "Berhasil Keluar Akun", Toast.LENGTH_SHORT).show()
                                onLogoutFinished()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Keluar Akun", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = TextGray)
            }
        }

        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
    }
}
