package com.example.cinelist

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class InfoAtualizacao(
    val versaoCode: Int,
    val versaoNome: String,
    val urlApk: String,
    val notasDaVersao: String
)

object UpdateManager {

    const val URL_JSON_PADRAO = "https://raw.githubusercontent.com/AlissonJr98/Cinelist-Atualiza-es/main/version.json"

    suspend fun checarAtualizacao(urlEndpointJson: String = URL_JSON_PADRAO): InfoAtualizacao? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlEndpointJson)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val resposta = reader.readText()
                    reader.close()

                    val json = JSONObject(resposta)

                    val versionCode = json.optInt("versionCode", 0)
                    val versionName = json.optString("versionName", "")
                    val apkUrl = json.optString("apkUrl", "")
                    val changelog = json.optString("changelog", "Melhorias gerais e correções.")

                    InfoAtualizacao(versionCode, versionName, apkUrl, changelog)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun checarAtualizacaoSilenciosa(urlEndpointJson: String = URL_JSON_PADRAO): InfoAtualizacao? {
        val info = checarAtualizacao(urlEndpointJson) ?: return null
        return if (info.versaoCode > BuildConfig.VERSION_CODE) info else null
    }

    fun baixarEInstalarApk(context: Context, apkUrl: String, nomeArquivo: String = "cinelist_update.apk") {
        // Checagem prévia de permissão de instalação de apps desconhecidos no Android 8.0+
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
            .setDestinationUri(Uri.fromFile(destino))
            .setMimeType("application/vnd.android.package-archive")

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val idRecebido = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (idRecebido == downloadId) {
                    instalarApk(context, destino)
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun instalarApk(context: Context, arquivoApk: File) {
        if (!arquivoApk.exists()) return

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
    }
}