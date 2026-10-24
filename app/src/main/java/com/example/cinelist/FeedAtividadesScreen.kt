package com.example.cinelist

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * =========================================================================
 * 💬 CINEFÓRUM & FEED SOCIAL DA COMUNIDADE (CineList 2.0)
 * =========================================================================
 * Um fórum social interativo completo onde cinéfilos abrem debates,
 * compartilham teorias, deixam reviews, interagem com likes, reações
 * e comentam em tempo real nos tópicos de filmes e séries.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedAtividadesScreen(
    viewModel: MidiaViewModel,
    onMidiaClick: (Int, String, String) -> Unit, // idTmdb, titulo, tipo
    onAdicionarAmigoClick: () -> Unit
) {
    val contexto = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val feed by viewModel.feedAtividades.collectAsState(initial = emptyList())
    val amigos by viewModel.amigosConectados.collectAsState(initial = emptyList())
    val midiasPessoais by viewModel.midiasPessoais.collectAsState(initial = emptyList())
    val meuUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    var mostrarModalCriarTopico by remember { mutableStateOf(false) }
    var topicoSelecionadoParaComentarios by remember { mutableStateOf<AtividadeSocial?>(null) }

    var textoPesquisa by remember { mutableStateOf("") }
    var categoriaSelecionada by remember { mutableStateOf("Todos") }

    val uidsAmigos = remember(amigos) { amigos.map { it.uid }.toSet() }

    val feedFiltrado = remember(feed, categoriaSelecionada, textoPesquisa, uidsAmigos, meuUid) {
        feed.filter { post ->
            // Filtro por categoria
            val matchCategoria = when (categoriaSelecionada) {
                "Amigos" -> post.autorUid in uidsAmigos || post.autorUid == meuUid
                "Discussões" -> post.tipo == "DISCUSSAO" || post.categoria.contains("Discussão", ignoreCase = true)
                "Reviews" -> post.tipo == "AVALIOU" || post.categoria.contains("Crítica", ignoreCase = true) || post.categoria.contains("Review", ignoreCase = true)
                "Teorias" -> post.tipo == "TEORIA" || post.categoria.contains("Teoria", ignoreCase = true)
                "Recomendações" -> post.tipo == "RECOMENDACAO" || post.categoria.contains("Recomenda", ignoreCase = true)
                "Assistidos" -> post.tipo == "ASSISTIU"
                else -> true
            }

            // Filtro por texto de pesquisa
            val matchPesquisa = if (textoPesquisa.isBlank()) true else {
                val q = textoPesquisa.trim().lowercase()
                post.topicoTitulo.lowercase().contains(q) ||
                post.tituloMidia.lowercase().contains(q) ||
                post.comentario.lowercase().contains(q) ||
                post.autorNome.lowercase().contains(q) ||
                post.categoria.lowercase().contains(q)
            }

            matchCategoria && matchPesquisa
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // =========================================================================
        // 1. BANNER & COMPOSITOR DO CINEFÓRUM
        // =========================================================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF131826),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF00E5FF).copy(alpha = 0.5f), Color(0xFFFFD700).copy(alpha = 0.3f), Color(0xFFA855F7).copy(alpha = 0.4f))
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬", fontSize = 16.sp)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "CINEFÓRUM",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF00FF7F).copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, Color(0xFF00FF7F))
                                ) {
                                    Text(
                                        text = "AO VIVO",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FF7F),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Debates, teorias, análises e dicas de filmes e séries",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Botão de Nova Discussão
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        mostrarModalCriarTopico = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ABRIR NOVA DISCUSSÃO NO FÓRUM ✍️",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // =========================================================================
        // 2. BARRA DE PESQUISA & FILTROS DE CATEGORIA
        // =========================================================================
        OutlinedTextField(
            value = textoPesquisa,
            onValueChange = { textoPesquisa = it },
            placeholder = { Text("Pesquisar debates, filmes ou cinéfilos...", fontSize = 12.sp, color = Color(0xFF64748B)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (textoPesquisa.isNotBlank()) {
                    IconButton(onClick = { textoPesquisa = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F1526),
                unfocusedContainerColor = Color(0xFF0F1526),
                focusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .height(48.dp)
        )

        // Chips Horizontais de Filtro
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categorias = listOf("Todos", "💬 Discussões", "⭐ Reviews", "🧠 Teorias", "🔥 Recomendações", "👥 Amigos")
            items(categorias) { cat ->
                val chave = cat.replace("💬 ", "").replace("⭐ ", "").replace("🧠 ", "").replace("🔥 ", "").replace("👥 ", "")
                val selecionado = categoriaSelecionada == chave
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selecionado) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                    border = BorderStroke(
                        1.dp,
                        if (selecionado) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.clickable { categoriaSelecionada = chave }
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
                        color = if (selecionado) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // =========================================================================
        // 3. LISTA DE TÓPICOS & POSTS DO FÓRUM
        // =========================================================================
        if (feedFiltrado.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎬", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (textoPesquisa.isNotBlank()) "Nenhuma discussão encontrada" else "O Fórum está esperando por você!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (textoPesquisa.isNotBlank()) "Tente buscar por outro termo ou limpe os filtros." else "Inicie um debate sobre o seu filme favorito ou compartilhe uma teoria intrigante!",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { mostrarModalCriarTopico = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddComment, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Iniciar Debate 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(feedFiltrado, key = { index, item -> item.id.ifBlank { "${item.autorUid}_${item.timestamp}_$index" } }) { _, atividade ->
                    val jaTenhoSalvo = remember(midiasPessoais, atividade.tmdbId, atividade.tituloMidia) {
                        midiasPessoais.any {
                            (atividade.tmdbId != 0 && it.idTmdb == atividade.tmdbId) ||
                            it.titulo.equals(atividade.tituloMidia, ignoreCase = true)
                        }
                    }

                    CardTopicoForum(
                        atividade = atividade,
                        meuUid = meuUid,
                        estaNaMinhaLista = jaTenhoSalvo,
                        onMidiaClick = {
                            if (atividade.tmdbId != 0 || atividade.tituloMidia.isNotBlank()) {
                                onMidiaClick(atividade.tmdbId, atividade.tituloMidia, atividade.tipoMidia)
                            }
                        },
                        onQueroAssistirTambem = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val nova = Midia(
                                idTmdb = atividade.tmdbId,
                                titulo = atividade.tituloMidia,
                                tipo = atividade.tipoMidia.ifBlank { "Filme" },
                                imagemCapa = atividade.posterPath,
                                status = StatusMidia.QUERO_ASSISTIR.valor,
                                isCasal = false
                            )
                            viewModel.inserir(nova)
                            Toast.makeText(contexto, "'${atividade.tituloMidia}' adicionado à sua lista!", Toast.LENGTH_SHORT).show()
                        },
                        onAlternarLike = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.alternarLikeAtividade(atividade.id)
                        },
                        onReagir = { emoji ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.alternarReacaoAtividade(atividade.id, emoji)
                        },
                        onAbrirComentarios = {
                            topicoSelecionadoParaComentarios = atividade
                        },
                        onDeletar = {
                            viewModel.deletarAtividade(atividade.id)
                            Toast.makeText(contexto, "Discussão excluída.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // =========================================================================
    // MODAL DE CRIAÇÃO DE NOVA DISCUSSÃO / TÓPICO
    // =========================================================================
    if (mostrarModalCriarTopico) {
        DialogoCriarTopicoForum(
            midiasSugeridas = midiasPessoais,
            onDismiss = { mostrarModalCriarTopico = false },
            onPublicar = { tituloTopico, categoria, texto, obraTitulo, tmdbId, poster, tipoMidia, spoiler ->
                viewModel.publicarTopicoForum(
                    topicoTitulo = tituloTopico,
                    categoria = categoria,
                    texto = texto,
                    tituloMidia = obraTitulo,
                    tmdbId = tmdbId,
                    posterPath = poster,
                    tipoMidia = tipoMidia,
                    contemSpoiler = spoiler
                ) { sucesso ->
                    if (sucesso) {
                        Toast.makeText(contexto, "Tópico publicado no CineFórum! 🚀", Toast.LENGTH_SHORT).show()
                        mostrarModalCriarTopico = false
                    } else {
                        Toast.makeText(contexto, "Erro ao publicar no fórum.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // =========================================================================
    // MODAL DE THREAD DE COMENTÁRIOS E RESPOSTAS
    // =========================================================================
    if (topicoSelecionadoParaComentarios != null) {
        val post = topicoSelecionadoParaComentarios!!
        DialogoThreadDiscussao(
            atividade = post,
            meuUid = meuUid,
            viewModel = viewModel,
            onDismiss = { topicoSelecionadoParaComentarios = null }
        )
    }
}

/**
 * =========================================================================
 * CARD DO TÓPICO / POST DO CINEFÓRUM
 * =========================================================================
 */
@Composable
fun CardTopicoForum(
    atividade: AtividadeSocial,
    meuUid: String,
    estaNaMinhaLista: Boolean,
    onMidiaClick: () -> Unit,
    onQueroAssistirTambem: () -> Unit,
    onAlternarLike: () -> Unit,
    onReagir: (String) -> Unit,
    onAbrirComentarios: () -> Unit,
    onDeletar: () -> Unit
) {
    val contexto = LocalContext.current
    var mostrarMenuOpcoes by remember { mutableStateOf(false) }
    var mostrarSeletorReacoes by remember { mutableStateOf(false) }
    var spoilerRevelado by remember { mutableStateOf(!atividade.contemSpoiler) }

    val emojisDisponiveis = listOf("❤️", "🔥", "🍿", "👏", "🤯", "😂")
    val deulike = atividade.usuarioDeuLike(meuUid)

    val corCategoria = remember(atividade.categoria, atividade.tipo) {
        when {
            atividade.categoria.contains("Teoria", ignoreCase = true) || atividade.tipo == "TEORIA" -> Color(0xFFA855F7)
            atividade.categoria.contains("Review", ignoreCase = true) || atividade.tipo == "AVALIOU" -> Color(0xFFFFD700)
            atividade.categoria.contains("Recomenda", ignoreCase = true) -> Color(0xFFFF6D00)
            atividade.tipo == "ASSISTIU" -> Color(0xFF10B981)
            else -> Color(0xFF00E5FF)
        }
    }

    val tempoRelativo = remember(atividade.timestamp) {
        try {
            val agora = System.currentTimeMillis()
            val diff = agora - atividade.timestamp
            when {
                diff < 60_000L -> "Agora há pouco"
                diff < 3600_000L -> "Há ${diff / 60_000L} min"
                diff < 86400_000L -> "Há ${diff / 3600_000L}h"
                diff < 172800_000L -> "Ontem"
                else -> DateUtils.getRelativeTimeSpanString(
                    atividade.timestamp,
                    agora,
                    DateUtils.DAY_IN_MILLIS
                ).toString()
            }
        } catch (e: Exception) {
            "Recentemente"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
        border = BorderStroke(
            width = 1.dp,
            color = if (deulike) Color(0xFFFF3366).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabeçalho: Autor + Categoria + Tempo + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(corCategoria.copy(alpha = 0.2f))
                            .border(1.dp, corCategoria.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (atividade.autorFotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = atividade.autorFotoUrl,
                                contentDescription = atividade.autorNome,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = atividade.autorNome.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = corCategoria,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = atividade.autorNome,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = corCategoria.copy(alpha = 0.15f),
                                border = BorderStroke(0.6.dp, corCategoria)
                            ) {
                                Text(
                                    text = atividade.categoria.ifBlank { "Geral" },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = corCategoria,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = tempoRelativo,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Box {
                    IconButton(onClick = { mostrarMenuOpcoes = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opções", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = mostrarMenuOpcoes,
                        onDismissRequest = { mostrarMenuOpcoes = false }
                    ) {
                        if (atividade.autorUid == meuUid) {
                            DropdownMenuItem(
                                text = { Text("Excluir publicação", color = Color(0xFFFF5252)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252)) },
                                onClick = {
                                    mostrarMenuOpcoes = false
                                    onDeletar()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Denunciar publicação", color = Color.White) },
                                leadingIcon = { Text("🚩", fontSize = 14.sp) },
                                onClick = {
                                    mostrarMenuOpcoes = false
                                    ModeracaoConteudoHelper.denunciarConteudo(
                                        tipoConteudo = "topico_forum",
                                        idConteudo = atividade.id,
                                        autorConteudoUid = atividade.autorUid,
                                        textoConteudo = atividade.topicoTitulo + " " + atividade.comentario,
                                        motivo = "Publicação com conteúdo impróprio ou ofensivo",
                                        contexto = contexto
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Título do Tópico (se for debate)
            if (atividade.topicoTitulo.isNotBlank()) {
                Text(
                    text = atividade.topicoTitulo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = 20.sp
                )
            }

            // Mídia Associada (se houver)
            if (atividade.tituloMidia.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMidiaClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val posterUrl = if (atividade.posterPath.isNotBlank()) {
                                if (atividade.posterPath.startsWith("http")) atividade.posterPath
                                else "https://image.tmdb.org/t/p/w200${atividade.posterPath}"
                            } else ""

                            if (posterUrl.isNotBlank()) {
                                AsyncImage(
                                    model = posterUrl,
                                    contentDescription = atividade.tituloMidia,
                                    modifier = Modifier
                                        .size(width = 38.dp, height = 54.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp, 54.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎬", fontSize = 18.sp)
                                }
                            }

                            Column {
                                Text(
                                    text = atividade.tituloMidia,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = atividade.tipoMidia.ifBlank { "Filme" },
                                        fontSize = 10.sp,
                                        color = Color(0xFF00E5FF)
                                    )
                                    if (atividade.nota > 0) {
                                        Text(
                                            text = "• ${atividade.nota.toInt()}/5 ★",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                    }
                                }
                            }
                        }

                        // Botão de Quero Assistir Também
                        if (estaNaMinhaLista) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, Color(0xFF10B981))
                            ) {
                                Text(
                                    text = "✓ Na Lista",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = onQueroAssistirTambem,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                border = BorderStroke(0.8.dp, Color(0xFF00E5FF))
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Quero Ver", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                            }
                        }
                    }
                }
            }

            // Alerta de Spoiler Interativo
            if (atividade.contemSpoiler && !spoilerRevelado) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFF5252).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { spoilerRevelado = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                        Text(
                            text = "Este post contém SPOILERS. Toque para revelar o texto.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252)
                        )
                    }
                }
            } else if (atividade.comentario.isNotBlank()) {
                Text(
                    text = atividade.comentario,
                    fontSize = 13.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 19.sp
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // =========================================================================
            // BARRA DE INTERAÇÕES: LIKES, COMENTÁRIOS E REAÇÕES
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Botão de Curtir ❤️
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (deulike) Color(0xFFFF3366).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (deulike) Color(0xFFFF3366) else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.clickable { onAlternarLike() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (deulike) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Curtir",
                                tint = if (deulike) Color(0xFFFF3366) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (atividade.totalLikes > 0) "${atividade.totalLikes}" else "Curtir",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (deulike) Color(0xFFFF3366) else Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Botão de Comentários / Discussão 💬
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.clickable { onAbrirComentarios() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = "Comentários",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (atividade.totalComentarios > 0) "${atividade.totalComentarios} respostas" else "Debater",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }

                // Seletor de Emojis / Reações
                Box {
                    IconButton(
                        onClick = { mostrarSeletorReacoes = !mostrarSeletorReacoes },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddReaction,
                            contentDescription = "Reagir",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = mostrarSeletorReacoes,
                        onDismissRequest = { mostrarSeletorReacoes = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            emojisDisponiveis.forEach { emoji ->
                                val jaReagiu = (atividade.reacoes[emoji] ?: emptyList()).contains(meuUid)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (jaReagiu) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable {
                                            mostrarSeletorReacoes = false
                                            onReagir(emoji)
                                        }
                                        .padding(8.dp)
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Exibição de reações em emoji acumuladas
            if (atividade.reacoes.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(atividade.reacoes.entries.toList().filter { it.value.isNotEmpty() }) { (emoji, uids) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { onReagir(emoji) }
                        ) {
                            Text(
                                text = "$emoji ${uids.size}",
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * =========================================================================
 * DIÁLOGO: THREAD DE DISCUSSÃO & RESPOSTAS EM TEMPO REAL
 * =========================================================================
 */
@Composable
fun DialogoThreadDiscussao(
    atividade: AtividadeSocial,
    meuUid: String,
    viewModel: MidiaViewModel,
    onDismiss: () -> Unit
) {
    val contexto = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val comentarios by viewModel.observarComentariosAtividade(atividade.id).collectAsState(initial = emptyList())
    var inputComentario by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F1526),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header do Thread
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💬", fontSize = 16.sp)
                            Text(
                                text = "DEBATE DO FÓRUM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF),
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = atividade.topicoTitulo.ifBlank { atividade.tituloMidia.ifBlank { "Discussão Geral" } },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Post Original Reduzido no Topo
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Por ${atividade.autorNome}:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = atividade.comentario.ifBlank { atividade.topicoTitulo },
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Respostas dos Cinéfilos (${comentarios.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Lista de Comentários / Respostas
                if (comentarios.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ainda não há respostas nesta discussão.\nSeja o primeiro a comentar!",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(comentarios, key = { it.id }) { c ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF161F36),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (c.autorFotoUrl.isNotBlank()) {
                                                    AsyncImage(
                                                        model = c.autorFotoUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                    )
                                                } else {
                                                    Text(c.autorNome.take(1).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                                                }
                                            }
                                            Text(c.autorNome, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        if (c.autorUid == meuUid) {
                                            IconButton(
                                                onClick = { viewModel.deletarComentarioAtividade(atividade.id, c.id) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                                            }
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    ModeracaoConteudoHelper.denunciarConteudo(
                                                        tipoConteudo = "comentario_forum",
                                                        idConteudo = c.id,
                                                        autorConteudoUid = c.autorUid,
                                                        textoConteudo = c.texto,
                                                        motivo = "Comentário impróprio ou spam",
                                                        contexto = contexto
                                                    )
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Text("🚩", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    Text(text = c.texto, fontSize = 12.sp, color = Color(0xFFE2E8F0), lineHeight = 17.sp)

                                    // Curtir Comentário
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val curtiu = c.usuarioCurtiu(meuUid)
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { viewModel.alternarCurtidaComentarioAtividade(atividade.id, c.id) }
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (curtiu) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = null,
                                                tint = if (curtiu) Color(0xFFFF3366) else Color(0xFF64748B),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            if (c.totalLikes > 0) {
                                                Text("${c.totalLikes}", fontSize = 10.sp, color = if (curtiu) Color(0xFFFF3366) else Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Caixa de Entrada de Comentário
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputComentario,
                        onValueChange = { inputComentario = it },
                        placeholder = { Text("Escreva uma resposta...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    IconButton(
                        onClick = {
                            val texto = inputComentario.trim()
                            if (texto.isBlank()) return@IconButton

                            val validacao = ModeracaoConteudoHelper.validarTexto(texto)
                            if (!validacao.aprovado) {
                                Toast.makeText(contexto, validacao.motivoBloqueio, Toast.LENGTH_LONG).show()
                                return@IconButton
                            }

                            if (!enviando) {
                                enviando = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val textoSanitizado = ModeracaoConteudoHelper.sanitizarTexto(texto)
                                viewModel.adicionarComentarioAtividade(atividade.id, textoSanitizado) { sucesso ->
                                    enviando = false
                                    if (sucesso) {
                                        inputComentario = ""
                                    } else {
                                        Toast.makeText(contexto, "Falha ao enviar resposta.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF)),
                        enabled = inputComentario.isNotBlank() && !enviando
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * =========================================================================
 * DIÁLOGO: CRIAR NOVO TÓPICO DE DISCUSSÃO NO FÓRUM
 * =========================================================================
 */
@Composable
fun DialogoCriarTopicoForum(
    midiasSugeridas: List<Midia>,
    onDismiss: () -> Unit,
    onPublicar: (
        tituloTopico: String,
        categoria: String,
        texto: String,
        obraTitulo: String,
        tmdbId: Int,
        poster: String,
        tipoMidia: String,
        spoiler: Boolean
    ) -> Unit
) {
    val contexto = LocalContext.current
    var tituloTopicoInput by remember { mutableStateOf("") }
    var categoriaInput by remember { mutableStateOf("Discussão Geral") }
    var textoInput by remember { mutableStateOf("") }
    var obraTituloInput by remember { mutableStateOf("") }
    var tmdbIdInput by remember { mutableIntStateOf(0) }
    var posterInput by remember { mutableStateOf("") }
    var tipoMidiaInput by remember { mutableStateOf("Filme") }
    var contemSpoilerInput by remember { mutableStateOf(false) }

    val categorias = listOf(
        "Discussão Geral",
        "Crítica / Review",
        "Teorias & Enigmas",
        "Recomendações",
        "Dúvidas"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F1526),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✍️", fontSize = 20.sp)
                        Column {
                            Text("NOVA DISCUSSÃO", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF00E5FF), letterSpacing = 1.sp)
                            Text("Publique um debate para a comunidade", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Título do debate
                    item {
                        OutlinedTextField(
                            value = tituloTopicoInput,
                            onValueChange = { tituloTopicoInput = it },
                            label = { Text("Título do Tópico (ex: 'O final de Interestelar explicado')") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }

                    // 2. Categoria em Chips
                    item {
                        Text("Categoria da Publicação:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            items(categorias) { cat ->
                                val selecionada = categoriaInput == cat
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selecionada) Color(0xFF00E5FF) else Color(0xFF1E293B),
                                    modifier = Modifier.clickable { categoriaInput = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (selecionada) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selecionada) Color.Black else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Mídia associada (opcional)
                    item {
                        Text("Obra relacionada (filme / série opcional):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                        if (midiasSugeridas.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                items(midiasSugeridas.take(5)) { m ->
                                    val escolhido = obraTituloInput == m.titulo
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (escolhido) Color(0xFFA855F7) else Color(0xFF1E293B),
                                        border = BorderStroke(0.8.dp, if (escolhido) Color(0xFFA855F7) else Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.clickable {
                                            obraTituloInput = m.titulo
                                            tmdbIdInput = m.idTmdb
                                            posterInput = m.imagemCapa
                                            tipoMidiaInput = m.tipo
                                        }
                                    ) {
                                        Text(
                                            text = m.titulo,
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = obraTituloInput,
                            onValueChange = { obraTituloInput = it },
                            label = { Text("Nome da Obra") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }

                    // 4. Texto / Conteúdo do debate
                    item {
                        OutlinedTextField(
                            value = textoInput,
                            onValueChange = { textoInput = it },
                            label = { Text("Escreva seus argumentos, resenha ou teoria...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }

                    // 5. Toggle de Spoiler
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⚠️", fontSize = 16.sp)
                                    Column {
                                        Text("Contém Spoilers", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Esconde o texto por padrão com aviso", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                                Switch(
                                    checked = contemSpoilerInput,
                                    onCheckedChange = { contemSpoilerInput = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFF5252),
                                        checkedTrackColor = Color(0xFFFF5252).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Botão Publicar
                Button(
                    onClick = {
                        val tituloLimpo = tituloTopicoInput.trim()
                        val textoLimpo = textoInput.trim()
                        if (tituloLimpo.isBlank() && textoLimpo.isBlank()) return@Button

                        if (tituloLimpo.isNotBlank()) {
                            val valTitulo = ModeracaoConteudoHelper.validarTexto(tituloLimpo)
                            if (!valTitulo.aprovado) {
                                Toast.makeText(contexto, "Título: ${valTitulo.motivoBloqueio}", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                        }

                        if (textoLimpo.isNotBlank()) {
                            val valTexto = ModeracaoConteudoHelper.validarTexto(textoLimpo)
                            if (!valTexto.aprovado) {
                                Toast.makeText(contexto, "Texto: ${valTexto.motivoBloqueio}", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                        }

                        onPublicar(
                            ModeracaoConteudoHelper.sanitizarTexto(tituloLimpo),
                            categoriaInput,
                            ModeracaoConteudoHelper.sanitizarTexto(textoLimpo),
                            obraTituloInput.trim(),
                            tmdbIdInput,
                            posterInput,
                            tipoMidiaInput,
                            contemSpoilerInput
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    enabled = tituloTopicoInput.isNotBlank() || textoInput.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publicar no CineFórum 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
