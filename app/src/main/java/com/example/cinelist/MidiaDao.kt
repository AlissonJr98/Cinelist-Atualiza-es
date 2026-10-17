package com.example.cinelist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para operações com mídias no banco de dados local.
 */
@Dao
interface MidiaDao {

    /**
     * Insere uma mídia no banco de dados.
     * @param midia Mídia a ser inserida
     * @return ID da mídia inserida
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirMidia(midia: Midia): Long

    /**
     * Atualiza uma mídia existente.
     * @param midia Mídia a ser atualizada
     */
    @Update
    suspend fun atualizarMidia(midia: Midia)

    /**
     * Retorna todas as mídias estritamente pessoais (não pertencem a nenhum grupo).
     */
    @Query("SELECT * FROM midias WHERE isCasal = 0 OR isCasal IS NULL OR casalId = ''")
    fun buscarTodasAsMidias(): Flow<List<Midia>>

    /**
     * Garante que traga apenas as mídias pessoais genuínas.
     */
    @Query("SELECT * FROM midias WHERE isCasal = 0 OR isCasal IS NULL OR casalId = ''")
    fun buscarMidiasPessoais(): Flow<List<Midia>>

    /**
     * Retorna todas as mídias de casal/grupo.
     */
    @Query("SELECT * FROM midias WHERE isCasal = 1")
    fun buscarMidiasCasal(): Flow<List<Midia>>

    /**
     * Busca mídias de um grupo/sala específico pelo ID exato.
     * @param grupoId ID do grupo
     */
    @Query("SELECT * FROM midias WHERE isCasal = 1 AND casalId = :grupoId")
    fun buscarMidiasPorGrupo(grupoId: String): Flow<List<Midia>>

    /**
     * Histórico estritamente pessoal (apenas itens concluídos particulares).
     */
    @Query("SELECT * FROM midias WHERE (isCasal = 0 OR isCasal IS NULL OR casalId = '') AND (status = 'Concluído' OR status = 'Concluido')")
    fun buscarHistoricoPessoal(): Flow<List<Midia>>

    /**
     * Histórico estritamente de um grupo/sala ativo.
     * @param grupoId ID do grupo
     */
    @Query("SELECT * FROM midias WHERE isCasal = 1 AND casalId = :grupoId AND (status = 'Concluído' OR status = 'Concluido')")
    fun buscarHistoricoGrupo(grupoId: String): Flow<List<Midia>>

    /**
     * Busca uma mídia pelo ID.
     * @param id ID da mídia
     */
    @Query("SELECT * FROM midias WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): Midia?

    /**
     * Deleta uma mídia do banco de dados.
     * @param midia Mídia a ser deletada
     */
    @Delete
    suspend fun deletarMidia(midia: Midia)

    /**
     * Incrementa o episódio atual de uma mídia.
     * @param idMidia ID da mídia
     */
    @Query("UPDATE midias SET episodioAtual = episodioAtual + 1 WHERE id = :idMidia")
    suspend fun incrementarEpisodio(idMidia: Int)

    /**
     * Atualiza o progresso de um episódio.
     * @param idMidia ID da mídia
     * @param temporada Número da temporada
     * @param episodio Número do episódio
     */
    @Query("UPDATE midias SET temporadaAtual = :temporada, episodioAtual = :episodio WHERE id = :idMidia")
    suspend fun atualizarProgressoEpisodio(idMidia: Int, temporada: Int, episodio: Int)

    /**
     * Atualiza o status de favorito de uma mídia.
     * @param idMidia ID da mídia
     * @param favorito Novo status de favorito
     */
    @Query("UPDATE midias SET favorito = :favorito WHERE id = :idMidia")
    suspend fun atualizarFavorito(idMidia: Int, favorito: Boolean)

    /**
     * Atualiza o status de uma mídia.
     * @param idMidia ID da mídia
     * @param novoStatus Novo status
     */
    @Query("UPDATE midias SET status = :novoStatus WHERE id = :idMidia")
    suspend fun atualizarStatusMidia(idMidia: Int, novoStatus: String)

    // --- GERENCIAMENTO DE GRUPOS / SALAS COMPARTILHADAS LOCALMENTE ---

    /**
     * Insere um grupo no banco de dados.
     * @param grupo Grupo a ser inserido
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirGrupo(grupo: GrupoEntity)

    /**
     * Retorna todos os grupos salvos.
     */
    @Query("SELECT * FROM grupos_compartilhados")
    fun buscarTodosOsGrupos(): Flow<List<GrupoEntity>>

    /**
     * Retorna o grupo ativo atualmente.
     */
    @Query("SELECT * FROM grupos_compartilhados WHERE ativo = 1 LIMIT 1")
    suspend fun buscarGrupoAtivo(): GrupoEntity?

    /**
     * Desativa todos os grupos.
     */
    @Query("UPDATE grupos_compartilhados SET ativo = 0")
    suspend fun desativarTodosOsGrupos()

    /**
     * Define um grupo como ativo.
     * @param grupoId ID do grupo
     */
    @Query("UPDATE grupos_compartilhados SET ativo = 1 WHERE grupoId = :grupoId")
    suspend fun definirGrupoAtivo(grupoId: String)

    /**
     * Deleta um grupo do banco de dados.
     * @param grupo Grupo a ser deletado
     */
    @Delete
    suspend fun deletarGrupo(grupo: GrupoEntity)
}