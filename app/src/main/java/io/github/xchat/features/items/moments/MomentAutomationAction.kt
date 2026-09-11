package io.github.xchat.features.items.moments

import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.preferences.WePrefs
import io.github.xchat.preferences.WePrefs.Companion.prefOption

@Feature(
    name = "朋友圈自动化动作",
    categories = ["朋友圈"],
    description = "配置朋友圈自动化的具体动作（点赞、评论、转发）"
)
object MomentAutomationAction : SwitchFeature(), IResolveDex {

    private const val TAG = "MomentAutoAction"

    private var actionType by prefOption("ma_action_type", "like")
    private var actionTarget by prefOption("ma_action_target", "")

    private val methodGetMoment by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "get sns info")
        }
    }

    override fun resolveDex(dexKit: io.github.xchat.dexkit.DexKitBridge) {
        methodGetMoment.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "get sns info")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodGetMoment.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持朋友圈自动化")
            return
        }
        runCatching {
            methodGetMoment.hookAfter {
                WeLogger.d(TAG, "moment automation action: $actionType on $actionTarget")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
