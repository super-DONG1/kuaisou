package com.kuaishou.app.ui

import android.Manifest
import android.app.ActivityManager
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.AppPrefs.UpdateRecord
import com.kuaishou.app.data.UPDATE_FREQ_DAILY
import com.kuaishou.app.data.UPDATE_FREQ_MANUAL
import com.kuaishou.app.data.UPDATE_FREQ_MONTHLY
import com.kuaishou.app.data.UPDATE_FREQ_WEEKLY
import com.kuaishou.app.data.UPDATE_REMINDER_DIALOG
import com.kuaishou.app.data.UPDATE_REMINDER_NONE
import com.kuaishou.app.data.UPDATE_REMINDER_NOTIFICATION
import com.kuaishou.app.data.appVersionName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 更新检查与下载：真实查询 GitHub Releases，并按设置下载 / 安装 APK。
 * 仓库固定为 super-DONG1/kuaisou（见 AboutScreen 的 REPO_URL）。
 */
object GitHubUpdateChecker {
    private const val OWNER = "super-DONG1"
    private const val REPO = "kuaisou"
    private const val API_LIST = "https://api.github.com/repos/$OWNER/$REPO/releases?per_page=30"
    const val REPO_URL = "https://github.com/$OWNER/$REPO"

    data class UpdateInfo(
        val version: String,
        val tag: String,
        val name: String,
        val notes: String,
        val releaseUrl: String,
        val apkUrl: String?,
        val publishedAt: Long,
        val prerelease: Boolean,
    )

    sealed interface CheckResult {
        data object UpToDate : CheckResult
        data class Found(val info: UpdateInfo) : CheckResult
        data class Error(val message: String) : CheckResult
    }

