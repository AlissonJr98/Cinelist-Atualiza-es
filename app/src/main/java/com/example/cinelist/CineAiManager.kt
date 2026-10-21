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
import java.util.concurrent.TimeUnit

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

/**
 * Sugestão gerada pelo CineAI com enriquecimento analítico.
 */
data class CineAiSugestao(
    val tmdbItem: TmdbFilme,
    val justificativaIa: String,
    val destaques: List<String>,
    val vibeNome: String,
    val isGeradoPorGemini: Boolean = false
)

/**
 * Motor Inteligente Híbrido do CineAI.
 * 
 * Funciona de forma 100% autônoma através de algoritmos heurísticos no TMDB,
 * e ativa o modelo Gemini 2.5 Flash automaticamente quando uma chave de API estiver presente.
 */
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
     * Recomenda títulos com base na vibe selecionada e/ou texto livre.
     */
    suspend fun recomendar(
        context: Context,
        vibe: VibeHumor?,
        textoLivre: String,
        filtroTipo: String // "TODOS", "FILME", "SERIE"
    ): List<CineAiSugestao> = withContext(Dispatchers.IO) {
        val chaveGemini = obterChaveGemini(context)

        // Se houver chave Gemini válida, tenta primeiro a IA generativa
        if (chaveGemini.isNotBlank()) {
            val resultadoGemini = runCatching {
                recomendarComGemini(chaveGemini, vibe, textoLivre, filtroTipo)
            }.getOrNull()

            if (!resultadoGemini.isNullOrEmpty()) {
                return@withContext resultadoGemini
            }
        }

        // Fallback robusto e instantâneo: Motor Heurístico Semântico TMDB
        return@withContext recomendarComMotorHeuristico(vibe, textoLivre, filtroTipo)
    }

    /**
     * Recomendações personalizadas via Google Gemini 2.5 Flash.
     */
    private suspend fun recomendarComGemini(
        apiKey: String,
        vibe: VibeHumor?,
        textoLivre: String,
        filtroTipo: String
    ): List<CineAiSugestao> = coroutineScope {
        val tipoDescricao = when (filtroTipo) {
            "FILME" -> "somente filmes"
            "SERIE" -> "somente séries de TV"
            else -> "filmes ou séries"
        }

        val promptVibe = vibe?.promptGemini ?: "excelente produção aclamada"
        val promptUsuario = textoLivre.trim()

        val promptFinal = """
            Você é o CineAI, um curador cinéfilo brilhante e carismático.
            O usuário deseja assistir $tipoDescricao com a seguinte vibe ou pedido:
            - Vibe selecionada: $promptVibe
            - Pedido do usuário: "$promptUsuario"
            
            Retorne exatamente 3 recomendações cinematográficas reais e incríveis que se encaixem perfeitamente.
            Para cada obra, forneça o título exato em português (ou original se mais conhecido), o ano aproximado, e uma justificativa envolvente de no máximo 2 frases explicando por que é perfeita para esse momento, além de 3 palavras-chave de destaque.
            
            Responda EXCLUSIVAMENTE em formato JSON com esta estrutura (sem markdown extra, sem blocos ```json):
            {
              "recomendacoes": [
                {
                  "titulo": "Nome do Filme",
                  "ano": 2021,
                  "justificativa": "Uma frase convincente destacando o motivo.",
                  "destaques": ["Destaque 1", "Destaque 2", "Destaque 3"]
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
        if (!response.isSuccessful) {
            Log.w(TAG, "Gemini API retornou código ${response.code}")
            return@coroutineScope emptyList()
        }

        val responseBody = response.body?.string().orEmpty()
        val jsonResposta = gson.fromJson(responseBody, JsonObject::class.java)
        val textoGerado = jsonResposta.getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString.orEmpty()

        val textoLimpo = textoGerado
            .replace("```json", "")
            .replace("```", "")
            .trim()

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
                val correspondencia = busca.firstOrNull { it.caminhoPoster != null } ?: busca.firstOrNull()
                
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
     * Motor Heurístico Semântico offline-first usando a base do TMDB com filtros precisos de vibe.
     */
    private suspend fun recomendarComMotorHeuristico(
        vibe: VibeHumor?,
        textoLivre: String,
        filtroTipo: String
    ): List<CineAiSugestao> = coroutineScope {
        val api = RetrofitClient.apiService

        // Se houver busca livre por palavras fortes, busca diretamente
        if (textoLivre.isNotBlank() && vibe == null) {
            val buscaDireta = runCatching { api.buscarMulti(textoLivre).resultados }.getOrNull().orEmpty()
                .filter { !it.caminhoPoster.isNullOrBlank() }
                .take(6)

            if (buscaDireta.isNotEmpty()) {
                return@coroutineScope buscaDireta.map { item ->
                    val destaque1 = if (item.mediaVotos >= 7.0) "⭐ Nota ${String.format("%.1f", item.mediaVotos)}" else "Em Alta"
                    val destaque2 = if (item.anoLancamento.isNotBlank()) "Ano ${item.anoLancamento}" else "Destaque"
                    val destaque3 = if (item.ehSerie) "Série de TV" else "Longa-metragem"

                    CineAiSugestao(
                        tmdbItem = item,
                        justificativaIa = "Correspondência encontrada com foco no que você pediu.",
                        destaques = listOf(destaque1, destaque2, destaque3),
                        vibeNome = "Busca Direta",
                        isGeradoPorGemini = false
                    )
                }
            }
        }

        // Seleção de gênero baseado na Vibe
        val vibeAlvo = vibe ?: VibeHumor.PLOT_TWIST
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
                            sortBy = if (vibeAlvo == VibeHumor.PLOT_TWIST) "vote_average.desc" else "popularity.desc"
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
                "Uma das obras mais aclamadas no gênero com reviravoltas primorosas e nota ${String.format("%.1f", item.mediaVotos)}."
            } else {
                vibeAlvo.justificativaPadrao
            }

            val destaqueNota = if (item.mediaVotos > 0.0) "⭐ ${String.format("%.1f", item.mediaVotos)}" else "Top Avaliado"
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
     * Gera 3 destaques objetivos sem spoilers sobre uma obra.
     */
    fun gerarDestaquesSemSpoiler(titulo: String, sinopse: String, nota: Double): List<String> {
        val pontos = mutableListOf<String>()

        if (nota >= 8.0) {
            pontos.add("🌟 Obra-prima aclamada pelo público e crítica especializada (Nota ${String.format("%.1f", nota)})")
        } else if (nota >= 7.0) {
            pontos.add("⭐ Muito bem avaliado pela comunidade cinéfila (Nota ${String.format("%.1f", nota)})")
        } else {
            pontos.add("🍿 Ótima pedida para entretenimento despretensioso")
        }

        if (sinopse.contains("mistério", ignoreCase = true) || sinopse.contains("crime", ignoreCase = true) || sinopse.contains("assassin", ignoreCase = true)) {
            pontos.add("🔍 Trama investigativa envolvente com segredos revelados aos poucos")
        } else if (sinopse.contains("amor", ignoreCase = true) || sinopse.contains("romance", ignoreCase = true) || sinopse.contains("coração", ignoreCase = true)) {
            pontos.add("💖 Forte carga afetiva e conexão cativante entre os personagens")
        } else if (sinopse.contains("espaço", ignoreCase = true) || sinopse.contains("planeta", ignoreCase = true) || sinopse.contains("futuro", ignoreCase = true)) {
            pontos.add("🚀 Exploração de conceitos fascinantes e visual imersivo")
        } else {
            pontos.add("🎭 Excelente desenvolvimento de personagens e conflitos dinâmicos")
        }

        pontos.add("✅ Veredito CineAI: Experiência recomendada que vale o seu tempo")

        return pontos
    }
}
