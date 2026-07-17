package com.zaidun.photozaidun.presentation.screen.watermarksettings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.widget.Toast
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage

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

    // Picker Logo
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
                Slider(value = scale, valueRange = 0.05f..0.5f, onValueChange = { scale = it })

                Spacer(Modifier.height(24.dp))

                // 2. LOGIC LIVE PREVIEW
                Text("Pratinjau Langsung", style = MaterialTheme.typography.titleMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewUri.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            // Foto Sample (Background)
                            AsyncImage(
                                model = if (previewUri.isNotBlank())
                                    Uri.parse(previewUri)
                                else
                                    null,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )


                            // Watermark (Overlay)
                            val align = when (position) {
                                "TOP_LEFT" -> Alignment.TopStart
                                "TOP_RIGHT" -> Alignment.TopEnd
                                "CENTER" -> Alignment.Center
                                "BOTTOM_LEFT" -> Alignment.BottomStart
                                "BOTTOM_RIGHT" -> Alignment.BottomEnd
                                else -> Alignment.BottomEnd
                            }
                            Box(modifier = Modifier.matchParentSize().padding(8.dp), contentAlignment = align) {
                                if (selectedUri.isNotBlank()) {
                                    AsyncImage(model = selectedUri, contentDescription = null, modifier = Modifier.fillMaxWidth(scale).alpha(opacity), contentScale = ContentScale.Fit)
                                } else {
                                    Text("PHOTO ZAIDUN", color = Color.Red.copy(alpha = opacity), fontSize = (scale * 100).sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Text("Belum ada folder dipilih", style = MaterialTheme.typography.bodySmall)
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
                listOf("TOP_LEFT", "TOP_RIGHT", "CENTER", "BOTTOM_LEFT", "BOTTOM_RIGHT").forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { position = item }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = position == item, onClick = { position = item })
                        Text(item)
                    }
                }
            }

            // TOMBOL SIMPAN (Sticky Bottom)
            Surface(tonalElevation = 3.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onBack) { Text("Batal") }
                    Button(
                        onClick = {
                            viewModel.save(
                            logoUri = selectedUri,
                            previewUri = previewUri?.toString() ?: "",
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