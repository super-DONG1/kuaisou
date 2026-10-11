package com.kuaishou.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.kuaishou.app.R
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.PRIVACY_POLICY
import com.kuaishou.app.data.USER_AGREEMENT
import com.kuaishou.app.data.appVersionName
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
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 关于应用内部的子视图。 */
private enum class AboutSubPage { About, License, Sponsor, Thanks, Agreement }

/** 仓库链接。 */
private const val REPO_URL = "https://github.com/super-DONG1/kuaisou"
private const val ISSUES_URL = "https://github.com/super-DONG1/kuaisou/issues"

/**
 * 关于应用二级页：问题反馈 / 项目仓库 / 开源许可证 / 赞助 / 致谢 / 用户协议与隐私政策。
 * 内部用局部状态切换子视图（返回键逐级回退）。
 * 在协议页可撤回同意：确认后写入「未同意」并立即退出应用，下次进入需重新同意。
 */
@Composable
fun AboutScreen(
    padding: PaddingValues,
    prefs: AppPrefs,
    onBack: () -> Unit,
    onOpenChangelog: () -> Unit = {},
) {
    var sub by rememberSaveable { mutableStateOf(AboutSubPage.About) }

    // 系统返回键：子页先退回关于主列表，主列表返回由 MainActivity 处理
    BackHandler(enabled = sub != AboutSubPage.About) { sub = AboutSubPage.About }

    when (sub) {
        AboutSubPage.About -> AboutMain(
            padding = padding,
            onBack = onBack,
            onOpen = { sub = it },
            onOpenChangelog = onOpenChangelog,
        )
        AboutSubPage.License -> LicensePage(
            padding = padding,
            onBack = { sub = AboutSubPage.About },
        )
        AboutSubPage.Sponsor -> SponsorPage(
            padding = padding,
            onBack = { sub = AboutSubPage.About },
        )
        AboutSubPage.Thanks -> ThanksPage(
            padding = padding,
            onBack = { sub = AboutSubPage.About },
        )
        AboutSubPage.Agreement -> AgreementPage(
            padding = padding,
            prefs = prefs,
            onBack = { sub = AboutSubPage.About },
        )
    }
}

/** 打开外部链接。 */
private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
    }
}

