package com.kuaishou.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.app.data.ChangeType
import com.kuaishou.app.data.appVersionName
import com.kuaishou.app.data.isChangelogInSync
import com.kuaishou.app.data.versionLogs
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 版本迭代记录页：读取本地 [versionLogs] 数据源，按版本分组渲染
 * 「新增 / 优化 / 修复」三类变更。最新版本在最前。
 * 每个版本条目支持点击头部折叠 / 展开。
 */
@Composable
fun ChangelogScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        CommonTopAppBar("版本迭代记录", onBack)

        Text(
            "记录快搜每一次更新带来的新功能、体验优化与问题修复。点击版本可折叠 / 展开。",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // 日志与构建版本不同步时兜底提示（正常发版流程下不会出现）
        if (!isChangelogInSync) {
            Text(
                "当前版本 v$appVersionName 尚未补充更新日志",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        SmallTitle("更新内容", insideMargin = PaddingValues(start = 16.dp, top = 16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            versionLogs.forEach { log -> VersionLogCard(log) }
        }

        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp))
    }
}

/** 单个版本的卡片：头部（版本号 + 日期 + 箭头，可点击折叠/展开）+ 分类变更列表。 */
@Composable
private fun VersionLogCard(log: com.kuaishou.app.data.VersionLog) {
    var expanded by remember { mutableStateOf(true) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ---- 头部：点击折叠 / 展开 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(
                        start = 16.dp,
                        top = 14.dp,
                        end = 16.dp,
                        bottom = if (expanded) 6.dp else 14.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "v${log.version}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    log.date,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "›",
                    fontSize = 18.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.rotate(if (expanded) 90f else 0f),
                )
            }

            // ---- 变更详情：随展开状态平滑显隐 ----
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 按分类顺序输出：新增 → 优化 → 修复
                    ChangeType.entries.forEach { type ->
                        val items = log.changes.filter { it.type == type }
                        if (items.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                ChangeTag(type)
                                Column(
                                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    items.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            Text(
                                                "•",
                                                fontSize = 13.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                modifier = Modifier.padding(end = 8.dp, top = 1.dp),
                                            )
                                            Text(
                                                item.content,
                                                fontSize = 13.sp,
                                                color = MiuixTheme.colorScheme.onBackground,
                                                lineHeight = 18.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                }
            }
        }
    }
}

/** 分类标签（彩色小药丸）。 */
@Composable
private fun ChangeTag(type: ChangeType) {
    val (bg, fg) = when (type) {
        ChangeType.New -> Color(0xFFE6F4EA) to Color(0xFF1B8A4B)
        ChangeType.Optimize -> Color(0xFFE7F0FF) to Color(0xFF2A6FE0)
        ChangeType.Fix -> Color(0xFFFDECEA) to Color(0xFFD2452F)
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            type.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
        )
    }
}