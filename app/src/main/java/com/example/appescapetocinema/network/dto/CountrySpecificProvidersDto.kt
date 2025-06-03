// En data/network/dto/watchproviders/CountrySpecificProvidersDto.kt (o similar)
package com.example.appescapetocinema.network.dto // Ajusta tu paquete

// Importamos las anotaciones necesarias para la serialización con Kotlinx Serialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Marcamos la clase como @Serializable para que pueda ser serializada/deserializada con Kotlinx Serialization
@Serializable
data class CountrySpecificProvidersDto(
    // El campo "link" representa un enlace a la página de TMDb "dónde ver" para un país específico.
    // Puede ser nulo si no hay información disponible.
    @SerialName("link") val link: String?,

    // El campo "flatrate" contiene una lista de proveedores que ofrecen contenido bajo suscripción.
    // Puede ser nulo si no hay proveedores disponibles para este tipo de servicio.
    @SerialName("flatrate") val flatrate: List<ProviderDto>? = null,

    // El campo "rent" contiene una lista de proveedores que ofrecen contenido para alquiler.
    // Puede ser nulo si no hay proveedores disponibles para este tipo de servicio.
    @SerialName("rent") val rent: List<ProviderDto>? = null,

    // El campo "buy" contiene una lista de proveedores que ofrecen contenido para compra.
    // Puede ser nulo si no hay proveedores disponibles para este tipo de servicio.
    @SerialName("buy") val buy: List<ProviderDto>? = null,

    // El campo "ads" contiene una lista de proveedores que ofrecen contenido con anuncios.
    // Puede ser nulo si no hay proveedores disponibles para este tipo de servicio.
    @SerialName("ads") val ads: List<ProviderDto>? = null,

    // El campo "free" contiene una lista de proveedores que ofrecen contenido gratuito.
    // Puede ser nulo si no hay proveedores disponibles para este tipo de servicio.
    @SerialName("free") val free: List<ProviderDto>? = null
)