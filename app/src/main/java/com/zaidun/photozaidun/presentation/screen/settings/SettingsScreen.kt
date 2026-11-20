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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalClipboardManager
import com.zaidun.photozaidun.data.remote.gemini.GeminiApiKeyItem
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
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(AccentPurple.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initial = uiState.googleUser?.name?.trim()?.firstOrNull()?.uppercase() ?: "?"
                                        Text(initial, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepPurple)
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            uiState.googleUser?.name ?: "Guest",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TitleDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            uiState.googleUser?.email ?: "Belum Login",
                                            fontSize = 11.sp,
                                            color = LabelGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                TextButton(
                                    onClick = { viewModel.resetLocalAccountOnly() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Logout", color = Color.Red.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.driveConnected) ConnectedGreen else Color.Gray)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (uiState.driveConnected) "Terhubung dengan Google Drive" else "Belum terhubung dengan Drive",
                                    fontSize = 11.sp,
                                    color = SubtleGray,
                                    maxLines = 1
                                )
                            }
                        }
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
            item {
                Spacer(Modifier.height(10.dp))
                SectionLabel("Sub-Folder Part & Batas Foto")
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Gunakan Sub-Folder Part", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TitleDark)
                        Text("Membagi foto per part folder (part 1, part 2...)", fontSize = 11.sp, color = SubtleGray)
                    }
                    Switch(
                        checked = uiState.showPart,
                        onCheckedChange = { viewModel.saveShowPart(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentPurple)
                    )
                }

                if (uiState.showPart) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.maxPhotoPerFolder,
                            onValueChange = {
                                val v = it.filter(Char::isDigit)
                                if (v.length <= 4) viewModel.saveMaxPhoto(v)
                            },
                            label = { Text("Foto per Part", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple)
                        )

                        OutlinedTextField(
                            value = uiState.startPartNumber.toString(),
                            onValueChange = {
                                val v = it.filter(Char::isDigit).toIntOrNull() ?: 1
                                viewModel.saveStartPartNumber(v)
                            },
                            label = { Text("Mulai dari Part #", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                SectionLabel("Resolusi Export: ${uiState.resizePercent}%")
                Spacer(Modifier.height(12.dp))

                WaveSlider(
                    value = uiState.resizePercent.toFloatOrNull() ?: 100f,
                    onValueChange = { viewModel.saveResizePercent(it.toInt().toString()) },
                    valueRange = 10f..100f
                )
            }



            // Section AI Auto-Tagging Gemini
            item {
                GeminiSettingsSection(uiState = uiState, viewModel = viewModel)
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
                    text = "Copyright by zaidun_photo",
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

@Composable
private fun GeminiSettingsSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var inputLabel by remember { mutableStateOf("") }
    var inputKey by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🤖 AI Auto-Tagging (Gemini API)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TitleDark
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "Pilih model Gemini & kelola API Key untuk tag otomatis model motor, plat, dan warna.",
                fontSize = 11.sp,
                color = SubtleGray
            )

            Spacer(Modifier.height(14.dp))
            Text("Pilihan Model Gemini:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TitleDark)
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.saveSelectedGeminiModel("gemini-2.5-flash-lite") }) {
                RadioButton(
                    selected = uiState.selectedGeminiModel == "gemini-2.5-flash-lite",
                    onClick = { viewModel.saveSelectedGeminiModel("gemini-2.5-flash-lite") }
                )
                Text("Gemini 2.5 Flash-Lite (Cepat & Hemat)", fontSize = 12.sp, color = TitleDark)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.saveSelectedGeminiModel("gemini-3.5-flash-lite") }) {
                RadioButton(
                    selected = uiState.selectedGeminiModel == "gemini-3.5-flash-lite",
                    onClick = { viewModel.saveSelectedGeminiModel("gemini-3.5-flash-lite") }
                )
                Text("Gemini 3.5 Flash-Lite (Akurasi Tinggi)", fontSize = 12.sp, color = TitleDark)
            }

            Spacer(Modifier.height(14.dp))

            // Tombol Buat API Key Gratis
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/api-keys"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🔑 Buat API Key Gratis di Google AI Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            Text("Tambah API Key Baru:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TitleDark)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = inputLabel,
                onValueChange = { inputLabel = it },
                label = { Text("Label (Opsional, misal: Akun 1)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text("API Key (AIzaSy...)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        val clipText = clipboardManager.getText()?.text
                        if (!clipText.isNullOrBlank()) inputKey = clipText
                    }
                ) {
                    Text("Tempel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (uiState.keyVerifyError != null) {
                Spacer(Modifier.height(4.dp))
                Text(uiState.keyVerifyError!!, color = Color.Red, fontSize = 11.sp)
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    viewModel.verifyAndAddGeminiKey(
                        label = inputLabel,
                        apiKey = inputKey,
                        onSuccess = {
                            inputLabel = ""
                            inputKey = ""
                            Toast.makeText(context, "API Key Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                enabled = !uiState.isVerifyingKey && inputKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                if (uiState.isVerifyingKey) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Memverifikasi Key...", fontSize = 12.sp)
                } else {
                    Text("Tes & Simpan Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daftar API Key Tersimpan (${uiState.geminiApiKeys.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TitleDark)

                if (uiState.geminiApiKeys.any { it.isQuotaExceeded }) {
                    TextButton(onClick = { viewModel.resetGeminiQuotaFlags() }) {
                        Text("Reset Limit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.geminiApiKeys.isEmpty()) {
                Surface(
                    color = Color.Yellow.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "⚠️ Belum ada API Key aktif. Ekspor & upload tetap berjalan normal, tetapi AI Auto-Tagging akan dilewati.",
                        fontSize = 11.sp,
                        color = Color(0xFF856404),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                uiState.geminiApiKeys.forEach { item ->
                    KeyItemRow(
                        item = item,
                        onToggle = { active -> viewModel.toggleGeminiKeyActive(item.id, active) },
                        onDelete = { viewModel.removeGeminiKey(item.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun KeyItemRow(
    item: GeminiApiKeyItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val maskedKey = if (item.apiKey.length > 8) {
        "${item.apiKey.take(6)}••••${item.apiKey.takeLast(4)}"
    } else {
        "••••••••"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenBg),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TitleDark)
                    Spacer(Modifier.width(8.dp))
                    when {
                        item.isQuotaExceeded -> {
                            Surface(color = Color(0xFFFFF3CD), shape = RoundedCornerShape(4.dp)) {
                                Text("Limit 429", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF856404), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        item.isActive -> {
                            Surface(color = Color(0xFFD4EDDA), shape = RoundedCornerShape(4.dp)) {
                                Text("Aktif", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF155724), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        else -> {
                            Surface(color = Color(0xFFE2E3E5), shape = RoundedCornerShape(4.dp)) {
                                Text("Nonaktif", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF383D41), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                Text(maskedKey, fontSize = 11.sp, color = LabelGray)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = item.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple)
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}