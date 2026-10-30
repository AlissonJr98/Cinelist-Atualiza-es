package com.example.cinelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Motor central de internacionalização (i18n) do CineList.
 * Fornece tradução reativa e instantânea para todos os textos da interface
 * nos idiomas suportados pelo aplicativo.
 */
object I18n {

    // Estado reativo do idioma ativo (para que componentes Compose recomponham automaticamente)
    var idiomaAtivo by mutableStateOf("pt-BR")

    fun definirIdioma(tag: String) {
        idiomaAtivo = tag
    }

    private val dicionario: Map<String, Map<String, String>> = mapOf(
        "en" to I18nDicionarioEn.mapa,
        "es" to I18nDicionarioEs.mapa,
        "fr" to I18nDicionarioFr.mapa,
        "de" to I18nDicionarioDe.mapa,
        "it" to I18nDicionarioIt.mapa,
        "ja" to I18nDicionarioJa.mapa,
        "ko" to I18nDicionarioKo.mapa
    )

    /**
     * Traduz uma string em português para o idioma ativo com suporte inteligente
     * para caracteres de pontuação, emojis, contadores, separadores compostos e datas relativas.
     */
    fun traduzir(textoPt: String): String {
        val lang = idiomaAtivo.lowercase().take(2)
        if (lang == "pt") return textoPt

        val limpo = textoPt.trim()
        if (limpo.isEmpty()) return textoPt

        val mapa = dicionario[lang] ?: return textoPt

        // 1. Match direto exato
        mapa[limpo]?.let { return it }

        // 2. Pontuação no final (ex: "Status:", "Gênero:", "Código:", "Ordenar por:")
        val terminaComDoisPontos = limpo.endsWith(":")
        val semPontos = if (terminaComDoisPontos) limpo.dropLast(1).trim() else limpo
        mapa[semPontos]?.let { return if (terminaComDoisPontos) "$it:" else it }

        // 3. Emojis ou símbolos no início (ex: "👤 Minha Lista Pessoal", "⚙️ Gerenciar Salas / Criar Nova", "💡 Recomendações...", "⏳ PENDENTE", "❌ RECUSADO", "🍿 Cinema / Streaming")
        val (prefixoEmoji, restoTexto) = extrairPrefixoEmoji(limpo)
        if (prefixoEmoji.isNotEmpty() && restoTexto.isNotEmpty()) {
            val tradResto = traduzir(restoTexto)
            if (tradResto != restoTexto) {
                return "$prefixoEmoji$tradResto"
            }
        }

        // 4. Emojis ou símbolos no final (ex: "Listas Compartilhadas 🍿", "CineMatch Coletivo! 🍿", "Convidar via WhatsApp 💚")
        val (inicioTexto, sufixoEmoji) = extrairSufixoEmoji(limpo)
        if (sufixoEmoji.isNotEmpty() && inicioTexto.isNotEmpty()) {
            val tradInicio = traduzir(inicioTexto)
            if (tradInicio != inicioTexto) {
                return "$tradInicio$sufixoEmoji"
            }
        }

        // 5. Separador composto " • " (ex: "Filme • Ação", "Série • Comédia", "Filme • Ação • Netflix")
        if (limpo.contains(" • ")) {
            val partes = limpo.split(" • ")
            val traduzidas = partes.map { parte ->
                if (parte.contains(", ")) {
                    parte.split(", ").joinToString(", ") { traduzir(it) }
                } else {
                    traduzir(parte)
                }
            }
            return traduzidas.joinToString(" • ")
        }

        // 6. Contador entre parênteses (ex: "Meus Amigos (3)", "Favoritos (12)", "Pedidos de Conexão (1)", "Membros na Sala (4):")
        val regexContador = Regex("^(.+?)\\s*\\((\\d+)\\)(:?)$")
        val matchContador = regexContador.matchEntire(limpo)
        if (matchContador != null) {
            val base = matchContador.groupValues[1].trim()
            val num = matchContador.groupValues[2]
            val doisPontos = matchContador.groupValues[3]
            val baseTraduzida = traduzir(base)
            return "$baseTraduzida ($num)$doisPontos"
        }

        // 7. Mensagem de títulos exibidos: "15 título(s) exibido(s)"
        val regexTitulosExibidos = Regex("^(\\d+)\\s+título\\(s\\)\\s+exibido\\(s\\)$", RegexOption.IGNORE_CASE)
        val matchTitulos = regexTitulosExibidos.matchEntire(limpo)
        if (matchTitulos != null) {
            val n = matchTitulos.groupValues[1]
            return when (lang) {
                "en" -> "$n title(s) displayed"
                "es" -> "$n título(s) mostrado(s)"
                "fr" -> "$n titre(s) affiché(s)"
                "de" -> "$n Titel angezeigt"
                "it" -> "$n titolo(i) visualizzato(i)"
                "ja" -> "$n 件の作品を表示中"
                "ko" -> "${n}개 작품 표시 중"
                else -> limpo
            }
        }

        // Regra Especial: "T1 • Episódio 4" ou "T1:Ep 4"
        val regexEpisodio = Regex("""^T(\d+)\s*(?:•|:)\s*(?:Episódio|Ep)\s*(\d+)$""", RegexOption.IGNORE_CASE)
        val matchEp = regexEpisodio.matchEntire(limpo)
        if (matchEp != null) {
            val t = matchEp.groupValues[1]
            val ep = matchEp.groupValues[2]
            return when (lang) {
                "en" -> "S$t • Episode $ep"
                "es" -> "T$t • Episodio $ep"
                "fr" -> "S$t • Épisode $ep"
                "de" -> "S$t • Folge $ep"
                "it" -> "S$t • Episodio $ep"
                "ja" -> "S$t • 第${ep}話"
                "ko" -> "시즌$t • ${ep}화"
                else -> limpo
            }
        }

        // Regra Especial: Ofensiva "X Semanas Seguidas"
        val regexSemanas = Regex("""^(\d+)\s+Semanas\s+Seguidas$""", RegexOption.IGNORE_CASE)
        val matchSem = regexSemanas.matchEntire(limpo)
        if (matchSem != null) {
            val n = matchSem.groupValues[1]
            return when (lang) {
                "en" -> "$n Weeks in a Row"
                "es" -> "$n Semanas Seguidas"
                "fr" -> "$n Semaines Consécutives"
                "de" -> "$n Wochen in Folge"
                "it" -> "$n Settimane di Fila"
                "ja" -> "$n 週間連続"
                "ko" -> "${n}주 연속 달성"
                else -> limpo
            }
        }

        // Regra Especial: Conquistas "X / Y Desbloqueadas"
        val regexConquistas = Regex("""^(\d+)\s*/\s*(\d+)\s+Desbloqueadas$""", RegexOption.IGNORE_CASE)
        val matchConq = regexConquistas.matchEntire(limpo)
        if (matchConq != null) {
            val n = matchConq.groupValues[1]
            val total = matchConq.groupValues[2]
            return when (lang) {
                "en" -> "$n / $total Unlocked"
                "es" -> "$n / $total Desbloqueados"
                "fr" -> "$n / $total Débloqués"
                "de" -> "$n / $total Freigeschaltet"
                "it" -> "$n / $total Sbloccati"
                "ja" -> "$n / $total 件解除済み"
                "ko" -> "$n / ${total}개 달성 완료"
                else -> limpo
            }
        }

        // Regra Especial: "X série(s)" pausadas
        val regexSeriesPausadas = Regex("""^(\d+)\s+série\(s\)$""", RegexOption.IGNORE_CASE)
        val matchSeries = regexSeriesPausadas.matchEntire(limpo)
        if (matchSeries != null) {
            val n = matchSeries.groupValues[1]
            return when (lang) {
                "en" -> "$n series"
                "es" -> "$n serie(s)"
                "fr" -> "$n série(s)"
                "de" -> "$n Serie(n)"
                "it" -> "$n serie"
                "ja" -> "$n 件のシリーズ"
                "ko" -> "${n}개 시리즈"
                else -> limpo
            }
        }

        // Regra Especial: Tempo relativo "Há X dias" e "(Há X dias)"
        val regexDias = Regex("""^\(?Há\s+(\d+)\s+dias?\)?$""", RegexOption.IGNORE_CASE)
        val matchDias = regexDias.matchEntire(limpo)
        if (matchDias != null) {
            val n = matchDias.groupValues[1]
            val temParenteses = limpo.startsWith("(") && limpo.endsWith(")")
            val res = when (lang) {
                "en" -> if (n == "1") "1 day ago" else "$n days ago"
                "es" -> if (n == "1") "Hace 1 día" else "Hace $n días"
                "fr" -> if (n == "1") "Il y a 1 jour" else "Il y a $n jours"
                "de" -> if (n == "1") "Vor 1 Tag" else "Vor $n Tagen"
                "it" -> if (n == "1") "1 giorno fa" else "$n giorni fa"
                "ja" -> "${n}日前"
                "ko" -> "${n}일 전"
                else -> if (n == "1") "Há 1 dia" else "Há $n dias"
            }
            return if (temParenteses) "($res)" else res
        }

        // Regra Especial: Contagem de Títulos por Streaming (Ex: "22 TÍTULOS", "1 TÍTULO")
        val regexTitulosStreaming = Regex("""^(\d+)\s+TÍTULOS?$""", RegexOption.IGNORE_CASE)
        val matchTitStreaming = regexTitulosStreaming.matchEntire(limpo)
        if (matchTitStreaming != null) {
            val n = matchTitStreaming.groupValues[1]
            return when (lang) {
                "en" -> if (n == "1") "1 TITLE" else "$n TITLES"
                "es" -> if (n == "1") "1 TÍTULO" else "$n TÍTULOS"
                "fr" -> if (n == "1") "1 TITRE" else "$n TITRES"
                "de" -> if (n == "1") "1 TITEL" else "$n TITEL"
                "it" -> if (n == "1") "1 TITOLO" else "$n TITOLI"
                "ja" -> "$n 作品"
                "ko" -> "${n}개 작품"
                else -> limpo
            }
        }

        // Regra Especial: Contagem de Respostas no Feed (Ex: "3 respostas", "1 resposta")
        val regexRespostasFeed = Regex("""^(\d+)\s+respostas?$""", RegexOption.IGNORE_CASE)
        val matchResp = regexRespostasFeed.matchEntire(limpo)
        if (matchResp != null) {
            val n = matchResp.groupValues[1]
            return when (lang) {
                "en" -> if (n == "1") "1 reply" else "$n replies"
                "es" -> if (n == "1") "1 respuesta" else "$n respuestas"
                "fr" -> if (n == "1") "1 réponse" else "$n réponses"
                "de" -> if (n == "1") "1 Antwort" else "$n Antworten"
                "it" -> if (n == "1") "1 risposta" else "$n risposte"
                "ja" -> "${n}件の返信"
                "ko" -> "${n}개 답변"
                else -> limpo
            }
        }

        // Regra Especial: Tempo relativo Feed "Há X min" e "Há Xh"
        val regexMin = Regex("""^Há\s+(\d+)\s*min$""", RegexOption.IGNORE_CASE)
        val matchMin = regexMin.matchEntire(limpo)
        if (matchMin != null) {
            val n = matchMin.groupValues[1]
            return when (lang) {
                "en" -> "${n}m ago"
                "es" -> "Hace ${n} min"
                "fr" -> "Il y a ${n} min"
                "de" -> "Vor ${n} Min."
                "it" -> "${n} min fa"
                "ja" -> "${n}分前"
                "ko" -> "${n}분 전"
                else -> limpo
            }
        }

        val regexHoras = Regex("""^Há\s+(\d+)\s*h$""", RegexOption.IGNORE_CASE)
        val matchH = regexHoras.matchEntire(limpo)
        if (matchH != null) {
            val n = matchH.groupValues[1]
            return when (lang) {
                "en" -> "${n}h ago"
                "es" -> "Hace ${n}h"
                "fr" -> "Il y a ${n}h"
                "de" -> "Vor ${n} Std."
                "it" -> "${n}h fa"
                "ja" -> "${n}時間前"
                "ko" -> "${n}시간 전"
                else -> limpo
            }
        }

        // 8. Mensagem de sugestões pendentes: "2 sugestão(ões) aguardando"
        val regexSugestoes = Regex("^(\\d+)\\s+sugestão\\(ões\\)\\s+aguardando$", RegexOption.IGNORE_CASE)
        val matchSugest = regexSugestoes.matchEntire(limpo)
        if (matchSugest != null) {
            val n = matchSugest.groupValues[1]
            return when (lang) {
                "en" -> "$n suggestion(s) pending"
                "es" -> "$n sugerencia(s) pendiente(s)"
                "fr" -> "$n suggestion(s) en attente"
                "de" -> "$n ausstehende Vorschläge"
                "it" -> "$n suggerimenti in attesa"
                "ja" -> "$n 件の保留中の提案"
                "ko" -> "${n}개 추천 대기 중"
                else -> limpo
            }
        }

        // 9. Visto: X (ex: "Visto: Hoje", "Visto: Ontem", "Visto: Há 3 dias")
        if (limpo.startsWith("Visto:") || limpo.startsWith("Visto :")) {
            val prefixoTraduzido = mapa["Visto"] ?: when (lang) {
                "en" -> "Watched"
                "es" -> "Visto"
                "fr" -> "Vu"
                "de" -> "Gesehen"
                "it" -> "Visto"
                "ja" -> "視聴済み"
                "ko" -> "시청 완료"
                else -> "Visto"
            }
            val dataPart = limpo.substringAfter(":").trim()
            val dataTraduzida = traduzir(dataPart)
            return "$prefixoTraduzido: $dataTraduzida"
        }

        // 9.1 Data relativa isolada ("Hoje", "Ontem", "Há X dias")
        val dataRel = traduzirDataRelativa(limpo, lang)
        if (dataRel != limpo) {
            return dataRel
        }

        // 10. Parou em: X min
        if (limpo.startsWith("Parou em:")) {
            val prefixoTraduzido = mapa["Parou em"] ?: "Paused at"
            val sufixoMin = mapa["min"] ?: "min"
            return limpo.replace("Parou em", prefixoTraduzido).replace("min", sufixoMin)
        }

        // 11. Sugerido por: X / Adicionado por: X
        if (limpo.startsWith("Sugerido por:")) {
            val rotulo = when (lang) {
                "en" -> "Suggested by:"
                "es" -> "Sugerido por:"
                "fr" -> "Suggéré par :"
                "de" -> "Vorgeschlagen von:"
                "it" -> "Suggerito da:"
                "ja" -> "提案者:"
                "ko" -> "추천자:"
                else -> "Sugerido por:"
            }
            return limpo.replace("Sugerido por:", rotulo)
        }
        if (limpo.startsWith("Adicionado por:")) {
            val rotulo = when (lang) {
                "en" -> "Added by:"
                "es" -> "Agregado por:"
                "fr" -> "Ajouté par :"
                "de" -> "Hinzugefügt von:"
                "it" -> "Aggiunto da:"
                "ja" -> "追加者:"
                "ko" -> "추가한 사람:"
                else -> "Adicionado por:"
            }
            return limpo.replace("Adicionado por:", rotulo)
        }

        // 12. Finalizado em $data
        if (limpo.startsWith("Finalizado em ")) {
            val data = limpo.removePrefix("Finalizado em ").trim()
            val prefixo = when (lang) {
                "en" -> "Completed on"
                "es" -> "Completado el"
                "fr" -> "Terminé le"
                "de" -> "Abgeschlossen am"
                "it" -> "Completato il"
                "ja" -> "完了日:"
                "ko" -> "완료일:"
                else -> "Finalizado em"
            }
            return "$prefixo $data"
        }

        // 13. Temporada e Episódio: T1 • Ep 1 (+1) ou T1 • Episódio 1
        if (limpo.startsWith("T") && limpo.contains("Ep")) {
            val epTrad = mapa["Episódio"] ?: "Ep"
            val tempTrad = when (lang) {
                "en", "fr", "de", "ja", "ko" -> "S"
                else -> "T"
            }
            return limpo.replaceFirst("T", tempTrad).replace("Episódio", epTrad)
        }

        return textoPt
    }

