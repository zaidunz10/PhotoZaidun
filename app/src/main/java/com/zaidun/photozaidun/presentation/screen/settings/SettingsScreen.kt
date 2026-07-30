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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.PI
import kotlin.math.sin

// --- Palet warna tema ungu, konsisten dengan HomeScreen ---
private val DeepPurple = Color(0xFF3B1F63)
private val AccentPurple = Color(0xFF7B4FE0)
private val ScreenBg = Color(0xFFF7F4FC)
private val TitleDark = Color(0xFF2A1A44)
private val SubtleGray = Color(0xFF6B6079)
private val LabelGray = Color(0xFF8A7F98)
private val ConnectedGreen = Color(0xFF3FBF6F)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val consentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.loginGoogle(
                    activity = context as Activity,
                    onNeedConsent = { },
                    onError = { }
                )
            }
        }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = { Text("Animated Export Settings", fontWeight = FontWeight.Bold, color = TitleDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TitleDark)
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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. Koneksi Cloud
            item {
                SectionLabel("Koneksi Cloud")
                Spacer(Modifier.height(12.dp))

                if (uiState.googleUser == null) {
                    Button(
                        onClick = {
                            viewModel.loginGoogle(
                                activity = context as Activity,
                                onNeedConsent = { sender ->
                                    consentLauncher.launch(
                                        IntentSenderRequest.Builder(sender).build()
                                    )
                                },
                                onError = { it.printStackTrace() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                    ) {
                        Icon(Icons.Default.CloudUpload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Login ke My Drive")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(AccentPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = uiState.googleUser?.name?.trim()?.firstOrNull()?.uppercase() ?: "?"
                                Text(initial, fontWeight = FontWeight.Bold, color = DeepPurple)
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    uiState.googleUser?.name ?: "User",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TitleDark
                                )
                                Text(
                                    uiState.googleUser?.email ?: "",
                                    fontSize = 11.sp,
                                    color = LabelGray
                                )
                            }

                            Surface(
                                color = ConnectedGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    "Connected",
                                    color = ConnectedGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            TextButton(onClick = { viewModel.logoutGoogle(context as Activity) }) {
                                Text("Logout", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (uiState.driveConnected) ConnectedGreen else Color.Gray)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.driveConnected) "Terhubung dengan Drive" else "Belum terhubung dengan Drive",
                            fontSize = 12.sp,
                            color = SubtleGray
                        )
                    }
                }
            }

            // Upload Otomatis
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Upload Otomatis ke Google Drive",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TitleDark
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Setelah export selesai, folder akan langsung diupload ke Google Drive.",
                                fontSize = 11.sp,
                                color = LabelGray
                            )
                        }
                        Switch(
                            checked = uiState.autoUpload,
                            onCheckedChange = { viewModel.saveAutoUpload(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentPurple
                            )
                        )
                    }
                }
            }

            // 2. Struktur Folder & Penamaan — accordion style
            item {
                SectionLabel("Struktur Folder & Penamaan")
                Spacer(Modifier.height(12.dp))
            }

            item {
                ExpandableFolderField(
                    icon = Icons.Default.Folder,
                    label = "Induk Folder (Level 1)",
                    value = uiState.rootFolder,
                    onValueChange = { viewModel.saveRootFolder(it) },
                    fieldLabel = "Nama Induk Folder",
                    initiallyExpanded = true
                )

            }


            item {
                Spacer(Modifier.height(10.dp))
                uiState.folderLevels.forEachIndexed { index, folder ->

                    ExpandableFolderField(

                        icon = Icons.Default.CreateNewFolder,

                        label = "Sub Folder ${index + 1}",

                        value = folder.name,

                        onValueChange = {
                            viewModel.updateFolderLevel(
                                folder.id,
                                it
                            )
                        },

                        fieldLabel = "Nama Folder",

                        placeholder = "Contoh : Touring"

                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    TextButton(

                        onClick = {
                            viewModel.removeFolderLevel(folder.id)
                        }

                    ) {

                        Icon(
                            Icons.Default.Delete,
                            null
                        )

                        Spacer(
                            Modifier.width(6.dp)
                        )

                        Text("Hapus")

                    }

                    HorizontalDivider()

                }
                Button(

                    onClick = {

                        viewModel.addFolderLevel()

                    },

                    modifier = Modifier.fillMaxWidth()

                ) {

                    Icon(
                        Icons.Default.Add,
                        null
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text("Tambah Sub Folder")

                }

            }

            item {
                Spacer(Modifier.height(10.dp))
                ExpandableFolderField(
                    icon = Icons.Default.Numbers,
                    label = "Prefix Nama Part (Level 3)",
                    value = uiState.partFolder,
                    onValueChange = { viewModel.savePartFolder(it) },
                    fieldLabel = "Prefix Nama Part",
                    placeholder = "Contoh: Part",
                    supportingText = "Akan menjadi ${uiState.partFolder}1, dst."
                )
            }

            item {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    // Tambahkan .toString() karena TextField hanya mau Teks
                    value = uiState.startPartNumber.toString(),
                    onValueChange = {
                        // Ambil angka saja, lalu ubah ke Int untuk disimpan ke ViewModel
                        val newValue = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 1
                        viewModel.saveStartPart(newValue)
                    },
                    label = { Text("Mulai dari Part Ke-") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = AccentPurple) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        focusedLabelColor = AccentPurple
                    )
                )
            }

            item {
                Spacer(Modifier.height(10.dp))
                ExpandableFolderField(
                    icon = Icons.Default.DriveFileRenameOutline,
                    label = "Nama Tambahan File",
                    value = uiState.fileSuffix,
                    onValueChange = { viewModel.saveFileSuffix(it) },
                    fieldLabel = "Nama Tambahan File",
                    supportingText = if (uiState.fileSuffix.isBlank())
                        "Contoh: IMG_01.jpg"
                    else
                        "Contoh: IMG_01_${uiState.fileSuffix}.jpg"
                )
            }

            // 3. Limit & Kualitas
            item {
                Spacer(Modifier.height(6.dp))
                SectionLabel("Limit & Kualitas")
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.maxPhotoPerFolder,
                    onValueChange = {
                        val value = it.filter(Char::isDigit)
                        if (value.length <= 5) viewModel.saveMaxPhoto(value)
                    },
                    label = { Text("Jumlah Foto per Folder") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Numbers, null, tint = AccentPurple) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        focusedLabelColor = AccentPurple
                    )
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    "Resolusi Export: ${uiState.resizePercent}%",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TitleDark
                )
                Spacer(Modifier.height(10.dp))

                WaveSlider(
                    value = uiState.resizePercent.toFloatOrNull() ?: 100f,
                    onValueChange = { viewModel.saveResizePercent(it.toInt().toString()) },
                    valueRange = 10f..100f
                )
            }

            // Preview Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentPurple.copy(alpha = 0.08f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Preview Lokasi Cloud:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TitleDark
                        )
                        Spacer(Modifier.height(4.dp))
                        val previewFile =
                            if (uiState.fileSuffix.isBlank()) {
                                "IMG_01.jpg"
                            } else {
                                "IMG_01_${uiState.fileSuffix}.jpg"
                            }
                        val previewPath =
                            buildString {

                                append("My Drive")
                                append(" / ")
                                append(uiState.rootFolder)

                                uiState.folderLevels.forEach {

                                    if (it.name.isNotBlank()) {
                                        append(" / ")
                                        append(it.name)
                                    }

                                }

                                append(" / ")
                                append(uiState.partFolder)
                                append("1")
                                append(" / ")
                                append(previewFile)

                            }

                        Text(
                            previewPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = SubtleGray
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Copyright by zaidunz_photo",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.Bold, color = DeepPurple, fontSize = 14.sp)
}

