package com.example.cinelist

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Gerenciador inteligente de localização para Títulos e Pôsteres de mídias salvas no CineList.
 * 
 * Permite que mídias adicionadas em português ("O Novato", "Interestelar", etc.)
 * sejam exibidas no idioma atualmente selecionado pelo usuário ("The Rookie", "Interstellar", etc.),
 * juntamente com a arte de capa oficial do TMDB naquele idioma.
 */
object MidiaLocalizationManager {

    fun limparCache() {
        cacheLocalizado.clear()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val PREFS_NAME = "midia_localization_cache"

    // Cache reativo em memória (chave: "$idTmdb-$idioma", valor: Pair(titulo, caminhoPoster))
    private val cacheLocalizado = mutableStateMapOf<String, Pair<String, String>>()

    // Dicionário pré-carregado para títulos consagrados mais comuns (resposta instantânea em 0ms)
    private val titulosPreCarregados = mapOf(
        // O Novato (The Rookie)
        79744 to mapOf(
            "en" to Pair("The Rookie", "/w2zXyN4w28HjO0lE9xLd6p9g1zT.jpg"),
            "es" to Pair("The Rookie", "/w2zXyN4w28HjO0lE9xLd6p9g1zT.jpg"),
            "fr" to Pair("The Rookie : Le Flic de Los Angeles", "/w2zXyN4w28HjO0lE9xLd6p9g1zT.jpg"),
            "de" to Pair("The Rookie", "/w2zXyN4w28HjO0lE9xLd6p9g1zT.jpg"),
            "it" to Pair("The Rookie", "/w2zXyN4w28HjO0lE9xLd6p9g1zT.jpg"),
            "ja" to Pair("ザ・ルーキー", "/w2zXyN4w28HjO0lE9xLd6p9g1zT.jpg"),
            "ko" to Pair("더 루키", "/w2zXyN4w28HjO0lE9xLd6p9g1zT.jpg")
        ),
        // Interestelar
        157336 to mapOf(
            "en" to Pair("Interstellar", "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"),
            "es" to Pair("Interstellar", "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"),
            "fr" to Pair("Interstellar", "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"),
            "de" to Pair("Interstellar", "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"),
            "it" to Pair("Interstellar", "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"),
            "ja" to Pair("インターステラー", "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"),
            "ko" to Pair("인터스텔라", "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg")
        ),
        // A Chegada
        329865 to mapOf(
            "en" to Pair("Arrival", "/x2O0vG29xRP69KaVhhAg4GB93hE.jpg"),
            "es" to Pair("La llegada", "/x2O0vG29xRP69KaVhhAg4GB93hE.jpg"),
            "fr" to Pair("Premier Contact", "/x2O0vG29xRP69KaVhhAg4GB93hE.jpg"),
            "de" to Pair("Arrival", "/x2O0vG29xRP69KaVhhAg4GB93hE.jpg"),
            "it" to Pair("Arrival", "/x2O0vG29xRP69KaVhhAg4GB93hE.jpg"),
            "ja" to Pair("メッセージ", "/x2O0vG29xRP69KaVhhAg4GB93hE.jpg"),
            "ko" to Pair("컨택트", "/x2O0vG29xRP69KaVhhAg4GB93hE.jpg")
        ),
        // Perdido em Marte
        286217 to mapOf(
            "en" to Pair("The Martian", "/5BHuvQ6p9kL0j0k7B1B6jL0j0k7.jpg"),
            "es" to Pair("Marte (The Martian)", "/5BHuvQ6p9kL0j0k7B1B6jL0j0k7.jpg"),
            "fr" to Pair("Seul sur Mars", "/5BHuvQ6p9kL0j0k7B1B6jL0j0k7.jpg"),
            "de" to Pair("Der Marsianer", "/5BHuvQ6p9kL0j0k7B1B6jL0j0k7.jpg"),
            "it" to Pair("Sopravvissuto - The Martian", "/5BHuvQ6p9kL0j0k7B1B6jL0j0k7.jpg"),
            "ja" to Pair("オデッセイ", "/5BHuvQ6p9kL0j0k7B1B6jL0j0k7.jpg"),
            "ko" to Pair("마션", "/5BHuvQ6p9kL0j0k7B1B6jL0j0k7.jpg")
        ),
        // Vingadores: Ultimato
        299534 to mapOf(
            "en" to Pair("Avengers: Endgame", "/or06FN3Dka5tukK1e9sl16pB3iy.jpg"),
            "es" to Pair("Vengadores: Endgame", "/or06FN3Dka5tukK1e9sl16pB3iy.jpg"),
            "fr" to Pair("Avengers: Endgame", "/or06FN3Dka5tukK1e9sl16pB3iy.jpg"),
            "de" to Pair("Avengers: Endgame", "/or06FN3Dka5tukK1e9sl16pB3iy.jpg"),
            "it" to Pair("Avengers: Endgame", "/or06FN3Dka5tukK1e9sl16pB3iy.jpg"),
            "ja" to Pair("アベンジャーズ/エンドゲーム", "/or06FN3Dka5tukK1e9sl16pB3iy.jpg"),
            "ko" to Pair("어벤져스: 엔드게임", "/or06FN3Dka5tukK1e9sl16pB3iy.jpg")
        )
    )

    private fun obterPrefixoIdioma(): String {
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
     * Retorna o título da mídia localizado no idioma atualmente ativo.
     */
    fun obterTituloLocalizado(midia: Midia): String {
        val lang = obterPrefixoIdioma()
        if (lang == "pt") return midia.titulo

        if (midia.idTmdb <= 0) {
            return midia.titulo.t()
        }

        val chave = "${midia.idTmdb}-$lang"

        // 1. Verifica cache reativo em memória
        cacheLocalizado[chave]?.let { return it.first }

        // 2. Verifica dicionário pré-carregado
        titulosPreCarregados[midia.idTmdb]?.get(lang)?.let {
            cacheLocalizado[chave] = it
            return it.first
        }

        // 3. Verifica cache persistente no SharedPreferences
        val context = CineListApplication.instance
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val salvoTitulo = prefs.getString("tit_$chave", null)
            val salvoPoster = prefs.getString("pos_$chave", null)
            if (!salvoTitulo.isNullOrBlank()) {
                val pair = Pair(salvoTitulo, salvoPoster.orEmpty())
                cacheLocalizado[chave] = pair
                return salvoTitulo
            }
        }

        // 4. Dispara busca assíncrona no TMDB para traduzir dinamicamente
        requisitarLocalizacaoTmdb(midia, lang, chave)

        return midia.titulo.t()
    }

    /**
     * Retorna a URL da capa localizada no idioma atualmente ativo.
     */
    fun obterCapaLocalizada(midia: Midia): String {
        val lang = obterPrefixoIdioma()
        if (lang == "pt") return midia.imagemCapa

        if (midia.idTmdb <= 0) return midia.imagemCapa

        val chave = "${midia.idTmdb}-$lang"

        cacheLocalizado[chave]?.let { (_, posterPath) ->
            if (posterPath.isNotBlank()) {
                return if (posterPath.startsWith("http")) posterPath else "https://image.tmdb.org/t/p/w500$posterPath"
            }
        }

        return midia.imagemCapa
    }

    private fun requisitarLocalizacaoTmdb(midia: Midia, lang: String, chave: String) {
        val context = CineListApplication.instance ?: return
        val id = midia.idTmdb

        scope.launch {
            try {
                val tagTmdb = I18n.idiomaAtivo
                val ehSerie = midia.ehSerieOuAnime

                val (novoTitulo, novoPoster) = if (ehSerie) {
                    val detalhe = RetrofitClient.apiService.obterDetalhesSerieOuAnime(idSerie = id, idioma = tagTmdb)
                    Pair(detalhe.titulo, detalhe.caminhoPoster)
                } else {
                    val detalhe = RetrofitClient.apiService.obterDetalhesFilme(idFilme = id, idioma = tagTmdb)
                    Pair(detalhe.titulo, detalhe.caminhoPoster)
                }

                if (novoTitulo.isNotBlank()) {
                    val posterFinal = novoPoster?.takeIf { it.isNotBlank() } ?: ""
                    val par = Pair(novoTitulo, posterFinal)
                    
                    // Salva no SharedPreferences
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("tit_$chave", novoTitulo)
                        .putString("pos_$chave", posterFinal)
                        .apply()

                    // Atualiza estado reativo na main thread
                    launch(Dispatchers.Main) {
                        cacheLocalizado[chave] = par
                    }
                }
            } catch (_: Throwable) {}
        }
    }
}
