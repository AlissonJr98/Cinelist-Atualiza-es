package com.example.cinelist

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.view.ViewGroup
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDetalhes(
    id: Int,
    tipoInicial: String = "Filme",
    viewModel: MidiaViewModel,
    onVoltar: () -> Unit,
    onRecomendacaoClique: (Int, String) -> Unit = { _, _ -> }
) {
    // Coleta o casalId ativo no ViewModel para saber onde salvar se for novo
    val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()
    val isModoCasal = casalIdAtivo.isNotBlank()

    // Observa as listas correspondentes de acordo com o modo ativo
    val listaAtualBanco by (if (isModoCasal) viewModel.midiasGrupoAtivo else viewModel.midiasPessoais).collectAsState(initial = emptyList())
    val todasAsMidiasbyBanco by viewModel.todasAsMidias.collectAsState(initial = emptyList())

    val midiaSalva = remember(listaAtualBanco, todasAsMidiasbyBanco, id, isModoCasal) {
        listaAtualBanco.find { it.idTmdb == id } ?: listaAtualBanco.find { it.id == id }
        ?: todasAsMidiasbyBanco.find { it.idTmdb == id && it.isCasal == isModoCasal }
    }

    val idRealBuscaApi = remember(midiaSalva, id) {
        midiaSalva?.let { if (it.idTmdb != 0) it.idTmdb else null } ?: id
    }

    val tipoRealUtilizado = midiaSalva?.tipo ?: tipoInicial

    var exibindoPlayerNativo by remember { mutableStateOf(false) }
    var imagemModalExpandida by remember { mutableStateOf<String?>(null) }
    var mostrarConfirmacaoExclusao by remember { mutableStateOf(false) }

    val detalhesApi by viewModel.detalhesEstendidosApi.collectAsState()
    val provedoresStreaming by viewModel.provedoresStreaming.collectAsState()
    val elencoAtivo by viewModel.elencoMidia.collectAsState()
    val chaveTrailer by viewModel.chaveTrailerYoutube.collectAsState()
    val recomendacoesAtivas by viewModel.recomendacoesMidia.collectAsState()
    val episodiosTmdb by viewModel.episodiosTemporada.collectAsState()
    val carregandoEpisodios by viewModel.carregandoEpisodios.collectAsState()
    val galeriaImagens by viewModel.galeriaImagens.collectAsState()

    val jaExisteNaLista = remember(idRealBuscaApi, listaAtualBanco) {
        listaAtualBanco.any { (it.idTmdb != 0 && it.idTmdb == idRealBuscaApi) || it.id == idRealBuscaApi }
    }

    val contexto = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun abrirAplicativoStreaming(contexto: Context, nomeProvedor: String) {
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
    var status by remember { mutableStateOf("Quero Assistir") }
    var listaCustomizada by remember { mutableStateOf("Geral") }
    var abaTemporadaVisualizada by remember { mutableIntStateOf(1) }
    var ehFavorito by remember { mutableStateOf(false) }

    val colecoesExistentes = remember(listaAtualBanco) {
        listOf("Geral") + listaAtualBanco.map { it.listaCustomizada }.filter { it.isNotBlank() && it != "Geral" }.distinct().sorted()
    }

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

    val ehSerieOuAnime = remember(tipoRealUtilizado) {
        tipoRealUtilizado.equals("Série", ignoreCase = true) ||
                tipoRealUtilizado.equals("Anime", ignoreCase = true) ||
                tipoRealUtilizado.equals("Novela", ignoreCase = true) ||
                tipoRealUtilizado.equals("Dorama", ignoreCase = true) ||
                tipoRealUtilizado.equals("tv", ignoreCase = true)
    }

    LaunchedEffect(midiaSalva) {
        if (midiaSalva != null) {
            nota = midiaSalva.nota
            sinopse = midiaSalva.sinopse
            temporadaAtual = midiaSalva.temporadaAtual
            episodioAtual = midiaSalva.episodioAtual
            status = midiaSalva.status
            listaCustomizada = midiaSalva.listaCustomizada.ifBlank { "Geral" }
            abaTemporadaVisualizada = midiaSalva.temporadaAtual
            ehFavorito = midiaSalva.favorito
        }
    }

    LaunchedEffect(idRealBuscaApi, tipoRealUtilizado) {
        viewModel.limparDetalhesEstendidos()
        exibindoPlayerNativo = false
        abaTemporadaVisualizada = midiaSalva?.temporadaAtual ?: 1
        if (idRealBuscaApi > 0) {
            viewModel.buscarDetalhesEstendidos(idTmdb = idRealBuscaApi, tipo = tipoRealUtilizado)
            viewModel.buscarOndeAssistir(idTmdb = idRealBuscaApi, tipo = tipoRealUtilizado)
        }
    }

    LaunchedEffect(idRealBuscaApi, abaTemporadaVisualizada, ehSerieOuAnime) {
        if (idRealBuscaApi > 0 && ehSerieOuAnime) {
            viewModel.buscarEpisodiosTemporada(idRealBuscaApi, abaTemporadaVisualizada)
        }
    }

    val capaParaPaleta = remember(midiaSalva, detalhesApi) {
        when {
            midiaSalva != null && midiaSalva.imagemCapa.isNotBlank() -> midiaSalva.imagemCapa
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

    if (mostrarConfirmacaoExclusao && midiaSalva != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoExclusao = false },
            title = { Text("Excluir Mídia", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente remover \"${midiaSalva.titulo}\" da lista?") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacaoExclusao = false
                        viewModel.deletar(midiaSalva)
                        onVoltar()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoExclusao = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    imagemModalExpandida?.let { urlFoto ->
        Dialog(
            onDismissRequest = { imagemModalExpandida = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { imagemModalExpandida = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = urlFoto,
                    contentDescription = "Foto Expandida",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { imagemModalExpandida = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0x88000000), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isModoCasal) "Detalhes (Casal ❤️)" else "Detalhes da Mídia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    midiaSalva?.let { midiaReal ->
                        IconButton(onClick = {
                            val novoFavorito = !ehFavorito
                            ehFavorito = novoFavorito
                            viewModel.atualizar(midiaReal.copy(favorito = novoFavorito))
                        }) {
                            Icon(
                                imageVector = if (ehFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (ehFavorito) "Remover dos favoritos" else "Favoritar",
                                tint = if (ehFavorito) Color(0xFFFF3366) else Color.White
                            )
                        }

                        IconButton(onClick = {
                            compartilharCardEstilizado(
                                contexto = contexto,
                                coroutineScope = coroutineScope,
                                midia = midiaReal.copy(
                                    nota = nota,
                                    status = status,
                                    temporadaAtual = temporadaAtual,
                                    episodioAtual = episodioAtual
                                )
                            )
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Compartilhar Card", tint = Color(0xFFFFD700))
                        }

                        IconButton(onClick = { mostrarConfirmacaoExclusao = true }) {
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
                    .height(if (exibindoPlayerNativo) 240.dp else 280.dp)
            ) {
                if (exibindoPlayerNativo && !chaveTrailer.isNullOrBlank()) {
                    PlayerTrailerNativo(
                        chaveVideo = chaveTrailer!!,
                        modifier = Modifier.fillMaxSize(),
                        onFechar = { exibindoPlayerNativo = false }
                    )
                } else {
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
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Spacer(modifier = Modifier.height(8.dp))

                val tituloDinamico = midiaSalva?.titulo ?: detalhesApi?.titulo ?: if (detalhesApi == null && idRealBuscaApi > 0) "Carregando..." else "Título Indisponível"

                Text(text = tituloDinamico, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "$tipoRealUtilizado • ${midiaSalva?.genero ?: detalhesApi?.generoTexto ?: "Geral"}", fontSize = 15.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exibindoPlayerNativo = !exibindoPlayerNativo },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (exibindoPlayerNativo) Color(0xFF333333) else Color(0xFFFF0000)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (exibindoPlayerNativo) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (exibindoPlayerNativo) "FECHAR PLAYER" else "VER TRAILER NATIVO",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val intentApp = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$chaveTrailer"))
                                val intentNavegador = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$chaveTrailer"))
                                try {
                                    contexto.startActivity(intentApp)
                                } catch (ex: Exception) {
                                    contexto.startActivity(intentNavegador)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Abrir no YouTube",
                                modifier = Modifier.size(18.dp)
                            )
                        }
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

                if (galeriaImagens.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Galeria de Fotos & Cenas:",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        galeriaImagens.take(15).forEach { imagemItem ->
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(105.dp)
                                    .clickable {
                                        imagemModalExpandida = imagemItem.urlOriginal
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                            ) {
                                AsyncImage(
                                    model = imagemItem.urlMiniatura,
                                    contentDescription = "Cena da Mídia",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
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

                if (ehSerieOuAnime) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Guia de Episódios:", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Selecione a Temporada:", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))

                            val totalTempDisponiveis = detalhesApi?.totalTemporadas ?: maxOf(temporadaAtual + 1, 1)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (1..totalTempDisponiveis).forEach { temp ->
                                    FilterChip(
                                        selected = (abaTemporadaVisualizada == temp),
                                        onClick = { abaTemporadaVisualizada = temp },
                                        label = { Text("Temporada $temp", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFD700),
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF2A2A2A),
                                            labelColor = Color.LightGray
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (carregandoEpisodios) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                                }
                            } else if (episodiosTmdb.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    episodiosTmdb.forEach { epItem ->
                                        val jaAssistido = (abaTemporadaVisualizada < temporadaAtual) ||
                                                (abaTemporadaVisualizada == temporadaAtual && epItem.numeroEpisodio <= episodioAtual)
                                        val ehOAtual = (abaTemporadaVisualizada == temporadaAtual && epItem.numeroEpisodio == episodioAtual)

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    temporadaAtual = abaTemporadaVisualizada
                                                    episodioAtual = epItem.numeroEpisodio
                                                    if (status == "Quero Assistir") status = "Assistindo"
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (ehOAtual) Color(0xFF2E2E1A) else Color(0xFF252525)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (epItem.urlImagemHorizontal.isNotBlank()) {
                                                    AsyncImage(
                                                        model = epItem.urlImagemHorizontal,
                                                        contentDescription = epItem.nome,
                                                        modifier = Modifier
                                                            .width(80.dp)
                                                            .height(48.dp)
                                                            .clip(RoundedCornerShape(4.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Ep. ${epItem.numeroEpisodio} • ${epItem.nome.ifBlank { "Sem Título" }}",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (ehOAtual) Color(0xFFFFD700) else Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (epItem.sinopse.isNotBlank()) {
                                                        Text(
                                                            text = epItem.sinopse,
                                                            fontSize = 11.sp,
                                                            color = Color.LightGray,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (ehOAtual) Color(0xFFFFD700)
                                                            else if (jaAssistido) Color(0x664CAF50)
                                                            else Color(0xFF333333)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (jaAssistido || ehOAtual) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Assistido",
                                                            tint = if (ehOAtual) Color.Black else Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Nenhum detalhe extra de episódios encontrado para esta temporada.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
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

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Coleção Temática / Lista:", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colecoesExistentes.forEach { col ->
                            FilterChip(
                                selected = (listaCustomizada == col),
                                onClick = { listaCustomizada = col },
                                label = { Text(col, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFD700),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E1E1E),
                                    labelColor = Color.Gray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (colecoesExistentes.contains(listaCustomizada)) "" else listaCustomizada,
                        onValueChange = { listaCustomizada = it },
                        label = { Text("Ou criar nova coleção...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Sinopse / Visão Geral:", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                val sinopseDinamica = if (midiaSalva != null && sinopse.isNotBlank()) sinopse else (detalhesApi?.sinopseApi ?: "Carregando sinopse...")

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
                            val tipoRecomendacao = when {
                                recomendacao.mediaType.equals("tv", ignoreCase = true) -> "Série"
                                recomendacao.mediaType.equals("movie", ignoreCase = true) -> "Filme"
                                recomendacao.ehSerie -> "Série"
                                tipoRealUtilizado.equals("Série", ignoreCase = true) -> "Série"
                                else -> "Filme"
                            }

                            Card(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(150.dp)
                                    .clickable {
                                        viewModel.limparDetalhesEstendidos()
                                        onRecomendacaoClique(recomendacao.idTmdb, tipoRecomendacao)
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
                            val streamingAtualizado = provedoresFiltrados.firstOrNull()?.nomeProvedor
                                ?: provedoresStreaming.firstOrNull()?.nomeProvedor
                                ?: midiaSalva.plataforma

                            val midiaAtualizada = midiaSalva.copy(
                                nota = nota,
                                temporadaAtual = temporadaAtual,
                                episodioAtual = episodioAtual,
                                jaEncerrou = (status == "Concluído"),
                                status = status,
                                sinopse = sinopse,
                                favorito = ehFavorito,
                                listaCustomizada = listaCustomizada.ifBlank { "Geral" },
                                plataforma = streamingAtualizado,
                                isCasal = isModoCasal,
                                casalId = casalIdAtivo
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
                            val streamingPrincipal = provedoresFiltrados.firstOrNull()?.nomeProvedor
                                ?: provedoresStreaming.firstOrNull()?.nomeProvedor
                                ?: (if (ehSerieOuAnime) "TV / Original" else "Cinema")

                            val tituloParaSalvar = detalhesApi?.titulo ?: "Título"
                            val capaParaSalvar = if (!detalhesApi?.urlPosterVertical.isNullOrBlank()) detalhesApi?.urlPosterVertical ?: "" else ""
                            val sinopseParaSalvar = detalhesApi?.sinopseApi ?: ""

                            val novaMidia = Midia(
                                id = 0,
                                idTmdb = idRealBuscaApi,
                                titulo = tituloParaSalvar,
                                tipo = tipoRealUtilizado,
                                nota = 0,
                                temporadaAtual = 1,
                                episodioAtual = 1,
                                minutoParado = 0,
                                jaEncerrou = false,
                                status = "Quero Assistir",
                                sinopse = sinopseParaSalvar,
                                imagemCapa = capaParaSalvar,
                                genero = "Geral",
                                plataforma = streamingPrincipal,
                                favorito = false,
                                listaCustomizada = "Geral",
                                isCasal = isModoCasal,
                                casalId = casalIdAtivo
                            )
                            viewModel.inserir(novaMidia)
                            val mensagemToast = if (isModoCasal) "Adicionado à lista do casal ❤️!" else "Adicionado à sua lista!"
                            Toast.makeText(contexto, mensagemToast, Toast.LENGTH_SHORT).show()
                            onVoltar()
                        },
                        enabled = !jaExisteNaLista,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text(
                            text = if (jaExisteNaLista) "JÁ ESTÁ NA LISTA" else (if (isModoCasal) "ADICIONAR À LISTA DO CASAL" else "ADICIONAR À MINHA LISTA"),
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerTrailerNativo(
    chaveVideo: String,
    modifier: Modifier = Modifier,
    onFechar: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

    DisposableEffect(chaveVideo) {
        onDispose {
            webViewRef?.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        databaseEnabled = true
                        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    }

                    webChromeClient = android.webkit.WebChromeClient()
                    webViewClient = android.webkit.WebViewClient()

                    val appPackage = ctx.packageName
                    val htmlPlayer = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <meta name="referrer" content="strict-origin-when-cross-origin">
                            <style>
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                html, body { width: 100%; height: 100%; background: #000000; overflow: hidden; }
                                iframe { width: 100%; height: 100%; border: none; }
                            </style>
                        </head>
                        <body>
                            <iframe 
                                src="https://www.youtube.com/embed/$chaveVideo?autoplay=1&playsinline=1&controls=1&rel=0&modestbranding=1&enablejsapi=1" 
                                frameborder="0"
                                referrerpolicy="strict-origin-when-cross-origin"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                allowfullscreen>
                            </iframe>
                        </body>
                        </html>
                    """.trimIndent()

                    loadDataWithBaseURL("https://$appPackage", htmlPlayer, "text/html", "utf-8", null)
                    webViewRef = this
                }
            }
        )

        IconButton(
            onClick = {
                webViewRef?.destroy()
                onFechar()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fechar Player",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun compartilharCardEstilizado(
    contexto: Context,
    coroutineScope: CoroutineScope,
    midia: Midia
) {
    Toast.makeText(contexto, "Gerando card estilizado...", Toast.LENGTH_SHORT).show()

    coroutineScope.launch {
        val bitmap = withContext(Dispatchers.IO) {
            gerarBitmapCardEstilizado(contexto, midia)
        }

        val uriImagem = withContext(Dispatchers.IO) {
            salvarBitmapEmCache(contexto, bitmap)
        }

        val textoProgresso = if (midia.tipo.equals("Filme", ignoreCase = true)) {
            if (midia.status == "Concluído") "já assisti" else "quero assistir"
        } else {
            if (midia.status == "Concluído") "concluí tudo" else "estou na T${midia.temporadaAtual} • Ep ${midia.episodioAtual}"
        }

        val mensagemTexto = """
            🍿 Olha o meu progresso no CineList!
            🎬 *${midia.titulo}* (${midia.tipo})
            📊 Status: ${midia.status} ($textoProgresso)
            ⭐ Avaliação: ${"★".repeat(midia.nota.coerceAtLeast(0))}
            
            Gerenciado pelo app CineList! 🔥
        """.trimIndent()

        val intentCompartilhar = Intent().apply {
            action = Intent.ACTION_SEND
            if (uriImagem != null) {
                type = "image/png"
                clipData = ClipData.newRawUri("card", uriImagem)
                putExtra(Intent.EXTRA_STREAM, uriImagem)
                putExtra(Intent.EXTRA_TEXT, mensagemTexto)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, mensagemTexto)
            }
        }

        val chooserIntent = Intent.createChooser(intentCompartilhar, "Compartilhar card via:").apply {
            if (uriImagem != null) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        contexto.startActivity(chooserIntent)
    }
}

private suspend fun gerarBitmapCardEstilizado(contexto: Context, midia: Midia): Bitmap {
    val largura = 1080
    val altura = 1600
    val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paintFundo = Paint().apply {
        isAntiAlias = true
        shader = android.graphics.LinearGradient(
            0f, 0f, 0f, altura.toFloat(),
            intArrayOf(
                android.graphics.Color.parseColor("#1C1E24"),
                android.graphics.Color.parseColor("#121214"),
                android.graphics.Color.parseColor("#090A0B")
            ),
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, largura.toFloat(), altura.toFloat(), paintFundo)

    val paintHeader = Paint().apply {
        color = android.graphics.Color.parseColor("#FFD700")
        textSize = 54f
        isFakeBoldText = true
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("🍿 CineList", 70f, 130f, paintHeader)

    val paintSubheader = Paint().apply {
        color = android.graphics.Color.parseColor("#A0A5B5")
        textSize = 32f
        isAntiAlias = true
    }
    canvas.drawText("Meu Diário de Cinema & Séries", 70f, 185f, paintSubheader)

    var bitmapPoster: Bitmap? = null
    if (midia.imagemCapa.isNotBlank()) {
        try {
            val requisicao = ImageRequest.Builder(contexto)
                .data(midia.imagemCapa)
                .allowHardware(false)
                .build()
            val resultado = coil.ImageLoader(contexto).execute(requisicao)
            if (resultado is SuccessResult) {
                bitmapPoster = resultado.drawable.toBitmap()
            }
        } catch (_: Exception) {}
    }

    val rectPoster = RectF(70f, 240f, largura - 70f, 1080f)
    val paintPosterBg = Paint().apply {
        color = android.graphics.Color.parseColor("#262933")
        isAntiAlias = true
    }
    canvas.drawRoundRect(rectPoster, 36f, 36f, paintPosterBg)

    if (bitmapPoster != null) {
        val path = android.graphics.Path().apply {
            addRoundRect(rectPoster, 36f, 36f, android.graphics.Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        val srcRect = Rect(0, 0, bitmapPoster.width, bitmapPoster.height)
        canvas.drawBitmap(bitmapPoster, srcRect, rectPoster, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    } else {
        val paintTextoSemCapa = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 42f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("SEM CAPA", rectPoster.centerX(), rectPoster.centerY(), paintTextoSemCapa)
    }

    val paintTitulo = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 58f
        isFakeBoldText = true
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val tituloTruncado = if (midia.titulo.length > 28) midia.titulo.take(28) + "..." else midia.titulo
    canvas.drawText(tituloTruncado, 70f, 1165f, paintTitulo)

    val paintGenero = Paint().apply {
        color = android.graphics.Color.parseColor("#9E9E9E")
        textSize = 34f
        isAntiAlias = true
    }
    canvas.drawText("${midia.tipo} • ${midia.genero}", 70f, 1220f, paintGenero)

    val textoBadge = when {
        midia.tipo.equals("Filme", ignoreCase = true) -> midia.status
        midia.status == "Concluído" -> "Concluído (Tudo Assistido)"
        else -> "${midia.status} • T${midia.temporadaAtual} Ep ${midia.episodioAtual}"
    }

    val paintBadgeBg = Paint().apply {
        color = when (midia.status) {
            "Assistindo" -> android.graphics.Color.parseColor("#00BFFF")
            "Concluído" -> android.graphics.Color.parseColor("#32CD32")
            else -> android.graphics.Color.parseColor("#FFD700")
        }
        isAntiAlias = true
    }
    val rectBadge = RectF(70f, 1260f, 70f + (textoBadge.length * 24f) + 40f, 1335f)
    canvas.drawRoundRect(rectBadge, 20f, 20f, paintBadgeBg)

    val paintBadgeTexto = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 32f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText(textoBadge, 90f, 1310f, paintBadgeTexto)

    val paintEstrelas = Paint().apply {
        color = android.graphics.Color.parseColor("#FFD700")
        textSize = 56f
        isAntiAlias = true
    }
    val estrelas = if (midia.nota > 0) "★".repeat(midia.nota) + "☆".repeat(5 - midia.nota) else "Sem avaliação"
    canvas.drawText(estrelas, 70f, 1420f, paintEstrelas)

    val paintRodape = Paint().apply {
        color = android.graphics.Color.parseColor("#606575")
        textSize = 28f
        isAntiAlias = true
    }
    canvas.drawText("Compartilhado pelo app CineList • Organizando histórias", 70f, 1515f, paintRodape)

    return bitmap
}

private fun salvarBitmapEmCache(contexto: Context, bitmap: Bitmap): Uri? {
    return try {
        val pastaImagens = File(contexto.cacheDir, "shared_images").apply { mkdirs() }
        val arquivo = File(pastaImagens, "cinelist_card_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(arquivo)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.provider",
            arquivo
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}