package com.example.cinelist

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Classificação canônica de episódios de animação japonesa (Animes).
 */
enum class TipoEpisodioAnime(val rotuloPt: String, val rotuloEn: String) {
    CANON("Cânone", "Canon"),
    FILLER("Filler", "Filler"),
    ANIME_CANON("Cânone do Anime", "Anime Canon"),
    MISTO("Misto", "Mixed Canon/Filler")
}

/**
 * Gerenciador inteligente de Episódios Fillers para Animes.
 *
 * Combina uma base curada offline de alta precisão para os animes mais assistidos
 * com detecção dinâmica e aprendizado via CineAI (Gemini) e cache permanente.
 */
object AnimeFillerManager {

    private const val TAG = "AnimeFillerManager"
    private const val PREFS_NAME = "cinelist_anime_fillers_cache"

    // Cache em memória de fillers por obra (chave: "titulo_temporada" -> conjunto de números de episódios)
    private val cacheMemoria = mutableMapOf<String, Set<Int>>()

    /**
     * Mapeamento estático e imediato de animes com grandes quantidades de episódios fillers.
     * Mapeado tanto por episódio absoluto (para TMDB que agrupa em temporadas longas)
     * quanto por temporada canônica.
     */
    private val fillersCuradosAbsolutos = mapOf(
        // Naruto Clássico (220 episódios)
        "naruto" to setOf(
            26, 97, 101, 102, 103, 104, 105, 106,
            136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150,
            151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165,
            166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180,
            181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195,
            196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210,
            211, 212, 213, 214, 215, 216, 217, 218, 219, 220
        ),

        // Naruto Shippuden (500 episódios)
        "naruto shippuden" to setOf(
            57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71,
            91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112,
            144, 145, 146, 147, 148, 149, 150, 151,
            170, 171,
            176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196,
            223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242,
            257, 258, 259, 260, 271,
            279, 280, 281, 284, 285, 286, 287, 288, 289,
            303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320,
            347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361,
            376, 377, 388, 389, 390,
            394, 395, 396, 397, 398, 399, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413,
            416, 417, 422, 423,
            427, 428, 429, 430, 431, 432, 433, 434, 435, 436, 437, 438, 439, 440, 441, 442, 443, 444, 445, 446, 447, 448, 449, 450,
            464, 465, 466, 467, 468, 469,
            480, 481, 482, 483
        ),

        // Bleach (366 episódios)
        "bleach" to setOf(
            33, 50,
            64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
            128, 129, 130, 131, 132, 133, 134, 135, 136, 137,
            147, 148, 149,
            168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189,
            204, 205, 213, 214,
            228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265,
            287, 299, 303, 304, 305,
            311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341,
            355
        ),

        // One Piece (1100+ episódios)
        "one piece" to setOf(
            54, 55, 56, 57, 58, 59, 60, 61,
            98, 99, 102,
            131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143,
            196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206,
            220, 221, 222, 223, 224, 225, 226,
            279, 280, 281, 282, 283,
            291, 292, 303,
            317, 318, 319,
            326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336,
            382, 383, 384,
            406, 407,
            426, 427, 428, 429,
            457, 458,
            492, 542,
            575, 576, 577, 578,
            590,
            626, 627, 628,
            747, 748, 749, 750,
            775,
            780, 781, 782,
            807, 881,
            895, 896,
            907,
            1029, 1030,
            1088
        ),

        // Boruto: Naruto Next Generations
        "boruto" to setOf(
            16, 17, 40, 41, 48, 49, 50, 67, 68, 69,
            96, 97, 112, 113, 114, 116, 117, 118, 119,
            138, 139, 140, 152, 153, 154, 155, 156,
            256, 257, 258
        ),

        // Dragon Ball Z (291 episódios)
        "dragon ball z" to setOf(
            9, 10, 12, 13, 14, 15, 16,
            39, 40, 41, 42, 43, 44,
            100, 102,
            108, 109, 110, 111, 112, 113, 114, 115, 116, 117,
            124, 125,
            170, 171, 174,
            195, 196, 197, 198, 199,
            202, 203, 204,
            274, 288
        ),

        // Dragon Ball Clássico (153 episódios)
        "dragon ball" to setOf(
            29, 30, 31, 32, 33,
            45,
            79, 80, 81, 82, 83,
            127, 128, 129, 130, 131, 132,
            149, 150, 151, 152, 153
        ),

        // Black Clover (170 episódios)
        "black clover" to setOf(
            29, 66, 68, 82,
            123, 124, 125,
            131, 134, 135,
            142, 143, 144, 145, 146, 147, 148
        ),

        // Fairy Tail
        "fairy tail" to setOf(
            9, 19, 20,
            69, 70, 71, 72, 73, 74, 75,
            125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150,
            202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226,
            268
        ),

        // My Hero Academia / Boku no Hero Academia
        "my hero academia" to setOf(39, 58, 64, 89, 104),
        "boku no hero academia" to setOf(39, 58, 64, 89, 104),

        // Rurouni Kenshin (Samurai X)
        "rurouni kenshin" to setOf(
            13, 14, 15, 16, 17, 18, 25, 26, 27,
            63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95
        ),
        "samurai x" to setOf(
            13, 14, 15, 16, 17, 18, 25, 26, 27,
            63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95
        )
    )

