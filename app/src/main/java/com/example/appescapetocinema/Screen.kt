
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLEncoder // Para codificar argumentos de ruta
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Filled.Home)
    object Search : Screen("search", "Buscar", Icons.Filled.Search)
    object News : Screen("news", "Noticias", Icons.Filled.Newspaper)
    object TriviaSelection : Screen("trivia_selection", "Trivia", Icons.Filled.Quiz) // Ruta y título actualizados
    object Profile : Screen("profile", "Perfil", Icons.Filled.AccountCircle)

    // Acepta un argumento 'category' en la ruta
    object TriviaGame : Screen("trivia_game/{category}", "Trivia", Icons.Filled.Quiz) { // Título puede ser genérico
        // Función helper para construir la ruta con la categoría codificada
        fun createRoute(category: String): String {
            val encodedCategory = URLEncoder.encode(category, StandardCharsets.UTF_8.toString())
            return "trivia_game/$encodedCategory"
        }
    }

    object Detail : Screen("detail/{movieId}", "Detalles", Icons.Filled.Movie) {
        fun createRoute(movieId: Int) = "detail/$movieId"
    }
    object NearbyShowtimes : Screen(
        // 1. Ruta
        route = "nearby_showtimes/{movieId}/{movieTitle}/{imdbId}",

        // 2. Título
        title = "Horarios Cercanos", // O "Cines Cercanos", "Dónde ver", etc.

        // 3. Icono
        icon = Icons.Filled.Theaters // O Icons.Filled.LocationOn, etc.
    ) {
        fun createRoute(movieId: Long, movieTitle: String, imdbId: String): String {
            return "nearby_showtimes/$movieId/$movieTitle/$imdbId"
        }
    }
    object Login : Screen("login", "Login", Icons.Filled.Lock)
    object Register : Screen("register", "Registro", Icons.Filled.PersonAdd)
    object Cinemas : Screen("cinema_detail/{cinemaId}", "Cines", Icons.Filled.Theaters){
        fun createRoute(cinemaId: Long): String = "cinema_detail/$cinemaId"
    }
    object Timeline : Screen("timeline_screen", "Línea Tiempo", Icons.Filled.Timeline)
    object More : Screen("more_screen", "Más", Icons.Filled.MoreHoriz)
    object ChatBot : Screen("chatbot_screen", "CineBot", Icons.Filled.Chat)
    object ChangePassword : Screen("change_password_screen", "Cambiar Contraseña", Icons.Filled.Password)
    object Acknowledgements : Screen("acknowledgements", "Agradecimientos", Icons.Filled.Info)


}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Cinemas,
    Screen.More,
    Screen.Profile
)