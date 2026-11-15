package com.example.cinelist

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Status de disponibilidade de dublagem em Português do Brasil (PT-BR).
 */
enum class StatusDublagem(val rotuloPt: String, val emoji: String) {
    DUBLADO("Dublado PT-BR", "🎙️"),
    AUDIO_ORIGINAL_PT("Áudio Original", "🇧🇷"),
    APENAS_LEGENDADO("Apenas Legendado", "💬"),
    DESCONHECIDO("A Confirmar", "❓")
}

/**
 * Informações de um personagem e seu dublador brasileiro.
 */
data class VozDublador(
    val personagem: String,
    val atorOriginal: String = "",
    val dublador: String = ""
)

/**
 * Ficha consolidada de dublagem para um filme, série ou anime.
 */
data class InfoDublagem(
    val status: StatusDublagem = StatusDublagem.DESCONHECIDO,
    val temDublagem: Boolean = false,
    val estudio: String? = null,
    val diretores: List<String> = emptyList(),
    val vozesPrincipais: List<VozDublador> = emptyList(),
    val fonte: String = "Dublapédia / Acervo BR",
    val anoGravacao: String? = null,
    val votosDublado: Int = 0,
    val votosLegendado: Int = 0
)

/**
 * Gerenciador inteligente de Dublagem Brasileira (PT-BR).
 *
 * Integra:
 * 1. Detecção automática de produções nacionais (Áudio Original em Português).
 * 2. Consulta em tempo real à API oficial do acervo Dublapédia (MediaWiki pública).
 * 3. Fallback inteligente via CineAI (Gemini) para títulos raros ou lançamentos recentes.
 * 4. Validação e votos comunitários persistidos localmente e em nuvem.
 * 5. Cache de alto desempenho em memória e em disco (SharedPreferences).
 */
object DublagemManager {

    private const val TAG = "DublagemManager"
    private const val PREFS_NAME = "cinelist_dublagem_cache_v1"
    private const val KEY_CACHE_PREFIX = "dub_"

    private val gson = Gson()
    private val cacheMemoria = ConcurrentHashMap<String, InfoDublagem>()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Gera chave de cache única baseada no ID do TMDB ou no título normalizado.
     */
    private fun gerarChave(idTmdb: Int, titulo: String): String {
        return if (idTmdb > 0) "tmdb_$idTmdb" else "tit_${normalizar(titulo)}"
    }

