package com.kuaishou.app.ui

import android.content.ClipData
import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.JUMP_COPY_FIRST
import com.kuaishou.app.data.Platform
import com.kuaishou.app.data.PlatformRegistry
import com.kuaishou.app.LaunchHandoff
import com.kuaishou.app.engine.JumpEngine
import com.kuaishou.app.engine.JumpResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.kuaishou.app.data.SuggestionItem
import com.kuaishou.app.data.SuggestionSource
import top.yukonga.miuix.kmp.icon.extended.WorldClock

/**
 * 设计令牌：统一字体层级与间距规范。
 * - 字体分 6 级：display(品牌) > title(区块/卡片标题) > body(正文) > caption(次级说明) > micro(标签/角标)，
 *   以及 hero(统计大数字，用于强调)。层级清晰、跨度稳定，避免字号随意导致的视觉疲劳。
 * - 间距分 5 档：xs(4) < sm(8) < md(12) < lg(16) < xl(24)，所有区块外边距与卡片内边距统一取自此处。
 */
private object TypeScale {
    val display = 24.sp
    val title = 16.sp
    val body = 14.sp
    val caption = 12.sp
    val micro = 11.sp
    val hero = 22.sp
}

private object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/**
 * 主页：搜索跳板。
 * 品牌区 + 自动聚焦搜索框（右侧 🔍 触发）+ 平台宫格（选中/默认/长按设默认）
 * + 最近搜索 + 跳转失败兜底抽屉。
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    padding: PaddingValues,
    prefs: AppPrefs,
    engine: JumpEngine,
    handoff: LaunchHandoff,
    vm: HomeViewModel,
    onOpenPlatforms: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // ---- 状态 ----
    val query by vm.query.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    var suggestionsVisible by remember { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedPlatformId by rememberSaveable { mutableStateOf<String?>(null) }
    var errorState by rememberSaveable { mutableStateOf(false) }

    val history by prefs.history.collectAsState(initial = emptyList())
    val enabledOrder by prefs.enabledPlatformIds.collectAsState(initial = emptyList())
    val defaultPlatformId by prefs.defaultPlatformId.collectAsState(initial = null)
    val preferApp by prefs.preferLaunchApp.collectAsState(initial = true)
    val customPlatforms by prefs.customPlatforms.collectAsState(initial = emptyList())
    val jumpMode by prefs.jumpMode.collectAsState(initial = com.kuaishou.app.data.JUMP_IMMEDIATE)
    val hapticsOn by prefs.hapticsEnabled.collectAsState(initial = true)
    val lastPlatformId by prefs.lastPlatformId.collectAsState(initial = null)
    val frequencySortEnabled by prefs.frequencySortEnabled.collectAsState(initial = false)
    val platformClicks by prefs.platformClicks.collectAsState(initial = emptyMap())
    val showStats by prefs.showStats.collectAsState(initial = true)
    val searchStats by prefs.searchStats.collectAsState(initial = AppPrefs.SearchStats())

    val customAsPlatform = remember(customPlatforms) { customPlatforms.map { it.toPlatform() } }
    val enabledPlatforms = remember(enabledOrder, customAsPlatform) {
        if (enabledOrder.isEmpty()) {
            PlatformRegistry.defaultEnabled + customAsPlatform
        } else {
            val byId = (PlatformRegistry.all + customAsPlatform).associateBy { it.id }
            enabledOrder.mapNotNull { byId[it] }
        }
    }
    // 频次排序开启时按点击次数降序（稳定排序，计数相同保留手动顺序）；否则沿用手动顺序
    val displayPlatforms = remember(enabledPlatforms, frequencySortEnabled, platformClicks) {
        if (frequencySortEnabled) {
            enabledPlatforms.sortedByDescending { platformClicks[it.id] ?: 0 }
        } else {
            enabledPlatforms
        }
    }
    val defaultId = defaultPlatformId ?: lastPlatformId ?: PlatformRegistry.defaultEnabled.first().id
    val allPlatformMap = remember(customAsPlatform) {
        (PlatformRegistry.all + customAsPlatform).associateBy { it.id }
    }
    // 概览页「使用平台」列表：取已实际搜索过的平台，按累计搜索次数降序取前 3 名，
    // 并记录其中的最大值作为比例条分母。
    // 次数取自 platformCounts（与顶部「累计搜索」完全同口径，都是实际跳转的搜索），
    // 因此各平台次数之和恒等于（老数据为小于）顶部总次数，不会出现「之和 > 总数」的悖论。
    val usedPlatforms = remember(searchStats.covered, searchStats.platformCounts, allPlatformMap) {
        val counts = searchStats.platformCounts
        val list = searchStats.covered.mapNotNull { allPlatformMap[it] }
        val maxCount = (list.maxOfOrNull { counts[it.id] ?: 0 } ?: 0).coerceAtLeast(1)
        val sorted = list.sortedByDescending { counts[it.id] ?: 0 }.take(3)
        sorted to maxCount
    }
    val currentPlatformId = selectedPlatformId ?: defaultId

    var fallbackPlatform by remember { mutableStateOf<Platform?>(null) }
    var fallbackKeyword by remember { mutableStateOf("") }
    // 清空最近搜索的二次确认
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    // 进入主页：消费启动参数（系统分享关键词 / 小组件预选平台）
    LaunchedEffect(handoff.query.value, handoff.platformId.value) {
        handoff.query.value?.let { q ->
            vm.query.value = q
            handoff.query.value = null
            errorState = false
        }
        handoff.platformId.value?.let { p ->
            selectedPlatformId = p
            handoff.platformId.value = null
        }
    }

    // 仅在「冷启动」或「小组件进入」时弹键盘：由 shouldShowKeyboard 一次性标记驱动，
    // 与页面重组解耦，避免从设置/平台管理等内部页返回首页时误弹键盘
    LaunchedEffect(handoff.shouldShowKeyboard.value) {
        if (handoff.shouldShowKeyboard.value) {
            focusRequester.requestFocus()
            keyboard?.show()
            handoff.shouldShowKeyboard.value = false
        }
    }

    // 离开主页（跳转到设置/平台管理等内部页或切后台）时清除搜索框焦点，
    // 确保返回首页时不会因保留焦点而自动弹出键盘
    DisposableEffect(Unit) {
        onDispose { focusManager.clearFocus() }
    }

    // 空词点击搜索 → 抖动动画
    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorState) {
        if (errorState) {
            shake.snapTo(0f)
            listOf(-12f, 12f, -8f, 8f, -4f, 0f).forEach { value ->
                shake.animateTo(value, tween(55))
            }
        }
    }

    fun doSearch(): JumpResult? {
        val keyword = vm.query.value.trim()
        if (keyword.isEmpty()) {
            errorState = true
            return null
        }
        errorState = false
        // 以当前选中（或默认）平台为目标；确保「点平台即搜」使用刚点选的平台
        val targetId = selectedPlatformId ?: defaultId
        val target = allPlatformMap[targetId] ?: enabledPlatforms.first()
        // 未手动选过平台 → 走默认平台，Toast 提示所用平台
        if (selectedPlatformId == null) {
            Toast.makeText(context, "已用${target.name}搜索", Toast.LENGTH_SHORT).apply {
                setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 140)
                show()
            }
        }
        keyboard?.hide()
        scope.launch { prefs.setLastPlatform(target.id) }
        // 复制后跳转：先把关键词写入剪贴板
        if (jumpMode == JUMP_COPY_FIRST) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("kuaishou", keyword))
            Toast.makeText(context, "已复制关键词", Toast.LENGTH_SHORT).show()
            hapticTick(context, hapticsOn)
        }
        return when (val result = engine.search(target, keyword, preferApp)) {
            is JumpResult.Failed -> {
                fallbackPlatform = result.platform
                fallbackKeyword = result.keyword
                result
            }
            else -> {
                // 仅跳转成功才计入历史与统计：失败也记录会让「最近搜索」与使用统计失真
                scope.launch {
                    prefs.addHistory(keyword)
                    prefs.recordSearch(target.id)
                }
                hapticTick(context, hapticsOn)
                result
            }
        }
    }

    // 根布局不再加 imePadding()：那会把整个内容区整体上顶，
    // 内容不足一屏时底部就露出一条空白死区（键盘上方的「小白条」）。
    // 键盘占位改为只由悬浮胶囊自己承担（见下方 capsule 的 imePadding）。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        Column(Modifier.fillMaxSize()) {
        // 可滚动内容区（Z层1）：品牌 / 搜索框 / 平台宫格 / 最近搜索 / 使用概览
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
        ) {
        // ---- 品牌区 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("快搜", fontSize = TypeScale.display, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary)
                Text("一次搜索，直达常用平台", fontSize = TypeScale.caption, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            // 设置入口：MIUIX 风格图标按钮（圆形表面容器 + 系统波纹反馈）
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(40.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Settings,
                        contentDescription = "设置",
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // ---- 搜索框（MIUIX 风格输入胶囊）----
        Spacer(Modifier.height(Spacing.lg))
        Box {
            // MIUIX 风格搜索框：原生 BasicTextField + 全自绘装饰。
            // 不用 Miuix 0.9.4 TextField —— 其 label/占位符机制会遮住输入文字并撑大高度。
            // 结构：Row(前置放大镜 + BasicTextField(weight 1f) + 清除按钮)，
            // 保证满框点击热区、文字垂直居中，且 focusRequester 正确绑在可聚焦组件上。
            var fieldFocused by remember { mutableStateOf(false) }
            val capsuleShape = RoundedCornerShape(26.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(capsuleShape)
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        // 描边粗细：在 52dp 胶囊、22dp 图标、16sp 加粗文字之上，
                        // 静止 2dp 更稳重清晰，聚焦/错误态 2.5dp 让状态变化更有张力。
                        width = if (errorState || fieldFocused) 2.dp else 2.dp,
                        color = when {
                            errorState -> MiuixTheme.colorScheme.error
                            fieldFocused -> MiuixTheme.colorScheme.primary
                            // 静止态用主题色极淡描边（HyperOS/MIUIX 常见处理）：
                            // 比中性灰更与整体色调统一，加粗后也不显脏。
                            else -> MiuixTheme.colorScheme.primary.copy(alpha = 0.30f)
                        },
                        shape = capsuleShape,
                    )
                    .graphicsLayer { translationX = shake.value }
                    .onGloballyPositioned { fieldSize = it.size },
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 前置放大镜：MIUIX 标志性元素，聚焦时点亮主题色
                    Icon(
                        imageVector = MiuixIcons.Search,
                        contentDescription = null,
                        tint = if (fieldFocused) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier
                            .padding(start = 18.dp)
                            .size(22.dp),
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = {
                            vm.onQueryChange(it)
                            if (errorState && it.isNotBlank()) errorState = false
                            suggestionsVisible = it.isNotBlank()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                // 描边加粗后同步放大内边距，维持内容与框线的呼吸感
                                start = 10.dp,
                                end = if (query.isNotEmpty()) 46.dp else 14.dp,
                            )
                            .focusRequester(focusRequester)
                            .onFocusChanged { fieldFocused = it.isFocused },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                        ),
                        keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                        // 文字：深色 onBackground + 16sp + Medium 字重 + 22sp 行高，
                        // 加粗笔划与更粗的框线、图标线条在视觉上更统一清晰。
                        textStyle = TextStyle(
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp,
                        ),
                        cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            // fillMaxSize 撑满满框点击热区，CenterStart 让文字垂直居中；
                            // 占位符仅空态显示，浅灰与真实文字明确区分。
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        "搜点什么…",
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    // 清除按钮：40dp 圆形触摸目标 + 24dp 浅色圆底，精致且不挡文字
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        vm.query.value = ""
                                        suggestionsVisible = false
                                    },
                                )
                                .semantics { contentDescription = "清空关键词" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MiuixTheme.colorScheme.onBackground.copy(alpha = 0.07f),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "×",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                }
            }
            // 联想浮层（Z层2）：覆盖在平台网格上方，点击条目填入并收起浮层与键盘
            if (suggestionsVisible && query.isNotBlank() && suggestions.isNotEmpty()) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, fieldSize.height),
                    onDismissRequest = { suggestionsVisible = false },
                    properties = PopupProperties(focusable = false),
                ) {
                    SuggestionDropdown(
                        items = suggestions,
                        query = query,
                        widthDp = Dp(fieldSize.width / LocalDensity.current.density),
                        onItemClick = { text ->
                            vm.query.value = text
                            suggestionsVisible = false
                            keyboard?.hide()
                        },
                    )
                }
            }
        }
        if (errorState) {
            Text(
                "先输入关键词",
                fontSize = TypeScale.caption,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xs),
            )
        }

        // ---- 选择平台 ----
        Spacer(Modifier.height(Spacing.xl))
        // 平台网格模块头：左侧操作提示 + 右侧「管理平台」入口（MIUIX Section Header Action）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "长按卡片可设为默认",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF999999),
                modifier = Modifier.weight(1f),
            )
            // 管理平台入口：10sp 浅灰小字 + 箭头，仅文字弱化，不与平台图标抢焦点
            Text(
                "管理平台 ›",
                fontSize = 12.sp,
                color = Color(0xFF999999),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onOpenPlatforms() }
                    // 扩大透明触摸区，满足最小可点击范围，同时不改变视觉位置
                    .padding(start = 10.dp, end = 5.dp, top = 8.dp, bottom = 8.dp)
                    .semantics { contentDescription = "管理平台" },
            )
        }
        // 网格间距：Column(纵向 12dp) + Row(横向 12dp) 双向统一，卡片间留足呼吸感
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            displayPlatforms.chunked(3).forEach { rowPlatforms ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    rowPlatforms.forEach { platform ->
                        PlatformCard(
                            platform = platform,
                            selected = platform.id == currentPlatformId,
                            isDefault = platform.id == defaultId,
                            modifier = Modifier.weight(1f),
                            onSelect = {
                                if (selectedPlatformId != platform.id) {
                                    scope.launch { prefs.incrementPlatformClick(platform.id) }
                                }
                                // 单击：选中/取消选中平台并聚焦搜索框（已废除双击自动搜索）
                                selectedPlatformId = if (selectedPlatformId == platform.id) null else platform.id
                                errorState = false
                                focusRequester.requestFocus()
                            },
                            onSetDefault = {
                                scope.launch { prefs.setDefaultPlatform(platform.id) }
                                Toast.makeText(context, "已设为默认平台：${platform.name}", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                    repeat(3 - rowPlatforms.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        // ---- 最近搜索 ----
        if (history.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xl))
            Box(modifier = Modifier.fillMaxWidth()) {
                SmallTitle("最近搜索", insideMargin = PaddingValues(start = 15.dp))
                Text(
                    "清空",
                    fontSize = TypeScale.caption,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .clickable { showClearHistoryConfirm = true }
                        .padding(Spacing.sm),
                )
            }
            FlowRow(
                // 标签整体右移 6.dp：不再紧贴屏幕左缘，与上方平台网格卡片形成错位层次，
                // 视觉上更平衡（对齐基准：网格模块头提示文字为 10.dp，量级一致）。
                modifier = Modifier.padding(start = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                history.forEach { keyword ->
                    Text(
                        keyword,
                        fontSize = TypeScale.body,
                        color = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                            .combinedClickable(
                                onClick = {
                                    vm.query.value = keyword
                                    suggestionsVisible = false
                                    errorState = false
                                    focusRequester.requestFocus()
                                },
                                onLongClick = {
                                    scope.launch { prefs.removeHistory(keyword) }
                                },
                            )
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    )
                }
            }
        }

        // ---- 个人使用统计（轻量自洞察，可在设置中关闭）----
        if (showStats) {
            Spacer(Modifier.height(Spacing.xl))
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                ) {
                    Text(
                        "使用概览",
                        fontSize = TypeScale.title,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    Text(
                        "统计搜索活跃度，数据仅保存在本机.",
                        fontSize = TypeScale.caption,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "${searchStats.total}",
                                fontSize = TypeScale.hero,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary,
                            )
                            Text(
                                "累计搜索（次）",
                                fontSize = TypeScale.caption,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "${searchStats.covered.size}",
                                fontSize = TypeScale.hero,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary,
                            )
                            Text(
                                "覆盖平台（个）",
                                fontSize = TypeScale.caption,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        "使用平台",
                        fontSize = TypeScale.body,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    if (usedPlatforms.first.isEmpty()) {
                        Text(
                            "暂无使用记录，搜索后将自动统计各平台的使用情况。",
                            fontSize = TypeScale.caption,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            usedPlatforms.first.forEach { p ->
                                val count = searchStats.platformCounts[p.id] ?: 0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        p.name,
                                        fontSize = TypeScale.body,
                                        color = MiuixTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "$count 次",
                                        fontSize = TypeScale.caption,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((count.toFloat() / usedPlatforms.second).coerceIn(0f, 1f))
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MiuixTheme.colorScheme.primary),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        } // 内层可滚动区域结束
        } // 内层 Column（内容区）结束

        // ---- 键盘悬浮胶囊（Z层3：唯一搜索触发器，吸附键盘正上方）----
        // 键盘可见性用「双信号」判定，不依赖单一途径（部分国产 ROM 的 IME inset 上报不可靠）：
        //  ① IME insets 可见性（边到边 / Android 15+ 与多数 ROM 可靠）
        //  ② 布局可视区收缩高度 > 屏高 15%（窗口为键盘让位时可靠，兼容不下发 insets 的机型）
        // 任一成立即视为键盘弹出，确保 OPPO / 华为 / 荣耀 / vivo 等机型都能稳定触发。
        val view = LocalView.current
        var imeInsetVisible by remember { mutableStateOf(false) }
        var imeHeightPx by remember { mutableIntStateOf(0) }
        DisposableEffect(view) {
            val listener = ViewTreeObserver.OnGlobalLayoutListener {
                val rect = Rect()
                view.getWindowVisibleDisplayFrame(rect)
                val height = view.rootView.height
                val keyboardPx = (height - rect.bottom).coerceAtLeast(0)
                imeHeightPx = if (keyboardPx > height * 0.15f) keyboardPx else 0
                imeInsetVisible = ViewCompat.getRootWindowInsets(view)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
            view.viewTreeObserver.addOnGlobalLayoutListener(listener)
            onDispose { view.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
        }
        val imeVisible = imeInsetVisible || imeHeightPx > 0
        val capsuleVisible = query.isNotBlank() && imeVisible
        val capsuleTarget = allPlatformMap[currentPlatformId] ?: enabledPlatforms.first()
        // 默认平台在 App 打开时即视为已选（产品决策：默认平台始终生效），
        // 因此输入时使用默认平台也应直接展示「用X搜索」而非「选择平台」。
        val capsuleHasSel = selectedPlatformId != null || defaultId != null
        AnimatedVisibility(
            visible = capsuleVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // 只给胶囊加 imePadding：让它单独随键盘弹起上移，贴在键盘正上方。
                // AnimatedVisibility 隐藏时内容被整体移除（而非 alpha/INVISIBLE），
                // 不占任何垂直高度，因此无输入时底部不会残留死空间。
                .imePadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (capsuleHasSel) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.primary.copy(alpha = 0.5f),
                    )
                    .clickable {
                        if (capsuleHasSel) {
                            doSearch()
                        } else {
                            Toast.makeText(context, "请先选择一个平台", Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (capsuleHasSel) "用${capsuleTarget.name}搜索" else "选择平台",
                    fontSize = TypeScale.title,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }
    } // 外层 Box 结束

    // ---- 跳转失败兜底抽屉 ----
    val failed = fallbackPlatform
    // ---- 清空最近搜索：二次确认 ----
    if (showClearHistoryConfirm) {
        DestructiveConfirmDialog(
            title = "确认清空搜索记录？",
            message = "此操作将永久清除您的所有历史记录，且无法撤销。",
            confirmText = "确认清空",
            onDismiss = { showClearHistoryConfirm = false },
            onConfirm = { scope.launch { prefs.clearHistory() } },
        )
    }

    OverlayBottomSheet(
        show = failed != null,
        title = "无法直接打开${failed?.name ?: ""}",
        onDismissRequest = { fallbackPlatform = null },
        onDismissFinished = { fallbackPlatform = null },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xs, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "未安装${failed?.name ?: ""}或跳转链接已失效",
                fontSize = TypeScale.body,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))
            Button(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("kuaishou", fallbackKeyword))
                    Toast.makeText(context, "已复制关键词", Toast.LENGTH_SHORT).show()
                    hapticTick(context, hapticsOn)
                    fallbackPlatform = null
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("复制关键词") }
            Spacer(Modifier.height(Spacing.sm))
            Button(
                onClick = {
                    failed?.let { engine.openWebSearch(it, fallbackKeyword) }
                    fallbackPlatform = null
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("打开网页版") }
            Spacer(Modifier.height(Spacing.sm))
            TextButton(
                text = "打开${failed?.name ?: ""}首页",
                onClick = {
                    failed?.let { engine.openPlatformHome(it) }
                    fallbackPlatform = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "点击空白处关闭",
                fontSize = TypeScale.caption,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

/** 平台宫格卡片：品牌色圆标 + 名称 + 选中描边 + 默认角标。 */
@Composable
private fun PlatformCard(
    platform: Platform,
    selected: Boolean,
    isDefault: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onSetDefault: () -> Unit,
) {
    // 卡片圆角统一复用，保证底纹/描边与裁剪完全贴合
    val cardShape = RoundedCornerShape(16.dp)
    // 手势直接绑定到 Miuix Card 自带的 onClick（单击选中）/ onLongPress（长按设为默认）。
    // 双击自动搜索已按规范废除，单击仅做选中/取消选中并聚焦搜索框。
    Card(
        modifier = modifier
            // 固定高度：所有卡片严格等高，「默认」小字作为附加行不会让卡片变形
            .height(100.dp)
            .semantics {
                contentDescription = platform.name + if (isDefault) "，默认平台" else ""
            }
            // 仅一套中性描边界定卡片边界，不随选中变化（避免"选中边框"造成杂乱）
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.30f), cardShape),
        cornerRadius = 16.dp,
        // 选中底纹必须走 Card 的容器色通道：Miuix Card 在组件内部绘制自身背景，
        // 该背景的绘制顺序位于传入 modifier 之后，会把挂在 modifier 上的 background
        // 完全覆盖——这正是此前点击平台看不到底纹的原因。容器色由 Card 自身绘制，
        // 能完整覆盖含内部 InsideMargin 的整个卡片，无边缘露白。
        colors = if (selected) {
            CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.primary.copy(alpha = 0.18f),
                contentColor = MiuixTheme.colorScheme.onBackground,
            )
        } else {
            // 未选中沿用 Miuix 默认容器色，观感与原先完全一致
            CardDefaults.defaultColors()
        },
        onClick = onSelect,
        onLongPress = onSetDefault,
    ) {
        // 顶部对齐布局：图标与名称的纵向位置在所有卡片中完全一致，
        // 「默认」小字向下追加，不会顶动图标，网格整体对齐整齐。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 图标统一规格 40dp 圆角表面容器（内置图标裁剪填充，自定义平台显示首字母）
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                // 自定义平台、或暂无图标资源的平台，一律回退为首字母块
                // （不能对 iconRes = 0 调用 painterResource，否则会抛资源未找到）
                if (platform.custom || platform.iconRes == 0) {
                    Text(
                        platform.name.take(1),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                } else {
                    Image(
                        painter = painterResource(platform.iconRes),
                        // 名称已由父级 semantics 提供，避免重复朗读
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                platform.name,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = MiuixTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            // 默认态：平台名称下方的极小「默认」小字（9sp 浅灰、不加粗、淡蓝底纹）
            if (isDefault) {
                Spacer(Modifier.height(3.dp))
                Text(
                    "默认",
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF999999),
                    maxLines = 1,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
    }
}

/** 联想浮层下拉列表：圆角卡片，最多 6 条，每条 50dp，左侧图标区分来源（放大镜=网络 / 时钟=历史）。 */
@Composable
private fun SuggestionDropdown(
    items: List<SuggestionItem>,
    query: String,
    widthDp: Dp,
    onItemClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(widthDp)
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        cornerRadius = 12.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable { onItemClick(item.text) }
                        .padding(horizontal = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (item.source == SuggestionSource.Network) MiuixIcons.Search else MiuixIcons.WorldClock,
                        contentDescription = if (item.source == SuggestionSource.Network) "网络联想" else "历史记录",
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    val idx = item.text.indexOf(query, ignoreCase = true)
                    if (idx < 0) {
                        Text(
                            text = item.text,
                            fontSize = TypeScale.body,
                            color = MiuixTheme.colorScheme.onBackground,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Row(modifier = Modifier.weight(1f)) {
                            Text(
                                item.text.substring(0, idx),
                                fontSize = TypeScale.body,
                                color = MiuixTheme.colorScheme.onBackground,
                                maxLines = 1,
                            )
                            Text(
                                item.text.substring(idx, idx + query.length),
                                fontSize = TypeScale.body,
                                color = MiuixTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                            Text(
                                item.text.substring(idx + query.length),
                                fontSize = TypeScale.body,
                                color = MiuixTheme.colorScheme.onBackground,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}