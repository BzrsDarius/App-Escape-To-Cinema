package com.example.appescapetocinema.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appescapetocinema.R

// Modelo de datos simple para el ejemplo
data class MovieItem(
    val id: Int,
    val title: String,
    val posterUrl: String?
)

@Composable
fun MovieCard(
    movie: MovieItem,
    onClick: (Int) -> Unit // Pasa el ID de la película al hacer clic
) {
    Card(
        modifier = Modifier
            .width(150.dp) // Ancho fijo para las tarjetas en la fila
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clickable { onClick(movie.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Un color ligeramente diferente al fondo
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movie.posterUrl)
                    .crossfade(true) // Animación suave al cargar
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo) // Imagen si falla la carga
                    .build(),
                contentDescription = movie.title,
                modifier = Modifier
                    .height(200.dp) // Altura fija para la imagen
                    .fillMaxWidth(),
                contentScale = ContentScale.Crop // Escala la imagen para llenar el espacio, recortando si es necesario
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = movie.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2, // Permite hasta dos líneas para el título
                overflow = TextOverflow.Ellipsis, // Pone "..." si el título es muy largo
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .heightIn(min = 40.dp) // Asegura altura mínima para alinear texto
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}