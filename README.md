# 快搜（Kuaishou）

> 搜索跳板 / 快捷搜索中枢 —— 输入关键词，点平台，绕过首页信息流直达搜索结果页。
> 原生 Android App（Kotlin + Jetpack Compose + Miuix 0.9.4 HyperOS 设计语言）。

## 构建方式

1. 用 **Android Studio** 打开本目录 `Y:\快搜`，等 Gradle Sync 完成
2. 连接手机（Android 7.0+ / API 24+）运行，或 Build → Generate APK

> 本工程已在本机验证编译通过（JDK 21 @ `E:\Android Studio\jbr`、SDK @ `D:\sdk`）。
> debug APK 直接位于 `app/build/outputs/apk/debug/app-debug.apk`；Release 构建开启 R8 压缩混淆。
> 已含 Gradle Wrapper（Gradle 9.6.0）；依赖仓库已配阿里云镜像。
> 注意：项目路径含中文，已通过 `android.overridePathCheck=true` 放行。

## 页面结构

- **主页**（根）：品牌区 + 自动聚焦搜索框（🔍 触发、× 清空保持键盘）+ 平台宫格（点击选中、长按设默认、默认角标）+ 最近搜索 chips（点击回填、长按删除）+ 搜索联想词 + 键盘悬浮胶囊 + 使用统计 + 剪贴板识别条（检测到剪贴板文本时一键填入）
- **平台管理**（子页）：已启用平台按分类分组展示（**长按同分类内拖动排序** + 开关 + 星标设默认）+ 未启用平台池 + 自定义平台（Scheme / 网页模板）+ 恢复默认
- **设置**（子页）：顶部搜索框实时过滤设置项，包含平台管理、主题外观（跟随系统/浅色/深色）、跳转方式、频次排序、使用统计、小组件设置、备份与恢复、触感反馈、清空最近搜索、更新设置、关于应用
- **关于应用**（二级）：应用介绍、问题反馈、项目仓库、开源许可证、赞助、致谢、用户协议与隐私政策、版本迭代记录
- **更新设置**（二级）：更新检查频率、自动下载、下载后自动安装、更新提醒方式（通知栏/弹窗/不提醒）、更新历史、手动检查更新

**快捷入口**：从其他 App「分享」纯文本给快搜会自动填入搜索框；桌面可添加 4×1 / 2×2 / 4×2 / 4×4 四种尺寸的小组件（平台宫格 / 搜索栏 / 最近搜索，点按直接预选平台并聚焦搜索）。

返回键：子页逐级回退（平台管理回其来源页；引导/关于/更新/小组件设置回设置），主页退出。

## 已实现功能

| 需求 | 实现 |
|---|---|
| 键盘回车联动 | 用当前选中平台跳转；未手动选过平台 → 走默认平台 + 顶部 Toast「已用XX搜索」 |
| 空输入防误触 | 搜索框为空时 🔍 置灰不可点 |
| 清空重输 | × 清空后键盘保持弹出 |
| 跳转失败兜底 | 半屏抽屉「无法直接打开XX」+ 复制关键词 / 打开网页版 / 打开首页 |
| 平台跳转兜底 | 主 App 未安装时自动唤起同系版本：如未装抖音、装有抖音极速版，点击抖音自动跳转极速版（装了两个仍优先主 App） |
| 默认平台 | 长按平台卡片 / 平台管理星标设默认，主页角标标记 |
| 历史 | 本地 DataStore 保存最近 10 条，去重，长按删单条，设置页可清空 |
| 深浅色 | 跟随系统 / 浅色 / 深色三档，Miuix squircle 圆角 |
| 跳转策略 | 默认优先唤起本地 App（带包名精确唤起 → 同系兜底包 → 系统解析 → 失败弹兜底抽屉）；设置页可改为直接开网页 |
| 剪贴板识别 | 进入主页检测剪贴板纯文本（仅一次），出现「剪贴板」条，一键填入；已填/已忽略不再打扰 |
| 搜索联想词 | 输入时提示全网热词，减少打字 |
| 键盘悬浮胶囊 | 随键盘弹起，输入后一键直达搜索 |
| 平台分类管理 | 分类折叠 + 长按同分类内拖动排序，自定义平台可配 Scheme 与网页模板 |
| 桌面小部件 | 4×1 / 2×2 / 4×2 / 4×4 四种尺寸，支持展示平台、排列方式、数据来源与点击行为自定义；修改平台配置后即时重绘 |
| 备份与恢复 | 导出配置 JSON 到文件，换机后一键恢复 |
| 系统分享 | 其他 App 分享纯文本给快搜 → 自动填入搜索框，点平台即可搜 |
| 触感反馈 | 跳转、复制、清空时轻微震动，可在设置页开关 |
| 更新机制 | 检查 GitHub Releases，支持通知栏 / 弹窗提醒、任意网络自动下载、下载后自动安装（未授权自动跳系统设置页） |

