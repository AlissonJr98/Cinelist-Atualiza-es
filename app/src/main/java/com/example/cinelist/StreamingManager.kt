package com.example.cinelist

import android.content.Context
import android.content.SharedPreferences

data class ServicoStreaming(
    val id: Int,
    val nome: String,
    val pacoteAndroid: String,
    val corHex: Long,
    val emoji: String
)

object StreamingManager {

    private const val PREFS_NAME = "StreamingPrefs"
    private const val KEY_MEUS_STREAMINGS = "meus_streamings_ids"

    val SERVICOS = listOf(
        ServicoStreaming(id = 8, nome = "Netflix", pacoteAndroid = "com.netflix.mediaclient", corHex = 0xFFE50914, emoji = "🔴"),
        ServicoStreaming(id = 119, nome = "Prime Video", pacoteAndroid = "com.amazon.avod.thirdpartyclient", corHex = 0xFF00A8E1, emoji = "📦"),
        ServicoStreaming(id = 337, nome = "Disney+", pacoteAndroid = "com.disney.disneyplus", corHex = 0xFF113CCF, emoji = "✨"),
        ServicoStreaming(id = 1899, nome = "Max", pacoteAndroid = "com.wbd.stream", corHex = 0xFF002BE7, emoji = "🟣"),
        ServicoStreaming(id = 350, nome = "Apple TV+", pacoteAndroid = "com.apple.atv.android.appletv", corHex = 0xFF333333, emoji = "🍏"),
        ServicoStreaming(id = 531, nome = "Paramount+", pacoteAndroid = "com.cbs.app", corHex = 0xFF0064FF, emoji = "🏔️"),
        ServicoStreaming(id = 283, nome = "Crunchyroll", pacoteAndroid = "com.crunchyroll.crunchyroid", corHex = 0xFFF47521, emoji = "🍥"),
        ServicoStreaming(id = 307, nome = "Globoplay", pacoteAndroid = "com.globo.globotv", corHex = 0xFFFF0033, emoji = "🌐")
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun obterMeusStreamingsIds(context: Context): Set<Int> {
        val stringSet = getPrefs(context).getStringSet(KEY_MEUS_STREAMINGS, emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun salvarMeusStreamingsIds(context: Context, ids: Set<Int>) {
        val stringSet = ids.map { it.toString() }.toSet()
        getPrefs(context).edit().putStringSet(KEY_MEUS_STREAMINGS, stringSet).apply()
    }

    fun obterQueryMeusStreamings(context: Context): String? {
        val ids = obterMeusStreamingsIds(context)
        if (ids.isEmpty()) return null
        return ids.joinToString("|")
    }

    fun midiaDisponivelNosMeusStreamings(context: Context, plataformaMidia: String): Boolean {
        if (plataformaMidia.isBlank()) return false
        val meusIds = obterMeusStreamingsIds(context)
        if (meusIds.isEmpty()) return true // Se não filtrou, tudo é válido

        val nomesMeus = SERVICOS.filter { it.id in meusIds }.map { it.nome.lowercase() }
        val plataformaLower = plataformaMidia.lowercase()

        return nomesMeus.any { nome ->
            plataformaLower.contains(nome) ||
            (nome == "prime video" && (plataformaLower.contains("prime") || plataformaLower.contains("amazon"))) ||
            (nome == "max" && (plataformaLower.contains("max") || plataformaLower.contains("hbo"))) ||
            (nome == "apple tv+" && (plataformaLower.contains("apple")))
        }
    }
}
