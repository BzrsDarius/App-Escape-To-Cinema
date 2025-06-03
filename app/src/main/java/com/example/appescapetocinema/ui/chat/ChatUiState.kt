package com.example.appescapetocinema.ui.chat

import com.example.appescapetocinema.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isBotTyping: Boolean = false,
    val inputEnabled: Boolean = true,
    val errorMessage: String? = null
)