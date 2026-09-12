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
    name = "朋友圈自动化类型规则",
    categories = ["朋友圈"],
    description = "配置朋友圈自动化的类型规则（图片、视频、文字等）"
)
object MomentTypeRule : SwitchFeature(), IResolveDex {

    private const val TAG = "MomentTypeRule"

    private var typeRule by prefOption("mtr_type_rule", "image")

    private val methodCheckType by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "check type rule")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodCheckType.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "check type rule")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodCheckType.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持类型规则")
            return
        }
        runCatching {
            methodCheckType.hookBefore {
                WeLogger.d(TAG, "type rule: $typeRule")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
