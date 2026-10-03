package com.kuaishou.app.data

import androidx.compose.ui.graphics.Color
import com.kuaishou.app.R

/**
 * 单个平台的搜索配置。
 * searchUrlTemplate 中用 {keyword} 占位关键词，跳转时替换为 URL 编码后的关键词。
 * scheme 为 App 唤起协议；packageName 为 Android 包名（优先唤起时用于精确命中该 App）。
 * iconRes 为平台实际 App 图标（drawable 资源）。
 */
data class Platform(
    val id: String,
    val name: String,
    val brandColor: Color,
    val searchUrlTemplate: String,
    val homeUrl: String,
    val scheme: String? = null,
    val packageName: String? = null,
    val iconRes: Int,
    val defaultEnabled: Boolean = true,
)

object PlatformRegistry {

    /** 全部平台（含默认启用与待选池），顺序即默认排序。 */
    val all: List<Platform> = listOf(
        Platform(
            id = "douyin", name = "抖音",
            brandColor = Color(0xFF161823),
            searchUrlTemplate = "https://www.douyin.com/search/{keyword}",
            homeUrl = "https://www.douyin.com/",
            scheme = "snssdk1128://search?keyword={keyword}",
            packageName = "com.ss.android.ugc.aweme",
            iconRes = R.drawable.ic_douyin,
        ),
        Platform(
            id = "xhs", name = "小红书",
            brandColor = Color(0xFFFF2442),
            searchUrlTemplate = "https://www.xiaohongshu.com/search_result?keyword={keyword}",
            homeUrl = "https://www.xiaohongshu.com/explore",
            scheme = "xhsdiscover://search/result?keyword={keyword}",
            packageName = "com.xingin.xhs",
            iconRes = R.drawable.ic_xhs,
        ),
        Platform(
            id = "bili", name = "B站",
            brandColor = Color(0xFF00AEEC),
            searchUrlTemplate = "https://search.bilibili.com/all?keyword={keyword}",
            homeUrl = "https://www.bilibili.com/",
            scheme = "bilibili://search?keyword={keyword}",
            packageName = "tv.danmaku.bili",
            iconRes = R.drawable.ic_bili,
        ),
        Platform(
            id = "zhihu", name = "知乎",
            brandColor = Color(0xFF0084FF),
            searchUrlTemplate = "https://www.zhihu.com/search?q={keyword}",
            homeUrl = "https://www.zhihu.com/",
            scheme = "zhihu://search?q={keyword}",
            packageName = "com.zhihu.android",
            iconRes = R.drawable.ic_zhihu,
        ),
        Platform(
            id = "weibo", name = "微博",
            brandColor = Color(0xFFE6162D),
            searchUrlTemplate = "https://s.weibo.com/weibo?q={keyword}",
            homeUrl = "https://weibo.com/",
            scheme = "sinaweibo://searchall?q={keyword}",
            packageName = "com.sina.weibo",
            iconRes = R.drawable.ic_weibo,
        ),
        Platform(
            id = "taobao", name = "淘宝",
            brandColor = Color(0xFFFF5000),
            searchUrlTemplate = "https://s.taobao.com/search?q={keyword}",
            homeUrl = "https://www.taobao.com/",
            scheme = "taobao://s.taobao.com/search?q={keyword}",
            packageName = "com.taobao.taobao",
            iconRes = R.drawable.ic_taobao,
        ),
        Platform(
            id = "kuaishou", name = "快手",
            brandColor = Color(0xFFFF4906),
            searchUrlTemplate = "https://www.kuaishou.com/search/video?searchKey={keyword}",
            homeUrl = "https://www.kuaishou.com/",
            iconRes = R.drawable.ic_kuaishou,
            defaultEnabled = false,
        ),
        Platform(
            id = "jd", name = "京东",
            brandColor = Color(0xFFE1251B),
            searchUrlTemplate = "https://search.jd.com/Search?keyword={keyword}",
            homeUrl = "https://www.jd.com/",
            iconRes = R.drawable.ic_jd,
            defaultEnabled = false,
        ),
        Platform(
            id = "baidu", name = "百度",
            brandColor = Color(0xFF2932E1),
            searchUrlTemplate = "https://www.baidu.com/s?wd={keyword}",
            homeUrl = "https://www.baidu.com/",
            iconRes = R.drawable.ic_baidu,
            defaultEnabled = false,
        ),
        Platform(
            id = "wechat", name = "微信搜一搜",
            brandColor = Color(0xFF07C160),
            searchUrlTemplate = "https://weixin.sogou.com/weixin?type=2&query={keyword}",
            homeUrl = "https://weixin.sogou.com/",
            iconRes = R.drawable.ic_wechat,
            defaultEnabled = false,
        ),
    )

    fun byId(id: String): Platform? = all.firstOrNull { it.id == id }

    /** 默认启用平台（首次安装时主页展示这些）。 */
    val defaultEnabled: List<Platform> = all.filter { it.defaultEnabled }
}
