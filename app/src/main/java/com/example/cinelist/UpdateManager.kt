package com.example.cinelist

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object UpdateManager {

    const val URL_JSON_PADRAO = AtualizadorGitHub.URL_VERSAO_JSON
    private const val CANAL_ID = "canal_atualizacoes_cinelist"
    private const val ID_NOTIFICACAO = 2001

    suspend fun checarAtualizacao(urlEndpointJson: String = URL_JSON_PADRAO): InfoAtualizacao? {
        return withContext(Dispatchers.IO) {
            try {
                val app = CineListApplication.instance ?: return@withContext null
                val res = AtualizadorGitHub.verificar(app)
                res.info
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun checarAtualizacaoSilenciosa(urlEndpointJson: String = URL_JSON_PADRAO): InfoAtualizacao? {
        return withContext(Dispatchers.IO) {
            try {
                val app = CineListApplication.instance ?: return@withContext null
                val res = AtualizadorGitHub.verificar(app)
                if (res.disponivel) res.info else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun exibirNotificacaoAtualizacao(context: Context, info: InfoAtualizacao) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Atualizações do CineList",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações quando novas versões estiverem disponíveis"
            }
            notificationManager.createNotificationChannel(canal)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("abrir_atualizacao", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumoNovidades = info.notas.ifBlank { "Uma nova versão com melhorias está disponível." }

        val notificacao = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nova Versão CineList v${info.versaoNome}!")
            .setContentText("Toque para conferir as novidades e atualizar.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Novidades da v${info.versaoNome}:\n$resumoNovidades"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(ID_NOTIFICACAO, notificacao)
    }

    fun baixarEInstalarApk(context: Context, apkUrl: String, nomeArquivo: String = "cinelist_update.apk") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intentPermissao = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intentPermissao)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Permita a instalação de fontes desconhecidas.", Toast.LENGTH_LONG).show()
                }
                return
            }
        }

        val destino = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            nomeArquivo
        )

        if (destino.exists()) {
            destino.delete()
        }

        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle("Atualizando CineList")
            .setDescription("Baixando nova versão do aplicativo...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, nomeArquivo)
            .setMimeType("application/vnd.android.package-archive")

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val handler = Handler(Looper.getMainLooper())
        var receiverDesregistrado = false

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val idRecebido = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (idRecebido == downloadId) {
                    if (!receiverDesregistrado) {
                        receiverDesregistrado = true
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {}
                    }
                    instalarApk(context, destino)
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )

        // Limpeza de segurança por timeout (30 min) para evitar vazamento se o download for cancelado ou abortado
        handler.postDelayed({
            if (!receiverDesregistrado) {
                receiverDesregistrado = true
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) {}
            }
        }, 30 * 60 * 1000L)
    }

    private fun instalarApk(context: Context, arquivoApk: File) {
        if (!arquivoApk.exists() || arquivoApk.length() == 0L) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Erro: Arquivo do APK não encontrado ou incompleto.", Toast.LENGTH_LONG).show()
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intentPermissao = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intentPermissao)
                return
            }
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                arquivoApk
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val resolvedorIntentList = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resolvedorIntentList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Erro ao abrir o instalador: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
