package com.example.cinelist

/**
 * Representa um post ou tópico no Fórum Social / Feed Compartilhado do CineList.
 * Pode ser um tópico de discussão, crítica/review, teoria, dúvida, recomendação ou registro de mídia assistida/avaliada.
 */
data class AtividadeSocial(
    val id: String = "",
    val autorUid: String = "",
    val autorNome: String = "",
    val autorFotoUrl: String = "",
    val tipo: String = "DISCUSSAO", // "DISCUSSAO", "AVALIOU", "ASSISTIU", "QUERO_VER", "FAVORITOU", "RECOMENDACAO", "TEORIA"
    val categoria: String = "Discussão Geral", // "Discussão Geral", "Crítica / Review", "Teorias & Enigmas", "Recomendações", "Dúvidas"
    val topicoTitulo: String = "", // Título do tópico no fórum (ex: "O que acharam do final de Interestelar?")
    val tmdbId: Int = 0,
    val tituloMidia: String = "",
    val posterPath: String = "",
    val tipoMidia: String = "Filme", // "Filme", "Série", "Anime", etc.
    val nota: Float = 0f, // 0..10 ou 0..5
    val comentario: String = "", // Texto principal do post
    val contemSpoiler: Boolean = false,
    val totalComentarios: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val reacoes: Map<String, List<String>> = emptyMap(), // ex: {"❤️": ["uid1"], "🔥": ["uid2"]}
    val curtidasUids: List<String> = emptyList() // Likes diretos
) {
    val totalLikes: Int
        get() {
            val likesLista = curtidasUids.size
            val likesReacao = reacoes["❤️"]?.size ?: 0
            return maxOf(likesLista, likesReacao)
        }

    fun usuarioDeuLike(meuUid: String): Boolean {
        if (meuUid.isBlank()) return false
        return curtidasUids.contains(meuUid) || (reacoes["❤️"]?.contains(meuUid) == true)
    }
}

/**
 * Representa uma resposta ou comentário dentro de uma discussão do Fórum / Feed.
 */
data class ComentarioForum(
    val id: String = "",
    val atividadeId: String = "",
    val autorUid: String = "",
    val autorNome: String = "Cinéfilo",
    val autorFotoUrl: String = "",
    val texto: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val curtidasUids: List<String> = emptyList()
) {
    val totalLikes: Int
        get() = curtidasUids.size

    fun usuarioCurtiu(meuUid: String): Boolean = meuUid.isNotBlank() && curtidasUids.contains(meuUid)
}
