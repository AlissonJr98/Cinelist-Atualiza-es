package com.example.cinelist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notificacoes")
data class NotificacaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tipo: String, // "LEMBRETE" ou "ATUALIZACAO"
    val titulo: String,
    val mensagem: String,
    val dataCriacao: Long = System.currentTimeMillis(),
    val lida: Boolean = false,
    val idReferencia: Int = 0 // versaoCode (updates) ou 0 (lembrete geral)
)