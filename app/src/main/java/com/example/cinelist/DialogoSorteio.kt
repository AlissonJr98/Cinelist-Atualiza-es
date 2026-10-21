package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun DialogoSorteio(
    listaDeMidias: List<Midia>,
    onDispensar: () -> Unit,
    onSelecionarMidia: (Midia) -> Unit
) {
    var filtroStatus by remember { mutableStateOf("Quero Assistir") }
    var filtroDuracao by remember { mutableStateOf("Qualquer") }
    var midiaSorteada by remember { mutableStateOf<Midia?>(null) }
    var sortearNovamente by remember { mutableIntStateOf(0) }
    var animandoRoleta by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    val listaCandidatos = remember(listaDeMidias, filtroStatus, filtroDuracao, sortearNovamente) {
        listaDeMidias.filter { midia ->
            val bateStatus = if (filtroStatus == "Todos") true else midia.status.equals(filtroStatus, ignoreCase = true)
            val bateDuracao = when (filtroDuracao) {
                "< 90 min" -> midia.duracaoTotal in 1..90
                "Séries" -> !midia.tipo.equals("Filme", ignoreCase = true)
                else -> true
            }
            bateStatus && bateDuracao
        }
    }

    LaunchedEffect(sortearNovamente) {
        if (listaCandidatos.isNotEmpty()) {
            animandoRoleta = true
            val totalTicks = 16
            for (i in 0 until totalTicks) {
                midiaSorteada = listaCandidatos[Random.nextInt(listaCandidatos.size)]
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) {}
                val delayTime = 35L + (i * i * 0.85).toLong()
                delay(delayTime)
            }
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
            animandoRoleta = false
        } else {
            midiaSorteada = null
        }
    }

    // Escala de vitória suave ao finalizar o sorteio
    val escalaVitoria by animateFloatAsState(
        targetValue = if (animandoRoleta) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escala_vitoria"
    )

    AlertDialog(
        onDismissRequest = onDispensar,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PulsarPontoNeon(cor = Color(0xFFFFD700), tamanho = 9.dp)
                    Column {
                        Text(
                            text = "ROLETA CIBERNÉTICA",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "SORTEIO INTELIGENTE // CINELINK",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF00E5FF).copy(alpha = 0.8f)
                        )
                    }
                }

                IconeAnimadoRotacao(
                    onClick = { if (!animandoRoleta) sortearNovamente++ },
                    anguloRotacaoTotal = 360f
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Girar Roleta",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        containerColor = Color(0xFF0C101B),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.border(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.5f),
                    Color(0xFFFFD700).copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.05f)
                )
            ),
            shape = RoundedCornerShape(22.dp)
        ),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // CHIPS DE STATUS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Quero Assistir", "Assistindo", "Todos").forEach { status ->
                        CyberFilterChip(
                            selected = (filtroStatus == status),
                            onClick = {
                                if (!animandoRoleta) {
                                    filtroStatus = status
                                    sortearNovamente++
                                }
                            },
                            label = status,
                            corDestaque = Color(0xFF00E5FF),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                    }
                }

                // CHIPS DE TEMPO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Qualquer" to "Qualquer Tempo", "< 90 min" to "< 90 min ⏱️", "Séries" to "Séries / Ep 📺").forEach { (chave, rotulo) ->
                        CyberFilterChip(
                            selected = (filtroDuracao == chave),
                            onClick = {
                                if (!animandoRoleta) {
                                    filtroDuracao = chave
                                    sortearNovamente++
                                }
                            },
                            label = rotulo,
                            corDestaque = Color(0xFFFFD700),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // CARD PRINCIPAL DO SORTEIO COM EFEITO CYBER GLASS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .scale(escalaVitoria)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F1526).copy(alpha = 0.95f))
                        .border(
                            width = if (animandoRoleta) 1.5.dp else 1.dp,
                            brush = if (animandoRoleta) {
                                Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFF00E5FF)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.4f), Color.White.copy(alpha = 0.08f)))
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (listaCandidatos.isEmpty()) {
                        Text(
                            text = "Nenhum título encontrado com esses filtros.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (midiaSorteada != null) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pôster com borda de neon
                            Box(
                                modifier = Modifier
                                    .width(96.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF080D1A))
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!midiaSorteada!!.imagemCapa.isNullOrBlank()) {
                                    SubcomposeAsyncImage(
                                        model = midiaSorteada!!.imagemCapa,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (animandoRoleta) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PulsarPontoNeon(cor = Color(0xFFFFD700), tamanho = 7.dp)
                                        Text(
                                            text = "RODANDO...",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFFD700)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF00FF7F).copy(alpha = 0.18f))
                                            .border(0.8.dp, Color(0xFF00FF7F).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SELECIONADO 🎯",
                                            color = Color(0xFF00FF7F),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = midiaSorteada!!.titulo,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "🍿 ${midiaSorteada!!.plataforma.ifBlank { "Cinema / Streaming" }}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (midiaSorteada!!.nota > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${midiaSorteada!!.nota}/5",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { sortearNovamente++ },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .bounceOnClick(enabled = !animandoRoleta && listaCandidatos.isNotEmpty()) {
                            sortearNovamente++
                        },
                    enabled = !animandoRoleta && listaCandidatos.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Girar Novamente", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        midiaSorteada?.let { onSelecionarMidia(it) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .bounceOnClick(enabled = !animandoRoleta && midiaSorteada != null) {
                            midiaSorteada?.let { onSelecionarMidia(it) }
                        },
                    enabled = !animandoRoleta && midiaSorteada != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF0077FF))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ver Detalhes", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Black)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDispensar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fechar", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    )
}
