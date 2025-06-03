package com.example.appescapetocinema.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- Data Class para el Estado de la UI ---
data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val navigateToHome: Boolean = false // Flag para indicar navegación
)

// --- El ViewModel ---
class LoginViewModel(
    private val repositorioAuth: RepositorioAutenticacionFirebase // Inyectamos el repositorio
) : ViewModel() {

    // --- StateFlow privado y público para la UI ---
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // --- Funciones para actualizar el estado desde la UI ---

    fun onCorreoChange(nuevoCorreo: String) {
        _uiState.update { it.copy(correo = nuevoCorreo.trim(), errorMessage = null, infoMessage = null) }
    }

    fun onContrasenaChange(nuevaContrasena: String) {
        _uiState.update { it.copy(contrasena = nuevaContrasena, errorMessage = null, infoMessage = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    // --- Funciones para acciones de negocio ---

    fun loginWithEmailPassword() {
        val currentState = _uiState.value // Obtenemos el estado actual
        if (currentState.correo.isBlank() || currentState.contrasena.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Correo y contraseña son requeridos") }
            return
        }

        // Indicamos que estamos cargando
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        // Usamos viewModelScope para lanzar la corrutina (aunque el repo use callbacks)
        viewModelScope.launch {
            repositorioAuth.iniciarSesion(currentState.correo, currentState.contrasena) { exito, error ->
                if (exito) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigateToHome = true // ¡Éxito! Indicamos que hay que navegar
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error ?: "Error desconocido al iniciar sesión"
                        )
                    }
                }
            }
        }
    }

    fun loginWithGoogleToken(idToken: String?) {
        if (idToken == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "No se pudo obtener el token de Google.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            repositorioAuth.iniciarSesionConGoogle(idToken) { exito, error ->
                if (exito) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigateToHome = true // ¡Éxito! Indicamos que hay que navegar
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error ?: "Error al iniciar sesión con Google"
                        )
                    }
                }
            }
        }
    }

    fun sendPasswordResetEmail() {
        val currentState = _uiState.value
        if (currentState.correo.isBlank() || !currentState.correo.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Ingresa un correo válido para recuperar contraseña") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            repositorioAuth.enviarCorreoRecuperacion(currentState.correo) { exito, error ->
                if (exito) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Correo de recuperación enviado a ${currentState.correo}"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error ?: "Error al enviar correo de recuperación"
                        )
                    }
                }
            }
        }
    }

    // --- Función para resetear el estado de navegación ---
    // La UI llamará a esto DESPUÉS de haber navegado
    fun onNavigationDone() {
        _uiState.update { it.copy(navigateToHome = false) }
    }
}