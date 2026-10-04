package com.kuaishou.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.app.data.AppPrefs
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页：搜索历史 / 跳转偏好 / 关于应用。
 * 清空最近搜索带确认弹窗；关于应用为二级页入口。
 */
@Composable
fun SettingsScreen(
    padding: PaddingValues,
    prefs: AppPrefs,
    onBack: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferApp by prefs.preferLaunchApp.collectAsState(initial = false)
    val historySize by prefs.history.collectAsState(initial = emptyList())

    // 清空确认弹窗
    var showClearConfirm by remember { mutableStateOf(false) }

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
                "设置",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onBackground,
            )
        }

        // ---- 新用户 ----
        SmallTitle("新用户")
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            BasicComponent(
                title = "新用户引导",
                summary = "4 步学会搜索跳转",
                endActions = {
                    Text(
                        "›",
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    )
                },
                onClick = onOpenTutorial,
            )
        }

        // ---- 搜索历史 ----
        SmallTitle("搜索历史")
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            BasicComponent(
                title = "清空最近搜索",
                summary = if (historySize.isEmpty()) "暂无搜索记录" else "共 ${historySize.size} 条",
                endActions = {
                    Text(
                        "›",
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    )
                },
                onClick = {
                    if (historySize.isEmpty()) {
                        Toast.makeText(context, "暂无搜索记录", Toast.LENGTH_SHORT).show()
                    } else {
                        showClearConfirm = true
                    }
                },
            )
        }

        // ---- 跳转 ----
        SmallTitle("跳转")
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            SwitchPreference(
                checked = preferApp,
                onCheckedChange = { value -> scope.launch { prefs.setPreferLaunchApp(value) } },
                title = "优先唤起App，失败开网页",
                summary = "抖音/小红书/B站将优先尝试打开App，失败时提供网页版选项",
            )
        }

        // ---- 关于 ----
        SmallTitle("关于")
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            BasicComponent(
                title = "关于应用",
                summary = "版本 v1.0.0 · 协议 · 致谢",
                endActions = {
                    Text(
                        "›",
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    )
                },
                onClick = onOpenAbout,
            )
        }
    }

    // ---- 清空确认弹窗 ----
    OverlayDialog(
        show = showClearConfirm,
        title = "清空最近搜索",
        summary = "将删除全部 ${historySize.size} 条搜索记录，此操作不可恢复。",
        onDismissRequest = { showClearConfirm = false },
        onDismissFinished = { showClearConfirm = false },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = "取消",
                onClick = { showClearConfirm = false },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    showClearConfirm = false
                    scope.launch { prefs.clearHistory() }
                    Toast.makeText(context, "已清空最近搜索", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.weight(1f),
            ) { Text("清空") }
        }
    }
}
