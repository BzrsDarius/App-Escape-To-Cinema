package com.example.appescapetocinema.ui.profile

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items // Necesario para LazyRow/LazyColumn items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer // Importar M3 PullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState // Importar M3 PullToRefresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll // Para PullToRefresh
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey

// --- Importaciones de tu proyecto (AJUSTA PAQUETES) ---
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase
import com.example.appescapetocinema.ui.detail.MovieDetails
import com.example.appescapetocinema.ui.components.MovieCard // Importa MovieCard
import com.example.appescapetocinema.repository.* // Importa todos los repositorios
import com.example.appescapetocinema.ui.trivia.triviaCategories
import com.example.appescapetocinema.ui.components.MovieItem
// Importa helpers/constantes de logros (AJUSTA PAQUETE)
import com.example.appescapetocinema.util.* // O importa IDs individuales
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale


// --- ProfileScreen (UI - Con Logros y PullToRefresh) ---
@OptIn(ExperimentalMaterial3Api::class) // Solo M3 es necesario ahora
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    myListPagingItems: LazyPagingItems<MovieDetails>?,
    onLogoutClick: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onRefreshProfile: () -> Unit,
    onRefreshMyList: () -> Unit,
    onOpenCountryDialog: () -> Unit,
    onCloseCountryDialog: () -> Unit,
    onUpdateCountryPreference: (String) -> Unit,
    onChangePasswordClick: () -> Unit,
    onOpenDeleteAccountConfirmationDialog: () -> Unit,
    onCloseDeleteAccountConfirmationDialog: () -> Unit,
    onProceedToReAuthForDelete: () -> Unit,
    onCloseReAuthDialogForDelete: () -> Unit,
    onDeleteAccountPasswordChange: (String) -> Unit,
    onConfirmAccountDeletionWithPassword: () -> Unit,
    onClearDeleteAccountMessages: () -> Unit,
    onClearMyListClick: () -> Unit,
    onConfirmClearMyList: () -> Unit,
    onDismissClearMyListDialog: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onClearSnackbarMessage: () -> Unit,
    onNavigateToAcknowledgements: () -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState()
    LaunchedEffect(Unit) { // `Unit` como clave significa que se ejecuta una vez
        Log.d("ProfileScreen", "LaunchedEffect(Unit): Entrando a ProfileScreen, llamando a onRefreshProfile()")
        onRefreshProfile()
    }
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) { // Este se encarga del pull-to-refresh manual
            Log.d("ProfileScreen", "PullToRefresh: Iniciando refresco manual.")
            onRefreshProfile()
        }
    }
    LaunchedEffect(uiState.isLoading) {
        Log.d("ProfileScreen", "LaunchedEffect para uiState.isLoading. Nuevo valor: ${uiState.isLoading}, pullRefreshState.isRefreshing: ${pullRefreshState.isRefreshing}")
        if (!uiState.isLoading) {
            if (pullRefreshState.isRefreshing) {
                Log.d("ProfileScreen", "PullToRefresh: uiState.isLoading es false, llamando a pullRefreshState.endRefresh()")
                pullRefreshState.endRefresh()
            }
        }
    }
    val clearMyListMessage = uiState.clearMyListMessage
    LaunchedEffect(clearMyListMessage) {
        if (clearMyListMessage != null) {
            snackbarHostState.showSnackbar(
                message = clearMyListMessage,
                duration = SnackbarDuration.Short
            )
            onClearSnackbarMessage() // Limpiar el mensaje en el ViewModel para que no se muestre de nuevo
        }
    }
    val deleteAccountSuccessMessage = uiState.deleteAccountSuccessMessage
    val deleteAccountErrorMessage = uiState.deleteAccountError

    LaunchedEffect(deleteAccountSuccessMessage) {
        if (deleteAccountSuccessMessage != null) {
            snackbarHostState.showSnackbar(
                message = deleteAccountSuccessMessage,
                duration = SnackbarDuration.Long // Mensaje importante
            )
            onClearDeleteAccountMessages() // Limpiar el mensaje en el ViewModel
        }
    }
    LaunchedEffect(deleteAccountErrorMessage) {
        if (deleteAccountErrorMessage != null && !uiState.showDeleteAccountReAuthDialog) { // No mostrar si el diálogo de reauth ya lo muestra
            snackbarHostState.showSnackbar(
                message = deleteAccountErrorMessage,
                duration = SnackbarDuration.Long,
                actionLabel = if (uiState.isDeletingAccount) null else "Reintentar" // Ocultar reintento si está en proceso
            )
            onClearDeleteAccountMessages()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onLogoutClick, enabled = !uiState.isLoggingOut) {
                        if (uiState.isLoggingOut) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Cerrar Sesión", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box( Modifier.padding(paddingValues).nestedScroll(pullRefreshState.nestedScrollConnection).fillMaxSize() ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) { // Para scroll general de la pantalla
                // --- Carga/Error Inicial General ---
                when {
                    uiState.isLoading && myListPagingItems == null -> { // Cargando IDs y email
                        item { FullScreenLoadingIndicator() }
                        return@LazyColumn // No mostrar nada más mientras carga
                    }
                    uiState.errorMessage != null && myListPagingItems == null -> {
                        item { FullScreenError(errorMessage = uiState.errorMessage, onRetry = onRefreshProfile) }
                        return@LazyColumn
                    }
                }

                // --- Sección Info Usuario ---
                item { UserInfoSection(email = uiState.userEmail) }
                item { SectionDivider() }

                // --- Sección Mi Lista (Título) ---
                item { SectionTitle("Mi Lista", isLoading = (myListPagingItems?.loadState?.refresh is LoadState.Loading && (myListPagingItems.itemCount > 0))) } // Muestra carga si ya hay items

                // --- Mi Lista Contenido (Cuadrícula Paginada) ---
                if (myListPagingItems != null) {
                    // Mostrar cuadrícula o mensaje de vacío/error basado en PagingItems
                    val refreshState = myListPagingItems.loadState.refresh
                    val itemCount = myListPagingItems.itemCount

                    when {
                        // Error inicial de Paging (después de obtener IDs)
                        refreshState is LoadState.Error && itemCount == 0 -> {
                            item {
                                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Error al cargar Mi Lista.", color = MaterialTheme.colorScheme.error)
                                    Button(onClick = onRefreshMyList) { Text("Reintentar") }
                                }
                            }
                        }
                        // Lista vacía (después de carga exitosa de Paging)
                        refreshState is LoadState.NotLoading && itemCount == 0 && !uiState.isLoading -> { // Y no está cargando IDs
                            item { EmptyListMessage("Aún no has añadido películas a tu lista.") }
                        }
                        // Mostrar la grid
                        else -> {
                            // Un item para la LazyVerticalGrid
                            item {

                                val numberOfRowsToShow = 3 // Mostrar hasta 3 filas de ejemplo
                                val approxCardHeight = 270.dp // Altura aprox de MovieCard + padding

                                // --- Verificamos si myListPagingItems no es nulo antes de usarlo ---
                                myListPagingItems?.let { currentPagingItems -> // Usamos currentPagingItems que es no nulo aquí
                                    // Obtener itemCount desde la instancia no nula
                                    val itemCount = currentPagingItems.itemCount

                                    // Solo mostrar la grid si hay items o si el estado de refresh es Loading
                                    if (itemCount > 0 || currentPagingItems.loadState.refresh is LoadState.Loading) {
                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = 150.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = approxCardHeight * numberOfRowsToShow) // Limitar altura
                                                .padding(bottom = 8.dp),
                                        ) {
                                            items(
                                                count = currentPagingItems.itemCount,
                                                key = currentPagingItems.itemKey { movieDetails -> movieDetails.id },
                                                contentType = currentPagingItems.itemContentType { "myListMovie" }
                                            ) { index ->
                                                val movieDetails = currentPagingItems[index] // Accedemos con seguridad al item
                                                if (movieDetails != null) { // Verificamos que no sea null
                                                    val movieItem = MovieItem(
                                                        id = movieDetails.id,
                                                        title = movieDetails.title,
                                                        posterUrl = movieDetails.posterUrl
                                                    )
                                                    MovieCard(movie = movieItem, onClick = { onMovieClick(movieDetails.id) })
                                                } else {
                                                    // Shimmer o placeholder para Paging
                                                    Box(
                                                        modifier = Modifier
                                                            .width(150.dp)
                                                            .height(260.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                    )

                                                }
                                            }
                                        } // Fin LazyVerticalGrid

                                        if (itemCount > (numberOfRowsToShow * 2)) { // Asumiendo 2 items por fila
                                            TextButton(
                                                onClick = { /* TODO: Navegar a pantalla completa de Mi Lista */ },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Ver todas (${itemCount})", textAlign = TextAlign.Center)
                                            }
                                        }
                                    } else if (currentPagingItems.loadState.refresh is LoadState.NotLoading && itemCount == 0) {
                                        // Si la carga terminó y no hay items, mostrar el mensaje de lista vacía
                                    }
                                } ?: run {
                                    // Esto se ejecuta si myListPagingItems ES null
                                    if (!uiState.isLoading) { // Solo si no hay una carga global
                                        // EmptyListMessage("Mi Lista no está disponible en este momento.")
                                    }
                                }
                            }
                        }
                    }
                } else if (!uiState.isLoading) { // Si PagingItems es null Y no estamos cargando IDs
                    item { EmptyListMessage("Mi Lista no está disponible.") }
                }


                item { SectionDivider(modifier = Modifier.padding(top = 16.dp)) }
                // --- Sección Logros ---
                item {
                    SectionTitle(
                        "Logros", // Título más genérico
                        isLoading = uiState.isLoading && uiState.achievementsForDisplay.isEmpty() // Carga si está en estado general de carga Y la lista está vacía
                    )
                }

