package com.example.appescapetocinema.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest

import com.example.appescapetocinema.R
import com.example.appescapetocinema.ui.components.MovieCard
import com.example.appescapetocinema.ui.theme.AppEscapeToCinemaTheme

@Composable
fun MovieSectionPaginated(
    title: String,
    movies: LazyPagingItems<MovieItem>,
    onMovieClick: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                // --- Usa Tipografía y Color del Tema ---
                style = MaterialTheme.typography.titleLarge, // Orbitron Bold
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }

        LazyRow(
            modifier = Modifier.height(280.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items( count = movies.itemCount, key = movies.itemKey { it.id }, contentType = movies.itemContentType { "movieItem" } ) { index ->
                val movie = movies[index]
                if (movie != null) {
                    MovieCard(movie = movie, onClick = onMovieClick)
                } else {
                    ShimmerMovieCardPlaceholder()
                }
            }
            item { HandlePagingLoadStatesRow(loadState = movies.loadState.append, onRetry = onRetry) }
        }

        val refreshState = movies.loadState.refresh
        if (refreshState is LoadState.Error) {
            val error = refreshState.error
            Log.e("MovieSectionPaginated", "Error en refresh para '$title': $error")
            Row( modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text( "Error al cargar.",
                    // --- Usa Color y Tipografía del Tema ---
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium // Texto más pequeño para error
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onRetry) {
                    // El texto del botón usará automáticamente el color primario y la tipografía labelLarge por defecto
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
fun HandlePagingLoadStatesRow( loadState: LoadState, onRetry: () -> Unit, modifier: Modifier = Modifier ) {
    Box( modifier = modifier.width(100.dp).height(260.dp).padding(vertical = 8.dp), contentAlignment = Alignment.Center ) {
        when (loadState) {
            is LoadState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary // NeonCyan
                )
            }
            is LoadState.Error -> {
                Log.e("HandlePagingStates", "Error en loadState.append/prepend: ${loadState.error}")
                TextButton(onClick = onRetry) {
                    Text("Reintentar")
                }
            }
            is LoadState.NotLoading -> {}
        }
    }
}