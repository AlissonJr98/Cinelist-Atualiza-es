package com.example.cinelist

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class EmojiFlutuante(
    val id: Long = System.nanoTime(),
    val emoji: String,
    val autor: String = "",
    val xOffsetFraction: Float = Random.nextFloat() * 0.7f + 0.15f
)

@Composable
fun DialogoWatchParty(
    tituloMidia: String,
    posterMidia: String = "",
    salaIdInicial: String = "",
    membrosSala: List<MembroGrupo> = emptyList(),
    grupoId: String = "",
    viewModel: MidiaViewModel? = null,
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val escopo = rememberCoroutineScope()
    val meuUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val meuNome = remember { FirebaseAuth.getInstance().currentUser?.displayName ?: "Usuário" }

    // Sala ID padronizada para sincronização no Firestore
    var salaId by remember {
        mutableStateOf(
            if (salaIdInicial.isNotBlank()) salaIdInicial
            else "cineparty_${(tituloMidia.filter { it.isLetterOrDigit() }.take(12).ifBlank { "sessao" })}_${System.currentTimeMillis() % 10000}"
        )
    }

    var mostrarDialogoTrocarCodigo by remember { mutableStateOf(false) }
    var inputCodigoSala by remember { mutableStateOf("") }

    // Coleta o estado em tempo real da sessão do Firestore caso viewModel esteja presente
    val sessaoAtiva by viewModel?.sessaoCinePartyAtiva?.collectAsState() ?: remember { mutableStateOf<CinePartySessao?>(null) }

    // Estados locais de apoio ou fallback caso offline
    var contagemRegressivaLocal by remember { mutableIntStateOf(3) }
    var emContagemLocal by remember { mutableStateOf(false) }
    var assistindoSincronizadoLocal by remember { mutableStateOf(false) }
    var estaPausadoLocal by remember { mutableStateOf(false) }
    var segundosDecorridosLocal by remember { mutableIntStateOf(0) }

    // Estados de voz estilo Discord (Portas abertas para WebRTC / Voice Channel)
    var microfoneMutado by remember { mutableStateOf(false) }
    var canalVozConectado by remember { mutableStateOf(true) }

    val emojisFlutuando = remember { mutableStateListOf<EmojiFlutuante>() }
    var ultimoTimestampReacaoProcessado by remember { mutableLongStateOf(0L) }

    // Conectar à sala no Firestore ao entrar
    LaunchedEffect(salaId) {
        if (viewModel != null && salaId.isNotBlank()) {
            viewModel.conectarCineParty(salaId)
            viewModel.iniciarOuEntrarCineParty(
                salaId = salaId,
                tituloMidia = tituloMidia,
                posterMidia = posterMidia
            ) { _ -> }
        }
    }

    // Desconectar ao fechar
    DisposableEffect(salaId) {
        onDispose {
            if (viewModel != null && salaId.isNotBlank()) {
                viewModel.desconectarCineParty(salaId)
            }
        }
    }

    // Sincronizar reações que chegam de outros participantes via Firestore
    LaunchedEffect(sessaoAtiva?.ultimaReacaoTimestamp) {
        val sessao = sessaoAtiva
        if (sessao != null && sessao.ultimaReacaoTimestamp > ultimoTimestampReacaoProcessado && sessao.ultimaReacaoEmoji.isNotBlank()) {
            ultimoTimestampReacaoProcessado = sessao.ultimaReacaoTimestamp
            val novoEmoji = EmojiFlutuante(
                emoji = sessao.ultimaReacaoEmoji,
                autor = sessao.ultimaReacaoAutor
            )
            emojisFlutuando.add(novoEmoji)
            escopo.launch {
                delay(2400L)
                emojisFlutuando.remove(novoEmoji)
            }
        }
    }

    // Determina se a sessão está sendo coordenada pelo Firestore
    val sincronizadoFirestore = sessaoAtiva != null
    val souAnfitriao = sessaoAtiva?.anfitriaoUid.isNullOrBlank() || sessaoAtiva?.anfitriaoUid == meuUid

    val estaEmContagem = if (sincronizadoFirestore) sessaoAtiva?.emContagem == true else emContagemLocal
    val numeroContagem = if (sincronizadoFirestore) (sessaoAtiva?.contagemNumero ?: 3) else contagemRegressivaLocal

    val estaEmReproducao = if (sincronizadoFirestore) {
        sessaoAtiva?.estadoReproducao == "REPRODUZINDO" || sessaoAtiva?.estadoReproducao == "PAUSADO"
    } else {
        assistindoSincronizadoLocal
    }

    val estaPausado = if (sincronizadoFirestore) {
        sessaoAtiva?.estadoReproducao == "PAUSADO"
    } else {
        estaPausadoLocal
    }

    // Cronômetro local quando reproduzindo
    LaunchedEffect(estaEmReproducao, estaPausado) {
        if (estaEmReproducao && !estaPausado) {
            while (true) {
                delay(1000L)
                segundosDecorridosLocal++
            }
        }
    }

    // Dispara reação local + Firestore
    fun dispararReacao(emoji: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val novo = EmojiFlutuante(emoji = emoji, autor = "Você")
        emojisFlutuando.add(novo)
        escopo.launch {
            delay(2400L)
            emojisFlutuando.remove(novo)
        }

        if (viewModel != null && salaId.isNotBlank()) {
            viewModel.enviarReacaoCineParty(salaId, emoji)
        }
    }

    // Dispara contagem sincronizada para todos os dispositivos
    fun iniciarContagemSincronizada() {
        if (viewModel != null && salaId.isNotBlank()) {
            viewModel.dispararContagemCineParty(salaId)
        } else {
            emContagemLocal = true
            contagemRegressivaLocal = 3
        }
    }

    // Ticker síncrono local que roda em simultâneo no celular de todos os participantes
    LaunchedEffect(estaEmContagem) {
        if (estaEmContagem) {
            contagemRegressivaLocal = 3
            emContagemLocal = true
            while (contagemRegressivaLocal > 0) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(1000L)
                contagemRegressivaLocal--
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(500L)
            emContagemLocal = false
            assistindoSincronizadoLocal = true
            if (souAnfitriao && viewModel != null && salaId.isNotBlank()) {
                viewModel.atualizarContagemPassoCineParty(salaId, 0)
            }
        }
    }

    Dialog(
        onDismissRequest = onDispensar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6050811)),
            contentAlignment = Alignment.Center
        ) {
            // CONTAINER DO COMPANHEIRO DE SESSÃO
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF0F1424),
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.90f)
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Bar com Código da Sala e Fechar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🍿", fontSize = 22.sp)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "CINEPARTY LIVE".t(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color(0xFF00E5FF)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        border = BorderStroke(0.8.dp, Color(0xFF10B981))
                                    ) {
                                        Text(
                                            text = "AO VIVO".t(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tituloMidia,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(onClick = onDispensar, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8))
                        }
                    }

                    // Card de Código da Sala e Compartilhar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF151D30),
                        border = BorderStroke(1.dp, Color(0xFF26324D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${"Sala".t()}: $salaId",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        mostrarDialogoTrocarCodigo = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = "Entrar com Código".t(), tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(salaId))
                                        Toast.makeText(contexto, "Código da sala copiado!".t(), Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Código".t(), tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "🍿 Bora assistir *$tituloMidia* comigo no CineParty do CineList?\nCódigo da sala: $salaId"
                                            )
                                            type = "text/plain"
                                        }
                                        contexto.startActivity(Intent.createChooser(sendIntent, "Convidar para CineParty"))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Compartilhar".t(), tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // DISCORD-LIKE PRESENCE & VOICE STAGE (PALCO AO VIVO)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF111827),
                        border = BorderStroke(1.dp, Color(0xFF374151)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (canalVozConectado) Color(0xFF10B981) else Color(0xFF6B7280))
                                    )
                                    Text(
                                        text = "PALCO // SALA DE VOZ & PRESENÇA".t(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }

                                // Controles de Áudio estilo Discord
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (microfoneMutado) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, if (microfoneMutado) Color(0xFFEF4444) else Color(0xFF10B981)),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable {
                                                microfoneMutado = !microfoneMutado
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                Toast.makeText(
                                                    contexto,
                                                    if (microfoneMutado) "Microfone mutado 🔇".t() else "Microfone aberto 🎙️".t(),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (microfoneMutado) Icons.Default.MicOff else Icons.Default.Mic,
                                                contentDescription = "Mutar voz".t(),
                                                tint = if (microfoneMutado) Color(0xFFEF4444) else Color(0xFF10B981),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Participantes no Palco (Ao Vivo)
                            val participantesLista = remember(sessaoAtiva?.participantes) {
                                sessaoAtiva?.obterParticipantesMapeados()?.ifEmpty { null } ?: listOf(
                                    CinePartyParticipante(uid = meuUid, nome = meuNome, fotoUrl = "", online = true, statusVoz = "FALANDO")
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                items(participantesLista, key = { it.uid.ifBlank { it.nome } }) { part ->
                                    val souEsse = part.uid == meuUid
                                    val ehHost = sessaoAtiva?.anfitriaoUid == part.uid || (sessaoAtiva == null && souEsse)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.width(62.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF1F2937),
                                                border = BorderStroke(
                                                    2.dp,
                                                    if (part.online) Color(0xFF10B981) else Color(0xFF4B5563)
                                                ),
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                if (part.fotoUrl.isNotBlank()) {
                                                    AsyncImage(
                                                        model = part.fotoUrl,
                                                        contentDescription = part.nome,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = part.nome.take(1).uppercase(),
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontSize = 16.sp
                                                        )
                                                    }
                                                }
                                            }

                                            if (ehHost) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFF59E0B)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("👑", fontSize = 9.sp)
                                                }
                                            }
                                        }

                                        Text(
                                            text = if (souEsse) "Você".t() else part.nome.split(" ").firstOrNull() ?: part.nome,
                                            fontSize = 11.sp,
                                            fontWeight = if (souEsse) FontWeight.Bold else FontWeight.Normal,
                                            color = if (souEsse) Color(0xFF00E5FF) else Color(0xFFE5E7EB),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Lista de amigos online na sala compartilhada para chamar instantaneamente
                            val ausentesOnline = remember(membrosSala, participantesLista) {
                                membrosSala.filter { m ->
                                    m.uid != meuUid && m.estaRealmenteOnline && participantesLista.none { p -> p.uid == m.uid }
                                }
                            }

                            if (ausentesOnline.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${"Amigos online na sala".t()} (${ausentesOnline.size}):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF)
                                    )
                                    ausentesOnline.forEach { amigo ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF151D30),
                                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF10B981))
                                                    )
                                                    Text(
                                                        text = amigo.nome,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }

                                                Button(
                                                    onClick = {
                                                        if (viewModel != null && grupoId.isNotBlank()) {
                                                            viewModel.enviarMensagemParaSala(
                                                                texto = "🍿 Chamando ${amigo.nome} para a chamada CineParty de '$tituloMidia'!",
                                                                tipoMensagem = "CINEPARTY_CHAMADA",
                                                                mediaUrl = salaId
                                                            )
                                                            Toast.makeText(contexto, "Chamando ${amigo.nome} para a chamada...", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(
                                                        text = "Chamar 📞".t(),
                                                        color = Color.Black,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Painel de Play Sincronizado / Contagem Regressiva em tempo real
                    if (estaEmContagem) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF151D30),
                            border = BorderStroke(2.dp, Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "CONTAGEM SINCRONIZADA".t(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = if (numeroContagem > 0) "$numeroContagem" else "PLAY! 🚀",
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (numeroContagem > 0) Color(0xFF00E5FF) else Color(0xFF10B981)
                                )

                                Text(
                                    text = "Todos os participantes apertam o play juntos!".t(),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (!estaEmReproducao) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF151D30),
                            border = BorderStroke(1.dp, Color(0xFF26324D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Prontos para assistir juntos?".t(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Quando disparar a contagem, todos na sala verão 3, 2, 1... e começarão no exato mesmo segundo!".t(),
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = { iniciarContagemSincronizada() },
                                    modifier = Modifier.fillMaxWidth().bounceOnClick(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (souAnfitriao) "Disparar Contagem ao Vivo (3, 2, 1...)".t() else "Iniciar Contagem (3, 2, 1...)".t(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    } else {
                        // Painel de Sessão Ativa / Cronômetro Sincronizado
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF151D30),
                            border = BorderStroke(1.2.dp, if (estaPausado) Color(0xFFE11D48) else Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val horas = segundosDecorridosLocal / 3600
                                val minutos = (segundosDecorridosLocal % 3600) / 60
                                val segundos = segundosDecorridosLocal % 60
                                val tempoFormatado = if (horas > 0) {
                                    String.format("%02d:%02d:%02d", horas, minutos, segundos)
                                } else {
                                    String.format("%02d:%02d", minutos, segundos)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (estaPausado) Color(0xFFE11D48) else Color(0xFF10B981))
                                    )
                                    Text(
                                        text = if (estaPausado) "SESSÃO PAUSADA".t() else "EM SINCRONIA AO VIVO".t(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (estaPausado) Color(0xFFE11D48) else Color(0xFF10B981),
                                        letterSpacing = 1.sp
                                    )
                                }

                                Text(
                                    text = tempoFormatado,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (estaPausado) Color(0xFFFDA4AF) else Color(0xFF00E5FF)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            val novoPausado = !estaPausado
                                            estaPausadoLocal = novoPausado
                                            if (viewModel != null && salaId.isNotBlank()) {
                                                viewModel.alternarPauseCineParty(salaId, novoPausado, segundosDecorridosLocal)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (estaPausado) Color(0xFF10B981) else Color(0xFFE11D48)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (estaPausado) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (estaPausado) "Retomar Geral".t() else "Pausar Geral".t(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            assistindoSincronizadoLocal = false
                                            segundosDecorridosLocal = 0
                                            if (viewModel != null && salaId.isNotBlank()) {
                                                viewModel.desconectarCineParty(salaId)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Encerrar".t(), fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }

                    // CHUVA DE REAÇÕES AO VIVO SINCRONIZADA
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Reações ao vivo (transmitidas a todos na sala):".t(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        val reacoes = listOf("😱", "🍿", "🤯", "😭", "🔥", "❤️", "🚀", "😂")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            reacoes.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .bounceOnClick()
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                        .border(1.dp, Color(0xFF334155), CircleShape)
                                        .clickable { dispararReacao(emoji) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }

            // EMOJIS FLUTUANTES SINCRONIZADOS NA TELA
            emojisFlutuando.forEach { item ->
                var animOffsetY by remember { mutableFloatStateOf(0f) }
                var animAlpha by remember { mutableFloatStateOf(1f) }

                LaunchedEffect(item.id) {
                    val tempoInicio = System.currentTimeMillis()
                    while (System.currentTimeMillis() - tempoInicio < 2200L) {
                        val progresso = (System.currentTimeMillis() - tempoInicio) / 2200f
                        animOffsetY = -progresso * 680f
                        animAlpha = (1f - progresso).coerceIn(0f, 1f)
                        delay(16L)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = (item.xOffsetFraction * 320).dp, bottom = 120.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer {
                            translationY = animOffsetY
                            alpha = animAlpha
                        }
                    ) {
                        if (item.autor.isNotBlank() && item.autor != "Você") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xCC1E293B),
                                border = BorderStroke(0.8.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = item.autor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = item.emoji,
                            fontSize = 38.sp
                        )
                    }
                }
            }

            if (mostrarDialogoTrocarCodigo) {
                AlertDialog(
                    onDismissRequest = { mostrarDialogoTrocarCodigo = false },
                    title = { Text("Entrar em Outra Sala CineParty".t()) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Digite ou cole o código da sala de CineParty fornecido pelo seu amigo:".t())
                            OutlinedTextField(
                                value = inputCodigoSala,
                                onValueChange = { inputCodigoSala = it },
                                label = { Text("Código da Sala".t()) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val novoCodigo = inputCodigoSala.trim()
                                if (novoCodigo.isNotBlank()) {
                                    salaId = novoCodigo
                                    mostrarDialogoTrocarCodigo = false
                                    Toast.makeText(contexto, "Conectando à sala $novoCodigo...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                        ) {
                            Text("Entrar na Sala".t(), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarDialogoTrocarCodigo = false }) {
                            Text("Cancelar".t(), color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}
