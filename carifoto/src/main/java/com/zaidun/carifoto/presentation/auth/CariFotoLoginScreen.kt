package com.zaidun.carifoto.presentation.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

private val ScreenBg = Color(0xFFF4F7FB)
private val PrimaryPurple = Color(0xFF7B4FE0)
private val DarkPurple = Color(0xFF23143D)
private val TextDark = Color(0xFF1E293B)
private val TextGray = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CariFotoLoginScreen(
    authViewModel: CariFotoAuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = ScreenBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(24.dp))

                // App Brand Icon
                Surface(
                    color = PrimaryPurple,
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "CariFoto Rider",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkPurple
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Cari & Beli Foto Jepretan Jalananmu Secara Instan",
                    fontSize = 13.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // Feature Highlight Cards
                FeatureCard(
                    title = "Foto Utuh Presisi (Anti-Crop)",
                    description = "Lihat jepretan aksi motor/cornering kamu tanpa terpotong atas/bawah."
                )

                Spacer(Modifier.height(12.dp))

                FeatureCard(
                    title = "Bayar Instan via QRIS",
                    description = "Dukungan GoPay, OVO, ShopeePay, DANA, LinkAja & BCA Mobile."
                )

                Spacer(Modifier.height(12.dp))

                FeatureCard(
                    title = "Download HD Bebas Watermark",
                    description = "Foto jernih langsung dari server Google Drive CDN milik fotografer."
                )
            }

            // Action Login Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val activity = LocalContext.current as? Activity
                Button(
                    onClick = {
                        activity?.let { act ->
                            authViewModel.login(act) {
                                Toast.makeText(act, "Selamat Datang di CariFoto!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    enabled = !uiState.isLoggingIn
                ) {
                    if (uiState.isLoggingIn) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "Masuk dengan Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkPurple),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        "Jelajahi Foto Dulu (Tamu)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkPurple
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "PhotoZaidun Ecosystem © 2026",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(2.dp))
                Text(description, fontSize = 11.sp, color = TextGray)
            }
        }
    }
}
