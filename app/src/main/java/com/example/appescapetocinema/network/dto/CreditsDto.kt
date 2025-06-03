// En app/src/main/java/com/example/appescapetocinema/network/dto/CreditsDto.kt
package com.example.appescapetocinema.network.dto

// Importamos las anotaciones necesarias para la serialización con Kotlinx Serialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Marcamos la clase como @Serializable para que pueda ser serializada/deserializada con Kotlinx Serialization
@Serializable
data class CastMemberDto(
    // El ID único del miembro del reparto
    val id: Int,

    // El nombre del miembro del reparto. Puede ser nulo si no hay información disponible.
    val name: String?,

    // La ruta al perfil del miembro del reparto (imagen). Puede ser nulo si no hay información disponible.
    @SerialName("profile_path") val profilePath: String?,

    // El personaje interpretado por el miembro del reparto. Puede ser nulo si no hay información disponible.
    val character: String?,

    // El orden en el que aparece el miembro del reparto en los créditos.
    val order: Int
)

@Serializable
data class CrewMemberDto(
    // El ID único del miembro del equipo técnico
    val id: Int,

    // El nombre del miembro del equipo técnico. Puede ser nulo si no hay información disponible.
    val name: String?,

    // La ruta al perfil del miembro del equipo técnico (imagen). Puede ser nulo si no hay información disponible.
    @SerialName("profile_path") val profilePath: String?,

    // El trabajo desempeñado por el miembro del equipo técnico (ej. "Director", "Screenplay"). Puede ser nulo.
    val job: String?
)

@Serializable
data class CreditsDto(
    // El ID de la película o serie a la que pertenecen los créditos
    val id: Int,

    // La lista de miembros del reparto. Puede ser nula si no hay información disponible.
    val cast: List<CastMemberDto>?,

    // La lista de miembros del equipo técnico. Puede ser nula si no hay información disponible.
    val crew: List<CrewMemberDto>?
)