package com.example.appescapetocinema.ui.detail

import android.text.format.DateFormat
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material3.Slider
import androidx.compose.material3.LinearProgressIndicator // Para feedback de carga
import androidx.compose.runtime.remember // Para estado local del slider
import androidx.compose.ui.Alignment
import java.text.NumberFormat
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appescapetocinema.R
import com.example.appescapetocinema.network.dto.CountrySpecificProvidersDto
import com.example.appescapetocinema.model.NewsArticle
import com.example.appescapetocinema.model.Review
import com.example.appescapetocinema.network.TmdbApiService // Para URLs base de imágenes
import com.example.appescapetocinema.network.dto.CastMemberDto
import com.example.appescapetocinema.network.dto.CrewMemberDto
import com.example.appescapetocinema.repository.MovieRepositoryImpl
import com.example.appescapetocinema.repository.NewsRepositoryImpl
import com.example.appescapetocinema.repository.ReviewRepositoryImpl
import com.example.appescapetocinema.repository.UserProfileRepositoryImpl
import com.example.appescapetocinema.repository.UserRepositoryImpl
import com.example.appescapetocinema.ui.components.MovieCard
import com.example.appescapetocinema.ui.components.MovieItem
import com.example.appescapetocinema.ui.components.MovieSectionPaginated
import com.example.appescapetocinema.ui.components.NewsArticleCard
import com.example.appescapetocinema.ui.components.RatingBar
import com.example.appescapetocinema.ui.components.ShimmerNewsArticleCardPlaceholder
import com.example.appescapetocinema.ui.components.ShimmerPersonCardPlaceholder
import com.example.appescapetocinema.ui.components.ShimmerProviderLogoItem
import com.example.appescapetocinema.ui.components.WatchProviderCategorySection
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder

// --- Composable de UI Desacoplado ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    uiState: DetailUiState, // Estado para detalles base, mi lista, rating, reseñas, cast, director
    similarMovies: LazyPagingItems<MovieItem>?, // <-- Recibe PagingItems Nullable
    onNavigateBack: () -> Unit,
    onRetryDetails: () -> Unit, // Retry para detalles base
    onToggleMyList: () -> Unit,
    onSimilarMovieClick: (Int) -> Unit,
    onRatingSubmit: (Double) -> Unit,
    onReviewTextChange: (String) -> Unit,
    onSubmitReviewClick: () -> Unit,
    onRetrySimilar: () -> Unit, // <-- Retry para similares
    onEditReviewClick: (Review) -> Unit,
    onDeleteReviewClick: (Review) -> Unit,
    snackbarHostState: SnackbarHostState,
    onFindNearbyShowtimesClick: () -> Unit

) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    uiState.movieDetails?.let {
                        Text(
                            it.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge // Orbitron Bold
                        )
                    } ?: Text("")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), // Surface semi-transparente
                    titleContentColor = MaterialTheme.colorScheme.primary, // Título en neón
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface // Icono de volver estándar
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) } // <--
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                // --- Carga Inicial ---
                uiState.isLoading && uiState.movieDetails == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        // --- Usa Color del Tema ---
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Estado de Error General (solo si no hay detalles)
                uiState.errorMessage != null && uiState.movieDetails == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetryDetails) { Text("Reintentar") }
                    }
                }
                // Estado Éxito (Mostrar detalles, incluso si hay error secundario)
                uiState.movieDetails != null -> {
                    MovieDetailContent(
                        movie = uiState.movieDetails,
                        isLoadingWatchProviders = uiState.isLoadingWatchProviders,
                        watchProvidersByCountry = uiState.watchProvidersByCountry,
                        watchProvidersError = uiState.watchProvidersError,
                        selectedWatchProviderCountry = uiState.selectedWatchProviderCountry,
                        isLoadingMovieNews = uiState.isLoadingMovieNews,
                        movieNewsArticles = uiState.movieNewsArticles,
                        movieNewsError = uiState.movieNewsError,
                        // Estados directos de UiState
                        isMovieInMyList = uiState.isMovieInMyList,
                        isUpdatingMyList = uiState.isUpdatingMyList,
                        userRating = uiState.userRating,
                        isUpdatingRating = uiState.isUpdatingRating,
                        cast = uiState.cast,
                        isLoading = uiState.isLoading,
                        director = uiState.director,
                        secondaryErrorMessage = if(uiState.isLoading) null else uiState.errorMessage, // Error que no impide mostrar detalles
                        reviews = uiState.reviews,
                        isLoadingReviews = uiState.isLoadingReviews,
                        userReviewInputText = uiState.userReviewInputText,
                        canSubmitReview = uiState.canSubmitReview,
                        isSubmittingReview = uiState.isSubmittingReview,
                        reviewSubmissionError = uiState.reviewSubmissionError,
                        userHasExistingReview = uiState.userHasExistingReview,
                        // PagingItems y lambdas
                        similarMovies = similarMovies, // Pasa los LazyPagingItems
                        onToggleMyListClick = onToggleMyList,
                        onRatingChanged = onRatingSubmit,
                        onSimilarMovieClick = onSimilarMovieClick,
                        onReviewTextChanged = onReviewTextChange,
                        onSubmitReview = onSubmitReviewClick,
                        onRetrySimilar = onRetrySimilar, // Pasa lambda reintento similares
                        currentUserId = uiState.currentUserId, // <-- Pasar User ID
                        onEditReviewClick = onEditReviewClick, // <-- Pasar lambda
                        onDeleteReviewClick = onDeleteReviewClick, // <-- Pasar lambda
                        onFindNearbyShowtimesClick = onFindNearbyShowtimesClick

                    )
                }
                // Fallback
                else -> { Text("Detalles no disponibles.", Modifier.align(Alignment.Center).padding(16.dp)) }
            }
            }
        }
    }



