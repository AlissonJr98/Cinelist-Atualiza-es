package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun DialogoMestresCinema(
    listaDeMidias: List<Midia>,
    onMidiaClique: (Int, String) -> Unit,
    onDispensar: () -> Unit
) {
    val statusDiretores = remember(listaDeMidias) {
        MestresCinemaManager.calcularProgresso(listaDeMidias)
    }

    var diretorAbertoId by remember { mutableStateOf<String?>(statusDiretores.firstOrNull()?.diretor?.id) }

    val titulosConcluidos = remember(listaDeMidias) {
        listaDeMidias.filter { it.obterStatusEnum() == StatusMidia.CONCLUIDO }
            .map { it.titulo.lowercase().trim() }
            .toSet()
    }
    val idsTmdbConcluidos = remember(listaDeMidias) {
        listaDeMidias.filter { it.obterStatusEnum() == StatusMidia.CONCLUIDO && it.idTmdb > 0 }
            .map { it.idTmdb }
            .toSet()
    }

    Dialog(onDismissRequest = onDispensar) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(2.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFBBF24).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎬", fontSize = 22.sp)
                        }
                        Column {
                            Text(
                                text = "MESTRES DO CINEMA",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFFFBBF24)
                            )
                            Text(
                                text = "Checklist de Filmografias Consagradas",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = onDispensar) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }

                // Lista de Diretores
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    statusDiretores.forEach { status ->
                        val diretor = status.diretor
                        val expandido = diretorAbertoId == diretor.id
                        val corBorda = if (status.completo) Color(0xFFFBBF24) else Color(0xFF334155)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (status.completo) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFF1E293B)
                            ),
                            border = BorderStroke(1.dp, corBorda),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .clickable {
                                        diretorAbertoId = if (expandido) null else diretor.id
                                    },
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(diretor.fotoEmoji, fontSize = 28.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = diretor.nome,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.White
                                            )
                                            if (status.completo) {
                                                Text("👑", fontSize = 14.sp)
                                            }
                                        }
                                        Text(
                                            text = diretor.estilo,
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Surface(
                                        color = if (status.completo) Color(0xFFFBBF24).copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (status.completo) Color(0xFFFBBF24).copy(alpha = 0.5f) else Color(0xFF38BDF8).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "${status.totalAssistidos}/${diretor.filmes.size}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (status.completo) Color(0xFFFBBF24) else Color(0xFF38BDF8)
                                        )
                                    }

                                    Icon(
                                        imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    )
                                }

                                // Barra de Progresso do Diretor
                                LinearProgressIndicator(
                                    progress = { status.porcentagem / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (status.completo) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                                    trackColor = Color(0xFF334155),
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${status.porcentagem.toInt()}% da filmografia concluída",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    if (status.completo) {
                                        Text(
                                            text = "🏆 ${diretor.badgeRecompensa} Desbloqueada!",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                }

                                // Checklist de Filmes do Diretor
                                AnimatedVisibility(visible = expandido) {
                                    Column(
                                        modifier = Modifier.padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)

                                        Text(
                                            text = "FILMES DA CARREIRA:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8)
                                        )

                                        diretor.filmes.forEach { filme ->
                                            val assistido = filme.idTmdb in idsTmdbConcluidos || filme.titulo.lowercase().trim() in titulosConcluidos

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (assistido) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFF0B132B),
                                                border = BorderStroke(1.dp, if (assistido) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFF334155).copy(alpha = 0.3f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onDispensar()
                                                        onMidiaClique(filme.idTmdb, "Filme")
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Text(if (assistido) "✅" else "⚪", fontSize = 14.sp)
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "${filme.titulo} (${filme.ano})",
                                                            fontWeight = if (assistido) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 12.sp,
                                                            color = if (assistido) Color(0xFF34D399) else Color.White
                                                        )
                                                        Text(
                                                            text = filme.genero,
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                    Text("Ver ➔", fontSize = 10.sp, color = Color(0xFF38BDF8))
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
        }
    }
}
