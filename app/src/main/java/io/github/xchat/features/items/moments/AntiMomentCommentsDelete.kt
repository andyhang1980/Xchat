package io.github.xchat.features.items.moments

import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.api.core.WeApi
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.preferences.WePrefs
import io.github.xchat.preferences.WePrefs.Companion.prefOption
import io.github.xchat.features.core.Feature

@Feature(
    name = "拦截朋友圈评论删除",
    categories = ["朋友圈"],
    description = "拦截他人删除朋友圈评论，评论依然可见"
)
object AntiMomentCommentsDelete : SwitchFeature(), IResolveDex {

    private const val TAG = "AntiMomentCommentsDel"

    private val methodCommentDelete by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsComment", "delete comment")
        }
    }

    override fun resolveDex(dexKit: io.github.xchat.dexkit.DexKitBridge) {
        methodCommentDelete.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsComment", "delete comment")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodCommentDelete.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持拦截评论删除")
            return
        }
        runCatching {
            methodCommentDelete.hookBefore {
                WeLogger.d(TAG, "intercepting comment delete")
                args[0] = null
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
