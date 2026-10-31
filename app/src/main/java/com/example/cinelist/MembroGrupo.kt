package com.example.cinelist

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MembroGrupo(
    val uid: String = "",
    val nome: String = "Membro",
    val fotoUrl: String = "",
    val online: Boolean = false,
    val vistoPorUltimo: Long = 0L,
    val assistindoAgoraTitulo: String = "",
    val assistindoAgoraEpisodio: String = ""
) {
    // Tolerância reduzida para 45 segundos. Se o aplicativo fechar forçado,
    // o status passa para offline automaticamente em 45s.
    val estaRealmenteOnline: Boolean
        get() {
            if (!online) return false
            val diferenca = System.currentTimeMillis() - vistoPorUltimo
            return diferenca in 0..90_000L
        }

    val estaAssistindoAlgo: Boolean
        get() = estaRealmenteOnline && assistindoAgoraTitulo.isNotBlank()

    fun obterTextoVistoPorUltimo(): String {
        if (estaRealmenteOnline) return "Online agora".t()
        if (vistoPorUltimo <= 0L) return "Offline".t()

        val diferenca = System.currentTimeMillis() - vistoPorUltimo
        if (diferenca < 0L) return "Online agora".t()

        val minutos = (diferenca / (1000 * 60)).toInt()
        val horas = minutos / 60
        val dias = horas / 24

        return when {
            minutos < 1 -> "Visto agora há pouco".t()
            minutos == 1 -> "Visto há 1 minuto".t()
            minutos < 60 -> "${"Visto há".t()} $minutos ${"minutos".t()}"
            horas == 1 -> "Visto há 1 hora".t()
            horas < 24 -> "${"Visto há".t()} $horas ${"horas".t()}"
            dias == 1 -> "Visto ontem".t()
            dias < 7 -> "${"Visto há".t()} $dias ${"dias".t()}"
            else -> {
                val formatador = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                "${"Visto em".t()} ${formatador.format(Date(vistoPorUltimo))}"
            }
        }
    }
}