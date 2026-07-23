package com.zaidun.photozaidun.presentation.screen.home

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import android.net.Uri
import androidx.compose.foundation.lazy.itemsIndexed
import coil3.compose.AsyncImage
import com.zaidun.photozaidun.utils.toReadableSize
import kotlinx.coroutines.delay

// --- Palet warna tema ungu, dipakai konsisten di seluruh layar ---
private val DeepPurple = Color(0xFF3B1F63)      // judul, tombol export
private val AccentPurple = Color(0xFF7B4FE0)    // tombol Pilih Folder, progress bar
private val CardTop = Color(0xFFE7DEF9)         // gradient card bagian atas
private val CardBottom = Color(0xFFF6F2FC)      // gradient card bagian bawah
private val ScreenTop = Color(0xFFF3EEFB)       // gradient background layar
private val ScreenBottom = Color(0xFFFFFFFF)
private val TitleDark = Color(0xFF2A1A44)
private val SubtleGray = Color(0xFF6B6079)
private val LabelGray = Color(0xFF8A7F98)


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

    val activity = LocalActivity.current
    val context = LocalContext.current

    // --- ANIMASI: trigger entrance animation sekali saat screen pertama muncul ---
    var screenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        activity?.let {
            viewModel.initialize(it)
        }
        screenVisible = true
    }

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                activity?.let { activity ->
                    viewModel.refreshTokenForUpload(activity) { token ->
                        viewModel.prepareExport(token)
                        onNavigateToExportLoading()
                    }
                }
            }
        }

    val photos by sharedFolderViewModel.photos.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(120),
        label = "buttonScale"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Photo Zaidun", fontWeight = FontWeight.Bold, color = DeepPurple)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(ScreenTop, ScreenBottom)))
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(Modifier.height(8.dp))

            // --- ANIMASI: fade + slide in dari atas waktu screen dibuka ---
            AnimatedVisibility(
                visible = screenVisible,
                enter = fadeIn(animationSpec = tween(450)) +
                        slideInVertically(
                            animationSpec = tween(450),
                            initialOffsetY = { -it / 6 }
                        )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
                            .padding(22.dp)
                    ) {

                        // Header: "Batch Summary" + badge jumlah foto
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Batch Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TitleDark
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    if (uiState.totalImages > 0) "${uiState.totalImages} Foto" else "Kosong",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepPurple
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            if (uiState.totalImages > 0) "Siap untuk diproses" else "Belum ada foto dipilih",
                            color = SubtleGray,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(22.dp))

                        Text(
                            "Penyimpanan Cloud",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TitleDark
                        )

                        Spacer(Modifier.height(8.dp))

                        val storage = uiState.driveStorage
                        val targetPercent = storage?.percent ?: 0f

                        // --- ANIMASI: progress bar mengisi pelan-pelan, bukan langsung penuh ---
                        val animatedProgress by animateFloatAsState(
                            targetValue = targetPercent,
                            animationSpec = tween(
                                durationMillis = 900,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            ),
                            label = "storageProgress"
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = AccentPurple,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )

                        Spacer(Modifier.height(16.dp))

                        // 3 kolom statistik sejajar: Digunakan / Tersisa / Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StorageStat(
                                value = storage?.usedBytes?.toReadableSize() ?: "-",
                                label = "Digunakan"
                            )
                            StorageStat(
                                value = storage?.freeBytes?.toReadableSize() ?: "-",
                                label = "Tersisa"
                            )
                            StorageStat(
                                value = storage?.totalBytes?.toReadableSize() ?: "-",
                                label = "Total"
                            )
                        }

                        Spacer(Modifier.height(22.dp))

                        Button(
                            onClick = onNavigateToFolderPicker,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPurple,
                                contentColor = Color.White
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

                        // Nama folder terpilih (kalau ada), tetap ditampilkan tanpa mengubah state
                        if (uiState.selectedFolder.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            val folderName = DocumentFile.fromTreeUri(
                                context,
                                Uri.parse(uiState.selectedFolder)
                            )?.name ?: "-"
                            Text(
                                folderName,
                                color = LabelGray,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Foto terpilih",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TitleDark
            )

            Spacer(Modifier.height(12.dp))

            if (photos.isNotEmpty()) {
                // Baris horizontal thumbnail, simple & minimalis
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(photos) { index, photo ->
                        // --- ANIMASI: tiap foto muncul dengan fade + scale, staggered per index ---
                        var itemVisible by remember(photo.uri) { mutableStateOf(false) }

                        LaunchedEffect(photo.uri) {
                            delay((index % 12) * 40L)
                            itemVisible = true
                        }

                        AnimatedVisibility(
                            visible = itemVisible,
                            enter = fadeIn(tween(300)) + scaleIn(
                                initialScale = 0.85f,
                                animationSpec = tween(300)
                            )
                        ) {
                            Card(
                                modifier = Modifier.size(78.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                AsyncImage(
                                    model = photo.uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            } else {
                Text("Belum ada foto dimuat", color = Color.Gray, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // Tombol Export — elemen paling menonjol
            Button(
                onClick = {
                    activity?.let { activity ->
                        viewModel.refreshTokenForUpload(activity) { token ->
                            viewModel.prepareExport(token)
                            onNavigateToExportLoading()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                enabled = uiState.totalImages > 0 && !uiState.isProcessing,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepPurple,
                    contentColor = Color.White,
                    disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mulai Export", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (uiState.totalImages == 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pilih folder foto terlebih dahulu",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Copyright by zaidunz_photo",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RowScope.StorageStat(value: String, label: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TitleDark)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = LabelGray)
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