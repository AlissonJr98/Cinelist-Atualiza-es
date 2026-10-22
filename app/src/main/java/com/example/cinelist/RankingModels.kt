package com.example.cinelist

/**
 * Representa a posição e o desempenho de um amigo no Ranking Semanal de Maratonas.
 */
data class ItemLeaderboard(
    val uid: String = "",
    val nome: String = "",
    val fotoUrl: String = "",
    val pontos: Int = 0,
    val titulosConcluidos: Int = 0,
    val horasEstimadas: Double = 0.0,
    val posicao: Int = 1,
    val souEu: Boolean = false,
    val medalha: String = "" // "🥇", "🥈", "🥉" ou posição "#4"
)
