
package com.zaidun.carifoto.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zaidun.carifoto.data.model.DrivePhotoItem
import com.zaidun.carifoto.data.remote.CariFotoDriveHelper
import com.zaidun.carifoto.data.model.DriveFolderItem
import com.zaidun.carifoto.data.remote.CariFotoPhotoDto
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

    val selectedSession = state.sessions.firstOrNull {
        it.id == state.selectedSessionId
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 18.dp)
    ) {
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

        Spacer(Modifier.height(20.dp))

        if (selectedSession == null) {
            Text(
                text = "Lokasi hunting",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.availableLocations) { location ->
                    FilterChip(
                        selected = state.selectedLocation == location,
                        onClick = {
                            viewModel.selectLocation(
                                if (state.selectedLocation == location) ""
                                else location
                            )
                        },
                        label = { Text(location) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurplePrimary,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Tanggal hunting",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.availableDates) { date ->
                    FilterChip(
                        selected = state.selectedDate == date,
                        onClick = {
                            viewModel.selectDate(
                                if (state.selectedDate == date) "" else date
                            )
                        },
                        label = { Text(date) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurplePrimary,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Folder sesi fotografer",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading -> {
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

                visibleSessions.isEmpty() -> {
                    EmptyState(
                        title = "Belum ada sesi hunting",
                        description = "Coba pilih lokasi atau tanggal lainnya."
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(
                            items = visibleSessions,
                            key = { it.id }
                        ) { session ->
                            val photographer = state.users.firstOrNull {
                                it.id == session.photographerId
                            }

                            SessionFolderCard(
                                session = session,
                                photographerName =
                                    photographer?.name ?: "Fotografer",
                                photoCount = state.photos.count {
                                    it.sessionId == session.id
                                },
                                onClick = {
                                    viewModel.selectSession(session.id)
                                }
                            )
                        }
                    }
                }
            }
        } else {
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

@Composable
private fun SessionFolderCard(
    session: CariFotoSessionDto,
    photographerName: String,
    photoCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            BorderDark
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📁",
                    fontSize = 30.sp
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = session.location,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = photographerName,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "BUKA",
                    color = PurplePrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            HorizontalDivider(color = BorderDark)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tanggal",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = session.sessionDate?.take(10) ?: "-",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }

                Column {
                    Text(
                        text = "Jumlah foto",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Google Drive",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }

                Column {
                    Text(
                        text = "Harga",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Rp${session.priceSingle ?: 10000}",
                        color = PurplePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
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
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text(
                    text = "‹ Kembali",
                    color = PurplePrimary
                )
            }

            Spacer(Modifier.width(8.dp))

            Column {
                Text(
                    text = session.location,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Text(
                    text = session.sessionDate?.take(10) ?: "-",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Breadcrumb lokasi folder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📁 Sesi",
                color = if (folderStack.isEmpty()) PurplePrimary else TextSecondary,
                fontSize = 12.sp
            )

            folderStack.forEach { folder ->
                Text(
                    text = "  /  ${folder.name}",
                    color = PurplePrimary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        Text(
            text = "Isi Folder",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(10.dp))

        // Pencarian foto
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Cari nama file foto...")
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PurplePrimary,
                unfocusedBorderColor = BorderDark,
                focusedPlaceholderColor = TextSecondary,
                unfocusedPlaceholderColor = TextSecondary,
                cursorColor = PurplePrimary
            )
        )

        Spacer(Modifier.height(14.dp))

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

                    // Informasi jumlah foto
                    if (photos.isNotEmpty()) {
                        item {
                            Text(
                                text = "${photos.size} foto ditemukan",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(
                                    top = 8.dp,
                                    bottom = 2.dp
                                )
                            )
                        }

                        // Tampilkan foto
                        items(
                            items = photos,
                            key = { "photo_${it.id}" }
                        ) { photo ->
                            PhotoPreviewCard(photo = photo)
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
        border = androidx.compose.foundation.BorderStroke(
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
    photo: DrivePhotoItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            BorderDark
        )
    ) {
        Column {
            AsyncImage(
                model = photo.imageUrl,
                contentDescription = photo.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier.padding(9.dp)
            ) {
                Text(
                    text = photo.name,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                Text(
                    text = "Google Drive",
                    color = TextSecondary,
                    fontSize = 10.sp
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
