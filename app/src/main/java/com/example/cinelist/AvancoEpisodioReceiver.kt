package com.example.cinelist

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BroadcastReceiver para gerenciar ações rápidas em notificações do sistema Android,
 * como "+1 Ep Assistido" sem precisar abrir o aplicativo.
 */
class AvancoEpisodioReceiver : BroadcastReceiver() {

    companion object {
        const val ACAO_AVANCAR_EPISODIO = "com.example.cinelist.ACAO_AVANCAR_EPISODIO"
        const val ACAO_DISPENSAR_CONTROLE = "com.example.cinelist.ACAO_DISPENSAR_CONTROLE"

        const val EXTRA_MIDIA_ID = "extra_midia_id"
        const val EXTRA_NOTIFICACAO_ID = "extra_notificacao_id"
        const val EXTRA_TITULO_SERIE = "extra_titulo_serie"

        fun criarPendingIntentAvancar(
            context: Context,
            midiaId: Int,
            notificacaoId: Int,
            tituloSerie: String? = null
        ): PendingIntent {
            val intent = Intent(context, AvancoEpisodioReceiver::class.java).apply {
                action = ACAO_AVANCAR_EPISODIO
                putExtra(EXTRA_MIDIA_ID, midiaId)
                putExtra(EXTRA_NOTIFICACAO_ID, notificacaoId)
                if (tituloSerie != null) putExtra(EXTRA_TITULO_SERIE, tituloSerie)
            }
            return PendingIntent.getBroadcast(
                context,
                notificacaoId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun criarPendingIntentDispensar(
            context: Context,
            notificacaoId: Int
        ): PendingIntent {
            val intent = Intent(context, AvancoEpisodioReceiver::class.java).apply {
                action = ACAO_DISPENSAR_CONTROLE
                putExtra(EXTRA_NOTIFICACAO_ID, notificacaoId)
            }
            return PendingIntent.getBroadcast(
                context,
                notificacaoId + 10000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val acao = intent.action ?: return
        val notificacaoId = intent.getIntExtra(EXTRA_NOTIFICACAO_ID, -1)
        val midiaId = intent.getIntExtra(EXTRA_MIDIA_ID, -1)
        val tituloSerie = intent.getStringExtra(EXTRA_TITULO_SERIE)

        if (acao == ACAO_DISPENSAR_CONTROLE) {
            if (notificacaoId != -1) {
                try {
                    NotificationManagerCompat.from(context).cancel(notificacaoId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }

        if (acao == ACAO_AVANCAR_EPISODIO) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                try {
                    val repository = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        WidgetEntryPoint::class.java
                    ).midiaRepository()

                    val idAlvo = if (midiaId > 0) {
                        midiaId
                    } else {
                        val midias = repository.midiasPessoais.firstOrNull() ?: emptyList()
                        val primeiraAssistindo = midias.firstOrNull {
                            it.status.equals("Assistindo", ignoreCase = true) &&
                                    !it.tipo.equals("Filme", ignoreCase = true)
                        }
                        primeiraAssistindo?.id ?: -1
                    }

                    if (idAlvo > 0) {
                        repository.incrementarEpisodio(idAlvo)

                        // Sincroniza widgets da tela inicial
                        CineListWidget.forcarAtualizacaoWidgets(context)

                        // Cancela notificação após registrar o episódio
                        if (notificacaoId != -1) {
                            try {
                                NotificationManagerCompat.from(context).cancel(notificacaoId)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        withContext(Dispatchers.Main) {
                            val msg = if (!tituloSerie.isNullOrBlank()) {
                                "+1 Ep registrado para $tituloSerie! 🍿"
                            } else {
                                "+1 Episódio registrado com sucesso! 🍿"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
