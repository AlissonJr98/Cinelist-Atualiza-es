package com.example.cinelist

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utilitário de tradução instantânea de comentários e publicações de usuários do CineList.
 * Permite traduzir posts de cinéfilos de qualquer país para o idioma ativo do usuário.
 */
object TradutorHelper {

    fun obterPrefixoIdiomaDestino(): String {
        val tag = I18n.idiomaAtivo
        return when {
            tag.startsWith("en", ignoreCase = true) -> "en"
            tag.startsWith("es", ignoreCase = true) -> "es"
            tag.startsWith("fr", ignoreCase = true) -> "fr"
            tag.startsWith("de", ignoreCase = true) -> "de"
            tag.startsWith("it", ignoreCase = true) -> "it"
            tag.startsWith("ja", ignoreCase = true) -> "ja"
            tag.startsWith("ko", ignoreCase = true) -> "ko"
            else -> "pt"
        }
    }

    /**
     * Converte um código de país ISO 3166-1 alpha-2 (ex: "BR", "US", "JP") no respectivo emoji de bandeira.
     */
    fun obterEmojiBandeira(codigoPais: String?): String {
        if (codigoPais.isNullOrBlank()) return ""
        val codigo = codigoPais.trim().uppercase()
        if (codigo.length != 2) return ""
        val primeiro = codigo[0]
        val segundo = codigo[1]
        if (primeiro !in 'A'..'Z' || segundo !in 'A'..'Z') return ""

        val flagOffset = 0x1F1E6
        val asciiOffset = 0x41
        val firstChar = Character.toChars(flagOffset + (primeiro.code - asciiOffset))
        val secondChar = Character.toChars(flagOffset + (segundo.code - asciiOffset))
        return String(firstChar) + String(secondChar)
    }

    /**
     * Traduz o texto fornecido para o idioma atual do usuário.
     */
    suspend fun traduzir(textoOriginal: String, idiomaDestino: String = obterPrefixoIdiomaDestino()): String? = withContext(Dispatchers.IO) {
        if (textoOriginal.isBlank()) return@withContext null
        try {
            val urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$idiomaDestino&dt=t&q=" + Uri.encode(textoOriginal)
            val url = URL(urlString)
            val conexao = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conexao.responseCode == 200) {
                val leitor = BufferedReader(InputStreamReader(conexao.inputStream, "UTF-8"))
                val resposta = leitor.use { it.readText() }
                conexao.disconnect()

                val json = JSONArray(resposta)
                val frasesArray = json.optJSONArray(0)
                if (frasesArray != null) {
                    val sb = java.lang.StringBuilder()
                    for (i in 0 until frasesArray.length()) {
                        val fraseItem = frasesArray.optJSONArray(i)
                        val parte = fraseItem?.optString(0)
                        if (!parte.isNullOrEmpty() && parte != "null") {
                            sb.append(parte)
                        }
                    }
                    val resultado = sb.toString().trim()
                    if (resultado.isNotBlank()) {
                        return@withContext resultado
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }
}
