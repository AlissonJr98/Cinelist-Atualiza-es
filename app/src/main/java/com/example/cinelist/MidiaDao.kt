package com.example.cinelist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MidiaDao {

    @Insert
    suspend fun inserirMidia(midia: Midia)

    @Update
    suspend fun atualizarMidia(midia: Midia)

    @Query("SELECT * FROM midias")
    fun buscarTodasAsMidias(): Flow<List<Midia>>

    @Delete
    suspend fun deletarMidia(midia: Midia)

    // Ajustado para 'midias' (plural) para bater com o SELECT acima
    @Query("UPDATE midias SET episodioAtual = episodioAtual + 1 WHERE id = :idMidia")
    suspend fun incrementarEpisodio(idMidia: Int)

    @Query("UPDATE midias SET temporadaAtual = :temporada, episodioAtual = :episodio WHERE id = :idMidia")
    suspend fun atualizarProgressoEpisodio(idMidia: Int, temporada: Int, episodio: Int)
}