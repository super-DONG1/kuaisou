package com.kuaishou.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.WIDGET_CLICK_DEFAULT
import com.kuaishou.app.data.WIDGET_CLICK_HOME
import com.kuaishou.app.data.WIDGET_SOURCE_ALL
import com.kuaishou.app.data.WIDGET_SOURCE_ENABLED
import com.kuaishou.app.data.WIDGET_SOURCE_FREQUENT
import com.kuaishou.app.data.WIDGET_SORT_FREQ
import com.kuaishou.app.data.WIDGET_SORT_NAME
import com.kuaishou.app.data.WIDGET_SORT_ORDER
import com.kuaishou.app.data.Platform
import com.kuaishou.app.data.PlatformRegistry
import com.kuaishou.app.widget.WidgetHelper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 小组件设置页（从「设置 → 个性化 → 小组件设置」进入）。
 *
 * - 展示平台：4×1 / 2×2 / 4×2 各自指定要展示的平台，留空则按「排列/来源」自动选取。
 * - 排列：排列方式（平台顺序 / 使用频次 / 名称）与数据来源（已启用 / 常用 / 全部）。
 * - 交互：点击行为（默认平台 / 仅首页）、自动弹键盘、是否显示最近搜索。
 *
 * 任何改动都会即时保存，并重绘桌面上已添加的对应小组件。
 */
