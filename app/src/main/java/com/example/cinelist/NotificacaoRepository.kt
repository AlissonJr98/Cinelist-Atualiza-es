package com.example.cinelist

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificacaoRepository @Inject constructor(
    private val notificacaoDao: NotificacaoDao
) {
    val todasNotificacoes: Flow<List<NotificacaoEntity>> = notificacaoDao.buscarTodas()
    val quantidadeNaoLidas: Flow<Int> = notificacaoDao.contarNaoLidas()

    suspend fun inserir(notificacao: NotificacaoEntity) = notificacaoDao.inserir(notificacao)
    suspend fun marcarComoLida(id: Int) = notificacaoDao.marcarComoLida(id)
    suspend fun marcarTodasComoLidas() = notificacaoDao.marcarTodasComoLidas()
    suspend fun deletar(notificacao: NotificacaoEntity) = notificacaoDao.deletar(notificacao)
    suspend fun limparTodas() = notificacaoDao.limparTodas()
    suspend fun contarNotificacaoRecente(idRef: Int, tipo: String, desde: Long): Int =
        notificacaoDao.contarNotificacaoRecente(idRef, tipo, desde)
}