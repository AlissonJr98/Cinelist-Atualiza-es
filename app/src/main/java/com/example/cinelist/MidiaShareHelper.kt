package com.example.cinelist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class DadosCompartilhamentoMidia(
    val idTmdb: Int = 0,
    val titulo: String,
    val tituloOriginal: String = "",
    val tipo: String = "Filme",
    val ano: String = "",
    val generos: String = "",
    val nota: Int = 0,
    val notaMediaTmdb: Double = 0.0,
    val sinopse: String = "",
    val urlPoster: String = "",
    val status: String = "",
    val temporadaAtual: Int = 0,
    val episodioAtual: Int = 0,
    val comentario: String = "",
    val provedores: List<String> = emptyList(),
    val temDublagem: Boolean? = null
)

object MidiaShareHelper {

    suspend fun carregarPosterBitmap(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = ImageLoader(context).execute(request)
            if (result is SuccessResult) {
                return@withContext result.drawable.toBitmap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    fun gerarTextoFormatado(dados: DadosCompartilhamentoMidia): String {
        val sb = StringBuilder()
        val tipoIcone = when (dados.tipo.lowercase()) {
            "série", "serie" -> "📺"
            "anime" -> "⚡"
            else -> "🎬"
        }

        val anoTexto = if (dados.ano.isNotBlank()) " (${dados.ano})" else ""
        sb.append("$tipoIcone *${dados.titulo}*$anoTexto\n")

        if (dados.generos.isNotBlank()) {
            sb.append("🏷️ ${dados.generos}\n")
        }

        if (dados.nota > 0) {
            val estrelas = "★".repeat(dados.nota.coerceIn(1, 5)) + "☆".repeat((5 - dados.nota).coerceAtLeast(0))
            sb.append("⭐ ${"Avaliação:".t()} $estrelas (${dados.nota}/5)\n")
        } else if (dados.notaMediaTmdb > 0.0) {
            sb.append("⭐ TMDB: ${String.format(Locale.US, "%.1f", dados.notaMediaTmdb)}/10\n")
        }

        if (dados.temporadaAtual > 0 && (dados.tipo.contains("série", true) || dados.tipo.contains("anime", true))) {
            sb.append("📍 ${formatarTemporadaEpisodio(dados.temporadaAtual, dados.episodioAtual)}\n")
        }

        if (dados.temDublagem == true) {
            sb.append("🎙️ ${"Dublagem PT-BR disponível".t()}\n")
        }

        if (dados.provedores.isNotEmpty()) {
            sb.append("📺 ${"Onde assistir:".t()} ${dados.provedores.joinToString(", ")}\n")
        }

        if (dados.comentario.isNotBlank() && !dados.comentario.startsWith("Marcado como", true)) {
            sb.append("\n💬 \"${dados.comentario}\"\n")
        } else if (dados.sinopse.isNotBlank()) {
            val sinopseCurta = if (dados.sinopse.length > 200) dados.sinopse.take(197) + "..." else dados.sinopse
            sb.append("\n📝 $sinopseCurta\n")
        }

        sb.append("\n🍿 ${"Acompanhado pelo app CineList 🎬".t()}\nhttps://github.com/AlissonJr98/Cinelist-Atualiza-es")
        return sb.toString()
    }

    fun gerarCineCardBitmap(context: Context, dados: DadosCompartilhamentoMidia, posterBitmap: Bitmap?): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Fundo Gradiente Cibernético Escuro
        val paintFundo = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#060913"),
                    Color.parseColor("#0B132B"),
                    Color.parseColor("#020617")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintFundo)

        // Moldura Externa Holográfica
        val paintBorda = Paint().apply {
            color = Color.parseColor("#7C4DFF")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(40f, 40f, width - 40f, height - 40f), 48f, 48f, paintBorda)

        // Glow interno ciano suave
        val paintBordaGlow = Paint().apply {
            color = Color.parseColor("#2200E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 14f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(40f, 40f, width - 40f, height - 40f), 48f, 48f, paintBordaGlow)

        // Header - CineList Branding Pill
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
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("CINELIST // DIÁRIO DE CINEMA".t(), width / 2f, 163f, paintPillText)

        // 2. Poster Central da Mídia
        val posterLeft = 240f
        val posterTop = 240f
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

        // Borda do poster ciano neon
        val paintPosterBorda = Paint().apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRoundRect(rectPoster, 32f, 32f, paintPosterBorda)

        // Badge de Dublagem sobre o poster
        if (dados.temDublagem == true) {
            val paintDubBadge = Paint().apply {
                color = Color.parseColor("#CC0F172A")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val paintDubBorda = Paint().apply {
                color = Color.parseColor("#10B981")
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            val paintDubText = Paint().apply {
                color = Color.parseColor("#10B981")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val rectDub = RectF(posterLeft + 20f, posterTop + 20f, posterLeft + 320f, posterTop + 75f)
            canvas.drawRoundRect(rectDub, 16f, 16f, paintDubBadge)
            canvas.drawRoundRect(rectDub, 16f, 16f, paintDubBorda)
            canvas.drawText("🎙️ DUBLADO PT-BR".t(), rectDub.centerX(), rectDub.centerY() + 10f, paintDubText)
        }

        // 3. Título da obra
        val paintTitulo = Paint().apply {
            color = Color.WHITE
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val tituloExibicao = if (dados.titulo.length > 28) dados.titulo.take(26) + "..." else dados.titulo
        canvas.drawText(tituloExibicao, width / 2f, posterBottom + 90f, paintTitulo)

        // 4. Subtítulo (Tipo • Ano • Gênero)
        val paintSub = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 34f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val generoCurto = if (dados.generos.length > 32) dados.generos.take(30) + "..." else dados.generos
        val anoStr = if (dados.ano.isNotBlank()) " • ${dados.ano}" else ""
        val genStr = if (generoCurto.isNotBlank()) " • $generoCurto" else ""
        canvas.drawText("${dados.tipo.uppercase()}$anoStr$genStr", width / 2f, posterBottom + 150f, paintSub)

        // 5. Estrelas de Avaliação / Nota
        val notaEfetiva = if (dados.nota > 0) dados.nota else (dados.notaMediaTmdb / 2.0).toInt().coerceIn(1, 5)
        val paintEstrelas = Paint().apply {
            color = Color.parseColor("#FFD700")
            textSize = 58f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val estrelasTexto = "★ ".repeat(notaEfetiva) + "☆ ".repeat((5 - notaEfetiva).coerceAtLeast(0))
        canvas.drawText(estrelasTexto.trim(), width / 2f, posterBottom + 230f, paintEstrelas)

        // 6. Card de Comentário / Sinopse
        val cardResenhaTop = posterBottom + 280f
        val cardResenhaBottom = cardResenhaTop + 190f
        val rectResenha = RectF(120f, cardResenhaTop, width - 120f, cardResenhaBottom)

        val paintCardResenha = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(rectResenha, 24f, 24f, paintCardResenha)

        val paintCardResenhaBorda = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(rectResenha, 24f, 24f, paintCardResenhaBorda)

        val frase = when {
            dados.comentario.isNotBlank() && !dados.comentario.startsWith("Marcado como", true) -> dados.comentario
            dados.sinopse.isNotBlank() -> dados.sinopse
            else -> "Experiência cinematográfica imperdível registrada no CineList!".t()
        }

        val paintTextoFrase = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val fraseLinha = if (frase.length > 50) frase.take(48) + "..." else frase
        canvas.drawText("“$fraseLinha”", width / 2f, cardResenhaTop + 105f, paintTextoFrase)

        // 7. Chips de Onde Assistir (se houver)
        if (dados.provedores.isNotEmpty()) {
            val provTexto = "${"Disponível em:".t()} ${dados.provedores.take(3).joinToString(" • ")}"
            val paintProv = Paint().apply {
                color = Color.parseColor("#38BDF8")
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(provTexto, width / 2f, cardResenhaBottom + 60f, paintProv)
        }

        // 8. Rodapé com Call To Action
        val paintRodape = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Disponível no app CineList // Compartilhado com paixão cinéfila ✨".t(), width / 2f, 1820f, paintRodape)

        return bitmap
    }

    fun salvarBitmapEObterUri(context: Context, bitmap: Bitmap): Uri {
        val pastaCache = File(context.cacheDir, "cinelist_imagens")
        if (!pastaCache.exists()) pastaCache.mkdirs()
        val arquivoImagem = File(pastaCache, "cinecard_share.png")
        val fos = FileOutputStream(arquivoImagem)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()

        val autoridade = "${context.packageName}.provider"
        return FileProvider.getUriForFile(context, autoridade, arquivoImagem)
    }

    fun compartilharGeral(context: Context, texto: String, uriImagem: Uri? = null) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                if (uriImagem != null) {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uriImagem)
                    clipData = ClipData.newRawUri("CineCard", uriImagem)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, texto)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Compartilhar Mídia via".t()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao abrir seletor de compartilhamento.".t(), Toast.LENGTH_SHORT).show()
        }
    }

    fun compartilharWhatsApp(context: Context, texto: String, uriImagem: Uri? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            if (uriImagem != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uriImagem)
                clipData = ClipData.newRawUri("CineCard", uriImagem)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, texto)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(texto)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                compartilharGeral(context, texto, uriImagem)
            }
        }
    }

    fun compartilharInstagramStories(context: Context, uriImagem: Uri, textoFallback: String) {
        val instagramIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(uriImagem, "image/png")
            putExtra(Intent.EXTRA_STREAM, uriImagem)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("source_application", context.packageName)
            setPackage("com.instagram.android")
        }

        if (instagramIntent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(instagramIntent)
            } catch (e: Exception) {
                compartilharGeral(context, textoFallback, uriImagem)
            }
        } else {
            Toast.makeText(context, "Instagram não instalado. Abrindo compartilhamento geral...".t(), Toast.LENGTH_SHORT).show()
            compartilharGeral(context, textoFallback, uriImagem)
        }
    }

    fun compartilharTwitterX(context: Context, texto: String, uriImagem: Uri? = null) {
        val twitterIntent = Intent(Intent.ACTION_SEND).apply {
            if (uriImagem != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uriImagem)
                clipData = ClipData.newRawUri("CineCard", uriImagem)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, texto)
            setPackage("com.twitter.android")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(twitterIntent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/intent/tweet?text=${Uri.encode(texto)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                compartilharGeral(context, texto, uriImagem)
            }
        }
    }

    fun copiarTexto(context: Context, texto: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("CineList Resumo", texto)
            clipboard.setPrimaryClip(clip)
            CineHaptics.confirm(context)
            Toast.makeText(context, "Resumo copiado para a área de transferência! 📋".t(), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Não foi possível copiar o texto.".t(), Toast.LENGTH_SHORT).show()
        }
    }
}
