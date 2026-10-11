package com.kuaishou.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 顶栏（全局统一返回键样式）
        CommonTopAppBar("新用户引导", onBack)

        SmallTitle("4 步学会快搜")
        StepItem(1, "输入关键词", "在顶部搜索框输入想搜的内容，键盘自动弹出；点右侧清空按钮可一键重输。")
        StepItem(2, "选中平台", "点平台图标选中目标平台（抖音/小红书/B站…），图标大，单手就能点。")
        StepItem(3, "一键直达", "点击键盘上方的悬浮胶囊按钮，或按键盘搜索键，立即跳转。")
        StepItem(4, "搜完即走", "看完成果直接返回快搜，全程不刷首页信息流，不被内容带走。")

        SmallTitle("小贴士")
        TipsCard(
            listOf(
                "长按平台卡片可设为默认平台，之后直接按回车即可搜索。",
                "手机没装该 App 时自动改用网页版；唤起失败会弹出抽屉，可复制关键词或打开首页。",
                "主页可进入平台管理：开关平台、调整顺序；设置里可关闭\"优先唤起App\"。",
                "从别的 App 分享文字给快搜，会自动填入搜索框，点平台即搜。",
                "把\"快搜\"小部件放到桌面，点平台图标直接打开快搜搜索。",
            )
        )
    }
}

/**
 * 单个步骤卡片：浅灰圆角底，左侧 32dp 蓝色数字圆圈与右侧文字首行垂直对齐。
 */
@Composable
private fun StepItem(index: Int, title: String, description: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF9F9F9))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF3482FF)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$index",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                description,
                fontSize = 13.sp,
                color = Color(0xFF666666),
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * 小贴士区块：整体放入一个大圆角浅灰卡片，行前以浅蓝小圆点作项目符号。
 */
@Composable
private fun TipsCard(tips: List<String>) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tips.forEach { TipRow(it) }
    }
}

@Composable
private fun TipRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF3482FF)),
        )
        Text(
            text,
            fontSize = 13.sp,
            color = Color(0xFF666666),
            lineHeight = 19.sp,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}