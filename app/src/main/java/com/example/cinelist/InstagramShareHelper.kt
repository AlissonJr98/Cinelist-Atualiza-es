package com.example.cinelist

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object InstagramShareHelper {

    fun compartilharWrappedNoInstagram(context: Context, dados: CineWrappedData) {
        try {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Fundo escuro com gradiente cibernético
            val paintFundo = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        Color.parseColor("#0B0E17"),
                        Color.parseColor("#071228"),
                        Color.parseColor("#050811")
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintFundo)

            // Moldura holográfica externa
            val paintBorda = Paint().apply {
                color = Color.parseColor("#1DE9B6")
                style = Paint.Style.STROKE
                strokeWidth = 6f
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(40f, 40f, width - 40f, height - 40f), 48f, 48f, paintBorda)

            // Borda interna de glow ciano
            val paintBordaGlow = Paint().apply {
                color = Color.parseColor("#3300E5FF")
                style = Paint.Style.STROKE
                strokeWidth = 14f
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(40f, 40f, width - 40f, height - 40f), 48f, 48f, paintBordaGlow)

            // Header - Logo CineList
            val paintHeaderPill = Paint().apply {
                color = Color.parseColor("#152238")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(width / 2f - 240f, 110f, width / 2f + 240f, 190f), 40f, 40f, paintHeaderPill)

            val paintHeaderPillStroke = Paint().apply {
                color = Color.parseColor("#00E5FF")
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(width / 2f - 240f, 110f, width / 2f + 240f, 190f), 40f, 40f, paintHeaderPillStroke)

            val paintPillText = Paint().apply {
                color = Color.parseColor("#00E5FF")
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("CINELIST // TELEMETRIA", width / 2f, 163f, paintPillText)

            // Título Principal
            val paintTitulo = Paint().apply {
                color = Color.WHITE
                textSize = 72f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("RETROSPECTIVA CINÉFILA", width / 2f, 300f, paintTitulo)

            val paintSub = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 36f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("SEU HISTÓRICO DE MARATONAS E CONQUISTAS", width / 2f, 360f, paintSub)

            // Função para desenhar cards holográficos
            fun desenharCard(top: Float, bottom: Float, label: String, valor: String, destaqueCor: Int) {
                val left = 90f
                val right = width - 90f
                val rect = RectF(left, top, right, bottom)

                val paintCardBg = Paint().apply {
                    color = Color.parseColor("#121A2A")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRoundRect(rect, 32f, 32f, paintCardBg)

                val paintCardBorder = Paint().apply {
                    color = destaqueCor
                    style = Paint.Style.STROKE
                    strokeWidth = 3.5f
                    alpha = 180
                    isAntiAlias = true
                }
                canvas.drawRoundRect(rect, 32f, 32f, paintCardBorder)

                val paintLabel = Paint().apply {
                    color = Color.parseColor("#64748B")
                    textSize = 32f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText(label.uppercase(), width / 2f, top + 65f, paintLabel)

                val paintValor = Paint().apply {
                    color = destaqueCor
                    textSize = 78f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText(valor, width / 2f, top + 155f, paintValor)
            }

            // Cards de estatísticas
            desenharCard(430f, 630f, "Tempo em Maratona", "${dados.horasTotaisAssistidas} Horas", Color.parseColor("#00E5FF"))
            desenharCard(670f, 870f, "Títulos Concluídos", "${dados.totalTitulosConcluidos} Obras", Color.parseColor("#10B981"))
            desenharCard(910f, 1110f, "Gênero Predominante", dados.generoFavorito.ifBlank { "Geral" }, Color.parseColor("#A855F7"))

            val destaque = if (dados.filmeOuSerieDestaque.length > 24) dados.filmeOuSerieDestaque.take(21) + "..." else dados.filmeOuSerieDestaque
            desenharCard(1150f, 1350f, "Destaque da Jornada", destaque.ifBlank { "CineList" }, Color.parseColor("#F59E0B"))

            // Frase de Personalidade
            if (dados.frasePersonalizada.isNotBlank()) {
                val paintFrase = Paint().apply {
                    color = Color.parseColor("#E2E8F0")
                    textSize = 34f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val fraseEncurtada = if (dados.frasePersonalizada.length > 45) dados.frasePersonalizada.take(42) + "..." else dados.frasePersonalizada
                canvas.drawText("“$fraseEncurtada”", width / 2f, 1460f, paintFrase)
            }

            // Badge / Rank Cinéfilo
            val paintRank = Paint().apply {
                color = Color.parseColor("#FF007F")
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("✦ PATENTE: COMANDANTE CINE ✦", width / 2f, 1620f, paintRank)

            // Rodapé
            val paintRodape = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 32f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("App CineList // Sincronizado na Nuvem", width / 2f, 1800f, paintRodape)

            // Salva na cache
            val pastaCache = File(context.cacheDir, "cinelist_imagens")
            if (!pastaCache.exists()) pastaCache.mkdirs()
            val arquivoImagem = File(pastaCache, "wrapped_story.png")
            val fos = FileOutputStream(arquivoImagem)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            val autoridade = "${context.packageName}.provider"
            val uriImagem = FileProvider.getUriForFile(context, autoridade, arquivoImagem)

            // Tenta abrir direto no Instagram Stories
            val instagramIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uriImagem, "image/png")
                putExtra(Intent.EXTRA_STREAM, uriImagem)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra("source_application", context.packageName)
                setPackage("com.instagram.android")
            }

            if (instagramIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(instagramIntent)
            } else {
                // Fallback elegante: compartilha com WhatsApp, Telegram, Stories, etc.
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uriImagem)
                    putExtra(Intent.EXTRA_TEXT, "Minha retrospectiva cinematográfica no CineList! 🍿✨")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar CineStats Retrospectiva"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao gerar imagem de retrospectiva.", Toast.LENGTH_LONG).show()
        }
    }
}
