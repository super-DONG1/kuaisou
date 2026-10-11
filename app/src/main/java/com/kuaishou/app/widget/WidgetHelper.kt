package com.kuaishou.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.kuaishou.app.EXTRA_FOCUS
import com.kuaishou.app.EXTRA_PLATFORM_ID
import com.kuaishou.app.EXTRA_QUERY
import com.kuaishou.app.MainActivity
import com.kuaishou.app.R
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.Platform
import com.kuaishou.app.data.PlatformRegistry
import com.kuaishou.app.data.WIDGET_CLICK_DEFAULT
import com.kuaishou.app.data.WIDGET_CLICK_HOME
import com.kuaishou.app.data.WIDGET_SOURCE_ALL
import com.kuaishou.app.data.WIDGET_SOURCE_ENABLED
import com.kuaishou.app.data.WIDGET_SOURCE_FREQUENT
import com.kuaishou.app.data.WIDGET_SORT_FREQ
import com.kuaishou.app.data.WIDGET_SORT_NAME
import com.kuaishou.app.data.WIDGET_SORT_ORDER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 桌面小组件共用逻辑：平台列表读取（含排序/数据来源）、PendingIntent 构造（防冲突 requestCode）、宫格填充。
 * 所有点击均拉起「快搜」MainActivity，由 App 端预选平台 / 填入关键词并（按需）强制弹出键盘。
 *
 * 配置来源：com.kuaishou.app.data.AppPrefs 中的小组件偏好（排列/数据来源/点击行为/最近搜索/自动弹键盘）。
 */
object WidgetHelper {

    /** 各尺寸在 PendingIntent requestCode 中的唯一前缀，避免跨尺寸 / 跨平台 Intent 互相覆盖导致传参失效。 */
    const val SIZE_SEARCH = 2
    const val SIZE_2X2 = 3
    const val SIZE_4X2 = 4
    const val SIZE_4X4 = 5

    private val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    private val ACTIVITY_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP

    /**
     * 取展示给小组件的平台列表。
     * - [source] 决定候选池：已启用平台 / 常用平台（按次数）/ 全部平台。
     * - [sort] 决定排序：平台顺序 / 使用频次 / 名称。
     * 若不足 [max] 个，自动用推荐平台（候选池外尚未展示的）补位，保证网格视觉丰满、不留巨大空白。
     */
    fun platforms(
        context: Context,
        max: Int,
        sort: String = WIDGET_SORT_ORDER,
        source: String = WIDGET_SOURCE_ENABLED,
    ): List<Platform> {
        if (max <= 0) return emptyList()
        val prefs = AppPrefs(context)
        val custom = runBlocking { prefs.customPlatforms.first() }.map { it.toPlatform() }
        // 自定义平台若与内置平台 id 冲突，以自定义为准；associateBy 后写覆盖先写，
        // 且保持该id 首次出现的位置，避免「全部平台/常用平台」下同一个平台出现两个格子。
        val byId = (PlatformRegistry.all + custom).associateBy { it.id }
        val all = byId.values.toList()

        // platformClicks 最多会被「数据来源」与「排列方式」各读一次，这里只读一次复用（DataStore 读盘代价高）
        var clicksCache: Map<String, Int>? = null
        fun clicks(): Map<String, Int> =
            clicksCache ?: runBlocking { prefs.platformClicks.first() }.also { clicksCache = it }

        val base = when (source) {
            WIDGET_SOURCE_ALL -> all
            WIDGET_SOURCE_FREQUENT -> {
                all.sortedByDescending { clicks()[it.id] ?: 0 }
            }
            else -> {
                val enabledIds = runBlocking { prefs.enabledPlatformIds.first() }
                if (enabledIds.isEmpty()) {
                    (PlatformRegistry.defaultEnabled + custom).distinctBy { it.id }
                } else {
                    enabledIds.mapNotNull { byId[it] }
                }
            }
        }

        val sorted = when (sort) {
            WIDGET_SORT_FREQ -> {
                base.sortedByDescending { clicks()[it.id] ?: 0 }
            }
            WIDGET_SORT_NAME -> base.sortedBy { it.name }
            else -> base
        }

        if (sorted.size >= max) return sorted.distinctBy { it.id }.take(max)
        val pool = all.filter { p -> sorted.none { it.id == p.id } }
        return (sorted + pool).distinctBy { it.id }.take(max)
    }

    /** 最近搜索关键词（最多 [max] 条，最近在前）。 */
    fun recentSearches(context: Context, max: Int): List<String> =
        runBlocking { AppPrefs(context).history.first() }.take(max)

    /** 当前默认平台 id（用于「点击行为 = 默认平台」时预选）。 */
    fun defaultPlatformId(context: Context): String? =
        runBlocking { AppPrefs(context).defaultPlatformId.first() }

