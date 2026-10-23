package com.example.cinelist

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay
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
    onClick: (() -> Unit)? = null
): Modifier {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.84f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_scale"
    )

    val base = this.scale(scale)
    return if (onClick != null) {
        base.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
    } else {
        base
    }
}

/**
 * Botão Favorito com Animação de Explosão/Pulso (Burst) e Feedback Háptico.
 */
@Composable
fun BotaoFavoritoAnimado(
    favorito: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    tamanhoIcone: Dp = 18.dp
) {
    val haptic = LocalHapticFeedback.current
    var escalaAlvo by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(favorito) {
        escalaAlvo = 1.38f
        delay(120)
        escalaAlvo = 1f
    }

    val escalaAnimada by animateFloatAsState(
        targetValue = escalaAlvo,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "burst_heart"
    )

    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.65f))
            .border(
                1.dp,
                if (favorito) Color(0xFFFF3366).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f),
                CircleShape
            )
            .bounceOnClick {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } catch (_: Exception) {}
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (favorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (favorito) "Remover dos favoritos" else "Favoritar",
            tint = if (favorito) Color(0xFFFF3366) else Color.White,
            modifier = Modifier
                .size(tamanhoIcone)
                .scale(escalaAnimada)
        )
    }
}

/**
 * Botão com Ícone Bouncy (Compressão e Mola elástica ao toque)
 */
@Composable
fun IconeBouncyIconButton(
    onClick: (() -> Unit)? = null,
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
    onClick: (() -> Unit)? = null,
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
                onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}


/**
 * ====================================================================
 * 4. COMPONENTES CYBER UI (Segmented Tabs, Search HUD, Live Radar)
 * ====================================================================
 */

data class CyberTabItem(
    val titulo: String,
    val icone: ImageVector? = null,
    val badgeCount: Int = 0,
    val badgePonto: Boolean = false
)

/**
 * Ponto neon pulsante com brilho dinâmico para beacons e radares sci-fi.
 */
@Composable
fun PulsarPontoNeon(
    cor: Color = Color(0xFF00FF7F),
    tamanho: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val transicao = rememberInfiniteTransition(label = "pulsar_neon")
    val alfa by transicao.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alfa_neon"
    )

    Box(
        modifier = modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(cor.copy(alpha = alfa))
            .border(1.5.dp, cor, CircleShape)
    )
}

/**
 * Tab Bar Segmentada Flutuante estilo Cyberpunk / Sci-Fi Glassmorphic.
 * Distribui as abas uniformemente na tela com cápsula ativa iluminada.
 */