    private fun traduzirDataRelativa(dataStr: String, lang: String): String {
        if (dataStr.equals("Hoje", ignoreCase = true)) {
            return when (lang) {
                "en" -> "Today"
                "es" -> "Hoy"
                "fr" -> "Aujourd'hui"
                "de" -> "Heute"
                "it" -> "Oggi"
                "ja" -> "今日"
                "ko" -> "오늘"
                else -> dataStr
            }
        }
        if (dataStr.equals("Ontem", ignoreCase = true)) {
            return when (lang) {
                "en" -> "Yesterday"
                "es" -> "Ayer"
                "fr" -> "Hier"
                "de" -> "Gestern"
                "it" -> "Ieri"
                "ja" -> "昨日"
                "ko" -> "어제"
                else -> dataStr
            }
        }
        val matchDias = Regex("^Há\\s+(\\d+)\\s+dias$", RegexOption.IGNORE_CASE).matchEntire(dataStr)
        if (matchDias != null) {
            val d = matchDias.groupValues[1]
            return when (lang) {
                "en" -> "$d days ago"
                "es" -> "Hace $d días"
                "fr" -> "Il y a $d jours"
                "de" -> "Vor $d Tagen"
                "it" -> "$d giorni fa"
                "ja" -> "${d}日前"
                "ko" -> "${d}일 전"
                else -> dataStr
            }
        }
        return dataStr
    }

