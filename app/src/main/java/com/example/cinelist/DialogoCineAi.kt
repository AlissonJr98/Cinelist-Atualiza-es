package com.example.cinelist

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoCineAi(
    onDispensar: () -> Unit,
    onAdicionarClique: (Int, String, String, String, Int, String, String, String, String) -> Unit,
    onVerDetalhes: (TmdbFilme, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var vibeSelecionada by remember { mutableStateOf<VibeHumor?>(VibeHumor.PLOT_TWIST) }
    var textoLivre by remember { mutableStateOf("") }
    var filtroTipo by remember { mutableStateOf("TODOS") } // "TODOS", "FILME", "SERIE"

    var sugestoes by remember { mutableStateOf<List<CineAiSugestao>>(emptyList()) }
    var buscando by remember { mutableStateOf(false) }
    var buscouUmaVez by remember { mutableStateOf(false) }

    var mostrarConfigChave by remember { mutableStateOf(false) }
    var chaveInput by remember { mutableStateOf(CineAiManager.obterChaveGemini(context)) }

    Dialog(
        onDismissRequest = onDispensar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // ==========================================
                // CABEÇALHO MODERNO COM GRADIENTE
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF8A2387),
                                            Color(0xFFE94057),
                                            Color(0xFFF27121)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CineAI",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (CineAiManager.obterChaveGemini(context).isNotBlank()) "Gemini 2.5 Flash ⚡" else "Smart Engine 🧠",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Curadoria inteligente por humor e vibe",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { mostrarConfigChave = true }) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Configurar Chave Gemini",
                                tint = if (CineAiManager.obterChaveGemini(context).isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = onDispensar) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // CONTEÚDO ROLÁVEL (VIBES + PROMPT + RESULTADOS)
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SELEÇÃO DE VIBE / HUMOR
                    Text(
                        text = "Qual é a sua vibe agora?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(VibeHumor.values()) { vibe ->
                            val selecionado = vibeSelecionada == vibe
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = if (selecionado) 4.dp else 1.dp,
                                modifier = Modifier.clickable {
                                    vibeSelecionada = if (selecionado) null else vibe
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(vibe.emoji, fontSize = 16.sp)
                                    Text(
                                        text = vibe.titulo,
                                        fontSize = 12.sp,
                                        fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selecionado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // FILTRO DE FORMATO (FILMES / SÉRIES / TODOS)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Formato:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        listOf("TODOS" to "Tanto faz", "FILME" to "Filmes", "SERIE" to "Séries").forEach { (chave, rotulo) ->
                            FilterChip(
                                selected = filtroTipo == chave,
                                onClick = { filtroTipo = chave },
                                label = { Text(rotulo, fontSize = 11.sp) }
                            )
                        }
                    }

                    // CAMPO LIVRE DE PROMPT
                    OutlinedTextField(
                        value = textoLivre,
                        onValueChange = { textoLivre = it },
                        placeholder = {
                            Text(
                                "Ou descreva com suas palavras (ex: um suspense na neve com reviravolta)...",
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    // BOTÃO GERAR / BUSCAR COM IA
                    Button(
                        onClick = {
                            buscando = true
                            coroutineScope.launch {
                                try {
                                    val resultados = CineAiManager.recomendar(
                                        context = context,
                                        vibe = vibeSelecionada,
                                        textoLivre = textoLivre,
                                        filtroTipo = filtroTipo
                                    )
                                    sugestoes = resultados
                                    buscouUmaVez = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    buscando = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !buscando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (buscando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Consultando CineAI...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (sugestoes.isNotEmpty()) "Gerar Novas Recomendações 🎲" else "Descobrir Títulos Perfeitos ✨",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // MENSAGEM QUANDO NÃO ENCONTRAR NADA
                    if (buscouUmaVez && !buscando && sugestoes.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Nenhuma recomendação encontrada para estes termos. Tente selecionar outra vibe acima ou simplificar a descrição.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // LISTAGEM DOS RESULTADOS RECOMENDADOS
                    if (sugestoes.isNotEmpty()) {
                        Text(
                            text = "💡 Recomendações Selecionadas para Você:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        sugestoes.forEach { sugestao ->
                            val item = sugestao.tmdbItem
                            val tipoReal = if (item.ehSerie || item.mediaType.equals("tv", ignoreCase = true)) "Série" else "Filme"
                            val posterUrl = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else ""

                            ElevatedCard(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // POSTER
                                        if (posterUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = posterUrl,
                                                contentDescription = item.titulo,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(65.dp, 95.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onVerDetalhes(item, tipoReal) }
                                            )
                                        }

                                        // INFOS PRINCIPAIS
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.titulo,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                if (item.mediaVotos > 0.0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFFFB800)
                                                    ) {
                                                        Text(
                                                            text = "⭐ ${String.format("%.1f", item.mediaVotos)}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "${if (item.anoLancamento.isNotBlank()) "${item.anoLancamento} • " else ""}$tipoReal • ${item.generoTexto}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // CHIPS DE DESTAQUES
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                sugestao.destaques.take(2).forEach { destaque ->
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = destaque,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // BALÃO DA IA
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("💡", fontSize = 12.sp)
                                            Text(
                                                text = sugestao.justificativaIa,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // AÇÕES: VER DETALHES + ADICIONAR
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { onVerDetalhes(item, tipoReal) },
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("Ver Detalhes", fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                onAdicionarClique(
                                                    item.idTmdb,
                                                    item.titulo,
                                                    tipoReal,
                                                    StatusMidia.QUERO_ASSISTIR.valor,
                                                    0,
                                                    item.sinopse,
                                                    posterUrl,
                                                    item.generoTexto,
                                                    if (tipoReal == "Série") "TV / Original" else "Cinema"
                                                )
                                            },
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Minha Lista", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    // ==========================================
    // DIÁLOGO DE CONFIGURAÇÃO DA CHAVE GEMINI
    // ==========================================
    if (mostrarConfigChave) {
        AlertDialog(
            onDismissRequest = { mostrarConfigChave = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Chave Gemini AI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "O CineAI funciona perfeitamente sem chave usando curadoria heurística. Se você tiver uma chave gratuita do Google AI Studio (Gemini 2.5 Flash), cole-a abaixo para ativar a inteligência generativa profunda:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedTextField(
                        value = chaveInput,
                        onValueChange = { chaveInput = it },
                        placeholder = { Text("AIzaSy...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    CineAiManager.salvarChaveGemini(context, chaveInput)
                    mostrarConfigChave = false
                }) {
                    Text("Salvar Chave", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    chaveInput = ""
                    CineAiManager.salvarChaveGemini(context, "")
                    mostrarConfigChave = false
                }) {
                    Text("Remover / Usar Motor Híbrido")
                }
            }
        )
    }
}
