package com.kuaishou.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/** 危险操作确认按钮：红底白字 */
private val DestructiveRed = Color(0xFFFF4D4F)
/** 取消按钮：浅灰 */
private val CancelGrey = Color(0xFF999999)
/** 正文说明：浅灰 */
private val MessageGrey = Color(0xFF666666)

/**
 * 破坏性 / 不可逆操作的二级确认弹窗（MIUIX 风格）。
 *
 * 防误触约束：点击遮罩层或按系统返回键一律等同于「取消」，
 * 只有明确点击红色确认按钮才会触发 [onConfirm]，且仅触发一次。
 */
@Composable
fun DestructiveConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = title,
        // 遮罩点击 / 返回键 → 走取消分支，绝不执行清理
        onDismissRequest = onDismiss,
        onDismissFinished = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                message,
                fontSize = 13.sp,
                color = MessageGrey,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(textColor = CancelGrey),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        // 先执行清理（内部为异步协程，立即返回）再关闭弹窗，
                        // 弹窗随即消失，不会与数据变化打架造成闪烁
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        color = DestructiveRed,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(confirmText)
                }
            }
        }
    }
}