package com.example.cinelist

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class PlaylistCine(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val descricao: String = "",
    val icone: String = "🎬",
    val corHex: String = "#7C4DFF",
    val criadoEm: Long = System.currentTimeMillis(),
    val midiaIdsTmdb: List<Int> = emptyList()
)

object PlaylistsManager {

    private const val PREFS_NAME = "cinelist_playlists_prefs"
    private const val KEY_PLAYLISTS = "playlists_json"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retorna todas as playlists do usuário, criando coleções temáticas padrão se for a primeira execução.
     */
    fun obterPlaylists(context: Context): List<PlaylistCine> {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_PLAYLISTS, null)

        if (json.isNullOrBlank()) {
            val padroes = listOf(
                PlaylistCine(
                    id = "mindfuck_classics",
                    nome = "Finais Que Explodem a Cabeça",
                    descricao = "Reviravoltas e plots que te deixam sem ar",
                    icone = "🤯",
                    corHex = "#EC4899"
                ),
                PlaylistCine(
                    id = "chorar_domingo",
                    nome = "Para Desidratar de Chorar",
                    descricao = "Histórias emocionantes e comoventes",
                    icone = "😭",
                    corHex = "#3B82F6"
                ),
                PlaylistCine(
                    id = "scifi_filosofico",
                    nome = "Ficção Científica Filosófica",
                    descricao = "Obras sobre o cosmos, IA e a existência humana",
                    icone = "🚀",
                    corHex = "#8B5CF6"
                ),
                PlaylistCine(
                    id = "pipoca_pura",
                    nome = "Para Assistir Comendo Pipoca",
                    descricao = "Ação, aventura e diversão sem compromisso",
                    icone = "🍿",
                    corHex = "#F59E0B"
                )
            )
            salvarPlaylists(context, padroes)
            return padroes
        }

        return try {
            val type = object : TypeToken<List<PlaylistCine>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun salvarPlaylists(context: Context, playlists: List<PlaylistCine>) {
        val json = gson.toJson(playlists)
        getPrefs(context).edit().putString(KEY_PLAYLISTS, json).apply()
    }

    fun criarPlaylist(context: Context, nome: String, descricao: String, icone: String, corHex: String): PlaylistCine {
        val nova = PlaylistCine(
            nome = nome,
            descricao = descricao,
            icone = icone.ifBlank { "🎬" },
            corHex = corHex.ifBlank { "#7C4DFF" }
        )
        val atuais = obterPlaylists(context).toMutableList()
        atuais.add(0, nova)
        salvarPlaylists(context, atuais)
        return nova
    }

    fun alternarMidiaNaPlaylist(context: Context, playlistId: String, idTmdb: Int): Boolean {
        val atuais = obterPlaylists(context).toMutableList()
        val index = atuais.indexOfFirst { it.id == playlistId }
        if (index == -1) return false

        val playlist = atuais[index]
        val listaIds = playlist.midiaIdsTmdb.toMutableList()
        val adicionou = if (idTmdb in listaIds) {
            listaIds.remove(idTmdb)
            false
        } else {
            listaIds.add(idTmdb)
            true
        }

        atuais[index] = playlist.copy(midiaIdsTmdb = listaIds)
        salvarPlaylists(context, atuais)
        return adicionou
    }

    fun excluirPlaylist(context: Context, playlistId: String) {
        val atuais = obterPlaylists(context).filter { it.id != playlistId }
        salvarPlaylists(context, atuais)
    }
}
