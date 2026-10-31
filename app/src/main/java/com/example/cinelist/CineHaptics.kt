package com.example.cinelist

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Utilitário central de feedback háptico e micro-interações táteis do CineList.
 * Fornece respostas físicas refinadas e responsivas para eventos de toque,
 * gestos de swipe, avanço de episódios, favoritos e celebração de matches.
 */
object CineHaptics {

    private fun obterVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Micro-clique tátil sutil (ex: seleção de estrelas de avaliação, rotação, troca de abas).
     */
    fun tick(context: Context) {
        val vibrator = obterVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Clique tátil nítido de confirmação (ex: marcar +1 episódio, favoritar título).
     */
    fun click(context: Context) {
        val vibrator = obterVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Confirmação com impacto mais firme (ex: conclusão de swipe horizontal, marcar como concluído).
     */
    fun confirm(context: Context) {
        val vibrator = obterVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Celebração com pulso duplo (ex: Deu Match no CineMatch!, conquista desbloqueada).
     */
    fun celebrate(context: Context) {
        val vibrator = obterVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 35, 50, 45)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val timings = longArrayOf(0, 35, 50, 45)
                vibrator.vibrate(timings, -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Alerta ou remoção com duplo toque rápido (ex: remover do catálogo, swipe de exclusão).
     */
    fun warning(context: Context) {
        val vibrator = obterVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 20, 40, 25)
                val amplitudes = intArrayOf(0, 140, 0, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val timings = longArrayOf(0, 20, 40, 25)
                vibrator.vibrate(timings, -1)
            }
        } catch (_: Exception) {}
    }
}
