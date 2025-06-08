package com.example.appescapetocinema.ui.login

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.appescapetocinema.ui.theme.AppEscapeToCinemaTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.example.appescapetocinema.R
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase


// --- Composable de UI Desacoplado ---
@Composable
fun LoginScreen(
    navController: NavController,
    // Parámetros para estado y eventos en lugar del ViewModel directo
    uiState: LoginUiState,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleLoginRequest: () -> Unit, // Para iniciar el flujo de Google
    onForgotPasswordClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginWithGoogleToken: (String?) -> Unit, // Para pasar el token desde el launcher
    onErrorShown: () -> Unit, // Para limpiar mensajes si es necesario
    onInfoShown: () -> Unit,
    onNavigationDone: () -> Unit // Necesaria para el LaunchedEffect
) {
    // --- Variables de UI que NO están en el ViewModel ---
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // --- Configuración Google Sign-In (Maneja Intents, se queda en UI) ---
    val context = LocalContext.current
    val googleSignInClient: GoogleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // Usa el ID del strings.xml
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // --- Activity Result Launcher para Google Sign-In ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginScreenGoogle", "Activity Result Recibido: ${result.resultCode}")
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val cuentaGoogle = task.getResult(ApiException::class.java)!!
            Log.d("LoginScreenGoogle", "Cuenta Google obtenida: ${cuentaGoogle.email}")
            // Llama a la lambda para pasar el token al ViewModel (a través del Container)
            onLoginWithGoogleToken(cuentaGoogle.idToken)
        } catch (e: ApiException) {
            Log.w("LoginScreenGoogle", "Google Sign In falló. Código: ${e.statusCode}", e)
            // Llama a la lambda con null para indicar el fallo
            onLoginWithGoogleToken(null)
        }
    }
    // --- FIN Configuración Google ---

    // --- Efecto para Navegación ---
    // Observa el flag del estado y navega cuando cambia a true
    LaunchedEffect(uiState.navigateToHome) {
        if (uiState.navigateToHome) {
            navController.navigate(Screen.Home.route) { // Usa la ruta segura
                popUpTo(Screen.Login.route) { inclusive = true } // Usa la ruta segura
                launchSingleTop = true
            }
            onNavigationDone() // Avisa (a través del Container) que ya se navegó
        }
    }
    // --- Fin Efecto Navegación ---

    // --- Layout Principal ---
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { // Color tema
        Column( modifier = Modifier.fillMaxWidth().padding(top = 50.dp, start = 32.dp, end = 32.dp).verticalScroll(scrollState).padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally ) {

            // Logo
            Image(painter = painterResource(id = R.drawable.logo2), contentDescription = "Logo", modifier = Modifier.height(300.dp).width(300.dp).padding(bottom = 10.dp), contentScale = ContentScale.Fit)

            // Título
            Text("¡Bienvenido de nuevo!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) // Typo/Color tema
            Spacer(modifier = Modifier.height(8.dp))
            // Enlace Registrarse
            val annotatedSignUpString = buildAnnotatedString {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)) { append("¿Aún no te has registrado? ") } // Color secundario
                pushStringAnnotation(tag = "NAV_REGISTER", annotation = "navigate_register")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, textDecoration = TextDecoration.Underline)) { append("Registrarse") } // Color primario
                pop()
            }
            ClickableText(text = annotatedSignUpString, onClick = { offset ->
                if (!uiState.isLoading) { // Usa estado del ViewModel
                    annotatedSignUpString.getStringAnnotations(tag = "NAV_REGISTER", start = offset, end = offset).firstOrNull()?.let {
                        onNavigateToRegister() // Llama a la lambda de navegación a registro
                    }
                }
            })
            Spacer(modifier = Modifier.height(20.dp))

            // Campo Correo
            OutlinedTextField( value = uiState.correo, onValueChange = onCorreoChange, label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                // --- Colores Tema TextField ---
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
                textStyle = MaterialTheme.typography.bodyLarge, // Typo tema
                singleLine = true, enabled = !uiState.isLoading, isError = uiState.errorMessage != null
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField( value = uiState.contrasena, onValueChange = onContrasenaChange, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
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
                ), // Colores tema
                textStyle = MaterialTheme.typography.bodyLarge, // Typo tema
                singleLine = true, enabled = !uiState.isLoading, isError = uiState.errorMessage != null
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Olvidaste Contraseña
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                val annotatedForgotString = buildAnnotatedString { pushStringAnnotation(tag = "ACTION_FORGOT", annotation = "action_forgot_password"); withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, textDecoration = TextDecoration.Underline)) { append("¿Olvidaste tu contraseña?") }; pop() }
                ClickableText(text = annotatedForgotString, onClick = { offset ->
                    if (!uiState.isLoading) { // Usa estado
                        annotatedForgotString.getStringAnnotations(tag = "ACTION_FORGOT", start = offset, end = offset).firstOrNull()?.let {
                            focusManager.clearFocus()
                            onForgotPasswordClick() // Llama a la lambda
                        }
                    }
                })
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Botón Login
            Button( onClick = { onLoginClick() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), enabled = !uiState.isLoading ) {
                Text("Iniciar Sesión", style = MaterialTheme.typography.labelLarge) // Typo tema
            }

            // Botón Google Sign-In
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Log.d("LoginScreenGoogle", "Botón Google presionado. Lanzando intent...")
                    onGoogleLoginRequest() // Llama a la lambda para limpiar mensajes
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent) // Lanza el intent (esto sigue aquí)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface, // Fondo claro
                    contentColor = MaterialTheme.colorScheme.onSurface // Texto oscuro/principal
                ),
                enabled = !uiState.isLoading // Usa estado
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.icono_google),
                        contentDescription = "Logo Google",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar Sesión con Google", style = MaterialTheme.typography.labelLarge) // Typo tema
                }
            }

            // Carga/Error/Info Box
            Box(modifier = Modifier.height(40.dp).padding(top = 8.dp)) {
                if (uiState.isLoading) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center)) } // Color tema
                else {
                    uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) } // Color/Typo tema
                    uiState.infoMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) } // Color/Typo tema (usamos primary para info)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

        } // Fin Column Login
    } // Fin Box Login
}


