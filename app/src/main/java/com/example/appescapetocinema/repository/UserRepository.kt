package com.example.appescapetocinema.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

data class UserListData(
    val myList: List<Long> = emptyList()
)

interface UserRepository {
    fun isMovieInMyListFlow(movieId: Int): Flow<Boolean>
    suspend fun addMovieToMyList(movieId: Int): Result<Unit>
    suspend fun removeMovieFromMyList(movieId: Int): Result<Unit>
    suspend fun getMyListMovieIds(): Result<List<Int>>
    suspend fun rateMovie(movieId: Int, rating: Double): Result<Unit>
    suspend fun getMyListSize(): Result<Int>
    suspend fun clearMyList(): Result<Unit>
    fun getUserRatingForMovieFlow(movieId: Int): Flow<Double?>
}

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : UserRepository {

    private companion object {
        const val COLLECTION_USER_LISTS = "user_lists"
        const val FIELD_MY_LIST = "myList"
        const val COLLECTION_USER_RATINGS = "user_ratings"
        const val FIELD_RATINGS_MAP = "ratings" // Nombre del campo mapa dentro del documento
    }

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    }

    override fun isMovieInMyListFlow(movieId: Int): Flow<Boolean> = callbackFlow {
        Log.d("UserRepository", "[Flow Start] Iniciando escucha para Movie ID: $movieId") // Log inicio Flow
        val userId = try { getCurrentUserId() } catch (e: Exception) {
            Log.e("UserRepository", "[Flow Error] No se pudo obtener User ID.", e)
            close(e); return@callbackFlow
        }

        val docRef = firestore.collection(COLLECTION_USER_LISTS).document(userId)
        Log.d("UserRepository", "[Listener Setup] Registrando listener en ${docRef.path} para Movie ID: $movieId")

        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            // --- Log para ver si el Listener se dispara ---
            Log.d("UserRepository", "[Listener Triggered] para Movie ID: $movieId. ¿Error? ${error != null}")

            if (error != null) {
                Log.e("UserRepository", "[Listener Error] Error en SnapshotListener:", error)
                trySend(false) // Envía false si hay error de escucha
                // Considera cerrar el flow si el error es persistente: close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val data = try {
                    snapshot.toObject(UserListData::class.java) // Intentar convertir
                } catch (e: Exception) {
                    Log.e("UserRepository", "[Listener Error] Fallo al convertir snapshot a UserListData:", e)
                    null // Poner null si la conversión falla
                }
                val currentList = data?.myList ?: emptyList() // Lista de Longs

                // --- Log para ver los datos y la comprobación ---
                Log.d("UserRepository", "[Listener Data] Documento existe. Lista actual (Longs): $currentList")
                val movieIdAsLong = movieId.toLong() // Convertir a Long para comparar
                val isInList = currentList.contains(movieIdAsLong)
                Log.d("UserRepository", "[Listener Check] ¿Contiene ${movieIdAsLong}L? $isInList. Enviando valor...")

                val sendResult = trySend(isInList) // Enviar resultado
                if (!sendResult.isSuccess) {
                    Log.w("UserRepository", "[Listener Send Error] No se pudo enviar el valor $isInList al Flow. ¿Está cerrado?")
                } else {
                    Log.d("UserRepository", "[Listener Send OK] Valor $isInList enviado al Flow.")
                }
            } else {
                Log.d("UserRepository", "[Listener Data] Snapshot es null o no existe. Enviando 'false'.")
                trySend(false) // Si el documento no existe, la película no está en la lista
            }
        }

        // --- Log cuando el Flow se cierra ---
        awaitClose {
            Log.d("UserRepository", "[Flow Close] Eliminando listener para Movie ID: $movieId en ${docRef.path}")
            listenerRegistration.remove()
        }
    } //

    override suspend fun addMovieToMyList(movieId: Int): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            val docRef = firestore.collection(COLLECTION_USER_LISTS).document(userId)
            Log.d("UserRepository", "[Add Attempt] User: $userId, MovieID: $movieId") // Log intento
            docRef.set(
                mapOf(FIELD_MY_LIST to FieldValue.arrayUnion(movieId.toLong())), // Usa Long
                SetOptions.merge()
            ).await()
            Log.d("UserRepository", "[Add Success] Movie $movieId added for user $userId") // Log éxito
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "[Add Error] Error adding $movieId", e) // Log error
            Result.failure(e)
        }
    }

    override suspend fun removeMovieFromMyList(movieId: Int): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            val docRef = firestore.collection(COLLECTION_USER_LISTS).document(userId)
            Log.d("UserRepository", "[Remove Attempt] User: $userId, MovieID: $movieId") // Log intento
            docRef.update(FIELD_MY_LIST, FieldValue.arrayRemove(movieId.toLong())).await() // Usa Long
            Log.d("UserRepository", "[Remove Success] Movie $movieId removed for user $userId") // Log éxito
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "[Remove Error] Error removing $movieId", e)
            Result.failure(e)
        }
    }

    override suspend fun getMyListMovieIds(): Result<List<Int>> {
        return try {
            val userId = getCurrentUserId()
            val docRef = firestore.collection(COLLECTION_USER_LISTS).document(userId)
            val snapshot = docRef.get().await()
            val data = snapshot.toObject(UserListData::class.java)
            // Convierte Long a Int
            val idList = data?.myList?.mapNotNull { it?.toInt() } ?: emptyList()
            Log.d("UserRepository", "[Get IDs Success] IDs obtenidos: $idList")
            Result.success(idList)
        } catch (e: Exception) {
            Log.e("UserRepository", "[Get IDs Error]", e)
            Result.failure(e)
        }
    }
    override suspend fun rateMovie(movieId: Int, rating: Double): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            val docRef = firestore.collection(COLLECTION_USER_RATINGS).document(userId)
            Log.d("UserRepository", "[Rate Attempt] User: $userId, MovieID: $movieId, Rating: $rating")
            val ratingUpdate = mapOf(FIELD_RATINGS_MAP to mapOf(movieId.toString() to rating))
            docRef.set(ratingUpdate, SetOptions.merge()).await()
            Log.d("UserRepository", "[Rate Success] Movie $movieId rated $rating for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "[Rate Error] Error rating movie $movieId", e)
            Result.failure(e)
        }
    }
    override suspend fun getMyListSize(): Result<Int> {
        return try {
            val userId = getCurrentUserId()
            val docRef = firestore.collection(COLLECTION_USER_LISTS).document(userId)
            val snapshot = docRef.get().await()
            val listData = snapshot.toObject(UserListData::class.java) // Conversión correcta
            Result.success(listData?.myList?.size ?: 0) // Manejo de null con tamaño predeterminado 0
        } catch (e: Exception) {
            Log.e("UserRepository", "[Get List Size Error]", e)
            Result.failure(e)
        }
    }

    override fun getUserRatingForMovieFlow(movieId: Int): Flow<Double?> = callbackFlow {
        Log.d("UserRepository", "[Rating Flow Start] Movie ID: $movieId")
        val userId = try { getCurrentUserId() } catch (e: Exception) { close(e); return@callbackFlow }
        val docRef = firestore.collection(COLLECTION_USER_RATINGS).document(userId)
        Log.d("UserRepository", "[Rating Listener Setup] Path: ${docRef.path} for Movie ID: $movieId")

        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            Log.d("UserRepository", "[Rating Listener Triggered] Movie ID: $movieId. Error? ${error != null}")
            if (error != null) {
                Log.e("UserRepository", "[Rating Listener Error]", error); trySend(null); return@addSnapshotListener
            }
            var userRating: Double? = null
            if (snapshot != null && snapshot.exists()) {
                try {
                    val ratingsMap = snapshot.data?.get(FIELD_RATINGS_MAP) as? Map<*, *>
                    if (ratingsMap != null) {
                        val ratingValue = ratingsMap[movieId.toString()]
                        userRating = when (ratingValue) {
                            is Number -> ratingValue.toDouble()
                            else -> null
                        }
                        Log.d("UserRepository", "[Rating Listener Data] Map: ${ratingsMap.keys}. Rating for $movieId: $userRating")
                    } else { Log.d("UserRepository", "[Rating Listener Data] Field '$FIELD_RATINGS_MAP' null or not map.") }
                } catch (e: Exception) { Log.e("UserRepository", "[Rating Listener Error] Processing snapshot:", e); userRating = null }
            } else { Log.d("UserRepository", "[Rating Listener Data] Snapshot null or doesn't exist."); userRating = null }

            Log.d("UserRepository", "[Rating Listener Check] Sending rating: $userRating")
            trySend(userRating)
        }
        awaitClose { Log.d("UserRepository", "[Rating Flow Close] Movie ID: $movieId"); listenerRegistration.remove() }
    }
    override suspend fun clearMyList(): Result<Unit> {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("UserRepository", "clearMyList: Usuario no logueado.")
            return Result.failure(IllegalStateException("Usuario no autenticado."))
        }
        val userListDocRef = firestore.collection("user_lists").document(userId)
        Log.d("UserRepository", "clearMyList: Intentando borrar Mi Lista para usuario $userId")

        return try {
            // Actualizar el campo 'myList' a un array vacío
            userListDocRef.update("myList", emptyList<Long>()).await()
            Log.i("UserRepository", "clearMyList: Mi Lista borrada exitosamente para $userId.")
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e("UserRepository", "clearMyList: Error de Firestore", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("UserRepository", "clearMyList: Excepción general", e)
            Result.failure(e)
        }
    }


}