package com.example.cinelist

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificacaoRepository @Inject constructor(
    private val notificacaoDao: NotificacaoDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val todasNotificacoes: Flow<List<NotificacaoEntity>> = notificacaoDao.buscarTodas()
    val quantidadeNaoLidas: Flow<Int> = notificacaoDao.contarNaoLidas()

    suspend fun inserir(notificacao: NotificacaoEntity) = notificacaoDao.inserir(notificacao)
    suspend fun marcarComoLida(id: Int) = notificacaoDao.marcarComoLida(id)
    suspend fun marcarTodasComoLidas() = notificacaoDao.marcarTodasComoLidas()

    // 🚀 ELIMINAÇÃO PERMANENTE (Local e Cloud)
    suspend fun deletar(notificacao: NotificacaoEntity) {
        notificacaoDao.deletar(notificacao)
        val uid = auth.currentUser?.uid ?: return

        try {
            // Se for chat, apaga do nó de chats pendentes para não regressar
            if (notificacao.tipo == "CHAT") {
                // assumimos que a ID ou algo identificava o remetente, senao tentamos varrer
                val snaps = firestore.collection("usuarios").document(uid).collection("notificacoes_chat").get().await()
                snaps.documents.forEach { doc ->
                    if (notificacao.mensagem.contains(doc.getString("remetenteNome") ?: "")) {
                        doc.reference.delete()
                    }
                }
            } else if (notificacao.tipo == "AMIZADE") {
                val snaps = firestore.collection("usuarios").document(uid).collection("solicitacoes").get().await()
                snaps.documents.forEach { doc ->
                    if (notificacao.mensagem.contains(doc.getString("remetenteNome") ?: "")) {
                        doc.reference.delete()
                    }
                }
            }
            // Tenta apagar do nó geral de notificacoes (se existir no seu DB)
            firestore.collection("usuarios").document(uid).collection("notificacoes").document(notificacao.id.toString()).delete().await()
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    // 🚀 LIMPEZA PERMANENTE DE TODAS
    suspend fun limparTodas() {
        notificacaoDao.limparTodas()
        val uid = auth.currentUser?.uid ?: return
        try {
            val batch = firestore.batch()
            // Limpa as de chat pendentes
            val chats = firestore.collection("usuarios").document(uid).collection("notificacoes_chat").get().await()
            chats.documents.forEach { batch.delete(it.reference) }

            // Limpa notificações gerais
            val notifs = firestore.collection("usuarios").document(uid).collection("notificacoes").get().await()
            notifs.documents.forEach { batch.delete(it.reference) }

            batch.commit().await()
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun contarNotificacaoRecente(idRef: Int, tipo: String, desde: Long): Int =
        notificacaoDao.contarNotificacaoRecente(idRef, tipo, desde)
}