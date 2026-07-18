package com.zaidun.photozaidun.presentation.screen.folderpicker

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zaidun.photozaidun.domain.model.PhotoItem
import com.zaidun.photozaidun.presentation.screen.folderpicker.component.EmptyState
import com.zaidun.photozaidun.presentation.screen.folderpicker.component.ImageGrid
import com.zaidun.photozaidun.presentation.screen.folderpicker.component.ProgressCard
import com.zaidun.photozaidun.presentation.shared.SharedFolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerScreen(
    viewModel: FolderPickerViewModel,
    sharedFolderViewModel: SharedFolderViewModel,
    onFolderSelected: (Uri, Int) -> Unit,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.scanning) {

        if (
            !uiState.scanning &&
            uiState.selectedFolderUri != null &&
            uiState.photos.isNotEmpty()
        ) {
            sharedFolderViewModel.updateFolder(
                uri = uiState.selectedFolderUri!!,
                folderName = uiState.folderName,
                totalImages = uiState.photos.size,
                photos = uiState.photos
            )

            onFolderSelected(
                uiState.selectedFolderUri!!,
                uiState.photos.size
            )

            onBack()

        }

    }

    val launcher = rememberLauncherForActivityResult(
        contract = OpenDocumentTree()
    ) { uri: Uri? ->

        uri?.let {

            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            viewModel.onEvent(
                FolderPickerEvent.FolderSelected(it)
            )
        }
    }

    LaunchedEffect(
        uiState.scanning,
        uiState.photos.size
    ) {

        if (
            !uiState.scanning &&
            uiState.selectedFolderUri != null &&
            uiState.photos.isNotEmpty()
        ) {
                sharedFolderViewModel.updateFolder(
                    uri = uiState.selectedFolderUri!!,
                    folderName = uiState.folderName,
                    totalImages = uiState.photos.size,
                    photos = uiState.photos
                )


            onFolderSelected(
                uiState.selectedFolderUri!!,
                uiState.photos.size

            )

            onBack()
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text("Photo Zaidun")

                }

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)

        ) {

            Button(

                onClick = {

                    launcher.launch(null)

                },

                modifier = Modifier.fillMaxWidth()

            ) {

                Text("Pilih Folder")

            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            ProgressCard(

                scanned = uiState.scanned,

                batch = uiState.currentBatch,

                scanning = uiState.scanning

            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            if (uiState.photos.isEmpty()) {

                EmptyState()

            } else {

                ImageGrid(

                    photos = uiState.photos

                )

            }

        }

    }

}