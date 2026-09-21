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

    // Retorna apenas mídias estritamente pessoais (não pertencem a nenhum grupo)
    @Query("SELECT * FROM midias WHERE isCasal = 0 OR isCasal IS NULL OR casalId = ''")
    fun buscarTodasAsMidias(): Flow<List<Midia>>

    // Garante que traga apenas as pessoais genuínas
    @Query("SELECT * FROM midias WHERE isCasal = 0 OR isCasal IS NULL OR casalId = ''")
    fun buscarMidiasPessoais(): Flow<List<Midia>>

    @Query("SELECT * FROM midias WHERE isCasal = 1")
    fun buscarMidiasCasal(): Flow<List<Midia>>

    // Busca mídias de um grupo/sala específico pelo ID exato
    @Query("SELECT * FROM midias WHERE isCasal = 1 AND casalId = :grupoId")
    fun buscarMidiasPorGrupo(grupoId: String): Flow<List<Midia>>

    // Histórico estritamente pessoal (apenas itens concluídos particulares)
    @Query("SELECT * FROM midias WHERE (isCasal = 0 OR isCasal IS NULL OR casalId = '') AND (status = 'Concluído' OR status = 'Concluido')")
    fun buscarHistoricoPessoal(): Flow<List<Midia>>

    // Histórico estritamente de um grupo/sala ativo
    @Query("SELECT * FROM midias WHERE isCasal = 1 AND casalId = :grupoId AND (status = 'Concluído' OR status = 'Concluido')")
    fun buscarHistoricoGrupo(grupoId: String): Flow<List<Midia>>

    @Query("SELECT * FROM midias WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): Midia?

    @Delete
    suspend fun deletarMidia(midia: Midia)

    @Query("UPDATE midias SET episodioAtual = episodioAtual + 1 WHERE id = :idMidia")
    suspend fun incrementarEpisodio(idMidia: Int)

    @Query("UPDATE midias SET temporadaAtual = :temporada, episodioAtual = :episodio WHERE id = :idMidia")
    suspend fun atualizarProgressoEpisodio(idMidia: Int, temporada: Int, episodio: Int)

    @Query("UPDATE midias SET favorito = :favorito WHERE id = :idMidia")
    suspend fun atualizarFavorito(idMidia: Int, favorito: Boolean)

    @Query("UPDATE midias SET status = :novoStatus WHERE id = :idMidia")
    suspend fun atualizarStatusMidia(idMidia: Int, novoStatus: String)

    // --- GERENCIAMENTO DE GRUPOS / SALAS COMPARTILHADAS LOCALMENTE ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirGrupo(grupo: GrupoEntity)

    @Query("SELECT * FROM grupos_compartilhados")
    fun buscarTodosOsGrupos(): Flow<List<GrupoEntity>>

    @Query("SELECT * FROM grupos_compartilhados WHERE ativo = 1 LIMIT 1")
    suspend fun buscarGrupoAtivo(): GrupoEntity?

    @Query("UPDATE grupos_compartilhados SET ativo = 0")
    suspend fun desativarTodosOsGrupos()

    @Query("UPDATE grupos_compartilhados SET ativo = 1 WHERE grupoId = :grupoId")
    suspend fun definirGrupoAtivo(grupoId: String)

    @Delete
    suspend fun deletarGrupo(grupo: GrupoEntity)
}