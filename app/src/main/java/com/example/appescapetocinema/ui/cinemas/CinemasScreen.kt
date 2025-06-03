package com.example.appescapetocinema.ui.cinemas

import android.Manifest // Importar Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager // Para comprobar GPS
import android.provider.Settings // Para abrir ajustes de ubicación
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // LocationOn, Map, Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.appescapetocinema.model.Cinema // Importa modelo UI Cinema
import com.example.appescapetocinema.repository.CinemaRepositoryImpl // Para Factory y preview
import com.example.appescapetocinema.ui.theme.AppEscapeToCinemaTheme
import com.google.android.gms.location.LocationServices // Para FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale // Para formatear distancia

// --- CinemasScreen (UI Desacoplada) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemasScreen(
    uiState: CinemasUiState,
    onRequestLocationPermission: () -> Unit, // Lambda para solicitar permiso
    onFetchLocationAndCinemas: () -> Unit, // Lambda para intentar obtener ubicación y cines
    onCinemaClick: (Long) -> Unit, // Pasa el ID del cine
    onRetry: () -> Unit,
    onOpenLocationSettings: () -> Unit // Para abrir ajustes si GPS está apagado
) {
    val context = LocalContext.current
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isGpsEnabled = remember { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cines Cercanos", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            when {
                // --- Solicitando Permiso o Ubicación ---
                uiState.isRequestingLocation || (uiState.isLoading && uiState.nearbyCinemas.isEmpty() && uiState.locationPermissionGranted && isGpsEnabled) -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally){
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Buscando cines...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // --- Permiso Denegado ---
                !uiState.locationPermissionGranted -> {
                    PermissionDeniedContent(onRequestPermission = onRequestLocationPermission)
                }
                // --- GPS Desactivado ---
                !isGpsEnabled -> {
                    GpsDisabledContent(onOpenLocationSettings = onOpenLocationSettings)
                }
                // --- Error General (después de tener permiso y GPS) ---
                uiState.errorMessage != null && uiState.nearbyCinemas.isEmpty() -> {
                    Column(Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRetry) { Text("Reintentar") } // onRetry intentará fetch si ya hay lat/lon
                    }
                }
                // --- Lista Vacía (después de carga exitosa) ---
                uiState.nearbyCinemas.isEmpty() && !uiState.isLoading -> {
                    Text("No se encontraron cines cercanos o para tu búsqueda.", modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // --- Mostrar Lista de Cines ---
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.nearbyCinemas, key = { it.id }) { cinema ->
                            CinemaCard(cinema = cinema, onClick = { onCinemaClick(cinema.id) })
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                    }
                }
            } // Fin when
        } // Fin Box
    } // Fin Scaffold
}

@Composable
fun CinemaCard(cinema: Cinema, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cinema.logoUrl?.let { logo -> // Muestra logo si existe
            AsyncImage(
                model = logo,
                contentDescription = "${cinema.name} logo",
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
        } ?: run { // Placeholder si no hay logo
            Icon(Icons.Filled.Theaters, contentDescription = "Cine", modifier = Modifier.size(40.dp).padding(end = 8.dp), tint = MaterialTheme.colorScheme.primary)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(cinema.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
            cinema.address?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        cinema.distance?.let {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${String.format(Locale.US, "%.1f", it)} mi", // Formato con 1 decimal
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.LocationOff, "Ubicación denegada", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Necesitamos tu permiso de ubicación para encontrar cines cercanos.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Conceder Permiso")
        }
    }
}

@Composable
fun GpsDisabledContent(onOpenLocationSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.LocationSearching, "GPS desactivado", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Parece que el GPS está desactivado. Actívalo para encontrar cines cercanos.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenLocationSettings) {
            Text("Abrir Ajustes de Ubicación")
        }
    }
}

@Composable
private fun locationManager(context: Context): LocationManager {
    return context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
}


