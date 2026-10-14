package com.kuaishou.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.CustomPlatformData
import com.kuaishou.app.data.Platform
import com.kuaishou.app.data.PlatformCategory
import com.kuaishou.app.data.PlatformRegistry
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
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 平台管理页：
 * - 已启用平台：按预设分类（视频 / 图文社交 / 搜索问答 / 其他平台）分组展示，
 *   分类标题可折叠（高度伸缩 + 淡入淡出 + 三角旋转），拖拽排序严格限制在「同一分类内部」；
 * - 未启用的平台：平铺展示，不分组、不折叠、不支持拖拽；
 * - 自定义平台：新增 / 开关 / 删除。
 */
@Composable
fun PlatformManageScreen(
    padding: PaddingValues,
    prefs: AppPrefs,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val enabledOrder by prefs.enabledPlatformIds.collectAsState(initial = emptyList())
    val customList by prefs.customPlatforms.collectAsState(initial = emptyList())
    val defaultId by prefs.defaultPlatformId.collectAsState(initial = null)

    val all = remember { PlatformRegistry.all }
    val customPlatforms = remember(customList) { customList.map { it.toPlatform() } }
    val byId = remember(all, customPlatforms) { (all + customPlatforms).associateBy { it.id } }

    val enabled = if (enabledOrder.isEmpty()) {
        PlatformRegistry.defaultEnabled + customPlatforms
    } else {
        enabledOrder.mapNotNull { byId[it] }
    }
    val enabledIds = enabled.map { it.id }.toSet()
    val candidates = all.filter { it.id !in enabledIds }

    // 已启用按预设分类聚合：分组顺序固定，分类内保留用户既有的相对次序
    val grouped = remember(enabled) {
        PlatformCategory.values().flatMap { cat -> enabled.filter { it.category == cat } }
    }

    // ---- 拖拽排序状态 ----
    // order 即「按分类分组的扁平顺序」，因此每个分类占据一段连续索引区间，
    // 拖拽时把目标索引钳制在该区间内即可天然禁止跨分类。
    var order by remember { mutableStateOf(grouped) }
    LaunchedEffect(grouped) {
        if (order.map { it.id } != grouped.map { it.id }) order = grouped
    }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragDeltaY by remember { mutableStateOf(0f) }
    var dragStartIndex by remember { mutableStateOf(0) }
    /**
     * 拖拽过程中记录的目标槽位。关键：拖拽期间**不改动 order**，
     * 只记录「打算落到第几位」，让位与落位全部交给弹簧动画；
     * 松手且落位动画结束后才一次性提交 order。
     * 若在拖拽中实时改 order，布局位移会与 graphicsLayer 偏移相互抵消，
     * 视觉位置恒等于终点 —— 也就是看到的「瞬间移动」。
     */
    var dragTargetIndex by remember { mutableStateOf<Int?>(null) }
    var rowHeightPx by remember { mutableStateOf(0) }
    // 手势闭包需读到最新 order / 行高：用 rememberUpdatedState 避免重启手势检测
    val liveOrder by rememberUpdatedState(order)
    val liveRowHeight by rememberUpdatedState(rowHeightPx)

    // ---- 分类折叠状态：默认全部展开 ----
    var expandedCats by remember { mutableStateOf(PlatformCategory.values().toSet()) }

    var showAddDialog by remember { mutableStateOf(false) }
    // 破坏性操作的二次确认
    var showResetConfirm by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    fun saveOrder(list: List<Platform>) {
        scope.launch { prefs.setEnabledOrder(list.map { it.id }) }
    }

    fun toggle(platform: Platform, on: Boolean) {
        val next = if (on) order + platform else order.filter { it.id != platform.id }
        saveOrder(next)
        if (on && defaultId == null) {
            scope.launch { prefs.setDefaultPlatform(platform.id) }
            Toast.makeText(context, "已设为默认平台：${platform.name}", Toast.LENGTH_SHORT).show()
        }
    }

    fun addCustom(name: String, scheme: String, web: String) {
        val newItem = CustomPlatformData(
            id = "custom_${System.currentTimeMillis()}",
            name = name.trim(),
            schemeTemplate = scheme.trim().takeIf { it.isNotBlank() },
            webTemplate = web.trim(),
        )
        scope.launch {
            prefs.setCustomPlatforms(customList + newItem)
            val base = if (enabledOrder.isEmpty()) PlatformRegistry.defaultEnabled.map { it.id } else enabledOrder
            prefs.setEnabledOrder(base + newItem.id)
        }
        Toast.makeText(context, "已添加自定义平台", Toast.LENGTH_SHORT).show()
    }

    fun deleteCustom(id: String) {
        scope.launch {
            prefs.setCustomPlatforms(customList.filter { it.id != id })
            val base = if (enabledOrder.isEmpty()) PlatformRegistry.defaultEnabled.map { it.id } else enabledOrder
            prefs.setEnabledOrder(base.filter { it != id })
        }
        Toast.makeText(context, "已删除自定义平台", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier.padding(padding),
    ) {
        // 顶栏与首页品牌区等高
        item(key = "top_bar") {
            CommonTopAppBar("平台管理", onBack)
        }

        // ---- 已启用：按分类分组，分类内长按拖动排序（禁止跨分类）----
        item(key = "enabled_title") {
            SmallTitle("已启用 · 长按拖动排序", insideMargin = PaddingValues(start = 12.dp, top = 16.dp))
        }
        PlatformCategory.values().forEach { cat ->
            val catPlatforms = order.filter { it.category == cat }
            if (catPlatforms.isNotEmpty()) {
                val expanded = expandedCats.contains(cat)
                item(key = "cat_${cat.name}") {
                    CategoryHeader(
                        title = cat.title,
                        expanded = expanded,
                        onToggle = {
                            expandedCats =
                                if (expanded) expandedCats - cat else expandedCats + cat
                        },
                    )
                }
                items(
                    items = catPlatforms,
                    key = { "enabled_${it.id}" },
                ) { platform ->
                    val idx = liveOrder.indexOfFirst { it.id == platform.id }
                    val isDragging = dragKey == platform.id
                    // 落位偏移（每行独立）：松手后把「手指偏移」平滑收回 0，
                    // 让拖拽项滑向最终位置，而不是瞬间跳回
                    val dropAnim = remember(platform.id) { Animatable(0f) }
                    // 让位偏移：拖拽期间 order 保持不变，被跨过的行自行平移一整行高度。
                    // 由弹簧驱动，所以是 Q 弹地弹开，而不是硬跳
                    val rowH = liveRowHeight.toFloat()
                    val dragFrom = dragStartIndex
                    val dragTo = dragTargetIndex
                    val displace = when {
                        dragTo == null || idx == dragFrom -> 0f
                        // 向下拖：被跨过的行上移一行
                        dragTo > dragFrom && idx in (dragFrom + 1)..dragTo -> -rowH
                        // 向上拖：被跨过的行下移一行
                        dragTo < dragFrom && idx in dragTo..(dragFrom - 1) -> rowH
                        else -> 0f
                    }
                    val animatedDisplace by animateFloatAsState(
                        targetValue = displace,
                        animationSpec = spring(
                            // 轻微回弹：被挤开的栏带一点 Q 弹地让位，而非「到位即停」
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    )
                    val starInteraction = remember(platform.id) { MutableInteractionSource() }
                    val starPressed by starInteraction.collectIsPressedAsState()
                    // 折叠/展开动画：高度伸缩 + 淡入淡出
                    // 首次进入页面不播放展开动画（否则整页所有行同时 expand+fade，造成跳转卡顿延迟感）；
                    // 之后手动折叠/展开分类时动画照常
                    var firstEntry by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) { firstEntry = false }
                    AnimatedVisibility(
                        visible = expanded,
                        // zIndex 与「位移 / 悬浮缩放」必须挂在 item 根：
                        // ① zIndex 只在同层兄弟间排序，挂到内部 Row 上无法跨 item 提升层级；
                        // ② GroupCardItem 内部带 clip，位移挂在 clip 以内会被裁掉。
                        // 挂根节点后整个 item 一起位移，且不添加任何阴影（MIUIX 纯扁平）
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                if (isDragging) {
                                    // 跟手：order 拖拽期间不变，直接 1:1 跟随手指
                                    translationY = dragDeltaY
                                    // 悬浮态：仅靠放大体现「拿起来」
                                    scaleX = 1.05f
                                    scaleY = 1.05f
                                    alpha = 0.95f
                                } else {
                                    // 其他行 Q 弹让位；刚被放开的那一行走落位滑行
                                    translationY = animatedDisplace + dropAnim.value
                                }
                            },
                        enter = if (firstEntry) EnterTransition.None else expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        GroupCardItem(
                            isFirst = idx == 0 ||
                                liveOrder.getOrNull(idx - 1)?.category != cat,
                            isLast = idx == liveOrder.lastIndex ||
                                liveOrder.getOrNull(idx + 1)?.category != cat,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned {
                                        if (rowHeightPx == 0) rowHeightPx = it.size.height
                                    }
                                    // 位移与悬浮缩放见 item 根的 graphicsLayer
                                    .pointerInput(platform.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                dragStartIndex =
                                                    liveOrder.indexOfFirst { it.id == platform.id }
                                                dragTargetIndex = dragStartIndex
                                                dragKey = platform.id
                                                dragDeltaY = 0f
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragDeltaY += amount.y
                                                val list = liveOrder
                                                val cur = list.indexOfFirst { it.id == platform.id }
                                                if (cur < 0 || liveRowHeight <= 0) {
                                                    return@detectDragGesturesAfterLongPress
                                                }
                                                val c = list[cur].category
                                                // 同分类区间钳制：目标索引只能落在本分类占据的
                                                // 连续区间内，因此绝对无法跨分类拖拽
                                                val catStart = list.indexOfFirst { it.category == c }
                                                val catEnd = list.indexOfLast { it.category == c }
                                                // 只记录目标槽位、绝不改动 order：
                                                // 让位与落位全部交给弹簧动画处理
                                                dragTargetIndex = (cur + Math.round(
                                                    dragDeltaY / liveRowHeight,
                                                )).coerceIn(catStart, catEnd)
                                            },
                                            onDragEnd = {
                                                // 先取快照再清零，避免协程读到已被重置的 0
                                                val offsetAtRelease = dragDeltaY
                                                val list = liveOrder
                                                val from = dragStartIndex
                                                val to = dragTargetIndex
                                                dragKey = null
                                                dragDeltaY = 0f
                                                val rowH = liveRowHeight.toFloat()
                                                scope.launch {
                                                    dropAnim.snapTo(offsetAtRelease)
                                                    // 落位用无回弹弹簧：干脆停稳，不在终点来回晃
                                                    val settle = spring<Float>(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMediumLow,
                                                    )
                                                    if (to != null && to != from &&
                                                        from in list.indices && to in list.indices
                                                    ) {
                                                        // 先让卡片从手指位置滑到目标槽位（布局未变）
                                                        dropAnim.animateTo((to - from) * rowH, settle)
                                                        // 滑到位后提交顺序：布局跳到新位置、
                                                        // 偏移同时归零，二者视觉位置相等 → 无跳变
                                                        order = list.toMutableList().apply {
                                                            add(to, removeAt(from))
                                                        }
                                                        saveOrder(order)
                                                        dropAnim.snapTo(0f)
                                                    } else {
                                                        // 未跨行：直接滑回原位
                                                        dropAnim.animateTo(0f, settle)
                                                    }
                                                    dragTargetIndex = null
                                                }
                                            },
                                            onDragCancel = {
                                                val offsetAtRelease = dragDeltaY
                                                dragKey = null
                                                dragDeltaY = 0f
                                                dragTargetIndex = null
                                                scope.launch {
                                                    dropAnim.snapTo(offsetAtRelease)
                                                    dropAnim.animateTo(0f)
                                                }
                                            },
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // 拖动把手：Canvas 绘制的三条横线（加粗、加长），
                                // 整体向左靠拢 6dp，并与右侧平台图标底座保持 14dp 间距，避免视觉粘连
                                DragHandle(
                                    modifier = Modifier
                                        .offset(x = (-4).dp)
                                        .padding(end = 10.dp),
                                )
                                PlatformGlyph(platform)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        platform.name,
                                        fontSize = 16.sp,
                                        color = MiuixTheme.colorScheme.onBackground,
                                    )
                                    if (platform.id == defaultId) {
                                        Text(
                                            "默认平台",
                                            fontSize = 11.sp,
                                            color = MiuixTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                // 五角星：设为默认的快捷入口。24sp + 8dp 外边距（约 40dp 触摸区），
                                // 未选中为灰色空心 ☆，默认平台为主题蓝实心 ★；按压时轻微缩放反馈
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = starInteraction,
                                            indication = null,
                                        ) {
                                            // 已是默认则不重复写入、不重复提示
                                            if (platform.id != defaultId) {
                                                scope.launch {
                                                    prefs.setDefaultPlatform(platform.id)
                                                }
                                                Toast.makeText(
                                                    context,
                                                    "已设为默认平台：${platform.name}",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        }
                                        // GraphicsLayerScope 用 scaleX/scaleY，无 scale 属性
                                        .graphicsLayer {
                                            val s = if (starPressed) 0.82f else 1f
                                            scaleX = s
                                            scaleY = s
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        if (platform.id == defaultId) "★" else "☆",
                                        fontSize = 24.sp,
                                        color = if (platform.id == defaultId)
                                            MiuixTheme.colorScheme.primary
                                        else MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    )
                                }
                                Switch(
                                    checked = true,
                                    onCheckedChange = { keep ->
                                        if (!keep) {
                                            // 默认平台强制保持开启：拦截关闭并提示
                                            if (platform.id == defaultId) {
                                                Toast.makeText(
                                                    context,
                                                    "默认平台不能关闭，请先设置其他平台为默认",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            } else {
                                                toggle(platform, false)
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                                if (platform.custom) {
                                    Icon(
                                        imageVector = MiuixIcons.Delete,
                                        contentDescription = "删除",
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .clickable { pendingDeleteId = platform.id }
                                            .padding(2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- 未启用的平台：平铺展示（无分类 / 无折叠 / 禁止拖拽）----
        if (candidates.isNotEmpty()) {
            item(key = "candidates_title") {
                SmallTitle("未启用的平台", insideMargin = PaddingValues(start = 12.dp, top = 16.dp))
            }
            item(key = "candidates_card") {
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    candidates.forEach { platform ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlatformGlyph(platform)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                platform.name,
                                fontSize = 16.sp,
                                color = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = false,
                                onCheckedChange = { on -> if (on) toggle(platform, true) },
                            )
                        }
                    }
                }
            }
        }

        // ---- 自定义平台 ----
        item(key = "custom_title") {
            SmallTitle("自定义平台", insideMargin = PaddingValues(start = 12.dp, top = 16.dp))
        }
        item(key = "custom_card") {
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                BasicComponent(
                    title = "添加自定义平台",
                    summary = "小众 App 或网页搜索，支持 Scheme 与网页模板",
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = null,
                            tint = Color(0xFF555555),
                            modifier = Modifier.size(38.dp).padding(end = 16.dp),
                        )
                    },
                    endActions = {
                        Text(
                            "›",
                            fontSize = 16.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onClick = { showAddDialog = true },
                )
                customPlatforms.forEach { platform ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlatformGlyph(platform)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                platform.name,
                                fontSize = 16.sp,
                                color = MiuixTheme.colorScheme.onBackground,
                            )
                            Text(
                                if (platform.scheme != null) "Scheme + 网页" else "仅网页",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Switch(
                            checked = platform.id in enabledIds,
                            onCheckedChange = { on -> toggle(platform, on) },
                        )
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = "删除",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .clickable { deleteCustom(platform.id) }
                                .padding(2.dp),
                        )
                    }
                }
            }
        }

        // ---- 恢复默认 ----
        item(key = "reset") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(
                    text = "恢复默认",
                    onClick = { showResetConfirm = true },
                )
            }
        }
    }

    // ---- 添加自定义平台弹窗 ----
    if (showAddDialog) {
        AddCustomPlatformDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, scheme, web ->
                addCustom(name, scheme, web)
                showAddDialog = false
            },
        )
    }

    // ---- 恢复默认：二次确认 ----
    if (showResetConfirm) {
        DestructiveConfirmDialog(
            title = "确认恢复默认设置？",
            message = "此操作将重置平台排序、开关状态与默认平台，且无法撤销。",
            confirmText = "确认恢复",
            onDismiss = { showResetConfirm = false },
            onConfirm = {
                scope.launch {
                    prefs.setEnabledOrder(PlatformRegistry.defaultEnabled.map { it.id })
                    prefs.setDefaultPlatform(PlatformRegistry.defaultEnabled.first().id)
                }
                Toast.makeText(context, "已恢复默认平台", Toast.LENGTH_SHORT).show()
            },
        )
    }

    // ---- 删除自定义平台：二次确认 ----
    if (pendingDeleteId != null) {
        DestructiveConfirmDialog(
            title = "确认删除该自定义平台？",
            message = "删除后该平台的名称、Scheme 与网页模板将永久移除，且无法撤销。",
            confirmText = "确认删除",
            onDismiss = { pendingDeleteId = null },
            onConfirm = {
                // onConfirm 先于 onDismiss 执行，此时 id 仍有效
                pendingDeleteId?.let { deleteCustom(it) }
            },
        )
    }
}

/**
 * 分类标题行：13sp / Medium / 深灰，无卡片背景。
 * 左侧箭头为同一个「向下」图标（MiuixIcons.ExpandMore），放在固定 24dp 容器内居中，
 * 由 animateFloatAsState 驱动平滑旋转（0° → 180°）；容器宽度恒定，
 * 因此无论箭头如何旋转，右侧标题文字的起始位置都不会左右晃动。
 */
@Composable
private fun CategoryHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing,
        ),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            // start = 28dp（卡片外 12dp + 行内 16dp）：与下方列表项拖动把手「≡」左边缘严格对齐
            .padding(start = 28.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF666666),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation),
            )
        }
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/**
 * 分组卡片内的单行容器：底色与 Miuix Card 一致（surfaceContainer），
 * 圆角按行位置单独设置，使 LazyColumn 中多个行 item 首尾相接拼成一张连续卡片。
 */
