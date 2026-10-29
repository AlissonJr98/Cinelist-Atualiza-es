package com.example.cinelist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Modelo de dados para os slides de apresentação das funções do app.
 */
data class ItemApresentacaoCineList(
    val titulo: String,
    val subtitulo: String,
    val tagDestaque: String,
    val icone: ImageVector,
    val corTema: Color,
    val beneficios: List<String>
)

/**
 * Tela animada de Abertura e Apresentação do CineList antes do login.
 * Mostra como o aplicativo funciona, suas funções inovadoras e convida o usuário a entrar.
 */
@Composable
fun TelaAberturaApp(
    onNavegarParaLogin: () -> Unit,
    onNavegarParaCadastro: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val itensApresentacao = remember {
        listOf(
            ItemApresentacaoCineList(
                titulo = "Organize seu Universo Cinematográfico".t(),
                subtitulo = "Gerencie tudo o que você assiste em um único lugar moderno e futurista.".t(),
                tagDestaque = "CONTROLE TOTAL".t(),
                icone = Icons.Default.VideoLibrary,
                corTema = Color(0xFF00E5FF),
                beneficios = listOf(
                    "Filmes, Séries, Animes e Novelas organizados por status".t(),
                    "Acompanhamento inteligente de temporadas e episódios".t(),
                    "Suas notas pessoais, histórico e retrospectiva anual".t(),
                    "Coleções customizadas para seus gêneros e franquias favoritas".t()
                )
            ),
            ItemApresentacaoCineList(
                titulo = "Salas Compartilhadas & CineParty".t(),
                subtitulo = "Assista junto e sincronize o entretenimento com o parceiro(a) ou amigos.".t(),
                tagDestaque = "AO VIVO & SOCIAL".t(),
                icone = Icons.Default.Group,
                corTema = Color(0xFFFF2A85),
                beneficios = listOf(
                    "Salas privadas sincronizadas em tempo real via nuvem".t(),
                    "Sessões de CineParty com contagem regressiva e chat".t(),
                    "Reações ao vivo e telemetria de quem está assistindo agora".t(),
                    "Votação coletiva e aprovação mútua do próximo título".t()
                )
            ),
            ItemApresentacaoCineList(
                titulo = "CineMatch & Roleta da Sorte".t(),
                subtitulo = "O fim definitivo da indecisão na hora de escolher o que assistir.".t(),
                tagDestaque = "ESCOLHA RÁPIDA".t(),
                icone = Icons.Default.Casino,
                corTema = Color(0xFFA855F7),
                beneficios = listOf(
                    "CineMatch estilo card game: dê like e receba o match na hora".t(),
                    "Roleta da Sorte com sorteios instantâneos e justos".t(),
                    "Filtre por streaming, gênero, duração e nota mínima".t(),
                    "Perfeito para noites de cinema a dois ou em grupo".t()
                )
            ),
            ItemApresentacaoCineList(
                titulo = "Radar dos Streamings & Lançamentos".t(),
                subtitulo = "Saiba exatamente onde cada filme ou série está disponível no Brasil.".t(),
                tagDestaque = "ONDE ASSISTIR".t(),
                icone = Icons.Default.Explore,
                corTema = Color(0xFFFFD700),
                beneficios = listOf(
                    "Catálogo completo com Netflix, Prime Video, Disney+, Max e outros".t(),
                    "Filtre recomendações pelos seus serviços favoritos".t(),
                    "Explore filmografias completas de atores e diretores".t(),
                    "Trailers oficiais em alta definição e sinopses completas".t()
                )
            ),
            ItemApresentacaoCineList(
                titulo = "CineFórum Seguro & App Familiar".t(),
                subtitulo = "Participe de discussões vibrantes em um ambiente protegido e acolhedor.".t(),
                tagDestaque = "AMBIENTE FAMILIAR".t(),
                icone = Icons.Default.Forum,
                corTema = Color(0xFF10B981),
                beneficios = listOf(
                    "Proteção automática contra spoilers com texto oculto".t(),
                    "Filtro inteligente familiar: livre de ofensas, política e discussões".t(),
                    "Compartilhe suas teorias, críticas e resenhas".t(),
                    "Interaja com curtidas, reações de emojis e respostas".t()
                )
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { itensApresentacao.size })

    // Animação de pulso contínuo neon para o halo de fundo
    val transitionInfinita = rememberInfiniteTransition(label = "haloPulse")
    val escalaHalo by transitionInfinita.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escalaHalo"
    )

    val itemAtual = itensApresentacao[pagerState.currentPage]
    val corAtualTema by animateColorAsState(targetValue = itemAtual.corTema, label = "corTemaTransicao")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070A14),
                        Color(0xFF0F1526),
                        Color(0xFF05070C)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Efeito de iluminação neon ambiental no topo
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            corAtualTema.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // =========================================================================
            // CABEÇALHO COM LOGO E BOTÃO PULAR
            // =========================================================================
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFFFFD700)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "CineList",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "TOUR INTERATIVO".t(),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = corAtualTema
                        )
                    }
                }

                TextButton(
                    onClick = onNavegarParaLogin,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Text(
                        text = "Pular Tour".t(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // CARROSSEL ANIMADO COM DETALHES DE CADA FUNÇÃO
            // =========================================================================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { indice ->
                val slide = itensApresentacao[indice]
                val corDestaque = slide.corTema

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Ícone com Halo e Brilho Neon
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(escalaHalo)
                            .clip(CircleShape)
                            .background(corDestaque.copy(alpha = 0.12f))
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        corDestaque,
                                        Color.White.copy(alpha = 0.6f),
                                        corDestaque
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F1526)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = slide.icone,
                                contentDescription = slide.titulo,
                                tint = corDestaque,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Pill com Tag de Categoria
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = corDestaque.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, corDestaque.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = slide.tagDestaque,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = corDestaque,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Título Principal do Recurso
                    Text(
                        text = slide.titulo,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtítulo Explicativo
                    Text(
                        text = slide.subtitulo,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Card de Benefícios e Recursos Inclusos
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF131826).copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            slide.beneficios.forEach { beneficio ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = corDestaque,
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    Text(
                                        text = beneficio,
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0),
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // INDICADORES DE PÁGINA COM EXPANSÃO ANIMADA
            // =========================================================================
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itensApresentacao.indices.forEach { indice ->
                    val ativo = pagerState.currentPage == indice
                    val larguraPill by animateDpAsState(
                        targetValue = if (ativo) 28.dp else 8.dp,
                        label = "pillLargura"
                    )
                    val corPill by animateColorAsState(
                        targetValue = if (ativo) corAtualTema else Color(0xFF334155),
                        label = "pillCor"
                    )

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(larguraPill)
                            .clip(RoundedCornerShape(4.dp))
                            .background(corPill)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(indice)
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // =========================================================================
            // CONTROLES E BOTÕES DE AÇÃO INFERIORES
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (pagerState.currentPage < itensApresentacao.size - 1) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = corAtualTema,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Próxima Função".t(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onNavegarParaLogin,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Fazer Login".t(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botão Secundário para Cadastro Grátis
            OutlinedButton(
                onClick = onNavegarParaCadastro,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(
                    text = "Ainda não tem conta? Criar Conta Grátis".t(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
