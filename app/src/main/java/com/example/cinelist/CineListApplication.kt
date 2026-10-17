package com.example.cinelist

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CineListApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        criarCanaisDeNotificacao()
    }

    private fun criarCanaisDeNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

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

            manager?.createNotificationChannel(canalSalas)
            manager?.createNotificationChannel(canalSolicitacoes)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
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
    }
}
