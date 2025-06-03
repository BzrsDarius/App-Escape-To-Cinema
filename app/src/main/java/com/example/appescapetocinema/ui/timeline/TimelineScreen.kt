package com.example.appescapetocinema.ui.timeline

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.* // Importa iconos para tipos de evento
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appescapetocinema.R // Si tienes un placeholder drawable
import com.example.appescapetocinema.model.TimelineEvent
import com.example.appescapetocinema.model.TimelineEventType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreenContainer(
    navControllerForBack: NavController,    // Recibe bottomNavController
    navControllerForDetail: NavController,  // Recibe mainNavController
    viewModel: TimelineViewModel = viewModel(factory = TimelineViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Línea de Tiempo del Cine", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant // Ajustado para consistencia
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d("TimelineContainer", "Botón Atrás presionado. Usando navControllerForBack.")
                        navControllerForBack.popBackStack() // Usa el NavController que te trajo a Timeline (bottomNavController)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        TimelineScreen(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onRetry = viewModel::retryLoad,
            onMovieEventClick = { movieId ->
                Log.d("TimelineContainer", "Click en película de Timeline. Navegando a Detail ID: $movieId usando navControllerForDetail.")
                navControllerForDetail.navigate(Screen.Detail.createRoute(movieId)) // Usa el NavController principal para ir a DetailScreen
            }
        )
    }
}

@Composable
fun TimelineScreen(
    modifier: Modifier = Modifier,
    uiState: TimelineUiState,
    onRetry: () -> Unit,
    onMovieEventClick: (tmdbId: Int) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
            }
            uiState.events.isEmpty() -> {
                Text("No hay eventos en la línea de tiempo.", modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp), // Ajustar padding
                    verticalArrangement = Arrangement.spacedBy(0.dp) // El espaciado ahora es parte del item con la línea
                ) {
                    items(
                        items = uiState.events,
                        key = { it.id }
                    ) { event ->
                        val index = uiState.events.indexOf(event) // Necesario para saber si es el primero/último
                        TimelineEventItem(
                            event = event,
                            onMovieClick = onMovieEventClick,
                            isFirstItem = index == 0,
                            isLastItem = index == uiState.events.size - 1
                        )
                        // Añadir un Spacer aquí si quieres más espacio vertical entre los nodos/tarjetas
                        if (index < uiState.events.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineEventItem(
    event: TimelineEvent,
    onMovieClick: (tmdbId: Int) -> Unit,
    isFirstItem: Boolean,
    isLastItem: Boolean
) {
    // Colores del tema para la línea y el nodo
    val stemColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) // OutlineGrey más tenue
    val nodeColor = MaterialTheme.colorScheme.primary // NeonCyan

    Row(modifier = Modifier.fillMaxWidth()) {
        // --- Columna para la Línea de Tiempo Vertical y Fecha ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(end = 16.dp) // Un poco más de padding
                .width(56.dp)      // Ancho ajustado para la fecha
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp) // Altura del segmento de línea superior
                    .background(if (isFirstItem) Color.Transparent else stemColor)
            )
            Box( // Nodo
                modifier = Modifier
                    .size(10.dp) // Nodo un poco más pequeño
                    .background(nodeColor, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.background, CircleShape) // Borde para destacar sobre la línea
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f) // Ocupa el espacio restante
                    .background(if (isLastItem) Color.Transparent else stemColor)
            )
        }

        // --- Columna para el Contenido del Evento (Tarjeta) ---
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp), // Mantener bordes redondeados o ajustar
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant // DarkSurfaceVariant
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) // OutlineGrey
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // --- Año ---
                Text(
                    text = event.year.toString(),
                    style = MaterialTheme.typography.headlineSmall, // Usa Orbitron Bold 24sp
                    color = MaterialTheme.colorScheme.primary // NeonCyan
                )

                // --- Mes y Día (opcional) ---
                val monthDayString = remember(event.month, event.day) {
                    buildString {
                        event.month?.let { append(" / $it") }
                        event.day?.let { append("/$it") }
                    }
                }
                if (monthDayString.isNotEmpty()) {
                    Text(
                        text = monthDayString,
                        style = MaterialTheme.typography.labelMedium, // Orbitron Medium 12sp
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), // TextGrey
                        modifier = Modifier.padding(top = 0.dp) // Menos padding si está justo debajo del año
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Título del Evento e Icono de Tipo ---
                Row(verticalAlignment = Alignment.Top) { // Alineación Top para que el icono quede con la primera línea del título
                    Icon(
                        imageVector = getIconForEventType(event.eventType),
                        contentDescription = event.eventType.name,
                        tint = MaterialTheme.colorScheme.secondary, // NeonMagenta
                        modifier = Modifier.size(22.dp).padding(top = 2.dp) // Ajustar tamaño y padding del icono
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge, // Orbitron Bold 22sp
                        color = MaterialTheme.colorScheme.onSurface // TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Descripción ---
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp), // Orbitron Normal 14sp, con lineHeight ajustado
                    color = MaterialTheme.colorScheme.onSurfaceVariant // TextGrey
                )

                // --- Imagen ---
                event.imageUrl?.let { url ->
                    Spacer(modifier = Modifier.height(14.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(url)
                            .crossfade(true)
                            .placeholder(R.drawable.logo) // Usa tus placeholders
                            .error(R.drawable.logo)
                            .build(),
                        contentDescription = event.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 200.dp) // Ajusta según tus imágenes
                            .clip(RoundedCornerShape(6.dp)), // Bordes más suaves para la imagen
                        contentScale = ContentScale.Crop
                    )
                }

                // --- Botón para Detalles de Película ---
                if (event.eventType == TimelineEventType.MOVIE_RELEASE && event.relatedMovieTMDbId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button( // Usar Button normal para más prominencia con color primario
                        onClick = { onMovieClick(event.relatedMovieTMDbId) },
                        modifier = Modifier.align(Alignment.End).height(40.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary, // NeonCyan
                            contentColor = MaterialTheme.colorScheme.onPrimary // OnPrimaryDark
                        )
                    ) {
                        Text(
                            "Ver Película",
                            style = MaterialTheme.typography.labelMedium // Orbitron Medium 12sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.PlayArrow, // Cambiado a PlayArrow
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}


// Helper para obtener un icono basado en el tipo de evento
@Composable
private fun getIconForEventType(eventType: TimelineEventType): ImageVector {
    return when (eventType) {
        TimelineEventType.MOVIE_RELEASE -> Icons.Filled.Movie
        TimelineEventType.TECHNOLOGY -> Icons.Filled.Build // O Settings, Memory
        TimelineEventType.PERSON_MILESTONE -> Icons.Filled.Person // O EmojiEvents, Star
        TimelineEventType.STUDIO_FORMATION -> Icons.Filled.Business // O Domain
        TimelineEventType.CINEMATIC_MOVEMENT -> Icons.Filled.FormatPaint // O Brush, Palette
        TimelineEventType.HISTORICAL_EVENT -> Icons.Filled.Event // O Gavel, Public
    }
}