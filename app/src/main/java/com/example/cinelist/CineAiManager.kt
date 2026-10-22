package com.example.cinelist

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class PerguntaQuiz(
    val pergunta: String,
    val opcoes: List<String>,
    val respostaCorretaIndex: Int,
    val curiosidade: String
)

/**
 * Representa uma Vibe ou Humor cinematográfico para o CineAI.
 */
enum class VibeHumor(
    val id: String,
    val emoji: String,
    val titulo: String,
    val subtitulo: String,
    val generosFilme: List<Int>,
    val generosSerie: List<Int>,
    val promptGemini: String,
    val justificativaPadrao: String,
    val destaques: List<String>
) {
    RIR_MUITO(
        id = "rir",
        emoji = "🎭",
        titulo = "Rir até doer",
        subtitulo = "Comédias leves e divertidas",
        generosFilme = listOf(35),
        generosSerie = listOf(35),
        promptGemini = "filmes ou séries de comédia muito engraçados, com ritmo dinâmico para dar boas risadas e desestressar",
        justificativaPadrao = "Perfeito para quem busca dar risada: humor afiado, situações hilárias e ritmo leve do início ao fim.",
        destaques = listOf("Humor Garantido", "Ritmo Leve", "Desestressante")
    ),
    PLOT_TWIST(
        id = "plot_twist",
        emoji = "🧠",
        titulo = "Explodir a mente",
        subtitulo = "Plot twists e enigmas psicológicos",
        generosFilme = listOf(9648, 53, 878),
        generosSerie = listOf(9648, 10765),
        promptGemini = "filmes ou séries com reviravoltas chocantes, plot twists geniais ou quebra-cabeças psicológicos imprevisíveis",
        justificativaPadrao = "Roteiro instigante com reviravoltas bem amarradas que vão te deixar pensando na história por dias.",
        destaques = listOf("Plot Twist Genial", "Suspense Tenso", "Final Chocante")
    ),
    CHORAR_EMOCAO(
        id = "chorar",
        emoji = "😭",
        titulo = "Chorar de emoção",
        subtitulo = "Dramas tocantes e superação",
        generosFilme = listOf(18, 10749),
        generosSerie = listOf(18),
        promptGemini = "filmes ou séries emocionantes e comoventes, com fortes lições de vida, amor, sacrifício ou superação pessoal",
        justificativaPadrao = "Carga emocional profunda com personagens inesquecíveis e cenas que tocam direto no coração.",
        destaques = listOf("Muito Emocionante", "Atuações Marcantes", "Lição de Vida")
    ),
    ADRENALINA_PURA(
        id = "adrenalina",
        emoji = "🍿",
        titulo = "Pipoca & Ação",
        subtitulo = "Ritmo acelerado e aventura",
        generosFilme = listOf(28, 12, 80),
        generosSerie = listOf(10759),
        promptGemini = "filmes ou séries de ação eletrizantes, com sequências empolgantes, ritmo sem pausas e diversão garantida",
        justificativaPadrao = "Entretenimento de primeira com ritmo frenético, grandes sequências de ação e pura adrenalina.",
        destaques = listOf("Pura Adrenalina", "Ritmo Sem Pausas", "Efeitos Espetaculares")
    ),
    OUTROS_MUNDOS(
        id = "outros_mundos",
        emoji = "🌌",
        titulo = "Ficção & Fantasia",
        subtitulo = "Viagem no tempo, espaço e magia",
        generosFilme = listOf(878, 14),
        generosSerie = listOf(10765),
        promptGemini = "filmes ou séries de ficção científica instigante, exploração espacial, viagem no tempo ou fantasia imersiva",
        justificativaPadrao = "Construção de mundo visualmente fascinante com conceitos criativos que expandem a imaginação.",
        destaques = listOf("Mundo Imersivo", "Visual Incrível", "Conceito Genial")
    ),
    FILME_CURTO(
        id = "curto",
        emoji = "⏱️",
        titulo = "Direto ao Ponto (< 95 min)",
        subtitulo = "Histórias ágeis para quem tem pouco tempo",
        generosFilme = listOf(35, 16, 53),
        generosSerie = listOf(35, 16),
        promptGemini = "filmes de excelente qualidade com menos de 95 minutos de duração, ágeis, sem enrolação",
        justificativaPadrao = "Narrativa concisa e direta ao ponto, perfeita para curtir naqueles dias em que você quer dormir cedo.",
        destaques = listOf("Duração Perfeita", "Sem Enrolação", "História Ágil")
    ),
    SESSAO_A_DOIS(
        id = "sessao_casal",
        emoji = "💑",
        titulo = "Sessão a Dois",
        subtitulo = "Romance envolvente e comédia romântica",
        generosFilme = listOf(10749, 35),
        generosSerie = listOf(18, 35),
        promptGemini = "filmes ou séries românticos cativantes, perfeitos para assistir a dois, com boa química entre os protagonistas",
        justificativaPadrao = "Química apaixonante entre os protagonistas com uma atmosfera aconchegante para assistir juntinho.",
        destaques = listOf("Sessão Aconchegante", "Boa Química", "História Cativante")
    ),
    FRIO_NA_ESPINHA(
        id = "terror",
        emoji = "👻",
        titulo = "Frio na Espinha",
        subtitulo = "Terror psicológico e suspense sombrio",
        generosFilme = listOf(27, 9648),
        generosSerie = listOf(9648, 10765),
        promptGemini = "filmes ou séries de terror de alta qualidade, tensão psicológica constante e atmosfera assustadora",
        justificativaPadrao = "Atmosfera opressiva com suspense bem construído e sustos inteligentes para testar a coragem.",
        destaques = listOf("Atmosfera Sombria", "Tensão Constante", "Sustos Inteligentes")
    )
}

