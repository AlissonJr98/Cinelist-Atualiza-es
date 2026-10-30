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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class CineFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificacaoRepository: NotificacaoRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        salvarTokenNoFirestore(token)
    }

    private fun salvarTokenNoFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            FirebaseFirestore.getInstance()
                .collection("usuarios_publicos")
                .document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val meuUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val autorUid = remoteMessage.data["autorUid"]
            ?: remoteMessage.data["remetenteUid"]
            ?: ""

        // 🚀 1. Se fui eu quem enviei a mensagem, ignora para não notificar a mim mesmo
        if (autorUid.isNotBlank() && meuUid.isNotBlank() && autorUid == meuUid) {
            return
        }

        val grupoId = remoteMessage.data["grupoId"] ?: ""
        // 🚀 2. Verifica se a sala específica foi silenciada pelo usuário
        if (grupoId.isNotBlank()) {
            val prefsMute = getSharedPreferences("CineListMutePrefs", Context.MODE_PRIVATE)
            if (prefsMute.getBoolean("mute_$grupoId", false)) {
                return // Ignora notificação desta sala específica
            }
        }

        val autorNome = remoteMessage.data["autorNome"]
            ?: remoteMessage.data["remetenteNome"]
            ?: ""

        val titulo = remoteMessage.notification?.title
            ?: remoteMessage.data["titulo"]
            ?: if (autorNome.isNotBlank()) "${"Nova mensagem de".t()} $autorNome" else "CineList"

        val mensagem = remoteMessage.notification?.body
            ?: remoteMessage.data["mensagem"]
            ?: remoteMessage.data["texto"]
            ?: "Você recebeu uma nova atualização no grupo.".t()

        val tipo = remoteMessage.data["tipo"] ?: remoteMessage.data["tipoAcao"] ?: "GERAL"
        val alvoId = remoteMessage.data["alvoId"] ?: remoteMessage.data["ALVO_ID"] ?: grupoId

        if (tipo == "SOLICITACAO_AMIZADE" || tipo == "AMIZADE_ACEITA") {
            NotificacaoHelper.dispararNotificacaoSolicitacaoAmizade(
                context = applicationContext,
                remetenteNome = if (autorNome.isNotBlank()) autorNome else "Um cinéfilo".t()
            )
        } else {
            exibirNotificacaoSistema(titulo.t(), mensagem.t(), grupoId, tipo, alvoId)
        }

        // Salva a notificação localmente no banco para aparecer na aba Notificações
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificacaoRepository.inserir(
                    NotificacaoEntity(
                        titulo = titulo.t(),
                        mensagem = mensagem.t(),
                        tipo = tipo,
                        dataCriacao = System.currentTimeMillis(),
                        lida = false
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun exibirNotificacaoSistema(
        titulo: String,
        mensagem: String,
        grupoId: String,
        tipo: String,
        alvoId: String = ""
    ) {
        val canalId = if (tipo == "SOLICITACAO_AMIZADE" || tipo == "AMIZADE_ACEITA") {
            "canal_solicitacoes_amizade"
        } else {
            "canal_salas_compartilhadas"
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                canalId,
                if (canalId == "canal_solicitacoes_amizade") "Solicitações de Amizade" else "Salas e Chat Compartilhados",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos em tempo real do CineList"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
            }
            notificationManager.createNotificationChannel(canal)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("GRUPO_ID", grupoId)
            putExtra("grupoId", grupoId)
            putExtra("TIPO_ACAO", tipo)
            putExtra("tipoAcao", tipo)
            putExtra("ALVO_ID", alvoId)
            putExtra("alvoId", alvoId)
        }

        val idNotif = Random.nextInt(1000, 999999)
        val pendingIntent = PendingIntent.getActivity(
            this,
            idNotif,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val somPadrao = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, canalId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(somPadrao)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(idNotif, builder.build())
    }
}
