package com.example.appescapetocinema.model

import java.util.UUID

// Enumeración que define los tipos de remitente en un mensaje de chat
enum class SenderType {
    // Remitente es el usuario
    USER,
    // Remitente es el bot
    BOT,
    // Remitente indica un error
    ERROR
}

// Clase de datos que representa un mensaje de chat
data class ChatMessage(
    // Identificador único del mensaje
    val id: String = UUID.randomUUID().toString(),
    // Texto del mensaje
    val text: String,
    // Tipo de remitente del mensaje
    val sender: SenderType,
    // Marca de tiempo del mensaje en milisegundos
    val timestamp: Long = System.currentTimeMillis()
)