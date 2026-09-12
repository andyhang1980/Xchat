package io.github.xchat.features.items.moments

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.SwitchFeature
import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.features.core.Feature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.preferences.WePrefs
import io.github.xchat.preferences.WePrefs.Companion.prefOption

@Feature(
    name = "朋友圈自动化设置",
    categories = ["朋友圈"],
    description = "配置朋友圈自动化的全局设置（启用/禁用、触发条件等）"
)
object MomentsAutomationSettings : SwitchFeature(), IResolveDex {

    private const val TAG = "MomentsAutoSettings"

    private var autoEnabled by prefOption("mas_enabled", false)
    private var triggerType by prefOption("mas_trigger_type", "timed")

    private val methodGetSettings by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "get auto settings")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodGetSettings.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "get auto settings")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodGetSettings.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持朋友圈自动化设置")
            return
        }
        runCatching {
            methodGetSettings.hookBefore {
                WeLogger.d(TAG, "automation settings: enabled=$autoEnabled, trigger=$triggerType")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
