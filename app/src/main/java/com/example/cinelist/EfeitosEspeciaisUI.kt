package com.example.cinelist

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ====================================================================
 * 1. FUNDO COM EFEITO 3D (Mesh Gradient + Parallax & Glassmorphism)
 * ====================================================================
 */

/**
 * Modifier de Fundo Glassmorphic com borda de luz metálica/iluminada.
 */
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.08f),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    accentGlowColor: Color = Color(0xFFFFD700).copy(alpha = 0.15f)
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                backgroundColor,
                backgroundColor.copy(alpha = (backgroundColor.alpha * 0.4f).coerceAtLeast(0.02f))
            )
        )
    )
    .border(
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor,
                    accentGlowColor,
                    borderColor.copy(alpha = 0.05f)
                )
            )
        ),
        shape = shape
    )

/**
 * Card Glassmorphic reutilizável com sombra ambiente 3D e borda iluminada.
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
    borderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val cardModifier = modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = primaryColor.copy(alpha = 0.25f),
            spotColor = Color.Black.copy(alpha = 0.5f)
        )
        .glassmorphic(
            shape = shape,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            accentGlowColor = primaryColor.copy(alpha = 0.22f)
        )
        .then(
            if (onClick != null) {
                Modifier.bounceOnClick(onClick = onClick)
            } else Modifier
        )

    Column(
        modifier = cardModifier,
        content = content
    )
}

/**
 * Fundo Mesh Gradient 3D com "orbes" dinâmicos e suporte a Parallax no scroll.
 */
@Composable
fun MeshBackground3D(
    modifier: Modifier = Modifier,
    scrollOffset: Float = 0f,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_orbs")

    // Animação dos orbes volumétricos no espaço 3D
    val animOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1"
    )

    val animOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2"
    )

    val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        // Renderiza os orbes de gradiente volumétrico no canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val parallaxY = (scrollOffset * 0.12f) % height

            // Orbe 1 - Canto superior direito
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor, Color.Transparent),
                    center = Offset(width * 0.85f + animOffset1, height * 0.18f + parallaxY),
                    radius = width * 0.65f
                ),
                center = Offset(width * 0.85f + animOffset1, height * 0.18f + parallaxY),
                radius = width * 0.65f
            )

            // Orbe 2 - Canto inferior esquerdo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryColor, Color.Transparent),
                    center = Offset(width * 0.15f + animOffset2, height * 0.72f - parallaxY),
                    radius = width * 0.75f
                ),
                center = Offset(width * 0.15f + animOffset2, height * 0.72f - parallaxY),
                radius = width * 0.75f
            )
        }

        content()
    }
}

/**
 * ====================================================================
 * 2. TOPBAR MODERNA E TRANSLÚCIDA (Frosted Glass + Linha de Luz)
 * ====================================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarModernaTranslucida(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(0.dp) // ativa renderizacao suave no android 12+
                    } else Modifier
                )
                .background(surfaceColor)
        ) {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions()
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }

        // Borda de Luz Inferior com gradiente metálico/brilhante
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.8f),
                            primaryColor,
                            primaryColor.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * ====================================================================
 * 3. ÍCONES COM MICRO-ANIMAÇÕES VIVAS (Bouncing, Pulse & Rotation)
 * ====================================================================
 */

/**
 * Modifier de Bouncing & Scale (Efeito elástico ao clicar)
 */
@Composable
fun Modifier.bounceOnClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.84f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_scale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Botão com Ícone Bouncy (Compressão e Mola elástica ao toque)
 */
@Composable
fun IconeBouncyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 38.dp, minHeight = 38.dp)
            .bounceOnClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Ícone com efeito de Pulsação Contínua (Ideal para o botão de "Match")
 */
@Composable
fun PulseMatchIcon(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_match")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier.scale(if (enabled) pulseScale else 1.0f),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Ícone com Rotação Suave (Ex: Engrenagem de Configurações 90°, Dados de Sorteio 360°)
 */
@Composable
fun IconeAnimadoRotacao(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    anguloRotacaoTotal: Float = 360f,
    content: @Composable () -> Unit
) {
    var estaGirando by remember { mutableStateOf(false) }

    val anguloAnimado by animateFloatAsState(
        targetValue = if (estaGirando) anguloRotacaoTotal else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        finishedListener = {
            estaGirando = false
        },
        label = "rotation_angle"
    )

    Box(
        modifier = modifier
            .sizeIn(minWidth = 38.dp, minHeight = 38.dp)
            .graphicsLayer { rotationZ = anguloAnimado }
            .bounceOnClick {
                estaGirando = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
