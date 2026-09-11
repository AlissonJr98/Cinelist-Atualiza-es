package com.example.cinelist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "midias")
data class Midia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idTmdb: Int = 0,
    val titulo: String = "",
    val tipo: String = "Filme",
    val status: String = "Quero Assistir",
    val nota: Int = 0,
    val temporadaAtual: Int = 1,
    val episodioAtual: Int = 1,
    val minutoParado: Int = 0,
    val jaEncerrou: Boolean = false,
    val sinopse: String = "",
    val imagemCapa: String = "",
    val genero: String = "Não Informado",
    val duracaoTotal: Int = 0,
    val plataforma: String = "Outros"
) {
    // Converte a entidade para Map garantindo compatibilidade com o Firestore
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