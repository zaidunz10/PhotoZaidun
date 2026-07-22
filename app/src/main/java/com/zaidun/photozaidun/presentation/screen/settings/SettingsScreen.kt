package com.zaidun.photozaidun.presentation.screen.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.PI
import kotlin.math.sin

// --- Palet Warna disesuaikan persis dengan gambar ---
private val ScreenBg = Color(0xFFF4F5F9)
private val CardBg = Color(0xFFFFFFFF)
private val PurpleAccent = Color(0xFF7C3AED) // Ungu persis seperti gambar
private val TextDark = Color(0xFF1E1E24)
private val TextGray = Color(0xFF8A8A8E)
private val ConnectedGreenText = Color(0xFF137333)
private val ConnectedGreenBg = Color(0xFFE6F4EA)
private val DividerColor = Color(0xFFF0F0F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loginGoogle(activity = context as Activity, onNeedConsent = { }, onError = { })
        }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = { Text("Animated Export Settings", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= 1. KONEKSI CLOUD =================
            item {
                SectionLabel("Koneksi Cloud")

                if (uiState.googleUser == null) {
                    Button(
                        onClick = {
                            viewModel.loginGoogle(
                                activity = context as Activity,
                                onNeedConsent = { sender ->
                                    consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
                                },
                                onError = { it.printStackTrace() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                    ) {
                        Icon(Icons.Default.CloudUpload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Login ke My Drive")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // User Info Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(PurpleAccent.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initial = uiState.googleUser?.name?.trim()?.firstOrNull()?.uppercase() ?: "?"
                                    Text(initial, fontWeight = FontWeight.Bold, color = PurpleAccent)
                                }

                                Spacer(Modifier.width(12.dp))

                                // Name & Email
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(uiState.googleUser?.name ?: "User", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                    Text(uiState.googleUser?.email ?: "", fontSize = 12.sp, color = TextGray)
                                }

                                // Connected Pill
                                Box(
                                    modifier = Modifier
                                        .background(ConnectedGreenBg, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Connected", color = ConnectedGreenText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }

                                Spacer(Modifier.width(12.dp))

                                // Logout Button
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF3F4F6))
                                    .clickable { viewModel.logoutGoogle(context as Activity) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Logout", color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Status Indicator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.driveConnected) Color(0xFF34C759) else Color.Gray)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (uiState.driveConnected) "Terhubung dengan Drive" else "Belum terhubung",
                                    fontSize = 13.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ================= 2. UPLOAD OTOMATIS =================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Upload Otomatis ke Google Drive",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Switch(
                        checked = uiState.autoUpload,
                        onCheckedChange = { viewModel.saveAutoUpload(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PurpleAccent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            // ================= 3. STRUKTUR FOLDER & PENAMAAN =================
            item {
                SectionLabel("Struktur Folder & Penamaan")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        FolderRow(
                            icon = Icons.Default.Folder,
                            label = "Induk Folder (Level 1)",
                            value = uiState.rootFolder,
                            fieldLabel = "Nama Induk Folder",
                            onValueChange = { viewModel.saveRootFolder(it) },
                            initiallyExpanded = true // Terbuka secara default sesuai gambar
                        )

                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 48.dp))

                        FolderRow(
                            icon = Icons.Default.CreateNewFolder,
                            label = "Folder Project (Level 2)",
                            value = uiState.mainFolder,
                            fieldLabel = "Nama Folder Project",
                            onValueChange = { viewModel.saveMainFolder(it) }
                        )

                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 48.dp))

                        FolderRow(
                            icon = Icons.Default.Numbers,
                            label = "Prefix Nama Part (Level 3)",
                            value = uiState.partFolder,
                            fieldLabel = "Prefix Nama Part",
                            onValueChange = { viewModel.savePartFolder(it) }
                        )

                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 48.dp))

                        FolderRow(
                            icon = Icons.Default.DriveFileRenameOutline,
                            label = "Nama Tambahan File",
                            value = uiState.fileSuffix,
                            fieldLabel = "Nama Tambahan File",
                            onValueChange = { viewModel.saveFileSuffix(it) }
                        )
                    }
                }
            }

            // ================= 4. LIMIT & KUALITAS =================
            item {
                SectionLabel("Limit & Kualitas")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        OutlinedTextField(
                            value = uiState.maxPhotoPerFolder,
                            onValueChange = {
                                val value = it.filter(Char::isDigit)
                                if (value.length <= 5) viewModel.saveMaxPhoto(value)
                            },
                            label = { Text("Jumlah Foto per Folder") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent,
                                focusedLabelColor = PurpleAccent,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )

                        Spacer(Modifier.height(20.dp))

                        Text(
                            "Resolusi Export: ${uiState.resizePercent}%",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = TextDark
                        )

                        Spacer(Modifier.height(8.dp))

                        WaveSlider(
                            value = uiState.resizePercent.toFloatOrNull() ?: 100f,
                            onValueChange = { viewModel.saveResizePercent(it.toInt().toString()) },
                            valueRange = 10f..100f
                        )
                    }
                }
            }

            // ================= 5. PREVIEW LOKASI =================
            item {
                val previewFile = if (uiState.fileSuffix.isBlank()) "IMG_01.jpg" else "IMG_01_${uiState.fileSuffix}.jpg"
                Text(
                    text = "Preview Lokasi Cloud: My Drive / ${uiState.rootFolder} / ${uiState.mainFolder} / ${uiState.partFolder}1 / $previewFile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
                Spacer(Modifier.height(32.dp)) // Jarak ekstra di bawah
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        fontSize = 15.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

/**
 * Komponen Baris Folder bergaya Accordion persis seperti di gambar.
 */
@Composable
private fun FolderRow(
    icon: ImageVector,
    label: String,
    value: String,
    fieldLabel: String,
    onValueChange: (String) -> Unit,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "chevronRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))

            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = TextDark,
                fontWeight = FontWeight.Medium
            )

            // Tampilkan value di kanan hanya jika collapsed
            AnimatedVisibility(visible = !expanded, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = value.ifBlank { "-" },
                    fontSize = 14.sp,
                    color = TextDark,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp, start = 34.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(fieldLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        focusedLabelColor = PurpleAccent,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        }
    }
}

/**
 * Slider bergaya "liquid wave". Skema warnanya disesuaikan
 * agar warnanya ungu solid (`#7C3AED`) persis seperti gambar.
 */
@Composable
private fun WaveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "wavePhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "phase"
    )

    fun updateFromX(x: Float, width: Float) {
        if (width <= 0f) return
        val newFraction = (x / width).coerceIn(0f, 1f)
        val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
        onValueChange(newValue)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(valueRange) {
                detectTapGestures { offset -> updateFromX(offset.x, size.width.toFloat()) }
            }
            .pointerInput(valueRange) {
                detectDragGestures { change, _ ->
                    change.consume()
                    updateFromX(change.position.x, size.width.toFloat())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val amplitude = size.height * 0.15f
            val waveLength = size.width / 2.5f
            val activeWidth = (size.width * fraction).coerceIn(0f, size.width)
            val step = 4f

            fun waveY(x: Float) = centerY + amplitude * sin((x / waveLength) + phase)

            val wavePath = Path().apply {
                moveTo(0f, waveY(0f))
                var x = step
                while (x <= activeWidth) {
                    lineTo(x, waveY(x))
                    x += step
                }
                lineTo(activeWidth, waveY(activeWidth))
            }

            // Area terisi di bawah kurva (Warna Ungu)
            if (activeWidth > 0f) {
                val fillPath = Path().apply {
                    addPath(wavePath)
                    lineTo(activeWidth, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(PurpleAccent.copy(alpha = 0.6f), PurpleAccent.copy(alpha = 0.1f))
                    )
                )
                drawPath(
                    path = wavePath,
                    color = PurpleAccent,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Sisa track abu-abu pucat (Inactive track)
            if (activeWidth < size.width) {
                drawLine(
                    color = Color(0xFFE5E5EA),
                    start = Offset(activeWidth, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Thumb Slider (Lingkaran Putih dengan border ungu)
            val thumbY = waveY(activeWidth)
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(activeWidth, thumbY))
            drawCircle(color = PurpleAccent, radius = 10.dp.toPx(), center = Offset(activeWidth, thumbY), style = Stroke(width = 3.dp.toPx()))
        }
    }
}