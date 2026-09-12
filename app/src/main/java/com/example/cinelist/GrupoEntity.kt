package com.example.cinelist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grupos_compartilhados")
data class GrupoEntity(
    @PrimaryKey var grupoId: String,       // O código/ID único da sala (ex: "casal_123" ou gerado automaticamente)
    var nomeGrupo: String = "",          // Nome amigável (ex: "Filmes com a Mô", "Maratona com os Amigos")
    var tipoGrupo: String = "Casal",     // "Casal", "Amigos", "Família"
    var ativo: Boolean = false           // Indica se este grupo está selecionado no momento
) {
    // Construtor vazio para o Firestore se necessário no futuro
    constructor() : this("", "", "Casal", false)

    fun toMap(): Map<String, Any> {
        return mapOf(
            "grupoId" to grupoId,
            "nomeGrupo" to nomeGrupo,
            "tipoGrupo" to tipoGrupo
        )
    }
}