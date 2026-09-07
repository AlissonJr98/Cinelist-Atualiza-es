package com.example.cinelist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacaoDao {
    @Insert
    suspend fun inserir(notificacao: NotificacaoEntity): Long

    @Query("SELECT * FROM notificacoes ORDER BY dataCriacao DESC")
    fun buscarTodas(): Flow<List<NotificacaoEntity>>

    @Query("SELECT COUNT(*) FROM notificacoes WHERE lida = 0")
    fun contarNaoLidas(): Flow<Int>

    @Query("UPDATE notificacoes SET lida = 1 WHERE id = :id")
    suspend fun marcarComoLida(id: Int)

    @Query("UPDATE notificacoes SET lida = 1")
    suspend fun marcarTodasComoLidas()

    @Delete
    suspend fun deletar(notificacao: NotificacaoEntity)

    @Query("DELETE FROM notificacoes")
    suspend fun limparTodas()

    @Query("SELECT COUNT(*) FROM notificacoes WHERE idReferencia = :idRef AND tipo = :tipo AND dataCriacao > :desde")
    suspend fun contarNotificacaoRecente(idRef: Int, tipo: String, desde: Long): Int
}