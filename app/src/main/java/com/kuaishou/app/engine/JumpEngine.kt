package com.kuaishou.app.engine

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
 * ① 主包精确唤起（已安装才尝试，未注册 scheme 会继续往下走）
 * ② 兜底包（如抖音主 App 缺失时唤起抖音极速版）：先查是否已安装，再试搜索协议，
 *    协议未注册则直接打开该 App 主页 —— 保证「无抖音、有极速版」时点击抖音能跳进极速版
 * ③ 系统解析唤起 → 均失败弹兜底抽屉（复制/网页版/首页）。
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
                if (isPackageInstalled(platform.packageName)) {
                    try {
                        context.startActivity(preciseIntent.setPackage(platform.packageName))
                        return JumpResult.LaunchedApp
                    } catch (e: ActivityNotFoundException) {
                        // 主包已安装但未注册该 scheme（极少见），继续尝试兜底
                    } catch (e: SecurityException) {
                        return JumpResult.Failed(platform, keyword)
                    }
                }
                // ② 依次尝试兜底包（如抖音极速版）：已安装 → 试搜索协议 → 未注册则打开 App 主页
                for (pkg in platform.fallbackPackages) {
                    if (!isPackageInstalled(pkg)) continue
                    try {
                        context.startActivity(preciseIntent.setPackage(pkg))
                        return JumpResult.LaunchedApp
                    } catch (e: ActivityNotFoundException) {
                        // 该兜底包未注册搜索协议：退而打开其 App 主页，同样算「跳转到该 App」
                        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (launch != null) {
                            try {
                                context.startActivity(launch)
                                return JumpResult.LaunchedApp
                            } catch (e2: Exception) {
                                // 启动主页也失败，尝试下一个兜底包
                            }
                        }
                    } catch (e: SecurityException) {
                        return JumpResult.Failed(platform, keyword)
                    }
                }
            }
            // ③ 系统解析唤起
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

    /**
     * 包是否已安装。Android 11+ 受包可见性限制：未在 Manifest <queries> 声明的包
     * 即使已安装也会查不到 —— 抖音极速版必须声明，否则兜底唤起必然失败。
     */
    private fun isPackageInstalled(pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun openWebUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // 无浏览器可用，极端情况忽略
        }
    }
}
