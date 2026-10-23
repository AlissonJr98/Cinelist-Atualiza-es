package com.example.cinelist

import android.content.Context

data class GeneroInfo(
    val id: Int,
    val nome: String,
    val iconeEmoji: String
)

object GeneroPreferencesManager {

    val GENEROS = listOf(
        GeneroInfo(28, "Ação", "💥"),
        GeneroInfo(12, "Aventura", "🗺️"),
        GeneroInfo(16, "Animação & Anime", "🎨"),
        GeneroInfo(35, "Comédia", "😂"),
        GeneroInfo(80, "Crime", "🕵️"),
        GeneroInfo(99, "Documentário", "📽️"),
        GeneroInfo(18, "Drama", "🎭"),
        GeneroInfo(10751, "Família", "👨‍👩‍👧"),
        GeneroInfo(14, "Fantasia", "🧙‍♂️"),
        GeneroInfo(878, "Ficção Científica", "🚀"),
        GeneroInfo(9648, "Mistério & Suspense", "🔍"),
        GeneroInfo(10749, "Romance", "❤️"),
        GeneroInfo(27, "Terror", "👻")
    )

    private const val PREFS_NAME = "ConfiguracoesPerfil"
    private const val KEY_GENEROS = "generos_favoritos_ids"

    fun obterGenerosFavoritosIds(context: Context): Set<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_GENEROS, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun salvarGenerosFavoritosIds(context: Context, ids: Set<Int>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_GENEROS, ids.map { it.toString() }.toSet()).apply()
    }
}
