package com.kuaishou.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.engine.JumpEngine
import com.kuaishou.app.ui.AboutScreen
import com.kuaishou.app.ui.HomeScreen
import com.kuaishou.app.ui.PlatformManageScreen
import com.kuaishou.app.ui.SettingsScreen
import com.kuaishou.app.ui.TutorialScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.window.WindowDialog

/** 页面：主页为根，平台管理 / 设置 / 新用户引导 / 关于应用为子页（返回键逐级回退）。 */
enum class Page { Home, Platforms, Settings, Tutorial, About }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KuaishouApp()
        }
    }
}

@Composable
fun KuaishouApp() {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val engine = remember { JumpEngine(context) }
    val scope = rememberCoroutineScope()

    val controller = remember { ThemeController(ColorSchemeMode.System) }
    MiuixTheme(controller = controller) {
        var page by rememberSaveable { mutableStateOf(Page.Home) }

        // 首次启动：未同意用户协议则弹窗（同意后方可使用）
        var showAgreement by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!prefs.agreementAccepted.first()) showAgreement = true
        }

        // 子页按返回键逐级回退：引导/关于回设置，其余子页回主页；主页按返回键退出
        BackHandler(enabled = page != Page.Home) {
            page = when (page) {
                Page.Tutorial, Page.About -> Page.Settings
                else -> Page.Home
            }
        }

        // 协议弹窗期间拦截返回键（必须同意或退出）
        BackHandler(enabled = showAgreement) { /* 不关闭 */ }

        Scaffold(containerColor = MiuixTheme.colorScheme.background) { innerPadding ->
            when (page) {
                Page.Home -> HomeScreen(
                    padding = innerPadding,
                    prefs = prefs,
                    engine = engine,
                    onOpenPlatforms = { page = Page.Platforms },
                    onOpenSettings = { page = Page.Settings },
                )
                Page.Platforms -> PlatformManageScreen(
                    padding = innerPadding,
                    prefs = prefs,
                    onBack = { page = Page.Home },
                )
                Page.Settings -> SettingsScreen(
                    padding = innerPadding,
                    prefs = prefs,
                    onBack = { page = Page.Home },
                    onOpenTutorial = { page = Page.Tutorial },
                    onOpenAbout = { page = Page.About },
                )
                Page.Tutorial -> TutorialScreen(
                    padding = innerPadding,
                    onBack = { page = Page.Settings },
                )
                Page.About -> AboutScreen(
                    padding = innerPadding,
                    prefs = prefs,
                    onBack = { page = Page.Settings },
                )
            }
        }

        // ---- 首次使用协议弹窗（窗口层，独立于 Scaffold）----
        if (showAgreement) {
            WindowDialog(
                show = true,
                title = "用户协议与隐私说明",
                summary = "请在使用前阅读并同意以下内容",
                onDismissRequest = { /* 必须同意或退出，点击外部不关闭 */ },
                onDismissFinished = { },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 380.dp)
                        .padding(top = 4.dp),
                ) {
                    Text(
                        "1. 快搜是一款搜索跳转工具，仅将你输入的关键词组装为对应平台的搜索链接，并交由系统唤起已安装的 App 或打开网页浏览器完成搜索。\n" +
                            "2. 关键词仅用于构造跳转链接，快搜不缓存、不上传、不分享你的任何搜索内容。\n" +
                            "3. 快搜不包含广告，不收集个人身份信息。\n\n" +
                            "隐私说明\n" +
                            "1. 数据存储：最近搜索记录、默认平台、平台开关等设置仅保存在本机应用数据中，不会上传到任何服务器。\n" +
                            "2. 权限：快搜不申请任何敏感权限。\n" +
                            "3. 删除：可在「设置 - 搜索历史」一键清空；卸载应用即删除全部本地数据。\n" +
                            "4. 第三方：跳转目标 App 的搜索行为受其自身隐私政策约束。\n\n" +
                            "你可在「设置 - 关于应用 - 用户协议与隐私说明」中随时撤回同意。",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            text = "不同意并退出",
                            onClick = { (context as? Activity)?.finish() },
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                scope.launch { prefs.setAgreementAccepted(true) }
                                showAgreement = false
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.weight(1f),
                        ) { Text("同意并继续") }
                    }
                }
            }
        }
    }
}
