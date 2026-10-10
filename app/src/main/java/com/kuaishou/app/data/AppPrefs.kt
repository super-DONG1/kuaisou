package com.kuaishou.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ---------- 用户协议 / 隐私政策正文 ----------

const val USER_AGREEMENT = """
《快搜用户协议》

欢迎使用快搜。本协议是你（以下称“用户”或“你”）与快搜应用（以下称“本应用”或“我们”）之间就下载、安装、使用本应用所订立的协议。

一、服务说明
1. 本应用是一个本地搜索跳转工具，帮助你在抖音、小红书、哔哩哔哩等平台之间快速发起搜索。
2. 本应用不提供任何内容存储服务，不展示信息流，也不参与平台内容运营。

二、账号与使用
1. 本应用不需要注册账号，无需登录即可使用全部功能。
2. 你应合法合规地使用本应用，不得利用本应用从事任何违反法律法规的活动。

三、跳转服务
1. 本应用通过各平台的官方或公开的跳转协议（Deep Link）发起搜索，跳转能力取决于你设备上安装的对应应用及其版本。
2. 若对应应用未安装或跳转协议失效，本应用会提供网页版搜索作为兜底方案。

四、隐私保护
你的全部数据（搜索历史、平台偏好等）仅保存在你的设备本地，我们不对其进行收集、上传或分析。详见《隐私政策》。

五、免责声明
1. 本应用按“现状”提供，不对跳转成功率、目标应用兼容性作任何明示或默示的保证。
2. 因使用本应用产生的任何直接或间接损失，我们不承担法律责任（法律另有规定的除外）。

六、协议变更
我们可能适时修订本协议，修订后将在应用内展示。继续使用即视为接受修订后的协议。
"""

const val PRIVACY_POLICY = """
《快搜隐私政策》

我们非常重视你的隐私。本政策说明本应用如何处理你的信息。

一、我们不收集的信息
1. 我们不设服务器，不收集任何搜索词。
2. 我们不收集你的身份信息、位置信息、设备标识（如 IMEI、OAID）。
3. 我们不使用任何统计 SDK 或广告 SDK。

二、仅保存在本地的数据
以下数据仅保存在你的设备本地，不上传、不共享：
1. 搜索历史与最近搜索记录；
2. 平台显示/排序偏好；
3. 默认平台、主题外观等设置项；
4. 你的自定义平台配置。

三、剪贴板与跳转
1. 当你使用“复制关键词”功能时，本应用会向系统剪贴板写入关键词，该操作仅在你主动触发时进行。
2. 本应用启动其他应用（如抖音、小红书）时，仅传递你输入的搜索关键词，不读取目标应用内数据。

四、第三方
本应用跳转到的各平台 App 及其网页版分别适用各自的隐私政策，与本应用无关。

五、未成年人保护
本应用不针对未成年人提供服务，若你为未成年人，请在监护人指导下使用。

六、联系我们
如你对本政策有任何疑问，可通过应用内“问题反馈”渠道联系我们。
"""

// ---------- 偏好键名（单一来源，避免散落的魔法字符串） ----------

const val KEY_DEFAULT_PLATFORM = "default_platform"
const val KEY_ENABLED_PLATFORMS = "enabled_platforms"
const val KEY_CUSTOM_PLATFORMS = "custom_platforms"
const val KEY_HISTORY = "history"
const val KEY_PLATFORM_CLICKS = "platform_clicks"
const val KEY_THEME_MODE = "theme_mode"
const val KEY_JUMP_MODE = "jump_mode"
const val KEY_HAptics_ENABLED = "haptics_enabled"
const val KEY_AUTO_OPEN_APP = "auto_open_app"
const val KEY_AGREEMENT_ACCEPTED = "agreement_accepted"
const val KEY_UPDATE_CHECK_INTERVAL = "update_check_interval"
const val KEY_LAST_UPDATE_CHECK = "last_update_check"
const val KEY_SHOW_WIDGET_KEYBOARD = "show_widget_keyboard"
const val KEY_WIDGET_PLATFORMS_4X1 = "widget_platforms_4x1"
const val KEY_WIDGET_PLATFORMS_2X2 = "widget_platforms_2x2"
const val KEY_WIDGET_PLATFORMS_4X2 = "widget_platforms_4x2"

// ---------- 枚举取值（与偏好存储值对应） ----------

const val THEME_SYSTEM = "system"
const val THEME_LIGHT = "light"
const val THEME_DARK = "dark"

const val JUMP_IMMEDIATE = "immediate"
const val JUMP_COPY_THEN_JUMP = "copy_then_jump"

const val UPDATE_CHECK_NEVER = "never"
const val UPDATE_CHECK_MANUAL = "manual"
const val UPDATE_CHECK_DAILY = "daily"
const val UPDATE_CHECK_WEEKLY = "weekly"

