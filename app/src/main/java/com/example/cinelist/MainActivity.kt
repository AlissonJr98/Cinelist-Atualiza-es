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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cinelist.ui.theme.CineListTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        enableEdgeToEdge()
        agendarChecagemAtualizacaoSegundoPlano(this)
        agendarBackupAutomaticoSemanal(this)

        setContent {
            val contexto = LocalContext.current
            val view = LocalView.current

            val sharedPreferences = remember {
                contexto.getSharedPreferences(
                    "ConfiguracoesPerfil",
                    Context.MODE_PRIVATE
                )
            }

            var modoEscuroAtivo by remember { mutableStateOf(sharedPreferences.getBoolean("modo_escuro", true)) }
            var corTemaSelecionada by remember { mutableStateOf(sharedPreferences.getString("cor_tema", "CineList") ?: "CineList") }

            DisposableEffect(sharedPreferences) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, chave ->
                    if (chave == "modo_escuro") {
                        modoEscuroAtivo = sharedPreferences.getBoolean("modo_escuro", true)
                    }
                    if (chave == "cor_tema") {
                        corTemaSelecionada = sharedPreferences.getString("cor_tema", "CineList") ?: "CineList"
                    }
                }
                sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                onDispose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            CineListTheme(
                darkTheme = modoEscuroAtivo,
                corPersonalizada = corTemaSelecionada
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize().statusBarsPadding(),
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

@Composable
fun ConfiguracaoNavegacao() {
    val navController = rememberNavController()
    val contexto = LocalContext.current
    val usuarioLogado = remember { FirebaseAuth.getInstance().currentUser != null }

    val rotaInicial = "splash"
    val viewModel: MidiaViewModel = hiltViewModel()
    val casalIdAtivoGlobal by viewModel.casalIdAtivo.collectAsState()

    val activity = contexto as? ComponentActivity
    val intentRecebida = activity?.intent
    val tipoAcao = remember { intentRecebida?.getStringExtra("TIPO_ACAO") }
    val alvoId = remember { intentRecebida?.getStringExtra("ALVO_ID") }

    // 🚀 Subscreve automaticamente o utilizador logado no tópico privado do FCM
    LaunchedEffect(Unit) {
        val meuUid = FirebaseAuth.getInstance().currentUser?.uid
        if (!meuUid.isNullOrBlank()) {
            FirebaseMessaging.getInstance().subscribeToTopic("user_$meuUid")
        }
        viewModel.socialRepository.atualizarTokenFcm()
    }

    LaunchedEffect(tipoAcao, alvoId) {
        if (!tipoAcao.isNullOrBlank() && usuarioLogado) {
            intentRecebida?.removeExtra("TIPO_ACAO")

            when (tipoAcao) {
                "ABRIR_CHAT", "ABRIR_MINHA_LISTA" -> {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }
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
                onItemClique = { midiaClicada ->
                    val idNavegacao = if (midiaClicada.idTmdb != 0) midiaClicada.idTmdb else midiaClicada.id
                    navController.navigate("detalhes/$idNavegacao/${midiaClicada.tipo}")
                },
                onTmdbItemClique = { itemTmdb, tipo ->
                    navController.navigate("detalhes/${itemTmdb.idTmdb}/$tipo")
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