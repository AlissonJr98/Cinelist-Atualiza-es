package com.example.cinelist

/**
 * Utilitário global para normalizar e traduzir títulos e corpos de notificações
 * (especialmente notificações de novas versões, estreias e lembretes com dados dinâmicos)
 * de acordo com o idioma atualmente selecionado pelo usuário.
 */
object NotificacaoLocalizationHelper {

    fun obterTituloFormatado(notificacao: NotificacaoEntity): String {
        val tituloRaw = notificacao.titulo.trim()
        if (tituloRaw.isBlank()) return ""

        // Se for notificação de atualização/versão em qualquer idioma ou tipo ATUALIZACAO
        val matchVersao = Regex("""(?i)(?:nova\s+vers[ãa]o|new\s+version|nueva\s+versi[óo]n|neuheiten|neue\s+version|nouvelle\s+version|nuova\s+versione|新バージョン|새\s*버전|atualiza[çc][ãa]o|update|version|vers[ãa]o).*?v?(\d+\.\d+\.\d+)""").find(tituloRaw)
            ?: if (notificacao.tipo == "ATUALIZACAO") Regex("""(?i)v?(\d+\.\d+\.\d+)""").find(tituloRaw) else null
        if (matchVersao != null) {
            val versao = matchVersao.groupValues[1]
            return "${"Nova Versão".t()} v$versao ${"Disponível".t()}"
        }

        // Se for notificação de estreia confirmada
        if (tituloRaw.contains("Estreia Confirmada", ignoreCase = true)) {
            return "Estreia Confirmada! 🍿".t()
        }

        // Se for lembrete de títulos esperando
        if (tituloRaw.contains("títulos esperando", ignoreCase = true) || tituloRaw.contains("titulos esperando", ignoreCase = true)) {
            return "Você tem títulos esperando 🎬".t()
        }

        // Se for estreia em streaming
        val matchStreaming = Regex("""(?i)dispon[íi]vel\s+no\s+(.+?)!""").find(tituloRaw)
        if (matchStreaming != null) {
            val streaming = matchStreaming.groupValues[1]
            return "${"Disponível no".t()} $streaming! 📺"
        }

        return tituloRaw.t()
    }

    fun obterMensagemFormatada(notificacao: NotificacaoEntity): String {
        val mensagemRaw = notificacao.mensagem.trim()
        if (mensagemRaw.isBlank()) return ""

        val linhas = mensagemRaw.split("\n")
        val linhasTraduzidas = linhas.map { linha ->
            val linhaTrim = linha.trim()
            if (linhaTrim.isBlank()) return@map ""

            // Cabeçalho de versão (ex: "Novidades da versão 2.0.60:" ou "What's new in version 2.0.60:")
            val matchHeaderVersao = Regex("""(?i)(?:novidades\s+da\s+vers[ãa]o|what'?s\s+new\s+in\s+version|novedades\s+de\s+la\s+versi[óo]n|neuheiten\s+der\s+version|nouveaut[ée]s\s+de\s+la\s+version|novit[àa]\s+della\s+versione|バージョン新機能|버전\s*업데이트\s*내역).*?v?(\d+\.\d+\.\d+):?""").find(linhaTrim)
            if (matchHeaderVersao != null) {
                val versao = matchHeaderVersao.groupValues[1]
                return@map "${"Novidades da versão".t()} $versao:"
            }

            // Frase geral de nova versão (ex: "Uma nova versão (2.0.60) com melhorias...")
            val matchFraseVersao = Regex("""(?i)(?:uma\s+nova\s+vers[ãa]o|a\s+new\s+version|una\s+nueva\s+versi[óo]n|eine\s+neue\s+version|une\s+nouvelle\s+version).*?v?(\d+\.\d+\.\d+).*""").find(linhaTrim)
            if (matchFraseVersao != null) {
                val versao = matchFraseVersao.groupValues[1]
                return@map "${"Uma nova versão".t()} ($versao) ${"com melhorias e correções está disponível.".t()}"
            }

            val ehItemLista = linhaTrim.startsWith("-") || linhaTrim.startsWith("•")
            val textoLimpo = if (ehItemLista) linhaTrim.substring(1).trim() else linhaTrim

            val textoTraduzido = when {
                textoLimpo.equals("Melhorias gerais e correções de desempenho.", ignoreCase = true) ||
                textoLimpo.equals("Melhorias gerais e correções de desempenho", ignoreCase = true) ||
                textoLimpo.equals("General improvements and performance fixes.", ignoreCase = true) ||
                textoLimpo.equals("General improvements and performance fixes", ignoreCase = true) ||
                textoLimpo.equals("Mejoras generales y correcciones de rendimiento.", ignoreCase = true) ||
                textoLimpo.equals("Allgemeine Verbesserungen und Leistungsoptimierungen.", ignoreCase = true) ||
                textoLimpo.equals("Améliorations générales et corrections de performances.", ignoreCase = true) ||
                textoLimpo.equals("Miglioramenti generali e correzioni di prestazioni.", ignoreCase = true) ||
                textoLimpo.equals("全般的な改善とパフォーマンスの向上。", ignoreCase = true) ||
                textoLimpo.equals("일반적인 개선 및 성능 최적화.", ignoreCase = true) ->
                    "Melhorias gerais e correções de desempenho.".t()

                textoLimpo.equals("Melhorias gerais de estabilidade e correções de bugs.", ignoreCase = true) ||
                textoLimpo.equals("Melhorias gerais de estabilidade e correções de bugs", ignoreCase = true) ||
                textoLimpo.equals("General stability improvements and bug fixes.", ignoreCase = true) ||
                textoLimpo.equals("General stability improvements and bug fixes", ignoreCase = true) ->
                    "Melhorias gerais de estabilidade e correções de bugs.".t()

                else -> textoLimpo.t()
            }

            if (ehItemLista) "• $textoTraduzido" else textoTraduzido
        }

        return linhasTraduzidas.joinToString("\n")
    }
}
