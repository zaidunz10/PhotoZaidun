package com.zaidun.photozaidun.presentation.screen.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.items // PENTING: Untuk looping list foto
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale    // PENTING: Untuk ContentScale.Crop
import coil3.compose.AsyncImage                 // Untuk menampilkan gambar
import com.zaidun.photozaidun.presentation.navigation.Screen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    sharedFolderViewModel: com.zaidun.photozaidun.presentation.shared.SharedFolderViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExportLoading: () -> Unit,
    onNavigateToFolderPicker: () -> Unit,
    onNavigateToWatermark: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val photos by sharedFolderViewModel.photos.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var pressed by remember {
        mutableStateOf(false)
    }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(120),
        label = "buttonScale"
    )


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Photo Zaidun", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Card Ringkasan Batch (Sesuai Screenshot V2)
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEBE9F1) // Warna background card di screenshot
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Ringkasan Batch",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        if (uiState.totalImages > 0) "${uiState.totalImages} Foto" else "Kosong",
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 4.dp
                                        ),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(Modifier.height(32.dp))

                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color.Gray.copy(alpha = 0.6f)
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                if (uiState.totalImages > 0) "Siap untuk diproses" else "Belum ada foto dipilih",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = onNavigateToFolderPicker,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Pilih Folder", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2. Foto-foto yang dipilih
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "Foto terpilih",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }


                if (photos.isNotEmpty()) {
                    // Pakai items() untuk melooping foto asli
                    items(photos) { photo ->
                        Card(
                            modifier = Modifier.aspectRatio(1f).padding(4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            coil3.compose.AsyncImage(
                                model = photo.uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    item(span = { GridItemSpan(2) }) {
                        Text("Belum ada foto dimuat", color = Color.Gray)
                    }
                }
            }


            // 3. Tombol Export di Bawah (Sticky)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {

                        viewModel.startBatchProcess()

                        onNavigateToExportLoading()

                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {

                            scaleX = scale

                            scaleY = scale
                        },
                    enabled = uiState.totalImages > 0,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Mulai Export", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (uiState.totalImages == 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pilih folder foto terlebih dahulu",
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Copyright by zaidunz_photo",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionCardV2(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5FB))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = color
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