/**
 * Baris folder yang bisa di-expand/collapse.
 * Collapsed: menampilkan value saat ini di kanan + chevron.
 * Expanded: menampilkan text field untuk mengedit value.
 */
@Composable
private fun ExpandableFolderField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    fieldLabel: String,
    placeholder: String? = null,
    supportingText: String? = null,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "chevronRotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    color = TitleDark,
                    fontWeight = FontWeight.Medium
                )

                AnimatedVisibility(visible = !expanded, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        value.ifBlank { "-" },
                        fontSize = 12.sp,
                        color = LabelGray,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = LabelGray,
                    modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text(fieldLabel) },
                        placeholder = placeholder?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            focusedLabelColor = AccentPurple
                        )
                    )
                    if (supportingText != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(supportingText, fontSize = 11.sp, color = LabelGray)
                    }
                }
            }
        }
    }
}

/**
 * Slider bergaya "liquid wave" ungu — pengganti Slider Material biasa.
 * Bisa di-drag/tap seperti slider normal; gelombangnya landai & mengalir pelan,
 * dengan area di bawah kurva diisi gradient supaya terasa "penuh" bukan sekadar garis.
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
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
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
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF1ECFB))
            .padding(horizontal = 6.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
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
            val centerY = size.height / 2f
            val amplitude = size.height * 0.14f          // gelombang landai, tidak norak
            val waveLength = size.width / 2.2f           // hanya ~2 lekukan penuh, terasa tenang
            val activeWidth = (size.width * fraction).coerceIn(0f, size.width)
            val step = 4f

            fun waveY(x: Float) = centerY + amplitude * sin((x / waveLength) + phase)

            // Path kurva gelombang dari 0 sampai activeWidth
            val wavePath = Path().apply {
                moveTo(0f, waveY(0f))
                var x = step
                while (x <= activeWidth) {
                    lineTo(x, waveY(x))
                    x += step
                }
                lineTo(activeWidth, waveY(activeWidth))
            }

            // Area terisi di bawah kurva — kesan "liquid", bukan cuma garis
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
                        listOf(AccentPurple.copy(alpha = 0.28f), AccentPurple.copy(alpha = 0.02f))
                    )
                )
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(listOf(AccentPurple, DeepPurple)),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )
            }

            // sisa track setelah posisi aktif — garis tipis datar, netral
            if (activeWidth < size.width) {
                drawLine(
                    color = Color(0xFFD9CFEF),
                    start = Offset(activeWidth, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // thumb dengan ring lembut
            val thumbY = waveY(activeWidth)
            drawCircle(color = DeepPurple.copy(alpha = 0.18f), radius = 14.dp.toPx(), center = Offset(activeWidth, thumbY))
            drawCircle(color = DeepPurple, radius = 9.dp.toPx(), center = Offset(activeWidth, thumbY))
            drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = Offset(activeWidth, thumbY))
        }
    }
}