package com.example.cinelist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemMidiaCard(
    midia: Midia,
    onClick: () -> Unit,
    onIncrementarEpisodio: (() -> Unit)? = null,
    onDeletar: (() -> Unit)? = null,
    onAlternarStatusConcluido: (() -> Unit)? = null,
    onAlternarFavorito: (() -> Unit)? = null,
    jaAdicionado: Boolean = false,
    onAdicionarRapido: (() -> Unit)? = null,
    modoListaHorizontal: Boolean = false
) {
    if (modoListaHorizontal) {
        ItemMidiaCardHorizontalComSwipe(
            midia = midia,
            onClick = onClick,
            onIncrementarEpisodio = onIncrementarEpisodio,
            onDeletar = onDeletar,
            onAlternarStatusConcluido = onAlternarStatusConcluido,
            onAlternarFavorito = onAlternarFavorito,
            jaAdicionado = jaAdicionado,
            onAdicionarRapido = onAdicionarRapido
        )
        return
    }

    if (onDeletar == null && onAlternarStatusConcluido == null) {
        ConteudoItemMidiaCard(
            midia = midia,
            onClick = onClick,
            onIncrementarEpisodio = onIncrementarEpisodio,
            onAlternarFavorito = onAlternarFavorito,
            jaAdicionado = jaAdicionado,
            onAdicionarRapido = onAdicionarRapido
        )
    } else {
        val currentOnDeletar by rememberUpdatedState(onDeletar)
        val currentOnAlternarConcluido by rememberUpdatedState(onAlternarStatusConcluido)

        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { valorDismiss ->
                when (valorDismiss) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        currentOnDeletar?.invoke()
                        false
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        currentOnAlternarConcluido?.invoke()
                        false
                    }
                    SwipeToDismissBoxValue.Settled -> false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            enableDismissFromStartToEnd = onAlternarStatusConcluido != null,
            enableDismissFromEndToStart = onDeletar != null,
            backgroundContent = {
                val direcao = dismissState.dismissDirection
                val corFundo by animateColorAsState(
                    targetValue = when (direcao) {
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32)
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFD32F2F)
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                    },
                    label = "cor_swipe"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(corFundo)
                        .padding(horizontal = 16.dp),
                    contentAlignment = when (direcao) {
                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                        else -> Alignment.CenterEnd
                    }
                ) {
                    if (direcao == SwipeToDismissBoxValue.StartToEnd) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Concluir",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (midia.status == "Concluído") "Reabrir" else "Concluir",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else if (direcao == SwipeToDismissBoxValue.EndToStart) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Excluir",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        ) {
            ConteudoItemMidiaCard(
                midia = midia,
                onClick = onClick,
                onIncrementarEpisodio = onIncrementarEpisodio,
                onAlternarFavorito = onAlternarFavorito,
                jaAdicionado = jaAdicionado,
                onAdicionarRapido = onAdicionarRapido
            )
        }
    }
}

