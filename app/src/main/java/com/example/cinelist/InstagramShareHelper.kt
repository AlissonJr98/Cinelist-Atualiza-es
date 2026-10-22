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

    /**
     * Gera um CineCard vertical no padrão Instagram Stories (1080x1920) para um filme ou série específico.
     */
    fun compartilharCineCardStory(
        context: Context,
        titulo: String,
        tipo: String,
        ano: String,
        genero: String,
        nota: Int,
        comentario: String,
        posterBitmap: Bitmap? = null
    ) {
        try {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Fundo escuro com degradê cinematográfico
            val paintFundo = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        Color.parseColor("#060913"),
                        Color.parseColor("#0F172A"),
                        Color.parseColor("#020617")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintFundo)

            // Moldura externa neon violeta/ciano
            val paintBorda = Paint().apply {
                color = Color.parseColor("#7C4DFF")
                style = Paint.Style.STROKE
                strokeWidth = 6f
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(40f, 40f, width - 40f, height - 40f), 48f, 48f, paintBorda)

            // Header - CineList Branding Pill
            val paintHeaderPill = Paint().apply {
                color = Color.parseColor("#1E1B4B")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(width / 2f - 240f, 110f, width / 2f + 240f, 190f), 40f, 40f, paintHeaderPill)

            val paintPillText = Paint().apply {
                color = Color.parseColor("#A78BFA")
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("CINELIST // DIÁRIO DE CINEMA", width / 2f, 163f, paintPillText)

            // 2. Poster do filme ou placeholder estilizado
            val posterLeft = 240f
            val posterTop = 260f
            val posterRight = width - 240f
            val posterBottom = posterTop + 850f
            val rectPoster = RectF(posterLeft, posterTop, posterRight, posterBottom)

            if (posterBitmap != null) {
                val paintBitmap = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
                val pathPoster = Path().apply {
                    addRoundRect(rectPoster, 32f, 32f, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(pathPoster)
                canvas.drawBitmap(posterBitmap, null, rectPoster, paintBitmap)
                canvas.restore()
            } else {
                val paintPlaceholder = Paint().apply {
                    color = Color.parseColor("#1E293B")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRoundRect(rectPoster, 32f, 32f, paintPlaceholder)

                val paintClaquete = Paint().apply {
                    color = Color.parseColor("#94A3B8")
                    textSize = 120f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("🎬", width / 2f, posterTop + 450f, paintClaquete)
            }

            // Borda do poster
            val paintPosterBorda = Paint().apply {
                color = Color.parseColor("#38BDF8")
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
            }
            canvas.drawRoundRect(rectPoster, 32f, 32f, paintPosterBorda)

            // 3. Título da obra
            val paintTitulo = Paint().apply {
                color = Color.WHITE
                textSize = 64f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val tituloExibicao = if (titulo.length > 28) titulo.take(25) + "..." else titulo
            canvas.drawText(tituloExibicao, width / 2f, 1200f, paintTitulo)

            // 4. Badges de Tipo, Ano e Gênero
            val paintSub = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 34f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val anoTxt = if (ano.isNotBlank()) " • $ano" else ""
            val generoTxt = if (genero.isNotBlank()) " • ${genero.split(",").firstOrNull()?.trim() ?: ""}" else ""
            canvas.drawText("${tipo.uppercase()}$anoTxt$generoTxt", width / 2f, 1260f, paintSub)

            // 5. Avaliação em Estrelas
            val paintEstrelas = Paint().apply {
                color = Color.parseColor("#FBBF24")
                textSize = 58f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val estrelasCount = if (nota > 5) (nota + 1) / 2 else nota
            val estrelasStr = "★".repeat(estrelasCount.coerceIn(0, 5)) + "☆".repeat((5 - estrelasCount).coerceIn(0, 5))
            canvas.drawText(estrelasStr, width / 2f, 1360f, paintEstrelas)

            // 6. Resenha / Citação do usuário
            val cardResenhaTop = 1430f
            val cardResenhaBottom = 1680f
            val rectResenha = RectF(100f, cardResenhaTop, width - 100f, cardResenhaBottom)
            val paintResenhaBg = Paint().apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(rectResenha, 28f, 28f, paintResenhaBg)

            val paintResenhaBorda = Paint().apply {
                color = Color.parseColor("#334155")
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(rectResenha, 28f, 28f, paintResenhaBorda)

            val paintAspas = Paint().apply {
                color = Color.parseColor("#38BDF8")
                textSize = 60f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("“", 130f, cardResenhaTop + 70f, paintAspas)

            val frase = if (comentario.isNotBlank()) comentario else "Experiência cinematográfica imperdível registrada no CineList!"
            val paintTextoFrase = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val fraseLinha1 = if (frase.length > 40) frase.take(38) + "..." else frase
            canvas.drawText(fraseLinha1, width / 2f, cardResenhaTop + 140f, paintTextoFrase)

            // 7. Rodapé com Call To Action
            val paintRodape = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 30f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Disponível no app CineList // @cinelistapp", width / 2f, 1800f, paintRodape)

            // Salva na cache
            val pastaCache = File(context.cacheDir, "cinelist_imagens")
            if (!pastaCache.exists()) pastaCache.mkdirs()
            val arquivoImagem = File(pastaCache, "cinecard_story.png")
            val fos = FileOutputStream(arquivoImagem)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            val autoridade = "${context.packageName}.provider"
            val uriImagem = FileProvider.getUriForFile(context, autoridade, arquivoImagem)

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
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uriImagem)
                    putExtra(Intent.EXTRA_TEXT, "Confira minha avaliação de '$titulo' no CineList! 🍿✨")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar CineCard"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao gerar CineCard para o Story.", Toast.LENGTH_LONG).show()
        }
    }
}