@SuppressLint("MissingPermission") // Justificado por las comprobaciones y solicitudes de permiso
@Composable
fun CinemasScreenContainer(
    navController: NavController, // Para futura navegación a CinemaDetailScreen
    viewModel: CinemasViewModel = viewModel(factory = CinemasViewModelFactory(CinemaRepositoryImpl()))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    // FusedLocationProviderClient para obtener la ubicación actual de forma eficiente
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- Función para intentar obtener la ubicación ---
    val fetchDeviceLocation: () -> Unit = fetchDeviceLocation@{
        val testLatitude = 40.416775
        val testLongitude = -3.703790
        Log.d("CinemasContainer", "USANDO COORDENADAS DE PRUEBA ESPAÑA: Lat=$testLatitude, Lon=$testLongitude")
        viewModel.updateLastKnownLocation(testLatitude, testLongitude) // Esto llama a fetchNearbyCinemas en VM
        viewModel.setRequestingLocation(false) // Finge que ya terminó
        return@fetchDeviceLocation

        Log.d("CinemasContainer", "Iniciando fetchDeviceLocation...")
        viewModel.setRequestingLocation(true)
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            viewModel.setRequestingLocation(false)
            if (location != null) {
                Log.d("CinemasContainer", "Ubicación obtenida: Lat=${location.latitude}, Lon=${location.longitude}")
                viewModel.updateLastKnownLocation(location.latitude, location.longitude)
            } else {
                Log.w("CinemasContainer", "getCurrentLocation devolvió null. Intentando lastLocation...")
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        Log.d("CinemasContainer", "Última ubicación: Lat=${lastLocation.latitude}, Lon=${lastLocation.longitude}")
                        viewModel.updateLastKnownLocation(lastLocation.latitude, lastLocation.longitude)
                    } else {
                        Log.w("CinemasContainer", "Última ubicación también es null.")
                        viewModel.updateLastKnownLocation(null,null)
                    }
                }.addOnFailureListener { e ->
                    Log.e("CinemasContainer", "Error obteniendo última ubicación.", e)
                    viewModel.updateLastKnownLocation(null,null)
                }
            }
        }.addOnFailureListener { exception ->
            viewModel.setRequestingLocation(false)
            Log.e("CinemasContainer", "Error obteniendo ubicación actual.", exception)
            viewModel.updateLastKnownLocation(null,null)
        }
    }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val permissionGranted = fineLocationGranted || coarseLocationGranted

        viewModel.onLocationPermissionResult(permissionGranted) // Informa al ViewModel

        if (permissionGranted) {
            Log.d("CinemasContainer", "Permiso concedido por el usuario. Obteniendo ubicación...")
            fetchDeviceLocation() // Intenta obtener ubicación ahora que hay permiso
        } else {
            Log.w("CinemasContainer", "Permiso denegado por el usuario.")
            viewModel.setRequestingLocation(false) // Asegura que no se quede cargando
        }
    }

    // --- Efecto para solicitar permiso al inicio si no se ha concedido ---
    // O también si se quiere re-solicitar después de un error o acción del usuario
    LaunchedEffect(uiState.locationPermissionGranted, Unit) { // Se ejecuta si cambia el permiso o al inicio
        if (!uiState.locationPermissionGranted) {
            Log.d("CinemasContainer", "Permiso no concedido inicialmente. Solicitando...")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            // No marcamos isRequestingLocation aquí, el launcher lo hará si lanza la petición
        } else {
            // Si ya tenemos permiso, intentamos obtener la ubicación si aún no la tenemos o si falló antes
            if(uiState.lastKnownLatitude == null || uiState.lastKnownLongitude == null){
                Log.d("CinemasContainer", "Permiso ya concedido, intentando obtener ubicación...")
                fetchDeviceLocation()
            }
        }
    }

    // --- Llama a la UI de CinemasScreen ---
    CinemasScreen(
        uiState = uiState,
        onRequestLocationPermission = { // Para el botón en la UI de permiso denegado
            Log.d("CinemasContainer", "Botón 'Conceder Permiso' presionado.")
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        },
        onFetchLocationAndCinemas = { // Podría ser llamado por un botón "Buscar cerca de mí"
            Log.d("CinemasContainer", "Botón 'Buscar Cerca' presionado o reintento de ubicación.")
            fetchDeviceLocation()
        },
        onCinemaClick = { cinemaId ->
            Log.d("CinemasContainer", "Clic en cine ID: $cinemaId. Navegando a detalles del cine...")
            navController.navigate(Screen.Cinemas.createRoute(cinemaId))
        },
        onRetry = { // Para el botón de reintento general si falla la carga de cines
            Log.d("CinemasContainer", "Botón 'Reintentar' (general) presionado.")
            // El ViewModel reintentará con la última ubicación conocida si la tiene
            viewModel.retry()
        },
        onOpenLocationSettings = {
            Log.d("CinemasContainer", "Abriendo ajustes de ubicación del sistema.")
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            try { context.startActivity(intent) }
            catch (e: Exception) { Log.e("CinemasContainer", "No se pudo abrir ajustes de ubicación", e)}
        }
    )
}