data class CineAiSugestao(
    val tmdbItem: TmdbFilme,
    val justificativaIa: String,
    val destaques: List<String>,
    val vibeNome: String,
    val isGeradoPorGemini: Boolean = false
)

object CineAiManager {

    private const val TAG = "CineAiManager"
    private const val PREFS_NAME = "CineAiPrefs"
    private const val KEY_GEMINI = "gemini_api_key"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private val gson by lazy { Gson() }

    fun obterChaveGemini(context: Context): String {
        val chaveSalva = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GEMINI, "")?.trim().orEmpty()
        if (chaveSalva.isNotBlank()) return chaveSalva

        return runCatching {
            BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
        }.getOrNull()?.trim().orEmpty()
    }

    fun salvarChaveGemini(context: Context, chave: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GEMINI, chave.trim())
            .apply()
    }

    /**
     * Extrai o nome de um filme de referência quando o usuário pesquisa por termos como
     * "parecido com X", "tipo X", "semelhante a X", "estilo X", "como X", etc.
     */
    fun extrairNomeFilmeSimilar(texto: String): String? {
        val regexPrefixo = Regex(
            """(?i)(?:parecido\s+com|semelhante\s+a|tipo|como|estilo|na\s+pegada\s+de|na\s+vibe\s+de|igual\s+a|similar\s+a)\s+["']?([A-Za-z0-9À-ÿ\s:—\-]+)["']?"""
        )
        val match = regexPrefixo.find(texto.trim())
        if (match != null) {
            val extraido = match.groupValues[1].trim()
            if (extraido.length >= 2) return extraido
        }

        // Detecta títulos entre aspas
        val regexAspas = Regex("""["']([A-Za-z0-9À-ÿ\s:—\-]{2,})["']""")
        val matchAspas = regexAspas.find(texto.trim())
        if (matchAspas != null) {
            return matchAspas.groupValues[1].trim()
        }

        return null
    }

    /**
     * Recomenda títulos com inteligência híbrida (Gemini AI + motor semântico TMDB).
     */
    suspend fun recomendar(
        context: Context,
        vibe: VibeHumor?,
        textoLivre: String,
        filtroTipo: String // "TODOS", "FILME", "SERIE"
    ): List<CineAiSugestao> = withContext(Dispatchers.IO) {
        val chaveGemini = obterChaveGemini(context)

        // 1. Se houver chave Gemini, tenta primeiro com a IA generativa do Google
        if (chaveGemini.isNotBlank()) {
            val resultadoGemini = runCatching {
                recomendarComGemini(chaveGemini, vibe, textoLivre, filtroTipo)
            }.getOrNull()

            if (!resultadoGemini.isNullOrEmpty()) {
                return@withContext resultadoGemini
            }
        }

        // 2. Motor Heurístico Semântico Inteligente TMDB (Resolve "Parecido com Interestelar" e buscas descritivas)
        return@withContext recomendarComMotorHeuristico(vibe, textoLivre, filtroTipo)
    }

    private suspend fun recomendarComGemini(
        apiKey: String,
        vibe: VibeHumor?,
        textoLivre: String,
        filtroTipo: String
    ): List<CineAiSugestao> = coroutineScope {
        val tipoDescricao = when (filtroTipo) {
            "FILME" -> "somente filmes de longa-metragem"
            "SERIE" -> "somente séries de TV ou animes"
            else -> "filmes ou séries"
        }

        val promptVibe = vibe?.promptGemini ?: "excelente produção consagrada e aclamada"
        val promptUsuario = textoLivre.trim()

        val promptFinal = """
            Você é o CineAI, o curador cinematográfico de inteligência artificial do CineList.
            O usuário deseja assistir $tipoDescricao com base no seguinte pedido:
            - Pedido do usuário: "$promptUsuario"
            - Vibe / Humor: "$promptVibe"
            
            REGRAS CRÍTICAS:
            1. Se o usuário pediu algo "parecido com X" (ex: Interestelar), recomende filmes REAIS, famosos e aclamados que pertençam EXATAMENTE ao mesmo subgênero e clima (ex: para Interestelar recomende A Chegada, Contato, Perdido em Marte, Gravidade, 2001). NUNCA traga filmes aleatórios que não tenham relação direta.
            2. Se o usuário descreveu um tema (ex: viagem no tempo, robôs, romance na chuva), traga as melhores obras reais desse nicho.
            3. Retorne exatamente de 3 a 5 recomendações reais existentes no TMDB.
            
            Responda EXCLUSIVAMENTE em JSON válido com esta estrutura (sem markdown, sem blocos ```json):
            {
              "recomendacoes": [
                {
                  "titulo": "Título em Português ou Original",
                  "ano": 2014,
                  "justificativa": "Frase clara e empolgante de no máximo 2 linhas explicando por que é perfeita para quem pediu isso.",
                  "destaques": ["Destaque 1", "Destaque 2", "Destaque 3"]
                }
              ]
            }
        """.trimIndent()

        // Modelos em ordem de preferência
        val modelos = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")

        var textoLimpo = ""
        for (modelo in modelos) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent?key=$apiKey"
                val jsonBody = JsonObject().apply {
                    val contentsArray = com.google.gson.JsonArray()
                    val contentObj = JsonObject()
                    val partsArray = com.google.gson.JsonArray()
                    val partObj = JsonObject()
                    partObj.addProperty("text", promptFinal)
                    partsArray.add(partObj)
                    contentObj.add("parts", partsArray)
                    contentsArray.add(contentObj)
                    add("contents", contentsArray)
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val jsonResp = gson.fromJson(body, JsonObject::class.java)
                    val candidate = jsonResp.getAsJsonArray("candidates")?.get(0)?.asJsonObject
                    val text = candidate?.getAsJsonObject("content")
                        ?.getAsJsonArray("parts")?.get(0)?.asJsonObject
                        ?.get("text")?.asString.orEmpty()

                    if (text.isNotBlank()) {
                        textoLimpo = text.replace("```json", "").replace("```", "").trim()
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Tentativa com modelo $modelo falhou: ${e.message}")
            }
        }

        if (textoLimpo.isBlank()) return@coroutineScope emptyList()

        val parsedData = runCatching {
            gson.fromJson(textoLimpo, JsonObject::class.java)
        }.getOrNull() ?: return@coroutineScope emptyList()

        val arrayRecomendacoes = parsedData.getAsJsonArray("recomendacoes") ?: return@coroutineScope emptyList()

        val listaResultados = arrayRecomendacoes.mapNotNull { elemento ->
            val obj = elemento.asJsonObject
            val titulo = obj.get("titulo")?.asString ?: return@mapNotNull null
            val justificativa = obj.get("justificativa")?.asString.orEmpty()
            val destaquesList = obj.getAsJsonArray("destaques")?.map { it.asString }.orEmpty()

            async {
                val api = RetrofitClient.apiService
                val busca = runCatching { api.buscarMulti(titulo).resultados }.getOrNull().orEmpty()
                val correspondencia = busca.firstOrNull { !it.caminhoPoster.isNullOrBlank() } ?: busca.firstOrNull()

                if (correspondencia != null) {
                    CineAiSugestao(
                        tmdbItem = correspondencia,
                        justificativaIa = justificativa.ifBlank { "Recomendação personalizada selecionada pelo CineAI." },
                        destaques = if (destaquesList.isNotEmpty()) destaquesList else listOf("Recomendado por IA", "Cinéfilo", "Alta Qualidade"),
                        vibeNome = vibe?.titulo ?: "Descoberta IA",
                        isGeradoPorGemini = true
                    )
                } else null
            }
        }.mapNotNull { it.await() }

        listaResultados
    }

    /**
     * Motor Heurístico Semântico inteligente conectado aos endpoints oficiais de recomendações do TMDB.
     */
    private suspend fun recomendarComMotorHeuristico(
        vibe: VibeHumor?,
        textoLivre: String,
        filtroTipo: String
    ): List<CineAiSugestao> = coroutineScope {
        val api = RetrofitClient.apiService
        val textoLimpo = textoLivre.trim()

        // 1. VERIFICA SE É UMA BUSCA POR SIMILARIDADE (ex: "parecido com interestelar", "tipo matrix")
        val nomeFilmeSimilar = extrairNomeFilmeSimilar(textoLimpo)
        if (!nomeFilmeSimilar.isNullOrBlank()) {
            val buscaRef = runCatching { api.buscarMulti(nomeFilmeSimilar).resultados }.getOrNull().orEmpty()
            val referencia = buscaRef.firstOrNull { !it.caminhoPoster.isNullOrBlank() } ?: buscaRef.firstOrNull()

            if (referencia != null && referencia.idTmdb != 0) {
                val ehSerie = referencia.ehSerie || referencia.mediaType.equals("tv", ignoreCase = true)
                val recomendacoesTmdb = if (ehSerie) {
                    runCatching { api.obterRecomendacoesSerieOuAnime(referencia.idTmdb).recomendacoes }.getOrNull().orEmpty()
                } else {
                    runCatching { api.obterRecomendacoesFilme(referencia.idTmdb).recomendacoes }.getOrNull().orEmpty()
                }

                val recomendacoesValidas = recomendacoesTmdb.filter { !it.caminhoPoster.isNullOrBlank() && it.idTmdb != referencia.idTmdb }

                if (recomendacoesValidas.isNotEmpty()) {
                    return@coroutineScope recomendacoesValidas.take(6).map { item ->
                        val notaFormatada = if (item.mediaVotos > 0) "⭐ ${String.format(Locale.US, "%.1f", item.mediaVotos)}" else "Em Alta"
                        val ano = if (item.anoLancamento.isNotBlank()) item.anoLancamento else "Clássico"

                        CineAiSugestao(
                            tmdbItem = item,
                            justificativaIa = "Recomendado por ser diretamente comparado e similar a '${referencia.titulo}', compartilhando a mesma temática, tom narrativo e estilo cinematográfico.",
                            destaques = listOf("Similar a ${referencia.titulo}", notaFormatada, ano),
                            vibeNome = "Similar a ${referencia.titulo}",
                            isGeradoPorGemini = false
                        )
                    }
                }
            }
        }

        // 2. BUSCA POR PALAVRAS-CHAVE TEMÁTICAS OU VIBE
        val vibeAlvo = when {
            vibe != null -> vibe
            textoLimpo.contains("rir", ignoreCase = true) || textoLimpo.contains("comédia", ignoreCase = true) || textoLimpo.contains("engraçad", ignoreCase = true) -> VibeHumor.RIR_MUITO
            textoLimpo.contains("espaço", ignoreCase = true) || textoLimpo.contains("universo", ignoreCase = true) || textoLimpo.contains("buraco negro", ignoreCase = true) || textoLimpo.contains("viagem no tempo", ignoreCase = true) -> VibeHumor.OUTROS_MUNDOS
            textoLimpo.contains("terror", ignoreCase = true) || textoLimpo.contains("medo", ignoreCase = true) || textoLimpo.contains("fantasma", ignoreCase = true) || textoLimpo.contains("susto", ignoreCase = true) -> VibeHumor.FRIO_NA_ESPINHA
            textoLimpo.contains("reviravolta", ignoreCase = true) || textoLimpo.contains("plot twist", ignoreCase = true) || textoLimpo.contains("investiga", ignoreCase = true) || textoLimpo.contains("mistério", ignoreCase = true) -> VibeHumor.PLOT_TWIST
            textoLimpo.contains("amor", ignoreCase = true) || textoLimpo.contains("romance", ignoreCase = true) || textoLimpo.contains("casal", ignoreCase = true) -> VibeHumor.SESSAO_A_DOIS
            textoLimpo.contains("ação", ignoreCase = true) || textoLimpo.contains("luta", ignoreCase = true) || textoLimpo.contains("tiro", ignoreCase = true) || textoLimpo.contains("adrenalina", ignoreCase = true) -> VibeHumor.ADRENALINA_PURA
            textoLimpo.contains("chora", ignoreCase = true) || textoLimpo.contains("drama", ignoreCase = true) || textoLimpo.contains("emocion", ignoreCase = true) -> VibeHumor.CHORAR_EMOCAO
            else -> VibeHumor.PLOT_TWIST
        }

        // Se o usuário digitou palavras diretas sem frases longas (ex: "interestelar", "avatar", "matrix")
        if (textoLimpo.isNotBlank() && textoLimpo.split(" ").size <= 3) {
            val buscaDireta = runCatching { api.buscarMulti(textoLimpo).resultados }.getOrNull().orEmpty()
                .filter { !it.caminhoPoster.isNullOrBlank() }
                .take(6)

            if (buscaDireta.isNotEmpty()) {
                return@coroutineScope buscaDireta.map { item ->
                    val destaque1 = if (item.mediaVotos >= 7.0) "⭐ Nota ${String.format(Locale.US, "%.1f", item.mediaVotos)}" else "Em Alta"
                    val destaque2 = if (item.anoLancamento.isNotBlank()) "Ano ${item.anoLancamento}" else "Destaque"
                    val destaque3 = if (item.ehSerie) "Série de TV" else "Longa-metragem"

                    CineAiSugestao(
                        tmdbItem = item,
                        justificativaIa = "Correspondência exata encontrada com base no título pesquisado.",
                        destaques = listOf(destaque1, destaque2, destaque3),
                        vibeNome = "Busca Direta",
                        isGeradoPorGemini = false
                    )
                }
            }
        }

        val generosFilmeStr = vibeAlvo.generosFilme.joinToString(",")
        val generosSerieStr = vibeAlvo.generosSerie.joinToString(",")

        val buscarFilmes = filtroTipo == "TODOS" || filtroTipo == "FILME"
        val buscarSeries = filtroTipo == "TODOS" || filtroTipo == "SERIE"

        val filmesDeferred = if (buscarFilmes) {
            async {
                val paginas = listOf(1, 2, 3).shuffled().take(2)
                paginas.flatMap { p ->
                    runCatching {
                        api.descobrirFilmes(
                            pagina = p,
                            generos = generosFilmeStr,
                            sortBy = if (vibeAlvo == VibeHumor.PLOT_TWIST || vibeAlvo == VibeHumor.OUTROS_MUNDOS) "vote_average.desc" else "popularity.desc"
                        ).resultados
                    }.getOrNull().orEmpty()
                }
            }
        } else null

        val seriesDeferred = if (buscarSeries) {
            async {
                val paginas = listOf(1, 2).shuffled().take(1)
                paginas.flatMap { p ->
                    runCatching {
                        api.descobrirSeries(
                            pagina = p,
                            generos = generosSerieStr,
                            sortBy = "popularity.desc"
                        ).resultados
                    }.getOrNull().orEmpty()
                }
            }
        } else null

        val filmes = filmesDeferred?.await().orEmpty()
        val series = seriesDeferred?.await().orEmpty()

        val pool = (filmes + series)
            .filter { !it.caminhoPoster.isNullOrBlank() && it.sinopse.isNotBlank() }
            .distinctBy { "${it.ehSerie}_${it.idTmdb}" }
            .shuffled()

        val selecionados = pool.take(6)

        selecionados.map { item ->
            val justificativa = if (vibeAlvo == VibeHumor.PLOT_TWIST && item.mediaVotos >= 7.5) {
                "Uma das obras mais aclamadas no gênero com reviravoltas primorosas e nota ${String.format(Locale.US, "%.1f", item.mediaVotos)}."
            } else if (vibeAlvo == VibeHumor.OUTROS_MUNDOS) {
                "Ficção científica grandiosa com exploração de conceitos fascinantes e visual imersivo."
            } else {
                vibeAlvo.justificativaPadrao
            }

            val destaqueNota = if (item.mediaVotos > 0.0) "⭐ ${String.format(Locale.US, "%.1f", item.mediaVotos)}" else "Top Avaliado"
            val destaqueAno = if (item.anoLancamento.isNotBlank()) item.anoLancamento else "Clássico"
            val listaDestaques = listOf(destaqueNota, destaqueAno) + vibeAlvo.destaques.take(1)

            CineAiSugestao(
                tmdbItem = item,
                justificativaIa = justificativa,
                destaques = listaDestaques,
                vibeNome = "${vibeAlvo.emoji} ${vibeAlvo.titulo}",
                isGeradoPorGemini = false
            )
        }
    }

    /**
     * Gera 4 perguntas de trivia cinéfila renovadas a cada rodada.
     */
    suspend fun gerarQuizCineAi(context: Context, tema: String = "Cinema Geral"): List<PerguntaQuiz> = withContext(Dispatchers.IO) {
        val apiKey = obterChaveGemini(context)

        if (apiKey.isNotBlank()) {
            val quizGerado = runCatching {
                gerarQuizComGemini(apiKey, tema)
            }.getOrNull()

            if (!quizGerado.isNullOrEmpty()) {
                return@withContext quizGerado
            }
        }

        // Fallback dinâmico com renovação contínua a cada partida
        return@withContext obterQuizRenovado(tema)
    }

    private suspend fun gerarQuizComGemini(apiKey: String, tema: String): List<PerguntaQuiz>? {
        val prompt = """
            Você é o apresentador do CineQuiz no aplicativo CineList.
            Gere exatamente 4 perguntas desafiadoras, divertidas e INÉDITAS sobre o tema de cinema: "$tema".
            As perguntas devem ser dinâmicas e cobrir fatos marcantes da sétima arte.
            
            Responda EXCLUSIVAMENTE em formato JSON estrito:
            {
              "perguntas": [
                {
                  "pergunta": "Texto claro da pergunta?",
                  "opcoes": ["Opção 1", "Opção 2", "Opção 3", "Opção 4"],
                  "respostaCorretaIndex": 0,
                  "curiosidade": "Curiosidade rápida e fascinante sobre a resposta."
                }
              ]
            }
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val jsonBody = JsonObject().apply {
            val contentsArray = com.google.gson.JsonArray()
            val contentObj = JsonObject()
            val partsArray = com.google.gson.JsonArray()
            val partObj = JsonObject()
            partObj.addProperty("text", prompt)
            partsArray.add(partObj)
            contentObj.add("parts", partsArray)
            contentsArray.add(contentObj)
            add("contents", contentsArray)
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string().orEmpty()
        val jsonResposta = gson.fromJson(responseBody, JsonObject::class.java)
        val textoGerado = jsonResposta.getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString.orEmpty()

        val textoLimpo = textoGerado.replace("```json", "").replace("```", "").trim()
        val parsed = gson.fromJson(textoLimpo, JsonObject::class.java)
        val array = parsed.getAsJsonArray("perguntas") ?: return null

        val lista = array.mapNotNull { el ->
            val obj = el.asJsonObject
            val perg = obj.get("pergunta")?.asString ?: return@mapNotNull null
            val ops = obj.getAsJsonArray("opcoes")?.map { it.asString } ?: return@mapNotNull null
            val corr = obj.get("respostaCorretaIndex")?.asInt ?: 0
            val cur = obj.get("curiosidade")?.asString ?: ""

            embaralharOpcoes(perg, ops, corr, cur)
        }

        return if (lista.size >= 4) lista.take(4) else null
    }

    private fun embaralharOpcoes(pergunta: String, opcoes: List<String>, indiceCorreto: Int, curiosidade: String): PerguntaQuiz {
        val respostaCorretaTexto = opcoes.getOrNull(indiceCorreto) ?: opcoes.first()
        val opcoesEmbaralhadas = opcoes.shuffled()
        val novoIndice = opcoesEmbaralhadas.indexOf(respostaCorretaTexto)
        return PerguntaQuiz(
            pergunta = pergunta,
            opcoes = opcoesEmbaralhadas,
            respostaCorretaIndex = novoIndice,
            curiosidade = curiosidade
        )
    }

    /**
     * Grande banco de trivia com renovação constante: sempre sorteia perguntas diferentes
     * e reembaralha as alternativas a cada execução.
     */
    private fun obterQuizRenovado(tema: String): List<PerguntaQuiz> {
        val bancoCompleto = listOf(
            // --- FICÇÃO CIENTÍFICA & ESPAÇO ---
            PerguntaQuiz(
                pergunta = "Em 'Interestelar' (2014) de Christopher Nolan, qual é o nome do buraco negro supermassivo?",
                opcoes = listOf("Gargantua", "Sagitário A*", "Cygnus X-1", "Behemoth"),
                respostaCorretaIndex = 0,
                curiosidade = "O físico Kip Thorne (ganhador do Nobel) ajudou a equipe de efeitos a calcular as equações reais do Gargantua."
            ),
            PerguntaQuiz(
                pergunta = "Qual clássico de ficção científica apresenta a inteligência artificial HAL 9000?",
                opcoes = listOf("2001: Uma Odisseia no Espaço", "Blade Runner", "Alien, o Oitavo Passageiro", "Matrix"),
                respostaCorretaIndex = 0,
                curiosidade = "O nome HAL é composto pelas letras anteriores a IBM no alfabeto (H-I, A-B, L-M)."
            ),
            PerguntaQuiz(
                pergunta = "Em 'Matrix' (1999), qual pílula Neo escolhe tomar para descobrir a verdade sobre a realidade?",
                opcoes = listOf("Pílula Vermelha", "Pílula Azul", "Pílula Verde", "Pílula Roxa"),
                respostaCorretaIndex = 0,
                curiosidade = "A pílula vermelha virou um dos maiores símbolos culturais da ficção científica mundial."
            ),
            PerguntaQuiz(
                pergunta = "Em 'De Volta Para o Futuro', qual velocidade o DeLorean precisava atingir para viajar no tempo?",
                opcoes = listOf("88 milhas por hora", "99 milhas por hora", "77 milhas por hora", "100 milhas por hora"),
                respostaCorretaIndex = 0,
                curiosidade = "88 mph equivale a cerca de 141,6 km/h no velocímetro analógico do carro."
            ),
            PerguntaQuiz(
                pergunta = "Qual é o nome do planeta coberto por oceanos gigantescos com ondas titânicas em 'Interestelar'?",
                opcoes = listOf("Planeta Miller", "Planeta Mann", "Planeta Edmunds", "Planeta Solaris"),
                respostaCorretaIndex = 0,
                curiosidade = "No Planeta Miller, 1 hora na superfície equivalia a 7 anos terrestres devido à dilatação gravitacional extrema."
            ),

            // --- CLÁSSICOS & OSCAR ---
            PerguntaQuiz(
                pergunta = "Qual foi o primeiro filme em língua não-inglesa a vencer o Oscar de Melhor Filme na história?",
                opcoes = listOf("Parasita (2019)", "O Labirinto do Fauno (2006)", "A Vida é Bela (1997)", "Roma (2018)"),
                respostaCorretaIndex = 0,
                curiosidade = "O filme sul-coreano de Bong Joon-ho fez história na 92ª cerimônia do Oscar em 2020."
            ),
            PerguntaQuiz(
                pergunta = "Quantos Oscars 'O Senhor dos Anéis: O Retorno do Rei' (2003) venceu?",
                opcoes = listOf("11 Oscars", "10 Oscars", "9 Oscars", "12 Oscars"),
                respostaCorretaIndex = 0,
                curiosidade = "O filme venceu TODAS as 11 categorias nas quais concorreu, empatando o recorde com Titanic e Ben-Hur."
            ),
            PerguntaQuiz(
                pergunta = "Quem interpretou Michael Corleone na lendária trilogia 'O Poderoso Chefão'?",
                opcoes = listOf("Al Pacino", "Robert De Niro", "Marlon Brando", "Joe Pesci"),
                respostaCorretaIndex = 0,
                curiosidade = "Os executivos da Paramount quase demitiram Al Pacino durante as primeiras semanas de filmagem."
            ),
            PerguntaQuiz(
                pergunta = "Qual filme detém a maior bilheteria da história do cinema sem ajuste pela inflação?",
                opcoes = listOf("Avatar (2009)", "Vingadores: Ultimato (2019)", "Titanic (1997)", "Star Wars: O Despertar da Força (2015)"),
                respostaCorretaIndex = 0,
                curiosidade = "Avatar arrecadou cerca de 2.92 bilhões de dólares globalmente com seus relançamentos."
            ),
            PerguntaQuiz(
                pergunta = "Qual clássico suspense de Alfred Hitchcock se passa quase inteiramente na janela de um apartamento?",
                opcoes = listOf("Janela Indiscreta (1954)", "Psicose (1960)", "Um Corpo que Cai (1958)", "Festim Diabólico (1948)"),
                respostaCorretaIndex = 0,
                curiosidade = "Todo o cenário do pátio foi construído dentro de um único estúdio gigante da Paramount."
            ),

            // --- SUSPENSE & PLOT TWISTS ---
            PerguntaQuiz(
                pergunta = "Em 'Clube da Luta' (1999), quem é na verdade o personagem Tyler Durden?",
                opcoes = listOf("Uma personalidade alternativa do Narrador", "O irmão gêmeo do Narrador", "Um espião do governo", "Um líder mafioso"),
                respostaCorretaIndex = 0,
                curiosidade = "O diretor David Fincher inseriu pistas subliminares de Tyler em vários frames antes de ele aparecer oficialmente."
            ),
            PerguntaQuiz(
                pergunta = "Em 'O Sexto Sentido' (1999), qual é a famosa revelação final sobre o Dr. Malcolm Crowe (Bruce Willis)?",
                opcoes = listOf("Ele estava morto o tempo todo", "Ele era o assassino", "Ele era uma alucinação da mãe", "Ele era o pai do garoto"),
                respostaCorretaIndex = 0,
                curiosidade = "Em todas as cenas do filme onde algo sobrenatural ocorre, a cor vermelha aparece em destaque."
            ),
            PerguntaQuiz(
                pergunta = "Qual filme de Christopher Nolan conta a história de um homem com perda de memória recente de trás para frente?",
                opcoes = listOf("Amnésia (Memento)", "A Origem", "Tenet", "O Grande Truque"),
                respostaCorretaIndex = 0,
                curiosidade = "As cenas em cores correm em ordem cronológica reversa, enquanto as em preto-e-branco correm para a frente."
            ),
            PerguntaQuiz(
                pergunta = "Em 'Se7en - Os Sete Crimes Capitais', qual detetive é interpretado por Morgan Freeman?",
                opcoes = listOf("William Somerset", "David Mills", "Martin Riggs", "Vincent Hanna"),
                respostaCorretaIndex = 0,
                curiosidade = "Freeman insistiu que seu personagem fosse calmo e metódico para contrastar com a explosividade de Brad Pitt."
            ),

            // --- ANIMAÇÃO, HERÓIS & POP ---
            PerguntaQuiz(
                pergunta = "Qual estúdio de animação japonês consagrado criou 'A Viagem de Chihiro' e 'Meu Amigo Totoro'?",
                opcoes = listOf("Studio Ghibli", "Toei Animation", "Madhouse", "Kyoto Animation"),
                respostaCorretaIndex = 0,
                curiosidade = "Hayao Miyazaki desenhou grande parte dos storyboards de Chihiro à mão, sem roteiro inicial prévio."
            ),
            PerguntaQuiz(
                pergunta = "No filme 'Vingadores: Guerra Infinita', quantas Joias do Infinito Thanos precisa reunir?",
                opcoes = listOf("6 Joias", "5 Joias", "7 Joias", "4 Joias"),
                respostaCorretaIndex = 0,
                curiosidade = "As seis joias são: Espaço, Mente, Realidade, Poder, Tempo e Alma."
            ),
            PerguntaQuiz(
                pergunta = "Qual foi o primeiro longa-metragem totalmente animado por computador da história?",
                opcoes = listOf("Toy Story (1995)", "Formiguinhaz (1998)", "Shrek (2001)", "Vida de Inseto (1998)"),
                respostaCorretaIndex = 0,
                curiosidade = "A Pixar quase cancelou Toy Story após uma versão inicial sombria rejeitada pela Disney."
            ),
            PerguntaQuiz(
                pergunta = "Em 'O Cavaleiro das Trevas' (2008), quem interpretou a icônica e premiada versão do Coringa?",
                opcoes = listOf("Heath Ledger", "Joaquin Phoenix", "Jack Nicholson", "Jared Leto"),
                respostaCorretaIndex = 0,
                curiosidade = "Heath Ledger recebeu um Oscar póstumo de Melhor Ator Coadjuvante por sua atuação arrebatadora."
            )
        )

        val perguntasEscolhidas = bancoCompleto.shuffled().take(4)

        return perguntasEscolhidas.map { p ->
            embaralharOpcoes(p.pergunta, p.opcoes, p.respostaCorretaIndex, p.curiosidade)
        }
    }

    fun gerarDestaquesSemSpoiler(
        titulo: String,
        sinopse: String,
        nota: Double
    ): List<String> {
        val pontos = mutableListOf<String>()

        if (nota >= 7.5) {
            pontos.add("⭐ Elogiado pela crítica e público com ótima recepção geral (${String.format(java.util.Locale.US, "%.1f", nota)})")
        } else if (nota >= 6.0) {
            pontos.add("🍿 Avaliação sólida (${String.format(java.util.Locale.US, "%.1f", nota)}), perfeito para uma sessão descontraída")
        }

        if (sinopse.contains("mistério", ignoreCase = true) || sinopse.contains("investiga", ignoreCase = true) || sinopse.contains("crime", ignoreCase = true)) {
            pontos.add("🔍 Trama investigativa com ritmo envolvente e quebra-cabeças instigantes")
        } else if (sinopse.contains("amor", ignoreCase = true) || sinopse.contains("paixão", ignoreCase = true) || sinopse.contains("casal", ignoreCase = true)) {
            pontos.add("❤️ Forte apelo emocional e conexão cativante entre os personagens")
        } else if (sinopse.contains("espaço", ignoreCase = true) || sinopse.contains("planeta", ignoreCase = true) || sinopse.contains("futuro", ignoreCase = true)) {
            pontos.add("🚀 Exploração de conceitos fascinantes e visual imersivo")
        } else {
            pontos.add("🎭 Excelente desenvolvimento de personagens e conflitos dinâmicos")
        }

        pontos.add("✅ Veredito CineAI: Experiência recomendada que vale o seu tempo")

        return pontos
    }

    /**
     * Gera perguntas especiais do minigame "Quem Disse Essa Frase? / Citações Clássicas".
     */
    fun gerarQuizCitacoes(): List<PerguntaQuiz> {
        val bancoCitacoes = listOf(
            PerguntaQuiz(
                pergunta = "“Que a Força esteja com você.” — De qual lendária saga pertence esta citação?",
                opcoes = listOf("Star Wars", "Star Trek", "Duna", "Guardiões da Galáxia"),
                respostaCorretaIndex = 0,
                curiosidade = "A frase foi dita pela primeira vez pelo General Dodonna em 'Uma Nova Esperança' (1977)."
            ),
            PerguntaQuiz(
                pergunta = "“Hasta la vista, baby.” — Qual clássico imortalizou esta frase de ação?",
                opcoes = listOf("O Exterminador do Futuro 2", "Predador", "RoboCop", "Rambo II"),
                respostaCorretaIndex = 0,
                curiosidade = "Arnold Schwarzenegger aprendeu a expressão com o jovem John Connor no roteiro de James Cameron."
            ),
            PerguntaQuiz(
                pergunta = "“Eu vejo pessoas mortas.” — Em qual marcante suspense esta frase é dita?",
                opcoes = listOf("O Sexto Sentido (1999)", "Os Outros (2001)", "Invocação do Mal (2013)", "Poltergeist (1982)"),
                respostaCorretaIndex = 0,
                curiosidade = "O jovem Haley Joel Osment gravou essa icônica cena tremendo de frio sob cobertores de verdade no set."
            ),
            PerguntaQuiz(
                pergunta = "“Vou fazer uma oferta que ele não poderá recusar.” — Qual marco do cinema imortalizou esse diálogo?",
                opcoes = listOf("O Poderoso Chefão (1972)", "Os Bons Companheiros (1990)", "Scarface (1983)", "Cassino (1995)"),
                respostaCorretaIndex = 0,
                curiosidade = "A frase foi eleita a segunda citação cinematográfica mais marcante pelo American Film Institute (AFI)."
            ),
            PerguntaQuiz(
                pergunta = "“Ao infinito e além!” — Qual herói dos cinemas repete com orgulho este lema?",
                opcoes = listOf("Buzz Lightyear (Toy Story)", "Woody (Toy Story)", "Homem de Ferro", "Wall-E"),
                respostaCorretaIndex = 0,
                curiosidade = "A frase foi sugerida por animadores da Pixar inspirados no livro de matemática de um dos produtores."
            ),
            PerguntaQuiz(
                pergunta = "“Por que tão sério?” (Why so serious?) — Dita por qual memorável vilão?",
                opcoes = listOf("Coringa (O Cavaleiro das Trevas)", "Bane (O Cavaleiro das Trevas Ressurge)", "Loki (Vingadores)", "Duende Verde (Homem-Aranha)"),
                respostaCorretaIndex = 0,
                curiosidade = "Heath Ledger improvisou várias pausas e lambidas nos lábios que tornaram a cena assustadoramente viva."
            ),
            PerguntaQuiz(
                pergunta = "“Nunca desista dos seus sonhos, não importa o quão difícil seja.” — Em 'À Procura da Felicidade', quem ensina isso ao filho?",
                opcoes = listOf("Chris Gardner (Will Smith)", "Forrest Gump", "Guido Orefice", "Coach Carter"),
                respostaCorretaIndex = 0,
                curiosidade = "O menino que interpreta o filho de Chris Gardner é Jaden Smith, filho real de Will Smith."
            ),
            PerguntaQuiz(
                pergunta = "“Pede pra sair!” — Qual filme brasileiro gerou esse célebre bordão da cultura pop?",
                opcoes = listOf("Tropa de Elite (2007)", "Cidade de Deus (2002)", "Carandiru (2003)", "O Auto da Compadecida (2000)"),
                respostaCorretaIndex = 0,
                curiosidade = "A cena da granada e o interrogatório com Capitão Nascimento foram gravadas com preparação policial autêntica."
            )
        )

        val selecionadas = bancoCitacoes.shuffled().take(4)
        return selecionadas.map { p ->
            embaralharOpcoes(p.pergunta, p.opcoes, p.respostaCorretaIndex, p.curiosidade)
        }
    }
}

