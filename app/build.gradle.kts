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
        // 版本号规则：MAJOR.MINOR.PATCH-MMDD（如 2.0.2-1010）
        // versionCode 取 MAJOR*1000000 + MINOR*1000 + PATCH，保证随版本号单调递增
        versionCode = 2000002
        versionName = "2.0.2-1010"
    }

    buildTypes {
        release {
            // 开启 R8 代码压缩 + 资源压缩：降低「安装后占用」
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
        // 生成 BuildConfig，供 AppVersion 统一读取版本号
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

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
