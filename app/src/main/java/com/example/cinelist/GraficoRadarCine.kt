package com.example.cinelist

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class CriteriosAvaliacao(
    val atuacao: Float = 0f,
    val roteiro: Float = 0f,
    val visual: Float = 0f,
    val trilha: Float = 0f,
    val ritmo: Float = 0f
) {
    fun media(): Float {
        val soma = atuacao + roteiro + visual + trilha + ritmo
        return if (soma > 0) soma / 5f else 0f
    }

    fun paraStringSerializada(): String {
        return "criterios:atuacao=$atuacao,roteiro=$roteiro,visual=$visual,trilha=$trilha,ritmo=$ritmo"
    }

    companion object {
        fun deStringSerializada(texto: String?): CriteriosAvaliacao? {
            if (texto.isNullOrBlank() || !texto.contains("criterios:")) return null
            return try {
                val valores = texto.substringAfter("criterios:").split(",").associate {
                    val partes = it.split("=")
                    partes[0].trim() to (partes.getOrNull(1)?.toFloatOrNull() ?: 0f)
                }
                CriteriosAvaliacao(
                    atuacao = valores["atuacao"] ?: 0f,
                    roteiro = valores["roteiro"] ?: 0f,
                    visual = valores["visual"] ?: 0f,
                    trilha = valores["trilha"] ?: 0f,
                    ritmo = valores["ritmo"] ?: 0f
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Composable
fun GraficoRadarCine(
    criterios: CriteriosAvaliacao,
    modifier: Modifier = Modifier,
    corPrimaria: Color = Color(0xFF00E5FF),
    corPreenchimento: Color = Color(0x3300E5FF)
) {
    val labels = listOf("🎭 Atuação", "📖 Roteiro", "🎨 Visual", "🎵 Trilha", "⚡ Ritmo")
    val valores = listOf(
        criterios.atuacao / 10f,
        criterios.roteiro / 10f,
        criterios.visual / 10f,
        criterios.trilha / 10f,
        criterios.ritmo / 10f
    )

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "radar_anim"
    )

    Canvas(modifier = modifier) {
        val centroX = size.width / 2f
        val centroY = size.height / 2f
        val raioMax = minOf(centroX, centroY) * 0.68f

        // 1. Desenha as teias concêntricas (25%, 50%, 75%, 100%)
        val niveis = listOf(0.25f, 0.5f, 0.75f, 1.0f)
        niveis.forEach { nivel ->
            val pathTeia = Path()
            for (i in 0 until 5) {
                val angulo = (i * 2 * PI / 5 - PI / 2).toFloat()
                val r = raioMax * nivel
                val x = centroX + r * cos(angulo)
                val y = centroY + r * sin(angulo)
                if (i == 0) pathTeia.moveTo(x, y) else pathTeia.lineTo(x, y)
            }
            pathTeia.close()
            drawPath(
                path = pathTeia,
                color = Color.White.copy(alpha = if (nivel == 1f) 0.25f else 0.10f),
                style = Stroke(width = if (nivel == 1f) 1.5f else 1f)
            )
        }

        // 2. Desenha os 5 eixos radiais e labels
        val paintTexto = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        for (i in 0 until 5) {
            val angulo = (i * 2 * PI / 5 - PI / 2).toFloat()
            val xEixo = centroX + raioMax * cos(angulo)
            val yEixo = centroY + raioMax * sin(angulo)

            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(centroX, centroY),
                end = Offset(xEixo, yEixo),
                strokeWidth = 1f
            )

            // Posiciona os rótulos além do raio máximo
            val rTexto = raioMax + 38f
            val xTexto = centroX + rTexto * cos(angulo)
            val yTexto = centroY + rTexto * sin(angulo) + 10f

            drawContext.canvas.nativeCanvas.drawText(labels[i], xTexto, yTexto, paintTexto)
        }

        // 3. Desenha o polígono de valores preenchido
        val pathDados = Path()
        val pontos = mutableListOf<Offset>()

        for (i in 0 until 5) {
            val angulo = (i * 2 * PI / 5 - PI / 2).toFloat()
            val rValor = raioMax * (valores[i] * animProgress).coerceIn(0.05f, 1f)
            val x = centroX + rValor * cos(angulo)
            val y = centroY + rValor * sin(angulo)
            pontos.add(Offset(x, y))
            if (i == 0) pathDados.moveTo(x, y) else pathDados.lineTo(x, y)
        }
        pathDados.close()

        // Preenchimento com glow
        drawPath(path = pathDados, color = corPreenchimento, style = Fill)
        drawPath(path = pathDados, color = corPrimaria, style = Stroke(width = 3.5f))

        // Vértices brilhantes
        pontos.forEach { ponto ->
            drawCircle(color = corPrimaria, radius = 6f, center = ponto)
            drawCircle(color = Color.White, radius = 3f, center = ponto)
        }
    }
}

@Composable
fun DialogoAvaliacaoCriterios(
    criteriosIniciais: CriteriosAvaliacao,
    tituloMidia: String,
    onSalvar: (CriteriosAvaliacao) -> Unit,
    onDispensar: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var atuacao by remember { mutableFloatStateOf(criteriosIniciais.atuacao) }
    var roteiro by remember { mutableFloatStateOf(criteriosIniciais.roteiro) }
    var visual by remember { mutableFloatStateOf(criteriosIniciais.visual) }
    var trilha by remember { mutableFloatStateOf(criteriosIniciais.trilha) }
    var ritmo by remember { mutableFloatStateOf(criteriosIniciais.ritmo) }

    val criteriosAtuais = remember(atuacao, roteiro, visual, trilha, ritmo) {
        CriteriosAvaliacao(atuacao, roteiro, visual, trilha, ritmo)
    }

    Dialog(onDismissRequest = onDispensar) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F1420),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                        Text("DNA CINEMATOGRÁFICO", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF00E5FF))
                    }
                    IconButton(onClick = onDispensar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = tituloMidia,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Gráfico Radar Animado
                GraficoRadarCine(
                    criterios = criteriosAtuais,
                    modifier = Modifier.size(190.dp).padding(vertical = 4.dp)
                )

                // Média geral calculada
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                        Text(
                            text = "Média Geral: ${String.format("%.1f", criteriosAtuais.media())} / 10",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                // Sliders de Avaliação
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ItemSliderCriterio("🎭 Atuação", atuacao) { atuacao = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    ItemSliderCriterio("📖 Roteiro", roteiro) { roteiro = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    ItemSliderCriterio("🎨 Visual & Efeitos", visual) { visual = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    ItemSliderCriterio("🎵 Trilha Sonora", trilha) { trilha = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    ItemSliderCriterio("⚡ Ritmo", ritmo) { ritmo = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                }

                Button(
                    onClick = {
                        onSalvar(criteriosAtuais)
                        onDispensar()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Salvar Avaliação Detalhada", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun ItemSliderCriterio(
    label: String,
    valor: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(115.dp))
        Slider(
            value = valor,
            onValueChange = onValueChange,
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier.weight(1f).height(30.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0xFF1E293B)
            )
        )
        Text(
            text = "${valor.toInt()}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}
