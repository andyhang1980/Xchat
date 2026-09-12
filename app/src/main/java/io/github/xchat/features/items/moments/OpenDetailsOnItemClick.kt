package io.github.xchat.features.items.moments

import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.features.core.Feature

@Feature(
    name = "点击展开朋友圈详情",
    categories = ["朋友圈"],
    description = "点击朋友圈动态直接展开详情，无需长按"
)
object OpenDetailsOnItemClick : SwitchFeature(), IResolveDex {

    private const val TAG = "OpenDetailsClick"

    private val methodOnClick by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "on item click")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodOnClick.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "on item click")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodOnClick.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持点击展开详情")
            return
        }
        runCatching {
            methodOnClick.hookBefore {
                WeLogger.d(TAG, "open details on click")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
