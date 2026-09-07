package com.example.cinelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun ItemMidiaCard(
    midia: Midia,
    onClick: () -> Unit
) {
    val corStatus = when (midia.status) {
        "Assistindo" -> Color(0xFF00BFFF)
        "Concluído" -> Color(0xFF32CD32)
        "Descobrir" -> Color(0xFFFF9800)
        else -> Color(0xFF888888)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
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
                        loading = {
                            // 🚀 Aplica o efeito Shimmer enquanto a capa baixa da internet
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shimmer()
                            )
                        },
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

                // Etiqueta de Status por cima da imagem
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

                // Progresso de tempo ou episódio
                val jaTeveProgresso = midia.minutoParado > 0 || midia.episodioAtual > 1 || midia.temporadaAtual > 1
                if (jaTeveProgresso && midia.status != "Concluído") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color(0xCC000000))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val textoProgresso = if (midia.tipo.equals("Filme", ignoreCase = true)) {
                            "Parou em: ${midia.minutoParado} min"
                        } else {
                            "T${midia.temporadaAtual} • Ep ${midia.episodioAtual}"
                        }
                        Text(
                            text = textoProgresso,
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = midia.titulo,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${midia.tipo} • ${midia.genero}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

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

                if (midia.plataforma.isNotBlank() && midia.plataforma != "Não Informado") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🍿 ${midia.plataforma}",
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
}

// 🚀 SKELETON COMPLETO: Usado na lista enquanto o TMDB busca a página inicial
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
            // Pôster fake com shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .shimmer()
            )

            Column(modifier = Modifier.padding(8.dp)) {
                // Título fake
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtítulo fake (Tipo • Gênero)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Estrelas fake
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
            }
        }
    }
}