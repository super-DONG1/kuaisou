# 快搜（Kuaishou）

> 搜索跳板 / 快捷搜索中枢 —— 输入关键词，点平台，绕过首页信息流直达搜索结果页。
> 原生 Android App（Kotlin + Jetpack Compose + Miuix 0.9.4 HyperOS 设计语言）。

## 构建方式

1. 用 **Android Studio** 打开本目录，等 Gradle Sync 完成
2. 连接手机（Android 7.0+ / API 24+）运行，或 Build → Generate APK

> 已含 Gradle Wrapper（Gradle 9.6.0）；依赖仓库已配阿里云镜像。
> debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 页面结构

- **主页**（根）：品牌区「快搜·搜完即走，不刷首页」+ 自动聚焦搜索框（右侧 🔍 触发，空词置灰，× 清空保持键盘）+ 平台宫格（点击选中、长按设默认、默认角标）+ 最近搜索 chips（点击回填、长按删除）+ 剪贴板识别条
- **平台管理**（子页）：已启用平台（长按拖动排序 + 开关 + 设默认）+ 待选平台池 + 自定义平台（新增/删除）+ 恢复默认
- **设置**（子页）：主题外观 / 跳转方式 / 触感反馈 / 备份恢复 / 小组件设置 / 新用户引导 / 关于应用（协议、隐私、开源许可、更新历史等）

**快捷入口**：从其他 App「分享」纯文本给快搜会自动填入搜索框；桌面可添加「快搜」小部件（4×1 / 2×2 / 4×2 / 4×4）。

## 已实现交互

| 需求 | 实现 |
|---|---|
| 键盘回车联动 | 用当前选中平台跳转；未手动选过平台 → 走默认平台 + Toast「已用XX搜索」 |
| 空输入防误触 | 搜索框为空时 🔍 置灰不可点 |
| 清空重输 | × 清空后键盘保持弹出 |
| 跳转失败兜底 | 半屏抽屉「无法直接打开XX」+ 复制关键词 / 打开网页版 / 打开首页 |
| 默认平台 | 长按平台卡片设默认（首次默认抖音），主页角标标记 |
| 历史 | 本地 DataStore 保存最近 10 条，去重，长按删单条，设置页可清空 |
| 深浅色 | `ThemeController` 跟随系统 / 浅色 / 深色，Miuix squircle 圆角 |
| 跳转策略 | 默认优先唤起本地 App（带包名精确唤起 → 系统解析 → 失败弹兜底抽屉）；设置页可关闭改为直接开网页 |
| 系统分享 | 其他 App 分享纯文本给快搜 → 自动填入搜索框 |
| 剪贴板识别 | 进入主页检测剪贴板纯文本（仅一次），出现「剪贴板」条，一键填入 |
| 桌面小部件 | 平台宫格 / 搜索栏，点按经 `EXTRA_PLATFORM_ID` 打开快搜并预选平台、聚焦搜索框 |
| 更新检查 | 启动后按频率（周/日/月/手动）与渠道（稳定/测试）静默检查 GitHub Releases |
| 备份恢复 | 导出 JSON 配置到文件，换机后一键恢复 |

## 平台池

默认启用：抖音、小红书、B站、知乎、微博、淘宝
待选池：快手、京东、百度、微信搜一搜（管理页开关加入主页）

| 平台 | 包名 | Deep Link | 网页兜底 |
|---|---|---|---|
| 抖音 | com.ss.android.ugc.aweme | snssdk1128://search?keyword={kw} | douyin.com/search/{kw} |
| 小红书 | com.xingin.xhs | xhsdiscover://search/result?keyword={kw} | xiaohongshu.com/search_result?keyword={kw} |
| B站 | tv.danmaku.bili | bilibili://search?keyword={kw} | search.bilibili.com/all?keyword={kw} |
| 知乎 | com.zhihu.android | zhihu://search?q={kw} | zhihu.com/search?q={kw} |
| 微博 | com.sina.weibo | sinaweibo://searchall?q={kw} | s.weibo.com/weibo?q={kw} |
| 淘宝 | com.taobao.taobao | taobao://s.taobao.com/search?q={kw} | s.taobao.com/search?q={kw} |

每个平台为一条配置（`data/Platform.kt`），加平台 = 加一行 + 品牌色，主页宫格自动更新；Manifest 的 `<queries>` 已声明全部包名。

## 版本历史

- **2.0.2-1010**：精简安装包体积（R8 压缩 + 移除 OkHttp），修复「用X搜索」按钮机型兼容问题
- **2.0.1-1007**：小组件展示平台自定义、多选弹窗、数量上限拦截、刷新修复
- **2.0.0-1007**：搜索联想词、键盘悬浮胶囊、平台分类管理、多种桌面小组件
- **1.0.0-0901**：首个版本发布

## 关键文件

```
settings.gradle.kts / build.gradle.kts / gradle.properties   # 工程配置（AGP 9.3.0 + Compose 2.4.20）
app/build.gradle.kts                                        # 依赖：miuix-ui/-preference/-icons 0.9.4
app/src/main/AndroidManifest.xml                            # <queries> 包可见性 + 小组件 receivers
app/src/main/java/com/kuaishou/app/
├── MainActivity.kt            # ThemeController + MiuixTheme + 页面路由 + 返回处理
├── data/                      # Platform / AppPrefs / SearchRepository / LegalText / Changelog
├── engine/JumpEngine.kt       # 跳转引擎（Deep Link + 网页兜底）
├── widget/                    # 桌面小部件（4×1 / 2×2 / 4×2 / 4×4）
└── ui/                        # Home / PlatformManage / Settings / About / Tutorial / Update 等
```
