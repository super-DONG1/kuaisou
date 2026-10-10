# 发布构建混淆/压缩规则（个人发布，优先保证稳定性）
#
# AGP 9 起只能配合 proguard-android-optimize.txt（自带 R8 代码优化：内联 / 合并 / 类重组）。
# 这里不再加 -dontoptimize，以取得完整压缩收益；若真机出现异常，加回该行即可回退为「仅压缩 + 混淆」。

# ---- 自有代码：保守保留，避免任何潜在反射/序列化路径被裁剪 ----
-keep class com.kuaishou.app.data.** { *; }
-keep class com.kuaishou.app.MainActivity { *; }
-keep class com.kuaishou.app.widget.Widget*Provider { *; }

# ---- 崩溃栈可读：保留源码文件名与行号 ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- 平台图标通过 R.drawable.* 常量引用，勿被资源压缩误删 ----
-keep class com.kuaishou.app.R$drawable { *; }
-keep class com.kuaishou.app.R$layout { *; }
-keep class com.kuaishou.app.R$xml { *; }