@Composable
fun WidgetSettingsScreen(
    padding: PaddingValues,
    prefs: AppPrefs,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val widgetSort by prefs.widgetSort.collectAsState(initial = WIDGET_SORT_ORDER)
    val widgetSource by prefs.widgetSource.collectAsState(initial = WIDGET_SOURCE_ENABLED)
    val widgetShowRecent by prefs.widgetShowRecent.collectAsState(initial = true)
    val widgetClickAction by prefs.widgetClickAction.collectAsState(initial = WIDGET_CLICK_DEFAULT)
    val widgetAutoFocus by prefs.widgetAutoFocus.collectAsState(initial = true)

    val widgetSearchPlatforms by prefs.widgetSearchPlatforms.collectAsState(initial = emptyList())
    val widget2x2Platforms by prefs.widget2x2Platforms.collectAsState(initial = emptyList())
    val widget4x2Platforms by prefs.widget4x2Platforms.collectAsState(initial = emptyList())

    var showSearchPlatforms by remember { mutableStateOf(false) }
    var show2x2Platforms by remember { mutableStateOf(false) }
    var show4x2Platforms by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showClickDialog by remember { mutableStateOf(false) }

    val customPlatforms by prefs.customPlatforms.collectAsState(initial = emptyList())
    // 全量可选平台（内置 + 自定义，按 id 去重以自定义为准），供「指定展示平台」弹窗展示
    val masterPlatforms = remember(customPlatforms) {
        (PlatformRegistry.all + customPlatforms.map { it.toPlatform() }).associateBy { it.id }.values.toList()
    }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    /** 保存配置后刷新桌面小组件（放在同一协程内，确保写盘完成后再重绘）。 */
    fun apply(msg: String, save: suspend () -> Unit) {
        scope.launch {
            save()
            WidgetHelper.updateAllWidgets(context)
        }
        toast(msg)
    }

    val sortText = when (widgetSort) {
        WIDGET_SORT_FREQ -> "使用频次"
        WIDGET_SORT_NAME -> "名称"
        else -> "平台顺序"
    }
    val sourceText = when (widgetSource) {
        WIDGET_SOURCE_FREQUENT -> "常用平台"
        WIDGET_SOURCE_ALL -> "全部平台"
        else -> "已启用平台"
    }
    val clickText = if (widgetClickAction == WIDGET_CLICK_HOME) "打开 App 首页" else "打开默认平台"

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        CommonTopAppBar("小组件设置", onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            // ---- 当前配置概览 ----
            SmallTitle("当前配置", insideMargin = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp))
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        "$sortText · $sourceText\n点击行为：$clickText · 弹键盘：${if (widgetAutoFocus) "开" else "关"} · 最近搜索：${if (widgetShowRecent) "开" else "关"}",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            // ---- 展示平台（4×1 / 2×2 / 4×2 可各自由选择要展示的平台）----
            SmallTitle("展示平台", insideMargin = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp))
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                BasicComponent(
                    title = "4×1 搜索栏平台",
                    summary = platformSummary(widgetSearchPlatforms, 3, 3),
                    startAction = {
                        Icon(MiuixIcons.GridView, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                    },
                    endActions = { Chevron() },
                    onClick = { showSearchPlatforms = true },
                )
                BasicComponent(
                    title = "2×2 宫格平台",
                    summary = platformSummary(widget2x2Platforms, 4, 6),
                    startAction = {
                        Icon(MiuixIcons.GridView, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                    },
                    endActions = { Chevron() },
                    onClick = { show2x2Platforms = true },
                )
                BasicComponent(
                    title = "4×2 宫格平台",
                    summary = platformSummary(widget4x2Platforms, 6, 8),
                    startAction = {
                        Icon(MiuixIcons.GridView, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                    },
                    endActions = { Chevron() },
                    onClick = { show4x2Platforms = true },
                )
            }

            // ---- 排列 ----
            SmallTitle("排列", insideMargin = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp))
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                BasicComponent(
                    title = "排列方式",
                    summary = "当前：$sortText",
                    startAction = {
                        Icon(MiuixIcons.Tune, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                    },
                    endActions = { Chevron() },
                    onClick = { showSortDialog = true },
                )
                BasicComponent(
                    title = "数据来源",
                    summary = "当前：$sourceText",
                    startAction = {
                        Icon(MiuixIcons.Backup, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                    },
                    endActions = { Chevron() },
                    onClick = { showSourceDialog = true },
                )
            }

            // ---- 交互 ----
            SmallTitle("交互", insideMargin = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp))
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                BasicComponent(
                    title = "点击行为",
                    summary = "当前：$clickText",
                    startAction = {
                        Icon(MiuixIcons.Settings, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                    },
                    endActions = { Chevron() },
                    onClick = { showClickDialog = true },
                )
                PreferenceSwitch(
                    checked = widgetAutoFocus,
                    onCheckedChange = { v ->
                        apply("自动弹键盘已${if (v) "开启" else "关闭"}") { prefs.setWidgetAutoFocus(v) }
                    },
                    title = "自动弹出键盘",
                    summary = "点击小组件进入应用时聚焦搜索框并弹键盘",
                    startAction = {
                        Icon(MiuixIcons.Forward, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                    },
                )
                PreferenceSwitch(
                    checked = widgetShowRecent,
                    onCheckedChange = { v ->
                        apply("最近搜索已${if (v) "开启" else "关闭"}") { prefs.setWidgetShowRecent(v) }
                    },
                    title = "显示最近搜索",
                    summary = "在 4×4 面板底部展示最近搜索关键词标签",
                    startAction = {
                        Icon(MiuixIcons.Ok, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                    },
                )
            }

            // ---- 说明 ----
            SmallTitle("说明", insideMargin = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp))
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        "支持 4×1 搜索栏、2×2 / 4×2 平台宫格与 4×4 全功能面板共四种尺寸，配置改动会即时同步到桌面已添加的小组件。",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ---- 4×1 指定展示平台 ----
    if (showSearchPlatforms) {
        WidgetPlatformsDialog(
            title = "4×1 搜索栏平台",
            platforms = masterPlatforms,
            selected = widgetSearchPlatforms,
            max = 3,
            onConfirm = { ids ->
                showSearchPlatforms = false
                apply("4×1 平台已更新") { prefs.setWidgetSearchPlatforms(ids) }
            },
            onDismiss = { showSearchPlatforms = false },
        )
    }

    // ---- 2×2 指定展示平台 ----
    if (show2x2Platforms) {
        WidgetPlatformsDialog(
            title = "2×2 宫格平台（4~6 个）",
            platforms = masterPlatforms,
            selected = widget2x2Platforms,
            max = 6,
            onConfirm = { ids ->
                show2x2Platforms = false
                apply("2×2 平台已更新") { prefs.setWidget2x2Platforms(ids) }
            },
            onDismiss = { show2x2Platforms = false },
        )
    }

    // ---- 4×2 指定展示平台 ----
    if (show4x2Platforms) {
        WidgetPlatformsDialog(
            title = "4×2 宫格平台（6~8 个）",
            platforms = masterPlatforms,
            selected = widget4x2Platforms,
            max = 8,
            onConfirm = { ids ->
                show4x2Platforms = false
                apply("4×2 平台已更新") { prefs.setWidget4x2Platforms(ids) }
            },
            onDismiss = { show4x2Platforms = false },
        )
    }

    // ---- 排列方式 ----
    OptionDialog(
        show = showSortDialog,
        title = "排列方式",
        options = listOf(
            Triple(WIDGET_SORT_ORDER, "平台顺序", "按「平台管理」中设置的启用顺序排列"),
            Triple(WIDGET_SORT_FREQ, "使用频次", "按各平台累计使用次数从高到低排列"),
            Triple(WIDGET_SORT_NAME, "名称", "按平台名称排序"),
        ),
        selected = widgetSort,
        onSelect = {
            showSortDialog = false
            apply("排列方式已更新") { prefs.setWidgetSort(it) }
        },
        onDismiss = { showSortDialog = false },
    )

    // ---- 数据来源 ----
    OptionDialog(
        show = showSourceDialog,
        title = "数据来源",
        options = listOf(
            Triple(WIDGET_SOURCE_ENABLED, "已启用平台", "只展示你在「平台管理」中开启的平台"),
            Triple(WIDGET_SOURCE_FREQUENT, "常用平台", "按使用频次自动选取最常用的平台"),
            Triple(WIDGET_SOURCE_ALL, "全部平台", "展示全部内置与自定义平台"),
        ),
        selected = widgetSource,
        onSelect = {
            showSourceDialog = false
            apply("数据来源已更新") { prefs.setWidgetSource(it) }
        },
        onDismiss = { showSourceDialog = false },
    )

    // ---- 点击行为 ----
    OptionDialog(
        show = showClickDialog,
        title = "点击行为",
        options = listOf(
            Triple(WIDGET_CLICK_DEFAULT, "打开默认平台", "点击空白/搜索栏时预选默认平台并进入搜索"),
            Triple(WIDGET_CLICK_HOME, "打开 App 首页", "点击后仅打开应用首页，不预选平台"),
        ),
        selected = widgetClickAction,
        onSelect = {
            showClickDialog = false
            apply("点击行为已更新") { prefs.setWidgetClickAction(it) }
        },
        onDismiss = { showClickDialog = false },
    )
}

/**
 * 指定展示平台多选弹窗。
 * - 高度自适应：列表区域上限约 60% 屏高，超限时内部滚动，避免弹窗过高。
 * - 多选反馈：每项右侧复选框（未选为描边方框、已选为实心蓝勾），选中行浅蓝高亮；
 *   标题下方动态显示「已选 X / Y」。
 * - 数量约束：达到 [max] 上限时禁止继续勾选并 Toast 提示，取消勾选实时解除。
 * - 系统返回优先关闭本弹窗（BackHandler 拦截），不直接返回上一级页面。
 * - 底部固定按钮：「取消」弱化、「确定」主题蓝主按钮，形成完整操作闭环。
 */
@Composable
private fun WidgetPlatformsDialog(
    title: String,
    platforms: List<Platform>,
    selected: List<String>,
    max: Int,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // 用不可变 Set + 每次重新赋值，确保 mutableStateOf 能感知变化并触发重组（直接 add/remove 原地修改不会触发）
    var sel by remember(selected) { mutableStateOf(selected.toSet()) }

    // 系统返回优先关闭弹窗，而非返回上一级页面
    BackHandler(enabled = true) { onDismiss() }

    val maxListHeight = LocalConfiguration.current.screenHeightDp.dp * 0.6f

    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 已选计数：标题下方一行小字，随勾选实时更新
            Text(
                text = "已选 ${sel.size} / $max",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, top = 4.dp),
            )

            // 列表区域：高度自适应 + 上限 + 滚动（按钮固定在下方始终可见）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxListHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                Column {
                    platforms.forEach { p ->
                        val checked = sel.contains(p.id)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (checked) Color(0xFFE5F0FF) else Color.Transparent),
                        ) {
                            BasicComponent(
                                title = p.name,
                                summary = if (p.custom) "自定义平台" else p.category.title,
                                onClick = {
                                    if (checked) {
                                        sel = sel - p.id
                                    } else {
                                        if (sel.size >= max) {
                                            Toast.makeText(
                                                context,
                                                "当前组件最多只能展示 $max 个平台",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            return@BasicComponent
                                        }
                                        sel = sel + p.id
                                    }
                                },
                                endActions = {
                                    // 复选框：未选=描边方框，已选=实心蓝底白勾
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(
                                                if (checked) Color(0xFF3482FF) else Color.Transparent,
                                                RoundedCornerShape(6.dp),
                                            )
                                            .border(
                                                1.5.dp,
                                                if (checked) Color(0xFF3482FF) else Color(0xFFBDBDBD),
                                                RoundedCornerShape(6.dp),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (checked) {
                                            Icon(
                                                imageVector = MiuixIcons.Ok,
                                                contentDescription = "已选中",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // 底部按钮：固定可见，取消弱化 / 确定主题蓝主按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(textColor = Color(0xFF666666)),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onConfirm(platforms.filter { sel.contains(it.id) }.map { it.id }) },
                    colors = ButtonDefaults.buttonColors(
                        color = Color(0xFF3482FF),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("确定", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 列表项摘要：未指定时说明会自动展示的数量区间（min == max 时显示固定值），
 * 已指定时显示「已选 n/max」。实际展示数量由小组件侧锁定在 min~max 区间。
 */
private fun platformSummary(ids: List<String>, min: Int, max: Int): String =
    if (ids.isEmpty()) {
        if (min == max) "默认（固定展示 $max 个）" else "默认（自动展示 $min~$max 个）"
    } else {
        "已选 ${ids.size}/$max"
    }