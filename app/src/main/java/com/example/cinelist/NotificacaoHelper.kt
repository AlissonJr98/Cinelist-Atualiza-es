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

    fun dispararNotificacaoGrupo(context: Context, titulo: String, autor: String, midiaTitulo: String, idRef: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_GRUPO_ID,
                "Salas Compartilhadas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de novos filmes e séries adicionados aos grupos"
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

        val textoCorpo = if (autor.isNotBlank()) "$autor adicionou \"$midiaTitulo\" ao grupo!" else "Novo item adicionado: \"$midiaTitulo\""

        val notificacao = NotificationCompat.Builder(context, CANAL_GRUPO_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
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
            .setContentTitle("Alguém terminou de assistir! 🎉")
            .setContentText("$amigoNome acabou de marcar \"$tituloMidia\" como concluído. Que tal ver também?")
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
                "Solicitações de Amizade",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações sonoras quando outro cinéfilo adiciona você"
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
            .setContentTitle("Novo pedido de amizade! 🍿")
            .setContentText("$remetenteNome quer se conectar com você no CineList.")
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
                "⚠️ Você foi removido da sala pelo Administrador.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}