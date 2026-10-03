package com.kuaishou.app.engine

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kuaishou.app.data.Platform

/** 一次搜索跳转的结果。 */
sealed class JumpResult {
    /** 已唤起 App（Deep Link 命中）。 */
    data object LaunchedApp : JumpResult()

    /** 已打开网页搜索页（默认路径）。 */
    data object OpenedWeb : JumpResult()

    /** 唤起 App 失败（未安装 / Deep Link 失效），需要 UI 弹兜底抽屉。 */
    data class Failed(val platform: Platform, val keyword: String) : JumpResult()
}

/**
 * 跳转引擎：开启“优先唤起App”时（默认开启）先精确唤起本地 App：
 * ① 带包名精确唤起 → ② 系统解析唤起 → 均失败弹兜底抽屉（复制/网页版/首页）。
 * 关闭该偏好时直接打开网页搜索 URL，保证“直达搜索结果页”。
 */
class JumpEngine(private val context: Context) {

    fun search(platform: Platform, keyword: String, preferLaunchApp: Boolean): JumpResult {
        val encoded = Uri.encode(keyword)
        if (preferLaunchApp && platform.scheme != null) {
            val deepLink = platform.scheme.replace("{keyword}", encoded)
            // ① 带包名精确唤起（命中该 App 的 scheme 处理器）
            if (platform.packageName != null) {
                val preciseIntent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
                    .setPackage(platform.packageName)
                try {
                    context.startActivity(preciseIntent)
                    return JumpResult.LaunchedApp
                } catch (e: ActivityNotFoundException) {
                    // 包名命中失败，回落到系统解析
                } catch (e: SecurityException) {
                    return JumpResult.Failed(platform, keyword)
                }
            }
            // ② 系统解析唤起
            val systemIntent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            return try {
                context.startActivity(systemIntent)
                JumpResult.LaunchedApp
            } catch (e: ActivityNotFoundException) {
                JumpResult.Failed(platform, keyword)
            } catch (e: SecurityException) {
                JumpResult.Failed(platform, keyword)
            }
        }
        openWebUrl(platform.searchUrlTemplate.replace("{keyword}", encoded))
        return JumpResult.OpenedWeb
    }

    /** 打开网页版搜索页（兜底抽屉的“打开网页版”也走这里）。 */
    fun openWebSearch(platform: Platform, keyword: String) {
        openWebUrl(platform.searchUrlTemplate.replace("{keyword}", Uri.encode(keyword)))
    }

    /** 打开平台首页（兜底抽屉的“打开抖音首页”）。 */
    fun openPlatformHome(platform: Platform) {
        openWebUrl(platform.homeUrl)
    }

    private fun openWebUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // 无浏览器可用，极端情况忽略
        }
    }
}