    /**
     * 将「用户在设置页指定的展示平台 id 列表」解析为有序、去重、仅保留真实存在的 [Platform]。
     * 顺序严格沿用传入的 [ids]（即用户的选择顺序），不另做排序或补位；
     * 与 [platforms] 的区别在于完全由用户选择决定，便于精确控制面板中平台的先后。
     * 出现未知/已删除的 id 会被跳过，重复 id 只保留首次出现。
     */
    private fun resolvePlatforms(context: Context, ids: List<String>): List<Platform> {
        if (ids.isEmpty()) return emptyList()
        val prefs = AppPrefs(context)
        val custom = runBlocking { prefs.customPlatforms.first() }.map { it.toPlatform() }
        val byId = (PlatformRegistry.all + custom).associateBy { it.id }
        val seen = mutableSetOf<String>()
        return ids.mapNotNull { id -> byId[id]?.takeIf { seen.add(it.id) } }
    }

    /**
     * 解析某个尺寸最终要展示的平台列表，并把数量锁定在 `[min]`~`[max]` 区间内。
     *
     * 规则：
     * - [ids] 非空（用户已指定）→ 按指定顺序取前 [max] 个；若不足 [min] 个，
     *   用「排列/来源」自动选取的结果**去重后补足**到 [min] 个，避免面板显得稀疏。
     * - [ids] 为空（未指定）→ 完全回退到 [platforms] 的自动选取。
     * - 结果始终按 id 去重，同一平台不会出现两次。
     *
     * 各尺寸约定：[min]/[max] 分别为 4×1 = 3/3、2×2 = 4/6、4×2 = 6/8。
     */
    fun resolveWidgetPlatforms(
        context: Context,
        ids: List<String>,
        min: Int,
        max: Int,
        sort: String = WIDGET_SORT_ORDER,
        source: String = WIDGET_SOURCE_ENABLED,
    ): List<Platform> {
        require(max >= min) { "max($max) 不能小于 min($min)" }
        val picked = resolvePlatforms(context, ids).take(max)
        if (picked.size >= min) return picked
        val filler = platforms(context, max, sort, source)
            .filter { f -> picked.none { it.id == f.id } }
        return (picked + filler).take(max)
    }

    /**
     * requestCode 用途分区：确保「打开 App」「平台点击」「关键词点击」三类 PendingIntent 永不同码。
     * PendingIntent 的相等性不包含 extras，三者 Intent 身份又完全一致，故 requestCode 是唯一区分手段；
     * 一旦撞码，FLAG_UPDATE_CURRENT 会用后写入的 extras 覆盖前者，导致「点平台却带上别的关键词」。
     */
    private const val TAG_OPEN = 0
    private const val TAG_PLATFORM = 1
    private const val TAG_QUERY = 2

    /**
     * 由「用途分区 + 尺寸 + 业务键」生成唯一且恒为非负的 requestCode。
     * 三段互不重叠：tag 占第 24 位起、sizeCode 占第 21 位起、seed 取低 21 位（先掩掉符号位与高位，避免负数溢出）。
     */
    private fun rc(tag: Int, sizeCode: Int, seed: Int): Int =
        tag * 0x1000000 + sizeCode * 0x200000 + (seed.toLong() and 0x1FFFFF).toInt()

