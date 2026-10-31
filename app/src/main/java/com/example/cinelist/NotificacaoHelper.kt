package com.example.cinelist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificacaoHelper {

    private const val CANAL_GRUPO_ID = "canal_salas_compartilhadas"
    private const val CANAL_SOLICITACOES_ID = "canal_solicitacoes_amizade"
    private const val CANAL_ESTREIAS_ID = "canal_estreias_streaming"

    fun dispararNotificacaoGrupo(context: Context, titulo: String, autor: String, midiaTitulo: String, idRef: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_GRUPO_ID,
                "Salas Compartilhadas".t(),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de novos filmes e séries adicionados aos grupos".t()
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(canal)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            idRef,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val textoCorpo = if (autor.isNotBlank()) "$autor ${"adicionou".t()} \"$midiaTitulo\" ${"ao grupo!".t()}" else "${"Novo item adicionado:".t()} \"$midiaTitulo\""

        val notificacao = NotificationCompat.Builder(context, CANAL_GRUPO_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo.t())
            .setContentText(textoCorpo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(idRef, notificacao)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // NOVO ALERTA: Quando um amigo conclui uma série/filme partilhado
    fun dispararNotificacaoConclusaoAmigo(context: Context, amigoNome: String, tituloMidia: String, idRef: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_GRUPO_ID,
                "Salas Compartilhadas",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(canal)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            idRef + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(context, CANAL_GRUPO_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Alguém terminou de assistir! 🎉".t())
            .setContentText("$amigoNome ${"acabou de marcar".t()} \"$tituloMidia\" ${"como concluído. Que tal ver também?".t()}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(idRef + 1000, notificacao)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun dispararNotificacaoSolicitacaoAmizade(context: Context, remetenteNome: String, idRef: Int = 9999) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_SOLICITACOES_ID,
                "Solicitações de Amizade".t(),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações sonoras quando outro cinéfilo adiciona você".t()
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 200, 350)
            }
            notificationManager.createNotificationChannel(canal)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            idRef,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(context, CANAL_SOLICITACOES_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Novo pedido de amizade! 🍿".t())
            .setContentText("$remetenteNome ${"quer se conectar com você no CineList.".t()}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 350, 200, 350))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(idRef, notificacao)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun dispararNotificacaoExpulsao(context: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "⚠️ Você foi removido da sala pelo Administrador.".t(),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun gerarTextoEstreiaStreaming(tituloMidia: String, provedorNome: String): Pair<String, String> {
        val lang = I18n.idiomaAtivo.lowercase().take(2)
        val titulo = when (lang) {
            "en" -> "🎉 Now on $provedorNome!"
            "es" -> "🎉 ¡Llegó a $provedorNome!"
            "fr" -> "🎉 Arrivé sur $provedorNome !"
            "de" -> "🎉 Jetzt auf $provedorNome!"
            "it" -> "🎉 Disponibile su $provedorNome!"
            "ja" -> "🎉 $provedorNome で配信開始！"
            "ko" -> "🎉 $provedorNome 공개!"
            else -> "🎉 Chegou no $provedorNome!"
        }
        val mensagem = when (lang) {
            "en" -> "\"$tituloMidia\" just arrived on $provedorNome! It's on your watchlist."
            "es" -> "¡\"$tituloMidia\" acaba de estrenarse en $provedorNome! Está en tu lista de espera."
            "fr" -> "« $tituloMidia » vient d'arriver sur $provedorNome ! C'est dans votre liste d'attente."
            "de" -> "„$tituloMidia“ ist jetzt auf $provedorNome verfügbar! Es ist auf deiner Watchlist."
            "it" -> "\"$tituloMidia\" è appena arrivato su $provedorNome! È nella tua lista d'attesa."
            "ja" -> "「$tituloMidia」が $provedorNome で配信開始されました！あなたのリストに入っています。"
            "ko" -> "\"$tituloMidia\"이(가) ${provedorNome}에 공개되었습니다! 보고 싶은 목록에 있습니다."
            else -> "\"$tituloMidia\" acabou de estrear na $provedorNome! Está na sua lista de espera."
        }
        return Pair(titulo, mensagem)
    }

    fun dispararNotificacaoEstreiaStreaming(context: Context, tituloMidia: String, provedorNome: String, idRef: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ESTREIAS_ID,
                "Radar de Estreias nos Streamings".t(),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações quando filmes monitorados chegarem à Netflix, Prime Video, Max, etc.".t()
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(canal)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            idRef,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (tituloFormatado, msgFormatada) = gerarTextoEstreiaStreaming(tituloMidia, provedorNome)

        val notificacao = NotificationCompat.Builder(context, CANAL_ESTREIAS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(tituloFormatado)
            .setContentText(msgFormatada)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msgFormatada))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(idRef, notificacao)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
