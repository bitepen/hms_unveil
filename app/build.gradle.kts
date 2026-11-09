plugins {
    id("com.android.application")
}

android {
    namespace = "com.light.hmsvpnunveil"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.light.hmsvpnunveil"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 直接复用 debug 签名配置，解决未签名无法安装的问题
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}