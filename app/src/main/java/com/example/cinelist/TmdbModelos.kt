package com.example.cinelist

import com.google.gson.annotations.SerializedName

data class TmdbResposta(
    @SerializedName("page") val pagina: Int = 1,
    @SerializedName("results") val resultados: List<TmdbFilme> = emptyList(),
    @SerializedName("total_pages") val totalPaginas: Int = 1
)

data class TmdbFilme(
    @SerializedName("id") val idTmdb: Int = 0,
    @SerializedName(value = "title", alternate = ["name"]) val titulo: String = "",
    @SerializedName(value = "original_title", alternate = ["original_name"]) val tituloOriginal: String? = null,
    @SerializedName("overview") val sinopse: String = "",
    @SerializedName("poster_path") val caminhoPoster: String? = null,
    @SerializedName("backdrop_path") val caminhoBackdrop: String? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("genre_ids") val generosIds: List<Int>? = emptyList(),
    val generoTexto: String = "Geral"
) {
    val ehSerie: Boolean
        get() = mediaType.equals("tv", ignoreCase = true)
}

data class TmdbDetalhesEstendidos(
    @SerializedName("id") val id: Int = 0,
    @SerializedName(value = "title", alternate = ["name"]) val titulo: String = "",
    @SerializedName("overview") val sinopse: String = "",
    @SerializedName("poster_path") val caminhoPoster: String? = null,
    @SerializedName("backdrop_path") val caminhoBackdrop: String? = null,
    @SerializedName("vote_average") val mediaVotos: Double = 0.0,
    @SerializedName(value = "release_date", alternate = ["first_air_date"]) val dataLancamento: String? = null,
    @SerializedName("number_of_seasons") val totalTemporadas: Int = 1,
    @SerializedName("number_of_episodes") val totalEpisodios: Int = 1,
    @SerializedName("runtime") val duracaoFilme: Int? = null,
    @SerializedName("episode_run_time") val duracaoEpisodios: List<Int>? = null,
    @SerializedName("tagline") val fraseEfeito: String? = null
) {
    val sinopseApi: String
        get() = sinopse

    val notaCritica: Double
        get() = mediaVotos

    val urlPosterVertical: String
        get() = if (!caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500$caminhoPoster" else ""

    val urlBackdrop: String
        get() = if (!caminhoBackdrop.isNullOrBlank()) "https://image.tmdb.org/t/p/w780$caminhoBackdrop" else ""

    val anoLancamento: String
        get() {
            val data = dataLancamento ?: ""
            return if (data.length >= 4) data.substring(0, 4) else "N/A"
        }

    val duracaoMinutos: Int
        get() = duracaoFilme ?: duracaoEpisodios?.firstOrNull() ?: 0
}

data class TmdbProvedoresResposta(
    @SerializedName("results") val resultados: Map<String, DetalhesPaisProvedor>? = null
)

data class DetalhesPaisProvedor(
    @SerializedName("flatrate") val streamingAssinatura: List<ItemProvedor>? = null
)

data class ItemProvedor(
    @SerializedName("provider_id") val idProvedor: Int = 0,
    @SerializedName("provider_name") val nomeProvedor: String = "",
    @SerializedName("logo_path") val logoPath: String? = null
) {
    val urlLogo: String
        get() = if (!logoPath.isNullOrBlank()) "https://image.tmdb.org/t/p/w200$logoPath" else ""
}

data class TmdbCreditosResposta(
    @SerializedName("cast") val elenco: List<TmdbAtor>? = null
)

data class TmdbAtor(
    @SerializedName("id") val idAtor: Int = 0,
    @SerializedName("name") val nome: String = "",
    @SerializedName("character") val personagem: String = "",
    @SerializedName("profile_path") val fotoPerfil: String? = null
) {
    val nomeReal: String
        get() = nome

    val nomePersonagem: String
        get() = personagem

    val urlFotoPerfil: String
        get() = if (!fotoPerfil.isNullOrBlank()) "https://image.tmdb.org/t/p/w185$fotoPerfil" else ""
}

data class TmdbVideosResposta(
    @SerializedName("results") val videos: List<TmdbVideoItem>? = null
)

data class TmdbVideoItem(
    @SerializedName("key") val chaveYoutube: String = "",
    @SerializedName("site") val sitePlataforma: String = "",
    @SerializedName("type") val tipoVideo: String = ""
)

data class TmdbRecomendacoesResposta(
    @SerializedName("results") val recomendacoes: List<TmdbFilme>? = null
)
