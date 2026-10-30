package com.example.cinelist

import java.util.Calendar

data class DesafioCineClube(
    val id: String,
    val titulo: String,
    val descricao: String,
    val icone: String,
    val metaTotal: Int,
    val progressoAtual: Int,
    val concluido: Boolean,
    val recompensaXp: Int,
    val badgeNome: String,
    val categoria: String
)

data class StatusCineClube(
    val mesNome: String,
    val temaMes: String,
    val diasRestantes: Int,
    val desafios: List<DesafioCineClube>,
    val xpTotalGanha: Int,
    val totalConcluidos: Int
)

object CineClubeManager {

    fun obterStatusClube(midias: List<Midia>): StatusCineClube {
        val cal = Calendar.getInstance()
        val mesAtual = cal.get(Calendar.MONTH) // 0-based
        val diasNoMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val diaAtual = cal.get(Calendar.DAY_OF_MONTH)
        val diasRestantes = (diasNoMes - diaAtual).coerceAtLeast(1)

        val mesesNomes = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )
        val mesNome = mesesNomes.getOrElse(mesAtual) { "Mês Atual" }.t()

        val concluidas = midias.filter { it.obterStatusEnum() == StatusMidia.CONCLUIDO }

        // 1. Desafio de Gênero do Mês (Sci-Fi / Espaço)
        val countSciFi = concluidas.count {
            it.genero.contains("Ficção", true) || it.genero.contains("Sci-Fi", true) || it.genero.contains("Fantasia", true)
        }
        val metaSciFi = 2
        val d1 = DesafioCineClube(
            id = "desafio_scifi",
            titulo = "🚀 Odisseia Espacial",
            descricao = "Assistir 2 filmes de Ficção Científica ou Fantasia",
            icone = "🌌",
            metaTotal = metaSciFi,
            progressoAtual = countSciFi.coerceAtMost(metaSciFi),
            concluido = countSciFi >= metaSciFi,
            recompensaXp = 250,
            badgeNome = "Navegador Interestelar",
            categoria = "TEMA DO MÊS"
        )

        // 2. Desafio Cinema Nacional
        val countNacional = concluidas.count {
            val sinopse = it.sinopse.lowercase()
            it.tipo.contains("Novela", true) || sinopse.contains("brasil") || sinopse.contains("brasileiro") || sinopse.contains("rio de janeiro") || sinopse.contains("são paulo")
        }
        val metaNacional = 1
        val d2 = DesafioCineClube(
            id = "desafio_nacional",
            titulo = "🇧🇷 Orgulho Nacional",
            descricao = "Assistir 1 produção brasileira no catálogo",
            icone = "🎬",
            metaTotal = metaNacional,
            progressoAtual = countNacional.coerceAtMost(metaNacional),
            concluido = countNacional >= metaNacional,
            recompensaXp = 150,
            badgeNome = "Cinéfilo Tupiniquim",
            categoria = "CULTURA"
        )

        // 3. Desafio Maratonista de Séries
        val epsAssistidos = midias.sumOf { if (!it.tipo.equals("Filme", true)) it.episodioAtual else 0 }
        val metaEps = 10
        val d3 = DesafioCineClube(
            id = "desafio_maratona_eps",
            titulo = "📺 Ritmo Acelerado",
            descricao = "Avançar 10 episódios de séries ou animes",
            icone = "⚡",
            metaTotal = metaEps,
            progressoAtual = epsAssistidos.coerceAtMost(metaEps),
            concluido = epsAssistidos >= metaEps,
            recompensaXp = 300,
            badgeNome = "Mestre das Maratonas",
            categoria = "SÉRIES"
        )

        // 4. Desafio Nota Máxima
        val countObrasTop = concluidas.count { it.nota >= 9 }
        val metaTop = 1
        val d4 = DesafioCineClube(
            id = "desafio_nota_10",
            titulo = "⭐ Crítica Exigente",
            descricao = "Avaliar pelo menos 1 filme ou série com nota 9 ou 10",
            icone = "🏆",
            metaTotal = metaTop,
            progressoAtual = countObrasTop.coerceAtMost(metaTop),
            concluido = countObrasTop >= metaTop,
            recompensaXp = 200,
            badgeNome = "Selo de Ouro CineList",
            categoria = "AVALIAÇÃO"
        )

        val lista = listOf(d1, d2, d3, d4)
        val concluidosCount = lista.count { it.concluido }
        val xpGanha = lista.filter { it.concluido }.sumOf { it.recompensaXp }

        return StatusCineClube(
            mesNome = mesNome,
            temaMes = "Exploradores do Cinema & Narrativas".t(),
            diasRestantes = diasRestantes,
            desafios = lista,
            xpTotalGanha = xpGanha,
            totalConcluidos = concluidosCount
        )
    }
}
