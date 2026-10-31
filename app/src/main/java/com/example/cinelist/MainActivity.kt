package com.example.cinelist

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout



import android.app.Activity



import android.content.Context



import android.content.ContextWrapper



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



import kotlinx.coroutines.launch



import java.util.concurrent.TimeUnit



fun agendarChecagemAtualizacaoSegundoPlano(context: Context) {



try {



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



} catch (e: Throwable) {



e.printStackTrace()



}



}



fun agendarBackupAutomaticoSemanal(context: Context) {



try {



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



} catch (e: Throwable) {



e.printStackTrace()



}



}



@AndroidEntryPoint



class MainActivity : ComponentActivity() {

override fun attachBaseContext(newBase: Context) {
    val contextComLocale = IdiomaAppManager.aplicarContextLocale(newBase)
    super.attachBaseContext(contextComLocale)
}

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
IdiomaAppManager.inicializar(this)



if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {



try {



if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {



requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)



}



} catch (e: Throwable) {



e.printStackTrace()



}



}



try {
    enableEdgeToEdge()
} catch (e: Throwable) {
    android.util.Log.w("MainActivity", "EdgeToEdge indisponível: ${e.message}")
}



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



var corFundoSelecionada by remember { mutableStateOf(sharedPreferences.getString("cor_fundo", "Padrao") ?: "Padrao") }



var corTextoSelecionada by remember { mutableStateOf(sharedPreferences.getString("cor_texto", "Padrao") ?: "Padrao") }



DisposableEffect(sharedPreferences) {



val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, chave ->



if (chave == "modo_escuro") {



modoEscuroAtivo = sharedPreferences.getBoolean("modo_escuro", true)



}



if (chave == "cor_tema") {



corTemaSelecionada = sharedPreferences.getString("cor_tema", "CineList") ?: "CineList"



}



if (chave == "cor_fundo") {



corFundoSelecionada = sharedPreferences.getString("cor_fundo", "Padrao") ?: "Padrao"



}



if (chave == "cor_texto") {



corTextoSelecionada = sharedPreferences.getString("cor_texto", "Padrao") ?: "Padrao"



}



}



sharedPreferences.registerOnSharedPreferenceChangeListener(listener)



onDispose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }



}



