package com.example.appescapetocinema.ui.showtimes

import android.content.Context
import android.location.LocationManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.* // LocationOn, Error, Theaters, etc.
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appescapetocinema.model.CinemaShowtimeResultItem
import com.example.appescapetocinema.ui.cinemas.GpsDisabledContent
import com.example.appescapetocinema.ui.cinemas.PermissionDeniedContent
import java.util.* // Para Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyShowtimesScreen(
    uiState: NearbyShowtimesUiState,
    onNavigateBack: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onFetchLocation: () -> Unit, // Para reintentar ubicación
    onRetry: () -> Unit, // Reintenta la lógica del VM con la ubicación actual
    onOpenLocationSettings: () -> Unit
) {
    val context = LocalContext.current
    // Comprobar GPS (similar a CinemasScreen)
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    // Necesitamos observar cambios en el estado del GPS, usar rememberUpdatedState o similar si es necesario
    // Por simplicidad, lo comprobamos una vez. Considera un State para esto si quieres que reaccione dinámicamente.
    val isGpsEnabled by remember { mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cines para: ${uiState.movieTitle}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium // Un poco más pequeño quizás
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            when {
                // --- Cargando ---
                uiState.isLoading -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Buscando horarios...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // --- Permiso Denegado ---
                !uiState.locationPermissionGranted -> {
                    // Reutiliza el Composable de CinemasScreen si lo hiciste público
                    PermissionDeniedContent(onRequestPermission = onRequestLocationPermission)
                }
                // --- GPS Desactivado ---
                !isGpsEnabled -> {
                    // Reutiliza el Composable de CinemasScreen
                    GpsDisabledContent(onOpenLocationSettings = onOpenLocationSettings)
                }
                // --- Error General ---
                uiState.error != null -> {
                    Column(Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(uiState.error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        // El botón de reintento aquí podría intentar obtener la ubicación de nuevo O reintentar la búsqueda si ya hay ubicación
                        Button(onClick = onRetry) { Text("Reintentar") }
                    }
                }
                // --- No se encontraron resultados ---
                uiState.noResultsFound && uiState.showtimeResults.isEmpty() -> {
                    Text(
                        "No se encontraron cines cercanos proyectando '${uiState.movieTitle}' hoy.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                // --- Mostrar Lista de Resultados ---
                uiState.showtimeResults.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre tarjetas
                    ) {
                        items(uiState.showtimeResults, key = { it.cinemaId }) { resultItem ->
                            CinemaShowtimeResultCard(item = resultItem)
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
                // --- Caso inicial o fallback ---
                else -> {
                    Text("Busca cines cercanos para esta película.", modifier = Modifier.align(Alignment.Center))
                }
            } // Fin when
        } // Fin Box
    } // Fin Scaffold
}

// --- Tarjeta para mostrar un cine y sus horarios ---
@Composable
fun CinemaShowtimeResultCard(item: CinemaShowtimeResultItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Theaters,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.cinemaName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                item.distance?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f", it)} mi", // O km
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Mostrar horarios por formato
            item.formattedShowtimes.forEach { (format, times) ->
                Row(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
                    Text(
                        "$format: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        times,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}