package io.github.xchat.features.items.contacts

import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.api.core.WeApi
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.features.core.Feature

@Feature(
    name = "自动添加附近的人",
    categories = ["联系人与群组"],
    description = "自动通过附近的人的好友请求"
)
object AutoAddNearbyFriends : SwitchFeature(), IResolveDex {

    private const val TAG = "AutoAddNearby"

    private val methodAddNearby by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.Nearby", "add nearby")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodAddNearby.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.Nearby", "add nearby")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodAddNearby.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持自动添加附近的人")
            return
        }
        runCatching {
            methodAddNearby.hookAfter {
                WeLogger.d(TAG, "auto add nearby triggered")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
