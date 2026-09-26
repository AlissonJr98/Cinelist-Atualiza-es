package com.example.cinelist

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object InstagramShareHelper {

    fun compartilharWrappedNoInstagram(context: Context, dados: CineWrappedData) {
        try {
            // 1. Criar um Bitmap no formato de Stories (1080x1920)
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Fundo com Gradiente Premium (Azul/Ciano escuro para Preto)
            val paintFundo = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#00BFFF"), Color.parseColor("#090A0B"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintFundo)

            // Configuração dos textos
            val paintTitulo = Paint().apply {
                color = Color.WHITE
                textSize = 85f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val paintDestaque = Paint().apply {
                color = Color.parseColor("#FFD700")
                textSize = 130f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val paintTextoNormal = Paint().apply {
                color = Color.LTGRAY
                textSize = 55f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            // Desenhando os dados no Canvas
            var yOffset = 300f
            canvas.drawText("🍿 Meu CineList Wrapped", width / 2f, yOffset, paintTitulo)

            yOffset += 250f
            canvas.drawText("TEMPO DE MARATONA", width / 2f, yOffset, paintTextoNormal)
            yOffset += 130f
            canvas.drawText("${dados.horasTotaisAssistidas} Horas", width / 2f, yOffset, paintDestaque)

            yOffset += 220f
            canvas.drawText("TÍTULOS CONCLUÍDOS", width / 2f, yOffset, paintTextoNormal)
            yOffset += 130f
            canvas.drawText("${dados.totalTitulosConcluidos}", width / 2f, yOffset, paintDestaque)

            yOffset += 220f
            canvas.drawText("GÊNERO FAVORITO", width / 2f, yOffset, paintTextoNormal)
            yOffset += 130f
            canvas.drawText(dados.generoFavorito, width / 2f, yOffset, paintDestaque)

            yOffset += 250f
            canvas.drawText("⭐ DESTAQUE DO ANO", width / 2f, yOffset, paintTextoNormal)
            yOffset += 130f
            val destaqueCurto = if (dados.filmeOuSerieDestaque.length > 25) dados.filmeOuSerieDestaque.take(22) + "..." else dados.filmeOuSerieDestaque
            canvas.drawText(destaqueCurto, width / 2f, yOffset, paintTitulo)

            // Rodapé
            canvas.drawText("Gerado pelo app CineList", width / 2f, 1800f, paintTextoNormal)

            // 2. Salvar a imagem temporariamente na cache do dispositivo
            val pastaCache = File(context.cacheDir, "cinelist_imagens")
            if (!pastaCache.exists()) pastaCache.mkdirs()
            val arquivoImagem = File(pastaCache, "wrapped_story.png")
            val fos = FileOutputStream(arquivoImagem)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            // 3. Obter URI segura com FileProvider (Corrigido para bater com o Manifest)
            val autoridade = "${context.packageName}.provider"
            val uriImagem = FileProvider.getUriForFile(context, autoridade, arquivoImagem)

            // 4. Criar Intent para o Instagram Stories (Versão Robusta)
            val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uriImagem, "image/png")
                putExtra(Intent.EXTRA_STREAM, uriImagem) // Anexo explícito para evitar tela preta
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra("source_application", context.packageName)
                setPackage("com.instagram.android")
            }

            // Tentar abrir o Instagram
            val activityManager = context.packageManager
            if (intent.resolveActivity(activityManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Instagram não está instalado neste aparelho.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao abrir o Instagram. Verifique as permissões.", Toast.LENGTH_LONG).show()
        }
    }
}