package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Modal completo com os detalhes do ator/atriz e sua filmografia (Filmes e Séries).
 * Inspirado na interface moderna da Imagem 2 de referência.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoPerfilAtor(
    idAtor: Int,
    nomeAtorInicial: String,
    fotoPerfilInicial: String = "",
    onDispensar: () -> Unit,
    onMidiaSelecionada: (idTmdb: Int, titulo: String, tipo: String) -> Unit
) {
    var detalhes by remember { mutableStateOf<TmdbPessoaDetalhes?>(null) }
    var creditos by remember { mutableStateOf<List<TmdbCreditoItem>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erroCarregamento by remember { mutableStateOf<String?>(null) }
    var bioExpandida by remember { mutableStateOf(false) }
    var filtroTipo by remember { mutableStateOf("Todos") } // "Todos", "Filmes", "Séries"

    LaunchedEffect(idAtor) {
        carregando = true
        erroCarregamento = null
        try {
            withContext(Dispatchers.IO) {
                var det = try {
                    RetrofitClient.apiService.obterDetalhesPessoa(idAtor, "pt-BR")
                } catch (e: Exception) {
                    null
                }

                // Se não tem biografia em português, tenta buscar em inglês
                if (det != null && det.biografia.isBlank()) {
                    val detEn = try {
                        RetrofitClient.apiService.obterDetalhesPessoa(idAtor, "en-US")
                    } catch (e: Exception) {
                        null
                    }
                    if (detEn != null && detEn.biografia.isNotBlank()) {
                        det = det.copy(biografia = detEn.biografia)
                    }
                }

                val cred = try {
                    RetrofitClient.apiService.obterCreditosPessoa(idAtor, "pt-BR").atuacoes ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                // Filtra apenas com poster e ordena por avaliação / relevância
                val creditosOrdenados = cred
                    .filter { !it.caminhoPoster.isNullOrBlank() }
                    .distinctBy { it.id }
                    .sortedByDescending { it.mediaVotos }

                detalhes = det
                creditos = creditosOrdenados
            }
        } catch (e: Exception) {
            erroCarregamento = "Não foi possível carregar os dados do ator."
        } finally {
            carregando = false
        }
    }

    val creditosFiltrados = remember(creditos, filtroTipo) {
        when (filtroTipo) {
            "Filmes" -> creditos.filter { !it.ehSerie }
            "Séries" -> creditos.filter { it.ehSerie }
            else -> creditos
        }
    }

    Dialog(
        onDismissRequest = onDispensar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TopBar estilo Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDispensar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = detalhes?.nome?.ifBlank { nomeAtorInicial } ?: nomeAtorInicial,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (carregando) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "Carregando perfil e filmografia...",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (erroCarregamento != null && detalhes == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Text(erroCarregamento ?: "Erro ao carregar", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                            Button(onClick = onDispensar) { Text("Fechar") }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        // CABEÇALHO DO ATOR
                        item {
                            val fotoUrl = detalhes?.urlFotoPerfil?.ifBlank { fotoPerfilInicial } ?: fotoPerfilInicial

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (fotoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = fotoUrl,
                                            contentDescription = detalhes?.nome ?: nomeAtorInicial,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = nomeAtorInicial.take(2).uppercase(),
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = detalhes?.nome ?: nomeAtorInicial,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )

                                detalhes?.dataNascimento?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Data de nascimento: ${detalhes?.dataNascimentoFormatada}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                detalhes?.localNascimento?.takeIf { it.isNotBlank() }?.let { local ->
                                    Text(
                                        text = "Local: $local",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                    )
                                }

                                detalhes?.departamento?.takeIf { it.isNotBlank() }?.let { depto ->
                                    val deptoPt = when (depto.lowercase()) {
                                        "acting" -> "Atuação"
                                        "directing" -> "Direção"
                                        "production" -> "Produção"
                                        "writing" -> "Roteiro"
                                        else -> depto
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = deptoPt,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Biografia
                                val biografiaTexto = detalhes?.biografia?.ifBlank { "Biografia não disponível." } ?: "Biografia não disponível."
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(14.dp)
                                        .animateContentSize()
                                ) {
                                    Text(
                                        text = "Biografia",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = biografiaTexto,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (bioExpandida) Int.MAX_VALUE else 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (biografiaTexto.length > 200) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (bioExpandida) "Mostrar menos ▲" else "Mostrar mais ▼",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { bioExpandida = !bioExpandida }
                                        )
                                    }
                                }
                            }
                        }

                        // BARRA DE FILTROS DA FILMOGRAFIA
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FILMOGRAFIA E TRABALHOS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${creditosFiltrados.size} títulos",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Todos", "Filmes", "Séries").forEach { rotulo ->
                                    val selecionado = filtroTipo == rotulo
                                    FilterChip(
                                        selected = selecionado,
                                        onClick = { filtroTipo = rotulo },
                                        label = { Text(rotulo, fontSize = 11.sp, fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // LISTA DE FILMES E SÉRIES (Estilo Imagem 2)
                        if (creditosFiltrados.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhum trabalho encontrado para o filtro selecionado.",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(creditosFiltrados, key = { "ator_obra_${it.id}_${it.mediaType}" }) { obra ->
                                val tipoObra = if (obra.ehSerie) "Série" else "Filme"

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clickable {
                                            onMidiaSelecionada(obra.id, obra.titulo, tipoObra)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Poster com Tag sobreposta (TV / FILME)
                                        Box(
                                            modifier = Modifier
                                                .width(75.dp)
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            if (obra.urlPoster.isNotBlank()) {
                                                AsyncImage(
                                                    model = obra.urlPoster,
                                                    contentDescription = obra.titulo,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }

                                            // Badge TV / Filme
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp),
                                                color = if (obra.ehSerie) Color(0xFF00E5FF).copy(alpha = 0.9f) else Color(0xFFFF9100).copy(alpha = 0.9f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (obra.ehSerie) "TV" else "FILME",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        // Detalhes da Obra
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = obra.titulo,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (obra.anoLancamento.isNotBlank()) {
                                                    Text(
                                                        text = obra.anoLancamento,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }

                                                if (obra.mediaVotos > 0) {
                                                    Surface(
                                                        color = Color(0xFFE2B616).copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Text("★", fontSize = 9.sp, color = Color(0xFFFFD700))
                                                            Text(
                                                                text = String.format("%.1f", obra.mediaVotos),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFFFD700)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            obra.personagem?.takeIf { it.isNotBlank() }?.let { char ->
                                                Text(
                                                    text = "Papel: $char",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Text(
                                                text = obra.sinopse.ifBlank { "Sinopse não disponível." },
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Ver detalhes",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
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
