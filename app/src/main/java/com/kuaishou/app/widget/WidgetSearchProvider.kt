package com.kuaishou.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.kuaishou.app.R
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.WIDGET_CLICK_HOME
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** 4×1 搜索栏：伪输入框（拉起 App + 弹键盘）+ 右侧 2~4 个平台小图标（拉起并预选）。 */
class WidgetSearchProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val prefs = AppPrefs(context)
        // 偏好与列表只在本次广播内读取一次：runBlocking 会阻塞主线程做 DataStore 读盘，
        // 若放进 appWidgetIds 循环，同屏N 个同尺寸小组件就会把读盘次数放大 N 倍（卡顿 / ANR 风险）。
        val sort = runBlocking { prefs.widgetSort.first() }
        val source = runBlocking { prefs.widgetSource.first() }
        val autoFocus = runBlocking { prefs.widgetAutoFocus.first() }
        val clickAction = runBlocking { prefs.widgetClickAction.first() }
        val defaultId =
            if (clickAction == WIDGET_CLICK_HOME) null else WidgetHelper.defaultPlatformId(context)
        // 4×1 固定展示 3 个图标：用户指定优先，不足/未指定时按「排列/来源」自动补足到 3
        val barList = WidgetHelper.resolveWidgetPlatforms(
            context = context,
            ids = runBlocking { prefs.widgetSearchPlatforms.first() },
            min = 3,
            max = 3,
            sort = sort,
            source = source,
        )
        // 同一个 PendingIntent 实例可安全复用（requestCode 与参数完全相同）
        val openPi = WidgetHelper.openPendingIntent(
            context = context,
            sizeCode = WidgetHelper.SIZE_SEARCH,
            focus = autoFocus,
            clickAction = clickAction,
            defaultPlatform = defaultId,
        )

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_search)
            // 伪输入框 + 卡片空白处：拉起 App（按点击行为预选默认平台 / 仅首页）+ 弹键盘。
            // 根容器也挂上，避免图标区与搜索框之间的空隙成为点不动的死区。
            views.setOnClickPendingIntent(R.id.widget_bar, openPi)
            views.setOnClickPendingIntent(R.id.widget_root, openPi)
            // 先清后加，语义与原因见 WidgetHelper.fillGrid
            views.removeAllViews(R.id.widget_icons)
            // 右侧核心平台小图标（固定 36dp 图标垂直居中、由容器 divider 等距），按配置排序/来源选取
            barList.forEach { p ->
                val item = RemoteViews(context.packageName, R.layout.widget_bar_icon)
                if (p.custom || p.iconRes == 0) {
                    item.setImageViewResource(R.id.widget_icon_img, R.drawable.ic_launcher_foreground)
                } else {
                    item.setImageViewResource(R.id.widget_icon_img, p.iconRes)
                }
                item.setOnClickPendingIntent(
                    R.id.widget_icon,
                    WidgetHelper.platformPendingIntent(context, p.id, WidgetHelper.SIZE_SEARCH, focus = autoFocus),
                )
                views.addView(R.id.widget_icons, item)
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}