@Composable
private fun ConteudoItemMidiaCard(
    midia: Midia,
    onClick: () -> Unit,
    onIncrementarEpisodio: (() -> Unit)? = null,
    onAlternarFavorito: (() -> Unit)? = null,
    jaAdicionado: Boolean = false,
    onAdicionarRapido: (() -> Unit)? = null
) {
    val corStatus = when (midia.status) {
        "Assistindo" -> Color(0xFF00BFFF)
        "Concluído" -> Color(0xFF32CD32)
        "Descobrir" -> Color(0xFFFF9800)
        else -> Color(0xFF888888)
    }

    val ehSerieOuAnime = midia.tipo.equals("Série", ignoreCase = true) ||
            midia.tipo.equals("Anime", ignoreCase = true) ||
            midia.tipo.equals("Novela", ignoreCase = true) ||
            midia.tipo.equals("Dorama", ignoreCase = true)

    val plataformaPrincipal = remember(midia.plataforma, midia.tipo) {
        val limpa = midia.plataforma.trim()
        if (limpa.isBlank() || limpa.equals("Não Informado", ignoreCase = true) || limpa.equals("Outros", ignoreCase = true)) {
            if (ehSerieOuAnime) "TV / Original" else "Cinema"
        } else {
            limpa.split(",").firstOrNull()?.trim()?.ifBlank { if (ehSerieOuAnime) "TV / Original" else "Cinema" } ?: (if (ehSerieOuAnime) "TV / Original" else "Cinema")
        }
    }

    val colecaoExibicao = if (midia.listaCustomizada.isBlank()) "Geral" else midia.listaCustomizada

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (!midia.imagemCapa.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = midia.imagemCapa,
                        contentDescription = "Capa de ${midia.titulo}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SEM IMAGEM",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                } else {
                    Text(
                        text = "SEM CAPA",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (midia.status != "Descobrir") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(corStatus, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = midia.status,
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (onAlternarFavorito != null && midia.status != "Descobrir") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onAlternarFavorito() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (midia.favorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (midia.favorito) "Remover dos favoritos" else "Favoritar",
                            tint = if (midia.favorito) Color(0xFFFF3366) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onAdicionarRapido != null || jaAdicionado) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (jaAdicionado) Color(0xFF2E7D32).copy(alpha = 0.9f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                            .clickable(enabled = !jaAdicionado) {
                                onAdicionarRapido?.invoke()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (jaAdicionado) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = if (jaAdicionado) "Já Adicionado" else "Adicionar à Lista",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (midia.status != "Concluído" && midia.status != "Descobrir") {
                    if (!ehSerieOuAnime && midia.minutoParado > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color(0xCC000000))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Parou em: ${midia.minutoParado} min",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (ehSerieOuAnime && onIncrementarEpisodio != null && midia.status == "Assistindo") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            color = Color(0xDD000000)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "T${midia.temporadaAtual} • Ep ${midia.episodioAtual}",
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onIncrementarEpisodio() },
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Adicionar Episódio",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "1 Ep",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else if (ehSerieOuAnime && (midia.episodioAtual > 1 || midia.temporadaAtual > 1)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color(0xCC000000))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "T${midia.temporadaAtual} • Ep ${midia.episodioAtual}",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = midia.titulo,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (midia.favorito) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorito",
                            tint = Color(0xFFFF3366),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = "${midia.tipo} • $colecaoExibicao",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Indicação sutil de quem adicionou no modo casal
                if (midia.isCasal && midia.adicionadoPor.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val nomeCurto = midia.adicionadoPor.substringBefore("@")
                    Text(
                        text = "Adicionado por: $nomeCurto ❤️",
                        color = Color(0xFFFF69B4),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (midia.nota > 0) {
                        repeat(midia.nota) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Sem avaliação",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🍿 $plataformaPrincipal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemMidiaCardHorizontalComSwipe(
    midia: Midia,
    onClick: () -> Unit,
    onIncrementarEpisodio: (() -> Unit)? = null,
    onDeletar: (() -> Unit)? = null,
    onAlternarStatusConcluido: (() -> Unit)? = null,
    onAlternarFavorito: (() -> Unit)? = null,
    jaAdicionado: Boolean = false,
    onAdicionarRapido: (() -> Unit)? = null
) {
    if (onDeletar == null && onAlternarStatusConcluido == null) {
        ConteudoItemMidiaCardHorizontal(
            midia = midia,
            onClick = onClick,
            onIncrementarEpisodio = onIncrementarEpisodio,
            onAlternarFavorito = onAlternarFavorito,
            jaAdicionado = jaAdicionado,
            onAdicionarRapido = onAdicionarRapido
        )
    } else {
        val currentOnDeletar by rememberUpdatedState(onDeletar)
        val currentOnAlternarConcluido by rememberUpdatedState(onAlternarStatusConcluido)

        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { valorDismiss ->
                when (valorDismiss) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        currentOnDeletar?.invoke()
                        false
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        currentOnAlternarConcluido?.invoke()
                        false
                    }
                    SwipeToDismissBoxValue.Settled -> false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            enableDismissFromStartToEnd = onAlternarStatusConcluido != null,
            enableDismissFromEndToStart = onDeletar != null,
            backgroundContent = {
                val direcao = dismissState.dismissDirection
                val corFundo by animateColorAsState(
                    targetValue = when (direcao) {
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32)
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFD32F2F)
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                    },
                    label = "cor_swipe_horizontal"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(corFundo)
                        .padding(horizontal = 16.dp),
                    contentAlignment = when (direcao) {
                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                        else -> Alignment.CenterEnd
                    }
                ) {
                    if (direcao == SwipeToDismissBoxValue.StartToEnd) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Concluir",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (midia.status == "Concluído") "Reabrir" else "Concluir",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else if (direcao == SwipeToDismissBoxValue.EndToStart) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Excluir",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        ) {
            ConteudoItemMidiaCardHorizontal(
                midia = midia,
                onClick = onClick,
                onIncrementarEpisodio = onIncrementarEpisodio,
                onAlternarFavorito = onAlternarFavorito,
                jaAdicionado = jaAdicionado,
                onAdicionarRapido = onAdicionarRapido
            )
        }
    }
}

@Composable
private fun ConteudoItemMidiaCardHorizontal(
    midia: Midia,
    onClick: () -> Unit,
    onIncrementarEpisodio: (() -> Unit)? = null,
    onAlternarFavorito: (() -> Unit)? = null,
    jaAdicionado: Boolean = false,
    onAdicionarRapido: (() -> Unit)? = null
) {
    val corStatus = when (midia.status) {
        "Assistindo" -> Color(0xFF00BFFF)
        "Concluído" -> Color(0xFF32CD32)
        "Descobrir" -> Color(0xFFFF9800)
        else -> Color(0xFF888888)
    }

    val ehSerieOuAnime = midia.tipo.equals("Série", ignoreCase = true) ||
            midia.tipo.equals("Anime", ignoreCase = true) ||
            midia.tipo.equals("Novela", ignoreCase = true) ||
            midia.tipo.equals("Dorama", ignoreCase = true)

    val plataformaPrincipal = remember(midia.plataforma, midia.tipo) {
        val limpa = midia.plataforma.trim()
        if (limpa.isBlank() || limpa.equals("Não Informado", ignoreCase = true) || limpa.equals("Outros", ignoreCase = true)) {
            if (ehSerieOuAnime) "TV / Original" else "Cinema"
        } else {
            limpa.split(",").firstOrNull()?.trim()?.ifBlank { if (ehSerieOuAnime) "TV / Original" else "Cinema" } ?: (if (ehSerieOuAnime) "TV / Original" else "Cinema")
        }
    }

    val colecaoExibicao = if (midia.listaCustomizada.isBlank()) "Geral" else midia.listaCustomizada

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(75.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (!midia.imagemCapa.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = midia.imagemCapa,
                        contentDescription = "Capa de ${midia.titulo}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "SEM CAPA",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                if (onAlternarFavorito != null && midia.status != "Descobrir") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onAlternarFavorito() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (midia.favorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = if (midia.favorito) Color(0xFFFF3366) else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (onAdicionarRapido != null || jaAdicionado) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (jaAdicionado) Color(0xFF2E7D32).copy(alpha = 0.9f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                            .clickable(enabled = !jaAdicionado) {
                                onAdicionarRapido?.invoke()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (jaAdicionado) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = "Adicionar",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = midia.titulo,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (midia.status != "Descobrir") {
                        Box(
                            modifier = Modifier
                                .background(corStatus, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = midia.status,
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "${midia.tipo} • $colecaoExibicao • 🍿 $plataformaPrincipal",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (midia.isCasal && midia.adicionadoPor.isNotBlank()) {
                        val nomeCurto = midia.adicionadoPor.substringBefore("@")
                        Text(
                            text = "Adicionado por: $nomeCurto ❤️",
                            color = Color(0xFFFF69B4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (midia.nota > 0) {
                            repeat(midia.nota) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Sem avaliação",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (ehSerieOuAnime && onIncrementarEpisodio != null && midia.status == "Assistindo") {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onIncrementarEpisodio() },
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "T${midia.temporadaAtual} Ep ${midia.episodioAtual}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemMidiaCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )
            }
        }
    }
}