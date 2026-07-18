package com.zaidun.photozaidun.presentation.screen.watermarksettings

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import androidx.compose.ui.unit.dp
import com.zaidun.photozaidun.utils.BitmapUtils
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.zaidun.photozaidun.data.processor.watermark.WatermarkDrawer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkSettingsScreen(
    onBack: () -> Unit,
    viewModel: WatermarkSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 1. STATE LOKAL (Agar Slider Mulus)
    var selectedUri by remember(uiState.watermarkUri) { mutableStateOf(uiState.watermarkUri) }
    var opacity by remember(uiState.opacity) { mutableFloatStateOf(uiState.opacity) }
    var scale by remember(uiState.scale) { mutableFloatStateOf(uiState.scale) }
    var position by remember(uiState.position) { mutableStateOf(uiState.position) }
    var previewUri by remember(uiState.previewUri) {
        mutableStateOf(uiState.previewUri)
    }
    var renderedPreview by remember {
        mutableStateOf<Bitmap?>(null)
    }

    // Picker Logo
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedUri = it.toString()
        }
    }
    val previewPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            uri?.let {

                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                previewUri = it.toString()
                Log.d("URI", previewUri)
            }

        }
    LaunchedEffect(previewUri, selectedUri, opacity, scale, position) {
        if (previewUri.isBlank() || selectedUri.isBlank()) {
            renderedPreview = null
            return@LaunchedEffect
        }

        try {
            // Gunakan try-catch agar tidak Force Close jika izin ditolak
            val preview = BitmapUtils.loadBitmap(context, Uri.parse(previewUri))
            val logo = BitmapUtils.loadBitmap(context, Uri.parse(selectedUri))

            val drawer = WatermarkDrawer()
            val resizedPreview = drawer.createPreview(preview)

            renderedPreview = drawer.draw(
                resizedPreview,
                logo,
                opacity,
                scale,
                position
            )
        } catch (e: SecurityException) {
            Log.e("Watermark", "Izin akses file ditolak: ${e.message}")
            // Tampilkan pesan ke user daripada crash
            Toast.makeText(
                context,
                "Izin akses foto hilang. Silakan pilih ulang foto.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e("Watermark", "Gagal merender preview: ${e.message}")
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watermark Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // AREA PENGATURAN (Scrollable)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                Button(
                    onClick = { picker.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) { Text("Pilih Logo Watermark") }

                Spacer(Modifier.height(24.dp))

                Text("Opacity ${(opacity * 100).toInt()}%")
                Slider(value = opacity, onValueChange = { opacity = it })

                Spacer(Modifier.height(16.dp))

                Text("Ukuran ${(scale * 100).toInt()}%")
                Slider(value = scale, valueRange = 0.05f..1.0f, onValueChange = { scale = it })

                Spacer(Modifier.height(24.dp))

                // 2. LOGIC LIVE PREVIEW
                Text(
                    "Pratinjau Langsung",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {

                    if (renderedPreview != null) {

                        Image(
                            bitmap = renderedPreview!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                    } else {

                        Text("Pilih logo dan foto preview")

                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        previewPicker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Pilih Foto Preview")
                }
                Spacer(Modifier.height(24.dp))

                // 3. PILIHAN POSISI
                Text("Posisi", style = MaterialTheme.typography.titleMedium)
                listOf(
                    "TOP_LEFT",
                    "TOP_RIGHT",
                    "CENTER",
                    "BOTTOM_LEFT",
                    "BOTTOM_RIGHT"
                ).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { position = item }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = position == item, onClick = { position = item })
                        Text(item)
                    }
                }
            }

            // TOMBOL SIMPAN (Sticky Bottom)
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onBack) { Text("Batal") }
                    Button(
                        onClick = {
                            viewModel.save(
                                logoUri = selectedUri,
                                previewUri = previewUri,
                                opacity = opacity,
                                scale = scale,
                                position = position
                            )
                            Toast.makeText(
                                context,
                                "Watermark berhasil disimpan",
                                Toast.LENGTH_SHORT
                            ).show()
                            onBack()
                        },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("SIMPAN")

                    }
                }
            }
        }
    }
}