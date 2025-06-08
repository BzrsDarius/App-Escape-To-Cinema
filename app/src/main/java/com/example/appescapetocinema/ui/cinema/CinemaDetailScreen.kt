package com.example.appescapetocinema.ui.cinema

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appescapetocinema.model.MovieScreening
import com.example.appescapetocinema.repository.CinemaRepositoryImpl
import com.example.appescapetocinema.R
import com.example.appescapetocinema.repository.MovieRepositoryImpl
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- CinemaDetailScreen (UI Desacoplada) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaDetailScreen(
    uiState: CinemaDetailUiState,
    onNavigateBack: () -> Unit,
    onMovieClick: (String?) -> Unit, // <-- Recibe imdbId nullable
    onDateChange: (String) -> Unit, // Llama al VM para recargar con nueva fecha
    onRetry: () -> Unit
) {
    // TODO: Implementar DatePickerDialog para onDateChange
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    // Si uiState.date está vacío, usa la fecha actual
    val initialDateString = uiState.date.ifEmpty {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.cinemaName ?: "Cartelera", style = MaterialTheme.typography.titleLarge)
                        // Mostrar fecha seleccionada
                        Text(
                            "Para: ${initialDateString}", // Formatear mejor si es necesario
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    IconButton(onClick = {
                        // --- Lógica para mostrar DatePickerDialog ---
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH)
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        calendar.time = Date() // Asegura que empieza en hoy si no hay fecha previa
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            if (uiState.date.isNotEmpty()) calendar.time = sdf.parse(uiState.date) ?: Date()
                        } catch (e:Exception) { /* usa fecha actual */ }

                        DatePickerDialog(
                            context,
                            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                                val newDate = String.format(
                                    Locale.getDefault(),
                                    "%d-%02d-%02d",
                                    selectedYear,
                                    selectedMonth + 1,
                                    selectedDayOfMonth
                                )
                                onDateChange(newDate)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) { Icon(Icons.Filled.CalendarToday, "Cambiar Fecha") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 8.dp)) {
            when {
                uiState.isLoading -> { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
                uiState.errorMessage != null -> {
                    Column(Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center); Spacer(Modifier.height(8.dp)); Button(onClick = onRetry) { Text("Reintentar") }
                    }
                }
                uiState.movieScreenings.isEmpty() -> {
                    Text("No hay películas en cartelera para esta fecha.", modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(uiState.movieScreenings, key = { it.filmGluId }) { screening ->
                            // --- Pasa el imdbId al onClick ---
                            MovieScreeningCard(
                                screening = screening,
                                onMovieClick = { onMovieClick(screening.imdbId) } // <-- Llama con imdbId
                            )
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

// --- Composable para Tarjeta de Película en Cartelera ---
@Composable
fun MovieScreeningCard(screening: MovieScreening, onMovieClick: () -> Unit,modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onMovieClick).padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        screening.posterImageUrl?.let { url ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).placeholder(R.drawable.logo).error(R.drawable.logo).build(),
                contentDescription = screening.filmName,
                modifier = Modifier.width(100.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)), // Aspect ratio póster
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(screening.filmName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
            screening.ageRating?.let { Text("Calificación: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            screening.durationMins?.let { Text("Duración: $it min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar formatos y horarios
            screening.screeningFormats.forEach { formatWithTimes ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MovieFilter, contentDescription = "Formato", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(formatWithTimes.formatName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                // Muestra los horarios en una LazyRow o FlowRow (si son muchos)
                // Por simplicidad, un Text con joinToString
                Text(
                    formatWithTimes.times.joinToString(" | ") { it.startTime },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 20.dp, top = 2.dp, bottom = 4.dp) // Indentado
                )
            }
        }
    }
}


// --- CinemaDetailScreenContainer ---
@Composable
fun CinemaDetailScreenContainer(
    navController: NavController
    // El cinemaId se obtiene del SavedStateHandle en el ViewModel
) {
    val cinemaRepository = remember { CinemaRepositoryImpl() }
    val movieRepository = remember { MovieRepositoryImpl() } // <-- Necesario para el Factory

    val viewModel: CinemaDetailViewModel = viewModel(
        factory = CinemaDetailViewModel.Factory( // Llama al Factory actualizado
            cinemaRepository = cinemaRepository,
            movieRepository = movieRepository // <-- Pasa MovieRepository
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Por ahora, el ViewModel no obtiene el nombre, la UI lo muestra si está en uiState.
    // Si la API de cinemaShowTimes devuelve el nombre del cine, lo se usa desde uiState.
    // Si no, la AppBar podría mostrar "Cartelera" y el id del cine.

    LaunchedEffect(Unit) {
        viewModel.navEvent.collectLatest { event ->
            when (event) {
                is CinemaDetailNavEvent.NavigateToMovieDetail -> {
                    Log.d("CinemaDetailContainer", "Evento Navegación: A DetailScreen con TMDb ID ${event.tmdbId}")
                    navController.navigate(Screen.Detail.createRoute(event.tmdbId))
                }
            }
        }
    }


    CinemaDetailScreen(
        uiState = uiState,
        onNavigateBack = { navController.popBackStack() },
        // --- Llama a la función del ViewModel pasando el imdbId ---
        onMovieClick = { imdbId ->
            viewModel.onMovieScreeningClicked(imdbId) // Llama a la función del VM
        },
        onDateChange = { newDateString ->
            uiState.cinemaId?.let { cinemaId -> viewModel.fetchShowtimes(cinemaId, newDateString) }
        },
        onRetry = viewModel::retry
    )
}