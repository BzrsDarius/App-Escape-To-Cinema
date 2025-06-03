package com.example.appescapetocinema.ui.registro

// Data Class para el estado de la UI de Registro
data class RegistroUiState(
    val nombre: String = "",
    val apellido: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val contrasenaConfirm: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val passwordsMatch: Boolean = true, // Para controlar el mensaje de error de contraseñas
    val navigateToHome: Boolean = false // Flag para indicar navegación post-registro
)