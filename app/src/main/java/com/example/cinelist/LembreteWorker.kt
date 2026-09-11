package com.example.cinelist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class LembreteWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CANAL_LEMBRETES = "cinelist_lembretes"
        const val CANAL_ATUALIZACOES = "cinelist_atualizacoes"
        const val CANAL_LANCAMENTOS = "cinelist_lancamentos"
    }

    override suspend fun doWork(): Result {
        criarCanais()

        val db = AppDatabase.getDatabase(applicationContext)
        val midiaDao = db.midiaDao()
        val notificacaoDao = db.notificacaoDao()

        return try {
            val todasMidias = midiaDao.buscarTodasAsMidias().first()

            // 1. CHECAGEM DE NOVOS EPISÓDIOS (Séries / Animes em "Assistindo")
            val seriesAssistindo = todasMidias.filter { midia ->
                midia.status.equals("Assistindo", ignoreCase = true) &&
                        !midia.tipo.equals("Filme", ignoreCase = true) &&
                        midia.idTmdb > 0
            }

            for (serie in seriesAssistindo) {
                try {
                    val detalhes = RetrofitClient.apiService.obterDetalhesSerieOuAnime(serie.idTmdb)
                    val proximoEp = detalhes.proximoEpisodio

                    if (proximoEp != null && !proximoEp.dataExibicao.isNullOrBlank()) {
                        val formatoData = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val dataEstreia = formatoData.parse(proximoEp.dataExibicao)

                        if (dataEstreia != null) {
                            val hojeCalendar = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val diffDias = TimeUnit.MILLISECONDS.toDays(dataEstreia.time - hojeCalendar.timeInMillis)

                            // Notifica se estreou hoje ou vai estrear nos próximos 7 dias
                            if (diffDias in 0..7) {
                                val idReferenciaEp = (serie.idTmdb * 1000) + (proximoEp.numeroTemporada * 100) + proximoEp.numeroEpisodio

                                val jaNotificouEp = notificacaoDao.contarNotificacaoRecente(
                                    idRef = idReferenciaEp,
                                    tipo = "LANCAMENTO",
                                    desde = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                                ) > 0

                                if (!jaNotificouEp) {
                                    val dataBonita = SimpleDateFormat("dd/MM", Locale.getDefault()).format(dataEstreia)
                                    val quando = if (diffDias == 0L) "estreia hoje!" else "estreia dia $dataBonita"

                                    val tituloNotif = "Novo Episódio: ${serie.titulo} 📺"
                                    val nomeEpFormatado = if (!proximoEp.nome.isNullOrBlank()) " (\"${proximoEp.nome}\")" else ""
                                    val corpoNotif = "T${proximoEp.numeroTemporada}:E${proximoEp.numeroEpisodio}$nomeEpFormatado $quando"

                                    notificacaoDao.inserir(
                                        NotificacaoEntity(
                                            tipo = "LANCAMENTO",
                                            titulo = tituloNotif,
                                            mensagem = corpoNotif,
                                            idReferencia = idReferenciaEp,
                                            dataCriacao = System.currentTimeMillis()
                                        )
                                    )

                                    enviarNotificacaoSistema(
                                        CANAL_LANCAMENTOS,
                                        idReferenciaEp,
                                        tituloNotif,
                                        corpoNotif
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Continua verificando as próximas séries mesmo se uma falhar na API
                    e.printStackTrace()
                }
            }

            // 2. LEMBRETE: títulos parados em "Quero Assistir"
            val pendentes = todasMidias.filter { it.status == "Quero Assistir" }
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

            // 3. ATUALIZAÇÃO: nova versão do app disponível
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
                NotificationChannel(CANAL_LANCAMENTOS, "Novos Episódios e Estreias", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos sobre datas de novos episódios de séries que você está assistindo"
                }
            )

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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }
}