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
    val id: Int
        get() = idTmdb

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

data class TmdbTemporadaItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("season_number") val numeroTemporada: Int = 1,
    @SerializedName("name") val nome: String = "",
    @SerializedName("overview") val sinopse: String = "",
    @SerializedName("episode_count") val quantidadeEpisodios: Int = 0,
    @SerializedName("air_date") val dataExibicao: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null
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
    @SerializedName("in_production") val emProducao: Boolean? = null,
    @SerializedName("budget") val orcamento: Long? = 0L,
    @SerializedName("revenue") val bilheteria: Long? = 0L,
    @SerializedName("production_companies") val produtoras: List<TmdbProdutora>? = emptyList(),
    @SerializedName("networks") val emissoras: List<TmdbProdutora>? = emptyList(),
    @SerializedName("created_by") val criadoresSerie: List<TmdbCriadorItem>? = emptyList(),
    @SerializedName("seasons") val temporadas: List<TmdbTemporadaItem>? = emptyList()
) {
    val totalTemporadasReais: Int
        get() {
            val maxSeason = temporadas?.filter { it.numeroTemporada > 0 }?.maxOfOrNull { it.numeroTemporada } ?: 0
            return maxOf(totalTemporadas, maxSeason)
        }

    fun obterNomeTemporada(temp: Int): String? {
        return temporadas?.firstOrNull { it.numeroTemporada == temp }?.nome?.takeIf { it.isNotBlank() }
    }

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

    val orcamentoFormatado: String?
        get() {
            val valor = orcamento ?: 0L
            if (valor <= 0L) return null
            return when {
                valor >= 1_000_000_000L -> "$${String.format(java.util.Locale.US, "%.1f", valor / 1_000_000_000.0)}B"
                valor >= 1_000_000L -> "$${String.format(java.util.Locale.US, "%.1f", valor / 1_000_000.0)}M"
                valor >= 1_000L -> "$${String.format(java.util.Locale.US, "%.1f", valor / 1_000.0)}K"
                else -> "$$valor"
            }
        }

    val bilheteriaFormatada: String?
        get() {
            val valor = bilheteria ?: 0L
            if (valor <= 0L) return null
            return when {
                valor >= 1_000_000_000L -> "$${String.format(java.util.Locale.US, "%.2f", valor / 1_000_000_000.0)}B"
                valor >= 1_000_000L -> "$${String.format(java.util.Locale.US, "%.1f", valor / 1_000_000.0)}M"
                valor >= 1_000L -> "$${String.format(java.util.Locale.US, "%.1f", valor / 1_000.0)}K"
                else -> "$$valor"
            }
        }

    val multiplicadorBilheteria: Double?
        get() {
            val o = orcamento ?: 0L
            val b = bilheteria ?: 0L
            if (o <= 0L || b <= 0L) return null
            return b.toDouble() / o.toDouble()
        }
}

data class TmdbProvedoresResposta(
    @SerializedName("results") val resultados: Map<String, DetalhesPaisProvedor>? = null
)

data class DetalhesPaisProvedor(
    @SerializedName("flatrate") val streamingAssinatura: List<ItemProvedor>? = null,
    @SerializedName("free") val streamingGratis: List<ItemProvedor>? = null,
    @SerializedName("ads") val streamingComAnuncios: List<ItemProvedor>? = null,
    @SerializedName("rent") val aluguel: List<ItemProvedor>? = null,
    @SerializedName("buy") val compra: List<ItemProvedor>? = null
)

data class ItemProvedor(
    @SerializedName("provider_id") val idProvedor: Int = 0,
    @SerializedName("provider_name") val nomeProvedor: String = "",
    @SerializedName("logo_path") val logoPath: String? = null
) {
    val urlLogo: String
        get() = if (!logoPath.isNullOrBlank()) "https://image.tmdb.org/t/p/w200$logoPath" else ""
}

data class TmdbProdutora(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val nome: String = "",
    @SerializedName("logo_path") val logoPath: String? = null,
    @SerializedName("origin_country") val paisOrigem: String? = null
) {
    val urlLogo: String
        get() = if (!logoPath.isNullOrBlank()) "https://image.tmdb.org/t/p/w200$logoPath" else ""
}

data class TmdbCriadorItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val nome: String = "",
    @SerializedName("profile_path") val fotoPerfil: String? = null
)

data class TmdbCreditosResposta(
    @SerializedName("cast") val elenco: List<TmdbAtor>? = null,
    @SerializedName("crew") val equipe: List<TmdbMembroEquipe>? = null
)

data class TmdbMembroEquipe(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val nome: String = "",
    @SerializedName("job") val funcao: String = "",
    @SerializedName("department") val departamento: String = "",
    @SerializedName("profile_path") val fotoPerfil: String? = null
) {
    val urlFotoPerfil: String
        get() = if (!fotoPerfil.isNullOrBlank()) "https://image.tmdb.org/t/p/w185$fotoPerfil" else ""
}

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
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val nome: String = "",
    @SerializedName("key") val chaveYoutube: String = "",
    @SerializedName("site") val sitePlataforma: String = "",
    @SerializedName("type") val tipoVideo: String = "",
    @SerializedName("official") val oficial: Boolean = false
) {
    val ehYoutube: Boolean
        get() = sitePlataforma.equals("YouTube", ignoreCase = true)
    val urlThumbnail: String
        get() = if (chaveYoutube.isNotBlank()) "https://img.youtube.com/vi/$chaveYoutube/mqdefault.jpg" else ""
}

data class TmdbReleaseDatesResposta(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("results") val resultados: List<TmdbPaisReleaseDate>? = null
)

data class TmdbPaisReleaseDate(
    @SerializedName("iso_3166_1") val codigoPais: String = "",
    @SerializedName("release_dates") val datasLancamento: List<TmdbReleaseDateItem>? = null
)

data class TmdbReleaseDateItem(
    @SerializedName("certification") val classificacao: String = "",
    @SerializedName("descriptors") val descritores: List<String>? = null,
    @SerializedName("type") val tipoLancamento: Int = 0
)

data class TmdbContentRatingsResposta(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("results") val resultados: List<TmdbRatingSerieItem>? = null
)

data class TmdbRatingSerieItem(
    @SerializedName("iso_3166_1") val codigoPais: String = "",
    @SerializedName("rating") val classificacao: String = "",
    @SerializedName("descriptors") val descritores: List<String>? = null
)

data class ClassificacaoIndicativaInfo(
    val faixaEtaria: String,
    val corFundo: Long,
    val descricao: String,
    val descritores: List<String> = emptyList()
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
    @SerializedName("vote_average") val avaliacao: Double = 0.0,
    @SerializedName("runtime") val duracaoMinutos: Int? = null
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

data class TmdbGruposEpisodiosResposta(
    @SerializedName("results") val resultados: List<TmdbGrupoEpisodioSumario>? = emptyList()
)

data class TmdbGrupoEpisodioSumario(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val nome: String = "",
    @SerializedName("order") val ordem: Int = 0,
    @SerializedName("group_count") val quantidadeGrupos: Int = 0,
    @SerializedName("type") val tipo: Int = 1
)

data class TmdbGrupoDetalhesResposta(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val nome: String = "",
    @SerializedName("groups") val grupos: List<TmdbSubgrupoEpisodio> = emptyList()
)

data class TmdbSubgrupoEpisodio(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val nome: String = "",
    @SerializedName("order") val ordem: Int = 0,
    @SerializedName("episodes") val episodios: List<TmdbEpisodioItem> = emptyList()
)

