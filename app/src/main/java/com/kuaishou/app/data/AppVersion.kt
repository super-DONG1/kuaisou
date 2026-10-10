package com.kuaishou.app.data

import com.kuaishou.app.BuildConfig

/**
 * 应用版本号的**唯一来源**：取自构建期生成的 [BuildConfig]，与 app/build.gradle.kts 中的
 * `versionName` 同源。升级版本时只需改构建脚本，所有展示位置（关于页、设置页、更新检查等）
 * 会自动同步，无需逐处手改，避免出现「包已是新版本、界面仍显示旧版本号」的滞后问题。
 *
 * 命名规则：MAJOR.MINOR.PATCH-MMDD（如 2.0.1-1007 即 2.0.1 版、10-07 构建）。
 */
val appVersionName: String = BuildConfig.VERSION_NAME

/** 应用版本号对应的 versionCode（单调递增，Android 以此判断能否覆盖安装）。 */
val appVersionCode: Int = BuildConfig.VERSION_CODE

/**
 * 版本日志是否与当前构建版本一致。
 * 更新版本号时若忘记在 [versionLogs] 补一条记录，这里即为 false，
 * 可据此在版本迭代记录页给出提示，避免「包已升级、日志仍停在旧版本」的滞后。
 */
val isChangelogInSync: Boolean
    get() = versionLogs.firstOrNull()?.version == appVersionName
