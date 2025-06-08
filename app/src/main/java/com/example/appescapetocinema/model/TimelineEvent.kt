package com.example.appescapetocinema.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Enum que representa los tipos de eventos en la línea de tiempo
enum class TimelineEventType {
    MOVIE_RELEASE,         // Representa el lanzamiento de una película
    TECHNOLOGY,            // Representa un avance tecnológico
    PERSON_MILESTONE,      // Representa un logro importante de una persona
    STUDIO_FORMATION,      // Representa la formación de un estudio cinematográfico
    CINEMATIC_MOVEMENT,    // Representa un movimiento cinematográfico
    HISTORICAL_EVENT       // Representa un evento histórico relevante
}

// Clase de datos que representa un evento en la línea de tiempo
@Serializable // Anotación que indica que esta clase es serializable
data class TimelineEvent(
    @SerialName("id") val id: String,                     // Identificador único del evento
    @SerialName("year") val year: Int,                   // Año en el que ocurrió el evento
    @SerialName("month") val month: Int? = null,         // Mes en el que ocurrió el evento (opcional)
    @SerialName("day") val day: Int? = null,             // Día en el que ocurrió el evento (opcional)
    @SerialName("title") val title: String,              // Título del evento
    @SerialName("description") val description: String,  // Descripción detallada del evento
    @SerialName("eventType") val eventType: TimelineEventType, // Tipo de evento según el enum definido
    @SerialName("imageUrl") val imageUrl: String? = null,       // URL de una imagen relacionada con el evento (opcional)
    @SerialName("relatedMovieTMDbId") val relatedMovieTMDbId: Int? = null // ID de una película relacionada en TMDb (opcional)
)