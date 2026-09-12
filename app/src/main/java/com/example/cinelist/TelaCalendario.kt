package com.example.cinelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ItemCalendario(
    val serieTitulo: String,
    val capaUrl: String,
    val numeroTemporada: Int,
    val numeroEpisodio: Int,
    val nomeEpisodio: String?,
    val dataExibicaoStr: String,
    val dataExibicaoMillis: Long,
    val idTmdb: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCalendario(
    listaDeMidias: List<Midia>,
    onVoltar: () -> Unit,
    onMidiaClique: (Int, String) -> Unit
) {
    var itensCalendario by remember { mutableStateOf<List<ItemCalendario>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }

    LaunchedEffect(listaDeMidias) {
        carregando = true
        val seriesAssistindo = listaDeMidias.filter { midia ->
            midia.status.equals("Assistindo", ignoreCase = true) &&
                    !midia.tipo.equals("Filme", ignoreCase = true) &&
                    midia.idTmdb > 0
        }

        val listaTemporaria = mutableListOf<ItemCalendario>()
        val formatoApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        withContext(Dispatchers.IO) {
            for (serie in seriesAssistindo) {
                try {
                    val detalhes = RetrofitClient.apiService.obterDetalhesSerieOuAnime(serie.idTmdb)
                    val proximoEp = detalhes.proximoEpisodio

                    if (proximoEp != null && !proximoEp.dataExibicao.isNullOrBlank()) {
                        val dataParseada = formatoApi.parse(proximoEp.dataExibicao)
                        val millis = dataParseada?.time ?: 0L

                        listaTemporaria.add(
                            ItemCalendario(
                                serieTitulo = serie.titulo,
                                capaUrl = serie.imagemCapa,
                                numeroTemporada = proximoEp.numeroTemporada,
                                numeroEpisodio = proximoEp.numeroEpisodio,
                                nomeEpisodio = proximoEp.nome,
                                dataExibicaoStr = proximoEp.dataExibicao,
                                dataExibicaoMillis = millis,
                                idTmdb = serie.idTmdb
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        itensCalendario = listaTemporaria.sortedBy { it.dataExibicaoMillis }
        carregando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendário de Lançamentos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (carregando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (itensCalendario.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum próximo episódio agendado para as séries que você está assistindo.",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(itensCalendario) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMidiaClique(item.idTmdb, "Série") },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(85.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.capaUrl.isNotBlank()) {
                                        SubcomposeAsyncImage(
                                            model = item.capaUrl,
                                            contentDescription = item.serieTitulo,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(imageVector = Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.serieTitulo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Temporada ${item.numeroTemporada} • Episódio ${item.numeroEpisodio}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (!item.nomeEpisodio.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "\"${item.nomeEpisodio}\"",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Estreia: ${item.dataExibicaoStr}",
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
}