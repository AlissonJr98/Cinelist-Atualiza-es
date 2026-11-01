package com.example.cinelist

/**
 * Utilitário global para normalizar e traduzir títulos e corpos de notificações
 * (notificações de salas compartilhadas, atualizações, lançamentos, lembretes e conexões sociais)
 * rigorosamente no idioma ativo do usuário.
 */
object NotificacaoLocalizationHelper {

    fun obterTituloFormatado(notificacao: NotificacaoEntity): String {
        val tituloRaw = notificacao.titulo.trim()
        if (tituloRaw.isBlank()) return ""

        // 1. Notificação de atualização/versão em qualquer idioma ou tipo ATUALIZACAO
        val matchVersao = Regex("""(?i)(?:nova\s+vers[ãa]o|new\s+version|nueva\s+versi[óo]n|neuheiten|neue\s+version|nouvelle\s+version|nuova\s+versione|新バージョン|새\s*버전|atualiza[çc][ãa]o|update|version|vers[ãa]o).*?v?(\d+\.\d+\.\d+)""").find(tituloRaw)
            ?: if (notificacao.tipo == "ATUALIZACAO") Regex("""(?i)v?(\d+\.\d+\.\d+)""").find(tituloRaw) else null
        if (matchVersao != null) {
            val versao = matchVersao.groupValues[1]
            return "${"Nova Versão".t()} v$versao ${"Disponível".t()}"
        }

        // 2. Novo Episódio
        val matchEpisodio = Regex("""(?i)(?:novo\s+epis[óo]dio|new\s+episode|nuevo\s+episodio|nouvel\s+[ée]pisode|neue\s+folge|nuovo\s+episodio|新エピソード|새\s*에피소드):\s*(.+?)(?:\s*📺)?""").find(tituloRaw)
        if (matchEpisodio != null) {
            val tituloMidia = matchEpisodio.groupValues[1].trim()
            return "${"Novo Episódio".t()}: $tituloMidia 📺"
        }

        // 3. Estreia confirmada
        if (tituloRaw.contains("Estreia Confirmada", ignoreCase = true) ||
            tituloRaw.contains("Premiere Confirmed", ignoreCase = true) ||
            tituloRaw.contains("Estreno Confirmado", ignoreCase = true)) {
            return "Estreia Confirmada! 🍿".t()
        }

        // 4. Lembrete de títulos esperando
        if (tituloRaw.contains("títulos esperando", ignoreCase = true) ||
            tituloRaw.contains("titulos esperando", ignoreCase = true) ||
            tituloRaw.contains("titles waiting", ignoreCase = true) ||
            tituloRaw.contains("títulos en espera", ignoreCase = true)) {
            return "Você tem títulos esperando 🎬".t()
        }

        // 5. Estreia em streaming (ex: "Disponível no Netflix!" ou "Agora no Prime Video!")
        val matchStreaming = Regex("""(?i)(?:dispon[íi]vel\s+no|available\s+on|disponible\s+en|disponible\s+sur|verf[üu]gbar\s+auf|disponibile\s+su|chegou\s+no|now\s+on|lleg[óo]\s+a)\s+(.+?)!""").find(tituloRaw)
        if (matchStreaming != null) {
            val streaming = matchStreaming.groupValues[1].trim()
            return "${"Disponível no".t()} $streaming! 📺"
        }

        // 6. Chamada CineParty / CineMatch
        if (tituloRaw.contains("CineParty", ignoreCase = true)) {
            return "CineParty // Ao Vivo 🍿".t()
        }
        if (tituloRaw.contains("CineMatch", ignoreCase = true)) {
            return "CineMatch 🍿".t()
        }

        // 7. Lista / Sala compartilhada (ex: "Novo item na lista compartilhada" ou "Novo item na sala compartilhada")
        if (tituloRaw.contains("lista compartilhada", ignoreCase = true) ||
            tituloRaw.contains("sala compartilhada", ignoreCase = true) ||
            tituloRaw.contains("shared list", ignoreCase = true) ||
            tituloRaw.contains("shared room", ignoreCase = true) ||
            notificacao.tipo == "NOVO_GRUPO") {
            return "Novo item na lista compartilhada".t()
        }

        // 8. Pedido de amizade
        if (tituloRaw.contains("pedido de amizade", ignoreCase = true) ||
            tituloRaw.contains("solicitação de amizade", ignoreCase = true) ||
            tituloRaw.contains("solicitacao de amizade", ignoreCase = true) ||
            tituloRaw.contains("friend request", ignoreCase = true) ||
            notificacao.tipo == "SOLICITACAO_AMIZADE") {
            return "Novo pedido de amizade! 🍿".t()
        }

        // 9. Amigo concluiu obra
        if (tituloRaw.contains("terminou de assistir", ignoreCase = true) ||
            tituloRaw.contains("finished watching", ignoreCase = true)) {
            return "Alguém terminou de assistir! 🎉".t()
        }

        return tituloRaw.t()
    }