@Composable
private fun GroupCardItem(
    isFirst: Boolean,
    isLast: Boolean,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 16.dp else 0.dp,
        topEnd = if (isFirst) 16.dp else 0.dp,
        bottomStart = if (isLast) 16.dp else 0.dp,
        bottomEnd = if (isLast) 16.dp else 0.dp,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        content()
    }
}

/** 添加自定义平台表单弹窗：名称 / Scheme 模板（可选）/ 网页模板（必填，含 {keyword}）。 */
@Composable
private fun AddCustomPlatformDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, scheme: String, web: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var scheme by remember { mutableStateOf("") }
    var web by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && web.isNotBlank() && web.contains("{keyword}")

    OverlayDialog(
        show = true,
        title = "添加自定义平台",
        summary = "网页模板必填，{keyword} 为关键词占位；Scheme 可选（App 唤起协议）",
        onDismissRequest = onDismiss,
        onDismissFinished = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = "平台名称",
                useLabelAsPlaceholder = true,
                singleLine = true,
            )
            TextField(
                value = scheme,
                onValueChange = { scheme = it },
                label = "App Scheme（可选）",
                useLabelAsPlaceholder = true,
                singleLine = true,
            )
            TextField(
                value = web,
                onValueChange = { web = it },
                label = "网页搜索模板（必填，含 {keyword}）",
                useLabelAsPlaceholder = true,
                singleLine = true,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onConfirm(name, scheme, web) },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    enabled = valid,
                    modifier = Modifier.weight(1f),
                ) { Text("保存") }
            }
        }
    }
}

