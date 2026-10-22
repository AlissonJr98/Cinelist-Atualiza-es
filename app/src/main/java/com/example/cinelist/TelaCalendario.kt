package com.example.cinelist

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val idTmdb: Int,
    val tipoItem: String = "SERIE" // "SERIE" ou "FILME"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCalendario(
    listaDeMidias: List<Midia>,
    onVoltar: () -> Unit,
    onMidiaClique: (Int, String) -> Unit
) {
    val contexto = LocalContext.current
    var itensCalendario by remember { mutableStateOf<List<ItemCalendario>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var filtroSelecionado by remember { mutableStateOf("TODOS") } // "TODOS", "SERIE", "FILME"

    LaunchedEffect(listaDeMidias) {
        carregando = true
        val formatoApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val listaTemporaria = mutableListOf<ItemCalendario>()
        val hojeMillis = System.currentTimeMillis() - 86400000L // Permite estreias de hoje

        withContext(Dispatchers.IO) {
            // 1. Processa Séries em "Assistindo"
            val seriesAssistindo = listaDeMidias.filter { midia ->
                midia.status.equals("Assistindo", ignoreCase = true) &&
                        !midia.tipo.equals("Filme", ignoreCase = true) &&
                        midia.idTmdb > 0
            }

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
                                idTmdb = serie.idTmdb,
                                tipoItem = "SERIE"
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Processa Filmes em "Quero Assistir" / "Descobrir" com data de lançamento futura ou recente
            val filmesPlanejados = listaDeMidias.filter { midia ->
                midia.tipo.equals("Filme", ignoreCase = true) &&
                        midia.obterStatusEnum() != StatusMidia.CONCLUIDO &&
                        midia.dataLancamento.isNotBlank() &&
                        midia.idTmdb > 0
            }

            for (filme in filmesPlanejados) {
                try {
                    val dataParseada = formatoApi.parse(filme.dataLancamento)
                    val millis = dataParseada?.time ?: 0L

                    // Inclui se a data for futura ou dos últimos 30 dias
                    if (millis >= (hojeMillis - 2592000000L)) {
                        listaTemporaria.add(
                            ItemCalendario(
                                serieTitulo = filme.titulo,
                                capaUrl = filme.imagemCapa,
                                numeroTemporada = 0,
                                numeroEpisodio = 0,
                                nomeEpisodio = "Estreia Oficial nos Cinemas / Streaming",
                                dataExibicaoStr = filme.dataLancamento,
                                dataExibicaoMillis = millis,
                                idTmdb = filme.idTmdb,
                                tipoItem = "FILME"
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

    val itensFiltrados = remember(itensCalendario, filtroSelecionado) {
        when (filtroSelecionado) {
            "SERIE" -> itensCalendario.filter { it.tipoItem == "SERIE" }
            "FILME" -> itensCalendario.filter { it.tipoItem == "FILME" }
            else -> itensCalendario
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agenda do Cinéfilo", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chips de Filtro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filtroSelecionado == "TODOS",
                    onClick = { filtroSelecionado = "TODOS" },
                    label = { Text("Todos (${itensCalendario.size})") }
                )
                FilterChip(
                    selected = filtroSelecionado == "SERIE",
                    onClick = { filtroSelecionado = "SERIE" },
                    label = { Text("📺 Séries") }
                )
                FilterChip(
                    selected = filtroSelecionado == "FILME",
                    onClick = { filtroSelecionado = "FILME" },
                    label = { Text("🎬 Filmes") }
                )
            }

            if (carregando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (itensFiltrados.isEmpty()) {
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
                            text = "Nenhum lançamento futuro encontrado para as mídias da sua lista.",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(itensFiltrados) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMidiaClique(item.idTmdb, if (item.tipoItem == "FILME") "Filme" else "Série") },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(92.dp)
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
                                        Icon(
                                            imageVector = if (item.tipoItem == "FILME") Icons.Default.Movie else Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
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
                                    if (item.tipoItem == "SERIE") {
                                        Text(
                                            text = "Temporada ${item.numeroTemporada} • Episódio ${item.numeroEpisodio}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "🎬 Lançamento de Filme",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
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
                                        text = "📅 Data: ${item.dataExibicaoStr}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                // Botão Adicionar ao Calendário do Sistema
                                IconButton(
                                    onClick = {
                                        adicionarAoGoogleAgenda(contexto, item)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = "Agendar Lembrete",
                                        tint = MaterialTheme.colorScheme.primary
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

/**
 * Dispara uma Intent nativa para adicionar o evento ao Google Calendar / Calendário padrão do Android.
 */
private fun adicionarAoGoogleAgenda(context: Context, item: ItemCalendario) {
    try {
        val tituloEvento = if (item.tipoItem == "SERIE") {
            "Lançamento: ${item.serieTitulo} (T${item.numeroTemporada}E${item.numeroEpisodio})"
        } else {
            "Estreia do Filme: ${item.serieTitulo}"
        }

        val descricao = item.nomeEpisodio ?: "Lembrete sincronizado pelo CineList"

        val dataMillis = if (item.dataExibicaoMillis > 0) item.dataExibicaoMillis else System.currentTimeMillis()

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, tituloEvento)
            putExtra(CalendarContract.Events.DESCRIPTION, descricao)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, dataMillis + 20 * 3600 * 1000L) // 20:00 do dia
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, dataMillis + 22 * 3600 * 1000L) // 22:00
            putExtra(CalendarContract.Events.ALL_DAY, true)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Não foi possível abrir o aplicativo de calendário.", Toast.LENGTH_SHORT).show()
    }
}
