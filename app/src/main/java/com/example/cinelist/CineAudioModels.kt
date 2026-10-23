package com.example.cinelist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID

data class CineAudioReview(
    val id: String = UUID.randomUUID().toString(),
    val autorNome: String,
    val autorFoto: String = "",
    val tituloMidia: String,
    val duracaoSegundos: Int = 28,
    val caminhoArquivo: String = "",
    val criadoEm: Long = System.currentTimeMillis()
)

@Composable
fun CineAudioPlayerWidget(
    review: CineAudioReview,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    var tocando by remember { mutableStateOf(false) }
    var progresso by remember { mutableFloatStateOf(0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(review.caminhoArquivo) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    // Atualiza progresso da reprodução real
    LaunchedEffect(tocando) {
        if (tocando) {
            val mp = mediaPlayer
            if (mp != null) {
                while (tocando && mp.isPlaying) {
                    val dur = mp.duration
                    if (dur > 0) {
                        progresso = (mp.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    }
                    delay(100L)
                }
            } else {
                // Fallback suave caso sem arquivo físico
                while (progresso < 1f && tocando) {
                    delay(200L)
                    progresso += 0.02f
                }
                if (progresso >= 1f) {
                    tocando = false
                    progresso = 0f
                }
            }
        }
    }

    fun alternarReproducao() {
        try {
            if (tocando) {
                mediaPlayer?.pause()
                tocando = false
            } else {
                val arquivo = if (review.caminhoArquivo.isNotBlank()) File(review.caminhoArquivo) else null
                if (arquivo != null && arquivo.exists() && arquivo.length() > 0) {
                    if (mediaPlayer == null) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(arquivo.absolutePath)
                            prepare()
                            setOnCompletionListener {
                                tocando = false
                                progresso = 0f
                            }
                        }
                    }
                    mediaPlayer?.start()
                    tocando = true
                } else {
                    // Sem arquivo local válido, anima barras como demonstração
                    tocando = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(contexto, "Falha ao reproduzir áudio gravado.", Toast.LENGTH_SHORT).show()
            tocando = false
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
                onClick = { alternarReproducao() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (tocando) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (tocando) "Pausar" else "Reproduzir",
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

                // Ondas de áudio dinâmicas
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
    val contexto = LocalContext.current
    var gravando by remember { mutableStateOf(false) }
    var segundos by remember { mutableIntStateOf(0) }
    var concluido by remember { mutableStateOf(false) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var arquivoGravado by remember { mutableStateOf<File?>(null) }

    // Launcher para pedir permissão de microfone de forma reativa
    val permissaoAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            Toast.makeText(contexto, "Permissão concedida! Toque para gravar.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(contexto, "Permissão de microfone necessária para gravar CineÁudio.", Toast.LENGTH_LONG).show()
        }
    }

    fun pararGravacao(salvar: Boolean) {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        gravando = false
        if (salvar && segundos > 0) {
            concluido = true
        }
    }

    fun iniciarGravacao() {
        val temPermissao = ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!temPermissao) {
            permissaoAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        try {
            val diretorioAudios = File(contexto.filesDir, "cine_audios").apply { if (!exists()) mkdirs() }
            val novoArquivo = File(diretorioAudios, "audio_${System.currentTimeMillis()}.m4a")
            arquivoGravado = novoArquivo

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(contexto)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(novoArquivo.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            gravando = true
            concluido = false
            segundos = 0
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(contexto, "Erro ao iniciar microfone: ${e.message}", Toast.LENGTH_LONG).show()
            gravando = false
        }
    }

    // Cronômetro da gravação
    LaunchedEffect(gravando) {
        if (gravando) {
            while (gravando && segundos < 45) {
                delay(1000L)
                segundos++
            }
            if (segundos >= 45) {
                pararGravacao(salvar = true)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (gravando) {
                pararGravacao(salvar = false)
            }
        }
    }

    Dialog(onDismissRequest = {
        if (gravando) pararGravacao(salvar = false)
        onDispensar()
    }) {
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
                    text = "Grave sua voz e comentários de até 45s sobre '$tituloMidia'",
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
                                iniciarGravacao()
                            } else {
                                pararGravacao(salvar = true)
                            }
                        },
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(if (gravando) Color(0xFFEF4444) else Color(0xFF00E5FF))
                    ) {
                        Icon(
                            imageVector = if (gravando) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (gravando) "Parar" else "Gravar",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = if (gravando) "Gravando sua voz... 00:${segundos.toString().padStart(2, '0')}"
                    else if (concluido) "Gravação concluída (${segundos}s)! Voz gravada com sucesso."
                    else "Toque no microfone para falar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (gravando) Color(0xFFEF4444) else Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        if (gravando) pararGravacao(salvar = false)
                        onDispensar()
                    }) {
                        Text("Cancelar", color = Color(0xFF94A3B8))
                    }

                    Button(
                        onClick = {
                            if (gravando) pararGravacao(salvar = true)
                            val review = CineAudioReview(
                                id = UUID.randomUUID().toString(),
                                autorNome = "Você",
                                tituloMidia = tituloMidia,
                                duracaoSegundos = if (segundos > 0) segundos else 1,
                                caminhoArquivo = arquivoGravado?.absolutePath ?: ""
                            )
                            onAudioGravado(review)
                            onDispensar()
                        },
                        enabled = (concluido && arquivoGravado?.exists() == true) || segundos > 1,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Salvar CineÁudio")
                    }
                }
            }
        }
    }
}
