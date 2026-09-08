package com.example.cinelist

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.cinelist.ui.theme.CineListTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val contexto = LocalContext.current
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

            // Solicitação de permissão de notificação para Android 13+ (Tiramisu)
            val launcherPermissaoNotificacao = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcherPermissaoNotificacao.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val midiaViewModel: MidiaViewModel = hiltViewModel()
            val updateInfo by midiaViewModel.updatePendente.collectAsState()

            CineListTheme(darkTheme = modoEscuroAtivo) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    updateInfo?.let { info ->
                        DialogoNovidadesAtualizacao(
                            info = info,
                            onDispensar = { midiaViewModel.dispensarUpdate() },
                            onConfirmarAtualizacao = {
                                midiaViewModel.dispensarUpdate()
                                UpdateManager.baixarEInstalarApk(contexto, info.urlApk)
                            }
                        )
                    }

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
val tiposDisponiveis = listOf("Filme", "Série", "Anime", "Novela", "Dorama")

@Composable
fun ConfiguracaoNavegacao() {
    val navController = rememberNavController()
    val usuarioLogado = remember { FirebaseAuth.getInstance().currentUser != null }
    val rotaInicial = if (usuarioLogado) "home" else "login"

    val viewModel: MidiaViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = rotaInicial) {
        composable("login") {
            TelaLogin(
                onLoginSucesso = { navController.navigate("home") { popUpTo("login") { inclusive = true } } },
                onNavegarParaCadastro = { navController.navigate("cadastro") }
            )
        }

        composable("cadastro") {
            TelaCadastro(
                onCadastroSucesso = { navController.navigate("home") { popUpTo("login") { inclusive = true } } },
                onVoltarParaLogin = { navController.popBackStack() }
            )
        }

        composable("home") {
            val listaDeMidiasReal by viewModel.todasAsMidias.collectAsState(initial = emptyList())

            TelaPrincipal(
                listaDeMidias = listaDeMidiasReal,
                viewModel = viewModel,
                onItemClique = { midiaClicada ->
                    navController.navigate("detalhes/${midiaClicada.id}/${midiaClicada.tipo}")
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
                        plataforma = plataforma
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
                onVoltar = { navController.popBackStack() }
            )
        }

        composable("perfil") {
            val listaDeMidiasReal by viewModel.todasAsMidias.collectAsState(initial = emptyList())
            TelaPerfil(
                listaDeMidias = listaDeMidiasReal,
                viewModel = viewModel,
                onVoltar = { navController.popBackStack() },
                onLogout = { navController.navigate("login") { popUpTo("home") { inclusive = true } } }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TelaPrincipal(
    listaDeMidias: List<Midia>,
    viewModel: MidiaViewModel,
    onAdicionarClique: (idTmdb: Int, titulo: String, tipo: String, status: String, nota: Int, sinopse: String, capa: String, genero: String, plataforma: String) -> Unit,
    onItemClique: (Midia) -> Unit,
    onTmdbItemClique: (TmdbFilme, String) -> Unit,
    onPerfilClique: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    var textoPesquisa by rememberSaveable { mutableStateOf("") }

    val streamingsFiltro = remember(listaDeMidias) {
        listOf("Todas") + listaDeMidias.map { it.plataforma }.filter { it.isNotBlank() && it != "Não Informado" }.distinct().sorted()
    }

    var categoriaSelecionada by rememberSaveable { mutableStateOf("Todos") }
    var filtroPlataforma by rememberSaveable { mutableStateOf("Todas") }
    var filtroStatusMinhaLista by rememberSaveable { mutableStateOf("Ativos") }
    var ordenacaoMinhaLista by rememberSaveable { mutableStateOf("Padrão (Assistindo primeiro)") }

    var mostrarDialogo by remember { mutableStateOf(false) }
    var mostrarBottomSheetFiltrosDescobrir by remember { mutableStateOf(false) }
    var mostrarBottomSheetFiltrosMinhaLista by remember { mutableStateOf(false) }

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
        if (tipoPaginado != "Filme") count++
        if (provedorSelecionadoId != null) count++
        if (generoSelecionadoId != null) count++
        if (ordenacaoSelecionada != "popularity.desc") count++
        count
    }

    val quantidadeFiltrosAtivosMinhaLista = remember(categoriaSelecionada, filtroPlataforma, filtroStatusMinhaLista, ordenacaoMinhaLista) {
        var count = 0
        if (categoriaSelecionada != "Todos") count++
        if (filtroPlataforma != "Todas") count++
        if (filtroStatusMinhaLista != "Ativos") count++
        if (ordenacaoMinhaLista != "Padrão (Assistindo primeiro)") count++
        count
    }

    LaunchedEffect(textoPesquisa) {
        delay(400)
        viewModel.atualizarQueryEFiltrarPaginado(textoPesquisa, tipoPaginado)
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

                Text("Categoria:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoriasMinhaLista.forEach { cat ->
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

                Text("Exibir Status:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Ativos", "Quero Assistir", "Assistindo", "Concluído", "Todos").forEach { s ->
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

                Text("Ordenar por:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    opcoesOrdenacaoMinhaLista.forEach { opt ->
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
                    Text("Plataforma:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        streamingsFiltro.forEach { st ->
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
                    Text("Aplicar", fontWeight = FontWeight.Bold)
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
                                viewModel.selecionarTipo("Filme")
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

                Text("Tipo de Conteúdo:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tiposDisponiveis.forEach { tipo ->
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

                Text("Ordenar por:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    opcoesOrdenacaoDescobrir.forEach { (rotulo, chaveSort) ->
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

                Text("Plataformas de Streaming:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    provedoresDisponiveis.forEach { (nome, id) ->
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

                val listaGenerosExibir = if (tipoPaginado == "Filme") generosFilmes else generosSeries
                Text("Gêneros:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listaGenerosExibir.forEach { (nome, id) ->
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
                        tiposDisponiveis.forEach { t ->
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
                            val streamingPrincipal = provedoresStreamingApi.firstOrNull()?.nomeProvedor ?: "Não Informado"
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
                title = { Text("CineList", fontWeight = FontWeight.Bold) },
                actions = {
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

            TextField(
                value = textoPesquisa,
                onValueChange = { textoPesquisa = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        if (pagerState.currentPage == 0) "Pesquisar na minha lista..." else "Buscar online no TMDB...",
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pagina ->
                if (pagina == 0) {
                    val listaFiltrada = listaDeMidias.filter { midia ->
                        val bateTexto = midia.titulo.contains(textoPesquisa, ignoreCase = true)
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
                        val batePlataforma = if (filtroPlataforma == "Todas") true else {
                            midia.plataforma.contains(filtroPlataforma, ignoreCase = true)
                        }
                        val bateStatus = when (filtroStatusMinhaLista) {
                            "Ativos" -> midia.status != "Concluído"
                            "Quero Assistir" -> midia.status == "Quero Assistir"
                            "Assistindo" -> midia.status == "Assistindo"
                            "Concluído" -> midia.status == "Concluído"
                            else -> true
                        }

                        bateTexto && bateCategoria && batePlataforma && bateStatus
                    }.let { lista ->
                        when (ordenacaoMinhaLista) {
                            "Melhor Avaliados" -> lista.sortedByDescending { it.nota }
                            "Ordem Alfabética (A-Z)" -> lista.sortedBy { it.titulo.lowercase() }
                            "Adicionados Recentemente" -> lista.sortedByDescending { it.id }
                            else -> lista.sortedByDescending { it.status == "Assistindo" }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (categoriaSelecionada != "Todos") "Exibindo: $categoriaSelecionada" else "Todos os títulos",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (quantidadeFiltrosAtivosMinhaLista > 0) {
                                    IconButton(
                                        onClick = {
                                            categoriaSelecionada = "Todos"
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
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = "Filtros",
                                            tint = if (quantidadeFiltrosAtivosMinhaLista > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (quantidadeFiltrosAtivosMinhaLista > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        labelColor = if (quantidadeFiltrosAtivosMinhaLista > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (listaFiltrada.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "Nenhum item encontrado com os filtros selecionados.", color = MaterialTheme.colorScheme.secondary)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(listaFiltrada) { mi ->
                                    ItemMidiaCard(midia = mi, onClick = { onItemClique(mi) })
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
                                if (quantidadeFiltrosAtivosDescobrir > 0) {
                                    IconButton(
                                        onClick = {
                                            viewModel.selecionarTipo("Filme")
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
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        labelColor = if (quantidadeFiltrosAtivosDescobrir > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            if (resultadosPaginadosApi.loadState.refresh is LoadState.Loading) {
                                items(6) {
                                    ItemMidiaCardSkeleton()
                                }
                            } else {
                                items(
                                    count = resultadosPaginadosApi.itemCount,
                                    key = resultadosPaginadosApi.itemKey { it.idTmdb },
                                    contentType = resultadosPaginadosApi.itemContentType { "tmdb_media" }
                                ) { index ->
                                    val item = resultadosPaginadosApi[index]
                                    if (item != null) {
                                        val midiaItem = Midia(
                                            idTmdb = item.idTmdb,
                                            titulo = item.titulo,
                                            tipo = tipoPaginado,
                                            status = "Descobrir",
                                            nota = 0,
                                            sinopse = item.sinopse,
                                            imagemCapa = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "",
                                            genero = item.generoTexto,
                                            plataforma = ""
                                        )
                                        ItemMidiaCard(
                                            midia = midiaItem,
                                            onClick = { onTmdbItemClique(item, tipoPaginado) }
                                        )
                                    }
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