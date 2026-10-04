package com.example.cinelist

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

// 🚀 NOVO ENUM: Centraliza e padroniza todos os status da aplicação
enum class StatusMidia(val valor: String) {
    QUERO_ASSISTIR("Quero Assistir"),
    ASSISTINDO("Assistindo"),
    CONCLUIDO("Concluído"),
    DESCOBRIR("Descobrir");

    companion object {
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

@Entity(tableName = "midias")
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
    var uidsAprovados: String = ""
) {
    constructor() : this(id = 0)

    fun obterStatusEnum(): StatusMidia = StatusMidia.fromString(status)

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
            "uidsAprovados" to uidsAprovados // 🚀 Guarda na nuvem
        )
    }
}