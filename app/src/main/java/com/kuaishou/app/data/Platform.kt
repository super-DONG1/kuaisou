package com.kuaishou.app.data

import androidx.compose.ui.graphics.Color
import com.kuaishou.app.R

/**
 * 平台分类：平台管理页按此分组展示，拖拽排序严格限制在同一分类内部。
 * 顺序即管理页与默认排序中的分组顺序。
 */
enum class PlatformCategory(val title: String) {
    VIDEO("视频"),
    SOCIAL("图文社交"),
    SEARCH("搜索问答"),
    OTHER("其他平台"),
}

/**
 * 单个平台的搜索配置。
 * searchUrlTemplate 中用 {keyword} 占位关键词，跳转时替换为 URL 编码后的关键词。
 * scheme 为 App 唤起协议（同样用 {keyword} 占位）；packageName 为 Android 包名（优先唤起时用于精确命中该 App）。
 * iconRes 为平台实际 App 图标（drawable 资源）；为 0 时 UI 回退为「首字母块」占位。
 */
data class Platform(
    val id: String,
    val name: String,
    val brandColor: Color,
    val searchUrlTemplate: String,
    val homeUrl: String,
    val scheme: String? = null,
    val packageName: String? = null,
    /** 主包未安装时的备选包名（如抖音主 App 缺失时唤起抖音极速版），跳转时按序尝试。 */
    val fallbackPackages: List<String> = emptyList(),
    val iconRes: Int,
    val defaultEnabled: Boolean = true,
    /** 是否为用户自定义平台（无内置图标，UI 显示首字母块）。 */
    val custom: Boolean = false,
    /** 所属分类；自定义平台固定归入「其他平台」。 */
    val category: PlatformCategory = PlatformCategory.OTHER,
)

/** 用户自定义平台（设置-平台管理新增）。schemeTemplate 为 App 唤起协议模板（含 {keyword}），webTemplate 为网页搜索模板。 */
data class CustomPlatformData(
    val id: String,
    val name: String,
    val schemeTemplate: String?,
    val webTemplate: String,
) {
    fun toPlatform(): Platform = Platform(
        id = id,
        name = name,
        brandColor = Color(0xFF8A8A8E),
        searchUrlTemplate = webTemplate,
        homeUrl = webTemplate.substringBefore("?") ,
        scheme = schemeTemplate,
        packageName = null,
        iconRes = 0,
        defaultEnabled = true,
        custom = true,
        category = PlatformCategory.OTHER,
    )
}

object PlatformRegistry {

