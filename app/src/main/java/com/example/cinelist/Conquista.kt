package com.example.cinelist

import java.util.Calendar

data class Conquista(
    val id: String,
    val titulo: String,
    val descricao: String,
    val iconeEmoji: String,
    val nivel: String = "Bronze",
    val regraDesbloqueio: (lista: List<Midia>) -> Boolean
)

fun calcularOfensivaSemanal(listaDeMidias: List<Midia>): Int {
    if (listaDeMidias.isEmpty()) return 0
    val agora = System.currentTimeMillis()
    val cal = Calendar.getInstance()

    val semanasAtivas = mutableSetOf<String>()
    for (midia in listaDeMidias) {
        val t = if (midia.dataConclusao > 0L) midia.dataConclusao else midia.atualizadoEm
        if (t > 0L) {
            cal.timeInMillis = t
            val ano = cal.get(Calendar.YEAR)
            val semana = cal.get(Calendar.WEEK_OF_YEAR)
            semanasAtivas.add("${ano}_$semana")
        }
    }

    var streak = 0
    cal.timeInMillis = agora
    while (true) {
        val ano = cal.get(Calendar.YEAR)
        val semana = cal.get(Calendar.WEEK_OF_YEAR)
        if (semanasAtivas.contains("${ano}_$semana")) {
            streak++
            cal.add(Calendar.WEEK_OF_YEAR, -1)
        } else {
            if (streak == 0) {
                cal.add(Calendar.WEEK_OF_YEAR, -1)
                val anoAnterior = cal.get(Calendar.YEAR)
                val semanaAnterior = cal.get(Calendar.WEEK_OF_YEAR)
                if (semanasAtivas.contains("${anoAnterior}_$semanaAnterior")) {
                    continue
                }
            }
            break
        }
    }
    return streak
}

val listaDeConquistasCineList = listOf(
    Conquista(
        id = "primeiro_filme",
        titulo = "Estreia no Cinema",
        descricao = "Conclua seu primeiro filme.",
        iconeEmoji = "🎟️",
        nivel = "Bronze",
        regraDesbloqueio = { lista ->
            lista.any { it.tipo.equals("Filme", ignoreCase = true) && (it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true)) }
        }
    ),
    Conquista(
        id = "maratonista_10_ep",
        titulo = "Maratonista Bronze",
        descricao = "Assista / avance pelo menos 10 episódios.",
        iconeEmoji = "🥉",
        nivel = "Bronze",
        regraDesbloqueio = { lista ->
            lista.sumOf { if (!it.tipo.equals("Filme", ignoreCase = true)) it.episodioAtual else 0 } >= 10
        }
    ),
    Conquista(
        id = "maratonista_50_ep",
        titulo = "Maratonista Prata",
        descricao = "Assista / avance pelo menos 50 episódios.",
        iconeEmoji = "🥈",
        nivel = "Prata",
        regraDesbloqueio = { lista ->
            lista.sumOf { if (!it.tipo.equals("Filme", ignoreCase = true)) it.episodioAtual else 0 } >= 50
        }
    ),
    Conquista(
        id = "maratonista_100_ep",
        titulo = "Maratonista Ouro",
        descricao = "Assista / avance 100 ou mais episódios.",
        iconeEmoji = "🥇",
        nivel = "Ouro",
        regraDesbloqueio = { lista ->
            lista.sumOf { if (!it.tipo.equals("Filme", ignoreCase = true)) it.episodioAtual else 0 } >= 100
        }
    ),
    Conquista(
        id = "critico_cinematografico",
        titulo = "Crítico Oficial",
        descricao = "Avalie 5 ou mais títulos com notas.",
        iconeEmoji = "⭐",
        nivel = "Bronze",
        regraDesbloqueio = { lista ->
            lista.count { it.nota > 0 } >= 5
        }
    ),
    Conquista(
        id = "critico_ouro",
        titulo = "Crítico Renomado",
        descricao = "Avalie 15 ou mais títulos com notas e comentários.",
        iconeEmoji = "📝",
        nivel = "Ouro",
        regraDesbloqueio = { lista ->
            lista.count { it.nota > 0 && it.comentarioPessoal.isNotBlank() } >= 15
        }
    ),
    Conquista(
        id = "mestre_terror",
        titulo = "Mestre do Terror",
        descricao = "Conclua 3 ou mais títulos de Terror.",
        iconeEmoji = "👻",
        nivel = "Prata",
        regraDesbloqueio = { lista ->
            lista.count { it.genero.contains("Terror", ignoreCase = true) && (it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true)) } >= 3
        }
    ),
    Conquista(
        id = "mestre_comedia",
        titulo = "Mestre da Comédia",
        descricao = "Conclua 3 ou mais títulos de Comédia.",
        iconeEmoji = "😂",
        nivel = "Prata",
        regraDesbloqueio = { lista ->
            lista.count { it.genero.contains("Comédia", ignoreCase = true) && (it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true)) } >= 3
        }
    ),
    Conquista(
        id = "mestre_acao",
        titulo = "Mestre da Ação",
        descricao = "Conclua 3 ou mais títulos de Ação.",
        iconeEmoji = "💥",
        nivel = "Prata",
        regraDesbloqueio = { lista ->
            lista.count { it.genero.contains("Ação", ignoreCase = true) && (it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true)) } >= 3
        }
    ),
    Conquista(
        id = "casal_conecta",
        titulo = "Cinéfilo Social",
        descricao = "Conecte-se a uma sala compartilhada.",
        iconeEmoji = "❤️",
        nivel = "Ouro",
        regraDesbloqueio = { lista ->
            lista.any { it.isCasal }
        }
    )
)