/** 沿 Context 链向上查找宿主 Activity（撤回同意后用于退出应用）。 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ---------------- About 主列表 ----------------

@Composable
private fun AboutMain(
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (AboutSubPage) -> Unit,
    onOpenChangelog: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        CommonTopAppBar("关于应用", onBack)

        // ---- 应用介绍（置于页面最顶部）----
        AppIntro()

        // ---- 支持 ----
        SmallTitle("支持", insideMargin = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp))
        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            BasicComponent(
                title = "问题反馈",
                summary = "在 GitHub Issues 提交问题或建议",
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Report,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(38.dp).padding(end = 16.dp),
                    )
                },
                endActions = { Chevron() },
                onClick = { openUrl(context, ISSUES_URL) },
            )
            BasicComponent(
                title = "项目仓库",
                summary = "开源 · 欢迎 Star",
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Link,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(38.dp).padding(end = 16.dp),
                    )
                },
                endActions = {
                    Icon(
                        imageVector = MiuixIcons.Link,
                        contentDescription = "打开仓库",
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                },
                onClick = { openUrl(context, REPO_URL) },
            )
        }

        // ---- 项目 ----
        SmallTitle("项目", insideMargin = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp))
        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            BasicComponent(
                title = "开源许可证",
                summary = "本项目用到的开源组件与许可",
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Info,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(38.dp).padding(end = 16.dp),
                    )
                },
                endActions = { Chevron() },
                onClick = { onOpen(AboutSubPage.License) },
            )
            BasicComponent(
                title = "赞助",
                summary = "免费开源 · 如何支持快搜",
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Favorites,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(38.dp).padding(end = 16.dp),
                    )
                },
                endActions = { Chevron() },
                onClick = { onOpen(AboutSubPage.Sponsor) },
            )
            BasicComponent(
                title = "致谢",
                summary = "感谢开源项目与每一位用户",
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Community,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(38.dp).padding(end = 16.dp),
                    )
                },
                endActions = { Chevron() },
                onClick = { onOpen(AboutSubPage.Thanks) },
            )
        }

        // ---- 隐私与协议 ----
        SmallTitle("隐私与协议", insideMargin = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp))
        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            BasicComponent(
                title = "用户协议与隐私政策",
                summary = "首次使用需同意，可随时撤回",
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Notes,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(38.dp).padding(end = 16.dp),
                    )
                },
                endActions = { Chevron() },
                onClick = { onOpen(AboutSubPage.Agreement) },
            )
        }

        // ---- 版本 ----
        SmallTitle("版本", insideMargin = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp))
        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            BasicComponent(
                title = "版本 v$appVersionName",
                summary = "点击查看版本迭代记录",
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Notes,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(38.dp).padding(end = 16.dp),
                    )
                },
                endActions = { Chevron() },
                onClick = onOpenChangelog,
            )
        }

        // ---- 开发团队 ----
        SmallTitle("开发团队", insideMargin = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp))
        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            BasicComponent(
                title = "开发团队",
                summary = "DHY",
                startAction = {
                    Icon(
                        imageVector = MiuixIcons.Community,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(38.dp).padding(end = 16.dp),
                    )
                },
            )
        }
    }
}

// ---------------- 应用介绍（关于页最顶部） ----------------

/** 应用介绍：置于关于页最顶部，概览应用名称、核心功能、主要特点与适用场景。 */
@Composable
private fun AppIntro() {
    val context = LocalContext.current
    Card(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
        ) {
            // 标题区：图标 + 名称 + 一句话定位
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 用原生 ImageView 承载启动图标，兼容自适应图标（AdaptiveIcon）避免 painterResource 在某些机型崩溃
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageDrawable(ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher))
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "快搜",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    Text(
                        "绕过首页，直达搜索",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            IntroSection(
                title = "核心功能",
                items = listOf(
                    "聚合搜索跳转：输入关键词，一键直达各平台搜索结果",
                    "平台分类管理：视频 / 社交 / 电商等分类折叠，长按拖动排序",
                    "搜索联想词：输入即提示全网热词，减少打字",
                    "键盘悬浮胶囊：随键盘弹起，输入后一键搜索",
                    "桌面小组件：4x1 / 2x2 / 4x2 / 4x4，点击直接进 App 并弹键盘",
                ),
            )
            IntroSection(
                title = "主要特点",
                items = listOf(
                    "极简无广告：纯净界面，专注搜索本身",
                    "本地优先：搜索记录与本机配置仅存于设备，隐私可控",
                    "高度可定制：自定义默认平台、显示开关与排序",
                    "MIUIX 风格：统一卡片分组与交互，观感克制清爽",
                ),
            )
            IntroSection(
                title = "适用场景",
                items = listOf(
                    "想在多个平台快速搜索同一内容，省去反复切换",
                    "桌面一键直达常用平台，输入即搜",
                    "管理常用平台，把高频入口放在最顺手的位置",
                ),
            )
        }
    }
}

/** 介绍分区：小标题 + 项目符号列表。 */
@Composable
private fun IntroSection(title: String, items: List<String>) {
    Text(
        title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MiuixTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
    items.forEach { text ->
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
                text,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackground,
                lineHeight = 18.sp,
            )
        }
    }
}

// ---------------- 开源许可证 ----------------

private data class LicenseItem(
    val name: String,
    val license: String,
    val usage: String,
    val url: String,
)

private val licenses = listOf(
    LicenseItem(
        "Miuix", "Apache-2.0",
        "HyperOS 设计语言组件库，构建全部界面与交互", "https://github.com/compose-miuix-ui/miuix",
    ),
    LicenseItem(
        "Jetpack Compose", "Apache-2.0",
        "声明式 UI 框架", "https://developer.android.com/jetpack/compose",
    ),
    LicenseItem(
        "AndroidX DataStore", "Apache-2.0",
        "本地偏好存储（设置、搜索历史）", "https://developer.android.com/jetpack/androidx/releases/datastore",
    ),
    LicenseItem(
        "AndroidX Activity / Lifecycle", "Apache-2.0",
        "Activity 与生命周期组件", "https://developer.android.com/jetpack/androidx",
    ),
    LicenseItem(
        "Kotlin", "Apache-2.0",
        "开发语言", "https://github.com/JetBrains/kotlin",
    ),
)

@Composable
private fun LicensePage(
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        CommonTopAppBar("开源许可证", onBack)

        Text(
            "本项目基于以下开源组件构建，遵循 Apache License 2.0。点击条目查看用途与开源主页。",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            licenses.forEach { item ->
                BasicComponent(
                    title = item.name,
                    summary = if (expanded == item.name) "${item.license} · ${item.usage}" else item.license,
                    endActions = { Chevron() },
                    onClick = {
                        if (expanded == item.name) {
                            openUrl(context, item.url)
                        } else {
                            expanded = item.name
                        }
                    },
                )
            }
        }
    }
}

// ---------------- 赞助 ----------------

