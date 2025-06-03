// En data/network/dto/watchproviders/ProviderDto.kt (o similar)
package com.example.appescapetocinema.network.dto // Ajusta tu paquete

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Clase que representa un proveedor de servicios de visualización
@Serializable
data class ProviderDto(
    // Ruta parcial al logo del proveedor
    @SerialName("logo_path") val logoPath: String?,

    // Identificador único del proveedor
    @SerialName("provider_id") val providerId: Int,

    // Nombre del proveedor
    @SerialName("provider_name") val providerName: String?,

    // Prioridad de visualización del proveedor. Puede ser nula si no está presente.
    @SerialName("display_priority") val displayPriority: Int? = null
)