package com.example.appescapetocinema.ui.cinemas

import com.example.appescapetocinema.model.Cinema

data class CinemasUiState(
    val nearbyCinemas: List<Cinema> = emptyList(),
    val isLoading: Boolean = false, // Para carga de cines
    val errorMessage: String? = null,
    val locationPermissionGranted: Boolean = false,
    val lastKnownLatitude: Double? = null,
    val lastKnownLongitude: Double? = null,
    val isRequestingLocation: Boolean = false
)