    /** 点击平台：拉起 App 并预选该平台 +（按需）弹键盘。 */
    fun platformPendingIntent(
        context: Context,
        platformId: String,
        sizeCode: Int,
        focus: Boolean = true,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_PLATFORM_ID, platformId)
            if (focus) putExtra(EXTRA_FOCUS, true)
            flags = ACTIVITY_FLAGS
        }
        return PendingIntent.getActivity(context, rc(TAG_PLATFORM, sizeCode, platformId.hashCode()), intent, PI_FLAGS)
    }

    /**
     * 点击空白 / 搜索栏：
     * - [clickAction]=home 时仅打开 App 首页（不预选平台）。
     * - 否则预选默认平台；[defaultPlatform] 为 null 时退化为仅弹键盘。
     * [focus] 控制是否强制弹出键盘。
     */
    fun openPendingIntent(
        context: Context,
        sizeCode: Int,
        focus: Boolean = true,
        clickAction: String = WIDGET_CLICK_DEFAULT,
        defaultPlatform: String? = null,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            if (clickAction != WIDGET_CLICK_HOME && defaultPlatform != null) {
                putExtra(EXTRA_PLATFORM_ID, defaultPlatform)
            }
            if (focus) putExtra(EXTRA_FOCUS, true)
            flags = ACTIVITY_FLAGS
        }
        return PendingIntent.getActivity(
            context,
            rc(TAG_OPEN, sizeCode, if (clickAction == WIDGET_CLICK_HOME) 1 else 0),
            intent,
            PI_FLAGS,
        )
    }

    /** 点击最近搜索标签：拉起 App 并将关键词填入搜索框 +（按需）弹键盘。 */
    fun queryPendingIntent(
        context: Context,
        query: String,
        sizeCode: Int,
        focus: Boolean = true,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_QUERY, query)
            if (focus) putExtra(EXTRA_FOCUS, true)
            flags = ACTIVITY_FLAGS
        }
        return PendingIntent.getActivity(context, rc(TAG_QUERY, sizeCode, query.hashCode()), intent, PI_FLAGS)
    }

    /**
     * 在 [gridId] 容器内按 [columns] 列铺平台宫格（复用 widget_grid_item 布局）。
     * 仅显示图标、不显示平台名称；单元内边距由布局 XML 静态声明。
     * [focus] 传入点击平台时的键盘行为。
     */
    fun fillGrid(
        context: Context,
        root: RemoteViews,
        gridId: Int,
        platforms: List<Platform>,
        columns: Int,
        sizeCode: Int,
        focus: Boolean = true,
    ) {
        // 【先清后加】RemoteViews 仅有 addView（追加语义），无逐个 removeView；
        // 而桌面（尤其 MIUI）为省开销会复用已膨胀的 View 树、直接重放 action，
        // 于是新行会追加在旧行之后，出现图标重叠 / 错位 / 残留。
        // removeAllViews 会清空该容器下所有子项（空容器时为空操作），保证「先清除旧图标，再添加新图标」。
        // 必须放在「空内容提前 return」之前，这样本次无内容时也能正确清掉上一轮的旧行。
        root.removeAllViews(gridId)
        // 防御：columns<=0 会让 chunked 抛 IllegalArgumentException、平台为空时无需铺格。
        // 正常调用方 columns 恒为 2/4，此处仅兜底，避免后续改动误传导致小组件整体不刷新。
        if (columns <= 0 || platforms.isEmpty()) return
        platforms.chunked(columns).forEach { rowItems ->
            val row = RemoteViews(context.packageName, R.layout.widget_row)
            rowItems.forEach { p ->
                val item = RemoteViews(context.packageName, R.layout.widget_grid_item)

                // 图标与首字母占位「成对」下发：不做 else 复位时，上一次
                // 自定义平台留下的 VISIBLE 占位会叠加在真实图标上。
                if (p.custom || p.iconRes == 0) {
                    item.setViewVisibility(R.id.widget_item_icon, View.GONE)
                    item.setViewVisibility(R.id.widget_item_icon_text, View.VISIBLE)
                    item.setTextViewText(R.id.widget_item_icon_text, p.name.take(1))
                } else {
                    item.setViewVisibility(R.id.widget_item_icon, View.VISIBLE)
                    item.setViewVisibility(R.id.widget_item_icon_text, View.GONE)
                    item.setImageViewResource(R.id.widget_item_icon, p.iconRes)
                }

                item.setOnClickPendingIntent(
                    R.id.widget_item,
                    platformPendingIntent(context, p.id, sizeCode, focus = focus),
                )
                row.addView(R.id.widget_row_container, item)
            }
            root.addView(gridId, row)
        }
    }

    /** 后台执行小组件刷新，避免 runBlocking 读 DataStore 时阻塞调用方（设置页）主线程。 */
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 立即重绘全部已添加的小组件，使配置变更（如指定展示平台）实时生效，无需删除组件再重新添加。
     *
     * 直接调用各 Provider 的 onUpdate（内部即 AppWidgetManager.updateAppWidget 推送新 RemoteViews），
     * 而不自行发送 ACTION_APPWIDGET_UPDATE 广播 —— 部分桌面（含 MIUI）收到自广播后仍沿用旧缓存。
     * onUpdate 内含 runBlocking 读盘，故整体放在 IO 线程，避免阻塞调用方主线程。
     */
    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        refreshScope.launch {
            val mgr = AppWidgetManager.getInstance(appContext)
            val providers = listOf(
                WidgetSearchProvider::class.java,
                Widget2x2Provider::class.java,
                Widget4x2Provider::class.java,
                Widget4x4Provider::class.java,
            )
            providers.forEach { cls ->
                runCatching {
                    val ids = mgr.getAppWidgetIds(ComponentName(appContext, cls))
                    if (ids.isNotEmpty()) {
                        // 用系统 Application 上下文直接驱动 onUpdate；onUpdate 内 runBlocking 读盘在本 IO 线程执行。
                        cls.getDeclaredConstructor().newInstance().onUpdate(appContext, mgr, ids)
                    }
                }
            }
        }
    }
}