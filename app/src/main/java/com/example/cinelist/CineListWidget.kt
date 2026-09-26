package com.example.cinelist

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.core.net.toUri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun midiaRepository(): MidiaRepository
}

class CineListWidget : AppWidgetProvider() {

    companion object {
        const val ACAO_MAIS_UM_EP = "com.example.cinelist.ACAO_MAIS_UM_EP"
        const val ACAO_TROCAR_SERIE = "com.example.cinelist.ACAO_TROCAR_SERIE"

        fun forcarAtualizacaoWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, CineListWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

            val intent = Intent(context, CineListWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            atualizarWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val prefs = context.getSharedPreferences("CineListWidgetPrefs", Context.MODE_PRIVATE)

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val repository = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).midiaRepository()
            val midias = repository.midiasPessoais.firstOrNull() ?: emptyList()
            val listaAssistindo = midias.filter {
                it.status.equals("Assistindo", ignoreCase = true) &&
                        !it.tipo.equals("Filme", ignoreCase = true)
            }

            if (listaAssistindo.isNotEmpty()) {
                val chavePref = "indice_serie_$appWidgetId"
                var indiceAtual = prefs.getInt(chavePref, 0)

                if (intent.action == ACAO_TROCAR_SERIE) {
                    indiceAtual = (indiceAtual + 1) % listaAssistindo.size
                    prefs.edit().putInt(chavePref, indiceAtual).apply()
                } else if (intent.action == ACAO_MAIS_UM_EP) {
                    if (indiceAtual >= listaAssistindo.size) indiceAtual = 0
                    val serieAlvo = listaAssistindo[indiceAtual]
                    repository.incrementarEpisodio(serieAlvo.id)
                }
            }

            // Atualiza todos os widgets após a ação
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val esteWidget = ComponentName(context, CineListWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(esteWidget)
            for (id in ids) {
                atualizarWidget(context, appWidgetManager, id)
            }
        }
    }

    private fun atualizarWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.cine_list_widget_layout)

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val repository = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).midiaRepository()
                val midias = repository.midiasPessoais.firstOrNull() ?: emptyList()
                val listaAssistindo = midias.filter {
                    it.status.equals("Assistindo", ignoreCase = true) &&
                            !it.tipo.equals("Filme", ignoreCase = true)
                }

                val prefs = context.getSharedPreferences("CineListWidgetPrefs", Context.MODE_PRIVATE)
                val chavePref = "indice_serie_$appWidgetId"
                var indice = prefs.getInt(chavePref, 0)
                if (indice >= listaAssistindo.size) indice = 0

                val assistindo = if (listaAssistindo.isNotEmpty()) listaAssistindo[indice] else null

                if (assistindo != null) {
                    views.setTextViewText(R.id.widget_titulo_serie, assistindo.titulo)
                    views.setTextViewText(R.id.widget_subtitulo_ep, "T${assistindo.temporadaAtual} • Ep ${assistindo.episodioAtual}")

                    // Carrega a imagem da capa em segundo plano
                    if (assistindo.imagemCapa.isNotBlank()) {
                        try {
                            val url = URL(assistindo.imagemCapa)
                            val bitmap = withContext(Dispatchers.IO) {
                                BitmapFactory.decodeStream(url.openConnection().getInputStream())
                            }
                            if (bitmap != null) {
                                views.setImageViewBitmap(R.id.widget_poster, bitmap)
                            }
                        } catch (e: Exception) {
                            views.setImageViewResource(R.id.widget_poster, android.R.drawable.ic_menu_gallery)
                        }
                    } else {
                        views.setImageViewResource(R.id.widget_poster, android.R.drawable.ic_menu_gallery)
                    }

                    // Clique para abrir detalhes da série
                    val idNavegacao = if (assistindo.idTmdb != 0) assistindo.idTmdb else assistindo.id
                    val intentApp = Intent(Intent.ACTION_VIEW, "cinelist://detalhes/$idNavegacao/${assistindo.tipo}".toUri(), context, MainActivity::class.java)
                    val pendingApp = PendingIntent.getActivity(context, assistindo.id, intentApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_titulo_serie, pendingApp)
                    views.setOnClickPendingIntent(R.id.widget_subtitulo_ep, pendingApp)
                    views.setOnClickPendingIntent(R.id.widget_poster, pendingApp)

                    // Botão +1 Episódio
                    val intentMaisUm = Intent(context, CineListWidget::class.java).apply {
                        action = ACAO_MAIS_UM_EP
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val pendingMaisUm = PendingIntent.getBroadcast(context, appWidgetId * 10 + 1, intentMaisUm, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_btn_mais_um, pendingMaisUm)

                    // Botão Trocar de Série
                    val intentTrocar = Intent(context, CineListWidget::class.java).apply {
                        action = ACAO_TROCAR_SERIE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val pendingTrocar = PendingIntent.getBroadcast(context, appWidgetId * 10 + 2, intentTrocar, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_btn_trocar, pendingTrocar)

                } else {
                    views.setTextViewText(R.id.widget_titulo_serie, "CineList")
                    views.setTextViewText(R.id.widget_subtitulo_ep, "Nenhuma série ativa")
                    views.setImageViewResource(R.id.widget_poster, android.R.drawable.ic_menu_gallery)

                    val intentApp = Intent(context, MainActivity::class.java)
                    val pendingApp = PendingIntent.getActivity(context, 0, intentApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_titulo_serie, pendingApp)
                    views.setOnClickPendingIntent(R.id.widget_subtitulo_ep, pendingApp)
                    views.setOnClickPendingIntent(R.id.widget_btn_mais_um, pendingApp)
                    views.setOnClickPendingIntent(R.id.widget_btn_trocar, pendingApp)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}