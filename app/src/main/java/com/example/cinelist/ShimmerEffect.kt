package com.example.cinelist

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape // 🚀 IMPORTAÇÃO CORRIGIDA AQUI
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun Modifier.shimmer(): Modifier {
    // 1. Configura a transição infinita (o brilho passando sem parar)
    val transicaoInfinita = rememberInfiniteTransition(label = "shimmer")

    val xShimmer = transicaoInfinita.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    // 2. Define os tons de cinza do efeito (fundo escuro, brilho claro no meio)
    val coresShimmer = listOf(
        Color(0xFF242424),
        Color(0xFF3A3A3A),
        Color(0xFF242424),
    )

    // 3. Cria o gradiente linear que se move baseado no valor animado xShimmer
    val brush = Brush.linearGradient(
        colors = coresShimmer,
        start = Offset(xShimmer.value - 300f, xShimmer.value - 300f),
        end = Offset(xShimmer.value, xShimmer.value)
    )

    return this.background(brush)
}