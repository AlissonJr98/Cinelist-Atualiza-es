package com.example.cinelist

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import javax.inject.Singleton

data class MensagemChat(
    val id: String = "",
    val remetenteUid: String = "",
    val texto: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val tipoMensagem: String = "TEXTO", // "TEXTO", "IMAGEM", "STICKER"
    val mediaUrl: String = ""
)

data class MensagemGrupo(
    val id: String = "",
    val autorUid: String = "",
    val autorNome: String = "",
    val autorFotoUrl: String = "",
    val texto: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val editada: Boolean = false,
    val tipoMensagem: String = "TEXTO", // "TEXTO", "IMAGEM", "STICKER"
    val mediaUrl: String = ""
)

data class RespostaSocial(
    val id: String = "",
    val autorUid: String = "",
    val autorNome: String = "",
    val autorFoto: String = "",
    val texto: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ComentarioSocial(
    val id: String = "",
    val autorUid: String = "",
    val autorNome: String = "",
    val autorFoto: String = "",
    val texto: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val curtidasUids: List<String> = emptyList(),
    val respostas: List<RespostaSocial> = emptyList()
)

@Singleton
class SocialRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun observarMensagensGrupo(grupoId: String): Flow<List<MensagemGrupo>> = callbackFlow {
        if (grupoId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("grupos").document(grupoId)
            .collection("chat_mensagens")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val mensagens = snap?.documents?.mapNotNull { it.toObject(MensagemGrupo::class.java) } ?: emptyList()
                trySend(mensagens)
            }

        awaitClose { listener.remove() }
    }

    suspend fun enviarAnexoChat(amigoUid: String, uri: android.net.Uri): Boolean = withContext(Dispatchers.IO) {
        val meuUid = auth.currentUser?.uid ?: return@withContext false
        val chatId = obterIdChat(meuUid, amigoUid)

        try {
            val ref = storage.reference.child("chats_anexos/$chatId/${UUID.randomUUID()}.jpg")
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            enviarMensagemChat(
                amigoUid = amigoUid,
                texto = "",
                tipoMensagem = "IMAGEM",
                mediaUrl = downloadUrl
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun enviarMensagemGrupo(grupoId: String, texto: String, tipoMensagem: String = "TEXTO", mediaUrl: String = "") = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        if (grupoId.isBlank() || (texto.isBlank() && mediaUrl.isBlank())) return@withContext

        try {
            val docUsuario = firestore.collection("usuarios_publicos").document(user.uid).get().await()
            val nome = docUsuario.getString("nome") ?: "Usuário"
            val foto = docUsuario.getString("fotoUrl") ?: ""

            val ref = firestore.collection("grupos").document(grupoId)
                .collection("chat_mensagens").document()

            val mensagem = MensagemGrupo(
                id = ref.id,
                autorUid = user.uid,
                autorNome = nome,
                autorFotoUrl = foto,
                texto = texto.trim(),
                tipoMensagem = tipoMensagem,
                mediaUrl = mediaUrl
            )

            ref.set(mensagem).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun editarMensagemGrupo(grupoId: String, mensagemId: String, novoTexto: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("grupos").document(grupoId)
                .collection("chat_mensagens").document(mensagemId)
                .update("texto", novoTexto.trim(), "editada", true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletarMensagemGrupo(grupoId: String, mensagemId: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("grupos").document(grupoId)
                .collection("chat_mensagens").document(mensagemId)
                .delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun observarComentariosMidia(tmdbId: Int): Flow<List<ComentarioSocial>> = callbackFlow {
        if (tmdbId == 0) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("comentarios_globais").document(tmdbId.toString())
            .collection("comentarios").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lista = snap?.documents?.mapNotNull { it.toObject(ComentarioSocial::class.java) } ?: emptyList()
                trySend(lista)
            }

        awaitClose { listener.remove() }
    }

    suspend fun adicionarComentarioPublico(tmdbId: Int, texto: String) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        if (tmdbId == 0 || texto.isBlank()) return@withContext
        try {
            val docUsuario = firestore.collection("usuarios_publicos").document(user.uid).get().await()
            val nome = docUsuario.getString("nome") ?: "Usuário"
            val foto = docUsuario.getString("fotoUrl") ?: ""

            val ref = firestore.collection("comentarios_globais").document(tmdbId.toString())
                .collection("comentarios").document()

            val comentario = ComentarioSocial(
                id = ref.id,
                autorUid = user.uid,
                autorNome = nome,
                autorFoto = foto,
                texto = texto.trim()
            )
            ref.set(comentario).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun curtirComentario(tmdbId: Int, comentarioId: String, curtiu: Boolean) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val ref = firestore.collection("comentarios_globais").document(tmdbId.toString())
                .collection("comentarios").document(comentarioId)

            if (curtiu) {
                ref.update("curtidasUids", com.google.firebase.firestore.FieldValue.arrayUnion(uid)).await()
            } else {
                ref.update("curtidasUids", com.google.firebase.firestore.FieldValue.arrayRemove(uid)).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun adicionarRespostaComentario(tmdbId: Int, comentarioId: String, texto: String) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        if (texto.isBlank()) return@withContext
        try {
            val docUsuario = firestore.collection("usuarios_publicos").document(user.uid).get().await()
            val nome = docUsuario.getString("nome") ?: "Usuário"
            val foto = docUsuario.getString("fotoUrl") ?: ""

            val resposta = RespostaSocial(
                id = UUID.randomUUID().toString(),
                autorUid = user.uid,
                autorNome = nome,
                autorFoto = foto,
                texto = texto.trim()
            )

            val ref = firestore.collection("comentarios_globais").document(tmdbId.toString())
                .collection("comentarios").document(comentarioId)

            ref.update("respostas", com.google.firebase.firestore.FieldValue.arrayUnion(resposta)).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletarComentarioPublico(tmdbId: Int, comentarioId: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("comentarios_globais").document(tmdbId.toString())
                .collection("comentarios").document(comentarioId)
                .delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun editarComentarioPublico(tmdbId: Int, comentarioId: String, novoTexto: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("comentarios_globais").document(tmdbId.toString())
                .collection("comentarios").document(comentarioId)
                .update("texto", novoTexto.trim()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🚀 LÊ AS NOVAS REDES SOCIAIS NA BUSCA
    suspend fun buscarUsuarios(termo: String): List<AmigoPerfil> = withContext(Dispatchers.IO) {
        val termoLimpo = termo.trim().lowercase()
        if (termoLimpo.isEmpty()) return@withContext emptyList()
        val meuUid = auth.currentUser?.uid ?: ""

        try {
            val snapshotBloqueados = firestore.collection("usuarios")
                .document(meuUid)
                .collection("bloqueados")
                .get()
                .await()
            val uidsBloqueados = snapshotBloqueados.documents.map { it.id }.toSet()

            val snapshot = firestore.collection("usuarios_publicos")
                .whereGreaterThanOrEqualTo("nomeBusca", termoLimpo)
                .whereLessThanOrEqualTo("nomeBusca", termoLimpo + "\uf8ff")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                if (doc.id == meuUid || doc.id in uidsBloqueados) null
                else {
                    AmigoPerfil(
                        uid = doc.id,
                        nome = doc.getString("nome") ?: "Usuário",
                        fotoUrl = doc.getString("fotoUrl") ?: "",
                        bio = doc.getString("bio") ?: "",
                        email = doc.getString("email") ?: "",
                        online = doc.getBoolean("online") ?: false,
                        vistoPorUltimo = doc.getLong("vistoPorUltimo") ?: 0L,
                        instagram = doc.getString("instagram") ?: "",
                        twitter = doc.getString("twitter") ?: "",
                        letterboxd = doc.getString("letterboxd") ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun atualizarPresencaGlobal(online: Boolean) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val dados = mapOf(
                "online" to online,
                "vistoPorUltimo" to System.currentTimeMillis()
            )
            firestore.collection("usuarios_publicos").document(uid)
                .set(dados, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun enviarSolicitacaoAmizade(amigoUid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val meuUid = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Não autenticado"))
        try {
            val docBloqueio = firestore.collection("usuarios")
                .document(amigoUid)
                .collection("bloqueados")
                .document(meuUid)
                .get()
                .await()

            if (docBloqueio.exists()) {
                return@withContext Result.failure(Exception("Não foi possível enviar solicitação para este usuário."))
            }

            val meuBloqueio = firestore.collection("usuarios")
                .document(meuUid)
                .collection("bloqueados")
                .document(amigoUid)
                .get()
                .await()

            if (meuBloqueio.exists()) {
                return@withContext Result.failure(Exception("Você bloqueou este usuário. Desbloqueie para interagir."))
            }

            val timestamp = System.currentTimeMillis()
            val meuPerfilDoc = firestore.collection("usuarios_publicos").document(meuUid).get().await()
            val meuNome = meuPerfilDoc.getString("nome") ?: auth.currentUser?.displayName ?: "Cinéfilo"
            val minhaFoto = meuPerfilDoc.getString("fotoUrl") ?: auth.currentUser?.photoUrl?.toString() ?: ""

            firestore.collection("usuarios")
                .document(meuUid)
                .collection("amigos")
                .document(amigoUid)
                .set(mapOf("status" to "pendente", "atualizadoEm" to timestamp))
                .await()

            val dadosRecebimento = mapOf(
                "remetenteUid" to meuUid,
                "remetenteNome" to meuNome,
                "remetenteFoto" to minhaFoto,
                "status" to "pendente",
                "dataCriacao" to timestamp
            )

            firestore.collection("usuarios")
                .document(amigoUid)
                .collection("solicitacoes")
                .document(meuUid)
                .set(dadosRecebimento)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun cancelarSolicitacaoEnviada(amigoUid: String): Boolean = withContext(Dispatchers.IO) {
        val meuUid = auth.currentUser?.uid ?: return@withContext false
        try {
            firestore.collection("usuarios")
                .document(meuUid)
                .collection("amigos")
                .document(amigoUid)
                .delete()
                .await()

            try {
                firestore.collection("usuarios")
                    .document(amigoUid)
                    .collection("solicitacoes")
                    .document(meuUid)
                    .delete()
                    .await()
            } catch (eIgnorada: Exception) {
                eIgnorada.printStackTrace()
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun aceitarSolicitacao(remetenteUid: String): Boolean = withContext(Dispatchers.IO) {
        val meuUid = auth.currentUser?.uid ?: return@withContext false
        try {
            val batch = firestore.batch()
            val timestamp = System.currentTimeMillis()

            val refMeuAmigo = firestore.collection("usuarios").document(meuUid).collection("amigos").document(remetenteUid)
            val refAmigoDele = firestore.collection("usuarios").document(remetenteUid).collection("amigos").document(meuUid)
            val refSolicitacao = firestore.collection("usuarios").document(meuUid).collection("solicitacoes").document(remetenteUid)

            batch.set(refMeuAmigo, mapOf("status" to "aceito", "adicionadoEm" to timestamp))
            batch.set(refAmigoDele, mapOf("status" to "aceito", "adicionadoEm" to timestamp))
            batch.delete(refSolicitacao)

            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun recusarSolicitacao(remetenteUid: String): Boolean = withContext(Dispatchers.IO) {
        val meuUid = auth.currentUser?.uid ?: return@withContext false
        try {
            val batch = firestore.batch()
            val refSolicitacao = firestore.collection("usuarios").document(meuUid).collection("solicitacoes").document(remetenteUid)
            val refAmigoPendente = firestore.collection("usuarios").document(remetenteUid).collection("amigos").document(meuUid)

            batch.delete(refSolicitacao)
            batch.delete(refAmigoPendente)

            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun atualizarTokenFcm() = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            firestore.collection("usuarios_publicos").document(uid).set(
                mapOf("fcmToken" to token),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun observarSolicitacoesRecebidas(): Flow<List<SolicitacaoAmizadeRecebida>> = callbackFlow {
        val meuUid = auth.currentUser?.uid
        if (meuUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("usuarios").document(meuUid).collection("solicitacoes")
            .addSnapshotListener { snapshot, _ ->
                val docs = snapshot?.documents ?: emptyList()
                val remetentesUids = docs.map { it.id }

                if (remetentesUids.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                firestore.collection("usuarios_publicos")
                    .whereIn(FieldPath.documentId(), remetentesUids.take(10))
                    .get()
                    .addOnSuccessListener { amigosSnap ->
                        val lista = amigosSnap.documents.map { doc ->
                            SolicitacaoAmizadeRecebida(
                                remetenteUid = doc.id,
                                nome = doc.getString("nome") ?: "Cinéfilo",
                                fotoUrl = doc.getString("fotoUrl") ?: "",
                                bio = doc.getString("bio") ?: ""
                            )
                        }
                        trySend(lista)
                    }
                    .addOnFailureListener {
                        trySend(emptyList())
                    }
            }

        awaitClose { listener.remove() }
    }

    // 🚀 LÊ AS NOVAS REDES SOCIAIS DOS AMIGOS
    fun observarAmigos(): Flow<List<AmigoPerfil>> = callbackFlow {
        val meuUid = auth.currentUser?.uid
        if (meuUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var listenerPublicos: com.google.firebase.firestore.ListenerRegistration? = null

        val listenerAmigos = firestore.collection("usuarios").document(meuUid).collection("amigos")
            .whereEqualTo("status", "aceito")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val uids = snapshot?.documents?.map { it.id } ?: emptyList()

                if (uids.isEmpty()) {
                    listenerPublicos?.remove()
                    listenerPublicos = null
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                listenerPublicos?.remove()

                listenerPublicos = firestore.collection("usuarios_publicos")
                    .whereIn(FieldPath.documentId(), uids.take(10))
                    .addSnapshotListener { amigosSnap, errPublicos ->
                        if (errPublicos != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        val lista = amigosSnap?.documents?.map { doc ->
                            AmigoPerfil(
                                uid = doc.id,
                                nome = doc.getString("nome") ?: "Amigo",
                                fotoUrl = doc.getString("fotoUrl") ?: "",
                                bio = doc.getString("bio") ?: "",
                                email = doc.getString("email") ?: "",
                                online = doc.getBoolean("online") ?: false,
                                vistoPorUltimo = doc.getLong("vistoPorUltimo") ?: 0L,
                                instagram = doc.getString("instagram") ?: "",
                                twitter = doc.getString("twitter") ?: "",
                                letterboxd = doc.getString("letterboxd") ?: ""
                            )
                        } ?: emptyList()

                        trySend(lista)
                    }
            }

        awaitClose {
            listenerAmigos.remove()
            listenerPublicos?.remove()
        }
    }

    fun observarUidsStatusPendente(): Flow<List<String>> = callbackFlow {
        val meuUid = auth.currentUser?.uid
        if (meuUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("usuarios").document(meuUid).collection("amigos")
            .whereEqualTo("status", "pendente")
            .addSnapshotListener { snap, _ ->
                val uids = snap?.documents?.map { it.id } ?: emptyList()
                trySend(uids)
            }

        awaitClose { listener.remove() }
    }

    suspend fun buscarListaAmigo(amigoUid: String): List<Midia> = withContext(Dispatchers.IO) {
        if (amigoUid.isBlank()) return@withContext emptyList()
        try {
            val snap = firestore.collection("usuarios").document(amigoUid).collection("midias").get().await()
            snap.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Midia::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun obterIdChat(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    fun observarMensagensChat(amigoUid: String): Flow<List<MensagemChat>> = callbackFlow {
        val meuUid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val chatId = obterIdChat(meuUid, amigoUid)

        val listener = firestore.collection("chats").document(chatId).collection("mensagens")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val mensagens = snap?.documents?.mapNotNull { it.toObject(MensagemChat::class.java) } ?: emptyList()
                trySend(mensagens)
            }

        awaitClose { listener.remove() }
    }

    suspend fun enviarMensagemChat(amigoUid: String, texto: String, tipoMensagem: String = "TEXTO", mediaUrl: String = "") = withContext(Dispatchers.IO) {
        val meuUid = auth.currentUser?.uid ?: return@withContext
        if (texto.isBlank() && mediaUrl.isBlank()) return@withContext
        val chatId = obterIdChat(meuUid, amigoUid)

        val msg = MensagemChat(
            id = firestore.collection("chats").document(chatId).collection("mensagens").document().id,
            remetenteUid = meuUid,
            texto = texto.trim(),
            timestamp = System.currentTimeMillis(),
            tipoMensagem = tipoMensagem,
            mediaUrl = mediaUrl
        )

        firestore.collection("chats").document(chatId).collection("mensagens").document(msg.id).set(msg).await()

        try {
            val docUsuario = firestore.collection("usuarios_publicos").document(meuUid).get().await()
            val meuNome = docUsuario.getString("nome") ?: "Usuário"
            val notificacaoChat = mapOf(
                "remetenteUid" to meuUid,
                "remetenteNome" to meuNome,
                "textoRecente" to texto.trim(),
                "timestamp" to System.currentTimeMillis(),
                "naoLida" to true
            )
            firestore.collection("usuarios").document(amigoUid)
                .collection("notificacoes_chat").document(meuUid)
                .set(notificacaoChat, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🚀 GRAVA AS NOVAS REDES SOCIAIS NA NUVEM
    suspend fun atualizarPerfilPublico(nome: String, bio: String, instagram: String = "", twitter: String = "", letterboxd: String = "") = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        try {
            val dados = mapOf(
                "nome" to nome,
                "nomeBusca" to nome.trim().lowercase(),
                "bio" to bio,
                "instagram" to instagram,
                "twitter" to twitter,
                "letterboxd" to letterboxd,
                "fotoUrl" to (user.photoUrl?.toString() ?: ""),
                "email" to (user.email ?: ""),
                "atualizadoEm" to System.currentTimeMillis()
            )
            firestore.collection("usuarios_publicos").document(user.uid).set(dados, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removerAmigo(amigoUid: String): Boolean = withContext(Dispatchers.IO) {
        val meuUid = auth.currentUser?.uid ?: return@withContext false
        try {
            val batchAmigos = firestore.batch()
            val refMeuAmigo = firestore.collection("usuarios").document(meuUid).collection("amigos").document(amigoUid)
            val refAmigoDele = firestore.collection("usuarios").document(amigoUid).collection("amigos").document(meuUid)

            batchAmigos.delete(refMeuAmigo)
            batchAmigos.delete(refAmigoDele)
            batchAmigos.commit().await()

            val chatId = obterIdChat(meuUid, amigoUid)
            val colecaoMensagens = firestore.collection("chats").document(chatId).collection("mensagens")

            val snapshotMensagens = colecaoMensagens.get().await()
            if (!snapshotMensagens.isEmpty) {
                val batchChat = firestore.batch()
                snapshotMensagens.documents.forEach { doc ->
                    batchChat.delete(doc.reference)
                }
                batchChat.commit().await()
            }

            firestore.collection("chats").document(chatId).delete().await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun bloquearUsuario(alvoUid: String): Boolean = withContext(Dispatchers.IO) {
        val meuUid = auth.currentUser?.uid ?: return@withContext false
        try {
            val timestamp = System.currentTimeMillis()

            firestore.collection("usuarios")
                .document(meuUid)
                .collection("bloqueados")
                .document(alvoUid)
                .set(mapOf("bloqueadoEm" to timestamp))
                .await()

            removerAmigo(alvoUid)

            try {
                firestore.collection("usuarios").document(meuUid).collection("solicitacoes").document(alvoUid).delete().await()
                firestore.collection("usuarios").document(alvoUid).collection("solicitacoes").document(meuUid).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun desbloquearUsuario(alvoUid: String): Boolean = withContext(Dispatchers.IO) {
        val meuUid = auth.currentUser?.uid ?: return@withContext false
        try {
            firestore.collection("usuarios")
                .document(meuUid)
                .collection("bloqueados")
                .document(alvoUid)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 🚀 LÊ AS REDES SOCIAIS NA LISTA DE BLOQUEADOS TAMBÉM
    fun observarUsuariosBloqueados(): Flow<List<AmigoPerfil>> = callbackFlow {
        val meuUid = auth.currentUser?.uid
        if (meuUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var listenerPublicos: com.google.firebase.firestore.ListenerRegistration? = null

        val listenerBloqueados = firestore.collection("usuarios")
            .document(meuUid)
            .collection("bloqueados")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val uidsBloqueados = snapshot?.documents?.map { it.id } ?: emptyList()

                if (uidsBloqueados.isEmpty()) {
                    listenerPublicos?.remove()
                    listenerPublicos = null
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                listenerPublicos?.remove()
                listenerPublicos = firestore.collection("usuarios_publicos")
                    .whereIn(FieldPath.documentId(), uidsBloqueados.take(10))
                    .addSnapshotListener { docsPublicos, errPublicos ->
                        if (errPublicos != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        val lista = docsPublicos?.documents?.map { doc ->
                            AmigoPerfil(
                                uid = doc.id,
                                nome = doc.getString("nome") ?: "Usuário",
                                fotoUrl = doc.getString("fotoUrl") ?: "",
                                bio = doc.getString("bio") ?: "",
                                email = doc.getString("email") ?: "",
                                instagram = doc.getString("instagram") ?: "",
                                twitter = doc.getString("twitter") ?: "",
                                letterboxd = doc.getString("letterboxd") ?: ""
                            )
                        } ?: emptyList()

                        trySend(lista)
                    }
            }

        awaitClose {
            listenerBloqueados.remove()
            listenerPublicos?.remove()
        }
    }
}