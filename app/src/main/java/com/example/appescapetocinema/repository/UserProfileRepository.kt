package com.example.appescapetocinema.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.example.appescapetocinema.util.ACHIEVEMENT_DECADE_TRAVELER_5
import com.example.appescapetocinema.util.ACHIEVEMENT_EASTER_EGG_CARPENTER
import com.example.appescapetocinema.util.ACHIEVEMENT_FIRST_ADD_MYLIST
import com.example.appescapetocinema.util.ACHIEVEMENT_FIRST_RATING
import com.example.appescapetocinema.util.ACHIEVEMENT_FIRST_REVIEW
import com.example.appescapetocinema.util.ACHIEVEMENT_PROFILE_COMPLETE_BASIC
import com.example.appescapetocinema.util.ACHIEVEMENT_REVIEW_AUTHOR_5
import com.example.appescapetocinema.util.ACHIEVEMENT_REVIEW_PRO_15
import com.example.appescapetocinema.util.ACHIEVEMENT_TRIVIA_TOTAL_SCORE_100

interface UserProfileRepository {
    suspend fun unlockAchievement(achievementId: String): Result<Boolean>
    fun getUnlockedAchievementDataFlow(): Flow<Map<String, Timestamp>>
    fun getUnlockedAchievementsFlow(): Flow<Set<String>>
    suspend fun updateTriviaHighScore(categoryId: String, score: Int): Result<Unit>
    fun getTriviaHighScoresFlow(): Flow<Map<String, Long>>
    suspend fun incrementFilteredSearchCountAndCheckAchievement(): Result<Unit>
    suspend fun incrementReviewCountAndCheckAchievements(): Result<Unit>
    suspend fun addSearchedDecadeAndCheckAchievement(year: Int): Result<Unit>
    suspend fun addScoreToTotalTriviaScoreAndCheckAchievement(scoreEarnedInRound: Int): Result<Unit>
    suspend fun addCarpenterMovieToProfile(movieId: Int): Result<Unit>
    suspend fun removeCarpenterMovieFromProfile(movieId: Int): Result<Unit>
}

class UserProfileRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : UserProfileRepository {