// Mostrar shimmer o placeholders si isLoadingAchievements es true y la lista está vacía
                if (uiState.isLoading && uiState.achievementsForDisplay.isEmpty()) { // O usa isLoadingAchievements si lo implementas
                    // Muestra algunos placeholders/shimmers para los logros
                    items(3) { index -> // Muestra 3 placeholders de ejemplo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .height(70.dp) // Altura similar a la tarjeta real
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                } else if (uiState.achievementsForDisplay.isEmpty() && !uiState.isLoading) { // No hay logros y no está cargando
                    item { EmptyListMessage("¡Sigue explorando para desbloquear logros!") }
                } else {
                    // Itera sobre la nueva lista
                    items(
                        items = uiState.achievementsForDisplay,
                        key = { it.definition.id } // Usa el ID de la definición como clave
                    ) { achievementDisp ->
                        AchievementItem(achievementDisplay = achievementDisp) // Pasa el objeto AchievementDisplayInfo
                        // Ya no necesitas el Spacer aquí si AchievementItem tiene padding vertical
                    }
                }
                item { SectionDivider(modifier = Modifier.padding(top = 16.dp)) } // Separador antes de puntuaciones

                // --- PUNTUACIONES MÁXIMAS TRIVIA ---
                item {
                    SectionTitle("Mejores Puntuaciones Trivia", isLoading = false)
                }

                if (uiState.triviaHighScores.isEmpty()) {
                    // Mensaje si no hay puntuaciones guardadas
                    item { EmptyListMessage("Aún no has completado ninguna trivia.") }
                } else {
                    // Muestra las puntuaciones ordenadas
                    // Convertimos el mapa a lista y ordenamos por categoría (o puntuación)
                    val sortedScores = uiState.triviaHighScores.toList()
                        .sortedBy { (categoryId, _) ->
                            // Ordena usando el nombre legible de la categoría
                            triviaCategories.find { it.first == categoryId }?.second ?: categoryId
                        }

                    items(sortedScores) { (categoryId, score) ->
                        HighScoreItem(
                            // Busca el nombre legible de la categoría
                            categoryName = triviaCategories.find { it.first == categoryId }?.second ?: categoryId,
                            highScore = score
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                item { SectionDivider(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) }

                item {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp, end = 16.dp), // Más padding inferior
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                item { SettingItemDivider() }
                item {
                    SettingsItem(
                        icon = Icons.Filled.Logout,
                        title = "Cerrar Sesión",
                        onClick = onLogoutClick, // Reutiliza la lambda existente
                        titleColor = MaterialTheme.colorScheme.error,
                        showTrailingIcon = false // No necesita flecha
                    )
                }

                // --- Sub-sección Cuenta ---
                item { SettingItemDivider() }
                item {
                    SettingsItem(
                        icon = Icons.Filled.DeleteForever,
                        title = "Eliminar Mi Cuenta",
                        onClick = onOpenDeleteAccountConfirmationDialog,
                        titleColor = MaterialTheme.colorScheme.error
                    )
                }
                item { SettingItemDivider() }

                // --- Sub-sección Mis Datos ---
                item {
                    SettingsItem(
                        icon = Icons.Filled.ListAlt,
                        title = "Borrar 'Mi Lista'",
                        onClick = onClearMyListClick, // Llama a la lambda para abrir el diálogo
                        titleColor = MaterialTheme.colorScheme.error
                    )
                }
                item { SettingItemDivider() }
                item {
                    // Solo mostrar si el usuario está logueado con Email/Contraseña
                    SettingsItem(
                        icon = Icons.Filled.Password, // O Icons.Filled.Key
                        title = "Cambiar Contraseña",
                        onClick = {
                            // Navegar a la nueva pantalla de cambio de contraseña
                            onChangePasswordClick() // Lambda que el ProfileScreenContainer manejará
                        }
                    )
                }
                item { SettingItemDivider() }

                // --- Sub-sección Preferencias ---
                item {
                    SettingsItem(
                        icon = Icons.Filled.Public,
                        title = "País para 'Dónde Ver'",
                        subtitle = "Actual: ${getCountryDisplayName(uiState.watchProviderCountryPreference, uiState.availableWatchProviderCountries)}",
                        onClick = onOpenCountryDialog
                    )
                }
                item { SettingItemDivider() }

                // --- Sub-sección Información ---
                item {
                    val uriHandler = LocalUriHandler.current
                    SettingsItem(
                        icon = Icons.Filled.Article,
                        title = "Política de Privacidad",
                        onClick = {
                            val privacyPolicyUrl = "https://escape-to-cinema-landing-page-750534913480.us-west1.run.app"
                            try { uriHandler.openUri(privacyPolicyUrl) }
                            catch (e: Exception) { Log.e("ProfileScreen", "Error URL Política", e) }
                        }
                    )
                }
                item { SettingItemDivider() }
                item {
                    SettingsItem(
                        icon = Icons.Filled.FavoriteBorder, // O Icons.Filled.Info, Icons.Filled.EmojiPeople
                        title = "Agradecimientos y Atribuciones",
                        onClick = onNavigateToAcknowledgements
                    )
                }

                item { Spacer(modifier = Modifier.height(32.dp)) } // Más espacio al final de la lista de Ajustes
                // --- *** FIN SECCIÓN AJUSTES *** ---

            } // Fin LazyColumn

            PullToRefreshContainer(state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter), contentColor = MaterialTheme.colorScheme.primary)
            if (uiState.showCountrySelectionDialog) {
                CountrySelectionDialog(
                    availableCountries = uiState.availableWatchProviderCountries,
                    isLoading = uiState.isLoadingCountries,
                    currentSelectedCountryCode = uiState.watchProviderCountryPreference,
                    onCountrySelected = onUpdateCountryPreference,
                    onDismiss = onCloseCountryDialog
                )
            }
            if (uiState.showClearMyListDialog) {
                AlertDialog(
                    onDismissRequest = onDismissClearMyListDialog, // Llama al ViewModel para cerrar
                    title = { Text("Borrar Mi Lista") },
                    text = { Text("¿Estás seguro de que quieres borrar todas las películas de 'Mi Lista'? Esta acción no se puede deshacer.") },
                    confirmButton = {
                        Button(
                            onClick = onConfirmClearMyList, // Llama al ViewModel para confirmar
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Borrar") }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissClearMyListDialog) { Text("Cancelar") }
                    }
                )
            }
            if (uiState.showDeleteAccountConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = onCloseDeleteAccountConfirmationDialog,
                    icon = { Icon(Icons.Filled.Warning, contentDescription = "Advertencia", tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Eliminar Cuenta") },
                    text = { Text("¿Estás absolutamente seguro de que quieres eliminar tu cuenta? Todos tus datos (listas, valoraciones, reseñas, logros, etc.) se borrarán permanentemente. Esta acción no se puede deshacer.") },
                    confirmButton = {
                        Button(
                            onClick = onProceedToReAuthForDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Sí, Eliminar") }
                    },
                    dismissButton = {
                        TextButton(onClick = onCloseDeleteAccountConfirmationDialog) { Text("Cancelar") }
                    }
                )
            }

            if (uiState.showDeleteAccountReAuthDialog) {
                var passwordVisible by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = onCloseReAuthDialogForDelete, // Permite cerrar si el usuario quiere cancelar
                    title = { Text("Reautenticación Requerida") },
                    text = {
                        Column {
                            Text("Para continuar con la eliminación de tu cuenta, por favor, ingresa tu contraseña actual.")
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.deleteAccountPasswordInput,
                                onValueChange = onDeleteAccountPasswordChange,
                                label = { Text("Contraseña Actual") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña")
                                    }
                                },
                                isError = uiState.deleteAccountError != null,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (uiState.deleteAccountError != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(uiState.deleteAccountError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (!uiState.isDeletingAccount) { // Evitar múltiples clics
                                    onConfirmAccountDeletionWithPassword()
                                }
                            },
                            enabled = !uiState.isDeletingAccount && uiState.deleteAccountPasswordInput.isNotBlank()
                        ) {
                            if (uiState.isDeletingAccount) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Eliminando...")
                            } else {
                                Text("Confirmar Eliminación")
                            }
                        }
                    },
                    dismissButton = {
                        if (!uiState.isDeletingAccount) { // No permitir cancelar si ya está en proceso
                            TextButton(onClick = onCloseReAuthDialogForDelete) { Text("Cancelar") }
                        }
                    }
                )
            }
            // Indicador de carga global si isDeletingAccount es true pero los diálogos están cerrados (ej. después de reauth)
            if (uiState.isDeletingAccount && !uiState.showDeleteAccountReAuthDialog) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
@Composable
fun HighScoreItem(
    categoryName: String,
    highScore: Long, // Recibe Long desde Firestore
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Separa nombre y puntuación
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.EmojiEvents, // Icono de trofeo/premio
                contentDescription = "Puntuación Máxima",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.tertiary // Color Terciario (NeonOrange?)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = categoryName, // Nombre legible de la categoría
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = highScore.toString(), // Muestra la puntuación
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold, // Destaca la puntuación
            color = MaterialTheme.colorScheme.tertiary // Mismo color que icono
        )
    }
}


// --- Composables Auxiliares para ProfileScreen ---

@Composable
private fun UserInfoSection(email: String?) {
    Row( modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically ) {
        Icon(Icons.Filled.Person, "Usuario", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Usuario", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(email ?: "Email no disponible", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(title: String, isLoading: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end=16.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text( title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f) )
        if(isLoading){ CircularProgressIndicator(modifier=Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun SectionDivider(modifier: Modifier = Modifier) {
    Divider(modifier = modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun MyListRow(movies: List<MovieDetails>, onMovieClick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = movies, key = { movie -> movie.id }) { movieDetails ->
            val movieItem = MovieItem(movieDetails.id, movieDetails.title, movieDetails.posterUrl)
            MovieCard( movie = movieItem, onClick = { onMovieClick(movieDetails.id) } )
        }
    }
}

@Composable
fun AchievementItem( // Puedes renombrarlo a AchievementCard si prefieres
    achievementDisplay: AchievementDisplayInfo, // Recibe el nuevo objeto
    modifier: Modifier = Modifier
) {
    val cardBackgroundColor = if (achievementDisplay.isUnlocked) {
        MaterialTheme.colorScheme.surfaceVariant // Color normal para desbloqueado
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) // Más tenue para bloqueado
    }

    val iconToShow = if (achievementDisplay.isUnlocked) {
        achievementDisplay.definition.iconUnlocked
    } else {
        achievementDisplay.definition.iconLocked
    }

    val iconColor = if (achievementDisplay.isUnlocked) {
        MaterialTheme.colorScheme.secondary // NeonMagenta para desbloqueado (o tu elección)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // Grisáceo para bloqueado
    }

    val titleTextColor = if (achievementDisplay.isUnlocked) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    val descriptionTextColor = if (achievementDisplay.isUnlocked) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp), // Ajusta padding
        elevation = CardDefaults.cardElevation(defaultElevation = if (achievementDisplay.isUnlocked) 3.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(10.dp) // Ajusta forma si quieres
    ) {
        Row(
            modifier = Modifier.padding(12.dp).heightIn(min = 56.dp), // Padding interno y altura mínima
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconToShow,
                contentDescription = achievementDisplay.definition.name,
                modifier = Modifier.size(38.dp), // Ajusta tamaño
                tint = iconColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievementDisplay.definition.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleTextColor
                )
                Text(
                    text = achievementDisplay.definition.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = descriptionTextColor
                )
                // Mostrar fecha si está desbloqueado y la fecha existe
                if (achievementDisplay.isUnlocked && achievementDisplay.unlockedDate != null) {
                    Spacer(Modifier.height(4.dp))
                    val formattedDate = remember(achievementDisplay.unlockedDate) {
                        // Formatear Timestamp a String. Maneja posibles errores.
                        try {
                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            sdf.format(achievementDisplay.unlockedDate.toDate())
                        } catch (e: Exception) {
                            Log.e("AchievementItem", "Error formateando fecha", e)
                            "Fecha no disp." // Fallback
                        }
                    }
                    Text(
                        text = "Desbloqueado: $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (achievementDisplay.isUnlocked) MaterialTheme.colorScheme.tertiary else descriptionTextColor
                    )
                }
            }
            if (!achievementDisplay.isUnlocked) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Lock, // Icono de candado
                    contentDescription = "Bloqueado",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

fun getCountryDisplayName(countryCode: String, countries: List<Pair<String, String>>): String {
    return countries.find { it.first == countryCode }?.second ?: countryCode
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface, // Usa onSurface para el título principal
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, // TextGrey para subtítulo
    showTrailingIcon: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp), // Un poco más de padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title, // Descripción para accesibilidad
            tint = MaterialTheme.colorScheme.primary, // NeonCyan para el icono
            modifier = Modifier.size(24.dp) // Tamaño estándar de icono
        )
        Spacer(modifier = Modifier.width(20.dp)) // Más espacio
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium, // Tu Orbitron Medium 16sp
                color = titleColor
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall, // Tu Orbitron Normal 12sp
                    color = subtitleColor
                )
            }
        }
        if (showTrailingIcon) {
            Icon(
                imageVector = Icons.Filled.ChevronRight, // O Icons.AutoMirrored.Filled.KeyboardArrowRight
                contentDescription = null, // Decorativo
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SettingItemDivider() {
    Divider(
        modifier = Modifier.padding(
            start = (20.dp + 24.dp + 20.dp), // padding_start + icon_size + spacer_width
            end = 16.dp
        ),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), // Más sutil
        thickness = 0.5.dp // Más fino
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionDialog(
    availableCountries: List<Pair<String, String>>, // Pair de (code, name)
    isLoading: Boolean,
    currentSelectedCountryCode: String,
    onCountrySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar País", style = MaterialTheme.typography.titleLarge) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 300.dp) // Limitar altura y permitir scroll
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    availableCountries.isEmpty() -> {
                        Text(
                            "No se pudieron cargar los países o no hay disponibles.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        val scrollState = rememberScrollState()
                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            availableCountries.forEach { (code, name) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCountrySelected(code) }
                                        .padding(vertical = 10.dp, horizontal = 8.dp), // Padding para cada item
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = code.equals(currentSelectedCountryCode, ignoreCase = true),
                                        onClick = { onCountrySelected(code) },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "$name ($code)",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CERRAR", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
            }
        }
    )
}


@Composable
private fun EmptyListMessage(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical = 32.dp), contentAlignment = Alignment.Center) { // Más padding vertical
        Text( message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center )
    }
}

@Composable
private fun FullScreenLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun FullScreenError(errorMessage: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text( errorMessage ?: "Error desconocido", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
fun ProfileScreenContainer(
    navController: NavController,
    authRepository: RepositorioAutenticacionFirebase = remember { RepositorioAutenticacionFirebase() },
    movieRepository: MovieRepositoryImpl = remember { MovieRepositoryImpl() },
    userRepository: UserRepositoryImpl = remember { UserRepositoryImpl() },
    userProfileRepository: UserProfileRepositoryImpl = remember { UserProfileRepositoryImpl() }
) {
    val application = LocalContext.current.applicationContext as Application

    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(
            application,
            movieRepository,
            userRepository,
            authRepository,
            userProfileRepository
        )
    )
    val uiState by profileViewModel.uiState.collectAsState()
    val myListPagingItems = profileViewModel.myListMoviesFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isLoggingOut, authRepository.obtenerUsuarioActual(), uiState.deleteAccountSuccessMessage) {
        val userLoggedOut = authRepository.obtenerUsuarioActual() == null
        if ((uiState.isLoggingOut && userLoggedOut) || (uiState.deleteAccountSuccessMessage != null && userLoggedOut)) {
            Log.d("ProfileContainer", "Logout o eliminación completada, navegando a Login.")
            if (uiState.deleteAccountSuccessMessage != null) {
                // Pequeña demora para asegurar que el Snackbar se vea si es por eliminación
                // y permitir que el usuario lea el mensaje antes de la navegación.
                delay(2500)
            }

            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
            // Limpia el mensaje después de la navegación o si la navegación no ocurre pero el mensaje está presente
            // para evitar que se quede "pegado" si el usuario vuelve a esta pantalla sin una recarga completa.
            if (uiState.deleteAccountSuccessMessage != null) {
                profileViewModel.clearDeleteAccountMessages()
            }
        }
    }

    // Esta es la llamada completa a ProfileScreen con todos sus parámetros:
    ProfileScreen(
        uiState = uiState,
        myListPagingItems = myListPagingItems,
        onLogoutClick = profileViewModel::logout,
        onMovieClick = { movieId -> navController.navigate(Screen.Detail.createRoute(movieId)) },
        onRefreshProfile = profileViewModel::refresh,
        onRefreshMyList = { myListPagingItems.refresh() },
        onOpenCountryDialog = profileViewModel::openCountrySelectionDialog,
        onCloseCountryDialog = profileViewModel::closeCountrySelectionDialog,
        onUpdateCountryPreference = profileViewModel::updateWatchProviderCountryPreference,
        onChangePasswordClick = { navController.navigate(Screen.ChangePassword.route) },

        // Lambdas para "Eliminar Cuenta"
        onOpenDeleteAccountConfirmationDialog = profileViewModel::openDeleteAccountConfirmationDialog,
        onCloseDeleteAccountConfirmationDialog = profileViewModel::closeDeleteAccountConfirmationDialog,
        onProceedToReAuthForDelete = profileViewModel::proceedToReAuthForDelete,
        onCloseReAuthDialogForDelete = profileViewModel::closeReAuthDialogForDelete,
        onDeleteAccountPasswordChange = profileViewModel::onDeleteAccountPasswordInputChange,
        onConfirmAccountDeletionWithPassword = profileViewModel::confirmAccountDeletionWithPassword,
        onClearDeleteAccountMessages = profileViewModel::clearDeleteAccountMessages,
        // Lambdas para "Borrar Mi Lista"
        onClearMyListClick = profileViewModel::openClearMyListDialog,
        onConfirmClearMyList = profileViewModel::confirmClearMyList,
        onDismissClearMyListDialog = profileViewModel::closeClearMyListDialog,

        // Snackbar y limpieza de mensaje de "Borrar Mi Lista"
        snackbarHostState = snackbarHostState,
        onClearSnackbarMessage = profileViewModel::clearClearMyListMessage,
        onNavigateToAcknowledgements = {
            navController.navigate(Screen.Acknowledgements.route)
        }
    )
}