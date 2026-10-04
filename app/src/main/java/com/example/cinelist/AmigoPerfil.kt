package com.example.cinelist

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AmigoPerfil(
    val uid: String = "",
    val nome: String = "",
    val fotoUrl: String = "",
    val bio: String = "",
    val email: String = "",
    val online: Boolean = false,
    val vistoPorUltimo: Long = 0L,
    // 🚀 NOVOS CAMPOS: Redes Sociais
    val instagram: String = "",
    val twitter: String = "",
    val letterboxd: String = ""
) {
    // Mesma tolerância de 45 segundos da sala compartilhada: se o app fechar ou cair a net, vira offline
    val estaRealmenteOnline: Boolean
        get() {
            if (!online) return false
            val diferenca = System.currentTimeMillis() - vistoPorUltimo
            return diferenca in 0..45_000L
        }

    fun obterTextoVistoPorUltimo(): String {
        if (estaRealmenteOnline) return "Online agora"
        if (vistoPorUltimo <= 0L) return "Offline"

        val diferenca = System.currentTimeMillis() - vistoPorUltimo
        if (diferenca < 0L) return "Online agora"

        val minutos = (diferenca / (1000 * 60)).toInt()
        val horas = minutos / 60
        val dias = horas / 24

        return when {
            minutos < 1 -> "Visto agora há pouco"
            minutos == 1 -> "Visto há 1 minuto"
            minutos < 60 -> "Visto há $minutos minutos"
            horas == 1 -> "Visto há 1 hora"
            horas < 24 -> "Visto há $horas horas"
            dias == 1 -> "Visto ontem"
            dias < 7 -> "Visto há $dias dias"
            else -> {
                val formatador = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                "Visto em ${formatador.format(Date(vistoPorUltimo))}"
            }
        }
    }
}

data class SolicitacaoAmizadeRecebida(
    val remetenteUid: String = "",
    val nome: String = "Cinéfilo",
    val fotoUrl: String = "",
    val bio: String = "",
    val dataCriacao: Long = System.currentTimeMillis()
)