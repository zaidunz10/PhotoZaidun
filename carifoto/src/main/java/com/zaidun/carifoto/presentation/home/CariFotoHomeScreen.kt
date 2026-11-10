package com.zaidun.carifoto.presentation.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zaidun.carifoto.data.model.DriveFolderItem
import com.zaidun.carifoto.data.model.DrivePhotoItem
import com.zaidun.carifoto.data.remote.CariFotoSessionDto

private val BackgroundDark = Color(0xFF100E17)
private val SurfaceDark = Color(0xFF181520)
private val BorderDark = Color(0xFF302A3A)
private val PurplePrimary = Color(0xFF7B4FE0)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAA1B8)

@Composable
fun CariFotoHomeScreen(
    viewModel: CariFotoHomeViewModel,
    onNavigateToOrders: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchTriggered by remember { mutableStateOf(false) }

    val selectedSession = state.sessions.firstOrNull {
        it.id == state.selectedSessionId
    }

    val availableDatesForSelectedLocation = remember(state.sessions, state.selectedLocation) {
        if (state.selectedLocation.isBlank()) {
            emptyList()
        } else {
            state.sessions
                .filter { it.location.contains(state.selectedLocation, ignoreCase = true) }
                .mapNotNull { it.sessionDate?.take(10) }
                .filter { it.isNotBlank() }
                .distinct()
        }
    }

    val visibleSessions = state.sessions.filter { session ->
        val matchLocation =
            state.selectedLocation.isBlank() ||
                    session.location.contains(
                        state.selectedLocation,
                        ignoreCase = true
                    )

        val matchDate =
            state.selectedDate.isBlank() ||
                    session.sessionDate?.take(10) == state.selectedDate

        val isPublished = session.isPublished != false

        matchLocation && matchDate && isPublished
    }

    if (selectedSession == null) {
        // UNIFIED SCROLL LAZYCOLUMN (Scroll Menyatu agar Card Filter ikut tergulung ke atas & list sesi tampil Full Screen)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. TOP HEADER BRAND
            item {
                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CariFoto",
                            color = TextPrimary,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Temukan momen berkendaramu",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    TextButton(onClick = onNavigateToOrders) {
                        Text(
                            text = "Pesanan",
                            color = PurplePrimary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // 2. CARD CONTAINER FILTER LOKASI / EVENT & TANGGAL
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Lokasi / Event Header
                        Text(
                            text = "📍 Pilih Lokasi / Event",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.availableLocations) { location ->
                                FilterChip(
                                    selected = state.selectedLocation == location,
                                    onClick = {
                                        val newLoc = if (state.selectedLocation == location) "" else location
                                        viewModel.selectLocation(newLoc)
                                        viewModel.selectDate("")
                                    },
                                    label = { Text(location, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PurplePrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = BackgroundDark,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Tanggal Hunting Header (Ditampilkan bertahap)
                        Text(
                            text = "📅 Pilih Tanggal Hunting",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(6.dp))

                        if (state.selectedLocation.isBlank()) {
                            Text(
                                text = "Pilih Lokasi / Event terlebih dahulu",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        } else if (availableDatesForSelectedLocation.isEmpty()) {
                            Text(
                                text = "Belum ada riwayat tanggal untuk lokasi ini",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(availableDatesForSelectedLocation) { date ->
                                    FilterChip(
                                        selected = state.selectedDate == date,
                                        onClick = {
                                            viewModel.selectDate(
                                                if (state.selectedDate == date) "" else date
                                            )
                                        },
                                        label = { Text(date, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PurplePrimary,
                                            selectedLabelColor = Color.White,
                                            containerColor = BackgroundDark,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Tombol Cari Sesi Foto
                        Button(
                            onClick = { isSearchTriggered = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cari Folder Sesi 🔍", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }

            // 3. HASIL FOLDER SESI FOTOGRAFER
            if (isSearchTriggered || state.selectedLocation.isNotBlank() || state.selectedDate.isNotBlank()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Folder sesi fotografer",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PurplePrimary
                            )
                        }
                    }
                } else if (visibleSessions.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Belum ada sesi hunting",
                            description = "Coba pilih lokasi atau tanggal lainnya."
                        )
                    }
                } else {
                    items(
                        items = visibleSessions,
                        key = { it.id }
                    ) { session ->
                        SessionFolderCard(
                            session = session,
                            photoCount = state.photos.count {
                                it.sessionId == session.id
                            },
                            onClick = {
                                viewModel.selectSession(session.id)
                            }
                        )
                    }
                }
            } else {
                item {
                    EmptyState(
                        title = "Pilih Lokasi & Tanggal",
                        description = "Tentukan lokasi dan tanggal hunting lalu klik tombol 'Cari Folder Sesi'."
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(horizontal = 18.dp)
        ) {
            SessionGallery(
                session = selectedSession,
                folders = state.driveFolders,
                folderStack = state.driveFolderStack,
                photos = state.filteredDrivePhotos,
                searchQuery = state.searchQuery,
                isLoading = state.isLoading,
                onSearchChange = viewModel::updateSearchQuery,
                onOpenFolder = viewModel::openDriveFolder,
                onBack = viewModel::backDriveFolder
            )
        }
    }
}

// Kartu Sesi Fotografer yang Super Pendek & Ringkas (1 Baris)
@Composable
private fun SessionFolderCard(
    session: CariFotoSessionDto,
    photoCount: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val rawIg = session.instagramLink?.trim()?.removePrefix("@") ?: ""
    val rawWa = session.whatsappNumber?.trim()?.replace("+", "")?.replace(" ", "")?.replace("-", "") ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        border = BorderStroke(
            1.dp,
            BorderDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📁",
                fontSize = 24.sp
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.location.ifBlank { "Lokasi Hunting" },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (rawIg.isNotBlank()) {
                        Text(
                            text = "@$rawIg",
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$rawIg"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membuka Instagram", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    if (rawIg.isNotBlank() && rawWa.isNotBlank()) {
                        Text("  •  ", color = TextSecondary, fontSize = 11.sp)
                    }

                    if (rawWa.isNotBlank()) {
                        val waNumber = if (rawWa.startsWith("0")) "62" + rawWa.substring(1) else rawWa
                        Text(
                            text = "💬 WA",
                            color = Color(0xFF25D366),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNumber"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membuka WhatsApp", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            Surface(
                color = PurplePrimary.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onClick() }
            ) {
                Text(
                    text = "BUKA  ›",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionGallery(
    session: CariFotoSessionDto,
    folders: List<DriveFolderItem>,
    folderStack: List<DriveFolderItem>,
    photos: List<DrivePhotoItem>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onOpenFolder: (DriveFolderItem) -> Unit,
    onBack: () -> Unit
) {
    var gridColumnCount by remember { mutableIntStateOf(2) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom < 0.82f && gridColumnCount == 2) {
                        gridColumnCount = 3
                    } else if (zoom > 1.18f && gridColumnCount == 3) {
                        gridColumnCount = 2
                    }
                }
            }
    ) {
        // Top Header: Tombol Back & Breadcrumb Sesi (Rapi & Minimalis)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "‹ Kembali",
                    color = PurplePrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(14.dp))

            // Breadcrumb Sesi & Folder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "📁 Sesi",
                    color = if (folderStack.isEmpty()) PurplePrimary else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                folderStack.forEach { folder ->
                    Text(
                        text = "  /  ${folder.name}",
                        color = PurplePrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PurplePrimary
                    )
                }
            }

            folders.isEmpty() && photos.isEmpty() -> {
                EmptyState(
                    title = "Folder masih kosong",
                    description = "Belum ada folder atau foto di lokasi ini."
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    // Tampilkan subfolder
                    items(
                        items = folders,
                        key = { "folder_${it.id}" }
                    ) { folder ->
                        DriveFolderCard(
                            folder = folder,
                            onClick = {
                                onOpenFolder(folder)
                            }
                        )
                    }

                    // Informasi jumlah foto & Tombol Ubah Layout Grid (2 Kolom / 3 Kolom)
                    if (photos.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${photos.size} foto ditemukan",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                Spacer(Modifier.width(8.dp))

                                Surface(
                                    color = SurfaceDark,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderDark),
                                    modifier = Modifier.clickable {
                                        gridColumnCount = if (gridColumnCount == 2) 3 else 2
                                    }
                                ) {
                                    Text(
                                        text = if (gridColumnCount == 2) "Grid: 2 Kolom" else "Grid: 3 Kolom",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Tampilkan foto dalam Barisan Chunks (2 Kolom atau 3 Kolom)
                        val photoRows = photos.chunked(gridColumnCount)

                        items(
                            items = photoRows,
                            key = { row -> "row_${row.firstOrNull()?.id ?: System.currentTimeMillis()}" }
                        ) { rowPhotos ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPhotos.forEach { photo ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        PhotoPreviewCard(
                                            photo = photo,
                                            columnCount = gridColumnCount
                                        )
                                    }
                                }

                                // Isi slot kosong jika jumlah item dalam baris kurang dari gridColumnCount
                                repeat(gridColumnCount - rowPhotos.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriveFolderCard(
    folder: DriveFolderItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        border = BorderStroke(
            1.dp,
            BorderDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📁",
                fontSize = 27.sp
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = folder.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Folder Google Drive",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Text(
                text = "BUKA  ›",
                color = PurplePrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PhotoPreviewCard(
    photo: DrivePhotoItem,
    columnCount: Int = 2
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        border = BorderStroke(
            1.dp,
            BorderDark
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (columnCount == 2) 175.dp else 125.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photo.imageUrl,
                    contentDescription = photo.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = photo.name,
                    color = TextPrimary,
                    fontSize = if (columnCount == 2) 11.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Google Drive",
                    color = TextSecondary,
                    fontSize = if (columnCount == 2) 10.sp else 8.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
