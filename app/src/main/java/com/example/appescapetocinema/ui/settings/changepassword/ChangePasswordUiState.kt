package com.example.appescapetocinema.ui.settings.changepassword

data class ChangePasswordUiState(
    val currentPasswordInput: String = "",
    val newPasswordInput: String = "",
    val confirmNewPasswordInput: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val isEmailUser: Boolean = true // Para mostrar/ocultar UI si no es usuario de email
)