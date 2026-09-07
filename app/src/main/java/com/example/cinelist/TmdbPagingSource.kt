package com.example.cinelist

import androidx.paging.PagingSource
import androidx.paging.PagingState

class TmdbPagingSource(
    private val apiService: TmdbApiService,
    private val query: String,
    private val tipo: String,
    private val provedorId: Int? = null,
    private val generoId: Int? = null,
    private val sortBy: String = "popularity.desc"
) : PagingSource<Int, TmdbFilme>() {

    override fun getRefreshKey(state: PagingState<Int, TmdbFilme>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TmdbFilme> {
        val paginaAtual = params.key ?: 1

        return try {
            val ehFilme = tipo.equals("Filme", ignoreCase = true)

            val resposta = if (query.isNotBlank()) {
                if (ehFilme) {
                    apiService.buscarFilme(nomeFilme = query, pagina = paginaAtual)
                } else {
                    apiService.buscarSerieOuAnime(nomeSerie = query, pagina = paginaAtual)
                }
            } else {
                val provedorStr = provedorId?.toString()
                var generoFinal = generoId?.toString()
                var idiomaOriginal: String? = null

                when (tipo) {
                    "Anime" -> {
                        // Gênero 16 (Animação) + produções japonesas
                        generoFinal = if (generoId != null) "$generoId,16" else "16"
                        idiomaOriginal = "ja"
                    }
                    "Novela" -> {
                        // Gênero 10766 (Soap / Novela)
                        generoFinal = if (generoId != null) "$generoId,10766" else "10766"
                    }
                    "Dorama" -> {
                        // Produções da Coreia do Sul (K-Dramas)
                        idiomaOriginal = "ko"
                    }
                }

                if (ehFilme) {
                    apiService.descobrirFilmes(
                        pagina = paginaAtual,
                        provedores = provedorStr,
                        generos = generoFinal,
                        sortBy = sortBy
                    )
                } else {
                    apiService.descobrirSeries(
                        pagina = paginaAtual,
                        provedores = provedorStr,
                        generos = generoFinal,
                        idiomaOriginal = idiomaOriginal,
                        sortBy = sortBy
                    )
                }
            }

            val itens = resposta.resultados ?: emptyList()
            val proximaChave = if (itens.isEmpty()) null else paginaAtual + 1
            val chaveAnterior = if (paginaAtual == 1) null else paginaAtual - 1

            LoadResult.Page(
                data = itens,
                prevKey = chaveAnterior,
                nextKey = proximaChave
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}