package com.example.cinelist

import androidx.paging.PagingSource
import androidx.paging.PagingState

class TmdbPagingSource(
    private val apiService: TmdbApiService,
    private val query: String,
    private val tipo: String,
    private val provedorId: Int?,
    private val generoId: Int?,
    private val sortBy: String
) : PagingSource<Int, TmdbFilme>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TmdbFilme> {
        val paginaAtual = params.key ?: 1
        return try {
            val listaResultado: List<TmdbFilme>
            val totalPaginas: Int

            if (query.isNotBlank()) {
                // PESQUISA POR NOME
                when {
                    tipo.equals("Filme", ignoreCase = true) -> {
                        val resp = apiService.buscarFilme(query, paginaAtual)
                        listaResultado = resp.resultados.map { it.copy(mediaType = "movie") }
                        totalPaginas = resp.totalPaginas
                    }
                    tipo.equals("Série", ignoreCase = true) || tipo.equals("Anime", ignoreCase = true) ||
                            tipo.equals("Novela", ignoreCase = true) || tipo.equals("Dorama", ignoreCase = true) -> {
                        val resp = apiService.buscarSerieOuAnime(query, paginaAtual)
                        listaResultado = resp.resultados.map { it.copy(mediaType = "tv") }
                        totalPaginas = resp.totalPaginas
                    }
                    else -> {
                        // "Todos": busca unificada multi-search ignorando pessoas (atores/diretores)
                        val resp = apiService.buscarMulti(query, paginaAtual)
                        listaResultado = resp.resultados.filter {
                            it.mediaType.equals("movie", ignoreCase = true) || it.mediaType.equals("tv", ignoreCase = true)
                        }
                        totalPaginas = resp.totalPaginas
                    }
                }
            } else {
                // DESCOBERTA SEM TEXTO
                when {
                    tipo.equals("Filme", ignoreCase = true) -> {
                        val resp = apiService.descobrirFilmes(
                            pagina = paginaAtual,
                            provedores = provedorId?.toString(),
                            generos = generoId?.toString(),
                            sortBy = sortBy
                        )
                        listaResultado = resp.resultados.map { it.copy(mediaType = "movie") }
                        totalPaginas = resp.totalPaginas
                    }
                    tipo.equals("Série", ignoreCase = true) -> {
                        val resp = apiService.descobrirSeries(
                            pagina = paginaAtual,
                            provedores = provedorId?.toString(),
                            generos = generoId?.toString(),
                            sortBy = sortBy
                        )
                        listaResultado = resp.resultados.map { it.copy(mediaType = "tv") }
                        totalPaginas = resp.totalPaginas
                    }
                    tipo.equals("Anime", ignoreCase = true) -> {
                        val resp = apiService.descobrirSeries(
                            pagina = paginaAtual,
                            provedores = provedorId?.toString(),
                            generos = generoId?.toString(),
                            idiomaOriginal = "ja",
                            sortBy = sortBy
                        )
                        listaResultado = resp.resultados.map { it.copy(mediaType = "tv") }
                        totalPaginas = resp.totalPaginas
                    }
                    tipo.equals("Novela", ignoreCase = true) -> {
                        val resp = apiService.descobrirSeries(
                            pagina = paginaAtual,
                            provedores = provedorId?.toString(),
                            generos = generoId?.toString(),
                            sortBy = sortBy
                        )
                        listaResultado = resp.resultados.map { it.copy(mediaType = "tv") }
                        totalPaginas = resp.totalPaginas
                    }
                    tipo.equals("Dorama", ignoreCase = true) -> {
                        val resp = apiService.descobrirSeries(
                            pagina = paginaAtual,
                            provedores = provedorId?.toString(),
                            generos = generoId?.toString(),
                            idiomaOriginal = "ko",
                            sortBy = sortBy
                        )
                        listaResultado = resp.resultados.map { it.copy(mediaType = "tv") }
                        totalPaginas = resp.totalPaginas
                    }
                    else -> {
                        // "Todos": combina filmes e séries da página para exibição mista
                        val filmesResp = apiService.descobrirFilmes(
                            pagina = paginaAtual,
                            provedores = provedorId?.toString(),
                            generos = generoId?.toString(),
                            sortBy = sortBy
                        )
                        val seriesResp = apiService.descobrirSeries(
                            pagina = paginaAtual,
                            provedores = provedorId?.toString(),
                            generos = generoId?.toString(),
                            sortBy = sortBy
                        )

                        val filmes = filmesResp.resultados.map { it.copy(mediaType = "movie") }
                        val series = seriesResp.resultados.map { it.copy(mediaType = "tv") }

                        listaResultado = filmes.zip(series) { f, s -> listOf(f, s) }.flatten() +
                                if (filmes.size > series.size) filmes.drop(series.size) else series.drop(filmes.size)

                        totalPaginas = maxOf(filmesResp.totalPaginas, seriesResp.totalPaginas)
                    }
                }
            }

            val proximaChave = if (paginaAtual < totalPaginas && listaResultado.isNotEmpty()) paginaAtual + 1 else null
            val chaveAnterior = if (paginaAtual > 1) paginaAtual - 1 else null

            LoadResult.Page(
                data = listaResultado,
                prevKey = chaveAnterior,
                nextKey = proximaChave
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TmdbFilme>): Int? {
        return state.anchorPosition?.let { pos ->
            val pagina = state.closestPageToPosition(pos)
            pagina?.prevKey?.plus(1) ?: pagina?.nextKey?.minus(1)
        }
    }
}