package com.example.cinelist

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CineListApplication : Application(), ImageLoaderFactory {

    companion object {
        var instance: CineListApplication? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            IdiomaAppManager.inicializar(this)
        } catch (e: Throwable) {
            Log.w("CineListApplication", "Falha ao inicializar idioma do app", e)
        }

        // 🛡️ Log defensivo de exceções e persistência de crash para diagnóstico e auto-recuperação
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CineListFatal", "Exceção fatal capturada na thread ${thread.name}: ${throwable.message}", throwable)
            try {
                val sw = java.io.StringWriter()
                val pw = java.io.PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTraceStr = sw.toString()

                val crashFile = java.io.File(filesDir, "ultimo_crash.txt")
                crashFile.writeText("Thread: ${thread.name}\nTimestamp: ${System.currentTimeMillis()}\nErro: ${throwable.message}\n\n$stackTraceStr")

                val prefs = getSharedPreferences("CineListCrashPrefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("houve_crash_recente", true)
                    .putString("ultimo_erro", throwable.message ?: "Desconhecido")
                    .putString("ultimo_stacktrace", stackTraceStr)
                    .putLong("ultimo_timestamp", System.currentTimeMillis())
                    .apply()
            } catch (e: Throwable) {
                Log.e("CineListFatal", "Falha ao gravar arquivo de crash", e)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            criarCanaisDeNotificacao()
        } catch (e: Throwable) {
            Log.w("CineListApplication", "Falha ao registrar canais de notificação na inicialização", e)
        }
    }

    private fun criarCanaisDeNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val canalSalas = NotificationChannel(
                "canal_salas_compartilhadas",
                "Salas e Chat Compartilhados",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de mensagens e atividades nas salas"
                enableLights(true)
                enableVibration(true)
            }

            val canalSolicitacoes = NotificationChannel(
                "canal_solicitacoes_amizade",
                "Solicitações de Amizade",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pedidos de amizade e interações sociais"
                enableLights(true)
                enableVibration(true)
            }

            manager.createNotificationChannel(canalSalas)
            manager.createNotificationChannel(canalSolicitacoes)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return try {
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache_cinelist"))
                        .maxSizeBytes(150L * 1024 * 1024)
                        .build()
                }
                .respectCacheHeaders(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build()
        } catch (e: Throwable) {
            Log.w("CineListApplication", "Falha ao instanciar ImageLoader customizado, usando padrão", e)
            ImageLoader(this)
        }
    }
}
