package com.example.cinelist

data class Conquista(
    val id: String,
    val titulo: String,
    val descricao: String,
    val iconeEmoji: String,
    val regraDesbloqueio: (lista: List<Midia>) -> Boolean
)

val listaDeConquistasCineList = listOf(
    Conquista(
        id = "primeiro_filme",
        titulo = "Estreia no Cinema",
        descricao = "Conclua seu primeiro filme.",
        iconeEmoji = "🎟️",
        regraDesbloqueio = { lista ->
            lista.any { it.tipo.equals("Filme", ignoreCase = true) && (it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true)) }
        }
    ),
    Conquista(
        id = "maratonista_serie",
        titulo = "Maratonista de Plantão",
        descricao = "Conclua pelo menos 3 séries ou animes.",
        iconeEmoji = "📺",
        regraDesbloqueio = { lista ->
            lista.count { !it.tipo.equals("Filme", ignoreCase = true) && (it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true)) } >= 3
        }
    ),
    Conquista(
        id = "critico_cinematografico",
        titulo = "Crítico Oficial",
        descricao = "Avalie 5 ou mais títulos com notas.",
        iconeEmoji = "⭐",
        regraDesbloqueio = { lista ->
            lista.count { it.nota > 0 } >= 5
        }
    ),
    Conquista(
        id = "colecionador_50",
        titulo = "Cinéfilo Raiz",
        descricao = "Adicione 10 ou mais títulos à sua lista.",
        iconeEmoji = "🍿",
        regraDesbloqueio = { lista ->
            lista.size >= 10
        }
    ),
    Conquista(
        id = "casal_conecta",
        titulo = "Sintonia Perfeita",
        descricao = "Conecte-se a uma sala compartilhada.",
        iconeEmoji = "❤️",
        regraDesbloqueio = { lista ->
            lista.any { it.isCasal }
        }
    )
)