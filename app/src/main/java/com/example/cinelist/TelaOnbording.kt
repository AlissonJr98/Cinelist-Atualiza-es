package com.example.cinelist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelaOnboarding(
    onConcluir: () -> Unit
) {
    val paginas = listOf(
        PaginaOnboarding(
            titulo = "Bem-vindo ao CineList",
            descricao = "O seu companheiro definitivo para organizar, descobrir e partilhar a paixão pelo cinema e pelas séries.",
            icone = Icons.Default.LocalMovies,
            corFundo = Color(0xFF1E1E1E), // Escuro Padrão
            corDestaque = CineListTokens.CorPremium
        ),
        PaginaOnboarding(
            titulo = "Salas Compartilhadas ❤️",
            descricao = "Crie uma sala com o seu parceiro(a) ou amigos. Sincronize o que estão a assistir, deem notas juntos e receba avisos em tempo real!",
            icone = Icons.Default.Group,
            corFundo = Color(0xFF2E1A1A), // Tom avermelhado
            corDestaque = CineListTokens.CorMatch
        ),
        PaginaOnboarding(
            titulo = "Comunidade e Fóruns 💬",
            descricao = "Deixe a sua opinião pública sobre um título. Dê 'Likes', responda aos seus amigos e debata sobre os últimos episódios.",
            icone = Icons.Default.Forum,
            corFundo = Color(0xFF1A2630), // Tom azulado
            corDestaque = CineListTokens.CorOnline
        ),
        PaginaOnboarding(
            titulo = "O seu Ano no Cinema 🌟",
            descricao = "Acompanhe dezenas de estatísticas, horas investidas e plataformas mais usadas, e gere o seu 'CineList Wrapped' a qualquer momento!",
            icone = Icons.Default.WorkspacePremium,
            corFundo = Color(0xFF2E2A1A), // Tom dourado/Premium
            corDestaque = CineListTokens.CorPremium
        )
    )

    val pagerState = rememberPagerState(pageCount = { paginas.size })
    val escopoCorrotina = rememberCoroutineScope()

    // 🚀 Anima a cor de fundo de forma suave ao mudar de página
    val corBackgroundAnimada by animateColorAsState(
        targetValue = paginas[pagerState.currentPage].corFundo,
        animationSpec = tween(durationMillis = 600),
        label = "corFundoAnimada"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        corBackgroundAnimada,
                        Color(0xFF121212)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Botão "Pular" no topo (Se não for a última página)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (pagerState.currentPage < paginas.size - 1) {
                    TextButton(onClick = onConcluir) {
                        Text("Pular", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp)) // Ocupa espaço para não saltar
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // CARROSSEL DE CONTEÚDO
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pagina ->
                val conteudo = paginas[pagina]

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Círculo com o Ícone
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(conteudo.corDestaque.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = conteudo.icone,
                            contentDescription = null,
                            tint = conteudo.corDestaque,
                            modifier = Modifier.size(70.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = conteudo.titulo,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = conteudo.descricao,
                        fontSize = 15.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // INDICADORES DE PÁGINA (As "bolinhas")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(paginas.size) { indice ->
                    val selecionado = pagerState.currentPage == indice
                    val larguraAnimada by animateDpAsState(targetValue = if (selecionado) 24.dp else 8.dp)
                    val corAnimada by animateColorAsState(targetValue = if (selecionado) paginas[pagerState.currentPage].corDestaque else Color.DarkGray)

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(larguraAnimada)
                            .clip(RoundedCornerShape(4.dp))
                            .background(corAnimada)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BOTÃO PRINCIPAL (Avançar ou Concluir)
            Button(
                onClick = {
                    if (pagerState.currentPage < paginas.size - 1) {
                        escopoCorrotina.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onConcluir()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = paginas[pagerState.currentPage].corDestaque
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage < paginas.size - 1) "Próximo" else "Começar Agora",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (pagerState.currentPage < paginas.size - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Classe de dados para armazenar o conteúdo de cada página
data class PaginaOnboarding(
    val titulo: String,
    val descricao: String,
    val icone: ImageVector,
    val corFundo: Color,
    val corDestaque: Color
)