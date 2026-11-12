package com.zaidun.photozaidun.presentation.screen.home

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import java.util.Calendar
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.zaidun.photozaidun.data.ads.AdMobManager
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.zaidun.photozaidun.presentation.component.motion.PressAnimatedButton
import com.zaidun.photozaidun.presentation.shared.SharedFolderViewModel
import com.zaidun.photozaidun.presentation.theme.motion.MotionTokens
import com.zaidun.photozaidun.utils.toReadableSize
import kotlinx.coroutines.delay
import java.util.Locale

// --- Palet Warna Presisi Sesuai Mockup UI ---
private val ScreenBg = Color(0xFFF4F7FB)
private val DeepNavyCard = Color(0xFF0C4566)
private val CardWhite = Color(0xFFFFFFFF)
private val AccentOceanBlue = Color(0xFF007BFF)
private val PrimaryCyan = Color(0xFF0077B6)
private val TextDark = Color(0xFF1E293B)
private val TextGray = Color(0xFF64748B)
private val ConnectedGreen = Color(0xFF22C55E)
private val PillEmailBg = Color(0xFFE8F2FF)
private val PhotoCardTeal = Color(0xFF0D5C75)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    sharedFolderViewModel: SharedFolderViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExportLoading: () -> Unit,
    onNavigateToFolderPicker: () -> Unit,
    onNavigateToWatermark: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalActivity.current
    val context = LocalContext.current

    var screenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        activity?.let {
            viewModel.initialize(it)
        }
        screenVisible = true
    }

    val launcher = rememberLauncherForActivityResult(
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
    var locationText by remember { mutableStateOf("") }

    // Modal Bottom Sheet Input Lokasi Hunting (Matching Mockup 2)
    if (uiState.showLocationDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissLocationDialog() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "Tentukan lokasi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Lokasi ini akan ditampilkan pada modul Rider.",
                    fontSize = 13.sp,
                    color = TextGray
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    placeholder = { Text("Lokasi Atau Event", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7B4FE0),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(Modifier.height(16.dp))

                val dbSuggestions = uiState.locationSuggestions.filter { it.isNotBlank() }.distinct()
                val filtered = dbSuggestions.filter { it.contains(locationText, ignoreCase = true) }

                if (filtered.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filtered.take(6).forEach { itemLoc ->
                            val isSelected = locationText.trim().equals(itemLoc.trim(), ignoreCase = true)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { locationText = itemLoc },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFFF7F4FC) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = Color(0xFFF3E8FF),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF7B4FE0), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(itemLoc, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                            Text("Database Lokasi Hunting", fontSize = 12.sp, color = TextGray)
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF7B4FE0), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (dbSuggestions.isEmpty()) "Belum ada riwayat lokasi di Database" else "Lokasi tidak ditemukan",
                            fontSize = 13.sp,
                            color = TextGray
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (locationText.isNotBlank()) {
                            activity?.let { act ->
                                AdMobManager.showInterstitialAdBeforeExport(
                                    activity = act,
                                    userEmail = uiState.userEmail
                                ) {
                                    viewModel.refreshTokenForUpload(act) { token ->
                                        viewModel.confirmLocationAndExport(locationText.trim(), token)
                                        onNavigateToExportLoading()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B4FE0))
                ) {
                    Text("Konfirmasi & Export", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    Scaffold(
        containerColor = ScreenBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            // =========================================================
            // 1. TOP HEADER: Avatar Profil & Salam Dinamis (Pagi/Siang/Sore/Malam)
            // =========================================================
            val greeting = remember { getDynamicGreeting() }
            val userName = uiState.userName.ifBlank { "GUest" }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val photoUrl = uiState.userPhotoUrl.takeIf { it.isNotBlank() && it != "null" }
                    val initials = userName.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")

                    if (photoUrl != null) {
                        SubcomposeAsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD6C7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials.ifEmpty { "ZS" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF2E184D)
                                    )
                                }
                            },
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD6C7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials.ifEmpty { "ZS" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF2E184D)
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD6C7FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials.ifEmpty { "ZS" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF2E184D)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = greeting,
                            fontSize = 13.sp,
                            color = TextGray
                        )
                        Text(
                            text = userName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }

                // Tombol Ikon Kanan (History 🕒 & Settings ⚙️)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = TextDark, modifier = Modifier.size(24.dp))
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextDark, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // =========================================================
            // 2. KARTU PURPLE: Penyimpanan Google Drive
            // =========================================================
            AnimatedVisibility(
                visible = screenVisible,
                enter = fadeIn(tween(450)) + slideInVertically(tween(450), initialOffsetY = { -it / 6 })
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E184D)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Row Top: Icon + "Penyimpanan Google Drive" & Badge "Terhubung" (Horizontal Rapi)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Surface(
                                    color = Color(0xFF4A3075),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Penyimpanan Google Drive",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF166534))
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Terhubung",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        val storage = uiState.driveStorage
                        val usedStr = storage?.usedBytes?.toReadableSize() ?: "12,4 GB"
                        val totalStr = when {
                            storage == null -> "5 TB"
                            storage.totalBytes <= 0L -> "5 TB (Workspace)"
                            else -> storage.totalBytes.toReadableSize()
                        }
                        val freeStr = if (storage != null && storage.totalBytes > 0L) storage.freeBytes.toReadableSize() else "Sangat Luas"
                        val percentValue = if (storage != null && storage.totalBytes > 0L) (storage.percent * 100f) else 5.0f

                        val animatedPercent by animateFloatAsState(
                            targetValue = percentValue,
                            animationSpec = tween(900),
                            label = "percentAnim"
                        )
                        
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                usedStr,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "/ $totalStr",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Linear Progress Bar Horizontal
                        LinearProgressIndicator(
                            progress = { (animatedPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = Color(0xFFB89EFF),
                            trackColor = Color(0xFF4D3375)
                        )

                        Spacer(Modifier.height(10.dp))

                        // Bottom Stats: 82,7% terpakai (kiri) | 2,6 GB tersedia (kanan)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                String.format(Locale.getDefault(), "%.1f%% terpakai", animatedPercent),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )

                            Text(
                                "$freeStr tersedia",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // =========================================================
            // 3. KARTU WHITE: Folder Sumber Foto & Horizontal Preview
            // =========================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header Card: Folder Sumber Foto + Tombol Ubah Folder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "FOLDER SUMBER FOTO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan
                        )

                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { onNavigateToFolderPicker() }
                        ) {
                            val buttonLabel = if (uiState.selectedFolder.isNotBlank()) "Ubah Folder" else "Pilih Folder"
                            Text(
                                buttonLabel,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOceanBlue
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Path Nama Folder (Menampilkan teks asli atau status Belum Dipilih)
                    val folderName = if (uiState.selectedFolder.isNotBlank()) {
                        DocumentFile.fromTreeUri(context, Uri.parse(uiState.selectedFolder))?.name
                            ?: "Folder Terpilih"
                    } else {
                        "Belum ada folder dipilih"
                    }

                    Text(
                        folderName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.selectedFolder.isNotBlank()) TextDark else TextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(16.dp))

                    // Horizontal Photo Thumbnail Preview Cards
                    if (photos.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(photos, key = { _, photo -> photo.uri.toString() }) { index, photo ->
                                var itemVisible by remember(photo.uri) { mutableStateOf(false) }

                                LaunchedEffect(photo.uri) {
                                    delay((index % 12) * 40L)
                                    itemVisible = true
                                }

                                with(sharedTransitionScope) {
                                    AnimatedVisibility(
                                        visible = itemVisible,
                                        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.85f, animationSpec = tween(300))
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .size(width = 76.dp, height = 96.dp)
                                                .sharedElement(
                                                    rememberSharedContentState(key = "photo-${photo.uri}"),
                                                    animatedVisibilityScope = animatedVisibilityScope,
                                                    boundsTransform = { _, _ -> tween(350, easing = MotionTokens.ScreenEasing) }
                                                )
                                                .clickable { },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = PhotoCardTeal)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                AsyncImage(
                                                    model = photo.uri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                
                                                // Overlay timestamp & file name
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.5f),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomCenter)
                                                ) {
                                                    Text(
                                                        text = photo.name ?: "IMG_${index + 1}.JPG",
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Tampilan Kosong jika belum memilih folder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Belum ada foto dimuat",
                                fontSize = 13.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Bottom info bar: Kamera & EXIF Jam Terbaca
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextDark, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${uiState.totalImages} Foto Dimuat",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        if (uiState.totalImages > 0) {
                            Text(
                                "EXIF Jam Terbaca",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConnectedGreen
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // =========================================================
            // 4. TOMBOL UTAMA "Mulai Export" (Aktif hanya jika folder dipilih)
            // =========================================================
            val hasPhotos = uiState.totalImages > 0 && uiState.selectedFolder.isNotBlank() && !uiState.isProcessing
            val buttonText = if (uiState.totalImages > 0) "Mulai Export (${uiState.totalImages} Foto)" else "Mulai Export"

            PressAnimatedButton(
                onClick = {
                    if (hasPhotos) {
                        if (!viewModel.onStartExportClicked()) {
                            Toast.makeText(context, "Harap lengkapi WA dan Instagram di menu Profil terlebih dahulu!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Pilih folder foto terlebih dahulu!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = hasPhotos
            ) {
                Surface(
                    color = if (hasPhotos) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (hasPhotos) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    buttonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasPhotos) Color.White else Color.White.copy(alpha = 0.6f)
                )
            }

            if (!hasPhotos) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pilih folder foto terlebih dahulu",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun getDynamicGreeting(): String {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val timeInMinutes = hour * 60 + minute

    return when {
        timeInMinutes in 60..600 -> "Selamat pagi,"
        timeInMinutes in 601..900 -> "Selamat siang,"
        timeInMinutes in 901..1080 -> "Selamat sore,"
        else -> "Selamat malam,"
    }
}
