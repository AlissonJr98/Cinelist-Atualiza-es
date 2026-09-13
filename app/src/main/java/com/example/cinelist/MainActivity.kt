@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.cinelist

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cinelist.ui.theme.CineListTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

fun agendarChecagemAtualizacaoSegundoPlano(context: Context) {
    val restricoes = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val requisicao = PeriodicWorkRequestBuilder<AtualizacaoWorker>(6, TimeUnit.HOURS)
        .setConstraints(restricoes)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "CineListVerificacaoOta",
        ExistingPeriodicWorkPolicy.KEEP,
        requisicao
    )
}

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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        agendarChecagemAtualizacaoSegundoPlano(this)

        setContent {
            val contexto = LocalContext.current
            val view = LocalView.current

            val sharedPreferences = remember {
                contexto.getSharedPreferences(
                    "ConfiguracoesPerfil",
                    android.content.Context.MODE_PRIVATE
                )
            }

            var modoEscuroAtivo by remember {
                mutableStateOf(sharedPreferences.getBoolean("modo_escuro", true))
            }

            DisposableEffect(sharedPreferences) {
                val listener =
                    android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, chave ->
                        if (chave == "modo_escuro") {
                            modoEscuroAtivo = sharedPreferences.getBoolean("modo_escuro", true)
                        }
                    }
                sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                onDispose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            if (!view.isInEditMode) {
                SideEffect {
                    val window = (contexto as Activity).window
                    val insetsController = WindowCompat.getInsetsController(window, view)

                    insetsController.show(WindowInsetsCompat.Type.statusBars())
                    insetsController.isAppearanceLightStatusBars = !modoEscuroAtivo
                }
            }

            val launcherPermissaoNotificacao = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcherPermissaoNotificacao.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            CineListTheme(darkTheme = modoEscuroAtivo) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConfiguracaoNavegacao()
                }
            }
        }
    }
}

