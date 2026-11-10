package com.zaidun.photozaidun.presentation.screen.settings

import android.app.Activity
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.auth.GoogleAuthManager
import com.zaidun.photozaidun.data.auth.GoogleUser
import com.zaidun.photozaidun.data.remote.UserLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore,
    private val googleAuthManager: GoogleAuthManager

) : ViewModel() {
    private fun saveFolderLevels() {

        viewModelScope.launch {

            preferences.saveFolderLevels(
                _uiState.value.folderLevels
            )

        }

    }
    fun removeFolderLevel(id: Long) {

        _uiState.update { state ->

            state.copy(
                folderLevels =
                    state.folderLevels.filterNot {
                        it.id == id
                    }
            )

        }

        saveFolderLevels()
    }
    fun updateFolderLevel(
        id: Long,
        name: String
    ) {
        _uiState.update { state ->
            state.copy(
                folderLevels =
                    state.folderLevels.map {

                        if (it.id == id)
                            it.copy(name = name)

                        else
                            it
                    }
            )
        }

        saveFolderLevels()
    }
    fun addFolderLevel() {
        _uiState.update {
            it.copy(
                folderLevels = it.folderLevels + FolderLevel()
            )
        }

        saveFolderLevels()
    }

    fun saveAutoUpload(value: Boolean) {
        _uiState.update {
            it.copy(autoUpload = value)
        }

        viewModelScope.launch {
            preferences.saveAutoUpload(value)
        }
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // PERBAIKAN: Menggunakan Array<Any?> karena flow yang digabung > 5
        viewModelScope.launch {
            combine(
                preferences.userId,
                preferences.userName,
                preferences.userEmail,
                preferences.userPhotoUrl,
                preferences.photographerWa,
                preferences.photographerIg,
                preferences.rootFolder,
                preferences.folderLevels,
                preferences.partFolder,
                preferences.fileSuffix,
                preferences.resizePercent,
                preferences.maxPhotoPerFolder,
                preferences.autoUpload,
                preferences.driveAccessToken,
                preferences.startPartNumber,
                preferences.bankName,
                preferences.accountNumber,
                preferences.accountHolderName,
                preferences.defaultPhotoPrice,
                preferences.minQtyDiscount,
                preferences.priceDiscount
            ) { args: Array<Any?> ->
                val id = args[0] as String
                val name = args[1] as String
                val email = args[2] as String
                val photoUrl = args[3] as String
                val wa = args[4] as String
                val ig = args[5] as String
                val root = args[6] as String
                val folderLevels = args[7] as List<*>
                val part = args[8] as String
                val suffix = args[9] as String
                val resize = args[10] as Int
                val max = args[11] as Int
                val auto = args[12] as Boolean
                val token = args[13] as String
                val startPart = args[14] as Int
                val bank = args[15] as String
                val accNum = args[16] as String
                val holder = args[17] as String
                val price = args[18] as Int
                val minQty = args[19] as Int
                val discountPrice = args[20] as Int?

                val user = if (id.isNotEmpty()) GoogleUser(id, name, email, photoUrl.ifBlank { null }) else null

                if (user != null && user.email.isNotBlank()) {
                    UserLogger.logLogin(user.email, user.name)
                }

                _uiState.update {
                    it.copy(
                        googleUser = user,
                        photographerWa = wa,
                        photographerIg = ig,
                        bankName = bank,
                        accountNumber = accNum,
                        accountHolderName = holder,
                        defaultPhotoPrice = price,
                        minQtyDiscount = minQty,
                        priceDiscount = discountPrice,
                        driveConnected = token.isNotBlank(),
                        rootFolder = root,
                        folderLevels = folderLevels.filterIsInstance<FolderLevel>(),
                        partFolder = part,
                        fileSuffix = suffix,
                        resizePercent = resize.toString(),
                        maxPhotoPerFolder = max.toString(),
                        autoUpload = auto,
                        startPartNumber = startPart
                    )
                }
            }.collect()
        }
    }

    fun loginGoogle(
        activity: Activity,
        onNeedConsent: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // 1. Login Identitas
                val user = googleAuthManager.signIn(activity)
                if (user != null) {
                    preferences.saveUser(user)

                    // Catat log login ke Firebase Firestore (Email, Nama, Jam Login)
                    UserLogger.logLogin(user.email, user.name)

                    // 2. LANGSUNG MINTA IZIN DRIVE (Scope: DRIVE_FILE)
                    googleAuthManager.requestDriveAccess(
                        activity = activity,
                        onTokenReady = { token ->
                            viewModelScope.launch { preferences.saveDriveAccessToken(token) }

                            Timber.tag("Drive").d("TOKEN DITERIMA = $token")

                        },
                        onNeedConsent = onNeedConsent,
                        onError = onError
                    )
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun savePhotographerProfile(wa: String, ig: String) {
        viewModelScope.launch {
            // Kita simpan di DataStore juga jika diperlukan nantinya untuk cek kelengkapan
            preferences.savePhotographerWa(wa)
            preferences.savePhotographerIg(ig)
        }
    }

    fun logoutGoogle(activity: Activity) {
        viewModelScope.launch {
            val currentEmail = _uiState.value.googleUser?.email ?: ""
            if (currentEmail.isNotBlank()) {
                // Catat log logout ke Firebase Firestore (Jam Logout)
                UserLogger.logLogout(currentEmail)
            }

            googleAuthManager.signOut(activity)
            preferences.clearUser()
            preferences.saveDriveAccessToken("")
            _uiState.update { it.copy(googleUser = null) }
        }
    }


    // Fungsi Save Responsif (Update UI Langsung + Simpan DB)
    fun saveRootFolder(v: String) { _uiState.update { it.copy(rootFolder = v) }; viewModelScope.launch { preferences.saveRootFolder(v) } }
    fun savePartFolder(v: String) { _uiState.update { it.copy(partFolder = v) }; viewModelScope.launch { preferences.savePartFolder(v) } }
    fun saveFileSuffix(v: String) { _uiState.update { it.copy(fileSuffix = v) }; viewModelScope.launch { preferences.saveFileSuffix(v) } }
    fun saveStartPart(v: Int) {
        _uiState.update { it.copy(startPartNumber = v) }
        viewModelScope.launch { preferences.saveStartPartNumber(v) }
    }
    fun saveResizePercent(v: String) {
        _uiState.update { it.copy(resizePercent = v) }
        viewModelScope.launch { v.toIntOrNull()?.let { preferences.saveResizePercent(it) } }
    }

    fun saveMaxPhoto(v: String) {
        _uiState.update { it.copy(maxPhotoPerFolder = v) }
        viewModelScope.launch { v.toIntOrNull()?.let { preferences.saveMaxPhotoPerFolder(it) } }
    }

    fun savePhotographerProfileAndBank(
        wa: String,
        ig: String,
        bank: String,
        accountNum: String,
        holderName: String,
        priceSingle: Int,
        minQtyDiscount: Int,
        priceDiscount: Int?
    ) {
        viewModelScope.launch {
            preferences.savePhotographerProfile(wa, ig)
            preferences.saveBankAndPricingInfo(bank, accountNum, holderName, priceSingle, minQtyDiscount, priceDiscount)
        }
    }
}
