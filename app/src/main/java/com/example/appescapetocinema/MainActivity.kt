package com.example.appescapetocinema

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.appescapetocinema.ui.theme.AppEscapeToCinemaTheme
import android.annotation.SuppressLint
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import bottomNavItems
import com.example.appescapetocinema.repository.RepositorioAutenticacionFirebase
import com.example.appescapetocinema.ui.chat.ChatScreenContainer
import com.example.appescapetocinema.ui.cinema.CINEMA_ID_ARG
import com.example.appescapetocinema.ui.cinema.CinemaDetailScreenContainer
import com.example.appescapetocinema.ui.cinemas.CinemasScreenContainer
import com.example.appescapetocinema.ui.detail.DetailScreenContainer
import com.example.appescapetocinema.ui.home.HomeScreenContainer
import com.example.appescapetocinema.ui.login.LoginScreenContainer
import com.example.appescapetocinema.ui.more.MoreScreenContainer
import com.example.appescapetocinema.ui.news.NewsScreenContainer
import com.example.appescapetocinema.ui.profile.ProfileScreenContainer
import com.example.appescapetocinema.ui.registro.PantallaRegistroContainer
import com.example.appescapetocinema.ui.search.SearchScreenContainer
import com.example.appescapetocinema.ui.settings.acknowledgements.AcknowledgementsScreen
import com.example.appescapetocinema.ui.settings.changepassword.ChangePasswordScreenContainer
import com.example.appescapetocinema.ui.showtimes.NearbyShowtimesContainer
import com.example.appescapetocinema.ui.timeline.TimelineScreenContainer
import com.example.appescapetocinema.ui.trivia.TriviaScreenContainer
import com.example.appescapetocinema.ui.trivia.TriviaSelectionScreen

private const val TAG_SIGN_UP = "signup"
private const val TAG_FORGOT_PASSWORD = "forgot_password"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // --- Repositorio para lógica de MainActivity y MainScreen ---
        val authRepositorioParaMain = RepositorioAutenticacionFirebase()

        setContent {
            AppEscapeToCinemaTheme {
                NavegacionAplicacion(authRepositorioParaMain = authRepositorioParaMain)
            }
        }
    }
}

