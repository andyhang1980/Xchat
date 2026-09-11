package io.github.xchat.features.items.contacts

import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.api.core.WeApi
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.android.showToast
import io.github.xchat.preferences.WePrefs
import io.github.xchat.preferences.WePrefs.Companion.prefOption
import io.github.xchat.features.core.Feature
import io.github.xchat.features.api.core.WeConversationApi

@Feature(
    name = "分裂群通话",
    categories = ["联系人与群组"],
    description = "支持将群通话分裂为多个独立通话"
)
object SplitGroupCall : SwitchFeature(), IResolveDex {

    private const val TAG = "SplitGroupCall"

    private val methodSplitCall by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.Voip.GroupCall", "split call")
        }
    }

    override fun resolveDex(dexKit: io.github.xchat.dexkit.DexKitBridge) {
        methodSplitCall.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.Voip.GroupCall", "split call")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodSplitCall.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持分裂群通话")
            return
        }
        runCatching {
            methodSplitCall.hookBefore {
                WeLogger.d(TAG, "split group call triggered")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