    /** 比较版本号：<0 表示 a<b，0 相等，>0 表示 a>b。支持 "1.0.0" 与 "1.1.0-beta.1"。 */
    fun compareVersion(a: String, b: String): Int {
        fun parse(v: String): Pair<List<Int>, String> {
            val parts = v.split("-", limit = 2)
            val main = parts[0]
            val suffix = if (parts.size > 1) parts[1] else ""
            val nums = main.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            return nums to suffix
        }
        val (an, asuf) = parse(a)
        val (bn, bsuf) = parse(b)
        val len = if (an.size > bn.size) an.size else bn.size
        for (i in 0 until len) {
            val x = an.getOrElse(i) { 0 }
            val y = bn.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return when {
            asuf.isEmpty() && bsuf.isEmpty() -> 0
            asuf.isEmpty() -> 1
            bsuf.isEmpty() -> -1
            else -> asuf.compareTo(bsuf)
        }
    }

    /** 请求 GitHub Releases 列表，按渠道筛选最新版本，并与当前版本比对。 */
    suspend fun check(currentVersion: String): CheckResult {
        return withContext(Dispatchers.IO) {
            try {
                val conn = (URL(API_LIST).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "kuaisou-android")
                    setRequestProperty("Accept", "application/vnd.github+json")
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val code = conn.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    conn.disconnect()
                    return@withContext CheckResult.Error("GitHub 返回 $code（可能限流或仓库不存在）")
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val release = pickRelease(JSONArray(text))
                    ?: return@withContext CheckResult.Error("该渠道暂无可用发布版本")
                val tag = release.optString("tag_name", "")
                val version = tag.removePrefix("v").ifBlank { tag }
                if (version.isBlank() || compareVersion(version, currentVersion) <= 0) {
                    return@withContext CheckResult.UpToDate
                }
                val assets = release.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.optJSONObject(i) ?: continue
                        val n = a.optString("name", "")
                        if (n.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = a.optString("browser_download_url", "").ifEmpty { null }
                            break
                        }
                    }
                }
                val notes = release.optString("body", "").trim().replace("\r", "").take(400)
                CheckResult.Found(
                    UpdateInfo(
                        version = version,
                        tag = tag,
                        name = release.optString("name", tag),
                        notes = notes,
                        releaseUrl = release.optString("html_url", REPO_URL),
                        apkUrl = apkUrl,
                        publishedAt = parseIso(release.optString("published_at", "")),
                        prerelease = release.optBoolean("prerelease", false),
                    ),
                )
            } catch (e: Exception) {
                CheckResult.Error(e.message ?: "网络请求失败")
            }
        }
    }

    /** 应用启动时按「检查频率 / 提醒方式」静默检查；仅在到达间隔时执行（手动模式不自动检查）。 */
    suspend fun autoCheck(context: Context, prefs: AppPrefs) {
        val freq = prefs.updateCheckFreq.first()
        if (freq == UPDATE_FREQ_MANUAL) return
        val last = prefs.lastUpdateCheck.first()
        val intervalMs = when (freq) {
            UPDATE_FREQ_DAILY -> 24L * 3_600_000
            UPDATE_FREQ_WEEKLY -> 7L * 24 * 3_600_000
            UPDATE_FREQ_MONTHLY -> 30L * 24 * 3_600_000
            else -> 7L * 24 * 3_600_000
        }
        if (System.currentTimeMillis() - last < intervalMs) return

        val current = currentVersion(context)
        when (val result = check(current)) {
            is CheckResult.UpToDate -> prefs.setLastUpdateCheck(System.currentTimeMillis())
            is CheckResult.Found -> {
                prefs.setLastUpdateCheck(System.currentTimeMillis())
                prefs.addUpdateHistory(
                    UpdateRecord(
                        version = result.info.version,
                        time = if (result.info.publishedAt > 0) result.info.publishedAt else System.currentTimeMillis(),
                        status = "自动检查发现新版本",
                    ),
                )
                when (prefs.updateReminder.first()) {
                    UPDATE_REMINDER_NOTIFICATION -> notifyUpdate(context, result.info)
                    UPDATE_REMINDER_DIALOG -> Toast.makeText(
                        context,
                        "发现新版本 v${result.info.version}，请到「更新设置」查看",
                        Toast.LENGTH_LONG,
                    ).show()
                    UPDATE_REMINDER_NONE -> { /* 静默 */ }
                }
            }
            is CheckResult.Error -> { /* 静默忽略，不打扰用户 */ }
        }
    }

    private fun pickRelease(arr: JSONArray): JSONObject? {
        // 只接收正式版（跳过 Pre-release 与草稿）
        val candidates = mutableListOf<JSONObject>()
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            if (r.optBoolean("draft", false)) continue
            if (r.optBoolean("prerelease", false)) continue
            candidates.add(r)
        }
        if (candidates.isEmpty()) return null
        candidates.sortByDescending { parseIso(it.optString("published_at", "")) }
        return candidates.first()
    }

    /**
     * 当前已安装版本号。直接取 [appVersionName]（构建期生成、与 build.gradle.kts 同源），
     * 不再读 PackageManager —— 那条路径需要一个硬编码兜底值，改版本时极易漏改而误报「有新版本」。
     */
    internal fun currentVersion(context: Context): String = appVersionName

    private fun parseIso(s: String): Long {        if (s.isBlank()) return 0L
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(s)?.time ?: 0L
        } catch (_: Exception) { 0L }
    }

    /**
     * 通过 DownloadManager 下载 APK；完成后回调本地 Uri（失败为 null）。
     * 关键：先注册完成广播再入队，避免小安装包秒下完时广播先于注册发出而永久漏收；
     * 完成后优先取 LOCAL_URI（content://），为空时回退 LOCAL_FILENAME 拼 file://。
     */
    fun downloadApk(context: Context, apkUrl: String, onResult: (Uri?) -> Unit) {
        val appCtx = context.applicationContext
        val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("快搜 更新下载")
            setDescription("正在下载安装包…")
            setMimeType("application/vnd.android.package-archive")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(appCtx, Environment.DIRECTORY_DOWNLOADS, "kuaisou_update.apk")
        }
        // 先声明下载 id（-1 占位），注册完成后 enqueue 赋值；
        // 接收器闭包捕获该 var，广播到达时读到的即真实 id
        var id = -1L
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val ref = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (ref != id) return
                try { appCtx.unregisterReceiver(this) } catch (_: Exception) {}
                val c = dm.query(DownloadManager.Query().setFilterById(id))
                var uri: Uri? = null
                if (c.moveToFirst()) {
                    if (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        // Android 10+ 下 LOCAL_URI 通常是 content://（MediaStore/FileProvider）；
                        // 个别 ROM 可能返回空，回退 LOCAL_FILENAME（file://）兜底
                        val localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        uri = if (!localUri.isNullOrBlank()) {
                            Uri.parse(localUri)
                        } else {
                            val file = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME))
                            if (file.isNullOrBlank()) null else Uri.fromFile(java.io.File(file))
                        }
                    }
                }
                c.close()
                onResult(uri)
            }
        }
        // 先注册再入队：防止下载完成广播早于接收器注册而漏收
        ContextCompat.registerReceiver(
            appCtx,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        id = dm.enqueue(req)
    }

    /** 是否允许安装未知来源应用（Android O+）。 */
    fun canInstallPackages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    /** 拉起系统安装器安装本地 APK。 */
    fun installApk(context: Context, uri: Uri) {
        val appCtx = context.applicationContext
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appCtx.startActivity(intent)
        } catch (e: Exception) {
            // 个别 ROM 对 ACTION_INSTALL_PACKAGE 支持不佳，回退到 ACTION_VIEW 安装
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                appCtx.startActivity(fallback)
            } catch (e2: Exception) {
                // 均失败：留给调用方提示
            }
        }
    }

    /** 下载完成后的安装动作，供调用方给出对应反馈。 */
    sealed class InstallAction {
        /** 已直接拉起系统安装器。 */
        data object Installing : InstallAction()
        /** 应用在后台无法直接弹安装器，已发「点击安装」通知引导。 */
        data object PromptedByNotification : InstallAction()
        /** 未授予「安装未知应用」权限，需跳系统设置页。 */
        data object NeedPermission : InstallAction()
    }

    /**
     * 下载完成后的自动安装决策（前台直接装；后台发通知引导，规避 Android 10+
     * 后台启动 Activity 限制；未授权则提示跳设置）。返回动作供 UI 反馈。
     */
    fun installDownloadedApk(context: Context, uri: Uri): InstallAction {
        val appCtx = context.applicationContext
        if (!canInstallPackages(appCtx)) return InstallAction.NeedPermission
        if (isAppForeground(appCtx)) {
            installApk(appCtx, uri)
            return InstallAction.Installing
        }
        notifyInstallPrompt(appCtx, uri)
        return InstallAction.PromptedByNotification
    }

    /** 应用是否处于前台（用于判断能否直接拉起安装器）。 */
    private fun isAppForeground(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        return try {
            am.appTasks.any { it.taskInfo?.topActivity?.packageName == context.packageName }
        } catch (_: Exception) {
            false
        }
    }

    /** 后台场景：发「更新已下载，点击安装」通知，PendingIntent 豁免后台启动限制。 */
    private fun notifyInstallPrompt(context: Context, uri: Uri) {
        val appCtx = context.applicationContext
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "update_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "更新提醒", NotificationManager.IMPORTANCE_HIGH),
            )
        }
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            appCtx,
            1,
            installIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(appCtx, channelId)
            .setContentTitle("快搜 更新下载完成")
            .setContentText("点击安装新版本")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            nm.notify(1002, notif)
        } catch (_: Exception) {
            // 通知权限缺失等极端情况：静默失败
        }
    }

    /** 拉起系统「安装未知应用」设置页（Android O+）。 */
    fun openInstallSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** 在浏览器中打开发布页。 */
    fun openRelease(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** 发送更新通知（提醒方式为通知栏时）。 */
    fun notifyUpdate(context: Context, info: UpdateInfo) {
        val appCtx = context.applicationContext
        // Android 13+ 需要「通知」运行时权限，未授权时通知不会展示，静默跳过
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(appCtx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "update_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "更新提醒", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = android.app.PendingIntent.getActivity(
            appCtx,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val firstLine = info.notes.lines().firstOrNull()?.take(60) ?: "点击前往发布页查看与下载"
        val notif = NotificationCompat.Builder(appCtx, channelId)
            .setContentTitle("快搜 发现新版本 v${info.version}")
            .setContentText(firstLine)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(1001, notif)
    }
}
