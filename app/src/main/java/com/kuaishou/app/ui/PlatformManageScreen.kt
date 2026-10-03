package com.kuaishou.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.Platform
import com.kuaishou.app.data.PlatformRegistry
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 平台管理页：已启用平台（开关 + 上下移动排序）+ 待选平台池 + 恢复默认。
 */
@Composable
fun PlatformManageScreen(
    padding: PaddingValues,
    prefs: AppPrefs,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val enabledOrder by prefs.enabledPlatformIds.collectAsState(initial = emptyList())
    val all = PlatformRegistry.all
    val byId = rememberPlatformMap(all)

    val enabled = if (enabledOrder.isEmpty()) {
        PlatformRegistry.defaultEnabled
    } else {
        enabledOrder.mapNotNull { byId[it] }
    }
    val enabledIds = enabled.map { it.id }
    val candidates = all.filter { it.id !in enabledIds }

    fun saveOrder(newList: List<Platform>) {
        scope.launch { prefs.setEnabledOrder(newList.map { it.id }) }
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState()),
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
                "平台",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onBackground,
            )
        }

        // ---- 已启用 ----
        SmallTitle("已启用 · 上下移动排序")
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            enabled.forEachIndexed { index, platform ->
                SwitchPreference(
                    checked = true,
                    onCheckedChange = { keep ->
                        if (!keep) saveOrder(enabled.filter { it.id != platform.id })
                    },
                    title = platform.name,
                    startAction = { BrandDot(platform) },
                    endActions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "▲",
                                fontSize = 13.sp,
                                color = if (index > 0) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = index > 0) {
                                        saveOrder(enabled.toMutableList().apply {
                                            val t = removeAt(index); add(index - 1, t)
                                        })
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                            Text(
                                "▼",
                                fontSize = 13.sp,
                                color = if (index < enabled.lastIndex) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = index < enabled.lastIndex) {
                                        saveOrder(enabled.toMutableList().apply {
                                            val t = removeAt(index); add(index + 1, t)
                                        })
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    },
                )
            }
        }

        // ---- 待选平台池 ----
        if (candidates.isNotEmpty()) {
            SmallTitle("待选平台")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                candidates.forEach { platform ->
                    SwitchPreference(
                        checked = false,
                        onCheckedChange = { add ->
                            if (add) saveOrder(enabled + platform)
                        },
                        title = platform.name,
                        summary = "开启后加入主页宫格",
                        startAction = { BrandDot(platform) },
                    )
                }
            }
        }

        // ---- 恢复默认 ----
        TextButton(
            text = "恢复默认",
            onClick = { scope.launch { prefs.setEnabledOrder(PlatformRegistry.defaultEnabled.map { it.id }) } },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 14.dp),
        )
    }
}

/** 平台 App 图标（列表行首小图标）。 */
@Composable
private fun BrandDot(platform: Platform) {
    Image(
        painter = painterResource(platform.iconRes),
        contentDescription = platform.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(5.dp)),
    )
}

@Composable
private fun rememberPlatformMap(list: List<Platform>) =
    androidx.compose.runtime.remember(list) { list.associateBy { it.id } }
