package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@Composable
fun DialogoRankingAmigos(
    viewModel: MidiaViewModel,
    minhasMidias: List<Midia>,
    onDispensar: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val ranking by viewModel.rankingSemanal.collectAsState()
    val carregando by viewModel.carregandoRanking.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.carregarRankingSemanal(minhasMidias)
    }

    Dialog(
        onDismissRequest = onDispensar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0B0F1C),
            border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header com Troféu e Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "RANKING SEMANAL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF59E0B),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Disputa de Maratonas da Semana",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = onDispensar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8))
                    }
                }

                if (carregando) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFF59E0B))
                    }
                } else if (ranking.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum dado encontrado para o ranking ainda.\nAdicione amigos e conclua títulos para pontuar!",
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // PÓDIO ANIMADO (TOP 3)
                    val primeiro = ranking.getOrNull(0)
                    val segundo = ranking.getOrNull(1)
                    val terceiro = ranking.getOrNull(2)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // 2º LUGAR (Prata)
                        if (segundo != null) {
                            ItemPodio(item = segundo, altura = 100.dp, corBase = Color(0xFF94A3B8), corTrofeu = Color(0xFFE2E8F0))
                        } else {
                            Spacer(modifier = Modifier.width(70.dp))
                        }

                        // 1º LUGAR (Ouro)
                        if (primeiro != null) {
                            ItemPodio(item = primeiro, altura = 135.dp, corBase = Color(0xFFF59E0B), corTrofeu = Color(0xFFFDE68A), ehCampeao = true)
                        }

                        // 3º LUGAR (Bronze)
                        if (terceiro != null) {
                            ItemPodio(item = terceiro, altura = 85.dp, corBase = Color(0xFFB45309), corTrofeu = Color(0xFFFDBA74))
                        } else {
                            Spacer(modifier = Modifier.width(70.dp))
                        }
                    }

                    // Card do Usuário Atual (Destaque pessoal)
                    val meuStatus = ranking.firstOrNull { it.souEu }
                    if (meuStatus != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = meuStatus.medalha,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Column {
                                        Text(
                                            text = "Sua Posição",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E5FF)
                                        )
                                        Text(
                                            text = "${meuStatus.titulosConcluidos} títulos concluídos nesta semana",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "${meuStatus.pontos} pts",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // LISTA COMPLETA RANQUEADA
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(ranking, key = { index, item -> "${item.uid}_$index" }) { _, item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (item.souEu) Color(0xFF1A2338) else Color(0xFF111827),
                                border = BorderStroke(
                                    1.dp,
                                    if (item.souEu) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFF1F2937)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = item.medalha,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFCBD5E1),
                                            modifier = Modifier.width(28.dp),
                                            textAlign = TextAlign.Center
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF334155),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            if (item.fotoUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = item.fotoUrl,
                                                    contentDescription = item.nome,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = item.nome.take(1).uppercase(),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = item.nome,
                                                fontWeight = if (item.souEu) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (item.souEu) Color(0xFF00E5FF) else Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.titulosConcluidos} concluídos • ~${String.format(java.util.Locale.US, "%.1f", item.horasEstimadas)}h",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${item.pontos} pts",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFF59E0B)
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

@Composable
fun ItemPodio(
    item: ItemLeaderboard,
    altura: androidx.compose.ui.unit.Dp,
    corBase: Color,
    corTrofeu: Color,
    ehCampeao: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Coroa para o 1º colocado
        if (ehCampeao) {
            Text("👑", fontSize = 16.sp)
        }

        // Avatar com borda
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E293B),
            border = BorderStroke(2.dp, corBase),
            modifier = Modifier.size(if (ehCampeao) 48.dp else 40.dp)
        ) {
            if (item.fotoUrl.isNotBlank()) {
                AsyncImage(
                    model = item.fotoUrl,
                    contentDescription = item.nome,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.nome.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = if (ehCampeao) 18.sp else 14.sp
                    )
                }
            }
        }

        Text(
            text = item.nome.split(" ").firstOrNull() ?: item.nome,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (item.souEu) Color(0xFF00E5FF) else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${item.pontos} pts",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = corBase
        )

        // Coluna do Pódio
        Surface(
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            color = corBase.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, corBase.copy(alpha = 0.5f)),
            modifier = Modifier
                .width(if (ehCampeao) 82.dp else 70.dp)
                .height(altura)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = item.medalha,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
