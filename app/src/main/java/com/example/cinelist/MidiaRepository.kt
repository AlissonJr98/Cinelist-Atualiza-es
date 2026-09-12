package com.example.cinelist

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MidiaRepository @Inject constructor(
    private val midiaDao: MidiaDao,
    private val notificacaoRepository: NotificacaoRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val todasAsMidias: Flow<List<Midia>> = midiaDao.buscarTodasAsMidias()
    val midiasPessoais: Flow<List<Midia>> = midiaDao.buscarMidiasPessoais()
    val gruposSalvos: Flow<List<GrupoEntity>> = midiaDao.buscarTodosOsGrupos()

    fun buscarMidiasPorGrupo(grupoId: String): Flow<List<Midia>> {
        return midiaDao.buscarMidiasPorGrupo(grupoId)
    }

    suspend fun obterGrupoAtivoLocal(): GrupoEntity? {
        return midiaDao.buscarGrupoAtivo()
    }

    suspend fun ativarGrupoLocal(grupoId: String) {
        midiaDao.desativarTodosOsGrupos()
        if (grupoId.isNotBlank()) {
            midiaDao.definirGrupoAtivo(grupoId)
        }
    }

    // Gera um código aleatório curto e amigável (ex: CINE-7482)
    fun gerarCodigoAleatorio(): String {
        val numero = Random.nextInt(1000, 9999)
        return "CINE-$numero"
    }

    suspend fun salvarOuEntrarNoGrupo(grupoId: String, nomeGrupo: String, tipo: String) {
        val grupoLimpo = grupoId.trim().uppercase()
        val grupo = GrupoEntity(grupoId = grupoLimpo, nomeGrupo = nomeGrupo, tipoGrupo = tipo, ativo = true)
        midiaDao.desativarTodosOsGrupos()
        midiaDao.inserirGrupo(grupo)

        // Garante que o documento raiz do grupo existe no Firestore para permitir sincronização cruzada
        withContext(Dispatchers.IO) {
            try {
                val docRef = firestore.collection("grupos").document(grupoLimpo)
                val snapshot = docRef.get().await()
                if (!snapshot.exists()) {
                    val dadosGrupo = mapOf(
                        "grupoId" to grupoLimpo,
                        "nomeGrupo" to nomeGrupo,
                        "tipoGrupo" to tipo,
                        "criadoEm" to System.currentTimeMillis()
                    )
                    docRef.set(dadosGrupo).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deletarGrupoLocal(grupo: GrupoEntity) {
        midiaDao.deletarGrupo(grupo)
    }

    private fun obterColecaoUsuario(): com.google.firebase.firestore.CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("usuarios").document(uid).collection("midias")
    }

    // Caminho compartilhado na raiz do Firestore acessível por qualquer membro da sala
    private fun obterColecaoGrupo(grupoId: String): com.google.firebase.firestore.CollectionReference? {
        if (grupoId.isBlank()) return null
        return firestore.collection("grupos").document(grupoId).collection("midias")
    }

    // Busca o nome real do usuário no Firestore (usuarios_publicos)
    private suspend fun obterNomeRealUsuario(): String {
        val usuario = auth.currentUser ?: return "Alguém"
        return try {
            val doc = firestore.collection("usuarios_publicos").document(usuario.uid).get().await()
            val nome = doc.getString("nome")
            if (!nome.isNullOrBlank()) nome else usuario.email?.substringBefore("@") ?: "Alguém"
        } catch (e: Exception) {
            usuario.email?.substringBefore("@") ?: "Alguém"
        }
    }

    suspend fun inserir(midia: Midia) {
        val midiasLocais = midiaDao.buscarTodasAsMidias().firstOrNull() ?: emptyList()
        val nomeReal = obterNomeRealUsuario()

        val plataformaNormalizada = if (midia.plataforma.isBlank() || midia.plataforma.equals("Não Informado", ignoreCase = true) || midia.plataforma.equals("Outros", ignoreCase = true) || midia.plataforma.equals("TV / Original", ignoreCase = true)) {
            val tituloLower = midia.titulo.lowercase()
            when {
                tituloLower.contains("reacher") || tituloLower.contains("the boys") || tituloLower.contains("invincible") || tituloLower.contains("rings of power") -> "Prime Video"
                else -> if (midia.tipo.equals("Filme", ignoreCase = true)) "Cinema" else "TV / Original"
            }
        } else {
            midia.plataforma
        }

        val autorFinal = if (midia.isCasal && midia.adicionadoPor.isBlank()) nomeReal else midia.adicionadoPor

        val midiaTratada = midia.copy(
            plataforma = plataformaNormalizada,
            adicionadoPor = autorFinal
        )

        val midiaExistente = midiasLocais.find {
            it.isCasal == midiaTratada.isCasal && it.casalId == midiaTratada.casalId && (
                    (it.idTmdb != 0 && it.idTmdb == midiaTratada.idTmdb) ||
                            (it.titulo.equals(midiaTratada.titulo, ignoreCase = true) && it.tipo.equals(midiaTratada.tipo, ignoreCase = true))
                    )
        }

        if (midiaExistente != null) {
            val midiaAtualizada = midiaTratada.copy(id = midiaExistente.id)
            midiaDao.atualizarMidia(midiaAtualizada)
            sincronizarItemIndividualFirestore(midiaAtualizada)
        } else {
            val idGerado = midiaDao.inserirMidia(midiaTratada)
            val midiaComId = if (midiaTratada.id == 0) midiaTratada.copy(id = idGerado.toInt()) else midiaTratada
            sincronizarItemIndividualFirestore(midiaComId)
        }
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
            val colecao = if (midia.isCasal) {
                obterColecaoGrupo(midia.casalId)
            } else {
                obterColecaoUsuario()
            }
            val chaveDoc = if (midia.idTmdb != 0) "tmdb_${midia.idTmdb}" else midia.id.toString()
            colecao?.document(chaveDoc)?.set(midia.toMap(), SetOptions.merge())?.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun removerItemFirestore(midia: Midia) = withContext(Dispatchers.IO) {
        try {
            val colecao = if (midia.isCasal) {
                obterColecaoGrupo(midia.casalId)
            } else {
                obterColecaoUsuario()
            } ?: return@withContext

            if (midia.idTmdb != 0) {
                colecao.document("tmdb_${midia.idTmdb}").delete().await()
            }
            if (midia.id != 0) {
                colecao.document(midia.id.toString()).delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletar(midia: Midia) {
        midiaDao.deletarMidia(midia)
        removerItemFirestore(midia)
    }

    // Sincronização em tempo real para o grupo via Firestore Snapshot Listener
    fun observarMidiasDoGrupoFirestore(grupoId: String): Flow<List<Midia>> = callbackFlow {
        val colecao = obterColecaoGrupo(grupoId)
        if (colecao == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val usuarioAtualUid = auth.currentUser?.uid ?: ""

        val listener = colecao.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val midiasNuvem = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Midia::class.java)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            launch(Dispatchers.IO) {
                val locais = midiaDao.buscarMidiasPorGrupo(grupoId).firstOrNull() ?: emptyList()
                midiasNuvem.forEach { nuvem ->
                    val existente = locais.find { (it.idTmdb != 0 && it.idTmdb == nuvem.idTmdb) || it.titulo.equals(nuvem.titulo, ignoreCase = true) }
                    if (existente != null) {
                        midiaDao.atualizarMidia(nuvem.copy(id = existente.id, isCasal = true, casalId = grupoId))
                    } else {
                        midiaDao.inserirMidia(nuvem.copy(id = 0, isCasal = true, casalId = grupoId))

                        // Dispara notificação se o item foi adicionado por outro membro
                        if (nuvem.adicionadoPor.isNotBlank()) {
                            val jaNotificado = notificacaoRepository.contarNotificacaoRecente(
                                idRef = nuvem.idTmdb,
                                tipo = "NOVO_GRUPO",
                                desde = System.currentTimeMillis() - 60000
                            ) > 0

                            if (!jaNotificado) {
                                val novaNotificacao = NotificacaoEntity(
                                    tipo = "NOVO_GRUPO",
                                    titulo = "Novo item na lista compartilhada",
                                    mensagem = "${nuvem.adicionadoPor} adicionou \"${nuvem.titulo}\" para o grupo!",
                                    dataCriacao = System.currentTimeMillis(),
                                    lida = false,
                                    idReferencia = nuvem.idTmdb
                                )
                                notificacaoRepository.inserir(novaNotificacao)
                            }
                        }
                    }
                }
            }

            trySend(midiasNuvem)
        }

        awaitClose { listener.remove() }
    }

    suspend fun sincronizacaoAutomaticaSilenciosa() = withContext(Dispatchers.IO) {
        try {
            val colecao = obterColecaoUsuario() ?: return@withContext
            val snapshot = colecao.get().await()

            val midiasNuvem = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Midia::class.java)
                } catch (e: Exception) {
                    null
                }
            }

            val midiasLocais = midiaDao.buscarMidiasPessoais().firstOrNull() ?: emptyList()

            midiasNuvem.forEach { midiaNuvem ->
                val midiaExistente = midiasLocais.find {
                    (it.idTmdb != 0 && it.idTmdb == midiaNuvem.idTmdb) ||
                            (it.titulo.equals(midiaNuvem.titulo, ignoreCase = true) && it.tipo.equals(midiaNuvem.tipo, ignoreCase = true))
                }

                val itemTratado = midiaNuvem.copy(isCasal = false, casalId = "")
                if (midiaExistente != null) {
                    midiaDao.atualizarMidia(itemTratado.copy(id = midiaExistente.id))
                } else {
                    midiaDao.inserirMidia(itemTratado.copy(id = 0))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restaurarDoFirestore(): Int = withContext(Dispatchers.IO) {
        try {
            val colecao = obterColecaoUsuario() ?: return@withContext 0
            val snapshot = colecao.get().await()

            val midiasNuvem = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Midia::class.java)
                } catch (e: Exception) {
                    null
                }
            }

            val midiasLocais = midiaDao.buscarMidiasPessoais().firstOrNull() ?: emptyList()

            midiasNuvem.forEach { midiaNuvem ->
                val midiaExistente = midiasLocais.find {
                    (it.idTmdb != 0 && it.idTmdb == midiaNuvem.idTmdb) ||
                            (it.titulo.equals(midiaNuvem.titulo, ignoreCase = true) && it.tipo.equals(midiaNuvem.tipo, ignoreCase = true))
                }

                val itemTratado = midiaNuvem.copy(isCasal = false, casalId = "")
                if (midiaExistente != null) {
                    midiaDao.atualizarMidia(itemTratado.copy(id = midiaExistente.id))
                } else {
                    midiaDao.inserirMidia(itemTratado.copy(id = 0))
                }
            }
            midiasNuvem.size
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun backupCompletoParaFirestore(): Boolean = withContext(Dispatchers.IO) {
        try {
            val colecao = obterColecaoUsuario() ?: return@withContext false
            val midiasLocais = midiaDao.buscarMidiasPessoais().firstOrNull() ?: emptyList()

            midiasLocais.forEach { midia ->
                val chaveDoc = if (midia.idTmdb != 0) "tmdb_${midia.idTmdb}" else midia.id.toString()
                colecao.document(chaveDoc).set(midia.toMap(), SetOptions.merge()).await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun limparTudoCompleto() = withContext(Dispatchers.IO) {
        try {
            val colecao = obterColecaoUsuario()
            colecao?.get()?.await()?.documents?.forEach { it.reference.delete().await() }

            val locais = midiaDao.buscarTodasAsMidias().firstOrNull() ?: emptyList()
            locais.forEach { midiaDao.deletarMidia(it) }
        } catch (e: Exception) {
            e.printStackTrace()
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
            config = PagingConfig(pageSize = 20, enablePlaceholders = false, initialLoadSize = 20),
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