    /**
     * Verifica de forma confiável se uma obra é um Anime.
     */
    fun ehAnime(tipo: String?, generos: String?, titulo: String?): Boolean {
        val t = tipo.orEmpty().trim().lowercase()
        if (t == "anime") return true

        val g = generos.orEmpty().lowercase()
        val tit = titulo.orEmpty().lowercase()
        val chaveNormalizada = normalizarChaveObra(tit)

        // Se for um anime da base curada (ex: Naruto, Bleach, Dragon Ball, One Piece), reconhece imediatamente
        if (fillersCuradosAbsolutos.keys.any { chaveNormalizada.contains(it) }) return true

        val ehAnimacao = g.contains("animação") || g.contains("animacao") || g.contains("animation") || g.contains("anime") || g.contains("アニメ")
        val termoAnime = g.contains("anime") || tit.contains("anime")

        return ehAnimacao && (termoAnime || t.contains("série") || t.contains("serie"))
    }

    private fun normalizarChaveObra(titulo: String): String {
        return titulo.trim().lowercase()
            .replace(Regex("""[^a-z0-9\s]"""), "")
            .replace(Regex("""\s+"""), " ")
    }

    /**
     * Obtém o conjunto de episódios fillers para uma obra e temporada.
     * Suporta temporadas divididas pelo TMDB calculando o offset de episódios acumulados.
     */
    suspend fun obterEpisodiosFillers(
        context: Context,
        titulo: String,
        temporada: Int,
        totalEpisodiosNestaTemporada: Int,
        temporadasSumario: List<TmdbTemporadaItem>? = null
    ): Set<Int> = withContext(Dispatchers.IO) {
        val chaveNormalizada = normalizarChaveObra(titulo)
        val chaveCache = "${chaveNormalizada}_t$temporada"

        // 1. Consulta cache em memória
        cacheMemoria[chaveCache]?.let { return@withContext it }

        // 2. Consulta SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedStr = prefs.getString(chaveCache, null)
        if (cachedStr != null) {
            val conjunto = if (cachedStr == "NENHUM" || cachedStr.isBlank()) {
                emptySet()
            } else {
                cachedStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            }
            cacheMemoria[chaveCache] = conjunto
            return@withContext conjunto
        }

        // 3. Consulta base curada offline com offset de temporadas
        val chaveCurada = fillersCuradosAbsolutos.keys.firstOrNull { chaveNormalizada.contains(it) }
        if (chaveCurada != null) {
            val todosFillers = fillersCuradosAbsolutos[chaveCurada].orEmpty()

            // Calcula o número de episódios das temporadas anteriores para mapear a numeração absoluta
            val offsetEpisodio = if (temporada > 1 && temporadasSumario != null) {
                temporadasSumario
                    .filter { it.numeroTemporada in 1 until temporada }
                    .sumOf { it.quantidadeEpisodios }
            } else {
                0
            }

            val maxEpisodio = if (totalEpisodiosNestaTemporada > 0) totalEpisodiosNestaTemporada else 100
            val fillersNestaTemporada = (1..maxEpisodio).filter { epNum ->
                val epAbsoluto = offsetEpisodio + epNum
                todosFillers.contains(epAbsoluto) || todosFillers.contains(epNum)
            }.toSet()

            salvarNoCache(prefs, chaveCache, fillersNestaTemporada)
            return@withContext fillersNestaTemporada
        }

        // 4. Fallback dinâmico via CineAI (Gemini)
        val fillersDetectados = consultarFillersComIa(context, titulo, temporada)
        salvarNoCache(prefs, chaveCache, fillersDetectados)
        fillersDetectados
    }

