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
    @SerializedName("vote_average") val mediaVotos: Double = 0.0,
    @SerializedName(value = "release_date", alternate = ["first_air_date"]) val dataLancamento: String? = null,
    val generoTexto: String = "Geral",
    var plataformaDetectada: String = ""
) {
    val ehSerie: Boolean
        get() = mediaType.equals("tv", ignoreCase = true)

    val anoLancamento: String
        get() = dataLancamento?.take(4)?.takeIf { it.isNotBlank() } ?: ""
}

data class TmdbProximoEpisodio(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val nome: String? = null,
    @SerializedName("air_date") val dataExibicao: String? = null,
    @SerializedName("episode_number") val numeroEpisodio: Int = 0,
    @SerializedName("season_number") val numeroTemporada: Int = 0,
    @SerializedName("overview") val sinopse: String? = null
)

data class TmdbGenero(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val nome: String = ""
)

data class TmdbFranquiaColecao(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val nome: String = "",
    @SerializedName("poster_path") val caminhoPoster: String? = null,
    @SerializedName("backdrop_path") val caminhoBackdrop: String? = null
)

data class TmdbFranquiaColecaoDetalhes(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val nome: String = "",
    @SerializedName("overview") val sinopse: String = "",
    @SerializedName("poster_path") val caminhoPoster: String? = null,
    @SerializedName("backdrop_path") val caminhoBackdrop: String? = null,
    @SerializedName("parts") val partes: List<TmdbFilme>? = emptyList()
)

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
    @SerializedName("tagline") val fraseEfeito: String? = null,
    @SerializedName("next_episode_to_air") val proximoEpisodio: TmdbProximoEpisodio? = null,
    @SerializedName("genres") val generos: List<TmdbGenero>? = emptyList(),
    @SerializedName("belongs_to_collection") val colecaoFranquia: TmdbFranquiaColecao? = null,
    @SerializedName("status") val statusSerieApi: String? = null,
    @SerializedName("in_production") val emProducao: Boolean? = null
) {
    val statusSeloTexto: String
        get() = when {
            statusSerieApi.equals("Canceled", ignoreCase = true) -> "⚠️ Esta série foi cancelada"
            statusSerieApi.equals("Ended", ignoreCase = true) -> "✅ Série finalizada (Todas temporadas disponíveis)"
            statusSerieApi.equals("Returning Series", ignoreCase = true) -> "📺 Em exibição (Novas temporadas em produção)"
            else -> ""
        }
    val generoTexto: String
        get() = generos?.map { it.nome }?.filter { it.isNotBlank() }?.joinToString(", ")?.ifBlank { "Geral" } ?: "Geral"

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

data class TmdbTemporadaDetalhes(
    @SerializedName("season_number") val numeroTemporada: Int = 1,
    @SerializedName("name") val nome: String = "",
    @SerializedName("episodes") val episodios: List<TmdbEpisodioItem> = emptyList()
)

data class TmdbEpisodioItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("episode_number") val numeroEpisodio: Int = 1,
    @SerializedName("name") val nome: String = "",
    @SerializedName("overview") val sinopse: String = "",
    @SerializedName("still_path") val caminhoImagem: String? = null,
    @SerializedName("vote_average") val avaliacao: Double = 0.0
) {
    val urlImagemHorizontal: String
        get() = if (!caminhoImagem.isNullOrBlank()) "https://image.tmdb.org/t/p/w300$caminhoImagem" else ""
}

data class TmdbImagensResposta(
    @SerializedName("backdrops") val backdrops: List<TmdbImagemItem>? = emptyList(),
    @SerializedName("posters") val posters: List<TmdbImagemItem>? = emptyList()
)

data class TmdbImagemItem(
    @SerializedName("file_path") val caminhoArquivo: String = "",
    @SerializedName("vote_average") val mediaVotos: Double = 0.0,
    @SerializedName("width") val largura: Int = 0,
    @SerializedName("height") val altura: Int = 0
) {
    val urlMiniatura: String
        get() = if (caminhoArquivo.isNotBlank()) "https://image.tmdb.org/t/p/w500$caminhoArquivo" else ""

    val urlOriginal: String
        get() = if (caminhoArquivo.isNotBlank()) "https://image.tmdb.org/t/p/original$caminhoArquivo" else ""
}

data class TmdbKeywordsResposta(
    @SerializedName("keywords") val keywords: List<TmdbKeywordItem>? = emptyList()
)

data class TmdbKeywordItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val nome: String = ""
)

data class TmdbPessoaDetalhes(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val nome: String = "",
    @SerializedName("biography") val biografia: String = "",
    @SerializedName("birthday") val dataNascimento: String? = null,
    @SerializedName("place_of_birth") val localNascimento: String? = null,
    @SerializedName("profile_path") val fotoPerfil: String? = null,
    @SerializedName("known_for_department") val departamento: String? = null
) {
    val urlFotoPerfil: String
        get() = if (!fotoPerfil.isNullOrBlank()) "https://image.tmdb.org/t/p/w500$fotoPerfil" else ""

    val dataNascimentoFormatada: String
        get() {
            if (dataNascimento.isNullOrBlank()) return "Não informada"
            return try {
                val partes = dataNascimento.split("-")
                if (partes.size == 3) {
                    val meses = listOf("", "jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez.")
                    val mesIndex = partes[1].toIntOrNull() ?: 0
                    val mesNome = if (mesIndex in 1..12) meses[mesIndex] else partes[1]
                    "${partes[2]} de $mesNome de ${partes[0]}"
                } else dataNascimento
            } catch (e: Exception) {
                dataNascimento
            }
        }
}

data class TmdbPessoaCreditosResposta(
    @SerializedName("cast") val atuacoes: List<TmdbCreditoItem>? = emptyList()
)

data class TmdbCreditoItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName(value = "title", alternate = ["name"]) val titulo: String = "",
    @SerializedName("overview") val sinopse: String = "",
    @SerializedName("poster_path") val caminhoPoster: String? = null,
    @SerializedName("backdrop_path") val caminhoBackdrop: String? = null,
    @SerializedName("media_type") val mediaType: String? = "movie",
    @SerializedName("vote_average") val mediaVotos: Double = 0.0,
    @SerializedName(value = "release_date", alternate = ["first_air_date"]) val dataLancamento: String? = null,
    @SerializedName("character") val personagem: String? = null
) {
    val ehSerie: Boolean
        get() = mediaType.equals("tv", ignoreCase = true)

    val anoLancamento: String
        get() = dataLancamento?.take(4)?.takeIf { it.isNotBlank() } ?: ""

    val urlPoster: String
        get() = if (!caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500$caminhoPoster" else ""

    val urlBackdrop: String
        get() = if (!caminhoBackdrop.isNullOrBlank()) "https://image.tmdb.org/t/p/w780$caminhoBackdrop" else ""
}
