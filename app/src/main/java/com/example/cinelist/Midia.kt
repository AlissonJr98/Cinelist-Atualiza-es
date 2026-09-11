package com.example.cinelist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "midias")
data class Midia(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
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
    var plataforma: String = "Outros"
) {
    // Construtor vazio explícito exigido pelo Firestore
    constructor() : this(
        0, 0, "", "Filme", "Quero Assistir", 0, 1, 1, 0, false, "", "", "Não Informado", 0, "Outros"
    )

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
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
            "plataforma" to plataforma
        )
    }
}