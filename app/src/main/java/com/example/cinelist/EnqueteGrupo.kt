package com.example.cinelist

/**
 * Representa uma enquete de votação rápida em um grupo/sala compartilhada.
 */
data class EnqueteGrupo(
    val id: String = "",
    val grupoId: String = "",
    val criadorUid: String = "",
    val criadorNome: String = "",
    val criadorFotoUrl: String = "",
    val titulo: String = "O que vamos assistir hoje?",
    val descricao: String = "",
    val opcoes: List<Map<String, Any>> = emptyList(), // Lista de mapas com { "id", "titulo", "posterPath", "tmdbId" }
    val votos: Map<String, String> = emptyMap(), // mapa de uidUsuario -> opcaoId
    val encerrada: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val expiraEm: Long = 0L
)
