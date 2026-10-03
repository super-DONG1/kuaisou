# 快搜（Kuaishou）

> 搜索跳板 / 快捷搜索中枢 —— 输入关键词，点平台，绕过首页信息流直达搜索结果页。
> 原生 Android App（Kotlin + Jetpack Compose + Miuix 0.9.4 HyperOS 设计语言）。

## 构建方式

1. 用 **Android Studio** 打开本目录，等 Gradle Sync 完成
2. 连接手机（Android 7.0+ / API 24+）运行，或 Build → Generate APK

> 本工程已在本机验证编译通过（JDK 21、SDK 35+）。
> debug APK 直接位于 `app/build/outputs/apk/debug/app-debug.apk`。
> 已含 Gradle Wrapper（Gradle 9.6.0）；依赖仓库已配阿里云镜像。
> 注意：项目路径含中文时已通过 `android.overridePathCheck=true` 放行。

## 页面结构

- **主页**（根）：品牌区「快搜·搜完即走，不刷首页」+ 自动聚焦搜索框（右侧 🔍 触发，空词置灰，× 清空保持键盘）+ 平台宫格（点击选中、长按设默认、默认角标）+ 最近搜索 chips（点击回填、长按删除）
- **平台管理**（子页）：已启用平台（开关 + ▲▼ 上下移动排序）+ 待选平台池 + 恢复默认
- **设置**（子页）：新用户引导 + 清空最近搜索 + 优先唤起App开关 + 关于

返回键：子页回主页，主页退出。

## 已实现交互（对照确认方案）

| 需求 | 实现 |
|---|---|
| 键盘回车联动 | 用当前选中平台跳转；未手动选过平台 → 走默认平台 + 顶部 Toast「已用XX搜索」 |
| 空输入防误触 | 搜索框为空时 🔍 置灰不可点 |
| 清空重输 | × 清空后键盘保持弹出 |
| 跳转失败兜底 | 半屏抽屉「无法直接打开XX」+ 复制关键词 / 打开网页版 / 打开首页 |
| 默认平台 | 长按平台卡片设默认（首次默认抖音），主页角标标记 |
| 历史 | 本地 DataStore 保存最近 10 条，去重，长按删单条，设置页可清空 |
| 深浅色 | `ThemeController(System)` 跟随系统，Miuix squircle 圆角 |
| 跳转策略 | **默认优先唤起本地 App**（带包名精确唤起 → 系统解析 → 失败弹兜底抽屉）；设置页可关闭该偏好改为直接开网页 |

## 平台池

默认启用：抖音、小红书、B站、知乎、微博、淘宝
待选池：快手、京东、百度、微信搜一搜（管理页开关加入主页）

每个平台为一条配置（`data/Platform.kt`），加平台 = 加一行（含包名/scheme/网页 URL）+ 品牌色，主页宫格自动更新；Manifest 的 `<queries>` 已声明全部包名（Android 11+ 包可见性）。

## 仓库说明

- **图标资源**：`app/src/main/res/drawable/ic_*.png`（各平台官方 App 图标，10 个）为二进制文件，本仓库未包含；clone 后请从原工程目录拷贝，或从各平台官方渠道获取同名 PNG 放入该目录。
- **gradle-wrapper.jar**：二进制文件未包含；用 Android Studio 打开仓库后会自动补齐，或在本地工程执行 `gradlew wrapper` 重新生成。
- **本地构建**：需要 Android SDK（`local.properties` 指向本机 SDK 路径）与 JDK 17+。

## 已知边界（第一版）

- 抖音/小红书/B站 的 Deep Link 可能因平台规则失效 → 已由兜底抽屉承接
- 平台排序第一版用 ▲▼ 按钮，未做拖拽（后续可升级）
- 语音输入为架构预留（需录音权限），未实现
- 已通过本机编译验证；实机交互（Deep Link 唤起、键盘弹出、Toast 位置）仍需真机测试

## 关键文件

```
settings.gradle.kts / build.gradle.kts / gradle.properties   # 工程配置（AGP 9.3.0 + Compose 插件 2.4.20，AGP 9 内置 Kotlin）
app/build.gradle.kts                                        # 依赖：miuix-ui/-preference/-icons 0.9.4
app/src/main/AndroidManifest.xml                            # 含 <queries> 包可见性声明
app/src/main/java/com/kuaishou/app/
├── MainActivity.kt            # ThemeController(System) + MiuixTheme + 页面路由 + 返回处理
├── data/Platform.kt           # 平台注册表（名称/品牌色/搜索URL/scheme/包名/图标资源）
├── data/AppPrefs.kt           # DataStore：默认平台/启用顺序/历史/唤起偏好
├── engine/JumpEngine.kt       # 跳转引擎（Deep Link 尝试 + 网页兜底 + 失败结果）
└── ui/
    ├── HomeScreen.kt          # 主页 + 跳转失败兜底抽屉
    ├── PlatformManageScreen.kt# 平台管理（开关 + ▲▼ 排序 + 待选池）
    ├── SettingsScreen.kt      # 设置（引导/清空历史/唤起偏好/关于）
    └── TutorialScreen.kt      # 新用户引导页
```
