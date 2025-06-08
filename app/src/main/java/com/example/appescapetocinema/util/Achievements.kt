
package com.example.appescapetocinema.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.ThumbsUpDown
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.Timestamp

const val ACHIEVEMENT_FIRST_ADD_MYLIST = "FIRST_ADD_MYLIST"
const val ACHIEVEMENT_FIRST_RATING = "FIRST_RATING"
const val ACHIEVEMENT_MYLIST_5 = "MYLIST_5"
const val ACHIEVEMENT_RATED_5 = "RATED_5"
const val ACHIEVEMENT_FIRST_REVIEW = "FIRST_REVIEW_WRITTEN"
const val ACHIEVEMENT_TRIVIA_MASTER_ACTION = "TRIVIA_MASTER_ACTION"
const val ACHIEVEMENT_TRIVIA_MASTER_GENERAL = "TRIVIA_MASTER_GENERAL"
const val ACHIEVEMENT_CINEPHILE_EXPLORER = "CINEPHILE_EXPLORER"
const val ACHIEVEMENT_GEM_COLLECTOR_25 = "GEM_COLLECTOR_25"
const val ACHIEVEMENT_REVIEW_AUTHOR_5 = "REVIEW_AUTHOR_5"
const val ACHIEVEMENT_REVIEW_PRO_15 = "REVIEW_PRO_15"
const val ACHIEVEMENT_DECADE_TRAVELER_5 = "DECADE_TRAVELER_5"
const val ACHIEVEMENT_GENRE_FAN_SCIFI = "GENRE_FAN_SCIFI"
const val ACHIEVEMENT_GENRE_FAN_COMEDY = "GENRE_FAN_COMEDY"
const val ACHIEVEMENT_TRIVIA_STREAK_10 = "TRIVIA_STREAK_10"
const val ACHIEVEMENT_TRIVIA_TOTAL_SCORE_100 = "TRIVIA_TOTAL_SCORE_10K"
const val ACHIEVEMENT_PROFILE_COMPLETE_BASIC = "PROFILE_COMPLETE_BASIC"
const val ACHIEVEMENT_EASTER_EGG_CARPENTER = "EASTER_EGG_CARPENTER"

data class AchievementDefinition(
    val id: String,
    val name: String,
    val description: String,
    val iconUnlocked: ImageVector,
    val iconLocked: ImageVector,
    val secret: Boolean = false
)

data class AchievementDisplayInfo(
    val definition: AchievementDefinition,
    val isUnlocked: Boolean,
    val unlockedDate: Timestamp?
)

// --- Lista Completa de Definiciones de Logros ---
val allAchievementDefinitions: List<AchievementDefinition> = listOf(
    AchievementDefinition(
        id = ACHIEVEMENT_FIRST_ADD_MYLIST, name = "Primeros Pasos",
        description = "Añadiste tu primera película a Mi Lista.",
        iconUnlocked = Icons.Filled.PlaylistAddCheck, iconLocked = Icons.Outlined.PlaylistAdd
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_FIRST_RATING, name = "Crítico Novato",
        description = "¡Has valorado tu primera película!",
        iconUnlocked = Icons.Filled.StarRate, iconLocked = Icons.Outlined.StarOutline
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_MYLIST_5, name = "Coleccionista Principiante",
        description = "Tienes 5 películas en Mi Lista.",
        iconUnlocked = Icons.Filled.LibraryAddCheck, iconLocked = Icons.Outlined.LibraryBooks
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_RATED_5, name = "Paladar Exigente",
        description = "Has valorado 5 películas diferentes.",
        iconUnlocked = Icons.Filled.ThumbsUpDown, iconLocked = Icons.Outlined.ThumbsUpDown
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_FIRST_REVIEW, name = "¡Tu Opinión Cuenta!",
        description = "Escribiste tu primera reseña.",
        iconUnlocked = Icons.Filled.RateReview, iconLocked = Icons.Outlined.RateReview
    ),
    AchievementDefinition( // Genérico por ahora, se puede especializar
        id = ACHIEVEMENT_TRIVIA_MASTER_GENERAL, name = "Maestro de la Trivia",
        description = "Completaste una ronda de trivia con puntuación perfecta.",
        iconUnlocked = Icons.Filled.EmojiEvents, iconLocked = Icons.Outlined.EmojiEvents
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_CINEPHILE_EXPLORER, name = "Explorador Cinéfilo",
        description = "Realizaste 10 búsquedas utilizando filtros.",
        iconUnlocked = Icons.Filled.TravelExplore, iconLocked = Icons.Outlined.Search
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_GEM_COLLECTOR_25, name = "Coleccionista de Gemas",
        description = "Añadiste 25 películas a 'Mi Lista'.",
        iconUnlocked = Icons.Filled.Diamond, iconLocked = Icons.Outlined.CheckBoxOutlineBlank
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_REVIEW_AUTHOR_5, name = "Voz Autorizada",
        description = "Has escrito 5 reseñas.",
        iconUnlocked = Icons.Filled.RecordVoiceOver, iconLocked = Icons.Outlined.RecordVoiceOver
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_REVIEW_PRO_15, name = "Crítico Consagrado",
        description = "Has escrito 15 reseñas de películas o series.",
        iconUnlocked = Icons.Filled.WorkspacePremium, iconLocked = Icons.Outlined.WorkspacePremium
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_DECADE_TRAVELER_5, name = "Viajero Temporal",
        description = "Has buscado películas de 5 décadas diferentes.",
        iconUnlocked = Icons.Filled.HistoryEdu, iconLocked = Icons.Outlined.HistoryEdu
    ),
    AchievementDefinition( // Ejemplo para un género
        id = ACHIEVEMENT_GENRE_FAN_SCIFI, name = "Fanático Sci-Fi",
        description = "Has añadido 10 películas de Ciencia Ficción a 'Mi Lista'.",
        iconUnlocked = Icons.Filled.RocketLaunch, iconLocked = Icons.Outlined.RocketLaunch
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_TRIVIA_STREAK_10, name = "Racha Imparable",
        description = "Respondiste 10 preguntas de trivia seguidas correctamente.",
        iconUnlocked = Icons.Filled.LocalFireDepartment, iconLocked = Icons.Outlined.LocalFireDepartment
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_TRIVIA_TOTAL_SCORE_100, name = "Enciclopedia del Cine",
        description = "Alcanzaste 10,000 puntos totales en trivia.",
        iconUnlocked = Icons.Filled.MenuBook, iconLocked = Icons.Outlined.MenuBook
    ),
    AchievementDefinition(
        id = ACHIEVEMENT_PROFILE_COMPLETE_BASIC, name = "Perfil Completo",
        description = "Añadiste a Mi Lista, valoraste y escribiste una reseña.",
        iconUnlocked = Icons.Filled.CheckCircle, iconLocked = Icons.Outlined.CheckCircle
    ),
    AchievementDefinition( // Logro secreto
        id = ACHIEVEMENT_EASTER_EGG_CARPENTER, name = "???",
        description = "Has encontrado un secreto oscuro y neón...",
        iconUnlocked = Icons.Filled.Visibility,
        iconLocked = Icons.Filled.QuestionMark,
        secret = true
    )
)
fun getAchievementName(id: String): String {
    return allAchievementDefinitions.find { it.id == id }?.name ?: id
}

fun getAchievementDefinition(id: String): AchievementDefinition? {
    return allAchievementDefinitions.find { it.id == id }
}