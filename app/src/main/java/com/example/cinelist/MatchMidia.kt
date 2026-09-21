package com.example.cinelist

data class MatchMidia(
    val idDoc: String = "",
    val idTmdb: Int = 0,
    val titulo: String = "",
    val imagemCapa: String = "",
    val tipo: String = "Filme",
    val genero: String = "",
    val sinopse: String = "",
    val votos: List<String> = emptyList()
) {
    fun deuMatch(quantidadeMembrosMinima: Int = 2): Boolean {
        return votos.distinct().size >= quantidadeMembrosMinima
    }
}