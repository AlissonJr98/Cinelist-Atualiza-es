package com.example.cinelist

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape // 🚀 IMPORTAÇÃO CORRIGIDA AQUI
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

/**
 * Fix #3 — placeholder de carregamento para um card de mídia, no formato de grid (vertical,
 * poster + duas linhas de texto) ou de lista horizontal (retângulo único). Usado enquanto a
 * primeira página de resultados do TMDB ainda não chegou, no lugar de uma tela em branco.
 */
@Composable
fun CardMidiaEsqueleto(modoListaHorizontal: Boolean = false, modifier: Modifier = Modifier) {
    if (modoListaHorizontal) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(10.dp))
                .shimmer(),
            horizontalArrangement = Arrangement.Start
        ) {}
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .shimmer()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Spacer(
                modifier = Modifier
                    .width(60.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
        }
    }
}