@Composable
fun DialogoNovidadesAtualizacao(
    info: InfoAtualizacao,
    onDispensar: () -> Unit,
    onConfirmarAtualizacao: () -> Unit
) {
    val linhasNovidades = remember(info.notasDaVersao) {
        info.notasDaVersao
            .split("\n", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    Dialog(onDismissRequest = onDispensar) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Novidades",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "O que há de novo?",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Versão ${info.versaoNome} (Build ${info.versaoCode})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Alterações implementadas:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (linhasNovidades.isNotEmpty()) {
                        linhasNovidades.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp)
                                )
                                Text(
                                    text = item.removePrefix("-").trim(),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = info.notasDaVersao.ifBlank { "Melhorias gerais de estabilidade e novas otimizações no sistema." },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDispensar,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Mais Tarde", color = MaterialTheme.colorScheme.secondary)
                    }

                    Button(
                        onClick = onConfirmarAtualizacao,
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Atualizar Agora",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
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

@Composable
fun ConfiguracaoNavegacao() {
    val navController = rememberNavController()
    val usuarioLogado = remember { FirebaseAuth.getInstance().currentUser != null }
    val rotaInicial = if (usuarioLogado) "home" else "login"

    val viewModel: MidiaViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = rotaInicial) {
        composable("login") {
            TelaLogin(
                onLoginSucesso = {
                    viewModel.iniciarSincronizacaoSilenciosaNuvem()
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                },
                onNavegarParaCadastro = { navController.navigate("cadastro") }
            )
        }

        composable("cadastro") {
            TelaCadastro(
                onCadastroSucesso = {
                    viewModel.iniciarSincronizacaoSilenciosaNuvem()
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                },
                onVoltarParaLogin = { navController.popBackStack() }
            )
        }

        composable("home") {
            LaunchedEffect(Unit) {
                viewModel.iniciarSincronizacaoSilenciosaNuvem()
            }

            val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()
            val midiasGrupoAtivo by viewModel.midiasGrupoAtivo.collectAsState(initial = emptyList())
            val midiasPessoais by viewModel.midiasPessoais.collectAsState(initial = emptyList())

            val listaDeMidiasReal = if (casalIdAtivo.isNotBlank()) midiasGrupoAtivo else midiasPessoais

            TelaPrincipal(
                listaDeMidias = listaDeMidiasReal,
                viewModel = viewModel,
                isModoCompartilhado = casalIdAtivo.isNotBlank(),
                onItemClique = { midiaClicada ->
                    val idNavegacao = if (midiaClicada.idTmdb != 0) midiaClicada.idTmdb else midiaClicada.id
                    navController.navigate("detalhes/$idNavegacao/${midiaClicada.tipo}")
                },
                onTmdbItemClique = { itemTmdb, tipo ->
                    navController.navigate("detalhes/${itemTmdb.idTmdb}/$tipo")
                },
                onAdicionarClique = { idTmdb, titulo, tipo, status, nota, sinopse, capa, genero, plataforma ->
                    val novaMidia = Midia(
                        idTmdb = idTmdb,
                        titulo = titulo,
                        tipo = tipo,
                        status = status,
                        nota = nota,
                        temporadaAtual = 1,
                        episodioAtual = 1,
                        minutoParado = 0,
                        jaEncerrou = false,
                        sinopse = sinopse,
                        imagemCapa = capa,
                        genero = genero,
                        plataforma = plataforma,
                        favorito = false,
                        listaCustomizada = "Geral",
                        isCasal = casalIdAtivo.isNotBlank(),
                        casalId = casalIdAtivo
                    )
                    viewModel.inserir(novaMidia)
                },
                onPerfilClique = { navController.navigate("perfil") }
            )
        }

        composable("detalhes/{id}/{tipo}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: 0
            val tipo = backStackEntry.arguments?.getString("tipo") ?: "Filme"

            TelaDetalhes(
                id = id,
                tipoInicial = tipo,
                viewModel = viewModel,
                onVoltar = { navController.popBackStack() },
                onRecomendacaoClique = { novoId, novoTipo ->
                    navController.navigate("detalhes/$novoId/$novoTipo")
                }
            )
        }

        composable("perfil") {
            val listaDeMidiasReal by viewModel.todasAsMidias.collectAsState(initial = emptyList())
            TelaPerfil(
                listaDeMidias = listaDeMidiasReal,
                viewModel = viewModel,
                onVoltar = { navController.popBackStack() },
                onLogout = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                onMidiaClique = { midia ->
                    val idNavegacao = if (midia.idTmdb != 0) midia.idTmdb else midia.id
                    navController.navigate("detalhes/$idNavegacao/${midia.tipo}")
                },
                onCalendarioClique = {
                    navController.navigate("calendario")
                }
            )
        }

        composable("calendario") {
            val listaDeMidiasReal by viewModel.todasAsMidias.collectAsState(initial = emptyList())
            TelaCalendario(
                listaDeMidias = listaDeMidiasReal,
                onVoltar = { navController.popBackStack() },
                onMidiaClique = { idTmdb, tipo ->
                    navController.navigate("detalhes/$idTmdb/$tipo")
                }
            )
        }

        composable("notificacoes") {
            TelaNotificacoes(
                viewModel = viewModel,
                onVoltar = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun TelaPrincipal(
    listaDeMidias: List<Midia>,
    viewModel: MidiaViewModel,
    isModoCompartilhado: Boolean,
    onAdicionarClique: (idTmdb: Int, titulo: String, tipo: String, status: String, nota: Int, sinopse: String, capa: String, genero: String, plataforma: String) -> Unit,
    onItemClique: (Midia) -> Unit,
    onTmdbItemClique: (TmdbFilme, String) -> Unit,
    onPerfilClique: () -> Unit
) {
    val contextoLocal = LocalContext.current
    val historicoManager = remember { HistoricoBuscaManager(contextoLocal) }
    var historicoBuscas by remember { mutableStateOf(historicoManager.obterHistorico()) }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    var textoPesquisa by rememberSaveable { mutableStateOf("") }
    var midiaParaExcluir by remember { mutableStateOf<Midia?>(null) }
    var colecaoParaExcluir by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoGerenciarSalas by remember { mutableStateOf(false) }

    var modoListaMinhaLista by rememberSaveable { mutableStateOf(false) }
    var modoListaDescobrir by rememberSaveable { mutableStateOf(false) }
    var mostrarDialogoSorteio by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        textoPesquisa = ""
        viewModel.limparBuscaApi()
    }

    if (mostrarDialogoGerenciarSalas) {
        DialogoGerenciarSalasCompartilhadas(
            viewModel = viewModel,
            onDispensar = { mostrarDialogoGerenciarSalas = false }
        )
    }

    if (mostrarDialogoSorteio) {
        DialogoSorteio(
            listaDeMidias = listaDeMidias,
            onDispensar = { mostrarDialogoSorteio = false },
            onSelecionarMidia = { midiaSorteada ->
                mostrarDialogoSorteio = false
                onItemClique(midiaSorteada)
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
                        midiaParaExcluir?.let { viewModel.deletar(it) }
                        midiaParaExcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { midiaParaExcluir = null }) {
                    Text("Cancelar")
                }
            }
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
                        listaDeMidias.filter { it.listaCustomizada.equals(nomeColecao, ignoreCase = true) }.forEach { m ->
                            viewModel.atualizar(m.copy(listaCustomizada = "Geral"))
                        }
                        colecaoParaExcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { colecaoParaExcluir = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val streamingsFiltro = remember(listaDeMidias) {
        listOf("Todas") + listaDeMidias.map { it.plataforma }.filter { it.isNotBlank() && it != "Não Informado" }.distinct().sorted()
    }

    val colecoesDisponiveis = remember(listaDeMidias) {
        listOf("Geral") + listaDeMidias.map { it.listaCustomizada }.filter { it.isNotBlank() && it != "Geral" }.distinct().sorted()
    }

    var categoriaSelecionada by rememberSaveable { mutableStateOf("Todos") }
    var colecaoSelecionada by rememberSaveable { mutableStateOf("Geral") }
    var filtroPlataforma by rememberSaveable { mutableStateOf("Todas") }
    var filtroStatusMinhaLista by rememberSaveable { mutableStateOf("Ativos") }
    var ordenacaoMinhaLista by rememberSaveable { mutableStateOf("Padrão (Assistindo primeiro)") }

    var mostrarDialogo by remember { mutableStateOf(false) }
    var mostrarBottomSheetFiltrosDescobrir by remember { mutableStateOf(false) }
    var mostrarBottomSheetFiltrosMinhaLista by remember { mutableStateOf(false) }
    var mostrarDialogoGerenciarColecoes by remember { mutableStateOf(false) }

    var novoTitulo by remember { mutableStateOf("") }
    var novoTipo by remember { mutableStateOf("Filme") }
    var novoStatus by remember { mutableStateOf("Quero Assistir") }
    var novaNota by remember { mutableIntStateOf(0) }
    var sinopseSelecionada by remember { mutableStateOf("Nenhuma sinopse adicionada ainda.") }
    var capaSelecionada by remember { mutableStateOf("") }
    var generoSelecionado by remember { mutableStateOf("Geral") }
    var idTmdbSelecionado by remember { mutableIntStateOf(0) }

    val provedorSelecionadoId by viewModel.provedorSelecionadoId.collectAsState()
    val generoSelecionadoId by viewModel.generoSelecionadoId.collectAsState()
    val ordenacaoSelecionada by viewModel.ordenacaoSelecionada.collectAsState()
    val tipoPaginado by viewModel.tipoPaginado.collectAsState()
    val provedoresStreamingApi by viewModel.provedoresStreaming.collectAsState()
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Selecione uma coleção para excluir:", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    val colecoesCustom = colecoesDisponiveis.filter { it != "Geral" }
                    if (colecoesCustom.isEmpty()) {
                        Text("Nenhuma coleção customizada criada ainda.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        colecoesCustom.forEach { col ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(col, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                IconButton(
                                    onClick = { colecaoParaExcluir = col },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir Coleção", tint = Color(0xFFFF5252))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoGerenciarColecoes = false }) {
                    Text("Fechar", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (mostrarBottomSheetFiltrosMinhaLista) {
        ModalBottomSheet(
            onDismissRequest = { mostrarBottomSheetFiltrosMinhaLista = false },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtros da Minha Lista",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (quantidadeFiltrosAtivosMinhaLista > 0) {
                        TextButton(
                            onClick = {
                                categoriaSelecionada = "Todos"
                                colecaoSelecionada = "Geral"
                                filtroPlataforma = "Todas"
                                filtroStatusMinhaLista = "Ativos"
                                ordenacaoMinhaLista = "Padrão (Assistindo primeiro)"
                            }
                        ) {
                            Text("Redefinir", color = Color(0xFFFF5252))
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Coleção Temática:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { mostrarDialogoGerenciarColecoes = true }) {
                            Text("Gerenciar / Excluir", fontSize = 11.sp, color = Color(0xFFFF5252))
                        }
                    }
                    FlowRowWithSpacing(items = colecoesDisponiveis) { col ->
                        FilterChip(
                            selected = (colecaoSelecionada == col),
                            onClick = { colecaoSelecionada = col },
                            label = { Text(col, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Categoria:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    FlowRowWithSpacing(items = categoriasMinhaLista) { cat ->
                        FilterChip(
                            selected = (categoriaSelecionada == cat),
                            onClick = { categoriaSelecionada = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Exibir Status:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    FlowRowWithSpacing(items = listOf("Ativos", "Favoritos", "Quero Assistir", "Assistindo", "Concluído", "Todos")) { s ->
                        FilterChip(
                            selected = (filtroStatusMinhaLista == s),
                            onClick = { filtroStatusMinhaLista = s },
                            label = { Text(s, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ordenar por:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    FlowRowWithSpacing(items = opcoesOrdenacaoMinhaLista) { opt ->
                        FilterChip(
                            selected = (ordenacaoMinhaLista == opt),
                            onClick = { ordenacaoMinhaLista = opt },
                            label = { Text(opt, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                if (streamingsFiltro.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Plataforma:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        FlowRowWithSpacing(items = streamingsFiltro) { st ->
                            FilterChip(
                                selected = (filtroPlataforma == st),
                                onClick = { filtroPlataforma = st },
                                label = { Text(st, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { mostrarBottomSheetFiltrosMinhaLista = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Aplicar Filtros", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (mostrarBottomSheetFiltrosDescobrir) {
        ModalBottomSheet(
            onDismissRequest = { mostrarBottomSheetFiltrosDescobrir = false },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtros de Descoberta",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (quantidadeFiltrosAtivosDescobrir > 0) {
                        TextButton(
                            onClick = {
                                viewModel.selecionarTipo("Todos")
                                viewModel.selecionarProvedorStreaming(null)
                                viewModel.selecionarGenero(null)
                                viewModel.selecionarOrdenacao("popularity.desc")
                            }
                        ) {
                            Text("Redefinir Tudo", color = Color(0xFFFF5252))
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tipo de Conteúdo:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    FlowRowWithSpacing(items = tiposDisponiveis) { tipo ->
                        FilterChip(
                            selected = (tipoPaginado == tipo),
                            onClick = { viewModel.selecionarTipo(tipo) },
                            label = { Text(tipo, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ordenar por:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    FlowRowWithSpacing(items = opcoesOrdenacaoDescobrir.map { it.first }) { rotulo ->
                        val chaveSort = opcoesOrdenacaoDescobrir.first { it.first == rotulo }.second
                        FilterChip(
                            selected = (ordenacaoSelecionada == chaveSort),
                            onClick = { viewModel.selecionarOrdenacao(chaveSort) },
                            label = { Text(rotulo, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Plataformas de Streaming:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    FlowRowWithSpacing(items = provedoresDisponiveis) { (nome, id) ->
                        FilterChip(
                            selected = (provedorSelecionadoId == id),
                            onClick = { viewModel.selecionarProvedorStreaming(id) },
                            label = { Text(nome, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                val listaGenerosExibir = if (tipoPaginado == "Série" || tipoPaginado == "Anime") generosSeries else generosFilmes
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Gêneros:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    FlowRowWithSpacing(items = listaGenerosExibir) { (nome, id) ->
                        FilterChip(
                            selected = (generoSelecionadoId == id),
                            onClick = { viewModel.selecionarGenero(id) },
                            label = { Text(nome, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { mostrarBottomSheetFiltrosDescobrir = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Aplicar Filtros", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogo = false
                viewModel.limparBuscaApi()
            },
            title = {
                Text(
                    "Adicionar Nova Mídia",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = novoTitulo,
                        onValueChange = {
                            novoTitulo = it
                            viewModel.atualizarQueryEFiltrarPaginado(it, novoTipo)
                        },
                        label = { Text("Título da Mídia") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (resultadosPaginadosApi.itemCount > 0) {
                        Text(
                            "Sugestões da Internet:",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp))
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(resultadosPaginadosApi.itemCount) { index ->
                                val item = resultadosPaginadosApi[index]
                                item?.let {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                novoTitulo = it.titulo
                                                idTmdbSelecionado = it.idTmdb
                                                generoSelecionado = it.generoTexto
                                                sinopseSelecionada = it.sinopse.ifBlank { "Nenhuma sinopse disponível." }
                                                capaSelecionada = if (!it.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${it.caminhoPoster}" else ""
                                                viewModel.buscarOndeAssistir(it.idTmdb, novoTipo)
                                            }
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = it.titulo,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text("Tipo:", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Filme", "Série", "Anime", "Novela", "Dorama").forEach { t ->
                            FilterChip(
                                selected = (novoTipo == t),
                                onClick = {
                                    novoTipo = t
                                    viewModel.atualizarQueryEFiltrarPaginado(novoTitulo, t)
                                },
                                label = { Text(t, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Text("Status:", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Quero Assistir", "Assistindo", "Concluído").forEach { s ->
                            FilterChip(
                                selected = (novoStatus == s),
                                onClick = { novoStatus = s },
                                label = { Text(text = s, fontSize = 11.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Text("Sua Avaliação (Estrelas):", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { estrela ->
                            val ativa = estrela <= novaNota
                            IconButton(onClick = { novaNota = estrela }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Nota $estrela",
                                    tint = if (ativa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (novoTitulo.isNotBlank()) {
                            val ehSerie = novoTipo.equals("Série", ignoreCase = true) || novoTipo.equals("Anime", ignoreCase = true) || novoTipo.equals("Novela", ignoreCase = true) || novoTipo.equals("Dorama", ignoreCase = true)
                            val streamingPrincipal = provedoresStreamingApi.firstOrNull()?.nomeProvedor ?: (if (ehSerie) "TV / Original" else "Cinema")
                            onAdicionarClique(
                                idTmdbSelecionado,
                                novoTitulo.trim(),
                                novoTipo,
                                novoStatus,
                                novaNota,
                                sinopseSelecionada,
                                capaSelecionada,
                                generoSelecionado,
                                streamingPrincipal
                            )
                            novoTitulo = ""
                            novoStatus = "Quero Assistir"
                            novaNota = 0
                            idTmdbSelecionado = 0
                            sinopseSelecionada = "Nenhuma sinopse adicionada ainda."
                            capaSelecionada = ""
                            generoSelecionado = "Geral"
                            mostrarDialogo = false
                            viewModel.limparBuscaApi()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Adicionar", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogo = false
                    viewModel.limparBuscaApi()
                }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (isModoCompartilhado) "CineList (Grupo ❤️)" else "CineList", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoGerenciarSalas = true }) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Gerenciar Salas Compartilhadas",
                            tint = if (isModoCompartilhado) Color(0xFFFF4C4C) else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { mostrarDialogoSorteio = true }) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = "O Que Assistir Hoje", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = onPerfilClique) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogo = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = { Text("Minha Lista", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = { Text("Descobrir", fontWeight = FontWeight.Bold) }
                )
            }

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = textoPesquisa,
                        onValueChange = { textoPesquisa = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                if (pagerState.currentPage == 0) "Buscar por título, gênero ou streaming..." else "Buscar online no TMDB...",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp
                            )
                        },
                        trailingIcon = {
                            if (textoPesquisa.isNotBlank()) {
                                IconButton(onClick = { textoPesquisa = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpar busca", tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(if (textoPesquisa.isBlank() && historicoBuscas.isNotEmpty()) 12.dp else 28.dp)
                    )

                    if (textoPesquisa.isBlank() && historicoBuscas.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pesquisas Recentes",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = {
                                            historicoManager.limparHistorico()
                                            historicoBuscas = emptyList()
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Limpar histórico", fontSize = 10.sp, color = Color(0xFFFF5252))
                                    }
                                }

                                historicoBuscas.forEach { termo ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { textoPesquisa = termo }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = termo,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                        val termoBusca = textoPesquisa.trim()
                        val bateTexto = termoBusca.isBlank() ||
                                midia.titulo.contains(termoBusca, ignoreCase = true) ||
                                midia.genero.contains(termoBusca, ignoreCase = true) ||
                                midia.plataforma.contains(termoBusca, ignoreCase = true)

                        val bateCategoria = if (categoriaSelecionada == "Todos") true else {
                            val tipoMapeado = when (categoriaSelecionada) {
                                "Filmes" -> "Filme"
                                "Séries" -> "Série"
                                "Animes" -> "Anime"
                                "Novelas" -> "Novela"
                                "Doramas" -> "Dorama"
                                else -> ""
                            }
                            midia.tipo.equals(tipoMapeado, ignoreCase = true)
                        }

                        val bateColecao = if (colecaoSelecionada == "Geral") true else {
                            midia.listaCustomizada.equals(colecaoSelecionada, ignoreCase = true)
                        }

                        val batePlataforma = if (filtroPlataforma == "Todas") true else {
                            midia.plataforma.contains(filtroPlataforma, ignoreCase = true)
                        }

                        val bateStatus = when (filtroStatusMinhaLista) {
                            "Ativos" -> midia.status != "Concluído"
                            "Favoritos" -> midia.favorito
                            "Quero Assistir" -> midia.status == "Quero Assistir"
                            "Assistindo" -> midia.status == "Assistindo"
                            "Concluído" -> midia.status == "Concluído"
                            else -> true
                        }

                        bateTexto && bateCategoria && bateColecao && batePlataforma && bateStatus
                    }.let { lista ->
                        when (ordenacaoMinhaLista) {
                            "Favoritos Primeiro" -> lista.sortedWith(
                                compareByDescending<Midia> { it.favorito }
                                    .thenByDescending { it.status == "Assistindo" }
                            )
                            "Melhor Avaliados" -> lista.sortedByDescending { it.nota }
                            "Ordem Alfabética (A-Z)" -> lista.sortedBy { it.titulo.lowercase() }
                            "Adicionados Recentemente" -> lista.sortedByDescending { it.id }
                            else -> lista.sortedByDescending { it.status == "Assistindo" }
                        }
                    }

                    var isRefreshing by remember { mutableStateOf(false) }

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            coroutineScope.launch {
                                isRefreshing = true
                                viewModel.iniciarSincronizacaoSilenciosaNuvem()
                                delay(800)
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${listaFiltrada.size} título(s) exibido(s)",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { modoListaMinhaLista = !modoListaMinhaLista },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (modoListaMinhaLista) Icons.Default.GridView else Icons.Default.ViewList,
                                            contentDescription = "Alternar Visualização",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (quantidadeFiltrosAtivosMinhaLista > 0 || textoPesquisa.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                textoPesquisa = ""
                                                categoriaSelecionada = "Todos"
                                                colecaoSelecionada = "Geral"
                                                filtroPlataforma = "Todas"
                                                filtroStatusMinhaLista = "Ativos"
                                                ordenacaoMinhaLista = "Padrão (Assistindo primeiro)"
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Limpar Filtros",
                                                tint = Color(0xFFFF5252),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    AssistChip(
                                        onClick = { mostrarBottomSheetFiltrosMinhaLista = true },
                                        label = {
                                            Text(
                                                text = if (quantidadeFiltrosAtivosMinhaLista > 0) "Filtros ($quantidadeFiltrosAtivosMinhaLista)" else "Filtros",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.FilterList,
                                                contentDescription = "Filtros",
                                                tint = if (quantidadeFiltrosAtivosMinhaLista > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (quantidadeFiltrosAtivosMinhaLista > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            labelColor = if (quantidadeFiltrosAtivosMinhaLista > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (listaFiltrada.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = "Nenhum item encontrado com esses filtros.", color = MaterialTheme.colorScheme.secondary)
                                }
                            } else {
                                if (modoListaMinhaLista) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(listaFiltrada, key = { it.id }) { mi ->
                                            ItemMidiaCard(
                                                midia = mi,
                                                onClick = { onItemClique(mi) },
                                                onIncrementarEpisodio = { viewModel.incrementarEpisodioRapido(mi) },
                                                onDeletar = {
                                                    midiaParaExcluir = mi
                                                },
                                                onAlternarStatusConcluido = {
                                                    val novoStatus = if (mi.status == "Concluído") "Assistindo" else "Concluído"
                                                    viewModel.atualizar(mi.copy(status = novoStatus))
                                                },
                                                onAlternarFavorito = {
                                                    viewModel.alternarFavorito(mi)
                                                },
                                                modoListaHorizontal = true
                                            )
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp)
                                    ) {
                                        items(listaFiltrada, key = { it.id }) { mi ->
                                            ItemMidiaCard(
                                                midia = mi,
                                                onClick = { onItemClique(mi) },
                                                onIncrementarEpisodio = { viewModel.incrementarEpisodioRapido(mi) },
                                                onDeletar = {
                                                    midiaParaExcluir = mi
                                                },
                                                onAlternarStatusConcluido = {
                                                    val novoStatus = if (mi.status == "Concluído") "Assistindo" else "Concluído"
                                                    viewModel.atualizar(mi.copy(status = novoStatus))
                                                },
                                                onAlternarFavorito = {
                                                    viewModel.alternarFavorito(mi)
                                                },
                                                modoListaHorizontal = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Exibindo: $tipoPaginado",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { modoListaDescobrir = !modoListaDescobrir },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (modoListaDescobrir) Icons.Default.GridView else Icons.Default.ViewList,
                                        contentDescription = "Alternar Visualização",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
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
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpar Filtros",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                AssistChip(
                                    onClick = { mostrarBottomSheetFiltrosDescobrir = true },
                                    label = {
                                        Text(
                                            text = if (quantidadeFiltrosAtivosDescobrir > 0) "Filtros ($quantidadeFiltrosAtivosDescobrir)" else "Filtros",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = "Filtros",
                                            tint = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        labelColor = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        if (modoListaDescobrir) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    count = resultadosPaginadosApi.itemCount,
                                    key = { index ->
                                        val item = resultadosPaginadosApi.peek(index)
                                        if (item != null) {
                                            "${item.mediaType ?: "midia"}_${item.idTmdb}_$index"
                                        } else {
                                            index
                                        }
                                    },
                                    contentType = resultadosPaginadosApi.itemContentType { "tmdb_media" }
                                ) { index ->
                                    val item = resultadosPaginadosApi[index]
                                    if (item != null) {
                                        val tipoReal = when {
                                            tipoPaginado != "Todos" -> tipoPaginado
                                            item.mediaType.equals("tv", ignoreCase = true) -> "Série"
                                            item.mediaType.equals("movie", ignoreCase = true) -> "Filme"
                                            item.ehSerie -> "Série"
                                            else -> "Filme"
                                        }

                                        val plataformaFinal = if (item.plataformaDetectada.isNotBlank()) {
                                            item.plataformaDetectada
                                        } else {
                                            if (tipoReal.equals("Filme", ignoreCase = true)) "Cinema" else "TV / Original"
                                        }

                                        val midiaItem = Midia(
                                            idTmdb = item.idTmdb,
                                            titulo = item.titulo,
                                            tipo = tipoReal,
                                            status = "Descobrir",
                                            nota = 0,
                                            sinopse = item.sinopse,
                                            imagemCapa = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "",
                                            genero = item.generoTexto,
                                            plataforma = plataformaFinal,
                                            favorito = false,
                                            listaCustomizada = "Geral"
                                        )

                                        val jaNaLista = listaDeMidias.any { it.idTmdb != 0 && it.idTmdb == item.idTmdb }

                                        ItemMidiaCard(
                                            midia = midiaItem,
                                            onClick = { onTmdbItemClique(item, tipoReal) },
                                            jaAdicionado = jaNaLista,
                                            onAdicionarRapido = {
                                                onAdicionarClique(
                                                    item.idTmdb,
                                                    item.titulo,
                                                    tipoReal,
                                                    "Quero Assistir",
                                                    0,
                                                    item.sinopse,
                                                    if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "",
                                                    item.generoTexto,
                                                    plataformaFinal
                                                )
                                            },
                                            modoListaHorizontal = true
                                        )
                                    }
                                }

                                when (val appendState = resultadosPaginadosApi.loadState.append) {
                                    is LoadState.Loading -> {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                            }
                                        }
                                    }
                                    is LoadState.Error -> {
                                        item {
                                            Text(
                                                text = "Erro ao carregar mais itens: ${appendState.error.localizedMessage}",
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(
                                    count = resultadosPaginadosApi.itemCount,
                                    key = { index ->
                                        val item = resultadosPaginadosApi.peek(index)
                                        if (item != null) {
                                            "${item.mediaType ?: "midia"}_${item.idTmdb}_$index"
                                        } else {
                                            index
                                        }
                                    },
                                    contentType = resultadosPaginadosApi.itemContentType { "tmdb_media" }
                                ) { index ->
                                    val item = resultadosPaginadosApi[index]
                                    if (item != null) {
                                        val tipoReal = when {
                                            tipoPaginado != "Todos" -> tipoPaginado
                                            item.mediaType.equals("tv", ignoreCase = true) -> "Série"
                                            item.mediaType.equals("movie", ignoreCase = true) -> "Filme"
                                            item.ehSerie -> "Série"
                                            else -> "Filme"
                                        }

                                        val plataformaFinal = if (item.plataformaDetectada.isNotBlank()) {
                                            item.plataformaDetectada
                                        } else {
                                            if (tipoReal.equals("Filme", ignoreCase = true)) "Cinema" else "TV / Original"
                                        }

                                        val midiaItem = Midia(
                                            idTmdb = item.idTmdb,
                                            titulo = item.titulo,
                                            tipo = tipoReal,
                                            status = "Descobrir",
                                            nota = 0,
                                            sinopse = item.sinopse,
                                            imagemCapa = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "",
                                            genero = item.generoTexto,
                                            plataforma = plataformaFinal,
                                            favorito = false,
                                            listaCustomizada = "Geral"
                                        )

                                        val jaNaLista = listaDeMidias.any { it.idTmdb != 0 && it.idTmdb == item.idTmdb }

                                        ItemMidiaCard(
                                            midia = midiaItem,
                                            onClick = { onTmdbItemClique(item, tipoReal) },
                                            jaAdicionado = jaNaLista,
                                            onAdicionarRapido = {
                                                onAdicionarClique(
                                                    item.idTmdb,
                                                    item.titulo,
                                                    tipoReal,
                                                    "Quero Assistir",
                                                    0,
                                                    item.sinopse,
                                                    if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "",
                                                    item.generoTexto,
                                                    plataformaFinal
                                                )
                                            },
                                            modoListaHorizontal = false
                                        )
                                    }
                                }

                                when (val appendState = resultadosPaginadosApi.loadState.append) {
                                    is LoadState.Loading -> {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                            }
                                        }
                                    }
                                    is LoadState.Error -> {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Text(
                                                text = "Erro ao carregar mais itens: ${appendState.error.localizedMessage}",
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoGerenciarSalasCompartilhadas(
    viewModel: MidiaViewModel,
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    val gruposSalvos by viewModel.gruposSalvos.collectAsState(initial = emptyList())
    val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()

    var nomeGrupoInput by remember { mutableStateOf("") }
    var codigoGrupoInput by remember { mutableStateOf("") }
    var senhaGrupoInput by remember { mutableStateOf("") }
    var tipoGrupoSelecionado by remember { mutableStateOf("Casal") }
    var abaModoCriarEntrar by remember { mutableStateOf(0) } // 0 = Salvas, 1 = Criar, 2 = Entrar
    var mensagemErro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text("Listas Compartilhadas 🍿", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabRow(selectedTabIndex = abaModoCriarEntrar) {
                    Tab(selected = abaModoCriarEntrar == 0, onClick = { abaModoCriarEntrar = 0; mensagemErro = null }, text = { Text("Salas") })
                    Tab(selected = abaModoCriarEntrar == 1, onClick = { abaModoCriarEntrar = 1; mensagemErro = null }, text = { Text("Criar") })
                    Tab(selected = abaModoCriarEntrar == 2, onClick = { abaModoCriarEntrar = 2; mensagemErro = null }, text = { Text("Entrar") })
                }

                if (!mensagemErro.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mensagemErro ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (abaModoCriarEntrar == 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selecionarGrupoAtivo("")
                                onDispensar()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (casalIdAtivo.isBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Minha Lista Pessoal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Seus filmes e séries privados", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    if (gruposSalvos.isEmpty()) {
                        Text("Nenhuma sala salva ainda. Toque em 'Criar' ou 'Entrar' acima!", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    } else {
                        gruposSalvos.forEach { grupo ->
                            val ehAtiva = casalIdAtivo == grupo.grupoId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selecionarGrupoAtivo(grupo.grupoId)
                                        onDispensar()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (ehAtiva) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(grupo.nomeGrupo.ifBlank { grupo.grupoId }, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Código: ${grupo.grupoId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    IconButton(onClick = { viewModel.excluirGrupoSalvo(grupo) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir Grupo", tint = Color(0xFFFF5252))
                                    }
                                }
                            }
                        }
                    }
                } else if (abaModoCriarEntrar == 1) {
                    OutlinedTextField(
                        value = nomeGrupoInput,
                        onValueChange = { nomeGrupoInput = it },
                        label = { Text("Nome da Sala (ex: Casal ❤️)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = codigoGrupoInput,
                        onValueChange = { codigoGrupoInput = it.uppercase() },
                        label = { Text("Código da Sala") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = senhaGrupoInput,
                        onValueChange = { senhaGrupoInput = it },
                        label = { Text("Senha de Acesso (Opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { codigoGrupoInput = viewModel.gerarNovoCodigoGrupo() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Gerar Código", fontSize = 12.sp)
                        }
                        if (codigoGrupoInput.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Código da Sala", codigoGrupoInput)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(contexto, "Código copiado!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Copiar", fontSize = 12.sp)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (codigoGrupoInput.isNotBlank()) {
                                carregando = true
                                val nomeFinal = nomeGrupoInput.ifBlank { "Lista Compartilhada" }
                                viewModel.criarGrupoComSenha(codigoGrupoInput, nomeFinal, tipoGrupoSelecionado, senhaGrupoInput) { sucesso ->
                                    carregando = false
                                    if (sucesso) onDispensar()
                                    else mensagemErro = "Erro ao criar grupo na nuvem."
                                }
                            } else {
                                mensagemErro = "Preencha o código do grupo."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !carregando,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (carregando) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Criar e Entrar na Sala", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedTextField(
                        value = nomeGrupoInput,
                        onValueChange = { nomeGrupoInput = it },
                        label = { Text("Apelido Local da Lista (ex: Com o Amor)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = codigoGrupoInput,
                        onValueChange = { codigoGrupoInput = it.uppercase() },
                        label = { Text("Código da Sala Criada") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = senhaGrupoInput,
                        onValueChange = { senhaGrupoInput = it },
                        label = { Text("Senha da Sala (se houver)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (codigoGrupoInput.isNotBlank()) {
                                carregando = true
                                val nomeFinal = nomeGrupoInput.ifBlank { "Lista Compartilhada" }
                                viewModel.entrarEmGrupoExistente(codigoGrupoInput, nomeFinal, tipoGrupoSelecionado, senhaGrupoInput) { sucesso, erro ->
                                    carregando = false
                                    if (sucesso) onDispensar()
                                    else mensagemErro = erro
                                }
                            } else {
                                mensagemErro = "Digite o código da sala."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !carregando,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (carregando) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Verificar e Entrar na Sala", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDispensar) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun <T> FlowRowWithSpacing(
    items: List<T>,
    content: @Composable (T) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { item ->
            content(item)
        }
    }
}