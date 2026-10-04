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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.google.firebase.auth.FirebaseAuth
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
    val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()
    val isModoCasal = casalIdAtivo.isNotBlank()
    val uidLogado = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    val listaAtualBanco by (if (isModoCasal) viewModel.midiasGrupoAtivo else viewModel.midiasPessoais).collectAsState(initial = emptyList())
    val todasAsMidiasbyBanco by viewModel.todasAsMidias.collectAsState(initial = emptyList())

    // 🚀 LÓGICA DE NOME DINÂMICO DA SALA
    val gruposSalvos by viewModel.gruposSalvos.collectAsState(initial = emptyList())
    val nomeSalaAtiva = remember(gruposSalvos, casalIdAtivo) {
        gruposSalvos.find { it.grupoId == casalIdAtivo }?.nomeGrupo?.ifBlank { "Sala Compartilhada" } ?: "Sala Compartilhada"
    }

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

    var mostrarDialogFimTemporada by remember { mutableStateOf(false) }
    var temporadaConcluidaAlvo by remember { mutableIntStateOf(1) }
    var proximaTemporadaExiste by remember { mutableStateOf(false) }

    val detalhesApi by viewModel.detalhesEstendidosApi.collectAsState()
    val provedoresStreaming by viewModel.provedoresStreaming.collectAsState()
    val elencoAtivo by viewModel.elencoMidia.collectAsState()
    val chaveTrailer by viewModel.chaveTrailerYoutube.collectAsState()
    val recomendacoesAtivas by viewModel.recomendacoesMidia.collectAsState()
    val episodiosTmdb by viewModel.episodiosTemporada.collectAsState()
    val carregandoEpisodios by viewModel.carregandoEpisodios.collectAsState()
    val galeriaImagens by viewModel.galeriaImagens.collectAsState()

    val comentariosPublicos by viewModel.comentariosPublicosAtivos.collectAsState(initial = emptyList())

    val isLoadingSkeleton = remember(detalhesApi, midiaSalva, idRealBuscaApi) {
        midiaSalva == null && detalhesApi == null && idRealBuscaApi > 0
    }

    var conteudoVisivel by remember { mutableStateOf(false) }
    LaunchedEffect(isLoadingSkeleton) {
        if (!isLoadingSkeleton) {
            conteudoVisivel = true
        }
    }

    val jaExisteNaLista = remember(idRealBuscaApi, listaAtualBanco) {
        listaAtualBanco.any { (it.idTmdb != 0 && it.idTmdb == idRealBuscaApi) || it.id == idRealBuscaApi }
    }

    val contexto = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun abrirAplicativoStreaming(contexto: Context, nomeProvedor: String) {
        val pacoteApp = when {
            nomeProvedor.contains("Netflix", ignoreCase = true) -> "com.netflix.mediaclient"
            nomeProvedor.contains("Prime Video", ignoreCase = true) || nomeProvedor.contains("Amazon", ignoreCase = true) -> "com.amazon.avod.thirdpartyclient"
            nomeProvedor.contains("Disney", ignoreCase = true) -> "com.disney.disneyplus"
            nomeProvedor.contains("Max", ignoreCase = true) || nomeProvedor.contains("HBO", ignoreCase = true) -> "com.wbd.stream"
            nomeProvedor.contains("Crunchyroll", ignoreCase = true) -> "com.crunchyroll.crunchyroid"
            nomeProvedor.contains("Globoplay", ignoreCase = true) -> "com.globo.globotv"
            nomeProvedor.contains("Apple", ignoreCase = true) -> "com.apple.atv.android.appletv"
            nomeProvedor.contains("Paramount", ignoreCase = true) -> "com.cbs.app"
            else -> null
        }

        var appAberto = false
        if (pacoteApp != null) {
            try {
                val intent = contexto.packageManager.getLaunchIntentForPackage(pacoteApp)
                if (intent != null) {
                    contexto.startActivity(intent)
                    appAberto = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!appAberto) {
            val query = Uri.encode("assistir $nomeProvedor online")
            val intentWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
            try {
                contexto.startActivity(intentWeb)
            } catch (e: Exception) {
                Toast.makeText(contexto, "Não foi possível abrir o link.", Toast.LENGTH_SHORT).show()
            }
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
    var listaCustomizada by remember { mutableStateOf("Geral") }
    var abaTemporadaVisualizada by remember { mutableIntStateOf(1) }
    var ehFavorito by remember { mutableStateOf(false) }
    var comentarioPessoalInput by remember { mutableStateOf("") }

    var statusAtual by remember { mutableStateOf(StatusMidia.QUERO_ASSISTIR) }

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
            statusAtual = midiaSalva.obterStatusEnum()
            listaCustomizada = midiaSalva.listaCustomizada.ifBlank { "Geral" }
            abaTemporadaVisualizada = midiaSalva.temporadaAtual
            ehFavorito = midiaSalva.favorito
            comentarioPessoalInput = midiaSalva.comentarioPessoal
        }
    }

    LaunchedEffect(idRealBuscaApi, tipoRealUtilizado) {
        viewModel.limparDetalhesEstendidos()
        exibindoPlayerNativo = false
        abaTemporadaVisualizada = midiaSalva?.temporadaAtual ?: 1
        if (idRealBuscaApi > 0) {
            viewModel.buscarDetalhesEstendidos(idTmdb = idRealBuscaApi, tipo = tipoRealUtilizado)
            viewModel.buscarOndeAssistir(idTmdb = idRealBuscaApi, tipo = tipoRealUtilizado)
            viewModel.carregarComentariosMidia(idRealBuscaApi)
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
        onDispose {
            viewModel.limparDetalhesEstendidos()
            if (isModoCasal) {
                viewModel.pararAssistirAgora()
            }
        }
    }

    if (mostrarDialogFimTemporada && midiaSalva != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogFimTemporada = false },
            title = { Text("Fim da Temporada $temporadaConcluidaAlvo! 🎬", fontWeight = FontWeight.Bold) },
            text = {
                val textoMsg = if (proximaTemporadaExiste) {
                    "Este era o último episódio da Temporada $temporadaConcluidaAlvo. Deseja avançar para a próxima temporada e manter o status como Assistindo, ou marcar a série inteira como Concluída?"
                } else {
                    "Este era o último episódio disponível da série. Deseja marcar a série como Concluída?"
                }
                Text(textoMsg)
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogFimTemporada = false
                        if (proximaTemporadaExiste) {
                            temporadaAtual = temporadaConcluidaAlvo + 1
                            episodioAtual = 1
                            statusAtual = StatusMidia.ASSISTINDO
                            Toast.makeText(contexto, "Avançando para a Temporada $temporadaAtual! 🚀", Toast.LENGTH_SHORT).show()
                        } else {
                            statusAtual = StatusMidia.CONCLUIDO
                            if (isModoCasal) viewModel.pararAssistirAgora()
                            Toast.makeText(contexto, "Série marcada como Concluída! 🎉", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (proximaTemporadaExiste) "Avançar Temporada" else "Marcar Concluída", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogFimTemporada = false
                        statusAtual = StatusMidia.CONCLUIDO
                        if (isModoCasal) viewModel.pararAssistirAgora()
                    }
                ) {
                    Text("Marcar Concluída", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
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
                title = {
                    Text(
                        text = if (isModoCasal) "Detalhes • $nomeSalaAtiva" else "Detalhes da Mídia",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.onSurface)
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
                                tint = if (ehFavorito) Color(0xFFFF3366) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = {
                            compartilharCardEstilizado(
                                contexto = contexto,
                                coroutineScope = coroutineScope,
                                midia = midiaReal.copy(
                                    nota = nota,
                                    status = statusAtual.valor,
                                    temporadaAtual = temporadaAtual,
                                    episodioAtual = episodioAtual
                                )
                            )
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Compartilhar Card", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(onClick = { mostrarConfirmacaoExclusao = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Deletar", tint = Color(0xFFFF4C4C))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(corSuaveAnimada.copy(alpha = 0.4f), MaterialTheme.colorScheme.background),
                        endY = 1200f
                    )
                )
        ) {
            if (isLoadingSkeleton) {
                TelaDetalhesSkeleton()
            } else {
                AnimatedVisibility(
                    visible = conteudoVisivel,
                    enter = fadeIn(animationSpec = tween(durationMillis = 400)) +
                            slideInVertically(animationSpec = tween(durationMillis = 400), initialOffsetY = { it / 8 })
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                                            colors = listOf(Color.Transparent, Color(0xAA121212), MaterialTheme.colorScheme.background),
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
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

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Spacer(modifier = Modifier.height(8.dp))

                            val tituloDinamico = midiaSalva?.titulo ?: detalhesApi?.titulo ?: "Título Indisponível"

                            Text(text = tituloDinamico, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "$tipoRealUtilizado • ${midiaSalva?.genero ?: detalhesApi?.generoTexto ?: "Geral"}", fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)

                            if (detalhesApi != null && !detalhesApi?.fraseEfeito.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"${detalhesApi?.fraseEfeito}\"",
                                    fontSize = 14.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!ehSerieOuAnime) {
                                Button(
                                    onClick = {
                                        val query = Uri.encode("cinemas e ingressos para $tituloDinamico")
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                                        contexto.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Consultar Sessões e Ingressos 🎟️", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        Text("Lançamento", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp)
                                        Text(ano, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Duração Real", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp)
                                        Text(duracao, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Média Crítica", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp)
                                        Text(notaPublico, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ASSISTIR TRAILER NO YOUTUBE",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            if (provedoresFiltrados.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Disponível por Assinatura em:",
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                                color = MaterialTheme.colorScheme.secondary,
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
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = ator.nomeReal,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Text(
                                                text = ator.nomePersonagem,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.secondary,
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
                                Text(text = "Guia de Episódios:", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "Selecione a Temporada:", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
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
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        labelColor = MaterialTheme.colorScheme.secondary
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
                                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
                                                                if (statusAtual == StatusMidia.QUERO_ASSISTIR || statusAtual == StatusMidia.DESCOBRIR) {
                                                                    statusAtual = StatusMidia.ASSISTINDO
                                                                }

                                                                val ultimoEpisodioDaLista = episodiosTmdb.maxOfOrNull { it.numeroEpisodio } ?: epItem.numeroEpisodio
                                                                if (epItem.numeroEpisodio >= ultimoEpisodioDaLista) {
                                                                    temporadaConcluidaAlvo = abaTemporadaVisualizada
                                                                    proximaTemporadaExiste = (temporadaConcluidaAlvo < totalTempDisponiveis)
                                                                    mostrarDialogFimTemporada = true
                                                                }
                                                            },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (ehOAtual) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
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
                                                                    color = if (ehOAtual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                if (epItem.sinopse.isNotBlank()) {
                                                                    Text(
                                                                        text = epItem.sinopse,
                                                                        fontSize = 11.sp,
                                                                        color = MaterialTheme.colorScheme.secondary,
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
                                                                        if (ehOAtual) MaterialTheme.colorScheme.primary
                                                                        else if (jaAssistido) Color(0x664CAF50)
                                                                        else MaterialTheme.colorScheme.surface
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (jaAssistido || ehOAtual) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Check,
                                                                        contentDescription = "Assistido",
                                                                        tint = if (ehOAtual) MaterialTheme.colorScheme.onPrimary else Color.White,
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
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (midiaSalva != null && isModoCasal) {
                                Spacer(modifier = Modifier.height(20.dp))
                                SecaoAvaliacoesSala(
                                    midia = midiaSalva,
                                    onSalvarAvaliacao = { notaMembro, comentarioMembro ->
                                        viewModel.avaliarMidiaNaSala(midiaSalva, notaMembro, comentarioMembro)
                                    }
                                )
                            }

                            if (midiaSalva != null) {
                                Spacer(modifier = Modifier.height(20.dp))

                                Text(text = "Sua Nota Geral:", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    repeat(5) { index ->
                                        val estrelaAtiva = index < nota
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (estrelaAtiva) MaterialTheme.colorScheme.primary else Color.DarkGray,
                                            modifier = Modifier.size(36.dp).clickable { nota = index + 1 }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("O que achou deste título?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = comentarioPessoalInput,
                                    onValueChange = { comentarioPessoalInput = it },
                                    placeholder = { Text("Escreva a sua análise ou comentário pessoal...", color = MaterialTheme.colorScheme.secondary) },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(text = "Status Atual da Mídia:", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(StatusMidia.QUERO_ASSISTIR, StatusMidia.ASSISTINDO, StatusMidia.CONCLUIDO).forEach { sEnum ->
                                        FilterChip(
                                            selected = (statusAtual == sEnum),
                                            onClick = {
                                                statusAtual = sEnum
                                                if (sEnum == StatusMidia.CONCLUIDO && isModoCasal) {
                                                    viewModel.pararAssistirAgora()
                                                }
                                            },
                                            label = { Text(sEnum.valor, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(text = "Coleção Temática / Lista:", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(text = "Sinopse / Visão Geral:", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))

                            val sinopseDinamica = if (midiaSalva != null && sinopse.isNotBlank()) sinopse else (detalhesApi?.sinopse ?: "Carregando sinopse...")

                            OutlinedTextField(
                                value = sinopseDinamica,
                                onValueChange = { if (midiaSalva != null) sinopse = it },
                                readOnly = (midiaSalva == null),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                            )

                            // 🚀 MUDANÇA NAS RECOMENDAÇÕES (AGORA COM CARROSSEL ELEGANTE E ADIÇÃO RÁPIDA)
                            if (recomendacoesAtivas.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Quem assistiu a este título também gostou:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    itemsIndexed(recomendacoesAtivas.take(15)) { _, recomendacao ->
                                        val tipoRecomendacao = when {
                                            recomendacao.mediaType.equals("tv", ignoreCase = true) -> "Série"
                                            recomendacao.mediaType.equals("movie", ignoreCase = true) -> "Filme"
                                            recomendacao.ehSerie -> "Série"
                                            tipoRealUtilizado.equals("Série", ignoreCase = true) -> "Série"
                                            else -> "Filme"
                                        }

                                        val urlPoster = if (!recomendacao.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${recomendacao.caminhoPoster}" else ""
                                        val plataformaFinalRec = if (tipoRecomendacao == "Série") "TV / Original" else "Cinema"

                                        // Criação provisória de uma "Midia" para renderizar no ItemMidiaCard
                                        val midiaRecCard = Midia(
                                            idTmdb = recomendacao.idTmdb,
                                            titulo = recomendacao.titulo,
                                            tipo = tipoRecomendacao,
                                            status = StatusMidia.DESCOBRIR.valor,
                                            nota = 0,
                                            sinopse = "",
                                            imagemCapa = urlPoster,
                                            genero = "Sugerido",
                                            plataforma = plataformaFinalRec,
                                            favorito = false,
                                            listaCustomizada = "Geral"
                                        )

                                        val jaNaListaRec = listaAtualBanco.any { it.idTmdb != 0 && it.idTmdb == recomendacao.idTmdb }

                                        Box(modifier = Modifier.width(130.dp)) {
                                            ItemMidiaCard(
                                                midia = midiaRecCard,
                                                onClick = {
                                                    viewModel.limparDetalhesEstendidos()
                                                    onRecomendacaoClique(recomendacao.idTmdb, tipoRecomendacao)
                                                },
                                                jaAdicionado = jaNaListaRec,
                                                onAdicionarRapido = {
                                                    val novaMidiaRec = Midia(
                                                        id = 0,
                                                        idTmdb = recomendacao.idTmdb,
                                                        titulo = recomendacao.titulo,
                                                        tipo = tipoRecomendacao,
                                                        nota = 0,
                                                        temporadaAtual = 1,
                                                        episodioAtual = 1,
                                                        minutoParado = 0,
                                                        jaEncerrou = false,
                                                        status = StatusMidia.QUERO_ASSISTIR.valor,
                                                        sinopse = "",
                                                        imagemCapa = urlPoster,
                                                        genero = "Recomendação",
                                                        plataforma = plataformaFinalRec,
                                                        favorito = false,
                                                        listaCustomizada = "Geral",
                                                        isCasal = isModoCasal,
                                                        casalId = casalIdAtivo
                                                    )
                                                    viewModel.inserir(novaMidiaRec)
                                                    Toast.makeText(contexto, "Adicionado à lista!", Toast.LENGTH_SHORT).show()
                                                },
                                                modoListaHorizontal = false
                                            )
                                        }
                                    }
                                }
                            }

                            if (idRealBuscaApi > 0) {
                                Spacer(modifier = Modifier.height(24.dp))
                                SecaoComunidade(
                                    tmdbId = idRealBuscaApi,
                                    comentariosPublicos = comentariosPublicos,
                                    uidLogado = uidLogado,
                                    onEnviarComentario = { texto -> viewModel.enviarComentarioPublico(idRealBuscaApi, texto) },
                                    onCurtirComentario = { idComentario, curtiu -> viewModel.curtirComentarioPublico(idRealBuscaApi, idComentario, curtiu) },
                                    onResponderComentario = { idComentario, resposta -> viewModel.responderComentarioPublico(idRealBuscaApi, idComentario, resposta) },
                                    onDeletarComentario = { idComentario -> viewModel.deletarComentarioPublico(idRealBuscaApi, idComentario) },
                                    onEditarComentario = { idComentario, novoTexto -> viewModel.editarComentarioPublico(idRealBuscaApi, idComentario, novoTexto) }
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (midiaSalva != null && isModoCasal) {
                                Button(
                                    onClick = {
                                        statusAtual = StatusMidia.ASSISTINDO
                                        val midiaAtualizada = midiaSalva.copy(
                                            temporadaAtual = temporadaAtual,
                                            episodioAtual = episodioAtual,
                                            status = StatusMidia.ASSISTINDO.valor,
                                            jaEncerrou = false
                                        )
                                        viewModel.atualizar(midiaAtualizada)
                                        viewModel.iniciarAssistirMidiaAgora(midiaAtualizada)
                                        Toast.makeText(contexto, "Transmitindo e alterando status para 'Assistindo'! 🎬", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (ehSerieOuAnime) "ASSISTIR T$temporadaAtual • EP $episodioAtual AGORA (SALA)" else "ASSISTINDO FILME AGORA (SALA)",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiary,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (midiaSalva != null) {
                                Button(
                                    onClick = {
                                        val streamingAtualizado = provedoresFiltrados.firstOrNull()?.nomeProvedor
                                            ?: provedoresStreaming.firstOrNull()?.nomeProvedor
                                            ?: midiaSalva.plataforma

                                        val concluido = statusAtual == StatusMidia.CONCLUIDO
                                        val querAssistir = statusAtual == StatusMidia.QUERO_ASSISTIR

                                        val midiaAtualizada = midiaSalva.copy(
                                            nota = nota,
                                            temporadaAtual = temporadaAtual,
                                            episodioAtual = episodioAtual,
                                            jaEncerrou = concluido,
                                            status = statusAtual.valor,
                                            sinopse = sinopse,
                                            favorito = ehFavorito,
                                            listaCustomizada = listaCustomizada.ifBlank { "Geral" },
                                            plataforma = streamingAtualizado,
                                            isCasal = isModoCasal,
                                            casalId = casalIdAtivo,
                                            comentarioPessoal = comentarioPessoalInput
                                        )
                                        viewModel.atualizar(midiaAtualizada)

                                        if (comentarioPessoalInput.isNotBlank()) {
                                            viewModel.enviarComentarioPublico(idRealBuscaApi, comentarioPessoalInput)
                                        }

                                        if (statusAtual == StatusMidia.ASSISTINDO && isModoCasal) {
                                            viewModel.iniciarAssistirMidiaAgora(midiaAtualizada)
                                        } else if (concluido || querAssistir) {
                                            viewModel.pararAssistirAgora()
                                        }

                                        onVoltar()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("SALVAR ALTERAÇÕES", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val streamingPrincipal = provedoresFiltrados.firstOrNull()?.nomeProvedor
                                            ?: provedoresStreaming.firstOrNull()?.nomeProvedor
                                            ?: (if (ehSerieOuAnime) "TV / Original" else "Cinema")

                                        val tituloParaSalvar = detalhesApi?.titulo ?: "Título"
                                        val capaParaSalvar = if (!detalhesApi?.urlPosterVertical.isNullOrBlank()) detalhesApi?.urlPosterVertical ?: "" else ""
                                        val sinopseParaSalvar = detalhesApi?.sinopse ?: ""

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
                                            status = StatusMidia.QUERO_ASSISTIR.valor,
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
                                        val mensagemToast = if (isModoCasal) "Adicionado à sala: $nomeSalaAtiva 🍿!" else "Adicionado à sua lista!"
                                        Toast.makeText(contexto, mensagemToast, Toast.LENGTH_SHORT).show()
                                        onVoltar()
                                    },
                                    enabled = !jaExisteNaLista,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (jaExisteNaLista) "JÁ ESTÁ NA LISTA" else (if (isModoCasal) "ADICIONAR À SALA COMPARTILHADA" else "ADICIONAR À MINHA LISTA"),
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
        }
    }
}

// 🚀 FÓRUM / COMUNIDADE MANTIDO INTACTO
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecaoComunidade(
    tmdbId: Int,
    comentariosPublicos: List<ComentarioSocial>,
    uidLogado: String,
    onEnviarComentario: (String) -> Unit,
    onCurtirComentario: (String, Boolean) -> Unit,
    onResponderComentario: (String, String) -> Unit,
    onDeletarComentario: (String) -> Unit,
    onEditarComentario: (String, String) -> Unit
) {
    var novoComentarioInput by remember { mutableStateOf("") }
    var mostrarBottomSheet by remember { mutableStateOf(false) }

    var responderAoComentarioId by remember { mutableStateOf<String?>(null) }
    var respostaInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("O que a comunidade diz:", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (comentariosPublicos.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Ainda ninguém comentou sobre este título. Seja o primeiro!",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                comentariosPublicos.take(3).forEach { comentario ->
                    ItemComentarioSocial(
                        comentario = comentario,
                        uidLogado = uidLogado,
                        responderAoComentarioId = responderAoComentarioId,
                        respostaInput = respostaInput,
                        onCurtir = { onCurtirComentario(comentario.id, !comentario.curtidasUids.contains(uidLogado)) },
                        onAlternarResponder = { responderAoComentarioId = if (responderAoComentarioId == comentario.id) null else comentario.id },
                        onRespostaChange = { respostaInput = it },
                        onEnviarResposta = {
                            onResponderComentario(comentario.id, respostaInput)
                            respostaInput = ""
                            responderAoComentarioId = null
                        },
                        onDeletarComentario = { onDeletarComentario(comentario.id) },
                        onEditarComentario = { novoTexto -> onEditarComentario(comentario.id, novoTexto) }
                    )
                }

                if (comentariosPublicos.size > 3) {
                    TextButton(
                        onClick = { mostrarBottomSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver todos os ${comentariosPublicos.size} comentários", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = novoComentarioInput,
                onValueChange = { novoComentarioInput = it },
                placeholder = { Text("Publicar no fórum...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (novoComentarioInput.isNotBlank()) {
                        onEnviarComentario(novoComentarioInput)
                        novoComentarioInput = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Postar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (mostrarBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { mostrarBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.background,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Todos os Comentários", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxHeight(0.8f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(comentariosPublicos.size) { index ->
                        val comentario = comentariosPublicos[index]
                        ItemComentarioSocial(
                            comentario = comentario,
                            uidLogado = uidLogado,
                            responderAoComentarioId = responderAoComentarioId,
                            respostaInput = respostaInput,
                            onCurtir = { onCurtirComentario(comentario.id, !comentario.curtidasUids.contains(uidLogado)) },
                            onAlternarResponder = { responderAoComentarioId = if (responderAoComentarioId == comentario.id) null else comentario.id },
                            onRespostaChange = { respostaInput = it },
                            onEnviarResposta = {
                                onResponderComentario(comentario.id, respostaInput)
                                respostaInput = ""
                                responderAoComentarioId = null
                            },
                            onDeletarComentario = { onDeletarComentario(comentario.id) },
                            onEditarComentario = { novoTexto -> onEditarComentario(comentario.id, novoTexto) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemComentarioSocial(
    comentario: ComentarioSocial,
    uidLogado: String,
    responderAoComentarioId: String?,
    respostaInput: String,
    onCurtir: () -> Unit,
    onAlternarResponder: () -> Unit,
    onRespostaChange: (String) -> Unit,
    onEnviarResposta: () -> Unit,
    onDeletarComentario: () -> Unit,
    onEditarComentario: (String) -> Unit
) {
    val jaCurtiu = comentario.curtidasUids.contains(uidLogado)
    var mostrarDialogExclusao by remember { mutableStateOf(false) }

    var modoEdicao by remember { mutableStateOf(false) }
    var textoEditado by remember { mutableStateOf(comentario.texto) }

    if (mostrarDialogExclusao) {
        AlertDialog(
            onDismissRequest = { mostrarDialogExclusao = false },
            title = { Text("Apagar Comentário", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente apagar o seu comentário do fórum público?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletarComentario()
                        mostrarDialogExclusao = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                ) {
                    Text("Apagar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogExclusao = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (comentario.autorFoto.isNotBlank()) {
                            AsyncImage(
                                model = comentario.autorFoto,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(comentario.autorNome.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(comentario.autorNome, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (comentario.autorUid == uidLogado) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { modoEdicao = !modoEdicao }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Comentário",
                                tint = if (modoEdicao) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { mostrarDialogExclusao = true }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Apagar Comentário",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (modoEdicao) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textoEditado,
                    onValueChange = { textoEditado = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF333333)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        modoEdicao = false
                        textoEditado = comentario.texto
                    }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (textoEditado.isNotBlank() && textoEditado != comentario.texto) {
                                onEditarComentario(textoEditado)
                            }
                            modoEdicao = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Salvar", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(comentario.texto, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onCurtir() }.padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (jaCurtiu) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Curtir",
                            tint = if (jaCurtiu) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${comentario.curtidasUids.size}", color = if (jaCurtiu) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onAlternarResponder() }.padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = "Responder", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Responder", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                if (comentario.respostas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .border(width = 1.dp, color = Color(0xFF333333), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        comentario.respostas.forEach { resposta ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (resposta.autorFoto.isNotBlank()) {
                                        AsyncImage(
                                            model = resposta.autorFoto,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(resposta.autorNome, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(resposta.texto, color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = responderAoComentarioId == comentario.id) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = respostaInput,
                            onValueChange = onRespostaChange,
                            placeholder = { Text("Escreva a sua resposta...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onEnviarResposta,
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Enviar", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ... (O Resto do ficheiro mantém-se igual com o TelaDetalhesSkeleton e o CompartilharCard)

@Composable
fun TelaDetalhesSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_detalhes")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_shimmer"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color.DarkGray.copy(alpha = alphaAnim)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(220.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Gray.copy(alpha = alphaAnim))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = alphaAnim))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = alphaAnim))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = alphaAnim))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = alphaAnim))
            )
        }
    }
}

fun compartilharCardEstilizado(
    contexto: Context,
    coroutineScope: CoroutineScope,
    midia: Midia
) {
    coroutineScope.launch(Dispatchers.IO) {
        try {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = android.graphics.Color.parseColor("#121212")
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            var coverBitmap: Bitmap? = null
            if (midia.imagemCapa.isNotBlank()) {
                val request = ImageRequest.Builder(contexto)
                    .data(midia.imagemCapa)
                    .allowHardware(false)
                    .build()
                val result = coil.ImageLoader(contexto).execute(request)
                if (result is SuccessResult) {
                    coverBitmap = result.drawable.toBitmap()
                }
            }

            if (coverBitmap != null) {
                val rectF = RectF(140f, 200f, 940f, 1200f)
                canvas.drawBitmap(coverBitmap, null, rectF, paint)
            } else {
                paint.color = android.graphics.Color.parseColor("#2A2A2A")
                val rectF = RectF(140f, 200f, 940f, 1200f)
                canvas.drawRoundRect(rectF, 40f, 40f, paint)
            }

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 80f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER

            val tituloLimitado = if (midia.titulo.length > 25) midia.titulo.take(22) + "..." else midia.titulo
            canvas.drawText(tituloLimitado, width / 2f, 1350f, paint)
            paint.color = android.graphics.Color.parseColor("#FFD700")
            paint.textSize = 60f
            canvas.drawText("Nota: ${midia.nota} ★", width / 2f, 1480f, paint)
            paint.color = android.graphics.Color.LTGRAY
            paint.textSize = 50f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Status: ${midia.status}", width / 2f, 1580f, paint)

            if (midia.tipo.equals("Série", ignoreCase = true) || midia.tipo.equals("Anime", ignoreCase = true)) {
                canvas.drawText("Temporada ${midia.temporadaAtual} • Ep ${midia.episodioAtual}", width / 2f, 1680f, paint)
            }
            paint.color = android.graphics.Color.DKGRAY
            paint.textSize = 40f
            canvas.drawText("Gerado por CineList", width / 2f, 1850f, paint)

            val cachePath = File(contexto.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "compartilhar_midia.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Dá uma olhada no que estou acompanhando: ${midia.titulo} 🎬")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("", uri)
            }
            contexto.startActivity(Intent.createChooser(intent, "Compartilhar Card via"))
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(contexto, "Erro ao gerar card de compartilhamento.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun SecaoAvaliacoesSala(
    midia: Midia,
    onSalvarAvaliacao: (Int, String) -> Unit
) {
    var notaMembro by remember { mutableIntStateOf(5) }
    var comentarioMembro by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Avaliações da Sala", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (midia.avaliacoesGrupo.isEmpty()) {
                Text("Nenhuma avaliação ainda.", color = Color.Gray, fontSize = 14.sp)
            } else {
                midia.avaliacoesGrupo.forEach { (_, avaliacao) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(avaliacao.autorNome, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${avaliacao.nota}/5 ★", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }
                        if (avaliacao.comentario.isNotBlank()) {
                            Text(avaliacao.comentario, color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Sua Avaliação", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                repeat(5) { index ->
                    val estrelaAtiva = index < notaMembro
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (estrelaAtiva) MaterialTheme.colorScheme.primary else Color.DarkGray,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { notaMembro = index + 1 }
                    )
                }
            }

            OutlinedTextField(
                value = comentarioMembro,
                onValueChange = { comentarioMembro = it },
                label = { Text("Comentário (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onSalvarAvaliacao(notaMembro, comentarioMembro)
                    comentarioMembro = ""
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("ENVIAR AVALIAÇÃO", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}