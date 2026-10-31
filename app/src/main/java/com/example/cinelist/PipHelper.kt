package com.example.cinelist

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * CompositionLocal que indica para os componentes Compose se o aplicativo está atualmente
 * sendo exibido em uma janela flutuante Picture-in-Picture (PiP).
 */
val LocalPipMode = compositionLocalOf { false }

/**
 * Utilitário para controle do modo Picture-in-Picture (PiP) nativo do Android.
 */
object PipHelper {

    var isInPipMode by mutableStateOf(false)

    /**
     * Solicita a transição para o modo Picture-in-Picture com proporção cinematográfica 16:9.
     */
    fun entrarModoPip(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                activity.enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }
}