@Composable
fun CyberSegmentedTabBar(
    tabs: List<CyberTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    corDestaque: Color = Color(0xFF00E5FF)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0F1526).copy(alpha = 0.88f))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        corDestaque.copy(alpha = 0.40f),
                        Color(0xFFFFD700).copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val selecionado = selectedIndex == index

                val backgroundModifier = if (selecionado) {
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    corDestaque.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.06f)
                                )
                            )
                        )
                        .border(1.dp, corDestaque.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                } else {
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Transparent)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(backgroundModifier)
                        .bounceOnClick { onTabSelected(index) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (tab.icone != null) {
                            Icon(
                                imageVector = tab.icone,
                                contentDescription = null,
                                tint = if (selecionado) corDestaque else Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = tab.titulo,
                            fontSize = 12.sp,
                            fontWeight = if (selecionado) FontWeight.Black else FontWeight.SemiBold,
                            color = if (selecionado) Color.White else Color.White.copy(alpha = 0.60f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (tab.badgeCount > 0) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF007F).copy(alpha = 0.85f))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (tab.badgeCount > 99) "99+" else tab.badgeCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else if (tab.badgePonto) {
                            Spacer(modifier = Modifier.width(5.dp))
                            PulsarPontoNeon(cor = Color(0xFF00FF7F), tamanho = 7.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab Bar Segmentada com Rolagem Horizontal (perfeita para telas com muitas abas como Perfil).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CyberScrollableSegmentedTabBar(
    tabs: List<CyberTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    corDestaque: Color = Color(0xFF00E5FF)
) {
    val scrollState = rememberScrollState()
    val bringIntoViewRequesters = remember(tabs.size) { List(tabs.size) { BringIntoViewRequester() } }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in tabs.indices) {
            bringIntoViewRequesters[selectedIndex].bringIntoView()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0F1526).copy(alpha = 0.88f))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        corDestaque.copy(alpha = 0.35f),
                        Color(0xFFFFD700).copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selecionado = selectedIndex == index

                val backgroundModifier = if (selecionado) {
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    corDestaque.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.06f)
                                )
                            )
                        )
                        .border(1.dp, corDestaque.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                } else {
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Transparent)
                }

                Box(
                    modifier = Modifier
                        .bringIntoViewRequester(bringIntoViewRequesters[index])
                        .then(backgroundModifier)
                        .bounceOnClick { onTabSelected(index) }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (tab.icone != null) {
                            Icon(
                                imageVector = tab.icone,
                                contentDescription = null,
                                tint = if (selecionado) corDestaque else Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = tab.titulo,
                            fontSize = 12.sp,
                            fontWeight = if (selecionado) FontWeight.Black else FontWeight.SemiBold,
                            color = if (selecionado) Color.White else Color.White.copy(alpha = 0.60f),
                            maxLines = 1
                        )

                        if (tab.badgeCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF007F).copy(alpha = 0.85f))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (tab.badgeCount > 99) "99+" else tab.badgeCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else if (tab.badgePonto) {
                            Spacer(modifier = Modifier.width(5.dp))
                            PulsarPontoNeon(cor = Color(0xFF00FF7F), tamanho = 7.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Terminal de Busca HUD Holográfico com borda brilhante e suporte a histórico.
 */
@Composable
fun CyberSearchHUD(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    corDestaque: Color = Color(0xFF00E5FF),
    onClear: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0C101B).copy(alpha = 0.85f))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                brush = if (isFocused) {
                    Brush.horizontalGradient(
                        listOf(
                            corDestaque,
                            Color(0xFF7C4DFF),
                            corDestaque
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                },
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = if (isFocused) corDestaque else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(19.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(corDestaque),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotBlank()) {
                IconeBouncyIconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpar busca",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Banner de Telemetria e Radar Sci-Fi para Sala Compartilhada ("Assistindo Agora").
 */
@Composable
fun CyberLiveRadarBanner(
    nomeMembro: String,
    tituloMidia: String,
    detalheEpisodio: String = "",
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val corDestaque = Color(0xFF00FF7F)
    val corSecundaria = Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0F172A).copy(alpha = 0.95f),
                        Color(0xFF070C18).copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        corDestaque.copy(alpha = 0.50f),
                        corSecundaria.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null) Modifier.bounceOnClick(onClick = onClick) else Modifier
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(corDestaque.copy(alpha = 0.15f))
                    .border(1.2.dp, corDestaque.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = corDestaque,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PulsarPontoNeon(cor = corDestaque, tamanho = 8.dp)
                    Text(
                        text = "RADAR DA SALA • ASSISTINDO AGORA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        color = corDestaque
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$nomeMembro: $tituloMidia $detalheEpisodio".trim(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Chip holográfico para categorias, coleções e filtros.
 */
@Composable
fun CyberFilterChip(
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    label: String,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
    corDestaque: Color = Color(0xFF00E5FF)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) {
                    Brush.linearGradient(
                        listOf(
                            corDestaque.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    )
                }
            )
            .border(
                width = if (selected) 1.2.dp else 1.dp,
                color = if (selected) corDestaque.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .bounceOnClick(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icone != null) {
                Icon(
                    imageVector = icone,
                    contentDescription = null,
                    tint = if (selected) corDestaque else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.70f)
            )
        }
    }
}
