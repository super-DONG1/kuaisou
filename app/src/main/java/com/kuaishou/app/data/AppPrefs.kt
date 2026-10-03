package com.kuaishou.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kuaishou_prefs")

/** 本地偏好：默认/上次平台、启用平台顺序、最近搜索、唤起偏好。 */
class AppPrefs(private val context: Context) {

    private val keyDefaultPlatform = stringPreferencesKey("default_platform")
    private val keyLastPlatform = stringPreferencesKey("last_platform")
    private val keyEnabledOrder = stringPreferencesKey("enabled_order")
    private val keyHistory = stringPreferencesKey("history")
    private val keyPreferLaunchApp = booleanPreferencesKey("prefer_launch_app")

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

    suspend fun setDefaultPlatform(id: String) {
        context.dataStore.edit { it[keyDefaultPlatform] = id }
    }

    suspend fun setLastPlatform(id: String) {
        context.dataStore.edit { it[keyLastPlatform] = id }
    }

    suspend fun setEnabledOrder(ids: List<String>) {
        context.dataStore.edit { it[keyEnabledOrder] = ids.joinToString(",") }
    }

    suspend fun addHistory(keyword: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[keyHistory] ?: "").split("\u0001").filter { it.isNotBlank() }.toMutableList()
            current.remove(keyword)
            current.add(0, keyword)
            prefs[keyHistory] = current.take(10).joinToString("\u0001")
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

    suspend fun setPreferLaunchApp(enabled: Boolean) {
        context.dataStore.edit { it[keyPreferLaunchApp] = enabled }
    }
}
