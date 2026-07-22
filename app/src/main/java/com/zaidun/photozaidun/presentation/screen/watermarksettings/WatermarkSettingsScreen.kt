package com.zaidun.photozaidun.presentation.screen.watermarksettings

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaidun.photozaidun.data.processor.watermark.WatermarkDrawer
import com.zaidun.photozaidun.utils.BitmapUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkSettingsScreen(
    onBack: () -> Unit,
    viewModel: WatermarkSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 1. CACHE BITMAP (Kunci Performa Ringan)
    var cachedPreview by remember { mutableStateOf<Bitmap?>(null) }
    var cachedLogo by remember { mutableStateOf<Bitmap?>(null) }
    var renderedPreview by remember { mutableStateOf<Bitmap?>(null) }

    // State Lokal untuk Slider agar instan (tidak menunggu DataStore)
    var logoOpacity by remember(uiState.opacity) { mutableFloatStateOf(uiState.opacity) }
    var logoScale by remember(uiState.scale) { mutableFloatStateOf(uiState.scale) }
    var logoX by remember(uiState.logoOffsetX) { mutableFloatStateOf(uiState.logoOffsetX) }
    var logoY by remember(uiState.logoOffsetY) { mutableFloatStateOf(uiState.logoOffsetY) }

    var infoX by remember(uiState.infoOffsetX) { mutableFloatStateOf(uiState.infoOffsetX) }
    var infoY by remember(uiState.infoOffsetY) { mutableFloatStateOf(uiState.infoOffsetY) }
    var infoSize by remember(uiState.infoFontSize) { mutableFloatStateOf(uiState.infoFontSize) }
    var showFile by remember(uiState.showFilename) { mutableStateOf(uiState.showFilename) }
    var showPart by remember(uiState.showPart) { mutableStateOf(uiState.showPart) }
    var showDate by remember(uiState.showDate) {
        mutableStateOf(uiState.showDate)
    }

    var showTime by remember(uiState.showTime) {
        mutableStateOf(uiState.showTime)
    }
    // Load Preview Bitmap hanya jika URI berubah
    LaunchedEffect(uiState.previewUri) {
        if (uiState.previewUri.isNotBlank()) {
            val original = BitmapUtils.loadBitmap(context, Uri.parse(uiState.previewUri))
            // Resize ke 900px agar render preview enteng tapi tetap tajam
            cachedPreview = WatermarkDrawer().createPreview(original, 900)
        }
    }

    // Load Logo Bitmap hanya jika URI berubah
    LaunchedEffect(uiState.watermarkUri) {
        if (uiState.watermarkUri.isNotBlank()) {
            cachedLogo = BitmapUtils.loadBitmap(context, Uri.parse(uiState.watermarkUri))
        }
    }

    // RENDER PREVIEW REALTIME (Sangat cepat karena hanya draw, tidak decode)
    LaunchedEffect(cachedPreview, cachedLogo, logoOpacity, logoScale, logoX, logoY, infoX, infoY, infoSize, showFile, showPart) {
        cachedPreview?.let { preview ->
            renderedPreview = WatermarkDrawer().draw(
                bitmap = preview,
                watermark = cachedLogo,
                alpha = logoOpacity,
                scale = logoScale,
                logoOffsetX = logoX,
                logoOffsetY = logoY,
                infoOffsetX = infoX,
                infoOffsetY = infoY,
                infoSize = infoSize,
                showFilename = showFile,
                showPart = showPart,
                fileName = "IMG_0001.jpg",
                partName = "Part 1",
                showDate = showDate,
                showTime = showTime
            )
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.saveLogoUri(it.toString())
        }
    }

    val previewPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.savePreviewUri(it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watermark Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    Button(
                        onClick = {
                            viewModel.updateLogoSettings(logoOpacity, logoScale, logoX, logoY)
                            viewModel.updateInfoSettings(showFile, showPart,    showDate,
                                showTime, infoX, infoY, infoSize)
                            Toast.makeText(context, "Pengaturan Disimpan", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Bagian Atas: Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (renderedPreview != null) {
                    Image(
                        bitmap = renderedPreview!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Pilih foto & logo untuk memulai", color = Color.Gray)
                }
            }

            // Bagian Bawah: Kontrol (Scrollable)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Pickers
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { previewPicker.launch(arrayOf("image/*")) }, Modifier.weight(1f)) {
                        Text("Ganti Foto")
                    }
                    Button(onClick = { picker.launch(arrayOf("image/*")) }, Modifier.weight(1f)) {
                        Text("Ganti Logo")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // LOGO SETTINGS
                Text("Logo Watermark", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ControlSlider("Opacity", logoOpacity, 0f, 1f) { logoOpacity = it }
                ControlSlider("Skala", logoScale, 0.05f, 0.8f) { logoScale = it }
                ControlSlider("Posisi X", logoX, 0f, 1f) { logoX = it }
                ControlSlider("Posisi Y", logoY, 0f, 1f) { logoY = it }

                Spacer(Modifier.height(24.dp))

                // FILE INFO SETTINGS
                Text("Info File (Nama & Part)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showFile, onCheckedChange = { showFile = it })
                    Text("Tampilkan Nama File")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showPart, onCheckedChange = { showPart = it })
                    Text("Tampilkan Part")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showDate,
                        onCheckedChange = { showDate = it }
                    )
                    Text("Tampilkan Tanggal")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showTime,
                        onCheckedChange = { showTime = it }
                    )
                    Text("Tampilkan Jam")
                }
                ControlSlider("Ukuran Teks", infoSize, 0.01f, 0.1f) { infoSize = it }
                ControlSlider("Posisi X", infoX, 0f, 1f) { infoX = it }
                ControlSlider("Posisi Y", infoY, 0f, 1f) { infoY = it }

                Spacer(Modifier.height(32.dp))
                Text("zaidunz_photo © 2024", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun ControlSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, valueRange = min..max, onValueChange = onValueChange)
    }
}