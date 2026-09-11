package com.example.cinelist

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("search/multi")
    suspend fun buscarMulti(
        @Query("query") query: String,
        @Query("page") pagina: Int = 1,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbResposta

    @GET("search/movie")
    suspend fun buscarFilme(
        @Query("query") nomeFilme: String,
        @Query("page") pagina: Int = 1,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbResposta

    @GET("search/tv")
    suspend fun buscarSerieOuAnime(
        @Query("query") nomeSerie: String,
        @Query("page") pagina: Int = 1,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbResposta

    @GET("discover/movie")
    suspend fun descobrirFilmes(
        @Query("page") pagina: Int = 1,
        @Query("with_watch_providers") provedores: String? = null,
        @Query("watch_region") regiao: String = "BR",
        @Query("with_genres") generos: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("language") idioma: String = "pt-BR"
    ): TmdbResposta

    @GET("discover/tv")
    suspend fun descobrirSeries(
        @Query("page") pagina: Int = 1,
        @Query("with_watch_providers") provedores: String? = null,
        @Query("watch_region") regiao: String = "BR",
        @Query("with_genres") generos: String? = null,
        @Query("with_original_language") idiomaOriginal: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("language") idioma: String = "pt-BR"
    ): TmdbResposta

    @GET("movie/{movie_id}")
    suspend fun obterDetalhesFilme(
        @Path("movie_id") idFilme: Int,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbDetalhesEstendidos

    @GET("tv/{series_id}")
    suspend fun obterDetalhesSerieOuAnime(
        @Path("series_id") idSerie: Int,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbDetalhesEstendidos

    @GET("tv/{series_id}/season/{season_number}")
    suspend fun obterEpisodiosTemporada(
        @Path("series_id") idSerie: Int,
        @Path("season_number") numeroTemporada: Int,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbTemporadaDetalhes

    @GET("movie/{movie_id}/watch/providers")
    suspend fun obterProvedoresFilme(
        @Path("movie_id") idFilme: Int
    ): TmdbProvedoresResposta

    @GET("tv/{series_id}/watch/providers")
    suspend fun obterProvedoresSerieOuAnime(
        @Path("series_id") idSerie: Int
    ): TmdbProvedoresResposta

    @GET("movie/{movie_id}/credits")
    suspend fun obterCreditosFilme(
        @Path("movie_id") idFilme: Int,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbCreditosResposta

    @GET("tv/{series_id}/credits")
    suspend fun obterCreditosSerieOuAnime(
        @Path("series_id") idSerie: Int,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbCreditosResposta

    @GET("movie/{movie_id}/videos")
    suspend fun obterVideosFilme(
        @Path("movie_id") idFilme: Int,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbVideosResposta

    @GET("tv/{series_id}/videos")
    suspend fun obterVideosSerieOuAnime(
        @Path("series_id") idSerie: Int,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbVideosResposta

    @GET("movie/{movie_id}/recommendations")
    suspend fun obterRecomendacoesFilme(
        @Path("movie_id") idFilme: Int,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbRecomendacoesResposta

    @GET("tv/{series_id}/recommendations")
    suspend fun obterRecomendacoesSerieOuAnime(
        @Path("series_id") idSerie: Int,
        @Query("language") idioma: String = "pt-BR"
    ): TmdbRecomendacoesResposta

    @GET("movie/{movie_id}/images")
    suspend fun obterImagensFilme(
        @Path("movie_id") idFilme: Int,
        @Query("include_image_language") idiomas: String = "pt,en,null"
    ): TmdbImagensResposta

    @GET("tv/{series_id}/images")
    suspend fun obterImagensSerieOuAnime(
        @Path("series_id") idSerie: Int,
        @Query("include_image_language") idiomas: String = "pt,en,null"
    ): TmdbImagensResposta
}