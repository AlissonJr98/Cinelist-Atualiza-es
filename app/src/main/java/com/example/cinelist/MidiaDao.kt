package com.example.cinelist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MidiaDao {

    // 1. Função exclusiva para ADICIONAR uma nova mídia pela primeira vez
    @Insert
    suspend fun inserirMidia(midia: Midia)

    // 2. Função exclusiva para ATUALIZAR os minutos/episódios de uma mídia que já existe
    @Update
    suspend fun atualizarMidia(midia: Midia)

    // 3. Buscar todas as mídias salvas
    @Query("SELECT * FROM midias")
    fun buscarTodasAsMidias(): Flow<List<Midia>>

    // 4. Deletar uma mídia
    @Delete
    suspend fun deletarMidia(midia: Midia)
}