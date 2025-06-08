package com.example.appescapetocinema.ui.home

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.appescapetocinema.ui.theme.AppEscapeToCinemaTheme
import com.example.appescapetocinema.ui.components.MovieItem
import com.example.appescapetocinema.repository.MovieRepositoryImpl
import com.example.appescapetocinema.ui.components.MovieSectionPaginated

// --- HomeScreen (UI - Modificada para recibir LazyPagingItems) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    popularMovies: LazyPagingItems<MovieItem>,
    topRatedMovies: LazyPagingItems<MovieItem>,
    nowPlayingMovies: LazyPagingItems<MovieItem>,
    horrorMovies: LazyPagingItems<MovieItem>,
    actionMovies: LazyPagingItems<MovieItem>,
    eightiesMovies: LazyPagingItems<MovieItem>,
    carpenterMovies: LazyPagingItems<MovieItem>,
    onMovieClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Escape to Cinema",
                        // --- Usa Tipografía del Tema ---
                        style = MaterialTheme.typography.headlineLarge // O displaySmall si quieres Rajdhani
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    // --- Usa Colores del Tema ---
                    containerColor = MaterialTheme.colorScheme.background, // Fondo de la AppBar igual al general
                    titleContentColor = MaterialTheme.colorScheme.primary // Título con color primario (NeonCyan)
                )
            )
        },
        // --- Usa Color del Tema ---
        containerColor = MaterialTheme.colorScheme.background // Fondo general de la pantalla
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // --- Sección Populares ---
            item {
                MovieSectionPaginated(
                    title = "Populares", // El título se mostrará con el style por defecto de MovieSectionPaginated
                    movies = popularMovies,
                    onMovieClick = onMovieClick,
                    onRetry = { popularMovies.retry() }
                )
            }

            // --- Sección Mejor Valoradas ---
            item {
                MovieSectionPaginated(
                    title = "Mejor Valoradas",
                    movies = topRatedMovies,
                    onMovieClick = onMovieClick,
                    onRetry = { topRatedMovies.retry() }
                )
            }

            // --- Sección En Cartelera ---
            item {
                MovieSectionPaginated(
                    title = "En Cartelera",
                    movies = nowPlayingMovies,
                    onMovieClick = onMovieClick,
                    onRetry = { nowPlayingMovies.retry() }
                )
            }
            item {
                MovieSectionPaginated(
                    title = "Terror que Hiela la Sangre",
                    movies = horrorMovies,
                    onMovieClick = onMovieClick,
                    onRetry = { horrorMovies.retry() }
                )
            }
            item {
                MovieSectionPaginated(
                    title = "Pura Adrenalina (Acción)",
                    movies = actionMovies,
                    onMovieClick = onMovieClick,
                    onRetry = { actionMovies.retry() }
                )
            }
            item {
                MovieSectionPaginated(
                    title = "Joyas de los 80",
                    movies = eightiesMovies,
                    onMovieClick = onMovieClick,
                    onRetry = { eightiesMovies.retry() }
                )
            }
            item {
                MovieSectionPaginated(
                    title = "Maestría Carpenter",
                    movies = carpenterMovies,
                    onMovieClick = onMovieClick,
                    onRetry = { carpenterMovies.retry() }
                )
            }

            // ... más secciones ...
        } // Fin LazyColumn
    } // Fin Scaffold
}

// --- HomeScreenContainer (MODIFICADO para coleccionar Flows) ---
@Composable
fun HomeScreenContainer(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(MovieRepositoryImpl()))
) {
    val popularMovies = viewModel.popularMoviesFlow.collectAsLazyPagingItems()
    val topRatedMovies = viewModel.topRatedMoviesFlow.collectAsLazyPagingItems()
    val nowPlayingMovies = viewModel.nowPlayingMoviesFlow.collectAsLazyPagingItems()
    val horrorMovies = viewModel.horrorMoviesFlow.collectAsLazyPagingItems()
    val actionMovies = viewModel.actionMoviesFlow.collectAsLazyPagingItems()
    val eightiesMovies = viewModel.eightiesMoviesFlow.collectAsLazyPagingItems()
    val carpenterMovies = viewModel.carpenterMoviesFlow.collectAsLazyPagingItems()

    HomeScreen(
        popularMovies = popularMovies,
        topRatedMovies = topRatedMovies,
        nowPlayingMovies = nowPlayingMovies,
        horrorMovies = horrorMovies,
        actionMovies = actionMovies,
        eightiesMovies = eightiesMovies,
        carpenterMovies = carpenterMovies,
        onMovieClick = { movieId ->
            Log.d("HomeScreenContainer", "Navegando a detail/$movieId")
            navController.navigate(Screen.Detail.createRoute(movieId))
        }
    )
}

