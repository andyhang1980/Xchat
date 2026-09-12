package io.github.xchat.features.items.beautify

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.features.core.Feature

@Feature(
    name = "美化底部导航栏",
    categories = ["界面美化"],
    description = "自定义微信底部导航栏样式"
)
object BottomNavBeautify : SwitchFeature(), IResolveDex {

    private const val TAG = "BottomNavBeautify"

    private val methodGetNavBg by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.UI", "get nav background")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodGetNavBg.find(dexKit, allowFailure = true) {
            matcher { usingEqStrings("MicroMsg.UI", "get nav background") }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodGetNavBg.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持底部导航栏美化")
            return
        }
        runCatching {
            methodGetNavBg.hookBefore {
                WeLogger.d(TAG, "beautifying bottom navigation")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
