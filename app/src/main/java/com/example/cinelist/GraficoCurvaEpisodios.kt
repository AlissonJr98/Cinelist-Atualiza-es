package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GraficoCurvaEpisodios(
    episodios: List<TmdbEpisodioItem>,
    numeroTemporada: Int,
    modifier: Modifier = Modifier
) {
    if (episodios.isEmpty()) return

    val episodiosComNota = remember(episodios) {
        episodios.filter { it.avaliacao > 0.0 }
    }
    if (episodiosComNota.isEmpty()) return

    val mediaTemporada = remember(episodiosComNota) {
        val total = episodiosComNota.sumOf { it.avaliacao }
        total / episodiosComNota.size
    }

    val melhorEp = remember(episodiosComNota) {
        episodiosComNota.maxByOrNull { it.avaliacao }
    }

    var epSelecionado by remember { mutableStateOf<TmdbEpisodioItem?>(melhorEp) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📊 ${"CURVA DE QUALIDADE".t()} (T$numeroTemporada)",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Variação de notas por episódio (Estilo IMDb)".t(),
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${"Média".t()}: ${String.format(java.util.Locale.US, "%.1f", mediaTemporada)} ⭐",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            // Gráfico Interativo em Linha/Pontos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF060913))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val minNota = 5.0
                    val maxNota = 10.0
                    val stepX = if (episodiosComNota.size > 1) w / (episodiosComNota.size - 1) else w / 2

                    // Linha guia média (7.0 e 9.0)
                    val yMedia = h - (((7.0 - minNota) / (maxNota - minNota)) * h).toFloat()
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(0f, yMedia),
                        end = Offset(w, yMedia),
                        strokeWidth = 1f
                    )

                    val pathLinha = Path()
                    val pontos = mutableListOf<Offset>()

                    episodiosComNota.forEachIndexed { i, ep ->
                        val x = if (episodiosComNota.size > 1) i * stepX else w / 2
                        val notaNormalizada = (ep.avaliacao.coerceIn(minNota, maxNota) - minNota) / (maxNota - minNota)
                        val y = (h - (notaNormalizada * h)).toFloat()
                        val ponto = Offset(x, y)
                        pontos.add(ponto)

                        if (i == 0) {
                            pathLinha.moveTo(ponto.x, ponto.y)
                        } else {
                            pathLinha.lineTo(ponto.x, ponto.y)
                        }
                    }

                    // Desenha linha conectora
                    drawPath(
                        path = pathLinha,
                        color = Color(0xFF00E5FF),
                        style = Stroke(width = 4f)
                    )

                    // Desenha pontos
                    pontos.forEachIndexed { i, pt ->
                        val ep = episodiosComNota[i]
                        val corPonto = when {
                            ep.avaliacao >= 9.0 -> Color(0xFFFBBF24)
                            ep.avaliacao >= 8.0 -> Color(0xFF10B981)
                            ep.avaliacao >= 7.0 -> Color(0xFF38BDF8)
                            else -> Color(0xFFF97316)
                        }
                        val selecionado = epSelecionado?.id == ep.id
                        val raio = if (selecionado) 9f else 5.5f

                        drawCircle(
                            color = corPonto,
                            radius = raio,
                            center = pt
                        )
                        if (selecionado) {
                            drawCircle(
                                color = Color.White,
                                radius = 3.5f,
                                center = pt
                            )
                        }
                    }
                }

                // Linha de botões transparentes para clique em cada episódio
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    episodiosComNota.forEach { ep ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { epSelecionado = ep }
                        )
                    }
                }
            }

            // Detalhes do Episódio Selecionado
            epSelecionado?.let { ep ->
                val corDestaque = when {
                    ep.avaliacao >= 9.0 -> Color(0xFFFBBF24)
                    ep.avaliacao >= 8.0 -> Color(0xFF10B981)
                    ep.avaliacao >= 7.0 -> Color(0xFF38BDF8)
                    else -> Color(0xFFF97316)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, corDestaque.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = corDestaque.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "E${ep.numeroEpisodio}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = corDestaque
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ep.nome.ifBlank { "${"Episódio".t()} ${ep.numeroEpisodio}" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (ep == melhorEp) {
                                Text(
                                    text = "👑 Maior avaliação da temporada!".t(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                            } else if (ep.sinopse.isNotBlank()) {
                                Text(
                                    text = ep.sinopse,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", ep.avaliacao)} ⭐",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = corDestaque
                        )
                    }
                }
            }
        }
    }
}
