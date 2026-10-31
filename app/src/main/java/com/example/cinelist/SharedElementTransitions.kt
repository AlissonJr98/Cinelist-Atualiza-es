package com.example.cinelist

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Modificador para aplicar Shared Element Transition em pôsteres de filmes/séries.
 * Faz com que a imagem do pôster transicione suavemente entre a tela de origem (Lista, Destaques, etc.)
 * e a tela de destino (TelaDetalhes), expandindo de forma cinematográfica.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedPosterElement(key: String): Modifier {
    if (key.isBlank()) return this
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedVisibilityScope.current ?: return this

    val boundsTransform = BoundsTransform { _, _ ->
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    }

    return with(sharedScope) {
        this@sharedPosterElement.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = boundsTransform
        )
    }
}