    private fun extrairPrefixoEmoji(texto: String): Pair<String, String> {
        var idx = 0
        while (idx < texto.length) {
            val codePoint = texto.codePointAt(idx)
            val charCount = Character.charCount(codePoint)
            val type = Character.getType(codePoint)
            val isSymbolOrPunct = type == Character.OTHER_SYMBOL.toInt() ||
                    type == Character.MATH_SYMBOL.toInt() ||
                    type == Character.CURRENCY_SYMBOL.toInt() ||
                    type == Character.MODIFIER_SYMBOL.toInt() ||
                    type == Character.SURROGATE.toInt() ||
                    type == Character.SPACE_SEPARATOR.toInt()
            val isEmojiRange = codePoint in 0x1F300..0x1FAFF ||
                    codePoint in 0x2600..0x27BF ||
                    codePoint in 0xFE00..0xFE0F ||
                    codePoint == 0x200D
            if (isSymbolOrPunct || isEmojiRange) {
                idx += charCount
            } else {
                break
            }
        }
        return Pair(texto.substring(0, idx), texto.substring(idx))
    }

    private fun extrairSufixoEmoji(texto: String): Pair<String, String> {
        var idx = texto.length
        while (idx > 0) {
            val codePoint = Character.codePointBefore(texto, idx)
            val charCount = Character.charCount(codePoint)
            val type = Character.getType(codePoint)
            val isSymbolOrPunct = type == Character.OTHER_SYMBOL.toInt() ||
                    type == Character.MATH_SYMBOL.toInt() ||
                    type == Character.CURRENCY_SYMBOL.toInt() ||
                    type == Character.MODIFIER_SYMBOL.toInt() ||
                    type == Character.SURROGATE.toInt() ||
                    type == Character.SPACE_SEPARATOR.toInt()
            val isEmojiRange = codePoint in 0x1F300..0x1FAFF ||
                    codePoint in 0x2600..0x27BF ||
                    codePoint in 0xFE00..0xFE0F ||
                    codePoint == 0x200D
            if (isSymbolOrPunct || isEmojiRange) {
                idx -= charCount
            } else {
                break
            }
        }
        return Pair(texto.substring(0, idx), texto.substring(idx))
    }
}

