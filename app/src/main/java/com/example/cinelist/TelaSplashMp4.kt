package com.example.cinelist

import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Composable
fun TelaSplashMp4(
    onSplashConcluida: () -> Unit
) {
    var jaConcluiu by remember { mutableStateOf(false) }

    fun finalizarSplash() {
        if (!jaConcluiu) {
            jaConcluiu = true
            onSplashConcluida()
        }
    }

    // 🛡️ TIMEOUT DE SEGURANÇA: Garante que NENHUM celular (Motorola, Xiaomi, Samsung)
    // fique preso na splash caso o decoder de vídeo do hardware falhe ou demore
    LaunchedEffect(Unit) {
        delay(2200)
        finalizarSplash()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .clickable { finalizarSplash() }, // Permite saltar ao tocar na tela
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val textureView = TextureView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                var mediaPlayer: MediaPlayer? = null

                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                        try {
                            mediaPlayer = MediaPlayer().apply {
                                val uri = Uri.parse("android.resource://${ctx.packageName}/raw/splash_animada")
                                setDataSource(ctx, uri)
                                setSurface(Surface(surfaceTexture))
                                isLooping = false

                                setOnErrorListener { _, what, extra ->
                                    android.util.Log.w("TelaSplashMp4", "Aviso de reprodução de vídeo ($what, $extra), avançando para o app...")
                                    finalizarSplash()
                                    true
                                }

                                setOnPreparedListener { mp ->
                                    try {
                                        // Ajuste dinâmico de escala (Center Crop perfeito sem distorções)
                                        val videoWidth = mp.videoWidth.toFloat()
                                        val videoHeight = mp.videoHeight.toFloat()

                                        if (videoWidth > 0 && videoHeight > 0 && width > 0 && height > 0) {
                                            val viewRatio = width.toFloat() / height.toFloat()
                                            val videoRatio = videoWidth / videoHeight

                                            val scaleX: Float
                                            val scaleY: Float

                                            if (viewRatio > videoRatio) {
                                                scaleX = 1f
                                                scaleY = viewRatio / videoRatio
                                            } else {
                                                scaleX = videoRatio / viewRatio
                                                scaleY = 1f
                                            }

                                            val matrix = android.graphics.Matrix().apply {
                                                setScale(scaleX, scaleY, width / 2f, height / 2f)
                                            }
                                            textureView.setTransform(matrix)
                                        }

                                        mp.start()
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                        finalizarSplash()
                                    }
                                }

                                setOnCompletionListener {
                                    finalizarSplash()
                                }

                                prepareAsync()
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            finalizarSplash()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                        try {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                        } catch (_: Throwable) {}
                        mediaPlayer = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                }

                textureView
            }
        )
    }
}
