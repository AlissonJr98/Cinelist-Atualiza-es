@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.cinelist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
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

// Fix #9 — backup automático e silencioso da lista pessoal para o Firestore, uma vez por semana
fun agendarBackupAutomaticoSemanal(context: Context) {
    val restricoes = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val requisicao = PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
        .setConstraints(restricoes)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "CineListBackupAutomatico",
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
        agendarBackupAutomaticoSemanal(this)

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
            ) { concedida ->
                if (concedida) {
                    sharedPreferences.edit().putBoolean("notificacoes", true).apply()
                    configurarLembretes(contexto, true)
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val jaTemPermissao = ContextCompat.checkSelfPermission(
                        contexto,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (jaTemPermissao) {
                        if (!sharedPreferences.getBoolean("notificacoes", false)) {
                            sharedPreferences.edit().putBoolean("notificacoes", true).apply()
                        }
                        configurarLembretes(contexto, true)
                    } else {
                        launcherPermissaoNotificacao.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    sharedPreferences.edit().putBoolean("notificacoes", true).apply()
                    configurarLembretes(contexto, true)
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
    val contexto = LocalContext.current
    val usuarioLogado = remember { FirebaseAuth.getInstance().currentUser != null }

    val rotaInicial = "splash"

    val viewModel: MidiaViewModel = hiltViewModel()

    val casalIdAtivoGlobal by viewModel.casalIdAtivo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.socialRepository.atualizarTokenFcm()
    }

    LaunchedEffect(Unit) {
        var primeiraExecucao = true
        var quantidadeAnterior = 0
        viewModel.solicitacoesRecebidas.collect { lista ->
            if (primeiraExecucao) {
                quantidadeAnterior = lista.size
                primeiraExecucao = false
            } else {
                if (lista.size > quantidadeAnterior) {
                    val novaSolicitacao = lista.firstOrNull()
                    if (novaSolicitacao != null) {
                        NotificacaoHelper.dispararNotificacaoSolicitacaoAmizade(
                            context = contexto,
                            remetenteNome = novaSolicitacao.nome
                        )
                    }
                }
                quantidadeAnterior = lista.size
            }
        }
    }

    LaunchedEffect(casalIdAtivoGlobal) {
        while (true) {
            viewModel.atualizarPresencaGlobal(true)
            if (casalIdAtivoGlobal.isNotBlank()) {
                viewModel.atualizarStatusPresenca(true)
            }
            delay(20_000L)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.atualizarPresencaGlobal(true)
        if (casalIdAtivoGlobal.isNotBlank()) {
            viewModel.atualizarStatusPresenca(true)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.atualizarPresencaGlobal(false)
        if (casalIdAtivoGlobal.isNotBlank()) {
            viewModel.atualizarStatusPresenca(false)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.atualizarPresencaGlobal(false)
        if (casalIdAtivoGlobal.isNotBlank()) {
            viewModel.atualizarStatusPresenca(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.atualizarPresencaGlobal(false)
            viewModel.atualizarStatusPresenca(false)
        }
    }

    NavHost(navController = navController, startDestination = rotaInicial) {

        composable("splash") {
            TelaSplashMp4(
                onSplashConcluida = {
                    // Fix #6 — onboarding aparece uma única vez, antes de login/home,
                    // controlado pelo mesmo SharedPreferences "ConfiguracoesPerfil" já usado no app
                    val prefsOnboarding = contexto.getSharedPreferences("ConfiguracoesPerfil", Context.MODE_PRIVATE)
                    val onboardingConcluido = prefsOnboarding.getBoolean("onboarding_concluido", false)

                    val destinoFinal = when {
                        !onboardingConcluido -> "onboarding"
                        usuarioLogado -> "home"
                        else -> "login"
                    }
                    navController.navigate(destinoFinal) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("onboarding") {
            TelaOnboarding(
                onConcluir = {
                    val prefsOnboarding = contexto.getSharedPreferences("ConfiguracoesPerfil", Context.MODE_PRIVATE)
                    prefsOnboarding.edit().putBoolean("onboarding_concluido", true).apply()

                    val destino = if (usuarioLogado) "home" else "login"
                    navController.navigate(destino) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            TelaLogin(
                onLoginSucesso = {
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                },
                onNavegarParaCadastro = { navController.navigate("cadastro") }
            )
        }

        composable("cadastro") {
            TelaCadastro(
                onCadastroSucesso = {
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
                onPerfilClique = { navController.navigate("perfil") },
                onAbrirMatch = { navController.navigate("match") }
            )
        }

        composable("match") {
            val midiasGrupoAtivo by viewModel.midiasGrupoAtivo.collectAsState(initial = emptyList())

            TelaModoMatch(
                listaDeMidiasDaSala = midiasGrupoAtivo,
                viewModel = viewModel,
                onVoltar = { navController.popBackStack() },
                onAbrirDetalhesMidia = { midia ->
                    val idNavegacao = if (midia.idTmdb != 0) midia.idTmdb else midia.id
                    navController.navigate("detalhes/$idNavegacao/${midia.tipo}")
                }
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
            val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()
            val midiasGrupoAtivo by viewModel.midiasGrupoAtivo.collectAsState(initial = emptyList())
            val midiasPessoais by viewModel.midiasPessoais.collectAsState(initial = emptyList())

            val listaDeMidiasReal = if (casalIdAtivo.isNotBlank()) midiasGrupoAtivo else midiasPessoais

            TelaPerfil(
                listaDeMidias = listaDeMidiasReal,
                viewModel = viewModel,
                onVoltar = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
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
            val listaDeMidiasReal by viewModel.todasAsMidias.collectAsState(emptyList())
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
    onPerfilClique: () -> Unit,
    onAbrirMatch: () -> Unit
) {
    val contextoLocal = LocalContext.current
    val historicoManager = remember { HistoricoBuscaManager(contextoLocal) }
    var historicoBuscas by remember { mutableStateOf(historicoManager.obterHistorico()) }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()
    val gruposSalvos by viewModel.gruposSalvos.collectAsState(initial = emptyList())
    var menuSalasExpandido by remember { mutableStateOf(false) }

    var textoPesquisa by rememberSaveable { mutableStateOf("") }
    // Fix #10 (histórico de busca) — só aparece com o campo em foco, some ao perder o foco
    val interacaoCampoBusca = remember { MutableInteractionSource() }
    val campoBuscaFocado by interacaoCampoBusca.collectIsFocusedAsState()
    var midiaParaExcluir by remember { mutableStateOf<Midia?>(null) }
    var midiaParaConcluir by remember { mutableStateOf<Midia?>(null) }
    var midiaParaRecusar by remember { mutableStateOf<Midia?>(null) }
    var colecaoParaExcluir by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoGerenciarSalas by remember { mutableStateOf(false) }

    var modoListaMinhaLista by rememberSaveable { mutableStateOf(false) }
    var modoListaDescobrir by rememberSaveable { mutableStateOf(false) }
    var mostrarDialogoSorteio by remember { mutableStateOf(false) }

    val membrosSala by viewModel.membrosGrupoAtivo.collectAsState(initial = emptyList())
    val meuUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val membroAssistindo = remember(membrosSala, meuUid) {
        membrosSala.firstOrNull { it.uid != meuUid && it.estaAssistindoAlgo }
    }

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

    if (midiaParaRecusar != null) {
        val midiaAlvo = midiaParaRecusar!!
        AlertDialog(
            onDismissRequest = { midiaParaRecusar = null },
            title = { Text("Recusar Sugestão ❌", fontWeight = FontWeight.Bold) },
            text = { Text("Não quer assistir \"${midiaAlvo.titulo}\" com o seu parceiro(a). Deseja adicioná-lo apenas à sua Lista Pessoal?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.processarMidiaRecusadaPeloParceiro(midiaAlvo, true)
                        midiaParaRecusar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Sim, minha lista", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.processarMidiaRecusadaPeloParceiro(midiaAlvo, false)
                        midiaParaRecusar = null
                    }
                ) {
                    Text("Não, descartar", color = CineListTokens.CorErro)
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
                        midiaParaExcluir?.let { viewModel.deletar(it) }
                        midiaParaExcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineListTokens.CorErro)
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

    if (midiaParaConcluir != null) {
        val midiaAlvo = midiaParaConcluir!!
        val estaConcluido = midiaAlvo.status.equals("Concluído", ignoreCase = true) || midiaAlvo.status.equals("Concluido", ignoreCase = true)
        val acaoTexto = if (estaConcluido) "Reabrir" else "Concluir"

        AlertDialog(
            onDismissRequest = { midiaParaConcluir = null },
            title = { Text("$acaoTexto Mídia", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (estaConcluido) {
                        "Deseja reabrir \"${midiaAlvo.titulo}\" e voltar para o status Assistindo?"
                    } else {
                        "Deseja realmente marcar \"${midiaAlvo.titulo}\" como Concluído?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (estaConcluido) {
                            viewModel.atualizar(midiaAlvo.copy(status = "Assistindo", dataConclusao = 0L, concluidoPor = ""))
                        } else {
                            viewModel.concluirMidia(midiaAlvo)
                        }
                        midiaParaConcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (estaConcluido) MaterialTheme.colorScheme.primary else CineListTokens.CorConcluidoEscuro)
                ) {
                    Text(acaoTexto, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { midiaParaConcluir = null }) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = CineListTokens.CorErro)
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
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir Coleção", tint = CineListTokens.CorErro)
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
                            Text("Redefinir", color = CineListTokens.CorErro)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Fix #6 — paleta padronizada: "primary" para filtros excludentes (categoria/status),
                // "secondaryContainer" para filtros informativos (coleção/ordenação/plataforma)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Coleção Temática:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { mostrarDialogoGerenciarColecoes = true }) {
                            Text("Gerenciar / Excluir", fontSize = 11.sp, color = CineListTokens.CorErro)
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
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                            Text("Redefinir Tudo", color = CineListTokens.CorErro)
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
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                // Fix #7 — cabeçalho com ícone em destaque, no mesmo padrão do DialogoNovidadesAtualizacao
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        "Adicionar Nova Mídia",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
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

    val nomeListaAtiva = if (casalIdAtivo.isBlank()) {
        "Minha Lista Pessoal"
    } else {
        gruposSalvos.find { it.grupoId == casalIdAtivo }?.nomeGrupo?.ifBlank { "Sala Compartilhada" } ?: "Sala Compartilhada"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { menuSalasExpandido = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CineList",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Trocar de Lista",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = nomeListaAtiva,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Fix #8 — itens do menu com Icon + Text alinhados, sem emoji cru no meio do texto
                    DropdownMenu(
                        expanded = menuSalasExpandido,
                        onDismissRequest = { menuSalasExpandido = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Minha Lista Pessoal", fontWeight = if (casalIdAtivo.isBlank()) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                viewModel.selecionarGrupoAtivo("")
                                menuSalasExpandido = false
                            }
                        )

                        if (gruposSalvos.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            gruposSalvos.forEach { grupo ->
                                val isAtivo = casalIdAtivo == grupo.grupoId
                                DropdownMenuItem(
                                    text = {
                                        val nomeExibicao = grupo.nomeGrupo.ifBlank { "Sala Compartilhada" }
                                        Text(nomeExibicao, fontWeight = if (isAtivo) FontWeight.Bold else FontWeight.Normal)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        viewModel.selecionarGrupoAtivo(grupo.grupoId)
                                        menuSalasExpandido = false
                                    }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        DropdownMenuItem(
                            text = { Text("Gerenciar Salas / Criar Nova") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            onClick = {
                                menuSalasExpandido = false
                                mostrarDialogoGerenciarSalas = true
                            }
                        )
                    }
                },
                actions = {
                    // Fix #4 — "Match" separado visualmente do grupo de ícones de ação
                    if (isModoCompartilhado) {
                        Button(
                            onClick = onAbrirMatch,
                            colors = ButtonDefaults.buttonColors(containerColor = CineListTokens.CorMatch),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Match 🍿", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        VerticalDivider(
                            modifier = Modifier.height(20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
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
            // Fix #3 — ícone "+" real em vez de Text("+")
            FloatingActionButton(
                onClick = { mostrarDialogo = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar mídia")
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

            // Fix #2 — card "assistindo agora" usando tokens do tema, não mais cores fixas de tema escuro
            if (isModoCompartilhado && membroAssistindo != null && pagerState.currentPage == 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CineListTokens.CorOnline.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = CineListTokens.CorOnline,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CineListTokens.CorSucesso))
                                Text(
                                    text = "${membroAssistindo.nome} está assistindo agora:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${membroAssistindo.assistindoAgoraTitulo} ${membroAssistindo.assistindoAgoraEpisodio}".trim(),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Fix #10 — busca com ícone de lupa e histórico que só aparece com o campo focado
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = textoPesquisa,
                        onValueChange = { textoPesquisa = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(Modifier),
                        interactionSource = interacaoCampoBusca,
                        placeholder = {
                            Text(
                                if (pagerState.currentPage == 0) "Buscar por título, gênero ou streaming..." else "Buscar online no TMDB...",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
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
                        shape = RoundedCornerShape(if (campoBuscaFocado && textoPesquisa.isBlank() && historicoBuscas.isNotEmpty()) 12.dp else 28.dp)
                    )

                    // Só mostra o histórico quando o campo está com foco — não ocupa espaço fixo na tela
                    if (campoBuscaFocado && textoPesquisa.isBlank() && historicoBuscas.isNotEmpty()) {
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
                                        Text("Limpar histórico", fontSize = 10.sp, color = CineListTokens.CorErro)
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
                    // Fix #8 — evita recalcular filtro+ordenação em toda recomposição da tela;
                    // só reprocessa quando algum dos filtros ou a lista de origem realmente mudam
                    val listaFiltrada = remember(
                        listaDeMidias,
                        textoPesquisa,
                        categoriaSelecionada,
                        colecaoSelecionada,
                        filtroPlataforma,
                        filtroStatusMinhaLista,
                        ordenacaoMinhaLista
                    ) {
                        listaDeMidias.filter { midia ->
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
                                "Ativos" -> !midia.status.equals("Concluído", ignoreCase = true) && !midia.status.equals("Concluido", ignoreCase = true)
                                "Favoritos" -> midia.favorito
                                "Quero Assistir" -> midia.status.equals("Quero Assistir", ignoreCase = true)
                                "Assistindo" -> midia.status.equals("Assistindo", ignoreCase = true)
                                "Concluído" -> midia.status.equals("Concluído", ignoreCase = true) || midia.status.equals("Concluido", ignoreCase = true)
                                else -> true
                            }

                            bateTexto && bateCategoria && bateColecao && batePlataforma && bateStatus
                        }.let { lista ->
                            when (ordenacaoMinhaLista) {
                                "Favoritos Primeiro" -> lista.sortedWith(
                                    compareByDescending<Midia> { it.favorito }
                                        .thenByDescending { it.status.equals("Assistindo", ignoreCase = true) }
                                )
                                "Melhor Avaliados" -> lista.sortedByDescending { it.nota }
                                "Ordem Alfabética (A-Z)" -> lista.sortedBy { it.titulo.lowercase() }
                                "Adicionados Recentemente" -> lista.sortedByDescending { it.id }
                                else -> lista.sortedByDescending { it.status.equals("Assistindo", ignoreCase = true) }
                            }
                        }
                    }

                    var isRefreshing by remember { mutableStateOf(false) }

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            coroutineScope.launch {
                                isRefreshing = true
                                viewModel.forcarSincronizacaoManual()
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
                                                tint = CineListTokens.CorErro,
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
                                // Fix #5 — empty state com ícone, no mesmo padrão do resto do app
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Nenhum item encontrado com esses filtros.", color = MaterialTheme.colorScheme.secondary)
                                    }
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
                                                onDeletar = { midiaParaExcluir = mi },
                                                onAlternarStatusConcluido = { midiaParaConcluir = mi },
                                                onAlternarFavorito = { viewModel.alternarFavorito(mi) },
                                                modoListaHorizontal = true,
                                                onAceitarPendente = { viewModel.aceitarMidiaPendente(mi) },
                                                onRecusarPendente = { viewModel.recusarMidiaPendente(mi) },
                                                onProcessarRecusado = { querListaPessoal ->
                                                    viewModel.processarMidiaRecusadaPeloParceiro(mi, querListaPessoal)
                                                }
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
                                                onDeletar = { midiaParaExcluir = mi },
                                                onAlternarStatusConcluido = { midiaParaConcluir = mi },
                                                onAlternarFavorito = { viewModel.alternarFavorito(mi) },
                                                modoListaHorizontal = false,
                                                onAceitarPendente = { viewModel.aceitarMidiaPendente(mi) },
                                                onRecusarPendente = { viewModel.recusarMidiaPendente(mi) },
                                                onProcessarRecusado = { querListaPessoal ->
                                                    viewModel.processarMidiaRecusadaPeloParceiro(mi, querListaPessoal)
                                                }
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
                                            tint = CineListTokens.CorErro,
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

                        // Fix #3 e #4 — estados de carregamento inicial (shimmer) e de erro/offline
                        // tratados separadamente do "sem resultados", em vez de tela em branco
                        val estadoCargaInicial = resultadosPaginadosApi.loadState.refresh

                        when {
                            estadoCargaInicial is LoadState.Loading && resultadosPaginadosApi.itemCount == 0 -> {
                                if (modoListaDescobrir) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(6) { CardMidiaEsqueleto(modoListaHorizontal = true) }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(6) { CardMidiaEsqueleto(modoListaHorizontal = false) }
                                    }
                                }
                            }

                            estadoCargaInicial is LoadState.Error && resultadosPaginadosApi.itemCount == 0 -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Não foi possível carregar.\nVerifique sua conexão com a internet.",
                                            color = MaterialTheme.colorScheme.secondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { resultadosPaginadosApi.retry() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Tentar novamente", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            resultadosPaginadosApi.itemCount == 0 -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Nenhum resultado encontrado.", color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }

                            modoListaDescobrir -> {
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
                            }

                            else -> {
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
}

fun compartilharCodigoSalaWhatsApp(context: Context, codigoSala: String, nomeSala: String, senhaSala: String) {
    val senhaTexto = if (senhaSala.isNotBlank()) " e senha: *$senhaSala*" else ""
    val mensagem = "🍿 Olá! Entra na minha sala compartilhada no *CineList* para vermos filmes juntos!\n\n" +
            "📍 Sala: *$nomeSala*\n" +
            "🔑 Código: `$codigoSala`$senhaTexto\n\n" +
            "Baixe o app e insira o código para se conectar!"

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, mensagem)
        setPackage("com.whatsapp")
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val intentFallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(mensagem)}"))
        context.startActivity(intentFallback)
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
    val isAdministrador by viewModel.isAdministradorSala.collectAsState()

    var nomeGrupoInput by remember { mutableStateOf("") }
    var codigoGrupoInput by remember { mutableStateOf("") }
    var senhaGrupoInput by remember { mutableStateOf("") }
    var novaSenhaInput by remember { mutableStateOf("") }
    var tipoGrupoSelecionado by remember { mutableStateOf("Casal") }
    var abaModoCriarEntrar by remember { mutableStateOf(0) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }

    val membrosSala by viewModel.membrosGrupoAtivo.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDispensar,
        title = { Text("Listas Compartilhadas 🍿", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
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

                    if (casalIdAtivo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                compartilharCodigoSalaWhatsApp(
                                    context = contexto,
                                    codigoSala = casalIdAtivo,
                                    nomeSala = "Sala Compartilhada",
                                    senhaSala = ""
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CineListTokens.CorWhatsapp
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Convidar via WhatsApp 💚", fontWeight = FontWeight.Bold)
                        }

                        // PAINEL DE ADMINISTRADOR DA SALA ATIVA
                        if (isAdministrador) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("👑 Painel de Administrador", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)

                                    // Alterar Senha
                                    OutlinedTextField(
                                        value = novaSenhaInput,
                                        onValueChange = { novaSenhaInput = it },
                                        label = { Text("Nova Senha da Sala") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            if (novaSenhaInput.isNotBlank()) {
                                                viewModel.atualizarSenhaDaSala(casalIdAtivo, novaSenhaInput) { sucesso ->
                                                    if (sucesso) {
                                                        Toast.makeText(contexto, "Senha atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                                                        novaSenhaInput = ""
                                                    } else {
                                                        Toast.makeText(contexto, "Erro ao atualizar senha.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Salvar Nova Senha", fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Membros na Sala (${membrosSala.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                    membrosSala.forEach { membro ->
                                        val meuUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                        val ehMim = membro.uid == meuUid
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(membro.nome + if (ehMim) " (Você)" else "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            if (!ehMim) {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.excluirMembroDaSala(casalIdAtivo, membro.uid) { sucesso ->
                                                            if (sucesso) {
                                                                Toast.makeText(contexto, "${membro.nome} foi removido da sala.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Text("Remover", color = CineListTokens.CorErro, fontSize = 11.sp)
                                                }
                                            }
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
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir Grupo", tint = CineListTokens.CorErro)
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

                    // CAMPO COM PREFIXO CINE- AUTOMÁTICO
                    OutlinedTextField(
                        value = codigoGrupoInput,
                        onValueChange = { digitos ->
                            codigoGrupoInput = digitos.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("Código Numérico") },
                        prefix = { Text("CINE-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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

                    // Fix #9 — "Gerar Código" é a ação principal (Outlined); "Copiar" é conveniência (TextButton)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val aleatorio = (1000..9999).random().toString()
                                codigoGrupoInput = aleatorio
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Gerar Código", fontSize = 12.sp)
                        }
                        if (codigoGrupoInput.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    val codigoCompleto = "CINE-$codigoGrupoInput"
                                    val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Código da Sala", codigoCompleto)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(contexto, "Código $codigoCompleto copiado!", android.widget.Toast.LENGTH_SHORT).show()
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
                                val codigoCompleto = "CINE-$codigoGrupoInput"
                                viewModel.criarGrupoComSenha(codigoCompleto, nomeFinal, tipoGrupoSelecionado, senhaGrupoInput) { sucesso ->
                                    carregando = false
                                    if (sucesso) onDispensar()
                                    else mensagemErro = "Erro ao criar grupo na nuvem."
                                }
                            } else {
                                mensagemErro = "Preencha os números do código."
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
                        label = { Text("Apelido Local da Lista") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // CAMPO COM PREFIXO CINE- AUTOMÁTICO PARA ENTRAR
                    OutlinedTextField(
                        value = codigoGrupoInput,
                        onValueChange = { digitos ->
                            codigoGrupoInput = digitos.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("Código Numérico") },
                        prefix = { Text("CINE-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
                                val codigoCompleto = "CINE-$codigoGrupoInput"
                                viewModel.entrarEmGrupoExistente(codigoCompleto, nomeFinal, tipoGrupoSelecionado, senhaGrupoInput) { sucesso, erro ->
                                    carregando = false
                                    if (sucesso) onDispensar()
                                    else mensagemErro = erro
                                }
                            } else {
                                mensagemErro = "Digite os números do código."
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