package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


object PaletteColorCache {
    val cache = android.util.LruCache<String, Int>(250)
}

fun formatarDataRelativa(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val agora = System.currentTimeMillis()
    val diffMs = agora - timestamp
    if (diffMs < 0) return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))

    val calAgora = Calendar.getInstance().apply { timeInMillis = agora }
    val calData = Calendar.getInstance().apply { timeInMillis = timestamp }

    val mesmoAno = calAgora.get(Calendar.YEAR) == calData.get(Calendar.YEAR)
    if (mesmoAno) {
        val diffDias = calAgora.get(Calendar.DAY_OF_YEAR) - calData.get(Calendar.DAY_OF_YEAR)
        return when {
            diffDias == 0 -> "Hoje".t()
            diffDias == 1 -> "Ontem".t()
            diffDias in 2..6 -> "Há $diffDias dias".t()
            else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
        }
    }
    return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
}

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
    modoListaHorizontal: Boolean = false,
    onAceitarPendente: (() -> Unit)? = null,
    onRecusarPendente: (() -> Unit)? = null,
    onProcessarRecusado: ((Boolean) -> Unit)? = null
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
            onAdicionarRapido = onAdicionarRapido,
            onAceitarPendente = onAceitarPendente,
            onRecusarPendente = onRecusarPendente,
            onProcessarRecusado = onProcessarRecusado
        )
    } else {
        ConteudoItemMidiaCard(
            midia = midia,
            onClick = onClick,
            onIncrementarEpisodio = onIncrementarEpisodio,
            onDeletar = onDeletar,
            onAlternarStatusConcluido = onAlternarStatusConcluido,
            onAlternarFavorito = onAlternarFavorito,
            jaAdicionado = jaAdicionado,
            onAdicionarRapido = onAdicionarRapido,
            onAceitarPendente = onAceitarPendente,
            onRecusarPendente = onRecusarPendente,
            onProcessarRecusado = onProcessarRecusado
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConteudoItemMidiaCard(
    midia: Midia,
    onClick: () -> Unit,
    onIncrementarEpisodio: (() -> Unit)? = null,
    onDeletar: (() -> Unit)? = null,
    onAlternarStatusConcluido: (() -> Unit)? = null,
    onAlternarFavorito: (() -> Unit)? = null,
    jaAdicionado: Boolean = false,
    onAdicionarRapido: (() -> Unit)? = null,
    onAceitarPendente: (() -> Unit)? = null,
    onRecusarPendente: (() -> Unit)? = null,
    onProcessarRecusado: ((Boolean) -> Unit)? = null
) {
    val ehPendente = midia.status == "Pendente"
    val ehRecusado = midia.status == "Recusado"
    val statusEnum = midia.obterStatusEnum()
    val tituloExibicao = MidiaLocalizationManager.obterTituloLocalizado(midia)
    val capaExibicao = MidiaLocalizationManager.obterCapaLocalizada(midia)

    val uidAtual = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val fuiEuQueSugeri = midia.adicionadoPor.startsWith(uidAtual)
    val nomeSugeridor = midia.adicionadoPor.substringAfter("_", "Alguém").substringBefore("@")

    if (ehRecusado && !fuiEuQueSugeri) return

    val corStatus = when (statusEnum) {
        StatusMidia.ASSISTINDO -> Color(0xFF00BFFF)
        StatusMidia.CONCLUIDO -> Color(0xFF32CD32)
        StatusMidia.DESCOBRIR -> Color(0xFFFF9800)
        else -> if (ehPendente) Color(0xFFFFC107) else if (ehRecusado) Color(0xFFFF4C4C) else Color(0xFF888888)
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

    val contexto = LocalContext.current
    var corCapa by remember { mutableStateOf(Color.Transparent) }
    val corFundoAnimada by animateColorAsState(targetValue = corCapa, animationSpec = tween(800), label = "corPaletteAnimada")

    // 🚀 Estado para mostrar o Menu de Opções no Toque Longo
    var mostrarOpcoes by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = {
                    if (onDeletar != null || onAlternarStatusConcluido != null) {
                        mostrarOpcoes = true
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(corFundoAnimada.copy(alpha = 0.35f), Color.Transparent),
                            startY = 0f,
                            endY = 400f
                        )
                    )
            )

            // 🚀 MENU ELEGANTE NO TOPO DO CARTÃO (Para concluir/excluir via Toque Longo)
            DropdownMenu(
                expanded = mostrarOpcoes,
                onDismissRequest = { mostrarOpcoes = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (onAlternarStatusConcluido != null && !ehPendente && !ehRecusado) {
                    val isConcluido = statusEnum == StatusMidia.CONCLUIDO
                    DropdownMenuItem(
                        text = { Text(if (isConcluido) "Reabrir Título" else "Marcar como Concluído", fontWeight = FontWeight.Bold) },
                        onClick = {
                            mostrarOpcoes = false
                            onAlternarStatusConcluido.invoke()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isConcluido) Icons.Default.PlayArrow else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isConcluido) Color(0xFFFFD700) else Color(0xFF4CAF50)
                            )
                        }
                    )
                }
                if (onDeletar != null) {
                    DropdownMenuItem(
                        text = { Text("Excluir Mídia", color = Color(0xFFFF4C4C), fontWeight = FontWeight.Bold) },
                        onClick = {
                            mostrarOpcoes = false
                            onDeletar.invoke()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4C4C)) }
                    )
                }
            }

            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!capaExibicao.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(contexto)
                                .data(capaExibicao)
                                .crossfade(true)
                                .allowHardware(false)
                                .build(),
                            contentDescription = "Capa de $tituloExibicao",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onSuccess = { state ->
                                val drawable = state.result.drawable
                                val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                bitmap?.let { bmp ->
                                    val urlCapa = capaExibicao
                                    val corEmCache = if (urlCapa.isNotBlank()) PaletteColorCache.cache.get(urlCapa) else null
                                    if (corEmCache != null) {
                                        corCapa = Color(corEmCache)
                                    } else {
                                        androidx.palette.graphics.Palette.from(bmp).generate { palette ->
                                            val swatch = palette?.vibrantSwatch ?: palette?.dominantSwatch ?: palette?.mutedSwatch
                                            swatch?.rgb?.let { colorInt ->
                                                if (urlCapa.isNotBlank()) PaletteColorCache.cache.put(urlCapa, colorInt)
                                                corCapa = Color(colorInt)
                                            }
                                        }
                                    }
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

                    if (statusEnum != StatusMidia.DESCOBRIR) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(corStatus, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (ehPendente) "⏳ PENDENTE" else if (ehRecusado) "❌ RECUSADO" else statusEnum.valor,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!ehPendente && !ehRecusado) {
                        if (onAlternarFavorito != null && statusEnum != StatusMidia.DESCOBRIR) {
                            BotaoFavoritoAnimado(
                                favorito = midia.favorito,
                                onToggle = { onAlternarFavorito() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            )
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
                                    .bounceOnClick(enabled = !jaAdicionado) {
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
                    }

                    if (!ehPendente && !ehRecusado && statusEnum != StatusMidia.CONCLUIDO && statusEnum != StatusMidia.DESCOBRIR) {
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
                        } else if (ehSerieOuAnime && onIncrementarEpisodio != null && statusEnum == StatusMidia.ASSISTINDO) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                color = Color(0xEE000000)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatarTemporadaEpisodio(midia.temporadaAtual, midia.episodioAtual),
                                        color = Color(0xFFFFD700),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = { onIncrementarEpisodio() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Avançar Episódio".t(),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "+1 Ep".t(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold
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
                                    text = formatarTemporadaEpisodio(midia.temporadaAtual, midia.episodioAtual),
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
                            text = tituloExibicao,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (midia.favorito && !ehPendente && !ehRecusado) {
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
                        text = if (generoEhValido(midia.genero)) "${midia.tipo} • ${midia.genero}" else midia.tipo,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (midia.isCasal && midia.adicionadoPor.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (ehPendente) "Sugerido por: $nomeSugeridor ⏳" else if (ehRecusado) "Parceiro(a) recusou 💔" else "Adicionado por: $nomeSugeridor ❤️",
                            color = if (ehPendente) Color(0xFFFFC107) else if (ehRecusado) Color(0xFFFF4C4C) else Color(0xFFFF69B4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!ehPendente && !ehRecusado) {
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
                                    text = "Sem avaliação".t(),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    val servicoStreaming = remember(plataformaPrincipal) { StreamingManager.identificarServico(plataformaPrincipal) }
                    if (servicoStreaming != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(servicoStreaming.corHex).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(servicoStreaming.corHex).copy(alpha = 0.65f)),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(servicoStreaming.emoji, fontSize = 9.sp)
                                Text(
                                    text = servicoStreaming.nome,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(servicoStreaming.corHex)
                                )
                            }
                        }
                    } else if (plataformaPrincipal.isNotBlank() && !plataformaPrincipal.equals("Outros", ignoreCase = true)) {
                        Text(
                            text = "🍿 $plataformaPrincipal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (statusEnum == StatusMidia.ASSISTINDO && midia.atualizadoEm > 0L) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Visto: ${formatarDataRelativa(midia.atualizadoEm)}".t(),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (statusEnum == StatusMidia.CONCLUIDO) {
                        if (midia.dataConclusao > 0L) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))

                                val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                                val dataStr = sdf.format(Date(midia.dataConclusao))

                                val textoFinal = if (midia.isCasal && midia.concluidoPor.isNotBlank()) {
                                    "Em $dataStr por ${midia.concluidoPor}"
                                } else {
                                    "Finalizado em $dataStr"
                                }

                                Text(
                                    text = textoFinal,
                                    fontSize = 10.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (ehPendente) {
                        if (!fuiEuQueSugeri) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRecusarPendente?.invoke() },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Recusar", fontSize = 11.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { onAceitarPendente?.invoke() },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32CD32)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Aceitar", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aguardando aprovação do parceiro(a)...", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    if (ehRecusado && fuiEuQueSugeri) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onProcessarRecusado?.invoke(false) },
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Descartar", fontSize = 11.sp, color = Color(0xFFFF4C4C))
                            }
                            Button(
                                onClick = { onProcessarRecusado?.invoke(true) },
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Para Minha Lista", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
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
    onAdicionarRapido: (() -> Unit)? = null,
    onAceitarPendente: (() -> Unit)? = null,
    onRecusarPendente: (() -> Unit)? = null,
    onProcessarRecusado: ((Boolean) -> Unit)? = null
) {
    if (onDeletar == null && onAlternarStatusConcluido == null) {
        ConteudoItemMidiaCardHorizontal(
            midia = midia,
            onClick = onClick,
            onIncrementarEpisodio = onIncrementarEpisodio,
            onAlternarFavorito = onAlternarFavorito,
            jaAdicionado = jaAdicionado,
            onAdicionarRapido = onAdicionarRapido,
            onAceitarPendente = onAceitarPendente,
            onRecusarPendente = onRecusarPendente,
            onProcessarRecusado = onProcessarRecusado
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

        val statusEnum = midia.obterStatusEnum()

        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            enableDismissFromStartToEnd = onAlternarStatusConcluido != null && midia.status != "Pendente" && midia.status != "Recusado",
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
                                contentDescription = "Concluir".t(),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (statusEnum == StatusMidia.CONCLUIDO) "Reabrir".t() else "Concluir".t(),
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
                                text = "Excluir".t(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir".t(),
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
                onAdicionarRapido = onAdicionarRapido,
                onAceitarPendente = onAceitarPendente,
                onRecusarPendente = onRecusarPendente,
                onProcessarRecusado = onProcessarRecusado
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
    onAdicionarRapido: (() -> Unit)? = null,
    onAceitarPendente: (() -> Unit)? = null,
    onRecusarPendente: (() -> Unit)? = null,
    onProcessarRecusado: ((Boolean) -> Unit)? = null
) {
    val ehPendente = midia.status == "Pendente"
    val ehRecusado = midia.status == "Recusado"

    val statusEnum = midia.obterStatusEnum()
    val tituloExibicao = MidiaLocalizationManager.obterTituloLocalizado(midia)
    val capaExibicao = MidiaLocalizationManager.obterCapaLocalizada(midia)

    val uidAtual = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val fuiEuQueSugeri = midia.adicionadoPor.startsWith(uidAtual)
    val nomeSugeridor = midia.adicionadoPor.substringAfter("_", "Alguém").substringBefore("@")

    if (ehRecusado && !fuiEuQueSugeri) return

    val corStatus = when (statusEnum) {
        StatusMidia.ASSISTINDO -> Color(0xFF00BFFF)
        StatusMidia.CONCLUIDO -> Color(0xFF32CD32)
        StatusMidia.DESCOBRIR -> Color(0xFFFF9800)
        else -> if (ehPendente) Color(0xFFFFC107) else if (ehRecusado) Color(0xFFFF4C4C) else Color(0xFF888888)
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

    val contexto = LocalContext.current
    var corCapa by remember { mutableStateOf(Color.Transparent) }
    val corFundoAnimada by animateColorAsState(targetValue = corCapa, animationSpec = tween(800), label = "corPaletteHorizontal")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )
            .bounceOnClick { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(corFundoAnimada.copy(alpha = 0.35f), Color.Transparent),
                            startX = 0f,
                            endX = 500f
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (ehPendente || ehRecusado) 140.dp
                        else if (ehSerieOuAnime && statusEnum == StatusMidia.ASSISTINDO) 128.dp
                        else 110.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(85.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!capaExibicao.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(contexto)
                                .data(capaExibicao)
                                .crossfade(true)
                                .allowHardware(false)
                                .build(),
                            contentDescription = "Capa de $tituloExibicao",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onSuccess = { state ->
                                val drawable = state.result.drawable
                                val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                bitmap?.let { bmp ->
                                    val urlCapa = capaExibicao
                                    val corEmCache = if (urlCapa.isNotBlank()) PaletteColorCache.cache.get(urlCapa) else null
                                    if (corEmCache != null) {
                                        corCapa = Color(corEmCache)
                                    } else {
                                        androidx.palette.graphics.Palette.from(bmp).generate { palette ->
                                            val swatch = palette?.vibrantSwatch ?: palette?.dominantSwatch ?: palette?.mutedSwatch
                                            swatch?.rgb?.let { colorInt ->
                                                if (urlCapa.isNotBlank()) PaletteColorCache.cache.put(urlCapa, colorInt)
                                                corCapa = Color(colorInt)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "SEM CAPA",
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }

                    if (!ehPendente && !ehRecusado) {
                        if (onAlternarFavorito != null && statusEnum != StatusMidia.DESCOBRIR) {
                            BotaoFavoritoAnimado(
                                favorito = midia.favorito,
                                onToggle = { onAlternarFavorito() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp),
                                tamanhoIcone = 15.dp
                            )
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
                            text = tituloExibicao,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (statusEnum != StatusMidia.DESCOBRIR) {
                            Box(
                                modifier = Modifier
                                    .background(corStatus, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (ehPendente) "⏳ PENDENTE" else if (ehRecusado) "❌ RECUSADO" else statusEnum.valor,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (generoEhValido(midia.genero)) "${midia.tipo} • ${midia.genero}" else midia.tipo,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val servico = remember(plataformaPrincipal) { StreamingManager.identificarServico(plataformaPrincipal) }
                            if (servico != null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(servico.corHex).copy(alpha = 0.16f),
                                    border = BorderStroke(0.8.dp, Color(servico.corHex).copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "${servico.emoji} ${servico.nome}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(servico.corHex),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            } else if (plataformaPrincipal.isNotBlank() && !plataformaPrincipal.equals("Outros", ignoreCase = true)) {
                                Text(
                                    text = "• 🍿 $plataformaPrincipal",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp
                                )
                            }
                        }                        
                        if (midia.isCasal && midia.adicionadoPor.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (ehPendente) "Sugerido por: $nomeSugeridor ⏳" else if (ehRecusado) "Parceiro(a) recusou assistir 💔" else "Adicionado por: $nomeSugeridor ❤️",
                                color = if (ehPendente) Color(0xFFFFC107) else if (ehRecusado) Color(0xFFFF4C4C) else Color(0xFFFF69B4),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (statusEnum == StatusMidia.ASSISTINDO && midia.atualizadoEm > 0L) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Visto: ${formatarDataRelativa(midia.atualizadoEm)}".t(),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (statusEnum == StatusMidia.CONCLUIDO) {
                        if (midia.dataConclusao > 0L) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))

                                val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                                val dataStr = sdf.format(Date(midia.dataConclusao))

                                val textoFinal = if (midia.isCasal && midia.concluidoPor.isNotBlank()) {
                                    "Em $dataStr por ${midia.concluidoPor}"
                                } else {
                                    "Finalizado em $dataStr"
                                }

                                Text(
                                    text = textoFinal,
                                    fontSize = 10.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (ehPendente) {
                        if (!fuiEuQueSugeri) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRecusarPendente?.invoke() },
                                    modifier = Modifier.weight(1f).height(28.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Recusar", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { onAceitarPendente?.invoke() },
                                    modifier = Modifier.weight(1f).height(28.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32CD32)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Aceitar", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        } else {
                            Text("Aguardando aprovação...", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    } else if (ehRecusado && fuiEuQueSugeri) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onProcessarRecusado?.invoke(false) },
                                modifier = Modifier.weight(1f).height(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Descartar", fontSize = 10.sp, color = Color(0xFFFF4C4C))
                            }
                            Button(
                                onClick = { onProcessarRecusado?.invoke(true) },
                                modifier = Modifier.weight(1f).height(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Para Minha Lista", fontSize = 9.sp, color = Color.White)
                            }
                        }
                    } else {
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
                                        text = "Sem avaliação".t(),
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (ehSerieOuAnime && onIncrementarEpisodio != null && statusEnum == StatusMidia.ASSISTINDO) {
                                Button(
                                    onClick = { onIncrementarEpisodio() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Avançar Episódio".t(),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${formatarTemporadaEpisodio(midia.temporadaAtual, midia.episodioAtual)} (+1)",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 12.sp,
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