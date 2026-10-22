package com.example.cinelist

/**
 * Item monitorado pelo Radar de Estreias nos Streamings Brasileiros.
 * Rastreia títulos que o usuário quer assistir e alerta quando chegam à Netflix, Prime Video, Max, etc.
 */
data class AlertaRadarStreaming(
    val idTmdb: Int = 0,
    val titulo: String = "",
    val posterUrl: String = "",
    val tipo: String = "Filme",
    val anoLancamento: String = "",
    val jaDisponivel: Boolean = false,
    val provedoresEncontrados: List<String> = emptyList(),
    val dataCriacao: Long = System.currentTimeMillis(),
    val ultimaVerificacao: Long = System.currentTimeMillis()
)
