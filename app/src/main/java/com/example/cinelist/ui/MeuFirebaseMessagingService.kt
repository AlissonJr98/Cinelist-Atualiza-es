package com.example.cinelist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MeuFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val grupoId = remoteMessage.data["grupoId"] ?: ""
        val autorUid = remoteMessage.data["autorUid"] ?: ""
        val tipoAcao = remoteMessage.data["tipoAcao"] ?: ""
        val alvoId = remoteMessage.data["alvoId"] ?: ""
        val meuUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // 🚀 1. SE FUI EU QUE ENVIEI A MENSAGEM, NÃO MOSTRAMOS NOTIFICAÇÃO PARA MIM MESMO
        if (autorUid.isNotBlank() && meuUid.isNotBlank() && autorUid == meuUid) {
            return
        }

        // 🚀 2. VERIFICAÇÃO EXCLUSIVA DO MUTE DESTE GRUPO ESPECÍFICO (Isolado do App todo)
        if (grupoId.isNotBlank()) {
            val prefsMuteChat = getSharedPreferences("CineListMutePrefs", Context.MODE_PRIVATE)
            val grupoMutado = prefsMuteChat.getBoolean("mute_$grupoId", false)
            if (grupoMutado) {
                return // Ignora a notificação apenas se ESTE chat específico estiver silenciado
            }
        }

        remoteMessage.notification?.let { notificacao ->
            mostrarNotificacao(
                titulo = notificacao.title ?: "CineList",
                mensagem = notificacao.body ?: "Nova mensagem",
                tipoAcao = tipoAcao,
                alvoId = alvoId
            )
        }
    }

    private fun mostrarNotificacao(titulo: String, mensagem: String, tipoAcao: String, alvoId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("TIPO_ACAO", tipoAcao)
            putExtra("ALVO_ID", alvoId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, Random.nextInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canalId = "chat_cinelist_canal"
        val somPadrao = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val construtorNotificacao = NotificationCompat.Builder(this, canalId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setAutoCancel(true)
            .setSound(somPadrao)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                canalId,
                "Avisos do Chat",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(canal)
        }

        notificationManager.notify(Random.nextInt(), construtorNotificacao.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}