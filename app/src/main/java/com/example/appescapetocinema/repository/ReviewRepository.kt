package com.example.appescapetocinema.repository

import com.example.appescapetocinema.model.Review // Modelo UI
import com.example.appescapetocinema.model.ReviewFirestoreData // Modelo Firestore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.util.Date

// --- Interfaz ---
interface ReviewRepository {
    /**
     * Envía o actualiza una reseña.
     */
    suspend fun submitReview(movieId: Int, userId: String, userName: String, rating: Double, text: String): Result<Unit>

    /**
     * Obtiene Flow de reseñas para una película, ordenadas por fecha.
     */
    fun getReviewsForMovieFlow(movieId: Int): Flow<List<Review>>

    /**
     * Obtiene la reseña de un usuario específico para una película.
     */
    suspend fun getUserReviewForMovie(movieId: Int, userId: String): Result<Review?>

    /**
     * Elimina la reseña de un usuario.
     */
    suspend fun deleteReview(movieId: Int, userId: String): Result<Unit>

    suspend fun getUserReviewCount(userId: String): Result<Int>

}


// --- Implementación ---
class ReviewRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // Para validación
) : ReviewRepository {

    private companion object {
        const val COLLECTION_MOVIE_REVIEWS = "movie_reviews"
        const val SUBCOLLECTION_REVIEWS = "reviews"
        const val FIELD_TIMESTAMP = "timestamp"
    }

    // Helper para validar usuario actual vs ID proporcionado
    private fun checkCurrentUser(userId: String): Boolean {
        val currentUid = auth.currentUser?.uid
        val matches = currentUid != null && currentUid == userId
        if (!matches) { Log.e("ReviewRepository", "Security Check Failed: User $userId != current $currentUid") }
        return matches
    }

    override suspend fun submitReview(
        movieId: Int,
        userId: String,
        userName: String,
        rating: Double,
        text: String
    ): Result<Unit> {
        // Validaciones
        if (text.isBlank()) return Result.failure(IllegalArgumentException("La reseña no puede estar vacía."))
        if (!checkCurrentUser(userId)) return Result.failure(SecurityException("Intento de escribir reseña para otro usuario."))
        if (rating <= 0.0 || rating > 10.0) return Result.failure(IllegalArgumentException("Valoración inválida para reseña.")) // Requiere valoración

        return try {
            val reviewDocRef = firestore
                .collection(COLLECTION_MOVIE_REVIEWS).document(movieId.toString())
                .collection(SUBCOLLECTION_REVIEWS).document(userId)

            val reviewData = ReviewFirestoreData(
                userId = userId,
                userName = userName.ifBlank { "Usuario" },
                rating = rating,
                text = text,
                timestamp = null // Para que @ServerTimestamp funcione
            )

            Log.d("ReviewRepository", "[Submit Review Attempt] Movie: $movieId, User: $userId")
            reviewDocRef.set(reviewData).await() // set crea o sobrescribe

            Log.d("ReviewRepository", "[Submit Review Success] Movie: $movieId, User: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ReviewRepository", "[Submit Review Error] Movie: $movieId, User: $userId", e)
            Result.failure(e)
        }
    }

    override fun getReviewsForMovieFlow(movieId: Int): Flow<List<Review>> = callbackFlow {
        Log.d("ReviewRepository", "[Review Flow Start] Movie ID: $movieId")
        val reviewsCollectionRef = firestore
            .collection(COLLECTION_MOVIE_REVIEWS).document(movieId.toString())
            .collection(SUBCOLLECTION_REVIEWS)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING) // Más nuevas primero

        val constructedPath = "${COLLECTION_MOVIE_REVIEWS}/${movieId}/${SUBCOLLECTION_REVIEWS}"
        Log.d("ReviewRepository", "[Review Listener Setup] Path: $constructedPath (Querying with orderBy)")

        val listenerRegistration = reviewsCollectionRef.addSnapshotListener { querySnapshot, error ->
            Log.d("ReviewRepository", "[Review Listener Triggered] Movie ID: $movieId. Error? ${error != null}. Docs: ${querySnapshot?.size()}")
            if (error != null) {
                Log.e("ReviewRepository", "[Review Listener Error]", error)
                // Cierra el flow con el error para que el ViewModel pueda manejarlo
                close(error); return@addSnapshotListener
            }

            if (querySnapshot != null) {
                val reviews = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.toObject<ReviewFirestoreData>()
                        data?.let {
                            Review( // Mapeo a modelo UI
                                reviewId = doc.id,
                                movieId = movieId.toString(),
                                userId = it.userId,
                                userName = it.userName,
                                rating = it.rating,
                                text = it.text,
                                timestamp = it.timestamp?.toDate() // Convierte Timestamp a Date?
                            )
                        }
                    } catch (e: Exception) { Log.e("ReviewRepository", "[Review Listener Error] Mapping doc ${doc.id}", e); null }
                }
                Log.d("ReviewRepository", "[Review Listener Send OK] Sending ${reviews.size} reviews for Movie ID: $movieId")
                trySend(reviews) // Envía la lista actualizada
            } else {
                Log.w("ReviewRepository", "[Review Listener Data] QuerySnapshot es null.")
                trySend(emptyList())
            }
        }
        awaitClose { Log.d("ReviewRepository", "[Review Flow Close] Movie ID: $movieId"); listenerRegistration.remove() }
    }

    override suspend fun getUserReviewForMovie(movieId: Int, userId: String): Result<Review?> {
        return try {
            Log.d("ReviewRepository", "[Get User Review Attempt] Movie: $movieId, User: $userId")
            val docRef = firestore
                .collection(COLLECTION_MOVIE_REVIEWS).document(movieId.toString())
                .collection(SUBCOLLECTION_REVIEWS).document(userId)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val data = snapshot.toObject<ReviewFirestoreData>()
                val review = data?.let { Review( snapshot.id, movieId.toString(), it.userId, it.userName, it.rating, it.text, it.timestamp?.toDate() ) }
                Log.d("ReviewRepository", "[Get User Review Success] Found: ${review != null}")
                Result.success(review)
            } else {
                Log.d("ReviewRepository", "[Get User Review] Not found for User $userId, Movie $movieId.")
                Result.success(null) // Devuelve éxito con resultado null
            }
        } catch (e: Exception) {
            Log.e("ReviewRepository", "[Get User Review Error]", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteReview(movieId: Int, userId: String): Result<Unit> {
        if (!checkCurrentUser(userId)) return Result.failure(SecurityException("User ID mismatch."))
        return try {
            val docRef = firestore
                .collection(COLLECTION_MOVIE_REVIEWS).document(movieId.toString())
                .collection(SUBCOLLECTION_REVIEWS).document(userId)
            Log.d("ReviewRepository", "[Delete Review Attempt] Movie: $movieId, User: $userId")
            docRef.delete().await()
            Log.d("ReviewRepository", "[Delete Review Success] Movie: $movieId, User: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ReviewRepository", "[Delete Review Error]", e)
            Result.failure(e)
        }
    }
    override suspend fun getUserReviewCount(userId: String): Result<Int> {
        // Esta implementación depende MUCHO de tu estructura de Firestore para las reseñas.
        // Si las reseñas están anidadas bajo cada película: movie_reviews/{movieId}/reviews/{userId}
        // Contarlas todas para un usuario es muy ineficiente y costoso.
        // DEBERÍAS tener un contador en user_profiles o una colección separada de reseñas por usuario.

        // Si tienes una colección raíz "reviews" con un campo "userId":
        return try {
            val querySnapshot = firestore.collection("reviews") // O el nombre de tu colección de reseñas
                .whereEqualTo("userId", userId) // Asegúrate que el campo se llame 'userId'
                .get()
                .await()
            Result.success(querySnapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
        // SI NO TIENES ESA ESTRUCTURA, este método no es viable.
        // En ese caso, debes optar por el contador en user_profiles.
    }
}