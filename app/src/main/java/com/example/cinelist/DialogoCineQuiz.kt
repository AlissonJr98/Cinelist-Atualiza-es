package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DialogoCineQuiz(
    tema: String = "Cinema Geral",
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val escopo = rememberCoroutineScope()

    var carregando by remember { mutableStateOf(true) }
    var listaPerguntas by remember { mutableStateOf<List<PerguntaQuiz>>(emptyList()) }
    var indicePerguntaAtual by remember { mutableIntStateOf(0) }
    var opcaoSelecionada by remember { mutableStateOf<Int?>(null) }
    var acertos by remember { mutableIntStateOf(0) }
    var quizFinalizado by remember { mutableStateOf(false) }

    // Carrega o quiz ao abrir
    fun carregarPerguntas() {
        carregando = true
        quizFinalizado = false
        indicePerguntaAtual = 0
        opcaoSelecionada = null
        acertos = 0

        escopo.launch {
            val perguntas = CineAiManager.gerarQuizCineAi(contexto, tema)
            listaPerguntas = perguntas
            carregando = false
        }
    }

    LaunchedEffect(Unit) {
        carregarPerguntas()
    }

    Dialog(onDismissRequest = onDispensar) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B0E17),
            border = BorderStroke(1.5.dp, Color(0xFF7C4DFF).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header do Quiz
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C4DFF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎮", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "CINEQUIZ // TRIVIA",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFF7C4DFF)
                            )
                            Text(
                                text = "Desafio de Cinema CineAI",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = onDispensar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8))
                    }
                }

                if (carregando) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF7C4DFF), modifier = Modifier.size(38.dp))
                            Text(
                                text = "CineAI está gerando perguntas desafiadoras...",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (quizFinalizado) {
                    // TELA DE RESULTADO / PÓDIO
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val percentual = if (listaPerguntas.isNotEmpty()) (acertos.toFloat() / listaPerguntas.size) else 0f
                        val emojiResultado = if (percentual >= 0.75f) "🏆" else if (percentual >= 0.5f) "🍿" else "🎬"
                        val tituloResultado = if (percentual >= 0.75f) "Mestre Cinéfilo Lendário!" else if (percentual >= 0.5f) "Muito Bom, Cinéfilo!" else "Bom Treino!"

                        Text(text = emojiResultado, fontSize = 54.sp)
                        Text(
                            text = tituloResultado,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF7C4DFF))
                        ) {
                            Text(
                                text = "Pontuação: $acertos de ${listaPerguntas.size} acertos (${(percentual * 100).toInt()}%)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Button(
                            onClick = { carregarPerguntas() },
                            modifier = Modifier.fillMaxWidth().bounceOnClick(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Jogar Novamente", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else if (listaPerguntas.isNotEmpty()) {
                    val perguntaAtual = listaPerguntas[indicePerguntaAtual]

                    // Barra de progresso do Quiz
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pergunta ${indicePerguntaAtual + 1} de ${listaPerguntas.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C4DFF)
                            )
                            Text(
                                text = "Pontos: $acertos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (indicePerguntaAtual + 1).toFloat() / listaPerguntas.size },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF7C4DFF),
                            trackColor = Color(0xFF1E293B)
                        )
                    }

                    // Card da Pergunta
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF151A28),
                        border = BorderStroke(1.dp, Color(0xFF26324D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = perguntaAtual.pergunta,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    // 4 Opções de Resposta
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        perguntaAtual.opcoes.forEachIndexed { index, opcaoTexto ->
                            val jaRespondeu = opcaoSelecionada != null
                            val ehEstaSelecionada = opcaoSelecionada == index
                            val ehACorreta = index == perguntaAtual.respostaCorretaIndex

                            val corBorda = when {
                                !jaRespondeu -> Color(0xFF26324D)
                                ehACorreta -> Color(0xFF10B981)
                                ehEstaSelecionada -> Color(0xFFEF4444)
                                else -> Color(0xFF1E293B)
                            }

                            val corFundo = when {
                                !jaRespondeu -> Color(0xFF121622)
                                ehACorreta -> Color(0x3310B981)
                                ehEstaSelecionada -> Color(0x33EF4444)
                                else -> Color(0xFF0F121C)
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = corFundo,
                                border = BorderStroke(1.2.dp, corBorda),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !jaRespondeu) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        opcaoSelecionada = index
                                        if (index == perguntaAtual.respostaCorretaIndex) {
                                            acertos++
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = opcaoTexto,
                                        fontSize = 13.sp,
                                        fontWeight = if (ehEstaSelecionada || (jaRespondeu && ehACorreta)) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (jaRespondeu) {
                                        if (ehACorreta) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Correto", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                        } else if (ehEstaSelecionada) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Incorreto", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Curiosidade Revelada e Botão Avançar
                    if (opcaoSelecionada != null) {
                        AnimatedVisibility(visible = true, enter = fadeIn()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (perguntaAtual.curiosidade.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("💡 Curiosidade Cinéfila:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF00E5FF))
                                            Text(perguntaAtual.curiosidade, fontSize = 11.sp, color = Color(0xFFE2E8F0))
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (indicePerguntaAtual + 1 < listaPerguntas.size) {
                                            indicePerguntaAtual++
                                            opcaoSelecionada = null
                                        } else {
                                            quizFinalizado = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().bounceOnClick(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (indicePerguntaAtual + 1 < listaPerguntas.size) "Próxima Pergunta ➔" else "Ver Resultado Final 🏆",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black
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
