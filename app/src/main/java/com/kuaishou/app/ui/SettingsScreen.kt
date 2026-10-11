package com.kuaishou.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.JUMP_COPY_FIRST
import com.kuaishou.app.data.JUMP_IMMEDIATE
import com.kuaishou.app.data.THEME_DARK
import com.kuaishou.app.data.THEME_LIGHT
import com.kuaishou.app.data.THEME_SYSTEM
import com.kuaishou.app.data.appVersionName
import com.kuaishou.app.widget.WidgetHelper
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import com.kuaishou.app.R
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Update

/**
 * 设置页（重构版）。
 * - 信息架构：按功能模块分组的卡片（帮助/平台/外观跳转/个性化/数据与反馈/更新/关于）。
 * - 交互：每项含标题、说明与当前状态。
 * - 搜索：顶部搜索框按关键词跨分类过滤全部设置项。
 * - 保存与反馈：改动即时保存并给出 Toast 反馈。
 * - 重置：底部提供「恢复全部默认设置」（含确认）。
 * - 响应式：内容区自适应宽度并随搜索结果滚动。
 * - 可访问性：分类项标注 Role.Button 与 contentDescription；触控目标不小于 36dp。
 */
@Composable
fun SettingsScreen(
    padding: PaddingValues,
    prefs: AppPrefs,
    onBack: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPlatforms: () -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenWidgetSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val historySize by prefs.history.collectAsState(initial = emptyList())
    val themeMode by prefs.themeMode.collectAsState(initial = THEME_SYSTEM)
    val jumpMode by prefs.jumpMode.collectAsState(initial = JUMP_IMMEDIATE)
    val hapticsOn by prefs.hapticsEnabled.collectAsState(initial = true)
    val frequencySortEnabled by prefs.frequencySortEnabled.collectAsState(initial = false)
    val showStats by prefs.showStats.collectAsState(initial = true)

    var searchQuery by remember { mutableStateOf("") }

    var showClearConfirm by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember { mutableStateOf(false) }

    fun tick() = hapticTick(context, hapticsOn)
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    // ---- 导出：写 JSON 到用户选择的位置 ----
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = prefs.buildBackupJson()
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }.onSuccess {
                    toast("配置已导出")
                    tick()
                }.onFailure {
                    toast("导出失败")
                }
            }
        }
    }

    // ---- 导入：读取 JSON 恢复配置 ----
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                }.getOrNull()
                if (json.isNullOrBlank()) {
                    toast("读取文件失败")
                } else if (prefs.restoreFromJson(json)) {
                    WidgetHelper.updateAllWidgets(context)
                    toast("配置已恢复")
                    tick()
                } else {
                    toast("配置文件无效")
                }
            }
        }
    }

    // ---- 分类与可见项（随搜索/筛选实时计算）----
    val categories = listOf(
        SettingCategory("help", "帮助与引导", MiuixIcons.Help),
        SettingCategory("platform", "平台", MiuixIcons.GridView),
        SettingCategory("appearance", "外观与跳转", MiuixIcons.Theme),
        SettingCategory("personalize", "个性化", MiuixIcons.Tune),
        SettingCategory("data", "数据与反馈", MiuixIcons.Backup),
        SettingCategory("update", "更新", MiuixIcons.Update),
        SettingCategory("about", "关于", MiuixIcons.Info),
    )

    val themeText = when (themeMode) {
        THEME_LIGHT -> "浅色"
        THEME_DARK -> "深色"
        else -> "跟随系统"
    }
    val jumpText = if (jumpMode == JUMP_COPY_FIRST) "复制关键词后跳转" else "立刻跳转"
    val historyCount = historySize.size

    val items = listOf(
        SettingItem("tutorial", "help", "新用户引导", "4 步学会搜索跳转", "引导 教程 帮助 新手"),
        SettingItem("platforms", "platform", "平台管理", "排序 · 开关显示 · 默认平台 · 自定义", "平台 管理 自定义 启用 排序"),
        SettingItem("theme", "appearance", "主题外观", "当前：$themeText", "主题 外观 深色 浅色 夜间 模式"),
        SettingItem("jump", "appearance", "跳转方式", "当前：$jumpText", "跳转 复制 立刻 方式"),
        SettingItem("frequencySort", "personalize", "按使用频次自动排序", "依各平台使用次数自动置顶", "频次 排序 自动 热门"),
        SettingItem("showStats", "personalize", "首页显示使用统计", "本月搜索次数与覆盖平台数", "统计 概览 数据 使用"),
        SettingItem("widgetSettings", "personalize", "小组件设置", "展示平台 · 排列方式 · 数据来源 · 交互行为", "小组件 桌面 组件 平台 展示 排列 排序 数据 来源 交互 点击 键盘 最近 搜索 卡片 磁贴"),
        SettingItem("backup", "data", "备份与恢复", "导出配置到文件，换机后一键恢复", "备份 恢复 导入 导出 配置"),
        SettingItem("haptics", "data", "触感反馈", "跳转、复制、清空时轻微震动", "震动 触感 反馈 触觉"),
        SettingItem("clearHistory", "data", "清空最近搜索", if (historyCount == 0) "暂无记录" else "$historyCount 条", "清空 历史 搜索 记录 删除"),
        SettingItem("about", "about", "关于应用", "版本 v$appVersionName · 协议 · 致谢", "关于 版本 协议 致谢 信息"),
        SettingItem("updateSettings", "update", "更新设置", "检查频率 · 渠道 · 历史 · 手动检查", "更新 升级 版本 设置 检查 渠道 下载 安装"),
    )

    val lowerQ = searchQuery.trim().lowercase()
    val filtered = items.filter { item ->
        lowerQ.isEmpty() || (item.title + item.summary + item.keywords + item.category).lowercase().contains(lowerQ)
    }
    val grouped = categories.mapNotNull { cat ->
        val its = filtered.filter { it.category == cat.id }
        if (its.isEmpty()) null else cat to its
    }

    /** 设置项内容（随筛选结果渲染，含「恢复全部默认」入口）。 */
    @Composable
    fun SettingsContentImpl() {
        if (grouped.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (searchQuery.isBlank()) "该分类下暂无设置" else "未找到与「$searchQuery」相关的设置",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            return
        }

        Column {
            grouped.forEach { (cat, its) ->
                SmallTitle(cat.label, insideMargin = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp))
                Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                    its.forEach { item ->
                        when (item.id) {
                            "tutorial" -> BasicComponent(
                                title = "新用户引导",
                                summary = "4 步学会搜索跳转",
                                startAction = {
                                    Icon(MiuixIcons.Help, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                                endActions = { Chevron() },
                                onClick = onOpenTutorial,
                            )
                            "platforms" -> BasicComponent(
                                title = "平台管理",
                                summary = "排序 · 开关显示 · 默认平台 · 自定义",
                                startAction = {
                                    Icon(MiuixIcons.GridView, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                                endActions = { Chevron() },
                                onClick = onOpenPlatforms,
                            )
                            "theme" -> BasicComponent(
                                title = "主题外观",
                                summary = "当前：$themeText",
                                startAction = {
                                    Icon(MiuixIcons.Theme, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                                endActions = { Chevron() },
                                onClick = { showThemeDialog = true },
                            )
                            "jump" -> BasicComponent(
                                title = "跳转方式",
                                summary = "当前：$jumpText",
                                startAction = {
                                    Icon(MiuixIcons.Forward, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                                endActions = { Chevron() },
                                onClick = { showJumpDialog = true },
                            )
                            "frequencySort" -> PreferenceSwitch(
                                checked = frequencySortEnabled,
                                onCheckedChange = { v ->
                                    scope.launch { prefs.setFrequencySort(v) }
                                    toast("按频次排序已${if (v) "开启" else "关闭"}")
                                },
                                title = "按使用频次自动排序",
                                summary = "依各平台使用次数自动置顶（不影响手动启用顺序）",
                                startAction = {
                                    Icon(MiuixIcons.Tune, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                            )
                            "showStats" -> PreferenceSwitch(
                                checked = showStats,
                                onCheckedChange = { v ->
                                    scope.launch { prefs.setShowStats(v) }
                                    toast("首页使用统计已${if (v) "开启" else "关闭"}")
                                },
                                title = "首页显示使用统计",
                                summary = "本月搜索次数与覆盖平台数",
                                startAction = {
                                    Icon(MiuixIcons.Info, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                            )
                            "widgetSettings" -> BasicComponent(
                                title = "小组件设置",
                                summary = "展示样式 · 排列方式 · 数据来源 · 交互行为",
                                startAction = {
                                    Icon(MiuixIcons.GridView, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                                endActions = { Chevron() },
                                onClick = onOpenWidgetSettings,
                            )
                            "backup" -> BasicComponent(
                                title = "备份与恢复",
                                summary = "导出配置到文件，换机后一键恢复",
                                startAction = {
                                    Icon(MiuixIcons.Backup, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                                endActions = { Chevron() },
                                onClick = { showBackupSheet = true },
                            )
                            "haptics" -> PreferenceSwitch(
                                checked = hapticsOn,
                                onCheckedChange = { v ->
                                    scope.launch { prefs.setHapticsEnabled(v) }
                                    toast("触感反馈已${if (v) "开启" else "关闭"}")
                                    if (v) tick()
                                },
                                title = "触感反馈",
                                summary = "跳转、复制关键词、清空记录时轻微震动",
                                startAction = {
                                    Icon(MiuixIcons.Tune, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                            )
                            "clearHistory" -> ClearHistoryRow(
                                count = historyCount,
                                onClick = {
                                    if (historyCount == 0) toast("暂无搜索记录")
                                    else showClearConfirm = true
                                },
                            )
                            "about" -> BasicComponent(
                                title = "关于应用",
                                summary = "版本 v$appVersionName · 协议 · 致谢",
                                startAction = {
                                    Icon(MiuixIcons.Info, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                                endActions = { Chevron() },
                                onClick = onOpenAbout,
                            )
                            "updateSettings" -> BasicComponent(
                                title = "更新设置",
                                summary = "更新检查、下载安装、渠道与历史",
                                startAction = {
                                    Icon(MiuixIcons.Update, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                                },
                                endActions = { Chevron() },
                                onClick = onOpenUpdate,
                            )
                        }
                    }
                }
            }

            // ---- 恢复全部默认（始终可见）----
            Spacer(Modifier.height(16.dp))
            SmallTitle("重置", insideMargin = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp))
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                BasicComponent(
                    title = "恢复全部默认设置",
                    summary = "将主题、跳转、排序、触感等偏好恢复为默认值",
                    startAction = {
                        Icon(MiuixIcons.Backup, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                    },
                    endActions = { Chevron() },
                    onClick = { showResetAllConfirm = true },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        // ---- 顶栏（全局统一返回键样式）----
        CommonTopAppBar("设置", onBack)

        // ---- 搜索 ----
        SettingsSearchBar(searchQuery) { searchQuery = it }
        Spacer(Modifier.height(8.dp))

        // ---- 设置内容（随搜索结果实时筛选，跨分类展示）----
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            SettingsContentImpl()
        }
    }

    // ---- 清空确认弹窗 ----
    OverlayDialog(
        show = showClearConfirm,
        title = "清空最近搜索",
        summary = "将删除全部 $historyCount 条搜索记录，此操作不可恢复。",
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
                    tick()
                    toast("已清空最近搜索")
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.weight(1f),
            ) { Text("清空") }
        }
    }

    // ---- 主题外观选项弹窗 ----
    OptionDialog(
        show = showThemeDialog,
        title = "主题外观",
        options = listOf(
            Triple(THEME_SYSTEM, "跟随系统", "根据系统深浅色自动切换"),
            Triple(THEME_LIGHT, "浅色", "始终使用浅色主题"),
            Triple(THEME_DARK, "深色", "始终使用深色主题"),
        ),
        selected = themeMode,
        onSelect = {
            scope.launch { prefs.setThemeMode(it) }
            tick()
            showThemeDialog = false
        },
        onDismiss = { showThemeDialog = false },
    )

    // ---- 跳转方式选项弹窗 ----
    OptionDialog(
        show = showJumpDialog,
        title = "跳转方式",
        options = listOf(
            Triple(JUMP_IMMEDIATE, "立刻跳转", "点击搜索后直接唤起 App 或打开网页"),
            Triple(JUMP_COPY_FIRST, "复制后跳转", "先复制关键词到剪贴板，再发起跳转"),
        ),
        selected = jumpMode,
        onSelect = {
            scope.launch { prefs.setJumpMode(it) }
            tick()
            showJumpDialog = false
        },
        onDismiss = { showJumpDialog = false },
    )

    // ---- 恢复全部默认确认弹窗 ----
    OverlayDialog(
        show = showResetAllConfirm,
        title = "恢复全部默认设置",
        summary = "将主题、跳转、排序、触感、更新、小组件配置等偏好恢复为默认值。搜索历史、平台自定义与使用统计不受影响，更新历史会被清空。",
        onDismissRequest = { showResetAllConfirm = false },
        onDismissFinished = { showResetAllConfirm = false },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = "取消",
                onClick = { showResetAllConfirm = false },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    showResetAllConfirm = false
                    scope.launch {
                        prefs.resetAllSettings()
                        WidgetHelper.updateAllWidgets(context)
                    }
                    tick()
                    toast("已恢复全部默认设置")
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.weight(1f),
            ) { Text("恢复默认") }
        }
    }

    // ---- 备份与恢复操作抽屉 ----
    OverlayBottomSheet(
        show = showBackupSheet,
        title = "备份与恢复",
        onDismissRequest = { showBackupSheet = false },
        onDismissFinished = { showBackupSheet = false },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "导出的 JSON 包含：平台顺序、默认平台、自定义平台、主题、跳转方式、触感反馈、小组件配置与搜索历史。",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Button(
                onClick = {
                    showBackupSheet = false
                    exportLauncher.launch("kuaishou-backup-${System.currentTimeMillis()}.json")
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导出配置到文件") }
            Button(
                onClick = {
                    showBackupSheet = false
                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("从文件恢复配置") }
        }
    }

}

/** 设置项元信息（用于搜索过滤与分类）。 */

/** 手动「检查更新」行（带加载态）。 */
@Composable
fun CheckUpdateRow(
    isChecking: Boolean,
    summary: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isChecking, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { role = Role.Button; contentDescription = "检查更新" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            MiuixIcons.Refresh,
            null,
            tint = if (isChecking) MiuixTheme.colorScheme.onSurfaceVariantActions else MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isChecking) "检查中…" else "检查更新",
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Text(
                summary,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
private data class SettingItem(
    val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val keywords: String = "",
)

/** 设置分类（含图标，用于侧边栏 / 胶囊）。 */
private data class SettingCategory(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

/** 设置页顶部搜索框：与主页同款胶囊结构（前置放大镜 + BasicTextField + 清除），
 *  但按设置页规范用纯浅灰 #F2F2F2 无边胶囊；点击即进入输入态，实时过滤下方设置项（不沿用主页跳转逻辑）。 */
@Composable
private fun SettingsSearchBar(query: String, onQueryChange: (String) -> Unit) {
    val capsuleShape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .clip(capsuleShape)
            // 极浅灰底、无边框（设置页规范；与主页胶囊同形、同左对齐排版）
            .background(Color(0xFFF2F2F2)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 前置细线放大镜：16dp 左内边距，18dp，#999999
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = Color(0xFF999999),
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(18.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        // 与放大镜间距 8dp；有内容时给清除按钮预留 44dp 右侧空间
                        start = 8.dp,
                        end = if (query.isNotEmpty()) 44.dp else 12.dp,
                    ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                textStyle = TextStyle(
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    // 撑满点击热区、文字垂直居中；空态显示占位符「搜索设置项...」
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                "搜索设置项...",
                                color = Color(0xFF999999),
                                fontSize = 14.sp,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            // 清除按钮：36dp 圆形触摸目标 + 浅色圆底（与主页同款）
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = { onQueryChange("") },
                        )
                        .semantics { contentDescription = "清空搜索" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF000000).copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "×",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666),
                        )
                    }
                }
            }
        }
    }
}

/** 设置页行尾统一箭头。 */
@Composable
fun Chevron() {
    Text(
        "›",
        fontSize = 16.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
    )
}

/** 带开关的偏好行（Miuix 无 SwitchPreference，用 BasicComponent + Switch 组合）。 */
@Composable
fun PreferenceSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    summary: String,
    enabled: Boolean = true,
    startAction: (@Composable (() -> Unit)?) = null,
) {
    BasicComponent(
        title = title,
        summary = summary,
        startAction = startAction,
        endActions = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
    )
}

/** 清空最近搜索（危险操作，红色文字）。 */
@Composable
private fun ClearHistoryRow(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { role = Role.Button; contentDescription = "清空最近搜索" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "清空最近搜索",
            fontSize = 16.sp,
            color = MiuixTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (count == 0) "暂无记录" else "$count 条",
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            "›",
            fontSize = 16.sp,
            color = MiuixTheme.colorScheme.error,
        )
    }
}

/** 单选选项弹窗（选中打勾）。 */
@Composable
fun OptionDialog(
    show: Boolean,
    title: String,
    options: List<Triple<String, String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = title,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            options.forEach { (id, label, summary) ->
                BasicComponent(
                    title = label,
                    summary = summary,
                    endActions = {
                        if (id == selected) {
                            Icon(
                                imageVector = MiuixIcons.Ok,
                                contentDescription = "已选中",
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    onClick = { onSelect(id) },
                )
            }
        }
    }
}