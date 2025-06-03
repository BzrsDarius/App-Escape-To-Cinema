package com.example.appescapetocinema.ui.registro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

// --- Importa el Estado y el Factory ---
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase // Asegúrate que es el paquete correcto
import com.example.appescapetocinema.ui.theme.AppEscapeToCinemaTheme

@Composable
fun PantallaRegistro(
    navController: NavController, 
    uiState: RegistroUiState,
    onNombreChange: (String) -> Unit,
    onApellidoChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onContrasenaConfirmChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateBackToLogin: () -> Unit,
    onNavigationDone: () -> Unit // Para resetear flag post-registro
) {
    // --- Variables locales de UI ---
    val scrollState = rememberScrollState()

    // --- Efecto para Navegación Post-Registro ---
    LaunchedEffect(uiState.navigateToHome) {
        if (uiState.navigateToHome) {
            navController.navigate(Screen.Home.route) { // Navega a Home
                popUpTo(Screen.Login.route) { inclusive = true } // Limpia hasta Login
                launchSingleTop = true
            }
            onNavigationDone() // Avisa que se ha navegado
        }
    }

    // --- Layout Principal ---
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { // Color tema
        Column( modifier = Modifier.fillMaxWidth().padding(top = 50.dp, start = 32.dp, end = 32.dp).verticalScroll(scrollState).padding(bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally ) {
            Text("Crear Cuenta", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) // Typo/Color tema
            Spacer(modifier = Modifier.height(15.dp))

            // --- Campos de Texto ---
            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = onNombreChange,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true,
                enabled = !uiState.isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.apellido,
                onValueChange = onApellidoChange,
                label = { Text("Apellido") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true,
                enabled = !uiState.isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = uiState.correo, onValueChange = onCorreoChange, label = { Text("Correo Electrónico", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true, enabled = !uiState.isLoading)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.contrasena,
                onValueChange = onContrasenaChange,
                label = { Text("Contraseña", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true,
                enabled = !uiState.isLoading,
                // Marca error si las contraseñas NO coinciden Y el campo de confirmación no está vacío
                isError = !uiState.passwordsMatch && uiState.contrasenaConfirm.isNotEmpty()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.contrasenaConfirm,
                onValueChange = onContrasenaConfirmChange,
                label = { Text("Confirmar Contraseña", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, // Correct parameter
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // Correct parameter
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = !uiState.passwordsMatch // Marca error si no coinciden
            )

            // Mensaje si las contraseñas no coinciden
            if (!uiState.passwordsMatch && !uiState.isLoading) {
                Text("Las contraseñas no coinciden", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) // Color/Typo tema
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Botón Registrarse (Usa tema por defecto)
            Button( onClick = { onRegisterClick() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), enabled = !uiState.isLoading && uiState.passwordsMatch && uiState.contrasena.isNotEmpty() ) {
                Text("Registrarse")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Enlace Volver a Login
            TextButton( onClick = { if (!uiState.isLoading) onNavigateBackToLogin() }, enabled = !uiState.isLoading ) {
                Text( text = "¿Ya tienes cuenta? Inicia Sesión", textDecoration = TextDecoration.Underline ) // Color primario por defecto
            }

            // Indicador Carga / Error
            Box(modifier = Modifier.height(40.dp).padding(top = 8.dp)) {
                if (uiState.isLoading) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center)) } // Color tema
                else { uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) } } // Color/Typo tema
            }
            Spacer(modifier = Modifier.height(20.dp))
        } // Fin Column
    } // Fin Box
}

// --- Composable Contenedor ---
@Composable
fun PantallaRegistroContainer(
    navController: NavController,
    viewModel: RegistroViewModel = viewModel(factory = RegistroViewModelFactory(RepositorioAutenticacionFirebase()))
) {
    val uiState by viewModel.uiState.collectAsState()

    PantallaRegistro(
        navController = navController, // Necesario para el LaunchedEffect de navegación
        uiState = uiState,
        onNombreChange = viewModel::onNombreChange,
        onApellidoChange = viewModel::onApellidoChange,
        onCorreoChange = viewModel::onCorreoChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onContrasenaConfirmChange = viewModel::onContrasenaConfirmChange,
        onRegisterClick = viewModel::attemptRegistration,
        onNavigateBackToLogin = { navController.popBackStack() }, // Acción para volver
        onNavigationDone = viewModel::onNavigationDone
    )
}