@Composable
private fun SponsorPage(
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        CommonTopAppBar("赞助", onBack)

        Text(
            "快搜完全免费、无广告、无任何收费功能。如果你觉得它有用，可以通过以下方式支持：",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            BasicComponent(
                title = "给仓库点个 Star",
                summary = "最好的认可与支持",
                endActions = { Chevron() },
                onClick = { openUrl(context, REPO_URL) },
            )
            BasicComponent(
                title = "反馈问题",
                summary = "帮助快搜变得更好",
                endActions = { Chevron() },
                onClick = { openUrl(context, ISSUES_URL) },
            )
        }

        // ---- 微信赞赏码：白底居中放大，保证可被识别与扫描（长按亦可识别） ----
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.mm_reward_qrcode),
                    contentDescription = "微信赞赏码，扫码或长按识别即可赞赏支持快搜",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Text(
                    "微信扫码赞赏",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    "感谢每一份支持，快搜会一直免费、无广告",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
                )
            }
        }

        Text(
            "快搜完全免费、无广告，你的每一次 Star 与反馈都是最大的支持。",
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Text(
            "您的赞助将用于解决服务器接口维护与开发者的咖啡。",
            fontSize = 11.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

// ---------------- 致谢 ----------------

@Composable
private fun ThanksPage(
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        CommonTopAppBar("致谢", onBack)

        Text(
            "快搜的诞生离不开以下开源项目与每个人的贡献：",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            BasicComponent(
                title = "Miuix 开源项目",
                summary = "yukonga 及贡献者 · HyperOS 设计语言的 Compose 实现",
                endActions = { Chevron() },
                onClick = { openUrl(context, "https://github.com/compose-miuix-ui/miuix") },
            )
            BasicComponent(
                title = "Jetpack Compose",
                summary = "Google · 声明式 UI 框架",
                endActions = { Chevron() },
                onClick = { openUrl(context, "https://developer.android.com/jetpack/compose") },
            )
            BasicComponent(
                title = "JetBrains",
                summary = "Kotlin 语言与开发工具",
                endActions = { Chevron() },
                onClick = { openUrl(context, "https://github.com/JetBrains/kotlin") },
            )
            BasicComponent(
                title = "AndroidX 团队",
                summary = "DataStore、Activity、Lifecycle 等 Android 基础组件",
                endActions = { Chevron() },
                onClick = { openUrl(context, "https://developer.android.com/jetpack/androidx") },
            )
            BasicComponent(
                title = "每一位快搜用户",
                summary = "你的每一次使用与反馈都在推动快搜变得更好",
            )
        }
    }
}

// ---------------- 用户协议与隐私政策 ----------------

@Composable
private fun AgreementPage(
    padding: PaddingValues,
    prefs: AppPrefs,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accepted by prefs.agreementAccepted.collectAsState(initial = false)
    var showRevokeConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        CommonTopAppBar("用户协议与隐私政策", onBack)

        Text(
            USER_AGREEMENT,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Text(
            PRIVACY_POLICY,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Text(
            if (accepted) "当前状态：已同意" else "当前状态：未同意",
            fontSize = 12.sp,
            color = if (accepted) MiuixTheme.colorScheme.primary
            else MiuixTheme.colorScheme.onSurfaceVariantActions,
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp),
        )

        Button(
            onClick = {
                if (accepted) {
                    showRevokeConfirm = true
                } else {
                    scope.launch { prefs.setAgreementAccepted(true) }
                    Toast.makeText(context, "已同意，可继续使用", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColorsPrimary(),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(min = 44.dp),
        ) {
            Text(if (accepted) "撤回同意" else "同意协议")
        }
    }

    // ---- 撤回确认弹窗：明确提示撤回后立即退出，下次进入需重新同意 ----
    OverlayDialog(
        show = showRevokeConfirm,
        title = "撤回同意",
        summary = "撤回同意后，本应用将立即退出，下次进入时必须重新阅读并同意《用户协议》与《隐私政策》才能继续使用。\n你保存在本机的搜索记录与设置不会被自动删除：可在下次同意后继续使用，也可通过「清空最近搜索」删除，或通过卸载彻底清除。",
        onDismissRequest = { showRevokeConfirm = false },
        onDismissFinished = { showRevokeConfirm = false },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = "取消",
                onClick = { showRevokeConfirm = false },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    showRevokeConfirm = false
                    scope.launch {
                        // 先落盘再退出，确保下次启动仍要求重新同意
                        prefs.setAgreementAccepted(false)
                        Toast.makeText(context, "已撤回同意，应用即将退出", Toast.LENGTH_SHORT).show()
                        context.findActivity()?.finishAffinity()
                    }
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.weight(1f),
            ) { Text("撤回并退出") }
        }
    }
}