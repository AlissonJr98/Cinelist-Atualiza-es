package com.example.cinelist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "midias")
data class Midia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idTmdb: Int = 0,
    val titulo: String,
    val tipo: String,
    val status: String,
    val nota: Int,
    val temporadaAtual: Int = 1,
    val episodioAtual: Int = 1,
    val minutoParado: Int = 0,
    val jaEncerrou: Boolean = false,
    val sinopse: String,
    val imagemCapa: String,
    val genero: String = "Não Informado",
    val duracaoTotal: Int = 0,
    val plataforma: String = "Outros" // 🚀 NOVO: Guarda o nome do streaming principal (ex: "Netflix")
)