CineListTheme(



darkTheme = modoEscuroAtivo,



corPersonalizada = corTemaSelecionada,



corFundo = corFundoSelecionada,



corTexto = corTextoSelecionada



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



fun ConfiguracaoNavegacao() {



val navController = rememberNavController()



val contexto = LocalContext.current



val usuarioLogado = remember { FirebaseAuth.getInstance().currentUser != null }



val rotaInicial = "splash"



val viewModel: MidiaViewModel = hiltViewModel()



val casalIdAtivoGlobal by viewModel.casalIdAtivo.collectAsState()



val activity = contexto.findActivity() as? ComponentActivity



val intentRecebida = activity?.intent



val tipoAcao = remember {



intentRecebida?.getStringExtra("TIPO_ACAO")



?: intentRecebida?.getStringExtra("tipoAcao")



?: intentRecebida?.getStringExtra("tipo")



}



val alvoId = remember {



intentRecebida?.getStringExtra("ALVO_ID")



?: intentRecebida?.getStringExtra("alvoId")



?: intentRecebida?.getStringExtra("GRUPO_ID")



?: intentRecebida?.getStringExtra("grupoId")



}



// 🚀 Pop-up automático quando há atualização no GitHub (igual ao LivennIA)



val infoAtu = viewModel.infoAtualizacao



if (viewModel.mostrarDialogoAtualizacao && infoAtu != null && viewModel.temNovaAtualizacao) {



DialogoAtualizacaoGitHub(



info = infoAtu,



versaoInstalada = viewModel.versaoInstaladaNome,



baixando = viewModel.baixandoAtualizacao,



progresso = viewModel.progressoAtualizacao,



apkPronto = viewModel.apkAtualizacaoBaixado?.exists() == true,



erro = viewModel.erroAtualizacao,



onBaixarOuInstalar = { viewModel.iniciarDownloadOuInstalacaoGitHub() },



onFechar = { viewModel.fecharDialogoAtualizacao() }



)



}



// Se o usuário tocou na notificação do sistema para abrir a atualização



val abrirAtualizacaoExtra = remember { intentRecebida?.getBooleanExtra("abrir_atualizacao", false) ?: false }



LaunchedEffect(abrirAtualizacaoExtra) {



if (abrirAtualizacaoExtra) {



intentRecebida?.removeExtra("abrir_atualizacao")



viewModel.verificarAtualizacaoGitHub(manual = true)



}



}



// 🚀 Subscreve automaticamente o utilizador logado no seu tópico privado e nas salas salvas



LaunchedEffect(Unit) {



try {



val meuUid = FirebaseAuth.getInstance().currentUser?.uid



if (!meuUid.isNullOrBlank()) {



try {



FirebaseMessaging.getInstance().subscribeToTopic("user_$meuUid")



} catch (e: Throwable) {



e.printStackTrace()



}



}



viewModel.socialRepository.atualizarTokenFcm()



launch {



viewModel.gruposSalvos.collect { grupos ->



grupos.forEach { g ->



if (g.grupoId.isNotBlank()) {



try {



FirebaseMessaging.getInstance().subscribeToTopic("grupo_${g.grupoId}")



} catch (e: Throwable) {



e.printStackTrace()



}



}



}



}



}



} catch (e: Throwable) {



e.printStackTrace()



}



}



LaunchedEffect(tipoAcao, alvoId) {



if (!tipoAcao.isNullOrBlank() && usuarioLogado) {



intentRecebida?.removeExtra("TIPO_ACAO")



intentRecebida?.removeExtra("tipoAcao")



intentRecebida?.removeExtra("tipo")



when (tipoAcao) {



"ABRIR_CHAT", "ABRIR_MINHA_LISTA", "CHAT_GRUPO", "MIDIA_ADICIONADA", "ENQUETE_CRIADA" -> {



if (!alvoId.isNullOrBlank()) {



viewModel.selecionarGrupoAtivo(alvoId)



}



navController.navigate("home") {



popUpTo("splash") { inclusive = true }



}



}



"ABRIR_PERFIL", "SOLICITACAO_AMIZADE", "AMIZADE_ACEITA" -> {



navController.navigate("perfil") {



popUpTo("splash") { inclusive = true }



}



}



"ABRIR_CHAT_PRIVADO", "CHAT_PRIVADO" -> {



navController.navigate("perfil") {



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



@OptIn(ExperimentalSharedTransitionApi::class)
SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
        NavHost(navController = navController, startDestination = rotaInicial) {



composable("splash") {



TelaSplashMp4(



onSplashConcluida = {
    val prefsOnboarding = contexto.getSharedPreferences("ConfiguracoesPerfil", Context.MODE_PRIVATE)
    val aberturaApresentada = prefsOnboarding.getBoolean("abertura_apresentada", false)
    val destinoFinal = when {
        usuarioLogado -> "home"
        !aberturaApresentada -> "abertura"
        else -> "login"
    }
    navController.navigate(destinoFinal) {
        popUpTo("splash") { inclusive = true }
    }
}
)
}

composable("abertura") {
    TelaAberturaApp(
        onNavegarParaLogin = {
            val prefsOnboarding = contexto.getSharedPreferences("ConfiguracoesPerfil", Context.MODE_PRIVATE)
            prefsOnboarding.edit().putBoolean("abertura_apresentada", true).apply()
            navController.navigate("login") { popUpTo("abertura") { inclusive = true } }
        },
        onNavegarParaCadastro = {
            val prefsOnboarding = contexto.getSharedPreferences("ConfiguracoesPerfil", Context.MODE_PRIVATE)
            prefsOnboarding.edit().putBoolean("abertura_apresentada", true).apply()
            navController.navigate("cadastro")
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
        onNavegarParaCadastro = { navController.navigate("cadastro") },
        onNavegarParaAbertura = { navController.navigate("abertura") }
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
CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {




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
}



composable("match") {
CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {




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
}



composable("detalhes/{id}/{tipo}") { backStackEntry ->
CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {




val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: 0



val tipo = backStackEntry.arguments?.getString("tipo") ?: "Filme"



TelaDetalhes(



id = id,



tipoInicial = tipo,



viewModel = viewModel,



onVoltar = { navController.popBackStack() },



onRecomendacaoClique = { novoId, novoTipo ->
    if (novoId > 0) {
        val tipoSanitizado = if (novoTipo.contains("serie", ignoreCase = true) || novoTipo.contains("série", ignoreCase = true) || novoTipo.contains("tv", ignoreCase = true)) "Serie" else "Filme"
        try {
            navController.navigate("detalhes/$novoId/$tipoSanitizado")
        } catch (e: Exception) {
            android.util.Log.e("CineListNav", "Erro ao navegar para detalhes: ${e.message}")
        }
    }
}



)



}
}



composable("perfil") {
CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {




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
}



}