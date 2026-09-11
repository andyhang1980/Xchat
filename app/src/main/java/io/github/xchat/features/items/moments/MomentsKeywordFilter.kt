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
    name = "朋友圈关键词过滤",
    categories = ["朋友圈"],
    description = "根据关键词过滤朋友圈动态，不显示匹配的内容"
)
object MomentsKeywordFilter : SwitchFeature(), IResolveDex {

    private const val TAG = "MomentsKeywordFilter"

    private var keywords by prefOption("mkf_keywords", "")

    private val methodGetMomentList by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.SnsInfo", "get sns list")
        }
    }

    override fun resolveDex(dexKit: io.github.xchat.dexkit.DexKitBridge) {
        methodGetMomentList.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.SnsInfo", "get sns list")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodGetMomentList.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持朋友圈关键词过滤")
            return
        }
        runCatching {
            methodGetMomentList.hookBefore {
                val content = args.getOrNull(1) as? String ?: return@hookBefore
                val kwList = keywords.split(",").filter { it.isNotBlank() }
                if (kwList.any { content.contains(it) }) {
                    WeLogger.d(TAG, "filtered moment with keyword: $content")
                    args[1] = ""
                }
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
