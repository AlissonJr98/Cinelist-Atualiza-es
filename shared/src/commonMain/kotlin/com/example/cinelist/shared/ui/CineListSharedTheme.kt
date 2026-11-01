package com.example.cinelist.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FundoCineList = Color(0xFF0B0F19)
val SuperficieCard = Color(0xFF131B2E)
val SuperficieElevada = Color(0xFF1E293B)
val AmareloCine = Color(0xFFFACC15)
val CianoNeon = Color(0xFF06B6D4)
val RosaNeon = Color(0xFFEC4899)
val VerdeConcluido = Color(0xFF10B981)
val TextoPrincipal = Color(0xFFF8FAFC)
val TextoSecundario = Color(0xFF94A3B8)

private val DarkColorScheme = darkColorScheme(
    primary = AmareloCine,
    secondary = CianoNeon,
    tertiary = RosaNeon,
    background = FundoCineList,
    surface = SuperficieCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextoPrincipal,
    onSurface = TextoPrincipal
)

@Composable
fun CineListSharedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
