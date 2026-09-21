package com.example.cinelist

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

        val titulo = remoteMessage.notification?.title
            ?: remoteMessage.data["titulo"]
            ?: "CineList"

        val mensagem = remoteMessage.notification?.body
            ?: remoteMessage.data["mensagem"]
            ?: "Você tem uma nova atualização."

        val tipo = remoteMessage.data["tipo"] ?: "GERAL"
        val autor = remoteMessage.data["remetenteNome"] ?: ""

        if (tipo == "SOLICITACAO_AMIZADE") {
            NotificacaoHelper.dispararNotificacaoSolicitacaoAmizade(
                context = applicationContext,
                remetenteNome = if (autor.isNotBlank()) autor else "Um cinéfilo"
            )
        } else {
            exibirNotificacaoPadrao(titulo, mensagem)
        }

        // Salva a notificação localmente no banco para aparecer na aba Notificações
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificacaoRepository.inserir(
                    NotificacaoEntity(
                        titulo = titulo,
                        mensagem = mensagem,
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

    private fun exibirNotificacaoPadrao(titulo: String, mensagem: String) {
        val canalId = "canal_salas_compartilhadas"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, canalId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}