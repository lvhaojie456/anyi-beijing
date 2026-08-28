plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val apiBaseUrl = providers.gradleProperty("ANYI_API_BASE_URL")
    .orElse("https://api.anyibj.cn")
    .get()
val apiBaseUrls = providers.gradleProperty("ANYI_API_BASE_URLS")
    .orElse(apiBaseUrl)
    .get()
val wechatAppId = providers.gradleProperty("ANYI_WECHAT_APP_ID")
    .orElse("wx4ecdbc92d71a7ac8")
    .get()

fun buildConfigString(value: String): String {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

val releaseStoreFile: String? = System.getenv("ANYI_RELEASE_STORE_FILE")
val releaseStorePassword: String? = System.getenv("ANYI_RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("ANYI_RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("ANYI_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.anyi.memorial"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.anyi.memorial"
        minSdk = 24
        targetSdk = 36
        versionCode = 9
        versionName = "1.0.7-test"
        buildConfigField("String", "API_BASE_URL", buildConfigString(apiBaseUrl))
        buildConfigField("String", "API_BASE_URLS", buildConfigString(apiBaseUrls))
        buildConfigField("String", "WECHAT_APP_ID", buildConfigString(wechatAppId))
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.wechat.sdk.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
