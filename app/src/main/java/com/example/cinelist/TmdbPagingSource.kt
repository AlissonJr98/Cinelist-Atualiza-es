package com.example.cinelist

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
            val listaBruta: List<TmdbFilme>
            val totalPaginas: Int

            if (query.isNotBlank()) {
                when {
                    tipo.equals("Filme", ignoreCase = true) -> {
                        val resp = apiService.buscarFilme(query, paginaAtual)
                        listaBruta = resp.resultados.map { it.copy(mediaType = "movie") }
                        totalPaginas = resp.totalPaginas
                    }
                    tipo.equals("Série", ignoreCase = true) || tipo.equals("Anime", ignoreCase = true) ||
                            tipo.equals("Novela", ignoreCase = true) || tipo.equals("Dorama", ignoreCase = true) -> {
                        val resp = apiService.buscarSerieOuAnime(query, paginaAtual)
                        listaBruta = resp.resultados.map { it.copy(mediaType = "tv") }
                        totalPaginas = resp.totalPaginas
                    }
                    else -> {
                        val resp = apiService.buscarMulti(query, paginaAtual)
                        listaBruta = resp.resultados.filter {
                            it.mediaType.equals("movie", ignoreCase = true) || it.mediaType.equals("tv", ignoreCase = true)
                        }
                        totalPaginas = resp.totalPaginas
                    }
                }
            } else {
                // Ajusta a ordenação de data para o padrão correto do TMDB dependendo se é filme ou série
                val sortFilme = if (sortBy == "primary_release_date.desc" || sortBy.contains("date", ignoreCase = true)) "primary_release_date.desc" else sortBy
                val sortSerie = if (sortBy == "primary_release_date.desc" || sortBy.contains("date", ignoreCase = true)) "first_air_date.desc" else sortBy

                when {
                    tipo.equals("Filme", ignoreCase = true) -> {
                        val resp = apiService.descobrirFilmes(pagina = paginaAtual, provedores = provedorId?.toString(), generos = generoId?.toString(), sortBy = sortFilme)
                        listaBruta = resp.resultados.map { it.copy(mediaType = "movie") }
                        totalPaginas = resp.totalPaginas
                    }
                    tipo.equals("Série", ignoreCase = true) -> {
                        val resp = apiService.descobrirSeries(pagina = paginaAtual, provedores = provedorId?.toString(), generos = generoId?.toString(), sortBy = sortSerie)
                        listaBruta = resp.resultados.map { it.copy(mediaType = "tv") }
                        totalPaginas = resp.totalPaginas
                    }
                    tipo.equals("Anime", ignoreCase = true) -> {
                        // Animes usam ID de animação (16) e idioma original japonês (ja)
                        val genFinal = if (generoId != null) "$generoId,16" else "16"
                        val resp = apiService.descobrirSeries(pagina = paginaAtual, provedores = provedorId?.toString(), generos = genFinal, idiomaOriginal = "ja", sortBy = sortSerie)
                        listaBruta = resp.resultados.map { it.copy(mediaType = "tv") }
                        totalPaginas = resp.totalPaginas
                    }
                    tipo.equals("Novela", ignoreCase = true) -> {
                        // Novelas no TMDB correspondem ao gênero de Soap Opera (10766)
                        val genFinal = if (generoId != null) "$generoId,10766" else "10766"
                        val resp = apiService.descobrirSeries(pagina = paginaAtual, provedores = provedorId?.toString(), generos = genFinal, sortBy = sortSerie)
                        listaBruta = resp.resultados.map { it.copy(mediaType = "tv") }
                        totalPaginas = resp.totalPaginas
                    }
                    tipo.equals("Dorama", ignoreCase = true) -> {
                        // Doramas usam idioma original coreano (ko)
                        val resp = apiService.descobrirSeries(pagina = paginaAtual, provedores = provedorId?.toString(), generos = generoId?.toString(), idiomaOriginal = "ko", sortBy = sortSerie)
                        listaBruta = resp.resultados.map { it.copy(mediaType = "tv") }
                        totalPaginas = resp.totalPaginas
                    }
                    else -> {
                        val filmesResp = apiService.descobrirFilmes(pagina = paginaAtual, provedores = provedorId?.toString(), generos = generoId?.toString(), sortBy = sortFilme)
                        val seriesResp = apiService.descobrirSeries(pagina = paginaAtual, provedores = provedorId?.toString(), generos = generoId?.toString(), sortBy = sortSerie)

                        val filmes = filmesResp.resultados.map { it.copy(mediaType = "movie") }
                        val series = seriesResp.resultados.map { it.copy(mediaType = "tv") }

                        listaBruta = filmes.zip(series) { f, s -> listOf(f, s) }.flatten() +
                                if (filmes.size > series.size) filmes.drop(series.size) else series.drop(series.size)

                        totalPaginas = maxOf(filmesResp.totalPaginas, seriesResp.totalPaginas)
                    }
                }
            }

            // Consulta os provedores reais em paralelo para cada item da página atual
            val listaResultado = coroutineScope {
                listaBruta.map { filme ->
                    async {
                        try {
                            val ehSerie = filme.mediaType.equals("tv", ignoreCase = true) || filme.ehSerie
                            val resposta = if (ehSerie) {
                                apiService.obterProvedoresSerieOuAnime(idSerie = filme.idTmdb)
                            } else {
                                apiService.obterProvedoresFilme(idFilme = filme.idTmdb)
                            }

                            val providerBr = resposta.resultados?.get("BR")?.streamingAssinatura ?: emptyList()
                            val primeiroProvedor = providerBr.firstOrNull()?.nomeProvedor

                            val plataformaReal = when {
                                !primeiroProvedor.isNullOrBlank() -> primeiroProvedor
                                ehSerie -> "TV / Original"
                                else -> "Cinema"
                            }

                            val tituloLower = filme.titulo.lowercase()
                            val plataformaFinal = when {
                                tituloLower.contains("reacher") || tituloLower.contains("the boys") || tituloLower.contains("invincible") || tituloLower.contains("rings of power") -> "Prime Video"
                                tituloLower.contains("stranger things") || tituloLower.contains("squid game") || tituloLower.contains("wednesday") -> "Netflix"
                                else -> plataformaReal
                            }

                            filme.apply {
                                plataformaDetectada = plataformaFinal
                            }
                        } catch (e: Exception) {
                            filme
                        }
                    }
                }.awaitAll()
            }.distinctBy { "${it.mediaType ?: "midia"}_${it.idTmdb}" }

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