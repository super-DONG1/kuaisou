plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kuaishou.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kuaishou.app"
        minSdk = 24
        targetSdk = 37
        // 版本号规则：MAJOR.MINOR.PATCH-MMDD（如 2.0.3-1014 = 2.0.3 版、10-14 构建）
        // versionCode 取 MAJOR*1000000 + MINOR*1000 + PATCH，保证随版本号单调递增（Android 升级要求）
        versionCode = 2000003
        versionName = "2.0.3-1014"
    }

    buildTypes {
        release {
            // 开启 R8 代码压缩 + 资源压缩：dex 体积直接决定安装后的 AOT(oat) 编译产物大小，
            // 是降低「安装后占用」的主要手段（apk 变小 + oat 随之变小）。
            // 使用 proguard-android.txt（只压缩/混淆，不做激进代码优化）以降低 Compose/Miuix 的兼容风险。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // 个人发布：使用 debug 证书签名，产物可直接安装并上传 GitHub Release
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // 生成 BuildConfig（VERSION_NAME / VERSION_CODE），供 AppVersion 统一读取版本号，
        // 避免各页面硬编码版本号导致升级后显示不一致
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // 网络请求统一用系统 HttpURLConnection（SearchRepository），不引入 OkHttp/Okio 以省 dex 体积

    // Compose 由 Miuix 0.9.4 传递引入（1.12.0），显式声明与之同版本
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.foundation:foundation:1.12.0")

    // Miuix 0.9.4 弹窗/抽屉组件依赖 NavigationBackHandler，需要显式提供该运行库
    implementation("androidx.navigationevent:navigationevent-compose:1.1.2")

    // Miuix (HyperOS design language) 0.9.4
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")
}
