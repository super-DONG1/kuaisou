package com.kuaishou.app.data

import android.content.Context
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 联想词来源：用于浮层左侧图标区分（网络联想 / 本地历史）。 */
enum class SuggestionSource { Network, History }

/** 单条联想词：文本 + 来源。 */
data class SuggestionItem(val text: String, val source: SuggestionSource)

/**
 * 搜索联想仓库（数据层）：
 * - 百度 Suggest 网络请求，请求头伪装 Chrome 移动端 UA + Referer 防反爬；
 * - 防抖与状态管理放在 [HomeViewModel]，这里只负责「给定输入词 → 返回联想词」；
 * - 使用 [LruCache] 缓存相同输入词的结果；
 * - 网络超时（[NETWORK_TIMEOUT_MS]）/ 断网 / 接口失效时，无缝降级为本地历史记录模糊匹配；
 * - 任何异常都被吞掉，绝不向上抛出导致 App 崩溃。
 */
class SearchRepository(private val context: Context, private val prefs: AppPrefs) {

    /** 百度联想两个可用 host，互为备援。 */
    private val BAIDU_HOSTS = listOf("suggestion.baidu.com", "www.baidu.com")

    private val cache = LruCache<String, List<SuggestionItem>>(256)

    /** 针对输入词返回联想词（最多 [MAX_ITEMS] 条）。网络优先，失败降级本地历史。 */
    suspend fun suggest(query: String): List<SuggestionItem> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        cache.get(q)?.let { return it }
        val net = withTimeoutOrNull(NETWORK_TIMEOUT_MS) { fetchBaidu(q) }
        val result = if (net.isNullOrEmpty()) fallbackLocal(q) else net
        Log.d("SearchRepo", "suggest(q='$q') net=${net?.size ?: "null"} -> result=${result.size} (${if (net.isNullOrEmpty()) "history" else "network"})")
        if (result.isNotEmpty()) cache.put(q, result)
        return result
    }

    /**
     * 拉取百度联想词。用系统 HttpURLConnection 直连（不引入 OkHttp/Okio，省下可观 dex 体积）：
     * 请求头伪装 Chrome 移动端 UA + Referer 防反爬，超时沿用原来的 1500ms 口径。
     */
    private suspend fun fetchBaidu(q: String): List<SuggestionItem>? = withContext(Dispatchers.IO) {
        for (host in BAIDU_HOSTS) {
            try {
                val url = URL(
                    "https://$host/su?wd=${URLEncoder.encode(q, "UTF-8")}&cb=window.baidu.sug"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", CHROME_MOBILE_UA)
                    setRequestProperty("Referer", "https://www.baidu.com/")
                    connectTimeout = HTTP_TIMEOUT_MS.toInt()
                    readTimeout = HTTP_TIMEOUT_MS.toInt()
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        Log.d("SearchRepo", "fetchBaidu host=$host http=$code")
                        continue
                    }
                    val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val parsed = parseBaidu(body)
                        .map { SuggestionItem(it, SuggestionSource.Network) }
                        .take(MAX_ITEMS)
                    Log.d("SearchRepo", "fetchBaidu host=$host got ${parsed.size} items")
                    if (parsed.isNotEmpty()) return@withContext parsed
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.d("SearchRepo", "fetchBaidu host=$host failed: ${e.message}")
            }
        }
        null
    }

    /**
     * 解析 `window.baidu.sug({q:"考研",p:false,s:["考研数学",...]})` 中的 s 数组。
     * 用正则提取数组文本后，再逐条提取被双引号包裹的字符串，并还原转义。
     */
    private fun parseBaidu(body: String): List<String> {
        val arrayMatch = BAIDU_ARRAY_REGEX.find(body) ?: return emptyList()
        val arrayContent = arrayMatch.groupValues[1]
        return STRING_ITEM_REGEX.findAll(arrayContent)
            .map { it.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\") }
            .toList()
    }

    /** 本地历史模糊匹配降级：包含输入词、且不等于输入词的历史项。 */
    private suspend fun fallbackLocal(q: String): List<SuggestionItem> {
        return prefs.history.first()
            .filter { it.contains(q, ignoreCase = true) && it != q }
            .take(MAX_ITEMS)
            .map { SuggestionItem(it, SuggestionSource.History) }
    }

    private companion object {
        private const val CHROME_MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val NETWORK_TIMEOUT_MS = 2500L
        /** 连接 / 读取超时：移动网络下 DNS+TLS 握手常 >800ms，留足余量，外层另有总超时兜底。 */
        private const val HTTP_TIMEOUT_MS = 1500L
        private const val MAX_ITEMS = 6
        private val BAIDU_ARRAY_REGEX = Regex("\"s\"\\s*:\\s*\\[([^\\]]*)\\]")
        private val STRING_ITEM_REGEX = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")
    }
}
