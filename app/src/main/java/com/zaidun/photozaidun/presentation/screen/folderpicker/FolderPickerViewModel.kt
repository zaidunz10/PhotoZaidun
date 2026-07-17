package com.zaidun.photozaidun.presentation.screen.folderpicker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.processor.BatchProcessor
import com.zaidun.photozaidun.domain.usecase.scanner.ScanFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderPickerViewModel @Inject constructor(
    private val scanFolderUseCase: ScanFolderUseCase,
    private val preferences: UserPreferencesDataStore,
    private val batchProcessor: BatchProcessor = BatchProcessor()
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(FolderPickerUiState())

    val uiState =
        _uiState.asStateFlow()

    fun onEvent(
        event: FolderPickerEvent
    ) {

        when (event) {

            FolderPickerEvent.PickFolder -> {

            }

            FolderPickerEvent.Refresh -> {

            }

            is FolderPickerEvent.FolderSelected -> {

                viewModelScope.launch {
                    preferences.saveLastFolder(event.uri.toString())

                    _uiState.update {

                        it.copy(

                            scanning = true,

                            selectedFolderUri = event.uri,

                            photos = emptyList(),

                            scanned = 0,

                            currentBatch = 0

                        )

                    }

                    batchProcessor
                        .process(

                            scanFolderUseCase(event.uri)

                        )

                        .collect { batch ->

                            _uiState.update { state ->

                                state.copy(

                                    photos = state.photos + batch.items.map { it.photo },

                                    scanned = state.scanned + batch.items.size,
                                    currentBatch =
                                        batch.batchNumber

                                )

                            }

                        }

                    _uiState.update {

                        it.copy(

                            scanning = false

                        )

                    }

                }

            }
        }
    }
}