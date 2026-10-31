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
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.TextUnit
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
    var mostrarDialogoRadar by remember { mutableStateOf(false) }
    var mostrarDialogoWatchParty by remember { mutableStateOf(false) }
    var mostrarDialogoPlaylists by remember { mutableStateOf(false) }

    var atorSelecionadoParaPerfil by remember { mutableStateOf<TmdbAtor?>(null) }

    val detalhesApi by viewModel.detalhesEstendidosApi.collectAsState()
    val provedoresStreaming by viewModel.provedoresStreaming.collectAsState()
    val elencoAtivo by viewModel.elencoMidia.collectAsState()
    val chaveTrailer by viewModel.chaveTrailerYoutube.collectAsState()
    val recomendacoesAtivas by viewModel.recomendacoesMidia.collectAsState()
    val episodiosTmdb by viewModel.episodiosTemporada.collectAsState()
    val carregandoEpisodios by viewModel.carregandoEpisodios.collectAsState()
    val galeriaImagens by viewModel.galeriaImagens.collectAsState()

    val comentariosPublicos by viewModel.comentariosPublicosAtivos.collectAsState(initial = emptyList())

    val tituloDinamico = remember(I18n.idiomaAtivo, detalhesApi?.titulo, midiaSalva?.titulo, midiaSalva?.idTmdb) {
        val tituloApi = detalhesApi?.titulo
        when {
            I18n.idiomaAtivo != "pt-BR" && !tituloApi.isNullOrBlank() -> tituloApi
            midiaSalva != null -> MidiaLocalizationManager.obterTituloLocalizado(midiaSalva)
            !tituloApi.isNullOrBlank() -> tituloApi
            else -> "Título Indisponível".t()
        }
    }

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
    var rewatchContador by remember { mutableIntStateOf(0) }
    var tagsInput by remember { mutableStateOf("") }
    var mostrarTrailerInline by remember { mutableStateOf(false) }
    var estaNoRadar by remember(midiaSalva?.idTmdb, detalhesApi?.id) {
        mutableStateOf(
            RadarStreamingManager.isMonitorado(contexto, midiaSalva?.idTmdb ?: detalhesApi?.id ?: 0)
        )
    }
    var ehPrivadoState by remember { mutableStateOf(false) }

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
            rewatchContador = midiaSalva.contadorRewatch
            tagsInput = midiaSalva.tagsPersonalizadas
            ehPrivadoState = midiaSalva.ehPrivado
        }
    }
    // Preenche automaticamente o género vindo do TMDB quando o item salvo está sem género real
    LaunchedEffect(detalhesApi) {
        val d = detalhesApi
        if (d != null && d.id > 0 && d.titulo.isNotBlank()) {
            MidiaLocalizationManager.salvarNoCache(d.id, d.titulo, d.caminhoPoster.orEmpty())
        }
    }

    LaunchedEffect(detalhesApi?.generoTexto, midiaSalva?.genero) {
        val generoTmdb = detalhesApi?.generoTexto.orEmpty()
        val atual = midiaSalva
        if (atual != null && generoTmdb.isNotBlank() && !generoEhValido(atual.genero)) {
            viewModel.atualizar(atual.copy(genero = generoTmdb))
        }
    }

    var temCenaPosCreditos by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(idRealBuscaApi, tipoRealUtilizado) {
        viewModel.limparDetalhesEstendidos()
        exibindoPlayerNativo = false
        abaTemporadaVisualizada = midiaSalva?.temporadaAtual ?: 1
        if (idRealBuscaApi > 0) {
            viewModel.buscarDetalhesEstendidos(idTmdb = idRealBuscaApi, tipo = tipoRealUtilizado)
            viewModel.buscarOndeAssistir(idTmdb = idRealBuscaApi, tipo = tipoRealUtilizado)
            viewModel.carregarComentariosMidia(idRealBuscaApi)

            if (!ehSerieOuAnime) {
                try {
                    val kw = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.obterKeywordsFilme(idRealBuscaApi)
                    }
                    val listaKw = kw.keywords?.map {
                        it.nome.lowercase().replace(" ", "").replace("-", "").replace("_", "")
                    } ?: emptyList()

                    val temStinger = listaKw.any {
                        it.contains("aftercreditsstinger") ||
                        it.contains("duringcreditsstinger") ||
                        it.contains("postcredits") ||
                        it.contains("midcredits") ||
                        it.contains("aftercredits") ||
                        it.contains("sceneduringcredits") ||
                        it.contains("sceneaftercredits")
                    }

                    val tituloBaixo = (midiaSalva?.titulo ?: detalhesApi?.titulo ?: "").lowercase()
                    val ehFilmeMarvelOuSuperhero = tituloBaixo.contains("vingadores") ||
                            tituloBaixo.contains("avengers") ||
                            tituloBaixo.contains("spider-man") ||
                            tituloBaixo.contains("homem-aranha") ||
                            tituloBaixo.contains("marvel") ||
                            tituloBaixo.contains("batman") ||
                            tituloBaixo.contains("deadpool") ||
                            tituloBaixo.contains("capitão américa") ||
                            tituloBaixo.contains("capitan america") ||
                            tituloBaixo.contains("iron man") ||
                            tituloBaixo.contains("homem de ferro") ||
                            tituloBaixo.contains("thor") ||
                            tituloBaixo.contains("guardiões da galáxia") ||
                            tituloBaixo.contains("guardians of the galaxy") ||
                            listaKw.any { it.contains("marvel") || it.contains("mcu") || it.contains("superhero") || it.contains("dccomics") }

                    temCenaPosCreditos = temStinger || ehFilmeMarvelOuSuperhero
                } catch (e: Exception) {
                    val tituloBaixo = (midiaSalva?.titulo ?: detalhesApi?.titulo ?: "").lowercase()
                    temCenaPosCreditos = tituloBaixo.contains("vingadores") || tituloBaixo.contains("avengers") || tituloBaixo.contains("marvel")
                }
            } else {
                temCenaPosCreditos = null
            }
        }
    }

    LaunchedEffect(idRealBuscaApi, abaTemporadaVisualizada, ehSerieOuAnime) {
        if (idRealBuscaApi > 0 && ehSerieOuAnime) {
            viewModel.buscarEpisodiosTemporada(idRealBuscaApi, abaTemporadaVisualizada)
        }
    }

    var colecaoFranquiaDetalhes by remember { mutableStateOf<TmdbFranquiaColecaoDetalhes?>(null) }

    LaunchedEffect(detalhesApi?.colecaoFranquia?.id) {
        val idCol = detalhesApi?.colecaoFranquia?.id ?: 0
        if (idCol > 0) {
            try {
                colecaoFranquiaDetalhes = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.obterDetalhesColecaoFranquia(idCol)
                }
            } catch (e: Exception) {
                colecaoFranquiaDetalhes = null
            }
        } else {
            colecaoFranquiaDetalhes = null
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

    if (mostrarDialogoRadar) {
        val initCriterios = CriteriosAvaliacao.deStringSerializada(tagsInput) ?: CriteriosAvaliacao()
        DialogoAvaliacaoCriterios(
            criteriosIniciais = initCriterios,
            tituloMidia = midiaSalva?.titulo ?: detalhesApi?.titulo ?: "",
            onSalvar = { novosCriterios ->
                val serializado = novosCriterios.paraStringSerializada()
                val outrasTags = tagsInput.split(";").filter { !it.startsWith("criterios:") }.joinToString(";")
                tagsInput = if (outrasTags.isNotBlank()) "$outrasTags;$serializado" else serializado
                nota = (novosCriterios.media() / 2f).toInt().coerceIn(1, 5)
            },
            onDispensar = { mostrarDialogoRadar = false }
        )
    }

    val membrosSalaDetalhes by viewModel.membrosGrupoAtivo.collectAsState(initial = emptyList())

    if (mostrarDialogoWatchParty) {
        val midiaIdUnico = midiaSalva?.idTmdb ?: detalhesApi?.id ?: 1
        val salaCanonical = if (isModoCasal) "party_${casalIdAtivo}_$midiaIdUnico" else "party_$midiaIdUnico"
        DialogoWatchParty(
            tituloMidia = midiaSalva?.titulo ?: detalhesApi?.titulo ?: "",
            posterMidia = midiaSalva?.imagemCapa ?: detalhesApi?.caminhoPoster ?: "",
            salaIdInicial = salaCanonical,
            membrosSala = membrosSalaDetalhes,
            grupoId = casalIdAtivo,
            viewModel = viewModel,
            onDispensar = { mostrarDialogoWatchParty = false }
        )
    }

    if (mostrarDialogoPlaylists) {
        DialogoPlaylists(
            midiaIdTmdbParaAdicionar = midiaSalva?.idTmdb ?: detalhesApi?.id ?: 0,
            onDispensar = { mostrarDialogoPlaylists = false }
        )
    }



    atorSelecionadoParaPerfil?.let { ator ->
        DialogoPerfilAtor(
            idAtor = ator.idAtor,
            nomeAtorInicial = ator.nomeReal,
            fotoPerfilInicial = ator.urlFotoPerfil,
            onDispensar = { atorSelecionadoParaPerfil = null },
            onMidiaSelecionada = { idTmdb, _, tipo ->
                atorSelecionadoParaPerfil = null
                if (idTmdb > 0) {
                    onRecomendacaoClique(idTmdb, tipo)
                }
            }
        )
    }

    if (mostrarDialogFimTemporada && midiaSalva != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogFimTemporada = false },
            title = { Text("${"Fim da Temporada".t()} $temporadaConcluidaAlvo! 🎬", fontWeight = FontWeight.Bold) },
            text = {
                val textoMsg = if (proximaTemporadaExiste) {
                    "${"Este era o último episódio da Temporada".t()} $temporadaConcluidaAlvo. ${"Deseja avançar para a próxima temporada e manter o status como Assistindo, ou marcar a série inteira como Concluída?".t()}"
                } else {
                    "Este era o último episódio disponível da série. Deseja marcar a série como Concluída?".t()
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
                            Toast.makeText(contexto, "${"Avançando para a Temporada".t()} $temporadaAtual! 🚀", Toast.LENGTH_SHORT).show()
                        } else {
                            statusAtual = StatusMidia.CONCLUIDO
                            if (isModoCasal) viewModel.pararAssistirAgora()
                            Toast.makeText(contexto, "Série marcada como Concluída! 🎉".t(), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (proximaTemporadaExiste) "Avançar Temporada".t() else "Marcar Concluída".t(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
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
                    Text("Marcar Concluída".t(), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    if (mostrarConfirmacaoExclusao && midiaSalva != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoExclusao = false },
            title = { Text("Excluir Mídia".t(), fontWeight = FontWeight.Bold) },
            text = { Text("${"Deseja realmente remover".t()} \"${tituloDinamico}\" ${"da lista?".t()}") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacaoExclusao = false
                        viewModel.deletar(midiaSalva)
                        onVoltar()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                ) {
                    Text("Excluir".t(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoExclusao = false }) {
                    Text("Cancelar".t())
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

    val emModoPip = LocalPipMode.current
    if (emModoPip && !chaveTrailer.isNullOrBlank()) {
        InlineTrailerPlayer(
            chaveTrailer = chaveTrailer ?: "",
            modifier = Modifier.fillMaxSize(),
            onFechar = { mostrarTrailerInline = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopBarModernaTranslucida(
                title = {
                    Text(
                        text = if (isModoCasal) "${"Detalhes".t()} • $nomeSalaAtiva" else "Detalhes da Mídia".t(),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconeBouncyIconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar".t(), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    midiaSalva?.let { midiaReal ->
                        BotaoFavoritoAnimado(
                            favorito = ehFavorito,
                            onToggle = {
                                val novoFavorito = !ehFavorito
                                ehFavorito = novoFavorito
                                CineHaptics.click(contexto)
                                viewModel.atualizar(midiaReal.copy(favorito = novoFavorito))
                            }
                        )

                        IconeBouncyIconButton(onClick = {
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
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Compartilhar Card".t(), tint = MaterialTheme.colorScheme.primary)
                        }

                        val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()
                        if (casalIdAtivo.isNotBlank()) {
                            IconeBouncyIconButton(onClick = {
                                val titLocalizado = MidiaLocalizationManager.obterTituloLocalizado(midiaReal)
                                val payload = "${midiaReal.idTmdb}|||${midiaReal.titulo}|||${midiaReal.imagemCapa}|||${midiaReal.tipo}"
                                viewModel.enviarMensagemParaSala(
                                    texto = "${"Recomendo assistirmos".t()} \"$titLocalizado\"! 🍿",
                                    tipoMensagem = "INDICAÇÃO",
                                    mediaUrl = payload
                                )
                                Toast.makeText(contexto, "Indicação enviada no Chat da Sala! 🍿".t(), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Indicar na Sala".t(),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconeBouncyIconButton(onClick = { mostrarConfirmacaoExclusao = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Deletar".t(), tint = Color(0xFFFF4C4C))
                        }
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(corSuaveAnimada.copy(alpha = 0.4f), MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
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
                                    .width(154.dp)
                                    .height(224.dp)
                                    .align(Alignment.Center)
                                    .sharedPosterElement(if (idRealBuscaApi > 0) "poster_$idRealBuscaApi" else if (id > 0) "poster_$id" else ""),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            Color(0xFF00E5FF).copy(alpha = 0.5f),
                                            Color(0xFFFFD700).copy(alpha = 0.25f),
                                            Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    val urlPosterExibicao = when {
                                        I18n.idiomaAtivo != "pt-BR" && !detalhesApi?.urlPosterVertical.isNullOrBlank() -> detalhesApi?.urlPosterVertical
                                        midiaSalva != null && midiaSalva.imagemCapa.isNotBlank() -> MidiaLocalizationManager.obterCapaLocalizada(midiaSalva)
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



                            Text(text = tituloDinamico, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            val generoExibicao = if (generoEhValido(midiaSalva?.genero)) midiaSalva?.genero.orEmpty() else detalhesApi?.generoTexto.orEmpty()
                            val generosTraduzidos = remember(generoExibicao, I18n.idiomaAtivo) { GeneroLocalizationHelper.formatarGeneros(generoExibicao, I18n.idiomaAtivo) }
                            val tipoTraduzido = tipoRealUtilizado.t()
                            Text(
                                text = if (generosTraduzidos.isBlank()) tipoTraduzido else "$tipoTraduzido • $generosTraduzidos",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )

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

                            val duracaoTotalMin = detalhesApi?.duracaoMinutos ?: midiaSalva?.duracaoTotal ?: 0
                            val horaTerminoTexto = if (duracaoTotalMin > 0) {
                                val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, duracaoTotalMin) }
                                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                "Se começar agora, termina às ${sdf.format(cal.time)}"
                            } else ""

                            if (horaTerminoTexto.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "⏱️ $horaTerminoTexto",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }

                            // 🚀 SELO DE CENAS PÓS-CRÉDITOS (APENAS PARA FILMES)
                            if (!ehSerieOuAnime && temCenaPosCreditos != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                if (temCenaPosCreditos == true) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.MovieFilter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Text("🍿 Cenas Pós-Créditos: Fique até o fim dos créditos!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                            Text("✅ Sem cenas pós-créditos — Pode desligar a TV!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }

                            // 🚀 SELO DE STATUS DA SÉRIE (CANCELADA / FINALIZADA)
                            val statusTextoSelado = detalhesApi?.statusSeloTexto
                            val statusApiStr = detalhesApi?.statusSerieApi.orEmpty()
                            val ehCanceladaSerie = statusApiStr.equals("Canceled", ignoreCase = true)
                            if (ehSerieOuAnime && !statusTextoSelado.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = if (ehCanceladaSerie) Color(0xFFFF4C4C).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (ehCanceladaSerie) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (ehCanceladaSerie) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = statusTextoSelado,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // 🚀 BOTÕES TRILHA SONORA (SPOTIFY & YOUTUBE MUSIC)
                            val tituloParaBuscaOst = midiaSalva?.titulo ?: detalhesApi?.titulo ?: ""
                            if (tituloParaBuscaOst.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val query = Uri.encode("$tituloParaBuscaOst Soundtrack Original OST")
                                            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$query")).apply {
                                                setPackage("com.spotify.music")
                                            }
                                            try {
                                                contexto.startActivity(spotifyIntent)
                                            } catch (ex: Exception) {
                                                val intentWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$query"))
                                                contexto.startActivity(intentWeb)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Spotify 🎵", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val query = Uri.encode("$tituloParaBuscaOst Soundtrack OST")
                                            val ytmIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=$query")).apply {
                                                setPackage("com.google.android.apps.youtube.music")
                                            }
                                            try {
                                                contexto.startActivity(ytmIntent)
                                            } catch (ex: Exception) {
                                                val intentWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=$query"))
                                                contexto.startActivity(intentWeb)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFFF0000), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("YT Music 🎵", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (!chaveTrailer.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))

                                AnimatedVisibility(
                                    visible = mostrarTrailerInline,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        InlineTrailerPlayer(
                                            chaveTrailer = chaveTrailer ?: "",
                                            onFechar = { mostrarTrailerInline = false }
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }

                                Button(
                                    onClick = {
                                        CineHaptics.click(contexto)
                                        mostrarTrailerInline = !mostrarTrailerInline
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (mostrarTrailerInline) Color(0xFF334155) else Color(0xFFE50914)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (mostrarTrailerInline) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (mostrarTrailerInline) "FECHAR TRAILER".t() else "ASSISTIR TRAILER".t(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Botão Adicionar Lembrete na Agenda / Calendário
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clickable {
                                        try {
                                            val t = detalhesApi?.titulo ?: midiaSalva?.titulo ?: "Cinema"
                                            val s = detalhesApi?.sinopse ?: midiaSalva?.sinopse ?: ""
                                            val intent = Intent(Intent.ACTION_INSERT)
                                                .setData(android.provider.CalendarContract.Events.CONTENT_URI)
                                                .putExtra(android.provider.CalendarContract.Events.TITLE, "Assistir: $t 🍿")
                                                .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Salvo pelo CineList: " + s)
                                                .putExtra(android.provider.CalendarContract.Events.ALL_DAY, true)
                                            contexto.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(contexto, "Não foi possível abrir a agenda.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Agendar na Agenda do Celular".t(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                }
                            }

                            if (provedoresFiltrados.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Disponível por Assinatura em:".t(),
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
                                    text = "Galeria de Fotos & Cenas:".t(),
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
                                    text = "Elenco Principal:".t(),
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
                                            modifier = Modifier
                                                .width(76.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { atorSelecionadoParaPerfil = ator }
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

                            val colecaoDetalhes = colecaoFranquiaDetalhes
                            if (colecaoDetalhes != null && !colecaoDetalhes.partes.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "🍿 Saga / Franquia: ${colecaoDetalhes.nome}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(colecaoDetalhes.partes ?: emptyList()) { filmeFranquia ->
                                        val posterFranquia = if (!filmeFranquia.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${filmeFranquia.caminhoPoster}" else ""
                                        val jaEstaNaMinhaLista = listaAtualBanco.any { it.idTmdb != 0 && it.idTmdb == filmeFranquia.idTmdb }

                                        Card(
                                            modifier = Modifier.width(125.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                                                    if (posterFranquia.isNotBlank()) {
                                                        AsyncImage(
                                                            model = posterFranquia,
                                                            contentDescription = filmeFranquia.titulo,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = filmeFranquia.titulo,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Button(
                                                    onClick = {
                                                        if (!jaEstaNaMinhaLista) {
                                                            viewModel.inserir(
                                                                Midia(
                                                                    idTmdb = filmeFranquia.idTmdb,
                                                                    titulo = filmeFranquia.titulo,
                                                                    tipo = "Filme",
                                                                    status = StatusMidia.QUERO_ASSISTIR.valor,
                                                                    nota = 0,
                                                                    sinopse = filmeFranquia.sinopse,
                                                                    imagemCapa = posterFranquia,
                                                                    genero = filmeFranquia.generoTexto,
                                                                    plataforma = "Cinema",
                                                                    isCasal = isModoCasal,
                                                                    casalId = casalIdAtivo
                                                                )
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth().height(28.dp),
                                                    contentPadding = PaddingValues(0.dp),
                                                    enabled = !jaEstaNaMinhaLista,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (jaEstaNaMinhaLista) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Text(if (jaEstaNaMinhaLista) "Na Lista ✅" else "+ Lista", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (ehSerieOuAnime) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(text = "Guia de Episódios:".t(), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "Selecione a Temporada:".t(), color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
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
                                                    label = { Text("${"Temporada".t()} $temp", fontSize = 12.sp) },
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
                                            val totalEpsNestaTemporada = episodiosTmdb.size.coerceAtLeast(1)
                                            val epsAssistidosNestaTemporada = if (abaTemporadaVisualizada < temporadaAtual) {
                                                totalEpsNestaTemporada
                                            } else if (abaTemporadaVisualizada == temporadaAtual) {
                                                episodioAtual.coerceIn(0, totalEpsNestaTemporada)
                                            } else {
                                                0
                                            }
                                            val percentualTemporada = (epsAssistidosNestaTemporada.toFloat() / totalEpsNestaTemporada).coerceIn(0f, 1f)

                                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${"Progresso".t()} T$abaTemporadaVisualizada: $epsAssistidosNestaTemporada/$totalEpsNestaTemporada ${"eps".t()} (${(percentualTemporada * 100).toInt()}%)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )

                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                                                        modifier = Modifier.clickable {
                                                            temporadaAtual = abaTemporadaVisualizada
                                                            episodioAtual = totalEpsNestaTemporada
                                                            if (statusAtual == StatusMidia.QUERO_ASSISTIR || statusAtual == StatusMidia.DESCOBRIR) {
                                                                statusAtual = StatusMidia.ASSISTINDO
                                                            }
                                                            Toast.makeText(contexto, "${"Temporada".t()} $abaTemporadaVisualizada ${"marcada como concluída! 🎉".t()}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "Concluir Temporada ✓".t(),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF10B981),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }

                                                LinearProgressIndicator(
                                                    progress = { percentualTemporada },
                                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                    color = Color(0xFF00E5FF),
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            }
                                            // Gráfico Curva de Avaliação de Episódios (Estilo IMDb)
                                            GraficoCurvaEpisodios(
                                                episodios = episodiosTmdb,
                                                numeroTemporada = abaTemporadaVisualizada,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

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
                                                                CineHaptics.click(contexto)
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
                                                text = "Nenhum detalhe extra de episódios encontrado para esta temporada.".t(),
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
                                    onSalvarAvaliacao = { notaMembro, comentarioMembro, spoiler ->
                                        viewModel.avaliarMidiaNaSala(midiaSalva, notaMembro, comentarioMembro, spoiler)
                                    }
                                )
                            }

                            if (midiaSalva != null) {
                                Spacer(modifier = Modifier.height(20.dp))

                                Text(text = "Sua Nota Geral:".t(), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    repeat(5) { index ->
                                        val estrelaAtiva = index < nota
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (estrelaAtiva) MaterialTheme.colorScheme.primary else Color.DarkGray,
                                            modifier = Modifier.size(36.dp).clickable { CineHaptics.tick(contexto); nota = index + 1 }
                                        )
                                    }
                                }

                                // Botões de Ação Avançada: DNA de Critérios (Radar) & CineParty
                                val criteriosSalvos = remember(tagsInput) { CriteriosAvaliacao.deStringSerializada(tagsInput) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                                        modifier = Modifier.clickable { mostrarDialogoRadar = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                            Text("DNA por Critérios (Radar)".t(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFF007F).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFFFF007F).copy(alpha = 0.6f)),
                                        modifier = Modifier.clickable { mostrarDialogoWatchParty = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("🍿", fontSize = 12.sp)
                                            Text("CineParty Ao Vivo".t(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF007F))
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (estaNoRadar) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF00E5FF).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, if (estaNoRadar) Color(0xFF10B981) else Color(0xFF00E5FF).copy(alpha = 0.6f)),
                                        modifier = Modifier.clickable {
                                            val alerta = AlertaRadarStreaming(
                                                idTmdb = midiaSalva?.idTmdb ?: detalhesApi?.id ?: 0,
                                                titulo = midiaSalva?.titulo ?: detalhesApi?.titulo ?: "",
                                                posterUrl = midiaSalva?.imagemCapa ?: detalhesApi?.caminhoPoster ?: "",
                                                tipo = if (ehSerieOuAnime) "Série" else "Filme",
                                                anoLancamento = detalhesApi?.dataLancamento?.take(4) ?: ""
                                            )
                                            viewModel.alternarAlertaRadar(contexto, alerta) { adicionou ->
                                                estaNoRadar = adicionou
                                                android.widget.Toast.makeText(
                                                    contexto,
                                                    if (adicionou) "🔔 Título adicionado ao Radar! Você será avisado quando estrear nos streamings."
                                                    else "🔔 Removido do Radar de Estreias.",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (estaNoRadar) "🔔" else "📡",
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (estaNoRadar) "No Radar 🔔".t() else "Rastrear Estreia".t(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (estaNoRadar) Color(0xFF10B981) else Color(0xFF00E5FF),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    // Botão Salvar na Playlist Temática
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                                        modifier = Modifier.clickable { mostrarDialogoPlaylists = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("🏷️", fontSize = 12.sp)
                                            Text("Playlist", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                                        }
                                    }

                                    // Botão Compartilhar no Story
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEC4899).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.6f)),
                                        modifier = Modifier.clickable {
                                            val t = midiaSalva?.titulo ?: detalhesApi?.titulo ?: ""
                                            val tp = if (ehSerieOuAnime) "Série" else "Filme"
                                            val an = detalhesApi?.dataLancamento?.take(4) ?: ""
                                            val gen = detalhesApi?.generoTexto ?: midiaSalva?.genero ?: ""
                                            val nt = if (nota > 0) nota else 8
                                            val cmt = if (comentarioPessoalInput.isNotBlank()) comentarioPessoalInput else "Marcado como imperdível no CineList!"
                                            coroutineScope.launch {
                                                android.widget.Toast.makeText(contexto, "Gerando CineCard com poster...", android.widget.Toast.LENGTH_SHORT).show()
                                                val urlPoster = midiaSalva?.imagemCapa?.ifBlank { null }
                                                    ?: detalhesApi?.caminhoPoster?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w780$it" }
                                                    ?: detalhesApi?.caminhoBackdrop?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w780$it" }

                                                var posterBitmap: android.graphics.Bitmap? = null
                                                if (!urlPoster.isNullOrBlank()) {
                                                    try {
                                                        val req = coil.request.ImageRequest.Builder(contexto)
                                                            .data(urlPoster)
                                                            .allowHardware(false)
                                                            .build()
                                                        val result = coil.ImageLoader(contexto).execute(req)
                                                        if (result is coil.request.SuccessResult) {
                                                            posterBitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }

                                                InstagramShareHelper.compartilharCineCardStory(
                                                    context = contexto,
                                                    titulo = t,
                                                    tipo = tp,
                                                    ano = an,
                                                    genero = GeneroLocalizationHelper.formatarGeneros(gen, I18n.idiomaAtivo),
                                                    nota = nt,
                                                    comentario = cmt,
                                                    posterBitmap = posterBitmap
                                                )
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("✨", fontSize = 12.sp)
                                            Text("Story", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                                        }
                                    }

                                }

                                if (criteriosSalvos != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        GraficoRadarCine(
                                            criterios = criteriosSalvos,
                                            modifier = Modifier.size(180.dp).padding(vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("O que achou deste título?".t(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = comentarioPessoalInput,
                                    onValueChange = { comentarioPessoalInput = it },
                                    placeholder = { Text("Escreva a sua análise ou comentário pessoal...".t(), color = MaterialTheme.colorScheme.secondary) },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // 🚀 TAGS E PRIVACIDADE
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = tagsInput,
                                        onValueChange = { tagsInput = it },
                                        placeholder = { Text("#Cinema, #Chorei, #ComOPai", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary) },
                                        label = { Text("Tags Personalizadas".t(), fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    FilterChip(
                                        selected = ehPrivadoState,
                                        onClick = {
                                            ehPrivadoState = !ehPrivadoState
                                            Toast.makeText(contexto, if (ehPrivadoState) "Título Privado 🔒 (Oculto de amigos)" else "Título Público 🔓", Toast.LENGTH_SHORT).show()
                                        },
                                        label = { Text(if (ehPrivadoState) "Privado 🔒".t() else "Público 🔓".t(), fontSize = 11.sp) }
                                    )
                                }

                                // 🚀 CALCULADORA DE MARATONA (Para Séries/Animes) + MODO MAIS UM E CAMA 🛌
                                if (ehSerieOuAnime && detalhesApi != null) {
                                    val totalEps = (detalhesApi?.totalEpisodios ?: 1).coerceAtLeast(1)
                                    val duracao = (detalhesApi?.duracaoMinutos ?: 20).coerceAtLeast(20)
                                    val epsFaltam = maxOf(1, totalEps - episodioAtual)
                                    val horasTotais = (epsFaltam * duracao) / 60
                                    val dias2Ep = (epsFaltam + 1) / 2

                                    val calAgora = java.util.Calendar.getInstance()
                                    val fmtHora = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                    val horaTermino1Ep = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, duracao) }
                                    val horaTermino2Ep = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, duracao * 2) }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("⏱️ Calculadora de Maratona & 'Mais Um e Cama'".t(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = "${"Faltam".t()} $epsFaltam ${"episódios".t()} (~${horasTotais}h ${"de conteúdo".t()}). ${"Se assistir 2 eps por dia, você conclui em".t()} $dias2Ep ${"dias!".t()}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("🛌", fontSize = 14.sp)
                                                Text(
                                                    text = "${"Mais 1 e cama: Se der play agora".t()} (${fmtHora.format(calAgora.time)}), ${"você dorme às".t()} ${fmtHora.format(horaTermino1Ep.time)} ($duracao ${"min".t()}).",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFFBBF24)
                                                )
                                            }
                                            Text(
                                                text = "🌙 ${"Maratona de 2 episódios: término previsto para às".t()} ${fmtHora.format(horaTermino2Ep.time)}.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }

                                // 🚀 DETECTOR DE FRANQUIAS / COLEÇÃO
                                val franquiaObj = detalhesApi?.colecaoFranquia
                                if (franquiaObj != null) {
                                    val franquia = franquiaObj
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("🎬 Franquia / Saga", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                Text(franquia.nome, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Text("Parte da coleção oficial no TMDB", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }
                                }

                                // 🚀 CONTADOR DE REWATCH (Para Concluídos)
                                if (statusAtual == StatusMidia.CONCLUIDO) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Visto ${rewatchContador + 1}x 🍿", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                if (rewatchContador > 0) {
                                                    Text("Reassistido $rewatchContador vez(es)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    rewatchContador++
                                                    Toast.makeText(contexto, "Reassistido! (${rewatchContador + 1}x) 🍿", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("🔄 Reassisti (+1)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(text = "Status Atual da Mídia:".t(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                                            label = { Text(sEnum.valor.t(), fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(text = "Sinopse / Visão Geral:".t(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))

                            val sinopseDinamica = if (I18n.idiomaAtivo != "pt-BR" && !detalhesApi?.sinopse.isNullOrBlank()) {
                                detalhesApi?.sinopse ?: sinopse
                            } else if (midiaSalva != null && sinopse.isNotBlank()) {
                                sinopse
                            } else {
                                detalhesApi?.sinopse ?: "Carregando sinopse...".t()
                            }

                            OutlinedTextField(
                                value = sinopseDinamica,
                                onValueChange = { if (midiaSalva != null) sinopse = it },
                                readOnly = (midiaSalva == null),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                            )

                            // 💡 ANÁLISE CINEAI (SEM SPOILERS)
                            val notaParaAi = if (midiaSalva != null && midiaSalva.nota > 0) midiaSalva.nota.toDouble() else (detalhesApi?.mediaVotos ?: 0.0)
                            val destaquesAi = remember(detalhesApi?.id, midiaSalva?.id) {
                                CineAiManager.gerarDestaquesSemSpoiler(
                                    titulo = if (midiaSalva != null) midiaSalva.titulo else (detalhesApi?.titulo ?: ""),
                                    sinopse = sinopseDinamica,
                                    nota = notaParaAi
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Análise CineAI (Sem Spoilers)".t(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    destaquesAi.forEach { ponto ->
                                        Text(
                                            text = ponto,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // 🛡️ GUIA PARENTAL & ALERTA DE GATILHOS (Opção 8)
                            val guiaParentalInfo = remember(sinopseDinamica, detalhesApi?.generoTexto, midiaSalva?.genero) {
                                GuiaParentalDetector.detectarGuiaParental(
                                    sinopse = sinopseDinamica,
                                    generos = detalhesApi?.generoTexto ?: midiaSalva?.genero ?: "",
                                    tipo = if (ehSerieOuAnime) "Série" else "Filme",
                                    notaCritica = detalhesApi?.mediaVotos ?: (midiaSalva?.nota?.toDouble() ?: 0.0)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            GuiaParentalCard(info = guiaParentalInfo)

                            // 🚀 MUDANÇA NAS RECOMENDAÇÕES (AGORA COM CARROSSEL ELEGANTE E ADIÇÃO RÁPIDA)
                            if (recomendacoesAtivas.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Quem assistiu a este título também gostou:".t(),
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
                                            genero = recomendacao.generoTexto,
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
                                    onEnviarComentario = { texto ->
                                        val mod = ModeracaoConteudoHelper.validarTexto(texto)
                                        if (!mod.aprovado) {
                                            Toast.makeText(contexto, mod.motivoBloqueio, Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.enviarComentarioPublico(idRealBuscaApi, ModeracaoConteudoHelper.sanitizarTexto(texto))
                                        }
                                    },
                                    onCurtirComentario = { idComentario, curtiu -> viewModel.curtirComentarioPublico(idRealBuscaApi, idComentario, curtiu) },
                                    onResponderComentario = { idComentario, resposta ->
                                        val mod = ModeracaoConteudoHelper.validarTexto(resposta)
                                        if (!mod.aprovado) {
                                            Toast.makeText(contexto, mod.motivoBloqueio, Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.responderComentarioPublico(idRealBuscaApi, idComentario, ModeracaoConteudoHelper.sanitizarTexto(resposta))
                                        }
                                    },
                                    onDeletarComentario = { idComentario -> viewModel.deletarComentarioPublico(idRealBuscaApi, idComentario) },
                                    onEditarComentario = { idComentario, novoTexto ->
                                        val mod = ModeracaoConteudoHelper.validarTexto(novoTexto)
                                        if (!mod.aprovado) {
                                            Toast.makeText(contexto, mod.motivoBloqueio, Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.editarComentarioPublico(idRealBuscaApi, idComentario, ModeracaoConteudoHelper.sanitizarTexto(novoTexto))
                                        }
                                    }
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

                                        if (nota > 0 || comentarioPessoalInput.isNotBlank()) {
                                            viewModel.publicarAtividade(
                                                tipo = if (nota > 0) "AVALIOU" else if (concluido) "ASSISTIU" else "RECOMENDACAO",
                                                tmdbId = midiaAtualizada.idTmdb,
                                                tituloMidia = midiaAtualizada.titulo,
                                                posterPath = midiaAtualizada.imagemCapa,
                                                tipoMidia = midiaAtualizada.tipo,
                                                nota = nota.toFloat(),
                                                comentario = comentarioPessoalInput
                                            )
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
                                    Text("SALVAR ALTERAÇÕES".t(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
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
                                            genero = detalhesApi?.generoTexto ?: "",
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
                                        text = if (jaExisteNaLista) "JÁ ESTÁ NA LISTA".t() else (if (isModoCasal) "ADICIONAR À SALA COMPARTILHADA".t() else "ADICIONAR À MINHA LISTA".t()),
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
        Text("O que a comunidade diz:".t(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (comentariosPublicos.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Ainda ninguém comentou sobre este título. Seja o primeiro!".t(),
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
                        Text("${"Ver todos os".t()} ${comentariosPublicos.size} ${"comentários".t()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = novoComentarioInput,
                onValueChange = { novoComentarioInput = it },
                placeholder = { Text("Publicar no fórum...".t(), color = Color.Gray, fontSize = 13.sp) },
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
                Text("Postar".t(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                Text("Todos os Comentários".t(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
    val contexto = LocalContext.current
    val jaCurtiu = comentario.curtidasUids.contains(uidLogado)
    var mostrarDialogExclusao by remember { mutableStateOf(false) }

    var modoEdicao by remember { mutableStateOf(false) }
    var textoEditado by remember { mutableStateOf(comentario.texto) }

    if (mostrarDialogExclusao) {
        AlertDialog(
            onDismissRequest = { mostrarDialogExclusao = false },
            title = { Text("Apagar Comentário".t(), fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente apagar o seu comentário do fórum público?".t()) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletarComentario()
                        mostrarDialogExclusao = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4C4C))
                ) {
                    Text("Apagar".t(), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogExclusao = false }) {
                    Text("Cancelar".t())
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
                                contentDescription = "Editar Comentário".t(),
                                tint = if (modoEdicao) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { mostrarDialogExclusao = true }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Apagar Comentário".t(),
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = {
                            ModeracaoConteudoHelper.denunciarConteudo(
                                tipoConteudo = "comentario_midia",
                                idConteudo = comentario.id,
                                autorConteudoUid = comentario.autorUid,
                                textoConteudo = comentario.texto,
                                motivo = "Comentário impróprio ou spam no título",
                                contexto = contexto
                            )
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("🚩", fontSize = 13.sp)
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
                        Text("Cancelar".t(), color = Color.Gray)
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
                        Text("Salvar".t(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                TextoComProtecaoSpoiler(
                    texto = comentario.texto,
                    ehSpoiler = comentario.contemSpoiler,
                    fontSize = 13.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onCurtir() }.padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (jaCurtiu) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Curtir".t(),
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
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Responder".t(), tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Responder".t(), color = Color.Gray, fontSize = 12.sp)
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
                            placeholder = { Text("Escreva a sua resposta...".t(), fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onEnviarResposta,
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Enviar".t(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ... (O Resto do ficheiro mantém-se igual com o TelaDetalhesSkeleton e o CompartilharCard)
// Diz se o género guardado é um género real (e não um texto "placeholder")
fun generoEhValido(g: String?): Boolean {
    if (g.isNullOrBlank()) return false
    return g.trim().lowercase() !in listOf("geral", "sugerido", "recomendação", "recomendacao", "não informado")
}
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

            val tituloLocalizado = MidiaLocalizationManager.obterTituloLocalizadoPorId(midia.idTmdb, midia.titulo)
            val tituloLimitado = if (tituloLocalizado.length > 25) tituloLocalizado.take(22) + "..." else tituloLocalizado
            canvas.drawText(tituloLimitado, width / 2f, 1350f, paint)
            paint.color = android.graphics.Color.parseColor("#FFD700")
            paint.textSize = 60f
            canvas.drawText("${"Nota".t()}: ${midia.nota} ★", width / 2f, 1480f, paint)
            paint.color = android.graphics.Color.LTGRAY
            paint.textSize = 50f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("${"Status".t()}: ${midia.status.t()}", width / 2f, 1580f, paint)

            if (midia.tipo.equals("Série", ignoreCase = true) || midia.tipo.equals("Anime", ignoreCase = true)) {
                canvas.drawText(formatarTemporadaEpisodio(midia.temporadaAtual, midia.episodioAtual), width / 2f, 1660f, paint)
            }

            if (midia.comentarioPessoal.isNotBlank()) {
                paint.color = android.graphics.Color.parseColor("#E0E0E0")
                paint.textSize = 42f
                val comCurto = if (midia.comentarioPessoal.length > 40) "“${midia.comentarioPessoal.take(37)}...”" else "“${midia.comentarioPessoal}”"
                canvas.drawText(comCurto, width / 2f, 1740f, paint)
            }

            paint.color = android.graphics.Color.DKGRAY
            paint.textSize = 40f
            canvas.drawText("Gerado por CineList 🍿".t(), width / 2f, 1850f, paint)

            val cachePath = File(contexto.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "compartilhar_midia.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.provider", file)

            val instagramIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uri, "image/png")
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra("source_application", contexto.packageName)
                setPackage("com.instagram.android")
            }

            if (instagramIntent.resolveActivity(contexto.packageManager) != null) {
                contexto.startActivity(instagramIntent)
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "${"Dá uma olhada no que estou acompanhando no CineList".t()}: $tituloLocalizado 🍿")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newRawUri("", uri)
                }
                contexto.startActivity(Intent.createChooser(intent, "Compartilhar Card via".t()))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(contexto, "Erro ao gerar card de compartilhamento.".t(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun TextoComProtecaoSpoiler(
    texto: String,
    ehSpoiler: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
    color: Color = Color.LightGray
) {
    var revelado by remember { mutableStateOf(!ehSpoiler) }

    if (ehSpoiler && !revelado) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { revelado = true },
            color = Color(0xFF332211)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Spoiler",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "⚠️ " + "Contém Spoiler — Toque para revelar".t(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
            }
        }
    } else {
        Text(
            text = texto,
            fontSize = fontSize,
            color = color,
            modifier = modifier
        )
    }
}

@Composable
fun SecaoAvaliacoesSala(
    midia: Midia,
    onSalvarAvaliacao: (Int, String, Boolean) -> Unit
) {
    val contexto = LocalContext.current
    var notaMembro by remember { mutableIntStateOf(5) }
    var comentarioMembro by remember { mutableStateOf("") }
    var contemSpoiler by remember { mutableStateOf(false) }

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
                            TextoComProtecaoSpoiler(
                                texto = avaliacao.comentario,
                                ehSpoiler = avaliacao.contemSpoiler,
                                fontSize = 13.sp,
                                color = Color.LightGray
                            )
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
                            .clickable { CineHaptics.tick(contexto); notaMembro = index + 1 }
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { contemSpoiler = !contemSpoiler }
            ) {
                Checkbox(
                    checked = contemSpoiler,
                    onCheckedChange = { contemSpoiler = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF9800))
                )
                Text("Contém Spoiler ⚠️", fontSize = 13.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onSalvarAvaliacao(notaMembro, comentarioMembro, contemSpoiler)
                    comentarioMembro = ""
                    contemSpoiler = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("ENVIAR AVALIAÇÃO", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}