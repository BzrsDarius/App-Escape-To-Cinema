package com.example.appescapetocinema.ui.theme // O tu paquete

import androidx.compose.ui.graphics.Color



// --- PALETA "ESCAPE TO CINEMA" ---

// Fondos y Superficies Oscuras
val DarkBackground = Color(0xFF101419)       // Azul muy oscuro casi negro
val DarkSurface = Color(0xFF1A1F24)          // Un poco más claro
val DarkSurfaceVariant = Color(0xFF2C333A)     // Para Cards, Chips, etc.

// Colores Neón de Acento
val NeonPurple = Color(0xFFB266FF)             // Primario (botones principales, indicadores activos)
val NeonMagenta = Color(0xFFFF00FF)           // Secundario (elementos flotantes, selección)
val NeonOrange = Color(0xFFFF9100)            // Terciario (acentos menos importantes)

// Texto y Contenido "Sobre" fondos oscuros
val TextWhite = Color.White                  // Texto principal
val TextGrey = Color(0xFFB0BEC5)             // Texto secundario, iconos inactivos

// Bordes y Divisores
val OutlineGrey = Color(0xFF546E7A)          // Bordes sutiles
val OutlineVariant = Color(0xFF455A64)       // Borde ligeramente más oscuro o variante

// Error
val ErrorRed = Color(0xFFF2B8B5)             // Color del texto/icono de error sobre fondo oscuro
val OnErrorBlack = Color(0xFF601410)           // Color de fondo para un contenedor de error (opcional)

// Colores "Sobre" Acentos (Texto/Iconos encima de Primario/Secundario/Terciario)
// Como nuestros acentos son brillantes, el contenido encima suele ser oscuro
val OnPrimaryDark = Color(0xFF00363D)        // Texto/icono oscuro para poner sobre NeonCyan
val OnSecondaryDark = Color(0xFF3E003E)      // Texto/icono oscuro para poner sobre NeonMagenta
val OnTertiaryDark = Color(0xFF452700)       // Texto/icono oscuro para poner sobre NeonOrange

