package com.example.cinelist.shared.models

/**
 * Item monitorado pelo Radar de Estreias nos Streamings.
 * Multiplataforma (Android & iOS).
 */
data class AlertaRadarStreaming(
    val idTmdb: Int = 0,
    val titulo: String = "",
    val posterUrl: String = "",
    val tipo: String = "Filme",
    val anoLancamento: String = "",
    val jaDisponivel: Boolean = false,
    val provedoresEncontrados: List<String> = emptyList(),
    val dataCriacao: Long = 0L,
    val ultimaVerificacao: Long = 0L
)
