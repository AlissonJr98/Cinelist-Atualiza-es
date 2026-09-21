package com.example.cinelist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SecaoConquistasPerfil(listaDeMidias: List<Midia>) {
    val conquistasDesbloqueadas = remember(listaDeMidias) {
        listaDeConquistasCineList.count { it.regraDesbloqueio(listaDeMidias) }
    }

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

            // Grade de Selos
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
                                    text = conquista.titulo,
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