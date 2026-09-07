package com.example.cinelist

import com.google.gson.annotations.SerializedName

data class TmdbResposta(
    @SerializedName("results") val resultados: List<TmdbFilme>?
)

data class TmdbDetalhesEstendidos(
    @SerializedName("title") private val _tituloFilme: String?,
    @SerializedName("name") private val _nomeSerie: String?,
    @SerializedName("runtime") val duracaoFilme: Int?,
    @SerializedName("episode_run_time") val duracaoEpisodios: List<Int>?,
    @SerializedName("vote_average") val notaCritica: Double,
    @SerializedName("release_date") private val _dataFilme: String?,
    @SerializedName("first_air_date") private val _dataSerie: String?,
    @SerializedName("backdrop_path") val caminhoBackdrop: String?,
    @SerializedName("tagline") val fraseEfeito: String?,
    @SerializedName("poster_path") val caminhoPosterVertical: String?,
    @SerializedName("overview") val sinopseApi: String?
) {
    val titulo: String
        get() = _tituloFilme ?: _nomeSerie ?: "Detalhes"

    val anoLancamento: String
        get() {
            val data = _dataFilme ?: _dataSerie ?: ""
            return if (data.length >= 4) data.substring(0, 4) else "N/A"
        }

    val duracaoMinutos: Int
        get() = duracaoFilme ?: duracaoEpisodios?.firstOrNull() ?: 0

    val urlBackdrop: String
        get() = if (!caminhoBackdrop.isNullOrBlank()) "https://image.tmdb.org/t/p/w780$caminhoBackdrop" else ""

    val urlPosterVertical: String
        get() = if (!caminhoPosterVertical.isNullOrBlank()) "https://image.tmdb.org/t/p/w500$caminhoPosterVertical" else ""
}

data class TmdbFilme(
    @SerializedName("id") val idTmdb: Int,
    @SerializedName("title") private val _tituloFilme: String?,
    @SerializedName("name") private val _nomeSerie: String?,
    @SerializedName("overview") val sinopse: String,
    @SerializedName("poster_path") val caminhoPoster: String?,
    @SerializedName("genre_ids") val listaGenerosIds: List<Int>?
) {
    val titulo: String
        get() = _tituloFilme ?: _nomeSerie ?: "Título Desconhecido"

    val generoTexto: String
        get() {
            val primeiroId = listaGenerosIds?.firstOrNull() ?: return "Geral"
            return when (primeiroId) {
                28 -> "Ação"
                12 -> "Aventura"
                16 -> "Animação"
                35 -> "Comédia"
                80 -> "Crime"
                99 -> "Documentário"
                18 -> "Drama"
                10751 -> "Família"
                14 -> "Fantasia"
                36 -> "História"
                27 -> "Terror"
                10402 -> "Música"
                9648 -> "Mistério"
                10749 -> "Romance"
                878 -> "Ficção"
                10770 -> "Cinema TV"
                53 -> "Suspense"
                10752 -> "Guerra"
                37 -> "Faroeste"
                10759 -> "Ação e Aventura"
                10762 -> "Kids"
                10765 -> "Sci-Fi & Fantasy"
                else -> "Outros"
            }
        }
}

data class TmdbProvedoresResposta(
    @SerializedName("results") val resultados: Map<String, ProvedoresPorPais>?
)

data class ProvedoresPorPais(
    @SerializedName("flatrate") val streamingAssinatura: List<ItemProvedor>?
)

data class ItemProvedor(
    val idProvedor: Int,
    @SerializedName("provider_id") val providerId: Int,
    @SerializedName("provider_name") val nomeProvedor: String,
    @SerializedName("logo_path") val caminhoLogo: String?
) {
    val urlLogo: String
        get() = if (!caminhoLogo.isNullOrBlank()) "https://image.tmdb.org/t/p/w200$caminhoLogo" else ""
}

data class TmdbCreditosResposta(
    @SerializedName("cast") val elenco: List<TmdbAtor>?
)

data class TmdbAtor(
    @SerializedName("id") val idAtor: Int,
    @SerializedName("name") val nomeReal: String,
    @SerializedName("character") val nomePersonagem: String,
    @SerializedName("profile_path") val caminhoPerfil: String?
) {
    val urlFotoPerfil: String
        get() = if (!caminhoPerfil.isNullOrBlank()) "https://image.tmdb.org/t/p/w185$caminhoPerfil" else ""
}

data class TmdbVideosResposta(
    @SerializedName("results") val videos: List<TmdbVideo>?
)

data class TmdbVideo(
    @SerializedName("key") val chaveYoutube: String,
    @SerializedName("type") val tipoVideo: String,
    @SerializedName("site") val sitePlataforma: String
)

data class TmdbRecomendacoesResposta(
    @SerializedName("results") val recomendacoes: List<TmdbFilme>?
)