package com.zaidun.carifoto.presentation.auth

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zaidun.carifoto.data.auth.CariFotoGoogleAuthManager
import com.zaidun.carifoto.data.auth.CariFotoGoogleUser
import com.zaidun.carifoto.data.local.CariFotoDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CariFotoAuthUiState(
    val googleUser: CariFotoGoogleUser? = null,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null
)

class CariFotoAuthViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(CariFotoAuthUiState())
    val uiState: StateFlow<CariFotoAuthUiState> = _uiState.asStateFlow()

    private val authManager = CariFotoGoogleAuthManager(application)
    private val dataStore = CariFotoDataStore(application)

    init {
        viewModelScope.launch {
            dataStore.riderUser.collect { savedUser ->
                _uiState.update { it.copy(googleUser = savedUser) }
            }
        }
    }

    fun checkSilentLogin(context: Context) {
        viewModelScope.launch {
            val user = authManager.getAuthorizedUserSilently(context)
            if (user != null) {
                dataStore.saveUser(user)
                _uiState.update { it.copy(googleUser = user) }
            }
        }
    }

    fun login(context: Context, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
        viewModelScope.launch {
            try {
                val user = authManager.signIn(context)
                if (user != null) {
                    dataStore.saveUser(user)
                    _uiState.update { it.copy(googleUser = user, isLoggingIn = false) }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoggingIn = false, loginError = "Batal login") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoggingIn = false, loginError = e.message) }
            }
        }
    }

    fun loginGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            dataStore.saveGuestUser()
            val guest = CariFotoGoogleUser("guest_rider_001", "Pengendara (Tamu)", "guest@carifoto.id", null)
            _uiState.update { it.copy(googleUser = guest) }
            onSuccess()
        }
    }

    fun logout(context: Context, onLogoutFinished: () -> Unit) {
        viewModelScope.launch {
            if (context is Activity) {
                authManager.signOut(context)
            }
            dataStore.clearUser()
            _uiState.update { it.copy(googleUser = null) }
            onLogoutFinished()
        }
    }
}
