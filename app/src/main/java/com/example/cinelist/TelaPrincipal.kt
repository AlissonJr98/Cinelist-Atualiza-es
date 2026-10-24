@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.cinelist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// =====================================
// CLASSES E LISTAS DE APOIO
// =====================================

class HistoricoBuscaManager(context: Context) {
    private val prefs = context.getSharedPreferences("CineListBuscaPrefs", Context.MODE_PRIVATE)

    fun obterHistorico(): List<String> {
        val salvos = prefs.getString("historico_buscas", "") ?: ""
        return if (salvos.isBlank()) emptyList() else salvos.split("|||")
    }

    fun adicionarTermo(termo: String) {
        val termoLimpo = termo.trim()
        if (termoLimpo.length < 2) return
        val atual = obterHistorico().toMutableList()
        atual.remove(termoLimpo)
        atual.add(0, termoLimpo)
        val limitado = atual.take(5)
        prefs.edit().putString("historico_buscas", limitado.joinToString("|||")).apply()
    }

    fun limparHistorico() {
        prefs.edit().remove("historico_buscas").apply()
    }
}

val opcoesOrdenacaoDescobrir = listOf(
    Pair("Mais Populares", "popularity.desc"),
    Pair("Melhor Avaliados", "vote_average.desc"),
    Pair("Mais Recentes", "primary_release_date.desc")
)

val opcoesOrdenacaoMinhaLista = listOf(
    "Padrão (Assistindo primeiro)",
    "Favoritos Primeiro",
    "Melhor Avaliados",
    "Ordem Alfabética (A-Z)",
    "Adicionados Recentemente"
)

val provedoresDisponiveis = listOf(
    Pair("Netflix", 8),
    Pair("Prime Video", 119),
    Pair("Disney+", 337),
    Pair("Max", 1899),
    Pair("Apple TV+", 350),
    Pair("Paramount+", 531),
    Pair("Crunchyroll", 283),
    Pair("Globoplay", 307)
)

val generosFilmes = listOf(
    Pair("Ação", 28),
    Pair("Aventura", 12),
    Pair("Animação", 16),
    Pair("Comédia", 35),
    Pair("Crime", 80),
    Pair("Documentário", 99),
    Pair("Drama", 18),
    Pair("Família", 10751),
    Pair("Fantasia", 14),
    Pair("Terror", 27),
    Pair("Ficção Científica", 878),
    Pair("Suspense", 53),
    Pair("Romance", 10749)
)

val generosSeries = listOf(
    Pair("Ação & Aventura", 10759),
    Pair("Animação", 16),
    Pair("Comédia", 35),
    Pair("Crime", 80),
    Pair("Documentário", 99),
    Pair("Drama", 18),
    Pair("Família", 10751),
    Pair("Mistério", 9648),
    Pair("Sci-Fi & Fantasy", 10765),
    Pair("Kids", 10762)
)

val categoriasMinhaLista = listOf("Todos", "Filmes", "Séries", "Animes", "Novelas", "Doramas")
val tiposDisponiveis = listOf("Todos", "Filme", "Série", "Anime", "Novela", "Dorama")

