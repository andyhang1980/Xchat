package io.github.xchat.features.items.beautify

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.features.core.Feature

@Feature(
    name = "隐藏最近页面",
    categories = ["界面美化"],
    description = "隐藏主页下滑「最近」页，去掉不需要的页面"
)
object HideRecentPage : SwitchFeature(), IResolveDex {

    private const val TAG = "HideRecentPage"

    private val methodGetRecentVisibility by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.HomePage", "get recent visibility")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodGetRecentVisibility.find(dexKit, allowFailure = true) {
            matcher { usingEqStrings("MicroMsg.HomePage", "get recent visibility") }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodGetRecentVisibility.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持隐藏最近页面")
            return
        }
        runCatching {
            methodGetRecentVisibility.hookBefore {
                args[0] = false
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
