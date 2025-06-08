package com.example.appescapetocinema.ui.settings.changepassword

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase
import com.example.appescapetocinema.ui.settings.changepassword.ChangePasswordViewModel.ChangePasswordViewModelFactory
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreenContainer(navController: NavController) {
    val authRepository = remember { RepositorioAutenticacionFirebase() }
    val firebaseAuthInstance = remember { FirebaseAuth.getInstance() }
    val viewModel: ChangePasswordViewModel = viewModel(
        factory = ChangePasswordViewModelFactory(authRepository, firebaseAuthInstance)
    )
    val uiState by viewModel.uiState.collectAsState()

    ChangePasswordScreen(
        uiState = uiState,
        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmNewPasswordChange = viewModel::onConfirmNewPasswordChange,
        onSubmit = viewModel::submitChangePassword,
        onNavigateBack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    uiState: ChangePasswordUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cambiar Contraseña") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.isEmailUser) {
                Text(
                    text = uiState.errorMessage ?: "Esta opción solo está disponible para usuarios registrados con correo y contraseña.",
                    color = if (uiState.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedTextField(
                    value = uiState.currentPasswordInput,
                    onValueChange = onCurrentPasswordChange,
                    label = { Text("Contraseña Actual") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
                OutlinedTextField(
                    value = uiState.newPasswordInput,
                    onValueChange = onNewPasswordChange,
                    label = { Text("Nueva Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
                OutlinedTextField(
                    value = uiState.confirmNewPasswordInput,
                    onValueChange = onConfirmNewPasswordChange,
                    label = { Text("Confirmar Nueva Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!uiState.isLoading) onSubmit() }),
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.newPasswordInput != uiState.confirmNewPasswordInput && uiState.confirmNewPasswordInput.isNotEmpty(),
                    enabled = !uiState.isLoading
                )

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.currentPasswordInput.isNotBlank() &&
                                uiState.newPasswordInput.isNotBlank() &&
                                uiState.confirmNewPasswordInput.isNotBlank() &&
                                uiState.newPasswordInput == uiState.confirmNewPasswordInput
                    ) {
                        Text("Actualizar Contraseña")
                    }
                }

                uiState.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
                uiState.successMessage?.let {
                    Text(it, color = Color.Green, modifier = Modifier.padding(top = 8.dp)) // Color verde para éxito
                }
            }
        }
    }
}