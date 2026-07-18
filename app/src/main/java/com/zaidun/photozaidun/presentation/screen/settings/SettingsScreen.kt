package com.zaidun.photozaidun.presentation.screen.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zaidun.photozaidun.domain.model.PhotoItem
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaidun.photozaidun.presentation.shared.SharedFolderViewModel
import kotlin.context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    sharedFolderViewModel: SharedFolderViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val photos by sharedFolderViewModel.photos.collectAsState()
    val sharedState by sharedFolderViewModel.state.collectAsState()
    val context = LocalContext.current

    val firstPhoto = photos.firstOrNull()
    val totalPhoto = sharedState.totalImages

    var mainFolder = uiState.mainFolder
    var partFolder = uiState.partFolder
    var fileSuffix = uiState.fileSuffix


    var maxPhotoPerFolder  = uiState.maxPhotoPerFolder
    var resizePercent  = uiState.resizePercent
    var autoUpload  = uiState.autoUpload
    var localFolder by remember { mutableStateOf("/Pictures/Photo Zaidun") }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Pengaturan Export")
                },

                navigationIcon = {

                    IconButton(onClick = onBack) {

                        Icon(Icons.Default.ArrowBack, null)

                    }

                }

            )

        }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {


                ElevatedCard {

                    Column(
                        Modifier.padding(16.dp)
                    ) {Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            "Google Drive",
                            style = MaterialTheme.typography.titleMedium
                        )
                        // Indikator Status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isConnected = uiState.googleUser != null
                            Icon(
                                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isConnected) "Drive Terhubung" else "Drive Tidak Terhubung",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        }




                    Spacer(Modifier.height(12.dp))
                        if (uiState.googleUser == null) {

                            Button(
                                onClick = {
                                    viewModel.loginGoogle(context)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Login dengan Google")
                            }

                        }else {
                            // Tampilan Akun yang Terhubung
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(uiState.googleUser?.name ?: "User", style = MaterialTheme.typography.bodyMedium)
                                        Text(uiState.googleUser?.email ?: "", style = MaterialTheme.typography.labelSmall)
                                    }
                                    TextButton(onClick = { viewModel.logoutGoogle(activity = context as Activity) }) {
                                        Text("Logout", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mainFolder,
                            onValueChange = {
                                viewModel.saveMainFolder(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Nama Folder Utama")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CreateNewFolder, null)
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = partFolder,
                            onValueChange = {
                                viewModel.savePartFolder(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Nama Folder Part")
                            },
                            supportingText = {
                                Text("Otomatis menjadi part1, part2, part3...")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, null)
                            }
                        )

                    }

                }
            }
            item {


                ElevatedCard {

                    Column(
                        Modifier.padding(16.dp)
                    ) {

                        Text(
                            "Export",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = fileSuffix,
                            onValueChange = {
                                viewModel.saveFileSuffix(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Tambahan Nama File")
                            },
                            supportingText = {
                                Text("Contoh : IMG_8244_${fileSuffix}.jpg")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Image, null)
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = resizePercent,
                            onValueChange = {

                                val value = it.filter(Char::isDigit)

                                if (value.length <= 3) {
                                    viewModel.saveResizePercent(value)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            label = {
                                Text("Resolusi Export (%)")
                            },
                            supportingText = {
                                Text("Masukkan nilai 1 - 100")
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = maxPhotoPerFolder,
                            onValueChange = {
                                val value = it.filter(Char::isDigit)

                                if (value.length <= 5) {
                                    viewModel.saveMaxPhoto(value)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            label = {
                                Text("Maksimum Foto per Folder")
                            },
                            supportingText = {
                                Text("Folder otomatis pindah ke part berikutnya")
                            }
                        )

                    }

                }
            }
            item {


                ElevatedCard {

                    Column(
                        Modifier.padding(16.dp)
                    ) {

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Column {

                                Text("Upload Otomatis")

                                Text(
                                    "Upload ke Google Drive setelah export selesai",
                                    style = MaterialTheme.typography.bodySmall
                                )

                            }

                            Switch(
                                checked = autoUpload,
                                onCheckedChange = {
                                    viewModel.saveAutoUpload(it)
                                }
                            )

                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = localFolder,
                            onValueChange = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Folder Penyimpanan Lokal")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PhotoLibrary, null)
                            },
                            trailingIcon = {

                                IconButton(
                                    onClick = {

                                        // TODO pilih folder

                                    }
                                ) {

                                    Icon(Icons.Default.Folder, null)

                                }

                            }

                        )

                    }

                }
            }
            item {


                ElevatedCard {

                    Column(
                        Modifier.padding(16.dp)
                    ) {

                        Text(
                            "Preview",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            """
📂 $mainFolder
 └── ${partFolder}1
      ├── IMG_8244_${fileSuffix}.jpg
      ├── IMG_8245_${fileSuffix}.jpg
      ├── IMG_8246_${fileSuffix}.jpg
      └── dst...

📦 Maksimum ${maxPhotoPerFolder} foto/folder

Setelah penuh:

📂 ${partFolder}2
      ├── IMG_8445_${fileSuffix}.jpg
      ├── IMG_8446_${fileSuffix}.jpg
      └── dst...
""".trimIndent()
                        )


                    }

                }
            }

        }

    }

}