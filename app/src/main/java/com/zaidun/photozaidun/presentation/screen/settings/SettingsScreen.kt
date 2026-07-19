package com.zaidun.photozaidun.presentation.screen.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextAlign

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
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Export", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Google Drive Login
            item {
                Text("Koneksi Cloud", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

                                    /* Silently ignore for now to fix compile */ },
                                onError = {

                                    it.printStackTrace()

                                }
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
                    OutlinedCard(shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(uiState.googleUser?.name ?: "User", fontWeight = FontWeight.Bold)
                                Text(uiState.googleUser?.email ?: "", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { viewModel.logoutGoogle(context as Activity) }) {
                                Text("Logout", color = Color.Red)
                            }
                        }
                    }
                }
                AssistChip(
                    onClick = { },
                    enabled = false,
                    label = {
                        Text(
                            if (uiState.driveConnected)
                                "Terhubung dengan Drive"
                            else
                                "Belum terhubung dengan Drive"
                        )
                    }
                )
            }


            // 2. 3 Level Folder + Nama Tambahan
            item {
                Text("Struktur Folder & Penamaan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))

                // Level 1: Induk Folder
                OutlinedTextField(
                    value = uiState.rootFolder,
                    onValueChange = { viewModel.saveRootFolder(it) },
                    label = { Text("Induk Folder (Level 1)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Folder, null) }
                )

                Spacer(Modifier.height(16.dp))

                // Level 2: Project Folder (Sesuai Screenshot User)
                OutlinedTextField(
                    value = uiState.mainFolder,
                    onValueChange = { viewModel.saveMainFolder(it) },
                    label = { Text("Folder Project (Level 2)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) }
                )

                Spacer(Modifier.height(16.dp))

                // Level 3: Prefix Part
                OutlinedTextField(
                    value = uiState.partFolder,
                    onValueChange = { viewModel.savePartFolder(it) },
                    label = { Text("Prefix Nama Part (Level 3)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Contoh: Part") },
                    supportingText = { Text("Akan menjadi ${uiState.partFolder}1, dst.") }
                )

                Spacer(Modifier.height(16.dp))

                // Penamaan File: Nama Tambahan
                OutlinedTextField(
                    value = uiState.fileSuffix,
                    onValueChange = { viewModel.saveFileSuffix(it) },
                    label = { Text("Nama Tambahan File") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                    supportingText = {
                        val previewName =
                            if (uiState.fileSuffix.isBlank()) {
                                "IMG_01.jpg"
                            } else {
                                "IMG_01_${uiState.fileSuffix}.jpg"
                            }

                        Text("Contoh: $previewName")
                    }
                )
            }

            // 3. Limit & Kualitas
            item {
                Text("Limit & Kualitas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.maxPhotoPerFolder,
                    onValueChange = {
                        val value = it.filter(Char::isDigit)
                        if (value.length <= 5) viewModel.saveMaxPhoto(value)
                    },
                    label = { Text("Jumlah Foto per Folder") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Numbers, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(24.dp))

                Text("Resolusi Export: ${uiState.resizePercent}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.resizePercent.toFloatOrNull() ?: 100f,
                    onValueChange = { viewModel.saveResizePercent(it.toInt().toString()) },
                    valueRange = 10f..100f,
                    steps = 9
                )
            }

            // Preview Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Preview Lokasi Cloud:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val previewFile =
                            if (uiState.fileSuffix.isBlank()) {
                                "IMG_01.jpg"
                            } else {
                                "IMG_01_${uiState.fileSuffix}.jpg"
                            }

                        Text(
                            "My Drive / ${uiState.rootFolder} / ${uiState.mainFolder} / ${uiState.partFolder}1 / $previewFile",
                                    style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )


                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
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
