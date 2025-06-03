// Paquete que define la ubicación de esta clase dentro del proyecto
package com.example.appescapetocinema.model

// Importaciones necesarias para trabajar con fechas y Firestore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Clase de datos que representa el modelo UI para una reseña
data class Review(
    // Identificador único de la reseña
    val reviewId: String,       // userId
    // Identificador único de la película
    val movieId: String,
    // Identificador único del usuario
    val userId: String,
    // Nombre del usuario que realizó la reseña
    val userName: String,
    // Valoración asociada a la reseña
    val rating: Double,
    // Texto de la reseña
    val text: String,
    // Fecha de la reseña convertida
    val timestamp: Date?
)

// Clase de datos que representa el modelo para Firestore
data class ReviewFirestoreData(
    // Identificador único del usuario
    val userId: String = "",
    // Nombre del usuario que realizó la reseña
    val userName: String = "",
    // Valoración asociada a la reseña
    val rating: Double = 0.0,
    // Texto de la reseña
    val text: String = "",
    // Marca de tiempo gestionada por el servidor
    @ServerTimestamp val timestamp: Timestamp? = null
)