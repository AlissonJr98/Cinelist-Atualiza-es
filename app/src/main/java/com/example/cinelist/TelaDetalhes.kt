package com.example.cinelist

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDetalhes(
    id: Int,
    tipoInicial: String = "Filme",
    viewModel: MidiaViewModel,
    onVoltar: () -> Unit
) {
    val todasAsMidiasbyBanco by viewModel.todasAsMidias.collectAsState(initial = emptyList())

    val midiaSalva = remember(todasAsMidiasbyBanco, id) {
        todasAsMidiasbyBanco.find { it.id == id || (it.idTmdb != 0 && it.idTmdb == id) }
    }

    var idTmdbDinamico by remember { mutableStateOf<Int?>(null) }
    var tipoDinamico by remember { mutableStateOf(tipoInicial) }
    var recomendacaoSelecionada by remember { mutableStateOf<TmdbFilme?>(null) }

    val detalhesApi by viewModel.detalhesEstendidosApi.collectAsState()
    val provedoresStreaming by viewModel.provedoresStreaming.collectAsState()
    val elencoAtivo by viewModel.elencoMidia.collectAsState()
    val chaveTrailer by viewModel.chaveTrailerYoutube.collectAsState()
    val recomendacoesAtivas by viewModel.recomendacoesMidia.collectAsState()

    val jaExisteNaLista = remember(idTmdbDinamico, todasAsMidiasbyBanco) {
        todasAsMidiasbyBanco.any { it.idTmdb == idTmdbDinamico && it.idTmdb != 0 }
    }

    val contexto = LocalContext.current

    fun abrirAplicativoStreaming(contexto: android.content.Context, nomeProvedor: String) {
        val pacoteApp = when {
            nomeProvedor.contains("Netflix", ignoreCase = true) -> "com.netflix.mediaclient"
            nomeProvedor.contains("Prime Video", ignoreCase = true) -> "com.amazon.amazonvideo.livingroom"
            nomeProvedor.contains("Disney", ignoreCase = true) -> "com.disney.disneyplus"
            nomeProvedor.contains("Max", ignoreCase = true) -> "com.hbomax.android"
            nomeProvedor.contains("Crunchyroll", ignoreCase = true) -> "com.crunchyroll.crunchyroid"
            nomeProvedor.contains("Globoplay", ignoreCase = true) -> "com.globo.globotv"
            nomeProvedor.contains("Apple TV", ignoreCase = true) -> "com.apple.atv.sony.appletv"
            nomeProvedor.contains("Paramount", ignoreCase = true) -> "com.cbs.app"
            else -> null
        }

        if (pacoteApp != null) {
            val intent = contexto.packageManager.getLaunchIntentForPackage(pacoteApp)
            if (intent != null) {
                contexto.startActivity(intent)
            } else {
                try {
                    contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pacoteApp")))
                } catch (e: Exception) {
                    contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pacoteApp")))
                }
            }
        } else {
            val intentBusca = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=Onde+assistir+$nomeProvedor"))
            contexto.startActivity(intentBusca)
        }
    }

    var corPredominante by remember { mutableStateOf(Color(0xFF1E1E1E)) }
    val corSuaveAnimada by animateColorAsState(
        targetValue = corPredominante,
        animationSpec = tween(durationMillis = 800),
        label = "transicaoCorFundo"
    )

    var nota by remember { mutableIntStateOf(1) }
    var sinopse by remember { mutableStateOf("") }
    var temporadaAtual by remember { mutableIntStateOf(1) }
    var episodioAtual by remember { mutableIntStateOf(1) }
    var minutoParado by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Quero Assistir") }
    var abaTemporadaVisualizada by remember { mutableIntStateOf(1) }

    val provedoresFiltrados = remember(provedoresStreaming) {
        provedoresStreaming.distinctBy { item ->
            val nome = item.nomeProvedor.lowercase()
            when {
                nome.contains("netflix") -> "netflix"
                nome.contains("prime video") -> "prime video"
                nome.contains("disney") -> "disney"
                nome.contains("max") -> "max"
                nome.contains("crunchyroll") -> "crunchyroll"
                nome.contains("globoplay") -> "globoplay"
                nome.contains("apple tv") -> "apple tv"
                nome.contains("paramount") -> "paramount"
                else -> item.nomeProvedor.lowercase().trim()
            }
        }
    }

    LaunchedEffect(midiaSalva, id, tipoInicial) {
        if (midiaSalva != null) {
            idTmdbDinamico = midiaSalva.idTmdb
            tipoDinamico = midiaSalva.tipo
            nota = midiaSalva.nota
            sinopse = midiaSalva.sinopse
            temporadaAtual = midiaSalva.temporadaAtual
            episodioAtual = midiaSalva.episodioAtual
            minutoParado = midiaSalva.minutoParado
            status = midiaSalva.status
            abaTemporadaVisualizada = midiaSalva.temporadaAtual
        } else {
            idTmdbDinamico = id
            tipoDinamico = tipoInicial
        }
    }

    LaunchedEffect(idTmdbDinamico, tipoDinamico) {
        idTmdbDinamico?.let { tmdbId ->
            if (tmdbId > 0) {
                viewModel.buscarDetalhesEstendidos(idTmdb = tmdbId, tipo = tipoDinamico)
                viewModel.buscarOndeAssistir(idTmdb = tmdbId, tipo = tipoDinamico)
            }
        }
    }

    val capaParaPaleta = remember(midiaSalva, recomendacaoSelecionada, detalhesApi) {
        when {
            midiaSalva != null && midiaSalva.imagemCapa.isNotBlank() -> midiaSalva.imagemCapa
            recomendacaoSelecionada?.caminhoPoster != null -> "https://image.tmdb.org/t/p/w500${recomendacaoSelecionada?.caminhoPoster}"
            detalhesApi?.urlPosterVertical != null -> detalhesApi?.urlPosterVertical
            detalhesApi?.urlBackdrop != null -> detalhesApi?.urlBackdrop
            else -> null
        }
    }

    LaunchedEffect(capaParaPaleta) {
        if (!capaParaPaleta.isNullOrBlank()) {
            val requisicaoImagem = ImageRequest.Builder(contexto)
                .data(capaParaPaleta)
                .allowHardware(false)
                .build()

            val resultado = coil.ImageLoader(contexto).execute(requisicaoImagem)
            if (resultado is SuccessResult) {
                val bitmap = resultado.drawable.toBitmap()
                Palette.from(bitmap).generate { palette ->
                    val corExtraida = palette?.darkMutedSwatch?.rgb ?: palette?.dominantSwatch?.rgb
                    corExtraida?.let { rgb -> corPredominante = Color(rgb) }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.limparDetalhesEstendidos() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Mídia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    midiaSalva?.let { midiaReal ->
                        IconButton(onClick = {
                            val textoProgresso = if (midiaReal.tipo.equals("Filme", ignoreCase = true)) {
                                if (minutoParado > 0 && status != "Concluído") "estou no minuto $minutoParado" else "vou assistir"
                            } else {
                                if (status == "Concluído") "concluí tudo" else "estou na Temporada $temporadaAtual • Ep $episodioAtual"
                            }

                            val mensagemFinal = """
                                🍿 Olha o meu progresso no CineList!
                                🎬 *${midiaReal.titulo}* (${midiaReal.tipo})
                                📊 Status: $status ($textoProgresso)
                                ⭐ Minha Avaliação: ${"★".repeat(nota)}
                                
                                Gerenciado pelo meu app CineList! 💻🔥
                            """.trimIndent()

                            val intentCompartilhar = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, mensagemFinal)
                                type = "text/plain"
                            }

                            contexto.startActivity(Intent.createChooser(intentCompartilhar, "Compartilhar progresso via:"))
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Compartilhar", tint = Color(0xFFFFD700))
                        }

                        IconButton(onClick = { viewModel.deletar(midiaReal); onVoltar() }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Deletar", tint = Color(0xFFFF4C4C))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(corSuaveAnimada.copy(alpha = 0.4f), Color(0xFF121212)),
                        endY = 1200f
                    )
                )
                .verticalScroll(rememberScrollState())
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (detalhesApi != null && !detalhesApi?.urlBackdrop.isNullOrBlank()) {
                    AsyncImage(
                        model = detalhesApi?.urlBackdrop,
                        contentDescription = "Banner de Fundo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xAA121212), Color(0xFF121212)),
                                startY = 100f
                            )
                        )
                )

                Card(
                    modifier = Modifier
                        .width(150.dp)
                        .height(220.dp)
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2A2A2A)), contentAlignment = Alignment.Center) {
                        val urlPosterExibicao = when {
                            midiaSalva != null && midiaSalva.imagemCapa.isNotBlank() -> midiaSalva.imagemCapa
                            recomendacaoSelecionada?.caminhoPoster != null -> "https://image.tmdb.org/t/p/w500${recomendacaoSelecionada?.caminhoPoster}"
                            !detalhesApi?.urlPosterVertical.isNullOrBlank() -> detalhesApi?.urlPosterVertical
                            !detalhesApi?.urlBackdrop.isNullOrBlank() -> detalhesApi?.urlBackdrop
                            else -> null
                        }

                        if (!urlPosterExibicao.isNullOrBlank()) {
                            AsyncImage(
                                model = urlPosterExibicao,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("SEM CAPA", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Spacer(modifier = Modifier.height(8.dp))

                val tituloDinamico = when {
                    midiaSalva != null -> midiaSalva.titulo
                    recomendacaoSelecionada != null -> recomendacaoSelecionada?.titulo ?: "Título"
                    detalhesApi != null -> detalhesApi?.titulo ?: "Detalhes"
                    else -> "Carregando..."
                }

                Text(text = tituloDinamico, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "$tipoDinamico • ${midiaSalva?.genero ?: "Geral"}", fontSize = 15.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)

                if (detalhesApi != null && !detalhesApi?.fraseEfeito.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${detalhesApi?.fraseEfeito}\"",
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val ano = detalhesApi?.anoLancamento ?: "..."
                        val duracao = if ((detalhesApi?.duracaoMinutos ?: 0) > 0) "${detalhesApi?.duracaoMinutos} min" else "..."
                        val notaPublico = if (detalhesApi != null) String.format("%.1f ★", detalhesApi?.notaCritica) else "..."

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lançamento", color = Color.Gray, fontSize = 11.sp)
                            Text(ano, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Duração Real", color = Color.Gray, fontSize = 11.sp)
                            Text(duracao, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Média Crítica", color = Color.Gray, fontSize = 11.sp)
                            Text(notaPublico, color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!chaveTrailer.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intentApp = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$chaveTrailer"))
                            val intentNavegador = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$chaveTrailer"))
                            try {
                                contexto.startActivity(intentApp)
                            } catch (ex: Exception) {
                                contexto.startActivity(intentNavegador)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ASSISTIR TRAILER OFICIAL", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (provedoresFiltrados.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Disponível por Assinatura em:",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        provedoresFiltrados.forEach { streaming ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { abrirAplicativoStreaming(contexto, streaming.nomeProvedor) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E1E1E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = streaming.urlLogo,
                                        contentDescription = streaming.nomeProvedor,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val nomeOriginal = streaming.nomeProvedor
                                val nomeExibicao = when {
                                    nomeOriginal.contains("netflix", ignoreCase = true) -> "Netflix"
                                    nomeOriginal.contains("prime video", ignoreCase = true) -> "Prime Video"
                                    nomeOriginal.contains("disney", ignoreCase = true) -> "Disney+"
                                    nomeOriginal.contains("max", ignoreCase = true) -> "Max"
                                    nomeOriginal.contains("crunchyroll", ignoreCase = true) -> "Crunchyroll"
                                    nomeOriginal.contains("globoplay", ignoreCase = true) -> "Globoplay"
                                    nomeOriginal.contains("apple tv", ignoreCase = true) -> "Apple TV"
                                    nomeOriginal.contains("paramount", ignoreCase = true) -> "Paramount+"
                                    else -> nomeOriginal
                                }

                                Text(
                                    text = nomeExibicao,
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (elencoAtivo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Elenco Principal:",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        elencoAtivo.take(12).forEach { ator ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(76.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2A2A2A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ator.urlFotoPerfil.isNotBlank()) {
                                        AsyncImage(
                                            model = ator.urlFotoPerfil,
                                            contentDescription = ator.nomeReal,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = ator.nomeReal.take(2).uppercase(),
                                            color = Color.Gray,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = ator.nomeReal,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = ator.nomePersonagem,
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (midiaSalva != null) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Sua Nota Pessoal:", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        repeat(5) { index ->
                            val estrelaAtiva = index < nota
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (estrelaAtiva) Color(0xFFFFD700) else Color.DarkGray,
                                modifier = Modifier.size(36.dp).clickable { nota = index + 1 }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val ehFilme = midiaSalva.tipo.equals("Filme", ignoreCase = true)
                    Text(text = "Seu Progresso de Visualização:", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!ehFilme) {
                        // 🚀 RASTREADOR INTERATIVO DE EPISÓDIOS (OPÇÃO 2)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Régua de Episódios",
                                    color = Color(0xFFFFD700),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Seletor de Temporadas (T1, T2...)
                                Text(text = "Temporadas:", color = Color.Gray, fontSize = 12.sp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val maxTemporadas = maxOf(temporadaAtual + 2, 6)
                                    (1..maxTemporadas).forEach { temp ->
                                        FilterChip(
                                            selected = (abaTemporadaVisualizada == temp),
                                            onClick = { abaTemporadaVisualizada = temp },
                                            label = { Text("T$temp", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFFFD700),
                                                selectedLabelColor = Color.Black,
                                                containerColor = Color(0xFF2A2A2A),
                                                labelColor = Color.LightGray
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Régua de Episódios da Temporada Selecionada
                                Text(
                                    text = "Toque no último episódio assistido da Temporada $abaTemporadaVisualizada:",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    (1..30).forEach { ep ->
                                        val jaAssistido = (abaTemporadaVisualizada < temporadaAtual) ||
                                                (abaTemporadaVisualizada == temporadaAtual && ep <= episodioAtual)
                                        val epAtual = (abaTemporadaVisualizada == temporadaAtual && ep == episodioAtual)

                                        FilterChip(
                                            selected = jaAssistido,
                                            onClick = {
                                                temporadaAtual = abaTemporadaVisualizada
                                                episodioAtual = ep
                                                if (status == "Quero Assistir") status = "Assistindo"
                                            },
                                            leadingIcon = if (epAtual) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                            } else null,
                                            label = { Text("E$ep", fontSize = 11.sp, fontWeight = if (epAtual) FontWeight.Bold else FontWeight.Normal) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (epAtual) Color(0xFFFFD700) else Color(0x66FFD700),
                                                selectedLabelColor = if (epAtual) Color.Black else Color.White,
                                                containerColor = Color(0xFF2A2A2A),
                                                labelColor = Color.LightGray
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ContadorProgresso(label = "Temporada Atual", valor = temporadaAtual, onIncrementar = { temporadaAtual++; abaTemporadaVisualizada = temporadaAtual }, onDecrementar = { if (temporadaAtual > 1) { temporadaAtual--; abaTemporadaVisualizada = temporadaAtual } })
                            ContadorProgresso(label = "Episódio Assistido", valor = episodioAtual, onIncrementar = { episodioAtual++ }, onDecrementar = { if (episodioAtual > 1) episodioAtual-- })
                            ContadorProgresso(label = "Minutos Assistidos", valor = minutoParado, onIncrementar = { minutoParado += 5 }, onDecrementar = { if (minutoParado > 0) minutoParado -= 5 })
                        }
                    } else {
                        ContadorProgresso(label = "Minuto Parado no Filme", valor = minutoParado, onIncrementar = { minutoParado += 10 }, onDecrementar = { if (minutoParado > 0) minutoParado -= 10 })
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Status Atual da Mídia:", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Quero Assistir", "Assistindo", "Concluído").forEach { s ->
                            FilterChip(
                                selected = (status == s),
                                onClick = { status = s },
                                label = { Text(s, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFD700),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E1E1E),
                                    labelColor = Color.Gray
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Sinopse / Visão Geral:", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                val sinopseDinamica = when {
                    midiaSalva != null && sinopse.isNotBlank() -> sinopse
                    recomendacaoSelecionada != null -> recomendacaoSelecionada?.sinopse ?: ""
                    detalhesApi != null -> detalhesApi?.sinopseApi ?: ""
                    else -> "Carregando sinopse..."
                }

                OutlinedTextField(
                    value = sinopseDinamica,
                    onValueChange = { if (midiaSalva != null) sinopse = it },
                    readOnly = (midiaSalva == null),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                if (recomendacoesAtivas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Quem assistiu a este título também gostou:",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        recomendacoesAtivas.take(10).forEach { recomendacao ->
                            val urlPoster = "https://image.tmdb.org/t/p/w300${recomendacao.caminhoPoster}"

                            Card(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(150.dp)
                                    .clickable {
                                        recomendacaoSelecionada = recomendacao
                                        idTmdbDinamico = recomendacao.idTmdb
                                    },
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0xFF2A2A2A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!recomendacao.caminhoPoster.isNullOrBlank()) {
                                        AsyncImage(
                                            model = urlPoster,
                                            contentDescription = recomendacao.titulo,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = recomendacao.titulo,
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (midiaSalva != null) {
                    Button(
                        onClick = {
                            val midiaAtualizada = Midia(
                                id = midiaSalva.id,
                                idTmdb = midiaSalva.idTmdb,
                                titulo = midiaSalva.titulo,
                                tipo = midiaSalva.tipo,
                                nota = nota,
                                temporadaAtual = temporadaAtual,
                                episodioAtual = episodioAtual,
                                minutoParado = minutoParado,
                                jaEncerrou = (status == "Concluído"),
                                status = status,
                                sinopse = sinopse,
                                imagemCapa = midiaSalva.imagemCapa,
                                genero = midiaSalva.genero,
                                plataforma = midiaSalva.plataforma
                            )
                            viewModel.atualizar(midiaAtualizada)
                            onVoltar()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                    ) {
                        Text("SALVAR ALTERAÇÕES", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            val streamingPrincipal = provedoresStreaming.firstOrNull()?.nomeProvedor ?: "Não Informado"
                            val tituloParaSalvar = recomendacaoSelecionada?.titulo ?: detalhesApi?.titulo ?: "Título"
                            val capaParaSalvar = when {
                                recomendacaoSelecionada?.caminhoPoster != null -> "https://image.tmdb.org/t/p/w500${recomendacaoSelecionada?.caminhoPoster}"
                                !detalhesApi?.urlPosterVertical.isNullOrBlank() -> detalhesApi?.urlPosterVertical ?: ""
                                else -> ""
                            }
                            val sinopseParaSalvar = recomendacaoSelecionada?.sinopse ?: detalhesApi?.sinopseApi ?: ""

                            val novaMidia = Midia(
                                id = 0,
                                idTmdb = idTmdbDinamico ?: id,
                                titulo = tituloParaSalvar,
                                tipo = tipoDinamico,
                                nota = 0,
                                temporadaAtual = 1,
                                episodioAtual = 1,
                                minutoParado = 0,
                                jaEncerrou = false,
                                status = "Quero Assistir",
                                sinopse = sinopseParaSalvar,
                                imagemCapa = capaParaSalvar,
                                genero = "Geral",
                                plataforma = streamingPrincipal
                            )
                            viewModel.inserir(novaMidia)
                            Toast.makeText(contexto, "Adicionado à sua lista!", Toast.LENGTH_SHORT).show()
                            onVoltar()
                        },
                        enabled = !jaExisteNaLista,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text(
                            text = if (jaExisteNaLista) "JÁ ESTÁ NA SUA LISTA" else "ADICIONAR À MINHA LISTA",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ContadorProgresso(
    label: String,
    valor: Int,
    onIncrementar: () -> Unit,
    onDecrementar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrementar) {
                Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = valor.toString(),
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = onIncrementar) {
                Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}