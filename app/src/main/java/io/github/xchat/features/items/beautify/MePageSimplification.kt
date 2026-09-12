package io.github.xchat.features.items.beautify

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.preferences.WePrefs
import io.github.xchat.preferences.WePrefs.Companion.prefOption
import io.github.xchat.features.core.Feature

@Feature(
    name = "我页面精简",
    categories = ["界面美化"],
    description = "隐藏不需要的入口，让「我」页面更清爽"
)
object MePageSimplification : SwitchFeature(), IResolveDex {

    private const val TAG = "MePageSimplif"

    private var hideSettings by prefOption("mps_hide_settings", false)
    private var hidePay by prefOption("mps_hide_pay", false)
    private var hideCard by prefOption("mps_hide_card", false)
    private var hideFavorite by prefOption("mps_hide_favorite", false)

    private val methodGetMePageItems by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.MePage", "get page items")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodGetMePageItems.find(dexKit, allowFailure = true) {
            matcher { usingEqStrings("MicroMsg.MePage", "get page items") }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodGetMePageItems.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持我页面精简")
            return
        }
        runCatching {
            methodGetMePageItems.hookBefore {
                WeLogger.d(TAG, "simplifying me page: hideSettings=$hideSettings, hidePay=$hidePay")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
