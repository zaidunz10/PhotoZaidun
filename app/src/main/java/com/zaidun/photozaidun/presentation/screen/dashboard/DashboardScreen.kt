package com.zaidun.photozaidun.presentation.screen.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaidun.photozaidun.data.remote.SessionLocationDto
import com.zaidun.photozaidun.utils.toReadableSize

private val BackgroundDark = Color(0xFF100E17)
private val SurfaceDark = Color(0xFF181520)
private val BorderDark = Color(0xFF302A3A)
private val PurplePrimary = Color(0xFF7B4FE0)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAA1B8)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var sessionToDelete by remember { mutableStateOf<SessionLocationDto?>(null) }
    var isDeletingId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Scaffold(
        containerColor = BackgroundDark
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // Header TopBar
                item {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Dasbor Fotografer",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                uiState.photographerEmail.ifBlank { "Kelola sesi foto & link Vercel" },
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        IconButton(onClick = { viewModel.loadDashboardData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PurplePrimary)
                        }
                    }
                }

                // Kartu Status Google Drive Storage (Super Ringkas & Anti Vertikal-Wrap)
                item {
                    val storage = uiState.driveStorage
                    val usedStr = storage?.usedBytes?.toReadableSize() ?: "12,4 GB"
                    val totalStr = when {
                        storage == null -> "5 TB"
                        storage.totalBytes <= 0L -> "5 TB (Workspace)"
                        else -> storage.totalBytes.toReadableSize()
                    }
                    val percentValue = if (storage != null && storage.totalBytes > 0L) (storage.percent * 100f) else 5.0f

                    val animatedPercent by animateFloatAsState(
                        targetValue = percentValue,
                        animationSpec = tween(600),
                        label = "percentAnim"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E184D)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
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
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Penyimpanan Drive",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
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
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
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

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(usedStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(Modifier.width(4.dp))
                                    Text("/ $totalStr", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                }

                                Text("${percentValue.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB89EFF))
                            }

                            Spacer(Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { (animatedPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = Color(0xFFB89EFF),
                                trackColor = Color(0xFF4D3375)
                            )
                        }
                    }
                }

                // Section Header: Riwayat Sesi Saya
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Riwayat Sesi Saya (${uiState.mySessions.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // List Sesi Fotografer
                if (uiState.isLoading && uiState.mySessions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PurplePrimary)
                        }
                    }
                } else if (uiState.mySessions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Belum Ada Riwayat Sesi", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                Text("Ekspor foto pertama Anda untuk melihat riwayat sesi.", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.mySessions,
                        key = { it.id }
                    ) { session ->
                        SessionHistoryCard(
                            session = session,
                            isDeleting = isDeletingId == session.id,
                            onCopyVercelLink = { viewModel.copyVercelWebLink(context, session.id) },
                            onDeleteSessionClick = { sessionToDelete = session }
                        )
                    }
                }
            }

            // POP-UP CONFIRMATION ALERT DIALOG DUA TAHAP SEBELUM HAPUS SESI & GOOGLE DRIVE
            sessionToDelete?.let { session ->
                var deleteDriveFolder by remember { mutableStateOf(false) }
                var showSecondDriveAlert by remember { mutableStateOf(false) }

                if (!showSecondDriveAlert) {
                    // ALERT 1: Konfirmasi Hapus Sesi dari Rider + Opsi Checkbox Google Drive
                    AlertDialog(
                        onDismissRequest = {
                            if (isDeletingId == null) sessionToDelete = null
                        },
                        title = {
                            Text("Konfirmasi Hapus Sesi", fontWeight = FontWeight.Bold, color = TextPrimary)
                        },
                        text = {
                            Column {
                                Text(
                                    "Apakah Anda yakin ingin menghapus sesi ${session.location.ifBlank { "ini" }} dari Rider?",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { deleteDriveFolder = !deleteDriveFolder }
                                ) {
                                    Checkbox(
                                        checked = deleteDriveFolder,
                                        onCheckedChange = { deleteDriveFolder = it },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444), uncheckedColor = TextSecondary)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Apakah folder yang dari Google Drive mau dihapus juga?",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (deleteDriveFolder) Color(0xFFEF4444) else TextPrimary
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (deleteDriveFolder) {
                                        // Tampilkan Alert Konfirmasi Ke-2 Khusus Google Drive
                                        showSecondDriveAlert = true
                                    } else {
                                        val id = session.id
                                        isDeletingId = id
                                        viewModel.deleteSession(
                                            sessionId = id,
                                            driveFolderUrl = session.driveFolderId,
                                            deleteFromDrive = false
                                        ) {
                                            isDeletingId = null
                                            sessionToDelete = null
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                enabled = isDeletingId == null,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isDeletingId == session.id) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("Hapus", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { sessionToDelete = null },
                                enabled = isDeletingId == null
                            ) {
                                Text("Batal", color = TextSecondary, fontSize = 12.sp)
                            }
                        },
                        containerColor = SurfaceDark,
                        shape = RoundedCornerShape(18.dp)
                    )
                } else {
                    // ALERT 2: Konfirmasi Hapus Folder Google Drive Secara Permanen
                    AlertDialog(
                        onDismissRequest = {
                            if (isDeletingId == null) showSecondDriveAlert = false
                        },
                        title = {
                            Text("⚠️ Hapus Folder Google Drive?", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        },
                        text = {
                            Text(
                                "Anda yakin ingin DELETE folder beserta SELURUH FOTO di Google Drive secara permanen?",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val id = session.id
                                    isDeletingId = id
                                    viewModel.deleteSession(
                                        sessionId = id,
                                        driveFolderUrl = session.driveFolderId,
                                        deleteFromDrive = true
                                    ) {
                                        isDeletingId = null
                                        sessionToDelete = null
                                        showSecondDriveAlert = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                enabled = isDeletingId == null,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isDeletingId == session.id) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("YA, HAPUS PERMANEN", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showSecondDriveAlert = false },
                                enabled = isDeletingId == null
                            ) {
                                Text("Batal", color = TextSecondary, fontSize = 12.sp)
                            }
                        },
                        containerColor = SurfaceDark,
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(
    session: SessionLocationDto,
    isDeleting: Boolean = false,
    onCopyVercelLink: () -> Unit,
    onDeleteSessionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📁", fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            session.location.ifBlank { "Lokasi Hunting" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            session.sessionDate?.take(10) ?: "Hari Ini",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = Color(0xFF25D366).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "Terupload ke Cari Foto",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF25D366),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tombol Salin Link Website Vercel
                Button(
                    onClick = onCopyVercelLink,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("📋 Salin Link Cari Foto", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }

                // Tombol Hapus Sesi dari Supabase (Dengan Loading Indicator)
                IconButton(
                    onClick = onDeleteSessionClick,
                    enabled = !isDeleting,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(color = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus Sesi", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