/**
 * Extensão conveniente para traduzir qualquer string literal da UI do CineList.
 * Exemplo: "Minha Lista".t()
 */
fun String.t(): String {
    return I18n.traduzir(this)
}

/**
 * Formata temporada e episódio respeitando o idioma ativo do usuário.
 */
fun formatarTemporadaEpisodio(temporada: Int, episodio: Int): String {
    val lang = when {
        I18n.idiomaAtivo.startsWith("en", ignoreCase = true) -> "en"
        I18n.idiomaAtivo.startsWith("es", ignoreCase = true) -> "es"
        I18n.idiomaAtivo.startsWith("fr", ignoreCase = true) -> "fr"
        I18n.idiomaAtivo.startsWith("de", ignoreCase = true) -> "de"
        I18n.idiomaAtivo.startsWith("it", ignoreCase = true) -> "it"
        I18n.idiomaAtivo.startsWith("ja", ignoreCase = true) -> "ja"
        I18n.idiomaAtivo.startsWith("ko", ignoreCase = true) -> "ko"
        else -> "pt"
    }
    return when (lang) {
        "en" -> "S$temporada • Ep. $episodio"
        "es" -> "T$temporada • Ep. $episodio"
        "fr" -> "S$temporada • Ép. $episodio"
        "de" -> "S$temporada • Ep. $episodio"
        "it" -> "S$temporada • Ep. $episodio"
        "ja" -> "S$temporada • 第${episodio}話"
        "ko" -> "시즌 $temporada • ${episodio}화"
        else -> "T$temporada • Ep. $episodio"
    }
}
