package io.github.xchat.features.items.miniapps

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.preferences.WePrefs
import io.github.xchat.utils.TargetProcesses

@Feature(name = "跳过启动页面", categories = ["小程序"], description = "跳过小程序启动页面, 变相去广告 (实验性)")
object SkipSplash : SwitchFeature(), IResolveDex {

    private val methodShowSplash by dexMethod {
        searchPackages("com.tencent.mm.plugin.appbrand")
        matcher {
            declaredClass = "com.tencent.mm.plugin.appbrand.AppBrandRuntime"
            returnType = "void"
            paramCount = 0
            usingEqStrings(
                "public:prepare",
                "Loading页展示",
                "MicroMsg.AppBrandRuntime",
                "showSplash[AppBrandSplashAd], appId:%s, splash:%s"
            )
        }
    }

    override fun startup() {
        if (!TargetProcesses.isInMain && TargetProcesses.currentType != TargetProcesses.PROC_APPBRAND) return
        _isEnabled = WePrefs.getBoolOrFalse(name)
        if (_isEnabled) enable()
    }

    override fun onEnable() {
        methodShowSplash.hookBefore { result = null }
    }
}
