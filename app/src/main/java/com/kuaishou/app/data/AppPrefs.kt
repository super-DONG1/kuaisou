package com.kuaishou.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "kuaishou_prefs")

/** 主题外观：system / light / dark。 */
const val THEME_SYSTEM = "system"
const val THEME_LIGHT = "light"
const val THEME_DARK = "dark"

/** 跳转方式：immediate 立刻跳转 / copy_first 复制关键词后跳转。 */
const val JUMP_IMMEDIATE = "immediate"
const val JUMP_COPY_FIRST = "copy_first"

/** 更新检查频率：manual / daily / weekly / monthly。 */
const val UPDATE_FREQ_MANUAL = "manual"
const val UPDATE_FREQ_DAILY = "daily"
const val UPDATE_FREQ_WEEKLY = "weekly"
const val UPDATE_FREQ_MONTHLY = "monthly"

/** 更新渠道：stable / beta / all。 */
const val UPDATE_CHANNEL_STABLE = "stable"
const val UPDATE_CHANNEL_BETA = "beta"
const val UPDATE_CHANNEL_ALL = "all"

/** 更新提醒方式：notification / dialog / none。 */
const val UPDATE_REMINDER_NOTIFICATION = "notification"
const val UPDATE_REMINDER_DIALOG = "dialog"
const val UPDATE_REMINDER_NONE = "none"

/** 小组件排列方式：order 平台顺序（默认）/ freq 使用频次 / name 名称（拼音/首字母）。 */
const val WIDGET_SORT_ORDER = "order"
const val WIDGET_SORT_FREQ = "freq"
const val WIDGET_SORT_NAME = "name"

/** 小组件数据来源：enabled 已启用平台（默认）/ frequent 常用平台 / all 全部平台。 */
const val WIDGET_SOURCE_ENABLED = "enabled"
const val WIDGET_SOURCE_FREQUENT = "frequent"
const val WIDGET_SOURCE_ALL = "all"

/** 小组件点击空白/搜索栏行为：default 打开默认平台并弹键盘（默认）/ home 打开 App 首页。 */
const val WIDGET_CLICK_DEFAULT = "default"
const val WIDGET_CLICK_HOME = "home"

/**
 * 本地偏好：默认/上次平台、启用平台顺序、自定义平台、最近搜索、
 * 唤起偏好、主题外观、跳转方式、触感反馈、协议同意状态，以及备份恢复。
 */
class AppPrefs(private val context: Context) {

    companion object {
        /** 最近搜索关键词保留条数（首页「最近搜索」直接全量渲染，不宜过多）。 */
        private const val HISTORY_MAX = 10
    }

    /** 个人使用统计快照。 */
    data class SearchStats(
        val total: Int = 0,
        val covered: Set<String> = emptySet(),
        /**
         * 各平台的累计搜索次数（与 total 同口径，均为「实际跳转的搜索」）。
         * 各项之和恒等于 total，用于首页「使用平台」排行。
         */
        val platformCounts: Map<String, Int> = emptyMap(),
    )

    /** 更新记录（用于「更新历史」展示）。 */
    data class UpdateRecord(
        val version: String = "",
        val time: Long = 0L,
        val channel: String = UPDATE_CHANNEL_STABLE,
        val status: String = "",
    )

    private val keyDefaultPlatform = stringPreferencesKey("default_platform")
    private val keyLastPlatform = stringPreferencesKey("last_platform")
    private val keyEnabledOrder = stringPreferencesKey("enabled_order")
    private val keyHistory = stringPreferencesKey("history")
    private val keyPreferLaunchApp = booleanPreferencesKey("prefer_launch_app")
    private val keyAgreementAccepted = booleanPreferencesKey("agreement_accepted")
    private val keyThemeMode = stringPreferencesKey("theme_mode")
    private val keyJumpMode = stringPreferencesKey("jump_mode")
    private val keyHapticsEnabled = booleanPreferencesKey("haptics_enabled")
    private val keyCustomPlatforms = stringPreferencesKey("custom_platforms")
    private val keyPlatformClicks = stringPreferencesKey("platform_clicks")
    private val keySearchStats = stringPreferencesKey("search_stats")
    private val keyFrequencySort = booleanPreferencesKey("frequency_sort")
    private val keyShowStats = booleanPreferencesKey("show_stats")
    private val keyUpdateCheckFreq = stringPreferencesKey("update_check_freq")
    private val keyAutoDownload = booleanPreferencesKey("auto_download")
    private val keyWifiOnly = booleanPreferencesKey("wifi_only")
    private val keyAutoInstall = booleanPreferencesKey("auto_install")
    private val keyUpdateChannel = stringPreferencesKey("update_channel")
    private val keyUpdateReminder = stringPreferencesKey("update_reminder")
    private val keyLastUpdateCheck = longPreferencesKey("last_update_check")
    private val keyUpdateHistory = stringPreferencesKey("update_history")

