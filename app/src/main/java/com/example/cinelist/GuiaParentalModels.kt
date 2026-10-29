package com.example.cinelist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SeloClassificacaoEtaria(
    val sigla: String,
    val descricao: String,
    val corFundo: Color,
    val corTexto: Color = Color.White
)

data class AvisoGatilho(
    val icone: String,
    val titulo: String,
    val detalhe: String,
    val nivel: String // "LEVE", "MODERADO", "INTENSO"
)

data class GuiaParentalInfo(
    val classificacao: SeloClassificacaoEtaria,
    val avisos: List<AvisoGatilho>,
    val recomendacaoFamilia: String
)

object GuiaParentalDetector {

    fun detectarGuiaParental(
        sinopse: String,
        generos: String,
        tipo: String = "Filme",
        notaCritica: Double = 0.0
    ): GuiaParentalInfo {
        val s = sinopse.lowercase()
        val g = generos.lowercase()

        val avisos = mutableListOf<AvisoGatilho>()

        // 1. Violência
        if (s.contains("assassin") || s.contains("crime") || s.contains("tiroteio") || s.contains("guerra") || s.contains("sangue") || s.contains("tortura") || g.contains("guerra") || s.contains("massacre")) {
            val intensa = s.contains("tortura") || s.contains("massacre") || s.contains("sangue")
            avisos.add(
                AvisoGatilho(
                    icone = "🩸",
                    titulo = "Violência",
                    detalhe = if (intensa) "Cenas de ação gráfica e confronto violento" else "Confrontos físicos e perseguições",
                    nivel = if (intensa) "INTENSO" else "MODERADO"
                )
            )
        }

        // 2. Medo / Suspense / Jumpscares
        if (g.contains("terror") || g.contains("horror") || g.contains("suspense") || s.contains("demônio") || s.contains("fantasma") || s.contains("psicopata") || s.contains("monstro")) {
            val intenso = g.contains("terror") || s.contains("demônio") || s.contains("possuído")
            avisos.add(
                AvisoGatilho(
                    icone = "👻",
                    titulo = "Medo & Tensão",
                    detalhe = if (intenso) "Atmosfera aterrorizante e momentos de susto" else "Tensão psicológica e suspense contínuo",
                    nivel = if (intenso) "INTENSO" else "MODERADO"
                )
            )
        }

        // 3. Conteúdo Sensual / Nudez
        if (s.contains("erótic") || s.contains("sensual") || s.contains("nudez") || s.contains("amante") || s.contains("sedução")) {
            avisos.add(
                AvisoGatilho(
                    icone = "🔞",
                    titulo = "Conteúdo Adulto",
                    detalhe = "Temas íntimos e insinuações adultas",
                    nivel = "MODERADO"
                )
            )
        }

        // 4. Drogas / Substâncias
        if (s.contains("droga") || s.contains("tráfico") || s.contains("cocaína") || s.contains("álcool") || s.contains("vício") || s.contains("overdose")) {
            avisos.add(
                AvisoGatilho(
                    icone = "🚬",
                    titulo = "Substâncias",
                    detalhe = "Menção ou consumo de álcool, drogas ou medicamentos",
                    nivel = "MODERADO"
                )
            )
        }

        // 5. Luto / Doença / Temas Sensíveis
        if (s.contains("luto") || s.contains("câncer") || s.contains("morte da mãe") || s.contains("morte do pai") || s.contains("suicídio") || s.contains("depressão") || s.contains("perda dolorosa")) {
            avisos.add(
                AvisoGatilho(
                    icone = "💔",
                    titulo = "Carga Emocional",
                    detalhe = "Temas fortes de luto, perda ou superação pessoal",
                    nivel = "LEVE"
                )
            )
        }

        // 6. Infere a classificação indicativa brasileira
        val classificacao = when {
            avisos.any { it.nivel == "INTENSO" } || s.contains("massacre") || s.contains("tortura") -> {
                SeloClassificacaoEtaria("18", "Não recomendado para menores de 18 anos", Color(0xFF000000))
            }
            avisos.size >= 2 || g.contains("terror") || g.contains("crime") || s.contains("assassin") -> {
                SeloClassificacaoEtaria("16", "Não recomendado para menores de 16 anos", Color(0xFFDC2626))
            }
            avisos.isNotEmpty() || g.contains("ação") || g.contains("suspense") -> {
                SeloClassificacaoEtaria("14", "Não recomendado para menores de 14 anos", Color(0xFFF97316))
            }
            g.contains("comédia") || g.contains("aventura") || g.contains("fantasia") -> {
                SeloClassificacaoEtaria("10", "Não recomendado para menores de 10 anos", Color(0xFF3B82F6))
            }
            else -> {
                SeloClassificacaoEtaria("L", "Livre para todos os públicos", Color(0xFF10B981))
            }
        }

        val recomendacao = when (classificacao.sigla) {
            "L" -> "✅ Sessão ideal para curtir com toda a família e crianças de qualquer idade."
            "10" -> "👨‍👩‍👧 Ótimo entretenimento familiar, com leve supervisão para os pequeninos."
            "12", "14" -> "⚠️ Indicado para jovens e adultos. Contém cenas leves de conflito ou tensão."
            "16" -> "🔞 Sessão madura com temas complexos, ideal para público jovem/adulto."
            else -> "🚫 Conteúdo adulto restrito com fortes momentos de tensão ou impacto visual."
        }

        return GuiaParentalInfo(
            classificacao = classificacao,
            avisos = avisos,
            recomendacaoFamilia = recomendacao
        )
    }
}

@Composable
fun GuiaParentalCard(
    info: GuiaParentalInfo,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, info.classificacao.corFundo.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Linha com o Selo Oficial e Título
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selo estilizado
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = info.classificacao.corFundo,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = info.classificacao.sigla,
                            color = info.classificacao.corTexto,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Guia Parental & Conteúdo".t(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = info.classificacao.descricao.t(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dica familiar
            Text(
                text = info.recomendacaoFamilia.t(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )

            // Avisos de Gatilho / Tags
            if (info.avisos.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    info.avisos.take(3).forEach { aviso ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (aviso.nivel) {
                                "INTENSO" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                "MODERADO" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                else -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when (aviso.nivel) {
                                    "INTENSO" -> Color(0xFFEF4444).copy(alpha = 0.5f)
                                    "MODERADO" -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                                    else -> Color(0xFF3B82F6).copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(aviso.icone, fontSize = 12.sp)
                                Text(
                                    aviso.titulo.t(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