/** 默认启用平台（首次安装时的初始值，可被“恢复默认”重置）。 */
val DEFAULT_ENABLED_PLATFORMS = listOf(
    "douyin", "xiaohongshu", "bilibili", "zhihu", "weibo", "taobao",
    "baidu", "quark", "weixin", "kuaishou", "douban", "bing",
)

/**
 * 全部偏好读写入口（SharedPreferences 封装 + 响应式 StateFlow）。
 * 所有值均为带默认值的一次性读取，写入后同步更新 StateFlow，供 Compose 观察。
 */
class AppPrefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("kuaishou_prefs", Context.MODE_PRIVATE)

    // ---- 基础偏好（StateFlow 缓存，写后即发） ----

    private val _defaultPlatformId = MutableStateFlow(
        sp.getString(KEY_DEFAULT_PLATFORM, DEFAULT_ENABLED_PLATFORMS.first())
            ?: DEFAULT_ENABLED_PLATFORMS.first()
    )
    val defaultPlatformId: StateFlow<String> = _defaultPlatformId.asStateFlow()

    private val _enabledPlatformIds = MutableStateFlow(
        sp.getStringSet(KEY_ENABLED_PLATFORMS, null)
            ?.toList()
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_ENABLED_PLATFORMS
    )
    val enabledPlatformIds: StateFlow<List<String>> = _enabledPlatformIds.asStateFlow()

    private val _customPlatforms = MutableStateFlow(
        sp.getString(KEY_CUSTOM_PLATFORMS, null)?.let { parseCustomPlatforms(it) } ?: emptyList()
    )
    val customPlatforms: StateFlow<List<CustomPlatform>> = _customPlatforms.asStateFlow()

    private val _history = MutableStateFlow(
        sp.getString(KEY_HISTORY, null)?.split('\u0001')?.filter { it.isNotBlank() } ?: emptyList()
    )
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _platformClicks = MutableStateFlow(
        sp.getString(KEY_PLATFORM_CLICKS, null)
            ?.split('\u0001')
            ?.mapNotNull { line -> line.split(':', limit = 2).let { if (it.size == 2) it[0] to it[1].toIntOrNull() ?: 0 else null } }
            ?.toMap()
            ?: emptyMap()
    )
    val platformClicks: StateFlow<Map<String, Int>> = _platformClicks.asStateFlow()

    private val _themeMode = MutableStateFlow(
        sp.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _jumpMode = MutableStateFlow(
        sp.getString(KEY_JUMP_MODE, JUMP_IMMEDIATE) ?: JUMP_IMMEDIATE
    )
    val jumpMode: StateFlow<String> = _jumpMode.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(sp.getBoolean(KEY_HAptics_ENABLED, true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _autoOpenApp = MutableStateFlow(sp.getBoolean(KEY_AUTO_OPEN_APP, true))
    val autoOpenApp: StateFlow<Boolean> = _autoOpenApp.asStateFlow()

    private val _agreementAccepted = MutableStateFlow(sp.getBoolean(KEY_AGREEMENT_ACCEPTED, false))
    val agreementAccepted: StateFlow<Boolean> = _agreementAccepted.asStateFlow()

    private val _updateCheckInterval = MutableStateFlow(
        sp.getString(KEY_UPDATE_CHECK_INTERVAL, UPDATE_CHECK_WEEKLY) ?: UPDATE_CHECK_WEEKLY
    )
    val updateCheckInterval: StateFlow<String> = _updateCheckInterval.asStateFlow()

    private val _lastUpdateCheck = MutableStateFlow(sp.getLong(KEY_LAST_UPDATE_CHECK, 0L))
    val lastUpdateCheck: StateFlow<Long> = _lastUpdateCheck.asStateFlow()

    private val _showWidgetKeyboard = MutableStateFlow(sp.getBoolean(KEY_SHOW_WIDGET_KEYBOARD, true))
    val showWidgetKeyboard: StateFlow<Boolean> = _showWidgetKeyboard.asStateFlow()

    private val _widgetPlatforms4x1 = MutableStateFlow(
        sp.getStringSet(KEY_WIDGET_PLATFORMS_4X1, null)?.toList() ?: emptyList()
    )
    val widgetPlatforms4x1: StateFlow<List<String>> = _widgetPlatforms4x1.asStateFlow()

    private val _widgetPlatforms2x2 = MutableStateFlow(
        sp.getStringSet(KEY_WIDGET_PLATFORMS_2X2, null)?.toList() ?: emptyList()
    )
    val widgetPlatforms2x2: StateFlow<List<String>> = _widgetPlatforms2x2.asStateFlow()

    private val _widgetPlatforms4x2 = MutableStateFlow(
        sp.getStringSet(KEY_WIDGET_PLATFORMS_4X2, null)?.toList() ?: emptyList()
    )
    val widgetPlatforms4x2: StateFlow<List<String>> = _widgetPlatforms4x2.asStateFlow()

    // ---- 写方法（写 SharedPreferences + 更新 StateFlow） ----

    fun setDefaultPlatform(id: String) {
        sp.edit().putString(KEY_DEFAULT_PLATFORM, id).apply()
        _defaultPlatformId.value = id
    }

    fun setEnabledPlatforms(ids: List<String>) {
        sp.edit().putStringSet(KEY_ENABLED_PLATFORMS, ids.toSet()).apply()
        _enabledPlatformIds.value = ids
    }

    fun setCustomPlatforms(list: List<CustomPlatform>) {
        sp.edit().putString(KEY_CUSTOM_PLATFORMS, list.joinToString("\u0002") { it.toLine() }).apply()
        _customPlatforms.value = list
    }

    fun addHistory(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        val updated = (listOf(trimmed) + _history.value.filter { it != trimmed }).take(10)
        sp.edit().putString(KEY_HISTORY, updated.joinToString("\u0001")).apply()
        _history.value = updated
    }

    fun clearHistory() {
        sp.edit().remove(KEY_HISTORY).apply()
        _history.value = emptyList()
    }

    fun recordPlatformClick(platformId: String) {
        val updated = _platformClicks.value.toMutableMap()
        updated[platformId] = (updated[platformId] ?: 0) + 1
        sp.edit().putString(
            KEY_PLATFORM_CLICKS,
            updated.entries.joinToString("\u0001") { "${it.key}:${it.value}" },
        ).apply()
        _platformClicks.value = updated
    }

    fun setThemeMode(mode: String) {
        sp.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setJumpMode(mode: String) {
        sp.edit().putString(KEY_JUMP_MODE, mode).apply()
        _jumpMode.value = mode
    }

    fun setHapticsEnabled(enabled: Boolean) {
        sp.edit().putBoolean(KEY_HAptics_ENABLED, enabled).apply()
        _hapticsEnabled.value = enabled
    }

    fun setAutoOpenApp(enabled: Boolean) {
        sp.edit().putBoolean(KEY_AUTO_OPEN_APP, enabled).apply()
        _autoOpenApp.value = enabled
    }

    fun setAgreementAccepted(accepted: Boolean) {
        sp.edit().putBoolean(KEY_AGREEMENT_ACCEPTED, accepted).apply()
        _agreementAccepted.value = accepted
    }

    fun setUpdateCheckInterval(interval: String) {
        sp.edit().putString(KEY_UPDATE_CHECK_INTERVAL, interval).apply()
        _updateCheckInterval.value = interval
    }

    fun setLastUpdateCheck(timestamp: Long) {
        sp.edit().putLong(KEY_LAST_UPDATE_CHECK, timestamp).apply()
        _lastUpdateCheck.value = timestamp
    }

    fun setShowWidgetKeyboard(enabled: Boolean) {
        sp.edit().putBoolean(KEY_SHOW_WIDGET_KEYBOARD, enabled).apply()
        _showWidgetKeyboard.value = enabled
    }

    fun setWidgetPlatforms(sizeKey: String, ids: List<String>) {
        when (sizeKey) {
            KEY_WIDGET_PLATFORMS_4X1 -> {
                sp.edit().putStringSet(KEY_WIDGET_PLATFORMS_4X1, ids.toSet()).apply()
                _widgetPlatforms4x1.value = ids
            }
            KEY_WIDGET_PLATFORMS_2X2 -> {
                sp.edit().putStringSet(KEY_WIDGET_PLATFORMS_2X2, ids.toSet()).apply()
                _widgetPlatforms2x2.value = ids
            }
            KEY_WIDGET_PLATFORMS_4X2 -> {
                sp.edit().putStringSet(KEY_WIDGET_PLATFORMS_4X2, ids.toSet()).apply()
                _widgetPlatforms4x2.value = ids
            }
        }
    }

    // ---- 备份 / 恢复 ----

    /** 导出全部设置（不含协议同意状态），供备份。 */
    fun exportJson(): String = buildString {
        append("{")
        append("\"defaultPlatform\":\"${_defaultPlatformId.value}\",")
        append("\"enabledPlatforms\":[")
        append(_enabledPlatformIds.value.joinToString(",") { "\"$it\"" })
        append("],")
        append("\"customPlatforms\":[")
        append(_customPlatforms.value.joinToString(",") { it.toJson() })
        append("],")
        append("\"history\":[")
        append(_history.value.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" })
        append("],")
        append("\"platformClicks\":{")
        append(
            _platformClicks.value.entries.joinToString(",") { "\"${it.key}\":${it.value}" }
        )
        append("},")
        append("\"themeMode\":\"${_themeMode.value}\",")
        append("\"jumpMode\":\"${_jumpMode.value}\",")
        append("\"hapticsEnabled\":${_hapticsEnabled.value},")
        append("\"autoOpenApp\":${_autoOpenApp.value}")
        append("}")
    }

    /** 从备份 JSON 恢复设置（协议同意状态不恢复）。 */
    fun importJson(json: String): Boolean = try {
        val obj = org.json.JSONObject(json)
        obj.optString("defaultPlatform").takeIf { it.isNotBlank() }?.let { setDefaultPlatform(it) }
        obj.optJSONArray("enabledPlatforms")?.let { arr ->
            setEnabledPlatforms((0 until arr.length()).map { arr.getString(it) })
        }
        obj.optJSONArray("customPlatforms")?.let { arr ->
            val list = (0 until arr.length()).map { i ->
                val item = arr.getJSONObject(i)
                CustomPlatform(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    scheme = item.optString("scheme"),
                    webUrl = item.optString("webUrl"),
                    iconText = item.optString("iconText"),
                )
            }
            setCustomPlatforms(list)
        }
        obj.optJSONArray("history")?.let { arr ->
            val list = (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
            sp.edit().putString(KEY_HISTORY, list.joinToString("\u0001")).apply()
            _history.value = list
        }
        obj.optJSONObject("platformClicks")?.let { o ->
            val map = o.keys().asSequence().associateWith { o.optInt(it, 0) }
            sp.edit().putString(KEY_PLATFORM_CLICKS, map.entries.joinToString("\u0001") { "${it.key}:${it.value}" }).apply()
            _platformClicks.value = map
        }
        obj.optString("themeMode").takeIf { it.isNotBlank() }?.let { setThemeMode(it) }
        obj.optString("jumpMode").takeIf { it.isNotBlank() }?.let { setJumpMode(it) }
        if (obj.has("hapticsEnabled")) setHapticsEnabled(obj.optBoolean("hapticsEnabled"))
        if (obj.has("autoOpenApp")) setAutoOpenApp(obj.optBoolean("autoOpenApp"))
        true
    } catch (e: Exception) {
        false
    }

    /** 恢复出厂设置（保留协议同意状态，避免再次弹窗）。 */
    fun resetAll() {
        sp.edit()
            .remove(KEY_DEFAULT_PLATFORM)
            .remove(KEY_ENABLED_PLATFORMS)
            .remove(KEY_CUSTOM_PLATFORMS)
            .remove(KEY_HISTORY)
            .remove(KEY_PLATFORM_CLICKS)
            .remove(KEY_THEME_MODE)
            .remove(KEY_JUMP_MODE)
            .remove(KEY_HAptics_ENABLED)
            .remove(KEY_AUTO_OPEN_APP)
            .apply()
        _defaultPlatformId.value = DEFAULT_ENABLED_PLATFORMS.first()
        _enabledPlatformIds.value = DEFAULT_ENABLED_PLATFORMS
        _customPlatforms.value = emptyList()
        _history.value = emptyList()
        _platformClicks.value = emptyMap()
        _themeMode.value = THEME_SYSTEM
        _jumpMode.value = JUMP_IMMEDIATE
        _hapticsEnabled.value = true
        _autoOpenApp.value = true
    }
}

/** 自定义平台（用户添加的小众 App 或网页）。 */
data class CustomPlatform(
    val id: String,
    val name: String,
    val scheme: String,
    val webUrl: String,
    val iconText: String = name.take(1),
) {
    /** 持久化到单行字符串（\u0002 分隔字段）。 */
    fun toLine(): String = listOf(id, name, scheme, webUrl, iconText).joinToString("\u0002")

    /** 备份 JSON 片段。 */
    fun toJson(): String =
        "{\"id\":\"$id\",\"name\":\"$name\",\"scheme\":\"$scheme\",\"webUrl\":\"$webUrl\",\"iconText\":\"$iconText\"}"
}

/** 解析自定义平台存储串（\u0002 分隔每条，字段内 \u0002 分隔）。 */
fun parseCustomPlatforms(raw: String): List<CustomPlatform> =
    raw.split("\u0001").filter { it.isNotBlank() }.mapNotNull { line ->
        val parts = line.split("\u0002", limit = 5)
        if (parts.size >= 4) {
            CustomPlatform(
                id = parts[0],
                name = parts[1],
                scheme = parts[2],
                webUrl = parts[3],
                iconText = parts.getOrNull(4)?.takeIf { it.isNotBlank() } ?: parts[1].take(1),
            )
        } else null
    }
