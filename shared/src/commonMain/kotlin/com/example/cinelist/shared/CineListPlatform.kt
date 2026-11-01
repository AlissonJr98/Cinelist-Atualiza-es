package com.example.cinelist.shared

expect val nomePlataforma: String

object CineListSharedConfig {
    const val VERSAO_SHARED = "1.0.0"
    val descricao: String
        get() = "CineList Shared Core rodando em $nomePlataforma"
}
