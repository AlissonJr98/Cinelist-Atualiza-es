package com.example.cinelist

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MidiaRepository @Inject constructor(
    private val midiaDao: MidiaDao
) {
    val todasAsMidias: Flow<List<Midia>> = midiaDao.buscarTodasAsMidias()

    suspend fun inserir(midia: Midia) = midiaDao.inserirMidia(midia)
    suspend fun atualizar(midia: Midia) = midiaDao.atualizarMidia(midia)
    suspend fun deletar(midia: Midia) = midiaDao.deletarMidia(midia)

    fun buscarNoTmdbPaginado(
        query: String,
        tipo: String,
        provedorId: Int?,
        generoId: Int?,
        sortBy: String
    ): Flow<PagingData<TmdbFilme>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                TmdbPagingSource(
                    apiService = RetrofitClient.apiService,
                    query = query,
                    tipo = tipo,
                    provedorId = provedorId,
                    generoId = generoId,
                    sortBy = sortBy
                )
            }
        ).flow
    }

    suspend fun buscarNoTmdb(nome: String, tipo: String): List<TmdbFilme> {
        return if (tipo.equals("Série", ignoreCase = true) || tipo.equals("Anime", ignoreCase = true)) {
            RetrofitClient.apiService.buscarSerieOuAnime(nome).resultados.map { it.copy(mediaType = "tv") }
        } else if (tipo.equals("Filme", ignoreCase = true)) {
            RetrofitClient.apiService.buscarFilme(nome).resultados.map { it.copy(mediaType = "movie") }
        } else {
            RetrofitClient.apiService.buscarMulti(nome).resultados.filter {
                it.mediaType.equals("movie", ignoreCase = true) || it.mediaType.equals("tv", ignoreCase = true)
            }
        }
    }
}