// --- Composable MovieDetailContent ---
@Composable
fun MovieDetailContent(
    movie: MovieDetails,
    isMovieInMyList: Boolean,
    isUpdatingMyList: Boolean,
    onToggleMyListClick: () -> Unit,
    cast: List<CastMemberDto>,
    isLoading: Boolean,
    director: CrewMemberDto?,
    secondaryErrorMessage: String?, // Errores generales o de MiLista/Rating
    userRating: Double?,
    isUpdatingRating: Boolean,
    onRatingChanged: (Double) -> Unit, // Recibe Double
    similarMovies: LazyPagingItems<MovieItem>?, // Nullable PagingItems
    onSimilarMovieClick: (Int) -> Unit,
    onRetrySimilar: () -> Unit,
    userReviewInputText: String,
    onReviewTextChanged: (String) -> Unit,
    canSubmitReview: Boolean,
    isSubmittingReview: Boolean,
    onSubmitReview: () -> Unit,
    reviewSubmissionError: String?, // Error específico de envío
    reviews: List<Review>,
    isLoadingReviews: Boolean,
    userHasExistingReview: Boolean,
    currentUserId: String?,

    isLoadingWatchProviders: Boolean,
    watchProvidersByCountry: Map<String, CountrySpecificProvidersDto>?,
    watchProvidersError: String?,
    selectedWatchProviderCountry: String,

    isLoadingMovieNews: Boolean,
    movieNewsArticles: List<NewsArticle>,
    movieNewsError: String?,
    onEditReviewClick: (Review) -> Unit,
    onDeleteReviewClick: (Review) -> Unit,
    onFindNearbyShowtimesClick: () -> Unit
) {
    // Estado local para el Slider
    var sliderPosition by remember(userRating) { mutableStateOf(userRating?.toFloat() ?: 0f) }
    // Formateador para el valor del Slider
    val numberFormat = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 } }
    val uriHandler = LocalUriHandler.current // Para abrir URLs


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp) // Padding inferior general
    ) {
        // 1. Backdrop
        item {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(movie.backdropUrl ?: movie.posterUrl).crossfade(true).placeholder(
                    R.drawable.logo).error(R.drawable.logo).build(),
                contentDescription = "Backdrop de ${movie.title}",
                modifier = Modifier.fillMaxWidth().height(250.dp),
                contentScale = ContentScale.Crop
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 2. Título
        item {
            Text(
                text = movie.title,
                // --- Usa Typo y Color del Tema ---
                style = MaterialTheme.typography.headlineLarge, // Rajdhani Bold
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 3. Año, Géneros, Rating TMDb (Texto)
        item {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp) // Padding general para esta "fila"
            ) {
                // Crear referencias para cada Composable
                val (yearRef, genreChipsRef, ratingIconRef, ratingTextRef) = createRefs()

                // 1. Año (a la izquierda)
                Text(
                    text = movie.releaseYear,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.constrainAs(yearRef) {
                        start.linkTo(parent.start)
                        centerVerticallyTo(parent) // Centrar verticalmente con respecto al ConstraintLayout
                    }
                )

                // 2. Rating (a la derecha) - Texto y luego Icono
                Text(
                    text = "${String.format("%.1f", movie.rating)}/10",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.constrainAs(ratingTextRef) {
                        end.linkTo(parent.end)
                        centerVerticallyTo(parent)
                    }
                )
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Valoración media",
                    modifier = Modifier
                        .size(18.dp)
                        .constrainAs(ratingIconRef) {
                            end.linkTo(ratingTextRef.start, margin = 4.dp) // A la izquierda del texto del rating
                            centerVerticallyTo(ratingTextRef) // Alinear verticalmente con el texto del rating
                        },
                    tint = MaterialTheme.colorScheme.secondary
                )

                // 3. GenreChips (en el medio, ocupando el espacio restante)
                // Usamos un Box para aplicar las constraints y que LazyRow (GenreChips) se adapte
                Box(
                    modifier = Modifier.constrainAs(genreChipsRef) {
                        start.linkTo(yearRef.end, margin = 8.dp)       // A la derecha del año
                        end.linkTo(ratingIconRef.start, margin = 8.dp) // A la izquierda del icono de estrella
                        top.linkTo(parent.top)                         // Alinear con el top del ConstraintLayout
                        bottom.linkTo(parent.bottom)                   // Alinear con el bottom
                        width = Dimension.fillToConstraints            // Ocupar el espacio disponible horizontalmente
                        // height = Dimension.wrapContent // La altura se ajustará al contenido de GenreChips
                    }
                ) {
                    GenreChips(genres = movie.genres.take(3)) // O el número de géneros que quieras mostrar
                }
            }
        }

        // Director
        director?.let { dir ->
            item {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("Director: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dir.name ?: "Desconocido", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // 4. Sinopsis
        item {
            Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp))
        }
        item {
            Text(movie.overview, style = MaterialTheme.typography.bodyLarge,color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // 5. Sección Tu Valoración (Slider)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("Tu Valoración", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 8.dp))
                Row( modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically ) {
                    Slider( value = sliderPosition, onValueChange = { sliderPosition = it }, onValueChangeFinished = { val roundedRating = (sliderPosition * 4).toInt() / 4.0; onRatingChanged(roundedRating) }, valueRange = 0f..10f, steps = 39, modifier = Modifier.weight(1f), enabled = !isUpdatingRating, colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary, // NeonCyan
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant, // DarkSurfaceVariant
                        activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    ) )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text( numberFormat.format(sliderPosition), style = MaterialTheme.typography.titleSmall,color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = TextAlign.End )
                }
                if (isUpdatingRating) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),color = MaterialTheme.colorScheme.primary, // Track color
                    trackColor = MaterialTheme.colorScheme.surfaceVariant) }
            }
            Divider(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), color = MaterialTheme.colorScheme.outline)
        }

        // 6. Botón Mi Lista
        item {
            Spacer(modifier = Modifier.height(16.dp))
            secondaryErrorMessage?.let { errorMsg -> Text( text = errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp), textAlign = TextAlign.Center ) }
            Button( onClick = onToggleMyListClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(8.dp), enabled = !isUpdatingMyList, colors = if (isMovieInMyList) { ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary) }
            else { ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) } // Botón primario por defecto
            ) {
                if (isUpdatingMyList) { CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary) } // Color tema
                else { Row(verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = if (isMovieInMyList) Icons.Filled.Check else Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize)); Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing)); Text(if (isMovieInMyList) "En Mi Lista" else "Añadir a Mi Lista") } } // Texto usa color del botón por defecto
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Spacer(modifier = Modifier.height(8.dp)) // Espacio entre botones
            Button(
                onClick = onFindNearbyShowtimesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                // Puedes personalizar colores si quieres diferenciarlo
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(
                    Icons.Filled.Theaters, // O Movie, LocationOn
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Buscar Cines Cercanos")
            }
            Spacer(modifier = Modifier.height(16.dp)) // Espacio después del nuevo botón
        }

        // 7. Sección Escribir Reseña
        item {
            WriteReviewSection(
                inputText = userReviewInputText,
                onTextChange = onReviewTextChanged,
                onSubmitClick = onSubmitReview,
                canSubmit = canSubmitReview,
                isSubmitting = isSubmittingReview,
                submitError = reviewSubmissionError
            )
            Divider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }

        // 8. Reparto Principal
        item {
            Text("Reparto Principal",
                style = MaterialTheme.typography.titleLarge, // Typo tema
                color = MaterialTheme.colorScheme.onBackground, // Color tema
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        // --- Lógica para Shimmer o Contenido Real ---
        if (cast.isEmpty() && isLoading) { // Muestra Shimmer si la lista de cast está vacía Y AÚN está cargando detalles
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(6) { // Muestra, por ejemplo, 6 placeholders
                        ShimmerPersonCardPlaceholder() // Importa este Composable
                    }
                }
                Spacer(modifier = Modifier.height(16.dp)) // Espacio después
            }
        } else if (cast.isNotEmpty()) { // Si hay datos de cast, muéstralos
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cast, key = { it.id }) { castMember ->
                        PersonCard(person = castMember) // PersonCard ya debería usar el tema
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        // Opcional: Mensaje si no hay reparto y no está cargando
        else if (cast.isEmpty() && !isLoading) {
             item {
                Text(
                     "Reparto no disponible.",
                     modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                     style = MaterialTheme.typography.bodyMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
             }
        }

        // 9. Sección Mostrar Reseñas
        item { Text( "Reseñas", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp) ) }
        if (isLoadingReviews) { item { Box(Modifier.fillMaxWidth().padding(vertical=16.dp), contentAlignment = Alignment.Center){ CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) } } }
        else if (reviews.isEmpty()) { item { Text("Sé el primero...", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal=16.dp, vertical = 16.dp), textAlign = TextAlign.Center) } } // Añadido padding y centrado
        else {
            items(reviews, key = { it.reviewId }) { review ->
                // --- Pasa nuevos parámetros a ReviewItemCard ---
                ReviewItemCard(
                    review = review,
                    currentUserId = currentUserId, // <-- Pasa ID usuario
                    onEditClick = onEditReviewClick, // <-- Pasa lambda editar
                    onDeleteClick = onDeleteReviewClick // <-- Pasa lambda borrar
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
            }
        }

        if (isLoadingWatchProviders || watchProvidersByCountry?.get(selectedWatchProviderCountry) != null || watchProvidersError != null) {
            item {
                Spacer(modifier = Modifier.height(if (isLoadingMovieNews || movieNewsArticles.isNotEmpty() || movieNewsError != null) 16.dp else 24.dp)) // Ajustar espacio si hay noticias arriba
                Text(
                    text = "Disponible en (${selectedWatchProviderCountry.uppercase()})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        when {
            isLoadingWatchProviders -> {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Shimmer para "Streaming"
                        Text("Streaming", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)), modifier = Modifier.padding(bottom = 4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                            items(3) { ShimmerProviderLogoItem() } // Usa tu ShimmerProviderLogoItem
                        }
                        // Shimmer para "Alquilar"
                        Text("Alquilar", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)), modifier = Modifier.padding(bottom = 4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            items(2) { ShimmerProviderLogoItem() }
                        }
                    }
                }
            }
            watchProvidersError != null -> {
                item {
                    Text(
                        text = "No se pudo cargar dónde ver: ${watchProvidersError}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            watchProvidersByCountry != null -> {
                val providersForSelectedCountry = watchProvidersByCountry[selectedWatchProviderCountry]

                if (providersForSelectedCountry != null &&
                    (providersForSelectedCountry.flatrate?.isNotEmpty() == true ||
                            providersForSelectedCountry.rent?.isNotEmpty() == true ||
                            providersForSelectedCountry.buy?.isNotEmpty() == true ||
                            providersForSelectedCountry.ads?.isNotEmpty() == true ||
                            providersForSelectedCountry.free?.isNotEmpty() == true )) {

                    val countryLink = providersForSelectedCountry.link
                    val onCountryLinkClick = { url: String ->
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {
                            Log.e("DetailScreen", "Cannot open URI", e)
                        }
                        Unit
                    }
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            WatchProviderCategorySection(
                                title = "Streaming", // O "En tu suscripción"
                                providers = providersForSelectedCountry.flatrate,
                                countryLink = countryLink,
                                onCountryLinkClick = onCountryLinkClick
                            )
                            WatchProviderCategorySection(
                                title = "Alquilar",
                                providers = providersForSelectedCountry.rent,
                                countryLink = countryLink,
                                onCountryLinkClick = onCountryLinkClick
                            )
                            WatchProviderCategorySection(
                                title = "Comprar",
                                providers = providersForSelectedCountry.buy,
                                countryLink = countryLink,
                                onCountryLinkClick = onCountryLinkClick
                            )
                            WatchProviderCategorySection(
                                title = "Gratis con Anuncios",
                                providers = providersForSelectedCountry.ads,
                                countryLink = countryLink,
                                onCountryLinkClick = onCountryLinkClick
                            )
                            WatchProviderCategorySection(
                                title = "Gratis",
                                providers = providersForSelectedCountry.free,
                                countryLink = countryLink,
                                onCountryLinkClick = onCountryLinkClick
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "No hay información de dónde ver esta película en ${selectedWatchProviderCountry.uppercase()}.",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 10. Películas Similares (PAGINADO) ---
        similarMovies?.let { pagingItems ->
            if (pagingItems.itemCount > 0 || pagingItems.loadState.refresh != LoadState.NotLoading(endOfPaginationReached = true) ) {
                item { Text("Te podría gustar", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)) }
                item { MovieSectionPaginated(
                    title = "",
                    movies = pagingItems,
                    onMovieClick = onSimilarMovieClick,
                    onRetry = onRetrySimilar )
                } // Usa tema interno
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Solo mostramos el título de la sección si hay algo que mostrar o se está cargando
        if (isLoadingMovieNews || movieNewsArticles.isNotEmpty() || movieNewsError != null) {
            item {
                Text(
                    text = "Noticias Relacionadas",
                    style = MaterialTheme.typography.titleLarge, // Orbitron Bold 22sp
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground // TextWhite
                )
            }
        }

        when {
            isLoadingMovieNews -> {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(3) { // Muestra 3 shimmers
                            ShimmerNewsArticleCardPlaceholder() // Asegúrate de tener este Composable
                        }
                    }
                }
            }
            movieNewsError != null -> {
                item {
                    Text(
                        text = "Noticias no disponibles: ${movieNewsError}", // Mensaje más amigable
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), // Menos estridente que el color de error
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            movieNewsArticles.isNotEmpty() -> {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = movieNewsArticles, key = { it.id }) { article -> // Usa tu NewsArticle
                            NewsArticleCard(article = article) // Asegúrate de tener este Composable
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}


// --- Composable WriteReviewSection ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewSection( inputText: String, onTextChange: (String) -> Unit, onSubmitClick: () -> Unit, canSubmit: Boolean, isSubmitting: Boolean, submitError: String? ) {
    val focusManager = LocalFocusManager.current; val keyboardController = LocalSoftwareKeyboardController.current
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        Text("Escribe tu Reseña", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = inputText,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            placeholder = { Text("Comparte tu opinión...") },
            label = { Text("Tu Reseña") },
            isError = submitError != null,
            enabled = !isSubmitting,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // Fondo sutil
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error,
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            ),
            textStyle = MaterialTheme.typography.bodyMedium // Typo del tema
        )
        submitError?.let { Text( text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp) ) }
        Spacer(modifier = Modifier.height(8.dp))
        Button( onClick = { onSubmitClick(); keyboardController?.hide(); focusManager.clearFocus() }, enabled = canSubmit && !isSubmitting, modifier = Modifier.align(Alignment.End) ) { // Botón usa tema por defecto
            if (isSubmitting) { CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Enviando...") }
            else { Text("Enviar Reseña") } // Texto usa typo y color del botón
        }
    }
}


@Composable
fun ReviewItemCard(
    review: Review,
    currentUserId: String?,
    onEditClick: (Review) -> Unit,
    onDeleteClick: (Review) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = remember(context) { DateFormat.getDateFormat(context) }
    val isCurrentUserReview = review.userId == currentUserId && currentUserId != null

    Column( modifier = modifier.padding(vertical = 8.dp, horizontal = 16.dp) ) {
        Row(verticalAlignment = Alignment.Top) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon( Icons.Filled.AccountCircle, contentDescription = "Avatar", modifier = Modifier.size(32.dp).clip(CircleShape), tint = MaterialTheme.colorScheme.onSurfaceVariant ) // Color tema
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(review.userName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) // Typo y color tema
                review.timestamp?.let { Text(dateFormat.format(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } // Typo y color tema
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
            if (isCurrentUserReview) {
                Row {
                    IconButton(onClick = { onEditClick(review) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar Reseña", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { onDeleteClick(review) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Borrar Reseña", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                // Si no es del usuario actual, muestra el rating
                RatingBar( rating = review.rating / 2.0, isIndicator = true, starSize = 16.dp, starColor = MaterialTheme.colorScheme.secondary )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(review.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) // Texto de la reseña
    }
}


// --- Composable PersonCard ---
@Composable
fun PersonCard(person: Any, modifier: Modifier = Modifier) {
    val name = when (person) { is CastMemberDto -> person.name; is CrewMemberDto -> person.name; else -> "Desconocido" }
    val profilePath = when (person) { is CastMemberDto -> person.profilePath; is CrewMemberDto -> person.profilePath; else -> null }
    val subtitle = when (person) { is CastMemberDto -> person.character; is CrewMemberDto -> person.job; else -> null }?.takeIf { it.isNotBlank() }
    val imageUrl = TmdbApiService.getPosterUrl(profilePath)

    Column(
        modifier = modifier.width(100.dp).clickable { /* Acción futura */ }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(shape = CircleShape, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { // Color tema
            AsyncImage( model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).placeholder(
                R.drawable.logo).error(R.drawable.logo).build(), contentDescription = name, modifier = Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = name ?: "Desconocido", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp))
        subtitle?.let { Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp)) }
    }
}

// --- Composable GenreChips ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreChips(genres: List<String>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 4.dp)) { // Añadido padding horizontal
        items(genres) { genre ->
            SuggestionChip(
                onClick = { /* No action */ },
                label = { Text(genre, style = MaterialTheme.typography.labelSmall) }, // Typo tema
                shape = RoundedCornerShape(8.dp),
                // --- Colores y Borde del Tema ---
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color.Transparent, // Fondo transparente
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant // Color tema
                )
            )
        }
    }
}

@Composable
fun MovieSection(title: String, movies: List<MovieItem>, onMovieClick: (Int) -> Unit, modifier: Modifier = Modifier ) {
    if (movies.isNotEmpty()) {
        Column(modifier = modifier.padding(vertical = 8.dp)) {
            if (title.isNotBlank()) { Text( text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp) ) }
            LazyRow( modifier = Modifier.heightIn(min = 260.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp) ) {
                items( items = movies, key = { it.id } ) { movie ->
                    MovieCard(
                        movie = movie,
                        onClick = onMovieClick
                    )
                }
            }
        }
    }
}


@Composable
fun DetailScreenContainer(navController: NavController) {
    val movieRepository = remember { MovieRepositoryImpl() }
    val userRepository = remember { UserRepositoryImpl() }
    val reviewRepository = remember { ReviewRepositoryImpl() }
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val userProfileRepository = remember { UserProfileRepositoryImpl() }
    val newsRepository = remember { NewsRepositoryImpl() } // Asumiendo que tu NewsRepositoryImpl no necesita dependencias o las obtiene internamente

    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.Companion.Factory(
            movieRepository, userRepository, reviewRepository,
            userProfileRepository,
            firebaseAuth,
            newsRepository
        )
    )


    val uiState by viewModel.uiState.collectAsState()
    val similarMoviesPagingItems = viewModel.similarMoviesFlow?.collectAsLazyPagingItems()

    // --- Lógica para Confirmación de Borrado ---
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var reviewToDelete by remember { mutableStateOf<Review?>(null) }

    if (showDeleteConfirmationDialog && reviewToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false; reviewToDelete = null },
            title = { Text("Confirmar Borrado") },
            text = { Text("¿Seguro que quieres eliminar tu reseña?") },
            confirmButton = {
                Button(onClick = {
                    reviewToDelete?.let { viewModel.deleteCurrentUserReview(it) } // Llama al VM
                    showDeleteConfirmationDialog = false
                    reviewToDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false; reviewToDelete = null }) { Text("Cancelar") }
            }
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope() // Para lanzar el snackbar

    LaunchedEffect(Unit) { // Se lanza una vez cuando el Container entra en composición
        viewModel.achievementUnlockedEvent.collectLatest { message -> // Colecciona el SharedFlow
            Log.d("DetailContainer", "Mostrando Snackbar: $message")
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }
    val movieDetails = uiState.movieDetails // Tu modelo con imdbId

    DetailScreen(
        uiState = uiState,
        similarMovies = similarMoviesPagingItems,
        onNavigateBack = { navController.popBackStack() },
        onRetryDetails = viewModel::retryLoad,
        onToggleMyList = viewModel::toggleMyListStatus,
        onSimilarMovieClick = { similarMovieId ->
            Log.d("DetailContainer", "onSimilarMovieClick: Intentando navegar a detail/$similarMovieId")
            navController.navigate(Screen.Detail.createRoute(similarMovieId))
        },
        onRatingSubmit = viewModel::submitRating,
        onReviewTextChange = viewModel::onReviewTextChange,
        onSubmitReviewClick = viewModel::submitReview,
        onRetrySimilar = { similarMoviesPagingItems?.retry() },
        onEditReviewClick = { reviewToEdit ->
            Log.d("DetailContainer", "Edit click para: ${reviewToEdit.reviewId}")
            viewModel.startEditingReview(reviewToEdit) // Llama al VM (necesitamos crear esta función)
        },
        onDeleteReviewClick = { review ->
            Log.d("DetailContainer", "Delete click para: ${review.reviewId}")
            // Mostrar diálogo de confirmación
            reviewToDelete = review
            showDeleteConfirmationDialog = true
            // La llamada a viewModel.deleteCurrentUserReview se hace desde el diálogo
        },
        snackbarHostState = snackbarHostState, // <-- Pasa el SnackbarHostState
        onFindNearbyShowtimesClick = {
            if (movieDetails != null && movieDetails.id.toLong() != 0L) { // Verifica si hay detalles válidos
                val tmdbId = movieDetails.id
                val title = movieDetails.title
                val imdbId = movieDetails.imdbId

                if (imdbId.isNullOrBlank()) { // Comprobación robusta de IMDB ID
                    Log.e("DetailContainer", "IMDB ID es nulo/vacío para '$title'. No se puede buscar horarios.")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("No se pudo identificar la película para buscar horarios.")
                    }
                } else {
                    Log.d("DetailContainer", "Navegando a buscar cines para: $title (IMDB: $imdbId)")
                    try {
                        // Codificar título para URL
                        val encodedTitle = URLEncoder.encode(title, "UTF-8")

                        // Navegar a la nueva pantalla usando tu clase Screen
                        navController.navigate(
                            Screen.NearbyShowtimes.createRoute(
                                movieId = tmdbId.toLong(),
                                movieTitle = encodedTitle,
                                imdbId = imdbId
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("DetailContainer", "Error al codificar título o navegar", e)
                        coroutineScope.launch { snackbarHostState.showSnackbar("Error al iniciar búsqueda.") }
                    }
                }
            } else {
                Log.w("DetailContainer", "Detalles de película no disponibles para buscar cines.")
                coroutineScope.launch { snackbarHostState.showSnackbar("Detalles no disponibles.") }
            }
        }
    )
}

