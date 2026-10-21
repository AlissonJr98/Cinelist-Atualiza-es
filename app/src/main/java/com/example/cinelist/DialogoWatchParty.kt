package com.example.cinelist

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class EmojiFlutuante(
    val id: Long = System.nanoTime(),
    val emoji: String,
    val xOffsetFraction: Float = Random.nextFloat() * 0.8f + 0.1f
)

@Composable
fun DialogoWatchParty(
    tituloMidia: String,
    onDispensar: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val escopo = rememberCoroutineScope()

    var contagemRegressiva by remember { mutableIntStateOf(3) }
    var emContagem by remember { mutableStateOf(false) }
    var assistindoSincronizado by remember { mutableStateOf(false) }
    var estaPausado by remember { mutableStateOf(false) }
    var segundosDecorridos by remember { mutableIntStateOf(0) }

    val emojisFlutuando = remember { mutableStateListOf<EmojiFlutuante>() }

    // Cronômetro do tempo sincronizado
    LaunchedEffect(assistindoSincronizado, estaPausado) {
        if (assistindoSincronizado && !estaPausado) {
            while (true) {
                delay(1000L)
                segundosDecorridos++
            }
        }
    }

    // Limpa emojis após animação
    fun dispararReacao(emoji: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val novo = EmojiFlutuante(emoji = emoji)
        emojisFlutuando.add(novo)
        escopo.launch {
            delay(2200L)
            emojisFlutuando.remove(novo)
        }
    }

    Dialog(
        onDismissRequest = onDispensar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD9050811)),
            contentAlignment = Alignment.Center
        ) {
            // EMOJIS FLUTUANTES NA TELA
            emojisFlutuando.forEach { item ->
                var animOffsetY by remember { mutableFloatStateOf(0f) }
                var animAlpha by remember { mutableFloatStateOf(1f) }

                LaunchedEffect(item.id) {
                    val tempoInicio = System.currentTimeMillis()
                    while (System.currentTimeMillis() - tempoInicio < 2000L) {
                        val progresso = (System.currentTimeMillis() - tempoInicio) / 2000f
                        animOffsetY = -progresso * 600f
                        animAlpha = (1f - progresso).coerceIn(0f, 1f)
                        delay(16L)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = (item.xOffsetFraction * 320).dp, bottom = 120.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = item.emoji,
                        fontSize = 38.sp,
                        modifier = Modifier.graphicsLayer {
                            translationY = animOffsetY
                            alpha = animAlpha
                        }
                    )
                }
            }

            // CONTAINER DO COMPANHEIRO DE SESSÃO
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF0F1424),
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🍿", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "CINEPARTY // SESSÃO AO VIVO",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = tituloMidia,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(onClick = onDispensar, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8))
                        }
                    }

                    // Painel de Play Sincronizado
                    if (emContagem) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (contagemRegressiva > 0) "$contagemRegressiva" else "PLAY! 🚀",
                                fontSize = 68.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    } else if (!assistindoSincronizado) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF151D30),
                            border = BorderStroke(1.dp, Color(0xFF26324D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Assistindo com amigos?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Inicie a contagem regressiva para todo mundo apertar o play no mesmo segundo!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = {
                                        emContagem = true
                                        contagemRegressiva = 3
                                        escopo.launch {
                                            while (contagemRegressiva > 0) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                delay(1000L)
                                                contagemRegressiva--
                                            }
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            delay(700L)
                                            emContagem = false
                                            assistindoSincronizado = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().bounceOnClick(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Iniciar Play Sincronizado (3, 2, 1...)", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    } else {
                        // Painel de Sessão Ativa
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF151D30),
                            border = BorderStroke(1.2.dp, Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val horas = segundosDecorridos / 3600
                                val minutos = (segundosDecorridos % 3600) / 60
                                val segundos = segundosDecorridos % 60
                                val tempoFormatado = if (horas > 0) {
                                    String.format("%02d:%02d:%02d", horas, minutos, segundos)
                                } else {
                                    String.format("%02d:%02d", minutos, segundos)
                                }

                                Text(
                                    text = "TEMPO DECORRIDO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = tempoFormatado,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00E5FF)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { estaPausado = !estaPausado },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (estaPausado) Color(0xFF10B981) else Color(0xFFE11D48)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (estaPausado) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (estaPausado) "Retomar" else "Pausar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            assistindoSincronizado = false
                                            segundosDecorridos = 0
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Encerrar", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }

                    // CHUVA DE REAÇÕES AO VIVO
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Reações ao vivo (toque para disparar):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        val reacoes = listOf("😱", "🍿", "🤯", "😭", "🔥", "❤️", "🚀", "😂")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            reacoes.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .bounceOnClick()
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                        .border(1.dp, Color(0xFF334155), CircleShape)
                                        .clickable { dispararReacao(emoji) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
