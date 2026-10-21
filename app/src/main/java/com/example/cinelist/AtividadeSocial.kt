package com.example.cinelist

/**
 * Representa uma atividade no Feed Social compartilhado entre amigos.
 * Pode ser uma avaliação, conclusão de mídia (assistido), adição aos favoritos, etc.
 */
data class AtividadeSocial(
    val id: String = "",
    val autorUid: String = "",
    val autorNome: String = "",
    val autorFotoUrl: String = "",
    val tipo: String = "AVALIOU", // "AVALIOU", "ASSISTIU", "QUERO_VER", "FAVORITOU", "RECOMENDACAO"
    val tmdbId: Int = 0,
    val tituloMidia: String = "",
    val posterPath: String = "",
    val tipoMidia: String = "Filme", // "Filme", "Série", "Anime", etc.
    val nota: Float = 0f, // 0..10 ou 0..5
    val comentario: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val reacoes: Map<String, List<String>> = emptyMap() // ex: {"❤️": ["uid1", "uid2"], "🔥": ["uid3"]}
)
