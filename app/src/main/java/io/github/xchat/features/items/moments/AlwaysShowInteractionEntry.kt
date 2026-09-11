package io.github.xchat.features.items.moments

import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.features.core.Feature

@Feature(
    name = "始终显示互动入口",
    categories = ["朋友圈"],
    description = "始终显示朋友圈的点赞和评论入口（即使未关注）"
)
object AlwaysShowInteractionEntry : SwitchFeature(), IResolveDex {

    private const val TAG = "AlwaysShowEntry"

    private val methodGetEntryVisibility by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "get entry visibility")
        }
    }

    override fun resolveDex(dexKit: io.github.xchat.dexkit.DexKitBridge) {
        methodGetEntryVisibility.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "get entry visibility")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodGetEntryVisibility.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持始终显示互动入口")
            return
        }
        runCatching {
            methodGetEntryVisibility.hookBefore {
                args[0] = true
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
