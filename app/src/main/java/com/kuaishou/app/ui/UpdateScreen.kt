package com.kuaishou.app.ui

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.app.data.AppPrefs
import com.kuaishou.app.data.AppPrefs.UpdateRecord
import com.kuaishou.app.data.UPDATE_CHANNEL_ALL
import com.kuaishou.app.data.UPDATE_CHANNEL_BETA
import com.kuaishou.app.data.UPDATE_CHANNEL_STABLE
import com.kuaishou.app.data.UPDATE_FREQ_DAILY
import com.kuaishou.app.data.UPDATE_FREQ_MANUAL
import com.kuaishou.app.data.UPDATE_FREQ_MONTHLY
import com.kuaishou.app.data.UPDATE_FREQ_WEEKLY
import com.kuaishou.app.data.UPDATE_REMINDER_DIALOG
import com.kuaishou.app.data.UPDATE_REMINDER_NONE
import com.kuaishou.app.data.UPDATE_REMINDER_NOTIFICATION
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatTime(ts: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))

private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    return cm?.activeNetworkInfo?.isConnectedOrConnecting == true
}

/**
 * 更新设置：将原先分散在设置页的更新系列功能整合到统一页面，分区展示。
 * - 检查与下载：检查频率 / 自动下载 / 仅 Wi-Fi 下载 / 自动安装
 * - 渠道与提醒：更新渠道 / 提醒方式
 * - 历史与检查：更新历史 / 手动检查更新
 */
