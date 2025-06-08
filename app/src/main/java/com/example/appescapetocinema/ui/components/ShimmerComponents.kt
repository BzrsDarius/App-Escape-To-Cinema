package com.example.appescapetocinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer

@Composable
fun ShimmerMovieCardPlaceholder(modifier: Modifier = Modifier) {
    val shimmerBaseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // Base grisácea
    val shimmerHighlightColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) // Reflejo más claro

    Column(
        modifier = modifier
            .width(150.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .shimmer(), // <-- Aplica efecto shimmer al Column completo
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Placeholder para la Imagen
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .background(shimmerBaseColor, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) // Forma y color base
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Placeholder para la primera línea de texto
        Box(
            modifier = Modifier
                .height(18.dp) // Altura aproximada del texto
                .fillMaxWidth(0.8f) // Ancho menor que el total
                .background(shimmerBaseColor, shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Placeholder para la segunda línea de texto (más corta)
        Box(
            modifier = Modifier
                .height(18.dp)
                .fillMaxWidth(0.6f) // Más corto
                .background(shimmerBaseColor, shape = RoundedCornerShape(4.dp))
                .padding(bottom = 8.dp) // Padding inferior para simular espacio final de Card
        )
        // Asegura altura mínima total similar a MovieCard
        Spacer(modifier = Modifier.height(8.dp)) // Espacio inferior que faltaba
    }
}

@Composable
fun ShimmerPersonCardPlaceholder(modifier: Modifier = Modifier) {
    val shimmerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .width(100.dp) // Mismo ancho que PersonCard
            .padding(vertical = 4.dp) // Mismo padding vertical
            .shimmer(), // <-- Aplica Shimmer
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Placeholder Avatar Circular
        Spacer(
            modifier = Modifier
                .size(80.dp) // Mismo tamaño avatar
                .clip(CircleShape) // Misma forma
                .background(shimmerColor)
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Placeholder Nombre (2 líneas posibles)
        Spacer(
            modifier = Modifier.height(14.dp).fillMaxWidth(0.7f).background(shimmerColor, shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Spacer(
            modifier = Modifier.height(14.dp).fillMaxWidth(0.5f).background(shimmerColor, shape = RoundedCornerShape(4.dp))
        )
        // Placeholder Subtítulo (1 línea)
        Spacer(modifier = Modifier.height(6.dp))
        Spacer(
            modifier = Modifier.height(12.dp).fillMaxWidth(0.6f).background(shimmerColor, shape = RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun ShimmerNewsArticleCardPlaceholder(modifier: Modifier = Modifier) {
    val shimmerBaseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp) // Mismo padding que NewsArticleCard
            .shimmer(), // Aplicar shimmer a toda la fila
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna para Texto Placeholder
        Column(modifier = Modifier.weight(1f)) {
            // Placeholder para Fuente
            Spacer(
                modifier = Modifier
                    .height(14.dp) // Altura aprox. labelMedium
                    .fillMaxWidth(0.3f) // Ancho corto
                    .background(shimmerBaseColor, shape = RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Placeholder para Título (2 líneas)
            Spacer(
                modifier = Modifier
                    .height(18.dp) // Altura aprox. titleMedium
                    .fillMaxWidth(0.9f)
                    .background(shimmerBaseColor, shape = RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Spacer(
                modifier = Modifier
                    .height(18.dp)
                    .fillMaxWidth(0.7f)
                    .background(shimmerBaseColor, shape = RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Placeholder para Fecha
            Spacer(
                modifier = Modifier
                    .height(12.dp) // Altura aprox. labelSmall
                    .fillMaxWidth(0.4f)
                    .background(shimmerBaseColor, shape = RoundedCornerShape(4.dp))
            )
        }

        // Placeholder para Imagen
        Spacer(modifier = Modifier.width(12.dp)) // Espacio
        Spacer(
            modifier = Modifier
                .size(80.dp) // Mismo tamaño que AsyncImage en NewsArticleCard
                .clip(RoundedCornerShape(8.dp)) // Misma forma
                .background(shimmerBaseColor)
        )
    }
}

