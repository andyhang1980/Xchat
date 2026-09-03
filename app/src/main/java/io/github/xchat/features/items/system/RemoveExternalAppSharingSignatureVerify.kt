package io.github.xchat.features.items.system

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature

@Feature(name = "移除分享签名校验", categories = ["系统与隐私"], description = "移除第三方应用分享到微信的签名校验")
object RemoveExternalAppSharingSignatureVerify : SwitchFeature(), IResolveDex {

    private val methodSignCheck by dexMethod {
        searchPackages("com.tencent.mm.pluginsdk.model.app")
        matcher {
            usingEqStrings("checkAppSignature get local signature failed")
        }
    }

    override fun onEnable() {
        methodSignCheck.hookBefore {
            result = true
        }
    }
}
