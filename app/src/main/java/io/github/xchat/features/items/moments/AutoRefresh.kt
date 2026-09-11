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
    name = "朋友圈自动刷新",
    categories = ["朋友圈"],
    description = "定时自动刷新朋友圈，获取最新动态"
)
object AutoRefresh : SwitchFeature(), IResolveDex {

    private const val TAG = "AutoRefresh"

    private val methodRefreshMoment by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "refresh sns")
        }
    }

    override fun resolveDex(dexKit: io.github.xchat.dexkit.DexKitBridge) {
        methodRefreshMoment.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "refresh sns")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodRefreshMoment.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持朋友圈自动刷新")
            return
        }
        runCatching {
            methodRefreshMoment.hookBefore {
                WeLogger.d(TAG, "auto refresh triggered")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
