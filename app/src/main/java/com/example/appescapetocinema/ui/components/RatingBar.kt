package com.example.appescapetocinema.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star // Estrella llena
import androidx.compose.material.icons.filled.StarBorder // Borde de estrella
import androidx.compose.material.icons.filled.StarHalf // Media estrella
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme // Para el color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Muestra una barra de estrellas y permite la selección.
 *
 * @param modifier Modificador para el Row contenedor.
 * @param rating Valoración actual (de 0.0 a 5.0).
 * @param stars Número total de estrellas a mostrar (normalmente 5).
 * @param starSize Tamaño de cada icono de estrella.
 * @param starColor Color de las estrellas llenas/parciales.
 * @param isIndicator Si es true, solo muestra el rating, no permite selección.
 * @param onRatingChanged Lambda que se llama cuando el usuario selecciona un nuevo rating.
 */
@Composable
fun RatingBar(
    modifier: Modifier = Modifier,
    rating: Double,
    stars: Int = 5,
    starSize: Dp = 24.dp,
    starColor: Color = MaterialTheme.colorScheme.primary,
    isIndicator: Boolean = false, // Para solo mostrar
    onRatingChanged: ((Double) -> Unit)? = null // Nullable si es indicador
) {
    // Asegura que el rating esté dentro de los límites
    val coercedRating = rating.coerceIn(0.0, stars.toDouble())

    Row(modifier = modifier) {
        for (index in 1..stars) {
            val starValue = index.toDouble()
            Icon(
                imageVector = when {
                    coercedRating >= starValue -> Icons.Filled.Star // Estrella llena
                    coercedRating >= starValue - 0.5 -> Icons.Filled.StarHalf // Media estrella
                    else -> Icons.Filled.StarBorder // Estrella vacía
                },
                contentDescription = null, // Descripción podría ser mejorada para accesibilidad
                tint = starColor,
                modifier = Modifier
                    .size(starSize)
                    .then(
                        // Solo añadir clickable si NO es indicador y onRatingChanged existe
                        if (!isIndicator && onRatingChanged != null) {
                            Modifier.clickable { onRatingChanged(starValue) } // Selecciona estrella completa
                        } else {
                            Modifier // No clickable si es indicador
                        }
                    )
            )
        }
    }
}