// --- Composable Contenedor (Conecta ViewModel con UI) ---
@Composable
fun LoginScreenContainer(
    navController: NavController,
    // Obtiene el ViewModel usando el factory
    viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(
        RepositorioAutenticacionFirebase()
    ))
) {
    // Observa el estado del ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Llama al Composable de UI pasándole el estado y las lambdas conectadas al ViewModel
    LoginScreen(
        navController = navController, // Pasa el NavController para la navegación a Registro
        uiState = uiState,
        onCorreoChange = viewModel::onCorreoChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onLoginClick = viewModel::loginWithEmailPassword,
        onGoogleLoginRequest = viewModel::clearMessages, // Acción antes de lanzar Google (ej: limpiar msjs)
        onForgotPasswordClick = viewModel::sendPasswordResetEmail,
        onNavigateToRegister = { navController.navigate(Screen.Register.route) }, // Navegación directa
        onLoginWithGoogleToken = viewModel::loginWithGoogleToken, // Pasa la función del VM
        onErrorShown = viewModel::clearMessages, // Define qué hacer cuando se muestra un error (ej: limpiar)
        onInfoShown = viewModel::clearMessages, // Define qué hacer cuando se muestra info (ej: limpiar)
        onNavigationDone = viewModel::onNavigationDone // Pasa la función del VM para resetear el flag
    )
}


// --- ViewModel Factory (Necesario para pasar dependencias al ViewModel) ---
class LoginViewModelFactory(
    private val repositorioAuth: RepositorioAutenticacionFirebase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repositorioAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for LoginViewModelFactory")
    }
}
