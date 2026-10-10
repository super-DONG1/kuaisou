package com.kuaishou.app.data

/**
 * 版本迭代记录数据源（硬编码，最新版本在列表最前）。
 * 每条变更归类为「新增 / 优化 / 修复」三类，供 ChangelogScreen 渲染。
 */

/** 更新内容分类。 */
enum class ChangeType(val label: String) {
    New("新增"),
    Optimize("优化"),
    Fix("修复"),
}

/** 单条更新内容。 */
data class ChangeItem(
    val type: ChangeType,
    val content: String,
)

/** 单个版本迭代记录。 */
data class VersionLog(
    val version: String,
    val date: String,
    val changes: List<ChangeItem>,
)

/** 版本迭代记录列表（newest first）。 */
val versionLogs: List<VersionLog> = listOf(
    VersionLog(
        version = "2.0.2-1010",
        date = "2026-10-10",
        changes = listOf(
            ChangeItem(ChangeType.Optimize, "版本号 2.0.1-1007 → 2.0.2-1010（2026-10-10）：本次更新大幅精简安装包体积，并修复搜索按钮的机型兼容问题。"),
            // ---- 优化 ----
            ChangeItem(ChangeType.Optimize, "安装包体积大幅精简。"),
            ChangeItem(ChangeType.Optimize, "开启 R8 代码压缩与资源压缩，剔除无用代码与资源。"),
            ChangeItem(ChangeType.Optimize, "移除 OkHttp 依赖，网络请求改用系统 HttpURLConnection，进一步减小代码体积。"),
            ChangeItem(ChangeType.Optimize, "平台图标按需压缩。"),
            ChangeItem(ChangeType.Optimize, "打赏二维码图片优化，体积更小且不影响扫描识别。"),
            // ---- 修复 ----
            ChangeItem(ChangeType.Fix, "修复「用X搜索」悬浮按钮在 OPPO / 华为 / 荣耀 / vivo 等机型不弹出的问题：开启边到边使键盘高度能被稳定获取，并增加「可视区收缩」作为第二判定信号。"),
        ),
    ),
    VersionLog(
        version = "2.0.1-1007",
        date = "2026-10-07",
        changes = listOf(
            ChangeItem(ChangeType.Optimize, "本次更新聚焦桌面小组件的自定义能力与显示稳定性。"),
            // ---- 新增 ----
            ChangeItem(ChangeType.New, "小组件「展示平台」自定义：4×1 搜索栏、2×2、4×2 三个尺寸可各自指定要展示的平台，留空则按「排列/来源」自动选取。"),
            ChangeItem(ChangeType.New, "小组件平台多选弹窗：标题下实时显示「已选 X / Y」，每个平台带复选框与选中高亮，底部「取消 / 确定」操作主次分明。"),
            ChangeItem(ChangeType.New, "小组件平台数量上限拦截：超出该尺寸可展示上限时禁止勾选并提示（4×1 最多 3 个、2×2 最多 6 个、4×2 最多 8 个），取消勾选后实时解除。"),
            // ---- 优化 ----
            ChangeItem(ChangeType.Optimize, "小组件弹窗高度自适应：列表区上限约 60% 屏高并支持滚动，弹窗更紧凑，不再几乎占满全屏。"),
            ChangeItem(ChangeType.Optimize, "小组件「确定」按钮改为主题蓝加粗、「取消」弱化为中灰，保存主操作一眼可辨。"),
            ChangeItem(ChangeType.Optimize, "精简代码：移除小组件「展示样式」模块，清理无引用的偏好项与冗余注释。"),
            // ---- 修复 ----
            ChangeItem(ChangeType.Fix, "修复已添加的小组件不刷新：修改平台选择、默认平台、平台管理增删或排序、以及发起搜索后均即时重绘，无需删除组件重新添加。"),
            ChangeItem(ChangeType.Fix, "修复小组件更新时旧图标未清除、新图标叠加导致的显示错位：更新前先清空容器再添加。"),
            ChangeItem(ChangeType.Fix, "修复点击小组件空白处仍预选旧「默认平台」的问题。"),
            ChangeItem(ChangeType.Fix, "修复关闭「自动弹键盘」后，由小组件冷启动进入仍强制弹出键盘的问题。"),
            ChangeItem(ChangeType.Fix, "修复在设置/平台管理等子页点击小组件或分享时，启动参数残留导致返回主页后延迟弹键盘、关键词被重复填入的问题。"),
            ChangeItem(ChangeType.Fix, "修复搜索跳转失败时仍计入「最近搜索」与使用统计，导致数据失真的问题。"),
            ChangeItem(ChangeType.Fix, "修复「优先唤起 App」默认值在多处不一致导致的启动闪动。"),
        ),
    ),
    VersionLog(
        version = "2.0.0-1007",
        date = "2026-10-07",
        changes = listOf(
            // ---- 新增 ----
            ChangeItem(ChangeType.New, "搜索联想词功能：输入关键词时自动提示全网热词，减少打字。"),
            ChangeItem(ChangeType.New, "键盘悬浮胶囊按钮：随键盘弹起，输入后一键直达搜索。"),
            ChangeItem(ChangeType.New, "平台分类管理：支持视频、社交、电商等分类折叠，长按同分类内拖动排序。"),
            ChangeItem(ChangeType.New, "多种桌面小组件：4x1、2x2、4x2、4x4 四种尺寸，点击图标直接进 App 并弹键盘。"),
            ChangeItem(ChangeType.New, "版本迭代记录页面：随时查看历史更新。"),
            // ---- 优化 ----
            ChangeItem(ChangeType.Optimize, "首页搜索框：去除多余图标，还原极简输入体验。"),
            ChangeItem(ChangeType.Optimize, "平台网格排版：取消底座，统一图标尺寸，精选选中浅蓝底纹，视觉更克制。"),
            ChangeItem(ChangeType.Optimize, "最近搜索：拉开间距，让标签更饱满，不与上方网格拥挤。"),
            ChangeItem(ChangeType.Optimize, "使用概览：统计算法改为累计次数，解决数据逻辑矛盾。"),
            ChangeItem(ChangeType.Optimize, "设置页与关于页：统一卡片分组，加大加粗功能图标，整体 UI 达到 MIUIX 风格。"),
            ChangeItem(ChangeType.Optimize, "新用户引导：修正交互文案，配合卡片式排版，阅读更顺畅。"),
            ChangeItem(ChangeType.Optimize, "桌面小组件：删除 1x1 尺寸，调整比例，解决空旷感。"),
            // ---- 修复 ----
            ChangeItem(ChangeType.Fix, "修复从设置页返回首页时，键盘意外弹出的问题（现在仅冷启动或点击小组件时弹键盘）。"),
            ChangeItem(ChangeType.Fix, "修复拖拽排序时卡片图层穿透（被下方卡片遮挡）的问题，增加平滑排挤动画。"),
            ChangeItem(ChangeType.Fix, "修复“默认平台”可以被意外关闭的漏洞（默认平台必须保持启用状态）。"),
            ChangeItem(ChangeType.Fix, "修复平台管理页分类标题字号过小、对齐不齐的视觉问题。"),
        ),
    ),
    VersionLog(
        version = "1.0.0-0901",
        date = "2026-09-01",
        changes = listOf(
            ChangeItem(ChangeType.New, "快搜首个版本发布：绕过首页，直达搜索。"),
            ChangeItem(ChangeType.New, "平台管理与最近搜索功能。"),
        ),
    ),
)
