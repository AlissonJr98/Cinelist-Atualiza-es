package com.example.cinelist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Player inline moderno para exibição fluida de trailers do YouTube dentro do CineList,
 * com suporte nativo a Picture-in-Picture (janela flutuante), controles de tela cheia e
 * redirecionamento opcional para o app oficial.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InlineTrailerPlayer(
    chaveTrailer: String,
    modifier: Modifier = Modifier,
    onFechar: () -> Unit
) {
    val context = LocalContext.current
    val emModoPip = LocalPipMode.current

    Surface(
        shape = RoundedCornerShape(if (emModoPip) 0.dp else 16.dp),
        color = Color.Black,
        border = if (emModoPip) null else BorderStroke(1.5.dp, Color(0xFFE50914).copy(alpha = 0.85f)),
        modifier = modifier
            .fillMaxWidth()
            .then(if (emModoPip) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webChromeClient = WebChromeClient()
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; overflow: hidden; }
                                    iframe { width: 100%; height: 100%; border: none; }
                                </style>
                            </head>
                            <body>
                                <iframe 
                                    src="https://www.youtube.com/embed/$chaveTrailer?autoplay=1&playsinline=1&rel=0&modestbranding=1&enablejsapi=1" 
                                    frameborder="0" 
                                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                                    allowfullscreen>
                                </iframe>
                            </body>
                            </html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                    }
                },
                onRelease = { webView ->
                    try {
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.onPause()
                        webView.destroy()
                    } catch (_: Throwable) {}
                },
                modifier = Modifier.fillMaxSize()
            )

            // Controles flutuantes sobre o player (somente quando não estiver no modo PiP)
            if (!emModoPip) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE50914)
                        ) {
                            Box(modifier = Modifier.size(8.dp))
                        }
                        Text(
                            text = "Trailer Oficial".t(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Botão PiP (Janela Flutuante)
                        IconButton(
                            onClick = {
                                CineHaptics.click(context)
                                (context as? Activity)?.let { PipHelper.entrarModoPip(it) }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Janela Flutuante (PiP)".t(),
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Botão Abrir no App do YouTube
                        IconButton(
                            onClick = {
                                CineHaptics.click(context)
                                val intentApp = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$chaveTrailer")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                val intentNav = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$chaveTrailer")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intentApp)
                                } catch (_: Exception) {
                                    context.startActivity(intentNav)
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Abrir no YouTube".t(),
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Botão Fechar Player
                        IconButton(
                            onClick = {
                                CineHaptics.click(context)
                                onFechar()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE50914).copy(alpha = 0.85f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar Trailer".t(),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
