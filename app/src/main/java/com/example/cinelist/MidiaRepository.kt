package com.example.cinelist

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import com.google.firebase.messaging.FirebaseMessaging

@Singleton
class MidiaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val midiaDao: MidiaDao,
    private val notificacaoRepository: NotificacaoRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val mutexSincronizacao = Mutex()
    private val mutexSyncGrupo = Mutex()
    private var uidSincronizado: String? = null

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

    fun gerarCodigoAleatorio(): String {
        val numero = Random.nextInt(1000, 9999)
        return "CINE-$numero"
    }

    suspend fun verificarEEntrarNoGrupo(grupoId: String, nomeGrupo: String, tipo: String, senhaDigitada: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val grupoLimpo = grupoId.trim().uppercase()
            val docRef = firestore.collection("grupos").document(grupoLimpo)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                return@withContext Result.failure(Exception("Este código de grupo não existe."))
            }

            val senhaSalva = snapshot.getString("senha") ?: ""
            if (senhaSalva.isNotBlank() && senhaSalva != senhaDigitada.trim()) {
                return@withContext Result.failure(Exception("Senha incorreta para este grupo."))
            }

            val grupo = GrupoEntity(grupoId = grupoLimpo, nomeGrupo = nomeGrupo, tipoGrupo = tipo, ativo = true)
            midiaDao.desativarTodosOsGrupos()
            midiaDao.inserirGrupo(grupo)

            try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    firestore.collection("usuarios").document(uid).set(
                        mapOf("ultimoGrupoAtivo" to grupoLimpo, "ultimoNomeGrupo" to nomeGrupo),
                        SetOptions.merge()
                    ).await()

                    firestore.collection("usuarios").document(uid)
                        .collection("minhas_salas").document(grupoLimpo).set(
                            mapOf("grupoId" to grupoLimpo, "nomeGrupo" to nomeGrupo, "tipoGrupo" to tipo),
                            SetOptions.merge()
                        ).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            inscreverNoTopicoDoGrupo(grupoLimpo)
            atualizarPresencaNoGrupo(grupoLimpo, true)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun criarNovoGrupoNaNuvem(grupoId: String, nomeGrupo: String, tipo: String, senha: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val grupoLimpo = grupoId.trim().uppercase()
            val docRef = firestore.collection("grupos").document(grupoLimpo)

            val uid = auth.currentUser?.uid ?: ""
            val dadosGrupo = mapOf(
                "grupoId" to grupoLimpo,
                "nomeGrupo" to nomeGrupo,
                "tipoGrupo" to tipo,
                "senha" to senha.trim(),
                "criadoEm" to System.currentTimeMillis(),
                "criadorUid" to uid
            )
            docRef.set(dadosGrupo).await()

            val grupo = GrupoEntity(grupoId = grupoLimpo, nomeGrupo = nomeGrupo, tipoGrupo = tipo, ativo = true)
            midiaDao.desativarTodosOsGrupos()
            midiaDao.inserirGrupo(grupo)

            try {
                val uidUsuario = auth.currentUser?.uid
                if (uidUsuario != null) {
                    firestore.collection("usuarios").document(uidUsuario).set(
                        mapOf("ultimoGrupoAtivo" to grupoLimpo, "ultimoNomeGrupo" to nomeGrupo),
                        SetOptions.merge()
                    ).await()

                    firestore.collection("usuarios").document(uidUsuario)
                        .collection("minhas_salas").document(grupoLimpo).set(
                            mapOf("grupoId" to grupoLimpo, "nomeGrupo" to nomeGrupo, "tipoGrupo" to tipo),
                            SetOptions.merge()
                        ).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            inscreverNoTopicoDoGrupo(grupoLimpo)
            atualizarPresencaNoGrupo(grupoLimpo, true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deletarGrupoLocal(grupo: GrupoEntity) {
        atualizarPresencaNoGrupo(grupo.grupoId, false)
        desinscreverDoTopicoDoGrupo(grupo.grupoId)
        midiaDao.deletarGrupo(grupo)

        try {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                firestore.collection("usuarios").document(uid)
                    .collection("minhas_salas").document(grupo.grupoId).delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun verificarSeUsuarioEhAdmin(grupoId: String, uid: String): Boolean = withContext(Dispatchers.IO) {
        if (grupoId.isBlank() || uid.isBlank()) return@withContext false
        try {
            val doc = firestore.collection("grupos").document(grupoId).get().await()
            val criadorUid = doc.getString("criadorUid") ?: doc.getString("adminUid") ?: ""
            criadorUid == uid
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun removerMembroDoGrupo(grupoId: String, membroUid: String): Boolean = withContext(Dispatchers.IO) {
        if (grupoId.isBlank() || membroUid.isBlank()) return@withContext false
        try {
            // Apaga imediatamente o documento do membro para sumir do painel do Admin instantaneamente
            firestore.collection("grupos").document(grupoId)
                .collection("membros").document(membroUid).delete().await()

            try {
                firestore.collection("usuarios").document(membroUid)
                    .collection("minhas_salas").document(grupoId).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun atualizarSenhaDoGrupo(grupoId: String, novaSenha: String): Boolean = withContext(Dispatchers.IO) {
        if (grupoId.isBlank()) return@withContext false
        try {
            firestore.collection("grupos").document(grupoId)
                .update("senha", novaSenha.trim()).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun obterColecaoUsuario(): com.google.firebase.firestore.CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("usuarios").document(uid).collection("midias")
    }

    private fun obterColecaoGrupo(grupoId: String): com.google.firebase.firestore.CollectionReference? {
        if (grupoId.isBlank()) return null
        return firestore.collection("grupos").document(grupoId).collection("midias")
    }

    suspend fun obterNomeRealUsuario(): String {
        val usuario = auth.currentUser ?: return "Alguém"
        return try {
            val doc = firestore.collection("usuarios_publicos").document(usuario.uid).get().await()
            val nome = doc.getString("nome")
            if (!nome.isNullOrBlank()) {
                nome
            } else {
                val email = usuario.email ?: ""
                if (email.contains("@")) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "Alguém"
            }
        } catch (e: Exception) {
            val email = usuario.email ?: ""
            if (email.contains("@")) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "Alguém"
        }
    }

    private suspend fun obterFotoUsuario(): String {
        val usuario = auth.currentUser ?: return ""
        return try {
            val doc = firestore.collection("usuarios_publicos").document(usuario.uid).get().await()
            doc.getString("fotoUrl") ?: usuario.photoUrl?.toString() ?: ""
        } catch (e: Exception) {
            usuario.photoUrl?.toString() ?: ""
        }
    }

    private fun gerarChaveDocumento(midia: Midia): String {
        return if (midia.idTmdb != 0) "tmdb_${midia.idTmdb}" else "local_${midia.uuid}"
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
                            (it.uuid.isNotBlank() && it.uuid == midiaTratada.uuid) ||
                            (it.titulo.trim().equals(midiaTratada.titulo.trim(), ignoreCase = true) && it.tipo.equals(midiaTratada.tipo, ignoreCase = true))
                    )
        }

        if (midiaExistente != null) {
            val midiaAtualizada = midiaTratada.copy(id = midiaExistente.id, uuid = midiaExistente.uuid)
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
            val chaveDoc = gerarChaveDocumento(midia)
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

            colecao.document(gerarChaveDocumento(midia)).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletar(midia: Midia) {
        midiaDao.deletarMidia(midia)
        removerItemFirestore(midia)
    }

    fun observarMidiasDoGrupoFirestore(grupoId: String): Flow<List<Midia>> = callbackFlow {
        val colecao = obterColecaoGrupo(grupoId)
        if (colecao == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = colecao.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val midiasNuvem = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val rawData = doc.data ?: return@mapNotNull null
                    val midiaObj = doc.toObject(Midia::class.java) ?: return@mapNotNull null

                    val rawAvaliacoes = rawData["avaliacoesGrupo"] as? Map<*, *>
                    val mapaAvaliacoes = mutableMapOf<String, AvaliacaoMembro>()
                    rawAvaliacoes?.forEach { (k, v) ->
                        if (k is String && v is Map<*, *>) {
                            @Suppress("UNCHECKED_CAST")
                            mapaAvaliacoes[k] = AvaliacaoMembro.fromMap(v as Map<String, Any?>)
                        }
                    }
                    midiaObj.copy(avaliacoesGrupo = mapaAvaliacoes)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            launch(Dispatchers.IO) {
                mutexSyncGrupo.withLock {
                    val locais = midiaDao.buscarMidiasPorGrupo(grupoId).firstOrNull()?.toMutableList() ?: mutableListOf()
                    val nomeUsuarioLogado = obterNomeRealUsuario()

                    midiasNuvem.forEach { nuvem ->
                        val existente = locais.find {
                            (it.idTmdb != 0 && it.idTmdb == nuvem.idTmdb) ||
                                    (it.uuid.isNotBlank() && it.uuid == nuvem.uuid) ||
                                    it.titulo.trim().equals(nuvem.titulo.trim(), ignoreCase = true)
                        }

                        if (existente != null) {
                            val midiaAtualizada = nuvem.copy(id = existente.id, isCasal = true, casalId = grupoId)
                            midiaDao.atualizarMidia(midiaAtualizada)
                            val idx = locais.indexOfFirst { it.id == existente.id }
                            if (idx != -1) locais[idx] = midiaAtualizada
                        } else {
                            val newId = midiaDao.inserirMidia(nuvem.copy(id = 0, isCasal = true, casalId = grupoId))
                            val inserida = nuvem.copy(id = newId.toInt(), isCasal = true, casalId = grupoId)
                            locais.add(inserida)

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

                                    if (!nuvem.adicionadoPor.equals(nomeUsuarioLogado, ignoreCase = true)) {
                                        val idNotificacao = if (nuvem.idTmdb != 0) nuvem.idTmdb else newId.toInt()
                                        NotificacaoHelper.dispararNotificacaoGrupo(
                                            context = context,
                                            titulo = "Novo item na sala compartilhada",
                                            autor = nuvem.adicionadoPor,
                                            midiaTitulo = nuvem.titulo,
                                            idRef = idNotificacao
                                        )
                                    }
                                }
                            }
                        }
                    }

                    locais.forEach { local ->
                        val aindaExisteNaNuvem = midiasNuvem.any { nuvem ->
                            (local.idTmdb != 0 && local.idTmdb == nuvem.idTmdb) ||
                                    (local.uuid.isNotBlank() && local.uuid == nuvem.uuid) ||
                                    local.titulo.trim().equals(nuvem.titulo.trim(), ignoreCase = true)
                        }
                        if (!aindaExisteNaNuvem) {
                            midiaDao.deletarMidia(local)
                        }
                    }
                }
            }

            trySend(midiasNuvem)
        }

        awaitClose { listener.remove() }
    }

    suspend fun atualizarPresencaNoGrupo(grupoId: String, online: Boolean) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (grupoId.isBlank()) return@withContext

        try {
            val nomeUsuario = obterNomeRealUsuario()
            val fotoUrl = obterFotoUsuario()

            val docRef = firestore.collection("grupos").document(grupoId)
                .collection("membros").document(uid)

            val dados = mutableMapOf<String, Any>(
                "uid" to uid,
                "nome" to nomeUsuario,
                "fotoUrl" to fotoUrl,
                "online" to online,
                "vistoPorUltimo" to System.currentTimeMillis()
            )

            if (!online) {
                dados["assistindoAgoraTitulo"] = ""
                dados["assistindoAgoraEpisodio"] = ""
            }

            docRef.set(dados, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun definirAssistindoAgora(grupoId: String, titulo: String, episodioTexto: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (grupoId.isBlank()) return@withContext

        try {
            val docRef = firestore.collection("grupos").document(grupoId)
                .collection("membros").document(uid)

            docRef.set(
                mapOf(
                    "assistindoAgoraTitulo" to titulo,
                    "assistindoAgoraEpisodio" to episodioTexto,
                    "online" to true,
                    "vistoPorUltimo" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun limparAssistindoAgora(grupoId: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (grupoId.isBlank()) return@withContext

        try {
            val docRef = firestore.collection("grupos").document(grupoId)
                .collection("membros").document(uid)

            docRef.set(
                mapOf(
                    "assistindoAgoraTitulo" to "",
                    "assistindoAgoraEpisodio" to "",
                    "vistoPorUltimo" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun observarMembrosDoGrupo(grupoId: String): Flow<List<MembroGrupo>> = callbackFlow {
        if (grupoId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("grupos").document(grupoId).collection("membros")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val lista = snapshot?.documents?.mapNotNull { doc ->
                try {
                    MembroGrupo(
                        uid = doc.getString("uid") ?: doc.id,
                        nome = doc.getString("nome") ?: "Membro",
                        fotoUrl = doc.getString("fotoUrl") ?: "",
                        online = doc.getBoolean("online") ?: false,
                        vistoPorUltimo = doc.getLong("vistoPorUltimo") ?: 0L,
                        assistindoAgoraTitulo = doc.getString("assistindoAgoraTitulo") ?: "",
                        assistindoAgoraEpisodio = doc.getString("assistindoAgoraEpisodio") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            trySend(lista)
        }

        awaitClose { listener.remove() }
    }

    suspend fun salvarAvaliacaoMembro(
        grupoId: String,
        midia: Midia,
        nota: Int,
        comentario: String
    ) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (grupoId.isBlank()) return@withContext

        try {
            val nome = obterNomeRealUsuario()
            val foto = obterFotoUsuario()
            val avaliacao = AvaliacaoMembro(
                autorUid = uid,
                autorNome = nome,
                autorFoto = foto,
                nota = nota,
                comentario = comentario.trim(),
                dataAtualizacao = System.currentTimeMillis()
            )

            val chaveDoc = if (midia.idTmdb != 0) "tmdb_${midia.idTmdb}" else "local_${midia.uuid}"
            val docRef = firestore.collection("grupos").document(grupoId)
                .collection("midias").document(chaveDoc)

            docRef.update("avaliacoesGrupo.$uid", avaliacao.toMap()).await()

            val mapaAtualizado = midia.avaliacoesGrupo.toMutableMap()
            mapaAtualizado[uid] = avaliacao
            midiaDao.atualizarMidia(midia.copy(avaliacoesGrupo = mapaAtualizado))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun votarNoMatch(grupoId: String, midia: Midia, curtiu: Boolean) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (grupoId.isBlank()) return@withContext

        try {
            val chaveDoc = if (midia.idTmdb != 0) "match_tmdb_${midia.idTmdb}" else "match_local_${midia.uuid}"
            val docRef = firestore.collection("grupos").document(grupoId)
                .collection("matches").document(chaveDoc)

            if (curtiu) {
                docRef.set(
                    mapOf(
                        "idDoc" to chaveDoc,
                        "idTmdb" to midia.idTmdb,
                        "titulo" to midia.titulo,
                        "imagemCapa" to (midia.imagemCapa ?: ""),
                        "tipo" to midia.tipo,
                        "genero" to midia.genero,
                        "sinopse" to midia.sinopse,
                        "votos" to com.google.firebase.firestore.FieldValue.arrayUnion(uid)
                    ),
                    SetOptions.merge()
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "idDoc" to chaveDoc,
                        "votos" to com.google.firebase.firestore.FieldValue.arrayRemove(uid)
                    ),
                    SetOptions.merge()
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun observarMatchesDoGrupo(grupoId: String): Flow<List<MatchMidia>> = callbackFlow {
        if (grupoId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("grupos").document(grupoId)
            .collection("matches")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val matches = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val votos = (doc.get("votos") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        MatchMidia(
                            idDoc = doc.id,
                            idTmdb = doc.getLong("idTmdb")?.toInt() ?: 0,
                            titulo = doc.getString("titulo") ?: "",
                            imagemCapa = doc.getString("imagemCapa") ?: "",
                            tipo = doc.getString("tipo") ?: "Filme",
                            genero = doc.getString("genero") ?: "",
                            sinopse = doc.getString("sinopse") ?: "",
                            votos = votos
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(matches)
            }

        awaitClose { listener.remove() }
    }

    suspend fun reiniciarRodadaMatch(grupoId: String) = withContext(Dispatchers.IO) {
        if (grupoId.isBlank()) return@withContext
        try {
            val colecao = firestore.collection("grupos").document(grupoId).collection("matches")
            val docs = colecao.get().await()
            for (doc in docs.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun buscarPopularesParaMatch(pagina: Int): List<Midia> = withContext(Dispatchers.IO) {
        try {
            val respostaFilmes = RetrofitClient.apiService.descobrirFilmes(
                pagina = pagina,
                sortBy = "popularity.desc",
                provedores = null,
                generos = null
            )

            val respostaSeries = RetrofitClient.apiService.descobrirSeries(
                pagina = pagina,
                sortBy = "popularity.desc",
                provedores = null,
                generos = null
            )

            val listaFilmes = respostaFilmes.resultados.map { item ->
                Midia(
                    idTmdb = item.idTmdb,
                    titulo = item.titulo,
                    tipo = "Filme",
                    status = "Quero Assistir",
                    nota = 0,
                    sinopse = item.sinopse,
                    imagemCapa = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "",
                    genero = item.generoTexto,
                    plataforma = item.plataformaDetectada.ifBlank { "Cinema" }
                )
            }

            val listaSeries = respostaSeries.resultados.map { item ->
                Midia(
                    idTmdb = item.idTmdb,
                    titulo = item.titulo,
                    tipo = "Série",
                    status = "Quero Assistir",
                    nota = 0,
                    sinopse = item.sinopse,
                    imagemCapa = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${item.caminhoPoster}" else "",
                    genero = item.generoTexto,
                    plataforma = item.plataformaDetectada.ifBlank { "TV / Original" }
                )
            }

            (listaFilmes + listaSeries).shuffled()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun sincronizacaoAutomaticaSilenciosa() {
        val uid = auth.currentUser?.uid ?: return
        if (uidSincronizado == uid) return
        mutexSincronizacao.withLock {
            if (uidSincronizado == uid) return@withLock
            executarSincronizacao()
            uidSincronizado = uid
        }
    }

    suspend fun forcarSincronizacaoManual() {
        mutexSincronizacao.withLock {
            executarSincronizacao()
            uidSincronizado = auth.currentUser?.uid
        }
    }

    fun limparEstadoSincronizacao() {
        uidSincronizado = null
    }

    private suspend fun executarSincronizacao() = withContext(Dispatchers.IO) {
        try {
            val colecao = obterColecaoUsuario()
            if (colecao != null) {
                val snapshot = colecao.get().await()
                val midiasNuvem = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Midia::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }

                val midiasLocais = midiaDao.buscarMidiasPessoais().firstOrNull()?.toMutableList() ?: mutableListOf()

                midiasNuvem.forEach { midiaNuvem ->
                    val itemTratado = midiaNuvem.copy(isCasal = false, casalId = "")

                    val midiaExistente = midiasLocais.find {
                        (it.idTmdb != 0 && it.idTmdb == itemTratado.idTmdb) ||
                                (it.uuid.isNotBlank() && it.uuid == itemTratado.uuid) ||
                                (it.titulo.trim().equals(itemTratado.titulo.trim(), ignoreCase = true) && it.tipo.equals(itemTratado.tipo, ignoreCase = true))
                    }

                    if (midiaExistente != null) {
                        midiaDao.atualizarMidia(itemTratado.copy(id = midiaExistente.id))
                    } else {
                        val newId = midiaDao.inserirMidia(itemTratado.copy(id = 0))
                        midiasLocais.add(itemTratado.copy(id = newId.toInt()))
                    }
                }
            }

            restaurarGruposDoUsuario()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restaurarGruposDoUsuario() = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid ?: return@withContext
            val gruposLocais = midiaDao.buscarTodosOsGrupos().firstOrNull() ?: emptyList()

            val snapshotSalas = firestore.collection("usuarios").document(uid).collection("minhas_salas").get().await()

            snapshotSalas.documents.forEach { doc ->
                val grupoId = doc.getString("grupoId") ?: ""
                val nomeGrupo = doc.getString("nomeGrupo") ?: "Sala Compartilhada"
                val tipo = doc.getString("tipoGrupo") ?: "Casal"

                if (grupoId.isNotBlank()) {
                    val jaExisteLocalmente = gruposLocais.any { it.grupoId == grupoId }
                    if (!jaExisteLocalmente) {
                        val grupo = GrupoEntity(grupoId = grupoId, nomeGrupo = nomeGrupo, tipoGrupo = tipo, ativo = false)
                        midiaDao.inserirGrupo(grupo)
                    }
                }
            }

            if (snapshotSalas.isEmpty) {
                val docUsuario = firestore.collection("usuarios").document(uid).get().await()
                val ultimoGrupo = docUsuario.getString("ultimoGrupoAtivo") ?: ""
                val nomeUltimoGrupo = docUsuario.getString("ultimoNomeGrupo") ?: "Lista Compartilhada"

                if (ultimoGrupo.isNotBlank() && gruposLocais.none { it.grupoId == ultimoGrupo }) {
                    val grupo = GrupoEntity(grupoId = ultimoGrupo, nomeGrupo = nomeUltimoGrupo, tipoGrupo = "Casal", ativo = true)
                    midiaDao.inserirGrupo(grupo)

                    firestore.collection("usuarios").document(uid).collection("minhas_salas").document(ultimoGrupo).set(
                        mapOf("grupoId" to ultimoGrupo, "nomeGrupo" to nomeUltimoGrupo, "tipoGrupo" to "Casal"),
                        SetOptions.merge()
                    )
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

            val midiasLocais = midiaDao.buscarMidiasPessoais().firstOrNull()?.toMutableList() ?: mutableListOf()

            midiasNuvem.forEach { midiaNuvem ->
                val itemTratado = midiaNuvem.copy(isCasal = false, casalId = "")

                val midiaExistente = midiasLocais.find {
                    (it.idTmdb != 0 && it.idTmdb == itemTratado.idTmdb) ||
                            (it.uuid.isNotBlank() && it.uuid == itemTratado.uuid) ||
                            (it.titulo.trim().equals(itemTratado.titulo.trim(), ignoreCase = true) && it.tipo.equals(itemTratado.tipo, ignoreCase = true))
                }

                if (midiaExistente != null) {
                    midiaDao.atualizarMidia(itemTratado.copy(id = midiaExistente.id))
                } else {
                    val newId = midiaDao.inserirMidia(itemTratado.copy(id = 0))
                    midiasLocais.add(itemTratado.copy(id = newId.toInt()))
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
                colecao.document(gerarChaveDocumento(midia)).set(midia.toMap(), SetOptions.merge()).await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun limparTodaALista() = withContext(Dispatchers.IO) {
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

    fun inscreverNoTopicoDoGrupo(grupoId: String) {
        if (grupoId.isNotBlank()) {
            FirebaseMessaging.getInstance().subscribeToTopic("grupo_$grupoId")
        }
    }

    fun desinscreverDoTopicoDoGrupo(grupoId: String) {
        if (grupoId.isNotBlank()) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("grupo_$grupoId")
        }
    }
}