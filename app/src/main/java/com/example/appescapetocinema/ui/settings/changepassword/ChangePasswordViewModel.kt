package com.example.appescapetocinema.ui.settings.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

class ChangePasswordViewModel(
    private val authRepository: RepositorioAutenticacionFirebase,
    private val firebaseAuth: FirebaseAuth // Para currentUser
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    init {
        checkProvider()
    }

    private fun checkProvider() {
        val user = firebaseAuth.currentUser
        val providerId = user?.providerData?.find { it.providerId != "firebase" }?.providerId
        _uiState.update { it.copy(isEmailUser = providerId == EmailAuthProvider.PROVIDER_ID || providerId == null) }
        // Si providerId es null después de filtrar "firebase", y el usuario existe, es probable que sea email/pass.
        // Si el usuario se registró con email/pass, user.providerData contendrá un solo elemento con providerId "password".
        // Si se linkeó con Google después, contendrá "password" y "google.com".
        val isEmailPassword =
            user?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true
        _uiState.update { it.copy(isEmailUser = isEmailPassword) }
        if (!isEmailPassword) {
            _uiState.update { it.copy(errorMessage = "Cambio de contraseña no disponible para inicios de sesión con proveedores externos (ej. Google).") }
        }
    }


    fun onCurrentPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                currentPasswordInput = password,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                newPasswordInput = password,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onConfirmNewPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                confirmNewPasswordInput = password,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun submitChangePassword() {
        val currentPassword = _uiState.value.currentPasswordInput
        val newPassword = _uiState.value.newPasswordInput
        val confirmNewPassword = _uiState.value.confirmNewPasswordInput

        if (currentPassword.isBlank() || newPassword.isBlank() || confirmNewPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Todos los campos son obligatorios.") }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "La nueva contraseña debe tener al menos 6 caracteres.") }
            return
        }
        if (newPassword != confirmNewPassword) {
            _uiState.update { it.copy(errorMessage = "Las nuevas contraseñas no coinciden.") }
            return
        }
        if (newPassword == currentPassword) {
            _uiState.update { it.copy(errorMessage = "La nueva contraseña no puede ser igual a la actual.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            // 1. Reautenticar
            authRepository.reauthenticateUser(currentPassword) { reauthSuccess, reauthError ->
                if (reauthSuccess) {
                    Log.d("ChangePasswordVM", "Reautenticación exitosa.")
                    // 2. Actualizar contraseña
                    authRepository.updatePassword(newPassword) { updateSuccess, updateError ->
                        if (updateSuccess) {
                            Log.i("ChangePasswordVM", "Contraseña actualizada exitosamente.")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "Contraseña actualizada correctamente.",
                                    currentPasswordInput = "",
                                    newPasswordInput = "",
                                    confirmNewPasswordInput = ""
                                )
                            }
                        } else {
                            Log.e(
                                "ChangePasswordVM",
                                "Error al actualizar contraseña: $updateError"
                            )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Error al actualizar: ${updateError ?: "Desconocido"}"
                                )
                            }
                        }
                    }
                } else {
                    Log.w("ChangePasswordVM", "Error de reautenticación: $reauthError")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = reauthError
                                ?: "Contraseña actual incorrecta o error de autenticación."
                        )
                    }
                }
            }
        }
    }

    // Factory para ChangePasswordViewModel
    class ChangePasswordViewModelFactory(
        private val authRepository: RepositorioAutenticacionFirebase,
        private val firebaseAuth: FirebaseAuth
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChangePasswordViewModel::class.java)) {
                return ChangePasswordViewModel(authRepository, firebaseAuth) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
