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
    name = "朋友圈自动化模式规则",
    categories = ["朋友圈"],
    description = "配置朋友圈自动化的模式规则（仅特定类型动态触发）"
)
object MomentModeRule : SwitchFeature(), IResolveDex {

    private const val TAG = "MomentModeRule"

    private var modeRule by prefOption("mmr_mode_rule", "all")

    private val methodCheckRule by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "check mode rule")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodCheckRule.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "check mode rule")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodCheckRule.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持模式规则")
            return
        }
        runCatching {
            methodCheckRule.hookBefore {
                WeLogger.d(TAG, "mode rule: $modeRule")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
