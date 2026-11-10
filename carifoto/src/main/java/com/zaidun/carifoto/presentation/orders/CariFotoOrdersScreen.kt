package com.zaidun.carifoto.presentation.orders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zaidun.carifoto.presentation.home.CariFotoHomeViewModel

private val ScreenBg = Color(0xFFF4F7FB)
private val PrimaryPurple = Color(0xFF7B4FE0)
private val TextDark = Color(0xFF1E293B)
private val TextGray = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CariFotoOrdersScreen(
    viewModel: CariFotoHomeViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val purchasedPhotos = remember(uiState.photos, uiState.purchasedPhotoIds) {
        uiState.photos.filter { uiState.purchasedPhotoIds.contains(it.id) }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pesanan Saya (${purchasedPhotos.size})", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ScreenBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            if (purchasedPhotos.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(purchasedPhotos, key = { it.id }) { photo ->
                        val driveUrl = "https://lh3.googleusercontent.com/d/${photo.driveFileId}"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail Foto
                                AsyncImage(
                                    model = driveUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F172A)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        photo.fileName.ifBlank { "IMG_PHOTO.JPG" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Lunas via QRIS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                                    }
                                    Spacer(Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(driveUrl))
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Download HD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextGray, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Belum Ada Foto Dibeli", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Foto yang sudah kamu beli via QRIS akan muncul di sini beserta tombol Download HD.",
                            fontSize = 12.sp,
                            color = TextGray,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