## 平台池

默认启用：抖音、小红书、B站、知乎、微博
待选池：快手、百度、微信搜一搜、必应、夸克、豆瓣（平台管理页开关加入主页）

| 平台 | 包名 | 兜底包 | Deep Link | 网页兜底 |
|---|---|---|---|---|
| 抖音 | com.ss.android.ugc.aweme | com.ss.android.ugc.aweme.lite（极速版） | snssdk1128://search?keyword={kw} | douyin.com/search/{kw} |
| 小红书 | com.xingin.xhs | - | xhsdiscover://search/result?keyword={kw} | xiaohongshu.com/search_result?keyword={kw} |
| B站 | tv.danmaku.bili | - | bilibili://search?keyword={kw} | search.bilibili.com/all?keyword={kw} |
| 知乎 | com.zhihu.android | - | zhihu://search?q={kw} | zhihu.com/search?q={kw} |
| 微博 | com.sina.weibo | - | sinaweibo://searchall?q={kw} | s.weibo.com/weibo?q={kw} |
| 快手 | com.kuaishou.nebula | - | kwai://search?keyword={kw} | kuaishou.com/search/video?searchKey={kw} |
| 百度 | com.baidu.searchbox | - | baiduboxapp://search?word={kw} | baidu.com/s?wd={kw} |
| 微信搜一搜 | com.tencent.mm | - | 无（仅打开 App，无法带词直达，统一网页兜底） | weixin.sogou.com/weixin?type=2&query={kw} |
| 必应 | com.microsoft.bing | - | 无 | cn.bing.com/search?q={kw} |
| 夸克 | com.quark.browser | - | 无 | quark.sm.cn/s?q={kw} |
| 豆瓣 | com.douban.frodo | - | douban://search?q={kw} | douban.com/search?q={kw} |

每个平台为一条配置（`data/Platform.kt`），加平台 = 加一行（含包名/兜底包/scheme/网页 URL）+ 品牌色 + 图标，主页宫格自动更新；Manifest 的 `<queries>` 已声明全部包名（Android 11+ 包可见性）。

## 已知边界

- 抖音 / 小红书 / B站 等 Deep Link 可能因平台规则失效 → 已由兜底抽屉承接
- 抖音极速版兜底能否直达搜索页，取决于极速版是否注册 `snssdk1128` 搜索协议（同族协议通常支持）
- 语音输入仍未实现（需录音权限 + 语音识别，待后续）
- 微信搜一搜无法带关键词直达搜索（微信未开放该协议），统一走网页兜底
- 首次使用需同意《用户协议与隐私政策》，可在关于页撤回（撤回后退出应用，下次需重新同意）

## 关键文件

```
settings.gradle.kts / build.gradle.kts / gradle.properties   # 工程配置（AGP 9.3.0 + Compose 1.12 + Miuix 0.9.4）
app/build.gradle.kts                                        # 版本号 / 依赖 / R8 构建配置
app/src/main/AndroidManifest.xml                            # 含 <queries> 包可见性声明、通知权限
app/src/main/java/com/kuaishou/app/
├── MainActivity.kt            # 主题控制 + MiuixTheme + 页面路由（AnimatedContent）+ 返回处理
├── data/
│   ├── Platform.kt            # 平台注册表（名称/品牌色/搜索URL/scheme/兜底包）
│   ├── AppPrefs.kt            # DataStore：全部偏好与搜索历史
│   ├── AppVersion.kt          # 版本号读取与更新日志同步校验
│   └── ChangelogData.kt       # 版本迭代记录数据源
├── engine/JumpEngine.kt       # 跳转引擎（精确唤起 → 兜底包 → 系统解析 → 网页兜底）
├── widget/                    # 桌面小部件（4 种尺寸 + 数据/交互自定义）
└── ui/
    ├── HomeScreen.kt          # 主页 + 联想词 + 键盘悬浮胶囊 + 兜底抽屉
    ├── PlatformManageScreen.kt# 平台管理（分类折叠 + 长按拖拽排序 + 自定义平台）
    ├── SettingsScreen.kt      # 设置（搜索过滤 + 全部偏好入口）
    ├── UpdateScreen.kt        # 更新设置 + GitHubUpdateChecker（检查/下载/安装/通知）
    ├── AboutScreen.kt         # 关于（介绍/许可/赞助/致谢/协议）
    └── ChangelogScreen.kt     # 版本迭代记录
```
