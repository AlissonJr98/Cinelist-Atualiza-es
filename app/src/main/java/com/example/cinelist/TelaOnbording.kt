package com.example.cinelist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Onboarding Moderno e Interativo com Configuração do Explorar.
 * Permite ao usuário selecionar seus serviços de streaming (estilo Imagem 3)
 * e seus gêneros de preferência para personalizar a aba Descobrir.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelaOnboarding(
    onConcluir: () -> Unit
) {
    val contexto = LocalContext.current
    val escopoCorrotina = rememberCoroutineScope()

    // Estado dos streamings selecionados (padrão inicial: Netflix, Prime Video, Disney+, Max)
    var streamingsSelecionados by remember {
        mutableStateOf(
            StreamingManager.obterMeusStreamingsIds(contexto).ifEmpty {
                setOf(8, 119, 337, 1899)
            }
        )
    }

    // Estado dos gêneros favoritos selecionados (padrão inicial: Ação, Ficção Científica, Comédia)
    var generosSelecionados by remember {
        mutableStateOf(
            GeneroPreferencesManager.obterGenerosFavoritosIds(contexto).ifEmpty {
                setOf(28, 878, 35)
            }
        )
    }

    var buscaStreaming by remember { mutableStateOf("") }

    val streamingsFiltrados = remember(buscaStreaming) {
        if (buscaStreaming.isBlank()) {
            StreamingManager.SERVICOS
        } else {
            StreamingManager.SERVICOS.filter {
                it.nome.contains(buscaStreaming, ignoreCase = true)
            }
        }
    }

    val totalPaginas = 5
    val pagerState = rememberPagerState(pageCount = { totalPaginas })

    val corDestaqueGeral = Color(0xFFFFD700) // Dourado Cyber / Premium

    fun finalizarEAvancar() {
        StreamingManager.salvarMeusStreamingsIds(contexto, streamingsSelecionados)
        GeneroPreferencesManager.salvarGenerosFavoritosIds(contexto, generosSelecionados)
        onConcluir()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1526),
                        Color(0xFF090A0F)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TopBar: Logo & Botão "Pular"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFFFD700)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "CineList",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                if (pagerState.currentPage < totalPaginas - 1) {
                    TextButton(onClick = { finalizarEAvancar() }) {
                        Text("Pular", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CARROSSEL DE CONTEÚDO
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pagina ->
                when (pagina) {
                    0 -> {
                        // PÁGINA 0: BEM-VINDO
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                    .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalMovies,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(64.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Bem-vindo ao CineList",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "O seu universo definitivo para acompanhar filmes, maratonar séries, sincronizar salas com amigos e nunca mais perder onde parou.",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    1 -> {
                        // PÁGINA 1: SELECIONE SEUS STREAMINGS (Estilo Imagem 3)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Selecione seus streamings",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${streamingsSelecionados.size} selecionados",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Barra de busca dos serviços
                            OutlinedTextField(
                                value = buscaStreaming,
                                onValueChange = { buscaStreaming = it },
                                placeholder = { Text("Buscar serviço de streaming...", fontSize = 13.sp, color = Color(0xFF64748B)) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (buscaStreaming.isNotBlank()) {
                                        IconButton(onClick = { buscaStreaming = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                    focusedContainerColor = Color(0xFF131B2E),
                                    unfocusedContainerColor = Color(0xFF131B2E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Grid de Streamings
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(streamingsFiltrados, key = { it.id }) { servico ->
                                    val selecionado = servico.id in streamingsSelecionados
                                    val cor = Color(servico.corHex)

                                    Surface(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clickable {
                                                streamingsSelecionados = if (selecionado) {
                                                    streamingsSelecionados - servico.id
                                                } else {
                                                    streamingsSelecionados + servico.id
                                                }
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selecionado) cor.copy(alpha = 0.22f) else Color(0xFF131B2E),
                                        border = BorderStroke(
                                            width = if (selecionado) 2.dp else 1.dp,
                                            color = if (selecionado) cor else Color.White.copy(alpha = 0.08f)
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selecionado) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(cor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = servico.emoji,
                                                    fontSize = 24.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = servico.nome,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (selecionado) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = if (selecionado) Color.White else Color(0xFF94A3B8),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // PÁGINA 2: ESCOLHA SEUS GÊNEROS FAVORITOS
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Quais gêneros você mais curte?",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Configuramos as vitrines do seu Explorar conforme seu gosto",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                color = Color(0xFFFFD700).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${generosSelecionados.size} gêneros selecionados",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(GeneroPreferencesManager.GENEROS, key = { it.id }) { genero ->
                                    val selecionado = genero.id in generosSelecionados

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .clickable {
                                                generosSelecionados = if (selecionado) {
                                                    generosSelecionados - genero.id
                                                } else {
                                                    generosSelecionados + genero.id
                                                }
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selecionado) Color(0xFFFFD700).copy(alpha = 0.18f) else Color(0xFF131B2E),
                                        border = BorderStroke(
                                            width = if (selecionado) 1.5.dp else 1.dp,
                                            color = if (selecionado) Color(0xFFFFD700) else Color.White.copy(alpha = 0.08f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(genero.iconeEmoji, fontSize = 20.sp)
                                                Text(
                                                    text = genero.nome,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selecionado) Color.White else Color(0xFF94A3B8)
                                                )
                                            }

                                            if (selecionado) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFD700),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // PÁGINA 3: SALAS COMPARTILHADAS & CINEPARTY
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF2A85).copy(alpha = 0.15f))
                                    .border(2.dp, Color(0xFFFF2A85).copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = Color(0xFFFF2A85),
                                    modifier = Modifier.size(64.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Salas Compartilhadas ❤️",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Crie uma sala com o seu parceiro(a) ou grupo de amigos. Sincronizem suas listas, deem notas juntos e iniciem sessões síncronas de CineParty com contagem regressiva e reações ao vivo!",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    4 -> {
                        // PÁGINA 4: FÓRUM SOCIAL & RETROSPECTIVA
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                    .border(2.dp, Color(0xFFFFD700).copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(64.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Tudo Pronto Para Começar! 🚀",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Debata sobre filmes no CineFórum Social, acompanhe estatísticas avançadas e desbloqueie seu CineList Wrapped a qualquer momento.",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // INDICADORES DE PÁGINA (Pills Animadas)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPaginas) { indice ->
                    val selecionado = pagerState.currentPage == indice
                    val larguraAnimada by animateDpAsState(targetValue = if (selecionado) 24.dp else 8.dp, label = "larguraDot")
                    val corAnimada by animateColorAsState(
                        targetValue = if (selecionado) corDestaqueGeral else Color(0xFF334155),
                        label = "corDot"
                    )

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(larguraAnimada)
                            .clip(RoundedCornerShape(4.dp))
                            .background(corAnimada)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // BOTÕES DE NAVEGAÇÃO INFERIOR (Voltar e Avançar / Começar Agora)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            escopoCorrotina.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", modifier = Modifier.size(18.dp))
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < totalPaginas - 1) {
                            escopoCorrotina.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            finalizarEAvancar()
                        }
                    },
                    modifier = Modifier
                        .weight(if (pagerState.currentPage > 0) 0.65f else 1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = corDestaqueGeral,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < totalPaginas - 1) "Próximo" else "Começar Agora",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (pagerState.currentPage < totalPaginas - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
