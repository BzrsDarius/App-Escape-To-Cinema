// Paquete que define la ubicación de esta clase dentro del proyecto
package com.example.appescapetocinema.model

// Importaciones necesarias para la serialización con Kotlin
import kotlinx.serialization.Serializable

// Clase de datos que representa una pregunta de trivia
@Serializable // Anotación que indica que esta clase es serializable
data class TriviaQuestion(
    val id: String,                  // Identificador único de la pregunta
    val questionText: String,        // Texto de la pregunta
    val options: List<String>,       // Lista de opciones de respuesta
    val correctAnswerIndex: Int,     // Índice de la respuesta correcta en la lista de opciones
    val category: String? = null,    // Categoría de la pregunta (opcional)
    val difficulty: String? = null,  // Nivel de dificultad de la pregunta (opcional)
    val imageUrl: String? = null     // URL de una imagen relacionada con la pregunta (opcional)
)