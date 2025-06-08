package com.example.appescapetocinema.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.appescapetocinema.R

// Definir las familias de fuentes
val Orbitron = FontFamily(
    Font(R.font.orbitron_regular, FontWeight.Normal), // fontWeight 400
    Font(R.font.orbitron_medium, FontWeight.Medium), // fontWeight 500
    Font(R.font.orbitron_bold, FontWeight.Bold)      // fontWeight 700
)

val Rajdhani = FontFamily(
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold), // fontWeight 600
    Font(R.font.rajdhani_bold, FontWeight.Bold)          // fontWeight 700
)

// Define la tipografía de la aplicación usando estas familias
val AppTypography = Typography(
    // --- DISPLAY (Títulos muy grandes - Rajdhani) ---
    displayLarge = TextStyle( fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp ),
    displayMedium = TextStyle( fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp ),
    displaySmall = TextStyle( fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp ),

    // --- HEADLINE (Títulos importantes - Rajdhani o Orbitron Bold) ---
    headlineLarge = TextStyle( fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp ),
    headlineMedium = TextStyle( fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp ), // Ej: Título Appbar
    headlineSmall = TextStyle( fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp ), // Ej: Títulos de sección?

    // --- TITLE (Subtítulos prominentes - Orbitron Bold/Medium) ---
    titleLarge = TextStyle( fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp ), // Ej: Títulos de sección
    titleMedium = TextStyle( fontFamily = Orbitron, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp ), // Ej: Nombres en tarjetas
    titleSmall = TextStyle( fontFamily = Orbitron, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp ), // Ej: Nombres de usuario reseña

    // --- BODY (Texto principal - Orbitron Regular) ---
    bodyLarge = TextStyle( fontFamily = Orbitron, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp ), // Ej: Sinopsis
    bodyMedium = TextStyle( fontFamily = Orbitron, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp ), // Ej: Texto general, botones
    bodySmall = TextStyle( fontFamily = Orbitron, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp ), // Ej: Metadatos, fecha reseña

    // --- LABEL (Botones, captions - Orbitron Medium/Bold) ---
    labelLarge = TextStyle( fontFamily = Orbitron, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp ),
    labelMedium = TextStyle( fontFamily = Orbitron, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp ), // Ej: Texto botones pequeños
    labelSmall = TextStyle( fontFamily = Orbitron, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp ) // Ej: Géneros, subtítulo persona
)