package com.example.appescapetocinema.ui.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Patterns // Para validación de email
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase

class RegistroViewModel(
    private val repositorioAuth: RepositorioAutenticacionFirebase // Inyectar repositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    // --- Funciones de actualización desde la UI ---

    fun onNombreChange(value: String) {
        _uiState.update { it.copy(nombre = value, errorMessage = null) }
    }

    fun onApellidoChange(value: String) {
        _uiState.update { it.copy(apellido = value, errorMessage = null) }
    }

    fun onCorreoChange(value: String) {
        _uiState.update { it.copy(correo = value.trim(), errorMessage = null) }
    }

    fun onContrasenaChange(value: String) {
        val confirm = _uiState.value.contrasenaConfirm
        _uiState.update {
            it.copy(
                contrasena = value,
                passwordsMatch = value == confirm || confirm.isEmpty(), // Verifica si coinciden
                errorMessage = null
            )
        }
    }

    fun onContrasenaConfirmChange(value: String) {
        val currentPassword = _uiState.value.contrasena
        _uiState.update {
            it.copy(
                contrasenaConfirm = value,
                passwordsMatch = currentPassword == value, // Verifica si coinciden
                errorMessage = null
            )
        }
    }

    // --- Lógica de Registro ---

    fun attemptRegistration() {
        val state = _uiState.value // Estado actual

        // Validaciones
        if (state.nombre.isBlank() || state.apellido.isBlank() || state.correo.isBlank() || state.contrasena.isBlank() || state.contrasenaConfirm.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Todos los campos son obligatorios") }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(state.correo).matches()) {
            _uiState.update { it.copy(errorMessage = "Formato de correo inválido") }
            return
        }
        if (state.contrasena.length < 6) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres") }
            return
        }
        if (!state.passwordsMatch) {
            _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden") }
            return
        }

        // Iniciar carga
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            repositorioAuth.registrar(state.correo, state.contrasena) { exito, error ->
                if (exito) {
                    _uiState.update { it.copy(isLoading = false, navigateToHome = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error ?: "Error desconocido durante el registro"
                        )
                    }
                }
            }
        }
    }

    // --- Reseteo de Navegación ---
    fun onNavigationDone() {
        _uiState.update { it.copy(navigateToHome = false) }
    }
}