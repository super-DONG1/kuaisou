package com.kuaishou.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全局统一顶栏：返回键 + 标题。
 * 返回键样式（以设置页为基准，所有二级页共用）：
 *  - 触控热区 48dp；图标 24dp，颜色中灰 #666666，粗细适中；
 *  - 顶栏左边距 8dp（与设置页完全一致），标题 22sp 加粗，与返回键垂直居中。
 * 仅负责返回键与标题的统一样式，不改变各页点击逻辑。
 */
@Composable
fun CommonTopAppBar(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = MiuixIcons.Back,
                contentDescription = "返回",
                tint = Color(0xFF666666),
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onBackground,
        )
    }
}