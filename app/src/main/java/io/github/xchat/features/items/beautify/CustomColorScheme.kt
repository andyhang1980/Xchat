package io.github.xchat.features.items.beautify

import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.preferences.WePrefs
import io.github.xchat.preferences.WePrefs.Companion.prefOption
import io.github.xchat.features.core.Feature

@Feature(
    name = "自定义配色",
    categories = ["界面美化"],
    description = "个性化界面颜色方案"
)
object CustomColorScheme : SwitchFeature(), IResolveDex {

    private const val TAG = "CustomColorScheme"

    private var accentColor by prefOption("ccs_accent_color", 0xFF07C160.toInt())
    private var backgroundColor by prefOption("ccs_bg_color", 0xFFFFFFFF.toInt())

    private val methodSetAccent by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.UI", "set accent color")
        }
    }

    override fun resolveDex(dexKit: io.github.xchat.dexkit.DexKitBridge) {
        methodSetAccent.find(dexKit, allowFailure = true) {
            matcher { usingEqStrings("MicroMsg.UI", "set accent color") }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodSetAccent.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持自定义配色")
            return
        }
        runCatching {
            methodSetAccent.hookBefore {
                args[0] = accentColor
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
