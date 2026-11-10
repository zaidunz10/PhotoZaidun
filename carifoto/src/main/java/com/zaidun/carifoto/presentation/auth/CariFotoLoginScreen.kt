package com.zaidun.carifoto.presentation.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zaidun.carifoto.R

private val BackgroundDark = Color(0xFF100E17)
private val SurfaceDark = Color(0xFF181520)
private val BorderDark = Color(0xFF302A3A)
private val PurplePrimary = Color(0xFF7B4FE0)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAA1B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CariFotoLoginScreen(
    authViewModel: CariFotoAuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity

    Scaffold(
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Official Logo Image
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceDark,
                shadowElevation = 6.dp,
                border = BorderStroke(1.5.dp, PurplePrimary.copy(alpha = 0.6f)),
                modifier = Modifier.size(92.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = R.drawable.logo,
                        contentDescription = "CariFoto Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "CariFoto Rider",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Cari & Beli Foto Jepretan Jalananmu Secara Instan",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Feature Highlight Cards (Dark Minimalist)
            FeatureCard(
                title = "Foto Utuh Presisi (Anti-Crop)",
                description = "Lihat jepretan aksi motor/cornering kamu tanpa terpotong atas/bawah."
            )

            Spacer(Modifier.height(10.dp))

            FeatureCard(
                title = "Bayar Instan via QRIS",
                description = "Dukungan GoPay, OVO, ShopeePay, DANA, LinkAja & BCA Mobile."
            )

            Spacer(Modifier.height(10.dp))

            FeatureCard(
                title = "Download HD Bebas Watermark",
                description = "Foto jernih langsung disimpan otomatis ke folder Downloads/Galeri HP."
            )

            Spacer(Modifier.height(24.dp))

            // ACTION BUTTON 1: MASUK DENGAN GOOGLE (Sangat Jelas & Menonjol)
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
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                enabled = !uiState.isLoggingIn
            ) {
                if (uiState.isLoggingIn) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = "Masuk dengan Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ACTION BUTTON 2: JELAJAHI FOTO DULU (TAMU) (Sangat Besar & Jelas)
            Button(
                onClick = {
                    authViewModel.loginGuest {
                        onLoginSuccess()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                border = BorderStroke(1.5.dp, PurplePrimary)
            ) {
                Text(
                    text = "Jelajahi Foto Dulu (Tamu)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(24.dp))
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PurplePrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(description, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
