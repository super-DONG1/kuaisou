package com.kuaishou.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 关于应用内部的子视图。 */
private enum class AboutSubPage { About, License, Sponsor, Thanks, Agreement }

/** 仓库链接。 */
private const val REPO_URL = "https://github.com/super-DONG1/kuaisou"
private const val ISSUES_URL = "https://github.com/super-DONG1/kuaisou/issues"

/**
 * 关于应用二级页：问题反馈 / 项目仓库 / 开源许可证 / 赞助 / 致谢 / 用户协议与隐私说明。
 * 内部用局部状态切换子视图（返回键逐级回退）。
 */
@Composable
fun AboutScreen(
    padding: PaddingValues,
    prefs: AppPrefs,
    onBack: () -> Unit,
) {
    var sub by rememberSaveable { mutableStateOf(AboutSubPage.About) }

    // 系统返回键：子页先退回关于主列表，主列表返回由 MainActivity 处理
    BackHandler(enabled = sub != AboutSubPage.About) { sub = AboutSubPage.About }

    when (sub) {
        AboutSubPage.About -> AboutMain(
            padding = padding,
            onBack = onBack,
            onOpen = { sub = it },
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

/** 各子页共用的顶栏：返回键 + 标题，与首页品牌区等高。 */
@Composable
private fun AboutTopBar(title: String, onBack: () -> Unit) {
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
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onBackground,
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

/** 列表行右侧箭头。 */
@Composable
private fun Chevron() {
    Text(
        "›",
        fontSize = 16.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
    )
}

// ---------------- About 主列表 ----------------

@Composable
private fun AboutMain(
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (AboutSubPage) -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        AboutTopBar("关于应用", onBack)

        // ---- 支持 ----
        SmallTitle("支持")
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            BasicComponent(
                title = "问题反馈",
                summary = "在 GitHub Issues 中提交你遇到的问题或建议",
                endActions = { Chevron() },
                onClick = { openUrl(context, ISSUES_URL) },
            )
            BasicComponent(
                title = "项目仓库",
                summary = "github.com/super-DONG1/kuaisou",
                endActions = { Chevron() },
                onClick = { openUrl(context, REPO_URL) },
            )
        }

        // ---- 项目 ----
        SmallTitle("项目")
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            BasicComponent(
                title = "开源许可证",
                summary = "本项目使用到的开源组件与许可",
                endActions = { Chevron() },
                onClick = { onOpen(AboutSubPage.License) },
            )
            BasicComponent(
                title = "赞助",
                summary = "免费开源 · 如何支持快搜",
                endActions = { Chevron() },
                onClick = { onOpen(AboutSubPage.Sponsor) },
            )
            BasicComponent(
                title = "致谢",
                summary = "感谢开源项目与每一位用户",
                endActions = { Chevron() },
                onClick = { onOpen(AboutSubPage.Thanks) },
            )
        }

        // ---- 协议 ----
        SmallTitle("协议")
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            BasicComponent(
                title = "用户协议与隐私说明",
                summary = "首次使用需同意，可随时撤回",
                endActions = { Chevron() },
                onClick = { onOpen(AboutSubPage.Agreement) },
            )
        }

        // ---- 版本 ----
        SmallTitle("版本")
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
        AboutTopBar("开源许可证", onBack)

        Text(
            "本项目基于以下开源组件构建，遵循 Apache License 2.0。点击条目查看用途与开源主页。",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
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
        AboutTopBar("赞助", onBack)

        Text(
            "快搜完全免费、无广告、无任何收费功能。如果你觉得它有用，可以通过以下方式支持：",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
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

        Text(
            "目前暂不提供打赏渠道；对项目的关注与反馈就是最大的支持。",
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
        AboutTopBar("致谢", onBack)

        Text(
            "快搜的诞生离不开以下开源项目与每个人的贡献：",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            BasicComponent(
                title = "Miuix 开源项目",
                summary = "yukonga 及贡献者 · HyperOS 设计语言的 Compose 实现",
                endActions = { Chevron() },
                onClick = { openUrl(context, "https://github.com/compose-miuix-ui/miuix") },
            )
            BasicComponent(
                title = "JetBrains",
                summary = "Kotlin 语言与开发工具",
                endActions = { Chevron() },
                onClick = { openUrl(context, "https://github.com/JetBrains/kotlin") },
            )
            BasicComponent(
                title = "AndroidX 团队",
                summary = "Compose 与 Android 基础组件",
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

// ---------------- 用户协议与隐私说明 ----------------

private val agreementText = """
    用户协议

    1. 快搜是一款搜索跳转工具，仅将你输入的关键词组装为对应平台的搜索链接，并交由系统唤起已安装的 App 或打开网页浏览器完成搜索。
    2. 关键词仅用于构造跳转链接，快搜不缓存、不上传、不分享你的任何搜索内容。
    3. 快搜不包含广告，不收集个人身份信息。

    隐私说明

    1. 数据存储：最近搜索记录、默认平台、平台开关等设置仅保存在本机应用数据中（DataStore），不会上传到任何服务器。
    2. 权限：快搜不申请任何敏感权限；跳转时仅使用系统标准的打开链接能力。
    3. 删除：你可在「设置 - 搜索历史」中一键清空最近搜索；卸载应用即删除全部本地数据。
    4. 第三方：跳转目标 App（抖音、小红书、B站等）的搜索行为受其自身隐私政策约束，与快搜无关。

    同意与撤回

    点击「同意并继续」即表示你已阅读并同意以上内容；你可在「设置 - 关于应用 - 用户协议与隐私说明」中随时撤回同意，撤回后下次启动将重新展示本说明。
""".trimIndent()

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
        AboutTopBar("用户协议与隐私说明", onBack)

        Text(
            agreementText,
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
                    Toast.makeText(context, "已同意，下次启动不再展示", Toast.LENGTH_SHORT).show()
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

    // ---- 撤回确认弹窗 ----
    OverlayDialog(
        show = showRevokeConfirm,
        title = "撤回同意",
        summary = "撤回后下次启动将重新展示用户协议与隐私说明，需要再次同意才能使用。",
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
                text = "保留同意",
                onClick = { showRevokeConfirm = false },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    showRevokeConfirm = false
                    scope.launch { prefs.setAgreementAccepted(false) }
                    Toast.makeText(context, "已撤回同意，下次启动将重新展示协议", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.weight(1f),
            ) { Text("撤回") }
        }
    }
}
