package com.example.cinelist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700),       // Amarelo CineList
    background = Color(0xFF121212),    // Fundo Escuro
    surface = Color(0xFF1E1E1E),       // Cards Escuros
    onPrimary = Color.Black,
    onBackground = Color.White,        // Texto geral Branco
    onSurface = Color.White,           // Texto dentro de cards Branco
    secondary = Color.Gray             // Texto secundário
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB59400),       // Amarelo mais escuro para leitura
    background = Color(0xFFF5F5F5),    // Fundo Claro Suave
    surface = Color(0xFFFFFFFF),       // Cards Brancos Puro
    onPrimary = Color.White,
    onBackground = Color(0xFF121212),  // Texto geral Escuro
    onSurface = Color(0xFF121212),      // Texto dentro de cards Escuro
    secondary = Color(0xFF666666)      // Texto secundário cinza escuro
)

@Composable
fun CineListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}