    /**
     * 全部平台（含默认启用与待选池）。
     * 顺序即首页默认宫格与各类别的展示顺序；不默认启用的平台追加在末尾。
     * 图标缺失的（iconRes = 0）由 UI 回退为首字母块。
     */
    val all: List<Platform> = listOf(
        Platform(
            id = "douyin", name = "抖音",
            brandColor = Color(0xFF161823),
            searchUrlTemplate = "https://www.douyin.com/search/{keyword}",
            homeUrl = "https://www.douyin.com/",
            scheme = "snssdk1128://search?keyword={keyword}",
            packageName = "com.ss.android.ugc.aweme",
            // 未装抖音主 App 时，唤起抖音极速版（scheme 协议同族，极速版注册 snssdk1128 即可命中）
            fallbackPackages = listOf("com.ss.android.ugc.aweme.lite"),
            iconRes = R.drawable.ic_douyin,
            category = PlatformCategory.VIDEO,
        ),
        Platform(
            id = "xhs", name = "小红书",
            brandColor = Color(0xFFFF2442),
            searchUrlTemplate = "https://www.xiaohongshu.com/search_result?keyword={keyword}",
            homeUrl = "https://www.xiaohongshu.com/explore",
            scheme = "xhsdiscover://search/result?keyword={keyword}",
            packageName = "com.xingin.xhs",
            iconRes = R.drawable.ic_xiaohongshu,
            category = PlatformCategory.SOCIAL,
        ),
        Platform(
            id = "bili", name = "B站",
            brandColor = Color(0xFF00AEEC),
            searchUrlTemplate = "https://search.bilibili.com/all?keyword={keyword}",
            homeUrl = "https://www.bilibili.com/",
            scheme = "bilibili://search?keyword={keyword}",
            packageName = "tv.danmaku.bili",
            iconRes = R.drawable.ic_bilibili,
            category = PlatformCategory.VIDEO,
        ),
        Platform(
            id = "zhihu", name = "知乎",
            brandColor = Color(0xFF0084FF),
            searchUrlTemplate = "https://www.zhihu.com/search?q={keyword}",
            homeUrl = "https://www.zhihu.com/",
            scheme = "zhihu://search?q={keyword}",
            packageName = "com.zhihu.android",
            iconRes = R.drawable.ic_zhihu,
            category = PlatformCategory.SEARCH,
        ),
        Platform(
            id = "weibo", name = "微博",
            brandColor = Color(0xFFE6162D),
            searchUrlTemplate = "https://s.weibo.com/weibo?q={keyword}",
            homeUrl = "https://weibo.com/",
            scheme = "sinaweibo://searchall?q={keyword}",
            packageName = "com.sina.weibo",
            iconRes = R.drawable.ic_weibo,
            category = PlatformCategory.SOCIAL,
        ),
        Platform(
            id = "kuaishou", name = "快手",
            brandColor = Color(0xFFFF4906),
            searchUrlTemplate = "https://www.kuaishou.com/search/video?searchKey={keyword}",
            homeUrl = "https://www.kuaishou.com/",
            scheme = "kwai://search?keyword={keyword}",
            packageName = "com.kuaishou.nebula",
            iconRes = R.drawable.ic_kuaishou,
            defaultEnabled = false,
            category = PlatformCategory.VIDEO,
        ),
        Platform(
            id = "baidu", name = "百度",
            brandColor = Color(0xFF2932E1),
            searchUrlTemplate = "https://www.baidu.com/s?wd={keyword}",
            homeUrl = "https://www.baidu.com/",
            scheme = "baiduboxapp://search?word={keyword}",
            packageName = "com.baidu.searchbox",
            iconRes = R.drawable.ic_baidu,
            defaultEnabled = false,
            category = PlatformCategory.SEARCH,
        ),
        // 微信搜一搜：仅有「打开 App」的 weixin://，无法带关键词直达搜索，
        // 故不写 scheme，统一走网页兜底，避免出现「唤起微信但没搜索」的情况
        Platform(
            id = "wechat", name = "微信搜一搜",
            brandColor = Color(0xFF07C160),
            searchUrlTemplate = "https://weixin.sogou.com/weixin?type=2&query={keyword}",
            homeUrl = "https://weixin.sogou.com/",
            packageName = "com.tencent.mm",
            iconRes = R.drawable.ic_weixin,
            defaultEnabled = false,
            category = PlatformCategory.SEARCH,
        ),

        // ---------- 以下为新增平台（均不默认启用）----------
        // 必应：无公开搜索 Scheme，直接走网页兜底
        Platform(
            id = "bing", name = "必应",
            brandColor = Color(0xFF00809D),
            searchUrlTemplate = "https://cn.bing.com/search?q={keyword}",
            homeUrl = "https://cn.bing.com/",
            packageName = "com.microsoft.bing",
            iconRes = R.drawable.ic_bing,
            defaultEnabled = false,
            category = PlatformCategory.SEARCH,
        ),
        // 夸克：quark:// 的搜索参数未经验证，暂走网页兜底
        Platform(
            id = "quark", name = "夸克",
            brandColor = Color(0xFF4A6FE3),
            searchUrlTemplate = "https://quark.sm.cn/s?q={keyword}",
            homeUrl = "https://quark.sm.cn/",
            packageName = "com.quark.browser",
            iconRes = R.drawable.ic_quark,
            defaultEnabled = false,
            category = PlatformCategory.SEARCH,
        ),
        Platform(
            id = "douban", name = "豆瓣",
            brandColor = Color(0xFF00B51D),
            searchUrlTemplate = "https://www.douban.com/search?q={keyword}",
            homeUrl = "https://www.douban.com/",
            scheme = "douban://search?q={keyword}",
            packageName = "com.douban.frodo",
            iconRes = R.drawable.ic_douban,
            defaultEnabled = false,
            category = PlatformCategory.SOCIAL,
        ),
    )

    /** 默认启用平台（首次安装时主页展示这些）。 */
    val defaultEnabled: List<Platform> = all.filter { it.defaultEnabled }
}
