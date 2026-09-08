package com.example.cinelist

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AtualizacaoWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val update = UpdateManager.checarAtualizacaoSilenciosa()
            if (update != null) {
                // 1. Emite o alerta nativo na barra de notificações
                UpdateManager.exibirNotificacaoAtualizacao(appContext, update)

                // 2. Acessa o banco Room diretamente via AppDatabase
                val database = AppDatabase.getDatabase(appContext)
                val notificacaoDao = database.notificacaoDao()

                val jaRegistrada = notificacaoDao.contarNotificacaoRecente(
                    idRef = update.versaoCode,
                    tipo = "ATUALIZACAO",
                    desde = 0L
                ) > 0

                if (!jaRegistrada) {
                    val corpo = if (update.notasDaVersao.isNotBlank()) {
                        "Novidades da versão ${update.versaoNome}:\n${update.notasDaVersao}"
                    } else {
                        "Uma nova versão (${update.versaoNome}) com melhorias e correções está disponível."
                    }

                    notificacaoDao.inserir(
                        NotificacaoEntity(
                            tipo = "ATUALIZACAO",
                            titulo = "Nova Versão v${update.versaoNome} Disponível",
                            mensagem = corpo,
                            dataCriacao = System.currentTimeMillis(),
                            lida = false,
                            idReferencia = update.versaoCode
                        )
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}