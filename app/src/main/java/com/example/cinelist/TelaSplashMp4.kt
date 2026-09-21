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

@Composable
fun TelaSplashMp4(
    onSplashConcluida: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .clickable { onSplashConcluida() }, // Permite saltar ao tocar na tela
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

                                setOnPreparedListener { mp ->
                                    // Ajuste dinâmico de escala (Center Crop perfeito sem cortes indesejados)
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
                                }

                                setOnCompletionListener {
                                    onSplashConcluida()
                                }

                                prepareAsync()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onSplashConcluida()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                        mediaPlayer?.release()
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