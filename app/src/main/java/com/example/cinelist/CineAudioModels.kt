package com.example.cinelist

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

data class CineAudioReview(
    val id: String,
    val autorNome: String,
    val autorFoto: String = "",
    val tituloMidia: String,
    val duracaoSegundos: Int = 28,
    val criadoEm: Long = System.currentTimeMillis()
)

@Composable
fun CineAudioPlayerWidget(
    review: CineAudioReview,
    modifier: Modifier = Modifier
) {
    var tocando by remember { mutableStateOf(false) }
    var progresso by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(tocando) {
        if (tocando) {
            while (progresso < 1f && tocando) {
                delay(300)
                progresso += 0.035f
            }
            if (progresso >= 1f) {
                tocando = false
                progresso = 0f
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = { tocando = !tocando },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (tocando) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🎙️ Micro-Crítica de ${review.autorNome}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (tocando) "${(progresso * review.duracaoSegundos).toInt()}s" else "${review.duracaoSegundos}s",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Simulador de ondas de áudio com barras
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barras = listOf(6, 12, 18, 10, 24, 16, 20, 8, 22, 14, 18, 26, 12, 16, 20, 10, 14, 8)
                    barras.forEachIndexed { i, alturaBase ->
                        val proporcaoBarra = i.toFloat() / barras.size.toFloat()
                        val ativa = proporcaoBarra <= progresso

                        val alturaAnimada = if (tocando && ativa) {
                            val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
                            val h by infiniteTransition.animateFloat(
                                initialValue = alturaBase * 0.6f,
                                targetValue = alturaBase * 1.3f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(300 + (i * 30), easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "wave"
                            )
                            h.dp
                        } else {
                            alturaBase.dp
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(alturaAnimada)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (ativa) Color(0xFF00E5FF) else Color(0xFF475569))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoGravarCineAudio(
    tituloMidia: String,
    onAudioGravado: (CineAudioReview) -> Unit,
    onDispensar: () -> Unit
) {
    var gravando by remember { mutableStateOf(false) }
    var segundos by remember { mutableIntStateOf(0) }
    var concluido by remember { mutableStateOf(false) }

    LaunchedEffect(gravando) {
        if (gravando) {
            segundos = 0
            while (gravando && segundos < 45) {
                delay(1000)
                segundos++
            }
            if (segundos >= 45) {
                gravando = false
                concluido = true
            }
        }
    }

    Dialog(onDismissRequest = onDispensar) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🎙️ GRAVAR CINEÁUDIO",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color(0xFF00E5FF)
                )

                Text(
                    text = "Grave uma micro-crítica ou comentário de voz de até 45s sobre '$tituloMidia'",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Botão de Gravação Pulsante
                val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (gravando) 1.15f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            if (gravando) Color(0xFFEF4444).copy(alpha = 0.25f)
                            else Color(0xFF00E5FF).copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (!gravando) {
                                gravando = true
                                concluido = false
                            } else {
                                gravando = false
                                concluido = true
                            }
                        },
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(if (gravando) Color(0xFFEF4444) else Color(0xFF00E5FF))
                    ) {
                        Icon(
                            imageVector = if (gravando) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Gravar",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = if (gravando) "Gravando... 00:${segundos.toString().padStart(2, '0')}"
                    else if (concluido) "Gravação concluída (${segundos}s)!"
                    else "Toque no microfone para começar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (gravando) Color(0xFFEF4444) else Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDispensar) {
                        Text("Cancelar", color = Color(0xFF94A3B8))
                    }

                    Button(
                        onClick = {
                            val review = CineAudioReview(
                                id = java.util.UUID.randomUUID().toString(),
                                autorNome = "Você",
                                tituloMidia = tituloMidia,
                                duracaoSegundos = if (segundos > 0) segundos else 18
                            )
                            onAudioGravado(review)
                            onDispensar()
                        },
                        enabled = concluido || segundos > 2,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Anexar CineÁudio")
                    }
                }
            }
        }
    }
}
