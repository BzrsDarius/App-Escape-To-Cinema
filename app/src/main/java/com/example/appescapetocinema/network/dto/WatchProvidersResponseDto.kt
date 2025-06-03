// En data/network/dto/watchproviders/WatchProvidersResponseDto.kt (o similar)
package com.example.appescapetocinema.network.dto // Ajusta tu paquete

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchProvidersResponseDto(
    @SerialName("id") val id: Int, // El ID de la película de TMDb
    // Los resultados son un Mapa donde la clave es el código del país (ej. "ES", "US")
    // y el valor es el objeto CountrySpecificProvidersDto
    @SerialName("results") val results: Map<String, CountrySpecificProvidersDto> = emptyMap()
)
