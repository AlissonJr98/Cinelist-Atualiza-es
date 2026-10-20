package com.example.cinelist

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Enumeração que representa os status possíveis de uma mídia.
 */
enum class StatusMidia(val valor: String) {
    /** Mídia marcada para assistir */
    QUERO_ASSISTIR("Quero Assistir"),
    
    /** Mídia sendo assistida atualmente */
    ASSISTINDO("Assistindo"),
    
    /** Mídia concluída */
    CONCLUIDO("Concluído"),
    
    /** Mídia para descobrir */
    DESCOBRIR("Descobrir");

    companion object {
        /**
         * Converte uma string para o enum StatusMidia.
         * @param texto Texto a ser convertido
         * @return StatusMidia correspondente ou QUERO_ASSISTIR como padrão
         */
        fun fromString(texto: String?): StatusMidia {
            return when (texto?.trim()?.lowercase()) {
                "assistindo" -> ASSISTINDO
                "concluído", "concluido" -> CONCLUIDO
                "descobrir" -> DESCOBRIR
                else -> QUERO_ASSISTIR
            }
        }
    }
}

class AvaliacoesConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromMap(map: Map<String, AvaliacaoMembro>?): String {
        if (map == null) return "{}"
        return gson.toJson(map)
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, AvaliacaoMembro> {
        if (value.isNullOrBlank()) return emptyMap()
        val mapType = object : TypeToken<Map<String, AvaliacaoMembro>>() {}.type
        return try {
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

@Entity(
    tableName = "midias",
    indices = [
        Index("isCasal"),
        Index("casalId"),
        Index("status"),
        Index("idTmdb")
    ]
)
@TypeConverters(AvaliacoesConverters::class)
data class Midia(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var uuid: String = UUID.randomUUID().toString(),
    var idTmdb: Int = 0,
    var titulo: String = "",
    var tipo: String = "Filme",
    var status: String = StatusMidia.QUERO_ASSISTIR.valor,
    var nota: Int = 0,
    var temporadaAtual: Int = 1,
    var episodioAtual: Int = 1,
    var minutoParado: Int = 0,
    var jaEncerrou: Boolean = false,
    var sinopse: String = "",
    var imagemCapa: String = "",
    var genero: String = "Não Informado",
    var duracaoTotal: Int = 0,
    var plataforma: String = "Outros",
    var favorito: Boolean = false,
    var listaCustomizada: String = "Geral",
    var isCasal: Boolean = false,
    var casalId: String = "",
    var adicionadoPor: String = "",
    var dataConclusao: Long = 0L,
    var concluidoPor: String = "",
    var avaliacoesGrupo: Map<String, AvaliacaoMembro> = emptyMap(),
    var dataLancamento: String = "",
    var atualizadoEm: Long = System.currentTimeMillis(),
    var comentarioPessoal: String = "",
    var statusSugestao: String = "APROVADO",

    // 🚀 NOVO CAMPO: Guarda os UIDs separados por vírgula de quem já aceitou
    var uidsAprovados: String = "",

    // 🚀 NOVOS CAMPOS PARA REWATCH, TAGS E PRIVACIDADE
    var contadorRewatch: Int = 0,
    var tagsPersonalizadas: String = "",
    var ehPrivado: Boolean = false
) {
    constructor() : this(id = 0)

    fun obterStatusEnum(): StatusMidia = StatusMidia.fromString(status)

    /**
     * Verdadeiro quando o conteúdo é seriado (divide-se em episódios).
     */
    val ehSerieOuAnime: Boolean
        get() = tipo.equals("Série", ignoreCase = true) ||
                tipo.equals("Anime", ignoreCase = true) ||
                tipo.equals("Novela", ignoreCase = true) ||
                tipo.equals("Dorama", ignoreCase = true)

    /**
     * Verdadeiro quando o utilizador já avançou no conteúdo, ou seja,
     * quando existe um ponto de retorno real ("onde parei").
     *
     * Cobre os casos em que o episódio/temporada foi alterado manualmente
     * mesmo que o status se mantenha "Quero Assistir".
     */
    val temProgressoEpisodio: Boolean
        get() = ehSerieOuAnime && (
                temporadaAtual > 1 || episodioAtual > 1
                )

    /**
     * Avaliação de "prioridade para mostrar primeiro".
     *
     * Ordem decrescente:
     *  3. Está em Assistindo (estava a ver agora)
     *  2. Série com progresso de episódio (sabe-se onde parou)
     *  1. Restante conteúdo ativo
     *  0. Concluído (já terminou, vai para o fundo)
     */
    val pesoPrioridade: Int
        get() = when {
            obterStatusEnum() == StatusMidia.ASSISTINDO -> 3
            obterStatusEnum() == StatusMidia.CONCLUIDO -> 0
            temProgressoEpisodio -> 2
            else -> 1
        }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "uuid" to uuid,
            "idTmdb" to idTmdb,
            "titulo" to titulo,
            "tipo" to tipo,
            "status" to obterStatusEnum().valor,
            "nota" to nota,
            "temporadaAtual" to temporadaAtual,
            "episodioAtual" to episodioAtual,
            "minutoParado" to minutoParado,
            "jaEncerrou" to jaEncerrou,
            "sinopse" to sinopse,
            "imagemCapa" to imagemCapa,
            "genero" to genero,
            "duracaoTotal" to duracaoTotal,
            "plataforma" to plataforma,
            "favorito" to favorito,
            "listaCustomizada" to listaCustomizada,
            "isCasal" to isCasal,
            "casalId" to casalId,
            "adicionadoPor" to adicionadoPor,
            "dataConclusao" to dataConclusao,
            "concluidoPor" to concluidoPor,
            "avaliacoesGrupo" to avaliacoesGrupo.mapValues { it.value.toMap() },
            "dataLancamento" to dataLancamento,
            "atualizadoEm" to atualizadoEm,
            "comentarioPessoal" to comentarioPessoal,
            "statusSugestao" to statusSugestao,
            "uidsAprovados" to uidsAprovados,
            "contadorRewatch" to contadorRewatch,
            "tagsPersonalizadas" to tagsPersonalizadas,
            "ehPrivado" to ehPrivado
        )
    }
}