/**
 * 拖动把手：三条横线（≡）。
 * 用 Canvas 而非文字字符绘制，才能精确控制线长、线宽与线间距——
 * 相比原先 18sp 的「≡」字符，线条更长更粗，拖拽提示更醒目，同时保持克制的浅灰配色。
 * 容器尺寸固定（24×18dp），配合 Row 的 CenterVertically 保证在列表项中严格垂直居中。
 */
@Composable
private fun DragHandle(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFC0C0C0),
) {
    Canvas(
        modifier = modifier.size(width = 16.dp, height = 14.dp),
    ) {
        val lineWidth = 12.dp.toPx()      // 线长
        val strokeWidth = 2.dp.toPx()     // 线宽
        val gap = 3.5.dp.toPx()           // 线间距
        val left = (size.width - lineWidth) / 2f
        val top = (size.height - (strokeWidth * 3 + gap * 2)) / 2f
        repeat(3) { i ->
            val y = top + strokeWidth / 2f + i * (strokeWidth + gap)
            drawLine(
                color = color,
                start = Offset(left, y),
                end = Offset(left + lineWidth, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** 平台行首图标：直接裸露显示（去除浅灰底座）。内置平台显示 App 图标，自定义/无资源显示首字母。 */
@Composable
private fun PlatformGlyph(platform: Platform) {
    Box(
        modifier = Modifier.size(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (platform.custom || platform.iconRes == 0) {
            Text(
                platform.name.take(1),
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onBackground,
            )
        } else {
            Image(
                painter = painterResource(platform.iconRes),
                contentDescription = platform.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
