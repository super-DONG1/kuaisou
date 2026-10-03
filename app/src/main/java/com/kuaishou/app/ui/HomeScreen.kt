package com.kuaishou.app.ui

import android.content.ClipData
import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.Platform
import com.kuaishou.app.data.PlatformRegistry
import com.kuaishou.app.engine.JumpEngine
import com.kuaishou.app.engine.JumpResult
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    onOpenPlatforms: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // ---- 状态 ----
    var query by rememberSaveable { mutableStateOf("") }
    var selectedPlatformId by rememberSaveable { mutableStateOf<String?>(null) }
    var errorState by rememberSaveable { mutableStateOf(false) }

    val history by prefs.history.collectAsState(initial = emptyList())
    val enabledOrder by prefs.enabledPlatformIds.collectAsState(initial = emptyList())
    val defaultPlatformId by prefs.defaultPlatformId.collectAsState(initial = null)
    val preferApp by prefs.preferLaunchApp.collectAsState(initial = false)

    val enabledPlatforms = remember(enabledOrder) {
        if (enabledOrder.isEmpty()) {
            PlatformRegistry.defaultEnabled
        } else {
            val byId = PlatformRegistry.all.associateBy { it.id }
            enabledOrder.mapNotNull { byId[it] }
        }
    }
    val defaultId = defaultPlatformId ?: PlatformRegistry.defaultEnabled.first().id
    val currentPlatformId = selectedPlatformId ?: defaultId
    val currentPlatform = PlatformRegistry.byId(currentPlatformId) ?: enabledPlatforms.first()

    var fallbackPlatform by remember { mutableStateOf<Platform?>(null) }
    var fallbackKeyword by remember { mutableStateOf("") }

    // 进入主页自动聚焦并弹出键盘
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
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

    fun doSearch() {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            errorState = true
            return
        }
        errorState = false
        // 未手动选过平台 → 走默认平台，Toast 提示所用平台
        if (selectedPlatformId == null) {
            Toast.makeText(context, "已用${currentPlatform.name}搜索", Toast.LENGTH_SHORT).apply {
                setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 140)
                show()
            }
        }
        keyboard?.hide()
        scope.launch {
            prefs.setLastPlatform(currentPlatform.id)
            prefs.addHistory(keyword)
        }
        when (val result = engine.search(currentPlatform, keyword, preferApp)) {
            is JumpResult.Failed -> {
                fallbackPlatform = result.platform
                fallbackKeyword = result.keyword
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // ---- 品牌区 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("快搜", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.onBackground)
                Text("搜完即走，不刷首页", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Text(
                "⚙",
                fontSize = 20.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onOpenSettings() }
                    .padding(8.dp),
            )
        }

        // ---- 搜索框（右侧 🔍 触发，空词置灰）----
        Spacer(Modifier.height(16.dp))
        TextField(
            value = query,
            onValueChange = {
                query = it
                if (errorState && it.isNotBlank()) errorState = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .graphicsLayer { translationX = shake.value },
            cornerRadius = 28.dp,
            label = "搜点什么…",
            useLabelAsPlaceholder = true,
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = { doSearch() }),
            colors = if (errorState) {
                TextFieldDefaults.textFieldColors(
                    labelColor = MiuixTheme.colorScheme.error,
                    borderColor = MiuixTheme.colorScheme.error,
                )
            } else {
                TextFieldDefaults.textFieldColors()
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        Text(
                            "×",
                            fontSize = 18.sp,
                            color = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { query = "" }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (query.isNotBlank()) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.disabledPrimaryButton,
                            )
                            .clickable(enabled = query.isNotBlank()) { doSearch() }
                            .padding(9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            tint = if (query.isNotBlank()) MiuixTheme.colorScheme.onPrimary
                            else MiuixTheme.colorScheme.disabledOnPrimaryButton,
                            contentDescription = "搜索",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
        )
        if (errorState) {
            Text(
                "先输入关键词",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
            )
        }

        // ---- 选择平台 ----
        Spacer(Modifier.height(18.dp))
        SmallTitle("选择平台 · 长按设为默认")
        enabledPlatforms.chunked(3).forEach { rowPlatforms ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowPlatforms.forEach { platform ->
                    PlatformCard(
                        platform = platform,
                        selected = platform.id == currentPlatformId,
                        isDefault = platform.id == defaultId,
                        modifier = Modifier.weight(1f),
                        onSelect = {
                            selectedPlatformId = platform.id
                            errorState = false
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
        Text(
            "管理平台 +",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onOpenPlatforms() }
                .padding(top = 8.dp, end = 4.dp, bottom = 2.dp),
        )

        // ---- 最近搜索 ----
        if (history.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SmallTitle("最近搜索")
            FlowRow(
                modifier = Modifier.padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                history.forEach { keyword ->
                    Text(
                        "# $keyword",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                            .combinedClickable(
                                onClick = {
                                    query = keyword
                                    errorState = false
                                    focusRequester.requestFocus()
                                },
                                onLongClick = {
                                    scope.launch { prefs.removeHistory(keyword) }
                                },
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // ---- 跳转失败兜底抽屉 ----
    val failed = fallbackPlatform
    OverlayBottomSheet(
        show = failed != null,
        title = "无法直接打开${failed?.name ?: ""}",
        onDismissRequest = { fallbackPlatform = null },
        onDismissFinished = { fallbackPlatform = null },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "未安装${failed?.name ?: ""}或跳转链接已失效",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("kuaishou", fallbackKeyword))
                    Toast.makeText(context, "已复制关键词", Toast.LENGTH_SHORT).show()
                    fallbackPlatform = null
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("复制关键词") }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    failed?.let { engine.openWebSearch(it, fallbackKeyword) }
                    fallbackPlatform = null
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("打开网页版") }
            Spacer(Modifier.height(10.dp))
            TextButton(
                text = "打开${failed?.name ?: ""}首页",
                onClick = {
                    failed?.let { engine.openPlatformHome(it) }
                    fallbackPlatform = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "点击空白处关闭",
                fontSize = 12.sp,
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
    Card(
        modifier = modifier
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
            ),
        cornerRadius = 16.dp,
        onClick = onSelect,
        onLongPress = onSetDefault,
    ) {
        // 所有卡片内容结构一致（图标 + 名称单行），高度恒等；默认角标叠加右上角不占布局
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(platform.iconRes),
                    contentDescription = platform.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp)),
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    platform.name,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = MiuixTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }
            if (isDefault) {
                Text(
                    "默认",
                    fontSize = 9.sp,
                    color = MiuixTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
