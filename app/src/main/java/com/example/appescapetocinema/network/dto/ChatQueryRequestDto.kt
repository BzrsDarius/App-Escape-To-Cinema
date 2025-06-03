// En un paquete como data/network/dto/chat o similar
package com.example.appescapetocinema.network.dto // Ajusta tu paquete

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatQueryRequestDto(
    @SerialName("query") val query: String
)