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

/** 2×2 迷你网格：展示 4 个平台（2 列 2 行），无底座，点谁拉起谁。 */
class Widget2x2Provider : AppWidgetProvider() {
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
        val defaultId =
            if (clickAction == WIDGET_CLICK_HOME) null else WidgetHelper.defaultPlatformId(context)
        // 2×2 展示 4~6 个图标（2 列）：用户指定优先，不足 4 个时自动去重补足，超出取前 6 个
        val list = WidgetHelper.resolveWidgetPlatforms(
            context = context,
            ids = runBlocking { prefs.widget2x2Platforms.first() },
            min = 4,
            max = 6,
            sort = sort,
            source = source,
        )
        val openPi = WidgetHelper.openPendingIntent(
            context = context,
            sizeCode = WidgetHelper.SIZE_2X2,
            focus = autoFocus,
            clickAction = clickAction,
            defaultPlatform = defaultId,
        )

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_grid)
            // 空白区域：按点击行为拉起 App 默认平台 / 仅首页 +（按需）弹键盘
            views.setOnClickPendingIntent(R.id.widget_root, openPi)
            WidgetHelper.fillGrid(
                context = context,
                root = views,
                gridId = R.id.widget_grid,
                platforms = list,
                columns = 2,
                sizeCode = WidgetHelper.SIZE_2X2,
                focus = autoFocus,
            )
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}