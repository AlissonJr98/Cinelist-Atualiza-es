package com.example.cinelist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DialogoComparacaoAmigo(
    amigo: AmigoPerfil,
    minhaLista: List<Midia>,
    listaDoAmigo: List<Midia>,
    onDispensar: () -> Unit
) {
    // Estatísticas do Usuário Atual
    val meuTotal = minhaLista.size
    val meusConcluidos = minhaLista.count { it.status == "Concluído" }
    val meusFilmes = minhaLista.count { it.tipo.equals("Filme", ignoreCase = true) && it.status == "Concluído" }
    val minhasSeries = minhaLista.count { !it.tipo.equals("Filme", ignoreCase = true) && it.status == "Concluído" }
    val minhasHoras = (meusFilmes * 2) + (minhasSeries * 15) // estimativa rápida

    // Estatísticas do Amigo
    val amigoTotal = listaDoAmigo.size
    val amigoConcluidos = listaDoAmigo.count { it.status == "Concluído" }
    val amigoFilmes = listaDoAmigo.count { it.tipo.equals("Filme", ignoreCase = true) && it.status == "Concluído" }
    val amigoSeries = listaDoAmigo.count { !it.tipo.equals("Filme", ignoreCase = true) && it.status == "Concluído" }
    val amigoHoras = (amigoFilmes * 2) + (amigoSeries * 15)

    // Títulos em comum (por idTmdb ou título exato)
    val titulosEmComum = remember(minhaLista, listaDoAmigo) {
        val meusTmdb = minhaLista.map { it.idTmdb }.filter { it != 0 }.toSet()
        val meusTitulos = minhaLista.map { it.titulo.lowercase().trim() }.toSet()

        listaDoAmigo.count { midiaAmigo ->
            (midiaAmigo.idTmdb != 0 && meusTmdb.contains(midiaAmigo.idTmdb)) ||
                    meusTitulos.contains(midiaAmigo.titulo.lowercase().trim())
        }
    }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Duelo de Maratonistas", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Você vs. ${amigo.nome}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )

                // Cabeçalho dos Nomes
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Você", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                    Text(amigo.nome, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 15.sp)
                }

                // Linhas Comparativas
                CardComparacaoLinha(titulo = "Total na Lista", meuValor = meuTotal.toString(), amigoValor = amigoTotal.toString())
                CardComparacaoLinha(titulo = "Títulos Concluídos", meuValor = meusConcluidos.toString(), amigoValor = amigoConcluidos.toString())
                CardComparacaoLinha(titulo = "Filmes Vistos", meuValor = meusFilmes.toString(), amigoValor = amigoFilmes.toString())
                CardComparacaoLinha(titulo = "Séries/Outros", meuValor = minhasSeries.toString(), amigoValor = amigoSeries.toString())
                CardComparacaoLinha(titulo = "Tempo Estimado", meuValor = "${minhasHoras}h", amigoValor = "${amigoHoras}h")

                Spacer(modifier = Modifier.height(2.dp))

                // Destaque de Títulos em Comum
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Títulos em Comum na Lista", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "$titulosEmComum", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDispensar) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CardComparacaoLinha(titulo: String, meuValor: String, amigoValor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = meuValor, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(text = titulo, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
        Text(text = amigoValor, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}