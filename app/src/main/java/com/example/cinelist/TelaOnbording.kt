package com.example.cinelist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class PaginaOnboarding(
    val icone: ImageVector,
    val titulo: String,
    val descricao: String,
    val gradiente: Brush
)

private val paginasOnboarding = listOf(
    PaginaOnboarding(
        icone = Icons.Default.LocalMovies,
        titulo = "Organize tudo em um só lugar",
        descricao = "Monte sua lista de filmes, séries, animes, novelas e doramas com status, nota e progresso de episódios.",
        gradiente = Brush.verticalGradient(listOf(Color(0xFF1DB954), Color(0xFF121212)))
    ),
    PaginaOnboarding(
        icone = Icons.Default.Group,
        titulo = "Listas compartilhadas",
        descricao = "Crie uma sala com seu parceiro(a) ou amigos e monte uma lista em conjunto, vendo quem está assistindo o quê em tempo real.",
        gradiente = Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF121212)))
    ),
    PaginaOnboarding(
        icone = Icons.Default.Casino,
        titulo = "Modo Match e sorteio",
        descricao = "Sem ideia do que assistir? Jogue o Modo Match com a sala ou use o sorteio para decidir por você.",
        gradiente = Brush.verticalGradient(listOf(Color(0xFFFF3366), Color(0xFF121212)))
    ),
    PaginaOnboarding(
        icone = Icons.Default.WorkspacePremium,
        titulo = "Sua retrospectiva CineList",
        descricao = "Acompanhe horas assistidas, gêneros favoritos e veja o resumo da sua jornada cinéfila no Wrapped, direto no seu Perfil.",
        gradiente = Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFF121212)))
    )
)

@Composable
fun TelaOnboarding(onConcluir: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { paginasOnboarding.size })
    val escopo = rememberCoroutineScope()
    val ultimaPagina by remember {
        derivedStateOf { pagerState.currentPage == paginasOnboarding.lastIndex }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pagina ->
            val dados = paginasOnboarding[pagina]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dados.gradiente)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = dados.icone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(24.dp)
                                .size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = dados.titulo,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = dados.descricao,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )
                }
            }
        }

        // "Pular" — some sozinho na última página, já que ali o botão principal já conclui o fluxo
        if (!ultimaPagina) {
            TextButton(
                onClick = onConcluir,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text("Pular", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                paginasOnboarding.indices.forEach { indice ->
                    val ativo = indice == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (ativo) 24.dp else 8.dp)
                            .background(
                                color = if (ativo) Color.White else Color.White.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (ultimaPagina) {
                Button(
                    onClick = onConcluir,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Começar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                OutlinedButton(
                    onClick = {
                        escopo.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Próximo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}