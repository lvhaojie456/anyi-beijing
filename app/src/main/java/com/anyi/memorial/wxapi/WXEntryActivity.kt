package com.anyi.memorial.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.anyi.memorial.BuildConfig
import com.anyi.memorial.privacy.PrivacyConsentStore
import com.anyi.memorial.wechat.WechatAuthBridge
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory

class WXEntryActivity : Activity(), IWXAPIEventHandler {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleWechatIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWechatIntent(intent)
    }

    override fun onReq(req: BaseReq) = Unit

    override fun onResp(resp: BaseResp) {
        WechatAuthBridge.handleResp(resp)
        finish()
    }

    private fun handleWechatIntent(intent: Intent?) {
        if (!PrivacyConsentStore.isAccepted(this)) {
            finish()
            return
        }
        val appId = BuildConfig.WECHAT_APP_ID
        if (appId.isBlank()) {
            finish()
            return
        }
        val api = WXAPIFactory.createWXAPI(this, appId, false)
        api.handleIntent(intent, this)
    }
}