// =====================================
// HERO BANNER CARROSSEL (PREMIUM)
// =====================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroBannerDestaque(
    listaDestaques: List<TmdbFilme>,
    listaDeMidiasLocal: List<Midia>,
    onTmdbItemClique: (TmdbFilme, String) -> Unit,
    onAdicionarClique: (Int, String, String, String, Int, String, String, String, String) -> Unit
) {
    if (listaDestaques.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { listaDestaques.size })

    LaunchedEffect(pagerState.settledPage) {
        delay(6000L)
        if (!pagerState.isScrollInProgress) {
            val nextPage = (pagerState.currentPage + 1) % listaDestaques.size
            pagerState.animateScrollToPage(page = nextPage, animationSpec = tween(durationMillis = 800))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pagina ->
            val itemTmdb = listaDestaques[pagina]
            val tipoReal = if (itemTmdb.mediaType.equals("tv", ignoreCase = true) || itemTmdb.ehSerie) "Série" else "Filme"
            val plataformaFinal = itemTmdb.plataformaDetectada.ifBlank { if (tipoReal == "Série") "TV / Original" else "Cinema" }
            val jaNaLista = listaDeMidiasLocal.any { it.idTmdb != 0 && it.idTmdb == itemTmdb.idTmdb }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onTmdbItemClique(itemTmdb, tipoReal) }
            ) {
                AsyncImage(
                    model = if (!itemTmdb.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w780${itemTmdb.caminhoPoster}" else "",
                    contentDescription = itemTmdb.titulo,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.5f to Color(0x66000000),
                                    0.8f to MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    1.0f to MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color(0xAA000000),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("🔥 DESTAQUE DA SEMANA", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = itemTmdb.titulo,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 34.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$tipoReal • ${itemTmdb.generoTexto} • $plataformaFinal",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val sinopseResumo = itemTmdb.sinopse.ifBlank { "Um título incrível que está em alta no momento. Clique para descobrir mais!" }
                    Text(
                        text = sinopseResumo,
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onTmdbItemClique(itemTmdb, tipoReal) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detalhes", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (!jaNaLista) {
                                    onAdicionarClique(
                                        itemTmdb.idTmdb, itemTmdb.titulo, tipoReal, StatusMidia.QUERO_ASSISTIR.valor, 0, itemTmdb.sinopse,
                                        if (!itemTmdb.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${itemTmdb.caminhoPoster}" else "",
                                        itemTmdb.generoTexto, plataformaFinal
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (jaNaLista) Color(0xFF2E7D32) else Color(0xFF333333)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = if (jaNaLista) Icons.Default.Check else Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (jaNaLista) "Na Lista" else "Adicionar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .height(24.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(listaDestaques.size) { iteration ->
                val selecionado = pagerState.currentPage == iteration
                val color by animateColorAsState(targetValue = if (selecionado) Color.White else Color.White.copy(alpha = 0.3f), label = "corDot")
                val width by animateDpAsState(targetValue = if (selecionado) 24.dp else 8.dp, label = "larguraDot")
                Box(
                    modifier = Modifier.padding(horizontal = 4.dp).clip(CircleShape).background(color).height(8.dp).width(width)
                )
            }
        }
    }
}

// =====================================
// COMPONENTE PRINCIPAL (TELA PRINCIPAL)
// =====================================

@Composable
fun TelaPrincipal(
    listaDeMidias: List<Midia>,
    viewModel: MidiaViewModel,
    isModoCompartilhado: Boolean,
    onAdicionarClique: (idTmdb: Int, titulo: String, tipo: String, status: String, nota: Int, sinopse: String, capa: String, genero: String, plataforma: String) -> Unit,
    onItemClique: (Midia) -> Unit,
    onTmdbItemClique: (TmdbFilme, String) -> Unit,
    onPerfilClique: () -> Unit,
    onAbrirMatch: () -> Unit
) {
    val contextoLocal = LocalContext.current
    val historicoManager = remember { HistoricoBuscaManager(contextoLocal) }
    var historicoBuscas by remember { mutableStateOf(historicoManager.obterHistorico()) }

    val coroutineScope = rememberCoroutineScope()
    val totalPaginasPrincipal = if (isModoCompartilhado) 2 else 3
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPaginasPrincipal })

    LaunchedEffect(isModoCompartilhado) {
        if (isModoCompartilhado && pagerState.currentPage > 1) {
            pagerState.scrollToPage(0)
        }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var searchFocused by remember { mutableStateOf(false) }

    val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()
    val isAdministrador by viewModel.isAdministradorSala.collectAsState()
    val gruposSalvos by viewModel.gruposSalvos.collectAsState(initial = emptyList())
    var menuSalasExpandido by remember { mutableStateOf(false) }

    var textoPesquisa by rememberSaveable { mutableStateOf("") }
    var midiaParaExcluir by remember { mutableStateOf<Midia?>(null) }
    var midiaParaConcluir by remember { mutableStateOf<Midia?>(null) }
    var midiaParaRecusar by remember { mutableStateOf<Midia?>(null) }
    var colecaoParaExcluir by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoGerenciarSalas by remember { mutableStateOf(false) }
    var mostrarDialogoSugestoes by remember { mutableStateOf(false) }
    var midiaParaDecidirRecusada by remember { mutableStateOf<Midia?>(null) }

    var modoVisualizacaoMinhaLista by rememberSaveable { mutableIntStateOf(0) }
    var modoVisualizacaoDescobrir by rememberSaveable { mutableIntStateOf(0) }
    var mostrarDialogoSorteio by remember { mutableStateOf(false) }
    var mostrarDialogoCineBot by remember { mutableStateOf(false) }
    var mostrarDialogoMeusStreamings by remember { mutableStateOf(false) }
    var mostrarDialogoEnquetes by remember { mutableStateOf(false) }
    var mostrarCinePartySala by remember { mutableStateOf(false) }

    var mostrarMatchColetivoSala by remember { mutableStateOf(false) }
    var mostrarDialogoRadarEstreias by remember { mutableStateOf(false) }
    var mostrarDialogoPlaylistsPrincipal by remember { mutableStateOf(false) }
    var filtroApenasMeusStreamingsMinhaLista by rememberSaveable { mutableStateOf(false) }

    val nomeListaAtiva = if (casalIdAtivo.isBlank()) "Minha Lista Pessoal" else gruposSalvos.find { it.grupoId == casalIdAtivo }?.nomeGrupo?.ifBlank { "Sala Compartilhada" } ?: "Sala Compartilhada"
    val membrosSala by viewModel.membrosGrupoAtivo.collectAsState(initial = emptyList())
    val meuUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val membroAssistindo = remember(membrosSala, meuUid) {
        membrosSala.firstOrNull { it.uid != meuUid && it.estaAssistindoAlgo }
    }

    var mostrarChatSala by remember { mutableStateOf(false) }
    val mensagensSala by viewModel.mensagensGrupoAtivo.collectAsState(initial = emptyList())
    var inputMensagemSala by remember { mutableStateOf("") }
    val salaMutada by viewModel.salaMutada.collectAsState()

    var totalMensagensVistas by rememberSaveable { mutableIntStateOf(0) }
    var mensagensNaoLidas by remember { mutableIntStateOf(0) }

    // ===== FEEDBACK PROFISSIONAL (Snackbar) =====
    val snackbarHostState = remember { SnackbarHostState() }

    val mostrarSucesso: (String) -> Unit = { mensagem ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = mensagem,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }

    val mostrarErro: (String) -> Unit = { mensagem ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = mensagem,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
        }
    }

    // Reage ao estado de operação do ViewModel (sucesso/erro ao adicionar, etc.)
    val estadoOperacao by viewModel.estadoOperacao.collectAsState()
    LaunchedEffect(estadoOperacao) {
        when (val estado = estadoOperacao) {
            is Resultado.Sucesso -> {
                mostrarSucesso("Mídia adicionada com sucesso!")
                viewModel.limparEstadoOperacao()
            }
            is Resultado.Erro -> {
                mostrarErro(estado.mensagem)
                viewModel.limparEstadoOperacao()
            }
            else -> Unit
        }
    }

    LaunchedEffect(mensagensSala.size, mostrarChatSala) {
        if (mostrarChatSala) {
            totalMensagensVistas = mensagensSala.size
            mensagensNaoLidas = 0
        } else {
            if (mensagensSala.size > totalMensagensVistas && totalMensagensVistas > 0) {
                mensagensNaoLidas = mensagensSala.size - totalMensagensVistas
            } else if (totalMensagensVistas == 0) {
                totalMensagensVistas = mensagensSala.size
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        textoPesquisa = ""
        viewModel.limparBuscaApi()
        focusManager.clearFocus()
    }

    if (mostrarDialogoGerenciarSalas) {
        DialogoGerenciarSalasCompartilhadas(viewModel = viewModel, onDispensar = { mostrarDialogoGerenciarSalas = false })
    }

    if (mostrarDialogoSugestoes) {
        DialogoSugestoesPendentes(
            listaPendentes = listaDeMidias.filter { it.statusSugestao == "PENDENTE" },
            listaRecusados = listaDeMidias.filter { it.statusSugestao == "RECUSADO" },
            meuUid = meuUid,
            totalMembros = maxOf(2, membrosSala.size),
            isAdministrador = isAdministrador,
            onItemClique = { midia ->
                mostrarDialogoSugestoes = false
                onItemClique(midia)
            },
            onAceitar = {
                viewModel.aceitarMidiaPendente(it, maxOf(2, membrosSala.size))
                mostrarSucesso("Sugestão \"${it.titulo}\" aprovada!")
            },
            onRecusar = {
                viewModel.recusarMidiaPendente(it)
                mostrarSucesso("Sugestão recusada")
            },
            onResolverRecusado = { midiaParaDecidirRecusada = it },
            onRetirarSugestao = { viewModel.retirarSugestaoPendente(it) },
            onForcarAprovacaoAdmin = { viewModel.forcarAprovacaoAdmin(it) },
            onDispensar = { mostrarDialogoSugestoes = false }
        )
    }

    if (mostrarDialogoSorteio) {
        DialogoSorteio(listaDeMidias = listaDeMidias, onDispensar = { mostrarDialogoSorteio = false }, onSelecionarMidia = { m -> mostrarDialogoSorteio = false; onItemClique(m) })
    }

        if (mostrarDialogoMeusStreamings) {
        DialogoMeusStreamings(
            onDispensar = { mostrarDialogoMeusStreamings = false },
            onSalvo = { ids ->
                mostrarDialogoMeusStreamings = false
                val q = StreamingManager.obterQueryMeusStreamings(contextoLocal)
                viewModel.alternarFiltroMeusStreamings(q)
                viewModel.carregarNovidadesPorStreaming(contextoLocal, forcar = true)
                mostrarSucesso("Filtro atualizado com seus ${ids.size} streamings!")
            }
        )
    }

    if (mostrarDialogoCineBot) {
        DialogoCineAi(
            onDispensar = { mostrarDialogoCineBot = false },
            onAdicionarClique = { idTmdb, titulo, tipo, status, nota, sinopse, capa, genero, plataforma ->
                onAdicionarClique(idTmdb, titulo, tipo, status, nota, sinopse, capa, genero, plataforma)
                mostrarDialogoCineBot = false
                mostrarSucesso("\"$titulo\" adicionado à sua lista! 🍿")
            },
            onVerDetalhes = { tmdbItem, tipo ->
                mostrarDialogoCineBot = false
                onTmdbItemClique(tmdbItem, tipo)
            }
        )
    }

    if (mostrarDialogoEnquetes) {
        DialogoEnquetesSala(
            viewModel = viewModel,
            listaDeMidiasSala = listaDeMidias,
            onDispensar = { mostrarDialogoEnquetes = false }
        )
    }

    val sessaoMatchColetivoGlobal by viewModel.sessaoMatchColetivoAtiva.collectAsState(initial = null)
    var conviteMatchDispensadoId by remember { mutableStateOf("") }

    // Alerta ao vivo quando outro membro da sala inicia o CineMatch Coletivo
    if (sessaoMatchColetivoGlobal?.estado == "ATIVO" &&
        sessaoMatchColetivoGlobal?.criadorUid.isNullOrBlank() == false &&
        sessaoMatchColetivoGlobal?.criadorUid != meuUid &&
        conviteMatchDispensadoId != sessaoMatchColetivoGlobal?.id &&
        !mostrarMatchColetivoSala
    ) {
        val autorMatch = sessaoMatchColetivoGlobal?.criadorNome?.ifBlank { "Um amigo" } ?: "Um amigo"
        AlertDialog(
            onDismissRequest = { conviteMatchDispensadoId = sessaoMatchColetivoGlobal?.id ?: "" },
            icon = { Text("👥", fontSize = 32.sp) },
            title = { Text("CineMatch Coletivo! 🍿") },
            text = {
                Text("$autorMatch chamou a galera da sala '$nomeListaAtiva' para o CineMatch Coletivo! Vamos votar nos filmes agora?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        conviteMatchDispensadoId = sessaoMatchColetivoGlobal?.id ?: ""
                        mostrarMatchColetivoSala = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
                ) {
                    Text("Votar Agora 🚀", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { conviteMatchDispensadoId = sessaoMatchColetivoGlobal?.id ?: "" }) {
                    Text("Depois", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Alerta de chamada CineParty recebida de outro membro da sala
    val ultimaMsgSala = mensagensSala.lastOrNull()
    var ultimaChamadaProcessadaId by remember { mutableStateOf("") }
    if (ultimaMsgSala?.tipoMensagem == "CINEPARTY_CHAMADA" &&
        ultimaMsgSala.autorUid != meuUid &&
        ultimaChamadaProcessadaId != ultimaMsgSala.id &&
        !mostrarCinePartySala &&
        (System.currentTimeMillis() - ultimaMsgSala.timestamp < 120_000L)
    ) {
        AlertDialog(
            onDismissRequest = { ultimaChamadaProcessadaId = ultimaMsgSala.id },
            icon = { Text("📞", fontSize = 32.sp) },
            title = { Text("Chamada CineParty Recebida! 🍿") },
            text = {
                Text("${ultimaMsgSala.autorNome} está te chamando para assistir juntos no CineParty da sala '$nomeListaAtiva'!")
            },
            confirmButton = {
                Button(
                    onClick = {
                        ultimaChamadaProcessadaId = ultimaMsgSala.id
                        mostrarCinePartySala = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Atender e Entrar 🍿", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ultimaChamadaProcessadaId = ultimaMsgSala.id }) {
                    Text("Recusar", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (mostrarCinePartySala) {
        val grupoId = viewModel.casalIdAtivo.value
        DialogoWatchParty(
            tituloMidia = "CineParty: $nomeListaAtiva",
            posterMidia = "",
            salaIdInicial = "sala_${grupoId.ifBlank { nomeListaAtiva.filter { it.isLetterOrDigit() } }}",
            membrosSala = membrosSala,
            grupoId = casalIdAtivo,
            viewModel = viewModel,
            onDispensar = { mostrarCinePartySala = false }
        )
    }



    if (mostrarMatchColetivoSala) {
        DialogoCineMatchColetivo(
            grupoId = casalIdAtivo,
            nomeGrupo = nomeListaAtiva,
            midiasGrupo = listaDeMidias,
            membrosSala = membrosSala,
            viewModel = viewModel,
            onIniciarCineParty = { titulo, poster ->
                mostrarMatchColetivoSala = false
                mostrarCinePartySala = true
            },
            onDispensar = { mostrarMatchColetivoSala = false }
        )
    }

    if (mostrarDialogoRadarEstreias) {
        DialogoRadarStreaming(
            viewModel = viewModel,
            onDispensar = { mostrarDialogoRadarEstreias = false }
        )
    }

    if (mostrarDialogoPlaylistsPrincipal) {
        DialogoPlaylists(
            listaDeMidias = listaDeMidias,
            onMidiaClique = { id, tipo ->
                mostrarDialogoPlaylistsPrincipal = false
                val m = listaDeMidias.firstOrNull { it.idTmdb == id }
                if (m != null) {
                    onItemClique(m)
                } else {
                    onTmdbItemClique(TmdbFilme(idTmdb = id, titulo = "", mediaType = if (tipo.equals("Filme", true)) "movie" else "tv"), tipo)
                }
            },
            onDispensar = { mostrarDialogoPlaylistsPrincipal = false }
        )
    }

    // 🚀 ALERTA: CONFIRMAÇÃO DE RECUSA DE MÍDIA
    // 🚀 ALERTA: DECIDIR O QUE FAZER COM SUGESTÃO RECUSADA (Apenas para o Autor)

    if (midiaParaDecidirRecusada != null) {
        val midiaAlvo = midiaParaDecidirRecusada!!
        AlertDialog(
            onDismissRequest = { midiaParaDecidirRecusada = null },
            title = { Text("Sugestão Recusada ❌", fontWeight = FontWeight.Bold) },
            text = { Text("O grupo decidiu não assistir \"${midiaAlvo.titulo}\". Deseja adicionar este título à sua Lista Pessoal para assistir sozinho(a)?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.processarMidiaRecusadaPeloParceiro(midiaAlvo, true)
                        midiaParaDecidirRecusada = null
                        mostrarSucesso("\"${midiaAlvo.titulo}\" adicionado à sua lista pessoal")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Sim, adicionar à Minha Lista", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.processarMidiaRecusadaPeloParceiro(midiaAlvo, false); midiaParaDecidirRecusada = null; mostrarSucesso("Sugestão descartada") }) {
                    Text("Apenas Descartar", color = Color(0xFFFF4C4C))
                }
            }
        )
    }

    if (midiaParaExcluir != null) {
        AlertDialog(
            onDismissRequest = { midiaParaExcluir = null },
            title = { Text("Excluir Mídia", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente remover \"${midiaParaExcluir?.titulo}\" da lista?") },
            confirmButton = {
                Button(
                    onClick = {
                        midiaParaExcluir?.let { m ->
                            viewModel.deletar(m)
                            mostrarSucesso("\"${m.titulo}\" removido da lista")
                        }
                        midiaParaExcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                ) { Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { midiaParaExcluir = null }) { Text("Cancelar") } }
        )
    }

    if (midiaParaConcluir != null) {
        val midiaAlvo = midiaParaConcluir!!
        val estaConcluido = midiaAlvo.obterStatusEnum() == StatusMidia.CONCLUIDO
        val acaoTexto = if (estaConcluido) "Reabrir" else "Concluir"

        AlertDialog(
            onDismissRequest = { midiaParaConcluir = null },
            title = { Text("$acaoTexto Mídia", fontWeight = FontWeight.Bold) },
            text = { Text(if (estaConcluido) "Deseja reabrir \"${midiaAlvo.titulo}\" e voltar para o status Assistindo?" else "Deseja realmente marcar \"${midiaAlvo.titulo}\" como Concluído?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (estaConcluido) {
                            viewModel.atualizar(midiaAlvo.copy(status = StatusMidia.ASSISTINDO.valor, dataConclusao = 0L, concluidoPor = ""))
                            mostrarSucesso("\"${midiaAlvo.titulo}\" reaberta")
                        } else {
                            viewModel.concluirMidia(midiaAlvo)
                            mostrarSucesso("\"${midiaAlvo.titulo}\" concluída! 🎉")
                            if (isModoCompartilhado) {
                                viewModel.enviarMensagemParaSala(
                                    texto = "💬 Debate Pós-Filme: \"${midiaAlvo.titulo}\" foi concluído! O que acharam do final?",
                                    tipoMensagem = "DEBATE",
                                    mediaUrl = "${midiaAlvo.idTmdb}|||${midiaAlvo.titulo}|||${midiaAlvo.imagemCapa}"
                                )
                            }
                        }
                        midiaParaConcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (estaConcluido) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32))
                ) { Text(acaoTexto, color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { midiaParaConcluir = null }) { Text("Cancelar") } }
        )
    }

    colecaoParaExcluir?.let { nomeColecao ->
        AlertDialog(
            onDismissRequest = { colecaoParaExcluir = null },
            title = { Text("Excluir Coleção", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja apagar a coleção \"$nomeColecao\"? Os itens dela voltarão para a coleção Geral.") },
            confirmButton = {
                Button(
                    onClick = {
                        val filtradas = listaDeMidias.filter { it.listaCustomizada.equals(nomeColecao, ignoreCase = true) }
                        for (m in filtradas) {
                            viewModel.atualizar(m.copy(listaCustomizada = "Geral"))
                        }
                        colecaoParaExcluir = null
                        mostrarSucesso("Coleção \"$nomeColecao\" excluída")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                ) { Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { colecaoParaExcluir = null }) { Text("Cancelar") } }
        )
    }

    val streamingsFiltro = remember(listaDeMidias) { listOf("Todas") + listaDeMidias.map { it.plataforma }.filter { it.isNotBlank() && it != "Não Informado" }.distinct().sorted() }
    val colecoesDisponiveis = remember(listaDeMidias) { listOf("Geral") + listaDeMidias.map { it.listaCustomizada }.filter { it.isNotBlank() && it != "Geral" }.distinct().sorted() }

    var categoriaSelecionada by rememberSaveable { mutableStateOf("Todos") }
    var colecaoSelecionada by rememberSaveable { mutableStateOf("Geral") }
    var filtroPlataforma by rememberSaveable { mutableStateOf("Todas") }
    var filtroStatusMinhaLista by rememberSaveable { mutableStateOf("Ativos") }
    var ordenacaoMinhaLista by rememberSaveable { mutableStateOf("Padrão (Assistindo primeiro)") }

    var mostrarBottomSheetFiltrosDescobrir by remember { mutableStateOf(false) }
    var mostrarBottomSheetFiltrosMinhaLista by remember { mutableStateOf(false) }
    var mostrarDialogoGerenciarColecoes by remember { mutableStateOf(false) }

    val provedorSelecionadoId by viewModel.provedorSelecionadoId.collectAsState()
    val generoSelecionadoId by viewModel.generoSelecionadoId.collectAsState()
    val ordenacaoSelecionada by viewModel.ordenacaoSelecionada.collectAsState()
    val tipoPaginado by viewModel.tipoPaginado.collectAsState()
    val resultadosPaginadosApi = viewModel.resultadosBuscaPaginadaApi.collectAsLazyPagingItems()

    val quantidadeFiltrosAtivosDescobrir = remember(tipoPaginado, provedorSelecionadoId, generoSelecionadoId, ordenacaoSelecionada) {
        var count = 0
        if (tipoPaginado != "Todos") count++
        if (provedorSelecionadoId != null) count++
        if (generoSelecionadoId != null) count++
        if (ordenacaoSelecionada != "popularity.desc") count++
        count
    }

    val quantidadeFiltrosAtivosMinhaLista = remember(categoriaSelecionada, colecaoSelecionada, filtroPlataforma, filtroStatusMinhaLista, ordenacaoMinhaLista) {
        var count = 0
        if (categoriaSelecionada != "Todos") count++
        if (colecaoSelecionada != "Geral") count++
        if (filtroPlataforma != "Todas") count++
        if (filtroStatusMinhaLista != "Ativos") count++
        if (ordenacaoMinhaLista != "Padrão (Assistindo primeiro)") count++
        count
    }

    LaunchedEffect(textoPesquisa, quantidadeFiltrosAtivosMinhaLista) {
        if ((textoPesquisa.isNotBlank() || quantidadeFiltrosAtivosMinhaLista > 0) && modoVisualizacaoMinhaLista == 0) {
            modoVisualizacaoMinhaLista = 1
        }
    }

    LaunchedEffect(textoPesquisa) {
        delay(400)
        viewModel.atualizarQueryEFiltrarPaginado(textoPesquisa, tipoPaginado)
        if (textoPesquisa.isNotBlank()) {
            historicoManager.adicionarTermo(textoPesquisa)
            historicoBuscas = historicoManager.obterHistorico()
        }
    }

    if (mostrarDialogoGerenciarColecoes) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoGerenciarColecoes = false },
            title = { Text("Gerenciar Coleções", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selecione uma coleção para excluir:", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    val colecoesCustom = colecoesDisponiveis.filter { it != "Geral" }
                    if (colecoesCustom.isEmpty()) {
                        Text("Nenhuma coleção customizada criada ainda.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        for (col in colecoesCustom) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(col, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                IconButton(onClick = { colecaoParaExcluir = col }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, contentDescription = "Excluir Coleção", tint = Color(0xFFFF5252)) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrarDialogoGerenciarColecoes = false }) { Text("Fechar", fontWeight = FontWeight.Bold) } }
        )
    }

    if (mostrarBottomSheetFiltrosMinhaLista) {
        ModalBottomSheet(onDismissRequest = { mostrarBottomSheetFiltrosMinhaLista = false }, containerColor = MaterialTheme.colorScheme.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Filtros da Minha Lista", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (quantidadeFiltrosAtivosMinhaLista > 0) {
                        TextButton(onClick = { categoriaSelecionada = "Todos"; colecaoSelecionada = "Geral"; filtroPlataforma = "Todas"; filtroStatusMinhaLista = "Ativos"; ordenacaoMinhaLista = "Padrão (Assistindo primeiro)" }) { Text("Redefinir", color = Color(0xFFFF5252)) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Coleção Temática:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary); FlowRowWithSpacing(items = colecoesDisponiveis) { col -> FilterChip(selected = (colecaoSelecionada == col), onClick = { colecaoSelecionada = col }, label = { Text(col, fontSize = 12.sp) }) } }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Categoria:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary); FlowRowWithSpacing(items = categoriasMinhaLista) { cat -> FilterChip(selected = (categoriaSelecionada == cat), onClick = { categoriaSelecionada = cat }, label = { Text(cat, fontSize = 12.sp) }) } }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Exibir Status:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary); FlowRowWithSpacing(items = listOf("Ativos", "Favoritos", "Quero Assistir", "Assistindo", "Concluído", "Todos")) { s -> FilterChip(selected = (filtroStatusMinhaLista == s), onClick = { filtroStatusMinhaLista = s }, label = { Text(s, fontSize = 12.sp) }) } }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Ordenar por:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary); FlowRowWithSpacing(items = opcoesOrdenacaoMinhaLista) { opt -> FilterChip(selected = (ordenacaoMinhaLista == opt), onClick = { ordenacaoMinhaLista = opt }, label = { Text(opt, fontSize = 12.sp) }) } }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Coleções & Maratonas Temáticas:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { mostrarDialogoPlaylistsPrincipal = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🏷️", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Playlists Temáticas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA), maxLines = 1)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { mostrarDialogoRadarEstreias = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🔔", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Radar de Estreias", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), maxLines = 1)
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Meus Streamings:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { mostrarDialogoMeusStreamings = true }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Text("⚙️ Configurar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    val meusStreamingsQueryML = StreamingManager.obterQueryMeusStreamings(contextoLocal)
                    FilterChip(
                        selected = filtroApenasMeusStreamingsMinhaLista,
                        onClick = {
                            if (meusStreamingsQueryML != null) {
                                filtroApenasMeusStreamingsMinhaLista = !filtroApenasMeusStreamingsMinhaLista
                            } else {
                                mostrarDialogoMeusStreamings = true
                            }
                        },
                        label = { Text(if (meusStreamingsQueryML != null) "📺 Apenas disponível nos meus streamings" else "📺 Configurar meus streamings", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = if (filtroApenasMeusStreamingsMinhaLista) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                    )
                }
                if (streamingsFiltro.size > 1) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Plataforma:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary); FlowRowWithSpacing(items = streamingsFiltro) { st -> FilterChip(selected = (filtroPlataforma == st), onClick = { filtroPlataforma = st }, label = { Text(st, fontSize = 12.sp) }) } } }
                Button(onClick = { mostrarBottomSheetFiltrosMinhaLista = false }, modifier = Modifier.fillMaxWidth()) { Text("Aplicar Filtros", fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (mostrarBottomSheetFiltrosDescobrir) {
        ModalBottomSheet(onDismissRequest = { mostrarBottomSheetFiltrosDescobrir = false }, containerColor = MaterialTheme.colorScheme.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Filtros de Descoberta", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (quantidadeFiltrosAtivosDescobrir > 0) {
                        TextButton(onClick = { viewModel.selecionarTipo("Todos"); viewModel.selecionarProvedorStreaming(null); viewModel.selecionarGenero(null); viewModel.selecionarOrdenacao("popularity.desc") }) { Text("Redefinir Tudo", color = Color(0xFFFF5252)) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Tipo de Conteúdo:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary); FlowRowWithSpacing(items = tiposDisponiveis) { tipo -> FilterChip(selected = (tipoPaginado == tipo), onClick = { viewModel.selecionarTipo(tipo) }, label = { Text(tipo, fontSize = 12.sp) }) } }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Ordenar por:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary); FlowRowWithSpacing(items = opcoesOrdenacaoDescobrir.map { it.first }) { rotulo -> val chaveSort = opcoesOrdenacaoDescobrir.first { it.first == rotulo }.second; FilterChip(selected = (ordenacaoSelecionada == chaveSort), onClick = { viewModel.selecionarOrdenacao(chaveSort) }, label = { Text(rotulo, fontSize = 12.sp) }) } }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Plataformas de Streaming:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { mostrarDialogoMeusStreamings = true }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Text("⚙️ Configurar Meus", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    val filtroMeusStreamingsAtivo by viewModel.filtroMeusStreamingsAtivo.collectAsState()
                    val meusStreamingsQuery = StreamingManager.obterQueryMeusStreamings(contextoLocal)
                    FilterChip(
                        selected = filtroMeusStreamingsAtivo,
                        onClick = {
                            if (meusStreamingsQuery != null) {
                                viewModel.alternarFiltroMeusStreamings(meusStreamingsQuery)
                            } else {
                                mostrarDialogoMeusStreamings = true
                            }
                        },
                        label = { Text(if (meusStreamingsQuery != null) "📺 Disponível nos meus streamings" else "📺 Configurar meus streamings", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = if (filtroMeusStreamingsAtivo) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                    )
                    FlowRowWithSpacing(items = provedoresDisponiveis) { (nome, id) -> FilterChip(selected = (provedorSelecionadoId == id), onClick = { viewModel.selecionarProvedorStreaming(id) }, label = { Text(nome, fontSize = 12.sp) }) }
                }
                val listaGenerosExibir = if (tipoPaginado == "Série" || tipoPaginado == "Anime") generosSeries else generosFilmes
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Gêneros:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary); FlowRowWithSpacing(items = listaGenerosExibir) { (nome, id) -> FilterChip(selected = (generoSelecionadoId == id), onClick = { viewModel.selecionarGenero(id) }, label = { Text(nome, fontSize = 12.sp) }) } }
                Button(onClick = { mostrarBottomSheetFiltrosDescobrir = false }, modifier = Modifier.fillMaxWidth()) { Text("Aplicar Filtros", fontWeight = FontWeight.Bold) }
            }
        }
    }

    
    // ========================================================
    // 🚀 BOTTOM SHEET DO CHAT DE GRUPO
    // ========================================================
    if (mostrarChatSala) {
        val listState = rememberLazyListState()
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val formatoHora = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }

        var msgMenuExpandido by remember { mutableStateOf<String?>(null) }
        var msgSendoEditada by remember { mutableStateOf<MensagemGrupo?>(null) }
        var msgParaApagar by remember { mutableStateOf<MensagemGrupo?>(null) }
        var mostrarDialogoEnquete by remember { mutableStateOf(false) }

        LaunchedEffect(mensagensSala.size) {
            if (mensagensSala.isNotEmpty()) {
                listState.animateScrollToItem(mensagensSala.size - 1)
            }
        }

        if (msgParaApagar != null) {
            AlertDialog(
                onDismissRequest = { msgParaApagar = null },
                title = { Text("Apagar Mensagem", fontWeight = FontWeight.Bold) },
                text = { Text("Deseja realmente apagar esta mensagem da sala para todos?") },
                confirmButton = {
                    Button(onClick = { viewModel.deletarMensagemSala(msgParaApagar!!.id); msgParaApagar = null; mostrarSucesso("Mensagem apagada") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))) { Text("Apagar", color = Color.White) }
                },
                dismissButton = { TextButton(onClick = { msgParaApagar = null }) { Text("Cancelar") } }
            )
        }

        ModalBottomSheet(
            onDismissRequest = { mostrarChatSala = false },
            containerColor = MaterialTheme.colorScheme.background,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Chat: $nomeListaAtiva", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { mostrarCinePartySala = true }) {
                            Text("🍿", fontSize = 18.sp)
                        }
                        IconButton(onClick = { mostrarMatchColetivoSala = true }) {
                            Text("👥", fontSize = 18.sp)
                        }

                        IconButton(onClick = { mostrarDialogoEnquetes = true }) {
                            Icon(
                                imageVector = Icons.Default.Poll,
                                contentDescription = "Votações da Sala",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.alternarMuteSala(); mostrarSucesso(if (salaMutada) "Notificações da sala ativadas" else "Notificações da sala silenciadas") }) {
                            Icon(
                                imageVector = if (salaMutada) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                contentDescription = "Silenciar Sala",
                                tint = if (salaMutada) Color.Gray else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = { mostrarChatSala = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                val matchColetivoSessao by viewModel.sessaoMatchColetivoAtiva.collectAsState(initial = null)
                if (matchColetivoSessao != null && matchColetivoSessao?.estado == "ATIVO") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEC4899).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { mostrarMatchColetivoSala = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👥", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CineMatch Coletivo em andamento!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                                Text("Vote para encontrar o filme que todos querem assistir", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Votar ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                        }
                    }
                }

                val enquetesSalaChat by viewModel.enquetesSalaAtiva.collectAsState(initial = emptyList())
                val enqueteAbertaChat = enquetesSalaChat.firstOrNull { !it.encerrada }
                if (enqueteAbertaChat != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { mostrarDialogoEnquetes = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Poll, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Votação aberta: ${enqueteAbertaChat.titulo}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Toque para votar (${enqueteAbertaChat.votos.size} voto(s))", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(mensagensSala) { index, msg ->
                        val souEu = msg.autorUid == meuUid
                        val mensagemAnterior = if (index > 0) mensagensSala[index - 1] else null
                        val mesmoAutorDaAnterior = mensagemAnterior != null && mensagemAnterior.autorUid == msg.autorUid
                        val margemTopo = if (mesmoAutorDaAnterior) 2.dp else 12.dp

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = margemTopo),
                            horizontalArrangement = if (souEu) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (!souEu) {
                                if (!mesmoAutorDaAnterior) {
                                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                        if (msg.autorFotoUrl.isNotBlank()) AsyncImage(model = msg.autorFotoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        else Text(msg.autorNome.take(1).uppercase(), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(28.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column(horizontalAlignment = if (souEu) Alignment.End else Alignment.Start) {
                                if (!souEu && !mesmoAutorDaAnterior) {
                                    Text(msg.autorNome, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 2.dp, start = 4.dp))
                                }

                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (!souEu && !mesmoAutorDaAnterior) 4.dp else 16.dp, bottomEnd = if (souEu && !mesmoAutorDaAnterior) 4.dp else 16.dp),
                                        color = if (souEu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.widthIn(max = 280.dp).combinedClickable(onClick = {}, onLongClick = { if (souEu) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); msgMenuExpandido = msg.id } })
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            when (msg.tipoMensagem) {
                                                "ENQUETE" -> ConteudoCardEnquete(msg, souEu)
                                                "INDICAÇÃO" -> ConteudoCardIndicacao(msg, souEu, onTmdbItemClique)
                                                "DEBATE" -> ConteudoCardDebate(msg, souEu)
                                                else -> Text(text = msg.texto, color = if (souEu) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 20.sp)
                                            }
                                            Row(modifier = Modifier.align(Alignment.End).padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                if (msg.editada) Text(text = "(editado)", color = if (souEu) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 9.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                Text(text = formatoHora.format(java.util.Date(msg.timestamp)), color = if (souEu) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = msgMenuExpandido == msg.id,
                                        onDismissRequest = { msgMenuExpandido = null },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(text = { Text("Editar Mensagem", fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { inputMensagemSala = msg.texto; msgSendoEditada = msg; msgMenuExpandido = null })
                                        DropdownMenuItem(text = { Text("Apagar para todos", color = Color(0xFFFF4C4C), fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4C4C)) }, onClick = { msgParaApagar = msg; msgMenuExpandido = null })
                                    }
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(top = 8.dp, bottom = 8.dp)) {
                    if (msgSendoEditada != null) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 8.dp, end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Text("A editar mensagem...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { msgSendoEditada = null; inputMensagemSala = "" }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp)) }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = inputMensagemSala,
                            onValueChange = { inputMensagemSala = it },
                            placeholder = { Text("Mensagem...", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = false,
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            leadingIcon = {
                                IconButton(onClick = { /* O teclado nativo abre os emojis */ }) {
                                    Icon(Icons.Default.Face, contentDescription = "Emojis", tint = MaterialTheme.colorScheme.secondary)
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { mostrarDialogoEnquete = true }) {
                                    Icon(Icons.Default.Poll, contentDescription = "Criar Enquete", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputMensagemSala.isNotBlank()) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    if (msgSendoEditada != null) {
                                        viewModel.editarMensagemSala(msgSendoEditada!!.id, inputMensagemSala)
                                        msgSendoEditada = null
                                        mostrarSucesso("Mensagem editada")
                                    } else {
                                        viewModel.enviarMensagemParaSala(inputMensagemSala)
                                    }
                                    inputMensagemSala = ""
                                }
                            },
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .size(52.dp)
                        ) {
                            Icon(
                                imageVector = if (msgSendoEditada != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp).offset(x = if (msgSendoEditada != null) 0.dp else 2.dp)
                            )
                        }
                    }
                }
            }
        }

        if (mostrarDialogoEnquete) {
            DialogoCriarEnquete(
                onDispensar = { mostrarDialogoEnquete = false },
                onCriarEnquete = { pergunta, op1, op2, op3 ->
                    val payload = listOf(op1, op2, op3).filter { it.isNotBlank() }.joinToString("|||")
                    viewModel.enviarMensagemParaSala(pergunta, tipoMensagem = "ENQUETE", mediaUrl = payload)
                    mostrarDialogoEnquete = false
                    mostrarSucesso("Enquete enviada! 📊")
                }
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(12.dp)
                )
            }
        },
        floatingActionButton = {
            if (isModoCompartilhado) {
                BadgedBox(
                    badge = {
                        if (mensagensNaoLidas > 0) {
                            Badge(
                                containerColor = Color(0xFFFF3366),
                                contentColor = Color.White
                            ) { Text(mensagensNaoLidas.toString()) }
                        }
                    }
                ) {
                    FloatingActionButton(
                        onClick = { mostrarChatSala = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.Forum, contentDescription = "Abrir Chat da Sala")
                    }
                }
            }
        },
        topBar = {
            TopBarModernaTranslucida(
                title = {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { menuSalasExpandido = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("CineList", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Trocar de Lista", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(nomeListaAtiva, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }

                    DropdownMenu(
                        expanded = menuSalasExpandido,
                        onDismissRequest = { menuSalasExpandido = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(text = { Text("👤 Minha Lista Pessoal", fontWeight = if (casalIdAtivo.isBlank()) FontWeight.Bold else FontWeight.Normal) }, onClick = { viewModel.selecionarGrupoAtivo(""); menuSalasExpandido = false; mostrarSucesso("A minha lista pessoal") })
                        if (gruposSalvos.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            for (grupo in gruposSalvos) {
                                val isAtivo = casalIdAtivo == grupo.grupoId
                                DropdownMenuItem(text = { Text("🍿 ${grupo.nomeGrupo.ifBlank { "Sala" }}", fontWeight = if (isAtivo) FontWeight.Bold else FontWeight.Normal) }, onClick = { viewModel.selecionarGrupoAtivo(grupo.grupoId); menuSalasExpandido = false; mostrarSucesso("Lista \"${grupo.nomeGrupo}\" selecionada") })
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        DropdownMenuItem(text = { Text("⚙️ Gerenciar Salas / Criar Nova") }, onClick = { menuSalasExpandido = false; mostrarDialogoGerenciarSalas = true })
                    }
                },
                actions = {
                    if (isModoCompartilhado) {
                        PulseMatchIcon {
                            Button(
                                onClick = onAbrirMatch,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) { Text("Match 🍿", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    }
                    IconeBouncyIconButton(onClick = { mostrarDialogoCineBot = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "CineAI Assistente", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconeAnimadoRotacao(onClick = { mostrarDialogoSorteio = true }, anguloRotacaoTotal = 360f) {
                        Icon(Icons.Default.Casino, contentDescription = "O Que Assistir Hoje", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconeBouncyIconButton(onClick = onPerfilClique) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        MeshBackground3D(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            val cyberTabsPrincipal = remember(isModoCompartilhado) {
                if (isModoCompartilhado) {
                    listOf(
                        CyberTabItem(titulo = "Lista da Sala", icone = Icons.Default.VideoLibrary),
                        CyberTabItem(titulo = "Descobrir", icone = Icons.Default.Explore)
                    )
                } else {
                    listOf(
                        CyberTabItem(titulo = "Minha Lista", icone = Icons.Default.VideoLibrary),
                        CyberTabItem(titulo = "Descobrir", icone = Icons.Default.Explore),
                        CyberTabItem(titulo = "Feed Social", icone = Icons.Default.DynamicFeed)
                    )
                }
            }

            CyberSegmentedTabBar(
                tabs = cyberTabsPrincipal,
                selectedIndex = pagerState.currentPage,
                onTabSelected = { idx ->
                    focusManager.clearFocus()
                    coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                }
            )

            if (isModoCompartilhado && membroAssistindo != null && pagerState.currentPage == 0) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    CyberLiveRadarBanner(
                        nomeMembro = membroAssistindo.nome,
                        tituloMidia = membroAssistindo.assistindoAgoraTitulo,
                        detalheEpisodio = membroAssistindo.assistindoAgoraEpisodio
                    )
                }
            }

            if (pagerState.currentPage != 2) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CyberSearchHUD(
                            query = textoPesquisa,
                            onQueryChange = { textoPesquisa = it },
                            placeholder = if (pagerState.currentPage == 0) "Buscar por título, gênero ou streaming..." else "Buscar online no TMDB...",
                            isFocused = searchFocused,
                            onClear = {
                                textoPesquisa = ""
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.onFocusChanged { state -> searchFocused = state.isFocused }
                        )

                        if (searchFocused && textoPesquisa.isBlank() && historicoBuscas.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0F1526).copy(alpha = 0.95f))
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            PulsarPontoNeon(cor = Color(0xFF00E5FF), tamanho = 6.dp)
                                            Text(
                                                text = "HISTÓRICO DE BUSCAS // TELEMETRIA",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF00E5FF),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                historicoManager.limparHistorico()
                                                historicoBuscas = emptyList()
                                                focusManager.clearFocus()
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Limpar", fontSize = 10.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    for (termo in historicoBuscas) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    textoPesquisa = termo
                                                    focusManager.clearFocus()
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF00E5FF).copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                                            Text(termo, fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pagina ->
                if (pagina == 0) {
                    val listaFiltrada = listaDeMidias.filter { midia ->
                        val bateSugestao = midia.statusSugestao == "APROVADO"
                        val termoBusca = textoPesquisa.trim()
                        val bateTexto = termoBusca.isBlank() || midia.titulo.contains(termoBusca, ignoreCase = true) || midia.genero.contains(termoBusca, ignoreCase = true) || midia.plataforma.contains(termoBusca, ignoreCase = true)
                        val bateCategoria = if (categoriaSelecionada == "Todos") true else midia.tipo.equals(when (categoriaSelecionada) { "Filmes" -> "Filme"; "Séries" -> "Série"; "Animes" -> "Anime"; "Novelas" -> "Novela"; "Doramas" -> "Dorama"; else -> "" }, ignoreCase = true)
                        val bateColecao = if (colecaoSelecionada == "Geral") true else midia.listaCustomizada.equals(colecaoSelecionada, ignoreCase = true)
                        val batePlataforma = if (filtroPlataforma == "Todas") true else midia.plataforma.contains(filtroPlataforma, ignoreCase = true)
                        val statusEnum = midia.obterStatusEnum()
                        val bateStatus = when (filtroStatusMinhaLista) {
                            "Ativos" -> statusEnum != StatusMidia.CONCLUIDO
                            "Favoritos" -> midia.favorito
                            "Quero Assistir" -> statusEnum == StatusMidia.QUERO_ASSISTIR
                            "Assistindo" -> statusEnum == StatusMidia.ASSISTINDO
                            "Concluído" -> statusEnum == StatusMidia.CONCLUIDO
                            else -> true
                        }
                        val bateMeusStreamings = if (!filtroApenasMeusStreamingsMinhaLista) true else StreamingManager.midiaDisponivelNosMeusStreamings(contextoLocal, midia.plataforma)
                        bateSugestao && bateTexto && bateCategoria && bateColecao && batePlataforma && bateStatus && bateMeusStreamings
                    }.let { lista ->
                        when (ordenacaoMinhaLista) {
                            "Favoritos Primeiro" -> lista.sortedWith(compareByDescending<Midia> { it.favorito }.thenByDescending { it.obterStatusEnum() == StatusMidia.ASSISTINDO })
                            "Melhor Avaliados" -> lista.sortedByDescending { it.nota }
                            "Ordem Alfabética (A-Z)" -> lista.sortedBy { it.titulo.lowercase() }
                            "Adicionados Recentemente" -> lista.sortedByDescending { it.id }
                            // 🎬 "Padrão": o que estou a ver/vi por último sobe ao topo.
                            // Assim que o episódio é alterado, a série passa para a frente,
                            // revealing exatamente onde ficou o progresso.
                            else -> lista.sortedWith(
                                compareByDescending<Midia> { it.pesoPrioridade }
                                    .thenByDescending { it.atualizadoEm }
                            )
                        }
                    }

                    var isRefreshing by remember { mutableStateOf(false) }

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { coroutineScope.launch { isRefreshing = true; focusManager.clearFocus(); viewModel.forcarSincronizacaoManual(); delay(800); isRefreshing = false; mostrarSucesso("Sincronização concluída") } },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (listaFiltrada.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhum item encontrado com esses filtros.", color = MaterialTheme.colorScheme.secondary) }
                            } else {
                                // 🚀 MOSTRAR SUGESTÕES PENDENTES EM TODOS OS MODOS
                                val listaPendentes = listaDeMidias.filter { it.statusSugestao == "PENDENTE" }
                                val listaRecusados = listaDeMidias.filter { it.statusSugestao == "RECUSADO" }

                                // 🔖 Onde parei: séries com episódio/temporada avançada,
                                // ordenadas pela mais recentemente alterada.
                                // 🔖 Onde parei: APENAS A ÚLTIMA série cujo episódio foi editado
                                val listaOndeParei = listaFiltrada
                                    .filter {
                                        it.temProgressoEpisodio &&
                                                it.statusSugestao == "APROVADO" &&
                                                it.obterStatusEnum() != StatusMidia.CONCLUIDO
                                    }
                                    .sortedByDescending { it.atualizadoEm }
                                    .take(1) // 🚀 Pega SOMENTE na última série que foi atualizada!
                                    .sortedByDescending { it.atualizadoEm }
                                val listaAssistindo = listaFiltrada.filter { it.obterStatusEnum() == StatusMidia.ASSISTINDO && it.statusSugestao == "APROVADO" && it.uuid != listaOndeParei.firstOrNull()?.uuid }
                                val listaFavoritos = listaFiltrada.filter { it.favorito && it.statusSugestao == "APROVADO" }
                                val listaQueroAssistir = listaFiltrada.filter { it.obterStatusEnum() == StatusMidia.QUERO_ASSISTIR && it.statusSugestao == "APROVADO" }
                                val listaConcluidos = listaFiltrada.filter { it.obterStatusEnum() == StatusMidia.CONCLUIDO && it.statusSugestao == "APROVADO" }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Barra de cabeçalho fixa
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("${listaFiltrada.size} título(s) exibido(s)", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { modoVisualizacaoMinhaLista = (modoVisualizacaoMinhaLista + 1) % 3 }, modifier = Modifier.size(32.dp)) {
                                                Icon(
                                                    when (modoVisualizacaoMinhaLista) {
                                                        0 -> Icons.Default.AutoAwesome
                                                        1 -> Icons.Default.GridView
                                                        else -> Icons.AutoMirrored.Filled.ViewList
                                                    },
                                                    "Alternar Visualização",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            if (quantidadeFiltrosAtivosMinhaLista > 0 || textoPesquisa.isNotBlank()) {
                                                IconButton(onClick = { textoPesquisa = ""; focusManager.clearFocus(); categoriaSelecionada = "Todos"; colecaoSelecionada = "Geral"; filtroPlataforma = "Todas"; filtroStatusMinhaLista = "Ativos"; ordenacaoMinhaLista = "Padrão (Assistindo primeiro)" }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Clear, "Limpar Filtros", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            AssistChip(
                                                onClick = { focusManager.clearFocus(); mostrarBottomSheetFiltrosMinhaLista = true },
                                                label = { Text(if (quantidadeFiltrosAtivosMinhaLista > 0) "Filtros ($quantidadeFiltrosAtivosMinhaLista)" else "Filtros", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                leadingIcon = { Icon(Icons.Default.FilterList, "Filtros", tint = if (quantidadeFiltrosAtivosMinhaLista > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = if (quantidadeFiltrosAtivosMinhaLista > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                                    labelColor = if (quantidadeFiltrosAtivosMinhaLista > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                    }

                                    // 🚀 SUGESTÕES PENDENTES - visíveis em todos os modos
                                    if (listaPendentes.isNotEmpty() || listaRecusados.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "${listaPendentes.size + listaRecusados.size} sugestão(ões) aguardando",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                TextButton(onClick = { mostrarDialogoSugestoes = true }) {
                                                    Text("Ver", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    // Conteúdo principal com os diferentes modos de visualização
                                    when (modoVisualizacaoMinhaLista) {
                                        0 -> {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(bottom = 24.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                // 🔖 Secção dedicada ao ponto de retorno das séries
                                                // 🔖 Secção dedicada apenas à última série editada
                                                if (listaOndeParei.isNotEmpty()) {
                                                    val ultimaSerie = listaOndeParei.first()
                                                    val dataTexto = if (ultimaSerie.atualizadoEm > 0L) " (${formatarDataRelativa(ultimaSerie.atualizadoEm)})" else ""
                                                    item {
                                                        Column(modifier = Modifier.fillMaxWidth()) {
                                                            Text(
                                                                text = "🔖 Onde parei",
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onBackground,
                                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                                            )
                                                            Text(
                                                                text = "A última série em que atualizou o progresso$dataTexto",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.secondary,
                                                                modifier = Modifier.padding(horizontal = 16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            BannerOndePareiHorizontal(
                                                                midia = ultimaSerie,
                                                                onClick = { onItemClique(ultimaSerie) },
                                                                onIncrementarEpisodio = {
                                                                    viewModel.incrementarEpisodioRapido(ultimaSerie)
                                                                    mostrarSucesso("Episódio avançado em \"${ultimaSerie.titulo}\"")
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                                if (listaAssistindo.isNotEmpty()) { item { CarrosselMinhaLista("▶️ Continuar Assistindo", listaAssistindo, onItemClique, onIncrementarEpisodio = { m -> viewModel.incrementarEpisodioRapido(m); mostrarSucesso("Episódio avançado em \"${m.titulo}\"") }, onDeletar = { midiaParaExcluir = it }, onAlternarStatusConcluido = { midiaParaConcluir = it }, onAlternarFavorito = { m -> viewModel.alternarFavorito(m); mostrarSucesso(if (m.favorito) "Removido dos favoritos" else "Adicionado aos favoritos ❤️") }) } }
                                                if (listaFavoritos.isNotEmpty()) { item { CarrosselMinhaLista("❤️ Meus Favoritos", listaFavoritos, onItemClique, onIncrementarEpisodio = { m -> viewModel.incrementarEpisodioRapido(m); mostrarSucesso("Episódio avançado em \"${m.titulo}\"") }, onDeletar = { midiaParaExcluir = it }, onAlternarStatusConcluido = { midiaParaConcluir = it }, onAlternarFavorito = { m -> viewModel.alternarFavorito(m); mostrarSucesso(if (m.favorito) "Removido dos favoritos" else "Adicionado aos favoritos ❤️") }) } }
                                                if (listaQueroAssistir.isNotEmpty()) { item { CarrosselMinhaLista("👀 Quero Assistir", listaQueroAssistir, onItemClique, onIncrementarEpisodio = { m -> viewModel.incrementarEpisodioRapido(m); mostrarSucesso("Episódio avançado em \"${m.titulo}\"") }, onDeletar = { midiaParaExcluir = it }, onAlternarStatusConcluido = { midiaParaConcluir = it }, onAlternarFavorito = { m -> viewModel.alternarFavorito(m); mostrarSucesso(if (m.favorito) "Removido dos favoritos" else "Adicionado aos favoritos ❤️") }) } }
                                                if (listaConcluidos.isNotEmpty()) { item { CarrosselMinhaLista("✅ Concluídos", listaConcluidos, onItemClique, onIncrementarEpisodio = { m -> viewModel.incrementarEpisodioRapido(m); mostrarSucesso("Episódio avançado em \"${m.titulo}\"") }, onDeletar = { midiaParaExcluir = it }, onAlternarStatusConcluido = { midiaParaConcluir = it }, onAlternarFavorito = { m -> viewModel.alternarFavorito(m); mostrarSucesso(if (m.favorito) "Removido dos favoritos" else "Adicionado aos favoritos ❤️") }) } }
                                            }
                                        }
                                        1 -> {
                                            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                                                itemsIndexed(listaFiltrada, key = { index, mi -> "${mi.uuid.ifBlank { mi.id.toString() }}_$index" }) { _, mi -> ItemMidiaCard(midia = mi, onClick = { focusManager.clearFocus(); onItemClique(mi) }, onIncrementarEpisodio = { viewModel.incrementarEpisodioRapido(mi); mostrarSucesso("Episódio avançado em \"${mi.titulo}\"") }, onDeletar = { midiaParaExcluir = mi }, onAlternarStatusConcluido = { midiaParaConcluir = mi }, onAlternarFavorito = { viewModel.alternarFavorito(mi); mostrarSucesso(if (mi.favorito) "Removido dos favoritos" else "Adicionado aos favoritos ❤️") }, modoListaHorizontal = false, jaAdicionado = false, onAdicionarRapido = null, onAceitarPendente = { viewModel.aceitarMidiaPendente(mi, maxOf(2, membrosSala.size)) }, onRecusarPendente = { midiaParaRecusar = mi }, onProcessarRecusado = { quer -> viewModel.processarMidiaRecusadaPeloParceiro(mi, quer) }) }
                                            }
                                        }
                                        2 -> {
                                            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                itemsIndexed(listaFiltrada, key = { index, mi -> "${mi.uuid.ifBlank { mi.id.toString() }}_$index" }) { _, mi -> ItemMidiaCard(midia = mi, onClick = { focusManager.clearFocus(); onItemClique(mi) }, onIncrementarEpisodio = { viewModel.incrementarEpisodioRapido(mi); mostrarSucesso("Episódio avançado em \"${mi.titulo}\"") }, onDeletar = { midiaParaExcluir = mi }, onAlternarStatusConcluido = { midiaParaConcluir = mi }, onAlternarFavorito = { viewModel.alternarFavorito(mi); mostrarSucesso(if (mi.favorito) "Removido dos favoritos" else "Adicionado aos favoritos ❤️") }, modoListaHorizontal = true, jaAdicionado = false, onAdicionarRapido = null, onAceitarPendente = { viewModel.aceitarMidiaPendente(mi, maxOf(2, membrosSala.size)) }, onRecusarPendente = { midiaParaRecusar = mi }, onProcessarRecusado = { quer -> viewModel.processarMidiaRecusadaPeloParceiro(mi, quer) }) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (pagina == 1) {
                    // Modo Descobrir (não compartilhado)
                    Column(modifier = Modifier.fillMaxSize()) {
                        LaunchedEffect(textoPesquisa, quantidadeFiltrosAtivosDescobrir) {
                            if ((textoPesquisa.isNotBlank() || quantidadeFiltrosAtivosDescobrir > 0) && modoVisualizacaoDescobrir == 0) { modoVisualizacaoDescobrir = 1 }
                        }

                        val mostrarLayoutNetflix = modoVisualizacaoDescobrir == 0 && textoPesquisa.isBlank() && quantidadeFiltrosAtivosDescobrir == 0
                        val modoListaDescobrir = modoVisualizacaoDescobrir == 2

                        var isRefreshingDescobrir by remember { mutableStateOf(false) }
                        var refreshKeyDescobrir by remember { mutableIntStateOf(0) }

                        PullToRefreshBox(
                            isRefreshing = isRefreshingDescobrir,
                            onRefresh = {
                                coroutineScope.launch {
                                    isRefreshingDescobrir = true
                                    focusManager.clearFocus()
                                    viewModel.carregarCarrosseisDescobrir(forcar = true)
                                    viewModel.forcarSincronizacaoManual()
                                    refreshKeyDescobrir++
                                    delay(1200)
                                    isRefreshingDescobrir = false
                                    mostrarSucesso("Descobrir atualizado")
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (mostrarLayoutNetflix) {
                                val carrosselPopulares by viewModel.carrosselPopulares.collectAsState()
                                val carrosselSeriesAlta by viewModel.carrosselSeriesAlta.collectAsState()
                                val carrosselComedias by viewModel.carrosselComedias.collectAsState()
                                val carrosselAcao by viewModel.carrosselAcao.collectAsState()
                                val lancamentosStreaming by viewModel.secoesLancamentosStreaming.collectAsState()
                                val carrosseisGenerosFav by viewModel.carrosseisGenerosFavoritos.collectAsState()
                                val carregandoCarrosseis by viewModel.carregandoCarrosseis.collectAsState()

                                LaunchedEffect(Unit) {
                                    viewModel.carregarNovidadesPorStreaming(contextoLocal)
                                    viewModel.carregarCarrosseisGenerosFavoritos(contextoLocal)
                                }

                                if (carregandoCarrosseis && carrosselPopulares.isEmpty()) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(3) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(16.dp)
                                                        .width(150.dp)
                                                        .height(20.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                                )
                                                LazyRow(
                                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    items(3) {
                                                        Box(modifier = Modifier.width(140.dp)) {
                                                            ItemMidiaCardSkeleton()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val destaquesRotativos = remember(carrosselPopulares, refreshKeyDescobrir) { carrosselPopulares.take(15).shuffled().take(5) }
                                    val listaPopularesEmbaralhada = remember(carrosselPopulares, refreshKeyDescobrir) { carrosselPopulares.shuffled() }
                                    val listaSeriesAltaEmbaralhada = remember(carrosselSeriesAlta, refreshKeyDescobrir) { carrosselSeriesAlta.shuffled() }
                                    val listaComediasEmbaralhada = remember(carrosselComedias, refreshKeyDescobrir) { carrosselComedias.shuffled() }
                                    val listaAcaoEmbaralhada = remember(carrosselAcao, refreshKeyDescobrir) { carrosselAcao.shuffled() }

                                    // Se o token do TMDB não estiver configurado, explica o motivo
                                    // em vez de mostrar uma aba vazia sem contexto.
                                    if (!RetrofitClient.tokenConfigurado) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                "Token do TMDB não configurado",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Adicione a linha abaixo ao ficheiro local.properties na raiz do projeto e recompile:\n\ntmdb.token=SEU_TOKEN_V4_DO_TMDB",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else if (!carregandoCarrosseis && carrosselPopulares.isEmpty() && carrosselSeriesAlta.isEmpty()) {
                                        // Está carregado, mas tudo veio vazio → provável falha de rede/API
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.WifiOff,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                "Não foi possível carregar as sugestões",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Verifica a tua ligação e o token do TMDB.\nPuxa para baixo para tentar novamente.",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { viewModel.carregarCarrosseisDescobrir(forcar = true) },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Tentar novamente", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 0.dp, bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(24.dp)
                                    ) {
                                        if (carrosselPopulares.isNotEmpty()) {
                                            item { HeroBannerDestaque(listaDestaques = destaquesRotativos, listaDeMidiasLocal = listaDeMidias, onTmdbItemClique = { item, tipo -> focusManager.clearFocus(); onTmdbItemClique(item, tipo) }, onAdicionarClique = onAdicionarClique) }
                                        }

                                        item {
                                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "Exibindo: $tipoPaginado", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = { modoVisualizacaoDescobrir = (modoVisualizacaoDescobrir + 1) % 3 }, modifier = Modifier.size(32.dp)) {
                                                        val iconView = when (modoVisualizacaoDescobrir) {
                                                            0 -> Icons.Default.AutoAwesome
                                                            1 -> Icons.Default.GridView
                                                            else -> Icons.AutoMirrored.Filled.ViewList
                                                        }
                                                        Icon(iconView, "Alternar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                    }
                                                    if (quantidadeFiltrosAtivosDescobrir > 0) {
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.selecionarTipo("Todos")
                                                                viewModel.selecionarProvedorStreaming(null)
                                                                viewModel.selecionarGenero(null)
                                                                viewModel.selecionarOrdenacao("popularity.desc")
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Clear,
                                                                contentDescription = "Limpar Filtros",
                                                                tint = Color(0xFFFF5252),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    AssistChip(onClick = { focusManager.clearFocus(); mostrarBottomSheetFiltrosDescobrir = true }, label = { Text(if (quantidadeFiltrosAtivosDescobrir > 0) "Filtros ($quantidadeFiltrosAtivosDescobrir)" else "Filtros", fontSize = 12.sp, fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Default.FilterList, "Filtros", tint = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) }, colors = AssistChipDefaults.assistChipColors(containerColor = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, labelColor = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface))
                                                }
                                            }
                                        }

                                        // 🆕 Seção de Lançamentos por Streaming estilo JustWatch (Imagem 1)
                                        if (lancamentosStreaming.isNotEmpty()) {
                                            item {
                                                SecaoLancamentosStreaming(
                                                    secoes = lancamentosStreaming,
                                                    listaDeMidiasLocal = listaDeMidias,
                                                    onTmdbItemClique = { item, tipo -> focusManager.clearFocus(); onTmdbItemClique(item, tipo) },
                                                    onAdicionarClique = onAdicionarClique,
                                                    onConfigurarStreamings = { mostrarDialogoMeusStreamings = true }
                                                )
                                            }
                                        }

                                        // 🎨 Seções personalizadas baseadas nos gêneros favoritos do usuário (Configurados no Onboarding)
                                        carrosseisGenerosFav.forEach { (genInfo, listaFilmes) ->
                                            if (listaFilmes.isNotEmpty()) {
                                                item {
                                                    CarrosselSessao(
                                                        titulo = "${genInfo.iconeEmoji} Especial ${genInfo.nome} (Para Você)",
                                                        listaTmdb = listaFilmes,
                                                        listaDeMidiasLocal = listaDeMidias,
                                                        onTmdbItemClique = { item, tipo -> focusManager.clearFocus(); onTmdbItemClique(item, tipo) },
                                                        onAdicionarClique = onAdicionarClique
                                                    )
                                                }
                                            }
                                        }

                                        item { CarrosselSessao(titulo = "🎬 Filmes Populares", listaTmdb = listaPopularesEmbaralhada, listaDeMidiasLocal = listaDeMidias, onTmdbItemClique = { item, tipo -> focusManager.clearFocus(); onTmdbItemClique(item, tipo) }, onAdicionarClique = onAdicionarClique) }
                                        item { CarrosselSessao(titulo = "🔥 Séries em Alta", listaTmdb = listaSeriesAltaEmbaralhada, listaDeMidiasLocal = listaDeMidias, onTmdbItemClique = { item, tipo -> focusManager.clearFocus(); onTmdbItemClique(item, tipo) }, onAdicionarClique = onAdicionarClique) }
                                        item { CarrosselSessao(titulo = "😂 Top Comédias", listaTmdb = listaComediasEmbaralhada, listaDeMidiasLocal = listaDeMidias, onTmdbItemClique = { item, tipo -> focusManager.clearFocus(); onTmdbItemClique(item, tipo) }, onAdicionarClique = onAdicionarClique) }
                                        item { CarrosselSessao(titulo = "💥 Adrenalina Pura", listaTmdb = listaAcaoEmbaralhada, listaDeMidiasLocal = listaDeMidias, onTmdbItemClique = { item, tipo -> focusManager.clearFocus(); onTmdbItemClique(item, tipo) }, onAdicionarClique = onAdicionarClique) }
                                    }
                                    } // fecha o else dos estados de erro/vazio
                                }
                            } else {
                                if (modoListaDescobrir) {
                                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        item {
                                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("Exibindo: $tipoPaginado", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = { modoVisualizacaoDescobrir = (modoVisualizacaoDescobrir + 1) % 3 }, modifier = Modifier.size(32.dp)) { val iconView = when (modoVisualizacaoDescobrir) { 0 -> Icons.Default.AutoAwesome; 1 -> Icons.Default.GridView; else -> Icons.AutoMirrored.Filled.ViewList }; Icon(iconView, "Alternar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                                    if (quantidadeFiltrosAtivosDescobrir > 0) { IconButton(onClick = { viewModel.selecionarTipo("Todos"); viewModel.selecionarProvedorStreaming(null); viewModel.selecionarGenero(null); viewModel.selecionarOrdenacao("popularity.desc") }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Clear, "Limpar Filtros", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp)) } }
                                                    AssistChip(onClick = { focusManager.clearFocus(); mostrarBottomSheetFiltrosDescobrir = true }, label = { Text(if (quantidadeFiltrosAtivosDescobrir > 0) "Filtros ($quantidadeFiltrosAtivosDescobrir)" else "Filtros", fontSize = 12.sp, fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Default.FilterList, "Filtros", tint = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) }, colors = AssistChipDefaults.assistChipColors(containerColor = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, labelColor = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface))
                                                }
                                            }
                                        }
                                        items(count = resultadosPaginadosApi.itemCount, key = { index -> val item = resultadosPaginadosApi.peek(index); if (item != null) "${item.mediaType ?: "midia"}_${item.idTmdb}_$index" else index }, contentType = resultadosPaginadosApi.itemContentType { "tmdb_media" }) { index ->
                                            val item = resultadosPaginadosApi[index]
                                            if (item != null) {
                                                val tipoReal = when { tipoPaginado != "Todos" -> tipoPaginado; item.mediaType.equals("tv", ignoreCase = true) -> "Série"; item.mediaType.equals("movie", ignoreCase = true) -> "Filme"; item.ehSerie -> "Série"; else -> "Filme" }
                                                val plataformaFinal = if (item.plataformaDetectada.isNotBlank()) item.plataformaDetectada else if (tipoReal.equals("Filme", ignoreCase = true)) "Cinema" else "TV / Original"
                                                val midiaItem = Midia(idTmdb = item.idTmdb, titulo = item.titulo, tipo = tipoReal, status = StatusMidia.DESCOBRIR.valor, nota = 0, sinopse = item.sinopse, imagemCapa = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "", genero = item.generoTexto, plataforma = plataformaFinal, favorito = false, listaCustomizada = "Geral")
                                                val jaNaLista = listaDeMidias.any { it.idTmdb != 0 && it.idTmdb == item.idTmdb }
                                                ItemMidiaCard(midia = midiaItem, onClick = { focusManager.clearFocus(); onTmdbItemClique(item, tipoReal) }, jaAdicionado = jaNaLista, onAdicionarRapido = { onAdicionarClique(item.idTmdb, item.titulo, tipoReal, StatusMidia.QUERO_ASSISTIR.valor, 0, item.sinopse, if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "", item.generoTexto, plataformaFinal) }, modoListaHorizontal = true)
                                            }
                                        }
                                        when (val appendState = resultadosPaginadosApi.loadState.append) {
                                            is LoadState.Loading -> { item { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(28.dp)) } } }
                                            is LoadState.Error -> { item { Text("Erro ao carregar mais itens: ${appendState.error.localizedMessage}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) } }
                                            else -> Unit
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 8.dp, bottom = 16.dp)) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("Exibindo: $tipoPaginado", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = { modoVisualizacaoDescobrir = (modoVisualizacaoDescobrir + 1) % 3 }, modifier = Modifier.size(32.dp)) { val iconView = when (modoVisualizacaoDescobrir) { 0 -> Icons.Default.AutoAwesome; 1 -> Icons.Default.GridView; else -> Icons.AutoMirrored.Filled.ViewList }; Icon(iconView, "Alternar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                                    if (quantidadeFiltrosAtivosDescobrir > 0) { IconButton(onClick = { viewModel.selecionarTipo("Todos"); viewModel.selecionarProvedorStreaming(null); viewModel.selecionarGenero(null); viewModel.selecionarOrdenacao("popularity.desc") }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Clear, "Limpar Filtros", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp)) } }
                                                    AssistChip(onClick = { focusManager.clearFocus(); mostrarBottomSheetFiltrosDescobrir = true }, label = { Text(if (quantidadeFiltrosAtivosDescobrir > 0) "Filtros ($quantidadeFiltrosAtivosDescobrir)" else "Filtros", fontSize = 12.sp, fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Default.FilterList, "Filtros", tint = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) }, colors = AssistChipDefaults.assistChipColors(containerColor = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, labelColor = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface))
                                                }
                                            }
                                        }
                                        items(count = resultadosPaginadosApi.itemCount, key = { index -> val item = resultadosPaginadosApi.peek(index); if (item != null) "${item.mediaType ?: "midia"}_${item.idTmdb}_$index" else index }, contentType = resultadosPaginadosApi.itemContentType { "tmdb_media" }) { index ->
                                            val item = resultadosPaginadosApi[index]
                                            if (item != null) {
                                                val tipoReal = when { tipoPaginado != "Todos" -> tipoPaginado; item.mediaType.equals("tv", ignoreCase = true) -> "Série"; item.mediaType.equals("movie", ignoreCase = true) -> "Filme"; item.ehSerie -> "Série"; else -> "Filme" }
                                                val plataformaFinal = if (item.plataformaDetectada.isNotBlank()) item.plataformaDetectada else if (tipoReal.equals("Filme", ignoreCase = true)) "Cinema" else "TV / Original"
                                                val midiaItem = Midia(idTmdb = item.idTmdb, titulo = item.titulo, tipo = tipoReal, status = StatusMidia.DESCOBRIR.valor, nota = 0, sinopse = item.sinopse, imagemCapa = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "", genero = item.generoTexto, plataforma = plataformaFinal, favorito = false, listaCustomizada = "Geral")
                                                val jaNaLista = listaDeMidias.any { it.idTmdb != 0 && it.idTmdb == item.idTmdb }
                                                ItemMidiaCard(midia = midiaItem, onClick = { focusManager.clearFocus(); onTmdbItemClique(item, tipoReal) }, jaAdicionado = jaNaLista, onAdicionarRapido = { onAdicionarClique(item.idTmdb, item.titulo, tipoReal, StatusMidia.QUERO_ASSISTIR.valor, 0, item.sinopse, if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "", item.generoTexto, plataformaFinal) }, modoListaHorizontal = false)
                                            }
                                        }
                                        when (val appendState = resultadosPaginadosApi.loadState.append) {
                                            is LoadState.Loading -> { item(span = { GridItemSpan(maxLineSpan) }) { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(28.dp)) } } }
                                            is LoadState.Error -> { item(span = { GridItemSpan(maxLineSpan) }) { Text("Erro ao carregar mais itens: ${appendState.error.localizedMessage}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) } }
                                            else -> Unit
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (!isModoCompartilhado && pagina == 2) {
                    FeedAtividadesScreen(
                        viewModel = viewModel,
                        onMidiaClick = { idTmdb, titulo, tipo ->
                            val fakeFilme = TmdbFilme(
                                idTmdb = idTmdb,
                                titulo = titulo,
                                mediaType = if (tipo.equals("Série", ignoreCase = true)) "tv" else "movie"
                            )
                            onTmdbItemClique(fakeFilme, tipo)
                        },
                        onAdicionarAmigoClick = onPerfilClique
                    )
                }
            }
        }
    }
}
}

// 🚀 O NOVO COMPONENTE QUE DESENHA AS SUGESTÕES
@Composable
fun CardSugestaoCasal(
    midia: Midia,
    meuUid: String,
    totalMembros: Int,
    isAdministrador: Boolean,
    onItemClique: () -> Unit,
    onAceitar: () -> Unit,
    onRecusar: () -> Unit,
    onResolverRecusado: () -> Unit,
    onRetirarSugestao: () -> Unit,
    onForcarAprovacaoAdmin: () -> Unit
) {
    val souOAutor = midia.adicionadoPor.startsWith(meuUid)
    val nomeRemetente = midia.adicionadoPor.substringAfter("_", "Parceiro(a)")
    val jaAprovei = midia.uidsAprovados.contains(meuUid)
    val votosAtuais = midia.uidsAprovados.split(",").filter { it.isNotBlank() }.size + 1 // +1 do autor

    val diasPendente = ((System.currentTimeMillis() - midia.atualizadoEm) / (1000 * 60 * 60 * 24)).toInt()
    val textoTempo = if (diasPendente == 0) "Hoje" else if (diasPendente == 1) "Ontem" else "Há $diasPendente dias"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClique() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (midia.statusSugestao) {
                        "PENDENTE" -> if (souOAutor) "⏳ Aguardando... ($votosAtuais/$totalMembros)" else "💡 $nomeRemetente sugeriu:"
                        "RECUSADO" -> if (souOAutor) "❌ A sua sugestão foi recusada." else "Sugestão recusada."
                        else -> ""
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (midia.statusSugestao == "PENDENTE") {
                    Text(textoTempo, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (midia.imagemCapa.isNotBlank()) {
                    AsyncImage(
                        model = midia.imagemCapa,
                        contentDescription = midia.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp, 66.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = midia.titulo, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "${midia.tipo} • ${midia.genero}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(text = "Toque para ver detalhes 🎬", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (midia.statusSugestao == "PENDENTE") {
                    if (souOAutor) {
                        OutlinedButton(
                            onClick = onRetirarSugestao,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text("Retirar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (jaAprovei) {
                        Text("Você aprovou ✅", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    } else {
                        OutlinedButton(
                            onClick = onRecusar,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp).padding(end = 8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text("Recusar", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onAceitar,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text("Aprovar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (midia.statusSugestao == "RECUSADO" && souOAutor) {
                    Button(
                        onClick = onResolverRecusado,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("Resolver", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (midia.statusSugestao == "PENDENTE" && isAdministrador) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "👑 Controles de Administrador:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRecusar,
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Forçar Recusa", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onForcarAprovacaoAdmin,
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Forçar Aprovação", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoSugestoesPendentes(
    listaPendentes: List<Midia>,
    listaRecusados: List<Midia>,
    meuUid: String,
    totalMembros: Int,
    isAdministrador: Boolean,
    onItemClique: (Midia) -> Unit,
    onAceitar: (Midia) -> Unit,
    onRecusar: (Midia) -> Unit,
    onResolverRecusado: (Midia) -> Unit,
    onRetirarSugestao: (Midia) -> Unit,
    onForcarAprovacaoAdmin: (Midia) -> Unit,
    onDispensar: () -> Unit
) {
    Dialog(onDismissRequest = onDispensar) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sugestões Pendentes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDispensar) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val listaMista = listaPendentes + listaRecusados

                if (listaMista.isEmpty()) {
                    Text(
                        "Nenhuma sugestão pendente",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listaMista.size) { index ->
                            val midia = listaMista[index]
                            CardSugestaoCasal(
                                midia = midia,
                                meuUid = meuUid,
                                totalMembros = totalMembros,
                                isAdministrador = isAdministrador,
                                onItemClique = { onItemClique(midia) },
                                onAceitar = { onAceitar(midia) },
                                onRecusar = { onRecusar(midia) },
                                onResolverRecusado = { onResolverRecusado(midia) },
                                onRetirarSugestao = { onRetirarSugestao(midia) },
                                onForcarAprovacaoAdmin = { onForcarAprovacaoAdmin(midia) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDispensar,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Fechar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BannerOndePareiHorizontal(
    midia: Midia,
    onClick: () -> Unit,
    onIncrementarEpisodio: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(108.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (midia.imagemCapa.isNotBlank()) {
                    AsyncImage(
                        model = midia.imagemCapa,
                        contentDescription = midia.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("SEM CAPA", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = midia.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "T${midia.temporadaAtual} • Episódio ${midia.episodioAtual}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Visto: ${formatarDataRelativa(midia.atualizadoEm)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "🍿 ${midia.plataforma.ifBlank { "TV / Original" }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onIncrementarEpisodio,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Avançar Episódio",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "+1 Ep",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun CarrosselMinhaLista(
    titulo: String,
    lista: List<Midia>,
    onItemClique: (Midia) -> Unit,
    onIncrementarEpisodio: (Midia) -> Unit,
    onDeletar: (Midia) -> Unit,
    onAlternarStatusConcluido: (Midia) -> Unit,
    onAlternarFavorito: (Midia) -> Unit
) {
    if (lista.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        // Título é opcional: quando vazio, o caller já forneceu o seu próprio cabeçalho
        if (titulo.isNotBlank()) {
            Text(
                text = titulo,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(lista, key = { index, mi -> "${mi.uuid.ifBlank { mi.id.toString() }}_$index" }) { _, mi ->
                Box(modifier = Modifier.width(140.dp)) {
                    ItemMidiaCard(
                        midia = mi,
                        onClick = { onItemClique(mi) },
                        onIncrementarEpisodio = { onIncrementarEpisodio(mi) },
                        onDeletar = { onDeletar(mi) },
                        onAlternarStatusConcluido = { onAlternarStatusConcluido(mi) },
                        onAlternarFavorito = { onAlternarFavorito(mi) },
                        modoListaHorizontal = false,
                        jaAdicionado = false,
                        onAdicionarRapido = null,
                        onAceitarPendente = {},
                        onRecusarPendente = {},
                        onProcessarRecusado = {}
                    )
                }
            }
        }
    }
}

@Composable
fun CarrosselSessao(
    titulo: String,
    listaTmdb: List<TmdbFilme>,
    listaDeMidiasLocal: List<Midia>,
    onTmdbItemClique: (TmdbFilme, String) -> Unit,
    onAdicionarClique: (Int, String, String, String, Int, String, String, String, String) -> Unit
) {
    if (listaTmdb.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = titulo,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(listaTmdb, key = { index, item -> "${item.idTmdb}_$index" }) { _, item ->
                val tipoReal = if (item.mediaType.equals("tv", ignoreCase = true) || item.ehSerie) "Série" else "Filme"
                val plataformaFinal = item.plataformaDetectada.ifBlank { if (tipoReal == "Série") "TV / Original" else "Cinema" }
                val midiaItem = Midia(idTmdb = item.idTmdb, titulo = item.titulo, tipo = tipoReal, status = StatusMidia.DESCOBRIR.valor, nota = 0, sinopse = item.sinopse, imagemCapa = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "", genero = item.generoTexto, plataforma = plataformaFinal, favorito = false, listaCustomizada = "Geral")
                val jaNaLista = listaDeMidiasLocal.any { it.idTmdb != 0 && it.idTmdb == item.idTmdb }

                Box(modifier = Modifier.width(140.dp)) {
                    ItemMidiaCard(midia = midiaItem, onClick = { onTmdbItemClique(item, tipoReal) }, jaAdicionado = jaNaLista, onAdicionarRapido = { onAdicionarClique(item.idTmdb, item.titulo, tipoReal, StatusMidia.QUERO_ASSISTIR.valor, 0, item.sinopse, if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "", item.generoTexto, plataformaFinal) }, modoListaHorizontal = false, onAceitarPendente = {}, onRecusarPendente = {}, onProcessarRecusado = {})
                }
            }
        }
    }
}

fun compartilharCodigoSalaWhatsApp(context: Context, codigoSala: String, nomeSala: String, senhaSala: String) {
    val senhaTexto = if (senhaSala.isNotBlank()) " e senha: *$senhaSala*" else ""
    val mensagem = "🍿 Olá! Entra na minha sala compartilhada no *CineList* para vermos filmes juntos!\n\n📍 Sala: *$nomeSala*\n🔑 Código: `$codigoSala`$senhaTexto\n\nBaixe o app e insira o código para se conectar!"
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, mensagem); setPackage("com.whatsapp") }
    try { context.startActivity(intent) } catch (e: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(mensagem)}"))) }
}

@Composable
fun DialogoGerenciarSalasCompartilhadas(viewModel: MidiaViewModel, onDispensar: () -> Unit) {
    val contexto = LocalContext.current
    val gruposSalvos by viewModel.gruposSalvos.collectAsState(initial = emptyList())
    val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()
    val isAdministrador by viewModel.isAdministradorSala.collectAsState()

    var nomeGrupoInput by remember { mutableStateOf("") }
    var codigoGrupoInput by remember { mutableStateOf("") }
    var senhaGrupoInput by remember { mutableStateOf("") }
    var novaSenhaInput by remember { mutableStateOf("") }
    var tipoGrupoSelecionado by remember { mutableStateOf("Casal") }
    var abaModoCriarEntrar by remember { mutableIntStateOf(0) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }

    val membrosSala by viewModel.membrosGrupoAtivo.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text("Listas Compartilhadas 🍿", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TabRow(selectedTabIndex = abaModoCriarEntrar) {
                    Tab(selected = abaModoCriarEntrar == 0, onClick = { abaModoCriarEntrar = 0; mensagemErro = null }, text = { Text("Salas") })
                    Tab(selected = abaModoCriarEntrar == 1, onClick = { abaModoCriarEntrar = 1; mensagemErro = null }, text = { Text("Criar") })
                    Tab(selected = abaModoCriarEntrar == 2, onClick = { abaModoCriarEntrar = 2; mensagemErro = null }, text = { Text("Entrar") })
                }

                if (!mensagemErro.isNullOrBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) { Text(text = mensagemErro ?: "", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
                }

                if (abaModoCriarEntrar == 0) {
                    Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.selecionarGrupoAtivo(""); onDispensar() }, colors = CardDefaults.cardColors(containerColor = if (casalIdAtivo.isBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) { Column(modifier = Modifier.padding(12.dp)) { Text("Minha Lista Pessoal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text("Seus filmes e séries privados", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary) } }

                    if (casalIdAtivo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(onClick = { compartilharCodigoSalaWhatsApp(context = contexto, codigoSala = casalIdAtivo, nomeSala = "Sala Compartilhada", senhaSala = "") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)), shape = RoundedCornerShape(10.dp)) { Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Convidar via WhatsApp 💚", fontWeight = FontWeight.Bold) }
                        if (isAdministrador) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(10.dp)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("👑 Painel de Administrador", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                    OutlinedTextField(value = novaSenhaInput, onValueChange = { novaSenhaInput = it }, label = { Text("Nova Senha da Sala") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Button(onClick = { if (novaSenhaInput.isNotBlank()) { viewModel.atualizarSenhaDaSala(casalIdAtivo, novaSenhaInput) { sucesso -> if (sucesso) { Toast.makeText(contexto, "Senha atualizada com sucesso!", Toast.LENGTH_SHORT).show(); novaSenhaInput = "" } else { Toast.makeText(contexto, "Erro ao atualizar senha.", Toast.LENGTH_SHORT).show() } } } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Salvar Nova Senha", fontSize = 12.sp) }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Membros na Sala (${membrosSala.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    for (membro in membrosSala) {
                                        val ehMim = membro.uid == (FirebaseAuth.getInstance().currentUser?.uid ?: "")
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(membro.nome + if (ehMim) " (Você)" else "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            if (!ehMim) { TextButton(onClick = { viewModel.excluirMembroDaSala(casalIdAtivo, membro.uid) { sucesso -> if (sucesso) Toast.makeText(contexto, "${membro.nome} foi removido da sala.", Toast.LENGTH_SHORT).show() } }) { Text("Remover", color = Color(0xFFFF4C4C), fontSize = 11.sp) } }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (gruposSalvos.isEmpty()) {
                        Text("Nenhuma sala salva ainda. Toque em 'Criar' ou 'Entrar' acima!", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    } else {
                        for (grupo in gruposSalvos) {
                            val ehAtiva = casalIdAtivo == grupo.grupoId
                            Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.selecionarGrupoAtivo(grupo.grupoId); onDispensar() }, colors = CardDefaults.cardColors(containerColor = if (ehAtiva) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) { Text(grupo.nomeGrupo.ifBlank { grupo.grupoId }, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text("Código: ${grupo.grupoId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary) }
                                    IconButton(onClick = { viewModel.excluirGrupoSalvo(grupo) }) { Icon(Icons.Default.Delete, contentDescription = "Excluir Grupo", tint = Color(0xFFFF4C4C)) }
                                }
                            }
                        }
                    }
                } else if (abaModoCriarEntrar == 1) {
                    OutlinedTextField(value = nomeGrupoInput, onValueChange = { nomeGrupoInput = it }, label = { Text("Nome da Sala (ex: Casal ❤️)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = codigoGrupoInput, onValueChange = { digitos -> codigoGrupoInput = digitos.filter { it.isDigit() }.take(4) }, label = { Text("Código Numérico") }, prefix = { Text("CINE-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = senhaGrupoInput, onValueChange = { senhaGrupoInput = it }, label = { Text("Senha de Acesso (Opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { codigoGrupoInput = (1000..9999).random().toString() }, modifier = Modifier.weight(1f)) { Text("Gerar Código", fontSize = 12.sp) }
                        if (codigoGrupoInput.isNotBlank()) {
                            TextButton(onClick = { val codigoCompleto = "CINE-$codigoGrupoInput"; val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager; clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Código da Sala", codigoCompleto)); Toast.makeText(contexto, "Código $codigoCompleto copiado!", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) { Text("Copiar", fontSize = 12.sp) }
                        }
                    }
                    Button(onClick = { if (codigoGrupoInput.isNotBlank()) { carregando = true; viewModel.criarGrupoComSenha("CINE-$codigoGrupoInput", nomeGrupoInput.ifBlank { "Lista Compartilhada" }, tipoGrupoSelecionado, senhaGrupoInput) { sucesso -> carregando = false; if (sucesso) onDispensar() else mensagemErro = "Erro ao criar grupo na nuvem." } } else { mensagemErro = "Preencha os números do código." } }, modifier = Modifier.fillMaxWidth(), enabled = !carregando, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { if (carregando) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text("Criar e Entrar na Sala", fontWeight = FontWeight.Bold) }
                } else {
                    OutlinedTextField(value = nomeGrupoInput, onValueChange = { nomeGrupoInput = it }, label = { Text("Apelido Local da Lista") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = codigoGrupoInput, onValueChange = { digitos -> codigoGrupoInput = digitos.filter { it.isDigit() }.take(4) }, label = { Text("Código Numérico") }, prefix = { Text("CINE-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = senhaGrupoInput, onValueChange = { senhaGrupoInput = it }, label = { Text("Senha da Sala (se houver)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { if (codigoGrupoInput.isNotBlank()) { carregando = true; viewModel.entrarEmGrupoExistente("CINE-$codigoGrupoInput", nomeGrupoInput.ifBlank { "Lista Compartilhada" }, tipoGrupoSelecionado, senhaGrupoInput) { sucesso, erro -> carregando = false; if (sucesso) onDispensar() else mensagemErro = erro } } else { mensagemErro = "Digite os números do código." } }, modifier = Modifier.fillMaxWidth(), enabled = !carregando, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { if (carregando) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text("Verificar e Entrar na Sala", fontWeight = FontWeight.Bold) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDispensar) { Text("Fechar") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> FlowRowWithSpacing(items: List<T>, content: @Composable (T) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (item in items) {
            content(item)
        }
    }
}

@Composable
fun DialogoCriarEnquete(
    onDispensar: () -> Unit,
    onCriarEnquete: (pergunta: String, opcao1: String, opcao2: String, opcao3: String) -> Unit
) {
    var pergunta by remember { mutableStateOf("") }
    var op1 by remember { mutableStateOf("") }
    var op2 by remember { mutableStateOf("") }
    var op3 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text("Criar Enquete Rápida 📊", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pergunta,
                    onValueChange = { pergunta = it },
                    label = { Text("Pergunta (ex: O que assistir hoje?)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = op1,
                    onValueChange = { op1 = it },
                    label = { Text("Opção 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = op2,
                    onValueChange = { op2 = it },
                    label = { Text("Opção 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = op3,
                    onValueChange = { op3 = it },
                    label = { Text("Opção 3 (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pergunta.isNotBlank() && op1.isNotBlank() && op2.isNotBlank()) {
                        onCriarEnquete(pergunta.trim(), op1.trim(), op2.trim(), op3.trim())
                    }
                },
                enabled = pergunta.isNotBlank() && op1.isNotBlank() && op2.isNotBlank()
            ) {
                Text("Enviar Enquete 📊", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDispensar) { Text("Cancelar") }
        }
    )
}

@Composable
fun ConteudoCardEnquete(
    msg: MensagemGrupo,
    souEu: Boolean
) {
    val opcoes = remember(msg.mediaUrl) {
        msg.mediaUrl.split("|||").map { it.trim() }.filter { it.isNotBlank() }
    }
    var opcaoSelecionada by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📊 Enquete da Sala", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Text(msg.texto, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (souEu) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)

        opcoes.forEachIndexed { index, op ->
            val selecionado = opcaoSelecionada == index
            Surface(
                onClick = { opcaoSelecionada = index },
                shape = RoundedCornerShape(8.dp),
                color = if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = op,
                        fontSize = 13.sp,
                        fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                        color = if (selecionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selecionado) {
                        Text("Votado ✅", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ConteudoCardIndicacao(
    msg: MensagemGrupo,
    souEu: Boolean,
    onTmdbItemClique: (TmdbFilme, String) -> Unit
) {
    val partes = remember(msg.mediaUrl) { msg.mediaUrl.split("|||") }
    val idTmdb = partes.getOrNull(0)?.toIntOrNull() ?: 0
    val titulo = partes.getOrNull(1) ?: msg.texto
    val capa = partes.getOrNull(2) ?: ""
    val tipo = partes.getOrNull(3) ?: "Filme"

    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🍿 Indicação Recomendada", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Text(msg.texto, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (souEu) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (capa.isNotBlank()) {
                AsyncImage(model = capa, contentDescription = titulo, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp, 60.dp).clip(RoundedCornerShape(6.dp)))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tipo, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
            }
            Button(
                onClick = {
                    val itemTmdb = TmdbFilme(idTmdb = idTmdb, titulo = titulo, caminhoPoster = capa.removePrefix("https://image.tmdb.org/t/p/w500"), mediaType = if (tipo.equals("Série", ignoreCase = true)) "tv" else "movie")
                    onTmdbItemClique(itemTmdb, tipo)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Ver 🎬", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ConteudoCardDebate(
    msg: MensagemGrupo,
    souEu: Boolean
) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("💬 Debate Pós-Filme", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Text(msg.texto, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (souEu) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Perguntas Quebra-Gelo:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("• Qual foi a melhor cena?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• O final fez jus à história?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• Que nota você daria agora?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun ConteudoCardCinema(
    msg: MensagemGrupo,
    souEu: Boolean
) {
    Surface(
        color = Color(0xFFE50914).copy(alpha = 0.25f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🍿", fontSize = 22.sp)
            Text(
                text = msg.texto,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFFFF5252)
            )
        }
    }
}

private val palavrasIgnoradasBusca = setOf(
    "de", "da", "do", "das", "dos", "um", "uma", "uns", "umas", "os", "as", "em", "no", "na", "nos", "nas",
    "com", "por", "para", "pra", "que", "ao", "aos", "sua", "seu", "num", "numa", "sobre", "filme", "serie",
    "onde", "quando", "ele", "ela", "eles", "elas", "mas", "ou", "como", "qual", "tem", "foi", "sao", "ser",
    "quer", "esta", "estao", "isso", "essa", "esse", "muito", "mais", "tudo", "fica", "ficou"
)

private fun normalizarTextoBusca(s: String): String =
    java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

suspend fun buscarPorEnredo(consulta: String): List<TmdbFilme> = coroutineScope {
    val api = RetrofitClient.apiService
    val textoOriginal = consulta.trim()

    val palavras = normalizarTextoBusca(textoOriginal)
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length > 2 && it !in palavrasIgnoradasBusca }
        .distinct()

    // Busca direta por título (caso a pessoa escreva o nome do filme)
    val diretoDeferred =
        async { runCatching { api.buscarMulti(textoOriginal).resultados }.getOrNull().orEmpty() }

    // Busca por cada palavra forte da frase
    val porPalavraDeferred = palavras.sortedByDescending { it.length }.take(4).map { palavra ->
        async { runCatching { api.buscarMulti(palavra).resultados }.getOrNull().orEmpty() }
    }

    // Filmes e séries populares (páginas 1 a 5), para casar com a sinopse
    val populares = (1..5).flatMap { pagina ->
        listOf(
            async {
                runCatching { api.descobrirFilmes(pagina = pagina).resultados }.getOrNull()
                    .orEmpty()
            },
            async {
                runCatching { api.descobrirSeries(pagina = pagina).resultados }.getOrNull()
                    .orEmpty()
            }
        )
    }

    val direto = diretoDeferred.await().filter { !it.caminhoPoster.isNullOrBlank() }
    val candidatos = (porPalavraDeferred + populares).awaitAll().flatten()
        .filter { !it.caminhoPoster.isNullOrBlank() }
        .distinctBy { "${it.ehSerie}_${it.idTmdb}" }

    fun pontuar(item: TmdbFilme): Int {
        val texto = normalizarTextoBusca(item.titulo + " " + item.sinopse)
        return palavras.count { palavra ->
            val raiz = if (palavra.length >= 5) palavra.take(5) else palavra
            texto.contains(raiz)
        }
    }

    val minimo = if (palavras.size <= 2) 1 else 2
    val porSinopse = candidatos
        .map { it to pontuar(it) }
        .filter { it.second >= minimo }
        .sortedByDescending { it.second }
        .map { it.first }

    (direto + porSinopse)
        .distinctBy { "${it.ehSerie}_${it.idTmdb}" }
        .take(15)
}

@Composable
fun DialogoCineBot(
    onDispensar: () -> Unit,
    onAdicionarClique: (Int, String, String, String, Int, String, String, String, String) -> Unit
) {
    var pedidoText by remember { mutableStateOf("") }
    var sugestoesBot by remember { mutableStateOf<List<TmdbFilme>>(emptyList()) }
    var buscando by remember { mutableStateOf(false) }
    var buscouUmaVez by remember { mutableStateOf(false) }
    var todosResultados by remember { mutableStateOf<List<TmdbFilme>>(emptyList()) }
    var ultimaConsulta by remember { mutableStateOf("") }
    var indiceAtual by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDispensar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("CineBot IA Assistente 🤖", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Descreva o que quer ver ou o que acontece no filme/série (busca por título, gênero ou sinopse):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                OutlinedTextField(
                    value = pedidoText,
                    onValueChange = { pedidoText = it },
                    placeholder = { Text("Ex: Homem preso num barco com tigre...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (pedidoText.isNotBlank()) {
                            val consulta = pedidoText.trim()
                            val ehMesmaBusca = consulta.equals(ultimaConsulta, ignoreCase = true) && todosResultados.isNotEmpty()

                            if (ehMesmaBusca) {
                                // Mesma busca: mostra os próximos 3 resultados
                                val proximo = indiceAtual + 3
                                if (proximo >= todosResultados.size) {
                                    todosResultados = todosResultados.shuffled()
                                    indiceAtual = 0
                                } else {
                                    indiceAtual = proximo
                                }
                                sugestoesBot = todosResultados.drop(indiceAtual).take(3)
                            } else {
                                // Busca nova
                                buscando = true
                                coroutineScope.launch {
                                    try {
                                        val resultado = buscarPorEnredo(consulta)
                                        todosResultados = resultado
                                        ultimaConsulta = consulta
                                        indiceAtual = 0
                                        sugestoesBot = resultado.take(3)
                                        buscouUmaVez = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        todosResultados = emptyList()
                                        sugestoesBot = emptyList()
                                        buscouUmaVez = true
                                    } finally {
                                        buscando = false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !buscando && pedidoText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (buscando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text(
                            if (sugestoesBot.isNotEmpty() && pedidoText.trim().equals(ultimaConsulta, ignoreCase = true)) "Ver outras sugestões 🔄" else "Buscar Sugestão IA ✨",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (buscouUmaVez && !buscando && sugestoesBot.isEmpty()) {
                    Text(
                        "Nada encontrado. Tente palavras mais específicas (ex: tigre, naufrágio, robô) ou o título.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (sugestoesBot.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sugestoesBot.forEach { item ->
                            val tipoReal = if (item.ehSerie || item.mediaType.equals("tv", ignoreCase = true)) "Série" else "Filme"
                            val posterUrl = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else ""

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (posterUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = posterUrl,
                                            contentDescription = item.titulo,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(40.dp, 60.dp).clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.sinopse.ifBlank { "Sem sinopse disponível." }, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Button(
                                        onClick = {
                                            onAdicionarClique(
                                                item.idTmdb,
                                                item.titulo,
                                                tipoReal,
                                                StatusMidia.QUERO_ASSISTIR.valor,
                                                0,
                                                item.sinopse,
                                                posterUrl,
                                                item.generoTexto,
                                                if (tipoReal == "Série") "TV / Original" else "Cinema"
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("+ Lista", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDispensar) { Text("Fechar") }
        }
    )
}