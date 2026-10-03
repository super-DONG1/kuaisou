package com.kuaishou.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 新用户引导页：教新用户如何在 3 秒内完成一次搜索跳转。
 */
@Composable
fun TutorialScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 顶栏与首页品牌区等高：Miuix 返回键 + 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = MiuixIcons.Back,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            }
            Text(
                "新用户引导",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onBackground,
            )
        }

        SmallTitle("4 步学会快搜")
        StepCard(1, "输入关键词", "在顶部搜索框输入想搜的内容，键盘自动弹出；点右侧清空按钮可一键重输。")
        StepCard(2, "选中平台", "点平台图标选中目标平台（抖音/小红书/B站…），图标大，单手就能点。")
        StepCard(3, "一键直达", "点搜索框右侧的搜索按钮，或直接按键盘搜索键，立即跳转到该 App 的搜索结果页。")
        StepCard(4, "搜完即走", "看完成果直接返回快搜，全程不刷首页信息流，不被内容带走。")

        SmallTitle("小贴士")
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            TipRow("长按平台卡片可设为默认平台，之后直接按回车即可搜索。")
            TipRow("手机没装该 App 时自动改用网页版；唤起失败会弹出抽屉，可复制关键词或打开首页。")
            TipRow("主页可进入平台管理：开关平台、调整顺序；设置里可关闭\"优先唤起App\"。")
        }
    }
}

@Composable
private fun StepCard(step: Int, title: String, desc: String) {
    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$step",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    desc,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun TipRow(text: String) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text("·", fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
        Text(
            text,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
