package com.example.cinelist

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ============================================================
// DESIGN TOKENS — cores e espaçamento centralizados (Fix #1 e #5)
// ============================================================
object CineListTokens {
    // Cores semânticas (antes espalhadas como Color(0xFF...) em dezenas de lugares)
    val CorSucesso = Color(0xFF4CAF50)
    val CorErro = Color(0xFFFF4C4C)
    val CorPremium = Color(0xFFFFD700)
    val CorOnline = Color(0xFF38BDF8)
    val CorConcluidoEscuro = Color(0xFF2E7D32)
    val CorMatch = Color(0xFFFF3366)
    val CorWhatsapp = Color(0xFF25D366)

    // Escala de espaçamento (4, 8, 12, 16, 24, 32)
    val EspacoXXS = 4.dp
    val EspacoXS = 8.dp
    val EspacoS = 12.dp
    val EspacoM = 16.dp
    val EspacoL = 24.dp
    val EspacoXL = 32.dp
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TelaPerfil(
    listaDeMidias: List<Midia>,
    viewModel: MidiaViewModel,
    onVoltar: () -> Unit,
    onLogout: () -> Unit,
    onMidiaClique: ((Midia) -> Unit)? = null,
    onCalendarioClique: (() -> Unit)? = null
) {
    val firebaseAuth = FirebaseAuth.getInstance()
    val usuarioAtual = firebaseAuth.currentUser
    val contexto = LocalContext.current
    val escopoCorrotina = rememberCoroutineScope()
    val escopoTabs = rememberCoroutineScope()

    val sharedPreferences = remember {
        contexto.getSharedPreferences("ConfiguracoesPerfil", Context.MODE_PRIVATE)
    }

    val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()
    val modoGrupoAtivo = casalIdAtivo.isNotBlank()
    val isAdministrador by viewModel.isAdministradorSala.collectAsState()

    var novaSenhaSalaInput by remember { mutableStateOf("") }
    var exibindoWrapped by remember { mutableStateOf(false) }

    val membrosSala by viewModel.membrosGrupoAtivo.collectAsState(initial = emptyList())
    val amigosConectados by viewModel.amigosConectados.collectAsState(initial = emptyList())
    val solicitacoesRecebidas by viewModel.solicitacoesRecebidas.collectAsState(initial = emptyList())

    var mostrarBottomSheetConfiguracoes by remember { mutableStateOf(false) }
    var mostrarDialogoBloqueados by remember { mutableStateOf(false) }

    if (mostrarDialogoBloqueados) {
        DialogoUsuariosBloqueados(
            viewModel = viewModel,
            onDispensar = { mostrarDialogoBloqueados = false }
        )
    }

    var relogioTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            relogioTick = System.currentTimeMillis()
        }
    }

    var amigoSelecionadoParaVer by remember { mutableStateOf<AmigoPerfil?>(null) }
    val listaAmigoSelecionado by viewModel.listaAmigoSelecionado.collectAsState(initial = emptyList())

    var mostrarConfirmacaoSair by remember { mutableStateOf(false) }
    var mostrarConfirmacaoReset by remember { mutableStateOf(false) }
    var midiaParaExcluirHistorico by remember { mutableStateOf<Midia?>(null) }
    var midiaParaReabrirHistorico by remember { mutableStateOf<Midia?>(null) }

    if (mostrarConfirmacaoSair) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoSair = false },
            title = { Text("Sair da Conta", fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza de que deseja sair da sua conta?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmacaoSair = false
                        viewModel.limparEstadoSincronizacao()
                        firebaseAuth.signOut()
                        onLogout()
                    }
                ) {
                    Text("Sair", color = CineListTokens.CorErro, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoSair = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (mostrarConfirmacaoReset) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoReset = false },
            title = { Text("⚠️ Limpar Toda a Lista", fontWeight = FontWeight.Bold, color = CineListTokens.CorErro) },
            text = { Text("Tem certeza absoluta? Todos os títulos da sua lista ativa serão apagados permanentemente.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmacaoReset = false
                        viewModel.limparTodaALista()
                        Toast.makeText(contexto, "Todos os dados da lista foram apagados.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Apagar Tudo", color = CineListTokens.CorErro, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoReset = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (midiaParaExcluirHistorico != null) {
        val midia = midiaParaExcluirHistorico!!
        AlertDialog(
            onDismissRequest = { midiaParaExcluirHistorico = null },
            title = { Text("Excluir do Histórico", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente apagar \"${midia.titulo}\" permanentemente do seu histórico e da sua lista?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletar(midia)
                        midiaParaExcluirHistorico = null
                        Toast.makeText(contexto, "Removido do histórico.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineListTokens.CorErro)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { midiaParaExcluirHistorico = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (midiaParaReabrirHistorico != null) {
        val midia = midiaParaReabrirHistorico!!
        AlertDialog(
            onDismissRequest = { midiaParaReabrirHistorico = null },
            title = { Text("Reabrir Mídia", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja reabrir \"${midia.titulo}\"? O título voltará para a lista como 'Assistindo' e a data de conclusão será apagada.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.atualizar(midia.copy(status = "Assistindo", dataConclusao = 0L, concluidoPor = ""))
                        midiaParaReabrirHistorico = null
                        Toast.makeText(contexto, "Mídia reaberta com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineListTokens.CorConcluidoEscuro)
                ) {
                    Text("Reabrir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { midiaParaReabrirHistorico = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val membrosOrdenados = remember(membrosSala, usuarioAtual?.uid, relogioTick) {
        val meuUid = usuarioAtual?.uid ?: ""
        membrosSala.sortedWith(
            compareByDescending<MembroGrupo> { it.estaRealmenteOnline }
                .thenByDescending { it.uid == meuUid }
                .thenByDescending { it.vistoPorUltimo }
                .thenBy { it.nome.lowercase() }
        )
    }

    val midiasContextoAtual = remember(listaDeMidias, casalIdAtivo) {
        if (modoGrupoAtivo) {
            listaDeMidias.filter { it.isCasal && it.casalId == casalIdAtivo }
        } else {
            listaDeMidias.filter { !it.isCasal || it.casalId.isBlank() }
        }
    }

    if (exibindoWrapped) {
        TelaWrapped(
            midias = midiasContextoAtual,
            viewModel = viewModel,
            onFechar = { exibindoWrapped = false }
        )
        return
    }

    amigoSelecionadoParaVer?.let { amigo ->
        ModalBottomSheet(
            onDismissRequest = { amigoSelecionadoParaVer = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ModalPerfilAmigo(
                amigo = amigo,
                minhasMidias = midiasContextoAtual,
                midiasAmigo = listaAmigoSelecionado,
                viewModel = viewModel,
                onFechar = { amigoSelecionadoParaVer = null }
            )
        }
    }

    var membroSelecionadoParaVer by remember { mutableStateOf<MembroGrupo?>(null) }
    var filtroTipoMembro by remember { mutableStateOf("Todos") }
    var filtroStatusMembro by remember { mutableStateOf("Todos") }

    LaunchedEffect(membroSelecionadoParaVer) {
        filtroTipoMembro = "Todos"
        filtroStatusMembro = "Todos"
    }

    val midiasDoMembroInspecionado = remember(midiasContextoAtual, membroSelecionadoParaVer) {
        val membroAlvo = membroSelecionadoParaVer
        if (membroAlvo == null) emptyList()
        else {
            val nomeAlvo = membroAlvo.nome.trim().lowercase()
            midiasContextoAtual.filter {
                val autor = it.adicionadoPor.trim().lowercase()
                val autorPrefixo = autor.substringBefore("@")
                autor == nomeAlvo || autor.contains(nomeAlvo) || nomeAlvo.contains(autorPrefixo)
            }
        }
    }

    val midiasExibidasModal = remember(midiasDoMembroInspecionado, filtroTipoMembro, filtroStatusMembro) {
        midiasDoMembroInspecionado.filter { midia ->
            val bateTipo = when (filtroTipoMembro) {
                "Filmes" -> midia.tipo.equals("Filme", ignoreCase = true)
                "Séries" -> !midia.tipo.equals("Filme", ignoreCase = true)
                else -> true
            }

            val bateStatus = when (filtroStatusMembro) {
                "Assistindo" -> midia.status.equals("Assistindo", ignoreCase = true)
                "Concluídos" -> midia.status.equals("Concluído", ignoreCase = true) || midia.status.equals("Concluido", ignoreCase = true)
                "Quero Assistir" -> midia.status.equals("Quero Assistir", ignoreCase = true)
                else -> true
            }

            bateTipo && bateStatus
        }
    }

    var mostrarDialogoGerenciarSalas by remember { mutableStateOf(false) }

    if (mostrarDialogoGerenciarSalas) {
        DialogoGerenciarSalasCompartilhadas(
            viewModel = viewModel,
            onDispensar = { mostrarDialogoGerenciarSalas = false }
        )
    }

    membroSelecionadoParaVer?.let { membro ->
        ModalBottomSheet(
            onDismissRequest = { membroSelecionadoParaVer = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .padding(horizontal = CineListTokens.EspacoM, vertical = CineListTokens.EspacoS)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)
                    ) {
                        AvatarComIniciais(
                            fotoUrl = membro.fotoUrl,
                            nome = membro.nome,
                            tamanho = 52.dp,
                            corBorda = MaterialTheme.colorScheme.primary
                        )

                        Column {
                            Text(
                                text = membro.nome,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (membro.estaAssistindoAlgo) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXXS)) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = CineListTokens.CorOnline, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "Assistindo: ${membro.assistindoAgoraTitulo}",
                                        fontSize = 11.sp,
                                        color = CineListTokens.CorOnline,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = membro.obterTextoVistoPorUltimo(),
                                    fontSize = 12.sp,
                                    color = if (membro.estaRealmenteOnline) CineListTokens.CorSucesso else MaterialTheme.colorScheme.secondary,
                                    fontWeight = if (membro.estaRealmenteOnline) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    IconButton(onClick = { membroSelecionadoParaVer = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(CineListTokens.EspacoS))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(CineListTokens.EspacoXS))

                Text(
                    text = "Mídias de ${membro.nome} (${midiasExibidasModal.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(CineListTokens.EspacoXS))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tipo:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    listOf("Todos", "Filmes", "Séries").forEach { tipoOpcao ->
                        FilterChip(
                            selected = (filtroTipoMembro == tipoOpcao),
                            onClick = { filtroTipoMembro = tipoOpcao },
                            label = { Text(tipoOpcao, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    listOf("Todos", "Assistindo", "Concluídos", "Quero Assistir").forEach { statusOpcao ->
                        FilterChip(
                            selected = (filtroStatusMembro == statusOpcao),
                            onClick = { filtroStatusMembro = statusOpcao },
                            label = { Text(statusOpcao, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(CineListTokens.EspacoS))

                if (midiasExibidasModal.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (midiasDoMembroInspecionado.isEmpty()) {
                                "${membro.nome} ainda não adicionou títulos nesta sala."
                            } else {
                                "Nenhum título encontrado com os filtros selecionados."
                            },
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                        verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(midiasExibidasModal, key = { it.id }) { midiaMembro ->
                            ItemMidiaCard(
                                midia = midiaMembro,
                                onClick = {
                                    membroSelecionadoParaVer = null
                                    onMidiaClique?.invoke(midiaMembro)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    var biografia by remember { mutableStateOf(sharedPreferences.getString("bio", "") ?: "") }

    LaunchedEffect(Unit) {
        if (usuarioAtual != null) {
            viewModel.atualizarMeuPerfilPublico(
                nome = usuarioAtual.displayName ?: "Usuário CineList",
                bio = biografia
            )
        }
    }

    val titulosAbas = listOf("Visão Geral", "Amigos & Sala", "Notificações", "Estatísticas", "Histórico")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { titulosAbas.size })
    var abaSelecionada by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        abaSelecionada = pagerState.currentPage
    }

    val listaNotificacoes by viewModel.todasNotificacoes.collectAsState(initial = emptyList())
    val quantidadeNaoLidas by viewModel.quantidadeNaoLidas.collectAsState(initial = 0)

    var notificacaoParaExcluir by remember { mutableStateOf<NotificacaoEntity?>(null) }
    var notificacaoDetalhada by remember { mutableStateOf<NotificacaoEntity?>(null) }
    var mostrarConfirmacaoLimparTudoNotif by remember { mutableStateOf(false) }

    notificacaoParaExcluir?.let { notif ->
        AlertDialog(
            onDismissRequest = { notificacaoParaExcluir = null },
            title = { Text("Excluir Notificação", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja apagar esta notificação do seu histórico?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletarNotificacao(notif)
                        notificacaoParaExcluir = null
                        Toast.makeText(contexto, "Notificação excluída.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Excluir", color = CineListTokens.CorErro, fontWeight = FontWeight.Bold)
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

    if (mostrarConfirmacaoLimparTudoNotif) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoLimparTudoNotif = false },
            title = { Text("Limpar Histórico", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja apagar todas as notificações recebidas?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.limparTodasNotificacoes()
                        mostrarConfirmacaoLimparTudoNotif = false
                        Toast.makeText(contexto, "Histórico de notificações limpo.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Limpar Tudo", color = CineListTokens.CorErro, fontWeight = FontWeight.Bold)
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

    var verificandoAtualizacao by remember { mutableStateOf(false) }
    var infoNovaVersao by remember { mutableStateOf<InfoAtualizacao?>(null) }
    var mostrarDialogoAtualizacao by remember { mutableStateOf(false) }

    if (mostrarDialogoAtualizacao && infoNovaVersao != null) {
        DialogoNovidadesAtualizacao(
            info = infoNovaVersao!!,
            onDispensar = { mostrarDialogoAtualizacao = false }
        )
    }

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
            Toast.makeText(contexto, "Notificações diárias ativadas!", Toast.LENGTH_SHORT).show()
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
            if (casalIdAtivo.isNotBlank()) {
                viewModel.atualizarStatusPresenca(true)
            }
        }
    }

    val exportarLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val sucesso = BackupManager.exportarParaJson(contexto, it, midiasContextoAtual)
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

    var modoEdicaoBio by remember { mutableStateOf(false) }
    var novaBio by remember { mutableStateOf(biografia) }

    var generoFavorito by remember { mutableStateOf(sharedPreferences.getString("genero", "Não definido") ?: "Não definido") }
    var menuGeneroExpandido by remember { mutableStateOf(false) }
    val listaGeneros = listOf("Ação", "Animes", "Comédia", "Drama", "Ficção Científica", "Terror", "Suspense", "Romance", "Novelas", "Doramas")

    var metaAnualDefinida by remember { mutableIntStateOf(sharedPreferences.getInt("meta_anual_filmes", 50)) }
    var modoEdicaoMeta by remember { mutableStateOf(false) }

    var mostrarConfirmacaoExclusao by remember { mutableStateOf(false) }
    var erroExclusao by remember { mutableStateOf("") }
    var carregandoExclusao by remember { mutableStateOf(false) }

    val totalMidias = midiasContextoAtual.size
    val totalFilmes = midiasContextoAtual.count { it.tipo.equals("Filme", ignoreCase = true) }
    val totalSeriesAnimes = midiasContextoAtual.count {
        it.tipo.equals("Série", ignoreCase = true) ||
                it.tipo.equals("Anime", ignoreCase = true) ||
                it.tipo.equals("Novela", ignoreCase = true) ||
                it.tipo.equals("Dorama", ignoreCase = true)
    }

    val filmesConcluidos = midiasContextoAtual.count {
        it.tipo.equals("Filme", ignoreCase = true) &&
                (it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true))
    }
    val seriesEAnimes = midiasContextoAtual.filter { !it.tipo.equals("Filme", ignoreCase = true) }
    val seriesConcluidas = seriesEAnimes.count {
        it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true)
    }
    val totalConcluidosGeral = filmesConcluidos + seriesConcluidas

    val taxaConclusaoPercentual = if (totalMidias > 0) (totalConcluidosGeral * 100) / totalMidias else 0

    val totalEpisodiosAssistidos = seriesEAnimes.sumOf { if (it.episodioAtual > 0) it.episodioAtual - 1 else 0 }
    val minutosParadosFilmes = midiasContextoAtual.filter {
        it.tipo.equals("Filme", ignoreCase = true) &&
                !it.status.equals("Concluído", ignoreCase = true) &&
                !it.status.equals("Concluido", ignoreCase = true)
    }.sumOf { it.minutoParado }

    val minutosTotais = (filmesConcluidos * 115) + (totalEpisodiosAssistidos * 45) + minutosParadosFilmes
    val horasTotais = minutosTotais / 60
    val diasTotais = horasTotais / 24
    val horasRestantes = horasTotais % 24

    val tempoFormatado = if (diasTotais > 0) "${diasTotais}d ${horasRestantes}h" else "${horasTotais}h"

    val midiasComNota = midiasContextoAtual.filter { it.nota > 0 }
    val mediaNotas = if (midiasComNota.isNotEmpty()) {
        String.format(Locale.US, "%.1f", midiasComNota.map { it.nota }.average())
    } else {
        "0.0"
    }

    val distribuicaoNotas = remember(midiasContextoAtual) {
        (5 downTo 1).associateWith { estrela ->
            midiasContextoAtual.count { it.nota == estrela }
        }
    }

    val listaHistoricoConcluido = remember(midiasContextoAtual) {
        midiasContextoAtual.filter {
            it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true)
        }
    }

    val estatisticasGenero = remember(midiasContextoAtual) {
        midiasContextoAtual
            .filter { it.genero.isNotBlank() && it.genero != "Geral" }
            .groupingBy { it.genero }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(6)
            .toMap()
    }

    val estatisticasPlataforma = remember(midiasContextoAtual) {
        midiasContextoAtual
            .filter { it.plataforma.isNotBlank() && it.plataforma != "Não Informado" }
            .groupingBy { it.plataforma }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()
    }

    if (mostrarBottomSheetConfiguracoes) {
        ModalBottomSheet(
            onDismissRequest = { mostrarBottomSheetConfiguracoes = false },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CineListTokens.EspacoL)
                    .padding(bottom = CineListTokens.EspacoXL)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoM)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configurações do Sistema",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { mostrarBottomSheetConfiguracoes = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.secondary)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column {
                    Text(text = "Informações da Conta", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(CineListTokens.EspacoXXS))
                    Text(text = "Provedor: ${usuarioAtual?.providerData?.lastOrNull()?.providerId?.uppercase() ?: "E-MAIL"}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Text(text = "ID: ${usuarioAtual?.uid?.take(12)}...", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                }

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
                                .padding(horizontal = CineListTokens.EspacoS, vertical = 6.dp)
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Lembretes e Notificações", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Text(text = "Avisos de novos itens e lembretes", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = receberNotificacoes,
                        onCheckedChange = { valor ->
                            if (valor) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val jaTemPermissao = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    var modoEscuro by remember { mutableStateOf(sharedPreferences.getBoolean("modo_escuro", true)) }
                    Text(text = "Tema Escuro", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            mostrarBottomSheetConfiguracoes = false
                            mostrarDialogoBloqueados = true
                        }
                        .padding(vertical = CineListTokens.EspacoXXS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS)) {
                        Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = CineListTokens.CorErro, modifier = Modifier.size(18.dp))
                        Text(text = "Gerenciar Contas Bloqueadas", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !verificandoAtualizacao) {
                            verificandoAtualizacao = true
                            escopoCorrotina.launch {
                                val info = withContext(Dispatchers.IO) {
                                    UpdateManager.checarAtualizacao()
                                }
                                verificandoAtualizacao = false

                                val versaoAtual = BuildConfig.VERSION_CODE
                                if (info != null && info.versaoCode > versaoAtual) {
                                    infoNovaVersao = info
                                    mostrarDialogoAtualizacao = true
                                } else {
                                    Toast.makeText(contexto, "Você já possui a versão mais recente!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Verificar Atualização", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "Instalada: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    if (verificandoAtualizacao) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = "Atualizar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS)) {
                    Text(text = "Backup e Sincronização", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    // Fix #9 — mostra quando o backup automático (silencioso, semanal) rodou pela última vez
                    val ultimoBackupMillis = sharedPreferences.getLong("ultimo_backup_automatico", 0L)
                    val textoUltimoBackup = if (ultimoBackupMillis > 0L) {
                        "Último backup automático: ${SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()).format(Date(ultimoBackupMillis))}"
                    } else {
                        "Backup automático ainda não foi executado"
                    }
                    Text(text = textoUltimoBackup, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS)) {
                        OutlinedButton(
                            onClick = { exportarLauncher.launch("cinelist_backup_${System.currentTimeMillis()}.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Exportar JSON", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { importarLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Importar JSON", fontSize = 12.sp)
                        }
                    }
                }

                // Fix #7 — ações destrutivas com mais respiro e hierarquia visual clara
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS)) {
                    OutlinedButton(
                        onClick = { mostrarConfirmacaoReset = true; mostrarBottomSheetConfiguracoes = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CineListTokens.CorErro),
                        border = BorderStroke(1.dp, CineListTokens.CorErro.copy(alpha = 0.4f))
                    ) {
                        Text("Limpar Todos os Dados Locais", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { mostrarConfirmacaoSair = true; mostrarBottomSheetConfiguracoes = false },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CineListTokens.CorErro)
                    ) {
                        Text(text = "SAIR DA CONTA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Text(
                        text = "Excluir permanentemente minha conta",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = CineListTokens.EspacoXXS)
                            .clickable { mostrarConfirmacaoExclusao = true; mostrarBottomSheetConfiguracoes = false }
                            .padding(vertical = CineListTokens.EspacoXXS),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (modoGrupoAtivo) "Sala Compartilhada" else "Meu Perfil",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (modoGrupoAtivo) {
                                Text(
                                    text = "Código: $casalIdAtivo",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onVoltar) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { mostrarBottomSheetConfiguracoes = true }) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Configurações", tint = MaterialTheme.colorScheme.primary)
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
                                val totalAvisosNotificacao = quantidadeNaoLidas + solicitacoesRecebidas.size
                                if (index == 1 && solicitacoesRecebidas.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text(solicitacoesRecebidas.size.toString()) } }) {
                                        Text(titulo, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                } else if (index == 2 && totalAvisosNotificacao > 0) {
                                    BadgedBox(badge = { Badge { Text(totalAvisosNotificacao.toString()) } }) {
                                        Text(titulo, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                } else {
                                    Text(titulo, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Fix #8 — header mais alto e com gradiente em 3 paradas para transição suave
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                                0.6f to MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                1.0f to MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val fotoGoogleUrl = usuarioAtual?.photoUrl?.toString() ?: ""
                                val imagemParaExibir = if (fotoPerfilUriString.isNotEmpty()) fotoPerfilUriString else fotoGoogleUrl

                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                                        .clickable { seletorGaleriaLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (imagemParaExibir.isNotEmpty()) {
                                        AsyncImage(
                                            model = imagemParaExibir,
                                            contentDescription = "Foto de perfil",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(text = nomeExibicao.take(1).uppercase(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(CineListTokens.EspacoXS))

                                if (modoEdicaoNome) {
                                    Row(modifier = Modifier.fillMaxWidth(0.8f), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = novoNome,
                                            onValueChange = { novoNome = it },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        IconButton(onClick = {
                                            if (novoNome.isNotBlank()) {
                                                val atualizacao = userProfileChangeRequest { displayName = novoNome.trim() }
                                                usuarioAtual?.updateProfile(atualizacao)?.addOnCompleteListener { t ->
                                                    if (t.isSuccessful) { nomeExibicao = novoNome.trim(); modoEdicaoNome = false }
                                                }
                                            }
                                        }) { Icon(imageVector = Icons.Default.Check, contentDescription = "Salvar", tint = CineListTokens.CorSucesso) }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Text(text = nomeExibicao, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(CineListTokens.EspacoXXS))
                                        IconButton(onClick = { modoEdicaoNome = true; novoNome = nomeExibicao }, modifier = Modifier.size(24.dp)) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (modoGrupoAtivo) {
                                        if (isAdministrador) CineListTokens.CorPremium.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    modifier = Modifier.padding(top = CineListTokens.EspacoXXS, bottom = CineListTokens.EspacoXS)
                                ) {
                                    Text(
                                        text = if (modoGrupoAtivo) {
                                            if (isAdministrador) "👑 Administrador da Sala" else "👤 Membro da Sala"
                                        } else "🎬 Lista Pessoal",
                                        color = if (modoGrupoAtivo && isAdministrador) CineListTokens.CorPremium else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = CineListTokens.EspacoS, vertical = CineListTokens.EspacoXXS)
                                    )
                                }

                                if (!modoGrupoAtivo) {
                                    if (modoEdicaoBio) {
                                        Row(modifier = Modifier.fillMaxWidth(0.8f), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(value = novaBio, onValueChange = { novaBio = it }, modifier = Modifier.weight(1f), singleLine = true)
                                            IconButton(onClick = {
                                                biografia = novaBio.trim()
                                                sharedPreferences.edit().putString("bio", biografia).apply()
                                                modoEdicaoBio = false
                                                viewModel.atualizarMeuPerfilPublico(nomeExibicao, biografia)
                                            }) { Icon(imageVector = Icons.Default.Check, contentDescription = "Salvar Bio", tint = CineListTokens.CorSucesso) }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = CineListTokens.EspacoXL)) {
                                            Text(text = if (biografia.isEmpty()) "Adicione uma biografia..." else "\"$biografia\"", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
                                            Spacer(modifier = Modifier.width(CineListTokens.EspacoXXS))
                                            IconButton(onClick = { modoEdicaoBio = true; novaBio = biografia }, modifier = Modifier.size(20.dp)) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar Bio", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(horizontal = CineListTokens.EspacoM), verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)) {

                            Spacer(modifier = Modifier.height(CineListTokens.EspacoXXS))

                            SecaoConquistasPerfil(listaDeMidias = midiasContextoAtual)

                            PremiumBannerCard(
                                titulo = "Retrospectiva CineList",
                                subtitulo = "Sua jornada cinéfila em destaque",
                                icone = Icons.Default.WorkspacePremium,
                                gradient = Brush.horizontalGradient(listOf(Color(0xFF1DB954), Color(0xFF0F7A33))),
                                onClick = { exibindoWrapped = true }
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)) {
                                PremiumBannerCardSmall(
                                    titulo = "Lançamentos",
                                    icone = Icons.Default.CalendarMonth,
                                    gradient = Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCalendarioClique?.invoke() }
                                )

                                PremiumBannerCardSmall(
                                    titulo = "Gerir Salas",
                                    icone = Icons.Default.Group,
                                    gradient = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFB45309))),
                                    modifier = Modifier.weight(1f),
                                    onClick = { mostrarDialogoGerenciarSalas = true }
                                )
                            }

                            if (modoGrupoAtivo && isAdministrador) {
                                Spacer(modifier = Modifier.height(CineListTokens.EspacoXXS))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, CineListTokens.CorPremium.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(CineListTokens.EspacoM),
                                        verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)
                                    ) {
                                        Text(text = "👑 Controle de Administrador", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CineListTokens.CorPremium)

                                        OutlinedTextField(
                                            value = novaSenhaSalaInput,
                                            onValueChange = { novaSenhaSalaInput = it },
                                            label = { Text("Atualizar Senha da Sala", color = MaterialTheme.colorScheme.secondary) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CineListTokens.CorPremium)
                                        )
                                        Button(
                                            onClick = {
                                                if (novaSenhaSalaInput.isNotBlank()) {
                                                    viewModel.atualizarSenhaDaSala(casalIdAtivo, novaSenhaSalaInput) { sucesso ->
                                                        if (sucesso) {
                                                            Toast.makeText(contexto, "Senha atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                                                            novaSenhaSalaInput = ""
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = CineListTokens.CorPremium)
                                        ) {
                                            Text("Salvar Nova Senha", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Membros Conectados (${membrosOrdenados.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                                        membrosOrdenados.forEach { membro ->
                                            val ehMim = membro.uid == (usuarioAtual?.uid ?: "")
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = membro.nome + if (ehMim) " (Você)" else "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = if (ehMim) FontWeight.Bold else FontWeight.Normal)
                                                if (!ehMim) {
                                                    TextButton(onClick = {
                                                        viewModel.excluirMembroDaSala(casalIdAtivo, membro.uid) { sucesso ->
                                                            if (sucesso) Toast.makeText(contexto, "${membro.nome} foi removido.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }) { Text("Remover", color = CineListTokens.CorErro, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(CineListTokens.EspacoM))
                        }
                    }
                }

                1 -> {
                    if (modoGrupoAtivo) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(CineListTokens.EspacoM)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)
                        ) {
                            Text(text = "Cinéfilos na Sala (${membrosOrdenados.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Toque no avatar para inspecionar os títulos adicionados pelo membro.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                            Spacer(modifier = Modifier.height(CineListTokens.EspacoXXS))

                            membrosOrdenados.forEach { membro ->
                                val souEu = membro.uid == (usuarioAtual?.uid ?: "")
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(CineListTokens.EspacoS)) {
                                        ItemMembroPresenca(
                                            membro = membro,
                                            souEu = souEu,
                                            onMembroClique = { membroSelecionadoParaVer = membro }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        AbaAmigosPerfil(
                            viewModel = viewModel,
                            paddingValues = paddingValues,
                            onAmigoClique = { amigo ->
                                viewModel.carregarListaDoAmigo(amigo.uid)
                                amigoSelecionadoParaVer = amigo
                            },
                            onGerenciarBloqueados = { mostrarDialogoBloqueados = true }
                        )
                    }
                }

                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(CineListTokens.EspacoM)
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

                            Row(horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXXS)) {
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

                        Spacer(modifier = Modifier.height(CineListTokens.EspacoS))

                        if (solicitacoesRecebidas.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        escopoTabs.launch { pagerState.animateScrollToPage(1) }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Novas Solicitações de Amizade (${solicitacoesRecebidas.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Você tem pedidos de conexão pendentes. Toque para responder.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(CineListTokens.EspacoS))
                        }

                        if (listaNotificacoes.isEmpty() && solicitacoesRecebidas.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(CineListTokens.EspacoXS))
                                    Text(
                                        text = "Nenhuma notificação registrada ainda.",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS)) {
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

                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = CineListTokens.EspacoM, vertical = CineListTokens.EspacoS)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoM)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(CineListTokens.EspacoM)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = if (modoGrupoAtivo) "Progresso da Sala" else "🎯 Meta de Filmes Anual", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = "$totalConcluidosGeral de ${if (modoGrupoAtivo) totalMidias else metaAnualDefinida} concluídos", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    if (!modoGrupoAtivo) {
                                        IconButton(onClick = { modoEdicaoMeta = !modoEdicaoMeta }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Ajustar Meta", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                if (!modoGrupoAtivo && modoEdicaoMeta) {
                                    Spacer(modifier = Modifier.height(CineListTokens.EspacoXS))
                                    Row(horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS), verticalAlignment = Alignment.CenterVertically) {
                                        listOf(25, 50, 75, 100).forEach { metaOpcao ->
                                            FilterChip(
                                                selected = (metaAnualDefinida == metaOpcao),
                                                onClick = {
                                                    metaAnualDefinida = metaOpcao
                                                    sharedPreferences.edit().putInt("meta_anual_filmes", metaOpcao).apply()
                                                    modoEdicaoMeta = false
                                                },
                                                label = { Text("$metaOpcao", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(CineListTokens.EspacoS))
                                val progressoMeta = if (modoGrupoAtivo) {
                                    if (totalMidias > 0) totalConcluidosGeral.toFloat() / totalMidias.toFloat() else 0f
                                } else {
                                    if (metaAnualDefinida > 0) (totalConcluidosGeral.toFloat() / metaAnualDefinida.toFloat()).coerceIn(0f, 1f) else 0f
                                }

                                LinearProgressIndicator(
                                    progress = { progressoMeta },
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                    color = if (modoGrupoAtivo) MaterialTheme.colorScheme.primary else CineListTokens.CorPremium,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)) {
                            CardEstatistica(titulo = "Total Assistido", valor = tempoFormatado, subtitulo = "Horas na tela", modifier = Modifier.weight(1f))
                            CardEstatistica(titulo = "Média Notas", valor = "$mediaNotas ★", subtitulo = "${midiasComNota.size} avaliados", modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)) {
                            CardEstatistica(titulo = "Filmes", valor = filmesConcluidos.toString(), subtitulo = "De $totalFilmes na lista", modifier = Modifier.weight(1f))
                            CardEstatistica(titulo = "Episódios", valor = totalEpisodiosAssistidos.toString(), subtitulo = "De $seriesConcluidas séries", modifier = Modifier.weight(1f))
                        }

                        if (midiasComNota.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(CineListTokens.EspacoM)) {
                                    Text(text = "Distribuição de Avaliações", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(CineListTokens.EspacoS))
                                    GraficoDistribuicaoNotas(dados = distribuicaoNotas, totalAvaliados = midiasComNota.size)
                                }
                            }
                        }

                        if (estatisticasGenero.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(CineListTokens.EspacoM)) {
                                    Text(text = "Gêneros Predominantes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(CineListTokens.EspacoS))
                                    ListaGenerosMaisAssistidos(dados = estatisticasGenero)
                                }
                            }
                        }

                        if (estatisticasPlataforma.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(CineListTokens.EspacoM)) {
                                    Text(text = "Plataformas Mais Usadas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(CineListTokens.EspacoS))
                                    ListaPlataformasMaisUtilizadas(dados = estatisticasPlataforma)
                                }
                            }
                        }
                    }
                }

                4 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = CineListTokens.EspacoM, vertical = CineListTokens.EspacoS)
                    ) {
                        Text(
                            text = if (modoGrupoAtivo) "Concluídos na Sala (${listaHistoricoConcluido.size})" else "Seus Títulos Concluídos (${listaHistoricoConcluido.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = CineListTokens.EspacoS)
                        )

                        if (listaHistoricoConcluido.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (modoGrupoAtivo) "Nenhum item marcado como Concluído nesta sala." else "Nenhum item marcado como Concluído na sua lista particular.",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(listaHistoricoConcluido.sortedByDescending { it.dataConclusao }, key = { it.id }) { itemConcluido ->
                                    val dataFormatada = remember(itemConcluido.dataConclusao) {
                                        if (itemConcluido.dataConclusao > 0L) {
                                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(itemConcluido.dataConclusao))
                                        } else ""
                                    }

                                    val currentItem by rememberUpdatedState(itemConcluido)
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { valorDismiss ->
                                            when (valorDismiss) {
                                                SwipeToDismissBoxValue.EndToStart -> {
                                                    midiaParaExcluirHistorico = currentItem
                                                    false
                                                }
                                                SwipeToDismissBoxValue.StartToEnd -> {
                                                    midiaParaReabrirHistorico = currentItem
                                                    false
                                                }
                                                SwipeToDismissBoxValue.Settled -> false
                                            }
                                        }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = true,
                                        enableDismissFromEndToStart = true,
                                        backgroundContent = {
                                            val direcao = dismissState.dismissDirection
                                            val corFundo by animateColorAsState(
                                                targetValue = when (direcao) {
                                                    SwipeToDismissBoxValue.StartToEnd -> CineListTokens.CorConcluidoEscuro
                                                    SwipeToDismissBoxValue.EndToStart -> CineListTokens.CorErro
                                                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                                                },
                                                label = "cor_swipe_historico"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(corFundo)
                                                    .padding(horizontal = CineListTokens.EspacoM),
                                                contentAlignment = when (direcao) {
                                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                                    else -> Alignment.CenterEnd
                                                }
                                            ) {
                                                if (direcao == SwipeToDismissBoxValue.StartToEnd) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXXS)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Reabrir", tint = Color.White)
                                                        Text("Reabrir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                } else if (direcao == SwipeToDismissBoxValue.EndToStart) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXXS)
                                                    ) {
                                                        Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onMidiaClique?.invoke(itemConcluido) },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(CineListTokens.EspacoS),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(45.dp)
                                                        .height(65.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.background)
                                                ) {
                                                    if (itemConcluido.imagemCapa.isNotBlank()) {
                                                        AsyncImage(
                                                            model = itemConcluido.imagemCapa,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = itemConcluido.titulo,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(text = itemConcluido.tipo, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

                                                        if (itemConcluido.nota > 0) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = CineListTokens.CorPremium, modifier = Modifier.size(10.dp))
                                                                Text(text = " ${itemConcluido.nota}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineListTokens.CorPremium)
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(CineListTokens.EspacoXXS))

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CineListTokens.CorSucesso, modifier = Modifier.size(12.dp))

                                                        val textoConclusao = buildString {
                                                            if (dataFormatada.isNotBlank()) append("Concluído em $dataFormatada") else append("Concluído")
                                                            if (modoGrupoAtivo && itemConcluido.concluidoPor.isNotBlank()) append(" por ${itemConcluido.concluidoPor}")
                                                        }

                                                        Text(
                                                            text = textoConclusao,
                                                            fontSize = 10.sp,
                                                            color = CineListTokens.CorSucesso,
                                                            fontWeight = FontWeight.Medium
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
                }
            }
        }
    }
}


@Composable
fun PremiumBannerCard(
    titulo: String,
    subtitulo: String,
    icone: ImageVector,
    gradient: Brush,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(gradient)) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.align(Alignment.CenterEnd).size(100.dp).offset(x = 20.dp, y = 10.dp)
            )
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = CineListTokens.EspacoL),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = subtitulo, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun PremiumBannerCardSmall(
    titulo: String,
    icone: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(70.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(gradient)) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.align(Alignment.BottomEnd).size(60.dp).offset(x = 10.dp, y = 10.dp)
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(CineListTokens.EspacoS),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ============================================================
// Fix #2 — card de estatística ÚNICO reutilizado em toda a tela
// (substitui WidgetEstatistica, ItemEstatistica e CardEstatisticaDetalhada)
// ============================================================
@Composable
fun CardEstatistica(
    titulo: String,
    valor: String,
    subtitulo: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(CineListTokens.EspacoM),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = titulo, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(CineListTokens.EspacoXXS))
            Text(text = valor, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            if (subtitulo != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitulo, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ============================================================
// Fix #4 — avatar (foto ou iniciais) ÚNICO reutilizado em toda a tela
// ============================================================
@Composable
fun AvatarComIniciais(
    fotoUrl: String,
    nome: String,
    tamanho: androidx.compose.ui.unit.Dp = 48.dp,
    corBorda: Color? = null,
    indicadorOnline: Boolean? = null
) {
    Box(modifier = Modifier.size(tamanho), contentAlignment = Alignment.BottomEnd) {
        val baseModifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .let { m -> if (corBorda != null) m.border(2.dp, corBorda, CircleShape) else m }

        if (fotoUrl.isNotBlank()) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = nome,
                modifier = baseModifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = baseModifier.background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nome.take(1).uppercase(),
                    fontSize = (tamanho.value * 0.4f).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (indicadorOnline != null) {
            Box(
                modifier = Modifier
                    .size((tamanho.value * 0.26f).dp)
                    .clip(CircleShape)
                    .background(if (indicadorOnline) CineListTokens.CorSucesso else Color.Gray)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun AbaAmigosPerfil(
    viewModel: MidiaViewModel,
    paddingValues: PaddingValues,
    onAmigoClique: (AmigoPerfil) -> Unit,
    onGerenciarBloqueados: () -> Unit = {}
) {
    val contexto = LocalContext.current
    val amigos by viewModel.amigosConectados.collectAsState(initial = emptyList())
    val solicitacoesRecebidas by viewModel.solicitacoesRecebidas.collectAsState(initial = emptyList())
    val solicitacoesPendentesEnviadas by viewModel.solicitacoesPendentesEnviadas.collectAsState(initial = emptyList())
    val usuariosBloqueados by viewModel.usuariosBloqueados.collectAsState(initial = emptyList())
    val resultadosBusca by viewModel.resultadosBuscaAmigos.collectAsState()
    val meuUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var tickAmigos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            tickAmigos = System.currentTimeMillis()
        }
    }

    var termoBusca by remember { mutableStateOf("") }
    var buscando by remember { mutableStateOf(false) }
    var jaBuscouAoMenosUmaVez by remember { mutableStateOf(false) }

    val uidsAmigos = remember(amigos) { amigos.map { it.uid }.toSet() }

    LaunchedEffect(termoBusca) {
        if (termoBusca.trim().length >= 2) {
            buscando = true
            delay(400)
            viewModel.pesquisarUsuarios(termoBusca.trim())
            jaBuscouAoMenosUmaVez = true
            buscando = false
        } else {
            viewModel.pesquisarUsuarios("")
            jaBuscouAoMenosUmaVez = false
            buscando = false
        }
    }

    val resultadosFiltrados = remember(resultadosBusca, uidsAmigos, meuUid) {
        resultadosBusca.filter { it.uid !in uidsAmigos && it.uid != meuUid }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(CineListTokens.EspacoM)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(CineListTokens.EspacoM)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔍 Encontrar Cinéfilos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (buscando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(CineListTokens.EspacoS))
                    OutlinedTextField(
                        value = termoBusca,
                        onValueChange = { termoBusca = it },
                        placeholder = { Text("Digite o nome do usuário...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (termoBusca.isNotEmpty()) {
                                IconButton(onClick = { termoBusca = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Limpar busca")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(CineListTokens.EspacoM))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)
            ) {
                if (solicitacoesRecebidas.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pedidos de Conexão (${solicitacoesRecebidas.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(solicitacoesRecebidas, key = { "solicitacao_${it.remetenteUid}" }) { sol ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(CineListTokens.EspacoS),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AvatarComIniciais(fotoUrl = sol.fotoUrl, nome = sol.nome, tamanho = 44.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sol.nome, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Quer adicionar você", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.aceitarSolicitacaoAmizade(sol.remetenteUid) { sucesso ->
                                                if (sucesso) {
                                                    Toast.makeText(contexto, "${sol.nome} agora é seu amigo!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CineListTokens.CorSucesso),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Aceitar", fontSize = 12.sp, color = Color.White)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.recusarSolicitacaoAmizade(sol.remetenteUid) { sucesso ->
                                                if (sucesso) {
                                                    Toast.makeText(contexto, "Pedido recusado.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Recusar", fontSize = 12.sp, color = CineListTokens.CorErro)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(CineListTokens.EspacoXXS))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(CineListTokens.EspacoXXS))
                    }
                }

                if (termoBusca.trim().length >= 2) {
                    item {
                        Text(
                            text = "Resultados da busca:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (!buscando && jaBuscouAoMenosUmaVez && resultadosFiltrados.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhum usuário encontrado com esse nome.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = CineListTokens.EspacoXS)
                            )
                        }
                    }

                    items(resultadosFiltrados, key = { "busca_${it.uid}" }) { usuario ->
                        val jaPendente = solicitacoesPendentesEnviadas.contains(usuario.uid)
                        var processandoAcao by remember(usuario.uid) { mutableStateOf(false) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(CineListTokens.EspacoS),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AvatarComIniciais(fotoUrl = usuario.fotoUrl, nome = usuario.nome, tamanho = 44.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(usuario.nome, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(if (usuario.bio.isNotBlank()) usuario.bio else usuario.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                if (jaPendente) {
                                    OutlinedButton(
                                        onClick = {
                                            if (!processandoAcao) {
                                                processandoAcao = true
                                                viewModel.cancelarSolicitacaoAmigo(usuario.uid) { sucesso ->
                                                    processandoAcao = false
                                                    if (sucesso) {
                                                        Toast.makeText(contexto, "Solicitação cancelada com sucesso!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(contexto, "Erro ao cancelar solicitação.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !processandoAcao,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CineListTokens.CorErro),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        if (processandoAcao) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CineListTokens.CorErro)
                                        } else {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(CineListTokens.EspacoXXS))
                                            Text("Cancelar", fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (!processandoAcao) {
                                                processandoAcao = true
                                                viewModel.enviarSolicitacaoAmigo(usuario.uid) { sucesso, erroMsg ->
                                                    processandoAcao = false
                                                    if (sucesso) {
                                                        Toast.makeText(contexto, "Solicitação enviada para ${usuario.nome}!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(contexto, erroMsg ?: "Erro ao enviar solicitação.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !processandoAcao,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        if (processandoAcao) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                        } else {
                                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(CineListTokens.EspacoXXS))
                                            Text("Adicionar", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(CineListTokens.EspacoXXS))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(CineListTokens.EspacoXXS))
                    }
                }

                item {
                    Text(
                        text = "Meus Amigos (${amigos.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (amigos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(CineListTokens.EspacoXS))
                                Text("Nenhum amigo adicionado ainda.\nUse a barra de pesquisa acima para encontrar cinéfilos!", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(amigos, key = { it.uid }) { amigo ->
                        val estaOnline = amigo.estaRealmenteOnline
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAmigoClique(amigo) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)) {
                                    AvatarComIniciais(fotoUrl = amigo.fotoUrl, nome = amigo.nome, tamanho = 48.dp, indicadorOnline = estaOnline)

                                    Column {
                                        Text(amigo.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(
                                            text = amigo.obterTextoVistoPorUltimo(),
                                            fontSize = 13.sp,
                                            color = if (estaOnline) CineListTokens.CorSucesso else MaterialTheme.colorScheme.secondary,
                                            fontWeight = if (estaOnline) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModalPerfilAmigo(
    amigo: AmigoPerfil,
    minhasMidias: List<Midia>,
    midiasAmigo: List<Midia>,
    viewModel: MidiaViewModel,
    onFechar: () -> Unit
) {
    val contexto = LocalContext.current
    val mensagens by remember(amigo.uid) {
        viewModel.observarMensagensAmigo(amigo.uid)
    }.collectAsState(initial = emptyList())

    var textoMensagem by remember { mutableStateOf("") }
    var abaAmigoSelecionada by remember { mutableIntStateOf(0) }
    var mostrarConfirmacaoRemover by remember { mutableStateOf(false) }
    var mostrarConfirmacaoBloquear by remember { mutableStateOf(false) }
    var processandoAcao by remember { mutableStateOf(false) }

    val estaOnline = amigo.estaRealmenteOnline

    val midiasEmComum = remember(minhasMidias, midiasAmigo) {
        try {
            viewModel.calcularMidiasEmComum(minhasMidias, midiasAmigo)
        } catch (e: Exception) {
            emptyList()
        }
    }

    if (mostrarConfirmacaoRemover) {
        AlertDialog(
            onDismissRequest = { if (!processandoAcao) mostrarConfirmacaoRemover = false },
            title = { Text("Desfazer Amizade", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente remover ${amigo.nome} da sua lista de amigos?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        processandoAcao = true
                        viewModel.removerAmigo(amigo.uid) { sucesso ->
                            processandoAcao = false
                            mostrarConfirmacaoRemover = false
                            if (sucesso) {
                                Toast.makeText(contexto, "Amizade desfeita.", Toast.LENGTH_SHORT).show()
                                onFechar()
                            } else {
                                Toast.makeText(contexto, "Erro ao remover amigo.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !processandoAcao
                ) {
                    if (processandoAcao) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CineListTokens.CorErro)
                    } else {
                        Text("Remover", color = CineListTokens.CorErro, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarConfirmacaoRemover = false },
                    enabled = !processandoAcao
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (mostrarConfirmacaoBloquear) {
        AlertDialog(
            onDismissRequest = { if (!processandoAcao) mostrarConfirmacaoBloquear = false },
            title = { Text("Bloquear Usuário", fontWeight = FontWeight.Bold, color = CineListTokens.CorErro) },
            text = {
                Text("Deseja bloquear ${amigo.nome}? A amizade e as mensagens serão desfeitas e este usuário não poderá mais encontrar o seu perfil nem enviar novas solicitações.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        processandoAcao = true
                        viewModel.bloquearUsuario(amigo.uid) { sucesso ->
                            processandoAcao = false
                            mostrarConfirmacaoBloquear = false
                            if (sucesso) {
                                Toast.makeText(contexto, "${amigo.nome} foi bloqueado.", Toast.LENGTH_SHORT).show()
                                onFechar()
                            } else {
                                Toast.makeText(contexto, "Erro ao bloquear usuário.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !processandoAcao
                ) {
                    if (processandoAcao) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CineListTokens.CorErro)
                    } else {
                        Text("Bloquear", color = CineListTokens.CorErro, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarConfirmacaoBloquear = false },
                    enabled = !processandoAcao
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(horizontal = CineListTokens.EspacoM, vertical = CineListTokens.EspacoS)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS),
                modifier = Modifier.weight(1f)
            ) {
                AvatarComIniciais(fotoUrl = amigo.fotoUrl, nome = amigo.nome, tamanho = 52.dp, indicadorOnline = estaOnline)
                Column {
                    Text(amigo.nome, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = amigo.obterTextoVistoPorUltimo(),
                        fontSize = 12.sp,
                        color = if (estaOnline) CineListTokens.CorSucesso else MaterialTheme.colorScheme.secondary,
                        fontWeight = if (estaOnline) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { mostrarConfirmacaoBloquear = true }) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Bloquear usuário",
                        tint = CineListTokens.CorErro
                    )
                }

                IconButton(onClick = { mostrarConfirmacaoRemover = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Desfazer amizade",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                    )
                }

                IconButton(onClick = onFechar) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        }

        Spacer(modifier = Modifier.height(CineListTokens.EspacoS))

        ScrollableTabRow(selectedTabIndex = abaAmigoSelecionada, edgePadding = 0.dp) {
            Tab(
                selected = abaAmigoSelecionada == 0,
                onClick = { abaAmigoSelecionada = 0 },
                text = { Text("Favoritos (${midiasAmigo.size})") }
            )
            Tab(
                selected = abaAmigoSelecionada == 1,
                onClick = { abaAmigoSelecionada = 1 },
                text = { Text("🤝 Em Comum (${midiasEmComum.size})") }
            )
            Tab(
                selected = abaAmigoSelecionada == 2,
                onClick = { abaAmigoSelecionada = 2 },
                text = { Text("💬 Chat Rápido") }
            )
        }

        Spacer(modifier = Modifier.height(CineListTokens.EspacoS))

        when (abaAmigoSelecionada) {
            0 -> {
                if (midiasAmigo.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Este amigo ainda não sincronizou mídias públicas.", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                        verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(midiasAmigo, key = { it.id }) { midia ->
                            ItemMidiaCard(midia = midia, onClick = {})
                        }
                    }
                }
            }
            1 -> {
                if (midiasEmComum.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Vocês ainda não têm títulos em comum na lista.", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                        verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(midiasEmComum, key = { it.id }) { midiaComum ->
                            ItemMidiaCard(midia = midiaComum, onClick = {})
                        }
                    }
                }
            }
            2 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS)
                    ) {
                        items(mensagens, key = { it.id }) { msg ->
                            val souEu = msg.remetenteUid == FirebaseAuth.getInstance().currentUser?.uid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (souEu) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (souEu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Text(
                                        text = msg.texto,
                                        color = if (souEu) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = CineListTokens.EspacoXS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textoMensagem,
                            onValueChange = { textoMensagem = it },
                            placeholder = { Text("Enviar dica ou recomendação...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(CineListTokens.EspacoXS))
                        IconButton(
                            onClick = {
                                if (textoMensagem.isNotBlank()) {
                                    viewModel.enviarMensagemAmigo(amigo.uid, textoMensagem)
                                    textoMensagem = ""
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Enviar", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelaWrapped(
    midias: List<Midia>,
    viewModel: MidiaViewModel,
    onFechar: () -> Unit
) {
    val dadosWrapped = remember(midias) { viewModel.calcularDadosWrapped(midias) }
    val pagerState = rememberPagerState(pageCount = { 5 })
    val contexto = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "animSetinha")
    val offsetAnimado by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetSetinha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0B))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pagina ->
            when (pagina) {
                0 -> PaginaCapaWrapped(dadosWrapped.frasePersonalizada)
                1 -> PaginaTempoWrapped(dadosWrapped.horasTotaisAssistidas, dadosWrapped.totalTitulosConcluidos)
                2 -> PaginaEpisodiosWrapped(dadosWrapped.totalEpisodiosMaratonados)
                3 -> PaginaGenerosWrapped(dadosWrapped.generoFavorito, dadosWrapped.plataformaMaisUtilizada)
                4 -> PaginaFinalWrapped(
                    dadosWrapped = dadosWrapped,
                    onCompartilhar = {
                        // AQUI ESTÁ A MÁGICA CHAMANDO O INSTAGRAM!
                        InstagramShareHelper.compartilharWrappedNoInstagram(contexto, dadosWrapped)
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = CineListTokens.EspacoXL)
                .graphicsLayer { translationX = offsetAnimado },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Deslize para o lado",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Deslizar",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onFechar,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(CineListTokens.EspacoM)
                .background(Color(0x55000000), CircleShape)
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
        }
    }
}
@Composable
fun PaginaCapaWrapped(frase: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1DB954), Color(0xFF191414))))
            .padding(CineListTokens.EspacoXL),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoM)) {
            Text("🍿 CineList Wrapped", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = frase, color = Color.White, fontSize = 22.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PaginaTempoWrapped(horas: Int, total: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF8A2BE2), Color(0xFF121212))))
            .padding(CineListTokens.EspacoXL),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoM)) {
            Text("Neste ciclo, você dedicou", color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
            Text(text = "$horas horas", fontSize = 56.sp, fontWeight = FontWeight.Black, color = CineListTokens.CorPremium)
            Text(text = "assistindo a $total títulos entre filmes e séries.", fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PaginaEpisodiosWrapped(totalEpisodios: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFF5722), Color(0xFF121212))))
            .padding(CineListTokens.EspacoXL),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoM)) {
            Text("Viciante em séries!", color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp)
            Text(text = "$totalEpisodios", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = "episódios maratonados sem parar.", fontSize = 20.sp, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PaginaGenerosWrapped(genero: String, plataforma: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE91E63), Color(0xFF121212))))
            .padding(CineListTokens.EspacoXL),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoM)) {
            Text("Seu estilo principal", color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
            Text(text = genero, fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Sua plataforma favorita:", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
            Text(text = plataforma, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = CineListTokens.CorPremium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PaginaFinalWrapped(dadosWrapped: CineWrappedData, onCompartilhar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF00BFFF), Color(0xFF090A0B))))
            .padding(CineListTokens.EspacoXL),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoM)) {
            Text("Destaque da Temporada 🌟", color = CineListTokens.CorPremium, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = dadosWrapped.filmeOuSerieDestaque, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text(text = "Média de Avaliação: ${dadosWrapped.mediaNotasAtribuidas} ★", fontSize = 15.sp, color = Color.LightGray)
            Text(text = "Sua Maior Nota: ${"★".repeat(dadosWrapped.maiorNotaDada.coerceAtLeast(0))}", fontSize = 15.sp, color = CineListTokens.CorPremium)

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCompartilhar,
                colors = ButtonDefaults.buttonColors(containerColor = CineListTokens.CorPremium),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(CineListTokens.EspacoXS))
                Text("Compartilhar Resumo", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ItemMembroPresenca(
    membro: MembroGrupo,
    souEu: Boolean = false,
    onMembroClique: (() -> Unit)? = null
) {
    val estaOnline = membro.estaRealmenteOnline
    val statusTexto = if (membro.estaAssistindoAlgo) {
        "Assistindo: ${membro.assistindoAgoraTitulo}"
    } else {
        membro.obterTextoVistoPorUltimo()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onMembroClique?.invoke() }
            .padding(vertical = CineListTokens.EspacoXXS, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            AvatarComIniciais(fotoUrl = membro.fotoUrl, nome = membro.nome, tamanho = 36.dp, indicadorOnline = estaOnline)

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = membro.nome,
                        fontSize = 14.sp,
                        fontWeight = if (souEu) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (souEu) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Você",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (membro.estaAssistindoAlgo) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CineListTokens.CorOnline,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${membro.assistindoAgoraTitulo} ${membro.assistindoAgoraEpisodio}".trim(),
                            fontSize = 11.sp,
                            color = CineListTokens.CorOnline,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(CineListTokens.EspacoXS))

        if (!membro.estaAssistindoAlgo) {
            Text(
                text = statusTexto,
                fontSize = 11.sp,
                color = if (estaOnline) CineListTokens.CorSucesso else MaterialTheme.colorScheme.secondary,
                fontWeight = if (estaOnline) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

fun configurarLembretes(context: Context, ativar: Boolean) {
    val workManager = WorkManager.getInstance(context)
    if (ativar) {
        val requisicao = PeriodicWorkRequestBuilder<LembreteWorker>(24, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            "LembretesDiariosCineList",
            ExistingPeriodicWorkPolicy.KEEP,
            requisicao
        )
    } else {
        workManager.cancelUniqueWork("LembretesDiariosCineList")
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notificacao.lida) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (notificacao.tipo == "ATUALIZACAO") Icons.Default.SystemUpdate else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = notificacao.titulo,
                        fontWeight = if (notificacao.lida) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDeletar, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = notificacao.mensagem,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(CineListTokens.EspacoXS))
            Text(
                text = dataFormatada,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun GraficoDistribuicaoNotas(dados: Map<Int, Int>, totalAvaliados: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        dados.forEach { (nota, quantidade) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "$nota ★", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(30.dp))
                val porcentagem = if (totalAvaliados > 0) quantidade.toFloat() / totalAvaliados else 0f
                LinearProgressIndicator(
                    progress = { porcentagem },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(text = quantidade.toString(), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(24.dp))
            }
        }
    }
}

@Composable
fun ListaGenerosMaisAssistidos(dados: Map<String, Int>) {
    val total = dados.values.sum()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        dados.forEach { (genero, quantidade) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = genero, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(90.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val porcentagem = if (total > 0) quantidade.toFloat() / total else 0f
                LinearProgressIndicator(
                    progress = { porcentagem },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(text = quantidade.toString(), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(24.dp))
            }
        }
    }
}

@Composable
fun ListaPlataformasMaisUtilizadas(dados: Map<String, Int>) {
    val total = dados.values.sum()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        dados.forEach { (plataforma, quantidade) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = plataforma, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(90.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val porcentagem = if (total > 0) quantidade.toFloat() / total else 0f
                LinearProgressIndicator(
                    progress = { porcentagem },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(text = quantidade.toString(), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(24.dp))
            }
        }
    }
}

@Composable
fun DialogoUsuariosBloqueados(
    viewModel: MidiaViewModel,
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    val bloqueados by viewModel.usuariosBloqueados.collectAsState(initial = emptyList())
    var usuarioParaConfirmarDesbloqueio by remember { mutableStateOf<AmigoPerfil?>(null) }
    var uidDesbloqueando by remember { mutableStateOf<String?>(null) }

    if (usuarioParaConfirmarDesbloqueio != null) {
        val alvo = usuarioParaConfirmarDesbloqueio!!
        AlertDialog(
            onDismissRequest = {
                if (uidDesbloqueando == null) usuarioParaConfirmarDesbloqueio = null
            },
            title = {
                Text(
                    text = "Desbloquear Usuário",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CineListTokens.EspacoXXS),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)
                ) {
                    AvatarComIniciais(fotoUrl = alvo.fotoUrl, nome = alvo.nome, tamanho = 46.dp, corBorda = MaterialTheme.colorScheme.primary)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Deseja desbloquear ${alvo.nome}?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = alvo.email.ifBlank { "Sem e-mail" },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        uidDesbloqueando = alvo.uid
                        viewModel.desbloquearUsuario(alvo.uid) { sucesso ->
                            uidDesbloqueando = null
                            usuarioParaConfirmarDesbloqueio = null
                            if (sucesso) {
                                Toast.makeText(contexto, "${alvo.nome} foi desbloqueado.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(contexto, "Erro ao desbloquear.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = uidDesbloqueando == null
                ) {
                    if (uidDesbloqueando == alvo.uid) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Desbloquear", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { usuarioParaConfirmarDesbloqueio = null },
                    enabled = uidDesbloqueando == null
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS)
            ) {
                Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = CineListTokens.CorErro)
                Text("Usuários Bloqueados", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CineListTokens.EspacoS)
            ) {
                if (bloqueados.isEmpty()) {
                    Text(
                        text = "Você não possui usuários bloqueados no momento.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = CineListTokens.EspacoS)
                    )
                } else {
                    bloqueados.forEach { usuario ->
                        val estaDesbloqueando = uidDesbloqueando == usuario.uid
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(CineListTokens.EspacoS),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CineListTokens.EspacoXS),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AvatarComIniciais(fotoUrl = usuario.fotoUrl, nome = usuario.nome, tamanho = 48.dp, corBorda = MaterialTheme.colorScheme.primary)

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(usuario.nome, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(usuario.email.ifBlank { "Sem e-mail" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        usuarioParaConfirmarDesbloqueio = usuario
                                    },
                                    enabled = !estaDesbloqueando,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    if (estaDesbloqueando) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("Desbloquear", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDispensar) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}