    /** 小组件配置。 */
    private val keyWidgetSort = stringPreferencesKey("widget_sort")
    private val keyWidgetSource = stringPreferencesKey("widget_source")
    private val keyWidgetShowRecent = booleanPreferencesKey("widget_show_recent")
    private val keyWidgetClickAction = stringPreferencesKey("widget_click_action")
    private val keyWidgetAutoFocus = booleanPreferencesKey("widget_auto_focus")

    /** 各尺寸小组件「指定展示平台」id 列表（逗号分隔、有序；空字符串表示回退到排列/来源自动选取）。 */
    private val keyWidgetSearchPlatforms = stringPreferencesKey("widget_search_platforms")
    private val keyWidget2x2Platforms = stringPreferencesKey("widget_2x2_platforms")
    private val keyWidget4x2Platforms = stringPreferencesKey("widget_4x2_platforms")

    /** 逗号分隔 id 字符串 → 列表（过滤空白）；空字符串/缺失返回空列表。 */
    private fun parseIdList(raw: String?): List<String> =
        raw?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    val defaultPlatformId: Flow<String?> =
        context.dataStore.data.map { it[keyDefaultPlatform] }

    val lastPlatformId: Flow<String?> =
        context.dataStore.data.map { it[keyLastPlatform] }

    /** 已启用平台 id 列表（顺序即主页宫格顺序）；首次读取时为空 → UI 层回退到默认启用列表。 */
    val enabledPlatformIds: Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            prefs[keyEnabledOrder]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }

    /** 最近搜索关键词，最多 10 条，最近的在最前。 */
    val history: Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            prefs[keyHistory]?.split("\u0001")?.filter { it.isNotBlank() } ?: emptyList()
        }

    val preferLaunchApp: Flow<Boolean> =
        context.dataStore.data.map { it[keyPreferLaunchApp] ?: true }

    /** 用户是否已同意用户协议与隐私说明（首次启动弹窗；可在关于页撤回）。 */
    val agreementAccepted: Flow<Boolean> =
        context.dataStore.data.map { it[keyAgreementAccepted] ?: false }

    /** 主题外观：THEME_SYSTEM / THEME_LIGHT / THEME_DARK。 */
    val themeMode: Flow<String> =
        context.dataStore.data.map { it[keyThemeMode] ?: THEME_SYSTEM }

    /** 跳转方式：JUMP_IMMEDIATE / JUMP_COPY_FIRST。 */
    val jumpMode: Flow<String> =
        context.dataStore.data.map { it[keyJumpMode] ?: JUMP_IMMEDIATE }

    /** 触感反馈开关（跳转/复制/清空时轻微震动）。 */
    val hapticsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[keyHapticsEnabled] ?: true }

    /** 用户自定义平台列表。 */
    val customPlatforms: Flow<List<CustomPlatformData>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[keyCustomPlatforms] ?: return@map emptyList()
            try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    CustomPlatformData(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        schemeTemplate = if (obj.has("scheme")) obj.optString("scheme").takeIf { it.isNotBlank() } else null,
                        webTemplate = obj.optString("web"),
                    )
                }.filter { it.id.isNotBlank() && it.name.isNotBlank() && it.webTemplate.isNotBlank() }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun setAgreementAccepted(accepted: Boolean) {
        context.dataStore.edit { it[keyAgreementAccepted] = accepted }
    }

    suspend fun setDefaultPlatform(id: String) {
        context.dataStore.edit { it[keyDefaultPlatform] = id }
    }

    suspend fun setLastPlatform(id: String) {
        context.dataStore.edit { it[keyLastPlatform] = id }
    }

    suspend fun setEnabledOrder(ids: List<String>) {
        context.dataStore.edit { it[keyEnabledOrder] = ids.joinToString(",") }
    }

    /** 各平台点击次数（JSON: {"id":count}），用于「按频次自动排序」。 */
    val platformClicks: Flow<Map<String, Int>> =
        context.dataStore.data.map { prefs ->
            prefs[keyPlatformClicks]?.let { parseJsonMap(it) } ?: emptyMap()
        }

    /** 个人使用统计：总次数、按月桶、已覆盖平台集合。 */
    val searchStats: Flow<SearchStats> =
        context.dataStore.data.map { prefs ->
            prefs[keySearchStats]?.let { parseSearchStats(it) } ?: SearchStats()
        }

    /** 「按使用频次自动排序平台」开关，默认关闭（保留用户手动顺序）。 */
    val frequencySortEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[keyFrequencySort] ?: false }

    /** 「首页显示使用统计」开关，默认开启。 */
    val showStats: Flow<Boolean> =
        context.dataStore.data.map { it[keyShowStats] ?: true }

    /** 更新检查频率：manual / daily / weekly / monthly，默认每周。 */
    val updateCheckFreq: Flow<String> =
        context.dataStore.data.map { it[keyUpdateCheckFreq] ?: UPDATE_FREQ_WEEKLY }

    /** 自动下载更新开关，默认关闭（避免消耗流量）。 */
    val autoDownload: Flow<Boolean> =
        context.dataStore.data.map { it[keyAutoDownload] ?: false }

    /** 仅 Wi-Fi 下自动下载，默认开启。 */
    val wifiOnlyDownload: Flow<Boolean> =
        context.dataStore.data.map { it[keyWifiOnly] ?: true }

    /** 下载后自动安装开关，默认关闭。 */
    val autoInstall: Flow<Boolean> =
        context.dataStore.data.map { it[keyAutoInstall] ?: false }

    /** 更新渠道：stable / beta / all，默认稳定版。 */
    val updateChannel: Flow<String> =
        context.dataStore.data.map { it[keyUpdateChannel] ?: UPDATE_CHANNEL_STABLE }

    /** 更新提醒方式：notification / dialog / none，默认通知栏。 */
    val updateReminder: Flow<String> =
        context.dataStore.data.map { it[keyUpdateReminder] ?: UPDATE_REMINDER_NOTIFICATION }

    /** 上次手动检查更新的时间戳（毫秒），0 表示从未检查。 */
    val lastUpdateCheck: Flow<Long> =
        context.dataStore.data.map { it[keyLastUpdateCheck] ?: 0L }

    /** 更新历史记录（最新在前）。 */
    val updateHistory: Flow<List<UpdateRecord>> =
        context.dataStore.data.map { prefs -> parseUpdateHistory(prefs[keyUpdateHistory]) }

    // ---- 小组件配置（供桌面小组件与「小组件设置」读取）----

    /** 排列方式：order / freq / name，默认 order。 */
    val widgetSort: Flow<String> =
        context.dataStore.data.map { it[keyWidgetSort] ?: WIDGET_SORT_ORDER }

    /** 数据来源：enabled / frequent / all，默认 enabled。 */
    val widgetSource: Flow<String> =
        context.dataStore.data.map { it[keyWidgetSource] ?: WIDGET_SOURCE_ENABLED }

    /** 是否显示最近搜索（仅 4x4 面板生效），默认开启。 */
    val widgetShowRecent: Flow<Boolean> =
        context.dataStore.data.map { it[keyWidgetShowRecent] ?: true }

    /** 点击空白/搜索栏行为：default / home，默认 default。 */
    val widgetClickAction: Flow<String> =
        context.dataStore.data.map { it[keyWidgetClickAction] ?: WIDGET_CLICK_DEFAULT }

    /** 点击小组件是否自动弹出键盘，默认开启。 */
    val widgetAutoFocus: Flow<Boolean> =
        context.dataStore.data.map { it[keyWidgetAutoFocus] ?: true }

    /** 4×1 搜索栏指定展示的平台 id（有序；空表示回退到「排列/来源」自动选取）。 */
    val widgetSearchPlatforms: Flow<List<String>> =
        context.dataStore.data.map { parseIdList(it[keyWidgetSearchPlatforms]) }

    /** 2×2 宫格指定展示的平台 id（有序；空表示回退）。 */
    val widget2x2Platforms: Flow<List<String>> =
        context.dataStore.data.map { parseIdList(it[keyWidget2x2Platforms]) }

    /** 4×2 宫格指定展示的平台 id（有序；空表示回退）。 */
    val widget4x2Platforms: Flow<List<String>> =
        context.dataStore.data.map { parseIdList(it[keyWidget4x2Platforms]) }

    /** 平台被点击一次 → 计数 +1（仅在实际选中不同平台时调用）。 */
    suspend fun incrementPlatformClick(id: String) {
        context.dataStore.edit { prefs ->
            val obj = runCatching { JSONObject(prefs[keyPlatformClicks] ?: "{}") }.getOrDefault(JSONObject())
            obj.put(id, obj.optInt(id) + 1)
            prefs[keyPlatformClicks] = obj.toString()
        }
    }

    /** 一次成功搜索 → 总次数 +1、该平台计数 +1、覆盖平台集合追加（去重）。 */
    suspend fun recordSearch(platformId: String) {
        context.dataStore.edit { prefs ->
            val obj = runCatching { JSONObject(prefs[keySearchStats] ?: "{}") }.getOrDefault(JSONObject())
            obj.put("total", obj.optInt("total") + 1)
            // 按平台累计搜索次数：与 total 同口径，保证各平台之和 == total
            val counts = obj.optJSONObject("platformCounts")
                ?: JSONObject().also { obj.put("platformCounts", it) }
            counts.put(platformId, counts.optInt(platformId) + 1)
            val covered = obj.optJSONArray("covered") ?: org.json.JSONArray().also { obj.put("covered", it) }
            var exists = false
            for (i in 0 until covered.length()) {
                if (covered.getString(i) == platformId) { exists = true; break }
            }
            if (!exists) covered.put(platformId)
            prefs[keySearchStats] = obj.toString()
        }
    }

    suspend fun setFrequencySort(enabled: Boolean) {
        context.dataStore.edit { it[keyFrequencySort] = enabled }
    }

    suspend fun setShowStats(show: Boolean) {
        context.dataStore.edit { it[keyShowStats] = show }
    }

    /** 设置更新检查频率。 */
    suspend fun setUpdateCheckFreq(freq: String) {
        context.dataStore.edit { it[keyUpdateCheckFreq] = freq }
    }

    /** 设置自动下载更新开关。 */
    suspend fun setAutoDownload(enabled: Boolean) {
        context.dataStore.edit { it[keyAutoDownload] = enabled }
    }

    /** 设置仅 Wi-Fi 下下载开关。 */
    suspend fun setWifiOnlyDownload(enabled: Boolean) {
        context.dataStore.edit { it[keyWifiOnly] = enabled }
    }

    /** 设置下载后自动安装开关。 */
    suspend fun setAutoInstall(enabled: Boolean) {
        context.dataStore.edit { it[keyAutoInstall] = enabled }
    }

    /** 设置更新渠道。 */
    suspend fun setUpdateChannel(channel: String) {
        context.dataStore.edit { it[keyUpdateChannel] = channel }
    }

    /** 设置更新提醒方式。 */
    suspend fun setUpdateReminder(reminder: String) {
        context.dataStore.edit { it[keyUpdateReminder] = reminder }
    }

    // ---- 小组件配置 setter ----

    suspend fun setWidgetSort(sort: String) {
        context.dataStore.edit { it[keyWidgetSort] = sort }
    }

    suspend fun setWidgetSource(source: String) {
        context.dataStore.edit { it[keyWidgetSource] = source }
    }

    suspend fun setWidgetShowRecent(show: Boolean) {
        context.dataStore.edit { it[keyWidgetShowRecent] = show }
    }

    suspend fun setWidgetClickAction(action: String) {
        context.dataStore.edit { it[keyWidgetClickAction] = action }
    }

    suspend fun setWidgetAutoFocus(focus: Boolean) {
        context.dataStore.edit { it[keyWidgetAutoFocus] = focus }
    }

    /** 保存 4×1 搜索栏指定展示的平台 id（有序）。 */
    suspend fun setWidgetSearchPlatforms(ids: List<String>) {
        context.dataStore.edit { it[keyWidgetSearchPlatforms] = ids.joinToString(",") }
    }

    /** 保存 2×2 宫格指定展示的平台 id（有序）。 */
    suspend fun setWidget2x2Platforms(ids: List<String>) {
        context.dataStore.edit { it[keyWidget2x2Platforms] = ids.joinToString(",") }
    }

    /** 保存 4×2 宫格指定展示的平台 id（有序）。 */
    suspend fun setWidget4x2Platforms(ids: List<String>) {
        context.dataStore.edit { it[keyWidget4x2Platforms] = ids.joinToString(",") }
    }

    /** 记录本次手动检查更新的时间。 */
    suspend fun setLastUpdateCheck(ts: Long) {
        context.dataStore.edit { it[keyLastUpdateCheck] = ts }
    }

    /** 追加一条更新历史（最多保留 20 条，最新在前）。 */
    suspend fun addUpdateHistory(record: UpdateRecord) {
        context.dataStore.edit { prefs ->
            val list = parseUpdateHistory(prefs[keyUpdateHistory]).toMutableList()
            list.add(0, record)
            prefs[keyUpdateHistory] = serializeUpdateHistory(list.take(20))
        }
    }

    /** 清空更新历史。 */
    suspend fun clearUpdateHistory() {
        context.dataStore.edit { it.remove(keyUpdateHistory) }
    }

    /** 将用户偏好类设置恢复为默认值（不影响搜索历史、平台自定义与使用统计）。 */
    suspend fun resetAllSettings() {
        context.dataStore.edit { prefs ->
            prefs[keyThemeMode] = THEME_SYSTEM
            prefs[keyJumpMode] = JUMP_IMMEDIATE
            prefs[keyPreferLaunchApp] = true
            prefs[keyFrequencySort] = false
            prefs[keyShowStats] = true
            prefs[keyHapticsEnabled] = true
            prefs[keyUpdateCheckFreq] = UPDATE_FREQ_WEEKLY
            prefs[keyAutoDownload] = false
            prefs[keyWifiOnly] = true
            prefs[keyAutoInstall] = false
            prefs[keyUpdateChannel] = UPDATE_CHANNEL_STABLE
            prefs[keyUpdateReminder] = UPDATE_REMINDER_NOTIFICATION
            prefs[keyLastUpdateCheck] = 0L
            prefs.remove(keyUpdateHistory)
            // 小组件配置恢复默认
            prefs[keyWidgetSort] = WIDGET_SORT_ORDER
            prefs[keyWidgetSource] = WIDGET_SOURCE_ENABLED
            prefs[keyWidgetShowRecent] = true
            prefs[keyWidgetClickAction] = WIDGET_CLICK_DEFAULT
            prefs[keyWidgetAutoFocus] = true
            prefs.remove(keyWidgetSearchPlatforms)
            prefs.remove(keyWidget2x2Platforms)
            prefs.remove(keyWidget4x2Platforms)
        }
    }

    private fun parseJsonMap(raw: String): Map<String, Int> = runCatching {
        val obj = JSONObject(raw)
        buildMap<String, Int> {
            val it = obj.keys()
            while (it.hasNext()) {
                val k = it.next()
                put(k, obj.getInt(k))
            }
        }
    }.getOrDefault(emptyMap())

    private fun parseSearchStats(raw: String): SearchStats = runCatching {
        val obj = JSONObject(raw)
        val covered = (obj.optJSONArray("covered")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()).toSet()
        val countsObj = obj.optJSONObject("platformCounts") ?: JSONObject()
        val platformCounts = buildMap<String, Int> {
            val it = countsObj.keys()
            while (it.hasNext()) {
                val k = it.next()
                put(k, countsObj.getInt(k))
            }
        }
        SearchStats(
            total = obj.optInt("total"),
            covered = covered,
            platformCounts = platformCounts,
        )
    }.getOrDefault(SearchStats())

    private fun parseUpdateHistory(raw: String?): List<UpdateRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                UpdateRecord(
                    version = o.optString("version"),
                    time = o.optLong("time"),
                    channel = o.optString("channel", UPDATE_CHANNEL_STABLE),
                    status = o.optString("status"),
                ).takeIf { it.version.isNotBlank() }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeUpdateHistory(list: List<UpdateRecord>): String {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject()
                    .put("version", r.version)
                    .put("time", r.time)
                    .put("channel", r.channel)
                    .put("status", r.status),
            )
        }
        return arr.toString()
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[keyThemeMode] = mode }
    }

    suspend fun setJumpMode(mode: String) {
        context.dataStore.edit { it[keyJumpMode] = mode }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[keyHapticsEnabled] = enabled }
    }

    suspend fun setCustomPlatforms(list: List<CustomPlatformData>) {
        context.dataStore.edit { prefs ->
            if (list.isEmpty()) {
                prefs.remove(keyCustomPlatforms)
            } else {
                val arr = JSONArray()
                list.forEach { p ->
                    arr.put(
                        JSONObject()
                            .put("id", p.id)
                            .put("name", p.name)
                            .put("scheme", p.schemeTemplate ?: "")
                            .put("web", p.webTemplate)
                    )
                }
                prefs[keyCustomPlatforms] = arr.toString()
            }
        }
    }

    suspend fun addHistory(keyword: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[keyHistory] ?: "").split("\u0001").filter { it.isNotBlank() }.toMutableList()
            current.remove(keyword)
            current.add(0, keyword)
            prefs[keyHistory] = current.take(HISTORY_MAX).joinToString("\u0001")
        }
    }

    suspend fun removeHistory(keyword: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[keyHistory] ?: "").split("\u0001").filter { it.isNotBlank() }.toMutableList()
            current.remove(keyword)
            prefs[keyHistory] = current.joinToString("\u0001")
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { it.remove(keyHistory) }
    }

    // ---------------- 备份与恢复 ----------------

    /** 导出全部本地配置为 JSON（用于换机迁移/恢复）。 */
    suspend fun buildBackupJson(): String {
        val data = context.dataStore.data.first()
        val custom = JSONArray()
        try {
            val raw = data[keyCustomPlatforms] ?: ""
            if (raw.isNotBlank()) {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) custom.put(arr.getJSONObject(i))
            }
        } catch (_: Exception) {
        }
        return JSONObject()
            .put("version", 1)
            .put("default_platform", data[keyDefaultPlatform] ?: "")
            .put("last_platform", data[keyLastPlatform] ?: "")
            .put("enabled_order", data[keyEnabledOrder] ?: "")
            .put("custom_platforms", custom)
            .put("history", data[keyHistory] ?: "")
            .put("prefer_launch_app", data[keyPreferLaunchApp] ?: true)
            .put("theme_mode", data[keyThemeMode] ?: THEME_SYSTEM)
            .put("jump_mode", data[keyJumpMode] ?: JUMP_IMMEDIATE)
            .put("haptics_enabled", data[keyHapticsEnabled] ?: true)
            .put("update_check_freq", data[keyUpdateCheckFreq] ?: UPDATE_FREQ_WEEKLY)
            .put("auto_download", data[keyAutoDownload] ?: false)
            .put("wifi_only", data[keyWifiOnly] ?: true)
            .put("auto_install", data[keyAutoInstall] ?: false)
            .put("update_channel", data[keyUpdateChannel] ?: UPDATE_CHANNEL_STABLE)
            .put("update_reminder", data[keyUpdateReminder] ?: UPDATE_REMINDER_NOTIFICATION)
            .put("last_update_check", data[keyLastUpdateCheck] ?: 0L)
            .put("update_history", data[keyUpdateHistory] ?: "")
            .put("widget_sort", data[keyWidgetSort] ?: WIDGET_SORT_ORDER)
            .put("widget_source", data[keyWidgetSource] ?: WIDGET_SOURCE_ENABLED)
            .put("widget_show_recent", data[keyWidgetShowRecent] ?: true)
            .put("widget_click_action", data[keyWidgetClickAction] ?: WIDGET_CLICK_DEFAULT)
            .put("widget_auto_focus", data[keyWidgetAutoFocus] ?: true)
            .put("widget_search_platforms", data[keyWidgetSearchPlatforms] ?: "")
            .put("widget_2x2_platforms", data[keyWidget2x2Platforms] ?: "")
            .put("widget_4x2_platforms", data[keyWidget4x2Platforms] ?: "")
            .toString(2)
    }

    /** 从备份 JSON 恢复全部配置。成功返回 true；解析失败返回 false 且不写任何数据。 */
    suspend fun restoreFromJson(json: String): Boolean {
        val obj = try {
            JSONObject(json)
        } catch (e: Exception) {
            return false
        }
        val customArr = obj.optJSONArray("custom_platforms")
        val customList = mutableListOf<CustomPlatformData>()
        if (customArr != null) {
            for (i in 0 until customArr.length()) {
                val p = customArr.getJSONObject(i)
                customList.add(
                    CustomPlatformData(
                        id = p.optString("id"),
                        name = p.optString("name"),
                        schemeTemplate = p.optString("scheme").takeIf { it.isNotBlank() },
                        webTemplate = p.optString("web"),
                    )
                )
            }
        }
        context.dataStore.edit { prefs ->
            obj.optString("default_platform").takeIf { it.isNotBlank() }?.let { prefs[keyDefaultPlatform] = it }
            obj.optString("last_platform").takeIf { it.isNotBlank() }?.let { prefs[keyLastPlatform] = it }
            obj.optString("enabled_order").takeIf { it.isNotBlank() }?.let { prefs[keyEnabledOrder] = it }
            obj.optString("history").takeIf { it.isNotBlank() }?.let { prefs[keyHistory] = it }
            prefs[keyPreferLaunchApp] = obj.optBoolean("prefer_launch_app", true)
            prefs[keyThemeMode] = obj.optString("theme_mode", THEME_SYSTEM)
            prefs[keyJumpMode] = obj.optString("jump_mode", JUMP_IMMEDIATE)
            prefs[keyHapticsEnabled] = obj.optBoolean("haptics_enabled", true)
            prefs[keyUpdateCheckFreq] = obj.optString("update_check_freq", UPDATE_FREQ_WEEKLY)
            prefs[keyAutoDownload] = obj.optBoolean("auto_download", false)
            prefs[keyWifiOnly] = obj.optBoolean("wifi_only", true)
            prefs[keyAutoInstall] = obj.optBoolean("auto_install", false)
            prefs[keyUpdateChannel] = obj.optString("update_channel", UPDATE_CHANNEL_STABLE)
            prefs[keyUpdateReminder] = obj.optString("update_reminder", UPDATE_REMINDER_NOTIFICATION)
            prefs[keyLastUpdateCheck] = obj.optLong("last_update_check", 0L)
            obj.optString("update_history").takeIf { it.isNotBlank() }?.let { prefs[keyUpdateHistory] = it }
            prefs[keyWidgetSort] = obj.optString("widget_sort", WIDGET_SORT_ORDER)
            prefs[keyWidgetSource] = obj.optString("widget_source", WIDGET_SOURCE_ENABLED)
            prefs[keyWidgetShowRecent] = obj.optBoolean("widget_show_recent", true)
            prefs[keyWidgetClickAction] = obj.optString("widget_click_action", WIDGET_CLICK_DEFAULT)
            prefs[keyWidgetAutoFocus] = obj.optBoolean("widget_auto_focus", true)
            obj.optString("widget_search_platforms").takeIf { it.isNotBlank() }?.let { prefs[keyWidgetSearchPlatforms] = it }
            obj.optString("widget_2x2_platforms").takeIf { it.isNotBlank() }?.let { prefs[keyWidget2x2Platforms] = it }
            obj.optString("widget_4x2_platforms").takeIf { it.isNotBlank() }?.let { prefs[keyWidget4x2Platforms] = it }
            if (customList.isEmpty()) {
                prefs.remove(keyCustomPlatforms)
            } else {
                val arr = JSONArray()
                customList.forEach { p ->
                    arr.put(JSONObject().put("id", p.id).put("name", p.name).put("scheme", p.schemeTemplate ?: "").put("web", p.webTemplate))
                }
                prefs[keyCustomPlatforms] = arr.toString()
            }
        }
        return true
    }
}