@Composable
fun UpdateScreen(
    padding: PaddingValues,
    prefs: AppPrefs,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticsOn by prefs.hapticsEnabled.collectAsState(initial = true)

    val updateCheckFreq by prefs.updateCheckFreq.collectAsState(initial = UPDATE_FREQ_WEEKLY)
    val autoDownload by prefs.autoDownload.collectAsState(initial = false)
    val wifiOnlyDownload by prefs.wifiOnlyDownload.collectAsState(initial = true)
    val autoInstall by prefs.autoInstall.collectAsState(initial = false)
    val updateChannel by prefs.updateChannel.collectAsState(initial = UPDATE_CHANNEL_STABLE)
    val updateReminder by prefs.updateReminder.collectAsState(initial = UPDATE_REMINDER_NOTIFICATION)
    val lastUpdateCheck by prefs.lastUpdateCheck.collectAsState(initial = 0L)
    val updateHistory by prefs.updateHistory.collectAsState(initial = emptyList())

    var showCheckFreqDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    // 清空更新历史的二次确认
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<GitHubUpdateChecker.UpdateInfo?>(null) }

    fun tick() = hapticTick(context, hapticsOn)
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val checkFreqText = when (updateCheckFreq) {
        UPDATE_FREQ_MANUAL -> "手动检查"
        UPDATE_FREQ_DAILY -> "每天"
        UPDATE_FREQ_WEEKLY -> "每周"
        UPDATE_FREQ_MONTHLY -> "每月"
        else -> "每周"
    }
    val channelText = when (updateChannel) {
        UPDATE_CHANNEL_STABLE -> "稳定版"
        UPDATE_CHANNEL_BETA -> "测试版"
        UPDATE_CHANNEL_ALL -> "全部渠道"
        else -> "稳定版"
    }
    val reminderText = when (updateReminder) {
        UPDATE_REMINDER_NOTIFICATION -> "通知栏"
        UPDATE_REMINDER_DIALOG -> "弹窗"
        UPDATE_REMINDER_NONE -> "不提醒"
        else -> "通知栏"
    }
    val lastCheckText = if (lastUpdateCheck == 0L) "从未检查" else "上次检查：${formatTime(lastUpdateCheck)}"

    /** 手动检查更新：真实查询 GitHub Releases，比对版本后反馈，并按开关触发下载/安装。 */
    /** 下载并（可选）安装 APK；无 APK 时回退到打开发布页。 */
    fun startDownload(info: GitHubUpdateChecker.UpdateInfo, installAfter: Boolean) {
        val apk = info.apkUrl
        if (apk == null) {
            toast("该发布未提供 APK 安装包，已打开发布页")
            GitHubUpdateChecker.openRelease(context, info.releaseUrl)
            return
        }
        toast("开始下载 v${info.version}…")
        GitHubUpdateChecker.downloadApk(context, apk, wifiOnlyDownload) { uri ->
            if (uri != null) {
                if (installAfter && GitHubUpdateChecker.canInstallPackages(context)) {
                    GitHubUpdateChecker.installApk(context.applicationContext, uri)
                } else if (installAfter) {
                    toast("请先在系统设置中允许「安装未知应用」")
                    GitHubUpdateChecker.openRelease(context, info.releaseUrl)
                } else {
                    toast("下载完成，请点击系统通知或在下载管理中安装")
                }
            } else {
                toast("下载失败，请前往发布页手动下载")
                GitHubUpdateChecker.openRelease(context, info.releaseUrl)
            }
        }
    }

    fun checkUpdate() {
        if (isChecking) return
        isChecking = true
        if (!isNetworkAvailable(context)) {
            toast("无网络连接，无法检查更新")
            isChecking = false
            return
        }
        val current = GitHubUpdateChecker.currentVersion(context)
        scope.launch {
            when (val result = GitHubUpdateChecker.check(updateChannel, current)) {
                is GitHubUpdateChecker.CheckResult.UpToDate -> {
                    prefs.setLastUpdateCheck(System.currentTimeMillis())
                    toast("已是最新版本（v$current）")
                    isChecking = false
                }
                is GitHubUpdateChecker.CheckResult.Error -> {
                    toast(result.message)
                    isChecking = false
                }
                is GitHubUpdateChecker.CheckResult.Found -> {
                    val info = result.info
                    prefs.setLastUpdateCheck(System.currentTimeMillis())
                    prefs.addUpdateHistory(
                        UpdateRecord(
                            version = info.version,
                            time = if (info.publishedAt > 0) info.publishedAt else System.currentTimeMillis(),
                            channel = updateChannel,
                            status = "发现新版本",
                        ),
                    )
                    updateResult = info
                    if (autoDownload) startDownload(info, autoInstall)
                    isChecking = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        CommonTopAppBar("更新设置", onBack)

        Text(
            "管理软件更新的检查、下载、安装、渠道与提醒，并查看更新历史。",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // ---- 检查与下载 ----
        SmallTitle("检查与下载", insideMargin = PaddingValues(start = 16.dp, top = 16.dp))
        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            BasicComponent(
                title = "更新检查频率",
                summary = "当前：$checkFreqText",
                startAction = {
                    Icon(MiuixIcons.Refresh, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                },
                endActions = { Chevron() },
                onClick = { showCheckFreqDialog = true },
            )
            PreferenceSwitch(
                checked = autoDownload,
                onCheckedChange = { v ->
                    scope.launch { prefs.setAutoDownload(v) }
                    toast("自动下载更新已${if (v) "开启" else "关闭"}")
                },
                title = "自动下载更新",
                summary = "发现新版本时自动下载安装包",
                startAction = {
                    Icon(MiuixIcons.Download, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                },
            )
            PreferenceSwitch(
                checked = wifiOnlyDownload,
                enabled = autoDownload,
                onCheckedChange = { v ->
                    scope.launch { prefs.setWifiOnlyDownload(v) }
                    toast("仅 Wi-Fi 下载已${if (v) "开启" else "关闭"}")
                },
                title = "仅 Wi-Fi 下下载",
                summary = if (autoDownload) "避免消耗移动数据流量" else "需先开启「自动下载更新」",
                startAction = {
                    Icon(MiuixIcons.CloudFill, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                },
            )
            PreferenceSwitch(
                checked = autoInstall,
                onCheckedChange = { v ->
                    scope.launch { prefs.setAutoInstall(v) }
                    if (v) toast("已开启自动安装（需授予「安装未知应用」权限）") else toast("自动安装已关闭")
                },
                title = "下载后自动安装",
                summary = "下载完成后自动安装；需在系统设置授予安装权限",
                startAction = {
                    Icon(MiuixIcons.Update, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                },
            )
        }

        // ---- 渠道与提醒 ----
        SmallTitle("渠道与提醒", insideMargin = PaddingValues(start = 16.dp, top = 16.dp))
        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            BasicComponent(
                title = "更新渠道",
                summary = "当前：$channelText",
                startAction = {
                    Icon(MiuixIcons.Notes, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                },
                endActions = { Chevron() },
                onClick = { showChannelDialog = true },
            )
            BasicComponent(
                title = "更新提醒方式",
                summary = "当前：$reminderText",
                startAction = {
                    Icon(MiuixIcons.Report, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                },
                endActions = { Chevron() },
                onClick = { showReminderDialog = true },
            )
        }

        // ---- 历史与检查 ----
        SmallTitle("历史与检查", insideMargin = PaddingValues(start = 16.dp, top = 16.dp))
        Card(modifier = Modifier.padding(horizontal = 16.dp)) {
            BasicComponent(
                title = "更新历史",
                summary = if (updateHistory.isEmpty()) "暂无记录" else "${updateHistory.size} 条 · 最新 ${updateHistory.first().version}",
                startAction = {
                    Icon(MiuixIcons.CloudFill, null, tint = Color(0xFF555555), modifier = Modifier.size(38.dp).padding(end = 16.dp))
                },
                endActions = { Chevron() },
                onClick = { showHistoryDialog = true },
            )
            CheckUpdateRow(
                isChecking = isChecking,
                summary = lastCheckText,
                onClick = { checkUpdate() },
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    // ---- 更新检查频率 ----
    OptionDialog(
        show = showCheckFreqDialog,
        title = "更新检查频率",
        options = listOf(
            Triple(UPDATE_FREQ_MANUAL, "手动检查", "仅在点击「检查更新」时检测"),
            Triple(UPDATE_FREQ_DAILY, "每天", "每日自动检查一次"),
            Triple(UPDATE_FREQ_WEEKLY, "每周", "每周自动检查一次"),
            Triple(UPDATE_FREQ_MONTHLY, "每月", "每月自动检查一次"),
        ),
        selected = updateCheckFreq,
        onSelect = {
            scope.launch { prefs.setUpdateCheckFreq(it) }
            tick()
            showCheckFreqDialog = false
        },
        onDismiss = { showCheckFreqDialog = false },
    )

    // ---- 更新渠道 ----
    OptionDialog(
        show = showChannelDialog,
        title = "更新渠道",
        options = listOf(
            Triple(UPDATE_CHANNEL_STABLE, "稳定版", "经过充分测试，推荐日常使用"),
            Triple(UPDATE_CHANNEL_BETA, "测试版", "抢先体验新功能，可能不稳定"),
            Triple(UPDATE_CHANNEL_ALL, "全部渠道", "同时接收稳定版与测试版推送"),
        ),
        selected = updateChannel,
        onSelect = {
            scope.launch { prefs.setUpdateChannel(it) }
            tick()
            showChannelDialog = false
        },
        onDismiss = { showChannelDialog = false },
    )

    // ---- 更新提醒方式 ----
    OptionDialog(
        show = showReminderDialog,
        title = "更新提醒方式",
        options = listOf(
            Triple(UPDATE_REMINDER_NOTIFICATION, "通知栏提醒", "有新版本时发送系统通知"),
            Triple(UPDATE_REMINDER_DIALOG, "弹窗提醒", "打开应用时弹出更新对话框"),
            Triple(UPDATE_REMINDER_NONE, "不提醒", "仅手动检查时显示结果"),
        ),
        selected = updateReminder,
        onSelect = {
            scope.launch { prefs.setUpdateReminder(it) }
            tick()
            showReminderDialog = false
        },
        onDismiss = { showReminderDialog = false },
    )

    // ---- 更新历史 ----
    OverlayDialog(
        show = showHistoryDialog,
        title = "更新历史",
        onDismissRequest = { showHistoryDialog = false },
        onDismissFinished = { showHistoryDialog = false },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
        ) {
            if (updateHistory.isEmpty()) {
                Text(
                    "暂无更新记录。点击「检查更新」后，检测到的版本会显示在这里。",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                updateHistory.forEachIndexed { index, rec ->
                    val chText = when (rec.channel) {
                        UPDATE_CHANNEL_BETA -> "测试版"
                        UPDATE_CHANNEL_ALL -> "全部渠道"
                        else -> "稳定版"
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "v${rec.version}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                chText,
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Text(
                            formatTime(rec.time),
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            rec.status,
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    if (index != updateHistory.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        )
                    }
                }
            }
            if (updateHistory.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showClearHistoryConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("清空历史") }
            }
        }
    }

    // ---- 清空更新历史：二次确认 ----
    if (showClearHistoryConfirm) {
        DestructiveConfirmDialog(
            title = "确认清空更新历史？",
            message = "此操作将永久清除全部更新检查记录，且无法撤销。",
            confirmText = "确认清空",
            onDismiss = { showClearHistoryConfirm = false },
            onConfirm = {
                scope.launch { prefs.clearUpdateHistory() }
                tick()
                toast("已清空更新历史")
            },
        )
    }

    // ---- 检查结果 ----
    updateResult?.let { info ->
        OverlayDialog(
            show = true,
            title = "发现新版本 v${info.version}",
            onDismissRequest = { updateResult = null },
            onDismissFinished = { updateResult = null },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
            ) {
                if (info.prerelease) {
                    Text(
                        "测试版（Pre-release）",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(
                    if (info.notes.isBlank()) "暂无更新说明。" else info.notes,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    "发布时间：${if (info.publishedAt > 0) formatTime(info.publishedAt) else "未知"}",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        startDownload(info, autoInstall)
                        updateResult = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("下载安装") }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    text = "前往发布页",
                    onClick = {
                        GitHubUpdateChecker.openRelease(context, info.releaseUrl)
                        updateResult = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    text = "稍后",
                    onClick = { updateResult = null },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}