    private companion object {
        const val COLLECTION_USER_PROFILES = "user_profiles"
        const val FIELD_ACHIEVEMENTS = "achievements"
        const val FIELD_TRIVIA_HIGH_SCORES = "triviaHighScores"
    }

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    override fun getUnlockedAchievementDataFlow(): Flow<Map<String, Timestamp>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(emptyMap()) // Enviar mapa vacío
            close(IllegalStateException("User not logged in."))
            return@callbackFlow
        }
        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)
        val listener = profileDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("UserProfileRepo", "Listener Error for Achievements Data", error)
                trySend(emptyMap()) // Enviar mapa vacío
                return@addSnapshotListener
            }
            //enviar el mapa completo
            val achievementsMap = snapshot?.get(FIELD_ACHIEVEMENTS) as? Map<String, Timestamp> ?: emptyMap()
            trySend(achievementsMap)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun unlockAchievement(achievementId: String): Result<Boolean> {
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in."))
        if (achievementId.isBlank()) return Result.failure(IllegalArgumentException("achievementId is blank."))
        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)

        return try {
            var newlyUnlockedPrimary = false // Para el logro que se está intentando desbloquear directamente
            var newlyUnlockedProfileComplete = false // Para el logro de perfil completo

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(profileDocRef)
                var currentAchievements = snapshot.get(FIELD_ACHIEVEMENTS) as? Map<String, Timestamp> ?: emptyMap()

                // 1. Desbloquear el logro primario solicitado (si no está ya)
                if (!currentAchievements.containsKey(achievementId)) {
                    currentAchievements = (currentAchievements + (achievementId to FieldValue.serverTimestamp())) as Map<String, Timestamp>
                    newlyUnlockedPrimary = true
                    Log.i("UserProfileRepo", "Logro '$achievementId' marcado para desbloqueo (primario).")
                }

                // 2. Comprobar si se debe desbloquear ACHIEVEMENT_PROFILE_COMPLETE_BASIC
                // Solo si el logro primario que se acaba de desbloquear es uno de los componentes,
                // O si ya estaban todos y este es otro logro diferente.
                // O más simple: siempre comprobar después de cualquier desbloqueo.
                // Para ser más eficientes, solo comprobamos si el achievementId es uno de los componentes
                // Y si este nuevo desbloqueo (newlyUnlockedPrimary) realmente ocurrió.

                val isFirstAdd = currentAchievements.containsKey(ACHIEVEMENT_FIRST_ADD_MYLIST)
                val isFirstRating = currentAchievements.containsKey(ACHIEVEMENT_FIRST_RATING)
                val isFirstReview = currentAchievements.containsKey(ACHIEVEMENT_FIRST_REVIEW)

                if (isFirstAdd && isFirstRating && isFirstReview &&
                    !currentAchievements.containsKey(ACHIEVEMENT_PROFILE_COMPLETE_BASIC)) {
                    // Todos los componentes están, y el de perfil completo no está aún
                    currentAchievements = (currentAchievements + (ACHIEVEMENT_PROFILE_COMPLETE_BASIC to FieldValue.serverTimestamp())) as Map<String, Timestamp>
                    newlyUnlockedProfileComplete = true // Marcamos que este también se desbloqueó ahora
                    Log.i("UserProfileRepo", "Logro '$ACHIEVEMENT_PROFILE_COMPLETE_BASIC' marcado para desbloqueo (perfil completo).")
                }

                // Aplicar todos los cambios a Firestore si hubo alguno
                if (newlyUnlockedPrimary || newlyUnlockedProfileComplete) {
                    transaction.set(profileDocRef, mapOf(FIELD_ACHIEVEMENTS to currentAchievements), SetOptions.merge())
                }

            }.await()
            // El Result<Boolean> de esta función se refiere al 'achievementId' original.
            Result.success(newlyUnlockedPrimary)
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Error en unlockAchievement para '$achievementId'", e)
            Result.failure(e)
        }
    }

    override fun getUnlockedAchievementsFlow(): Flow<Set<String>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) { trySend(emptySet()); close(IllegalStateException("User not logged in.")); return@callbackFlow }
        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)
        val listener = profileDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) { Log.e("UserProfileRepo", "Listener Error", error); trySend(emptySet()); return@addSnapshotListener }
            val achievementsMap = snapshot?.get(FIELD_ACHIEVEMENTS) as? Map<String, Timestamp> ?: emptyMap()
            trySend(achievementsMap.keys)
        }
        awaitClose { listener.remove() }
    }
    override suspend fun updateTriviaHighScore(categoryId: String, score: Int): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("Usuario no autenticado."))
        if (categoryId.isBlank()) return Result.failure(IllegalArgumentException("ID de categoría vacío."))
        if (score < 0) return Result.failure(IllegalArgumentException("Puntuación no puede ser negativa."))

        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)
        val scoreLong = score.toLong() // Convertir a Long para Firestore

        Log.d("UserProfileRepo", "[Update HighScore Attempt] User: $userId, Cat: $categoryId, Score: $scoreLong")

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(profileDocRef)
                val currentHighScores = snapshot.get(FIELD_TRIVIA_HIGH_SCORES) as? Map<String, Long> ?: emptyMap()
                val currentHighScore = currentHighScores[categoryId] ?: 0L // Puntuación actual para esta categoría (o 0 si no existe)

                // Solo actualizar si la nueva puntuación es MAYOR
                if (scoreLong > currentHighScore) {
                    Log.d("UserProfileRepo", "Nuevo récord para '$categoryId': $scoreLong (anterior: $currentHighScore). Actualizando...")
                    // Creamos el mapa anidado para la actualización con merge
                    val newHighScoreData = mapOf(
                        FIELD_TRIVIA_HIGH_SCORES to mapOf(categoryId to scoreLong)
                    )
                    transaction.set(profileDocRef, newHighScoreData, SetOptions.merge())
                } else {
                    Log.d("UserProfileRepo", "Puntuación $scoreLong no supera el récord actual $currentHighScore para '$categoryId'. No se actualiza.")
                }
                null // Éxito de la transacción
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "[Update HighScore Error] User: $userId, Cat: $categoryId", e)
            Result.failure(e)
        }
    }


    override fun getTriviaHighScoresFlow(): Flow<Map<String, Long>> = callbackFlow {
        Log.d("UserProfileRepo", "[HighScores Flow Start]")
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w("UserProfileRepo", "[HighScores Flow] Usuario no logueado.")
            trySend(emptyMap()); close(IllegalStateException("User not logged in.")); return@callbackFlow
        }

        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)
        Log.d("UserProfileRepo", "[HighScores Listener Setup] Path: ${profileDocRef.path}")

        val listenerRegistration = profileDocRef.addSnapshotListener { snapshot, error ->
            Log.d("UserProfileRepo", "[HighScores Listener Triggered] Error? ${error != null}")
            if (error != null) {
                Log.e("UserProfileRepo", "[HighScores Listener Error]", error)
                trySend(emptyMap()); return@addSnapshotListener // Envía mapa vacío en error
            }

            val highScoresMap = snapshot?.get(FIELD_TRIVIA_HIGH_SCORES) as? Map<String, Long> ?: emptyMap()
            Log.d("UserProfileRepo", "[HighScores Listener Send OK] Enviando ${highScoresMap.size} puntuaciones.")
            trySend(highScoresMap) // Envía el mapa completo
        }
        awaitClose { Log.d("UserProfileRepo", "[HighScores Flow Close]"); listenerRegistration.remove() }
    }
    override suspend fun incrementFilteredSearchCountAndCheckAchievement(): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in."))
        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(profileDocRef)
                val currentCount = snapshot.getLong("filteredSearchCount") ?: 0L
                val newCount = currentCount + 1
                transaction.update(profileDocRef, "filteredSearchCount", newCount)

                // Comprobar si se alcanza el umbral para el logro
                if (newCount >= 10) {
                    // Obtener logros actuales para no intentar desbloquear si ya está
                    val currentAchievements = snapshot.get(FIELD_ACHIEVEMENTS) as? Map<String, Timestamp> ?: emptyMap()
                    if (!currentAchievements.containsKey(com.example.appescapetocinema.util.ACHIEVEMENT_CINEPHILE_EXPLORER)) {
                        // Logro no desbloqueado, añadirlo
                        val achievementUpdate = mapOf(
                            FIELD_ACHIEVEMENTS to (currentAchievements + (com.example.appescapetocinema.util.ACHIEVEMENT_CINEPHILE_EXPLORER to FieldValue.serverTimestamp()))
                        )
                        // Usar set con merge para añadir el logro sin sobrescribir otros campos de achievements
                        transaction.set(profileDocRef, achievementUpdate, SetOptions.merge())
                        Log.i("UserProfileRepo", "Logro CINEPHILE_EXPLORER desbloqueado por contador.")
                    }
                }
                null // Resultado de la transacción
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Error incrementFilteredSearchCount", e)
            Result.failure(e)
        }
    }
    override suspend fun incrementReviewCountAndCheckAchievements(): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in."))
        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(profileDocRef)
                val currentReviewCount = snapshot.getLong("reviewCount") ?: 0L
                val newReviewCount = currentReviewCount + 1

                Log.d("UserProfileRepo", "Incrementando reviewCount para $userId a: $newReviewCount")
                transaction.update(profileDocRef, "reviewCount", newReviewCount)

                // Obtener logros actuales para no intentar desbloquear si ya están
                val currentAchievements = snapshot.get(FIELD_ACHIEVEMENTS) as? Map<String, Timestamp> ?: emptyMap()
                var achievementsToUpdate = currentAchievements // Empezar con los actuales

                // Comprobar logro para 5 reseñas
                if (newReviewCount >= 5 && !currentAchievements.containsKey(ACHIEVEMENT_REVIEW_AUTHOR_5)) {
                    achievementsToUpdate = (achievementsToUpdate + (ACHIEVEMENT_REVIEW_AUTHOR_5 to FieldValue.serverTimestamp())) as Map<String, Timestamp>
                    Log.i("UserProfileRepo", "Logro '$ACHIEVEMENT_REVIEW_AUTHOR_5' marcado para desbloqueo.")
                }

                // Comprobar logro para 15 reseñas
                if (newReviewCount >= 15 && !currentAchievements.containsKey(ACHIEVEMENT_REVIEW_PRO_15)) {
                    achievementsToUpdate = (achievementsToUpdate + (ACHIEVEMENT_REVIEW_PRO_15 to FieldValue.serverTimestamp())) as Map<String, Timestamp>
                    Log.i("UserProfileRepo", "Logro '$ACHIEVEMENT_REVIEW_PRO_15' marcado para desbloqueo.")
                }

                // Si hubo algún cambio en los logros a actualizar, aplicarlo
                if (achievementsToUpdate.size > currentAchievements.size) {
                    transaction.set(profileDocRef, mapOf(FIELD_ACHIEVEMENTS to achievementsToUpdate), SetOptions.merge())
                }
                null // La transacción fue exitosa
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Error en incrementReviewCountAndCheckAchievements", e)
            Result.failure(e)
        }
    }
    override suspend fun addSearchedDecadeAndCheckAchievement(year: Int): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in."))
        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)

        // Calcular el inicio de la década (ej. 1987 -> 1980, 2003 -> 2000)
        val decadeStartYear = (year / 10) * 10

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(profileDocRef)
                val currentSearchedDecades = snapshot.get("searchedDecades") as? List<Long> ?: emptyList()

                var decadesUpdated = false
                val newDecadesList: List<Long>
                if (!currentSearchedDecades.contains(decadeStartYear.toLong())) {
                    newDecadesList = currentSearchedDecades + decadeStartYear.toLong()
                    transaction.update(profileDocRef, "searchedDecades", newDecadesList)
                    decadesUpdated = true
                    Log.d("UserProfileRepo", "Añadida década $decadeStartYear a searchedDecades para $userId. Nueva lista: $newDecadesList")
                } else {
                    newDecadesList = currentSearchedDecades // No hubo cambios en la lista de décadas
                    Log.d("UserProfileRepo", "La década $decadeStartYear ya estaba en searchedDecades para $userId.")
                }

                // Si se actualizó la lista O aunque no se haya actualizado, si ya cumple la condición
                // (esto cubre el caso donde se añade la 5ta década o si ya tenía 5 y se busca una repetida)
                if (newDecadesList.size >= 5) {
                    val currentAchievements = snapshot.get(FIELD_ACHIEVEMENTS) as? Map<String, Timestamp> ?: emptyMap()
                    if (!currentAchievements.containsKey(ACHIEVEMENT_DECADE_TRAVELER_5)) {
                        val achievementUpdate = mapOf(
                            FIELD_ACHIEVEMENTS to (currentAchievements + (ACHIEVEMENT_DECADE_TRAVELER_5 to FieldValue.serverTimestamp()))
                        )
                        transaction.set(profileDocRef, achievementUpdate, SetOptions.merge())
                        Log.i("UserProfileRepo", "Logro '$ACHIEVEMENT_DECADE_TRAVELER_5' desbloqueado en transacción.")
                    }
                }
                null // Transacción exitosa
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Error en addSearchedDecadeAndCheckAchievement", e)
            Result.failure(e)
        }
    }
    override suspend fun addScoreToTotalTriviaScoreAndCheckAchievement(scoreEarnedInRound: Int): Result<Unit> {
        if (scoreEarnedInRound < 0) return Result.failure(IllegalArgumentException("Score earned cannot be negative."))
        if (scoreEarnedInRound == 0) return Result.success(Unit) // No hacer nada si no se ganó puntaje

        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in."))
        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)
        val pointsToUnlock = 100L  // Umbral para el logro

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(profileDocRef)
                val currentTotalTriviaScore = snapshot.getLong("totalTriviaScore") ?: 0L
                val newTotalTriviaScore = currentTotalTriviaScore + scoreEarnedInRound.toLong()

                Log.d("UserProfileRepo", "Añadiendo $scoreEarnedInRound a totalTriviaScore para $userId. Nuevo total: $newTotalTriviaScore")
                transaction.update(profileDocRef, "totalTriviaScore", newTotalTriviaScore)

                // Comprobar si se alcanza el umbral para el logro
                if (newTotalTriviaScore >= pointsToUnlock) {
                    val currentAchievements = snapshot.get(FIELD_ACHIEVEMENTS) as? Map<String, Timestamp> ?: emptyMap()
                    if (!currentAchievements.containsKey(ACHIEVEMENT_TRIVIA_TOTAL_SCORE_100)) {
                        val achievementUpdate = mapOf(
                            FIELD_ACHIEVEMENTS to (currentAchievements + (ACHIEVEMENT_TRIVIA_TOTAL_SCORE_100 to FieldValue.serverTimestamp()))
                        )
                        transaction.set(profileDocRef, achievementUpdate, SetOptions.merge())
                        Log.i("UserProfileRepo", "Logro '$ACHIEVEMENT_TRIVIA_TOTAL_SCORE_100' desbloqueado en transacción.")
                    }
                }
                null // Transacción exitosa
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Error en addScoreToTotalTriviaScoreAndCheckAchievement", e)
            Result.failure(e)
        }
    }

    override suspend fun addCarpenterMovieToProfile(movieId: Int): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in."))
        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)
        val movieIdLong = movieId.toLong()

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(profileDocRef)
                val currentCarpenterMovies = snapshot.get("carpenterMovieIdsInMyList") as? List<Long> ?: emptyList()
                var achievements = snapshot.get(FIELD_ACHIEVEMENTS) as? Map<String, Timestamp> ?: emptyMap()

                if (!currentCarpenterMovies.contains(movieIdLong)) {
                    val newCarpenterMovies = currentCarpenterMovies + movieIdLong
                    transaction.update(profileDocRef, "carpenterMovieIdsInMyList", newCarpenterMovies)
                    Log.d("UserProfileRepo", "Añadido Carpenter Movie ID $movieIdLong. Nuevo total: ${newCarpenterMovies.size}")

                    if (newCarpenterMovies.size >= 3 && !achievements.containsKey(ACHIEVEMENT_EASTER_EGG_CARPENTER)) {
                        achievements = (achievements + (ACHIEVEMENT_EASTER_EGG_CARPENTER to FieldValue.serverTimestamp())) as Map<String, Timestamp>
                        transaction.set(profileDocRef, mapOf(FIELD_ACHIEVEMENTS to achievements), SetOptions.merge())
                        Log.i("UserProfileRepo", "Logro '$ACHIEVEMENT_EASTER_EGG_CARPENTER' desbloqueado.")
                    }
                }
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Error en addCarpenterMovieToProfile", e)
            Result.failure(e)
        }
    }

    override suspend fun removeCarpenterMovieFromProfile(movieId: Int): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in."))
        val profileDocRef = firestore.collection(COLLECTION_USER_PROFILES).document(userId)
        val movieIdLong = movieId.toLong()

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(profileDocRef)
                val currentCarpenterMovies = snapshot.get("carpenterMovieIdsInMyList") as? List<Long> ?: emptyList()

                if (currentCarpenterMovies.contains(movieIdLong)) {
                    val newCarpenterMovies = currentCarpenterMovies - movieIdLong
                    transaction.update(profileDocRef, "carpenterMovieIdsInMyList", newCarpenterMovies)
                    Log.d("UserProfileRepo", "Quitado Carpenter Movie ID $movieIdLong. Nuevo total: ${newCarpenterMovies.size}")
                }
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Error en removeCarpenterMovieFromProfile", e)
            Result.failure(e)
        }
    }
}