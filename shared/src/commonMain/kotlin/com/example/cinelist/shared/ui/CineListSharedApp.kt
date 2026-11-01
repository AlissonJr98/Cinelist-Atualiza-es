package com.example.cinelist.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cinelist.shared.CineListSharedConfig
import com.example.cinelist.shared.i18n.I18nCore
import com.example.cinelist.shared.models.TmdbMediaItem
import com.example.cinelist.shared.network.TmdbMultiplatformClient
import kotlinx.coroutines.launch

@Composable
fun CineListSharedApp() {
    CineListSharedTheme {
        var idiomaAtivo by remember { mutableStateOf("pt") }
        val idiomas = listOf("pt", "en", "es", "fr", "de", "it", "ja", "ko")
        val coroutineScope = rememberCoroutineScope()
        val client = remember { TmdbMultiplatformClient() }

        var abaSelecionada by remember { mutableStateOf("Em Alta") }
        val abas = listOf("Em Alta", "Filmes", "Séries", "Minha Lista")

        var itens by remember { mutableStateOf<List<TmdbMediaItem>>(emptyList()) }
        var carregando by remember { mutableStateOf(false) }
        var erro by remember { mutableStateOf<String?>(null) }

        // Carrega itens da API do TMDB
        fun carregarMidias(aba: String, lang: String) {
            coroutineScope.launch {
                carregando = true
                erro = null
                try {
                    val langTag = when (lang) {
                        "pt" -> "pt-BR"
                        "en" -> "en-US"
                        "es" -> "es-ES"
                        "fr" -> "fr-FR"
                        "de" -> "de-DE"
                        "it" -> "it-IT"
                        "ja" -> "ja-JP"
                        "ko" -> "ko-KR"
                        else -> "pt-BR"
                    }
                    val resposta = when (aba) {
                        "Filmes" -> client.getPopularMovies(language = langTag)
                        "Séries" -> client.getPopularTvShows(language = langTag)
                        else -> client.getTrending(language = langTag)
                    }
                    itens = resposta.results
                } catch (e: Exception) {
                    erro = e.message ?: "Erro ao carregar mídias"
                } finally {
                    carregando = false
                }
            }
        }

        LaunchedEffect(abaSelecionada, idiomaAtivo) {
            carregarMidias(abaSelecionada, idiomaAtivo)
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = FundoCineList
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // TopBar do CineList
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎬 CineList",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoPrincipal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AmareloCine)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CMP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }
                        Text(
                            text = I18nCore.traduzir("Sua Central Cinéfila Multiplataforma", idiomaAtivo),
                            fontSize = 12.sp,
                            color = TextoSecundario
                        )
                    }

                    // Seletor de Idioma Rápido
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SuperficieCard)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = idiomaAtivo.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmareloCine
                        )
                    }
                }

                // Seletor de Idiomas em Carrossel
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(idiomas) { lang ->
                        val selecionado = lang == idiomaAtivo
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selecionado) AmareloCine else SuperficieCard)
                                .clickable {
                                    idiomaAtivo = lang
                                    I18nCore.definirIdioma(lang)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = lang.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
                                color = if (selecionado) Color.Black else TextoPrincipal
                            )
                        }
                    }
                }

                // Abas de Navegação (Em Alta, Filmes, Séries, Minha Lista)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    abas.forEach { aba ->
                        val selecionada = aba == abaSelecionada
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selecionada) CianoNeon else SuperficieCard)
                                .clickable { abaSelecionada = aba }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = I18nCore.traduzir(aba, idiomaAtivo),
                                fontSize = 12.sp,
                                fontWeight = if (selecionada) FontWeight.Bold else FontWeight.Medium,
                                color = if (selecionada) Color.Black else TextoPrincipal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Conteúdo Principal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    when {
                        carregando -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AmareloCine)
                            }
                        }
                        erro != null -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "⚠️ " + I18nCore.traduzir("Erro de Conexão", idiomaAtivo),
                                    fontWeight = FontWeight.Bold,
                                    color = RosaNeon
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = erro ?: "",
                                    fontSize = 12.sp,
                                    color = TextoSecundario
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { carregarMidias(abaSelecionada, idiomaAtivo) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmareloCine)
                                ) {
                                    Text(
                                        text = I18nCore.traduzir("Tentar Novamente", idiomaAtivo),
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        itens.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = I18nCore.traduzir("Nenhum título encontrado", idiomaAtivo),
                                    color = TextoSecundario
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 20.dp)
                            ) {
                                items(itens, key = { it.id }) { midia ->
                                    CardItemMidiaShared(
                                        item = midia,
                                        idioma = idiomaAtivo
                                    )
                                }
                            }
                        }
                    }
                }

                // Rodapé de Plataforma
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = CineListSharedConfig.descricao,
                        fontSize = 11.sp,
                        color = TextoSecundario
                    )
                }
            }
        }
    }
}

@Composable
fun CardItemMidiaShared(
    item: TmdbMediaItem,
    idioma: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SuperficieCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder de Capa Futurista
            Box(
                modifier = Modifier
                    .size(width = 65.dp, height = 90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SuperficieElevada),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (item.isTvShow) "📺" else "🍿",
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (item.isTvShow) CianoNeon.copy(alpha = 0.2f) else AmareloCine.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.isTvShow) I18nCore.traduzir("Série", idioma) else I18nCore.traduzir("Filme", idioma),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isTvShow) CianoNeon else AmareloCine
                        )
                    }

                    if (item.voteAverage > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⭐", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = ((item.voteAverage * 10).toInt() / 10.0).toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmareloCine
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.displayTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.releaseYear.isNotEmpty()) {
                    Text(
                        text = item.releaseYear,
                        fontSize = 12.sp,
                        color = TextoSecundario
                    )
                }

                if (item.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.overview,
                        fontSize = 12.sp,
                        color = TextoSecundario,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ações Rápidas de Status do CineList
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BotaoAcaoStatusShared(
                        rotulo = I18nCore.traduzir("Quero Assistir", idioma),
                        cor = AmareloCine
                    )
                    BotaoAcaoStatusShared(
                        rotulo = I18nCore.traduzir("Assistindo", idioma),
                        cor = CianoNeon
                    )
                    BotaoAcaoStatusShared(
                        rotulo = I18nCore.traduzir("Concluído", idioma),
                        cor = VerdeConcluido
                    )
                }
            }
        }
    }
}

@Composable
fun BotaoAcaoStatusShared(
    rotulo: String,
    cor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(cor.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = rotulo,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = cor,
            maxLines = 1
        )
    }
}
