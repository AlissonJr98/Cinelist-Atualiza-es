package com.example.cinelist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class StreamingLancamentosSecao(
    val servico: ServicoStreaming,
    val titulos: List<TmdbFilme>,
    val totalTitulos: Int = titulos.size
)

/**
 * Seção de Lançamentos e Novidades organizados por Plataforma de Streaming.
 * Inspirado fielmente na interface da Imagem 1 (estilo JustWatch / Novos Lançamentos).
 */
@Composable
fun SecaoLancamentosStreaming(
    secoes: List<StreamingLancamentosSecao>,
    listaDeMidiasLocal: List<Midia>,
    onTmdbItemClique: (TmdbFilme, String) -> Unit,
    onAdicionarClique: (idTmdb: Int, titulo: String, tipo: String, status: String, nota: Int, sinopse: String, imagemCapa: String, genero: String, plataforma: String) -> Unit,
    onConfigurarStreamings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (secoes.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho Principal da Seção
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🆕 LANÇAMENTOS POR STREAMING",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Novas temporadas e filmes nas suas plataformas",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(
                onClick = onConfigurarStreamings,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configurar Meus Streamings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Carrosseis por Plataforma de Streaming
        secoes.forEach { secao ->
            val servico = secao.servico
            val corServico = Color(servico.corHex)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header do Streaming (Ex: [N] NETFLIX  22 TÍTULOS)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge com cor e emoji/letra da marca
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(corServico),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = servico.nome.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Text(
                            text = servico.nome.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Badge com total de títulos
                    Surface(
                        color = corServico.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, corServico.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "${secao.totalTitulos} TÍTULOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (servico.corHex == 0xFF333333L) Color.White else corServico,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Carrossel horizontal de cards estilo Imagem 1
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(secao.titulos, key = { "stream_${servico.id}_${it.idTmdb}" }) { item ->
                        val tipoReal = if (item.ehSerie || item.mediaType.equals("tv", ignoreCase = true)) "Série" else "Filme"
                        val posterUrl = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else ""
                        val jaNaLista = listaDeMidiasLocal.any { it.idTmdb != 0 && it.idTmdb == item.idTmdb }

                        Card(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { onTmdbItemClique(item, tipoReal) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                // Poster com Badge de Temporada / Tipo sobreposto
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E2024))
                                ) {
                                    if (posterUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = posterUrl,
                                            contentDescription = item.titulo,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Badge Topo Estilo JustWatch (Ex: T4 | 8 eps ou Filme)
                                    val badgeTexto = if (tipoReal == "Série") "Série • Nova" else "Filme"
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp),
                                        color = Color.Black.copy(alpha = 0.78f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = badgeTexto,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Botão de Adição Rápida
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (jaNaLista) Color(0xFF00E676) else Color.Black.copy(alpha = 0.7f))
                                            .clickable {
                                                if (!jaNaLista) {
                                                    onAdicionarClique(
                                                        item.idTmdb,
                                                        item.titulo,
                                                        tipoReal,
                                                        StatusMidia.QUERO_ASSISTIR.valor,
                                                        0,
                                                        item.sinopse,
                                                        posterUrl,
                                                        item.generoTexto,
                                                        servico.nome
                                                    )
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (jaNaLista) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = if (jaNaLista) "Na Lista" else "Adicionar à Lista",
                                            tint = if (jaNaLista) Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Título
                                Text(
                                    text = item.titulo,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                // Nota IMDb / TMDB e Ano
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.mediaVotos > 0) {
                                        Surface(
                                            color = Color(0xFFF5C518).copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = "★",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFFF5C518)
                                                )
                                                Text(
                                                    text = String.format("%.1f", item.mediaVotos),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF5C518)
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Novo",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (item.anoLancamento.isNotBlank()) {
                                        Text(
                                            text = item.anoLancamento,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.secondary
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
