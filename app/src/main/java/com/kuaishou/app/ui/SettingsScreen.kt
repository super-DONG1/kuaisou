package com.kuaishou.app.ui

import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.app.data.AppPrefs
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页：搜索历史 / 跳转偏好 / 关于。
 */
@Composable
fun SettingsScreen(
    padding: PaddingValues,
    prefs: AppPrefs,
    onBack: () -> Unit,
    onOpenTutorial: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferApp by prefs.preferLaunchApp.collectAsState(initial = false)
    val historySize by prefs.history.collectAsState(initial = emptyList())

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
                    scope.launch { prefs.clearHistory() }
                    Toast.makeText(context, "已清空最近搜索", Toast.LENGTH_SHORT).show()
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
            BasicComponent(title = "版本 v1.0.0")
            Text(
                "快搜 —— 绕过首页，直达搜索",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 14.dp),
            )
        }
    }
}
