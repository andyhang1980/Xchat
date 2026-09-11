package io.github.xchat.features.items.moments

import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.api.core.WeApi
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.features.core.Feature

@Feature(
    name = "自动转发朋友圈",
    categories = ["朋友圈"],
    description = "自动将新发布的朋友圈动态转发到指定联系人"
)
object AutoRepostMoments : SwitchFeature(), IResolveDex {

    private const val TAG = "AutoRepostMoments"

    private val methodPublishMoment by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "publish sns")
        }
    }

    override fun resolveDex(dexKit: io.github.xchat.dexkit.DexKitBridge) {
        methodPublishMoment.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "publish sns")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodPublishMoment.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持自动转发朋友圈")
            return
        }
        runCatching {
            methodPublishMoment.hookAfter {
                WeLogger.d(TAG, "auto repost triggered")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
