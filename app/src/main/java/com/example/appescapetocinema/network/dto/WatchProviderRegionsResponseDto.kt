package com.example.appescapetocinema.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Clase que representa una región de proveedor de visualización
@Serializable
data class WatchProviderRegionsResponseDto(
    // Código ISO 3166-1 de la región (ejemplo: "US", "ES")
    @SerialName("iso_3166_1") val iso3166_1: String,

    // Nombre en inglés de la región
    @SerialName("english_name") val englishName: String,

    // Nombre nativo de la región (puede ser nulo si no está disponible)
    @SerialName("native_name") val nativeName: String? = null
)

// Clase que representa una lista de regiones de proveedores de visualización
@Serializable
data class WatchProviderRegionsListResponseDto(
    // Lista de regiones obtenidas de la API, inicializada como una lista vacía por defecto
    @SerialName("results") val results: List<WatchProviderRegionsResponseDto> = emptyList()
)