package com.example.cinelist.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 🚀 As Nossas Cores de Temas Customizados
val CineListGold = Color(0xFFFFD700)
val NetflixRed = Color(0xFFE50914)
val MaxPurple = Color(0xFF8A2BE2)
val PrimeBlue = Color(0xFF38BDF8)
val SpotifyGreen = Color(0xFF1DB954)

// Cores secundárias base mantidas por segurança (caso estejam no seu Color.kt)
private val DarkColorSchemeBase = darkColorScheme(
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

private val LightColorSchemeBase = lightColorScheme(
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260)
)

@Composable
fun CineListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    corPersonalizada: String = "CineList", // 🚀 Cor de Destaque
    corFundo: String = "Padrao",           // 🚀 Cor de Fundo
    corTexto: String = "Padrao",           // 🚀 Cor da Letra / Texto
    dynamicColor: Boolean = false, // 🚀 Desligado por padrão para respeitar a nossa cor!
    content: @Composable () -> Unit
) {
    // 🎨 Identifica a Cor Principal baseada na string guardada no SharedPreferences
    val corPrimaria = when (corPersonalizada) {
        "Netflix" -> NetflixRed
        "Max" -> MaxPurple
        "Prime" -> PrimeBlue
        "Spotify" -> SpotifyGreen
        else -> CineListGold
    }

    // 🎨 Identifica a Cor de Fundo (background e surface)
    val (corBg, corSurf) = when (corFundo) {
        "Amoled" -> Color(0xFF000000) to Color(0xFF0E0E0E)
        "AzulNoite" -> Color(0xFF0B132B) to Color(0xFF1C2541)
        "Grafite" -> Color(0xFF181820) to Color(0xFF242432)
        "Claro" -> Color(0xFFF5F5F7) to Color(0xFFFFFFFF)
        else -> if (darkTheme) Color(0xFF121212) to Color(0xFF1E1E1E) else Color(0xFFF5F5F5) to Color(0xFFFFFFFF)
    }

    // 🎨 Identifica a Cor do Texto (onBackground e onSurface)
    val corLetra = when (corTexto) {
        "Branco" -> Color(0xFFFFFFFF)
        "Amarelo" -> Color(0xFFFFD700)
        "Verde" -> Color(0xFF00FF7F)
        "Ciano" -> Color(0xFF00E5FF)
        "Rosa" -> Color(0xFFFF69B4)
        else -> if (darkTheme || corFundo != "Claro") Color(0xFFFFFFFF) else Color(0xFF121212)
    }

    val isDarkScheme = darkTheme || corFundo != "Claro"

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDarkScheme -> darkColorScheme(
            primary = corPrimaria,
            onPrimary = Color.Black,
            primaryContainer = corPrimaria.copy(alpha = 0.2f),
            onPrimaryContainer = corPrimaria,
            background = corBg,
            surface = corSurf,
            surfaceVariant = corSurf.copy(alpha = 0.85f),
            onBackground = corLetra,
            onSurface = corLetra,
            onSurfaceVariant = corLetra.copy(alpha = 0.8f),
            secondary = corLetra.copy(alpha = 0.65f)
        )
        else -> lightColorScheme(
            primary = corPrimaria,
            onPrimary = Color.White,
            primaryContainer = corPrimaria.copy(alpha = 0.2f),
            onPrimaryContainer = corPrimaria,
            background = corBg,
            surface = corSurf,
            surfaceVariant = corSurf,
            onBackground = corLetra,
            onSurface = corLetra,
            onSurfaceVariant = corLetra.copy(alpha = 0.8f),
            secondary = corLetra.copy(alpha = 0.65f)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}