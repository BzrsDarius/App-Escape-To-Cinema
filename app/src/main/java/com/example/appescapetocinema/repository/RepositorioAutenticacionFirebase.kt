package com.example.appescapetocinema.repository

import android.content.ContentValues
import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RepositorioAutenticacionFirebase {
    private val autenticacionFirebase: FirebaseAuth = FirebaseAuth.getInstance()

    fun iniciarSesion(correo: String, contrasena: String, enResultado: (Boolean, String?) -> Unit) {
        if (correo.isBlank() || contrasena.isBlank()){
            enResultado(false, "Correo y contraseña no pueden estar vacíos")
            return
        }
        autenticacionFirebase.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) enResultado(true, null)
                else enResultado(false, tarea.exception?.localizedMessage ?: "Error desconocido")
            }
    }

    fun registrar(correo: String, contrasena: String, enResultado: (Boolean, String?) -> Unit) {
        if (correo.isBlank() || contrasena.isBlank()){
            enResultado(false, "Correo y contraseña no pueden estar vacíos")
            return
        }
        autenticacionFirebase.createUserWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    Log.d("AuthRepoSimple", "Registro Auth exitoso para $correo")
                    enResultado(true, null)
                } else {
                    Log.w("AuthRepoSimple", "Error registro Auth para $correo", tarea.exception)
                    enResultado(false, tarea.exception?.localizedMessage ?: "Error desconocido")
                }
            }
    }

    fun cerrarSesion() {
        autenticacionFirebase.signOut()
    }

    fun obtenerUsuarioActual(): FirebaseUser? = autenticacionFirebase.currentUser

    fun enviarCorreoRecuperacion(correo: String, enResultado: (Boolean, String?) -> Unit) {
        if (correo.isBlank()){
            enResultado(false, "El correo no puede estar vacío")
            return
        }
        autenticacionFirebase.sendPasswordResetEmail(correo)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) enResultado(true, null)
                else enResultado(false, tarea.exception?.localizedMessage ?: "Error desconocido")
            }
    }

    fun iniciarSesionConGoogle(idToken: String, enResultado: (Boolean, String?) -> Unit) {
        val credencial = GoogleAuthProvider.getCredential(idToken, null) // Crea la credencial
        autenticacionFirebase.signInWithCredential(credencial) // Inicia sesión con la credencial
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    Log.d("AuthRepoGoogle", "Inicio de sesión con Google exitoso.")
                    enResultado(true, null)
                } else {
                    Log.w("AuthRepoGoogle", "Error al iniciar sesión con Google.", tarea.exception)
                    enResultado(false, tarea.exception?.localizedMessage ?: "Error desconocido con Google")
                }
            }
    }
    fun reauthenticateUser(password: String, enResultado: (Boolean, String?) -> Unit) {
        val user = autenticacionFirebase.currentUser
        if (user == null || user.email.isNullOrBlank()) {
            enResultado(false, "Usuario no logueado o sin email para reautenticar.")
            return
        }
        if (password.isBlank()) {
            enResultado(false, "La contraseña actual no puede estar vacía.")
            return
        }

        // Verificar que el usuario se autenticó con Email/Password
        val isEmailPasswordUser = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
        if (!isEmailPasswordUser) {
            enResultado(false, "La reautenticación con contraseña solo es para usuarios de email/contraseña.")
            return
        }

        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(ContentValues.TAG, "reauthenticateUser:success")
                    enResultado(true, null)
                } else {
                    Log.w(ContentValues.TAG, "reauthenticateUser:failure", task.exception)
                    enResultado(false, task.exception?.localizedMessage ?: "Error de reautenticación")
                }
            }
    }

    /**
     * Actualiza la contraseña del usuario actual.
     * Requiere reautenticación reciente.
     */
    fun updatePassword(newPassword: String, enResultado: (Boolean, String?) -> Unit) {
        val user = autenticacionFirebase.currentUser
        if (user == null) {
            enResultado(false, "Usuario no logueado.")
            return
        }
        if (newPassword.isBlank() || newPassword.length < 6) { // Validación básica
            enResultado(false, "La nueva contraseña es inválida o demasiado corta.")
            return
        }

        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(ContentValues.TAG, "updatePassword:success")
                    enResultado(true, null)
                } else {
                    Log.w(ContentValues.TAG, "updatePassword:failure", task.exception)
                    enResultado(false, task.exception?.localizedMessage ?: "Error al actualizar contraseña")
                }
            }
    }
    suspend fun deleteCurrentUserAccountAndData(): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUser = autenticacionFirebase.currentUser
        if (currentUser == null) {
            return@withContext Result.failure(Exception("Usuario no autenticado para eliminar."))
        }
        val uid = currentUser.uid

        try {
            val firestore = FirebaseFirestore.getInstance()
            val batch = firestore.batch()

            val myListRef = firestore.collection("user_lists").document(uid)
            batch.delete(myListRef)

            val ratingsRef = firestore.collection("user_ratings").document(uid)
            batch.delete(ratingsRef)

            val userProfileRef = firestore.collection("user_profiles").document(uid)
            batch.delete(userProfileRef)

            val reviewsQuery = firestore.collectionGroup("reviews")
                .whereEqualTo("userId", uid) // <-- CONFIRMA ESTE NOMBRE DE CAMPO

            val userReviewsSnapshot = reviewsQuery.get().await()
            for (document in userReviewsSnapshot.documents) {
                batch.delete(document.reference)
            }

            batch.commit().await()
            Log.d(ContentValues.TAG, "Datos de Firestore para UID $uid eliminados correctamente.")

            try {
                currentUser.delete().await()
                Log.d(
                    ContentValues.TAG,
                    "Cuenta de Firebase Auth para UID $uid eliminada correctamente."
                )
                Result.success(Unit)
            } catch (authDeleteException: Exception) {
                Log.e(
                    ContentValues.TAG,
                    "Error al eliminar cuenta de Firebase Auth para UID $uid después de borrar datos de Firestore.",
                    authDeleteException
                )
                Result.failure(
                    Exception(
                        "Los datos se eliminaron pero ocurrió un error al eliminar la cuenta de autenticación. Por favor, contacta a soporte. Error: ${authDeleteException.message}",
                        authDeleteException
                    )
                )
            }

        } catch (firestoreException: Exception) {
            Log.e(
                ContentValues.TAG,
                "Error al eliminar datos de Firestore para UID $uid.",
                firestoreException
            )
            Result.failure(
                Exception(
                    "Error al eliminar los datos del usuario: ${firestoreException.message}",
                    firestoreException
                )
            )
        } catch (e: Exception) {
            Log.e(
                ContentValues.TAG,
                "Error general durante la eliminación de cuenta para UID $uid.",
                e
            )
            Result.failure(
                Exception(
                    "Error inesperado durante la eliminación de cuenta: ${e.message}",
                    e
                )
            )
        }
    }

    fun getAuthStateFlow(): Flow<FirebaseUser?> {
        return callbackFlow {
            val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                trySend(firebaseAuth.currentUser).isSuccess
            }
            autenticacionFirebase.addAuthStateListener(authStateListener)
            awaitClose { // Se ejecuta cuando el Flow es cancelado
                Log.d("AuthRepo", "Removing AuthStateListener")
                autenticacionFirebase.removeAuthStateListener(authStateListener)
            }
        }.distinctUntilChanged() // Solo emite si el estado del usuario realmente cambia
    }
}