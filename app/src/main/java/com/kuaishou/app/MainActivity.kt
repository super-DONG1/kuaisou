package com.kuaishou.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.PRIVACY_POLICY
import com.kuaishou.app.data.THEME_DARK
import com.kuaishou.app.data.THEME_LIGHT
import com.kuaishou.app.data.USER_AGREEMENT
import com.kuaishou.app.data.SearchRepository
import com.kuaishou.app.engine.JumpEngine
import com.kuaishou.app.ui.AboutScreen
import com.kuaishou.app.ui.ChangelogScreen
import com.kuaishou.app.ui.HomeViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuaishou.app.ui.GitHubUpdateChecker
import com.kuaishou.app.ui.HomeScreen
import com.kuaishou.app.ui.PlatformManageScreen
import com.kuaishou.app.ui.SettingsScreen
import com.kuaishou.app.ui.UpdateScreen
import com.kuaishou.app.ui.TutorialScreen
import com.kuaishou.app.ui.WidgetSettingsScreen
import com.kuaishou.app.widget.WidgetHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
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

/** 来自系统分享 / 桌面小部件的启动参数，经 MainActivity 解析后交给主页消费（只消费一次）。 */
const val EXTRA_PLATFORM_ID = "com.kuaishou.app.extra.PLATFORM_ID"
/** 桌面小组件「最近搜索」标签：把关键词直接填入搜索框。 */
const val EXTRA_QUERY = "com.kuaishou.app.extra.QUERY"
/** 桌面小组件任意点击：要求主页强制聚焦搜索框并弹出键盘（即便没有预选平台 / 关键词）。 */
const val EXTRA_FOCUS = "com.kuaishou.app.extra.FOCUS"

class LaunchHandoff {
    val query = mutableStateOf<String?>(null)
    val platformId = mutableStateOf<String?>(null)
    /**
     * 是否需要在主页弹出键盘的「一次性」标记，由主页消费后复位。
     * 冷启动（图标/分享进入）与小组件点击（携带 EXTRA_FOCUS）时置位；
     * 小组件「自动弹键盘」关闭时不会带 EXTRA_FOCUS，故此处不会置位。
     */
    val shouldShowKeyboard = mutableStateOf(false)
    /** 自增计数：每次小组件/分享进入 +1，应用据此回到主页，避免在子页残留过期的启动参数。 */
    val openHomeRequest = mutableStateOf(0)
}

/** 页面：主页为根，平台管理 / 设置 / 新用户引导 / 关于应用为子页（返回键逐级回退）。 */
enum class Page { Home, Platforms, Settings, Tutorial, About, Update, Changelog, WidgetSettings }

/** 页面导航层级，用于判断过渡动画的前进 / 返回方向。 */
private fun pageDepth(page: Page): Int = when (page) {
    Page.Home -> 0
    Page.Settings -> 1
    Page.Platforms -> 1
    Page.Tutorial -> 2
    Page.About -> 2
    Page.Update -> 2
    Page.WidgetSettings -> 2
    Page.Changelog -> 3
}

class MainActivity : ComponentActivity() {
    private val handoff = LaunchHandoff()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 开启边到边：让 IME(键盘) insets 在所有品牌 / 系统版本上都被稳定派发。
        // 不开启时 Android 15 以下处于「窗口适配系统栏」模式，部分国产 ROM（OPPO / 华为 /
        // 荣耀 / vivo 等）不下发 IME insets，导致依赖 WindowInsets.ime 的「用X搜索」悬浮按钮
        // 判定为「键盘不可见」而不显示（仅 MIUI 会宽松地上报）。targetSdk 37 在 Android 15+
        // 已被系统强制边到边，显式开启只是把低版本行为对齐，页面均通过 Scaffold innerPadding 消费系统栏。
        enableEdgeToEdge()
        handleIntent(intent)
        // 冷启动默认弹键盘；由小组件进入时不强制，弹键盘与否取决于小组件「自动弹键盘」开关
        if (intent?.action != Intent.ACTION_VIEW) handoff.shouldShowKeyboard.value = true
        setContent {
            KuaishouApp(handoff)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * 解析系统分享文本与桌面小组件参数写入 handoff 供主页一次性消费。
     * 小组件 PendingIntent 统一用 ACTION_VIEW，与桌面图标（ACTION_MAIN）区分开。
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        // 来自小组件即视为一次「打开主页」请求：即使未带任何 extra（自动弹键盘关闭的平台图标）也应回主页
        var handled = intent.action == Intent.ACTION_VIEW
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotBlank() }?.let {
                handoff.query.value = it
                handled = true
            }
        }
        intent.getStringExtra(EXTRA_PLATFORM_ID)?.takeIf { it.isNotBlank() }?.let {
            handoff.platformId.value = it
            handled = true
        }
        intent.getStringExtra(EXTRA_QUERY)?.takeIf { it.isNotBlank() }?.let {
            handoff.query.value = it
            handled = true
        }
        if (intent.getBooleanExtra(EXTRA_FOCUS, false)) {
            handoff.shouldShowKeyboard.value = true
            handled = true
        }
        if (!handled) return
        handoff.openHomeRequest.value++
        // 清掉已消费的 extra：Activity 因配置变更/进程重建会用同一 intent 再跑一次 onCreate，
        // 不清除会二次填入关键词、重复预选平台。
        intent.removeExtra(Intent.EXTRA_TEXT)
        intent.removeExtra(EXTRA_PLATFORM_ID)
        intent.removeExtra(EXTRA_QUERY)
        intent.removeExtra(EXTRA_FOCUS)
    }
}

