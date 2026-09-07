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

    suspend fun inserir(midia: Midia) {
        midiaDao.inserirMidia(midia)
    }

    suspend fun atualizar(midia: Midia) {
        midiaDao.atualizarMidia(midia)
    }

    suspend fun deletar(midia: Midia) {
        midiaDao.deletarMidia(midia)
    }

    suspend fun buscarNoTmdb(nome: String, tipo: String): List<TmdbFilme> {
        val ehSerieOuAnime = tipo.equals("Série", ignoreCase = true) || tipo.equals("Anime", ignoreCase = true)
        return if (ehSerieOuAnime) {
            RetrofitClient.apiService.buscarSerieOuAnime(nomeSerie = nome).resultados ?: emptyList()
        } else {
            RetrofitClient.apiService.buscarFilme(nomeFilme = nome).resultados ?: emptyList()
        }
    }

    fun buscarNoTmdbPaginado(
        query: String,
        tipo: String,
        provedorId: Int? = null,
        generoId: Int? = null,
        sortBy: String = "popularity.desc"
    ): Flow<PagingData<TmdbFilme>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
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
}