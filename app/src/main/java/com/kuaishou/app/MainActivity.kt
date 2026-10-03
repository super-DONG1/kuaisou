package com.kuaishou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.engine.JumpEngine
import com.kuaishou.app.ui.HomeScreen
import com.kuaishou.app.ui.PlatformManageScreen
import com.kuaishou.app.ui.SettingsScreen
import com.kuaishou.app.ui.TutorialScreen
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 页面：主页为根，平台管理 / 设置 / 新用户引导为子页（返回键逐级回退）。 */
enum class Page { Home, Platforms, Settings, Tutorial }

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

    val controller = remember { ThemeController(ColorSchemeMode.System) }
    MiuixTheme(controller = controller) {
        var page by rememberSaveable { mutableStateOf(Page.Home) }

        // 子页按返回键逐级回退：引导页回设置，其余子页回主页；主页按返回键退出
        BackHandler(enabled = page != Page.Home) {
            page = if (page == Page.Tutorial) Page.Settings else Page.Home
        }

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
                )
                Page.Tutorial -> TutorialScreen(
                    padding = innerPadding,
                    onBack = { page = Page.Settings },
                )
            }
        }
    }
}
