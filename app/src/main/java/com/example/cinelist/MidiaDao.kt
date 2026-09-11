package com.example.cinelist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MidiaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirMidia(midia: Midia): Long

    @Update
    suspend fun atualizarMidia(midia: Midia)

    @Query("SELECT * FROM midias")
    fun buscarTodasAsMidias(): Flow<List<Midia>>

    @Query("SELECT * FROM midias WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): Midia?

    @Delete
    suspend fun deletarMidia(midia: Midia)

    @Query("UPDATE midias SET episodioAtual = episodioAtual + 1 WHERE id = :idMidia")
    suspend fun incrementarEpisodio(idMidia: Int)

    @Query("UPDATE midias SET temporadaAtual = :temporada, episodioAtual = :episodio WHERE id = :idMidia")
    suspend fun atualizarProgressoEpisodio(idMidia: Int, temporada: Int, episodio: Int)
}