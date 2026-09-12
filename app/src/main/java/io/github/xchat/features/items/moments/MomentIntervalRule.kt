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
    name = "朋友圈自动化时间间隔规则",
    categories = ["朋友圈"],
    description = "配置朋友圈自动化的时间间隔规则"
)
object MomentIntervalRule : SwitchFeature(), IResolveDex {

    private const val TAG = "MomentIntervalRule"

    private var intervalMs by prefOption("mir_interval_ms", 60000)
    private var maxPerDay by prefOption("mir_max_per_day", 10)

    private val methodGetInterval by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "get interval")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodGetInterval.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "get interval")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodGetInterval.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持时间间隔规则")
            return
        }
        runCatching {
            methodGetInterval.hookBefore {
                WeLogger.d(TAG, "interval rule: $intervalMs ms, max: $maxPerDay")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