    fun obterMensagemFormatada(notificacao: NotificacaoEntity): String {
        val mensagemRaw = notificacao.mensagem.trim()
        if (mensagemRaw.isBlank()) return ""

        val linhas = mensagemRaw.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        val linhasTraduzidas = linhas.map { linha ->
            val linhaTrim = linha.trim()
            if (linhaTrim.isBlank()) return@map ""

            // 1. Cabeçalho de versão (ex: "Novidades da versão 2.0.60:" ou "What's new in version 2.0.60:")
            val matchHeaderVersao = Regex("""(?i)(?:novidades\s+da\s+vers[ãa]o|what'?s\s+new\s+in\s+version|novedades\s+de\s+la\s+versi[óo]n|neuheiten\s+der\s+version|nouveaut[ée]s\s+de\s+la\s+version|novit[àa]\s+della\s+versione|バージョン新機能|버전\s*업데이트\s*내역).*?v?(\d+\.\d+\.\d+):?""").find(linhaTrim)
            if (matchHeaderVersao != null) {
                val versao = matchHeaderVersao.groupValues[1]
                return@map "${"Novidades da versão".t()} $versao:"
            }

            // 2. Frase geral de nova versão (ex: "Uma nova versão (2.0.60) com melhorias...")
            val matchFraseVersao = Regex("""(?i)(?:uma\s+nova\s+vers[ãa]o|a\s+new\s+version|una\s+nueva\s+versi[óo]n|eine\s+neue\s+version|une\s+nouvelle\s+version).*?v?(\d+\.\d+\.\d+).*""").find(linhaTrim)
            if (matchFraseVersao != null) {
                val versao = matchFraseVersao.groupValues[1]
                return@map "${"Uma nova versão".t()} ($versao) ${"com melhorias e correções está disponível.".t()}"
            }

            // 3. Item adicionado à lista compartilhada pelo parceiro/grupo
            val matchGrupo = Regex("""(?i)^(.+?)\s+(?:adicionou|added|agregó|a ajouté|hat hinzugefügt|ha aggiunto|が追加しました|추가했습니다)\s+"([^"]+)"\s+(?:para o grupo|ao grupo|to the group|al grupo|au groupe|zur Gruppe|al gruppo|グループに|그룹에)!?$""").find(linhaTrim)
            if (matchGrupo != null) {
                val autor = matchGrupo.groupValues[1].trim()
                val midia = matchGrupo.groupValues[2].trim()
                return@map when {
                    I18n.idiomaAtivo.startsWith("en", ignoreCase = true) -> "$autor added \"$midia\" to the group!"
                    I18n.idiomaAtivo.startsWith("es", ignoreCase = true) -> "¡$autor agregó \"$midia\" al grupo!"
                    I18n.idiomaAtivo.startsWith("fr", ignoreCase = true) -> "$autor a ajouté \"$midia\" au groupe !"
                    I18n.idiomaAtivo.startsWith("de", ignoreCase = true) -> "$autor hat \"$midia\" zur Gruppe hinzugefügt!"
                    I18n.idiomaAtivo.startsWith("it", ignoreCase = true) -> "$autor ha aggiunto \"$midia\" al gruppo!"
                    I18n.idiomaAtivo.startsWith("ja", ignoreCase = true) -> "$autor が「$midia」をグループに追加しました！"
                    I18n.idiomaAtivo.startsWith("ko", ignoreCase = true) -> "$autor 님이 그룹에 \"$midia\" 항목을 추가했습니다!"
                    else -> "$autor adicionou \"$midia\" para o grupo!"
                }
            }

            // 4. Amigo concluiu obra compartilhada
            val matchConclusao = Regex("""(?i)^(.+?)\s+(?:acabou de marcar|just marked|acaba de marcar)\s+"([^"]+)"\s+(?:como conclu[íi]do|as completed|como completado).*$""").find(linhaTrim)
            if (matchConclusao != null) {
                val autor = matchConclusao.groupValues[1].trim()
                val midia = matchConclusao.groupValues[2].trim()
                return@map when {
                    I18n.idiomaAtivo.startsWith("en", ignoreCase = true) -> "$autor just marked \"$midia\" as completed. Want to watch it too?"
                    I18n.idiomaAtivo.startsWith("es", ignoreCase = true) -> "$autor acaba de marcar \"$midia\" como completado. ¿Te gustaría verlo también?"
                    I18n.idiomaAtivo.startsWith("fr", ignoreCase = true) -> "$autor vient de marquer \"$midia\" comme terminé. Et si vous le regardiez aussi ?"
                    I18n.idiomaAtivo.startsWith("de", ignoreCase = true) -> "$autor hat \"$midia\" gerade als abgeschlossen markiert. Möchtest du es auch sehen?"
                    I18n.idiomaAtivo.startsWith("it", ignoreCase = true) -> "$autor ha appena contrassegnato \"$midia\" come completato. Ti va di vederlo?"
                    I18n.idiomaAtivo.startsWith("ja", ignoreCase = true) -> "$autor が「$midia」を視聴完了にしました。あなたも観てみませんか？"
                    I18n.idiomaAtivo.startsWith("ko", ignoreCase = true) -> "$autor 님이 \"$midia\" 시청을 완료했습니다. 함께 보시겠습니까?"
                    else -> "$autor acabou de marcar \"$midia\" como concluído. Que tal ver também?"
                }
            }

            // 5. Pedido de amizade
            val matchAmizade = Regex("""(?i)^(.+?)\s+(?:quer se conectar com voc[êe] no CineList|wants to connect with you on CineList|quiere conectarse contigo en CineList).*$""").find(linhaTrim)
            if (matchAmizade != null) {
                val autor = matchAmizade.groupValues[1].trim()
                return@map when {
                    I18n.idiomaAtivo.startsWith("en", ignoreCase = true) -> "$autor wants to connect with you on CineList."
                    I18n.idiomaAtivo.startsWith("es", ignoreCase = true) -> "$autor quiere conectarse contigo en CineList."
                    I18n.idiomaAtivo.startsWith("fr", ignoreCase = true) -> "$autor souhaite se connecter avec vous sur CineList."
                    I18n.idiomaAtivo.startsWith("de", ignoreCase = true) -> "$autor möchte sich mit dir auf CineList verbinden."
                    I18n.idiomaAtivo.startsWith("it", ignoreCase = true) -> "$autor vuole connettersi con te su CineList."
                    I18n.idiomaAtivo.startsWith("ja", ignoreCase = true) -> "$autor が CineList であなたとつながりたがっています。"
                    I18n.idiomaAtivo.startsWith("ko", ignoreCase = true) -> "$autor 님이 CineList에서 친구 연결을 요청했습니다."
                    else -> "$autor quer se conectar com você no CineList."
                }
            }

            // 6. Título em Quero Assistir
            val matchQueroAssistir = Regex("""(?i)^"([^"]+)"\s+(?:est[áa] na sua lista de Quero Assistir|is on your Watchlist|est[áa] en tu lista de Querer Ver).*$""").find(linhaTrim)
            if (matchQueroAssistir != null) {
                val tit = matchQueroAssistir.groupValues[1].trim()
                return@map when {
                    I18n.idiomaAtivo.startsWith("en", ignoreCase = true) -> "\"$tit\" is on your Watchlist."
                    I18n.idiomaAtivo.startsWith("es", ignoreCase = true) -> "\"$tit\" está en tu lista de Querer Ver."
                    I18n.idiomaAtivo.startsWith("fr", ignoreCase = true) -> "\"$tit\" est dans votre liste À voir."
                    I18n.idiomaAtivo.startsWith("de", ignoreCase = true) -> "\"$tit\" ist auf deiner Merkliste."
                    I18n.idiomaAtivo.startsWith("it", ignoreCase = true) -> "\"$tit\" è nella tua lista Da vedere."
                    I18n.idiomaAtivo.startsWith("ja", ignoreCase = true) -> "「$tit」が観たい作品リストに入っています。"
                    I18n.idiomaAtivo.startsWith("ko", ignoreCase = true) -> "\"$tit\" 작품이 보고 싶은 목록에 있습니다."
                    else -> "\"$tit\" está na sua lista de Quero Assistir."
                }
            }

            // 7. Múltiplos títulos esperando
            val matchMultiplosEsperando = Regex("""(?i)^(\d+)\s+(?:t[íi]tulos est[ãa]o parados na sua lista de Quero Assistir|titles are waiting on your Watchlist).*$""").find(linhaTrim)
            if (matchMultiplosEsperando != null) {
                val qtd = matchMultiplosEsperando.groupValues[1].trim()
                return@map when {
                    I18n.idiomaAtivo.startsWith("en", ignoreCase = true) -> "$qtd titles are waiting on your Watchlist."
                    I18n.idiomaAtivo.startsWith("es", ignoreCase = true) -> "$qtd títulos te esperan en tu lista de Querer Ver."
                    I18n.idiomaAtivo.startsWith("fr", ignoreCase = true) -> "$qtd titres vous attendent dans votre liste À voir."
                    I18n.idiomaAtivo.startsWith("de", ignoreCase = true) -> "$qtd Titel warten auf deiner Merkliste."
                    I18n.idiomaAtivo.startsWith("it", ignoreCase = true) -> "$qtd titoli ti aspettano nella tua lista Da vedere."
                    I18n.idiomaAtivo.startsWith("ja", ignoreCase = true) -> "$qtd 件の作品が観たいリストで待機中です。"
                    I18n.idiomaAtivo.startsWith("ko", ignoreCase = true) -> "$qtd 개의 작품이 보고 싶은 목록에서 대기 중입니다."
                    else -> "$qtd títulos estão parados na sua lista de Quero Assistir."
                }
            }

            // 8. Estreia já disponível
            val matchEstreiaJaDisponivel = Regex("""(?i)^"([^"]+)"\s+(?:j[áa] estreou e est[áa] dispon[íi]vel para assistir!|has already premiered and is available to watch!|ya se estren[óo] y est[áa] disponible!).*$""").find(linhaTrim)
            if (matchEstreiaJaDisponivel != null) {
                val tit = matchEstreiaJaDisponivel.groupValues[1].trim()
                return@map when {
                    I18n.idiomaAtivo.startsWith("en", ignoreCase = true) -> "\"$tit\" has already premiered and is available to watch!"
                    I18n.idiomaAtivo.startsWith("es", ignoreCase = true) -> "¡\"$tit\" ya se estrenó y está disponible para ver!"
                    I18n.idiomaAtivo.startsWith("fr", ignoreCase = true) -> "\"$tit\" est déjà sorti et disponible !"
                    I18n.idiomaAtivo.startsWith("de", ignoreCase = true) -> "\"$tit\" ist bereits erschienen und verfügbar!"
                    I18n.idiomaAtivo.startsWith("it", ignoreCase = true) -> "\"$tit\" è già uscito ed è disponibile per la visione!"
                    I18n.idiomaAtivo.startsWith("ja", ignoreCase = true) -> "「$tit」が公開され、視聴可能になりました！"
                    I18n.idiomaAtivo.startsWith("ko", ignoreCase = true) -> "\"$tit\" 작품이 이미 공개되어 시청 가능합니다!"
                    else -> "\"$tit\" já estreou e está disponível para assistir!"
                }
            }

            // 9. Estreia de novo episódio de série (ex: "T1:E5 ("Nome") estreia hoje!" ou "T2:E1 estreia dia 15/11")
            val matchEpHoje = Regex("""(?i)T(\d+):E(\d+)(?:\s*\("([^"]+)"\))?\s+(?:estreia\s+hoje!|premieres\s+today!|se\s+estrena\s+hoy!|première\s+aujourd'hui\s*!|premiere\s+heute!|debutta\s+oggi!|本日初公開！|오늘\s*첫\s*공개!)""").find(linhaTrim)
            if (matchEpHoje != null) {
                val temp = matchEpHoje.groupValues[1]
                val ep = matchEpHoje.groupValues[2]
                val nomeEp = matchEpHoje.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { " (\"$it\")" } ?: ""
                return@map "T$temp:E$ep$nomeEp ${"estreia hoje!".t()}"
            }

            val matchEpDia = Regex("""(?i)T(\d+):E(\d+)(?:\s*\("([^"]+)"\))?\s+(?:estreia\s+dia|premieres\s+on|se\s+estrena\s+el|première\s+le|premiere\s+am|debutta\s+il|公開日:|첫\s*공개\s*날짜:)\s*(\d{1,2}/\d{1,2})""").find(linhaTrim)
            if (matchEpDia != null) {
                val temp = matchEpDia.groupValues[1]
                val ep = matchEpDia.groupValues[2]
                val nomeEp = matchEpDia.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { " (\"$it\")" } ?: ""
                val data = matchEpDia.groupValues[4]
                return@map "T$temp:E$ep$nomeEp ${"estreia dia".t()} $data"
            }

            // 10. Estreia no catálogo de streaming (ex: ""Duna" acabou de estrear na Max! Está na sua lista de espera.")
            val matchStreamingCorpo = Regex("""(?i)"?([^"]+)"?\s+(?:acabou\s+de\s+estrear\s+n[ao]|just\s+arrived\s+on|acaba\s+de\s+estrenarse\s+en|vient\s+d'arriver\s+sur|ist\s+jetzt\s+auf|è\s+appena\s+arrivato\s+su)\s+([^!.]+)[!.]\s*(.*)""").find(linhaTrim)
            if (matchStreamingCorpo != null) {
                val titulo = matchStreamingCorpo.groupValues[1].trim()
                val prov = matchStreamingCorpo.groupValues[2].trim()
                val (_, msgLocalizada) = NotificacaoHelper.gerarTextoEstreiaStreaming(titulo, prov)
                return@map msgLocalizada
            }

            val ehItemLista = linhaTrim.startsWith("-") || linhaTrim.startsWith("•") || linhaTrim.startsWith("*")
            val textoLimpo = if (ehItemLista) linhaTrim.substring(1).trim() else linhaTrim

            // 11. Frases conhecidas de atualizações, melhorias e changelog
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

                textoLimpo.contains("Distinção inteligente de streaming nos Detalhes", ignoreCase = true) ->
                    "Distinção inteligente de streaming nos Detalhes: identificação clara se a obra requer assinatura ou está disponível grátis".t()

                textoLimpo.contains("Novos Termos de Uso e Condições", ignoreCase = true) ->
                    "Novos Termos de Uso e Condições preparados para comercialização futura, licenciamento e diretrizes da plataforma".t()

                textoLimpo.contains("Nova Política de Privacidade com conformidade total LGPD", ignoreCase = true) ->
                    "Nova Política de Privacidade com conformidade total LGPD e GDPR, proteção de dados e exclusão de conta".t()

                textoLimpo.contains("Canal de Atendimento e Suporte oficial", ignoreCase = true) ->
                    "Canal de Atendimento e Suporte oficial com e-mail direto para suportecinelist@gmail.com".t()

                textoLimpo.contains("Atribuição oficial da API do TMDB", ignoreCase = true) ->
                    "Atribuição oficial da API do TMDB com link direto para themoviedb.org conforme diretrizes oficiais".t()

                textoLimpo.contains("Janela de atualização com nova animação", ignoreCase = true) ->
                    "Janela de atualização com nova animação de entrada com física de mola (Pop-in Spring) e anel orbital".t()

                textoLimpo.contains("Preservação definitiva do changelog", ignoreCase = true) ->
                    "Preservação definitiva do changelog nas publicações OTA pelo GitHub Actions".t()

                textoLimpo.contains("Tradução dinâmica e normalização automática de todos os gêneros", ignoreCase = true) ->
                    "Tradução dinâmica e normalização automática de todos os gêneros (filmes e séries)".t()

                else -> textoLimpo.t()
            }

            if (ehItemLista) "• $textoTraduzido" else textoTraduzido
        }

        return linhasTraduzidas.joinToString("\n")
    }
}
