package com.example.cinelist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class LembreteWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CANAL_LEMBRETES = "cinelist_lembretes"
        const val CANAL_ATUALIZACOES = "cinelist_atualizacoes"
    }

    override suspend fun doWork(): Result {
        criarCanais()

        val db = AppDatabase.getDatabase(applicationContext)
        val midiaDao = db.midiaDao()
        val notificacaoDao = db.notificacaoDao()

        return try {
            // 1. LEMBRETE: títulos parados em "Quero Assistir"
            val pendentes = midiaDao.buscarTodasAsMidias().first()
                .filter { it.status == "Quero Assistir" }

            if (pendentes.isNotEmpty()) {
                val jaNotificouHoje = notificacaoDao.contarNotificacaoRecente(
                    idRef = 0,
                    tipo = "LEMBRETE",
                    desde = System.currentTimeMillis() - (20L * 60 * 60 * 1000)
                ) > 0

                if (!jaNotificouHoje) {
                    val titulo = "Você tem títulos esperando 🎬"
                    val mensagem = if (pendentes.size == 1) {
                        "\"${pendentes.first().titulo}\" está na sua lista de Quero Assistir."
                    } else {
                        "${pendentes.size} títulos estão parados na sua lista de Quero Assistir."
                    }

                    notificacaoDao.inserir(
                        NotificacaoEntity(tipo = "LEMBRETE", titulo = titulo, mensagem = mensagem, idReferencia = 0)
                    )
                    enviarNotificacaoSistema(CANAL_LEMBRETES, 1001, titulo, mensagem)
                }
            }

            // 2. ATUALIZAÇÃO: nova versão do app disponível
            val infoAtualizacao = UpdateManager.checarAtualizacaoSilenciosa()
            if (infoAtualizacao != null) {
                val jaNotificouEssaVersao = notificacaoDao.contarNotificacaoRecente(
                    idRef = infoAtualizacao.versaoCode,
                    tipo = "ATUALIZACAO",
                    desde = 0L
                ) > 0

                if (!jaNotificouEssaVersao) {
                    val tituloUpdate = "Nova versão disponível: ${infoAtualizacao.versaoNome}"
                    notificacaoDao.inserir(
                        NotificacaoEntity(
                            tipo = "ATUALIZACAO",
                            titulo = tituloUpdate,
                            mensagem = infoAtualizacao.notasDaVersao,
                            idReferencia = infoAtualizacao.versaoCode
                        )
                    )
                    enviarNotificacaoSistema(CANAL_ATUALIZACOES, 1002, tituloUpdate, infoAtualizacao.notasDaVersao)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun criarCanais() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CANAL_LEMBRETES, "Lembretes Diários", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Avisos sobre filmes e séries pendentes na sua lista"
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(CANAL_ATUALIZACOES, "Atualizações do App", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos de novas versões do CineList"
                }
            )
        }
    }

    private fun enviarNotificacaoSistema(canalId: String, id: Int, titulo: String, mensagem: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, canalId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensagem))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }
}