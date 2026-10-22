package com.example.cinelist

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gerenciador local do Radar de Lançamentos nos Streamings Brasileiros.
 * Armazena os títulos que o usuário deseja monitorar e consulta o TMDB para verificar
 * se já estrearam na Netflix, Prime Video, Max, Disney+, Globoplay, etc.
 */
object RadarStreamingManager {
    private const val PREFS_NAME = "cinelist_radar_streaming"
    private const val KEY_ALERTAS = "alertas_streaming_json"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retorna a lista de todos os títulos monitorados.
     */
    fun obterAlertas(context: Context): List<AlertaRadarStreaming> {
        val json = getPrefs(context).getString(KEY_ALERTAS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AlertaRadarStreaming>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Salva a lista de títulos monitorados.
     */
    private fun salvarAlertas(context: Context, lista: List<AlertaRadarStreaming>) {
        val json = gson.toJson(lista)
        getPrefs(context).edit().putString(KEY_ALERTAS, json).apply()
    }

    /**
     * Verifica se um ID do TMDB já está sendo monitorado.
     */
    fun isMonitorado(context: Context, idTmdb: Int): Boolean {
        if (idTmdb <= 0) return false
        val lista = obterAlertas(context)
        return lista.any { it.idTmdb == idTmdb }
    }

    /**
     * Adiciona ou remove um título do radar.
     * Retorna true se foi adicionado, false se foi removido.
     */
    fun alternarAlerta(context: Context, novoAlerta: AlertaRadarStreaming): Boolean {
        if (novoAlerta.idTmdb <= 0) return false
        val listaAtual = obterAlertas(context).toMutableList()
        val index = listaAtual.indexOfFirst { it.idTmdb == novoAlerta.idTmdb }

        return if (index != -1) {
            listaAtual.removeAt(index)
            salvarAlertas(context, listaAtual)
            false
        } else {
            listaAtual.add(0, novoAlerta)
            salvarAlertas(context, listaAtual)
            true
        }
    }

    /**
     * Consulta os provedores no TMDB para o Brasil ("BR") e atualiza o status do título.
     */
    suspend fun verificarDisponibilidade(context: Context, alerta: AlertaRadarStreaming): AlertaRadarStreaming = withContext(Dispatchers.IO) {
        if (alerta.idTmdb <= 0) return@withContext alerta

        try {
            val ehSerie = alerta.tipo.contains("série", ignoreCase = true) || alerta.tipo.contains("anime", ignoreCase = true)
            val resposta = if (ehSerie) {
                RetrofitClient.apiService.obterProvedoresSerieOuAnime(alerta.idTmdb)
            } else {
                RetrofitClient.apiService.obterProvedoresFilme(alerta.idTmdb)
            }

            val provedoresBr = resposta.resultados?.get("BR")?.streamingAssinatura ?: emptyList()
            val nomesProvedores = provedoresBr.map { it.nomeProvedor }.distinct()

            val agoraDisponivel = nomesProvedores.isNotEmpty()
            val alertaAtualizado = alerta.copy(
                jaDisponivel = agoraDisponivel,
                provedoresEncontrados = nomesProvedores,
                ultimaVerificacao = System.currentTimeMillis()
            )

            // Se acabou de estrear e antes não estava disponível, notifica
            if (agoraDisponivel && !alerta.jaDisponivel) {
                val provedorDestaque = nomesProvedores.firstOrNull() ?: "Streaming"
                NotificacaoHelper.dispararNotificacaoEstreiaStreaming(
                    context = context,
                    tituloMidia = alerta.titulo,
                    provedorNome = provedorDestaque,
                    idRef = alerta.idTmdb
                )
            }

            // Atualiza na persistência
            val lista = obterAlertas(context).toMutableList()
            val idx = lista.indexOfFirst { it.idTmdb == alerta.idTmdb }
            if (idx != -1) {
                lista[idx] = alertaAtualizado
                salvarAlertas(context, lista)
            }

            alertaAtualizado
        } catch (e: Exception) {
            e.printStackTrace()
            alerta
        }
    }

    /**
     * Atualiza todos os títulos cadastrados no Radar de Lançamentos.
     */
    suspend fun sincronizarTodosAlertas(context: Context): List<AlertaRadarStreaming> = withContext(Dispatchers.IO) {
        val lista = obterAlertas(context)
        val listaAtualizada = mutableListOf<AlertaRadarStreaming>()
        for (alerta in lista) {
            val atualizado = verificarDisponibilidade(context, alerta)
            listaAtualizada.add(atualizado)
        }
        listaAtualizada
    }
}
