package com.example.cinelist

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MidiaRepository @Inject constructor(
    private val midiaDao: MidiaDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val todasAsMidias: Flow<List<Midia>> = midiaDao.buscarTodasAsMidias()

    private fun obterColecaoUsuario(): com.google.firebase.firestore.CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("usuarios").document(uid).collection("midias")
    }

    suspend fun inserir(midia: Midia) {
        val idGerado = midiaDao.inserirMidia(midia)
        val midiaComId = if (midia.id == 0) midia.copy(id = idGerado.toInt()) else midia
        sincronizarItemIndividualFirestore(midiaComId)
    }

    suspend fun atualizar(midia: Midia) {
        midiaDao.atualizarMidia(midia)
        sincronizarItemIndividualFirestore(midia)
    }

    suspend fun incrementarEpisodio(idMidia: Int) {
        midiaDao.incrementarEpisodio(idMidia)
        val midiaAtualizada = midiaDao.buscarPorId(idMidia)
        midiaAtualizada?.let { sincronizarItemIndividualFirestore(it) }
    }

    suspend fun atualizarProgressoEpisodio(idMidia: Int, temporada: Int, episodio: Int) {
        midiaDao.atualizarProgressoEpisodio(idMidia, temporada, episodio)
        val midiaAtualizada = midiaDao.buscarPorId(idMidia)
        midiaAtualizada?.let { sincronizarItemIndividualFirestore(it) }
    }

    private suspend fun sincronizarItemIndividualFirestore(midia: Midia) = withContext(Dispatchers.IO) {
        try {
            val colecao = obterColecaoUsuario() ?: return@withContext
            val chaveDoc = if (midia.id != 0) midia.id.toString() else "tmdb_${midia.idTmdb}"
            colecao.document(chaveDoc).set(midia.toMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun removerItemFirestore(midia: Midia) = withContext(Dispatchers.IO) {
        try {
            val colecao = obterColecaoUsuario() ?: return@withContext

            // 1. Apaga pelos IDs diretos conhecidos
            if (midia.id != 0) {
                colecao.document(midia.id.toString()).delete().await()
            }
            if (midia.idTmdb != 0) {
                colecao.document("tmdb_${midia.idTmdb}").delete().await()
            }

            // 2. Busca por segurança se o documento foi salvo com idTmdb ou título correspondente
            val docsPorTmdb = if (midia.idTmdb != 0) {
                colecao.whereEqualTo("idTmdb", midia.idTmdb).get().await()
            } else null
            docsPorTmdb?.documents?.forEach { doc -> doc.reference.delete().await() }

            if (midia.titulo.isNotBlank()) {
                val docsPorTitulo = colecao.whereEqualTo("titulo", midia.titulo).get().await()
                docsPorTitulo.documents.forEach { doc -> doc.reference.delete().await() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletar(midia: Midia) {
        midiaDao.deletarMidia(midia)
        removerItemFirestore(midia)
    }

    suspend fun limparTudoCompleto() = withContext(Dispatchers.IO) {
        try {
            // 1. Apaga tudo do Firestore em lote
            val colecao = obterColecaoUsuario()
            if (colecao != null) {
                val snapshot = colecao.get().await()
                if (!snapshot.isEmpty) {
                    val batch = firestore.batch()
                    snapshot.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                }
            }

            // 2. Apaga tudo do Room local
            val locais = midiaDao.buscarTodasAsMidias().firstOrNull() ?: emptyList()
            locais.forEach { midiaDao.deletarMidia(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sincronizacaoAutomaticaSilenciosa() = withContext(Dispatchers.IO) {
        try {
            val colecao = obterColecaoUsuario() ?: return@withContext
            val snapshot = colecao.get().await()

            val midiasNuvem = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Midia::class.java) ?: Midia(
                        id = doc.getLong("id")?.toInt() ?: 0,
                        idTmdb = doc.getLong("idTmdb")?.toInt() ?: 0,
                        titulo = doc.getString("titulo") ?: "",
                        tipo = doc.getString("tipo") ?: "Filme",
                        status = doc.getString("status") ?: "Quero Assistir",
                        nota = doc.getLong("nota")?.toInt() ?: 0,
                        temporadaAtual = doc.getLong("temporadaAtual")?.toInt() ?: 1,
                        episodioAtual = doc.getLong("episodioAtual")?.toInt() ?: 1,
                        minutoParado = doc.getLong("minutoParado")?.toInt() ?: 0,
                        jaEncerrou = doc.getBoolean("jaEncerrou") ?: false,
                        sinopse = doc.getString("sinopse") ?: "",
                        imagemCapa = doc.getString("imagemCapa") ?: "",
                        genero = doc.getString("genero") ?: "Não Informado",
                        duracaoTotal = doc.getLong("duracaoTotal")?.toInt() ?: 0,
                        plataforma = doc.getString("plataforma") ?: "Outros"
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val midiasLocais = midiaDao.buscarTodasAsMidias().firstOrNull() ?: emptyList()

            // 1. Nuvem -> Local
            midiasNuvem.forEach { midiaNuvem ->
                val jaExisteLocal = midiasLocais.any {
                    (it.idTmdb != 0 && it.idTmdb == midiaNuvem.idTmdb) ||
                            it.titulo.equals(midiaNuvem.titulo, ignoreCase = true)
                }
                if (!jaExisteLocal) {
                    midiaDao.inserirMidia(midiaNuvem.copy(id = 0))
                }
            }

            // 2. Local -> Nuvem
            val batch = firestore.batch()
            var temItensBatch = false
            midiasLocais.forEach { midiaLocal ->
                val chaveDoc = if (midiaLocal.id != 0) midiaLocal.id.toString() else "tmdb_${midiaLocal.idTmdb}"
                val jaEstaNaNuvem = midiasNuvem.any { it.id == midiaLocal.id || (it.idTmdb != 0 && it.idTmdb == midiaLocal.idTmdb) }
                if (!jaEstaNaNuvem) {
                    val ref = colecao.document(chaveDoc)
                    batch.set(ref, midiaLocal.toMap(), SetOptions.merge())
                    temItensBatch = true
                }
            }

            if (temItensBatch) {
                batch.commit().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restaurarDoFirestore(): Int = withContext(Dispatchers.IO) {
        try {
            val colecao = obterColecaoUsuario() ?: return@withContext 0
            val snapshot = colecao.get().await()
            val midiasNuvem = snapshot.toObjects(Midia::class.java)
            val midiasLocais = midiaDao.buscarTodasAsMidias().firstOrNull() ?: emptyList()

            var importadas = 0
            midiasNuvem.forEach { midiaNuvem ->
                val jaExiste = midiasLocais.any {
                    (it.idTmdb != 0 && it.idTmdb == midiaNuvem.idTmdb) ||
                            it.titulo.equals(midiaNuvem.titulo, ignoreCase = true)
                }

                if (!jaExiste) {
                    midiaDao.inserirMidia(midiaNuvem.copy(id = 0))
                    importadas++
                }
            }
            importadas
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun backupCompletoParaFirestore(): Boolean = withContext(Dispatchers.IO) {
        try {
            val colecao = obterColecaoUsuario() ?: return@withContext false
            val midiasLocais = midiaDao.buscarTodasAsMidias().firstOrNull() ?: emptyList()

            val batch = firestore.batch()
            midiasLocais.forEach { midia ->
                val chaveDoc = if (midia.id != 0) midia.id.toString() else "tmdb_${midia.idTmdb}"
                val ref = colecao.document(chaveDoc)
                batch.set(ref, midia.toMap(), SetOptions.merge())
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

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