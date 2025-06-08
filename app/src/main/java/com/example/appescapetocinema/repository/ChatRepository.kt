package com.example.appescapetocinema.repository

interface ChatRepository {
    /**
     * Envía la consulta del usuario al backend y obtiene la respuesta del bot.
     * @param userQuery El texto ingresado por el usuario.
     * @return Un Result que contiene la respuesta del bot como String en caso de éxito,
     *         o una excepción en caso de fallo.
     */
    suspend fun sendQueryToBot(userQuery: String): Result<String>
}