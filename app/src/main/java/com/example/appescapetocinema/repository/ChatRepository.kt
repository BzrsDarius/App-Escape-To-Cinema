// En data/repository/ChatRepository.kt (o donde tengas tus interfaces de repositorio)
package com.example.appescapetocinema.repository // Ajusta tu paquete

// No necesita imports específicos más allá de Result y String si solo devuelve String

interface ChatRepository {
    /**
     * Envía la consulta del usuario al backend y obtiene la respuesta del bot.
     * @param userQuery El texto ingresado por el usuario.
     * @return Un Result que contiene la respuesta del bot como String en caso de éxito,
     *         o una excepción en caso de fallo.
     */
    suspend fun sendQueryToBot(userQuery: String): Result<String>
}