@Composable
fun KuaishouApp(handoff: LaunchHandoff) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val engine = remember { JumpEngine(context) }
    val repo = remember { SearchRepository(context, prefs) }
    val vm: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repo) as T
        },
    )
    val scope = rememberCoroutineScope()

    val themeMode by prefs.themeMode.collectAsState(initial = com.kuaishou.app.data.THEME_SYSTEM)
    // 主题外观：跟随系统 / 浅色 / 深色（mode 变化时重建 ThemeController 生效）
    val controller = remember(themeMode) {
        ThemeController(
            when (themeMode) {
                THEME_LIGHT -> ColorSchemeMode.Light
                THEME_DARK -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.System
            }
        )
    }
    MiuixTheme(controller = controller) {
        // Miuix 0.9.4 的弹窗/抽屉组件需要 NavigationBackHandler 运行环境：
        // 在应用根创建根 dispatcher（显式 parent=null）并提供 LocalNavigationEventDispatcherOwner
        val navigationEventOwner = rememberNavigationEventDispatcherOwner(parent = null)
        CompositionLocalProvider(
            LocalNavigationEventDispatcherOwner provides navigationEventOwner,
            // 文本选中高亮：Miuix 未提供该色值，Compose 默认 Unspecified 会回退到
            // 兼容性旧色，在浅/深色主题下都难以辨认。这里显式给出主题蓝浅色底纹
            // + 主题色选择手柄，使全App（含搜索框输入文本）选中后都有清晰可见的高亮。
            LocalTextSelectionColors provides TextSelectionColors(
                handleColor = MiuixTheme.colorScheme.primary,
                backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.28f),
            ),
        ) {
            var page by rememberSaveable { mutableStateOf(Page.Home) }
            // 平台管理页的返回目标：从主页进入回主页，从设置进入回设置
            var platformsFrom by rememberSaveable { mutableStateOf(Page.Home) }

            // 小组件/分享进入：立即回到主页消费启动参数。
            // 否则参数会滞留在 handoff 中，等用户从设置/平台管理返回时才被应用（跨页残留 + 延迟弹键盘）。
            LaunchedEffect(handoff.openHomeRequest.value) {
                if (handoff.openHomeRequest.value > 0) page = Page.Home
            }

            // 首次启动：未同意用户协议则弹窗（同意后方可使用）
            var showAgreement by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (!prefs.agreementAccepted.first()) showAgreement = true
            }

            // 启动后按频率与提醒方式静默检查更新（手动模式不自动检查）
            LaunchedEffect(Unit) {
                GitHubUpdateChecker.autoCheck(context.applicationContext, prefs)
            }

            // 小组件内容同步：改默认平台 / 平台管理 / 搜索历史后，桌面已添加的小组件需重绘，
            // 否则组件会一直显示旧数据。集中在此观察，而不是在各写入点散落调用。
            LaunchedEffect(Unit) {
                val watched: List<Flow<Any?>> = listOf(
                    prefs.defaultPlatformId,
                    prefs.enabledPlatformIds,
                    prefs.customPlatforms,
                    prefs.history,
                    prefs.platformClicks,
                )
                combine(watched) { it }
                    .collectLatest {
                        // 一次搜索会连写历史与点击计数，用 collectLatest 合并短时间内的连续改动
                        delay(400)
                        WidgetHelper.updateAllWidgets(context)
                    }
            }

            // 子页按返回键逐级回退：平台管理回其来源页；引导/关于回设置；其余子页回主页；主页按返回键退出
            BackHandler(enabled = page != Page.Home) {
                page = when (page) {
                    Page.Platforms -> platformsFrom
                    Page.Tutorial, Page.About, Page.Update, Page.WidgetSettings -> Page.Settings
                    Page.Changelog -> Page.About
                    else -> Page.Home
                }
            }

            // 协议弹窗期间拦截返回键（必须同意或退出）
            BackHandler(enabled = showAgreement) { /* 不关闭 */ }

            Scaffold(containerColor = MiuixTheme.colorScheme.background) { innerPadding ->
                // 全局页面过渡：水平平推 + 淡入淡出（MIUIX 风格，280ms，平滑缓动）
                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.fillMaxSize(),
                    label = "pageTransition",
                    transitionSpec = {
                        val duration = 280
                        val easing = FastOutSlowInEasing
                        val forward = pageDepth(targetState) >= pageDepth(initialState)
                        if (forward) {
                            // 前进：新页面从右侧滑入 + 淡入；旧页面向左滑出 + 淡出
                            (slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(duration, easing = easing),
                            ) + fadeIn(animationSpec = tween(duration, easing = easing)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        targetOffsetX = { -it },
                                        animationSpec = tween(duration, easing = easing),
                                    ) + fadeOut(animationSpec = tween(duration, easing = easing)),
                                )
                        } else {
                            // 返回：新页面从左侧滑入 + 淡入；旧页面向右滑出 + 淡出
                            (slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(duration, easing = easing),
                            ) + fadeIn(animationSpec = tween(duration, easing = easing)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        targetOffsetX = { it },
                                        animationSpec = tween(duration, easing = easing),
                                    ) + fadeOut(animationSpec = tween(duration, easing = easing)),
                                )
                        }
                    },
                ) { currentPage ->
                    when (currentPage) {
                        Page.Home -> HomeScreen(
                            padding = innerPadding,
                            prefs = prefs,
                            engine = engine,
                            handoff = handoff,
                            vm = vm,
                            onOpenPlatforms = {
                                platformsFrom = Page.Home
                                page = Page.Platforms
                            },
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
                            onOpenPlatforms = {
                                platformsFrom = Page.Settings
                                page = Page.Platforms
                            },
                            onOpenUpdate = { page = Page.Update },
                            onOpenWidgetSettings = { page = Page.WidgetSettings },
                        )
                        Page.WidgetSettings -> WidgetSettingsScreen(
                            padding = innerPadding,
                            prefs = prefs,
                            onBack = { page = Page.Settings },
                        )
                        Page.Tutorial -> TutorialScreen(
                            padding = innerPadding,
                            onBack = { page = Page.Settings },
                        )
                        Page.About -> AboutScreen(
                            padding = innerPadding,
                            prefs = prefs,
                            onBack = { page = Page.Settings },
                            onOpenChangelog = { page = Page.Changelog },
                        )
                        Page.Changelog -> ChangelogScreen(
                            padding = innerPadding,
                            onBack = { page = Page.About },
                        )
                        Page.Update -> UpdateScreen(
                            padding = innerPadding,
                            prefs = prefs,
                            onBack = { page = Page.Settings },
                        )
                    }
                }
            }

            // ---- 首次使用协议弹窗（窗口层，独立于 Scaffold）----
            if (showAgreement) {
                WindowDialog(
                    show = true,
                    title = "用户协议与隐私说明",
                    summary = "请阅读并同意《用户协议》与《隐私政策》后使用；不同意将无法使用本应用。",
                    onDismissRequest = { /* 必须同意或退出，点击外部不关闭 */ },
                    onDismissFinished = { },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    ) {
                        // 协议正文：独立可滚动区域（高度上限 400dp），确保下方按钮始终可见、可点击
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = true)
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(end = 4.dp),
                        ) {
                            Text(
                                USER_AGREEMENT + "\n\n" + PRIVACY_POLICY,
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
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
        } // CompositionLocalProvider 提供 LocalNavigationEventDispatcherOwner 结束
    }
}
