package com.example.appescapetocinema.ui.showtimes

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appescapetocinema.repository.CinemaRepositoryImpl
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@SuppressLint("MissingPermission") // Justificado por comprobaciones y solicitudes
@Composable
fun NearbyShowtimesContainer(
    navController: NavController,
    movieTmdbId: Long,
    movieTitle: String, // Ya viene decodificada de la ruta
    movieImdbId: String,
    cinemaRepository: CinemaRepositoryImpl = remember { CinemaRepositoryImpl() },
    viewModel: NearbyShowtimesViewModel = viewModel(
        factory = NearbyShowtimesViewModel.Factory(cinemaRepository, movieTmdbId, movieTitle, movieImdbId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    // Guardar la última ubicación obtenida para reintentos
    var lastFetchedLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }


    // --- Función para obtener ubicación (Adaptada de CinemasScreenContainer) ---
    val fetchDeviceLocation: () -> Unit = fetchDeviceLocation@{
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!(fineLocationGranted || coarseLocationGranted)) {
            Log.w("NearbyShowtimesCont", "fetchDeviceLocation llamado sin permiso.")
            viewModel.onLocationPermissionResult(false)
            viewModel.setRequestingLocation(false)
            return@fetchDeviceLocation
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.w("NearbyShowtimesCont", "Proveedores de ubicación desactivados.")
            // La UI mostrará el mensaje de GPS basado en su propia comprobación
            viewModel.setRequestingLocation(false) // Detener carga si estaba activa
            viewModel.onLocationPermissionResult(true) // Tenemos permiso, pero GPS off
            return@fetchDeviceLocation
        }

        Log.d("NearbyShowtimesCont", "Iniciando fetchDeviceLocation...")
        viewModel.setRequestingLocation(true) // Indica que estamos buscando activamente
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY, // Prioridad alta para esta búsqueda puntual
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                Log.d("NearbyShowtimesCont", "Ubicación obtenida: Lat=${location.latitude}, Lon=${location.longitude}")
                lastFetchedLocation = location.latitude to location.longitude
                viewModel.findNearbyShowtimes(location.latitude, location.longitude)
            } else {
                Log.w("NearbyShowtimesCont", "getCurrentLocation devolvió null. Intentando lastLocation...")
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        Log.d("NearbyShowtimesCont", "Última ubicación: Lat=${lastLocation.latitude}, Lon=${lastLocation.longitude}")
                        lastFetchedLocation = lastLocation.latitude to lastLocation.longitude
                        viewModel.findNearbyShowtimes(lastLocation.latitude, lastLocation.longitude)
                    } else {
                        Log.w("NearbyShowtimesCont", "Última ubicación también es null.")
                        viewModel.retry(null, null) // Informa al VM que no hay ubicación
                    }
                }.addOnFailureListener { e ->
                    Log.e("NearbyShowtimesCont", "Error obteniendo última ubicación.", e)
                    viewModel.retry(null, null)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("NearbyShowtimesCont", "Error obteniendo ubicación actual.", exception)
            viewModel.retry(null, null)
        }
    }


    // --- Launcher Permisos (Idéntico a CinemasScreenContainer) ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val permissionGranted = permissions.values.any { it } // True si al menos uno se concedió
        viewModel.onLocationPermissionResult(permissionGranted)
        if (permissionGranted) {
            Log.d("NearbyShowtimesCont", "Permiso concedido. Obteniendo ubicación...")
            fetchDeviceLocation()
        } else {
            Log.w("NearbyShowtimesCont", "Permiso denegado.")
            viewModel.setRequestingLocation(false)
        }
    }

    // --- Efecto Inicial (Adaptado de CinemasScreenContainer) ---
    LaunchedEffect(Unit) { // Se ejecuta solo una vez al inicio
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!(fineLocationGranted || coarseLocationGranted)) {
            Log.d("NearbyShowtimesCont", "Permiso no concedido inicialmente. Solicitando...")
            viewModel.onLocationPermissionResult(false) // Informar estado inicial
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            viewModel.onLocationPermissionResult(true) // Ya teníamos permiso
            Log.d("NearbyShowtimesCont", "Permiso ya concedido, obteniendo ubicación...")
            fetchDeviceLocation()
        }
    }

    // --- Llamada a la UI ---
    NearbyShowtimesScreen(
        uiState = uiState,
        onNavigateBack = { navController.popBackStack() },
        onRequestLocationPermission = {
            Log.d("NearbyShowtimesCont", "Botón 'Conceder Permiso' presionado.")
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        },
        onFetchLocation = {
            Log.d("NearbyShowtimesCont", "Solicitando actualización de ubicación.")
            fetchDeviceLocation()
        },
        onRetry = {
            Log.d("NearbyShowtimesCont", "Botón 'Reintentar' presionado.")
            // Reintentar con la última ubicación conocida si existe
            viewModel.retry(lastFetchedLocation?.first, lastFetchedLocation?.second)
        },
        onOpenLocationSettings = {
            Log.d("NearbyShowtimesCont", "Abriendo ajustes de ubicación.")
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            try { context.startActivity(intent) }
            catch (e: Exception) { Log.e("NearbyShowtimesCont", "No se pudo abrir ajustes", e) }
        }
    )
}