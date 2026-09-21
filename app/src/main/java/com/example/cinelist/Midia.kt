package com.example.cinelist

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID


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
    var status: String = "Quero Assistir",
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
    var avaliacoesGrupo: Map<String, AvaliacaoMembro> = emptyMap()
) {
    // Construtor vazio explícito exigido pelo Firestore
    constructor() : this(
        0, "", 0, "", "Filme", "Quero Assistir", 0, 1, 1, 0, false, "", "", "Não Informado", 0, "Outros", false, "Geral", false, "", "", emptyMap()
    )

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "uuid" to uuid,
            "idTmdb" to idTmdb,
            "titulo" to titulo,
            "tipo" to tipo,
            "status" to status,
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
            "avaliacoesGrupo" to avaliacoesGrupo.mapValues { it.value.toMap() }
        )
    }
}