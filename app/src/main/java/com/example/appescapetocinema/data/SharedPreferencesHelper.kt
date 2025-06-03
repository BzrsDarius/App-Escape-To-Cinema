package com.example.appescapetocinema.data

import android.content.Context
import android.content.SharedPreferences

// Clase que ayuda a gestionar las preferencias compartidas de la aplicación
class SharedPreferencesHelper(context: Context) {

    // Instancia de SharedPreferences para almacenar datos persistentes
    private val prefs: SharedPreferences =
        context.getSharedPreferences("escape_to_cinema_prefs", Context.MODE_PRIVATE)

    // Objeto companion que contiene constantes relacionadas con las preferencias
    companion object {
        // Clave para identificar el país del proveedor de contenido
        private const val KEY_WATCH_PROVIDER_COUNTRY = "watch_provider_country"
        // Valor por defecto para el país del proveedor de contenido (España)
        const val DEFAULT_WATCH_PROVIDER_COUNTRY = "ES"
    }

    // Método para obtener el país del proveedor de contenido desde las preferencias
    fun getWatchProviderCountry(): String {
        return prefs.getString(KEY_WATCH_PROVIDER_COUNTRY, DEFAULT_WATCH_PROVIDER_COUNTRY) ?: DEFAULT_WATCH_PROVIDER_COUNTRY
    }

    // Método para establecer el país del proveedor de contenido en las preferencias
    fun setWatchProviderCountry(countryCode: String) {
        prefs.edit().putString(KEY_WATCH_PROVIDER_COUNTRY, countryCode).apply()
    }
}