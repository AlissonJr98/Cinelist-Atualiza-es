package com.example.cinelist.ui.theme

import android.app.Activity

import android.content.Context

import android.content.ContextWrapper

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

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// 🚀 Paleta de Destaque / Identidade Visual

val CineListGold = Color(0xFFFFD700)

val NetflixRed = Color(0xFFE50914)

val MaxPurple = Color(0xFF8B5CF6)

val PrimeCyan = Color(0xFF00E5FF)

val SpotifyGreen = Color(0xFF10B981)

val RosePink = Color(0xFFF43F5E)

@Composable

fun CineListTheme(

darkTheme: Boolean = isSystemInDarkTheme(),

corPersonalizada: String = "CineList", // Cor de Destaque

corFundo: String = "Padrao",           // Fundo do Sistema

corTexto: String = "Padrao",           // Estilo de Texto

dynamicColor: Boolean = false,

content: @Composable () -> Unit

) {

// 🎨 1. Cor Primária de Destaque

val corPrimaria = when (corPersonalizada) {

"Netflix" -> NetflixRed

"Max" -> MaxPurple

"Prime" -> PrimeCyan

"Spotify" -> SpotifyGreen

"Rose" -> RosePink

else -> CineListGold

}

// 🎨 2. Contraste nos Botões Primários (onPrimary)

// Para CineList Gold, texto escuro é ideal. Para vermelho, roxo, rosa, etc., texto branco puro!

val corTextoBotao = if (corPersonalizada == "CineList") Color(0xFF141000) else Color.White

val isDarkScheme = (darkTheme || corFundo != "Claro") && corFundo != "Claro"

// 🎨 3. Paleta de Fundo e Superfícies (Backdrops Cinematográficos)

val (corBg, corSurf, corSurfVar) = when (corFundo) {

"Amoled" -> Triple(

Color(0xFF000000), // Preto absoluto (desliga pixels OLED)

Color(0xFF0C0E14), // Superfície de card sutilmente destacada

Color(0xFF141822)

)

"AzulNoite" -> Triple(

Color(0xFF070C18), // Deep Space Midnight Blue

Color(0xFF0F172A),

Color(0xFF1E293B)

)

"Grafite" -> Triple(

Color(0xFF121316), // Titanium Slate

Color(0xFF1A1C22),

Color(0xFF242730)

)

"Claro" -> Triple(

Color(0xFFF8FAFC), // Fundo Claro Limpo

Color(0xFFFFFFFF), // Cards em Branco Puro

Color(0xFFF1F5F9)  // Variantes em Cinza Suave

)

else -> if (isDarkScheme) {

Triple(

Color(0xFF0D111A), // Padrão CineList: Slate Cósmico escuro

Color(0xFF151C2C),

Color(0xFF1E273D)

)

} else {

Triple(

Color(0xFFF8FAFC),

Color(0xFFFFFFFF),

Color(0xFFF1F5F9)

)

}

}

// 🎨 4. Legibilidade e Contraste do Texto

val corTextoPrincipal = when (corTexto) {

"Branco" -> if (isDarkScheme) Color(0xFFFFFFFF) else Color(0xFF0F172A)

"Quente" -> if (isDarkScheme) Color(0xFFFBF7EE) else Color(0xFF1C1917)

"Titanio" -> if (isDarkScheme) Color(0xFFE2E8F0) else Color(0xFF1E293B)

else -> if (isDarkScheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)

}

val corTextoSecundario = if (isDarkScheme) Color(0xFF94A3B8) else Color(0xFF64748B)

val colorScheme = when {

dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {

val context = LocalContext.current

if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

}

isDarkScheme -> darkColorScheme(

primary = corPrimaria,

onPrimary = corTextoBotao,

primaryContainer = corPrimaria.copy(alpha = 0.2f),

onPrimaryContainer = corPrimaria,

background = corBg,

surface = corSurf,

surfaceVariant = corSurfVar,

onBackground = corTextoPrincipal,

onSurface = corTextoPrincipal,

onSurfaceVariant = corTextoPrincipal.copy(alpha = 0.85f),

secondary = corTextoSecundario,

outline = corTextoSecundario.copy(alpha = 0.25f),

outlineVariant = Color.White.copy(alpha = 0.08f)

)

else -> lightColorScheme(

primary = corPrimaria,

onPrimary = corTextoBotao,

primaryContainer = corPrimaria.copy(alpha = 0.15f),

onPrimaryContainer = corPrimaria,

background = corBg,

surface = corSurf,

surfaceVariant = corSurfVar,

onBackground = corTextoPrincipal,

onSurface = corTextoPrincipal,

onSurfaceVariant = corTextoPrincipal.copy(alpha = 0.85f),

secondary = corTextoSecundario,

outline = corTextoSecundario.copy(alpha = 0.3f),

outlineVariant = Color.Black.copy(alpha = 0.08f)

)

}

val view = LocalView.current

if (!view.isInEditMode) {

SideEffect {

try {

val activity = view.context.findActivity()

val window = activity?.window

if (window != null) {

window.statusBarColor = Color.Transparent.toArgb()

WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkScheme

}

} catch (e: Throwable) {

e.printStackTrace()

}

}

}

MaterialTheme(

colorScheme = colorScheme,

typography = Typography,

content = content

)

}

