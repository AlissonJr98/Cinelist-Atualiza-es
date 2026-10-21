package com.example.cinelist

/**
 * Representa um emblema ou conquista no CineList.
 */
data class Emblema(
    val id: String,
    val titulo: String,
    val descricao: String,
    val emoji: String,
    val desbloqueado: Boolean,
    val progressoAtual: Int,
    val progressoMeta: Int,
    val dica: String
)

object EmblemasManager {

    fun calcularEmblemas(
        midias: List<Midia>,
        totalAmigos: Int,
        estaEmSala: Boolean
    ): List<Emblema> {
        val totalMidias = midias.size
        val assistidos = midias.count { it.obterStatusEnum() == StatusMidia.CONCLUIDO || it.jaEncerrou }
        val avaliados = midias.count { it.nota > 0 }
        val favoritos = midias.count { it.favorito }
        val sciFi = midias.count { it.genero.contains("Ficção", ignoreCase = true) || it.genero.contains("Sci-Fi", ignoreCase = true) }
        val animes = midias.count { it.tipo.equals("Anime", ignoreCase = true) || it.genero.contains("Animação", ignoreCase = true) }
        val series = midias.count { it.tipo.equals("Série", ignoreCase = true) }

        return listOf(
            Emblema(
                id = "primeiro_passo",
                titulo = "Primeira Sessão",
                descricao = "Adicionou o seu primeiro filme ou série ao CineList.",
                emoji = "🥉",
                desbloqueado = totalMidias >= 1,
                progressoAtual = totalMidias.coerceAtMost(1),
                progressoMeta = 1,
                dica = "Adicione 1 título à sua lista"
            ),
            Emblema(
                id = "maratonista_iniciante",
                titulo = "Maratonista",
                descricao = "Concluiu e assistiu a pelo menos 5 obras.",
                emoji = "🎬",
                desbloqueado = assistidos >= 5,
                progressoAtual = assistidos.coerceAtMost(5),
                progressoMeta = 5,
                dica = "Assista a 5 filmes ou séries ($assistidos/5)"
            ),
            Emblema(
                id = "cinefilo_carteirinha",
                titulo = "Cinéfilo de Carteirinha",
                descricao = "Assistiu a 25 filmes ou séries na sua trajetória.",
                emoji = "🍿",
                desbloqueado = assistidos >= 25,
                progressoAtual = assistidos.coerceAtMost(25),
                progressoMeta = 25,
                dica = "Assista a 25 títulos ($assistidos/25)"
            ),
            Emblema(
                id = "mestre_telas",
                titulo = "Mestre das Telas",
                descricao = "Assistiu a 100 filmes ou séries. Você é lendário!",
                emoji = "👑",
                desbloqueado = assistidos >= 100,
                progressoAtual = assistidos.coerceAtMost(100),
                progressoMeta = 100,
                dica = "Assista a 100 títulos ($assistidos/100)"
            ),
            Emblema(
                id = "critico_cinema",
                titulo = "Crítico Exigente",
                descricao = "Avaliou com nota e estrelas pelo menos 5 títulos.",
                emoji = "⭐",
                desbloqueado = avaliados >= 5,
                progressoAtual = avaliados.coerceAtMost(5),
                progressoMeta = 5,
                dica = "Avalie 5 títulos com estrelas ($avaliados/5)"
            ),
            Emblema(
                id = "coracao_ouro",
                titulo = "Amor à Sétima Arte",
                descricao = "Adicionou 10 ou mais títulos à sua lista de Favoritos.",
                emoji = "💖",
                desbloqueado = favoritos >= 10,
                progressoAtual = favoritos.coerceAtMost(10),
                progressoMeta = 10,
                dica = "Favorite 10 filmes ou séries ($favoritos/10)"
            ),
            Emblema(
                id = "serie_maniaco",
                titulo = "Viciado em Séries",
                descricao = "Acompanha ou já maratonou 5 séries diferentes.",
                emoji = "📺",
                desbloqueado = series >= 5,
                progressoAtual = series.coerceAtMost(5),
                progressoMeta = 5,
                dica = "Tenha 5 séries na sua lista ($series/5)"
            ),
            Emblema(
                id = "explorador_scifi",
                titulo = "Explorador Espacial",
                descricao = "Tem pelo menos 3 títulos de Ficção Científica salvos.",
                emoji = "🚀",
                desbloqueado = sciFi >= 3,
                progressoAtual = sciFi.coerceAtMost(3),
                progressoMeta = 3,
                dica = "Salve 3 títulos de ficção científica ($sciFi/3)"
            ),
            Emblema(
                id = "otaku_cinelist",
                titulo = "Cultura Pop & Animes",
                descricao = "Salvou pelo menos 3 animes ou animações.",
                emoji = "🎌",
                desbloqueado = animes >= 3,
                progressoAtual = animes.coerceAtMost(3),
                progressoMeta = 3,
                dica = "Salve 3 animes ou animações ($animes/3)"
            ),
            Emblema(
                id = "sociedade_cinema",
                titulo = "Em Boa Companhia",
                descricao = "Conectou-se com pelo menos 1 amigo no CineList.",
                emoji = "🤝",
                desbloqueado = totalAmigos >= 1,
                progressoAtual = totalAmigos.coerceAtMost(1),
                progressoMeta = 1,
                dica = "Adicione 1 amigo no perfil ($totalAmigos/1)"
            ),
            Emblema(
                id = "clube_cinema",
                titulo = "Membro do Clube",
                descricao = "Criou ou faz parte de uma sala compartilhada de grupo.",
                emoji = "🏰",
                desbloqueado = estaEmSala,
                progressoAtual = if (estaEmSala) 1 else 0,
                progressoMeta = 1,
                dica = "Participe de uma Sala Compartilhada"
            )
        )
    }
}
