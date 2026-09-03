package io.github.xchat.features.items.miniapps

import android.app.Activity
import com.tencent.mm.plugin.appbrand.ad.ui.AppBrandAdUI
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature

@Feature(name = "移除开屏广告", categories = ["小程序"], description = "跳过小程序开屏广告")
object RemoveSplashAds : SwitchFeature(), IResolveDex {

    private val methodIsAdContact by dexMethod {
        matcher {
            usingEqStrings("MicroMsg.AppBrandAdUtils[AppBrandSplashAd]", "isAdContact, appId:%s, canShowAd:%s")
        }
    }
    private val methodAdDataCallback by dexMethod {
        searchPackages("com.tencent.mm.plugin.appbrand.jsapi.auth")
        matcher {
            usingEqStrings(
                "MicroMsg.AppBrand.JsApiAdOperateWXData[AppBrandSplashAd]", "cgi callback, callbackId:%s, service not running or preloaded"
            )
        }
    }
    private val methodCheckCanShowAd by dexMethod {
        searchPackages("com.tencent.mm.plugin.appbrand")
        matcher {
            usingEqStrings("MicroMsg.AppBrandAdUtils[AppBrandSplashAd]", "checkCanShowAd, show ad (splash ad debug mode open)")
        }
    }

    override fun onEnable() {
        methodIsAdContact.hookBefore {
            result = false
        }

        methodAdDataCallback.hookBefore {
            result = null
        }

        methodCheckCanShowAd.hookBefore {
            result = false
        }

        AppBrandAdUI::class.java.hookBeforeOnCreate {
            val activity = thisObject as Activity
            activity.finish()
            result = null
        }
    }
}