// --- NavegacionAplicacion ---
@Composable
fun NavegacionAplicacion(authRepositorioParaMain: RepositorioAutenticacionFirebase) { // Recibe el repo
    val navController: NavHostController = rememberNavController()

    // --- DETERMINAR RUTA DE INICIO ---
    val currentUser = authRepositorioParaMain.obtenerUsuarioActual() // Usa el repo pasado
    val startDestination = if (currentUser != null) {
        Log.d("NavegacionApp", "Usuario ya logueado: ${currentUser.email}. Navegando a Home.")
        Screen.Home.route
    } else {
        Log.d("NavegacionApp", "No hay usuario logueado. Navegando a Login.")
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            // LoginScreenContainer ya crea su propio RepositorioAutenticacionFirebase
            // a través de LoginViewModelFactory
            LoginScreenContainer(navController = navController)
        }
        composable(Screen.Register.route) {
            // PantallaRegistroContainer ya crea su propio RepositorioAutenticacionFirebase
            // a través de RegistroViewModelFactory
            PantallaRegistroContainer(navController = navController)
        }
        composable(Screen.Home.route) {
            // MainScreen SÍ necesita el repositorio para pasarlo a ProfileScreen (para logout)
            MainScreen(mainNavController = navController, authRepositorio = authRepositorioParaMain)
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) {
            DetailScreenContainer(navController = navController)
        }
        composable(
            route = Screen.NearbyShowtimes.route,
            arguments = listOf(
                navArgument("movieId") { type = NavType.LongType },
                navArgument("movieTitle") { type = NavType.StringType },
                navArgument("imdbId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getLong("movieId") ?: 0L
            val movieTitleEncoded = backStackEntry.arguments?.getString("movieTitle") ?: ""
            val imdbId = backStackEntry.arguments?.getString("imdbId") ?: ""

            // Decodificar el título
            val movieTitle = remember(movieTitleEncoded) {
                try {
                    java.net.URLDecoder.decode(movieTitleEncoded, "UTF-8")
                } catch (e: Exception) {
                    movieTitleEncoded
                } // Fallback al codificado si falla
            }

            if (movieId != 0L && imdbId.isNotEmpty()) {
                NearbyShowtimesContainer(
                    navController = navController,
                    movieTmdbId = movieId,
                    movieTitle = movieTitle,
                    movieImdbId = imdbId
                )
            } else {
                Log.e(
                    "NavHost",
                    "Argumentos inválidos para NearbyShowtimes: $movieId, $movieTitleEncoded, $imdbId"
                )
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
        composable(
            route = Screen.TriviaGame.route, // "trivia_game/{category}"
            arguments = listOf(navArgument("category") { type = NavType.StringType }) // Define el argumento
        ) { backStackEntry ->
            // Extrae la categoría de los argumentos (ya viene decodificada por Navigation)
            val category = backStackEntry.arguments?.getString("category") ?: "all" // Usa "all" como fallback
            Log.d("NavegacionApp", "Navegando a TriviaGame con Categoría: $category")
            // Llama al contenedor del juego (TriviaViewModel usará SavedStateHandle para obtener la categoría)
            TriviaScreenContainer(navController = navController)
        }
        composable(
            route = Screen.Cinemas.route, // "cinema_detail/{cinemaId}"
            arguments = listOf(navArgument(CINEMA_ID_ARG) { // Usa la constante como clave
                type = NavType.LongType // MovieGlu usa Long para cinemaId
            })
        ) {
            // El ViewModel obtendrá el cinemaId del SavedStateHandle
            CinemaDetailScreenContainer(navController = navController)
        }
        composable(Screen.ChangePassword.route) {
            ChangePasswordScreenContainer(navController = navController)
        }
        composable(Screen.Acknowledgements.route) {
            AcknowledgementsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // Necesario si no uso paddingValues
@Composable
fun MainScreen(
    mainNavController: NavHostController,
    authRepositorio: RepositorioAutenticacionFirebase
) {
    val bottomNavController = rememberNavController() // El de la barra inferior

    Scaffold(
        bottomBar = {
            NavigationBar (
                modifier = Modifier.height(75.dp),
            ){ // Usará bottomNavItems, que ahora incluye Trivia
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen -> // Itera sobre la lista actualizada
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon, // O Painter
                                contentDescription = screen.title,
                                modifier = Modifier.size(if (isSelected) 20.dp else 22.dp)
                            )
                        },                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            bottomNavController.navigate(screen.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController, // Usa el NavController de la BottomBar
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreenContainer(navController = mainNavController) }
            composable(Screen.Search.route) { SearchScreenContainer(navController = mainNavController) }
            composable(Screen.Cinemas.route) { CinemasScreenContainer(navController = mainNavController) }
            composable(Screen.Profile.route) { ProfileScreenContainer(navController = mainNavController) }


            composable(Screen.More.route) {
                MoreScreenContainer(navController = bottomNavController)
            }
            composable(Screen.News.route) {
                NewsScreenContainer(navController = bottomNavController)
            }
            composable(Screen.TriviaSelection.route) {
                TriviaSelectionScreen(
                    navControllerForBack = bottomNavController, // Para el popBackStack de esta pantalla
                    navControllerForGame = mainNavController    // Para navegar a TriviaGame
                )
            }

            composable(Screen.Timeline.route) {
                TimelineScreenContainer(
                    navControllerForBack = bottomNavController,    // Para que el "Atrás" de Timeline vuelva a MoreScreen
                    navControllerForDetail = mainNavController   // Para que Timeline pueda navegar a DetailScreen
                )
            }
            composable(Screen.ChatBot.route) {
                ChatScreenContainer(navController = bottomNavController) // Para volver a MoreScreen
            }
        }
    }
}


