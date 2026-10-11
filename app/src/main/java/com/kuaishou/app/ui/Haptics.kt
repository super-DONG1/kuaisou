package com.kuaishou.app.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * 触感反馈工具：直接使用系统 Vibrator 服务。
 * Compose 的 LocalHapticFeedback 在部分真机上不触发震动，改用 Vibrator 更可靠。
 */
fun hapticTick(context: Context, hapticsOn: Boolean) {
    if (!hapticsOn) return
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(25)
    }
}