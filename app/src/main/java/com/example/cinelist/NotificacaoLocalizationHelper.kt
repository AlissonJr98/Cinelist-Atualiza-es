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

        // Se for notificação de atualização/versão (ex: "Nova Versão v2.0.60 Disponível", "Nova Versão 2.0.60 Disponível", "Nova versão disponível: 2.0.60")
        val matchVersao = Regex("""(?i)(?:nova\s+vers[ãa]o|new\s+version|nueva\s+versi[óo]n).*?v?(\d+\.\d+\.\d+)""").find(tituloRaw)
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

            // Cabeçalho de versão (ex: "Novidades da versão 2.0.60:" ou "Novidades da versão v2.0.60:")
            val matchHeaderVersao = Regex("""(?i)(?:novidades\s+da\s+vers[ãa]o|what'?s\s+new\s+in\s+version|novedades\s+de\s+la\s+versi[óo]n).*?v?(\d+\.\d+\.\d+):?""").find(linhaTrim)
            if (matchHeaderVersao != null) {
                val versao = matchHeaderVersao.groupValues[1]
                return@map "${"Novidades da versão".t()} $versao:"
            }

            // Frase geral de nova versão (ex: "Uma nova versão (2.0.60) com melhorias...")
            val matchFraseVersao = Regex("""(?i)(?:uma\s+nova\s+vers[ãa]o|a\s+new\s+version).*?v?(\d+\.\d+\.\d+).*""").find(linhaTrim)
            if (matchFraseVersao != null) {
                val versao = matchFraseVersao.groupValues[1]
                return@map "${"Uma nova versão".t()} ($versao) ${"com melhorias e correções está disponível.".t()}"
            }

            val ehItemLista = linhaTrim.startsWith("-") || linhaTrim.startsWith("•")
            val textoLimpo = if (ehItemLista) linhaTrim.substring(1).trim() else linhaTrim

            val textoTraduzido = when {
                textoLimpo.equals("Melhorias gerais e correções de desempenho.", ignoreCase = true) ||
                textoLimpo.equals("Melhorias gerais e correções de desempenho", ignoreCase = true) ->
                    "Melhorias gerais e correções de desempenho.".t()

                textoLimpo.equals("Melhorias gerais de estabilidade e correções de bugs.", ignoreCase = true) ||
                textoLimpo.equals("Melhorias gerais de estabilidade e correções de bugs", ignoreCase = true) ->
                    "Melhorias gerais de estabilidade e correções de bugs.".t()

                else -> textoLimpo.t()
            }

            if (ehItemLista) "• $textoTraduzido" else textoTraduzido
        }

        return linhasTraduzidas.joinToString("\n")
    }
}