    private fun normalizar(texto: String): String {
        return texto.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    /**
     * Consulta síncrona/rápida no cache local para ver se um item já é sabidamente dublado.
     */
    fun isDubladoRapido(context: Context, idTmdb: Int, titulo: String, idiomaOriginal: String?): Boolean {
        if (idiomaOriginal?.lowercase() in listOf("pt", "por", "pt-br", "pt-pt")) return true
        val chave = gerarChave(idTmdb, titulo)
        val emMemoria = cacheMemoria[chave]
        if (emMemoria != null) return emMemoria.temDublagem

        val json = getPrefs(context).getString("$KEY_CACHE_PREFIX$chave", null) ?: return false
        return try {
            val salvo = gson.fromJson(json, InfoDublagem::class.java)
            cacheMemoria[chave] = salvo
            salvo.temDublagem
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Obtém informações detalhadas sobre a dublagem de um título.
     */
    suspend fun obterInfoDublagem(
        context: Context,
        titulo: String,
        tituloOriginal: String? = null,
        ano: String? = null,
        idiomaOriginal: String? = null,
        idTmdb: Int = 0
    ): InfoDublagem = withContext(Dispatchers.IO) {
        val chave = gerarChave(idTmdb, titulo)

        // 1. Verificar cache em memória
        cacheMemoria[chave]?.let { return@withContext it }

        // 2. Verificar cache em disco (SharedPreferences)
        val jsonSalvo = getPrefs(context).getString("$KEY_CACHE_PREFIX$chave", null)
        if (!jsonSalvo.isNullOrBlank()) {
            try {
                val salvo = gson.fromJson(jsonSalvo, InfoDublagem::class.java)
                if (salvo != null) {
                    cacheMemoria[chave] = salvo
                    return@withContext salvo
                }
            } catch (_: Exception) {}
        }

        // 3. Regra de Produção Nacional Brasileira / Língua Portuguesa
        val idiomaNorm = idiomaOriginal?.trim()?.lowercase()
        if (idiomaNorm in listOf("pt", "por", "pt-br", "pt-pt")) {
            val nacional = InfoDublagem(
                status = StatusDublagem.AUDIO_ORIGINAL_PT,
                temDublagem = true,
                estudio = "Produção Nacional",
                fonte = "TMDB (Idioma Original PT)",
                diretores = emptyList(),
                vozesPrincipais = emptyList()
            )
            salvarEmCache(context, chave, nacional)
            return@withContext nacional
        }

        // 4. Consulta ao acervo Dublapédia (MediaWiki Pública)
        val resultadoWiki = consultarDublapedia(titulo, tituloOriginal)
        if (resultadoWiki != null && resultadoWiki.temDublagem) {
            salvarEmCache(context, chave, resultadoWiki)
            return@withContext resultadoWiki
        }

        // 5. Fallback via CineAI (Gemini) se disponível
        val resultadoAi = consultarCineAi(titulo, ano.orEmpty(), idiomaOriginal)
        if (resultadoAi != null) {
            salvarEmCache(context, chave, resultadoAi)
            return@withContext resultadoAi
        }

        // 6. Não encontrado / Não catalogado
        val desconhecido = InfoDublagem(
            status = StatusDublagem.DESCONHECIDO,
            temDublagem = false,
            estudio = null,
            fonte = "Acervo Colaborativo",
            diretores = emptyList(),
            vozesPrincipais = emptyList()
        )
        salvarEmCache(context, chave, desconhecido)
        return@withContext desconhecido
    }

    /**
     * Consulta a API do MediaWiki da Dublapédia (dublagem.fandom.com).
     */
    private fun consultarDublapedia(titulo: String, tituloOriginal: String?): InfoDublagem? {
        val termosBusca = mutableListOf<String>()
        termosBusca.add(titulo.trim())

        // Adiciona variações úteis (ex: sem subtítulo após ':' ou '-')
        if (titulo.contains(":")) {
            termosBusca.add(titulo.substringBefore(":").trim())
        }
        if (titulo.contains("-")) {
            termosBusca.add(titulo.substringBefore("-").trim())
        }
        if (!tituloOriginal.isNullOrBlank() && !tituloOriginal.equals(titulo, ignoreCase = true)) {
            termosBusca.add(tituloOriginal.trim())
        }

        for (termo in termosBusca.distinct()) {
            try {
                val searchUrl = "https://dublagem.fandom.com/api.php?action=query&list=search&srsearch=${URLEncoder.encode(termo, "UTF-8")}&format=json"
                val requestSearch = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "CineListApp/2.0 (Android; CineList Dublagem)")
                    .build()

                val responseSearch = httpClient.newCall(requestSearch).execute()
                val bodySearch = responseSearch.body?.string().orEmpty()
                if (!responseSearch.isSuccessful || bodySearch.isBlank()) continue

                val rootSearch = gson.fromJson(bodySearch, DublapediaSearchResponse::class.java)
                val itens = rootSearch?.query?.search ?: continue
                if (itens.isEmpty()) continue

                // Prioriza item com título idêntico ou que inicie com o termo buscado (ignorando "(Franquia)")
                val candidato = itens.firstOrNull { item ->
                    val t = item.title.lowercase()
                    !t.contains("(franquia)") && !t.contains("(universo)") && (
                        t == termo.lowercase() || t.startsWith(termo.lowercase())
                    )
                } ?: itens.firstOrNull { !it.title.lowercase().contains("(franquia)") } ?: itens.first()

                // Consulta wikitext da página candidata
                val parseUrl = "https://dublagem.fandom.com/api.php?action=parse&page=${URLEncoder.encode(candidato.title, "UTF-8")}&prop=wikitext&format=json"
                val requestParse = Request.Builder()
                    .url(parseUrl)
                    .header("User-Agent", "CineListApp/2.0 (Android; CineList Dublagem)")
                    .build()

                val responseParse = httpClient.newCall(requestParse).execute()
                val bodyParse = responseParse.body?.string().orEmpty()
                if (!responseParse.isSuccessful || bodyParse.isBlank()) continue

                val rootParse = gson.fromJson(bodyParse, DublapediaParseResponse::class.java)
                val wikitext = rootParse?.parse?.wikitext?.conteudo.orEmpty()
                if (wikitext.isBlank()) continue

                val info = extrairInfoDoWikitext(wikitext, candidato.title)
                if (info != null && (info.temDublagem || !info.estudio.isNullOrBlank() || info.vozesPrincipais.isNotEmpty())) {
                    return info
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao consultar Dublapédia para '$termo': ${e.message}")
            }
        }
        return null
    }

    /**
     * Analisa o código wikitext e extrai estúdio, diretores e elenco de dublagem.
     */
    private fun extrairInfoDoWikitext(wiki: String, tituloPagina: String): InfoDublagem? {
        val temIndicativoDublagem = wiki.contains("estúdio_de_dublagem", ignoreCase = true) ||
                wiki.contains("direção_de_dublagem", ignoreCase = true) ||
                wiki.contains("Dubladores", ignoreCase = true) ||
                wiki.contains("Categoria:Dublagens", ignoreCase = true)

        if (!temIndicativoDublagem) return null

        // 1. Estúdio de Dublagem
        val regexEstudio = Regex("""\|\s*(?:estúdio_de_dublagem|estudio|estúdio)\s*=\s*([^\r\n\|]+)""", RegexOption.IGNORE_CASE)
        val estudioBruto = regexEstudio.find(wiki)?.groupValues?.get(1)
        val estudioLimpo = estudioBruto?.let { limparWikiTexto(it) }?.takeIf { it.isNotBlank() }

        // 2. Direção de Dublagem
        val regexDirecao = Regex("""\|\s*(?:direção_de_dublagem|direção|direcao)\s*=\s*([^\r\n\|]+)""", RegexOption.IGNORE_CASE)
        val direcaoBruta = regexDirecao.find(wiki)?.groupValues?.get(1)
        val diretores = direcaoBruta?.let { limparWikiTexto(it) }
            ?.split(Regex("""<br\s*/?>|;|\n|,"""))
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        // 3. Ano de Gravação / Lançamento da Dublagem
        val regexAno = Regex("""\|\s*(?:data_de_gravação|data_de_gravacao|ano)\s*=\s*([^\r\n\|]+)""", RegexOption.IGNORE_CASE)
        val anoGravacao = regexAno.find(wiki)?.groupValues?.get(1)?.let { limparWikiTexto(it) }?.take(20)

        // 4. Elenco de Dublagem (==Elenco==)
        val vozes = mutableListOf<VozDublador>()
        val linhas = wiki.lines()
        var dentroElenco = false
        var celulasLinha = mutableListOf<String>()

        for (linha in linhas) {
            val l = linha.trim()
            if (l.contains("==Elenco==", ignoreCase = true) || l.contains("== Elenco ==", ignoreCase = true)) {
                dentroElenco = true
                continue
            }
            if (dentroElenco && l.startsWith("==") && !l.contains("Elenco", ignoreCase = true)) {
                dentroElenco = false
                break
            }
            if (!dentroElenco) continue

            if (l.startsWith("|-") || l.startsWith("|}") || l.startsWith("!}")) {
                if (celulasLinha.size >= 2) {
                    val celulasLimpas = celulasLinha.map { limparWikiTexto(it) }.filter { it.isNotBlank() }
                    if (celulasLimpas.size >= 3) {
                        // Formato: [Personagem, Ator Original, Dublador]
                        val personagem = celulasLimpas[0]
                        val ator = celulasLimpas[1]
                        val dublador = celulasLimpas[2]
                        if (dublador.isNotBlank() && dublador != "?" && !dublador.equals("Não informado", ignoreCase = true)) {
                            vozes.add(VozDublador(personagem = personagem, atorOriginal = ator, dublador = dublador))
                        }
                    } else if (celulasLimpas.size == 2) {
                        // Formato simples: [Personagem, Dublador]
                        val personagem = celulasLimpas[0]
                        val dublador = celulasLimpas[1]
                        if (dublador.isNotBlank() && dublador != "?") {
                            vozes.add(VozDublador(personagem = personagem, atorOriginal = "", dublador = dublador))
                        }
                    }
                }
                celulasLinha.clear()
                continue
            }

            if (l.startsWith("|") && !l.startsWith("|-") && !l.startsWith("|}") && !l.startsWith("!")) {
                if (!l.contains("Arquivo:", ignoreCase = true)) {
                    celulasLinha.add(l)
                }
            }
        }

        return InfoDublagem(
            status = StatusDublagem.DUBLADO,
            temDublagem = true,
            estudio = estudioLimpo ?: "Estúdio Brasileiro (Dublapédia)",
            diretores = diretores.take(3),
            vozesPrincipais = vozes.distinctBy { "${it.personagem}_${it.dublador}" }.take(10),
            fonte = "Dublapédia ($tituloPagina)",
            anoGravacao = anoGravacao
        )
    }

    private fun limparWikiTexto(texto: String): String {
        return texto
            .replace(Regex("""\[\[Arquivo:[^\]]+\]\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\[\[[^\]\|]+\|([^\]]+)\]\]"""), "$1")
            .replace(Regex("""\[\[([^\]]+)\]\]"""), "$1")
            .replace(Regex("""\{\{[^\}]+\}\}"""), "")
            .replace(Regex("""<small>[^<]*</small>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace(Regex("""rowspan="\d+"""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""colspan="\d+"""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""style="[^"]*"""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^\|\s*"""), "")
            .replace("''", "")
            .replace("  ", " ")
            .trim()
    }

    /**
     * Fallback via Gemini AI (CineAI) para obras não catalogadas na Dublapédia.
     */
    private suspend fun consultarCineAi(titulo: String, ano: String, idiomaOriginal: String?): InfoDublagem? {
        return try {
            CineAiManager.consultarDublagemAi(titulo, ano, idiomaOriginal)
        } catch (e: Exception) {
            Log.w(TAG, "Falha na consulta CineAI de dublagem: ${e.message}")
            null
        }
    }

    /**
     * Permite que o usuário confirme se a mídia é dublada ou legendada com 1 clique.
     */
    fun registrarVotoComunidade(context: Context, idTmdb: Int, titulo: String, ehDublado: Boolean) {
        val chave = gerarChave(idTmdb, titulo)
        val infoAtual = cacheMemoria[chave] ?: InfoDublagem(
            status = if (ehDublado) StatusDublagem.DUBLADO else StatusDublagem.APENAS_LEGENDADO,
            temDublagem = ehDublado,
            fonte = "Comunidade CineList"
        )

        val atualizada = infoAtual.copy(
            status = if (ehDublado) StatusDublagem.DUBLADO else StatusDublagem.APENAS_LEGENDADO,
            temDublagem = ehDublado,
            votosDublado = if (ehDublado) infoAtual.votosDublado + 1 else infoAtual.votosDublado,
            votosLegendado = if (!ehDublado) infoAtual.votosLegendado + 1 else infoAtual.votosLegendado,
            fonte = "Comunidade CineList"
        )

        salvarEmCache(context, chave, atualizada)

        // Sincronização segura no Firestore em segundo plano
        try {
            if (idTmdb > 0) {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = db.collection("midias_dublagem").document("tmdb_$idTmdb")
                val dados = mapOf(
                    "idTmdb" to idTmdb,
                    "titulo" to titulo,
                    "temDublagem" to ehDublado,
                    "votosDublado" to com.google.firebase.firestore.FieldValue.increment(if (ehDublado) 1L else 0L),
                    "votosLegendado" to com.google.firebase.firestore.FieldValue.increment(if (!ehDublado) 1L else 0L),
                    "atualizadoEm" to System.currentTimeMillis()
                )
                doc.set(dados, com.google.firebase.firestore.SetOptions.merge())
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Erro ao sincronizar voto de dublagem no Firestore: ${ex.message}")
        }
    }

    private fun salvarEmCache(context: Context, chave: String, info: InfoDublagem) {
        cacheMemoria[chave] = info
        try {
            val json = gson.toJson(info)
            getPrefs(context).edit().putString("$KEY_CACHE_PREFIX$chave", json).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao gravar cache de dublagem: ${e.message}")
        }
    }
}

// Modelos auxiliares para parsing JSON da Dublapédia
data class DublapediaSearchItem(
    val title: String,
    val pageid: Long
)
data class DublapediaQuery(
    val search: List<DublapediaSearchItem>? = null
)
data class DublapediaSearchResponse(
    val query: DublapediaQuery? = null
)
data class DublapediaWikitext(
    @com.google.gson.annotations.SerializedName("*")
    val conteudo: String? = null
)
data class DublapediaParse(
    val title: String? = null,
    val pageid: Long = 0,
    val wikitext: DublapediaWikitext? = null
)
data class DublapediaParseResponse(
    val parse: DublapediaParse? = null
)
