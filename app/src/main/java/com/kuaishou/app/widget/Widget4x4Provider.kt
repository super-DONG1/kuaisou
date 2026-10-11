package com.kuaishou.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.kuaishou.app.R
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.WIDGET_CLICK_HOME
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 4×4 全功能面板（进阶）：顶部伪搜索框 + 中部 4×3 平台网格(最多12图标) + 底部最近搜索标签。
 *
 * 最近搜索最多展示 [MAX_RECENT] 条：标签行净宽约 190dp，每个标签（56dp 文字 + 20dp 内边距 + 6dp 间隔）
 * 约 82dp，超过 2 条就会被卡片右边缘裁掉一半，3 条以上必然溢出，故固定 2 条以保证完整可见。
 */
class Widget4x4Provider : AppWidgetProvider() {

    companion object {
        /** 底部最近搜索最多展示的标签数（受标签行可用宽度限制，见类注释）。 */
        private const val MAX_RECENT = 2
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val prefs = AppPrefs(context)
        // 见 WidgetSearchProvider 注释：读盘只做一次，不要放进 appWidgetIds 循环
        val sort = runBlocking { prefs.widgetSort.first() }
        val source = runBlocking { prefs.widgetSource.first() }
        val autoFocus = runBlocking { prefs.widgetAutoFocus.first() }
        val clickAction = runBlocking { prefs.widgetClickAction.first() }
        val showRecent = runBlocking { prefs.widgetShowRecent.first() }
        val defaultId =
            if (clickAction == WIDGET_CLICK_HOME) null else WidgetHelper.defaultPlatformId(context)
        val list = WidgetHelper.platforms(context, 12, sort, source)
        val recents =
            if (showRecent) WidgetHelper.recentSearches(context, MAX_RECENT) else emptyList()
        // 搜索框与卡片空白处共用同一个 PendingIntent（requestCode 相同，复用同一实例）
        val openPi = WidgetHelper.openPendingIntent(
            context = context,
            sizeCode = WidgetHelper.SIZE_4X4,
            focus = autoFocus,
            clickAction = clickAction,
            defaultPlatform = defaultId,
        )

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_full)
            // 伪搜索框 / 空白区域：按点击行为拉起 App 默认平台 / 仅首页 +（按需）弹键盘
            views.setOnClickPendingIntent(R.id.widget_bar, openPi)
            views.setOnClickPendingIntent(R.id.widget_root, openPi)
            // 中部平台宫格：4 列 3 行共最多 12 个，按配置排序/来源/样式展示（不足则自动用推荐平台补位）
            WidgetHelper.fillGrid(
                context = context,
                root = views,
                gridId = R.id.widget_grid,
                platforms = list,
                columns = 4,
                sizeCode = WidgetHelper.SIZE_4X4,
                focus = autoFocus,
            )
            // 先清后加，语义与原因见 WidgetHelper.fillGrid
            views.removeAllViews(R.id.widget_recent_chips)
            // 底部最近搜索（历史记录模块）：开关开启时始终展示该模块，无记录时显示占位提示，
            // 避免模块在没有任何搜索历史时整体 GONE 而“消失”。
            views.setViewVisibility(R.id.widget_recent, if (showRecent) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_recent_title, if (showRecent) View.VISIBLE else View.GONE)
            if (recents.isEmpty()) {
                val placeholder = RemoteViews(context.packageName, R.layout.widget_chip)
                placeholder.setTextViewText(R.id.widget_chip_text, "暂无记录")
                // 占位标签同样可点：否则「暂无记录」是一个点不动的死区
                placeholder.setOnClickPendingIntent(R.id.widget_chip, openPi)
                views.addView(R.id.widget_recent_chips, placeholder)
            } else {
                recents.forEach { q ->
                    val chip = RemoteViews(context.packageName, R.layout.widget_chip)
                    chip.setTextViewText(R.id.widget_chip_text, q)
                    chip.setOnClickPendingIntent(
                        R.id.widget_chip,
                        WidgetHelper.queryPendingIntent(context, q, WidgetHelper.SIZE_4X4, focus = autoFocus),
                    )
                    views.addView(R.id.widget_recent_chips, chip)
                }
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}