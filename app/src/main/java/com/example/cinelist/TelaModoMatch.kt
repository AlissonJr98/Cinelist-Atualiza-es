package com.example.cinelist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaModoMatch(
    listaDeMidiasDaSala: List<Midia>,
    viewModel: MidiaViewModel,
    onVoltar: () -> Unit,
    onAbrirDetalhesMidia: (Midia) -> Unit
) {
    val meuUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val matchesConfirmados by viewModel.matchesDoGrupo.collectAsState(initial = emptyList())
    val popularesTmdb by viewModel.popularesTmdbMatch.collectAsState(initial = emptyList())

    // Chaves de títulos que JÁ DERAM MATCH (2 ou mais votos)
    val chavesJaDeramMatch = remember(matchesConfirmados) {
        matchesConfirmados.filter { it.deuMatch(2) }.map { it.idDoc }.toSet()
    }

    // Rastreia os IDs votados nesta sessão local para que saiam imediatamente da tela após o clique
    var votosSessaoLocal by remember { mutableStateOf(setOf<String>()) }
    var abaModoTorneio by remember { mutableStateOf(false) }
    var mostrarAlbumRecordacoes by remember { mutableStateOf(false) }

    // 1. Títulos da própria sala
    val midiasDaSalaParaVotar = remember(listaDeMidiasDaSala, chavesJaDeramMatch, votosSessaoLocal) {
        listaDeMidiasDaSala.filter { midia ->
            val chaveDoc = if (midia.idTmdb != 0) "match_tmdb_${midia.idTmdb}" else "match_local_${midia.uuid}"
            val naoConcluido = !midia.status.equals("Concluído", ignoreCase = true) && !midia.status.equals("Concluido", ignoreCase = true)
            naoConcluido && !chavesJaDeramMatch.contains(chaveDoc) && !votosSessaoLocal.contains(chaveDoc)
        }
    }

    // 2. Títulos populares do TMDB para quando a sala acabar
    val popularesParaVotar = remember(popularesTmdb, chavesJaDeramMatch, votosSessaoLocal) {
        popularesTmdb.filter { midia ->
            val chaveDoc = "match_tmdb_${midia.idTmdb}"
            !chavesJaDeramMatch.contains(chaveDoc) && !votosSessaoLocal.contains(chaveDoc)
        }
    }

    // FILA FINAL: Se houver mídias da sala, exibe elas; quando zerar, entra nos Populares do TMDB
    val filaFinalParaVotar = remember(midiasDaSalaParaVotar, popularesParaVotar) {
        if (midiasDaSalaParaVotar.isNotEmpty()) midiasDaSalaParaVotar else popularesParaVotar
    }

    val estaNoModoDescobertaTmdb = midiasDaSalaParaVotar.isEmpty()

    // Solicita mais itens populares quando a fila estiver acabando
    LaunchedEffect(filaFinalParaVotar.size) {
        if (filaFinalParaVotar.size <= 3) {
            viewModel.carregarMaisPopularesMatch()
        }
    }

    // Rastreia matches exibidos para nunca abrir o pop-up duas vezes
    var matchesJaExibidosIds by remember { mutableStateOf(setOf<String>()) }
    var midiaMatchCelebrada by remember { mutableStateOf<MatchMidia?>(null) }

    LaunchedEffect(matchesConfirmados) {
        val novoMatch = matchesConfirmados.firstOrNull { it.deuMatch(2) && !matchesJaExibidosIds.contains(it.idDoc) }
        if (novoMatch != null) {
            midiaMatchCelebrada = novoMatch
            matchesJaExibidosIds = matchesJaExibidosIds + novoMatch.idDoc

            // CORREÇÃO ANTI-DUPLICATA: Apenas o último votante (quem fechou o Match) persiste o registro no banco
            val souOUltimoVotante = novoMatch.votos.lastOrNull() == meuUid
            if (souOUltimoVotante) {
                viewModel.salvarMidiaMatchNaSala(novoMatch)
            }
        }
    }

    // DIÁLOGO COMEMORATIVO DE MATCH
    midiaMatchCelebrada?.let { match ->
        Dialog(onDismissRequest = { midiaMatchCelebrada = null }) {
            GlassmorphicCard(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PulseMatchIcon {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3366)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Match",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = "DEU MATCH! 🍿❤️",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Vocês dois escolheram este título! Ele foi adicionado à lista compartilhada.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )

                    if (match.imagemCapa.isNotBlank()) {
                        AsyncImage(
                            model = match.imagemCapa,
                            contentDescription = match.titulo,
                            modifier = Modifier
                                .width(140.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = match.titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            val midiaOriginal = listaDeMidiasDaSala.find {
                                (it.idTmdb != 0 && it.idTmdb == match.idTmdb) ||
                                        it.titulo.equals(match.titulo, ignoreCase = true)
                            } ?: Midia(
                                idTmdb = match.idTmdb,
                                titulo = match.titulo,
                                tipo = match.tipo,
                                status = "Quero Assistir",
                                nota = 0,
                                sinopse = match.sinopse,
                                imagemCapa = match.imagemCapa,
                                genero = match.genero
                            )

                            midiaMatchCelebrada = null
                            onAbrirDetalhesMidia(midiaOriginal)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Ver Detalhes do Título", fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { midiaMatchCelebrada = null }) {
                        Text("Continuar Votando", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    if (mostrarAlbumRecordacoes) {
        DialogoAlbumRecordacoesMatch(
            matches = matchesConfirmados.filter { it.deuMatch(2) },
            onDispensar = { mostrarAlbumRecordacoes = false }
        )
    }

    Scaffold(
        topBar = {
            TopBarModernaTranslucida(
                title = {
                    Column {
                        Text("Modo Match 🍿", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (estaNoModoDescobertaTmdb) "✨ Em Alta TMDB (${filaFinalParaVotar.size})" else "🍿 Títulos da sala (${filaFinalParaVotar.size})",
                            fontSize = 11.sp,
                            color = if (estaNoModoDescobertaTmdb) Color(0xFFFFD700) else MaterialTheme.colorScheme.secondary,
                            fontWeight = if (estaNoModoDescobertaTmdb) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    IconeBouncyIconButton(onClick = onVoltar) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconeBouncyIconButton(onClick = { mostrarAlbumRecordacoes = true }) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Álbum de Matches", tint = Color(0xFFFF3366))
                    }
                    TextButton(onClick = { abaModoTorneio = !abaModoTorneio }) {
                        Text(if (abaModoTorneio) "🍿 Match" else "🏆 Torneio 1v1", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    IconeAnimadoRotacao(onClick = {
                        viewModel.resetarRodadaMatch()
                        votosSessaoLocal = emptySet()
                        matchesJaExibidosIds = emptySet()
                    }, anguloRotacaoTotal = 360f) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reiniciar Rodada", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        MeshBackground3D(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
            if (abaModoTorneio) {
                TorneioModoView(listaDeMidias = listaDeMidiasDaSala, onAbrirDetalhesMidia = onAbrirDetalhesMidia)
            } else if (filaFinalParaVotar.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Buscando novas opções em alta...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                val midiaAtual = filaFinalParaVotar.first()
                val proximaMidia = if (filaFinalParaVotar.size > 1) filaFinalParaVotar[1] else null
                val chaveDocAtual = if (midiaAtual.idTmdb != 0) "match_tmdb_${midiaAtual.idTmdb}" else "match_local_${midiaAtual.uuid}"

                val haptic = LocalHapticFeedback.current
                val escopoVoto = rememberCoroutineScope()
                val offsetX = remember(midiaAtual) { Animatable(0f) }
                val offsetY = remember(midiaAtual) { Animatable(0f) }

                fun executarVotoAnimado(quero: Boolean) {
                    escopoVoto.launch {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val alvo = if (quero) 1400f else -1400f
                        offsetX.animateTo(alvo, animationSpec = tween(durationMillis = 260))
                        viewModel.votarMatch(midiaAtual, quero)
                        votosSessaoLocal = votosSessaoLocal + chaveDocAtual
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Deck de Cards Sobrepostos
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // Card de Fundo (Próximo título na fila com escala reduzida)
                        proximaMidia?.let { prox ->
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = 0.92f
                                        scaleY = 0.92f
                                        translationY = 24f
                                        alpha = 0.55f
                                    },
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!prox.imagemCapa.isNullOrBlank()) {
                                        AsyncImage(
                                            model = prox.imagemCapa,
                                            contentDescription = prox.titulo,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }

                        // Card Frontal Interativo (com Gestos de Swipe e Rotação)
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = offsetX.value
                                    translationY = offsetY.value
                                    rotationZ = (offsetX.value / 45f).coerceIn(-28f, 28f)
                                }
                                .pointerInput(midiaAtual) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            if (offsetX.value > 260f) {
                                                executarVotoAnimado(true)
                                            } else if (offsetX.value < -260f) {
                                                executarVotoAnimado(false)
                                            } else {
                                                escopoVoto.launch {
                                                    offsetX.animateTo(0f, spring(dampingRatio = 0.65f))
                                                    offsetY.animateTo(0f, spring(dampingRatio = 0.65f))
                                                }
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            escopoVoto.launch {
                                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                                offsetY.snapTo(offsetY.value + dragAmount.y * 0.4f)
                                            }
                                        }
                                    )
                                },
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (!midiaAtual.imagemCapa.isNullOrBlank()) {
                                    AsyncImage(
                                        model = midiaAtual.imagemCapa,
                                        contentDescription = midiaAtual.titulo,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("SEM CAPA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }

                                // Gradiente escuro para leitura das informações
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color(0xB3000000), Color(0xF2050811)),
                                                startY = 300f
                                            )
                                        )
                                )

                                // Informações do título
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(20.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${midiaAtual.tipo} • ${midiaAtual.genero.ifBlank { "Geral" }}",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        if (estaNoModoDescobertaTmdb) {
                                            Surface(
                                                color = Color(0xFFFF9800),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Em Alta TMDB",
                                                    color = Color.Black,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = midiaAtual.titulo,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )

                                    if (midiaAtual.sinopse.isNotBlank() && midiaAtual.sinopse != "Nenhuma sinopse adicionada ainda.") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = midiaAtual.sinopse,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                // Selo Holográfico Dinâmico QUERO (Direita)
                                if (offsetX.value > 20f) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(24.dp)
                                            .graphicsLayer {
                                                rotationZ = -14f
                                                alpha = (offsetX.value / 200f).coerceIn(0f, 1f)
                                            },
                                        color = Color(0xDD00E5FF),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(2.5.dp, Color(0xFF00E5FF))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
                                            Text("QUERO! 🍿", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.Black)
                                        }
                                    }
                                }

                                // Selo Holográfico Dinâmico PASSO (Esquerda)
                                if (offsetX.value < -20f) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(24.dp)
                                            .graphicsLayer {
                                                rotationZ = 14f
                                                alpha = (-offsetX.value / 200f).coerceIn(0f, 1f)
                                            },
                                        color = Color(0xDDFF1744),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(2.5.dp, Color(0xFFFF1744))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                                            Text("PASSO ❌", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Painel de Ações Rápidas
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão Passar (Esquerda)
                        IconButton(
                            onClick = { executarVotoAnimado(false) },
                            modifier = Modifier
                                .size(64.dp)
                                .bounceOnClick()
                                .clip(CircleShape)
                                .background(Color(0xFF181B22))
                                .border(2.dp, Color(0xFFFF1744).copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Não quero agora",
                                tint = Color(0xFFFF1744),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Dica de navegação por gesto
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "⟵ Passe ou Curta ⟶",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Botão Quero (Direita)
                        IconButton(
                            onClick = { executarVotoAnimado(true) },
                            modifier = Modifier
                                .size(64.dp)
                                .bounceOnClick()
                                .clip(CircleShape)
                                .background(Color(0xFF181B22))
                                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Quero assistir hoje!",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
}

@Composable
fun TorneioModoView(
    listaDeMidias: List<Midia>,
    onAbrirDetalhesMidia: (Midia) -> Unit
) {
    val candidatos = remember(listaDeMidias) {
        listaDeMidias.filter { !it.status.equals("Concluído", ignoreCase = true) && !it.status.equals("Concluido", ignoreCase = true) }.take(8)
    }

    var rodada by remember { mutableIntStateOf(0) }
    var competidores by remember(candidatos) { mutableStateOf(candidatos) }
    var campeao by remember { mutableStateOf<Midia?>(null) }

    if (candidatos.size < 2) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Adicione pelo menos 2 mídias à sala para iniciar o Torneio 🏆", color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
        }
        return
    }

    if (campeao != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🏆 CAMPEÃO DO TORNEIO DA NOITE", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.size(140.dp, 210.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                if (campeao!!.imagemCapa.isNotBlank()) {
                    AsyncImage(model = campeao!!.imagemCapa, contentDescription = campeao!!.titulo, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(campeao!!.titulo, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onAbrirDetalhesMidia(campeao!!) }) {
                Text("Ver Detalhes & Assistir 🍿", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    val itemA = competidores.getOrNull(0)
    val itemB = competidores.getOrNull(1)

    if (itemA == null || itemB == null) return

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
            Text("⚔️ Duelo ${rodada + 1} • Escolha o Melhor!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Card A
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                    val proximos = competidores.drop(2) + itemA
                    if (proximos.size <= 1) {
                        campeao = itemA
                    } else {
                        competidores = proximos
                        rodada++
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                        if (itemA.imagemCapa.isNotBlank()) {
                            AsyncImage(model = itemA.imagemCapa, contentDescription = itemA.titulo, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(itemA.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Text("VS", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterVertically))

            // Card B
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                    val proximos = competidores.drop(2) + itemB
                    if (proximos.size <= 1) {
                        campeao = itemB
                    } else {
                        competidores = proximos
                        rodada++
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                        if (itemB.imagemCapa.isNotBlank()) {
                            AsyncImage(model = itemB.imagemCapa, contentDescription = itemB.titulo, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(itemB.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun DialogoAlbumRecordacoesMatch(
    matches: List<MatchMidia>,
    onDispensar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDispensar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF3366))
                Text("Álbum de Matches da Sala 🖼️", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            if (matches.isEmpty()) {
                Text("Nenhum Match registrado ainda nesta sala. Continuem votando!", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
            } else {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    matches.forEach { match ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (match.imagemCapa.isNotBlank()) {
                                    AsyncImage(
                                        model = match.imagemCapa,
                                        contentDescription = match.titulo,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(40.dp, 60.dp).clip(RoundedCornerShape(6.dp))
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(match.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("❤️ Deu Match com ${match.votos.size} votos!", fontSize = 11.sp, color = Color(0xFFFF3366), fontWeight = FontWeight.Bold)
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