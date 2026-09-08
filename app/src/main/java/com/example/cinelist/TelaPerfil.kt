package com.example.cinelist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun configurarLembretes(context: Context, ativar: Boolean) {
    val workManager = WorkManager.getInstance(context)
    if (ativar) {
        val agora = Calendar.getInstance()
        val horarioDesejado = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (agora.after(horarioDesejado)) {
            horarioDesejado.add(Calendar.DAY_OF_YEAR, 1)
        }

        val atrasoInicialMs = horarioDesejado.timeInMillis - agora.timeInMillis

        val requisicao = PeriodicWorkRequestBuilder<LembreteWorker>(24L, TimeUnit.HOURS)
            .setInitialDelay(atrasoInicialMs, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "CineListLembretesDiarios",
            ExistingPeriodicWorkPolicy.UPDATE,
            requisicao
        )
    } else {
        workManager.cancelUniqueWork("CineListLembretesDiarios")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TelaPerfil(
    listaDeMidias: List<Midia>,
    viewModel: MidiaViewModel,
    onVoltar: () -> Unit,
    onLogout: () -> Unit
) {
    val firebaseAuth = FirebaseAuth.getInstance()
    val usuarioAtual = firebaseAuth.currentUser
    val contexto = LocalContext.current
    val escopoCorrotina = rememberCoroutineScope()
    val escopoTabs = rememberCoroutineScope()

    val sharedPreferences = remember {
        contexto.getSharedPreferences("ConfiguracoesPerfil", Context.MODE_PRIVATE)
    }

    val titulosAbas = listOf("Perfil", "Notificações", "Estatísticas", "Histórico")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { titulosAbas.size })
    var abaSelecionada by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        abaSelecionada = pagerState.currentPage
    }

    val listaNotificacoes by viewModel.todasNotificacoes.collectAsState(initial = emptyList())
    val quantidadeNaoLidas by viewModel.quantidadeNaoLidas.collectAsState(initial = 0)

    // Controle de Exclusão e Visualização de Notificações
    var notificacaoParaExcluir by remember { mutableStateOf<NotificacaoEntity?>(null) }
    var notificacaoDetalhada by remember { mutableStateOf<NotificacaoEntity?>(null) }
    var mostrarConfirmacaoLimparTudoNotif by remember { mutableStateOf(false) }

    // Estados do Verificador de Atualizações OTA
    var verificandoAtualizacao by remember { mutableStateOf(false) }
    var infoNovaVersao by remember { mutableStateOf<InfoAtualizacao?>(null) }
    var mostrarDialogoAtualizacao by remember { mutableStateOf(false) }

    val urlJsonAtualizacao = "https://raw.githubusercontent.com/AlissonJr98/Cinelist-Atualiza-es/main/version.json"

    var receberNotificacoes by remember {
        mutableStateOf(sharedPreferences.getBoolean("notificacoes", false))
    }

    val permissaoNotificacaoLauncher = rememberLauncherForActivityResult(
        contract = RequestPermission()
    ) { concedida ->
        if (concedida) {
            receberNotificacoes = true
            sharedPreferences.edit().putBoolean("notificacoes", true).apply()
            configurarLembretes(contexto, true)
            Toast.makeText(contexto, "Notificações diárias às 20h ativadas!", Toast.LENGTH_SHORT).show()
        } else {
            receberNotificacoes = false
            sharedPreferences.edit().putBoolean("notificacoes", false).apply()
            Toast.makeText(contexto, "Permissão negada. Ative nas configurações do aparelho.", Toast.LENGTH_LONG).show()
        }
    }

    var fotoPerfilUriString by remember { mutableStateOf(sharedPreferences.getString("foto_perfil", "") ?: "") }
    val seletorGaleriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            fotoPerfilUriString = it.toString()
            sharedPreferences.edit().putString("foto_perfil", fotoPerfilUriString).apply()
        }
    }

    val exportarLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val sucesso = BackupManager.exportarParaJson(contexto, it, listaDeMidias)
            Toast.makeText(
                contexto,
                if (sucesso) "Backup exportado com sucesso!" else "Erro ao exportar backup.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val importarLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val midiasImportadas = BackupManager.importarDeJson(contexto, it)
            if (!midiasImportadas.isNullOrEmpty()) {
                viewModel.importarMidiasEmLote(midiasImportadas)
                Toast.makeText(
                    contexto,
                    "${midiasImportadas.size} mídias restauradas com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(contexto, "Arquivo inválido ou vazio.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var nomeExibicao by remember { mutableStateOf(usuarioAtual?.displayName ?: "Usuário CineList") }
    var modoEdicaoNome by remember { mutableStateOf(false) }
    var novoNome by remember { mutableStateOf(nomeExibicao) }
    var carregandoNome by remember { mutableStateOf(false) }

    val emailUsuario = usuarioAtual?.email ?: "E-mail não cadastrado"

    var biografia by remember { mutableStateOf(sharedPreferences.getString("bio", "") ?: "") }
    var modoEdicaoBio by remember { mutableStateOf(false) }
    var novaBio by remember { mutableStateOf(biografia) }

    var generoFavorito by remember { mutableStateOf(sharedPreferences.getString("genero", "Não definido") ?: "Não definido") }
    var menuGeneroExpandido by remember { mutableStateOf(false) }
    val listaGeneros = listOf("Ação", "Animes", "Comédia", "Drama", "Ficção Científica", "Terror", "Suspense", "Romance", "Novelas", "Doramas")

    var mostrarConfirmacaoReset by remember { mutableStateOf(false) }
    var mostrarConfirmacaoExclusao by remember { mutableStateOf(false) }
    var erroExclusao by remember { mutableStateOf("") }
    var carregandoExclusao by remember { mutableStateOf(false) }

    val totalMidias = listaDeMidias.size
    val totalFilmes = listaDeMidias.count { it.tipo.equals("Filme", ignoreCase = true) }
    val totalSeriesAnimes = listaDeMidias.count {
        it.tipo.equals("Série", ignoreCase = true) ||
                it.tipo.equals("Anime", ignoreCase = true) ||
                it.tipo.equals("Novela", ignoreCase = true) ||
                it.tipo.equals("Dorama", ignoreCase = true)
    }

    val filmesConcluidos = listaDeMidias.count { it.tipo.equals("Filme", ignoreCase = true) && it.status == "Concluído" }
    val seriesEAnimes = listaDeMidias.filter { !it.tipo.equals("Filme", ignoreCase = true) }
    val seriesConcluidas = seriesEAnimes.count { it.status == "Concluído" }

    val totalEpisodiosAssistidos = seriesEAnimes.sumOf { if (it.episodioAtual > 0) it.episodioAtual - 1 else 0 }
    val minutosTotais = (filmesConcluidos * 115) + (totalEpisodiosAssistidos * 45)
    val horasTotais = minutosTotais / 60
    val diasTotais = horasTotais / 24
    val horasRestantes = horasTotais % 24

    val tempoFormatado = if (diasTotais > 0) "${diasTotais}d ${horasRestantes}h" else "${horasTotais}h"

    val midiasComNota = listaDeMidias.filter { it.nota > 0 }
    val mediaNotas = if (midiasComNota.isNotEmpty()) {
        String.format("%.1f", midiasComNota.map { it.nota }.average())
    } else {
        "0.0"
    }

    val listaHistoricoConcluido = remember(listaDeMidias) {
        listaDeMidias.filter { it.status == "Concluído" }
    }

    val estatisticasGenero = remember(listaDeMidias) {
        listaDeMidias
            .filter { it.genero.isNotBlank() && it.genero != "Geral" }
            .groupingBy { it.genero }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(6)
            .toMap()
    }

    val estatisticasPlataforma = remember(listaDeMidias) {
        listaDeMidias
            .filter { it.plataforma.isNotBlank() && it.plataforma != "Não Informado" }
            .groupingBy { it.plataforma }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()
    }

    // Modal de Detalhes da Notificação / Changelog Completo
    notificacaoDetalhada?.let { notif ->
        val dataFormatada = remember(notif.dataCriacao) {
            SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()).format(Date(notif.dataCriacao))
        }

        AlertDialog(
            onDismissRequest = { notificacaoDetalhada = null },
            icon = {
                Icon(
                    imageVector = if (notif.tipo == "ATUALIZACAO") Icons.Default.SystemUpdate else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = notif.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recebida em: $dataFormatada",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = notif.mensagem,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { notificacaoDetalhada = null }) {
                    Text("Fechar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Confirmação para deletar UMA notificação
    notificacaoParaExcluir?.let { notif ->
        AlertDialog(
            onDismissRequest = { notificacaoParaExcluir = null },
            title = { Text("Excluir Notificação", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente excluir este aviso do seu histórico?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletarNotificacao(notif)
                        notificacaoParaExcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { notificacaoParaExcluir = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Confirmação para LIMPAR TODAS as notificações
    if (mostrarConfirmacaoLimparTudoNotif) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoLimparTudoNotif = false },
            title = { Text("Limpar Histórico de Avisos", fontWeight = FontWeight.Bold) },
            text = { Text("Todas as notificações de atualizações e lembretes serão apagadas.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.limparTodasNotificacoes()
                        mostrarConfirmacaoLimparTudoNotif = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Limpar Tudo", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoLimparTudoNotif = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Diálogo de Nova Versão Disponível
    if (mostrarDialogoAtualizacao && infoNovaVersao != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAtualizacao = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Atualização",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nova Versão Disponível!", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Versão: ${infoNovaVersao?.versaoNome} (Build ${infoNovaVersao?.versaoCode})",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Novidades:\n${infoNovaVersao?.notasDaVersao}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoAtualizacao = false
                        UpdateManager.baixarEInstalarApk(contexto, infoNovaVersao!!.urlApk)
                        Toast.makeText(contexto, "Baixando atualização...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Atualizar Agora", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoAtualizacao = false }) {
                    Text("Depois", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    if (mostrarConfirmacaoReset) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoReset = false },
            title = { Text("Limpar Toda a Lista?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = { Text("Atenção: Esta ação vai apagar permanentemente todos os filmes, séries e animes salvos na sua lista local. Deseja continuar?", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        listaDeMidias.forEach { viewModel.deletar(it) }
                        mostrarConfirmacaoReset = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                ) {
                    Text("Sim, Limpar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoReset = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    if (mostrarConfirmacaoExclusao) {
        AlertDialog(
            onDismissRequest = {
                if (!carregandoExclusao) {
                    mostrarConfirmacaoExclusao = false
                    erroExclusao = ""
                }
            },
            title = { Text("Excluir Sua Conta Permanentemente?", color = Color(0xFFFF4C4C), fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Esta ação não pode ser desfeita. O seu perfil e credenciais de acesso serão totalmente apagados dos nossos servidores do Firebase.", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                    if (erroExclusao.isNotEmpty()) {
                        Text(text = erroExclusao, color = Color(0xFFFF4C4C), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                if (carregandoExclusao) {
                    CircularProgressIndicator(color = Color(0xFFFF4C4C), modifier = Modifier.size(24.dp))
                } else {
                    Button(
                        onClick = {
                            carregandoExclusao = true
                            erroExclusao = ""
                            usuarioAtual?.delete()?.addOnCompleteListener { tarefa ->
                                carregandoExclusao = false
                                if (tarefa.isSuccessful) {
                                    mostrarConfirmacaoExclusao = false
                                    onLogout()
                                } else {
                                    val msgErro = tarefa.exception?.message ?: ""
                                    erroExclusao = if (msgErro.contains("reauthenticate")) {
                                        "Por segurança, faça sair da conta e entre novamente antes de realizar esta operação."
                                    } else {
                                        "Não foi possível excluir a conta. Tente novamente mais tarde."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                    ) {
                        Text("Excluir Definitivamente", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!carregandoExclusao) {
                    TextButton(onClick = { mostrarConfirmacaoExclusao = false; erroExclusao = "" }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Meu Perfil CineList", fontWeight = FontWeight.Bold) },
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

                TabRow(
                    selectedTabIndex = abaSelecionada,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[abaSelecionada]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    titulosAbas.forEachIndexed { index, titulo ->
                        Tab(
                            selected = abaSelecionada == index,
                            onClick = {
                                escopoTabs.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                if (index == 1 && quantidadeNaoLidas > 0) {
                                    BadgedBox(badge = { Badge { Text(quantidadeNaoLidas.toString()) } }) {
                                        Text(
                                            titulo,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Text(
                                        titulo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pagina ->
            when (pagina) {
                // ABA 0: PERFIL & PREFERÊNCIAS
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { seletorGaleriaLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (fotoPerfilUriString.isNotEmpty()) {
                                AsyncImage(
                                    model = fotoPerfilUriString,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(text = nomeExibicao.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Text(text = "Toque para alterar a foto", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(10.dp))

                        if (modoEdicaoNome) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = novoNome,
                                    onValueChange = { novoNome = it },
                                    label = { Text("Alterar Nome") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (carregandoNome) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                } else {
                                    IconButton(onClick = {
                                        if (novoNome.isNotBlank()) {
                                            carregandoNome = true
                                            val atualizacao = userProfileChangeRequest { displayName = novoNome.trim() }
                                            usuarioAtual?.updateProfile(atualizacao)?.addOnCompleteListener { t ->
                                                carregandoNome = false
                                                if (t.isSuccessful) { nomeExibicao = novoNome.trim(); modoEdicaoNome = false }
                                            }
                                        }
                                    }) { Icon(imageVector = Icons.Default.Check, contentDescription = "Salvar", tint = Color.Green) }
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                Text(text = nomeExibicao, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { modoEdicaoNome = true; novoNome = nomeExibicao }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar nome", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(text = emailUsuario, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 12.dp))

                        if (modoEdicaoBio) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = novaBio,
                                    onValueChange = { novaBio = it },
                                    label = { Text("Escreva algo sobre você...") },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    biografia = novaBio.trim()
                                    sharedPreferences.edit().putString("bio", biografia).apply()
                                    modoEdicaoBio = false
                                }) { Icon(imageVector = Icons.Default.Check, contentDescription = "Salvar Bio", tint = Color.Green) }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                Text(text = if (biografia.isEmpty()) "Adicione uma biografia..." else "\"$biografia\"", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { modoEdicaoBio = true; novaBio = biografia }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar Bio", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Resumo da Lista", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ItemEstatistica(titulo = "Total", valor = totalMidias.toString(), modifier = Modifier.weight(1f))
                            ItemEstatistica(titulo = "Filmes", valor = totalFilmes.toString(), modifier = Modifier.weight(1f))
                            ItemEstatistica(titulo = "Séries/Outros", valor = totalSeriesAnimes.toString(), modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Preferências do CineList", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Gênero Favorito", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                    Box {
                                        Text(
                                            text = generoFavorito,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier
                                                .clickable { menuGeneroExpandido = true }
                                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                        DropdownMenu(expanded = menuGeneroExpandido, onDismissRequest = { menuGeneroExpandido = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                            listaGeneros.forEach { item ->
                                                DropdownMenuItem(
                                                    text = { Text(item, color = MaterialTheme.colorScheme.onSurface) },
                                                    onClick = {
                                                        generoFavorito = item
                                                        sharedPreferences.edit().putString("genero", generoFavorito).apply()
                                                        menuGeneroExpandido = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Lembretes e Notificações", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                        Text(text = "Avisos diários às 20h para continuar maratonando", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = receberNotificacoes,
                                        onCheckedChange = { valor ->
                                            if (valor) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    val jaTemPermissao = ContextCompat.checkSelfPermission(
                                                        contexto,
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    ) == PackageManager.PERMISSION_GRANTED

                                                    if (jaTemPermissao) {
                                                        receberNotificacoes = true
                                                        sharedPreferences.edit().putBoolean("notificacoes", true).apply()
                                                        configurarLembretes(contexto, true)
                                                    } else {
                                                        permissaoNotificacaoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                } else {
                                                    receberNotificacoes = true
                                                    sharedPreferences.edit().putBoolean("notificacoes", true).apply()
                                                    configurarLembretes(contexto, true)
                                                }
                                            } else {
                                                receberNotificacoes = false
                                                sharedPreferences.edit().putBoolean("notificacoes", false).apply()
                                                configurarLembretes(contexto, false)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.background
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var modoEscuro by remember { mutableStateOf(sharedPreferences.getBoolean("modo_escuro", true)) }

                                    Text(text = "Tema Escuro do Aplicativo", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                    Switch(
                                        checked = modoEscuro,
                                        onCheckedChange = { valor ->
                                            modoEscuro = valor
                                            sharedPreferences.edit().putBoolean("modo_escuro", valor).apply()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.background
                                        )
                                    )
                                }

                                TextButton(onClick = { mostrarConfirmacaoReset = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF4C4C))) {
                                    Text("Limpar Todos os Dados da Lista", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Atualizações do Aplicativo",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Verifique e instale as versões mais recentes do Cinelist",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !verificandoAtualizacao) {
                                            verificandoAtualizacao = true
                                            escopoCorrotina.launch {
                                                val info = withContext(Dispatchers.IO) {
                                                    UpdateManager.checarAtualizacao(urlJsonAtualizacao)
                                                }
                                                verificandoAtualizacao = false

                                                val versaoAtual = BuildConfig.VERSION_CODE
                                                if (info != null && info.versaoCode > versaoAtual) {
                                                    infoNovaVersao = info
                                                    mostrarDialogoAtualizacao = true
                                                } else if (info != null) {
                                                    Toast.makeText(contexto, "Você já está na versão mais recente!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(contexto, "Não foi possível verificar atualizações no momento.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Verificar Atualização",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Instalada: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    if (verificandoAtualizacao) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = "Atualizar",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Backup e Sincronização Local",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Exporte sua lista para um arquivo JSON seguro ou restaure dados salvos previamente.",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { exportarLauncher.launch("cinelist_backup_${System.currentTimeMillis()}.json") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Exportar (JSON)", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { importarLauncher.launch(arrayOf("application/json")) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Importar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Informações da Conta", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Provedor de Login: ${usuarioAtual?.providerData?.lastOrNull()?.providerId?.uppercase() ?: "E-MAIL"}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "ID do Usuário: ${usuarioAtual?.uid?.take(12)}...", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Deseja excluir permanentemente sua conta?",
                                    color = Color(0xFFFF4C4C),
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { mostrarConfirmacaoExclusao = true }
                                        .padding(vertical = 4.dp),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { firebaseAuth.signOut(); onLogout() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                        ) {
                            Text(text = "SAIR DA CONTA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                // ABA 1: NOTIFICAÇÕES (Histórico Completo + Exclusão Individual e Limpeza Total)
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Histórico de Notificações",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (listaNotificacoes.any { !it.lida }) {
                                    TextButton(onClick = { viewModel.marcarTodasNotificacoesComoLidas() }) {
                                        Text("Marcar lidas", fontSize = 12.sp)
                                    }
                                }

                                if (listaNotificacoes.isNotEmpty()) {
                                    IconButton(onClick = { mostrarConfirmacaoLimparTudoNotif = true }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "Limpar todas",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (listaNotificacoes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Nenhuma notificação registrada ainda.",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(listaNotificacoes, key = { it.id }) { notificacao ->
                                    ItemNotificacao(
                                        notificacao = notificacao,
                                        onClick = {
                                            if (!notificacao.lida) {
                                                viewModel.marcarNotificacaoComoLida(notificacao.id)
                                            }
                                            notificacaoDetalhada = notificacao
                                        },
                                        onDeletar = { notificacaoParaExcluir = notificacao }
                                    )
                                }
                            }
                        }
                    }
                }

                // ABA 2: ESTATÍSTICAS E DASHBOARD COMPLETO
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Sua Jornada Cinéfila", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Métricas consolidadas a partir do seu histórico local de filmes, animes e séries maratonadas.", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CardEstatisticaDetalhada(titulo = "Total Geral", valor = totalMidias.toString(), subtexto = "mídias cadastradas", modifier = Modifier.weight(1f))
                            CardEstatisticaDetalhada(titulo = "Tempo Estimado", valor = tempoFormatado, subtexto = "horas assistidas", modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CardEstatisticaDetalhada(titulo = "Filmes Vistos", valor = filmesConcluidos.toString(), subtexto = "de $totalFilmes na lista", modifier = Modifier.weight(1f))
                            CardEstatisticaDetalhada(titulo = "Séries Vistas", valor = seriesConcluidas.toString(), subtexto = "de $totalSeriesAnimes na lista", modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CardEstatisticaDetalhada(titulo = "Episódios", valor = totalEpisodiosAssistidos.toString(), subtexto = "episódios maratonados", modifier = Modifier.weight(1f))
                            CardEstatisticaDetalhada(titulo = "Média Avaliações", valor = "$mediaNotas ★", subtexto = "${midiasComNota.size} títulos avaliados", modifier = Modifier.weight(1f))
                        }

                        if (estatisticasGenero.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Gêneros Predominantes",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ListaGenerosMaisAssistidos(dados = estatisticasGenero)
                                }
                            }
                        }

                        if (estatisticasPlataforma.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Onde Você Mais Assiste",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ListaPlataformasMaisUtilizadas(dados = estatisticasPlataforma)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // ABA 3: HISTÓRICO DE CONCLUÍDOS
                3 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
                    ) {
                        Text(
                            text = "Seus Títulos Concluídos (${listaHistoricoConcluido.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (listaHistoricoConcluido.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Nenhum item marcado como Concluído ainda.", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(listaHistoricoConcluido) { itemConcluido ->
                                    ItemMidiaCard(midia = itemConcluido, onClick = { /* Detalhes */ })
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
fun ItemNotificacao(
    notificacao: NotificacaoEntity,
    onClick: () -> Unit,
    onDeletar: () -> Unit
) {
    val dataFormatada = remember(notificacao.dataCriacao) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(notificacao.dataCriacao))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notificacao.lida)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (notificacao.tipo == "ATUALIZACAO") Icons.Default.Update else Icons.Default.NotificationsActive,
                contentDescription = notificacao.tipo,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notificacao.titulo,
                    fontWeight = if (notificacao.lida) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notificacao.mensagem,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dataFormatada,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDeletar) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CardEstatisticaDetalhada(
    titulo: String,
    valor: String,
    subtexto: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(108.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = titulo, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = valor, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtexto, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
fun ItemEstatistica(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = titulo, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ListaGenerosMaisAssistidos(dados: Map<String, Int>) {
    val total = dados.values.sum()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        dados.forEach { (genero, quantidade) ->
            val porcentagem = if (total > 0) (quantidade * 100 / total) else 0
            val progressoFracao = if (total > 0) (quantidade.toFloat() / total.toFloat()) else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconeGenero(genero)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = genero,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$quantidade (${porcentagem}%)",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progressoFracao },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.background
                )
            }
        }
    }
}

@Composable
fun ListaPlataformasMaisUtilizadas(dados: Map<String, Int>) {
    val total = dados.values.sum()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        dados.forEach { (plataforma, quantidade) ->
            val porcentagem = if (total > 0) (quantidade * 100 / total) else 0
            val progressoFracao = if (total > 0) (quantidade.toFloat() / total.toFloat()) else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = plataforma,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = plataforma,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$quantidade (${porcentagem}%)",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progressoFracao },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.background
                )
            }
        }
    }
}

@Composable
fun IconeGenero(genero: String) {
    val icone = when (genero.lowercase()) {
        "ação", "ação e aventura" -> Icons.Default.LocalFireDepartment
        "aventura" -> Icons.Default.Explore
        "animação" -> Icons.Default.Animation
        "comédia" -> Icons.Default.Mood
        "crime" -> Icons.Default.Gavel
        "drama" -> Icons.Default.Favorite
        "fantasia", "sci-fi & fantasy" -> Icons.Default.AutoAwesome
        "ficção científica" -> Icons.Default.RocketLaunch
        "suspense" -> Icons.Default.Visibility
        "terror" -> Icons.Default.DarkMode
        "anime" -> Icons.Default.CatchingPokemon
        "novela", "novelas", "soap" -> Icons.Default.Theaters
        "dorama", "doramas" -> Icons.Default.Favorite
        else -> Icons.Default.Movie
    }

    Icon(
        imageVector = icone,
        contentDescription = genero,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
    )
}