package com.zaidun.photozaidun.data.processor.export
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zaidun.photozaidun.presentation.screen.home.HomeViewModel
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import timber.log.Timber

@Composable
fun ExportLoadingScreen(
    viewModel: HomeViewModel,
    navController: NavController
) {

    val uiState by viewModel.uiState.collectAsState()

    BackHandler(true) {
        // Disable back
    }
    var started by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(uiState.isProcessing) {

        Timber.tag("EXPORT_SCREEN").d("ExportLoadingScreen terbuka")

        if (uiState.isProcessing) {

            started = true

        }
        if (started && !uiState.isProcessing) {
            navController.popBackStack()
        }

    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Icon(
                Icons.Default.Photo,
                null,
                modifier = Modifier.size(72.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Mengekspor Foto",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { uiState.progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text("${uiState.progress}%")

            Spacer(Modifier.height(8.dp))

            Text("${uiState.current} / ${uiState.total}")

            Spacer(Modifier.height(16.dp))

            Text(
                uiState.currentFilename,
                maxLines = 1
            )

            Spacer(Modifier.height(40.dp))

            Button(

                onClick = {

                    viewModel.cancelExport()

                }

            ) {

                Text("Cancel")

            }

        }

    }

}