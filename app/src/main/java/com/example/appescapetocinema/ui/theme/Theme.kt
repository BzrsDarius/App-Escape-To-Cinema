package com.example.appescapetocinema.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- Definición del Esquema de Colores Oscuro ---
private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,            // Botones principales, indicadores activos
    onPrimary = OnPrimaryDark,       // Texto/icono sobre primario
    primaryContainer = Color(0xFF004F58), // Contenedor con énfasis primario (más oscuro)
    onPrimaryContainer = Color(0xFF97F0FF),// Texto sobre primaryContainer

    secondary = NeonMagenta,         // Botones flotantes, selección
    onSecondary = OnSecondaryDark,     // Texto/icono sobre secundario
    secondaryContainer = Color(0xFF7B007B),// Contenedor secundario
    onSecondaryContainer = Color(0xFFFFD6F9),// Texto sobre secundario

    tertiary = NeonOrange,           // Acentos terciarios
    onTertiary = OnTertiaryDark,       // Texto/icono sobre terciario
    tertiaryContainer = Color(0xFF614000), // Contenedor terciario
    onTertiaryContainer = Color(0xFFFFDDB1),// Texto sobre terciario

    error = ErrorRed,                // Color para errores (texto/iconos)
    onError = OnErrorBlack,          // Texto/icono sobre fondo de error
    errorContainer = OnErrorBlack,       // Fondo para destacar errores
    onErrorContainer = ErrorRed,         // Texto/icono sobre contenedor de error

    background = DarkBackground,       // Fondo principal de la app/pantallas
    onBackground = TextWhite,          // Texto principal sobre fondo

    surface = DarkSurface,           // Fondo de superficies elevadas (Cards, Dialogs, Menus)
    onSurface = TextWhite,           // Texto sobre superficies

    surfaceVariant = DarkSurfaceVariant, // Superficies con menos énfasis (Chips, OutlinedTextField)
    onSurfaceVariant = TextGrey,         // Texto sobre surfaceVariant

    outline = OutlineGrey,           // Bordes sutiles, dividers
    outlineVariant = OutlineVariant,     // Borde con énfasis diferente (opcional)

    // Otros colores importantes
    scrim = Color.Black.copy(alpha = 0.4f), // Oscurecimiento para dialogs modales
    inverseSurface = TextWhite.copy(alpha = 0.9f), // Para contenido sobre inversePrimary (raro en tema oscuro)
    inverseOnSurface = DarkBackground, // Texto sobre inverseSurface
    inversePrimary = NeonPurple.copy(alpha = 0.8f) // Para destacar sobre superficies claras (raro en tema oscuro)
    // surfaceBright, surfaceDim, surfaceContainer, etc. (Nuevos en M3, puedes definirlos o usar defaults)
)

@Composable
fun AppEscapeToCinemaTheme(
    // Forzar tema oscuro para mantener la estética "Escape to Cinema"
    darkTheme: Boolean = true,
    // Deshabilitar colores dinámicos de Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        DarkColorScheme // Por ahora, usa oscuro incluso si darkTheme es false
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Color de la barra de estado igual al fondo
            window.statusBarColor = colorScheme.background.toArgb()
            // Iconos de la barra de estado claros (porque el fondo es oscuro)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // Color barra de navegación inferior (opcional, puede ser surface)
            window.navigationBarColor = colorScheme.surface.toArgb() // O background
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // Asegúrate que AppTypography está definida en Type.kt y usa tus fuentes
        typography = AppTypography,
        // shapes = Shapes, // Comentado o eliminado si no tienes Shapes.kt
        content = content
    )
}