// Paquete que define la ubicación de esta clase dentro del proyecto
package com.example.appescapetocinema.model // O tu paquete

// Importación necesaria para trabajar con fechas
import java.util.Date // O usa String si prefieres

// Clase de datos que representa un artículo de noticias
data class NewsArticle(
    // ID único del artículo (puede ser la URL o un hash)
    val id: String,
    // Título del artículo
    val title: String,
    // Fuente del artículo (Ej: "Variety", "IGN Cine")
    val source: String,
    // Descripción breve o extracto del artículo
    val description: String?,
    // URL al artículo completo
    val url: String,
    // URL a una imagen destacada del artículo
    val imageUrl: String?,
    // Fecha de publicación del artículo
    val publishedDate: Date?
)