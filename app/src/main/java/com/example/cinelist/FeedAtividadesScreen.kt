package com.example.cinelist

import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

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

    var mostrarDialogoIndicar by remember { mutableStateOf(false) }
    var inputTituloIndicar by remember { mutableStateOf("") }
    var inputComentarioIndicar by remember { mutableStateOf("") }
    var inputTipoIndicar by remember { mutableStateOf("Filme") }

    var filtroSelecionado by remember { mutableStateOf("Todos") }
    val uidsAmigos = remember(amigos) { amigos.map { it.uid }.toSet() }

    val feedFiltrado = remember(feed, filtroSelecionado, uidsAmigos, meuUid) {
        when (filtroSelecionado) {
            "Amigos" -> feed.filter { it.autorUid in uidsAmigos || it.autorUid == meuUid }
            "Avaliações" -> feed.filter { it.tipo == "AVALIOU" }
            "Assistidos" -> feed.filter { it.tipo == "ASSISTIU" }
            else -> feed
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Barra de Indicação Rápida para o Feed
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { mostrarDialogoIndicar = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", fontSize = 16.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Indicar Filme ou Série no Feed",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Toque para recomendar uma obra aos amigos!",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
            }
        }

        // Chip de Filtros Holográficos Sci-Fi
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filtros = listOf("Todos", "Amigos", "Avaliações", "Assistidos")
            items(filtros) { filtro ->
                val ativo = filtroSelecionado == filtro
                CyberFilterChip(
                    selected = ativo,
                    onClick = { filtroSelecionado = filtro },
                    label = filtro,
                    icone = when (filtro) {
                        "Amigos" -> Icons.Default.People
                        "Avaliações" -> Icons.Default.Star
                        "Assistidos" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Dashboard
                    }
                )
            }
        }

        if (feedFiltrado.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PeopleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhuma atividade por aqui ainda",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quando você ou seus amigos assistirem, avaliarem ou salvarem filmes e séries, elas aparecerão aqui na timeline!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onAdicionarAmigoClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Meus Amigos")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(feedFiltrado, key = { it.id.ifBlank { "${it.autorUid}_${it.timestamp}" } }) { atividade ->
                    val jaTenhoSalvo = remember(midiasPessoais, atividade.tmdbId, atividade.tituloMidia) {
                        midiasPessoais.any {
                            (atividade.tmdbId != 0 && it.idTmdb == atividade.tmdbId) ||
                            it.titulo.equals(atividade.tituloMidia, ignoreCase = true)
                        }
                    }

                    CardAtividadeSocial(
                        atividade = atividade,
                        meuUid = meuUid,
                        estaNaMinhaLista = jaTenhoSalvo,
                        onMidiaClick = {
                            onMidiaClick(atividade.tmdbId, atividade.tituloMidia, atividade.tipoMidia)
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
                        onReagir = { emoji ->
                            viewModel.alternarReacaoAtividade(atividade.id, emoji)
                        },
                        onDeletar = {
                            viewModel.deletarAtividade(atividade.id)
                        }
                    )
                }
            }
        }

        if (mostrarDialogoIndicar) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoIndicar = false },
                title = { Text("Indicar para os Amigos ✨") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Recomende um título incrível para todos os seus amigos no feed:")
                        OutlinedTextField(
                            value = inputTituloIndicar,
                            onValueChange = { inputTituloIndicar = it },
                            label = { Text("Título da Obra (ex: Interestelar)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Filme", "Série", "Anime").forEach { tipo ->
                                FilterChip(
                                    selected = inputTipoIndicar == tipo,
                                    onClick = { inputTipoIndicar = tipo },
                                    label = { Text(tipo) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = inputComentarioIndicar,
                            onValueChange = { inputComentarioIndicar = it },
                            label = { Text("Seu comentário ou dica (ex: 'Roteiro genial!')") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val titulo = inputTituloIndicar.trim()
                            if (titulo.isNotBlank()) {
                                viewModel.publicarAtividade(
                                    tipo = "RECOMENDACAO",
                                    tmdbId = 0,
                                    tituloMidia = titulo,
                                    posterPath = "",
                                    tipoMidia = inputTipoIndicar,
                                    comentario = inputComentarioIndicar.trim()
                                )
                                Toast.makeText(contexto, "Indicação publicada no feed!", Toast.LENGTH_SHORT).show()
                                inputTituloIndicar = ""
                                inputComentarioIndicar = ""
                                mostrarDialogoIndicar = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text("Publicar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoIndicar = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun CardAtividadeSocial(
    atividade: AtividadeSocial,
    meuUid: String,
    estaNaMinhaLista: Boolean = false,
    onMidiaClick: () -> Unit,
    onQueroAssistirTambem: () -> Unit = {},
    onReagir: (String) -> Unit,
    onDeletar: () -> Unit
) {
    var mostrarMenuOpcoes by remember { mutableStateOf(false) }
    var mostrarSeletorReacoes by remember { mutableStateOf(false) }

    val emojisDisponiveis = listOf("❤️", "🔥", "🍿", "👏", "🤯", "😂")

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
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131826).copy(alpha = 0.88f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.35f),
                    Color(0xFFFFD700).copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Cabeçalho: Avatar + Nome + Ação + Tempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (atividade.autorFotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = atividade.autorFotoUrl,
                            contentDescription = atividade.autorNome,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = atividade.autorNome.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = atividade.autorNome,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• $tempoRelativo",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Ação (badge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        when (atividade.tipo) {
                            "AVALIOU" -> {
                                Text(
                                    text = "avaliou com ${atividade.nota.toInt()}/5 ★",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFB300),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            "ASSISTIU" -> {
                                Text(
                                    text = "marcou como assistido 🍿",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            "QUERO_VER" -> {
                                Text(
                                    text = "quer assistir 🔖",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            "FAVORITOU" -> {
                                Text(
                                    text = "adicionou aos favoritos ❤️",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF3366),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            else -> {
                                Text(
                                    text = "recomenda este título ✨",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Menu se for o autor
                if (atividade.autorUid == meuUid) {
                    Box {
                        IconButton(
                            onClick = { mostrarMenuOpcoes = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opções",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = mostrarMenuOpcoes,
                            onDismissRequest = { mostrarMenuOpcoes = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Excluir publicação", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    mostrarMenuOpcoes = false
                                    onDeletar()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card da Mídia (Clicável)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMidiaClick() },
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                                .size(width = 54.dp, height = 80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = atividade.tituloMidia,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = atividade.tipoMidia.ifBlank { "Filme" },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (atividade.nota > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color(0xFFFFB300)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${atividade.nota.toInt()}/5",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                }
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ver Detalhes",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Comentário da Atividade (se houver)
            if (atividade.comentario.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${atividade.comentario}\"",
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Botão de Ação Direta: Quero Assistir Também
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (estaNaMinhaLista) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                            Text("Na Minha Lista", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onQueroAssistirTambem,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF))
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quero Assistir Também", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Linha de Reações
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Reações existentes
                LazyRow(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(atividade.reacoes.entries.toList()) { (emoji, uids) ->
                        if (uids.isNotEmpty()) {
                            val jaReagiu = uids.contains(meuUid)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (jaReagiu) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (jaReagiu) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                                modifier = Modifier.clickable { onReagir(emoji) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = emoji, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uids.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (jaReagiu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Botão de Adicionar Reação
                Box {
                    IconButton(
                        onClick = { mostrarSeletorReacoes = !mostrarSeletorReacoes },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddReaction,
                            contentDescription = "Reagir",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Seletor flutuante de emojis
                    DropdownMenu(
                        expanded = mostrarSeletorReacoes,
                        onDismissRequest = { mostrarSeletorReacoes = false }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            emojisDisponiveis.forEach { emoji ->
                                val jaReagiu = (atividade.reacoes[emoji] ?: emptyList()).contains(meuUid)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (jaReagiu) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
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
        }
    }
}
