package com.example.cinelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SecaoConquistasPerfil(listaDeMidias: List<Midia>) {
    val conquistasDesbloqueadas = remember(listaDeMidias) {
        listaDeConquistasCineList.count { it.regraDesbloqueio(listaDeMidias) }
    }

    val ofensivaSemanas = remember(listaDeMidias) {
        calcularOfensivaSemanal(listaDeMidias)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Card de Ofensiva Semanal (Streaks)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF9800).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 26.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ofensiva Semanal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (ofensivaSemanas == 1) "1 Semana Ativa" else "$ofensivaSemanas Semanas Seguidas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Conclua filmes ou episódios para manter a chama acesa!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Card de CineClube / Desafios do Mês Dinâmicos
        var mostrarDialogoCineClube by remember { mutableStateOf(false) }
        val statusClube = remember(listaDeMidias) { CineClubeManager.obterStatusClube(listaDeMidias) }

        if (mostrarDialogoCineClube) {
            DialogoCineClube(
                status = statusClube,
                onDispensar = { mostrarDialogoCineClube = false }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { mostrarDialogoCineClube = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🍿 ${"CineClube".t()} (${statusClube.mesNome.t()})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Text("${statusClube.totalConcluidos}/${statusClube.desafios.size} ${"Concluídos".t()} ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    statusClube.desafios.take(2).forEach { desafio ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = if (desafio.concluido) Color(0xFF2E7D32).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${desafio.icone} ${desafio.titulo.t()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (desafio.concluido) "✅ ${"Completo".t()} (+${desafio.recompensaXp} XP)" else "${desafio.progressoAtual}/${desafio.metaTotal} (${desafio.descricao.t()})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card de Conquistas & Selos
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Conquistas & Selos 🏆",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$conquistasDesbloqueadas / ${listaDeConquistasCineList.size} Desbloqueadas",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listaDeConquistasCineList.forEach { conquista ->
                        val desbloqueada = conquista.regraDesbloqueio(listaDeMidias)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (desbloqueada) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (desbloqueada) Color(0xFFFFD700).copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conquista.iconeEmoji,
                                    fontSize = 22.sp,
                                    modifier = Modifier.alpha(if (desbloqueada) 1f else 0.4f)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${conquista.titulo} • ${conquista.nivel}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (desbloqueada) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = if (desbloqueada) "Desbloqueada ✅" else "Bloqueada 🔒",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (desbloqueada) Color(0xFF4CAF50) else Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = conquista.descricao,
                                    fontSize = 11.sp,
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