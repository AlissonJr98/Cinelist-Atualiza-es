package com.example.cinelist

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeCyberHelper {
    fun gerarQrCodeBitmap(
        conteudo: String,
        tamanho: Int = 512,
        corPontos: Int = Color.parseColor("#00E5FF"), // Neon Cyan
        corFundo: Int = Color.parseColor("#0B0E14")   // Obsidian Dark
    ): Bitmap? {
        if (conteudo.isBlank()) return null
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, tamanho, tamanho, hints)
            val bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888)
            for (x in 0 until tamanho) {
                for (y in 0 until tamanho) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) corPontos else corFundo)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
