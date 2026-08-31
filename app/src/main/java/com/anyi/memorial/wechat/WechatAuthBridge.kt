package com.anyi.memorial.wechat

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import com.anyi.memorial.BuildConfig
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.WXAPIFactory

sealed class WechatAuthResult {
    data class Success(val code: String) : WechatAuthResult()
    data class Failure(val message: String) : WechatAuthResult()
}

object WechatAuthBridge {
    private const val WechatPackageName = "com.tencent.mm"
    private var callback: ((WechatAuthResult) -> Unit)? = null

    fun isConfigured(): Boolean = BuildConfig.WECHAT_APP_ID.isNotBlank()

    fun startLogin(activity: Activity, onResult: (WechatAuthResult) -> Unit): Boolean {
        val appId = BuildConfig.WECHAT_APP_ID.trim()
        if (appId.isBlank()) {
            onResult(WechatAuthResult.Failure("微信登录还未配置 AppID"))
            return false
        }

        val api = WXAPIFactory.createWXAPI(activity.applicationContext, appId, true)
        api.registerApp(appId)
        if (!isWechatPackageInstalled(activity.packageManager)) {
            onResult(
                WechatAuthResult.Failure(
                    "当前设备未检测到微信，请确认微信安装在同一台设备和同一用户中"
                )
            )
            return false
        }
        if (!api.isWXAppInstalled) {
            onResult(WechatAuthResult.Failure("当前微信安装包未通过官方签名校验，请安装官方微信"))
            return false
        }

        callback = onResult
        val request = SendAuth.Req().apply {
            scope = "snsapi_userinfo"
            state = "anyi_wechat_${System.currentTimeMillis()}"
        }
        val sent = api.sendReq(request)
        if (!sent) {
            callback = null
            onResult(WechatAuthResult.Failure("微信登录唤起失败，请稍后重试"))
        }
        return sent
    }

    @Suppress("DEPRECATION")
    private fun isWechatPackageInstalled(packageManager: PackageManager): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    WechatPackageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                packageManager.getPackageInfo(WechatPackageName, 0)
            }
        }.isSuccess
    }

    fun handleResp(resp: BaseResp) {
        val next = callback ?: return
        callback = null
        if (resp is SendAuth.Resp && resp.errCode == BaseResp.ErrCode.ERR_OK && resp.code.isNotBlank()) {
            next(WechatAuthResult.Success(resp.code))
            return
        }

        val message = when (resp.errCode) {
            BaseResp.ErrCode.ERR_USER_CANCEL -> "已取消微信登录"
            BaseResp.ErrCode.ERR_AUTH_DENIED -> "微信授权被拒绝"
            else -> resp.errStr?.takeIf { it.isNotBlank() } ?: "微信登录失败"
        }
        next(WechatAuthResult.Failure(message))
    }
}
