package com.example.appescapetocinema.ui.news

import android.content.Intent
import android.net.Uri // Para abrir URLs
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Para LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // Para Intent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage // Para imágenes de noticias
import coil.request.ImageRequest
import com.example.appescapetocinema.R
import com.example.appescapetocinema.model.NewsArticle // Importa modelo de noticia
import com.example.appescapetocinema.repository.NewsRepositoryImpl // Para Factory y preview
import com.example.appescapetocinema.ui.components.ShimmerNewsArticleCardPlaceholder
import java.text.SimpleDateFormat // Para formatear fechas
import java.util.* // Para Date y Locale

// --- NewsScreen (UI Desacoplada) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    uiState: NewsUiState, // Recibe el estado
    onArticleClick: (String) -> Unit, // Lambda para clic en artículo (pasa URL)
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Noticias de Cine", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // --- Carga Inicial ---
                uiState.isLoading -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(6) { // Muestra, por ejemplo, 6 placeholders
                            ShimmerNewsArticleCardPlaceholder()
                            Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                    }
                }
                // --- Error ---
                uiState.errorMessage != null -> {
                    Column( modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center ) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text( uiState.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRetry) { Text("Reintentar") }
                    }
                }
                // --- Lista Vacía (después de carga exitosa) ---
                uiState.articles.isEmpty() && !uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text( "No hay noticias disponibles.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge )
                    }
                }
                // --- Mostrar Lista de Noticias ---
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items( items = uiState.articles, key = { article -> article.id } ) { article ->
                            NewsArticleCard( article = article, onClick = { onArticleClick(article.url) } )
                            Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                    }
                }
            } // Fin when
        } // Fin Box
    } // Fin Scaffold
}

// --- Composable para Tarjeta de Noticia ---
@Composable
fun NewsArticleCard(
    article: NewsArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Formateador de fecha simple (puedes hacerlo más sofisticado)
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Hace toda la fila clickable
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna para Texto
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.source,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium, // Título de noticia
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2, // Limita a 2 líneas
                overflow = TextOverflow.Ellipsis
            )
            article.description?.let { // Muestra descripción si existe
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall, // Descripción pequeña
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, // Limita a 2 líneas
                    overflow = TextOverflow.Ellipsis
                )
            }
            article.publishedDate?.let { // Muestra fecha si existe
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(it),
                    style = MaterialTheme.typography.labelSmall, // Fecha muy pequeña
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Imagen (si existe)
        article.imageUrl?.let { imageUrl ->
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.logo) // Reusa placeholder
                    .error(R.drawable.logo)
                    .build(),
                contentDescription = article.title,
                modifier = Modifier
                    .size(80.dp) // Tamaño fijo para miniatura
                    .clip(RoundedCornerShape(8.dp)), // Bordes redondeados
                contentScale = ContentScale.Crop // Recorta para ajustar
            )
        }
    }
}


// --- NewsScreenContainer ---
@Composable
fun NewsScreenContainer(
    navController: NavController,
    viewModel: NewsViewModel = viewModel(factory = NewsViewModelFactory(NewsRepositoryImpl()))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current // Para abrir URL

    NewsScreen(
        uiState = uiState,
        onArticleClick = { url ->
            // Abrir la URL en un navegador externo
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                // Manejar error si no se puede abrir la URL (ej. no hay navegador)
                Log.e("NewsScreenContainer", "Error al abrir URL: $url", e)
            }
        },
        onRetry = viewModel::refreshNews,
        onNavigateBack = { navController.popBackStack() }
    )
}

