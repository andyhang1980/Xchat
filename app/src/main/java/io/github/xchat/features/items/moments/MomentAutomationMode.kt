package io.github.xchat.features.items.moments

import io.github.xchat.features.core.Feature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.preferences.WePrefs
import io.github.xchat.preferences.WePrefs.Companion.prefOption

@Feature(
    name = "朋友圈自动化模式",
    categories = ["朋友圈"],
    description = "配置朋友圈自动化的触发模式（定时、收到消息、手动）"
)
object MomentAutomationMode : SwitchFeature(), IResolveDex {

    private const val TAG = "MomentAutoMode"

    private var mode by prefOption("mam_mode", "timed")
    private var intervalMs by prefOption("mam_interval_ms", 30000)

    private val methodCheckMode by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "check auto mode")
        }
    }

    override fun resolveDex(dexKit: io.github.xchat.dexkit.DexKitBridge) {
        methodCheckMode.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "check auto mode")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodCheckMode.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持朋友圈自动化模式")
            return
        }
        runCatching {
            methodCheckMode.hookBefore {
                WeLogger.d(TAG, "moment automation mode: $mode, interval: $intervalMs")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
