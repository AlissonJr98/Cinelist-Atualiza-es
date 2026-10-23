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

/**
 * Representa uma mensagem de chat entre dois usuários.
 */
data class MensagemChat(
    val id: String = "",
    val remetenteUid: String = "",
    val texto: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val tipoMensagem: String = "TEXTO", // "TEXTO", "IMAGEM", "STICKER"
    val mediaUrl: String = ""
)

/**
 * Representa uma mensagem de grupo/casal.
 */
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

/**
 * Representa uma resposta a um comentário social.
 */
data class RespostaSocial(
    val id: String = "",
    val autorUid: String = "",
    val autorNome: String = "",
    val autorFoto: String = "",
    val texto: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Representa um comentário social em uma mídia.
 */
data class ComentarioSocial(
    val id: String = "",
    val autorUid: String = "",
    val autorNome: String = "",
    val autorFoto: String = "",
    val texto: String = "",
    val contemSpoiler: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val curtidasUids: List<String> = emptyList(),
    val respostas: List<RespostaSocial> = emptyList()
)

/**
 * Repositório responsável pelas funcionalidades sociais do aplicativo.
 * 
 * Gerencia amigos, chats, comentários e interações sociais.
 */
@Singleton
class SocialRepository @Inject constructor() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

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
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val docRef = firestore.collection("grupos").document(grupoId)
                .collection("chat_mensagens").document(mensagemId)
            val doc = docRef.get().await()
            if (doc.getString("autorUid") == uid) {
                docRef.update("texto", novoTexto.trim(), "editada", true).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletarMensagemGrupo(grupoId: String, mensagemId: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val docRef = firestore.collection("grupos").document(grupoId)
                .collection("chat_mensagens").document(mensagemId)
            val doc = docRef.get().await()
            if (doc.getString("autorUid") == uid) {
                docRef.delete().await()
            }
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
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val docRef = firestore.collection("comentarios_globais").document(tmdbId.toString())
                .collection("comentarios").document(comentarioId)
            val doc = docRef.get().await()
            if (doc.getString("autorUid") == uid) {
                docRef.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun editarComentarioPublico(tmdbId: Int, comentarioId: String, novoTexto: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val docRef = firestore.collection("comentarios_globais").document(tmdbId.toString())
                .collection("comentarios").document(comentarioId)
            val doc = docRef.get().await()
            if (doc.getString("autorUid") == uid) {
                docRef.update("texto", novoTexto.trim()).await()
            }
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

    // =========================================================================
    // FEED SOCIAL DE ATIVIDADES DOS AMIGOS
    // =========================================================================

    suspend fun publicarAtividade(
        tipo: String, // "AVALIOU", "ASSISTIU", "QUERO_VER", "FAVORITOU", "RECOMENDACAO"
        tmdbId: Int,
        tituloMidia: String,
        posterPath: String,
        tipoMidia: String,
        nota: Float = 0f,
        comentario: String = ""
    ) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        if (tituloMidia.isBlank()) return@withContext

        try {
            val docUsuario = firestore.collection("usuarios_publicos").document(user.uid).get().await()
            val nome = docUsuario.getString("nome") ?: user.displayName ?: "Cinéfilo"
            val foto = docUsuario.getString("fotoUrl") ?: user.photoUrl?.toString() ?: ""

            val ref = firestore.collection("atividades_sociais").document()
            val atividade = AtividadeSocial(
                id = ref.id,
                autorUid = user.uid,
                autorNome = nome,
                autorFotoUrl = foto,
                tipo = tipo,
                tmdbId = tmdbId,
                tituloMidia = tituloMidia,
                posterPath = posterPath,
                tipoMidia = tipoMidia,
                nota = nota,
                comentario = comentario.trim(),
                timestamp = System.currentTimeMillis(),
                reacoes = emptyMap()
            )
            ref.set(atividade).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun observarFeedAtividades(): Flow<List<AtividadeSocial>> = callbackFlow {
        val listener = firestore.collection("atividades_sociais")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(60)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val ativ = doc.toObject(AtividadeSocial::class.java)
                        ativ?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(lista)
            }

        awaitClose { listener.remove() }
    }

    suspend fun alternarReacaoAtividade(atividadeId: String, emoji: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (atividadeId.isBlank() || emoji.isBlank()) return@withContext

        try {
            val ref = firestore.collection("atividades_sociais").document(atividadeId)
            val doc = ref.get().await()
            if (!doc.exists()) return@withContext

            val mapaRaw = doc.get("reacoes") as? Map<*, *> ?: emptyMap<String, List<String>>()
            val mapaAtual = mutableMapOf<String, List<String>>()
            mapaRaw.forEach { (k, v) ->
                if (k is String && v is List<*>) {
                    mapaAtual[k] = v.filterIsInstance<String>()
                }
            }

            val listaUids = (mapaAtual[emoji] ?: emptyList()).toMutableList()
            if (listaUids.contains(uid)) {
                listaUids.remove(uid)
            } else {
                listaUids.add(uid)
            }

            if (listaUids.isEmpty()) {
                mapaAtual.remove(emoji)
            } else {
                mapaAtual[emoji] = listaUids
            }

            ref.update("reacoes", mapaAtual).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletarAtividade(atividadeId: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val ref = firestore.collection("atividades_sociais").document(atividadeId)
            val doc = ref.get().await()
            if (doc.getString("autorUid") == uid) {
                ref.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // =========================================================================
    // ENQUETES RÁPIDAS NAS SALAS / GRUPOS
    // =========================================================================

    suspend fun criarEnqueteGrupo(
        grupoId: String,
        titulo: String,
        descricao: String,
        opcoes: List<Map<String, Any>>,
        duracaoMinutos: Int = 60
    ) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        if (grupoId.isBlank() || titulo.isBlank() || opcoes.isEmpty()) return@withContext

        try {
            val docUsuario = firestore.collection("usuarios_publicos").document(user.uid).get().await()
            val nome = docUsuario.getString("nome") ?: user.displayName ?: "Membro"
            val foto = docUsuario.getString("fotoUrl") ?: user.photoUrl?.toString() ?: ""

            val ref = firestore.collection("grupos").document(grupoId)
                .collection("enquetes").document()

            val agora = System.currentTimeMillis()
            val expiraEm = if (duracaoMinutos > 0) agora + (duracaoMinutos * 60_000L) else 0L

            val enquete = EnqueteGrupo(
                id = ref.id,
                grupoId = grupoId,
                criadorUid = user.uid,
                criadorNome = nome,
                criadorFotoUrl = foto,
                titulo = titulo.trim(),
                descricao = descricao.trim(),
                opcoes = opcoes,
                votos = emptyMap(),
                encerrada = false,
                timestamp = agora,
                expiraEm = expiraEm
            )

            ref.set(enquete).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun observarEnquetesGrupo(grupoId: String): Flow<List<EnqueteGrupo>> = callbackFlow {
        if (grupoId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("grupos").document(grupoId)
            .collection("enquetes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val enq = doc.toObject(EnqueteGrupo::class.java)
                        enq?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(lista)
            }

        awaitClose { listener.remove() }
    }

    suspend fun votarEnqueteGrupo(grupoId: String, enqueteId: String, opcaoId: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (grupoId.isBlank() || enqueteId.isBlank() || opcaoId.isBlank()) return@withContext

        try {
            val ref = firestore.collection("grupos").document(grupoId)
                .collection("enquetes").document(enqueteId)

            val doc = ref.get().await()
            if (!doc.exists()) return@withContext

            val encerrada = doc.getBoolean("encerrada") ?: false
            if (encerrada) return@withContext

            val votosRaw = doc.get("votos") as? Map<*, *> ?: emptyMap<String, String>()
            val mapaVotos = mutableMapOf<String, String>()
            votosRaw.forEach { (k, v) ->
                if (k is String && v is String) {
                    mapaVotos[k] = v
                }
            }

            // Alterna o voto se clicar na mesma opção, ou altera para nova opção
            if (mapaVotos[uid] == opcaoId) {
                mapaVotos.remove(uid)
            } else {
                mapaVotos[uid] = opcaoId
            }

            ref.update("votos", mapaVotos).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun encerrarEnqueteGrupo(grupoId: String, enqueteId: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (grupoId.isBlank() || enqueteId.isBlank()) return@withContext

        try {
            val ref = firestore.collection("grupos").document(grupoId)
                .collection("enquetes").document(enqueteId)

            val doc = ref.get().await()
            if (doc.getString("criadorUid") == uid) {
                ref.update("encerrada", true).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletarEnqueteGrupo(grupoId: String, enqueteId: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (grupoId.isBlank() || enqueteId.isBlank()) return@withContext

        try {
            val ref = firestore.collection("grupos").document(grupoId)
                .collection("enquetes").document(enqueteId)

            val doc = ref.get().await()
            val criadorUid = doc.getString("criadorUid")
            if (criadorUid == uid) {
                ref.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // =========================================================================
    // CINEPARTY SINCRONIZADO EM TEMPO REAL (ESTILO SALA / DISCORD)
    // =========================================================================

    fun observarSessaoCineParty(salaId: String): Flow<CinePartySessao?> = callbackFlow {
        if (salaId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("cineparty_salas").document(salaId)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }
            try {
                val sessao = snapshot.toObject(CinePartySessao::class.java)?.copy(id = snapshot.id)
                trySend(sessao)
            } catch (e: Exception) {
                e.printStackTrace()
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun iniciarOuEntrarCineParty(
        salaId: String,
        tituloMidia: String,
        posterMidia: String
    ): Result<CinePartySessao> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext Result.failure(Exception("Não autenticado"))
        if (salaId.isBlank()) return@withContext Result.failure(Exception("Código de sala inválido"))

        try {
            val docUsuario = firestore.collection("usuarios_publicos").document(user.uid).get().await()
            val meuNome = docUsuario.getString("nome") ?: user.displayName ?: "Cinéfilo"
            val minhaFoto = docUsuario.getString("fotoUrl") ?: user.photoUrl?.toString() ?: ""

            val docRef = firestore.collection("cineparty_salas").document(salaId)
            val snapshot = docRef.get().await()

            val meuParticipante = mapOf(
                "uid" to user.uid,
                "nome" to meuNome,
                "fotoUrl" to minhaFoto,
                "online" to true,
                "isAnfitriao" to (!snapshot.exists() || snapshot.getString("anfitriaoUid") == user.uid),
                "statusVoz" to "MUTADO",
                "entrouEm" to System.currentTimeMillis()
            )

            if (!snapshot.exists()) {
                val novaSessao = CinePartySessao(
                    id = salaId,
                    salaId = salaId,
                    tituloMidia = tituloMidia,
                    posterMidia = posterMidia,
                    anfitriaoUid = user.uid,
                    anfitriaoNome = meuNome,
                    emContagem = false,
                    contagemNumero = 3,
                    contagemIniciadaEm = 0L,
                    estadoReproducao = "PARADO",
                    tempoReproducaoSegundos = 0,
                    ultimaAtualizacaoTimestamp = System.currentTimeMillis(),
                    participantes = mapOf(user.uid to meuParticipante),
                    canalVozAtivo = false
                )
                docRef.set(novaSessao).await()
                Result.success(novaSessao)
            } else {
                docRef.update(
                    mapOf(
                        "participantes.${user.uid}" to meuParticipante,
                        "ultimaAtualizacaoTimestamp" to System.currentTimeMillis()
                    )
                ).await()
                val sessaoAtualizada = docRef.get().await().toObject(CinePartySessao::class.java)!!
                Result.success(sessaoAtualizada)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun dispararContagemCineParty(salaId: String) = withContext(Dispatchers.IO) {
        if (salaId.isBlank()) return@withContext
        try {
            val docRef = firestore.collection("cineparty_salas").document(salaId)
            docRef.update(
                mapOf(
                    "emContagem" to true,
                    "contagemNumero" to 3,
                    "contagemIniciadaEm" to System.currentTimeMillis(),
                    "estadoReproducao" to "CONTAGEM"
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun atualizarContagemPassoCineParty(salaId: String, numero: Int) = withContext(Dispatchers.IO) {
        if (salaId.isBlank()) return@withContext
        try {
            val docRef = firestore.collection("cineparty_salas").document(salaId)
            if (numero <= 0) {
                docRef.update(
                    mapOf(
                        "emContagem" to false,
                        "contagemNumero" to 0,
                        "estadoReproducao" to "REPRODUZINDO",
                        "tempoReproducaoSegundos" to 0,
                        "ultimaAtualizacaoTimestamp" to System.currentTimeMillis()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "contagemNumero" to numero
                    )
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun alternarPauseCineParty(salaId: String, pausar: Boolean, segundosAtuais: Int) = withContext(Dispatchers.IO) {
        if (salaId.isBlank()) return@withContext
        try {
            val docRef = firestore.collection("cineparty_salas").document(salaId)
            docRef.update(
                mapOf(
                    "estadoReproducao" to if (pausar) "PAUSADO" else "REPRODUZINDO",
                    "tempoReproducaoSegundos" to segundosAtuais,
                    "ultimaAtualizacaoTimestamp" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun enviarReacaoCineParty(salaId: String, emoji: String) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        if (salaId.isBlank() || emoji.isBlank()) return@withContext
        try {
            val docUsuario = firestore.collection("usuarios_publicos").document(user.uid).get().await()
            val meuNome = docUsuario.getString("nome") ?: user.displayName ?: "Cinéfilo"

            val docRef = firestore.collection("cineparty_salas").document(salaId)
            docRef.update(
                mapOf(
                    "ultimaReacaoEmoji" to emoji,
                    "ultimaReacaoAutor" to meuNome,
                    "ultimaReacaoTimestamp" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sairCineParty(salaId: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (salaId.isBlank()) return@withContext
        try {
            val docRef = firestore.collection("cineparty_salas").document(salaId)
            val snap = docRef.get().await()
            if (!snap.exists()) return@withContext

            val sessao = snap.toObject(CinePartySessao::class.java)
            if (sessao?.anfitriaoUid == uid) {
                val participantes = sessao.obterParticipantesMapeados().filter { it.uid != uid }
                if (participantes.isEmpty()) {
                    docRef.delete().await()
                    return@withContext
                } else {
                    val novoAnf = participantes.first()
                    docRef.update(
                        mapOf(
                            "anfitriaoUid" to novoAnf.uid,
                            "anfitriaoNome" to novoAnf.nome,
                            "participantes.$uid" to com.google.firebase.firestore.FieldValue.delete()
                        )
                    ).await()
                    return@withContext
                }
            }

            docRef.update(
                mapOf(
                    "participantes.$uid" to com.google.firebase.firestore.FieldValue.delete()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // 🏆 RANKING SEMANAL & PÓDIO ENTRE AMIGOS
    // ==========================================
    suspend fun obterRankingSemanal(minhasMidias: List<Midia>): List<ItemLeaderboard> = withContext(Dispatchers.IO) {
        val meuUid = auth.currentUser?.uid ?: ""
        val meuNome = auth.currentUser?.displayName?.ifBlank { "Você" } ?: "Você"
        val minhaFoto = auth.currentUser?.photoUrl?.toString() ?: ""

        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val inicioSemana = cal.timeInMillis

        // Pontos do usuário atual
        val meusConcluidosSemana = minhasMidias.filter {
            it.status == "Concluído" && (it.dataConclusao >= inicioSemana || it.atualizadoEm >= inicioSemana)
        }
        val meusFilmes = meusConcluidosSemana.count { it.tipo.equals("Filme", ignoreCase = true) }
        val minhasSeries = meusConcluidosSemana.size - meusFilmes
        val meusPontos = (meusFilmes * 50) + (minhasSeries * 30) + (minhasMidias.count { it.status == "Concluído" } * 5).coerceAtLeast(15)
        val minhasHoras = (meusFilmes * 2.0) + (minhasSeries * 1.5)

        val listaRanking = mutableListOf<ItemLeaderboard>()
        listaRanking.add(
            ItemLeaderboard(
                uid = meuUid,
                nome = meuNome,
                fotoUrl = minhaFoto,
                pontos = meusPontos,
                titulosConcluidos = meusConcluidosSemana.size.coerceAtLeast(minhasMidias.count { it.status == "Concluído" }),
                horasEstimadas = minhasHoras,
                souEu = true
            )
        )

        // Carrega amigos
        try {
            val snapAmigos = firestore.collection("usuarios").document(meuUid).collection("amigos").get().await()
            val amigosUids = snapAmigos.documents.map { it.id }

            for (amigoUid in amigosUids.take(15)) {
                try {
                    val perfilSnap = firestore.collection("usuarios_publicos").document(amigoUid).get().await()
                    val nomeAmigo = perfilSnap.getString("nome") ?: "Amigo"
                    val fotoAmigo = perfilSnap.getString("fotoUrl") ?: ""

                    val midiasSnap = firestore.collection("usuarios").document(amigoUid).collection("midias").get().await()
                    val midiasAmigo = midiasSnap.documents.mapNotNull { it.toObject(Midia::class.java) }
                    val concluidosAmigoSemana = midiasAmigo.filter {
                        it.status == "Concluído" && (it.dataConclusao >= inicioSemana || it.atualizadoEm >= inicioSemana)
                    }
                    val filmesAmigo = concluidosAmigoSemana.count { it.tipo.equals("Filme", ignoreCase = true) }
                    val seriesAmigo = concluidosAmigoSemana.size - filmesAmigo
                    val pontosAmigo = (filmesAmigo * 50) + (seriesAmigo * 30) + (midiasAmigo.count { it.status == "Concluído" } * 5).coerceAtLeast(10)
                    val horasAmigo = (filmesAmigo * 2.0) + (seriesAmigo * 1.5)

                    listaRanking.add(
                        ItemLeaderboard(
                            uid = amigoUid,
                            nome = nomeAmigo,
                            fotoUrl = fotoAmigo,
                            pontos = pontosAmigo,
                            titulosConcluidos = concluidosAmigoSemana.size.coerceAtLeast(midiasAmigo.count { it.status == "Concluído" }),
                            horasEstimadas = horasAmigo,
                            souEu = false
                        )
                    )
                } catch (e: Exception) {
                    // Ignora falha de amigo individual
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val ordenada = listaRanking.sortedByDescending { it.pontos }
        ordenada.mapIndexed { index, item ->
            val pos = index + 1
            val medalha = when (pos) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "#$pos"
            }
            item.copy(posicao = pos, medalha = medalha)
        }
    }

    // ==========================================
    // 🕵️ CINEBLIND (AMIGO OCULTO DE FILMES NA SALA)
    // ==========================================
    fun observarCineBlindSala(grupoId: String): Flow<CineBlindSessao?> = callbackFlow {
        if (grupoId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("grupos").document(grupoId)
            .collection("cineblind").document("sessao_ativa")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(CineBlindSessao::class.java))
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun iniciarCineBlind(grupoId: String): String = withContext(Dispatchers.IO) {
        if (grupoId.isBlank()) return@withContext ""
        val meuUid = auth.currentUser?.uid ?: ""
        val meuNome = auth.currentUser?.displayName ?: "Cinéfilo"
        val sessaoId = "sessao_ativa"

        val novaSessao = CineBlindSessao(
            id = sessaoId,
            grupoId = grupoId,
            criadorUid = meuUid,
            criadorNome = meuNome,
            estado = "INSCRICAO",
            sugestoes = emptyMap(),
            vencedorId = "",
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("grupos").document(grupoId)
            .collection("cineblind").document(sessaoId)
            .set(novaSessao)
            .await()

        sessaoId
    }

    suspend fun adicionarSugestaoBlind(
        grupoId: String,
        sessaoId: String,
        tituloReal: String,
        posterReal: String,
        idTmdb: Int,
        genero: String,
        ano: String,
        pistas: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        if (grupoId.isBlank() || sessaoId.isBlank()) return@withContext false
        val meuUid = auth.currentUser?.uid ?: return@withContext false
        val meuNome = auth.currentUser?.displayName ?: "Cinéfilo"

        try {
            val mapaSugestao = mapOf(
                "id" to meuUid,
                "autorUid" to meuUid,
                "autorNome" to meuNome,
                "tituloReal" to tituloReal,
                "posterReal" to posterReal,
                "idTmdb" to idTmdb,
                "genero" to genero,
                "ano" to ano,
                "pistas" to pistas,
                "votos" to emptyList<String>()
            )

            firestore.collection("grupos").document(grupoId)
                .collection("cineblind").document(sessaoId)
                .update("sugestoes.$meuUid", mapaSugestao)
                .await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun avancarParaVotacaoCineBlind(grupoId: String, sessaoId: String): Boolean = withContext(Dispatchers.IO) {
        if (grupoId.isBlank() || sessaoId.isBlank()) return@withContext false
        try {
            firestore.collection("grupos").document(grupoId)
                .collection("cineblind").document(sessaoId)
                .update("estado", "VOTACAO")
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun votarCineBlind(grupoId: String, sessaoId: String, sugestaoId: String): Boolean = withContext(Dispatchers.IO) {
        if (grupoId.isBlank() || sessaoId.isBlank() || sugestaoId.isBlank()) return@withContext false
        val meuUid = auth.currentUser?.uid ?: return@withContext false
        try {
            val docRef = firestore.collection("grupos").document(grupoId)
                .collection("cineblind").document(sessaoId)

            firestore.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                val sessao = snap.toObject(CineBlindSessao::class.java) ?: return@runTransaction
                val sugestoes = sessao.sugestoes.toMutableMap()

                // Remove voto anterior do usuário se já tiver votado
                sugestoes.forEach { (chave, mapa) ->
                    val mapaMutavel = mapa.toMutableMap()
                    @Suppress("UNCHECKED_CAST")
                    val votos = ((mapaMutavel["votos"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()).toMutableList()
                    if (votos.remove(meuUid)) {
                        mapaMutavel["votos"] = votos
                        sugestoes[chave] = mapaMutavel
                    }
                }

                // Adiciona voto na sugestão escolhida
                val alvo = sugestoes[sugestaoId]?.toMutableMap()
                if (alvo != null) {
                    @Suppress("UNCHECKED_CAST")
                    val votos = ((alvo["votos"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()).toMutableList()
                    if (!votos.contains(meuUid)) {
                        votos.add(meuUid)
                        alvo["votos"] = votos
                        sugestoes[sugestaoId] = alvo
                    }
                }

                transaction.update(docRef, "sugestoes", sugestoes)
            }.await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun revelarVencedorCineBlind(grupoId: String, sessaoId: String): Boolean = withContext(Dispatchers.IO) {
        if (grupoId.isBlank() || sessaoId.isBlank()) return@withContext false
        try {
            val docRef = firestore.collection("grupos").document(grupoId)
                .collection("cineblind").document(sessaoId)

            val snap = docRef.get().await()
            val sessao = snap.toObject(CineBlindSessao::class.java) ?: return@withContext false
            val maisVotada = sessao.obterSugestoesMapeadas().maxByOrNull { it.totalVotos }
            val idVencedor = maisVotada?.id ?: ""

            docRef.update(
                mapOf(
                    "estado" to "REVELADO",
                    "vencedorId" to idVencedor
                )
            ).await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun encerrarCineBlind(grupoId: String, sessaoId: String): Boolean = withContext(Dispatchers.IO) {
        if (grupoId.isBlank() || sessaoId.isBlank()) return@withContext false
        try {
            firestore.collection("grupos").document(grupoId)
                .collection("cineblind").document(sessaoId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==========================================
    // 👥 CINEMATCH COLETIVO (SWIPE EM GRUPO NA SALA)
    // ==========================================
    fun observarMatchColetivoSala(grupoId: String): Flow<MatchColetivoSessao?> = callbackFlow {
        if (grupoId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("grupos").document(grupoId)
            .collection("match_coletivo").document("sessao_ativa")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: emptyMap()
                    trySend(MatchColetivoSessao.fromMap(data))
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun iniciarMatchColetivo(grupoId: String, filmesCandidatos: List<FilmeCandidatoMatch>): Boolean = withContext(Dispatchers.IO) {
        if (grupoId.isBlank()) return@withContext false
        val meuUid = auth.currentUser?.uid ?: ""
        val meuNome = auth.currentUser?.displayName ?: "Cinéfilo"
        val sessaoId = "sessao_ativa"

        val novaSessao = MatchColetivoSessao(
            id = sessaoId,
            grupoId = grupoId,
            criadorUid = meuUid,
            criadorNome = meuNome,
            estado = "ATIVO",
            filmes = filmesCandidatos,
            votosSim = emptyMap(),
            timestamp = System.currentTimeMillis()
        )

        try {
            firestore.collection("grupos").document(grupoId)
                .collection("match_coletivo").document(sessaoId)
                .set(novaSessao.toMap())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun votarMatchColetivo(grupoId: String, idTmdb: Int, deuMatch: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (grupoId.isBlank()) return@withContext false
        val meuUid = auth.currentUser?.uid ?: return@withContext false
        val sessaoId = "sessao_ativa"
        val docRef = firestore.collection("grupos").document(grupoId)
            .collection("match_coletivo").document(sessaoId)

        try {
            firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) return@runTransaction

                val sessao = MatchColetivoSessao.fromMap(snap.data ?: emptyMap())
                val votos = sessao.votosSim.toMutableMap()
                val chaveFilme = idTmdb.toString()
                val votantes = (votos[chaveFilme] ?: emptyList()).toMutableList()

                if (deuMatch) {
                    if (!votantes.contains(meuUid)) votantes.add(meuUid)
                } else {
                    votantes.remove(meuUid)
                }
                votos[chaveFilme] = votantes

                val updates = mutableMapOf<String, Any>("votosSim" to votos)
                if (votantes.size >= 2 && sessao.estado == "ATIVO") {
                    val filmeVencedor = sessao.filmes.firstOrNull { it.idTmdb == idTmdb }
                    if (filmeVencedor != null) {
                        updates["estado"] = "MATCH"
                        updates["vencedorIdTmdb"] = filmeVencedor.idTmdb
                        updates["vencedorTitulo"] = filmeVencedor.titulo
                        updates["vencedorPoster"] = filmeVencedor.posterUrl
                    }
                }

                tx.update(docRef, updates)
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun encerrarMatchColetivo(grupoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("grupos").document(grupoId)
                .collection("match_coletivo").document("sessao_ativa")
                .delete()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