    private fun salvarNoCache(prefs: android.content.SharedPreferences, chave: String, fillers: Set<Int>) {
        cacheMemoria[chave] = fillers
        val valorSalvar = if (fillers.isEmpty()) "NENHUM" else fillers.joinToString(",")
        prefs.edit().putString(chave, valorSalvar).apply()
    }

    /**
     * Consulta o CineAI (Gemini) para catalogar fillers de qualquer anime não presente na lista estática.
     */
    private suspend fun consultarFillersComIa(context: Context, titulo: String, temporada: Int): Set<Int> {
        val apiKey = CineAiManager.obterChaveGemini(context).takeIf { it.isNotBlank() } ?: return emptySet()

        return try {
            val prompt = """
                Você é uma enciclopédia especializada em animes e mangás.
                Analise o anime "$titulo" e identifique quais episódios da Temporada $temporada são considerados FILLER (histórias não presentes no mangá canônico original).
                Responda EXCLUSIVAMENTE com os números dos episódios fillers separados por vírgula (exemplo: 12, 13, 14, 25).
                Se todos os episódios forem canônicos ou não houver fillers, responda apenas: NENHUM
            """.trimIndent()

            val resultado = CineAiManager.consultarGeminiTexto(apiKey, prompt)
            if (resultado.contains("NENHUM", ignoreCase = true) || resultado.isBlank()) {
                emptySet()
            } else {
                Regex("""\d+""").findAll(resultado)
                    .mapNotNull { it.value.toIntOrNull() }
                    .filter { it in 1..2000 }
                    .toSet()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao consultar fillers via IA para $titulo T$temporada: ${e.message}")
            emptySet()
        }
    }

    /**
     * Verifica de forma síncrona se um episódio específico já está catalogado como filler.
     */
    fun ehEpisodioFiller(
        context: Context,
        titulo: String,
        temporada: Int,
        numeroEpisodio: Int,
        temporadasSumario: List<TmdbTemporadaItem>? = null
    ): Boolean {
        val chaveNormalizada = normalizarChaveObra(titulo)
        val chaveCache = "${chaveNormalizada}_t$temporada"

        cacheMemoria[chaveCache]?.let { return it.contains(numeroEpisodio) }

        // Verifica na base curada considerando numeração absoluta ou da temporada
        val chaveCurada = fillersCuradosAbsolutos.keys.firstOrNull { chaveNormalizada.contains(it) }
        if (chaveCurada != null) {
            val fillers = fillersCuradosAbsolutos[chaveCurada].orEmpty()
            val offset = if (temporada > 1 && temporadasSumario != null) {
                temporadasSumario.filter { it.numeroTemporada in 1 until temporada }.sumOf { it.quantidadeEpisodios }
            } else 0
            if (fillers.contains(numeroEpisodio) || fillers.contains(offset + numeroEpisodio)) return true
        }

        // Verifica no SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(chaveCache, null)
        if (cached != null && cached != "NENHUM") {
            val set = cached.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            cacheMemoria[chaveCache] = set
            return set.contains(numeroEpisodio)
        }

        return false
    }

    /**
     * Calcula horas e minutos economizados ao pular determinada quantidade de episódios fillers.
     */
    fun calcularEconomiaFillers(
        quantidadeFillers: Int,
        duracaoMediaMinutos: Int = 24
    ): Pair<Int, Int> {
        val minutosTotais = quantidadeFillers * duracaoMediaMinutos
        return Pair(minutosTotais / 60, minutosTotais % 60)
    }

    /**
     * Formata horas e minutos economizados em texto legível.
     */
    fun formatarEconomiaTempo(horas: Int, minutos: Int): String {
        return when {
            horas > 0 && minutos > 0 -> "${horas}h ${minutos}m"
            horas > 0 -> "${horas}h"
            else -> "${minutos}m"
        }
    }

    /**
     * Retorna o total de fillers conhecidos na base curada para um anime.
     */
    fun obterTotalFillersObra(titulo: String): Int {
        val chaveNormalizada = normalizarChaveObra(titulo)
        val chaveCurada = fillersCuradosAbsolutos.keys.firstOrNull { chaveNormalizada.contains(it) }
        return if (chaveCurada != null) {
            fillersCuradosAbsolutos[chaveCurada]?.size ?: 0
        } else {
            0
        }
    }
}
