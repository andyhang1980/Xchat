package io.github.xchat.features.items.beautify

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.features.core.Feature

@Feature(
    name = "对话框背景模糊",
    categories = ["界面美化"],
    description = "毛玻璃效果，视觉升级"
)
object DialogBlur : SwitchFeature(), IResolveDex {

    private const val TAG = "DialogBlur"

    private val methodApplyBlur by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.UI", "apply blur effect")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodApplyBlur.find(dexKit, allowFailure = true) {
            matcher { usingEqStrings("MicroMsg.UI", "apply blur effect") }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodApplyBlur.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持对话框背景模糊")
            return
        }
        runCatching {
            methodApplyBlur.hookBefore {
                WeLogger.d(TAG, "applying blur to dialog")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
