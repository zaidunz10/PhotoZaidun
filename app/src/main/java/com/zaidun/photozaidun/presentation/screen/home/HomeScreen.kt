package com.zaidun.photozaidun.presentation.screen.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.Identity
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFolderPicker: () -> Unit,
    onNavigateToWatermark: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val activity = context as Activity

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

            try {

                val authorizationResult =
                    Identity.getAuthorizationClient(activity)
                        .getAuthorizationResultFromIntent(result.data)

                val accessToken = authorizationResult.accessToken

                Timber.d("Access Token = $accessToken")

                viewModel.onAccessTokenReceived(accessToken)

                viewModel.startBatchProcess()

            } catch (e: Exception) {

                Timber.e(e)

            }

        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Photo Zaidun", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Summary Card
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Ringkasan Batch", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${uiState.totalImages}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(text = "Total Foto", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${uiState.processedImages}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(text = "Selesai", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item { ActionCard("Pilih Folder", Icons.Default.Folder, onNavigateToFolderPicker) }
            item { ActionCard("Watermark", Icons.Default.Brush, onNavigateToWatermark) }
            item { ActionCard("Riwayat", Icons.Default.History, onNavigateToHistory) }
            item { ActionCard("Pengaturan", Icons.Default.Settings, onNavigateToSettings) }

            // Start Batch Button
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {

                        viewModel.requestDrivePermission(
                            activity = activity,

                            onNeedUserConsent = { sender ->

                                launcher.launch(
                                    IntentSenderRequest.Builder(sender).build()
                                )

                            },

                            onError = {

                            }

                        )

                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),

                    enabled = uiState.totalImages > 0
                ) {

                    Icon(Icons.Default.PlayArrow, null)

                    Spacer(Modifier.width(8.dp))

                    Text("MULAI Export")

                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge)
        }
    }
}