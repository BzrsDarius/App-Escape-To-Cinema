package com.example.appescapetocinema.ui.showtimes

import com.example.appescapetocinema.model.CinemaShowtimeResultItem

data class NearbyShowtimesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showtimeResults: List<CinemaShowtimeResultItem> = emptyList(),
    val movieTitle: String = "", // Para mostrar en la TopAppBar
    val locationPermissionGranted: Boolean = true, // La UI verificará y actualizará
    val isRequestingLocation: Boolean = false,
    val noResultsFound: Boolean = false // Flag para búsqueda exitosa sin resultados
)